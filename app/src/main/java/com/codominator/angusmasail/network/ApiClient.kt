package com.codominator.angusmasail.network

import com.codominator.angusmasail.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

object ApiClient {

    private val authInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        val token = TokenManager.getToken()
        val requestWithAuth = originalRequest.newBuilder().apply {
            if (!token.isNullOrBlank()) {
                addHeader("Authorization", "Bearer $token")
            }
        }.build()
        chain.proceed(requestWithAuth)
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // Trust manager to support all standard and custom certificates smoothly
    private val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
        override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
        override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
        override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
    })

    private val sslContext = try {
        SSLContext.getInstance("TLS").apply {
            init(null, trustAllCerts, SecureRandom())
        }
    } catch (_: Exception) {
        null
    }

    private val clientBuilder: OkHttpClient.Builder = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .writeTimeout(25, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .apply {
            sslContext?.let {
                sslSocketFactory(it.socketFactory, trustAllCerts[0] as X509TrustManager)
                hostnameVerifier { _, _ -> true }
            }
        }

    val client: OkHttpClient = clientBuilder.build()

    // Environment-aware configuration:
    val PROD_URL: String get() = BuildConfig.PROD_URL
    val LOCAL_URL: String get() = BuildConfig.DEV_URL

    // Optional runtime override
    var customBaseUrl: String? = null

    val BASE_URL: String
        get() {
            val url = customBaseUrl ?: BuildConfig.BASE_URL
            return if (url.endsWith("/")) url else "$url/"
        }

    fun resolveMediaUrl(path: String?): String? {
        if (path.isNullOrBlank()) return null
        if (path.startsWith("http://") || path.startsWith("https://")) return path
        val cleanPath = if (path.startsWith("/")) path else "/$path"
        val base = BASE_URL.trimEnd('/')
        return "$base$cleanPath"
    }

    @Volatile
    private var cachedBaseUrl: String? = null
    @Volatile
    private var cachedApiService: ApiService? = null

    val api: ApiService
        get() {
            val currentBase = BASE_URL
            if (cachedApiService == null || cachedBaseUrl != currentBase) {
                synchronized(this) {
                    if (cachedApiService == null || cachedBaseUrl != currentBase) {
                        cachedBaseUrl = currentBase
                        val retrofit = Retrofit.Builder()
                            .baseUrl(currentBase)
                            .client(client)
                            .addConverterFactory(GsonConverterFactory.create())
                            .build()
                        cachedApiService = retrofit.create(ApiService::class.java)
                    }
                }
            }
            return cachedApiService!!
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
