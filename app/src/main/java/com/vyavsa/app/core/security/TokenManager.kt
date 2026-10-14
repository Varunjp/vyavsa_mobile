package com.vyavsa.app.core.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.vyavsa.app.core.model.TokenResponse
import com.vyavsa.app.core.model.UserProfile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

sealed interface SessionState {
    object Unauthenticated : SessionState
    data class Authenticated(val user: UserProfile) : SessionState
}

@Singleton
class TokenManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val json = Json { ignoreUnknownKeys = true }

    private val prefs: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                "vyavsa_secure_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            // Fallback to standard private mode preferences if hardware keystore error occurs
            context.getSharedPreferences("vyavsa_fallback_prefs", Context.MODE_PRIVATE)
        }
    }

    private val _sessionState = MutableStateFlow<SessionState>(SessionState.Unauthenticated)
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    init {
        restoreSession()
    }

    fun restoreSession() {
        val accessToken = prefs.getString(KEY_ACCESS_TOKEN, null)
        val userJson = prefs.getString(KEY_USER_PROFILE, null)
        if (!accessToken.isNullOrBlank() && !userJson.isNullOrBlank()) {
            try {
                val user = json.decodeFromString<UserProfile>(userJson)
                _sessionState.value = SessionState.Authenticated(user)
            } catch (e: Exception) {
                _sessionState.value = SessionState.Unauthenticated
            }
        } else {
            _sessionState.value = SessionState.Unauthenticated
        }
    }

    fun saveSession(tokenResponse: TokenResponse) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, tokenResponse.accessToken)
            .putString(KEY_REFRESH_TOKEN, tokenResponse.refreshToken)
            .putString(KEY_USER_PROFILE, json.encodeToString(tokenResponse.user))
            .apply()
        _sessionState.value = SessionState.Authenticated(tokenResponse.user)
    }

    fun updateTokens(accessToken: String, refreshToken: String) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .apply()
    }

    fun getAccessToken(): String? = prefs.getString(KEY_ACCESS_TOKEN, null)

    fun getRefreshToken(): String? = prefs.getString(KEY_REFRESH_TOKEN, null)

    fun getUserProfile(): UserProfile? {
        val userJson = prefs.getString(KEY_USER_PROFILE, null) ?: return null
        return try {
            json.decodeFromString<UserProfile>(userJson)
        } catch (e: Exception) {
            null
        }
    }

    fun isTenantAdmin(): Boolean {
        return getUserProfile()?.role == "tenant_admin" || getUserProfile()?.role == "admin"
    }

    fun clearSession() {
        prefs.edit().clear().apply()
        _sessionState.value = SessionState.Unauthenticated
    }

    companion object {
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_USER_PROFILE = "user_profile"
    }
}
