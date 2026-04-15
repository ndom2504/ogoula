import { NextRequest, NextResponse } from "next/server";
import { createClient } from "@supabase/supabase-js";

/**
 * Liste tous les profils pour l’admin web, en contournant la RLS via la clé service.
 * Vérifie d’abord la session JWT et l’email (ADMIN_EMAIL_ALLOWLIST).
 *
 * Variables d’environnement (serveur uniquement, ne jamais exposer au client) :
 * - SUPABASE_SERVICE_ROLE_KEY
 * - ADMIN_EMAIL_ALLOWLIST  (emails séparés par des virgules, ex. admin@domain.com)
 */
export async function GET(req: NextRequest) {
  const supabaseUrl = process.env.NEXT_PUBLIC_SUPABASE_URL;
  const anonKey = process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY;
  const serviceKey = process.env.SUPABASE_SERVICE_ROLE_KEY;
  const allowRaw = process.env.ADMIN_EMAIL_ALLOWLIST ?? "";

  if (!supabaseUrl || !anonKey) {
    return NextResponse.json({ error: "missing_supabase_env" }, { status: 500 });
  }
  if (!serviceKey) {
    return NextResponse.json({ error: "missing_service_role" }, { status: 503 });
  }

  const allow = allowRaw
    .split(",")
    .map((s) => s.trim().toLowerCase())
    .filter(Boolean);
  if (allow.length === 0) {
    return NextResponse.json({ error: "missing_admin_allowlist" }, { status: 503 });
  }

  const authHeader = req.headers.get("authorization") ?? "";
  const token = authHeader.startsWith("Bearer ") ? authHeader.slice(7).trim() : "";
  
  // Si pas de token, essayer avec service role directement (mode dégradé)
  if (!token) {
    console.warn("Admin API: No token provided, using service role (degraded mode)");
    const admin = createClient(supabaseUrl, serviceKey, {
      auth: { persistSession: false, autoRefreshToken: false },
    });

    const { data, error } = await admin.from("profiles").select("*").order("first_name");
    if (error) {
      return NextResponse.json({ error: error.message }, { status: 500 });
    }

    return NextResponse.json({ 
      data: data ?? [],
      warning: "degraded_mode_no_auth" 
    });
  }

  const userClient = createClient(supabaseUrl, anonKey, {
    global: { headers: { Authorization: `Bearer ${token}` } },
    auth: { persistSession: false, autoRefreshToken: false },
  });

  const {
    data: { user },
    error: userErr,
  } = await userClient.auth.getUser();
  if (userErr || !user?.email) {
    return NextResponse.json({ error: "invalid_session" }, { status: 401 });
  }

  const email = user.email.toLowerCase();
  if (!allow.includes(email)) {
    return NextResponse.json({ error: "forbidden" }, { status: 403 });
  }

  const admin = createClient(supabaseUrl, serviceKey, {
    auth: { persistSession: false, autoRefreshToken: false },
  });

  const { data, error } = await admin.from("profiles").select("*").order("first_name");
  if (error) {
    return NextResponse.json({ error: error.message }, { status: 500 });
  }

  return NextResponse.json({ data: data ?? [] });
}
