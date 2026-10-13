// POST approve-permit(body: {permit_id, decision: 'approved'|'rejected'},
//   auth: ADMIN JWT atau lecturer JWT pemilik jadwal)
// -> {status, attendance_id} | {error}
// Fase-2: approved menulis baris IZIN (method 'izin', tanpa sesi) + update
// request; rejected hanya update request. Semua tulis via service role.

import { createClient } from "jsr:@supabase/supabase-js@2";
import {
  type ApprovePermitDeps,
  approvePermitCore,
  ERR_AUTH,
  ERR_DOSEN_SAJA,
  ERR_IZIN_SUDAH_DIPROSES,
  ERR_JADWAL_TIDAK_AKTIF,
  ERR_SESI_TIDAK_LENGKAP,
  ERR_UMUM,
  type PermitRequestRow,
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
  const permitId = Number(body["permit_id"]);
  const decision = body["decision"];
  if (
    !Number.isInteger(permitId) ||
    (decision !== "approved" && decision !== "rejected")
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
    const role = (profile as { role?: string } | null)?.role ?? null;

    const deps: ApprovePermitDeps = {
      findRequest: async (id) => {
        const { data } = await admin
          .from("permit_requests")
          .select("id, schedule_id, student_id, status, alasan")
          .eq("id", id)
          .maybeSingle();
        return (data ?? null) as PermitRequestRow | null;
      },
      findScheduleOwner: async (scheduleId) => {
        const { data } = await admin
          .from("schedules")
          .select("lecturer_id")
          .eq("id", scheduleId)
          .maybeSingle();
        return (data ?? null) as { lecturer_id: string | null } | null;
      },
      insertIzinRecord: async (scheduleId, studentId) => {
        const { data, error } = await admin
          .from("attendance_records")
          .insert({
            schedule_id: scheduleId,
            student_id: studentId,
            method: "izin",
            status: "IZIN",
            distance: null,
            session_id: null,
          })
          .select("id")
          .single();
        if (error || !data) throw error ?? new Error("insertIzin failed");
        return data as { id: number };
      },
      updateRequestStatus: async (id, status) => {
        const { error } = await admin
          .from("permit_requests")
          .update({ status })
          .eq("id", id);
        if (error) throw error;
      },
    };

    const result = await approvePermitCore(deps, {
      callerId: user.id,
      isAdmin: role === "ADMIN",
      isLecturer: role === "LECTURER",
      permitId,
      decision,
    });
    if (!result.ok) {
      switch (result.code) {
        case "NOT_FOUND":
        case "SCHEDULE_NOT_FOUND":
          return json({ error: ERR_JADWAL_TIDAK_AKTIF }, 404);
        case "ALREADY_DECIDED":
          return json({ error: ERR_IZIN_SUDAH_DIPROSES }, 409);
        case "INVALID_DECISION":
          return json({ error: ERR_SESI_TIDAK_LENGKAP }, 400);
        default:
          return json({ error: ERR_DOSEN_SAJA }, 403);
      }
    }
    return json({
      status: result.status,
      attendance_id: result.attendance_id,
    }, 200);
  } catch {
    return json({ error: ERR_UMUM }, 500);
  }
});
