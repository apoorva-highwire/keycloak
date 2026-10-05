import { NextRequest, NextResponse } from "next/server";
import { refreshTokens, SESSION_COOKIE, sessionCookieOptions } from "@/lib/keycloakAuth";

const PUBLIC_PREFIXES = ["/login", "/api/auth"];

export async function middleware(req: NextRequest) {
  const { pathname } = req.nextUrl;
  if (PUBLIC_PREFIXES.some((p) => pathname.startsWith(p))) {
    return NextResponse.next();
  }

  const raw = req.cookies.get(SESSION_COOKIE)?.value;
  if (!raw) {
    return NextResponse.redirect(new URL("/login", req.url));
  }

  let session;
  try {
    session = JSON.parse(raw);
  } catch {
    const res = NextResponse.redirect(new URL("/login", req.url));
    res.cookies.delete(SESSION_COOKIE);
    return res;
  }

  // Not expired yet — the common case, no extra Keycloak round trip.
  if (Date.now() <= session.expiresAt - 10_000) {
    return NextResponse.next();
  }

  // Expired — refresh it HERE so every page/API route downstream always
  // sees a valid token, rather than each one having to handle refresh
  // itself (Server Components can't even write the refreshed cookie
  // back; middleware is the one place that can do this for everyone).
  try {
    const fresh = await refreshTokens(session.refreshToken);
    const res = NextResponse.next();
    res.cookies.set(SESSION_COOKIE, JSON.stringify(fresh), sessionCookieOptions);
    return res;
  } catch {
    const res = NextResponse.redirect(new URL("/login", req.url));
    res.cookies.delete(SESSION_COOKIE);
    return res;
  }
}

export const config = {
  matcher: ["/((?!_next/static|_next/image|favicon.ico).*)"],
};
