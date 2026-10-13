// Test create-user (fase-2): validasi murni + pesan duplikat generik.
// Validator di ../_shared/attendance.ts diuji langsung (tanpa jaringan),
// pola loginErrorMessage di submit_attendance_test.ts.
//
// 1. NIM kosong -> INVALID_NIM.
// 2. Password < 6 -> INVALID_PASSWORD.
// 3. Role di luar STUDENT/LECTURER/ADMIN -> INVALID_ROLE.
// 4. Input minimal valid -> ok + email default {nim}@kampus.ac.id.
// 5. Pesan duplikat generik (tanpa bocoran "sudah ada"/"terdaftar").
//
// Jalankan: deno test supabase/functions/tests/create_user_test.ts

import { assert, assertEquals } from "jsr:@std/assert@1";
import {
  duplicateUserErrorMessage,
  ERR_USER_CREATE,
  validateCreateUserInput,
} from "../_shared/attendance.ts";

Deno.test("create-user 1: NIM kosong -> INVALID_NIM", () => {
  const res = validateCreateUserInput({
    nim: "   ",
    name: "Mahasiswa Baru",
    password: "rahasia1",
    role: "STUDENT",
  });
  assert(!res.ok, "NIM kosong harus gagal");
  if (!res.ok) assertEquals(res.code, "INVALID_NIM");
});

Deno.test("create-user 2: password pendek -> INVALID_PASSWORD", () => {
  const res = validateCreateUserInput({
    nim: "20240010",
    name: "Mahasiswa Baru",
    password: "12345",
    role: "STUDENT",
  });
  assert(!res.ok, "password < 6 harus gagal");
  if (!res.ok) assertEquals(res.code, "INVALID_PASSWORD");
});

Deno.test("create-user 3: role asing -> INVALID_ROLE", () => {
  const res = validateCreateUserInput({
    nim: "20240010",
    name: "Mahasiswa Baru",
    password: "rahasia1",
    role: "OPERATOR",
  });
  assert(!res.ok, "role asing harus gagal");
  if (!res.ok) assertEquals(res.code, "INVALID_ROLE");
});

Deno.test("create-user 4: input minimal valid -> ok + email default", () => {
  const res = validateCreateUserInput({
    nim: "20240010",
    name: "Mahasiswa Baru",
    password: "rahasia1",
    role: "STUDENT",
  });
  assert(res.ok, `harus ok, dapat: ${JSON.stringify(res)}`);
  if (res.ok) {
    assertEquals(res.nim, "20240010");
    assertEquals(res.email, "20240010@kampus.ac.id");
    assertEquals(res.role, "STUDENT");
  }
});

Deno.test("create-user 5: pesan duplikat generik anti-enumerasi", () => {
  const msg = duplicateUserErrorMessage();
  assertEquals(msg, ERR_USER_CREATE);
  assert(!msg.toLowerCase().includes("sudah ada"));
  assert(!msg.toLowerCase().includes("terdaftar"));
  assert(!msg.toLowerCase().includes("duplikat"));
});
