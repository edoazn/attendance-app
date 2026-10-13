# Desain Alur Dosen: QR Statis + ON/OFF Manual

**Status:** jawaban user masuk 2026-10-13 — menunggu persetujuan akhir dokumen.

## Intent & Why

Kelas nyata: satu QR statis per jadwal, ditempel fisik di kelas + ditampilkan di proyektor buat mahasiswa yang jauh. Absensi dilakukan dosen **setelah** proses belajar mengajar, dengan menyalakan sesi secara manual. Desain ini mematikan pola "scan di awal waktu lalu cabut", karena QR hanya hidup saat dosen menghendaki.

## Goals

1. Satu token QR per jadwal (`schedule`), sama persis di fisik dan proyektor. Satu jadwal = satu absensi.
2. Dosen menyalakan/mematikan sesi manual (tombol ON/OFF). Submit mahasiswa diterima hanya saat ON.
3. Jendela ON pendek (default 10 menit, bisa ditutup manual lebih cepat) untuk membatasi penyebaran screenshot QR.
4. Alur mahasiswa di HP tidak berubah (scan QR / kode manual → submit).
5. Dosen punya permukaan kontrol + monitoring (lihat bawah).

## Non-goals

- ON/OFF otomatis ikut jam jadwal (ditolak eksplisit — membuka pola scan-lalu-cabut).
- QR per mahasiswa / QR berganti tiap pertemuan.
- Absensi offline.

## Anti-goals (gagal walau teknis jalan)

- Dua token untuk satu jadwal yang sama.
- Absensi tercatat tanpa aksi dosen sama sekali.
- Dosen harus generate QR baru tiap pertemuan (bebas tugas ini — QR permanen).

## Constraints

- Pakai tabel yang ada: `schedules.is_active` sebagai saklar ON/OFF; `qr_tokens`/`attendance_codes` tetap; RLS tetap; tulis via Edge Function (service role), bukan dari client.
- Zona waktu eksplisit `Asia/Jakarta` untuk semua penentuan status (warisan fix Task 2).
- String UI Bahasa Indonesia; **tanpa emoji di UI** (aturan repo); pesan error generik anti-enumerasi tetap.
- Token QR tidak pernah berubah per jadwal; yang berubah hanya status + expiry sesi.

## Yang diputuskan (dari diskusi)

1. Satu token QR per jadwal, tampil di fisik + proyektor. (user)
2. ON/OFF manual oleh dosen, karena absensi habis ngajar; otomatis-ditolak. (user)
3. Mitigasi screenshot (my call): jendela ON pendek default 10 menit + tombol tutup manual; wajib-lokasi (geofence) jadi **opsional per jadwal** (default mati agar alur HP tetap ringan), bukan syarat submit QR. Rasional: window pendek sudah membatasi penyebaran; geofence wajib menambah friksi izin lokasi untuk semua mahasiswa. Bisa dibalik nanti tanpa ubah skema.
4. Permukaan dosen (DIPUTUSKAN user 2026-10-13): **dashboard web** — admin/dosen
   memanage sistem dari web (pengganti dashboard admin di BE lama). Satu layar:
   QR besar + tombol ON/OFF + daftar hadir live. HP dosen cadangan saja.
   Stack: web sederhana (Vite + React + TS + supabase-js), project Supabase
   yang sama; RLS lecturer. Login web pakai NIM + password via
   `login-with-nim` yang sudah ada.
5. Scoping dosen naik jadi in-scope: tambah `schedules.lecturer_id` + persempit policy lecturer (utang review Task 1/2) — wajib agar dashboard dosen hanya lihat jadwalnya sendiri.

## Aturan status + bertahap (jawaban user 2026-10-13)

- **TERLAMBAT dihapus praktis:** absensi hanya mungkin saat sesi ON (habis
  ngajar, window pendek); semua submit yang diterima = `HADIR`. Kolom
  `late_threshold_minutes` dipertahankan tapi diabaikan (warisan, hapus di
  migrasi berikutnya bila yakin).
- **Fase 1 (build ini):** token QR statis per jadwal + tombol ON/OFF +
  daftar hadir live di dashboard + penyesuaian backend minimum
  (`lecturer_id`, `session_open/expires_at`, tabel `attendance_sessions`,
  rombak `set-session-state` + cabang QR `submit-attendance`,
  `permit_requests.status` disiapkan tapi UI approve menyusul).
- **Fase 2 (menyusul):** CRUD jadwal/user + approve izin + laporan/riwayat
  + cetak QR fisik (unduh PNG).

## Perubahan backend yang dibutuhkan (ringkas, detail ikut plan)

- Migrasi: `schedules.lecturer_id uuid`, `schedules.session_expires_at timestamptz`, token QR statis per jadwal (pola seed `aaaaaaaa-...` dipromosi jadi data nyata via function sekali-per-jadwal).
- Function `ensure-session-qr(schedule_id)` (sekali, idempoten): buatkan token permanen bila belum ada.
- Function sesi dirombak jadi `set-session-state(schedule_id, on|off, duration_minutes?)`: ON → `is_active=true` + `session_expires_at=now+durasi`; OFF → `is_active=false`. Guard lecturer + pemilik jadwal.
- `submit-attendance` cabang QR diubah: cocokkan token statis + wajib `is_active=true` + wajib dalam `session_expires_at`; auto-OFF saat lewat expiry. Kode manual + geolocation tetap.
- `attendance_codes`: tetap sebagai cadangan (kamera rusak), diaktifkan/dimatikan bareng sesi.

## Diserahkan ke builder

- Pilihan stack dashboard (bila disetujui), struktur file, lib render QR, nilai default durasi, teks UI persis, QR fisik dicetak dari dashboard (tombol unduh PNG) atau tidak.

## Resolusi builder-check (my call, 2026-10-13)

Hasil pemeriksaan builder independen; yang di bawah ini kuputuskan sendiri
(kecuali 3 hal di chat yang butuh putusan user):

- **Arsip vs sesi (is_active ganda):** TIDAK reuse `is_active`. `is_active` tetap = jadwal aktif/arsip; tambah `schedules.session_open bool DEFAULT false` + `schedules.session_expires_at timestamptz`. View + guard arsip tak berubah makna.
- **Token statis:** `qr_tokens` satu baris per jadwal (`UNIQUE(schedule_id)`); `expires_at` per baris = expiry sesi terakhir (di-refresh tiap ON). Kode manual: permanen per jadwal, `is_active`-nya mirror `session_open`.
- **Expiry + auto-OFF:** sumber tunggal `schedules.session_expires_at`. Auto-OFF malas (lazy): submit yang datang setelah expiry ditolak + flag `session_open` dimatikan dalam transaksi yang sama — deterministik di batas waktu.
- **Idempotensi set-session-state:** ON saat ON → perpanjang expiry (return sama); OFF saat OFF → no-op sukses; guard pemilik-jadwal; `lecturer_id` null → semua lecturer boleh (pool belum di-assign).
- **Backfill lecturer_id:** seed mapping nama→UUID dosen; null = pool unassigned, terlihat semua lecturer, bisa di-assign via dashboard. RLS: lecturer lihat miliknya + pool null.
- **Format QR:** string token mentah (sama seperti sekarang) — client Android tak berubah.
- **Absensi mingguan berulang:** tambah sesi instans — tabel `attendance_sessions(id, schedule_id, opened_at, closed_at)`; `attendance_records` FK ke sesi + UNIQUE(session_id, student_id) ganti UNIQUE lama. Tanpa ini minggu kedua 409 massal.
- **Live dashboard:** Supabase Realtime untuk daftar hadir; tambah `permit_requests.status` (pending/approved/rejected) untuk approve.
- **Geofence:** konvensi lama dipertahankan — lat/long null = mati; submit QR cek lokasi hanya bila jadwal punya koordinat.
