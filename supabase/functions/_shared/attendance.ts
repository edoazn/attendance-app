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
}

export interface SubmitDeps {
  now: () => Date;
  findSchedule: (id: number) => Promise<ScheduleRow | null>;
  findExistingRecord: (
    scheduleId: number,
    studentId: string,
  ) => Promise<AttendanceRecordRow | null>;
  findQrToken: (token: string) => Promise<QrTokenRow | null>;
  findAttendanceCode: (code: string) => Promise<AttendanceCodeRow | null>;
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

  const existing = await deps.findExistingRecord(
    input.scheduleId,
    input.studentId,
  );
  if (existing) return fail("DUPLICATE", ERR_DUPLIKAT);

  let distance: number | null = null;

  if (input.method === "geolocation") {
    if (
      typeof input.latitude !== "number" ||
      typeof input.longitude !== "number" ||
      !Number.isFinite(input.latitude) ||
      !Number.isFinite(input.longitude)
    ) {
      return fail("LOCATION_INCOMPLETE", ERR_LOKASI_TIDAK_LENGKAP);
    }
    if (schedule.latitude == null || schedule.longitude == null) {
      return fail("LOCATION_NOT_SET", ERR_LOKASI_BELUM_DIATUR);
    }
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
  } else if (input.method === "qr_code") {
    const token = (input.qrToken ?? "").trim();
    if (!token) return fail("QR_INVALID", ERR_QR_TIDAK_VALID);
    const row = await deps.findQrToken(token);
    if (!row || row.schedule_id !== input.scheduleId) {
      return fail("QR_INVALID", ERR_QR_TIDAK_VALID);
    }
    if (isExpired(row.expires_at, serverNow)) {
      return fail("QR_EXPIRED", ERR_QR_KEDALUWARSA);
    }
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
  }

  const status = determineStatus(
    serverNow,
    schedule.start_time,
    schedule.late_threshold_minutes,
  );

  const attendance = await deps.insertRecord({
    schedule_id: input.scheduleId,
    student_id: input.studentId,
    method: input.method,
    status,
    distance,
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
