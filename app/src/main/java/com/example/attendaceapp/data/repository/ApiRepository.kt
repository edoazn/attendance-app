package com.example.attendaceapp.data.repository

import android.content.Context
import com.example.attendaceapp.data.local.UserPreferences
import com.example.attendaceapp.data.model.User
import com.example.attendaceapp.data.model.UserRole
import com.example.attendaceapp.data.remote.api.ApiConfig
import com.example.attendaceapp.data.remote.api.ApiService
import com.example.attendaceapp.data.remote.request.LoginRequest

class ApiRepository(
    private val apiService: ApiService = ApiConfig.getApiService(),
    private val context: Context? = null
) {

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
                // HTTP error (401, 422, 500, dll)
                val errorMsg = response.errorBody()?.string()
                Result.failure(Exception(errorMsg ?: "Error ${response.code()}"))
            }
        } catch (e: Exception) {
            // Network error, timeout, dll
            Result.failure(Exception("Tidak dapat terhubung ke server. Periksa koneksi internet."))
        }
    }


    // Hapus token & data sesi lokal (logout).
    fun clearSession() {
        context?.let { UserPreferences.getInstance(it).clear() }
    }
}
