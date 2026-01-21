package com.example.client.api

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    // 👇 LINK SERVER CỦA AN (Đừng dùng localhost)
    private const val BASE_URL = "http://10.0.2.2:3000/"

    // 👇 1. Cấu hình bộ đếm giờ (Timeout)
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS) // Chờ kết nối 60s
        .readTimeout(60, TimeUnit.SECONDS)    // Chờ đọc dữ liệu 60s
        .writeTimeout(60, TimeUnit.SECONDS)   // Chờ gửi dữ liệu 60s
        .build()

    val instance: AuthService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient) // 👈 2. Gắn bộ đếm giờ vào đây
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AuthService::class.java)
    }
}