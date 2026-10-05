import "server-only";
import { IAM_SERVICE_URL, REALM, RESOURCE_SERVER_CLIENT_ID } from "./config";
import { readSession } from "./session";

export class IamApiError extends Error {
  constructor(public status: number, public body: unknown) {
    super(`iam-service responded ${status}`);
  }
}

async function iamFetch<T>(path: string, init?: RequestInit): Promise<T> {
  // The logged-in admin's OWN token, not a shared service credential —
  // middleware.ts already guaranteed it's unexpired by the time any
  // page or route handler runs, so no refresh logic needed here.
  const session = await readSession();
  if (!session) {
    throw new IamApiError(401, { message: "Not logged in" });
  }
  const res = await fetch(`${IAM_SERVICE_URL}${path}`, {
    ...init,
    headers: {
      Authorization: `Bearer ${session.accessToken}`,
      ...(init?.body ? { "Content-Type": "application/json" } : {}),
      ...init?.headers,
    },
    cache: "no-store",
  });
  const body = await res.json().catch(() => null);
  if (!res.ok) {
    throw new IamApiError(res.status, body);
  }
  return body as T;
}

// ---- Shapes matching iam-service's ApiResponse/PagedApiResponse envelope ----

interface ApiResponse<T> {
  success: boolean;
  data: T;
  message: string | null;
}

interface PagedApiResponse<T> {
  success: boolean;
  data: T[];
  pagination: { page: number; size: number; totalElements: number; totalPages: number };
}

export interface RoleSummary {
  name: string;
  description: string | null;
}

export interface GroupSummary {
  name: string;
  roles: string[];
}

export interface UserSummary {
  username: string;
  journalCodes: string[];
}

export interface UserAccess {
  username: string;
  groups: string[];
  directRoles: string[];
  effectiveRoles: string[];
}

export interface ResourceSummary {
  name: string;
  scopes: string[];
}

export interface PolicySummary {
  name: string;
  roleNames: string[];
}

export interface PermissionSummary {
  name: string;
  resourceName: string | null;
  scopes: string[];
  policies: string[];
}

const realmPath = (p: string) => `/api/v1/realms/${REALM}${p}`;
const clientPath = (p: string) => `/api/v1/realms/${REALM}/clients/${RESOURCE_SERVER_CLIENT_ID}${p}`;

export async function listRoles(): Promise<RoleSummary[]> {
  const res = await iamFetch<PagedApiResponse<RoleSummary>>(realmPath(`/roles?page=1&size=100`));
  return res.data;
}

export async function createRole(name: string, description: string): Promise<void> {
  await iamFetch(realmPath(`/roles`), { method: "POST", body: JSON.stringify({ name, description }) });
}

export async function listGroups(): Promise<GroupSummary[]> {
  const res = await iamFetch<PagedApiResponse<GroupSummary>>(realmPath(`/groups?page=1&size=100`));
  return res.data;
}

export async function createGroup(name: string): Promise<void> {
  await iamFetch(realmPath(`/groups`), { method: "POST", body: JSON.stringify({ name }) });
}

export async function addRoleToGroup(groupName: string, roleName: string): Promise<void> {
  await iamFetch(realmPath(`/groups/${encodeURIComponent(groupName)}/roles`), {
    method: "POST",
    body: JSON.stringify({ roleName }),
  });
}

export async function removeRoleFromGroup(groupName: string, roleName: string): Promise<void> {
  await iamFetch(realmPath(`/groups/${encodeURIComponent(groupName)}/roles/${encodeURIComponent(roleName)}`), {
    method: "DELETE",
  });
}

export async function listUsers(): Promise<UserSummary[]> {
  const res = await iamFetch<PagedApiResponse<UserSummary>>(realmPath(`/users?page=1&size=100`));
  return res.data;
}

export async function getUserAccess(username: string): Promise<UserAccess> {
  const res = await iamFetch<ApiResponse<UserAccess>>(realmPath(`/users/${encodeURIComponent(username)}/access`));
  return res.data;
}

export async function addUserToGroup(username: string, groupName: string): Promise<void> {
  await iamFetch(realmPath(`/users/${encodeURIComponent(username)}/groups`), {
    method: "POST",
    body: JSON.stringify({ groupName }),
  });
}

export async function removeUserFromGroup(username: string, groupName: string): Promise<void> {
  await iamFetch(realmPath(`/users/${encodeURIComponent(username)}/groups/${encodeURIComponent(groupName)}`), {
    method: "DELETE",
  });
}

export async function addRoleToUser(username: string, roleName: string): Promise<void> {
  await iamFetch(realmPath(`/users/${encodeURIComponent(username)}/roles`), {
    method: "POST",
    body: JSON.stringify({ roleName }),
  });
}

export async function listResources(): Promise<ResourceSummary[]> {
  const res = await iamFetch<PagedApiResponse<ResourceSummary>>(clientPath(`/resources?page=1&size=100`));
  return res.data;
}

export async function createResource(name: string, scopes: string[]): Promise<void> {
  await iamFetch(clientPath(`/resources`), { method: "POST", body: JSON.stringify({ name, scopes }) });
}

export async function getResource(name: string): Promise<ResourceSummary | null> {
  try {
    const res = await iamFetch<ApiResponse<ResourceSummary>>(clientPath(`/resources/${encodeURIComponent(name)}`));
    return res.data;
  } catch (e) {
    if (e instanceof IamApiError && e.status === 404) return null;
    throw e;
  }
}

export async function getPolicy(name: string): Promise<PolicySummary | null> {
  try {
    const res = await iamFetch<ApiResponse<PolicySummary>>(clientPath(`/policies/${encodeURIComponent(name)}`));
    return res.data;
  } catch (e) {
    if (e instanceof IamApiError && e.status === 404) return null;
    throw e;
  }
}

export async function getPermission(name: string): Promise<PermissionSummary | null> {
  try {
    const res = await iamFetch<ApiResponse<PermissionSummary>>(clientPath(`/permissions/${encodeURIComponent(name)}`));
    return res.data;
  } catch (e) {
    if (e instanceof IamApiError && e.status === 404) return null;
    throw e;
  }
}

/** Every permission a role effectively grants — direct, via any policy that includes it (see iam-service's PermissionController roleName filter). */
export async function listPermissionsForRole(roleName: string): Promise<PermissionSummary[]> {
  const res = await iamFetch<PagedApiResponse<PermissionSummary>>(
    clientPath(`/permissions?roleName=${encodeURIComponent(roleName)}`),
  );
  return res.data;
}

/**
 * Every permission ANY of these roles grants, deduped by name — used to
 * show a logged-in caller its OWN effective content permissions (from
 * its token's own realm_access.roles, not an admin-only lookup like
 * getUserAccess). Unlike that endpoint, iam-service's permissions list
 * isn't scope-gated, so this works for a JOURNAL-scoped caller too.
 */
export async function getPermissionsForRoles(roleNames: string[]): Promise<PermissionSummary[]> {
  const lists = await Promise.all(roleNames.map((r) => listPermissionsForRole(r)));
  const byName = new Map<string, PermissionSummary>();
  for (const list of lists) {
    for (const p of list) {
      byName.set(p.name, p);
    }
  }
  return Array.from(byName.values());
}

export async function deletePermission(name: string): Promise<void> {
  await iamFetch(clientPath(`/permissions/${encodeURIComponent(name)}`), { method: "DELETE" });
}

/**
 * Grants roleName access to resourceName#scope — creates a dedicated
 * policy ("{roleName}-access") and permission for exactly this role if
 * they don't already exist, idempotent either way (a 409 from either
 * create means it's already granted, not an error). Deliberately does
 * NOT try to add this role into some OTHER existing policy instead —
 * that would need an "update policy" endpoint iam-service doesn't have,
 * and Keycloak evaluates every matching permission for a resource#scope
 * independently, so a second permission object granting the same access
 * via a different policy works just as well.
 */
export async function grantPermissionToRole(roleName: string, resourceName: string, scope: string): Promise<void> {
  const policyName = `${roleName}-access`;
  try {
    await iamFetch(clientPath(`/policies`), {
      method: "POST",
      body: JSON.stringify({ name: policyName, roleNames: [roleName] }),
    });
  } catch (e) {
    if (!(e instanceof IamApiError && e.status === 409)) throw e;
  }

  const permissionName = `${resourceName}-${scope}-for-${roleName}`;
  try {
    await iamFetch(clientPath(`/permissions`), {
      method: "POST",
      body: JSON.stringify({
        name: permissionName,
        resourceName,
        scopeNames: [scope],
        policyNames: [policyName],
      }),
    });
  } catch (e) {
    if (!(e instanceof IamApiError && e.status === 409)) throw e;
  }
}

export interface JournalOverview {
  code: string;
  readRole: string;
  writeRole: string;
  resource: ResourceSummary | null;
  readPolicy: PolicySummary | null;
  writePolicy: PolicySummary | null;
  readPermission: PermissionSummary | null;
  writePermission: PermissionSummary | null;
  configured: boolean;
}

const ARTICLES_SUFFIX = "-articles";

/** The realm's own declared journal list — see iam-service's RealmJournalsController. */
export async function getRealmJournals(): Promise<string[]> {
  const res = await iamFetch<ApiResponse<string[]>>(realmPath(`/journals`));
  return res.data;
}

/** Publisher-level only — iam-service 403s a JOURNAL-scoped caller, same as every other admin action. */
export async function setRealmJournals(journals: string[]): Promise<void> {
  await iamFetch(realmPath(`/journals`), { method: "PUT", body: JSON.stringify({ journals }) });
}

/**
 * Journals come from the realm's own declared list — the authoritative
 * "which journals does this publisher have", settable independently of
 * whether a journal's Authorization Services config exists yet (so a
 * newly-declared journal can show as "pending" instead of invisible).
 * Falls back to discovering from existing "{code}-articles" resources
 * ONLY if the realm attribute was never set (e.g. an older realm this
 * feature predates) — not a parallel source of truth once journals ARE
 * declared, since the list below always wins when non-empty.
 */
export async function getJournalsOverview(): Promise<JournalOverview[]> {
  const declared = await getRealmJournals();
  const codes =
    declared.length > 0
      ? declared
      : (await listResources())
          .map((r) => r.name)
          .filter((name) => name.endsWith(ARTICLES_SUFFIX))
          .map((name) => name.slice(0, -ARTICLES_SUFFIX.length));

  return Promise.all(
    codes.map(async (code) => {
      const [resource, readPolicy, writePolicy, readPermission, writePermission] = await Promise.all([
        getResource(`${code}-articles`),
        getPolicy(`${code}-readers`),
        getPolicy(`${code}-writers`),
        getPermission(`${code}-articles-read-perm`),
        getPermission(`${code}-articles-write-perm`),
      ]);
      return {
        code,
        readRole: `${code}-read`,
        writeRole: `${code}-write`,
        resource,
        readPolicy,
        writePolicy,
        readPermission,
        writePermission,
        configured: resource !== null && readPermission !== null && writePermission !== null,
      };
    }),
  );
}
