package com.vyavsa.app.core.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TenantLoginRequest(
    val email: String,
    val password: String,
    @SerialName("tenant_id") val tenantId: String? = null
)

@Serializable
data class TenantRegisterRequest(
    val name: String,
    @SerialName("admin_name") val adminName: String? = null,
    val email: String,
    val password: String,
    val phone: String? = null,
    @SerialName("plan_id") val planId: String? = null
)

@Immutable
@Serializable
data class TokenResponse(
    @SerialName("access_token") val accessToken: String = "",
    @SerialName("refresh_token") val refreshToken: String = "",
    @SerialName("token_type") val tokenType: String = "Bearer",
    @SerialName("expires_in") val expiresIn: Long = 0,
    val user: UserProfile = UserProfile()
)

@Immutable
@Serializable
data class UserProfile(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val role: String = "",
    @SerialName("user_type") val userType: String = "",
    @SerialName("tenant_id") val tenantId: String? = null
)

@Serializable
data class RefreshTokenRequest(
    @SerialName("refresh_token") val refreshToken: String
)

@Serializable
data class ForgotPasswordRequest(
    val email: String
)

@Serializable
data class VerifyResetOtpRequest(
    val email: String,
    val otp: String
)

@Immutable
@Serializable
data class VerifyResetOtpResponse(
    @SerialName("reset_token") val resetToken: String = ""
)

@Serializable
data class ResetPasswordRequest(
    @SerialName("reset_token") val resetToken: String,
    @SerialName("new_password") val newPassword: String
)
