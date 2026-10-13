-- RLS test Task 1 (query manual, tanpa pgTAP agar jalan via db execute).
-- Pra-migrasi: GAGAL dengan "relation ... does not exist" (sesuai Step 2 brief).
-- Pasca migrasi+seed: semua check PASS, diakhiri baris RLS_TEST_PASS.
-- Jalankan: supabase db execute -f supabase/tests/rls_test.sql
--
-- Check inti (Step 1 brief): sebagai student A, select attendance_records
-- milik student B harus 0 baris. Karena db execute jalan sebagai postgres
-- (bypass RLS), simulasi level-baris dilakukan via SET ROLE authenticated +
-- request.jwt.claim.sub, lalu verifikasi anon/auth tanpa klaim tidak bisa
-- baca (default-deny) dan katalog policy sesuai ekspektasi.

-- 1. Tabel brief ada semua (gagal di sini = FAIL pra-migrasi).
DO $$
DECLARE t text;
BEGIN
  FOREACH t IN ARRAY ARRAY[
    'profiles', 'schedules', 'enrollments', 'qr_tokens',
    'attendance_codes', 'attendance_records', 'permit_requests'
  ] LOOP
    IF NOT EXISTS (
      SELECT 1 FROM information_schema.tables
      WHERE table_schema = 'public' AND table_name = t
    ) THEN
      RAISE EXCEPTION 'RLS_TEST_FAIL: tabel public.% belum ada', t;
    END IF;
  END LOOP;
  RAISE NOTICE 'RLS_TEST: 7 tabel ada';
END $$;

-- 2. Kolom R1 ada di schedules.
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = 'public' AND table_name = 'schedules'
      AND column_name IN
        ('late_threshold_minutes', 'latitude', 'longitude', 'radius_meters')
    HAVING COUNT(*) = 4
  ) THEN
    RAISE EXCEPTION 'RLS_TEST_FAIL: kolom R1 schedules tidak lengkap';
  END IF;
  RAISE NOTICE 'RLS_TEST: kolom R1 lengkap';
END $$;

-- 3. RLS aktif di semua tabel + minimal 6 policy.
DO $$
DECLARE n int;
BEGIN
  SELECT COUNT(*) INTO n FROM pg_tables
  WHERE schemaname = 'public' AND rowsecurity = true
    AND tablename IN ('profiles', 'schedules', 'enrollments', 'qr_tokens',
                      'attendance_codes', 'attendance_records', 'permit_requests');
  IF n <> 7 THEN
    RAISE EXCEPTION 'RLS_TEST_FAIL: RLS aktif di %/7 tabel', n;
  END IF;
  SELECT COUNT(*) INTO n FROM pg_policies WHERE schemaname = 'public';
  IF n < 6 THEN
    RAISE EXCEPTION 'RLS_TEST_FAIL: hanya % policy (< 6)', n;
  END IF;
  RAISE NOTICE 'RLS_TEST: RLS on 7/7 tabel, % policies', n;
END $$;

-- 4. Policy kunci sesuai brief ada (termasuk deny direct insert).
DO $$
DECLARE p text;
BEGIN
  FOREACH p IN ARRAY ARRAY[
    'profiles_select_own', 'schedules_select_enrolled_or_lecturer',
    'attendance_records_select_own', 'attendance_records_no_direct_insert',
    'qr_tokens_select_enrolled', 'attendance_codes_select_enrolled',
    'permit_requests_select_own', 'permit_requests_insert_own'
  ] LOOP
    IF NOT EXISTS (
      SELECT 1 FROM pg_policies
      WHERE schemaname = 'public' AND policyname = p
    ) THEN
      RAISE EXCEPTION 'RLS_TEST_FAIL: policy % hilang', p;
    END IF;
  END LOOP;
  RAISE NOTICE 'RLS_TEST: 8 policy kunci ada';
END $$;

-- 5a. anon tanpa klaim: default-deny. Karena tidak ada GRANT ke anon,
--    hasilnya 0 baris ATAU error insufficient_privilege — keduanya PASS.
SET ROLE anon;
DO $$
DECLARE c int;
BEGIN
  BEGIN
    SELECT COUNT(*) INTO c FROM public.attendance_records
    WHERE student_id = '22222222-2222-2222-2222-222222222222';
    IF c <> 0 THEN
      RAISE EXCEPTION 'RLS_TEST_FAIL: anon membaca % baris milik student B', c;
    END IF;
  EXCEPTION WHEN insufficient_privilege THEN
    -- Tanpa GRANT ke anon: deny di level grant, sama validnya.
    NULL;
  END;
  RAISE NOTICE 'RLS_TEST: anon default-deny OK';
END $$;
RESET ROLE;

-- 5b. Check inti Step 1 brief: simulasi student A via klaim JWT
--    (cara yang didukung untuk testing RLS), select milik student B = 0 baris.
SET ROLE authenticated;
SELECT set_config('request.jwt.claims',
  '{"sub": "11111111-1111-1111-1111-111111111111"}', false);
DO $$
DECLARE c int;
BEGIN
  SELECT COUNT(*) INTO c FROM public.attendance_records
  WHERE student_id = '22222222-2222-2222-2222-222222222222';
  IF c <> 0 THEN
    RAISE EXCEPTION 'RLS_TEST_FAIL: student A membaca % baris milik student B', c;
  END IF;
  RAISE NOTICE 'RLS_TEST: cross-student read terblokir (0 baris)';
END $$;
RESET ROLE;
RESET "request.jwt.claims";

-- 6. View today_schedules_with_flags mengembalikan baris seed hari ini + flags benar.
DO $$
DECLARE c int; r record;
BEGIN
  SELECT COUNT(*) INTO c FROM public.today_schedules_with_flags;
  IF c < 1 THEN
    RAISE EXCEPTION 'RLS_TEST_FAIL: view hari ini kosong (seed day tidak cocok?)';
  END IF;
  SELECT * INTO r FROM public.today_schedules_with_flags WHERE id = 1;
  IF NOT FOUND THEN
    RAISE EXCEPTION 'RLS_TEST_FAIL: jadwal seed id=1 tidak muncul di view';
  END IF;
  IF r.has_qr IS NOT true OR r.has_active_code IS NOT true THEN
    RAISE EXCEPTION 'RLS_TEST_FAIL: flags jadwal 1 salah (has_qr=%, has_active_code=%)',
      r.has_qr, r.has_active_code;
  END IF;
  RAISE NOTICE 'RLS_TEST: view OK (% baris, flags jadwal 1 benar)', c;
END $$;

-- 7. View security_invoker: katalog + student non-enrolled melihat 0 baris.
--    Tanpa security_invoker=true, view jalan sebagai owner dan membocorkan
--    semua jadwal hari ini ke siapa pun yang punya GRANT.
DO $$
DECLARE opts text[];
BEGIN
  SELECT reloptions INTO opts FROM pg_class
  WHERE relnamespace = 'public'::regnamespace
    AND relname = 'today_schedules_with_flags';
  IF opts IS NULL OR NOT ('security_invoker=true' = ANY (opts)) THEN
    RAISE EXCEPTION 'RLS_TEST_FAIL: view tanpa security_invoker=true (reloptions=%)', opts;
  END IF;
  RAISE NOTICE 'RLS_TEST: view security_invoker=true OK';
END $$;

-- Mahasiswa C: ada di profiles, tidak enrolled di jadwal mana pun.
INSERT INTO auth.users (id) VALUES ('44444444-4444-4444-4444-444444444444')
ON CONFLICT DO NOTHING;
INSERT INTO public.profiles (id, identity_number, name, role) VALUES
  ('44444444-4444-4444-4444-444444444444', '20240003', 'Mahasiswa C', 'STUDENT')
ON CONFLICT (id) DO NOTHING;

SET ROLE authenticated;
SELECT set_config('request.jwt.claims',
  '{"sub": "44444444-4444-4444-4444-444444444444"}', false);
DO $$
DECLARE c int;
BEGIN
  SELECT COUNT(*) INTO c FROM public.today_schedules_with_flags;
  IF c <> 0 THEN
    RAISE EXCEPTION 'RLS_TEST_FAIL: non-enrolled melihat % baris via view', c;
  END IF;
  RAISE NOTICE 'RLS_TEST: non-enrolled 0 baris via view OK';
END $$;
RESET ROLE;
RESET "request.jwt.claims";
-- Bersihkan akun uji agar seed/test idempoten.
DELETE FROM public.profiles WHERE id = '44444444-4444-4444-4444-444444444444';
DELETE FROM auth.users WHERE id = '44444444-4444-4444-4444-444444444444';

SELECT 'RLS_TEST_PASS' AS result;
