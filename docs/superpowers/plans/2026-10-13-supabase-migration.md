# Supabase Migration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ganti backend mati `https://meng.my.id/api/v1/` dengan Supabase tanpa mengubah UI/UX mahasiswa yang sudah jadi.

**Architecture:** Postgres + RLS untuk semua read (jadwal, history, profile), Edge Functions Deno untuk 3 operasi tulis kritis (login-nim, generate-session, submit-attendance). Android cuma ganti isi `ApiRepository` jadi `SupabaseRepository`, ViewModel dan Screen tetap.

**Tech Stack:** Supabase (Postgres, Auth, Edge Functions), Kotlin, Retrofit/PostgREST + supabase-kt, CameraX + ML Kit tetap.

**Spec:** `AGENTS.md` + `PROJECT_DOCS.md` + endpoint aktual di `app/src/main/java/com/example/attendaceapp/data/remote/api/ApiService.kt` (login, schedules/today, schedules, attendance)

## Global Constraints

- Keep package typo `com.example.attendaceapp` (`attendace`) di semua path/file baru.
- ViewModels pakai `MutableStateFlow`/`StateFlow` + `copy(...)`, repo return `Result<T>` dengan `.onSuccess`/`.onFailure`.
- Screen baru pakai `LaunchedEffect` untuk load sekali, string UI Bahasa Indonesia.
- `FirebaseRepository` tetap typealias deprecated, jangan tambah Firebase baru.
- minSdk 26, targetSdk 35, dependency baru wajib via `gradle/libs.versions.toml`.
- Auth header Bearer otomatis, token disimpan di `UserPreferences` (atau Supabase session dengan cache nama/NIM yang sama).

## Review Focus

- Double-submit absensi 1 detik berselang dari 2 device → diharapkan 1 sukses 1 ditolak duplicate, bukan 2 hadir.
- Jam HP dimajukan 2 jam untuk hindari terlambat → diharapkan status tetap terlambat karena pakai jam server.
- QR token dipakai ulang setelah `expires_at` lewat → diharapkan ditolak expired.
- NIM enumeration via login error message beda (user-not-found vs wrong-password) → diharapkan pesan generik sama.
- Mahasiswa query `attendance_records` milik NIM lain via PostgREST langsung → diharapkan 0 baris oleh RLS.

---

### Task 1: Postgres Schema + RLS + Seed

**Files:**
- Create: `supabase/migrations/001_attendance_schema.sql`
- Create: `supabase/seed.sql`
- Test: `supabase/tests/rls_test.sql` (pgTAP atau query manual)

**Interfaces:**
- Consumes: nothing (task pertama)
- Produces:
  - `public.profiles(id uuid PK FK auth.users, identity_number text UNIQUE, name text, email text, role text CHECK role IN ('STUDENT','LECTURER'))`
  - `public.schedules(id bigint PK, course_name text, lecturer_name text, room text, day text, start_time time, end_time time, is_active bool DEFAULT true)`
  - `public.enrollments(schedule_id bigint, student_id uuid, PRIMARY KEY(schedule_id, student_id))`
  - `public.qr_tokens(token uuid PK DEFAULT gen_random_uuid(), schedule_id bigint, expires_at timestamptz)`
  - `public.attendance_codes(code char(6) PK, schedule_id bigint, expires_at timestamptz, is_active bool DEFAULT true)`
  - `public.attendance_records(id bigint PK, schedule_id bigint, student_id uuid, method text, status text, distance double precision, created_at timestamptz DEFAULT now(), UNIQUE(schedule_id, student_id))`
  - `public.permit_requests(id bigint PK, schedule_id bigint, student_id uuid, alasan text, created_at timestamptz DEFAULT now())`
  - View `public.today_schedules_with_flags` (join + `has_qr`, `has_active_code` via EXISTS)

- [ ] **Step 1: Write failing check — RLS blocks cross-student read**

```sql
-- sebagai student A, select attendance_records milik student B harus 0 baris
-- SELECT count(*) FROM attendance_records WHERE student_id = '<uuid-B>'; -- expect 0
```

- [ ] **Step 2: Run check, verify it fails (table belum ada / RLS off)**

Run: `supabase db execute -f supabase/tests/rls_test.sql`
Expected: FAIL (relation does not exist)

- [ ] **Step 3: Implement `001_attendance_schema.sql` — CREATE TABLE + ENABLE RLS + 6 policies**

Policies: `profiles select own + lecturer read enrolled`, `schedules select where enrolled or lecturer`, `attendance_records select own + lecturer own schedule, insert own via function only (no direct insert)`, `qr_tokens/attendance_codes select where enrolled, no direct insert`, `permit_requests insert own + select own`.

- [ ] **Step 4: Run migration + seed + RLS test, verify PASS**

Run: `supabase migration up; supabase db execute -f supabase/seed.sql; supabase db execute -f supabase/tests/rls_test.sql`
Expected: PASS, `today_schedules_with_flags` return baris seed hari ini.

- [ ] **Step 5: Commit**

```bash
git add supabase/migrations/001_attendance_schema.sql supabase/seed.sql supabase/tests/rls_test.sql
git commit -m "feat(supabase): schema, RLS, dan seed absensi"
```

### Task 2: Edge Functions (login-nim, generate-session, submit-attendance)

**Files:**
- Create: `supabase/functions/login-with-nim/index.ts`
- Create: `supabase/functions/generate-session/index.ts`
- Create: `supabase/functions/submit-attendance/index.ts`
- Test: `supabase/functions/tests/submit_attendance_test.ts` (Deno test, mock supabase client)

**Interfaces:**
- Consumes: tables dari Task 1
- Produces:
  - `POST login-with-nim(body: {nim: string, password: string}) -> {token: string, user: {id, identity_number, name, email, role}} | {error: string generik}`
  - `POST generate-session(body: {schedule_id: number, duration_minutes: number}, auth: lecturer JWT) -> {qr_token: string, attendance_code: string, expires_at: string}`
  - `POST submit-attendance(body: {schedule_id: number, method: 'geolocation'|'qr_code'|'attendance_code', latitude?: number, longitude?: number, qr_token?: string, attendance_code?: string}, auth: student JWT) -> {status: string, method: string, attendance: {id, status, method, created_at}} | {error: string}`

- [ ] **Step 1: Write failing tests — duplicate, expired QR, clock skew, generik login error**

```ts
// submit_attendance_test.ts
// 1st call expect ok, 2nd call same schedule+student expect error contains "sudah"
// expired qr_token expect error contains "kedaluwarsa"
// client clock maju 2 jam tetap tentukan status dari server now(), bukan dari payload
// login-with-nim NIM salah vs password salah expect pesan error identik generik
```

- [ ] **Step 2: Run test, verify FAIL**

Run: `deno test supabase/functions/tests/submit_attendance_test.ts`
Expected: FAIL (function not implemented)

- [ ] **Step 3: Implement `submit-attendance/index.ts`**

Logic: verify JWT → ambil student_id → server `now()` → cek schedule `is_active` → cek duplicate UNIQUE → branch method (geolocation: Haversine vs lat/long kampus + radius, qr_code: cek `qr_tokens` expiry, attendance_code: upper + cek `is_active` + expiry) → tentukan `hadir/terlambat` dari `now() vs start_time + lateThreshold` → insert → return. Semua error pesan generik Indonesia.

- [ ] **Step 4: Implement `login-with-nim` + `generate-session` (lookup NIM→email→signIn, lecturer-only guard + insert qr_tokens/attendance_codes)**

- [ ] **Step 5: Run tests + deploy dry-run, verify PASS**

Run: `deno test supabase/functions/tests/; supabase functions deploy --dry-run`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add supabase/functions/
git commit -m "feat(supabase): edge functions login, generate, submit"
```

### Task 3: Android — Ganti ApiRepository dengan SupabaseRepository

**Files:**
- Modify: `gradle/libs.versions.toml` (tambah supabase-kt atau okhttp PostgREST, jangan hardcode versi di build.gradle)
- Create: `app/src/main/java/com/example/attendaceapp/data/repository/SupabaseRepository.kt`
- Modify: `app/src/main/java/com/example/attendaceapp/ui/screens/auth/AuthViewModel.kt:26`
- Modify: `app/src/main/java/com/example/attendaceapp/ui/screens/student/StudentViewModel.kt:17`
- Modify: `app/src/main/java/com/example/attendaceapp/ui/screens/schedule/ScheduleViewModel.kt:27`
- Test: `app/src/test/java/com/example/attendaceapp/SupabaseRepositoryTest.kt` (JUnit + MockWebServer, tanpa perlu backend asli)

**Interfaces:**
- Consumes: Edge Functions dari Task 2 (URL + body persis)
- Produces:
  - `class SupabaseRepository(context: Context?) { suspend fun login(nim: String, password: String): Result<User>; suspend fun getTodaySchedules(): Result<List<ScheduleDto>>; suspend fun getAllSchedules(): Result<List<ScheduleDto>>; suspend fun submitAttendanceQr(scheduleId: Int, qrToken: String): Result<AttendanceData>; suspend fun submitAttendanceCode(scheduleId: Int, code: String): Result<AttendanceData>; fun clearSession() }`
  - Signature identik `ApiRepository` agar ViewModel cuma ganti konstruktor.

- [ ] **Step 1: Write failing unit test — login sukses simpan token**

```kotlin
@Test fun `login sukses simpan token dan return User`() {
  // mock 200 {success:true, data:{token:"t", user:{...}}}
  // assert repository.login("2114","pw").isSuccess && prefs.authToken == "t"
}
```

- [ ] **Step 2: Run test, verify FAIL**

Run: `.\gradlew.bat testDebugUnitTest --tests "*SupabaseRepositoryTest*"`
Expected: FAIL (class not found)

- [ ] **Step 3: Implement `SupabaseRepository.kt` — PostgREST untuk GET schedules + Edge Function untuk login/submit, baca token dari `UserPreferences`, mapping `ScheduleDto/AttendanceData` yang sudah ada (jangan bikin DTO baru)**

- [ ] **Step 4: Ganti konstruktor repo di 3 ViewModel + run tests PASS**

Run: `.\gradlew.bat testDebugUnitTest --tests "*SupabaseRepositoryTest*"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add gradle/libs.versions.toml app/src/main/java/com/example/attendaceapp/data/repository/SupabaseRepository.kt app/src/test/java/com/example/attendaceapp/SupabaseRepositoryTest.kt
git commit -m "feat(android): ganti ApiRepository ke SupabaseRepository"
```

### Task 4: Sambungkan History + Profile (hapus dummy)

**Files:**
- Modify: `app/src/main/java/com/example/attendaceapp/ui/screens/history/HistoryPage.kt:28`
- Modify: `app/src/main/java/com/example/attendaceapp/ui/screens/profile/ProfilePage.kt:44`
- Modify: `app/src/main/java/com/example/attendaceapp/data/repository/SupabaseRepository.kt` (tambah `getHistory(): Result<List<AttendanceHistory>>`)
- Test: `app/src/test/java/com/example/attendaceapp/HistoryProfileTest.kt`

**Interfaces:**
- Consumes: `SupabaseRepository` dari Task 3
- Produces: `HistoryPage(viewModel)` load via `LaunchedEffect`, `ProfilePage` baca `authViewModel.currentUser` (nama + NIM asli, bukan hardcode)

- [ ] **Step 1: Write failing test — history kosong → empty state, bukan crash**

```kotlin
@Test fun `history kosong tampilkan empty state`() { /* viewmodel return emptyList, assert uiState.todaySchedules.isEmpty() */ }
```

- [ ] **Step 2: Run test, verify FAIL**

Run: `.\gradlew.bat testDebugUnitTest --tests "*HistoryProfileTest*"`
Expected: FAIL

- [ ] **Step 3: Implement `getHistory()` (query `attendance_records` + join `schedules`) + refactor `HistoryPage`/`ProfilePage` hapus `remember dummy` dan string hardcode, pakai `LaunchedEffect` + `collectAsState`**

- [ ] **Step 4: Run tests + `assembleDebug`, verify PASS**

Run: `.\gradlew.bat testDebugUnitTest; .\gradlew.bat assembleDebug`
Expected: PASS, APK ter-build.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/attendaceapp/ui/screens/history/HistoryPage.kt app/src/main/java/com/example/attendaceapp/ui/screens/profile/ProfilePage.kt
git commit -m "feat(android): history dan profile dari Supabase"
```
