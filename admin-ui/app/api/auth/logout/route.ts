import { NextRequest, NextResponse } from "next/server";
import { APP_PUBLIC_URL, KEYCLOAK_PUBLIC_URL, REALM } from "@/lib/config";
import { SESSION_COOKIE } from "@/lib/keycloakAuth";

export async function GET(req: NextRequest) {
  const raw = req.cookies.get(SESSION_COOKIE)?.value;
  let idToken: string | undefined;
  if (raw) {
    try {
      idToken = JSON.parse(raw).idToken;
    } catch {
      // ignore — we're logging out either way
    }
  }

  // A real Keycloak end-session, not just dropping our own cookie — ends
  // the SSO session at the identity provider too, same as a user
  // expects "log out" to mean. The browser is redirected here, so this
  // needs the externally reachable address, same reasoning as login.
  const endSessionUrl = new URL(`${KEYCLOAK_PUBLIC_URL}/realms/${REALM}/protocol/openid-connect/logout`);
  if (idToken) endSessionUrl.searchParams.set("id_token_hint", idToken);
  endSessionUrl.searchParams.set("post_logout_redirect_uri", new URL("/login", APP_PUBLIC_URL).toString());

  const res = NextResponse.redirect(endSessionUrl);
  res.cookies.delete(SESSION_COOKIE);
  return res;
}
