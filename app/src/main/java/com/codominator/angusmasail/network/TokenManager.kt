package com.codominator.angusmasail.network

import android.content.Context
import android.os.Build

object TokenManager {
    private const val PREFS_NAME = "angus_prefs"
    private const val TOKEN_KEY = "jwt_token"
    private const val IMAM_ID_KEY = "selected_imam_profile_id"
    private const val BASE_URL_KEY = "base_url"

    private var token: String? = null
    private var imamProfileId: String? = null
    private var customBaseUrl: String? = null

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        token = prefs.getString(TOKEN_KEY, null)
        imamProfileId = prefs.getString(IMAM_ID_KEY, null)
        customBaseUrl = prefs.getString(BASE_URL_KEY, null)
    }

    fun saveToken(context: Context, newToken: String) {
        token = newToken
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(TOKEN_KEY, newToken)
            .apply()
    }

    fun getToken(): String? = token

    fun saveImamProfileId(context: Context, id: String) {
        imamProfileId = id
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(IMAM_ID_KEY, id)
            .apply()
    }

    fun getImamProfileId(): String? = imamProfileId

    fun saveBaseUrl(context: Context, url: String) {
        customBaseUrl = url
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(BASE_URL_KEY, url)
            .apply()
    }

    fun getBaseUrl(): String {
        customBaseUrl?.let { return it }

        val isEmulator = (Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86"))

        return if (isEmulator) {
            "http://10.0.2.2:8080"
        } else {
            "http://192.168.8.34:8080"
        }
    }
}
