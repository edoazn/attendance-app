-- Seed dev untuk Task 1 migrasi Supabase.
-- Prasyarat: 3 auth.users di bawah HARUS sudah ada (buat via Dashboard
-- Authentication atau supabase auth admin API), karena profiles.id FK ke
-- auth.users(id). UUID deterministik agar seed idempoten.
-- Jalankan: supabase db execute -f supabase/seed.sql (project dev SAJA).

-- Catatan: insert auth.users tidak dilakukan di sini (password hashing +
-- konfirmasi email harus lewat Auth API). Jika user auth belum ada, blok
-- profiles akan gagal FK — buat dulu user-nya, lalu ulangi file ini.

-- ------------------------------------------------------- profiles (butuh auth.users)
INSERT INTO public.profiles (id, identity_number, name, email, role) VALUES
  ('11111111-1111-1111-1111-111111111111', '20240001', 'Mahasiswa A', 'mhs.a@kampus.ac.id', 'STUDENT'),
  ('22222222-2222-2222-2222-222222222222', '20240002', 'Mahasiswa B', 'mhs.b@kampus.ac.id', 'STUDENT'),
  ('33333333-3333-3333-3333-333333333333', '19800001', 'Dr. Dosen', 'dosen@kampus.ac.id', 'LECTURER')
ON CONFLICT (id) DO UPDATE SET
  identity_number = EXCLUDED.identity_number,
  name = EXCLUDED.name,
  email = EXCLUDED.email,
  role = EXCLUDED.role;

-- ------------------------------------------------------- schedules
-- Baris pertama memakai hari BERJALAN (dinamis) supaya view
-- today_schedules_with_flags selalu mengembalikan >= 1 baris saat test.
INSERT INTO public.schedules
  (id, course_name, lecturer_name, room, day, start_time, end_time, is_active,
   late_threshold_minutes, latitude, longitude, radius_meters)
VALUES
  (1, 'Pemrograman Mobile', 'Dr. Dosen', 'Ruang 101', TRIM(TO_CHAR(now(), 'Day')),
   '08:00', '10:00', true, 15, -6.200000, 106.816666, 100),
  (2, 'Basis Data', 'Dr. Dosen', 'Ruang 202', 'Monday',
   '10:00', '12:00', true, 15, -6.200000, 106.816666, 100),
  (3, 'Jaringan Komputer (arsip)', 'Dr. Dosen', 'Ruang 303', 'Friday',
   '13:00', '15:00', false, 10, NULL, NULL, 50)
ON CONFLICT (id) DO UPDATE SET
  course_name = EXCLUDED.course_name,
  lecturer_name = EXCLUDED.lecturer_name,
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
  (1, '11111111-1111-1111-1111-111111111111'),
  (1, '22222222-2222-2222-2222-222222222222'),
  (2, '11111111-1111-1111-1111-111111111111')
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
INSERT INTO public.attendance_records (schedule_id, student_id, method, status, distance) VALUES
  (1, '22222222-2222-2222-2222-222222222222', 'QR', 'HADIR', 25.5)
ON CONFLICT (schedule_id, student_id) DO NOTHING;

-- ------------------------------------------------------- permit_requests (milik A di jadwal 2)
-- Id eksplisit + ON CONFLICT (id): idempoten nyata. (ON CONFLICT DO NOTHING
-- tanpa arbiter tidak pernah trigger di tabel ini karena PK id selalu baru.)
INSERT INTO public.permit_requests (id, schedule_id, student_id, alasan) VALUES
  (1, 2, '11111111-1111-1111-1111-111111111111', 'Sakit — surat dokter terlampir')
ON CONFLICT (id) DO UPDATE SET
  schedule_id = EXCLUDED.schedule_id,
  student_id = EXCLUDED.student_id,
  alasan = EXCLUDED.alasan;
SELECT setval(pg_get_serial_sequence('public.permit_requests', 'id'), 100);
