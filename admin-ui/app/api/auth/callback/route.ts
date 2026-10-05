import { NextRequest, NextResponse } from "next/server";
import { APP_PUBLIC_URL } from "@/lib/config";
import { exchangeCodeForTokens, SESSION_COOKIE, sessionCookieOptions } from "@/lib/keycloakAuth";

export async function GET(req: NextRequest) {
  const code = req.nextUrl.searchParams.get("code");
  const state = req.nextUrl.searchParams.get("state");
  const expectedState = req.cookies.get("oauth_state")?.value;

  if (!code || !state || !expectedState || state !== expectedState) {
    return NextResponse.redirect(new URL("/login?error=state", APP_PUBLIC_URL));
  }

  // Must match login/route.ts's redirect_uri exactly — both built from
  // APP_PUBLIC_URL, not req.url, for the same reason (see that
  // constant's own comment).
  const redirectUri = new URL("/api/auth/callback", APP_PUBLIC_URL).toString();

  let tokens;
  try {
    tokens = await exchangeCodeForTokens(code, redirectUri);
  } catch {
    return NextResponse.redirect(new URL("/login?error=exchange", APP_PUBLIC_URL));
  }

  const res = NextResponse.redirect(new URL("/", APP_PUBLIC_URL));
  res.cookies.set(SESSION_COOKIE, JSON.stringify(tokens), sessionCookieOptions);
  res.cookies.delete("oauth_state");
  return res;
}
