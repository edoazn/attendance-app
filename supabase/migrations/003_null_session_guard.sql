-- Migration: 003_null_session_guard
-- Temuan Important review fase-1: UNIQUE(session_id, student_id) dari 002
-- tidak menjerat baris session_id NULL (NULL tidak konflik di Postgres),
-- sehingga path non-sesi (geolocation/kode, selalu session NULL) kehilangan
-- jaring pengaman duplikat di level DB. Partial unique index ini
-- mengembalikannya: satu baris NULL-sesi per (schedule, student).
-- Idempoten; JANGAN apply ke production tanpa konfirmasi.

-- Bersihkan duplikat NULL-sesi yang mungkin sudah terbentuk (pertahankan
-- baris tertua per pasangan), agar index bisa dibuat.
DELETE FROM public.attendance_records a
USING public.attendance_records older
WHERE a.session_id IS NULL
  AND older.session_id IS NULL
  AND older.schedule_id = a.schedule_id
  AND older.student_id = a.student_id
  AND older.id < a.id;

CREATE UNIQUE INDEX IF NOT EXISTS
  attendance_records_null_session_unique
  ON public.attendance_records (schedule_id, student_id)
  WHERE session_id IS NULL;
