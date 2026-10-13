// Script setup ADMIN pertama (fase-2, sekali jalan, interaktif).
// Dipakai karena function create-user butuh JWT admin yang belum ada
// (deadlock bootstrap) — script ini memakai SERVICE ROLE langsung.
//
// Jalankan: deno run -A supabase/scripts/setup-admin.ts
// Env wajib: SUPABASE_URL, SUPABASE_SERVICE_ROLE_KEY.
// Idempoten: bila ADMIN sudah ada, info + exit 0 tanpa menulis.

import { createClient } from "jsr:@supabase/supabase-js@2";

const EMAIL_DOMAIN = "kampus.ac.id";
const MIN_PASSWORD_LENGTH = 6;

function fail(message: string): never {
  console.error(message);
  Deno.exit(1);
}

async function readLine(): Promise<string> {
  const bytes: number[] = [];
  const one = new Uint8Array(1);
  while (true) {
    const n = await Deno.stdin.read(one);
    if (n === null || one[0] === 10) break;
    if (one[0] === 13) continue;
    bytes.push(one[0]);
  }
  return new TextDecoder().decode(new Uint8Array(bytes)).trim();
}

async function prompt(text: string): Promise<string> {
  await Deno.stdout.write(new TextEncoder().encode(`${text}: `));
  return readLine();
}

const supabaseUrl = Deno.env.get("SUPABASE_URL") ?? "";
const serviceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "";
if (!supabaseUrl || !serviceKey) {
  fail(
    "Lingkungan belum dikonfigurasi: isi SUPABASE_URL dan SUPABASE_SERVICE_ROLE_KEY dulu.",
  );
}

const admin = createClient(supabaseUrl, serviceKey);

// Guard idempoten: ADMIN sudah ada -> selesai tanpa menulis.
const { data: existing, error: checkError } = await admin
  .from("profiles")
  .select("identity_number")
  .eq("role", "ADMIN")
  .limit(1)
  .maybeSingle();
if (checkError) fail(`Gagal memeriksa admin: ${checkError.message}`);
if (existing) {
  console.log(
    `Admin sudah ada (NIM ${(existing as { identity_number: string }).identity_number}). Tidak ada yang ditulis.`,
  );
  Deno.exit(0);
}

const nim = await prompt("NIM admin");
const name = await prompt("Nama admin");
const password = await prompt("Kata sandi (min 6 karakter)");
if (!nim) fail("Nomor identitas tidak valid.");
if (!name) fail("Nama tidak valid.");
if (password.length < MIN_PASSWORD_LENGTH) {
  fail(`Kata sandi minimal ${MIN_PASSWORD_LENGTH} karakter.`);
}
const email = `${nim}@${EMAIL_DOMAIN}`;

const { data: created, error: createError } = await admin.auth.admin
  .createUser({ email, password, email_confirm: true });
if (createError || !created.user) {
  fail(`Data pengguna tidak dapat dibuat: ${createError?.message ?? "unknown"}`);
}

const { error: profileError } = await admin.from("profiles").insert({
  id: (created as { user: { id: string } }).user.id,
  identity_number: nim,
  name,
  email,
  role: "ADMIN",
  rombel: null,
});
if (profileError) {
  await admin.auth.admin.deleteUser(
    (created as { user: { id: string } }).user.id,
  );
  fail(`Data pengguna tidak dapat dibuat: ${profileError.message}`);
}

console.log(`Admin ${nim} berhasil dibuat. Silakan login di dashboard.`);
