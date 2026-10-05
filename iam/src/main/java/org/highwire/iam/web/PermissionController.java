package org.highwire.iam.web;

import java.util.List;

import org.highwire.iam.gateway.KeycloakGateway;
import org.highwire.iam.gateway.KeycloakGateway.PagedPermissions;
import org.highwire.iam.web.dto.CreateScopePermissionRequest;
import org.highwire.iam.web.dto.PermissionResponse;
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
 * Scope permissions — ties a resource's scopes to the policies that
 * grant them. This is the piece a consuming service's UMA2 decision
 * check (audience={clientId}, permission={resourceName}#{scope}) is
 * actually evaluated against; see auth-sdk's AuthorizationCheckService
 * for that runtime side. decisionStrategy is always AFFIRMATIVE — any
 * one assigned policy passing is enough.
 */
@RestController
@RequestMapping("/api/v1/realms/{realm}/clients/{clientId}/permissions")
@Tag(name = "Permissions", description = "Scope-based Authorization Services (UMA2) permissions")
public class PermissionController {

    private final KeycloakGateway keycloak;
    private final MessageInformationFactory messageInformationFactory;

    public PermissionController(KeycloakGateway keycloak, MessageInformationFactory messageInformationFactory) {
        this.keycloak = keycloak;
        this.messageInformationFactory = messageInformationFactory;
    }

    @PostMapping
    @Operation(summary = "Create a scope permission")
    public ResponseEntity<ApiResponse<PermissionResponse>> create(@PathVariable String realm, @PathVariable String clientId,
            @Valid @RequestBody CreateScopePermissionRequest request) {
        keycloak.createScopePermission(realm, clientId, request.name(), request.resourceName(),
                request.scopeNames(), request.policyNames());
        PermissionResponse created = new PermissionResponse(
                request.name(), request.resourceName(), request.scopeNames(), request.policyNames());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "Permission created", messageInformationFactory.create()));
    }

    @GetMapping
    @Operation(summary = "List permissions under this client",
            description = "Pass roleName to see only permissions effectively granted to that role — "
                    + "every permission reachable through any policy that includes it. page/size are ignored when roleName is given.")
    public PagedApiResponse<PermissionResponse> list(
            @PathVariable String realm,
            @PathVariable String clientId,
            @RequestParam(required = false) String roleName,
            @Parameter(description = "1-based — page=1 is the first page.")
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (page < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page must be >= 1");
        }
        if (roleName != null) {
            List<PermissionResponse> content = keycloak.listPermissionsForRole(realm, clientId, roleName).stream()
                    .map(p -> new PermissionResponse(p.name(), p.resourceName(), p.scopes(), p.policyNames()))
                    .toList();
            return PagedApiResponse.success(
                    content,
                    PaginationInfo.of(1, content.size(), content.size()),
                    messageInformationFactory.create());
        }
        PagedPermissions result = keycloak.listPermissions(realm, clientId, page - 1, size);
        List<PermissionResponse> content = result.content().stream()
                .map(p -> new PermissionResponse(p.name(), p.resourceName(), p.scopes(), p.policyNames()))
                .toList();
        return PagedApiResponse.success(
                content,
                PaginationInfo.of(page, size, result.totalElements()),
                messageInformationFactory.create());
    }

    @GetMapping("/{permissionName}")
    @Operation(summary = "Get one permission")
    public ApiResponse<PermissionResponse> get(@PathVariable String realm, @PathVariable String clientId,
            @PathVariable String permissionName) {
        PermissionResponse permission = keycloak.getPermission(realm, clientId, permissionName)
                .map(p -> new PermissionResponse(p.name(), p.resourceName(), p.scopes(), p.policyNames()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No permission '" + permissionName + "'"));
        return ApiResponse.success(permission, null, messageInformationFactory.create());
    }

    @DeleteMapping("/{permissionName}")
    @Operation(summary = "Delete a permission")
    public ApiResponse<Void> delete(@PathVariable String realm, @PathVariable String clientId,
            @PathVariable String permissionName) {
        keycloak.deletePermission(realm, clientId, permissionName);
        return ApiResponse.success(null, "Permission deleted", messageInformationFactory.create());
    }
}
