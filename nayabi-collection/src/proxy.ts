import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";

// Renamed from `middleware.ts` — Next.js 16 calls this convention "Proxy".
// Runs before admin requests complete and gates them behind the session cookie.

/**
 * The admin cookie is base64(JSON) with an `expiresAt` timestamp. Validate both
 * presence AND expiry here so the dashboard layout can trust that any request
 * reaching it (other than /admin/login) carries a live session — which is what
 * lets the login page render without redirecting to itself.
 */
function hasValidAdminSession(raw?: string): boolean {
  if (!raw) return false;
  try {
    const session = JSON.parse(atob(raw)) as { expiresAt?: number };
    return typeof session.expiresAt === "number" && Date.now() < session.expiresAt;
  } catch {
    return false;
  }
}

export function proxy(request: NextRequest) {
  const { pathname } = request.nextUrl;

  // Protect all /admin routes except the login page itself
  if (pathname.startsWith("/admin") && !pathname.startsWith("/admin/login")) {
    const raw = request.cookies.get("nc_admin")?.value;
    if (!hasValidAdminSession(raw)) {
      return NextResponse.redirect(new URL("/admin/login", request.url));
    }
  }

  return NextResponse.next();
}

export const config = {
  matcher: ["/admin/:path*"],
};
