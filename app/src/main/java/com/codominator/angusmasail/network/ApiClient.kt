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
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    // Set to true for fast local development (runs against your local IntelliJ backend)
    // Set to false for remote production deployment (https://angus.iamshahid.pro)
    const val USE_LOCAL_DEV = true

    const val PROD_URL = "https://angus.iamshahid.pro/"
    const val LOCAL_URL = "http://127.0.0.1:8080/"

    val BASE_URL: String
        get() = if (USE_LOCAL_DEV) LOCAL_URL else PROD_URL

    fun resolveMediaUrl(path: String?): String? {
        if (path.isNullOrBlank()) return null
        if (path.startsWith("http://") || path.startsWith("https://")) return path
        val cleanPath = if (path.startsWith("/")) path else "/$path"
        val base = if (USE_LOCAL_DEV) LOCAL_URL.trimEnd('/') else PROD_URL.trimEnd('/')
        return "$base$cleanPath"
    }

    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    fun parseApiErrorMessage(raw: String?, fallback: String = "An error occurred"): String {
        if (raw.isNullOrBlank()) return fallback
        val trimmed = raw.trim()
        if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) {
            return trimmed
        }
        return try {
            val json = org.json.JSONObject(trimmed)
            when {
                json.has("message") && json.optString("message").isNotBlank() -> json.optString("message")
                json.has("error") && json.optString("error").isNotBlank() -> json.optString("error")
                json.has("detail") && json.optString("detail").isNotBlank() -> json.optString("detail")
                else -> fallback
            }
        } catch (_: Exception) {
            trimmed
        }
    }

    fun <T> extractUserFriendlyError(res: retrofit2.Response<T>, fallback: String = "Request could not be completed"): String {
        val raw = try {
            res.errorBody()?.string()?.trim()
        } catch (_: Exception) {
            null
        }
        return parseApiErrorMessage(raw, fallback)
    }
}