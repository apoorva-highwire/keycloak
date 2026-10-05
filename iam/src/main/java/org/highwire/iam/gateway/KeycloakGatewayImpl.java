package org.highwire.iam.gateway;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import org.highwire.iam.exception.KeycloakOperationException;
import org.highwire.iam.exception.RealmNotFoundException;
import org.highwire.iam.exception.ResourceAlreadyExistsException;
import org.highwire.iam.exception.ResourceNotFoundException;
import org.highwire.iam.exception.UserNotFoundException;
import org.highwire.authsdk.logging.AppLogger;
import org.highwire.authsdk.logging.Loggers;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.AuthorizationResource;
import org.keycloak.admin.client.resource.ScopePermissionResource;
import org.keycloak.admin.client.resource.GroupResource;
import org.keycloak.admin.client.resource.GroupsResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.authorization.DecisionStrategy;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.representations.idm.authorization.RolePolicyRepresentation;
import org.keycloak.representations.idm.authorization.ScopePermissionRepresentation;
import org.keycloak.representations.idm.authorization.ScopeRepresentation;
import org.springframework.stereotype.Component;

@Component
public class KeycloakGatewayImpl implements KeycloakGateway {

    private static final AppLogger log = Loggers.of(KeycloakGatewayImpl.class);

    private static final String JOURNAL_CODES_ATTRIBUTE = "journal_codes";

    private final Keycloak keycloak;

    public KeycloakGatewayImpl(Keycloak keycloak) {
        this.keycloak = keycloak;
    }

    @Override
    public boolean realmExists(String realmName) {
        try {
            keycloak.realm(realmName).toRepresentation();
            return true;
        } catch (WebApplicationException e) {
            if (e.getResponse().getStatus() == Response.Status.NOT_FOUND.getStatusCode()) {
                return false;
            }
            throw new KeycloakOperationException("Failed to check realm '" + realmName + "'", e);
        }
    }

    @Override
    public Optional<String> findUserId(String realmName, String username) {
        List<UserRepresentation> matches = realm(realmName).users().search(username, true);
        return matches.stream()
                .filter(u -> username.equals(u.getUsername()))
                .findFirst()
                .map(UserRepresentation::getId);
    }

    @Override
    public List<String> getJournalCodes(String realmName, String username) {
        UserResource user = userResource(realmName, username);
        UserRepresentation rep = user.toRepresentation();
        if (rep.getAttributes() == null) {
            return List.of();
        }
        List<String> codes = rep.getAttributes().get(JOURNAL_CODES_ATTRIBUTE);
        return codes != null ? List.copyOf(codes) : List.of();
    }

    @Override
    public void grantJournal(String realmName, String username, String journalCode) {
        UserResource user = userResource(realmName, username);
        UserRepresentation rep = user.toRepresentation();

        List<String> current = currentCodes(rep);
        if (current.contains(journalCode)) {
            log.info("grantJournal: '{}' already has journal '{}' in realm '{}', no-op", username, journalCode, realmName);
            return;
        }
        current.add(journalCode);
        setCodes(rep, current);
        try {
            user.update(rep);
            log.info("Granted journal '{}' to user '{}' in realm '{}'", journalCode, username, realmName);
        } catch (WebApplicationException e) {
            log.error("Failed to grant journal '" + journalCode + "' to '" + username + "' in realm '" + realmName + "'", e);
            throw new KeycloakOperationException(
                    "Failed to grant journal '" + journalCode + "' to '" + username + "'", e);
        }
    }

    @Override
    public void revokeJournal(String realmName, String username, String journalCode) {
        UserResource user = userResource(realmName, username);
        UserRepresentation rep = user.toRepresentation();

        List<String> current = currentCodes(rep);
        if (!current.remove(journalCode)) {
            log.info("revokeJournal: '{}' did not have journal '{}' in realm '{}', no-op", username, journalCode, realmName);
            return;
        }
        setCodes(rep, current);
        try {
            user.update(rep);
            log.info("Revoked journal '{}' from user '{}' in realm '{}'", journalCode, username, realmName);
        } catch (WebApplicationException e) {
            log.error("Failed to revoke journal '" + journalCode + "' from '" + username + "' in realm '" + realmName + "'", e);
            throw new KeycloakOperationException(
                    "Failed to revoke journal '" + journalCode + "' from '" + username + "'", e);
        }
    }

    @Override
    public PagedUsers listUsers(String realmName, String journalCodeFilter, int page, int size) {
        UsersResource users = realm(realmName).users();

        if (journalCodeFilter != null) {
            // Server-side attribute search (q=journal_codes:X) — bounded by
            // that one journal's own subscriber count, never the realm's
            // total. No count-by-attribute endpoint exists (confirmed
            // against the real UsersResource API), so this is the one
            // query that fetches its whole (bounded) matching set rather
            // than a page of it — exact total comes along for free since
            // we already have every matching row in hand.
            List<UserSummary> matches = users
                    .searchByAttributes(null, null, null, false, JOURNAL_CODES_ATTRIBUTE + ":" + journalCodeFilter)
                    .stream()
                    .map(this::toSummary)
                    .toList();
            return new PagedUsers(matches, matches.size());
        }

        // No filter — this IS "every user in the realm", the actually
        // unbounded case. Real server-side pagination via Keycloak's own
        // first/max + a real count(), never fetched whole into memory.
        long total = users.count();
        List<UserSummary> pageContent = users.list(page * size, size).stream()
                .map(this::toSummary)
                .toList();
        return new PagedUsers(pageContent, total);
    }

    @Override
    public void createRole(String realmName, String roleName, String description) {
        if (getRole(realmName, roleName).isPresent()) {
            throw new ResourceAlreadyExistsException("Role", roleName);
        }
        RoleRepresentation rep = new RoleRepresentation();
        rep.setName(roleName);
        rep.setDescription(description);
        try {
            realm(realmName).roles().create(rep);
            log.info("Created role '{}' in realm '{}'", roleName, realmName);
        } catch (WebApplicationException e) {
            log.error("Failed to create role '" + roleName + "' in realm '" + realmName + "'", e);
            throw new KeycloakOperationException("Failed to create role '" + roleName + "'", e);
        }
    }

    @Override
    public PagedRoles listRoles(String realmName, int page, int size) {
        List<RoleSummary> all = realm(realmName).roles().list().stream()
                .map(r -> new RoleSummary(r.getName(), r.getDescription()))
                .toList();
        int from = Math.min(page * size, all.size());
        int to = Math.min(from + size, all.size());
        return new PagedRoles(all.subList(from, to), all.size());
    }

    @Override
    public Optional<RoleSummary> getRole(String realmName, String roleName) {
        try {
            RoleRepresentation rep = realm(realmName).roles().get(roleName).toRepresentation();
            return Optional.of(new RoleSummary(rep.getName(), rep.getDescription()));
        } catch (WebApplicationException e) {
            if (e.getResponse().getStatus() == Response.Status.NOT_FOUND.getStatusCode()) {
                return Optional.empty();
            }
            throw new KeycloakOperationException("Failed to look up role '" + roleName + "'", e);
        }
    }

    @Override
    public void deleteRole(String realmName, String roleName) {
        if (getRole(realmName, roleName).isEmpty()) {
            return;
        }
        try {
            realm(realmName).roles().get(roleName).remove();
            log.info("Deleted role '{}' in realm '{}'", roleName, realmName);
        } catch (WebApplicationException e) {
            log.error("Failed to delete role '" + roleName + "' in realm '" + realmName + "'", e);
            throw new KeycloakOperationException("Failed to delete role '" + roleName + "'", e);
        }
    }

    @Override
    public void createGroup(String realmName, String groupName) {
        if (findGroupId(realmName, groupName).isPresent()) {
            throw new ResourceAlreadyExistsException("Group", groupName);
        }
        GroupRepresentation rep = new GroupRepresentation();
        rep.setName(groupName);
        try (Response response = realm(realmName).groups().add(rep)) {
            if (response.getStatus() != Response.Status.CREATED.getStatusCode()) {
                log.error("Failed to create group '" + groupName + "' in realm '" + realmName
                        + "', status=" + response.getStatus());
                throw new KeycloakOperationException("Failed to create group '" + groupName
                        + "', status=" + response.getStatus(), null);
            }
            log.info("Created group '{}' in realm '{}'", groupName, realmName);
        }
    }

    @Override
    public PagedGroups listGroups(String realmName, int page, int size) {
        long total = realm(realmName).groups().count().get("count");
        // search=null, exact=null, briefRepresentation=false — brief is
        // Keycloak's own default and OMITS role mappings entirely; the
        // non-brief fetch is required for roleNamesOf() to return
        // anything, confirmed against the real GroupRepresentation shape
        // (getGroup() already fetched non-brief via group(id), this list
        // path needs the same).
        List<GroupSummary> content = realm(realmName).groups()
                .groups(null, null, page * size, size, false)
                .stream()
                .map(g -> new GroupSummary(g.getName(), roleNamesOf(g)))
                .toList();
        return new PagedGroups(content, total);
    }

    @Override
    public Optional<GroupSummary> getGroup(String realmName, String groupName) {
        return findGroupId(realmName, groupName).map(id -> {
            GroupRepresentation rep = realm(realmName).groups().group(id).toRepresentation();
            return new GroupSummary(rep.getName(), roleNamesOf(rep));
        });
    }

    @Override
    public void deleteGroup(String realmName, String groupName) {
        Optional<String> groupId = findGroupId(realmName, groupName);
        if (groupId.isEmpty()) {
            return;
        }
        try {
            realm(realmName).groups().group(groupId.get()).remove();
            log.info("Deleted group '{}' in realm '{}'", groupName, realmName);
        } catch (WebApplicationException e) {
            log.error("Failed to delete group '" + groupName + "' in realm '" + realmName + "'", e);
            throw new KeycloakOperationException("Failed to delete group '" + groupName + "'", e);
        }
    }

    @Override
    public void addRoleToGroup(String realmName, String groupName, String roleName) {
        GroupResource group = groupResource(realmName, groupName);
        RoleRepresentation role = fetchRoleRepresentation(realmName, roleName);

        boolean alreadyAssigned = group.roles().realmLevel().listAll().stream()
                .anyMatch(r -> r.getName().equals(roleName));
        if (alreadyAssigned) {
            return;
        }
        try {
            group.roles().realmLevel().add(List.of(role));
            log.info("Added role '{}' to group '{}' in realm '{}'", roleName, groupName, realmName);
        } catch (WebApplicationException e) {
            log.error("Failed to add role '" + roleName + "' to group '" + groupName + "' in realm '" + realmName + "'", e);
            throw new KeycloakOperationException(
                    "Failed to add role '" + roleName + "' to group '" + groupName + "'", e);
        }
    }

    @Override
    public void removeRoleFromGroup(String realmName, String groupName, String roleName) {
        GroupResource group = groupResource(realmName, groupName);
        List<RoleRepresentation> current = group.roles().realmLevel().listAll();
        RoleRepresentation match = current.stream()
                .filter(r -> r.getName().equals(roleName))
                .findFirst()
                .orElse(null);
        if (match == null) {
            return;
        }
        try {
            group.roles().realmLevel().remove(List.of(match));
            log.info("Removed role '{}' from group '{}' in realm '{}'", roleName, groupName, realmName);
        } catch (WebApplicationException e) {
            log.error("Failed to remove role '" + roleName + "' from group '" + groupName + "' in realm '" + realmName + "'", e);
            throw new KeycloakOperationException(
                    "Failed to remove role '" + roleName + "' from group '" + groupName + "'", e);
        }
    }

    @Override
    public void addUserToGroup(String realmName, String username, String groupName) {
        UserResource user = userResource(realmName, username);
        String groupId = findGroupId(realmName, groupName)
                .orElseThrow(() -> new ResourceNotFoundException("Group", groupName));
        boolean alreadyMember = user.groups().stream().anyMatch(g -> g.getId().equals(groupId));
        if (alreadyMember) {
            return;
        }
        try {
            user.joinGroup(groupId);
            log.info("Added user '{}' to group '{}' in realm '{}'", username, groupName, realmName);
        } catch (WebApplicationException e) {
            log.error("Failed to add user '" + username + "' to group '" + groupName + "' in realm '" + realmName + "'", e);
            throw new KeycloakOperationException(
                    "Failed to add user '" + username + "' to group '" + groupName + "'", e);
        }
    }

    @Override
    public void removeUserFromGroup(String realmName, String username, String groupName) {
        UserResource user = userResource(realmName, username);
        Optional<String> groupId = findGroupId(realmName, groupName);
        if (groupId.isEmpty()) {
            return;
        }
        boolean isMember = user.groups().stream().anyMatch(g -> g.getId().equals(groupId.get()));
        if (!isMember) {
            return;
        }
        try {
            user.leaveGroup(groupId.get());
            log.info("Removed user '{}' from group '{}' in realm '{}'", username, groupName, realmName);
        } catch (WebApplicationException e) {
            log.error("Failed to remove user '" + username + "' from group '" + groupName + "' in realm '" + realmName + "'", e);
            throw new KeycloakOperationException(
                    "Failed to remove user '" + username + "' from group '" + groupName + "'", e);
        }
    }

    @Override
    public List<String> getUserGroups(String realmName, String username) {
        return userResource(realmName, username).groups().stream()
                .map(GroupRepresentation::getName)
                .toList();
    }

    @Override
    public void addRoleToUser(String realmName, String username, String roleName) {
        UserResource user = userResource(realmName, username);
        RoleRepresentation role = fetchRoleRepresentation(realmName, roleName);
        boolean alreadyAssigned = user.roles().realmLevel().listAll().stream()
                .anyMatch(r -> r.getName().equals(roleName));
        if (alreadyAssigned) {
            return;
        }
        try {
            user.roles().realmLevel().add(List.of(role));
            log.info("Added role '{}' to user '{}' in realm '{}'", roleName, username, realmName);
        } catch (WebApplicationException e) {
            log.error("Failed to add role '" + roleName + "' to user '" + username + "' in realm '" + realmName + "'", e);
            throw new KeycloakOperationException(
                    "Failed to add role '" + roleName + "' to user '" + username + "'", e);
        }
    }

    @Override
    public void removeRoleFromUser(String realmName, String username, String roleName) {
        UserResource user = userResource(realmName, username);
        List<RoleRepresentation> current = user.roles().realmLevel().listAll();
        RoleRepresentation match = current.stream()
                .filter(r -> r.getName().equals(roleName))
                .findFirst()
                .orElse(null);
        if (match == null) {
            return;
        }
        try {
            user.roles().realmLevel().remove(List.of(match));
            log.info("Removed role '{}' from user '{}' in realm '{}'", roleName, username, realmName);
        } catch (WebApplicationException e) {
            log.error("Failed to remove role '" + roleName + "' from user '" + username + "' in realm '" + realmName + "'", e);
            throw new KeycloakOperationException(
                    "Failed to remove role '" + roleName + "' from user '" + username + "'", e);
        }
    }

    @Override
    public List<String> getUserRoles(String realmName, String username) {
        return userResource(realmName, username).roles().realmLevel().listAll().stream()
                .map(RoleRepresentation::getName)
                .toList();
    }

    @Override
    public List<String> getEffectiveRolesForUser(String realmName, String username) {
        return userResource(realmName, username).roles().realmLevel().listEffective().stream()
                .map(RoleRepresentation::getName)
                .toList();
    }

    @Override
    public void createResource(String realmName, String resourceServerClientId, String resourceName, List<String> scopeNames) {
        AuthorizationResource authz = authz(realmName, resourceServerClientId);
        if (!authz.resources().findByName(resourceName).isEmpty()) {
            throw new ResourceAlreadyExistsException("Resource", resourceName);
        }
        ResourceRepresentation rep = new ResourceRepresentation();
        rep.setName(resourceName);
        rep.setScopes(scopeNames.stream().map(ScopeRepresentation::new).collect(Collectors.toSet()));
        try (Response response = authz.resources().create(rep)) {
            if (response.getStatus() != Response.Status.CREATED.getStatusCode()) {
                log.error("Failed to create resource '" + resourceName + "' in realm '" + realmName
                        + "', status=" + response.getStatus());
                throw new KeycloakOperationException(
                        "Failed to create resource '" + resourceName + "', status=" + response.getStatus(), null);
            }
            log.info("Created resource '{}' (scopes={}) in realm '{}' for client '{}'",
                    resourceName, scopeNames, realmName, resourceServerClientId);
        }
    }

    @Override
    public Optional<ResourceSummary> getResource(
            String realmName, String resourceServerClientId, String resourceName) {
        return authz(realmName, resourceServerClientId).resources().findByName(resourceName).stream()
                .findFirst()
                .map(this::toResourceSummary);
    }

    @Override
    public PagedResources listResources(String realmName, String resourceServerClientId, int page, int size) {
        List<ResourceSummary> all = authz(realmName, resourceServerClientId).resources().resources().stream()
                .map(this::toResourceSummary)
                .toList();
        int from = Math.min(page * size, all.size());
        int to = Math.min(from + size, all.size());
        return new PagedResources(all.subList(from, to), all.size());
    }

    @Override
    public void deleteResource(String realmName, String resourceServerClientId, String resourceName) {
        AuthorizationResource authz = authz(realmName, resourceServerClientId);
        List<ResourceRepresentation> matches = authz.resources().findByName(resourceName);
        if (matches.isEmpty()) {
            return;
        }
        try {
            authz.resources().resource(matches.get(0).getId()).remove();
            log.info("Deleted resource '{}' in realm '{}' for client '{}'", resourceName, realmName, resourceServerClientId);
        } catch (WebApplicationException e) {
            log.error("Failed to delete resource '" + resourceName + "' in realm '" + realmName + "'", e);
            throw new KeycloakOperationException("Failed to delete resource '" + resourceName + "'", e);
        }
    }

    @Override
    public void createRolePolicy(String realmName, String resourceServerClientId, String policyName, List<String> roleNames) {
        AuthorizationResource authz = authz(realmName, resourceServerClientId);
        if (findRolePolicy(authz, policyName) != null) {
            throw new ResourceAlreadyExistsException("Policy", policyName);
        }
        RolePolicyRepresentation rep = new RolePolicyRepresentation();
        rep.setName(policyName);
        roleNames.forEach(rep::addRole);
        try (Response response = authz.policies().role().create(rep)) {
            if (response.getStatus() != Response.Status.CREATED.getStatusCode()) {
                log.error("Failed to create policy '" + policyName + "' in realm '" + realmName
                        + "', status=" + response.getStatus());
                throw new KeycloakOperationException(
                        "Failed to create policy '" + policyName + "', status=" + response.getStatus(), null);
            }
            log.info("Created role-policy '{}' (roles={}) in realm '{}' for client '{}'",
                    policyName, roleNames, realmName, resourceServerClientId);
        }
    }

    @Override
    public Optional<PolicySummary> getPolicy(String realmName, String resourceServerClientId, String policyName) {
        RolePolicyRepresentation rep = findRolePolicy(authz(realmName, resourceServerClientId), policyName);
        if (rep == null) {
            return Optional.empty();
        }
        // RoleDefinition's "id" field genuinely IS the role's id here, not
        // its name — confirmed live against a real Keycloak response
        // (addRole(name,...) writes the name into that field for the
        // CREATE payload, but the server resolves it to a real id, and
        // every READ back of this policy returns that id, not the name).
        // Resolved back to a name via rolesById() so PolicySummary's
        // contract (names, not ids) holds either way.
        List<String> roleNames = rep.getRoles() == null
                ? List.of()
                : rep.getRoles().stream()
                        .map(roleDef -> keycloak.realm(realmName).rolesById().getRole(roleDef.getId()).getName())
                        .toList();
        return Optional.of(new PolicySummary(rep.getName(), roleNames));
    }

    @Override
    public PagedPolicies listPolicies(String realmName, String resourceServerClientId, int page, int size) {
        AuthorizationResource authz = authz(realmName, resourceServerClientId);
        // type="role" since that's the only policy type this app ever
        // creates; permission=false excludes permissions themselves,
        // which Keycloak's unified policy table would otherwise include.
        List<PolicyRepresentation> all = authz.policies()
                .policies(null, null, "role", null, null, Boolean.FALSE, null, null, null, null);
        List<PolicySummary> summaries = all.stream().map(p -> toPolicySummary(realmName, authz, p)).toList();
        int from = Math.min(page * size, summaries.size());
        int to = Math.min(from + size, summaries.size());
        return new PagedPolicies(summaries.subList(from, to), summaries.size());
    }

    @Override
    public void deletePolicy(String realmName, String resourceServerClientId, String policyName) {
        AuthorizationResource authz = authz(realmName, resourceServerClientId);
        PolicyRepresentation rep = findPolicy(authz, policyName);
        if (rep == null) {
            return;
        }
        try {
            authz.policies().policy(rep.getId()).remove();
            log.info("Deleted policy '{}' in realm '{}' for client '{}'", policyName, realmName, resourceServerClientId);
        } catch (WebApplicationException e) {
            log.error("Failed to delete policy '" + policyName + "' in realm '" + realmName + "'", e);
            throw new KeycloakOperationException("Failed to delete policy '" + policyName + "'", e);
        }
    }

    @Override
    public void createScopePermission(String realmName, String resourceServerClientId, String permissionName,
            String resourceName, List<String> scopeNames, List<String> policyNames) {
        AuthorizationResource authz = authz(realmName, resourceServerClientId);
        if (findPermission(authz, permissionName) != null) {
            throw new ResourceAlreadyExistsException("Permission", permissionName);
        }

        String resourceId = authz.resources().findByName(resourceName).stream()
                .findFirst()
                .map(ResourceRepresentation::getId)
                .orElseThrow(() -> new ResourceNotFoundException("Resource", resourceName));

        Set<String> policyIds = new HashSet<>();
        for (String policyName : policyNames) {
            PolicyRepresentation policy = findPolicy(authz, policyName);
            if (policy == null) {
                throw new ResourceNotFoundException("Policy", policyName);
            }
            policyIds.add(policy.getId());
        }

        ScopePermissionRepresentation rep = new ScopePermissionRepresentation();
        rep.setName(permissionName);
        rep.addResource(resourceId);
        rep.setScopes(new HashSet<>(scopeNames));
        rep.setPolicies(policyIds);
        // AFFIRMATIVE, not Keycloak's own UNANIMOUS default — see
        // createScopePermission's javadoc on KeycloakGateway for why.
        rep.setDecisionStrategy(DecisionStrategy.AFFIRMATIVE);

        try (Response response = authz.permissions().scope().create(rep)) {
            if (response.getStatus() != Response.Status.CREATED.getStatusCode()) {
                log.error("Failed to create permission '" + permissionName + "' in realm '" + realmName
                        + "', status=" + response.getStatus());
                throw new KeycloakOperationException(
                        "Failed to create permission '" + permissionName + "', status=" + response.getStatus(), null);
            }
            log.info("Created scope-permission '{}' (resource={}, scopes={}, policies={}) in realm '{}' for client '{}'",
                    permissionName, resourceName, scopeNames, policyNames, realmName, resourceServerClientId);
        }
    }

    @Override
    public Optional<PermissionSummary> getPermission(String realmName, String resourceServerClientId, String permissionName) {
        AuthorizationResource authz = authz(realmName, resourceServerClientId);
        ScopePermissionRepresentation rep = findPermission(authz, permissionName);
        if (rep == null) {
            return Optional.empty();
        }
        return Optional.of(toPermissionSummary(authz, rep));
    }

    @Override
    public PagedPermissions listPermissions(String realmName, String resourceServerClientId, int page, int size) {
        AuthorizationResource authz = authz(realmName, resourceServerClientId);
        List<ScopePermissionRepresentation> all = authz.permissions().scope().findAll(null, null, null, null, null, null);
        List<PermissionSummary> summaries = all.stream().map(p -> toPermissionSummary(authz, p)).toList();
        int from = Math.min(page * size, summaries.size());
        int to = Math.min(from + size, summaries.size());
        return new PagedPermissions(summaries.subList(from, to), summaries.size());
    }

    @Override
    public List<PermissionSummary> listPermissionsForRole(String realmName, String resourceServerClientId, String roleName) {
        AuthorizationResource authz = authz(realmName, resourceServerClientId);
        List<PolicyRepresentation> allPolicies = authz.policies()
                .policies(null, null, "role", null, null, Boolean.FALSE, null, null, null, null);

        // Which of this client's role-policies include roleName — a
        // policy with several roles is satisfied by ANY one of them
        // (see createRolePolicy's javadoc), so it counts if roleName is
        // anywhere in its list, not only when it's the sole role.
        Set<String> matchingPolicyNames = allPolicies.stream()
                .filter(p -> {
                    RolePolicyRepresentation rolePolicy = authz.policies().role().findById(p.getId()).toRepresentation();
                    if (rolePolicy.getRoles() == null) {
                        return false;
                    }
                    return rolePolicy.getRoles().stream()
                            .anyMatch(rd -> roleName.equals(keycloak.realm(realmName).rolesById().getRole(rd.getId()).getName()));
                })
                .map(PolicyRepresentation::getName)
                .collect(Collectors.toSet());
        if (matchingPolicyNames.isEmpty()) {
            return List.of();
        }

        // NOT filtered server-side by policyId — confirmed live that
        // Keycloak's permission search silently ignores that filter for
        // scope permissions (returns everything regardless), so this
        // fetches every permission once and filters by the SAME
        // associatedPolicies()-derived names toPermissionSummary already
        // resolves, rather than trusting a query param that doesn't work.
        List<ScopePermissionRepresentation> allPermissions = authz.permissions().scope().findAll(null, null, null, null, null, null);
        return allPermissions.stream()
                .map(p -> toPermissionSummary(authz, p))
                .filter(summary -> summary.policyNames().stream().anyMatch(matchingPolicyNames::contains))
                .toList();
    }

    private PermissionSummary toPermissionSummary(AuthorizationResource authz, ScopePermissionRepresentation rep) {
        // The main representation itself does NOT carry resources/scopes/
        // policies in any usable form on a read — confirmed live: with no
        // `fields` param they're simply absent, and even fields=* only
        // gives back ids (scopes/resources) and omits policies entirely.
        // The dedicated per-permission sub-resources below are the only
        // reliable source, and conveniently already resolve to names.
        ScopePermissionResource permissionResource = authz.permissions().scope().findById(rep.getId());
        String resourceName = permissionResource.resources().stream()
                .findFirst()
                .map(ResourceRepresentation::getName)
                .orElse(null);
        List<String> scopes = permissionResource.scopes().stream().map(ScopeRepresentation::getName).toList();
        List<String> policyNames = permissionResource.associatedPolicies().stream()
                .map(PolicyRepresentation::getName)
                .toList();
        return new PermissionSummary(rep.getName(), resourceName, scopes, policyNames);
    }

    @Override
    public void deletePermission(String realmName, String resourceServerClientId, String permissionName) {
        AuthorizationResource authz = authz(realmName, resourceServerClientId);
        ScopePermissionRepresentation rep = findPermission(authz, permissionName);
        if (rep == null) {
            return;
        }
        try {
            authz.permissions().scope().findById(rep.getId()).remove();
            log.info("Deleted permission '{}' in realm '{}' for client '{}'", permissionName, realmName, resourceServerClientId);
        } catch (WebApplicationException e) {
            log.error("Failed to delete permission '" + permissionName + "' in realm '" + realmName + "'", e);
            throw new KeycloakOperationException("Failed to delete permission '" + permissionName + "'", e);
        }
    }

    private PolicySummary toPolicySummary(String realmName, AuthorizationResource authz, PolicyRepresentation base) {
        RolePolicyRepresentation rolePolicy = authz.policies().role().findById(base.getId()).toRepresentation();
        List<String> roleNames = rolePolicy.getRoles() == null
                ? List.of()
                : rolePolicy.getRoles().stream()
                        .map(rd -> keycloak.realm(realmName).rolesById().getRole(rd.getId()).getName())
                        .toList();
        return new PolicySummary(rolePolicy.getName(), roleNames);
    }

    private ResourceSummary toResourceSummary(ResourceRepresentation rep) {
        List<String> scopes = rep.getScopes() == null
                ? List.of()
                : rep.getScopes().stream().map(ScopeRepresentation::getName).toList();
        return new ResourceSummary(rep.getName(), scopes);
    }

    /** The client's clientId (e.g. "journal-api"), not its internal UUID — resolved here. */
    private AuthorizationResource authz(String realmName, String resourceServerClientId) {
        String internalId = realm(realmName).clients().findByClientId(resourceServerClientId).stream()
                .findFirst()
                .map(ClientRepresentation::getId)
                .orElseThrow(() -> new ResourceNotFoundException("Client", resourceServerClientId));
        return realm(realmName).clients().get(internalId).authorization();
    }

    /**
     * The "/search?name=" lookups below are NOT confirmed to return null
     * vs. throw a 404 on a miss across every Keycloak version — these
     * helpers handle both so a missing policy/permission never surfaces
     * as an unhandled exception.
     */
    private RolePolicyRepresentation findRolePolicy(AuthorizationResource authz, String policyName) {
        try {
            return authz.policies().role().findByName(policyName);
        } catch (WebApplicationException e) {
            if (e.getResponse().getStatus() == Response.Status.NOT_FOUND.getStatusCode()) {
                return null;
            }
            throw new KeycloakOperationException("Failed to look up policy '" + policyName + "'", e);
        }
    }

    private PolicyRepresentation findPolicy(AuthorizationResource authz, String policyName) {
        try {
            return authz.policies().findByName(policyName);
        } catch (WebApplicationException e) {
            if (e.getResponse().getStatus() == Response.Status.NOT_FOUND.getStatusCode()) {
                return null;
            }
            throw new KeycloakOperationException("Failed to look up policy '" + policyName + "'", e);
        }
    }

    private ScopePermissionRepresentation findPermission(AuthorizationResource authz, String permissionName) {
        try {
            return authz.permissions().scope().findByName(permissionName);
        } catch (WebApplicationException e) {
            if (e.getResponse().getStatus() == Response.Status.NOT_FOUND.getStatusCode()) {
                return null;
            }
            throw new KeycloakOperationException("Failed to look up permission '" + permissionName + "'", e);
        }
    }

    /** Unlike getRole()'s RoleSummary, this keeps the real id — needed for role-mapping calls like addRoleToGroup. */
    private RoleRepresentation fetchRoleRepresentation(String realmName, String roleName) {
        try {
            return realm(realmName).roles().get(roleName).toRepresentation();
        } catch (WebApplicationException e) {
            if (e.getResponse().getStatus() == Response.Status.NOT_FOUND.getStatusCode()) {
                throw new ResourceNotFoundException("Role", roleName);
            }
            throw new KeycloakOperationException("Failed to look up role '" + roleName + "'", e);
        }
    }

    private List<String> roleNamesOf(GroupRepresentation rep) {
        if (rep.getRealmRoles() == null) {
            return List.of();
        }
        return List.copyOf(rep.getRealmRoles());
    }

    private Optional<String> findGroupId(String realmName, String groupName) {
        return realm(realmName).groups().groups(groupName, 0, Integer.MAX_VALUE).stream()
                .filter(g -> groupName.equals(g.getName()))
                .findFirst()
                .map(GroupRepresentation::getId);
    }

    private GroupResource groupResource(String realmName, String groupName) {
        String groupId = findGroupId(realmName, groupName)
                .orElseThrow(() -> new ResourceNotFoundException("Group", groupName));
        return realm(realmName).groups().group(groupId);
    }

    private UserSummary toSummary(UserRepresentation rep) {
        List<String> codes = rep.getAttributes() != null
                ? rep.getAttributes().getOrDefault(JOURNAL_CODES_ATTRIBUTE, List.of())
                : List.of();
        return new UserSummary(rep.getUsername(), codes);
    }

    private List<String> currentCodes(UserRepresentation rep) {
        if (rep.getAttributes() == null) {
            return new ArrayList<>();
        }
        List<String> codes = rep.getAttributes().get(JOURNAL_CODES_ATTRIBUTE);
        return codes != null ? new ArrayList<>(codes) : new ArrayList<>();
    }

    private void setCodes(UserRepresentation rep, List<String> codes) {
        if (rep.getAttributes() == null) {
            rep.setAttributes(new HashMap<>());
        }
        rep.getAttributes().put(JOURNAL_CODES_ATTRIBUTE, codes);
    }

    private UserResource userResource(String realmName, String username) {
        String userId = findUserId(realmName, username)
                .orElseThrow(() -> new UserNotFoundException(realmName, username));
        return realm(realmName).users().get(userId);
    }

    private RealmResource realm(String realmName) {
        if (!realmExists(realmName)) {
            throw new RealmNotFoundException(realmName);
        }
        return keycloak.realm(realmName);
    }

    private static final String JOURNALS_ATTRIBUTE = "journals";

    @Override
    public List<String> getRealmJournals(String realmName) {
        Map<String, String> attributes = realm(realmName).toRepresentation().getAttributes();
        String raw = attributes == null ? null : attributes.get(JOURNALS_ATTRIBUTE);
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(code -> !code.isEmpty())
                .toList();
    }

    @Override
    public void setRealmJournals(String realmName, List<String> journalCodes) {
        RealmResource realm = realm(realmName);
        RealmRepresentation rep = realm.toRepresentation();
        Map<String, String> attributes = rep.getAttributes();
        if (attributes == null) {
            attributes = new HashMap<>();
        }
        attributes.put(JOURNALS_ATTRIBUTE, String.join(",", journalCodes));
        rep.setAttributes(attributes);
        try {
            realm.update(rep);
            log.info("Updated realm '{}' journals attribute to {}", realmName, journalCodes);
        } catch (WebApplicationException e) {
            log.error("Failed to update realm '" + realmName + "' journals attribute", e);
            throw new KeycloakOperationException("Failed to update realm '" + realmName + "' journals attribute", e);
        }
    }
}
