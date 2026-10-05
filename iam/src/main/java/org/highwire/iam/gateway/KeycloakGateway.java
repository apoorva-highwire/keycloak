package org.highwire.iam.gateway;

import java.util.List;
import java.util.Optional;

/**
 * The one boundary between this app and Keycloak's own SDK — no method
 * here returns a Keycloak Representation type, and no class outside
 * {@code gateway} imports {@code org.keycloak.admin.client.*} or
 * {@code org.keycloak.representations.*}. If Keycloak's SDK shape ever
 * changes, {@link KeycloakGatewayImpl} is the only file that moves.
 */
public interface KeycloakGateway {

    boolean realmExists(String realmName);

    /**
     * The authoritative "which journals does this publisher have" list —
     * a realm attribute (comma-separated), not derived from which
     * resources happen to exist yet. Lets a journal be declared before
     * its Authorization Services config (resource/policy/permission) is
     * fully set up, so a UI can show it as "pending" instead of not at
     * all. Empty list if the attribute was never set.
     */
    List<String> getRealmJournals(String realmName);

    /** Replaces the whole list — the caller decides the final set, not a merge. */
    void setRealmJournals(String realmName, List<String> journalCodes);

    /** @return the found user's id, or empty if no such user exists in that realm. */
    Optional<String> findUserId(String realmName, String username);

    /** Current journal_codes attribute value for this user, or an empty list if unset. */
    List<String> getJournalCodes(String realmName, String username);

    /** Idempotent: no-ops if the user already has this code. */
    void grantJournal(String realmName, String username, String journalCode);

    /** Idempotent: no-ops if the user doesn't have this code. */
    void revokeJournal(String realmName, String username, String journalCode);

    /**
     * Deliberately bounded, never "fetch the whole realm" — see
     * KeycloakGatewayImpl for exactly how:
     *
     * @param journalCodeFilter if non-null, pushed down as a server-side
     *     Keycloak attribute search (journal_codes:X) — bounded by that
     *     one journal's own subscriber count, not the realm's total.
     * @param page zero-based; ignored (whole filtered set returned) when
     *     journalCodeFilter is set, since Keycloak has no server-side
     *     count for attribute searches — ENFORCED (not ignorable) when
     *     journalCodeFilter is null, where the realm could hold any
     *     number of users.
     */
    PagedUsers listUsers(String realmName, String journalCodeFilter, int page, int size);

    // --- User <-> Group/Role assignment — the mechanism a UI drives:
    // assign a user to a group (bundling several roles at once, auto-
    // granted via Keycloak's own group-role inheritance, no per-role
    // assignment needed), or directly to a role for the rare case a
    // single role doesn't warrant a whole group. ---

    /** Idempotent: no-ops if the user is already in this group. Throws ResourceNotFoundException if the group doesn't exist. */
    void addUserToGroup(String realmName, String username, String groupName);

    /** Idempotent: no-ops if the user isn't in this group. */
    void removeUserFromGroup(String realmName, String username, String groupName);

    /** Groups this user directly belongs to — and so, transitively, every role each of those groups bundles. */
    List<String> getUserGroups(String realmName, String username);

    /** Idempotent: no-ops if the user already has this role directly. Throws ResourceNotFoundException if the role doesn't exist. */
    void addRoleToUser(String realmName, String username, String roleName);

    /** Idempotent: no-ops if the user doesn't have this role directly. */
    void removeRoleFromUser(String realmName, String username, String roleName);

    /** Roles assigned directly to this user — NOT roles inherited from a group; see getEffectiveRolesForUser for the full picture. */
    List<String> getUserRoles(String realmName, String username);

    /** Every role this user effectively has — direct AND inherited from any group they belong to. */
    List<String> getEffectiveRolesForUser(String realmName, String username);

    // --- Roles — API authorization permissions (e.g. "journal-read",
    // "journal-write"), distinct from the editorial roles
    // (reader/author/.../publisher-admin) scripts/<realm>/03 manages. ---

    /** Throws ResourceAlreadyExistsException if the role already exists. */
    void createRole(String realmName, String roleName, String description);

    /**
     * page is zero-based. Roles per realm are a small, bounded set
     * (unlike users) — no server-side count exists for them (confirmed
     * against the real RolesResource API), so this fetches the whole
     * set once and paginates in memory; that's safe here specifically
     * because "whole set" is never large for roles the way it can be
     * for users.
     */
    PagedRoles listRoles(String realmName, int page, int size);

    Optional<RoleSummary> getRole(String realmName, String roleName);

    /** Idempotent: no-ops if the role doesn't exist. */
    void deleteRole(String realmName, String roleName);

    // --- Groups — bundle several roles so an API consumer is assigned
    // once (to the group), not role by role. ---

    /** Throws ResourceAlreadyExistsException if the group already exists. */
    void createGroup(String realmName, String groupName);

    /** page is zero-based — real server-side pagination via Keycloak's own first/max + count(), confirmed available on GroupsResource unlike RolesResource. */
    PagedGroups listGroups(String realmName, int page, int size);

    Optional<GroupSummary> getGroup(String realmName, String groupName);

    /** Idempotent: no-ops if the group doesn't exist. */
    void deleteGroup(String realmName, String groupName);

    /** Idempotent: no-ops if the group already has this role. Throws ResourceNotFoundException if the role doesn't exist. */
    void addRoleToGroup(String realmName, String groupName, String roleName);

    /** Idempotent: no-ops if the group doesn't have this role. */
    void removeRoleFromGroup(String realmName, String groupName, String roleName);

    // --- Resources/Policies/Permissions — Keycloak Authorization Services
    // (UMA2), scoped to one Keycloak client acting as the "resource
    // server" (resourceServerClientId). One resource per journal is the
    // intended shape; policies are realm-role-based; permissions tie a
    // resource's scopes to the policies that grant them. The actual
    // runtime allow/deny check is a live, cached UMA2 ticket exchange —
    // not performed here, this is admin/config only. ---

    /** Throws ResourceAlreadyExistsException if a resource with this name already exists under this client. */
    void createResource(String realmName, String resourceServerClientId, String resourceName, List<String> scopeNames);

    Optional<ResourceSummary> getResource(String realmName, String resourceServerClientId, String resourceName);

    /** page is zero-based — same in-memory pagination as listRoles(); no server-side count exists for this endpoint either. */
    PagedResources listResources(String realmName, String resourceServerClientId, int page, int size);

    /** Idempotent: no-ops if the resource doesn't exist. */
    void deleteResource(String realmName, String resourceServerClientId, String resourceName);

    /**
     * Role-based policy: satisfied if the caller holds ANY ONE of
     * roleNames (each added as non-required — logical OR, confirmed
     * against RolePolicyRepresentation.addRole's default). Throws
     * ResourceAlreadyExistsException if the name is already taken.
     */
    void createRolePolicy(String realmName, String resourceServerClientId, String policyName, List<String> roleNames);

    Optional<PolicySummary> getPolicy(String realmName, String resourceServerClientId, String policyName);

    /** page is zero-based — same in-memory pagination as listRoles(); a realm's policies are a small, admin-curated set. */
    PagedPolicies listPolicies(String realmName, String resourceServerClientId, int page, int size);

    /** Idempotent: no-ops if the policy doesn't exist. */
    void deletePolicy(String realmName, String resourceServerClientId, String policyName);

    /**
     * Ties a resource's scopes to the policies that grant them.
     * decisionStrategy=AFFIRMATIVE — satisfied if ANY ONE assigned policy
     * passes, the common "has role A or role B" shape; Keycloak's own
     * default (UNANIMOUS) would require every assigned policy to pass,
     * which is rarely what a multi-policy permission wants. Throws
     * ResourceAlreadyExistsException if the name is already taken,
     * ResourceNotFoundException if resourceName or any policyName doesn't exist.
     */
    void createScopePermission(String realmName, String resourceServerClientId, String permissionName,
            String resourceName, List<String> scopeNames, List<String> policyNames);

    Optional<PermissionSummary> getPermission(String realmName, String resourceServerClientId, String permissionName);

    /** page is zero-based — same in-memory pagination as listRoles(); a resource server's permissions are a small, admin-curated set. */
    PagedPermissions listPermissions(String realmName, String resourceServerClientId, int page, int size);

    /**
     * Every permission effectively granted to roleName — found by first
     * finding every policy that includes this role (role-based policies
     * can list several roles; any ONE of them satisfies the policy), then
     * every permission that references any of those policies. This is
     * what "click a role, see how many permissions it has" actually
     * means in this model: a role has no direct permissions of its own,
     * only ones reached through a policy.
     */
    List<PermissionSummary> listPermissionsForRole(String realmName, String resourceServerClientId, String roleName);

    /** Idempotent: no-ops if the permission doesn't exist. */
    void deletePermission(String realmName, String resourceServerClientId, String permissionName);

    record ResourceSummary(String name, List<String> scopes) {
    }

    record PagedResources(List<ResourceSummary> content, long totalElements) {
    }

    record PolicySummary(String name, List<String> roleNames) {
    }

    record PagedPolicies(List<PolicySummary> content, long totalElements) {
    }

    record PermissionSummary(String name, String resourceName, List<String> scopes, List<String> policyNames) {
    }

    record PagedPermissions(List<PermissionSummary> content, long totalElements) {
    }

    record UserSummary(String username, List<String> journalCodes) {
    }

    record PagedUsers(List<UserSummary> content, long totalElements) {
    }

    record RoleSummary(String name, String description) {
    }

    record GroupSummary(String name, List<String> roles) {
    }

    record PagedRoles(List<RoleSummary> content, long totalElements) {
    }

    record PagedGroups(List<GroupSummary> content, long totalElements) {
    }
}
