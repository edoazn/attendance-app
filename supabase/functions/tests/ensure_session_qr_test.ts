// Test ensure-session-qr (fase-1): token QR statis idempoten per jadwal.
// Pola sama seperti submit_attendance_test.ts: inti di
// ../_shared/attendance.ts diuji dengan mock (tanpa jaringan).
//
// 1. Panggil 2x -> token SAMA, created true lalu false.
// 2. Lecturer bukan pemilik -> NOT_OWNER (403 di handler).
// 3. Bukan lecturer -> NOT_LECTURER (403 di handler).
// 4. Jadwal arsip / hilang -> SCHEDULE_NOT_FOUND (404 di handler).
// 5. Pool unassigned (lecturer_id NULL) -> lecturer mana pun boleh.
//
// Jalankan: deno test supabase/functions/tests/ensure_session_qr_test.ts

import { assert, assertEquals } from "jsr:@std/assert@1";
import {
  type EnsureQrDeps,
  ensureSessionQrCore,
  type ScheduleOwnershipRow,
} from "../_shared/attendance.ts";

const DOSEN_A = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
const DOSEN_B = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb";

interface MockQrDb {
  schedule: ScheduleOwnershipRow | null;
  tokens: Map<number, string>;
}

function mockDb(schedule: ScheduleOwnershipRow | null): MockQrDb {
  return { schedule, tokens: new Map() };
}

function deps(db: MockQrDb, newToken = () => "token-baru-1"): EnsureQrDeps {
  return {
    newToken,
    findSchedule: (id) =>
      Promise.resolve(
        db.schedule && db.schedule.id === id ? db.schedule : null,
      ),
    findQrTokenBySchedule: (scheduleId) =>
      Promise.resolve(db.tokens.get(scheduleId) ?? null),
    insertQrToken: (scheduleId, token) => {
      if (db.tokens.has(scheduleId)) {
        return Promise.reject({ code: "23505" });
      }
      db.tokens.set(scheduleId, token);
      return Promise.resolve();
    },
  };
}

function jadwalMilikA(): ScheduleOwnershipRow {
  return { id: 1, is_active: true, lecturer_id: DOSEN_A };
}

Deno.test("ensure-qr 1: 2x panggil -> token sama, created true lalu false", async () => {
  const db = mockDb(jadwalMilikA());
  const d = deps(db);

  const first = await ensureSessionQrCore(d, {
    callerId: DOSEN_A,
    isLecturer: true,
    scheduleId: 1,
  });
  assert(first.ok, `panggilan pertama harus ok, dapat: ${JSON.stringify(first)}`);
  if (first.ok) {
    assertEquals(first.created, true);
    assertEquals(first.qr_token, "token-baru-1");
  }

  const second = await ensureSessionQrCore(d, {
    callerId: DOSEN_A,
    isLecturer: true,
    scheduleId: 1,
  });
  assert(second.ok, `panggilan kedua harus ok, dapat: ${JSON.stringify(second)}`);
  if (second.ok) {
    assertEquals(second.created, false);
    assertEquals(second.qr_token, "token-baru-1");
  }
});

Deno.test("ensure-qr 2: lecturer bukan pemilik -> NOT_OWNER", async () => {
  const res = await ensureSessionQrCore(deps(mockDb(jadwalMilikA())), {
    callerId: DOSEN_B,
    isLecturer: true,
    scheduleId: 1,
  });
  assert(!res.ok, "bukan pemilik harus gagal");
  if (!res.ok) assertEquals(res.code, "NOT_OWNER");
});

Deno.test("ensure-qr 3: bukan lecturer -> NOT_LECTURER", async () => {
  const res = await ensureSessionQrCore(deps(mockDb(jadwalMilikA())), {
    callerId: "11111111-1111-1111-1111-111111111111",
    isLecturer: false,
    scheduleId: 1,
  });
  assert(!res.ok, "bukan lecturer harus gagal");
  if (!res.ok) assertEquals(res.code, "NOT_LECTURER");
});

Deno.test("ensure-qr 4: jadwal arsip / hilang -> SCHEDULE_NOT_FOUND", async () => {
  const arsip = await ensureSessionQrCore(
    deps(mockDb({ id: 3, is_active: false, lecturer_id: DOSEN_A })),
    { callerId: DOSEN_A, isLecturer: true, scheduleId: 3 },
  );
  assert(!arsip.ok, "jadwal arsip harus gagal");
  if (!arsip.ok) assertEquals(arsip.code, "SCHEDULE_NOT_FOUND");

  const hilang = await ensureSessionQrCore(deps(mockDb(null)), {
    callerId: DOSEN_A,
    isLecturer: true,
    scheduleId: 999,
  });
  assert(!hilang.ok, "jadwal hilang harus gagal");
  if (!hilang.ok) assertEquals(hilang.code, "SCHEDULE_NOT_FOUND");
});

Deno.test("ensure-qr 5: pool unassigned (lecturer_id NULL) -> lecturer mana pun boleh", async () => {
  const db = mockDb({ id: 2, is_active: true, lecturer_id: null });
  const res = await ensureSessionQrCore(deps(db), {
    callerId: DOSEN_B,
    isLecturer: true,
    scheduleId: 2,
  });
  assert(res.ok, `pool harus boleh, dapat: ${JSON.stringify(res)}`);
});
