package com.vyavsa.app.core.security

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Immutable
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

@Immutable
sealed interface SessionState {
    object Unauthenticated : SessionState
    data class Authenticated(val user: UserProfile) : SessionState
}

@Singleton
open class TokenManager(
    private val context: Context? = null,
    private val customPrefs: SharedPreferences? = null
) {
    @Inject
    constructor(@ApplicationContext context: Context) : this(context, null)

    private val json = Json { ignoreUnknownKeys = true }

    private val prefs: SharedPreferences? by lazy {
        if (customPrefs != null) return@lazy customPrefs
        val ctx = context ?: return@lazy null
        try {
            val masterKey = MasterKey.Builder(ctx)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                ctx,
                "vyavsa_secure_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            // Fallback to standard private mode preferences if hardware keystore error occurs
            ctx.getSharedPreferences("vyavsa_fallback_prefs", Context.MODE_PRIVATE)
        }
    }

    // In-memory cache to avoid main-thread EncryptedSharedPreferences decryption and JSON decoding during UI recompositions
    @Volatile
    private var cachedAccessToken: String? = null

    @Volatile
    private var cachedRefreshToken: String? = null

    @Volatile
    private var cachedUserProfile: UserProfile? = null

    private val _sessionState = MutableStateFlow<SessionState>(SessionState.Unauthenticated)
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    init {
        restoreSession()
    }

    open fun restoreSession() {
        val p = prefs ?: return
        val accessToken = p.getString(KEY_ACCESS_TOKEN, null)
        val refreshToken = p.getString(KEY_REFRESH_TOKEN, null)
        val userJson = p.getString(KEY_USER_PROFILE, null)
        cachedAccessToken = accessToken
        cachedRefreshToken = refreshToken

        if (!accessToken.isNullOrBlank() && !userJson.isNullOrBlank()) {
            try {
                val user = json.decodeFromString<UserProfile>(userJson)
                cachedUserProfile = user
                _sessionState.value = SessionState.Authenticated(user)
            } catch (e: Exception) {
                cachedUserProfile = null
                _sessionState.value = SessionState.Unauthenticated
            }
        } else {
            cachedUserProfile = null
            _sessionState.value = SessionState.Unauthenticated
        }
    }

    open fun saveSession(tokenResponse: TokenResponse) {
        cachedAccessToken = tokenResponse.accessToken
        cachedRefreshToken = tokenResponse.refreshToken
        cachedUserProfile = tokenResponse.user
        prefs?.edit()
            ?.putString(KEY_ACCESS_TOKEN, tokenResponse.accessToken)
            ?.putString(KEY_REFRESH_TOKEN, tokenResponse.refreshToken)
            ?.putString(KEY_USER_PROFILE, json.encodeToString(tokenResponse.user))
            ?.apply()
        _sessionState.value = SessionState.Authenticated(tokenResponse.user)
    }

    open fun updateTokens(accessToken: String, refreshToken: String) {
        cachedAccessToken = accessToken
        cachedRefreshToken = refreshToken
        prefs?.edit()
            ?.putString(KEY_ACCESS_TOKEN, accessToken)
            ?.putString(KEY_REFRESH_TOKEN, refreshToken)
            ?.apply()
    }

    open fun getAccessToken(): String? = cachedAccessToken ?: prefs?.getString(KEY_ACCESS_TOKEN, null)?.also {
        cachedAccessToken = it
    }

    open fun getRefreshToken(): String? = cachedRefreshToken ?: prefs?.getString(KEY_REFRESH_TOKEN, null)?.also {
        cachedRefreshToken = it
    }

    open fun getUserProfile(): UserProfile? {
        cachedUserProfile?.let { return it }
        val userJson = prefs?.getString(KEY_USER_PROFILE, null) ?: return null
        return try {
            json.decodeFromString<UserProfile>(userJson).also {
                cachedUserProfile = it
            }
        } catch (e: Exception) {
            null
        }
    }

    open fun getUserRole(): com.vyavsa.app.core.model.UserRole {
        return getUserProfile()?.userRole ?: com.vyavsa.app.core.model.UserRole.ADMIN
    }

    open fun isTenantAdmin(): Boolean {
        val role = getUserRole()
        val rawRole = getUserProfile()?.role
        return role == com.vyavsa.app.core.model.UserRole.ADMIN || rawRole == "tenant_admin" || rawRole == "admin"
    }

    open fun isSales(): Boolean = getUserRole() == com.vyavsa.app.core.model.UserRole.SALES || getUserRole() == com.vyavsa.app.core.model.UserRole.LINE

    open fun isLine(): Boolean = getUserRole() == com.vyavsa.app.core.model.UserRole.LINE

    open fun isOffice(): Boolean = getUserRole() == com.vyavsa.app.core.model.UserRole.OFFICE

    open fun isAdminOrUser(): Boolean {
        val role = getUserRole()
        return role == com.vyavsa.app.core.model.UserRole.ADMIN || role == com.vyavsa.app.core.model.UserRole.USER
    }

    open fun getPermissions(): Set<AppPermission> {
        return AppPermission.permissionsForRole(getUserRole())
    }

    open fun hasPermission(permission: AppPermission): Boolean {
        return getPermissions().contains(permission)
    }

    open fun canAccessLineSales(): Boolean = hasPermission(AppPermission.CREATE_LINE_SALE)

    open fun canAccessCounterSales(): Boolean = hasPermission(AppPermission.CREATE_COUNTER_SALE)

    open fun canAccessPurchases(): Boolean = isTenantAdmin() || isOffice()

    open fun canAccessExpenses(): Boolean = isTenantAdmin() || isOffice()

    open fun canAccessStaff(): Boolean = hasPermission(AppPermission.MANAGE_STAFF)

    open fun canAccessTenantManagement(): Boolean = isTenantAdmin()

    open fun clearSession() {
        cachedAccessToken = null
        cachedRefreshToken = null
        cachedUserProfile = null
        prefs?.edit()?.clear()?.apply()
        _sessionState.value = SessionState.Unauthenticated
    }

    companion object {
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_USER_PROFILE = "user_profile"
    }
}
