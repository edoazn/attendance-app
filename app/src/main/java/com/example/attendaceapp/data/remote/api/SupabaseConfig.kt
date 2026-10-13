package com.example.attendaceapp.data.remote.api

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
     * Anon key Supabase. Kosong sampai kredensial backend tersedia;
     * di-inject via konstruktor repository (jangan commit key ke repo).
     */
    const val ANON_KEY = ""
}
