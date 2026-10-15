package com.vyavsa.app.core.network

import com.vyavsa.app.core.model.ApiResponse
import com.vyavsa.app.core.model.RefreshTokenRequest
import com.vyavsa.app.core.model.TokenResponse
import com.vyavsa.app.core.security.TokenManager
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.Authenticator
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
class TokenAuthenticator @Inject constructor(
    private val tokenManager: TokenManager,
    private val okHttpClientProvider: Provider<OkHttpClient>
) : Authenticator {

    private val json = Json { ignoreUnknownKeys = true }
    private val refreshLock = Any()

    override fun authenticate(route: Route?, response: Response): Request? {
        // Prevent infinite loops if retry also failed
        if (responseCount(response) >= 2) {
            return null
        }

        val currentRefreshToken = tokenManager.getRefreshToken() ?: run {
            tokenManager.clearSession()
            return null
        }

        synchronized(refreshLock) {
            val currentAccessToken = tokenManager.getAccessToken()
            val requestToken = response.request.header("Authorization")?.removePrefix("Bearer ")

            // If another thread already refreshed the token successfully, retry with new token
            if (currentAccessToken != null && currentAccessToken != requestToken) {
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $currentAccessToken")
                    .build()
            }

            // Perform synchronous refresh request
            val baseUrl = response.request.url.scheme + "://" + response.request.url.host +
                    if (response.request.url.port != 80 && response.request.url.port != 443) {
                        ":${response.request.url.port}"
                    } else ""

            val refreshUrl = "$baseUrl/api/v1/auth/refresh"
            val requestBody = json.encodeToString(RefreshTokenRequest(refreshToken = currentRefreshToken))
                .toRequestBody("application/json; charset=utf-8".toMediaType())

            val refreshRequest = Request.Builder()
                .url(refreshUrl)
                .post(requestBody)
                .build()

            try {
                // Use dedicated client without authenticator to prevent recursive loops
                val client = okHttpClientProvider.get().newBuilder()
                    .authenticator(Authenticator.NONE)
                    .build()

                val refreshResponse = client.newCall(refreshRequest).execute()
                if (refreshResponse.isSuccessful) {
                    val responseBodyString = refreshResponse.body?.string()
                    if (!responseBodyString.isNullOrBlank()) {
                        val apiResponse = json.decodeFromString<ApiResponse<TokenResponse>>(responseBodyString)
                        val newTokenData = apiResponse.data
                        if (newTokenData != null && newTokenData.accessToken.isNotBlank()) {
                            tokenManager.updateTokens(
                                accessToken = newTokenData.accessToken,
                                refreshToken = newTokenData.refreshToken.ifBlank { currentRefreshToken }
                            )

                            return response.request.newBuilder()
                                .header("Authorization", "Bearer ${newTokenData.accessToken}")
                                .build()
                        }
                    }
                }

                // If refresh failed (token expired, revoked, or invalid)
                tokenManager.clearSession()
                return null
            } catch (e: Exception) {
                return null
            }
        }
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}
