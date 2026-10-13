// POST create-user(
//   body: {nim, name, password, role: 'STUDENT'|'LECTURER'|'ADMIN',
//     email?, rombel?},
//   auth: ADMIN JWT)
// -> {user: {id, identity_number, name, role}} | {error}
// Fase-2: admin membuat akun (auth.users via Auth Admin API + profil).
// NIM duplikat / gagal tulis lain -> 409 generik anti-enumerasi.
// Gagal tulis profil setelah auth terbuat -> rollback hapus auth user.

import { createClient } from "jsr:@supabase/supabase-js@2";
import {
  duplicateUserErrorMessage,
  ERR_AUTH,
  ERR_DOSEN_SAJA,
  ERR_SESI_TIDAK_LENGKAP,
  ERR_UMUM,
  isWriteConflictError,
  validateCreateUserInput,
} from "../_shared/attendance.ts";

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

  const jwt = (req.headers.get("Authorization") ?? "").replace(
    /^Bearer\s+/i,
    "",
  );
  if (!jwt) return json({ error: ERR_AUTH }, 401);

  let body: Record<string, unknown>;
  try {
    body = await req.json();
  } catch {
    return json({ error: ERR_SESI_TIDAK_LENGKAP }, 400);
  }
  const validated = validateCreateUserInput({
    nim: body["nim"],
    name: body["name"],
    password: body["password"],
    role: body["role"],
    email: body["email"],
    rombel: body["rombel"],
  });
  if (!validated.ok) return json({ error: validated.error }, 400);

  const supabaseUrl = Deno.env.get("SUPABASE_URL") ?? "";
  const anonKey = Deno.env.get("SUPABASE_ANON_KEY") ?? "";
  const serviceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "";
  if (!supabaseUrl || !anonKey || !serviceKey) {
    return json({ error: ERR_UMUM }, 500);
  }

  try {
    const userClient = createClient(supabaseUrl, anonKey, {
      global: { headers: { Authorization: `Bearer ${jwt}` } },
    });
    const { data: { user }, error: userError } = await userClient.auth
      .getUser();
    if (userError || !user) return json({ error: ERR_AUTH }, 401);

    const admin = createClient(supabaseUrl, serviceKey);

    const { data: profile } = await admin
      .from("profiles")
      .select("role")
      .eq("id", user.id)
      .maybeSingle();
    if (!profile || profile.role !== "ADMIN") {
      return json({ error: ERR_DOSEN_SAJA }, 403);
    }

    // 1. Akun auth dulu (email konfirmasi otomatis: akun layanan internal).
    //    Hanya konflik nyata -> 409; error lain -> 500 agar bisa dibedakan.
    const { data: created, error: createError } = await admin.auth.admin
      .createUser({
        email: validated.email,
        password: body["password"] as string,
        email_confirm: true,
        user_metadata: { identity_number: validated.nim, name: validated.name },
      });
    if (createError || !created.user) {
      if (
        isWriteConflictError(
          createError as { code?: string; message?: string } | null,
        )
      ) {
        return json({ error: duplicateUserErrorMessage() }, 409);
      }
      return json({ error: ERR_UMUM }, 500);
    }

    // 2. Profil (NIM unik). Gagal -> rollback akun auth; konflik -> 409.
    const { error: profileError } = await admin
      .from("profiles")
      .insert({
        id: created.user.id,
        identity_number: validated.nim,
        name: validated.name,
        email: validated.email,
        role: validated.role,
        rombel: validated.rombel,
      });
    if (profileError) {
      await admin.auth.admin.deleteUser(created.user.id);
      if (
        isWriteConflictError(
          profileError as { code?: string; message?: string },
        )
      ) {
        return json({ error: duplicateUserErrorMessage() }, 409);
      }
      return json({ error: ERR_UMUM }, 500);
    }

    return json({
      user: {
        id: created.user.id,
        identity_number: validated.nim,
        name: validated.name,
        role: validated.role,
      },
    }, 200);
  } catch {
    return json({ error: ERR_UMUM }, 500);
  }
});
