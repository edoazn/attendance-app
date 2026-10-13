// Test submit-attendance (+ pesan error generik login-with-nim).
// Pola: logika inti di ../_shared/attendance.ts diuji dengan mock client
// (tanpa jaringan), sesuai Step 1 task-2-brief.md.
//
// 1. Duplicate: call ke-1 ok, call ke-2 schedule+student sama -> error "sudah".
// 2. Sesi kedaluwarsa: server lewat session_expires_at -> error "kedaluwarsa"
//    + lazy auto-OFF (session_open=false).
// 3. Clock skew: client clock maju 2 jam -> status tetap dari server now().
// 4. Login generik: NIM salah vs password salah -> pesan error IDENTIK.
// 5. QR valid + kode lower-case ok, geofence jauh ditolak.
// 6. HADIR-only (fase-1): submit kapan pun yang diterima -> HADIR.
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
// Sesi default TUTUP (fase-1: submit QR hanya saat ON).
function scheduleFisik(): ScheduleRow {
  return {
    id: 1,
    start_time: "08:00:00",
    is_active: true,
    late_threshold_minutes: 15,
    latitude: -6.2,
    longitude: 106.816666,
    radius_meters: 100,
    session_open: false,
    session_expires_at: null,
  };
}

interface MockDb {
  schedule: ScheduleRow | null;
  records: AttendanceRecordRow[];
  sessionRecords: { session_id: number; student_id: string }[];
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
  openSessionId: number | null;
}

function mockDb(serverNow: Date): MockDb {
  return {
    schedule: scheduleFisik(),
    records: [],
    sessionRecords: [],
    qrTokens: new Map(),
    codes: new Map(),
    serverNow,
    nextId: 1,
    openSessionId: null,
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
    findExistingSessionRecord: (sessionId, studentId) =>
      Promise.resolve(
        db.sessionRecords.some((r) =>
          r.session_id === sessionId && r.student_id === studentId
        )
          ? {
            id: 1,
            status: "HADIR",
            method: "qr_code",
            created_at: new Date(db.serverNow.getTime()).toISOString(),
          }
          : null,
      ),
    findOpenSession: (scheduleId) =>
      Promise.resolve(
        db.schedule && db.schedule.id === scheduleId &&
          db.openSessionId !== null
          ? { id: db.openSessionId }
          : null,
      ),
    autoCloseExpiredSession: (_scheduleId, _closedAt) => {
      if (db.schedule) {
        db.schedule.session_open = false;
        db.schedule.session_expires_at = null;
      }
      return Promise.resolve();
    },
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
      if (row.session_id !== null && row.session_id !== undefined) {
        db.sessionRecords.push({
          session_id: row.session_id,
          student_id: row.student_id,
        });
      }
      return Promise.resolve(rec);
    },
  };
}

// Instant eksplisit (UTC) agar independen dari TZ mesin uji maupun
// runtime Edge (UTC): 01:05 UTC == 08:05 WIB (dalam threshold 08:00+15).
// Versi lama memakai new Date(2026,9,14,8,5,0) runtime-local yang
// mem-masking bug zona di mesin ber-TZ WIB.
const SERVER_PAGI = new Date(Date.UTC(2026, 9, 14, 1, 5, 0));
// 01:20 UTC == 08:20 WIB (lewat deadline 08:15) -> harus TERLAMBAT.
// Di kode lama (getHours runtime UTC) ini terhitung 01:20 <= 08:15 -> HADIR.
const SERVER_TELAT_WIB = new Date(Date.UTC(2026, 9, 14, 1, 20, 0));

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
  if (!second.ok) {
    assertStringIncludes(second.error, "sudah");
    assertEquals(second.code, "DUPLICATE");
  }
});

Deno.test("submit 2: sesi kedaluwarsa mengandung 'kedaluwarsa' + auto-OFF", async () => {
  const db = mockDb(SERVER_PAGI);
  if (db.schedule) {
    db.schedule.session_open = true;
    // Expiry sudah lewat (mock now = SERVER_PAGI).
    db.schedule.session_expires_at =
      new Date(db.serverNow.getTime() - 1000).toISOString();
  }
  db.qrTokens.set("qr-statis-1", {
    token: "qr-statis-1",
    schedule_id: 1,
    // Expiry per-baris DIABAIKAN fase-1 (sumber tunggal = sesi).
    expires_at: new Date(db.serverNow.getTime() + 3600_000).toISOString(),
  });
  db.openSessionId = 7;

  const res = await submitAttendanceCore(deps(db), {
    studentId: STUDENT_A,
    scheduleId: 1,
    method: "qr_code",
    qrToken: "qr-statis-1",
  });
  assert(!res.ok, "sesi kedaluwarsa harus gagal");
  if (!res.ok) {
    assertStringIncludes(res.error, "kedaluwarsa");
    assertEquals(res.code, "QR_EXPIRED");
  }
  assert(
    db.schedule && !db.schedule.session_open,
    "sesi harus lazy auto-OFF",
  );
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
  // Sesi ON: submit QR diterima.
  if (db.schedule) {
    db.schedule.session_open = true;
    db.schedule.session_expires_at =
      new Date(db.serverNow.getTime() + 3600_000).toISOString();
  }
  db.openSessionId = 7;
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
  if (!jauh.ok) {
    assertStringIncludes(jauh.error.toLowerCase(), "jangkauan");
    assertEquals(jauh.code, "OUT_OF_RANGE");
  }
});

Deno.test("submit 6: HADIR-only - submit malam pun tetap HADIR", async () => {
  const geo = {
    studentId: STUDENT_A,
    scheduleId: 1,
    method: "geolocation" as const,
    latitude: -6.2,
    longitude: 106.816666,
  };
  const malam = await submitAttendanceCore(
    deps(mockDb(SERVER_TELAT_WIB)),
    geo,
  );
  assert(malam.ok, `harus ok, dapat: ${JSON.stringify(malam)}`);
  if (malam.ok) {
    assertEquals(malam.status, "HADIR");
    assertEquals(malam.attendance.status, "HADIR");
  }

  // Kontrol arah berlawanan: 23:30 UTC (=06:30 WIB, sebelum mulai) -> HADIR.
  const pagiButa = await submitAttendanceCore(
    deps(mockDb(new Date(Date.UTC(2026, 9, 13, 23, 30, 0)))),
    geo,
  );
  assert(pagiButa.ok, `harus ok, dapat: ${JSON.stringify(pagiButa)}`);
  if (pagiButa.ok) assertEquals(pagiButa.status, "HADIR");
});

// ============================================================ FASE-1 QR statis
// Task 4 plan lecturer-backend-fase1: token statis + sesi ON/OFF + HADIR-only.
// Mock session-aware (terpisah dari mockDb/deps lama di atas).

interface Fase1Db {
  sessionOpen: boolean;
  sessionExpiresAt: string | null;
  openSessionId: number | null;
  staticToken: string;
  staticTokenScheduleId: number;
  sessionRecords: { session_id: number; student_id: string }[];
  serverNow: Date;
  autoClosed: boolean;
  nextId: number;
}

function fase1Db(serverNow: Date): Fase1Db {
  return {
    sessionOpen: true,
    sessionExpiresAt: new Date(serverNow.getTime() + 600_000).toISOString(),
    openSessionId: 10,
    staticToken: "token-statis-jadwal-1",
    staticTokenScheduleId: 1,
    sessionRecords: [],
    serverNow,
    autoClosed: false,
    nextId: 1,
  };
}

function fase1Deps(db: Fase1Db): SubmitDeps {
  return {
    now: () => new Date(db.serverNow.getTime()),
    findSchedule: (id) =>
      Promise.resolve(
        id === 1
          ? {
            id: 1,
            start_time: "08:00:00",
            is_active: true,
            late_threshold_minutes: 15,
            latitude: null,
            longitude: null,
            radius_meters: 100,
            session_open: db.sessionOpen,
            session_expires_at: db.sessionExpiresAt,
          }
          : null,
      ),
    findExistingRecord: (_scheduleId, _studentId) =>
      Promise.resolve(null),
    findExistingSessionRecord: (sessionId, studentId) =>
      Promise.resolve(
        db.sessionRecords.some((r) =>
          r.session_id === sessionId && r.student_id === studentId
        )
          ? { id: 1, status: "HADIR", method: "qr_code", created_at: "" }
          : null,
      ),
    findQrToken: (token) =>
      Promise.resolve(
        token === db.staticToken
          ? {
            token,
            schedule_id: db.staticTokenScheduleId,
            expires_at: new Date(0).toISOString(),
          }
          : null,
      ),
    findAttendanceCode: (_code) => Promise.resolve(null),
    findOpenSession: (scheduleId) =>
      Promise.resolve(
        scheduleId === 1 && db.openSessionId !== null
          ? { id: db.openSessionId }
          : null,
      ),
    autoCloseExpiredSession: (_scheduleId, _closedAt) => {
      db.autoClosed = true;
      db.sessionOpen = false;
      db.sessionExpiresAt = null;
      return Promise.resolve();
    },
    insertRecord: (row) => {
      const rec = {
        id: db.nextId++,
        status: row.status,
        method: row.method,
        created_at: new Date(db.serverNow.getTime()).toISOString(),
      } as unknown as AttendanceRecordRow;
      if (row.session_id !== null && row.session_id !== undefined) {
        db.sessionRecords.push({
          session_id: row.session_id,
          student_id: row.student_id,
        });
      }
      return Promise.resolve(rec);
    },
  };
}

Deno.test("fase1 1: sesi minggu-2 - student sama di 2 sesi -> ok keduanya", async () => {
  const db = fase1Db(SERVER_PAGI);
  const d = fase1Deps(db);

  db.openSessionId = 10;
  const pertama = await submitAttendanceCore(d, {
    studentId: STUDENT_A,
    scheduleId: 1,
    method: "qr_code",
    qrToken: "token-statis-jadwal-1",
  });
  assert(pertama.ok, `sesi 1 harus ok, dapat: ${JSON.stringify(pertama)}`);
  if (pertama.ok) assertEquals(pertama.status, "HADIR");

  // Minggu kedua: sesi baru dibuka dosen, student yang sama absen lagi.
  db.openSessionId = 11;
  const kedua = await submitAttendanceCore(d, {
    studentId: STUDENT_A,
    scheduleId: 1,
    method: "qr_code",
    qrToken: "token-statis-jadwal-1",
  });
  assert(kedua.ok, `sesi 2 harus ok (bukan 409), dapat: ${JSON.stringify(kedua)}`);
});

Deno.test("fase1 2: 1 detik setelah expiry -> tolak + lazy auto-OFF", async () => {
  const expiry = new Date(Date.UTC(2026, 9, 14, 1, 10, 0));
  const db = fase1Db(new Date(expiry.getTime() + 1000));
  db.sessionExpiresAt = expiry.toISOString();
  const res = await submitAttendanceCore(fase1Deps(db), {
    studentId: STUDENT_A,
    scheduleId: 1,
    method: "qr_code",
    qrToken: "token-statis-jadwal-1",
  });
  assert(!res.ok, "lewat expiry harus gagal");
  if (!res.ok) {
    assertStringIncludes(res.error, "kedaluwarsa");
    assertEquals(res.code, "QR_EXPIRED");
  }
  assert(db.autoClosed, "sesi harus lazy auto-OFF");
  assertEquals(db.sessionOpen, false);
});

Deno.test("fase1 3: qr_token jadwal X untuk schedule_id Y -> 400 mismatch", async () => {
  const db = fase1Db(SERVER_PAGI);
  db.staticTokenScheduleId = 2; // token milik jadwal 2
  const res = await submitAttendanceCore(fase1Deps(db), {
    studentId: STUDENT_A,
    scheduleId: 1,
    method: "qr_code",
    qrToken: "token-statis-jadwal-1",
  });
  assert(!res.ok, "mismatch harus gagal");
  if (!res.ok) assertEquals(res.code, "QR_INVALID");
});

Deno.test("fase1 4: sesi OFF -> tolak kedaluwarsa", async () => {
  const db = fase1Db(SERVER_PAGI);
  db.sessionOpen = false;
  db.sessionExpiresAt = null;
  db.openSessionId = null;
  const res = await submitAttendanceCore(fase1Deps(db), {
    studentId: STUDENT_A,
    scheduleId: 1,
    method: "qr_code",
    qrToken: "token-statis-jadwal-1",
  });
  assert(!res.ok, "sesi OFF harus gagal");
  if (!res.ok) assertEquals(res.code, "QR_EXPIRED");
});
