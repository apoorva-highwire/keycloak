import { NextResponse } from "next/server";
import { APP_PUBLIC_URL, KEYCLOAK_PUBLIC_URL, LOGIN_CLIENT_ID, REALM } from "@/lib/config";

export async function GET() {
  const state = crypto.randomUUID();
  // From APP_PUBLIC_URL, not req.url — see that constant's own comment.
  const redirectUri = new URL("/api/auth/callback", APP_PUBLIC_URL).toString();

  // The user's own browser is redirected here — must be the externally
  // reachable address, not KEYCLOAK_URL's internal docker hostname.
  const authUrl = new URL(`${KEYCLOAK_PUBLIC_URL}/realms/${REALM}/protocol/openid-connect/auth`);
  authUrl.searchParams.set("client_id", LOGIN_CLIENT_ID);
  authUrl.searchParams.set("redirect_uri", redirectUri);
  authUrl.searchParams.set("response_type", "code");
  authUrl.searchParams.set("scope", "openid");
  authUrl.searchParams.set("state", state);

  const res = NextResponse.redirect(authUrl);
  res.cookies.set("oauth_state", state, {
    httpOnly: true,
    sameSite: "lax",
    secure: process.env.NODE_ENV === "production",
    path: "/",
    maxAge: 300,
  });
  return res;
}
