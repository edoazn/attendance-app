package com.example.attendaceapp.data.remote.api

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiConfig {
    private const val BASE_URL = "https://meng.my.id/api/v1/"

    /** Retrofit tanpa auth — untuk endpoint publik seperti login. */
    fun getApiService(): ApiService = buildRetrofit().create(ApiService::class.java)

    /**
     * Retrofit dengan Bearer token — untuk endpoint yang butuh autentikasi.
     * Gunakan ini setelah user login.
     */
    fun getAuthApiService(token: String): ApiService {
        return buildRetrofit(authToken = token).create(ApiService::class.java)
    }

    private fun buildRetrofit(authToken: String? = null): Retrofit {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val clientBuilder = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)

        // Tambah Authorization header bila token tersedia
        if (!authToken.isNullOrBlank()) {
            val authInterceptor = Interceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer $authToken")
                    .build()
                chain.proceed(request)
            }
            clientBuilder.addInterceptor(authInterceptor)
        }

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(clientBuilder.build())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}