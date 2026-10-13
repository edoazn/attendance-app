package com.example.attendaceapp.data.remote.api

import com.example.attendaceapp.BuildConfig

/**
 * Konstanta terpusat untuk backend Supabase.
 *
 * Backend live BELUM di-deploy (menunggu kredensial) — jangan hardcode URL
 * production di repository; selalu lewat konstanta ini atau override lewat
 * parameter konstruktor [com.example.attendaceapp.data.repository.SupabaseRepository]
 * (dipakai unit test dengan MockWebServer).
 */
object SupabaseConfig {
    /** Project ref Supabase (default sampai backend live di-deploy). */
    const val PROJECT_REF = "takntldmevqfasenqdiq"

    /** Base URL project Supabase. */
    const val SUPABASE_URL = "https://takntldmevqfasenqdiq.supabase.co"

    /** Base URL Edge Functions (login-with-nim, submit-attendance, ...). */
    const val FUNCTIONS_URL = "$SUPABASE_URL/functions/v1"

    /** Base URL PostgREST (schedules, today_schedules_with_flags, ...). */
    const val REST_URL = "$SUPABASE_URL/rest/v1"

    /**
     * Anon key Supabase — di-inject saat build TANPA ubah kode dan TANPA
     * commit secret ke repo. Urutan sumber (lihat app/build.gradle.kts):
     * env `SUPABASE_ANON_KEY` → `supabase.anonKey` di `local.properties`
     * (jangan commit file bila berisi key) → default "" (mode tanpa
     * backend, sama seperti sebelumnya).
     */
    val ANON_KEY: String = BuildConfig.SUPABASE_ANON_KEY
}
