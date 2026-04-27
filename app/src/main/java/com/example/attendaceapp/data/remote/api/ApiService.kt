package com.example.attendaceapp.data.remote.api

import com.example.attendaceapp.data.remote.request.LoginRequest
import com.example.attendaceapp.data.remote.response.LoginResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>
}