# Dashboard Fase 2 Backend Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Siapkan sisi backend untuk dashboard Fase 2 (peran ADMIN, kolom rombel, antrean approve izin, pembuatan akun, view rekap) tanpa mengubah kontrak Android yang sudah rilis.

**Architecture:** Satu migrasi Postgres (004: CHECK role + `is_admin()` + kolom rombel + policy admin/lecturer + view `attendance_summary`) lalu 2 Edge Function baru (approve-izin, create-user) + 1 script setup admin. Dashboard web (repo terpisah) mengonsumsi semuanya — Plan B menyusul di repo itu.

**Tech Stack:** Supabase (Postgres, RLS, Edge Functions Deno), Deno test + mock, `npx supabase db push --linked` ke project dev.

**Spec:** `docs/superpowers/specs/2026-10-13-dashboard-fase2-design.md` (putusan 8–14) + `docs/superpowers/specs/2026-10-13-lecturer-attendance-design.md` (kontrak Fase 1 yang dipertahankan)

## Global Constraints

- Pesan error ke client Bahasa Indonesia, generik (tidak membocorkan eksistensi user/NIM/jadwal).
- Tidak ada emoji di string user-facing manapun.
- Penentuan status/waktu dalam zona `Asia/Jakarta` eksplisit.
- Tulis kritis tetap via Edge Function (service role); dashboard tidak menulis `attendance_*` langsung. Tulis master oleh admin via RLS (user JWT) diizinkan.
- Submit yang diterima via `submit-attendance` tetap `HADIR`-only; status `IZIN` hanya lahir dari function approve-izin.
- Kontrak endpoint Fase 1 (`ensure-session-qr`, `set-session-state`, `submit-attendance`, `login-with-nim`, `generate-session`) tidak berubah.
- Migrasi memakai `IF NOT EXISTS` / guard kondisional agar jalan di project mana pun; JANGAN apply ke production tanpa konfirmasi.

## Review Focus

- Approve izin yang sama diklik 2x bersamaan → diharapkan 1 baris IZIN, bukan 2 (idempotensi). (Test: Task 2, double-approve.)
- Lecturer approve izin untuk jadwal milik lecturer lain → diharapkan 403. (Test: Task 2, guard pemilik.)
- Create-user dengan NIM yang sudah ada → diharapkan 409 generik, bukan pesan yang membedakan "NIM dipakai" vs error lain. (Test: Task 3, NIM duplikat.)
- Admin melihat semua jadwal, lecturer tetap own+pool. (Test: Task 1, RLS check admin.)
- Mahasiswa enrolled tanpa satu pun absensi → diharapkan muncul di rekap dengan 0 (bukan hilang). (Test: Task 1, view check.)

---

### Task 1: Migrasi 004 (ADMIN + rombel + policy + view rekap)

**Files:**
- Create: `supabase/migrations/004_admin_dashboard.sql`
- Modify: `supabase/seed.sql` (rombel seed + profil admin dev)
- Modify: `supabase/tests/rls_test.sql` (append checks 14–16 sebelum `SELECT 'RLS_TEST_PASS'`)
- Test: `supabase/tests/rls_test.sql` via linked DB

**Interfaces:**
- Consumes: skema 001–003 (`profiles`, `schedules`, `enrollments`, `attendance_records`, `attendance_sessions`, `permit_requests`).
- Produces:
  - `profiles.role CHECK (role IN ('STUDENT','LECTURER','ADMIN'))` (drop constraint lama, buat ulang melonggar).
  - `profiles.rombel text NULL`.
  - `public.is_admin()` (SECURITY DEFINER, pola `is_lecturer()` di 001).
  - Policy: `schedules` select untuk admin (semua); `schedules` insert/update/delete untuk admin; `enrollments` full untuk admin; `profiles` select+update untuk admin; `permit_requests` select untuk admin + lecturer pemilik jadwal (via join schedules).
  - View `public.today_schedules_with_flags` tetap; view baru `public.attendance_summary(security_invoker=true)`: satu baris per (schedule, enrolled student) berisi `total_sesi` (count sesi schedule), `hadir`, `izin`, `persen` (100*hadir/total_sesi, 0 bila total 0). Baris legacy `session_id NULL` dihitung 1 hadir + 1 penyebut.

- [ ] **Step 1: Write failing checks — CHECK ADMIN + rombel + view + policy admin**

```sql
-- check 14: profiles.role menerima 'ADMIN' (coba INSERT profil ADMIN uji -> sukses, lalu hapus)
-- check 15: kolom profiles.rombel ada + view attendance_summary ada + security_invoker=true
-- check 16: policy schedules_admin_all + enrollments_admin_all + profiles_admin_manage ada di katalog
```

- [ ] **Step 2: Run checks, verify FAIL (role ditolak / kolom-view-policy belum ada)**

Run: `npx supabase db query --linked -f supabase/tests/rls_test.sql`
Expected: FAIL (`RLS_TEST_FAIL`, salah satu check 14–16)

- [ ] **Step 3: Implement `004_admin_dashboard.sql` (DDL + helper + policy + view + grants)**

Grants: `GRANT SELECT, INSERT, UPDATE, DELETE ON schedules, enrollments TO admin`? — Tidak: RLS berbasis role, bukan role DB. Beri `GRANT SELECT, INSERT, UPDATE, DELETE ON public.schedules, public.enrollments TO authenticated` (policy yang membatasi), `GRANT SELECT, UPDATE ON public.profiles TO authenticated`, `GRANT SELECT ON public.attendance_summary TO authenticated`.

- [ ] **Step 4: Update `seed.sql` (rombel `TI-2A` untuk 2 mahasiswa + profil ADMIN dev) lalu push + seed + test, verify PASS**

Run: `npx supabase db push --linked; npx supabase db query --linked -f supabase/seed.sql; npx supabase db query --linked -f supabase/tests/rls_test.sql`
Expected: PASS (`RLS_TEST_PASS`; profil admin butuh `auth.users` dulu seperti pola seed — bila FK gagal, seed admin di-skip dengan NOTICE dan check 14 memakai akun uji temporer yang self-cleaning seperti pola check 12)

- [ ] **Step 5: Commit**

```bash
git add supabase/migrations/004_admin_dashboard.sql supabase/seed.sql supabase/tests/rls_test.sql
git commit -m "feat(supabase): peran ADMIN, rombel, view rekap"
```

### Task 2: Function approve-permit (izin → baris IZIN)

**Files:**
- Modify: `supabase/functions/_shared/attendance.ts` (tambah `approvePermitCore` + tipe)
- Create: `supabase/functions/approve-permit/index.ts`
- Test: `supabase/functions/tests/approve_permit_test.ts`

**Interfaces:**
- Consumes: `permit_requests.status` + policy select Task 1; pola core-vs-wiring Task 2/3 Fase 1.
- Produces: `POST approve-permit(body: {permit_id: number, decision: 'approved'|'rejected'}, auth: admin ATAU lecturer pemilik) -> {status: 'approved'|'rejected', attendance_id: number|null} | {error}`. Approved → update request + insert `attendance_records(status='IZIN', method='izin', session_id=NULL)`; rejected → update request saja. Request non-pending → 409 generik (idempoten: approve ganda tidak bikin baris kedua).

- [ ] **Step 1: Write failing tests — double-approve, guard pemilik, reject tanpa baris**

```ts
// approve_permit_test.ts (mock deps, pola set_session_state_test.ts)
// approve ok expect attendance row IZIN; approve lagi expect 409 + tetap 1 baris
// lecturer B atas request jadwal A expect NOT_OWNER; reject expect ok + 0 baris
// request hilang expect NOT_FOUND
```

- [ ] **Step 2: Run test, verify FAIL**

Run: `deno test supabase/functions/tests/approve_permit_test.ts`
Expected: FAIL (function not implemented)

- [ ] **Step 3: Implement core (`approvePermitCore`) + `approve-permit/index.ts` (service role, pesan generik)**

Error codes: `NOT_LECTURER_OR_ADMIN` → 403 `ERR_DOSEN_SAJA`; `NOT_FOUND` → 404 `ERR_JADWAL_TIDAK_AKTIF`; `NOT_OWNER` → 403; `ALREADY_DECIDED` → 409 pesan `"Pengajuan sudah diproses."`; decision tak valid → 400 `ERR_SESI_TIDAK_LENGKAP`.

- [ ] **Step 4: Run tests + check/lint, verify PASS**

Run: `deno test supabase/functions/tests/approve_permit_test.ts; deno check supabase/functions/approve-permit/index.ts; deno lint supabase/functions/approve-permit/`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add supabase/functions/approve-permit/ supabase/functions/tests/approve_permit_test.ts supabase/functions/_shared/attendance.ts
git commit -m "feat(supabase): approve-permit izin menjadi baris IZIN"
```

### Task 3: Function create-user (admin bikin akun)

**Files:**
- Modify: `supabase/functions/_shared/attendance.ts` (tambah `validateCreateUserInput` murni)
- Create: `supabase/functions/create-user/index.ts`
- Test: `supabase/functions/tests/create_user_test.ts`

**Interfaces:**
- Consumes: `is_admin()` Task 1; pola Auth admin API (`admin.auth.admin.createUser`).
- Produces: `POST create-user(body: {nim: string, name: string, password: string, role: 'STUDENT'|'LECTURER'|'ADMIN', email?: string, rombel?: string}, auth: ADMIN JWT) -> {user: {id, identity_number, name, role}} | {error}`. Non-admin → 403. NIM duplikat → 409 generik `"Data pengguna tidak dapat dibuat."` (tidak membedakan sebab). Email default `{nim}@kampus.ac.id` bila kosong (kebutuhan `login-with-nim`).

- [ ] **Step 1: Write failing tests — validasi NIM/nama/password/role + NIM duplikat generik**

```ts
// create_user_test.ts
// nim kosong/spasi expect INVALID; password < 6 expect INVALID; role 'X' expect INVALID
// duplikat (mock createUser 422/duplicate) expect pesan generik tanpa kata 'sudah ada'
```

- [ ] **Step 2: Run test, verify FAIL**

Run: `deno test supabase/functions/tests/create_user_test.ts`
Expected: FAIL (function not implemented)

- [ ] **Step 3: Implement validator + `create-user/index.ts` (guard admin via `profiles.role`, buat auth user dulu lalu profil; gagal profil → hapus auth user (rollback), tetap pesan generik)**

- [ ] **Step 4: Run tests + check/lint, verify PASS**

Run: `deno test supabase/functions/tests/create_user_test.ts; deno check supabase/functions/create-user/index.ts; deno lint supabase/functions/create-user/`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add supabase/functions/create-user/ supabase/functions/tests/create_user_test.ts supabase/functions/_shared/attendance.ts
git commit -m "feat(supabase): create-user oleh admin"
```

### Task 4: Script setup admin pertama

**Files:**
- Create: `supabase/scripts/setup-admin.ts`
- Test: verifikasi live linked (ceklist manual di langkah, bukan Deno test — script interaktif sekali jalan)

**Interfaces:**
- Consumes: `create-user` Task 3 (dipanggil sebagai admin? — tidak: script memakai SERVICE ROLE langsung, bukan JWT; function create-user butuh JWT admin yang belum ada = deadlock bootstrap, jadi script menulis via Auth Admin API + `profiles` langsung).
- Produces: `npx supabase db query` no — `deno run -A supabase/scripts/setup-admin.ts` meminta NIM/nama/password interaktif, membuat `auth.users` + `profiles(role='ADMIN')`, mencetak verifikasi login.

- [ ] **Step 1: Write script dengan guard — tolak bila ADMIN sudah ada (idempoten, bukan duplikat)**

```ts
// setup-admin.ts: prompt nim/nama/password (min 6); cek profiles role=ADMIN sudah ada -> info + exit 0 tanpa menulis; tulis auth + profil; catch -> pesan generik Indonesia
```

- [ ] **Step 2: Dry-run tanpa menulis, verify FAIL-safe (jalankan dengan env kosong → exit non-0 + pesan "lingkungan belum dikonfigurasi", tanpa menulis apa pun)**

Run: `deno run -A supabase/scripts/setup-admin.ts` (tanpa `SUPABASE_URL`/`SERVICE_ROLE_KEY`)
Expected: FAIL cepat dengan pesan konfigurasi (bukan prompt, bukan tulis)

- [ ] **Step 3: Implement tulis via Auth Admin API + insert profil (pesan sukses: NIM admin + langkah login dashboard)**

- [ ] **Step 4: Verifikasi live — buat ADMIN uji di project dev, login via `login-with-nim`, hapus kembali, verify PASS**

Run: `deno run -A supabase/scripts/setup-admin.ts` (input ADMIN uji) `; npx supabase db query --linked "delete from profiles where identity_number='<nim-uji>'; delete from auth.users where ..."`
Expected: PASS (script exit 0, login 200, bersih kembali)

- [ ] **Step 5: Commit**

```bash
git add supabase/scripts/setup-admin.ts
git commit -m "feat(supabase): script setup admin pertama"
```
