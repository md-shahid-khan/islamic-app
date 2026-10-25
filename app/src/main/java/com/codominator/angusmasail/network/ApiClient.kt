package com.codominator.angusmasail.network

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

object ApiClient {

    private val failoverInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        val originalUrl = originalRequest.url

        val token = TokenManager.getToken()
        val requestWithAuth = originalRequest.newBuilder().apply {
            if (token != null) {
                addHeader("Authorization", "Bearer $token")
            }
        }.build()

        try {
            chain.proceed(requestWithAuth)
        } catch (e: IOException) {
            // Fallback between production domain and local IP if offline
            val altHost = when (originalUrl.host) {
                "angus.iamshahid.pro" -> "10.0.2.2"
                "10.0.2.2", "127.0.0.1", "localhost" -> "angus.iamshahid.pro"
                else -> null
            }

            if (altHost != null && altHost != originalUrl.host) {
                val newUrl = originalUrl.newBuilder().host(altHost).build()
                val retryRequest = requestWithAuth.newBuilder().url(newUrl).build()
                chain.proceed(retryRequest)
            } else {
                throw e
            }
        }
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(failoverInterceptor)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    const val BASE_URL = "https://angus.iamshahid.pro"

    fun resolveMediaUrl(path: String?): String? {
        if (path.isNullOrBlank()) return null
        if (path.startsWith("http://") || path.startsWith("https://")) return path
        val cleanPath = if (path.startsWith("/")) path else "/$path"
        return "$BASE_URL$cleanPath"
    }

    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
