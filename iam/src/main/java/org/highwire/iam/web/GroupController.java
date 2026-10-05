package org.highwire.iam.web;

import java.util.List;

import org.highwire.iam.gateway.KeycloakGateway;
import org.highwire.iam.gateway.KeycloakGateway.PagedGroups;
import org.highwire.iam.web.dto.AssignRoleRequest;
import org.highwire.iam.web.dto.CreateGroupRequest;
import org.highwire.iam.web.dto.GroupResponse;
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
 * Groups bundle several API-authorization roles so a consumer (a
 * third-party client) is assigned ONCE — to the group — instead of role
 * by role.
 */
@RestController
@RequestMapping("/api/v1/realms/{realm}/groups")
@Tag(name = "Groups", description = "Bundles of API-authorization roles")
public class GroupController {

    private static final int DEFAULT_PAGE_SIZE = 20;

    private final KeycloakGateway keycloak;
    private final MessageInformationFactory messageInformationFactory;

    public GroupController(KeycloakGateway keycloak, MessageInformationFactory messageInformationFactory) {
        this.keycloak = keycloak;
        this.messageInformationFactory = messageInformationFactory;
    }

    @PostMapping
    @Operation(summary = "Create a group")
    public ResponseEntity<ApiResponse<GroupResponse>> create(@PathVariable String realm,
            @Valid @RequestBody CreateGroupRequest request) {
        keycloak.createGroup(realm, request.name());
        GroupResponse created = new GroupResponse(request.name(), List.of());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "Group created", messageInformationFactory.create()));
    }

    @GetMapping
    @Operation(summary = "List groups in a realm")
    public PagedApiResponse<GroupResponse> list(
            @PathVariable String realm,
            @Parameter(description = "1-based — page=1 is the first page.")
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) int size) {
        if (page < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page must be >= 1");
        }
        PagedGroups result = keycloak.listGroups(realm, page - 1, size);
        List<GroupResponse> content = result.content().stream()
                .map(g -> new GroupResponse(g.name(), g.roles()))
                .toList();
        return PagedApiResponse.success(
                content,
                PaginationInfo.of(page, size, result.totalElements()),
                messageInformationFactory.create());
    }

    @GetMapping("/{groupName}")
    @Operation(summary = "Get one group, including its bundled roles")
    public ApiResponse<GroupResponse> get(@PathVariable String realm, @PathVariable String groupName) {
        GroupResponse group = keycloak.getGroup(realm, groupName)
                .map(g -> new GroupResponse(g.name(), g.roles()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No group '" + groupName + "'"));
        return ApiResponse.success(group, null, messageInformationFactory.create());
    }

    @DeleteMapping("/{groupName}")
    @Operation(summary = "Delete a group")
    public ApiResponse<Void> delete(@PathVariable String realm, @PathVariable String groupName) {
        keycloak.deleteGroup(realm, groupName);
        return ApiResponse.success(null, "Group deleted", messageInformationFactory.create());
    }

    @PostMapping("/{groupName}/roles")
    @Operation(summary = "Bundle a role into this group",
            description = "Idempotent — adding a role the group already has is a no-op.")
    public ApiResponse<Void> addRole(@PathVariable String realm, @PathVariable String groupName,
            @Valid @RequestBody AssignRoleRequest request) {
        keycloak.addRoleToGroup(realm, groupName, request.roleName());
        return ApiResponse.success(null, "Role added to group", messageInformationFactory.create());
    }

    @DeleteMapping("/{groupName}/roles/{roleName}")
    @Operation(summary = "Remove a role from this group",
            description = "Idempotent — removing a role the group doesn't have is a no-op.")
    public ApiResponse<Void> removeRole(@PathVariable String realm, @PathVariable String groupName,
            @PathVariable String roleName) {
        keycloak.removeRoleFromGroup(realm, groupName, roleName);
        return ApiResponse.success(null, "Role removed from group", messageInformationFactory.create());
    }
}
