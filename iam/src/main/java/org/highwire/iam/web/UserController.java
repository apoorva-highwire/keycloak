package org.highwire.iam.web;

import org.highwire.authsdk.caller.CallerContext;
import org.highwire.authsdk.caller.CallerInfo;
import org.highwire.authsdk.caller.CallerScope;
import org.highwire.iam.gateway.KeycloakGateway;
import org.highwire.iam.gateway.KeycloakGateway.PagedUsers;
import org.highwire.iam.web.dto.AssignGroupRequest;
import org.highwire.iam.web.dto.AssignRoleRequest;
import org.highwire.iam.web.dto.UserAccessResponse;
import org.highwire.iam.web.dto.UserSummaryResponse;
import org.highwire.iam.web.response.ApiResponse;
import org.highwire.iam.web.response.MessageInformationFactory;
import org.highwire.iam.web.response.PagedApiResponse;
import org.highwire.iam.web.response.PaginationInfo;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Visibility rule, enforced from CallerInfo (resolved by auth-sdk's
 * CallerResolvingFilter from the validated token — never from a
 * client-supplied parameter, which could lie):
 *
 *   - JOURNAL scope (a Drupal site, one journal): sees only users of
 *     ITS OWN journal — any journalCode the caller passes is ignored
 *     and overridden to caller.journal(), it can't ask to see another
 *     journal's users by passing a different value.
 *   - PUBLISHER/SERVICE scope: sees everyone in the realm by default,
 *     or one journal's users if journalCode is given — this caller is
 *     trusted to ask for either.
 */
@RestController
@RequestMapping("/api/v1/realms/{realm}/users")
@Tag(name = "Users", description = "Realm users, visibility scoped by the caller's own journal/publisher/service identity")
public class UserController {

    private static final int DEFAULT_PAGE_SIZE = 20;

    private final KeycloakGateway keycloak;
    private final MessageInformationFactory messageInformationFactory;

    public UserController(KeycloakGateway keycloak, MessageInformationFactory messageInformationFactory) {
        this.keycloak = keycloak;
        this.messageInformationFactory = messageInformationFactory;
    }

    @GetMapping
    @Operation(summary = "List users in a realm",
            description = "A JOURNAL-scoped caller (e.g. a Drupal site) always sees only its own "
                    + "journal's users, regardless of any journalCode passed here. A PUBLISHER/SERVICE "
                    + "caller sees everyone by default, or one journal's users if journalCode is given. "
                    + "page is 1-based (page=1 is the first page).")
    public PagedApiResponse<UserSummaryResponse> list(
            @PathVariable String realm,
            @Parameter(description = "Restrict to one journal's users. Ignored/overridden for a JOURNAL-scoped caller.")
            @RequestParam(required = false) String journalCode,
            @Parameter(description = "1-based — page=1 is the first page.")
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) int size) {

        if (page < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page must be >= 1");
        }

        CallerInfo caller = CallerContext.get();
        String effectiveFilter = resolveFilter(caller, journalCode);

        // Keycloak's own paging (and KeycloakGateway's contract) is
        // 0-based — the 1-based/0-based conversion happens HERE, at the
        // API boundary, so the gateway/Keycloak-facing code never has to
        // know this API chose a different convention than Keycloak's own.
        int zeroBasedPage = page - 1;
        PagedUsers result = keycloak.listUsers(realm, effectiveFilter, zeroBasedPage, size);
        var content = result.content().stream()
                .map(u -> new UserSummaryResponse(u.username(), u.journalCodes()))
                .toList();

        return PagedApiResponse.success(
                content,
                PaginationInfo.of(page, size, result.totalElements()),
                messageInformationFactory.create());
    }

    @GetMapping("/{username}/access")
    @Operation(summary = "Get a user's groups and roles",
            description = "effectiveRoles includes roles inherited from any group the user belongs to, "
                    + "not just directRoles — what the user can actually do.")
    public ApiResponse<UserAccessResponse> getAccess(@PathVariable String realm, @PathVariable String username) {
        requireAdminCaller();
        UserAccessResponse access = new UserAccessResponse(
                username,
                keycloak.getUserGroups(realm, username),
                keycloak.getUserRoles(realm, username),
                keycloak.getEffectiveRolesForUser(realm, username));
        return ApiResponse.success(access, null, messageInformationFactory.create());
    }

    @PostMapping("/{username}/groups")
    @Operation(summary = "Add a user to a group",
            description = "Idempotent — adding a user to a group they're already in is a no-op. Every role "
                    + "bundled into that group is auto-granted to the user by Keycloak itself, no separate "
                    + "role assignment needed.")
    public ApiResponse<Void> addGroup(@PathVariable String realm, @PathVariable String username,
            @Valid @RequestBody AssignGroupRequest request) {
        requireAdminCaller();
        keycloak.addUserToGroup(realm, username, request.groupName());
        return ApiResponse.success(null, "User added to group", messageInformationFactory.create());
    }

    @DeleteMapping("/{username}/groups/{groupName}")
    @Operation(summary = "Remove a user from a group",
            description = "Idempotent — removing a user from a group they're not in is a no-op.")
    public ApiResponse<Void> removeGroup(@PathVariable String realm, @PathVariable String username,
            @PathVariable String groupName) {
        requireAdminCaller();
        keycloak.removeUserFromGroup(realm, username, groupName);
        return ApiResponse.success(null, "User removed from group", messageInformationFactory.create());
    }

    @PostMapping("/{username}/roles")
    @Operation(summary = "Assign a role directly to a user",
            description = "For the rare case a single role doesn't warrant a whole group — the intended "
                    + "mechanism is still groups (see POST .../groups). Idempotent.")
    public ApiResponse<Void> addRole(@PathVariable String realm, @PathVariable String username,
            @Valid @RequestBody AssignRoleRequest request) {
        requireAdminCaller();
        keycloak.addRoleToUser(realm, username, request.roleName());
        return ApiResponse.success(null, "Role added to user", messageInformationFactory.create());
    }

    @DeleteMapping("/{username}/roles/{roleName}")
    @Operation(summary = "Remove a role directly assigned to a user",
            description = "Idempotent — removing a role the user doesn't directly have is a no-op. Does not "
                    + "touch any role the user only has by way of group membership.")
    public ApiResponse<Void> removeRole(@PathVariable String realm, @PathVariable String username,
            @PathVariable String roleName) {
        requireAdminCaller();
        keycloak.removeRoleFromUser(realm, username, roleName);
        return ApiResponse.success(null, "Role removed from user", messageInformationFactory.create());
    }

    /** Group/role assignment is an administrative action — a JOURNAL-scoped caller (a Drupal site) never does this. */
    private void requireAdminCaller() {
        if (CallerContext.get().scope() == CallerScope.JOURNAL) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not permitted for a journal-scoped caller");
        }
    }

    private String resolveFilter(CallerInfo caller, String requestedJournalCode) {
        if (caller.scope() == CallerScope.JOURNAL) {
            if (caller.journal() == null) {
                // Shouldn't happen — JOURNAL scope is only assigned when a
                // journal was actually resolved — but fail closed, not open.
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No journal resolved for this caller");
            }
            return caller.journal();
        }
        return requestedJournalCode;
    }
}
