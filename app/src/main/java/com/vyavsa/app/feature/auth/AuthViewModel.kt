package com.vyavsa.app.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vyavsa.app.core.common.Resource
import com.vyavsa.app.core.common.UiState
import com.vyavsa.app.core.model.TenantProfile
import com.vyavsa.app.core.model.TokenResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _loginState = MutableStateFlow<UiState<TokenResponse>>(UiState.Idle)
    val loginState: StateFlow<UiState<TokenResponse>> = _loginState.asStateFlow()

    private val _registerState = MutableStateFlow<UiState<TenantProfile>>(UiState.Idle)
    val registerState: StateFlow<UiState<TenantProfile>> = _registerState.asStateFlow()

    private val _forgotPasswordState = MutableStateFlow<UiState<String>>(UiState.Idle)
    val forgotPasswordState: StateFlow<UiState<String>> = _forgotPasswordState.asStateFlow()

    private val _verifyOtpState = MutableStateFlow<UiState<String>>(UiState.Idle)
    val verifyOtpState: StateFlow<UiState<String>> = _verifyOtpState.asStateFlow()

    private val _resetPasswordState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val resetPasswordState: StateFlow<UiState<Unit>> = _resetPasswordState.asStateFlow()

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _loginState.value = UiState.Error("Please enter both email and password")
            return
        }

        viewModelScope.launch {
            authRepository.login(email.trim(), password).collect { resource ->
                _loginState.value = when (resource) {
                    is Resource.Loading -> UiState.Loading
                    is Resource.Success -> UiState.Success(resource.data)
                    is Resource.Error -> UiState.Error(resource.message, resource.code)
                }
            }
        }
    }

    fun register(name: String, adminName: String?, email: String, password: String, phone: String?) {
        if (name.isBlank() || email.isBlank() || password.isBlank()) {
            _registerState.value = UiState.Error("Business name, email, and password are required")
            return
        }
        if (password.length < 6) {
            _registerState.value = UiState.Error("Password must be at least 6 characters")
            return
        }

        viewModelScope.launch {
            authRepository.register(
                name = name.trim(),
                adminName = adminName?.trim()?.ifBlank { null },
                email = email.trim(),
                password = password,
                phone = phone?.trim()?.ifBlank { null },
                planId = null
            ).collect { resource ->
                _registerState.value = when (resource) {
                    is Resource.Loading -> UiState.Loading
                    is Resource.Success -> UiState.Success(resource.data)
                    is Resource.Error -> UiState.Error(resource.message, resource.code)
                }
            }
        }
    }

    fun forgotPassword(email: String) {
        if (email.isBlank()) {
            _forgotPasswordState.value = UiState.Error("Please enter your registered email")
            return
        }

        viewModelScope.launch {
            authRepository.forgotPassword(email.trim()).collect { resource ->
                _forgotPasswordState.value = when (resource) {
                    is Resource.Loading -> UiState.Loading
                    is Resource.Success -> UiState.Success(resource.data)
                    is Resource.Error -> UiState.Error(resource.message, resource.code)
                }
            }
        }
    }

    fun verifyResetOtp(email: String, otp: String) {
        if (otp.length != 6) {
            _verifyOtpState.value = UiState.Error("Please enter the 6-digit numeric OTP")
            return
        }

        viewModelScope.launch {
            authRepository.verifyResetOtp(email.trim(), otp.trim()).collect { resource ->
                _verifyOtpState.value = when (resource) {
                    is Resource.Loading -> UiState.Loading
                    is Resource.Success -> UiState.Success(resource.data)
                    is Resource.Error -> UiState.Error(resource.message, resource.code)
                }
            }
        }
    }

    fun resetPassword(resetToken: String, newPassword: String, confirmPassword: String) {
        if (newPassword.length < 6) {
            _resetPasswordState.value = UiState.Error("Password must be at least 6 characters")
            return
        }
        if (newPassword != confirmPassword) {
            _resetPasswordState.value = UiState.Error("Passwords do not match")
            return
        }

        viewModelScope.launch {
            authRepository.resetPassword(resetToken, newPassword).collect { resource ->
                _resetPasswordState.value = when (resource) {
                    is Resource.Loading -> UiState.Loading
                    is Resource.Success -> UiState.Success(Unit)
                    is Resource.Error -> UiState.Error(resource.message, resource.code)
                }
            }
        }
    }

    fun resetLoginState() {
        _loginState.value = UiState.Idle
    }

    fun resetRegisterState() {
        _registerState.value = UiState.Idle
    }

    fun resetForgotPasswordState() {
        _forgotPasswordState.value = UiState.Idle
    }

    fun resetVerifyOtpState() {
        _verifyOtpState.value = UiState.Idle
    }

    fun resetResetPasswordState() {
        _resetPasswordState.value = UiState.Idle
    }
}
