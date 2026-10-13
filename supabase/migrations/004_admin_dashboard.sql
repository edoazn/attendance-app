-- Migration: 004_admin_dashboard
-- Fase 2 dashboard (plan 2026-10-13-dashboard-fase2-backend, Task 1):
-- peran ADMIN + kolom rombel + policy admin/lecturer + view rekap.
-- Dijalankan via: npx supabase db push --linked (project dev).
-- JANGAN apply ke production tanpa konfirmasi.
--
-- Perubahan:
--   profiles.role CHECK dilonggarkan: STUDENT, LECTURER, ADMIN.
--   profiles.rombel text NULL (paket bundling per rombel per semester).
--   public.is_admin() — helper SECURITY DEFINER (pola is_lecturer 001).
--   Policy admin (FOR ALL/parsial, via is_admin) + select izin untuk
--     admin & lecturer pemilik (own+pool, cermin policy schedules).
--   View attendance_summary (security_invoker): satu baris per
--     (schedule, enrolled student): total_sesi, hadir, izin, persen.
--     Baris legacy session NULL dihitung 1 hadir + 1 penyebut.

-- ------------------------------------------------- role ADMIN + rombel
ALTER TABLE public.profiles
  DROP CONSTRAINT IF EXISTS profiles_role_check;
ALTER TABLE public.profiles
  ADD CONSTRAINT profiles_role_check
  CHECK (role IN ('STUDENT', 'LECTURER', 'ADMIN'));

ALTER TABLE public.profiles
  ADD COLUMN IF NOT EXISTS rombel text;

-- ------------------------------------------------- helper is_admin
CREATE OR REPLACE FUNCTION public.is_admin()
RETURNS boolean
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public
AS $$
  SELECT EXISTS (
    SELECT 1 FROM public.profiles
    WHERE id = auth.uid() AND role = 'ADMIN'
  );
$$;
GRANT EXECUTE ON FUNCTION public.is_admin() TO authenticated;
REVOKE EXECUTE ON FUNCTION public.is_admin() FROM PUBLIC, anon;

-- ------------------------------------------------------------------ policies
-- Admin: master penuh (schedules + enrollments). Lecturer/student tetap
-- default-deny untuk tulis (tanpa policy tulis bagi mereka).
CREATE POLICY schedules_admin_all
  ON public.schedules FOR ALL TO authenticated
  USING (public.is_admin())
  WITH CHECK (public.is_admin());

CREATE POLICY enrollments_admin_all
  ON public.enrollments FOR ALL TO authenticated
  USING (public.is_admin())
  WITH CHECK (public.is_admin());

-- Admin kelola profil (baca + ubah; tanpa hapus via RLS).
CREATE POLICY profiles_admin_select
  ON public.profiles FOR SELECT TO authenticated
  USING (public.is_admin());
CREATE POLICY profiles_admin_update
  ON public.profiles FOR UPDATE TO authenticated
  USING (public.is_admin())
  WITH CHECK (public.is_admin());

-- Izin: admin semua; lecturer hanya jadwal miliknya + pool NULL.
CREATE POLICY permit_requests_admin_lecturer_select
  ON public.permit_requests FOR SELECT TO authenticated
  USING (
    public.is_admin()
    OR (
      public.is_lecturer()
      AND EXISTS (
        SELECT 1 FROM public.schedules s
        WHERE s.id = permit_requests.schedule_id
          AND (s.lecturer_id = auth.uid() OR s.lecturer_id IS NULL)
      )
    )
  );

-- ------------------------------------------------------------------ grants
-- Policy membatasi; grant membuka aksi. Tanpa policy tulis, lecturer/
-- student tetap default-deny walau grant ada.
GRANT SELECT, INSERT, UPDATE, DELETE
  ON public.schedules, public.enrollments TO authenticated;
GRANT SELECT, UPDATE ON public.profiles TO authenticated;

-- ------------------------------------------------- view rekap kehadiran
-- Penyebut = sesi dibuka + baris legacy (session NULL, pra-Fase-1).
-- persen = 100*hadir/total (0 bila total 0); IZIN kolom sendiri.
CREATE OR REPLACE VIEW public.attendance_summary
WITH (security_invoker = true) AS
SELECT
  s.id AS schedule_id,
  e.student_id,
  (
    (SELECT COUNT(*) FROM public.attendance_sessions ss
     WHERE ss.schedule_id = s.id)
    + COUNT(ar.id) FILTER (WHERE ar.session_id IS NULL)
  ) AS total_sesi,
  COUNT(ar.id) FILTER (WHERE ar.status = 'HADIR') AS hadir,
  COUNT(ar.id) FILTER (WHERE ar.status = 'IZIN') AS izin,
  CASE
    WHEN (
      (SELECT COUNT(*) FROM public.attendance_sessions ss
       WHERE ss.schedule_id = s.id)
      + COUNT(ar.id) FILTER (WHERE ar.session_id IS NULL)
    ) = 0 THEN 0
    ELSE (
      100 * COUNT(ar.id) FILTER (WHERE ar.status = 'HADIR')
      / (
        (SELECT COUNT(*) FROM public.attendance_sessions ss
         WHERE ss.schedule_id = s.id)
        + COUNT(ar.id) FILTER (WHERE ar.session_id IS NULL)
      )
    )
  END AS persen
FROM public.schedules s
JOIN public.enrollments e ON e.schedule_id = s.id
LEFT JOIN public.attendance_records ar
  ON ar.schedule_id = s.id AND ar.student_id = e.student_id
GROUP BY s.id, e.student_id;

GRANT SELECT ON public.attendance_summary TO authenticated;
