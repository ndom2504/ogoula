import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";

/** Canonique : www.ogoula.com (évite 404 si seul le sous-domaine www est branché sur Vercel). */
export function middleware(request: NextRequest) {
  const host = request.headers.get("host")?.split(":")[0] ?? "";
  if (host === "ogoula.com") {
    const url = request.nextUrl.clone();
    url.hostname = "www.ogoula.com";
    url.protocol = "https:";
    return NextResponse.redirect(url, 308);
  }
  return NextResponse.next();
}

export const config = {
  matcher: ["/((?!_next/static|_next/image|favicon.ico).*)"],
};
