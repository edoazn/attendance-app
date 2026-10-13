package com.example.attendaceapp.data.repository

import android.content.Context
import com.example.attendaceapp.data.local.UserPreferences
import com.example.attendaceapp.data.model.User
import com.example.attendaceapp.data.model.UserRole
import com.example.attendaceapp.data.remote.api.SupabaseConfig
import com.example.attendaceapp.data.remote.request.AttendanceRequest
import com.example.attendaceapp.data.remote.response.AttendanceData
import com.example.attendaceapp.data.remote.response.AttendanceDetail
import com.example.attendaceapp.data.remote.response.ScheduleDto
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Repository Supabase — pengganti [ApiRepository].
 *
 * Signature public sengaja identik [ApiRepository] agar ViewModel hanya
 * ganti konstruktor. DTO lama dipakai ulang ([User], [ScheduleDto],
 * [AttendanceData]); tidak ada DTO public baru.
 *
 * - Auth & submit absensi → Edge Functions Task 2:
 *   `POST login-with-nim {nim, password}` dan
 *   `POST submit-attendance {schedule_id, method, latitude?, longitude?,
 *   qr_token?, attendance_code?}`.
 * - Daftar jadwal → PostgREST ke view/tabel Task 1
 *   (`today_schedules_with_flags`, `schedules`).
 * - `expires_at`/`created_at` format ISO-8601 string dipakai verbatim
 *   (tidak pernah dikonversi dari/ke epoch).
 * - `status` uppercase `HADIR`/`TERLAMBAT`; `method` verbatim
 *   `geolocation`/`qr_code`/`attendance_code`.
 *
 * Base URL & anon key configurable via konstruktor (default
 * [SupabaseConfig] → project ref `takntldmevqfasenqdiq`); unit test
 * mengarahkan ke MockWebServer tanpa perlu backend asli.
 */
class SupabaseRepository(
    private val context: Context? = null,
    private val functionBaseUrl: String = SupabaseConfig.FUNCTIONS_URL,
    private val restBaseUrl: String = SupabaseConfig.REST_URL,
    private val anonKey: String = SupabaseConfig.ANON_KEY,
    private val sessionStore: SessionStore? = null,
    private val httpClient: OkHttpClient = OkHttpClient()
) {

    private val gson = Gson()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    // ─── Auth ────────────────────────────────────────────────────────────────

    /**
     * Login dengan NIM & password via Edge Function `login-with-nim`.
     * Token & data user otomatis disimpan (via [SessionStore] bila
     * tersedia, bila tidak via [UserPreferences] bila context tersedia).
     */
    suspend fun login(nim: String, password: String): Result<User> {
        if (nim.isBlank() || password.isBlank()) {
            return Result.failure(Exception("NIM dan kata sandi wajib diisi."))
        }
        return try {
            val payload = mapOf("nim" to nim, "password" to password)
            val response = postFunction(
                path = "login-with-nim",
                body = gson.toJson(payload),
                token = null
            )
            val rawBody = response.body?.string().orEmpty()
            if (response.isSuccessful) {
                val parsed = parseLoginBody(rawBody)
                    ?: return Result.failure(Exception("Data user tidak ditemukan"))
                persistSession(parsed.first, parsed.second)
                Result.success(parsed.second)
            } else {
                Result.failure(
                    Exception(errorMessageOf(rawBody, "NIM atau kata sandi salah."))
                )
            }
        } catch (e: Exception) {
            Result.failure(Exception("Tidak dapat terhubung ke server. Periksa koneksi internet."))
        }
    }

    // ─── Jadwal ──────────────────────────────────────────────────────────────

    /**
     * Ambil daftar jadwal hari ini dari view PostgREST
     * `today_schedules_with_flags` (kolom flag + kolom R1 Task 1).
     */
    suspend fun getTodaySchedules(): Result<List<ScheduleDto>> {
        val token = currentToken() ?: return Result.failure(
            Exception("Sesi login tidak ditemukan. Silakan login ulang.")
        )
        return try {
            val request = restRequest(
                url = "$restBaseUrl/today_schedules_with_flags?select=*",
                token = token
            )
            httpClient.newCall(request).execute().use { response ->
                val rawBody = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    val type = object : TypeToken<List<ScheduleDto>>() {}.type
                    val list: List<ScheduleDto> =
                        gson.fromJson(rawBody.ifBlank { "[]" }, type) ?: emptyList()
                    Result.success(list)
                } else {
                    Result.failure(
                        Exception(errorMessageOf(rawBody, "Gagal memuat jadwal"))
                    )
                }
            }
        } catch (e: Exception) {
            Result.failure(Exception("Tidak dapat terhubung ke server. Periksa koneksi internet."))
        }
    }

    /**
     * Ambil seluruh jadwal aktif mahasiswa dari tabel PostgREST `schedules`.
     * Digunakan oleh SchedulePage untuk filter per hari.
     */
    suspend fun getAllSchedules(): Result<List<ScheduleDto>> {
        val token = currentToken() ?: return Result.failure(
            Exception("Sesi login tidak ditemukan. Silakan login ulang.")
        )
        return try {
            val request = restRequest(
                url = "$restBaseUrl/schedules?select=*&is_active=eq.true",
                token = token
            )
            httpClient.newCall(request).execute().use { response ->
                val rawBody = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    val type = object : TypeToken<List<ScheduleDto>>() {}.type
                    val list: List<ScheduleDto> =
                        gson.fromJson(rawBody.ifBlank { "[]" }, type) ?: emptyList()
                    Result.success(list)
                } else {
                    Result.failure(
                        Exception(errorMessageOf(rawBody, "Gagal memuat jadwal"))
                    )
                }
            }
        } catch (e: Exception) {
            Result.failure(Exception("Tidak dapat terhubung ke server. Periksa koneksi internet."))
        }
    }

    // ─── Absensi ─────────────────────────────────────────────────────────────

    /** Submit absensi via geolocation. */
    suspend fun submitAttendanceGeolocation(
        scheduleId: Int,
        latitude: Double,
        longitude: Double
    ): Result<AttendanceData> = submitAttendance(
        scheduleId = scheduleId,
        method = AttendanceRequest.METHOD_GEOLOCATION,
        latitude = latitude,
        longitude = longitude
    )

    /**
     * Submit absensi via QR Code.
     * [qrToken] adalah UUID yang di-decode dari QR image.
     */
    suspend fun submitAttendanceQr(
        scheduleId: Int,
        qrToken: String
    ): Result<AttendanceData> = submitAttendance(
        scheduleId = scheduleId,
        method = AttendanceRequest.METHOD_QR_CODE,
        qrToken = qrToken
    )

    /** Submit absensi via kode manual 6-karakter. */
    suspend fun submitAttendanceCode(
        scheduleId: Int,
        attendanceCode: String
    ): Result<AttendanceData> = submitAttendance(
        scheduleId = scheduleId,
        method = AttendanceRequest.METHOD_ATTENDANCE_CODE,
        attendanceCode = attendanceCode
    )

    // ─── Session ─────────────────────────────────────────────────────────────

    /** Hapus token & data sesi lokal (logout). */
    fun clearSession() {
        sessionStore?.clear()
        context?.let { UserPreferences.getInstance(it).clear() }
    }

    // ─── Internal ────────────────────────────────────────────────────────────

    private fun submitAttendance(
        scheduleId: Int,
        method: String,
        latitude: Double? = null,
        longitude: Double? = null,
        qrToken: String? = null,
        attendanceCode: String? = null
    ): Result<AttendanceData> {
        val token = currentToken() ?: return Result.failure(
            Exception("Sesi login tidak ditemukan. Silakan login ulang.")
        )
        return try {
            val payload = mutableMapOf<String, Any?>(
                "schedule_id" to scheduleId,
                "method" to method
            )
            if (latitude != null) payload["latitude"] = latitude
            if (longitude != null) payload["longitude"] = longitude
            if (qrToken != null) payload["qr_token"] = qrToken
            if (attendanceCode != null) payload["attendance_code"] = attendanceCode

            val response = postFunction(
                path = "submit-attendance",
                body = gson.toJson(payload),
                token = token
            )
            val rawBody = response.body?.string().orEmpty()
            if (response.isSuccessful) {
                val data = parseAttendanceBody(rawBody)
                    ?: return Result.failure(Exception("Response data kosong"))
                Result.success(data)
            } else {
                // Duplicate dari backend = HTTP 409 dengan pesan Indonesia;
                // teruskan pesan server apa adanya.
                Result.failure(Exception(errorMessageOf(rawBody, "Absensi gagal")))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Tidak dapat terhubung ke server. Periksa koneksi internet."))
        }
    }

    /** Token sesi: dari SessionStore bila ada, bila tidak dari UserPreferences. */
    private fun currentToken(): String? {
        sessionStore?.getToken()?.takeIf { it.isNotBlank() }?.let { return it }
        return context?.let { UserPreferences.getInstance(it).authToken }
            ?.takeIf { it.isNotBlank() }
    }

    private fun persistSession(token: String, user: User) {
        sessionStore?.save(token, user)
        if (sessionStore == null || context != null) {
            context?.let { ctx ->
                val prefs = UserPreferences.getInstance(ctx)
                prefs.authToken = token
                prefs.userId = user.id
                prefs.userName = user.name
                prefs.userNim = user.nim
                prefs.userEmail = user.email
                prefs.userRole = user.role.name
            }
        }
    }

    private fun postFunction(
        path: String,
        body: String,
        token: String?
    ): okhttp3.Response {
        val builder = Request.Builder()
            .url("$functionBaseUrl/$path")
            .post(body.toRequestBody(jsonMediaType))
        if (anonKey.isNotBlank()) builder.addHeader("apikey", anonKey)
        val bearer = token?.takeIf { it.isNotBlank() } ?: anonKey.takeIf { it.isNotBlank() }
        if (!bearer.isNullOrBlank()) builder.addHeader("Authorization", "Bearer $bearer")
        return httpClient.newCall(builder.build()).execute()
    }

    private fun restRequest(url: String, token: String): Request {
        val builder = Request.Builder()
            .url(url)
            .get()
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Accept", "application/json")
        if (anonKey.isNotBlank()) builder.addHeader("apikey", anonKey)
        return builder.build()
    }

    /**
     * Parse respons datar Edge Function `login-with-nim`:
     * `{token, user:{id, identity_number, name, email, role}}`.
     * Return Pair(token, user) atau null bila tak lengkap.
     */
    private fun parseLoginBody(raw: String): Pair<String, User>? {
        val root = gson.fromJson(raw, JsonObject::class.java) ?: return null
        val token = root.get("token")?.takeIf { !it.isJsonNull }?.asString
            ?.takeIf { it.isNotBlank() } ?: return null
        val userObj = root.getAsJsonObject("user") ?: return null
        fun str(key: String): String =
            userObj.get(key)?.takeIf { !it.isJsonNull }?.asString ?: ""
        val user = User(
            id = str("id"),
            nim = str("identity_number"),
            name = str("name"),
            email = str("email"),
            role = when (str("role").uppercase()) {
                "LECTURER" -> UserRole.LECTURER
                else -> UserRole.STUDENT
            }
        )
        return token to user
    }

    /**
     * Parse respons datar Edge Function `submit-attendance`:
     * `{status, method, attendance:{id, status, method, created_at}}`.
     * `created_at` ISO-8601 dipakai verbatim (bukan epoch).
     */
    private fun parseAttendanceBody(raw: String): AttendanceData? {
        val root = gson.fromJson(raw, JsonObject::class.java) ?: return null
        fun JsonObject.str(key: String): String? =
            get(key)?.takeIf { !it.isJsonNull }?.asString
        val detailObj = root.getAsJsonObject("attendance")
        val detail = detailObj?.let {
            AttendanceDetail(
                id = it.get("id")?.takeIf { v -> !v.isJsonNull }?.asInt,
                status = it.str("status"),
                method = it.str("method"),
                createdAt = it.str("created_at")
            )
        }
        val status = root.str("status")
        val method = root.str("method")
        if (status == null || method == null || detail == null) return null
        return AttendanceData(
            status = status,
            distance = root.get("distance")?.takeIf { !it.isJsonNull }?.asDouble,
            method = method,
            attendance = detail
        )
    }

    /** Ambil pesan `{error|message}` dari body error; fallback pesan default. */
    private fun errorMessageOf(raw: String, fallback: String): String {
        if (raw.isBlank()) return fallback
        return try {
            val root = gson.fromJson(raw, JsonObject::class.java) ?: return fallback
            root.get("error")?.takeIf { !it.isJsonNull }?.asString
                ?: root.get("message")?.takeIf { !it.isJsonNull }?.asString
                ?: fallback
        } catch (e: Exception) {
            if (raw.length <= 300) raw else fallback
        }
    }
}
