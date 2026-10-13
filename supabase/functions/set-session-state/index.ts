// POST set-session-state(
//   body: {schedule_id, state: 'on'|'off', duration_minutes?},
//   auth: lecturer JWT pemilik)
// -> {state, session_id, expires_at} | {error}
// ON/OFF manual dosen (fase-1): ON buka sesi + perpanjang bila sudah ON,
// OFF tutup sesi. Server now() tunggal; semua tulis via service role.

import { createClient } from "jsr:@supabase/supabase-js@2";
import {
  ERR_AUTH,
  ERR_DOSEN_SAJA,
  ERR_JADWAL_TIDAK_AKTIF,
  ERR_SESI_DURASI,
  ERR_SESI_TIDAK_LENGKAP,
  ERR_UMUM,
  generateAttendanceCode,
  type SessionScheduleRow,
  type SetSessionDeps,
  setSessionStateCore,
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

/** Kode manual permanen: kedaluwarsa 10 tahun (is_active mirror sesi). */
const PERMANENT_CODE_VALIDITY_MS = 10 * 365 * 24 * 3600_000;

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
  const scheduleId = Number(body["schedule_id"]);
  const state = body["state"];
  if (!Number.isInteger(scheduleId) || (state !== "on" && state !== "off")) {
    return json({ error: ERR_SESI_TIDAK_LENGKAP }, 400);
  }
  const durationMinutes = body["duration_minutes"] === undefined
    ? undefined
    : Number(body["duration_minutes"]);

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

    const deps: SetSessionDeps = {
      now: () => new Date(),
      findSchedule: async (id) => {
        const { data } = await admin
          .from("schedules")
          .select(
            "id, is_active, lecturer_id, session_open, session_expires_at",
          )
          .eq("id", id)
          .maybeSingle();
        return (data ?? null) as SessionScheduleRow | null;
      },
      findOpenSession: async (schedId) => {
        const { data } = await admin
          .from("attendance_sessions")
          .select("id")
          .eq("schedule_id", schedId)
          .is("closed_at", null)
          .order("opened_at", { ascending: false })
          .limit(1)
          .maybeSingle();
        return (data ?? null) as { id: number } | null;
      },
      createSession: async (schedId) => {
        const { data, error } = await admin
          .from("attendance_sessions")
          .insert({ schedule_id: schedId })
          .select("id")
          .single();
        if (error || !data) throw error ?? new Error("createSession failed");
        return data as { id: number };
      },
      closeOpenSessions: async (schedId, closedAt) => {
        const { data, error } = await admin
          .from("attendance_sessions")
          .update({ closed_at: closedAt })
          .eq("schedule_id", schedId)
          .is("closed_at", null)
          .select("id");
        if (error) throw error;
        return (data ?? []).length;
      },
      updateSchedule: async (schedId, patch) => {
        const { error } = await admin
          .from("schedules")
          .update({
            session_open: patch.session_open,
            session_expires_at: patch.session_expires_at,
          })
          .eq("id", schedId);
        if (error) throw error;
      },
      setCodesActive: async (schedId, active) => {
        const { error } = await admin
          .from("attendance_codes")
          .update({ is_active: active })
          .eq("schedule_id", schedId);
        if (error) throw error;
      },
      ensurePermanentCode: async (schedId, code) => {
        const { data: existing } = await admin
          .from("attendance_codes")
          .select("code")
          .eq("schedule_id", schedId)
          .limit(1)
          .maybeSingle();
        if (existing) return (existing as { code: string }).code;
        const expiresAt = new Date(Date.now() + PERMANENT_CODE_VALIDITY_MS)
          .toISOString();
        for (let attempt = 0; attempt < 3; attempt++) {
          const tryCode = attempt === 0 ? code : generateAttendanceCode();
          const { data, error } = await admin
            .from("attendance_codes")
            .insert({
              code: tryCode,
              schedule_id: schedId,
              expires_at: expiresAt,
              is_active: true,
            })
            .select("code")
            .single();
          if (!error && data) return (data as { code: string }).code;
        }
        throw new Error("ensurePermanentCode failed");
      },
      refreshQrExpiry: async (schedId, expiresAt) => {
        const { error } = await admin
          .from("qr_tokens")
          .update({ expires_at: expiresAt })
          .eq("schedule_id", schedId);
        if (error) throw error;
      },
    };

    const result = await setSessionStateCore(deps, {
      callerId: user.id,
      isLecturer,
      scheduleId,
      state,
      durationMinutes,
    });
    if (!result.ok) {
      switch (result.code) {
        case "SCHEDULE_NOT_FOUND":
          return json({ error: ERR_JADWAL_TIDAK_AKTIF }, 404);
        case "INVALID_DURATION":
          return json({ error: ERR_SESI_DURASI }, 400);
        case "INVALID_STATE":
          return json({ error: ERR_SESI_TIDAK_LENGKAP }, 400);
        default:
          return json({ error: ERR_DOSEN_SAJA }, 403);
      }
    }
    return json({
      state: result.state,
      session_id: result.session_id,
      expires_at: result.expires_at,
    }, 200);
  } catch {
    return json({ error: ERR_UMUM }, 500);
  }
});
