// POST generate-session(body: {schedule_id, duration_minutes}, auth: lecturer JWT)
// -> {qr_token, attendance_code, expires_at} | {error}
// Guard: hanya role LECTURER (cek via service role, anti rekursi RLS).
// Menulis 1 baris qr_tokens + 1 baris attendance_codes (pakai service role
// karena tidak ada policy direct insert — konsisten dengan Task 1).

import { createClient } from "jsr:@supabase/supabase-js@2";
import {
  buildSessionExpiry,
  ERR_AUTH,
  ERR_DOSEN_SAJA,
  ERR_JADWAL_TIDAK_AKTIF,
  ERR_SESI_TIDAK_LENGKAP,
  ERR_UMUM,
  generateAttendanceCode,
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

const MAX_DURATION_MINUTES = 480;

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

  let body: { schedule_id?: unknown; duration_minutes?: unknown };
  try {
    body = await req.json();
  } catch {
    return json({ error: ERR_SESI_TIDAK_LENGKAP }, 400);
  }
  const scheduleId = Number(body.schedule_id);
  const durationMinutes = Number(body.duration_minutes);
  if (
    !Number.isInteger(scheduleId) || !Number.isInteger(durationMinutes) ||
    durationMinutes < 1 || durationMinutes > MAX_DURATION_MINUTES
  ) {
    return json({ error: ERR_SESI_TIDAK_LENGKAP }, 400);
  }

  const supabaseUrl = Deno.env.get("SUPABASE_URL") ?? "";
  const anonKey = Deno.env.get("SUPABASE_ANON_KEY") ?? "";
  const serviceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "";
  if (!supabaseUrl || !anonKey || !serviceKey) {
    return json({ error: ERR_UMUM }, 500);
  }

  try {
    // 1. Verifikasi JWT penelepon.
    const userClient = createClient(supabaseUrl, anonKey, {
      global: { headers: { Authorization: `Bearer ${jwt}` } },
    });
    const { data: { user }, error: userError } = await userClient.auth
      .getUser();
    if (userError || !user) return json({ error: ERR_AUTH }, 401);

    const admin = createClient(supabaseUrl, serviceKey);

    // 2. Guard lecturer-only.
    const { data: profile } = await admin
      .from("profiles")
      .select("role")
      .eq("id", user.id)
      .maybeSingle();
    if (!profile || profile.role !== "LECTURER") {
      return json({ error: ERR_DOSEN_SAJA }, 403);
    }

    // 3. Jadwal harus ada.
    const { data: schedule } = await admin
      .from("schedules")
      .select("id")
      .eq("id", scheduleId)
      .maybeSingle();
    if (!schedule) return json({ error: ERR_JADWAL_TIDAK_AKTIF }, 404);

    // 4. Tulis sesi (server now + durasi).
    const expiresAt = buildSessionExpiry(new Date(), durationMinutes)
      .toISOString();

    const { data: qrRow, error: qrError } = await admin
      .from("qr_tokens")
      .insert({ schedule_id: scheduleId, expires_at: expiresAt })
      .select("token")
      .single();
    if (qrError || !qrRow) return json({ error: ERR_UMUM }, 500);

    // Kode 6 char UPPER; retry bila tabrakan PK (char(6) collision).
    let codeRow: { code: string } | null = null;
    for (let attempt = 0; attempt < 3 && !codeRow; attempt++) {
      const code = generateAttendanceCode();
      const { data, error } = await admin
        .from("attendance_codes")
        .insert({
          code,
          schedule_id: scheduleId,
          expires_at: expiresAt,
          is_active: true,
        })
        .select("code")
        .single();
      if (!error && data) codeRow = data;
    }
    if (!codeRow) return json({ error: ERR_UMUM }, 500);

    return json({
      qr_token: qrRow.token,
      attendance_code: codeRow.code,
      expires_at: expiresAt,
    }, 200);
  } catch {
    return json({ error: ERR_UMUM }, 500);
  }
});
