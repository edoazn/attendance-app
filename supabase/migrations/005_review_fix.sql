-- Migration: 005_review_fix
-- Temuan Critical review Plan B (fix-pass sekali jalan, TDD per temuan):
--   1. ADMIN via user JWT tidak membaca attendance_records/sessions
--      (view security_invoker) -> tambah 2 policy SELECT admin.
--   2. Baris IZIN (session NULL) menggelembungkan total_sesi view ->
--      suku legacy dibatasi status HADIR.
-- Dijalankan via: npx supabase db push --linked (project dev).
-- JANGAN apply ke production tanpa konfirmasi.

-- 1. Admin membaca records + sessions (dashboard rekap & live list).
CREATE POLICY attendance_records_admin_select
  ON public.attendance_records FOR SELECT TO authenticated
  USING (public.is_admin());

CREATE POLICY attendance_sessions_admin_select
  ON public.attendance_sessions FOR SELECT TO authenticated
  USING (public.is_admin());

-- 2. View rekap: suku legacy HANYA baris HADIR session-NULL.
--    Baris IZIN (session NULL, lahir dari approve-permit) tidak menambah
--    penyebut — persen tidak turun saat izin disetujui.
CREATE OR REPLACE VIEW public.attendance_summary
WITH (security_invoker = true) AS
SELECT
  s.id AS schedule_id,
  e.student_id,
  (
    (SELECT COUNT(*) FROM public.attendance_sessions ss
     WHERE ss.schedule_id = s.id)
    + COUNT(ar.id) FILTER (
      WHERE ar.session_id IS NULL AND ar.status = 'HADIR'
    )
  ) AS total_sesi,
  COUNT(ar.id) FILTER (WHERE ar.status = 'HADIR') AS hadir,
  COUNT(ar.id) FILTER (WHERE ar.status = 'IZIN') AS izin,
  CASE
    WHEN (
      (SELECT COUNT(*) FROM public.attendance_sessions ss
       WHERE ss.schedule_id = s.id)
      + COUNT(ar.id) FILTER (
        WHERE ar.session_id IS NULL AND ar.status = 'HADIR'
      )
    ) = 0 THEN 0
    ELSE (
      100 * COUNT(ar.id) FILTER (WHERE ar.status = 'HADIR')
      / (
        (SELECT COUNT(*) FROM public.attendance_sessions ss
         WHERE ss.schedule_id = s.id)
        + COUNT(ar.id) FILTER (
          WHERE ar.session_id IS NULL AND ar.status = 'HADIR'
        )
      )
    )
  END AS persen
FROM public.schedules s
JOIN public.enrollments e ON e.schedule_id = s.id
LEFT JOIN public.attendance_records ar
  ON ar.schedule_id = s.id AND ar.student_id = e.student_id
GROUP BY s.id, e.student_id;

GRANT SELECT ON public.attendance_summary TO authenticated;
