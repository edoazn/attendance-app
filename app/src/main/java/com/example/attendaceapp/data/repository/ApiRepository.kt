package com.example.attendaceapp.data.repository

import android.content.Context
import com.example.attendaceapp.data.local.UserPreferences
import com.example.attendaceapp.data.model.User
import com.example.attendaceapp.data.model.UserRole
import com.example.attendaceapp.data.remote.api.ApiConfig
import com.example.attendaceapp.data.remote.api.ApiService
import com.example.attendaceapp.data.remote.request.AttendanceRequest
import com.example.attendaceapp.data.remote.request.LoginRequest
import com.example.attendaceapp.data.remote.response.AttendanceData
import com.example.attendaceapp.data.remote.response.ScheduleDto

class ApiRepository(
    private val apiService: ApiService = ApiConfig.getApiService(),
    private val context: Context? = null
) {

    // ─── Auth ──────────────────────────────────────────────────────────────────

    /**
     * Login dengan NIM & password.
     * Token & data user otomatis disimpan ke UserPreferences bila context tersedia.
     */
    suspend fun login(nim: String, password: String): Result<User> {
        return try {
            val response = apiService.login(LoginRequest(nim, password))

            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true) {
                    val loginData = body.data
                    val userDto = loginData?.user

                    if (loginData != null && userDto != null) {
                        val user = User(
                            id = userDto.id?.toString() ?: "",
                            nim = userDto.identityNumber ?: "",
                            name = userDto.name ?: "",
                            email = userDto.email ?: "",
                            role = when (userDto.role?.uppercase()) {
                                "LECTURER" -> UserRole.LECTURER
                                else -> UserRole.STUDENT
                            }
                        )

                        // Simpan token & info user ke local preferences
                        context?.let { ctx ->
                            val prefs = UserPreferences.getInstance(ctx)
                            prefs.authToken = loginData.token
                            prefs.userId = user.id
                            prefs.userName = user.name
                            prefs.userNim = user.nim
                            prefs.userEmail = user.email
                            prefs.userRole = user.role.name
                        }

                        Result.success(user)
                    } else {
                        Result.failure(Exception("Data user tidak ditemukan"))
                    }
                } else {
                    Result.failure(Exception(body?.message ?: "Login gagal"))
                }
            } else {
                val errorMsg = response.errorBody()?.string()
                Result.failure(Exception(errorMsg ?: "Error ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Tidak dapat terhubung ke server. Periksa koneksi internet."))
        }
    }

    // ─── Jadwal ────────────────────────────────────────────────────────────────

    /**
     * Ambil daftar jadwal hari ini untuk mahasiswa.
     * Butuh token — pastikan context tersedia agar token bisa dibaca.
     */
    suspend fun getTodaySchedules(): Result<List<ScheduleDto>> {
        val authService = buildAuthService() ?: return Result.failure(
            Exception("Sesi login tidak ditemukan. Silakan login ulang.")
        )
        return try {
            val response = authService.getTodaySchedules()
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()?.data ?: emptyList())
            } else {
                val msg = response.body()?.message ?: response.errorBody()?.string()
                Result.failure(Exception(msg ?: "Gagal memuat jadwal"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Tidak dapat terhubung ke server. Periksa koneksi internet."))
        }
    }

    /**
     * Ambil seluruh jadwal mahasiswa (semua hari).
     * Digunakan oleh SchedulePage untuk filter per hari.
     */
    suspend fun getAllSchedules(): Result<List<ScheduleDto>> {
        val authService = buildAuthService() ?: return Result.failure(
            Exception("Sesi login tidak ditemukan. Silakan login ulang.")
        )
        return try {
            val response = authService.getAllSchedules()
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()?.data ?: emptyList())
            } else {
                val msg = response.body()?.message ?: response.errorBody()?.string()
                Result.failure(Exception(msg ?: "Gagal memuat jadwal"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Tidak dapat terhubung ke server. Periksa koneksi internet."))
        }
    }

    // ─── Absensi ───────────────────────────────────────────────────────────────

    /**
     * Submit absensi via geolocation.
     */
    suspend fun submitAttendanceGeolocation(
        scheduleId: Int,
        latitude: Double,
        longitude: Double
    ): Result<AttendanceData> = submitAttendance(
        AttendanceRequest(
            scheduleId = scheduleId,
            method = AttendanceRequest.METHOD_GEOLOCATION,
            latitude = latitude,
            longitude = longitude
        )
    )

    /**
     * Submit absensi via QR Code.
     * [qrToken] adalah UUID yang di-decode dari QR image.
     */
    suspend fun submitAttendanceQr(
        scheduleId: Int,
        qrToken: String
    ): Result<AttendanceData> = submitAttendance(
        AttendanceRequest(
            scheduleId = scheduleId,
            method = AttendanceRequest.METHOD_QR_CODE,
            qrToken = qrToken
        )
    )

    /**
     * Submit absensi via kode manual 6-karakter.
     */
    suspend fun submitAttendanceCode(
        scheduleId: Int,
        attendanceCode: String
    ): Result<AttendanceData> = submitAttendance(
        AttendanceRequest(
            scheduleId = scheduleId,
            method = AttendanceRequest.METHOD_ATTENDANCE_CODE,
            attendanceCode = attendanceCode
        )
    )

    // ─── Internal ──────────────────────────────────────────────────────────────

    private suspend fun submitAttendance(request: AttendanceRequest): Result<AttendanceData> {
        val authService = buildAuthService() ?: return Result.failure(
            Exception("Sesi login tidak ditemukan. Silakan login ulang.")
        )
        return try {
            val response = authService.submitAttendance(request)
            if (response.isSuccessful && response.body()?.success == true) {
                val data = response.body()?.data
                if (data != null) {
                    Result.success(data)
                } else {
                    Result.failure(Exception("Response data kosong"))
                }
            } else {
                val msg = response.body()?.message ?: response.errorBody()?.string()
                Result.failure(Exception(msg ?: "Absensi gagal (${response.code()})"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Tidak dapat terhubung ke server. Periksa koneksi internet."))
        }
    }

    /** Buat ApiService bertokens. Return null jika token tidak tersedia. */
    private fun buildAuthService(): ApiService? {
        val token = context?.let { UserPreferences.getInstance(it).authToken }
        return if (!token.isNullOrBlank()) {
            ApiConfig.getAuthApiService(token)
        } else null
    }

    // ─── Session ───────────────────────────────────────────────────────────────

    /** Hapus token & data sesi lokal (logout). */
    fun clearSession() {
        context?.let { UserPreferences.getInstance(it).clear() }
    }
}
