package com.example.japanesegrammarapp.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    private val client = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    // Base URL is dynamic so we just need a dummy base url to initialize Retrofit, 
    // we use @Url in the service interface to override it.
    val retrofit = Retrofit.Builder()
        .baseUrl("https://localhost/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val llmService: LlmApiService = retrofit.create(LlmApiService::class.java)
}
