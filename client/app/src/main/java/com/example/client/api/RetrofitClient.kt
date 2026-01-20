package com.example.client.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    // 👇 LINK SERVER CỦA AN (Đừng dùng localhost)
    private const val BASE_URL = "https://chat-app-0dv5.onrender.com/"

    val instance: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}