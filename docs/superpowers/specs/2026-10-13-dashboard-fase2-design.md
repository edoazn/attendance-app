# Desain Dashboard Fase 2: Web Dosen + Admin

**Status:** DISETUJUI pemilik 2026-10-13 — siap dibuatkan implementation plan.

## Intent & Why

Backend Fase 1 sudah bisa (QR statis, ON/OFF sesi, submit HADIR-only,
scoping lecturer), tapi semua aksi dosen masih harus ditembak manual via
API. Di dunia nyata dosen bekerja dari laptop yang tersambung proyektor:
dia butuh satu layar untuk menyalakan sesi, menampilkan QR besar ke kelas,
dan melihat siapa yang sudah hadir secara live. Di sisi lain, semua data
yang tampil di HP mahasiswa (jadwal, kelas, ruangan, titik + radius
geofence) harus ada yang mengatur — hari ini hanya bisa lewat SQL/seed.
Dashboard web Fase 2 adalah setir dan ruang mesinnya: pusat kendali sesi
untuk dosen + pengatur master data untuk admin/TU.

## Goals

1. Dosen kelola sesi dari laptop: pilih jadwal miliknya → tombol ON/OFF →
   QR besar tampil (untuk proyektor) → daftar hadir masuk live
   (nama + NIM + jam + metode).
2. Dosen unduh QR fisik (PNG) per jadwal untuk ditempel di kelas.
3. Admin/TU kelola master data: user (dosen/mahasiswa), jadwal
   (matkul, dosen pengampu, ruangan, hari/jam, titik + radius geofence),
   enrollment paket-per-semester per rombel (sistem kampus bundling, tanpa
   KRS/pilih manual).
4. Rekap + export Excel per jadwal (pertemuan × mahasiswa, persen
   kehadiran) untuk keperluan nilai dan setoran ke TU.
5. Antrean approve izin (setuju/tolak) — membaca `permit_requests` yang
   kolom statusnya sudah disiapkan Fase 1.
6. Login dashboard pakai NIM + password via `login-with-nim` yang sudah
   ada; menu berbeda per peran (`profiles.role`: `ADMIN` baru + `LECTURER`).

## Non-goals

- Form pengajuan izin di HP mahasiswa (ditunda eksplisit oleh pemilik;
  antrean approve tetap dibangun, terisi nanti).
- Statistik home HP jadi data asli (ditunda; endpoint rekap yang dibangun
  untuk Excel dirancang agar bisa dipakai HP nanti tanpa ubahan).
- Absensi offline, QR per mahasiswa, ON/OFF otomatis.
- Integrasi API sisfo (sumber akun tetap input admin menyamai data sisfo).

## Anti-goals (gagal walau teknis jalan)

- Dosen bisa melihat/mengubah jadwal dosen lain.
- Absensi tercatat tanpa sesi ON.
- Dua token aktif untuk satu jadwal.
- Emoji di UI (aturan repo).

## Constraints

- Project Supabase yang sama; RLS tetap; tulis kritis via Edge Function
  yang sudah ada (`ensure-session-qr`, `set-session-state`,
  `submit-attendance`) — dashboard tidak menulis `attendance_*` langsung.
- Peran baru `ADMIN`: tambah nilai CHECK di `profiles.role` via migrasi
  (aman: hanya melonggarkan CHECK) + policy RLS baca-tulis master data
  untuk admin; lecturer tetap own+pool.
- Stack (DIPUTUSKAN pemilik di diskusi Fase 1): web sederhana
  Vite + React + TS + supabase-js; realtime via Supabase Realtime untuk
  daftar hadir live.
- Bahasa Indonesia; tanpa emoji; error generik anti-enumerasi.
- `qr_tokens.expires_at` basi setelah OFF (hanya di-refresh saat ON) —
  jangan tampilkan sebagai "berlaku sampai" di dashboard.

## Yang diputuskan (dari diskusi)

1. Laptop dosen = pusat kendali; HP dosen cadangan saja. (pemilik)
2. Dashboard sumber kebenaran master data (jadwal, kelas, ruangan,
   jarak/radius). (pemilik)
3. Dua peran: ADMIN/TU (master data + semua jadwal) dan LECTURER
   (jadwalnya + sesi). Akun dibuatkan PPM/sisfo, dashboard hanya
   mengelola salinannya; tanpa registrasi mandiri. (pemilik + my call)
4. Enrollment model paket/bundling per rombel per semester, tanpa pilih
   matkul manual. (pemilik)
5. Izin diajukan lewat aplikasi (form mobile menyusul); dashboard
   sediakan antrean approve. (pemilik)
6. Rekap cukup export Excel; kartu statistik home HP (hari ini dummy
   85/5/15) dihitung dari sumber rekap yang sama nanti. (pemilik)
7. Scope Fase 2 = dashboard saja; tanpa ubahan aplikasi mahasiswa.
   (pemilik)
8. Enrollment paket-per-rombel = kolom `rombel` di `profiles` + UI bulk
   enroll (tanpa tabel baru). (pemilik, putusan builder-check)
9. Dosen read-only atas master; wewenang tulisnya hanya sesi (ON/OFF).
   (pemilik, putusan builder-check)
10. Approve izin = Edge Function baru yang menulis baris
    `attendance_records` berstatus `IZIN` (`method='izin'`,
    `session_id` NULL); yang boleh approve dosen pemilik + admin.
    (pemilik, putusan builder-check)
11. Rumus rekap: persen = HADIR / total sesi; IZIN kolom tersendiri,
    tidak mengangkat persen. (pemilik, putusan builder-check)
12. Tambah user = Edge Function akun (bikin `auth.users` + `profiles`
    sekaligus); admin pertama via script setup interaktif sekali jalan.
    (pemilik, putusan builder-check)
13. Kode dashboard di repo terpisah (bukan folder di repo ini).
    (pemilik, putusan builder-check)
14. Hosting frontend: Cloudflare Pages atau Vercel (static host, deploy
    dari repo dashboard via Git; Supabase tetap backend saja). Dev jalan
    lokal via `npm run dev`. (pemilik)

## Disiapkan untuk builder (my call)

- Rekap dihitung dari `attendance_records` + `attendance_sessions` +
  izin (`status='IZIN'` kolom sendiri); Excel dibuat client-side (tanpa
  lib berat, mis. SheetJS — builder boleh ganti dengan alasan).
- Mobile stats nanti pakai endpoint/view rekap yang sama (tanpa ubah
  kontrak): sediakan view `attendance_summary` sekalian di migrasi peran.
- Cetak QR = unduh PNG dari token statis (string token mentah, sama
  seperti yang di-scan HP).
- Baris `IZIN` dari approve memakai `session_id` NULL (permintaan izin
  tidak terikat sesi spesifik) dan dihitung di kolom IZIN, bukan HADIR —
  ini yang mendamaikan putusan 10 + 11.
- `login-with-nim` jalan tanpa ubahan untuk `role='ADMIN'` (lookup
  NIM→email→signIn tidak peduli peran).
- Penyebut persen = jumlah baris `attendance_sessions` per jadwal; baris
  legacy `session_id IS NULL` (pra-Fase-1) dihitung 1 hadir + 1 penyebut.

## Diserahkan ke builder

- Struktur file dashboard, lib render QR + unduh PNG, teks UI persis.
- Kolom tabel daftar hadir live selain nama/NIM/jam/metode.
- Batas durasi default + opsi durasi di tombol ON (backend izinkan 1–120).
- Excel: satu sheet per jadwal atau satu file multi-sheet; kolom status
  izin tampil sebagai apa.
- Cara assign paket semester ke rombel (pilih rombel → centang paket,
  atau impor CSV) — selama model datanya paket-per-rombel.
