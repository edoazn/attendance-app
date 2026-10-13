// POST submit-attendance(
//   body: {schedule_id, method: 'geolocation'|'qr_code'|'attendance_code',
//     latitude?, longitude?, qr_token?, attendance_code?},
//   auth: student JWT)
// -> {status, method, attendance: {id, status, method, created_at}} | {error}
// Inti (submitAttendanceCore di ../_shared/attendance.ts): verify JWT ->
// student_id -> server now() -> cek is_active -> cek duplicate UNIQUE ->
// branch method (Haversine vs lat/long kampus + threshold | expiry
// qr_tokens | UPPER + is_active + expiry) -> HADIR/TERLAMBAT dari
// now() vs start_time + lateThreshold -> insert -> return.

import { createClient } from "jsr:@supabase/supabase-js@2";
import {
  type AttendanceCodeRow,
  type AttendanceRecordRow,
  ERR_AUTH,
  ERR_DUPLIKAT,
  ERR_METODE_TIDAK_VALID,
  ERR_UMUM,
  type InsertRecordInput,
  isSubmitMethod,
  type QrTokenRow,
  type ScheduleRow,
  submitAttendanceCore,
  type SubmitDeps,
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

/** Kode Postgres untuk pelanggaran UNIQUE(schedule_id, student_id). */
const UNIQUE_VIOLATION = "23505";

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
    return json({ error: ERR_UMUM }, 400);
  }
  const scheduleId = Number(body["schedule_id"]);
  const method = body["method"];
  if (!Number.isInteger(scheduleId) || !isSubmitMethod(method)) {
    // schedule_id tak valid diperlakukan sebagai jadwal tak ditemukan
    // di core (tetap generik Indonesia); method tak valid ditolak di sini.
    if (!isSubmitMethod(method)) {
      return json({ error: ERR_METODE_TIDAK_VALID }, 400);
    }
  }

  const supabaseUrl = Deno.env.get("SUPABASE_URL") ?? "";
  const anonKey = Deno.env.get("SUPABASE_ANON_KEY") ?? "";
  const serviceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "";
  if (!supabaseUrl || !anonKey || !serviceKey) {
    return json({ error: ERR_UMUM }, 500);
  }

  try {
    // 1. Verify JWT -> student_id.
    const userClient = createClient(supabaseUrl, anonKey, {
      global: { headers: { Authorization: `Bearer ${jwt}` } },
    });
    const { data: { user }, error: userError } = await userClient.auth
      .getUser();
    if (userError || !user) return json({ error: ERR_AUTH }, 401);

    // 2. Wiring Supabase ke core (service role: function adalah penulis
    //    tepercaya — Task 1 memblokir direct insert dari client).
    const admin = createClient(supabaseUrl, serviceKey);
    const deps: SubmitDeps = {
      now: () => new Date(),
      findSchedule: async (id) => {
        const { data } = await admin
          .from("schedules")
          .select(
            "id, start_time, is_active, late_threshold_minutes, latitude, longitude, radius_meters",
          )
          .eq("id", id)
          .maybeSingle();
        return (data ?? null) as ScheduleRow | null;
      },
      findExistingRecord: async (schedId, studentId) => {
        const { data } = await admin
          .from("attendance_records")
          .select("id, status, method, created_at")
          .eq("schedule_id", schedId)
          .eq("student_id", studentId)
          .maybeSingle();
        return (data ?? null) as AttendanceRecordRow | null;
      },
      findQrToken: async (token) => {
        const { data } = await admin
          .from("qr_tokens")
          .select("token, schedule_id, expires_at")
          .eq("token", token)
          .maybeSingle();
        return (data ?? null) as QrTokenRow | null;
      },
      findAttendanceCode: async (code) => {
        const { data } = await admin
          .from("attendance_codes")
          .select("code, schedule_id, expires_at, is_active")
          .eq("code", code.toUpperCase())
          .maybeSingle();
        return (data ?? null) as AttendanceCodeRow | null;
      },
      insertRecord: async (row: InsertRecordInput) => {
        const { data, error } = await admin
          .from("attendance_records")
          .insert({
            schedule_id: row.schedule_id,
            student_id: row.student_id,
            method: row.method,
            status: row.status,
            distance: row.distance,
          })
          .select("id, status, method, created_at")
          .single();
        if (error) throw error;
        return data as AttendanceRecordRow;
      },
    };

    // 3. Core (server now, duplicate, branch method, HADIR/TERLAMBAT).
    let result: Awaited<ReturnType<typeof submitAttendanceCore>>;
    try {
      result = await submitAttendanceCore(deps, {
        studentId: user.id,
        scheduleId,
        method,
        latitude: typeof body["latitude"] === "number"
          ? body["latitude"] as number
          : undefined,
        longitude: typeof body["longitude"] === "number"
          ? body["longitude"] as number
          : undefined,
        qrToken: typeof body["qr_token"] === "string"
          ? body["qr_token"] as string
          : undefined,
        attendanceCode: typeof body["attendance_code"] === "string"
          ? body["attendance_code"] as string
          : undefined,
        // clientNow / timestamp kiriman client SENGAJA tidak diteruskan.
      });
    } catch (e) {
      // Race duplicate: UNIQUE violation saat insert balapan.
      const code = (e as { code?: string })?.code;
      if (code === UNIQUE_VIOLATION) {
        return json({ error: ERR_DUPLIKAT }, 409);
      }
      throw e;
    }

    if (!result.ok) {
      const status = result.error.includes("sudah tercatat") ? 409 : 400;
      return json({ error: result.error }, status);
    }
    return json({
      status: result.status,
      method: result.method,
      attendance: {
        id: result.attendance.id,
        status: result.attendance.status,
        method: result.attendance.method,
        created_at: result.attendance.created_at,
      },
    }, 200);
  } catch {
    return json({ error: ERR_UMUM }, 500);
  }
});
