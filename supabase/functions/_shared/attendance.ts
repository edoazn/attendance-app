// Logika inti Edge Functions absensi — MURNI, tanpa import Deno/remote.
// Dipakai langsung oleh submit-attendance/index.ts (wiring ke Supabase)
// dan oleh supabase/functions/tests/submit_attendance_test.ts (mock client).
//
// Kolom mengacu ke supabase/migrations/001_attendance_schema.sql (Task 1):
// schedules(start_time, is_active, late_threshold_minutes, latitude,
// longitude, radius_meters), qr_tokens, attendance_codes,
// attendance_records UNIQUE(schedule_id, student_id).
//
// Aturan jam: status HADIR/TERLAMBAT SELALU dihitung dari server now()
// (parameter `now`), field clientNow dari payload DIABAIKAN — client yang
// clock-nya skew tidak bisa memalsukan status.

// ---------------------------------------------------------------- pesan
// Semua pesan generik Bahasa Indonesia; pesan login SENGAJA identik untuk
// NIM salah vs password salah (anti enumerasi user).
export const ERR_LOGIN_GENERIK = "NIM atau kata sandi salah.";
export const ERR_JADWAL_TIDAK_AKTIF =
  "Jadwal tidak ditemukan atau tidak aktif.";
export const ERR_DUPLIKAT = "Kehadiran sudah tercatat untuk jadwal ini.";
export const ERR_QR_KEDALUWARSA =
  "Kode QR sudah kedaluwarsa. Minta sesi baru kepada dosen.";
export const ERR_QR_TIDAK_VALID = "Kode QR tidak valid.";
export const ERR_KODE_SALAH = "Kode absensi salah atau sudah kedaluwarsa.";
export const ERR_LOKASI_TIDAK_LENGKAP = "Data lokasi tidak lengkap.";
export const ERR_LOKASI_BELUM_DIATUR = "Lokasi kelas belum diatur dosen.";
export const ERR_LOKASI_JANGKAUAN = "Lokasi Anda di luar jangkauan kelas.";
export const ERR_METODE_TIDAK_VALID = "Metode absensi tidak valid.";
export const ERR_AUTH = "Tidak memiliki akses. Silakan masuk kembali.";
export const ERR_DOSEN_SAJA = "Hanya dosen yang dapat membuat sesi absensi.";
export const ERR_SESI_TIDAK_LENGKAP = "Data sesi tidak lengkap.";
export const ERR_SESI_DURASI = "Durasi sesi tidak valid (1-120 menit).";
export const ERR_UMUM = "Terjadi kesalahan. Silakan coba lagi.";

export const STATUS_HADIR = "HADIR";
export const STATUS_TERLAMBAT = "TERLAMBAT";

export const DEFAULT_LATE_THRESHOLD_MINUTES = 15;
export const DEFAULT_RADIUS_METERS = 100;

export type SubmitMethod = "geolocation" | "qr_code" | "attendance_code";

export function isSubmitMethod(v: unknown): v is SubmitMethod {
  return v === "geolocation" || v === "qr_code" || v === "attendance_code";
}

// ---------------------------------------------------------------- tipe baris
export interface ScheduleRow {
  id: number;
  start_time: string; // kolom Postgres `time`, mis. "08:00:00"
  is_active: boolean;
  late_threshold_minutes: number | null;
  latitude: number | null;
  longitude: number | null;
  radius_meters: number | null;
  // Fase-1: sesi ON/OFF manual dosen (migrasi 002).
  session_open: boolean;
  session_expires_at: string | null;
}

export interface QrTokenRow {
  token: string;
  schedule_id: number;
  expires_at: string;
}

export interface AttendanceCodeRow {
  code: string;
  schedule_id: number;
  expires_at: string;
  is_active: boolean;
}

export interface AttendanceRecordRow {
  id: number;
  status: string;
  method: string;
  created_at: string;
}

// ---------------------------------------------------------------- geofence
export function haversineMeters(
  lat1: number,
  lon1: number,
  lat2: number,
  lon2: number,
): number {
  const toRad = (d: number) => (d * Math.PI) / 180;
  const R = 6371000; // radius bumi (meter)
  const dLat = toRad(lat2 - lat1);
  const dLon = toRad(lon2 - lon1);
  const a = Math.sin(dLat / 2) ** 2 +
    Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) *
      Math.sin(dLon / 2) ** 2;
  return 2 * R * Math.asin(Math.sqrt(a));
}

// ---------------------------------------------------------------- status
// start_time disimpan sebagai waktu lokal kampus (WIB). Edge runtime
// berjalan di UTC sehingga getHours()/getMinutes() lokal SALAH sistematis
// (selisih 7 jam — TERLAMBAT hampir tak pernah keluar). Semua perbandingan
// memakai zona eksplisit Asia/Jakarta.
export const CAMPUS_TIME_ZONE = "Asia/Jakarta";

const jakartaTimeFormat = new Intl.DateTimeFormat("en-GB", {
  timeZone: CAMPUS_TIME_ZONE,
  hour: "2-digit",
  minute: "2-digit",
  second: "2-digit",
  hourCycle: "h23",
});

/** Menit sejak tengah malam di zona kampus untuk satu instant. */
export function jakartaMinutesOfDay(now: Date): number {
  const parts = jakartaTimeFormat.formatToParts(now);
  const get = (type: string) =>
    Number(parts.find((p) => p.type === type)?.value ?? 0);
  return get("hour") * 60 + get("minute") + get("second") / 60;
}

function startTimeToMinutes(startTime: string): number {
  const parts = startTime.split(":").map(Number);
  const h = parts[0] ?? 0;
  const m = parts[1] ?? 0;
  const s = parts[2] ?? 0;
  return h * 60 + m + s / 60;
}

/**
 * HADIR bila jam kampus(serverNow) <= start_time + threshold,
 * selain itu TERLAMBAT. serverNow adalah instant (epoch); konversi ke
 * waktu kampus dilakukan eksplisit via CAMPUS_TIME_ZONE.
 */
export function determineStatus(
  serverNow: Date,
  startTime: string,
  lateThresholdMinutes: number | null | undefined,
): string {
  const threshold = lateThresholdMinutes ?? DEFAULT_LATE_THRESHOLD_MINUTES;
  const nowMinutes = jakartaMinutesOfDay(serverNow);
  return nowMinutes <= startTimeToMinutes(startTime) + threshold
    ? STATUS_HADIR
    : STATUS_TERLAMBAT;
}

// ---------------------------------------------------------------- submit
export interface SubmitInput {
  studentId: string;
  scheduleId: number;
  method: SubmitMethod;
  latitude?: number;
  longitude?: number;
  qrToken?: string;
  attendanceCode?: string;
  /** Jam versi client — SELALU diabaikan (anti clock skew). */
  clientNow?: string;
}

export interface InsertRecordInput {
  schedule_id: number;
  student_id: string;
  method: string;
  status: string;
  distance: number | null;
  /** Sesi berjalan (cabang QR fase-1); null untuk metode non-sesi. */
  session_id: number | null;
}

export interface SubmitDeps {
  now: () => Date;
  findSchedule: (id: number) => Promise<ScheduleRow | null>;
  findExistingRecord: (
    scheduleId: number,
    studentId: string,
  ) => Promise<AttendanceRecordRow | null>;
  /** Duplikat fase-1: UNIQUE(session_id, student_id) untuk cabang QR. */
  findExistingSessionRecord: (
    sessionId: number,
    studentId: string,
  ) => Promise<AttendanceRecordRow | null>;
  findQrToken: (token: string) => Promise<QrTokenRow | null>;
  findAttendanceCode: (code: string) => Promise<AttendanceCodeRow | null>;
  /** Sesi berjalan milik jadwal (dibuka via set-session-state). */
  findOpenSession: (
    scheduleId: number,
  ) => Promise<{ id: number } | null>;
  /**
   * Lazy auto-OFF: matikan session_open + tutup sesi berjalan + padamkan
   * kode saat submit datang setelah expiry. Wiring menjalankannya sebagai
   * rangkaian call service-role (tanpa transaksi silang-statement).
   */
  autoCloseExpiredSession: (
    scheduleId: number,
    closedAt: string,
  ) => Promise<void>;
  insertRecord: (row: InsertRecordInput) => Promise<AttendanceRecordRow>;
}

export type SubmitResult =
  | {
    ok: true;
    status: string;
    method: string;
    attendance: AttendanceRecordRow;
  }
  | { ok: false; code: SubmitErrorCode; error: string };

/**
 * Kode error terketik untuk pemetaan status HTTP di handler
 * (jangan sniff teks pesan: redaksional boleh berubah tanpa
 * merusak status code).
 */
export type SubmitErrorCode =
  | "INVALID_METHOD"
  | "SCHEDULE_INACTIVE"
  | "DUPLICATE"
  | "LOCATION_INCOMPLETE"
  | "LOCATION_NOT_SET"
  | "OUT_OF_RANGE"
  | "QR_INVALID"
  | "QR_EXPIRED"
  | "CODE_INVALID";

function fail(code: SubmitErrorCode, error: string): SubmitResult {
  return { ok: false, code, error };
}

function isExpired(expiresAt: string, now: Date): boolean {
  return new Date(expiresAt).getTime() <= now.getTime();
}

export async function submitAttendanceCore(
  deps: SubmitDeps,
  input: SubmitInput,
): Promise<SubmitResult> {
  const serverNow = deps.now(); // SATU-SATUNYA sumber jam (abaikan clientNow).

  if (!isSubmitMethod(input.method)) {
    return fail("INVALID_METHOD", ERR_METODE_TIDAK_VALID);
  }

  const schedule = await deps.findSchedule(input.scheduleId);
  if (!schedule || !schedule.is_active) {
    return fail("SCHEDULE_INACTIVE", ERR_JADWAL_TIDAK_AKTIF);
  }

  // Fase-1: semua submit yang diterima berstatus HADIR. Aturan TERLAMBAT
  // tidak dipakai (absensi hanya mungkin saat sesi ON habis ngajar);
  // late_threshold_minutes diabaikan, determineStatus dipertahankan untuk
  // kompatibilitas.
  const status = STATUS_HADIR;

  let distance: number | null = null;
  let sessionId: number | null = null;

  if (input.method === "geolocation") {
    if (
      typeof input.latitude !== "number" ||
      typeof input.longitude !== "number" ||
      !Number.isFinite(input.latitude) ||
      !Number.isFinite(input.longitude)
    ) {
      return fail("LOCATION_INCOMPLETE", ERR_LOKASI_TIDAK_LENGKAP);
    }
    // Fase-1: geofence hanya bila jadwal punya koordinat (null = mati).
    if (schedule.latitude != null && schedule.longitude != null) {
      distance = haversineMeters(
        schedule.latitude,
        schedule.longitude,
        input.latitude,
        input.longitude,
      );
      const radius = schedule.radius_meters ?? DEFAULT_RADIUS_METERS;
      if (distance > radius) {
        return fail("OUT_OF_RANGE", ERR_LOKASI_JANGKAUAN);
      }
    }
    const existing = await deps.findExistingRecord(
      input.scheduleId,
      input.studentId,
    );
    if (existing) return fail("DUPLICATE", ERR_DUPLIKAT);
  } else if (input.method === "qr_code") {
    const token = (input.qrToken ?? "").trim();
    if (!token) return fail("QR_INVALID", ERR_QR_TIDAK_VALID);
    const row = await deps.findQrToken(token);
    // Token statis milik jadwal lain (mismatch) ditolak generik.
    if (!row || row.schedule_id !== input.scheduleId) {
      return fail("QR_INVALID", ERR_QR_TIDAK_VALID);
    }
    // Wajib sesi ON dan dalam jendela expiry (sumber tunggal
    // schedules.session_expires_at; expiry per-baris qr_tokens diabaikan).
    const expiry = schedule.session_expires_at == null
      ? null
      : new Date(schedule.session_expires_at);
    if (
      !schedule.session_open || expiry == null ||
      expiry.getTime() <= serverNow.getTime()
    ) {
      if (schedule.session_open) {
        await deps.autoCloseExpiredSession(
          input.scheduleId,
          serverNow.toISOString(),
        );
      }
      return fail("QR_EXPIRED", ERR_QR_KEDALUWARSA);
    }
    const open = await deps.findOpenSession(input.scheduleId);
    if (!open) {
      // Flag ON tapi baris sesi hilang: sembuhkan + tolak submit ini.
      await deps.autoCloseExpiredSession(
        input.scheduleId,
        serverNow.toISOString(),
      );
      return fail("QR_EXPIRED", ERR_QR_KEDALUWARSA);
    }
    const existing = await deps.findExistingSessionRecord(
      open.id,
      input.studentId,
    );
    if (existing) return fail("DUPLICATE", ERR_DUPLIKAT);
    sessionId = open.id;
  } else {
    const code = (input.attendanceCode ?? "").trim().toUpperCase();
    if (!code) return fail("CODE_INVALID", ERR_KODE_SALAH);
    const row = await deps.findAttendanceCode(code);
    if (!row || row.schedule_id !== input.scheduleId) {
      return fail("CODE_INVALID", ERR_KODE_SALAH);
    }
    if (!row.is_active || isExpired(row.expires_at, serverNow)) {
      return fail("CODE_INVALID", ERR_KODE_SALAH);
    }
    const existing = await deps.findExistingRecord(
      input.scheduleId,
      input.studentId,
    );
    if (existing) return fail("DUPLICATE", ERR_DUPLIKAT);
  }

  const attendance = await deps.insertRecord({
    schedule_id: input.scheduleId,
    student_id: input.studentId,
    method: input.method,
    status,
    distance,
    session_id: sessionId,
  });

  return { ok: true, status, method: input.method, attendance };
}

// ---------------------------------------------------------------- login
export type LoginFailureReason =
  | "nim_not_found"
  | "wrong_password"
  | "missing_email";

/** Sengaja identik untuk semua reason (anti enumerasi NIM). */
export function loginErrorMessage(_reason: LoginFailureReason): string {
  return ERR_LOGIN_GENERIK;
}

// ---------------------------------------------------------------- sesi dosen
// Fase-1: token QR statis per jadwal + sesi ON/OFF manual.
// ensureSessionQrCore dipakai handler ensure-session-qr; diuji di
// supabase/functions/tests/ensure_session_qr_test.ts dengan mock deps.
/**
 * Guard jadwal aktif untuk generate-session: jadwal arsip (is_active=false)
 * atau hilang (null) TIDAK boleh mendapat qr_token/code baru.
 * Dipakai handler generate-session; diuji di
 * supabase/functions/tests/generate_session_test.ts.
 */
export function isSchedulableForNewSession(
  schedule: { is_active: boolean } | null,
): boolean {
  return schedule != null && schedule.is_active === true;
}

export function buildSessionExpiry(now: Date, durationMinutes: number): Date {
  return new Date(now.getTime() + durationMinutes * 60_000);
}

const CODE_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

/** Kode absensi 6 char UPPER alnum; random dapat di-inject untuk test. */
export function generateAttendanceCode(
  randomInt?: (max: number) => number,
): string {
  const pick = randomInt ??
    ((max: number) => {
      const buf = new Uint32Array(1);
      crypto.getRandomValues(buf);
      return buf[0] % max;
    });
  let code = "";
  for (let i = 0; i < 6; i++) {
    code += CODE_ALPHABET[pick(CODE_ALPHABET.length)];
  }
  return code;
}

// ------------------------------------------------- ensure-session-qr (fase-1)
export interface ScheduleOwnershipRow {
  id: number;
  is_active: boolean;
  lecturer_id: string | null;
}

export interface EnsureQrInput {
  callerId: string;
  isLecturer: boolean;
  scheduleId: number;
}

export interface EnsureQrDeps {
  /** Token baru; default crypto.randomUUID, dapat di-inject untuk test. */
  newToken: () => string;
  findSchedule: (id: number) => Promise<ScheduleOwnershipRow | null>;
  findQrTokenBySchedule: (scheduleId: number) => Promise<string | null>;
  insertQrToken: (scheduleId: number, token: string) => Promise<void>;
}

export type EnsureQrErrorCode =
  | "NOT_LECTURER"
  | "SCHEDULE_NOT_FOUND"
  | "NOT_OWNER";

export type EnsureQrResult =
  | { ok: true; qr_token: string; created: boolean }
  | { ok: false; code: EnsureQrErrorCode; error: string };

/**
 * Idempoten: jadwal yang sudah punya token mengembalikan token yang sama
 * dengan created=false. lecturer_id NULL (pool unassigned) boleh diakses
 * lecturer mana pun; jadwal arsip/hilang ditolak generik.
 */
export async function ensureSessionQrCore(
  deps: EnsureQrDeps,
  input: EnsureQrInput,
): Promise<EnsureQrResult> {
  if (!input.isLecturer) {
    return { ok: false, code: "NOT_LECTURER", error: ERR_DOSEN_SAJA };
  }
  const schedule = await deps.findSchedule(input.scheduleId);
  if (!schedule || !schedule.is_active) {
    return {
      ok: false,
      code: "SCHEDULE_NOT_FOUND",
      error: ERR_JADWAL_TIDAK_AKTIF,
    };
  }
  if (schedule.lecturer_id != null && schedule.lecturer_id !== input.callerId) {
    return { ok: false, code: "NOT_OWNER", error: ERR_DOSEN_SAJA };
  }
  const existing = await deps.findQrTokenBySchedule(input.scheduleId);
  if (existing) return { ok: true, qr_token: existing, created: false };

  const token = deps.newToken();
  try {
    await deps.insertQrToken(input.scheduleId, token);
  } catch {
    // Balapan: baris sudah dibuat penelepon lain -> baca ulang.
    const raced = await deps.findQrTokenBySchedule(input.scheduleId);
    if (raced) return { ok: true, qr_token: raced, created: false };
    throw new Error("insertQrToken failed");
  }
  return { ok: true, qr_token: token, created: true };
}

// ------------------------------------------------- set-session-state (fase-1)
export const DEFAULT_SESSION_DURATION_MINUTES = 10;
export const MAX_SESSION_DURATION_MINUTES = 120;

export interface SessionScheduleRow {
  id: number;
  is_active: boolean;
  lecturer_id: string | null;
  session_open: boolean;
  session_expires_at: string | null;
}

export interface SetSessionInput {
  callerId: string;
  isLecturer: boolean;
  scheduleId: number;
  state: string;
  durationMinutes?: number;
}

export interface SetSessionDeps {
  now: () => Date;
  findSchedule: (id: number) => Promise<SessionScheduleRow | null>;
  findOpenSession: (
    scheduleId: number,
  ) => Promise<{ id: number } | null>;
  createSession: (scheduleId: number) => Promise<{ id: number }>;
  closeOpenSessions: (
    scheduleId: number,
    closedAt: string,
  ) => Promise<number>;
  updateSchedule: (
    scheduleId: number,
    patch: { session_open: boolean; session_expires_at: string | null },
  ) => Promise<void>;
  setCodesActive: (scheduleId: number, active: boolean) => Promise<void>;
  ensurePermanentCode: (
    scheduleId: number,
    code: string,
  ) => Promise<string>;
  refreshQrExpiry: (
    scheduleId: number,
    expiresAt: string,
  ) => Promise<void>;
}

export type SetSessionErrorCode =
  | "NOT_LECTURER"
  | "SCHEDULE_NOT_FOUND"
  | "NOT_OWNER"
  | "INVALID_DURATION"
  | "INVALID_STATE";

export type SetSessionResult =
  | {
    ok: true;
    state: "on" | "off";
    session_id: number | null;
    expires_at: string | null;
  }
  | { ok: false; code: SetSessionErrorCode; error: string };

function setFail(
  code: SetSessionErrorCode,
  error: string,
): SetSessionResult {
  return { ok: false, code, error };
}

/**
 * ON: buka/perpanjang sesi (satu baris sesi berjalan; ON ganda hanya
 * memundurkan expiry). OFF: tutup sesi berjalan (no-op bila sudah OFF).
 * Jam selalu server now(); durasi default 10, range 1-120.
 */
export async function setSessionStateCore(
  deps: SetSessionDeps,
  input: SetSessionInput,
): Promise<SetSessionResult> {
  if (!input.isLecturer) {
    return setFail("NOT_LECTURER", ERR_DOSEN_SAJA);
  }
  if (input.state !== "on" && input.state !== "off") {
    return setFail("INVALID_STATE", ERR_SESI_TIDAK_LENGKAP);
  }
  const schedule = await deps.findSchedule(input.scheduleId);
  if (!schedule || !schedule.is_active) {
    return setFail("SCHEDULE_NOT_FOUND", ERR_JADWAL_TIDAK_AKTIF);
  }
  if (
    schedule.lecturer_id != null && schedule.lecturer_id !== input.callerId
  ) {
    return setFail("NOT_OWNER", ERR_DOSEN_SAJA);
  }

  const serverNow = deps.now();

  if (input.state === "off") {
    if (!schedule.session_open) {
      return { ok: true, state: "off", session_id: null, expires_at: null };
    }
    const open = await deps.findOpenSession(input.scheduleId);
    await deps.closeOpenSessions(
      input.scheduleId,
      serverNow.toISOString(),
    );
    await deps.updateSchedule(input.scheduleId, {
      session_open: false,
      session_expires_at: null,
    });
    await deps.setCodesActive(input.scheduleId, false);
    return {
      ok: true,
      state: "off",
      session_id: open?.id ?? null,
      expires_at: null,
    };
  }

  const duration = input.durationMinutes ?? DEFAULT_SESSION_DURATION_MINUTES;
  if (
    !Number.isInteger(duration) || duration < 1 ||
    duration > MAX_SESSION_DURATION_MINUTES
  ) {
    return setFail("INVALID_DURATION", ERR_SESI_DURASI);
  }
  const expiresAt =
    new Date(serverNow.getTime() + duration * 60_000).toISOString();

  // ON saat ON: perpanjang expiry sesi berjalan, tanpa baris sesi baru.
  // Bila flag ON tapi baris sesi hilang (inkonsisten), sembuhkan: buat baru.
  let sessionId: number;
  const open = schedule.session_open
    ? await deps.findOpenSession(input.scheduleId)
    : null;
  if (open) {
    sessionId = open.id;
  } else {
    sessionId = (await deps.createSession(input.scheduleId)).id;
  }
  await deps.updateSchedule(input.scheduleId, {
    session_open: true,
    session_expires_at: expiresAt,
  });
  await deps.setCodesActive(input.scheduleId, true);
  await deps.ensurePermanentCode(input.scheduleId, generateAttendanceCode());
  await deps.refreshQrExpiry(input.scheduleId, expiresAt);
  return { ok: true, state: "on", session_id: sessionId, expires_at: expiresAt };
}
