package com.example.attendaceapp.data.remote.api

import com.example.attendaceapp.data.remote.request.AttendanceRequest
import com.example.attendaceapp.data.remote.request.LoginRequest
import com.example.attendaceapp.data.remote.response.AttendanceApiResponse
import com.example.attendaceapp.data.remote.response.LoginResponse
import com.example.attendaceapp.data.remote.response.ScheduleListResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {

    /** POST /login — publik, tidak butuh auth */
    @POST("login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    /**
     * POST /attendance — submit absensi mahasiswa.
     * Mendukung tiga metode: geolocation, qr_code, attendance_code.
     * Butuh Bearer token (gunakan ApiConfig.getAuthApiService).
     */
    @POST("attendance")
    suspend fun submitAttendance(@Body request: AttendanceRequest): Response<AttendanceApiResponse>

    /**
     * GET /schedules/today — ambil daftar jadwal hari ini untuk mahasiswa.
     * Response menyertakan flag is_active, has_qr, has_active_code.
     * Butuh Bearer token.
     */
    @GET("schedules/today")
    suspend fun getTodaySchedules(): Response<ScheduleListResponse>

    /**
     * GET /schedules — ambil semua jadwal mahasiswa (semua hari).
     * Digunakan oleh SchedulePage untuk menampilkan jadwal per hari.
     * Butuh Bearer token.
     */
    @GET("schedules")
    suspend fun getAllSchedules(): Response<ScheduleListResponse>
}