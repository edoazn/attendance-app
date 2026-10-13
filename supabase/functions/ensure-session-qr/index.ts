// POST ensure-session-qr(body: {schedule_id}, auth: lecturer JWT pemilik)
// -> {qr_token, created} | {error}
// Token QR statis per jadwal (fase-1): idempoten — panggil 2x token SAMA,
// created false pada panggilan kedua. Guard: lecturer pemilik jadwal
// (pool lecturer_id NULL boleh lecturer mana pun); jadwal arsip -> 404.
// Tulis via service role (tidak ada policy direct insert — Task 1).

import { createClient } from "jsr:@supabase/supabase-js@2";
import {
  type EnsureQrDeps,
  ensureSessionQrCore,
  ERR_AUTH,
  ERR_DOSEN_SAJA,
  ERR_JADWAL_TIDAK_AKTIF,
  ERR_SESI_TIDAK_LENGKAP,
  ERR_UMUM,
  type ScheduleOwnershipRow,
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

  let body: { schedule_id?: unknown };
  try {
    body = await req.json();
  } catch {
    return json({ error: ERR_SESI_TIDAK_LENGKAP }, 400);
  }
  const scheduleId = Number(body.schedule_id);
  if (!Number.isInteger(scheduleId)) {
    return json({ error: ERR_SESI_TIDAK_LENGKAP }, 400);
  }

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
    const isLecturer = !!profile && profile.role === "LECTURER";

    const deps: EnsureQrDeps = {
      newToken: () => crypto.randomUUID(),
      findSchedule: async (id) => {
        const { data } = await admin
          .from("schedules")
          .select("id, is_active, lecturer_id")
          .eq("id", id)
          .maybeSingle();
        return (data ?? null) as ScheduleOwnershipRow | null;
      },
      findQrTokenBySchedule: async (schedId) => {
        const { data } = await admin
          .from("qr_tokens")
          .select("token")
          .eq("schedule_id", schedId)
          .maybeSingle();
        return (data as { token: string } | null)?.token ?? null;
      },
      insertQrToken: async (schedId, token) => {
        // Insert polos (bukan upsert): konflik UNIQUE(schedule_id) saat
        // balapan dilempar ke core, yang lalu baca ulang baris pemenang.
        // Token baru lahir kedaluwarsa (epoch); ON sesi me-refresh expiry.
        const { error } = await admin
          .from("qr_tokens")
          .insert({
            token,
            schedule_id: schedId,
            expires_at: new Date(0).toISOString(),
          });
        if (error) throw error;
      },
    };

    const result = await ensureSessionQrCore(deps, {
      callerId: user.id,
      isLecturer,
      scheduleId,
    });
    if (!result.ok) {
      if (result.code === "SCHEDULE_NOT_FOUND") {
        return json({ error: ERR_JADWAL_TIDAK_AKTIF }, 404);
      }
      return json({ error: ERR_DOSEN_SAJA }, 403);
    }
    return json({ qr_token: result.qr_token, created: result.created }, 200);
  } catch {
    return json({ error: ERR_UMUM }, 500);
  }
});
