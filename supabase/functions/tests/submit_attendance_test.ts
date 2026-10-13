// Test submit-attendance (+ pesan error generik login-with-nim).
// Pola: logika inti di ../_shared/attendance.ts diuji dengan mock client
// (tanpa jaringan), sesuai Step 1 task-2-brief.md.
//
// 1. Duplicate: call ke-1 ok, call ke-2 schedule+student sama -> error "sudah".
// 2. QR kedaluwarsa: qr_token expired -> error "kedaluwarsa".
// 3. Clock skew: client clock maju 2 jam -> status tetap dari server now().
// 4. Login generik: NIM salah vs password salah -> pesan error IDENTIK.
//
// Jalankan: deno test supabase/functions/tests/submit_attendance_test.ts

import { assert, assertEquals, assertStringIncludes } from "jsr:@std/assert@1";
import {
  type AttendanceRecordRow,
  ERR_LOGIN_GENERIK,
  loginErrorMessage,
  type QrTokenRow,
  type ScheduleRow,
  submitAttendanceCore,
  type SubmitDeps,
} from "../_shared/attendance.ts";

const STUDENT_A = "11111111-1111-1111-1111-111111111111";

// Jadwal 08:00, threshold 15 mnt, kampus Monas (-6.2, 106.816666), radius 100 m.
function scheduleFisik(): ScheduleRow {
  return {
    id: 1,
    start_time: "08:00:00",
    is_active: true,
    late_threshold_minutes: 15,
    latitude: -6.2,
    longitude: 106.816666,
    radius_meters: 100,
  };
}

interface MockDb {
  schedule: ScheduleRow | null;
  records: AttendanceRecordRow[];
  qrTokens: Map<string, QrTokenRow>;
  codes: Map<
    string,
    {
      code: string;
      schedule_id: number;
      expires_at: string;
      is_active: boolean;
    }
  >;
  serverNow: Date;
  nextId: number;
}

function mockDb(serverNow: Date): MockDb {
  return {
    schedule: scheduleFisik(),
    records: [],
    qrTokens: new Map(),
    codes: new Map(),
    serverNow,
    nextId: 1,
  };
}

function deps(db: MockDb): SubmitDeps {
  return {
    now: () => new Date(db.serverNow.getTime()),
    findSchedule: (id) =>
      Promise.resolve(
        db.schedule && db.schedule.id === id ? db.schedule : null,
      ),
    findExistingRecord: (scheduleId, studentId) =>
      Promise.resolve(
        db.records.find((r) =>
          (r as unknown as { schedule_id: number; student_id: string })
              .schedule_id === scheduleId &&
          (r as unknown as { student_id: string }).student_id === studentId
        ) ?? null,
      ),
    findQrToken: (token) => Promise.resolve(db.qrTokens.get(token) ?? null),
    findAttendanceCode: (code) =>
      Promise.resolve(db.codes.get(code.toUpperCase()) ?? null),
    insertRecord: (row) => {
      const rec = {
        id: db.nextId++,
        status: row.status,
        method: row.method,
        created_at: new Date(db.serverNow.getTime()).toISOString(),
        ...({
          schedule_id: row.schedule_id,
          student_id: row.student_id,
        } as unknown as object),
      } as unknown as AttendanceRecordRow;
      db.records.push(rec);
      return Promise.resolve(rec);
    },
  };
}

// Kampus Monas: server jam 08:05 (masih dalam threshold 08:00+15) -> HADIR.
const SERVER_PAGI = new Date(2026, 9, 14, 8, 5, 0);

Deno.test("submit 1: duplicate - call ke-2 mengandung 'sudah'", async () => {
  const db = mockDb(SERVER_PAGI);
  const d = deps(db);

  const first = await submitAttendanceCore(d, {
    studentId: STUDENT_A,
    scheduleId: 1,
    method: "geolocation",
    latitude: -6.2,
    longitude: 106.816666,
  });
  assert(first.ok, `call pertama harus ok, dapat: ${JSON.stringify(first)}`);

  const second = await submitAttendanceCore(d, {
    studentId: STUDENT_A,
    scheduleId: 1,
    method: "geolocation",
    latitude: -6.2,
    longitude: 106.816666,
  });
  assert(!second.ok, "call kedua harus gagal (duplicate)");
  if (!second.ok) assertStringIncludes(second.error, "sudah");
});

Deno.test("submit 2: qr_token kedaluwarsa mengandung 'kedaluwarsa'", async () => {
  const db = mockDb(SERVER_PAGI);
  db.qrTokens.set("qr-expired-1", {
    token: "qr-expired-1",
    schedule_id: 1,
    // Sudah lewat 1 jam dari server now.
    expires_at: new Date(db.serverNow.getTime() - 3600_000).toISOString(),
  });

  const res = await submitAttendanceCore(deps(db), {
    studentId: STUDENT_A,
    scheduleId: 1,
    method: "qr_code",
    qrToken: "qr-expired-1",
  });
  assert(!res.ok, "qr kedaluwarsa harus gagal");
  if (!res.ok) assertStringIncludes(res.error, "kedaluwarsa");
});

Deno.test("submit 3: clock skew - client maju 2 jam, status dari server now()", async () => {
  const db = mockDb(SERVER_PAGI);
  // Client mengklaim jam 10:05 (sudah lewat deadline 08:15 -> seolah TERLAMBAT),
  // tapi server jam 08:05 -> harus HADIR.
  const clientSkew = new Date(db.serverNow.getTime() + 2 * 3600_000)
    .toISOString();

  const res = await submitAttendanceCore(deps(db), {
    studentId: STUDENT_A,
    scheduleId: 1,
    method: "geolocation",
    latitude: -6.2,
    longitude: 106.816666,
    clientNow: clientSkew,
  });
  assert(res.ok, `harus ok, dapat: ${JSON.stringify(res)}`);
  if (res.ok) {
    assertEquals(res.status, "HADIR");
    assertEquals(res.attendance.status, "HADIR");
  }
});

Deno.test("login 4: NIM salah vs password salah pesannya identik generik", () => {
  const errNimSalah = loginErrorMessage("nim_not_found");
  const errPasswordSalah = loginErrorMessage("wrong_password");
  assertEquals(errNimSalah, errPasswordSalah);
  assertEquals(errNimSalah, ERR_LOGIN_GENERIK);
  // Tidak boleh membocorkan info enumerasi user.
  assert(!errNimSalah.toLowerCase().includes("terdaftar"));
  assert(!errNimSalah.toLowerCase().includes("tidak ditemukan"));
});

Deno.test("submit 5: qr valid + kode lower-case tetap ok, lokasi jauh ditolak", async () => {
  const db = mockDb(SERVER_PAGI);
  db.qrTokens.set("qr-valid-1", {
    token: "qr-valid-1",
    schedule_id: 1,
    expires_at: new Date(db.serverNow.getTime() + 3600_000).toISOString(),
  });
  db.codes.set("ABC123", {
    code: "ABC123",
    schedule_id: 1,
    expires_at: new Date(db.serverNow.getTime() + 3600_000).toISOString(),
    is_active: true,
  });

  const qr = await submitAttendanceCore(deps(db), {
    studentId: STUDENT_A,
    scheduleId: 1,
    method: "qr_code",
    qrToken: "qr-valid-1",
  });
  assert(qr.ok, `qr valid harus ok, dapat: ${JSON.stringify(qr)}`);

  // Student lain pakai kode lower-case -> dinormalisasi ke UPPER, tetap ok.
  const code = await submitAttendanceCore(deps(db), {
    studentId: "22222222-2222-2222-2222-222222222222",
    scheduleId: 1,
    method: "attendance_code",
    attendanceCode: "abc123",
  });
  assert(code.ok, `kode lower-case harus ok, dapat: ${JSON.stringify(code)}`);

  // Student ketiga di luar geofence (Jakarta -> Bandung) -> ditolak.
  const jauh = await submitAttendanceCore(deps(db), {
    studentId: "33333333-3333-3333-3333-333333333333",
    scheduleId: 1,
    method: "geolocation",
    latitude: -6.914744,
    longitude: 107.60981,
  });
  assert(!jauh.ok, "di luar jangkauan harus gagal");
  if (!jauh.ok) assertStringIncludes(jauh.error.toLowerCase(), "jangkauan");
});
