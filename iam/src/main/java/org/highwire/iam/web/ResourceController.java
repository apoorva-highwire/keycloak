package org.highwire.iam.web;

import java.util.List;

import org.highwire.iam.gateway.KeycloakGateway;
import org.highwire.iam.gateway.KeycloakGateway.PagedResources;
import org.highwire.iam.web.dto.CreateResourceRequest;
import org.highwire.iam.web.dto.ResourceResponse;
import org.highwire.iam.web.response.ApiResponse;
import org.highwire.iam.web.response.MessageInformationFactory;
import org.highwire.iam.web.response.PagedApiResponse;
import org.highwire.iam.web.response.PaginationInfo;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Keycloak Authorization Services (UMA2) resources — the objects a
 * permission check is actually about. Intended shape: one resource per
 * journal, scoped under resourceServerClientId (the Keycloak client that
 * "owns" that authorization model, e.g. a journal-facing API). This is
 * admin/config only; the runtime allow/deny check is a separate, cached
 * UMA2 ticket exchange performed by consuming services via auth-sdk, not
 * by iam-service.
 */
@RestController
@RequestMapping("/api/v1/realms/{realm}/clients/{clientId}/resources")
@Tag(name = "Resources", description = "Authorization Services (UMA2) resources")
public class ResourceController {

    private static final int DEFAULT_PAGE_SIZE = 20;

    private final KeycloakGateway keycloak;
    private final MessageInformationFactory messageInformationFactory;

    public ResourceController(KeycloakGateway keycloak, MessageInformationFactory messageInformationFactory) {
        this.keycloak = keycloak;
        this.messageInformationFactory = messageInformationFactory;
    }

    @PostMapping
    @Operation(summary = "Create a resource", description = "e.g. one resource per journal, with scopes like 'read'/'write'.")
    public ResponseEntity<ApiResponse<ResourceResponse>> create(@PathVariable String realm, @PathVariable String clientId,
            @Valid @RequestBody CreateResourceRequest request) {
        keycloak.createResource(realm, clientId, request.name(), request.scopes());
        ResourceResponse created = new ResourceResponse(request.name(), request.scopes());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "Resource created", messageInformationFactory.create()));
    }

    @GetMapping
    @Operation(summary = "List resources under this client")
    public PagedApiResponse<ResourceResponse> list(
            @PathVariable String realm,
            @PathVariable String clientId,
            @Parameter(description = "1-based — page=1 is the first page.")
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) int size) {
        if (page < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page must be >= 1");
        }
        PagedResources result = keycloak.listResources(realm, clientId, page - 1, size);
        List<ResourceResponse> content = result.content().stream()
                .map(r -> new ResourceResponse(r.name(), r.scopes()))
                .toList();
        return PagedApiResponse.success(
                content,
                PaginationInfo.of(page, size, result.totalElements()),
                messageInformationFactory.create());
    }

    @GetMapping("/{resourceName}")
    @Operation(summary = "Get one resource")
    public ApiResponse<ResourceResponse> get(@PathVariable String realm, @PathVariable String clientId,
            @PathVariable String resourceName) {
        ResourceResponse resource = keycloak.getResource(realm, clientId, resourceName)
                .map(r -> new ResourceResponse(r.name(), r.scopes()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No resource '" + resourceName + "'"));
        return ApiResponse.success(resource, null, messageInformationFactory.create());
    }

    @DeleteMapping("/{resourceName}")
    @Operation(summary = "Delete a resource")
    public ApiResponse<Void> delete(@PathVariable String realm, @PathVariable String clientId,
            @PathVariable String resourceName) {
        keycloak.deleteResource(realm, clientId, resourceName);
        return ApiResponse.success(null, "Resource deleted", messageInformationFactory.create());
    }
}
