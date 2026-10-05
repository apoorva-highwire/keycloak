// No "server-only"/Node-specific APIs here on purpose — this file is
// imported from middleware.ts too, which runs on the Edge runtime
// (no Buffer, no next/headers). atob is available in both runtimes.
import { KEYCLOAK_URL, LOGIN_CLIENT_ID, REALM } from "./config";

export interface TokenSet {
  accessToken: string;
  refreshToken: string;
  idToken: string;
  expiresAt: number; // epoch ms
  username: string;
}

function base64UrlDecode(input: string): string {
  const base64 = input.replace(/-/g, "+").replace(/_/g, "/");
  const padded = base64 + "=".repeat((4 - (base64.length % 4)) % 4);
  const binary = atob(padded);
  return decodeURIComponent(
    binary
      .split("")
      .map((c) => "%" + c.charCodeAt(0).toString(16).padStart(2, "0"))
      .join(""),
  );
}

/** Decodes a JWT payload WITHOUT verifying it — fine for display (the sidebar's username); every real API call this app makes is still re-verified by iam-service's own signature check. */
export function decodeJwtPayload(token: string): Record<string, unknown> {
  return JSON.parse(base64UrlDecode(token.split(".")[1]));
}

function toTokenSet(body: {
  access_token: string;
  refresh_token: string;
  id_token?: string;
  expires_in: number;
}): TokenSet {
  const claims = decodeJwtPayload(body.access_token);
  return {
    accessToken: body.access_token,
    refreshToken: body.refresh_token,
    idToken: body.id_token ?? "",
    expiresAt: Date.now() + body.expires_in * 1000,
    username: (claims.preferred_username as string) ?? "unknown",
  };
}

export async function exchangeCodeForTokens(code: string, redirectUri: string): Promise<TokenSet> {
  const res = await fetch(`${KEYCLOAK_URL}/realms/${REALM}/protocol/openid-connect/token`, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      grant_type: "authorization_code",
      client_id: LOGIN_CLIENT_ID,
      code,
      redirect_uri: redirectUri,
    }),
    cache: "no-store",
  });
  if (!res.ok) {
    throw new Error(`Token exchange failed (${res.status}): ${await res.text()}`);
  }
  return toTokenSet(await res.json());
}

export async function refreshTokens(refreshToken: string): Promise<TokenSet> {
  const res = await fetch(`${KEYCLOAK_URL}/realms/${REALM}/protocol/openid-connect/token`, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      grant_type: "refresh_token",
      client_id: LOGIN_CLIENT_ID,
      refresh_token: refreshToken,
    }),
    cache: "no-store",
  });
  if (!res.ok) {
    throw new Error(`Refresh failed (${res.status})`);
  }
  return toTokenSet(await res.json());
}

export const SESSION_COOKIE = "session";

export const sessionCookieOptions = {
  httpOnly: true as const,
  sameSite: "lax" as const,
  secure: process.env.NODE_ENV === "production",
  path: "/",
  maxAge: 60 * 60 * 8,
};
