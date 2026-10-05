import { NextResponse } from "next/server";
import { decodeJwtPayload } from "@/lib/keycloakAuth";
import { getPermissionsForRoles } from "@/lib/iam";
import { getCallerJournal, readSession } from "@/lib/session";

// Roles every account gets from Keycloak itself, never tied to a content
// permission — not worth asking iam-service about on every /api/me call.
const NOISE_ROLES = new Set(["offline_access", "uma_authorization"]);

export async function GET() {
  const session = await readSession();
  if (!session) {
    return NextResponse.json({ message: "Not logged in" }, { status: 401 });
  }
  const journalCode = getCallerJournal(session);
  const claims = decodeJwtPayload(session.accessToken);
  const realmRoles = ((claims.realm_access as { roles?: string[] } | undefined)?.roles ?? []).filter(
    (r) => !NOISE_ROLES.has(r) && !r.startsWith("default-roles-"),
  );

  const permissions = await getPermissionsForRoles(realmRoles).catch(() => []);

  return NextResponse.json({
    username: session.username,
    journalCode,
    // JOURNAL/PUBLISHER mirrors iam-service's own CallerResolvingFilter
    // rule (journal_codes present -> JOURNAL, else -> PUBLISHER for a
    // non-master realm) — this app never logs in via the master realm,
    // so SERVICE never applies to a UI session.
    scope: journalCode ? "JOURNAL" : "PUBLISHER",
    roles: realmRoles,
    // Real content (UMA2) permissions this login's own roles grant — NOT
    // the same thing as admin capability. publisher-admin correctly
    // shows zero here; it's an administrative role, not a content-access
    // one. Computed from the token's own roles, not getUserAccess, so it
    // works for a JOURNAL-scoped login too (that endpoint 403s them).
    permissions,
    // Exposed to client JS deliberately, for this one "verify what's in
    // my own token" panel — see LoginInfoButton's own comment on the
    // trade-off that is.
    accessToken: session.accessToken,
    expiresAt: session.expiresAt,
    claims,
  });
}
