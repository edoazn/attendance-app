// POST login-with-nim(body: {nim, password})
// -> {token, user: {id, identity_number, name, email, role}} | {error}
// Alur: lookup NIM -> email di profiles (service role) -> signInWithPassword.
// NIM salah vs password salah MENGEMBALIKAN PESAN IDENTIK (generik).

import { createClient } from "jsr:@supabase/supabase-js@2";
import { ERR_UMUM, loginErrorMessage } from "../_shared/attendance.ts";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers":
    "authorization, x-client-info, apikey, content-type",
};

function json(body: unknown, status: number): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { ...corsHeaders, "Content-Type": "application/json" },
  });
}

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }
  if (req.method !== "POST") {
    return json({ error: ERR_UMUM }, 405);
  }

  let body: { nim?: unknown; password?: unknown };
  try {
    body = await req.json();
  } catch {
    return json({ error: ERR_UMUM }, 400);
  }
  const nim = typeof body.nim === "string" ? body.nim.trim() : "";
  const password = typeof body.password === "string" ? body.password : "";
  if (!nim || !password) {
    return json({ error: "NIM dan kata sandi wajib diisi." }, 400);
  }

  const supabaseUrl = Deno.env.get("SUPABASE_URL") ?? "";
  const anonKey = Deno.env.get("SUPABASE_ANON_KEY") ?? "";
  const serviceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "";
  if (!supabaseUrl || !anonKey || !serviceKey) {
    return json({ error: ERR_UMUM }, 500);
  }

  try {
    const admin = createClient(supabaseUrl, serviceKey);

    // 1. NIM -> email (service role agar lolos RLS profiles).
    const { data: profile, error: profileError } = await admin
      .from("profiles")
      .select("id, email")
      .eq("identity_number", nim)
      .maybeSingle();
    if (profileError || !profile?.email) {
      // Sengaja generik: sama persis dengan password salah.
      return json({ error: loginErrorMessage("nim_not_found") }, 401);
    }

    // 2. Email + password -> session.
    const authClient = createClient(supabaseUrl, anonKey);
    const { data: signIn, error: signInError } = await authClient.auth
      .signInWithPassword({ email: profile.email as string, password });
    if (signInError || !signIn.session) {
      return json({ error: loginErrorMessage("wrong_password") }, 401);
    }

    // 3. Profil lengkap untuk response user.
    const { data: full } = await admin
      .from("profiles")
      .select("id, identity_number, name, email, role")
      .eq("id", signIn.user.id)
      .maybeSingle();
    if (!full) return json({ error: loginErrorMessage("missing_email") }, 401);

    return json({
      token: signIn.session.access_token,
      user: {
        id: full.id,
        identity_number: full.identity_number,
        name: full.name,
        email: full.email,
        role: full.role,
      },
    }, 200);
  } catch {
    return json({ error: ERR_UMUM }, 500);
  }
});
