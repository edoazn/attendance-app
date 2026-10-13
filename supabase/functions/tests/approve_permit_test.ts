// Test approve-permit (fase-2): izin disetujui menjadi baris IZIN.
// Inti di ../_shared/attendance.ts diuji dengan mock (tanpa jaringan).
//
// 1. Approve ok -> baris IZIN + status approved.
// 2. Approve ganda -> 409 + tetap 1 baris.
// 3. Lecturer B atas request jadwal A -> NOT_OWNER.
// 4. Reject -> ok + 0 baris IZIN.
// 5. Request hilang -> NOT_FOUND.
//
// Jalankan: deno test supabase/functions/tests/approve_permit_test.ts

import { assert, assertEquals } from "jsr:@std/assert@1";
import {
  type ApprovePermitDeps,
  approvePermitCore,
  type PermitRequestRow,
} from "../_shared/attendance.ts";

const DOSEN_A = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
const DOSEN_B = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb";
const ADMIN = "9e9e9e9e-9e9e-9e9e-9e9e-9e9e9e9e9e9e";
const MAHASISWA = "11111111-1111-1111-1111-111111111111";

interface MockPermitDb {
  request: PermitRequestRow | null;
  scheduleLecturerId: string | null;
  izinRows: { schedule_id: number; student_id: string }[];
}

function mockDb(): MockPermitDb {
  return {
    request: {
      id: 1,
      schedule_id: 1,
      student_id: MAHASISWA,
      status: "pending",
      alasan: "Sakit",
    },
    scheduleLecturerId: DOSEN_A,
    izinRows: [],
  };
}

function deps(db: MockPermitDb): ApprovePermitDeps {
  return {
    findRequest: (id) =>
      Promise.resolve(db.request && db.request.id === id ? db.request : null),
    findScheduleOwner: (_scheduleId) =>
      Promise.resolve({ lecturer_id: db.scheduleLecturerId }),
    insertIzinRecord: (scheduleId, studentId) => {
      db.izinRows.push({ schedule_id: scheduleId, student_id: studentId });
      return Promise.resolve({ id: db.izinRows.length });
    },
    updateRequestStatus: (_permitId, status) => {
      if (db.request) db.request.status = status;
      return Promise.resolve();
    },
  };
}

Deno.test("permit 1: approve ok -> baris IZIN + status approved", async () => {
  const db = mockDb();
  const res = await approvePermitCore(deps(db), {
    callerId: DOSEN_A,
    isAdmin: false,
    isLecturer: true,
    permitId: 1,
    decision: "approved",
  });
  assert(res.ok, `approve harus ok, dapat: ${JSON.stringify(res)}`);
  if (res.ok) {
    assertEquals(res.status, "approved");
    assertEquals(typeof res.attendance_id, "number");
  }
  assertEquals(db.izinRows.length, 1);
  assertEquals(db.request?.status, "approved");
});

Deno.test("permit 2: approve ganda -> 409 + tetap 1 baris", async () => {
  const db = mockDb();
  const d = deps(db);
  const caller = {
    callerId: ADMIN,
    isAdmin: true,
    isLecturer: false,
    permitId: 1,
    decision: "approved" as const,
  };
  const first = await approvePermitCore(d, caller);
  assert(first.ok, `approve pertama harus ok, dapat: ${JSON.stringify(first)}`);
  const second = await approvePermitCore(d, caller);
  assert(!second.ok, "approve kedua harus gagal");
  if (!second.ok) assertEquals(second.code, "ALREADY_DECIDED");
  assertEquals(db.izinRows.length, 1);
});

Deno.test("permit 3: lecturer B atas request jadwal A -> NOT_OWNER", async () => {
  const res = await approvePermitCore(deps(mockDb()), {
    callerId: DOSEN_B,
    isAdmin: false,
    isLecturer: true,
    permitId: 1,
    decision: "approved",
  });
  assert(!res.ok, "bukan pemilik harus gagal");
  if (!res.ok) assertEquals(res.code, "NOT_OWNER");
});

Deno.test("permit 4: reject -> ok + 0 baris IZIN", async () => {
  const db = mockDb();
  const res = await approvePermitCore(deps(db), {
    callerId: DOSEN_A,
    isAdmin: false,
    isLecturer: true,
    permitId: 1,
    decision: "rejected",
  });
  assert(res.ok, `reject harus ok, dapat: ${JSON.stringify(res)}`);
  if (res.ok) {
    assertEquals(res.status, "rejected");
    assertEquals(res.attendance_id, null);
  }
  assertEquals(db.izinRows.length, 0);
  assertEquals(db.request?.status, "rejected");
});

Deno.test("permit 5: request hilang -> NOT_FOUND", async () => {
  const db = mockDb();
  db.request = null;
  const res = await approvePermitCore(deps(db), {
    callerId: ADMIN,
    isAdmin: true,
    isLecturer: false,
    permitId: 999,
    decision: "approved",
  });
  assert(!res.ok, "request hilang harus gagal");
  if (!res.ok) assertEquals(res.code, "NOT_FOUND");
});
