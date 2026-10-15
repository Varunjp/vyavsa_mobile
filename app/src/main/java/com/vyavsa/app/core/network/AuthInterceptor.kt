package com.vyavsa.app.core.network

import com.vyavsa.app.core.security.TokenManager
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager
) : Interceptor {

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // If request is public or already has an Authorization header, proceed
        val path = originalRequest.url.encodedPath
        if (path.contains("/auth/tenant/login") ||
            path.contains("/auth/tenant/register") ||
            path.contains("/auth/refresh") ||
            path.contains("/auth/forgot-password") ||
            path.contains("/auth/verify-reset-otp") ||
            path.contains("/auth/reset-password") ||
            originalRequest.header("Authorization") != null
        ) {
            return chain.proceed(originalRequest)
        }

        val token = tokenManager.getAccessToken()
        return if (!token.isNullOrBlank()) {
            val authenticatedRequest = originalRequest.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
            chain.proceed(authenticatedRequest)
        } else {
            chain.proceed(originalRequest)
        }
    }
}
