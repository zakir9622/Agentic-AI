import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";

// Renamed from `middleware.ts` — Next.js 16 calls this convention "Proxy".
// Runs before admin requests complete and gates them behind the session cookie.
export function proxy(request: NextRequest) {
  const { pathname } = request.nextUrl;

  // Protect all /admin routes except the login page itself
  if (pathname.startsWith("/admin") && !pathname.startsWith("/admin/login")) {
    const session = request.cookies.get("nc_admin");
    if (!session?.value) {
      return NextResponse.redirect(new URL("/admin/login", request.url));
    }
  }

  return NextResponse.next();
}

export const config = {
  matcher: ["/admin/:path*"],
};
