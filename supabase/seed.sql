-- Seed dev untuk Task 1 migrasi Supabase.
-- Prasyarat: 3 auth.users di bawah HARUS sudah ada (buat via Dashboard
-- Authentication atau supabase auth admin API), karena profiles.id FK ke
-- auth.users(id). UUID di bawah adalah ID ASLI dari project dev
-- takntldmevqfasenqdiq (diambil 2026-10-13); JANGAN jalankan file ini ke
-- project lain tanpa mengganti UUID-nya.
-- Jalankan: supabase db query --linked -f supabase/seed.sql (project dev SAJA).

-- Catatan: insert auth.users tidak dilakukan di sini (password hashing +
-- konfirmasi email harus lewat Auth API). Jika user auth belum ada, blok
-- profiles akan gagal FK — buat dulu user-nya, lalu ulangi file ini.

-- ------------------------------------------------------- profiles (butuh auth.users)
INSERT INTO public.profiles (id, identity_number, name, email, role, rombel) VALUES
  ('feaef403-a3ae-4c7f-88db-30c5cbcc9b2f', '20240001', 'Mahasiswa A', 'mhs.a@kampus.ac.id', 'STUDENT', 'TI-2A'),
  ('9940ab8d-1e2b-4bd4-b921-a40d8b85f971', '20240002', 'Mahasiswa B', 'mhs.b@kampus.ac.id', 'STUDENT', 'TI-2A'),
  ('1d389c57-e170-4c39-a06c-f5ba11ee2239', '19800001', 'Dr. Dosen', 'dosen@kampus.ac.id', 'LECTURER', NULL)
ON CONFLICT (id) DO UPDATE SET
  identity_number = EXCLUDED.identity_number,
  name = EXCLUDED.name,
  email = EXCLUDED.email,
  role = EXCLUDED.role,
  rombel = EXCLUDED.rombel;

-- Admin TU: auth.users-nya dibuat via Dashboard/Auth API (atau script
-- setup-admin Task 4); guard FK agar seed tetap jalan tanpanya.
DO $$
BEGIN
  INSERT INTO public.profiles
    (id, identity_number, name, email, role, rombel)
  VALUES
    ('9e9e9e9e-9e9e-9e9e-9e9e-9e9e9e9e9e9e', '19900001', 'Admin TU',
     'tu@kampus.ac.id', 'ADMIN', NULL)
  ON CONFLICT (id) DO UPDATE SET
    identity_number = EXCLUDED.identity_number,
    name = EXCLUDED.name,
    email = EXCLUDED.email,
    role = EXCLUDED.role,
    rombel = EXCLUDED.rombel;
EXCEPTION WHEN foreign_key_violation THEN
  RAISE NOTICE 'SEED: skip profil Admin TU (auth.users prasyarat belum ada — buat via Dashboard atau setup-admin Task 4)';
END $$;

-- ------------------------------------------------------- schedules
-- Baris pertama memakai hari BERJALAN (dinamis) supaya view
-- today_schedules_with_flags selalu mengembalikan >= 1 baris saat test.
-- lecturer_id eksplisit ke Dr. Dosen (UUID dev di atas); fase-1: dosen
-- hanya melihat jadwal miliknya + pool NULL.
INSERT INTO public.schedules
  (id, course_name, lecturer_name, lecturer_id, room, day, start_time, end_time, is_active,
   late_threshold_minutes, latitude, longitude, radius_meters)
VALUES
  (1, 'Pemrograman Mobile', 'Dr. Dosen', '1d389c57-e170-4c39-a06c-f5ba11ee2239', 'Ruang 101', TRIM(TO_CHAR(now(), 'Day')),
   '08:00', '10:00', true, 15, -6.200000, 106.816666, 100),
  (2, 'Basis Data', 'Dr. Dosen', '1d389c57-e170-4c39-a06c-f5ba11ee2239', 'Ruang 202', 'Monday',
   '10:00', '12:00', true, 15, -6.200000, 106.816666, 100),
  (3, 'Jaringan Komputer (arsip)', 'Dr. Dosen', '1d389c57-e170-4c39-a06c-f5ba11ee2239', 'Ruang 303', 'Friday',
   '13:00', '15:00', false, 10, NULL, NULL, 50)
ON CONFLICT (id) DO UPDATE SET
  course_name = EXCLUDED.course_name,
  lecturer_name = EXCLUDED.lecturer_name,
  lecturer_id = EXCLUDED.lecturer_id,
  room = EXCLUDED.room,
  day = EXCLUDED.day,
  start_time = EXCLUDED.start_time,
  end_time = EXCLUDED.end_time,
  is_active = EXCLUDED.is_active,
  late_threshold_minutes = EXCLUDED.late_threshold_minutes,
  latitude = EXCLUDED.latitude,
  longitude = EXCLUDED.longitude,
  radius_meters = EXCLUDED.radius_meters;
SELECT setval(pg_get_serial_sequence('public.schedules', 'id'), 100);

-- ------------------------------------------------------- enrollments
INSERT INTO public.enrollments (schedule_id, student_id) VALUES
  (1, 'feaef403-a3ae-4c7f-88db-30c5cbcc9b2f'),
  (1, '9940ab8d-1e2b-4bd4-b921-a40d8b85f971'),
  (2, 'feaef403-a3ae-4c7f-88db-30c5cbcc9b2f')
ON CONFLICT DO NOTHING;

-- ------------------------------------------------------- qr_tokens (1 valid stabil untuk jadwal 1)
-- Token UUID deterministik + ON CONFLICT DO UPDATE: re-run seed me-refresh
-- expiry tanpa menambah baris duplikat.
INSERT INTO public.qr_tokens (token, schedule_id, expires_at) VALUES
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 1, now() + interval '2 hours')
ON CONFLICT (token) DO UPDATE SET
  schedule_id = EXCLUDED.schedule_id,
  expires_at = EXCLUDED.expires_at;

-- ------------------------------------------------------- attendance_codes (1 aktif untuk jadwal 1)
INSERT INTO public.attendance_codes (code, schedule_id, expires_at, is_active) VALUES
  ('ABC123', 1, now() + interval '2 hours', true)
ON CONFLICT (code) DO UPDATE SET
  schedule_id = EXCLUDED.schedule_id,
  expires_at = EXCLUDED.expires_at,
  is_active = EXCLUDED.is_active;

-- ------------------------------------------------------- attendance_records (milik B di jadwal 1)
-- Fase-1: session_id NULL (baris pra-sesi, tetap valid karena NULL tidak
-- konflik di UNIQUE baru). Idempoten via WHERE NOT EXISTS (ON CONFLICT
-- arbiter kolom lama sudah tidak ada).
INSERT INTO public.attendance_records (schedule_id, student_id, method, status, distance)
SELECT 1, '9940ab8d-1e2b-4bd4-b921-a40d8b85f971', 'qr_code', 'HADIR', 25.5
WHERE NOT EXISTS (
  SELECT 1 FROM public.attendance_records
  WHERE schedule_id = 1
    AND student_id = '9940ab8d-1e2b-4bd4-b921-a40d8b85f971'
    AND session_id IS NULL
);

-- ------------------------------------------------------- permit_requests (milik A di jadwal 2)
-- Id eksplisit + ON CONFLICT (id): idempoten nyata. (ON CONFLICT DO NOTHING
-- tanpa arbiter tidak pernah trigger di tabel ini karena PK id selalu baru.)
INSERT INTO public.permit_requests (id, schedule_id, student_id, alasan) VALUES
  (1, 2, 'feaef403-a3ae-4c7f-88db-30c5cbcc9b2f', 'Sakit — surat dokter terlampir')
ON CONFLICT (id) DO UPDATE SET
  schedule_id = EXCLUDED.schedule_id,
  student_id = EXCLUDED.student_id,
  alasan = EXCLUDED.alasan;
SELECT setval(pg_get_serial_sequence('public.permit_requests', 'id'), 100);
