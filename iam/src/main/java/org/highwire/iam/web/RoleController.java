package org.highwire.iam.web;

import java.util.List;

import org.highwire.iam.gateway.KeycloakGateway;
import org.highwire.iam.gateway.KeycloakGateway.PagedRoles;
import org.highwire.iam.web.dto.CreateRoleRequest;
import org.highwire.iam.web.dto.RoleResponse;
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
 * API-authorization roles (e.g. "journal-read", "journal-write") — not
 * the editorial roles (reader/author/reviewer/editor/publisher-admin)
 * scripts/&lt;realm&gt;/03-create-admin-user.sh manages; those are fixed
 * and identical across every realm, these are created ad hoc per
 * whatever a consuming service's authorization model needs.
 */
@RestController
@RequestMapping("/api/v1/realms/{realm}/roles")
@Tag(name = "Roles", description = "API-authorization role definitions")
public class RoleController {

    private static final int DEFAULT_PAGE_SIZE = 20;

    private final KeycloakGateway keycloak;
    private final MessageInformationFactory messageInformationFactory;

    public RoleController(KeycloakGateway keycloak, MessageInformationFactory messageInformationFactory) {
        this.keycloak = keycloak;
        this.messageInformationFactory = messageInformationFactory;
    }

    @PostMapping
    @Operation(summary = "Create a role definition")
    public ResponseEntity<ApiResponse<RoleResponse>> create(@PathVariable String realm,
            @Valid @RequestBody CreateRoleRequest request) {
        keycloak.createRole(realm, request.name(), request.description());
        RoleResponse created = new RoleResponse(request.name(), request.description());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "Role created", messageInformationFactory.create()));
    }

    @GetMapping
    @Operation(summary = "List role definitions in a realm")
    public PagedApiResponse<RoleResponse> list(
            @PathVariable String realm,
            @Parameter(description = "1-based — page=1 is the first page.")
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) int size) {
        if (page < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page must be >= 1");
        }
        PagedRoles result = keycloak.listRoles(realm, page - 1, size);
        List<RoleResponse> content = result.content().stream()
                .map(r -> new RoleResponse(r.name(), r.description()))
                .toList();
        return PagedApiResponse.success(
                content,
                PaginationInfo.of(page, size, result.totalElements()),
                messageInformationFactory.create());
    }

    @GetMapping("/{roleName}")
    @Operation(summary = "Get one role definition")
    public ApiResponse<RoleResponse> get(@PathVariable String realm, @PathVariable String roleName) {
        RoleResponse role = keycloak.getRole(realm, roleName)
                .map(r -> new RoleResponse(r.name(), r.description()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No role '" + roleName + "'"));
        return ApiResponse.success(role, null, messageInformationFactory.create());
    }

    @DeleteMapping("/{roleName}")
    @Operation(summary = "Delete a role definition")
    public ApiResponse<Void> delete(@PathVariable String realm, @PathVariable String roleName) {
        keycloak.deleteRole(realm, roleName);
        return ApiResponse.success(null, "Role deleted", messageInformationFactory.create());
    }
}
