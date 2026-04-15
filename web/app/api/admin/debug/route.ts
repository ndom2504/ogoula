import { NextRequest, NextResponse } from "next/server";

export async function GET(req: NextRequest) {
  const env = {
    NEXT_PUBLIC_SUPABASE_URL: process.env.NEXT_PUBLIC_SUPABASE_URL ? "✓" : "✗ MISSING",
    NEXT_PUBLIC_SUPABASE_ANON_KEY: process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY ? "✓" : "✗ MISSING", 
    SUPABASE_SERVICE_ROLE_KEY: process.env.SUPABASE_SERVICE_ROLE_KEY ? "✓" : "✗ MISSING",
    ADMIN_EMAIL_ALLOWLIST: process.env.ADMIN_EMAIL_ALLOWLIST || "✗ MISSING",
  };

  const allowRaw = process.env.ADMIN_EMAIL_ALLOWLIST ?? "";
  const allow = allowRaw
    .split(",")
    .map((s) => s.trim().toLowerCase())
    .filter(Boolean);

  const authHeader = req.headers.get("authorization") ?? "";
  const token = authHeader.startsWith("Bearer ") ? authHeader.slice(7).trim() : "";
  
  return NextResponse.json({
    environment: env,
    admin_emails: allow,
    has_token: !!token,
    token_preview: token ? `${token.substring(0, 20)}...` : null,
  });
}
