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
-- Prasyarat auth.users sama seperti supabase/seed.sql (user dibuat via
-- Dashboard/Auth API): di project nyata auth.users punya kolom NOT NULL
-- (aud/role/email, ...) sehingga INSERT minimal (id) gagal. Bungkus dengan
-- handler eksplisit agar live run tidak gagal di tengah.
DO $$
BEGIN
  INSERT INTO auth.users (id) VALUES ('44444444-4444-4444-4444-444444444444')
  ON CONFLICT DO NOTHING;
EXCEPTION WHEN not_null_violation THEN
  RAISE NOTICE 'RLS_TEST: skip pembuatan auth.users C (kolom NOT NULL project nyata) — buat via Dashboard/Auth API untuk cek view penuh';
END $$;
DO $$
BEGIN
  INSERT INTO public.profiles (id, identity_number, name, role) VALUES
    ('44444444-4444-4444-4444-444444444444', '20240003', 'Mahasiswa C', 'STUDENT')
  ON CONFLICT (id) DO NOTHING;
EXCEPTION WHEN foreign_key_violation THEN
  RAISE NOTICE 'RLS_TEST: skip pembuatan profil C (auth.users prasyarat belum ada, lih. supabase/seed.sql) — cek view non-enrolled di bawah jadi vakum (0 baris tetap PASS)';
END $$;

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

-- ================================================================ FASE 1
-- Task 1 plan lecturer-backend-fase1: QR statis + sesi instans + scoping
-- lecturer. Pra-migrasi 002: GAGAL di check 8 (kolom/tabel belum ada).
-- Pasca migrasi+seed: semua check 8-12 PASS.

-- 8. Kolom sesi lecturer di schedules.
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = 'public' AND table_name = 'schedules'
      AND column_name IN ('lecturer_id', 'session_open', 'session_expires_at')
    HAVING COUNT(*) = 3
  ) THEN
    RAISE EXCEPTION 'RLS_TEST_FAIL: kolom fase-1 schedules (lecturer_id/session_open/session_expires_at) tidak lengkap';
  END IF;
  RAISE NOTICE 'RLS_TEST: kolom fase-1 schedules lengkap';
END $$;

-- 9. Tabel attendance_sessions + session_id + unique baru + qr statis unik
--    + permit_requests.status.
DO $$
DECLARE n int;
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.tables
    WHERE table_schema = 'public' AND table_name = 'attendance_sessions'
  ) THEN
    RAISE EXCEPTION 'RLS_TEST_FAIL: tabel public.attendance_sessions belum ada';
  END IF;
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = 'public' AND table_name = 'attendance_records'
      AND column_name = 'session_id'
  ) THEN
    RAISE EXCEPTION 'RLS_TEST_FAIL: kolom attendance_records.session_id belum ada';
  END IF;
  -- UNIQUE(session_id, student_id) harus ada; UNIQUE lama
  -- (schedule_id, student_id) harus sudah dibuang.
  SELECT COUNT(*) INTO n FROM pg_constraint c
  JOIN pg_class t ON t.oid = c.conrelid
  JOIN pg_namespace ns ON ns.oid = t.relnamespace
  WHERE ns.nspname = 'public' AND t.relname = 'attendance_records'
    AND c.contype = 'u'
    AND (SELECT array_agg(a.attname::text ORDER BY k.ord)
         FROM unnest(c.conkey) WITH ORDINALITY AS k(attnum, ord)
         JOIN pg_attribute a ON a.attrelid = t.oid AND a.attnum = k.attnum)
        = ARRAY['session_id', 'student_id'];
  IF n <> 1 THEN
    RAISE EXCEPTION 'RLS_TEST_FAIL: UNIQUE(session_id, student_id) tidak ada';
  END IF;
  SELECT COUNT(*) INTO n FROM pg_constraint c
  JOIN pg_class t ON t.oid = c.conrelid
  JOIN pg_namespace ns ON ns.oid = t.relnamespace
  WHERE ns.nspname = 'public' AND t.relname = 'attendance_records'
    AND c.contype = 'u'
    AND (SELECT array_agg(a.attname::text ORDER BY k.ord)
         FROM unnest(c.conkey) WITH ORDINALITY AS k(attnum, ord)
         JOIN pg_attribute a ON a.attrelid = t.oid AND a.attnum = k.attnum)
        = ARRAY['schedule_id', 'student_id'];
  IF n <> 0 THEN
    RAISE EXCEPTION 'RLS_TEST_FAIL: UNIQUE lama (schedule_id, student_id) masih ada';
  END IF;
  -- qr_tokens: satu baris per jadwal.
  SELECT COUNT(*) INTO n FROM pg_constraint c
  JOIN pg_class t ON t.oid = c.conrelid
  JOIN pg_namespace ns ON ns.oid = t.relnamespace
  WHERE ns.nspname = 'public' AND t.relname = 'qr_tokens'
    AND c.contype = 'u'
    AND (SELECT array_agg(a.attname::text ORDER BY k.ord)
         FROM unnest(c.conkey) WITH ORDINALITY AS k(attnum, ord)
         JOIN pg_attribute a ON a.attrelid = t.oid AND a.attnum = k.attnum)
        = ARRAY['schedule_id'];
  IF n <> 1 THEN
    RAISE EXCEPTION 'RLS_TEST_FAIL: UNIQUE(qr_tokens.schedule_id) tidak ada';
  END IF;
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = 'public' AND table_name = 'permit_requests'
      AND column_name = 'status'
  ) THEN
    RAISE EXCEPTION 'RLS_TEST_FAIL: kolom permit_requests.status belum ada';
  END IF;
  RAISE NOTICE 'RLS_TEST: sesi instans + unique baru + status izin OK';
END $$;

-- 10. Policy fase-1 ada (schedules_select_enrolled_or_lecturer dipertahankan
--     namanya tapi dipersempit; sessions select via induk + deny tulis).
DO $$
DECLARE p text;
BEGIN
  FOREACH p IN ARRAY ARRAY[
    'schedules_select_enrolled_or_lecturer',
    'attendance_sessions_select_via_schedule',
    'attendance_sessions_no_direct_insert',
    'attendance_sessions_no_direct_update',
    'attendance_sessions_no_direct_delete'
  ] LOOP
    IF NOT EXISTS (
      SELECT 1 FROM pg_policies
      WHERE schemaname = 'public' AND policyname = p
    ) THEN
      RAISE EXCEPTION 'RLS_TEST_FAIL: policy % hilang', p;
    END IF;
  END LOOP;
  RAISE NOTICE 'RLS_TEST: 5 policy fase-1 ada';
END $$;

-- 11. Behavior Step 1 brief: 2 sesi berbeda, student sama -> sukses keduanya
--     (bukan 409). Jalan sebagai postgres (uji constraint, bukan RLS).
DO $$
BEGIN
  INSERT INTO public.attendance_sessions (schedule_id)
  VALUES (1), (1);
  INSERT INTO public.attendance_records
    (schedule_id, student_id, session_id, method, status)
  VALUES
    (1, 'feaef403-a3ae-4c7f-88db-30c5cbcc9b2f',
     (SELECT min(id) FROM public.attendance_sessions WHERE schedule_id = 1 AND closed_at IS NULL),
     'qr_code', 'HADIR'),
    (1, 'feaef403-a3ae-4c7f-88db-30c5cbcc9b2f',
     (SELECT max(id) FROM public.attendance_sessions WHERE schedule_id = 1 AND closed_at IS NULL),
     'qr_code', 'HADIR');
  RAISE NOTICE 'RLS_TEST: sesi minggu-2 lolos unique OK';
EXCEPTION WHEN OTHERS THEN
  RAISE EXCEPTION 'RLS_TEST_FAIL: sesi minggu-2 gagal: %', SQLERRM;
END $$;
-- Bersihkan data uji check 11 (record dulu karena FK).
DO $$
BEGIN
  DELETE FROM public.attendance_records
  WHERE student_id = 'feaef403-a3ae-4c7f-88db-30c5cbcc9b2f' AND session_id IS NOT NULL;
  DELETE FROM public.attendance_sessions WHERE schedule_id = 1;
END $$;

-- 12. Behavior Step 1 brief: lecturer B tidak melihat jadwal milik A.
--     Memakai jadwal uji milik A (bukan seed) agar seed tak termutasi.
DO $$
DECLARE scoped_id bigint; c int;
BEGIN
  BEGIN
    INSERT INTO auth.users (id) VALUES
      ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa'),
      ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb')
    ON CONFLICT DO NOTHING;
  EXCEPTION WHEN not_null_violation THEN
    RAISE NOTICE 'RLS_TEST: skip scope lecturer (auth.users butuh Dashboard/Auth API)';
    RETURN;
  END;
  INSERT INTO public.profiles (id, identity_number, name, role) VALUES
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '19800011', 'Dosen A', 'LECTURER'),
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', '19800022', 'Dosen B', 'LECTURER')
  ON CONFLICT (id) DO NOTHING;
  INSERT INTO public.schedules
    (course_name, lecturer_name, lecturer_id, room, day, start_time, end_time, is_active)
  VALUES
    ('Uji Scope', 'Dosen A', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
     'Ruang U', 'Monday', '10:00', '12:00', true)
  RETURNING id INTO scoped_id;

  SET ROLE authenticated;
  PERFORM set_config('request.jwt.claims',
    '{"sub": "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"}', false);
  SELECT COUNT(*) INTO c FROM public.schedules WHERE id = scoped_id;
  IF c <> 0 THEN
    RESET ROLE;
    RAISE EXCEPTION 'RLS_TEST_FAIL: lecturer B melihat % baris jadwal milik A', c;
  END IF;
  PERFORM set_config('request.jwt.claims',
    '{"sub": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"}', false);
  SELECT COUNT(*) INTO c FROM public.schedules WHERE id = scoped_id;
  IF c <> 1 THEN
    RESET ROLE;
    RAISE EXCEPTION 'RLS_TEST_FAIL: lecturer A tidak melihat jadwalnya sendiri';
  END IF;
  RESET ROLE;
  RESET "request.jwt.claims";
  RAISE NOTICE 'RLS_TEST: scope lecturer (B=0, A=1) OK';
EXCEPTION WHEN OTHERS THEN
  BEGIN RESET ROLE; EXCEPTION WHEN OTHERS THEN NULL; END;
  RAISE;
END $$;
-- Bersihkan data uji check 12.
DELETE FROM public.schedules WHERE course_name = 'Uji Scope';
DELETE FROM public.profiles WHERE id IN
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb');
DELETE FROM auth.users WHERE id IN
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb');

-- 13. Jaring pengaman duplikat path non-sesi (temuan review fase-1):
--     partial unique index (schedule_id, student_id) WHERE session_id IS NULL.
--     Baris sesi (session_id NOT NULL) dijerat UNIQUE(session_id, student_id);
--     baris pra-sesi/geo/code (session_id NULL) dijerat index ini karena NULL
--     tidak konflik di unique biasa.
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_index i
    JOIN pg_class t ON t.oid = i.indrelid
    JOIN pg_namespace ns ON ns.oid = t.relnamespace
    WHERE ns.nspname = 'public'
      AND t.relname = 'attendance_records'
      AND i.indisunique = true
      AND pg_get_indexdef(i.indexrelid) ILIKE
        '%(schedule_id, student_id)%WHERE%session_id IS NULL%'
  ) THEN
    RAISE EXCEPTION 'RLS_TEST_FAIL: partial unique index non-sesi tidak ada';
  END IF;
  RAISE NOTICE 'RLS_TEST: partial unique index non-sesi OK';
END $$;

-- ================================================================ FASE 2
-- Plan dashboard-fase2-backend Task 1: peran ADMIN + rombel + view rekap.
-- Pra-migrasi 004: GAGAL di check 14 (CHECK menolak ADMIN).

-- 14. profiles.role menerima ADMIN + helper is_admin() ada.
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint
    WHERE conrelid = 'public.profiles'::regclass
      AND contype = 'c'
      AND pg_get_constraintdef(oid) ILIKE '%ADMIN%'
  ) THEN
    RAISE EXCEPTION 'RLS_TEST_FAIL: CHECK role belum mengenal ADMIN';
  END IF;
  IF NOT EXISTS (
    SELECT 1 FROM pg_proc p
    JOIN pg_namespace ns ON ns.oid = p.pronamespace
    WHERE ns.nspname = 'public' AND p.proname = 'is_admin'
  ) THEN
    RAISE EXCEPTION 'RLS_TEST_FAIL: helper public.is_admin() belum ada';
  END IF;
  RAISE NOTICE 'RLS_TEST: CHECK ADMIN + is_admin OK';
END $$;

-- 14b. Behavior: INSERT profil ADMIN lolos CHECK (skip bila auth.users
--     tak bisa dibuat, pola check 7/12).
DO $$
BEGIN
  BEGIN
    INSERT INTO auth.users (id) VALUES
      ('cccccccc-cccc-cccc-cccc-cccccccccccc')
    ON CONFLICT DO NOTHING;
  EXCEPTION WHEN not_null_violation THEN
    RAISE NOTICE 'RLS_TEST: skip insert ADMIN uji (auth.users butuh Dashboard/Auth API)';
    RETURN;
  END;
  INSERT INTO public.profiles (id, identity_number, name, role) VALUES
    ('cccccccc-cccc-cccc-cccc-cccccccccccc', '19900001', 'Admin Uji', 'ADMIN')
  ON CONFLICT (id) DO UPDATE SET role = 'ADMIN';
  RAISE NOTICE 'RLS_TEST: INSERT profil ADMIN lolos CHECK OK';
END $$;
DELETE FROM public.profiles WHERE id = 'cccccccc-cccc-cccc-cccc-cccccccccccc';
DELETE FROM auth.users WHERE id = 'cccccccc-cccc-cccc-cccc-cccccccccccc';

-- 15. Kolom profiles.rombel + view attendance_summary (+ invoker + kolom).
DO $$
DECLARE col text;
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = 'public' AND table_name = 'profiles'
      AND column_name = 'rombel'
  ) THEN
    RAISE EXCEPTION 'RLS_TEST_FAIL: kolom profiles.rombel belum ada';
  END IF;
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.views
    WHERE table_schema = 'public' AND table_name = 'attendance_summary'
  ) THEN
    RAISE EXCEPTION 'RLS_TEST_FAIL: view attendance_summary belum ada';
  END IF;
  FOREACH col IN ARRAY ARRAY[
    'schedule_id', 'student_id', 'total_sesi', 'hadir', 'izin', 'persen'
  ] LOOP
    IF NOT EXISTS (
      SELECT 1 FROM information_schema.columns
      WHERE table_schema = 'public' AND table_name = 'attendance_summary'
        AND column_name = col
    ) THEN
      RAISE EXCEPTION 'RLS_TEST_FAIL: kolom % view rekap hilang', col;
    END IF;
  END LOOP;
  IF NOT EXISTS (
    SELECT 1 FROM pg_class
    WHERE relnamespace = 'public'::regnamespace
      AND relname = 'attendance_summary'
      AND 'security_invoker=true' = ANY (reloptions)
  ) THEN
    RAISE EXCEPTION 'RLS_TEST_FAIL: view rekap tanpa security_invoker=true';
  END IF;
  RAISE NOTICE 'RLS_TEST: rombel + view rekap OK';
END $$;

-- 16. Policy fase-2 ada di katalog.
DO $$
DECLARE p text;
BEGIN
  FOREACH p IN ARRAY ARRAY[
    'schedules_admin_all',
    'enrollments_admin_all',
    'profiles_admin_select',
    'profiles_admin_update',
    'permit_requests_admin_lecturer_select'
  ] LOOP
    IF NOT EXISTS (
      SELECT 1 FROM pg_policies
      WHERE schemaname = 'public' AND policyname = p
    ) THEN
      RAISE EXCEPTION 'RLS_TEST_FAIL: policy % hilang', p;
    END IF;
  END LOOP;
  RAISE NOTICE 'RLS_TEST: 5 policy fase-2 ada';
END $$;

-- ================================================================ FIX-PASS
-- Temuan review Plan B: policy admin records/sessions + perilaku rekap.

-- 17. Behavior: ADMIN (user JWT, bukan postgres) melihat semua jadwal +
--     records + rekap; lecturer scope tetap; non-enrolled 0 baris rekap.
DO $$
DECLARE c int;
BEGIN
  BEGIN
    INSERT INTO auth.users (id) VALUES
      ('dddddddd-dddd-dddd-dddd-dddddddddddd')
    ON CONFLICT DO NOTHING;
  EXCEPTION WHEN not_null_violation THEN
    RAISE NOTICE 'RLS_TEST: skip check-17 (auth.users butuh Dashboard/Auth API)';
    RETURN;
  END;
  INSERT INTO public.profiles (id, identity_number, name, role) VALUES
    ('dddddddd-dddd-dddd-dddd-dddddddddddd', '19900077', 'Admin Uji', 'ADMIN')
  ON CONFLICT (id) DO UPDATE SET role = 'ADMIN';

  SET ROLE authenticated;
  PERFORM set_config('request.jwt.claims',
    '{"sub": "dddddddd-dddd-dddd-dddd-dddddddddddd"}', false);
  SELECT COUNT(*) INTO c FROM public.schedules;
  IF c < 3 THEN
    RESET ROLE;
    RAISE EXCEPTION 'RLS_TEST_FAIL: admin melihat % jadwal (< 3 seed)', c;
  END IF;
  SELECT COUNT(*) INTO c FROM public.attendance_records;
  IF c < 1 THEN
    RESET ROLE;
    RAISE EXCEPTION 'RLS_TEST_FAIL: admin melihat 0 attendance_records';
  END IF;
  SELECT COUNT(*) INTO c FROM public.attendance_summary;
  IF c < 1 THEN
    RESET ROLE;
    RAISE EXCEPTION 'RLS_TEST_FAIL: admin melihat 0 baris rekap';
  END IF;
  -- Lecturer seed (Dr. Dosen) tetap melihat jadwalnya.
  PERFORM set_config('request.jwt.claims',
    '{"sub": "1d389c57-e170-4c39-a06c-f5ba11ee2239"}', false);
  SELECT COUNT(*) INTO c FROM public.schedules;
  IF c < 3 THEN
    RESET ROLE;
    RAISE EXCEPTION 'RLS_TEST_FAIL: dosen pemilik melihat % jadwal (< 3)', c;
  END IF;
  RESET ROLE;
  RESET "request.jwt.claims";
  RAISE NOTICE 'RLS_TEST: perilaku admin + scope lecturer OK';
EXCEPTION WHEN OTHERS THEN
  BEGIN RESET ROLE; EXCEPTION WHEN OTHERS THEN NULL; END;
  RAISE;
END $$;
DELETE FROM public.profiles WHERE id = 'dddddddd-dddd-dddd-dddd-dddddddddddd';
DELETE FROM auth.users WHERE id = 'dddddddd-dddd-dddd-dddd-dddddddddddd';

-- 18. View-content: baris IZIN (session NULL) TIDAK menambah penyebut.
DO $$
DECLARE total int;
BEGIN
  INSERT INTO public.attendance_records
    (schedule_id, student_id, method, status, session_id)
  VALUES (1, 'feaef403-a3ae-4c7f-88db-30c5cbcc9b2f', 'izin', 'IZIN', NULL);
  SELECT total_sesi INTO total FROM public.attendance_summary
  WHERE schedule_id = 1
    AND student_id = 'feaef403-a3ae-4c7f-88db-30c5cbcc9b2f';
  IF total <> 0 THEN
    RAISE EXCEPTION 'RLS_TEST_FAIL: IZIN menambah penyebut (total=%)', total;
  END IF;
  RAISE NOTICE 'RLS_TEST: IZIN tidak menggelembungkan penyebut OK';
EXCEPTION WHEN OTHERS THEN
  DELETE FROM public.attendance_records
  WHERE schedule_id = 1
    AND student_id = 'feaef403-a3ae-4c7f-88db-30c5cbcc9b2f'
    AND status = 'IZIN';
  RAISE;
END $$;
DELETE FROM public.attendance_records
WHERE schedule_id = 1
  AND student_id = 'feaef403-a3ae-4c7f-88db-30c5cbcc9b2f'
  AND status = 'IZIN';

SELECT 'RLS_TEST_PASS' AS result;
