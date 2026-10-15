package com.vyavsa.app.feature.auth

import com.vyavsa.app.core.common.Resource
import com.vyavsa.app.core.model.ForgotPasswordRequest
import com.vyavsa.app.core.model.ResetPasswordRequest
import com.vyavsa.app.core.model.SubscriptionPlan
import com.vyavsa.app.core.model.TenantLoginRequest
import com.vyavsa.app.core.model.TenantProfile
import com.vyavsa.app.core.model.TenantRegisterRequest
import com.vyavsa.app.core.model.TokenResponse
import com.vyavsa.app.core.model.VerifyResetOtpRequest
import com.vyavsa.app.core.network.VyavsaApiService
import com.vyavsa.app.core.security.TokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

interface AuthRepository {
    fun login(email: String, password: String): Flow<Resource<TokenResponse>>
    fun register(
        name: String,
        adminName: String?,
        email: String,
        password: String,
        phone: String?,
        planId: String?
    ): Flow<Resource<TenantProfile>>
    fun forgotPassword(email: String): Flow<Resource<String>>
    fun verifyResetOtp(email: String, otp: String): Flow<Resource<String>>
    fun resetPassword(resetToken: String, newPassword: String): Flow<Resource<Unit>>
    fun logout(): Flow<Resource<Unit>>
    fun getPublicPlans(): Flow<Resource<List<SubscriptionPlan>>>
}

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val apiService: VyavsaApiService,
    private val tokenManager: TokenManager
) : AuthRepository {

    override fun login(email: String, password: String): Flow<Resource<TokenResponse>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.loginTenant(TenantLoginRequest(email = email, password = password))
            if (response.success && response.data != null) {
                tokenManager.saveSession(response.data)
                emit(Resource.Success(response.data, response.message))
            } else {
                emit(Resource.Error(response.error?.message ?: response.message ?: "Authentication failed", response.error?.code))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Network error during login"))
        }
    }.flowOn(Dispatchers.IO)

    override fun register(
        name: String,
        adminName: String?,
        email: String,
        password: String,
        phone: String?,
        planId: String?
    ): Flow<Resource<TenantProfile>> = flow {
        emit(Resource.Loading)
        try {
            val request = TenantRegisterRequest(
                name = name,
                adminName = adminName,
                email = email,
                password = password,
                phone = phone,
                planId = planId
            )
            val response = apiService.registerTenant(request)
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data, response.message))
            } else {
                emit(Resource.Error(response.error?.message ?: response.message ?: "Registration failed", response.error?.code))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Network error during registration"))
        }
    }.flowOn(Dispatchers.IO)

    override fun forgotPassword(email: String): Flow<Resource<String>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.forgotPassword(ForgotPasswordRequest(email))
            if (response.success) {
                emit(Resource.Success("OTP has been sent to your email", response.message))
            } else {
                emit(Resource.Error(response.error?.message ?: response.message ?: "Failed to send reset OTP", response.error?.code))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Network error"))
        }
    }.flowOn(Dispatchers.IO)

    override fun verifyResetOtp(email: String, otp: String): Flow<Resource<String>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.verifyResetOtp(VerifyResetOtpRequest(email, otp))
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data.resetToken, response.message))
            } else {
                emit(Resource.Error(response.error?.message ?: response.message ?: "Invalid or expired OTP", response.error?.code))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Network error"))
        }
    }.flowOn(Dispatchers.IO)

    override fun resetPassword(resetToken: String, newPassword: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.resetPassword(ResetPasswordRequest(resetToken, newPassword))
            if (response.success) {
                emit(Resource.Success(Unit, response.message))
            } else {
                emit(Resource.Error(response.error?.message ?: response.message ?: "Failed to reset password", response.error?.code))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Network error"))
        }
    }.flowOn(Dispatchers.IO)

    override fun logout(): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading)
        try {
            val refreshToken = tokenManager.getRefreshToken()
            apiService.logout(refreshToken?.let { com.vyavsa.app.core.model.RefreshTokenRequest(it) })
        } catch (e: Exception) {
            // Ignore server-side logout errors and always clear client session
        } finally {
            tokenManager.clearSession()
            emit(Resource.Success(Unit))
        }
    }.flowOn(Dispatchers.IO)

    override fun getPublicPlans(): Flow<Resource<List<SubscriptionPlan>>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.getPublicPlans()
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data))
            } else {
                emit(Resource.Error(response.error?.message ?: "Failed to load plans"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Network error loading plans"))
        }
    }.flowOn(Dispatchers.IO)
}
