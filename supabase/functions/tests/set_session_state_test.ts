// Test set-session-state (fase-1): ON/OFF manual dosen.
// Inti di ../_shared/attendance.ts diuji dengan mock (tanpa jaringan).
//
// 1. ON -> session_open, 1 baris sesi, expires = now + default 10 mnt.
// 2. ON lalu ON lagi -> tetap 1 baris sesi + expires_at mundur.
// 3. Lecturer B atas jadwal A -> NOT_OWNER.
// 4. OFF saat OFF -> ok no-op.
// 5. OFF saat ON -> sesi ditutup, codes off.
// 6. duration 0 / 999 -> INVALID_DURATION.
// 7. Jadwal arsip -> SCHEDULE_NOT_FOUND.
//
// Jalankan: deno test supabase/functions/tests/set_session_state_test.ts

import { assert, assertEquals } from "jsr:@std/assert@1";
import {
  type SessionScheduleRow,
  type SetSessionDeps,
  setSessionStateCore,
} from "../_shared/attendance.ts";

const DOSEN_A = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
const DOSEN_B = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb";
// 2026-10-14 01:00 UTC == 08:00 WIB.
const NOW = new Date(Date.UTC(2026, 9, 14, 1, 0, 0));

interface MockSession {
  id: number;
  schedule_id: number;
  opened_at: string;
  closed_at: string | null;
}

interface MockDb {
  schedule: SessionScheduleRow;
  sessions: MockSession[];
  codes: { code: string; is_active: boolean }[];
  qrExpiresAt: string | null;
  nextSessionId: number;
}

function mockDb(): MockDb {
  return {
    schedule: {
      id: 1,
      is_active: true,
      lecturer_id: DOSEN_A,
      session_open: false,
      session_expires_at: null,
    },
    sessions: [],
    codes: [],
    qrExpiresAt: null,
    nextSessionId: 1,
  };
}

function deps(db: MockDb): SetSessionDeps {
  return {
    now: () => new Date(NOW.getTime()),
    findSchedule: (id) =>
      Promise.resolve(db.schedule.id === id ? { ...db.schedule } : null),
    findOpenSession: (scheduleId) =>
      Promise.resolve(
        db.sessions.find((s) =>
          s.schedule_id === scheduleId && s.closed_at === null
        ) ?? null,
      ),
    createSession: (scheduleId) => {
      const row = {
        id: db.nextSessionId++,
        schedule_id: scheduleId,
        opened_at: NOW.toISOString(),
        closed_at: null,
      };
      db.sessions.push(row);
      return Promise.resolve({ ...row });
    },
    closeOpenSessions: (scheduleId, closedAt) => {
      let n = 0;
      for (const s of db.sessions) {
        if (s.schedule_id === scheduleId && s.closed_at === null) {
          s.closed_at = closedAt;
          n++;
        }
      }
      return Promise.resolve(n);
    },
    updateSchedule: (scheduleId, patch) => {
      if (patch.session_open !== undefined) {
        db.schedule.session_open = patch.session_open;
      }
      db.schedule.session_expires_at = patch.session_expires_at ?? null;
      void scheduleId;
      return Promise.resolve();
    },
    setCodesActive: (scheduleId, active) => {
      void scheduleId;
      for (const c of db.codes) c.is_active = active;
      return Promise.resolve();
    },
    ensurePermanentCode: (scheduleId, code) => {
      void scheduleId;
      if (db.codes.length === 0) db.codes.push({ code, is_active: true });
      return Promise.resolve(db.codes[0].code);
    },
    refreshQrExpiry: (_scheduleId, expiresAt) => {
      db.qrExpiresAt = expiresAt;
      return Promise.resolve();
    },
  };
}

const onA = {
  callerId: DOSEN_A,
  isLecturer: true,
  scheduleId: 1,
  state: "on" as const,
};

Deno.test("set-state 1: ON -> 1 sesi + expires now+10 mnt + codes on", async () => {
  const db = mockDb();
  const res = await setSessionStateCore(deps(db), onA);
  assert(res.ok, `ON harus ok, dapat: ${JSON.stringify(res)}`);
  if (res.ok) {
    assertEquals(res.state, "on");
    assertEquals(db.sessions.length, 1);
    assertEquals(
      res.expires_at,
      new Date(NOW.getTime() + 10 * 60_000).toISOString(),
    );
    assertEquals(db.schedule.session_open, true);
    assertEquals(db.qrExpiresAt, res.expires_at);
    assertEquals(db.codes.length, 1);
    assertEquals(db.codes[0].is_active, true);
  }
});

Deno.test("set-state 2: ON ganda -> tetap 1 sesi + expires mundur", async () => {
  const db = mockDb();
  const d = deps(db);
  const first = await setSessionStateCore(d, onA);
  assert(first.ok && first.session_id !== null);
  const second = await setSessionStateCore(d, {
    ...onA,
    durationMinutes: 30,
  });
  assert(second.ok, `ON kedua harus ok, dapat: ${JSON.stringify(second)}`);
  if (first.ok && second.ok) {
    assertEquals(db.sessions.length, 1);
    assertEquals(second.session_id, first.session_id);
    assert(
      new Date(second.expires_at!).getTime() >
        new Date(first.expires_at!).getTime(),
      "expires kedua harus lebih mundur",
    );
  }
});

Deno.test("set-state 3: lecturer B atas jadwal A -> NOT_OWNER", async () => {
  const res = await setSessionStateCore(deps(mockDb()), {
    ...onA,
    callerId: DOSEN_B,
  });
  assert(!res.ok, "bukan pemilik harus gagal");
  if (!res.ok) assertEquals(res.code, "NOT_OWNER");
});

Deno.test("set-state 4: OFF saat OFF -> ok no-op", async () => {
  const db = mockDb();
  const res = await setSessionStateCore(deps(db), {
    callerId: DOSEN_A,
    isLecturer: true,
    scheduleId: 1,
    state: "off",
  });
  assert(res.ok, `OFF no-op harus ok, dapat: ${JSON.stringify(res)}`);
  if (res.ok) {
    assertEquals(res.state, "off");
    assertEquals(db.sessions.length, 0);
  }
});

Deno.test("set-state 5: OFF saat ON -> sesi ditutup + codes off", async () => {
  const db = mockDb();
  const d = deps(db);
  await setSessionStateCore(d, onA);
  const res = await setSessionStateCore(d, {
    callerId: DOSEN_A,
    isLecturer: true,
    scheduleId: 1,
    state: "off",
  });
  assert(res.ok, `OFF harus ok, dapat: ${JSON.stringify(res)}`);
  if (res.ok) {
    assertEquals(res.state, "off");
    assertEquals(db.schedule.session_open, false);
    assertEquals(db.sessions[0].closed_at, NOW.toISOString());
    assertEquals(db.codes[0].is_active, false);
  }
});

Deno.test("set-state 6: duration 0 / 999 -> INVALID_DURATION", async () => {
  for (const durationMinutes of [0, 999]) {
    const res = await setSessionStateCore(deps(mockDb()), {
      ...onA,
      durationMinutes,
    });
    assert(!res.ok, `durasi ${durationMinutes} harus gagal`);
    if (!res.ok) assertEquals(res.code, "INVALID_DURATION");
  }
});

Deno.test("set-state 7: jadwal arsip -> SCHEDULE_NOT_FOUND", async () => {
  const db = mockDb();
  db.schedule.is_active = false;
  const res = await setSessionStateCore(deps(db), onA);
  assert(!res.ok, "jadwal arsip harus gagal");
  if (!res.ok) assertEquals(res.code, "SCHEDULE_NOT_FOUND");
});
