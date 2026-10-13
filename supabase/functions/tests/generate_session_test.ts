// Test guard jadwal aktif generate-session (F4).
// Jadwal arsip (is_active=false) atau hilang (null) tidak boleh mendapat
// qr_token/code baru -> handler balikan 404 ERR_JADWAL_TIDAK_AKTIF.
// Predikat murni isSchedulableForNewSession() diuji langsung (tanpa
// jaringan); handler generate-session/index.ts memakainya di step 3.
//
// Jalankan: deno test supabase/functions/tests/

import { assertEquals } from "jsr:@std/assert@1";
import { isSchedulableForNewSession } from "../_shared/attendance.ts";

Deno.test("generate-session guard: jadwal hilang (null) tidak schedulable", () => {
  assertEquals(isSchedulableForNewSession(null), false);
});

Deno.test("generate-session guard: jadwal arsip (is_active=false) ditolak", () => {
  // Seed id=3 'Jaringan Komputer (arsip)' adalah contoh jadwal arsip.
  assertEquals(isSchedulableForNewSession({ is_active: false }), false);
});

Deno.test("generate-session guard: jadwal aktif (is_active=true) lolos", () => {
  assertEquals(isSchedulableForNewSession({ is_active: true }), true);
});
