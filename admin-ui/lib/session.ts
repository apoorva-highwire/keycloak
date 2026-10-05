import "server-only";
import { cookies } from "next/headers";
import { decodeJwtPayload, SESSION_COOKIE, TokenSet } from "./keycloakAuth";

export type Session = TokenSet;

/**
 * Read-only — Server Components and Route Handlers can both call this.
 * Writing the cookie happens only in the three auth routes (login/
 * callback/logout) and in middleware.ts, each directly on the
 * NextResponse it returns — not through this module — since "can this
 * context set a cookie" varies (Server Components can't) and routes
 * already hold the response object that needs it.
 */
export async function readSession(): Promise<Session | null> {
  const raw = (await cookies()).get(SESSION_COOKIE)?.value;
  if (!raw) return null;
  try {
    return JSON.parse(raw) as Session;
  } catch {
    return null;
  }
}

/**
 * The same journal_codes claim CallerResolvingFilter reads server-side
 * in iam-service — decoded here purely to adjust what THIS console
 * shows a journal-scoped login (their own journal's rows, not every
 * publisher's). This is a display convenience only, not a security
 * boundary: iam-service's own read endpoints (resources/policies/
 * permissions) don't restrict by caller scope today, so a journal-
 * scoped caller could still reach other journals' data by calling the
 * API directly — only the mutation endpoints UserController gates
 * (group/role assignment) are actually enforced server-side.
 */
export function getCallerJournal(session: Session): string | null {
  const claims = decodeJwtPayload(session.accessToken);
  const codes = claims.journal_codes;
  if (Array.isArray(codes) && codes.length > 0) {
    return String(codes[0]);
  }
  if (typeof codes === "string" && codes.length > 0) {
    return codes;
  }
  return null;
}
