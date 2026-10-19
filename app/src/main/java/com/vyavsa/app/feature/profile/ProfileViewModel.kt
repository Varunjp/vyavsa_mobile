package com.vyavsa.app.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vyavsa.app.core.common.Resource
import com.vyavsa.app.core.common.UiState
import com.vyavsa.app.core.designsystem.LogoMemoryCache
import com.vyavsa.app.core.model.SubscriptionPlan
import com.vyavsa.app.core.model.TenantProfile
import com.vyavsa.app.core.model.TenantSubscription
import com.vyavsa.app.core.model.UpdateTenantProfileRequest
import com.vyavsa.app.core.network.VyavsaApiService
import com.vyavsa.app.core.security.TokenManager
import com.vyavsa.app.core.sync.DataMutation
import com.vyavsa.app.core.sync.DataRefreshCoordinator
import com.vyavsa.app.feature.auth.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val apiService: VyavsaApiService,
    private val authRepository: AuthRepository,
    private val refreshCoordinator: DataRefreshCoordinator,
    val tokenManager: TokenManager
) : ViewModel() {

    private val _profileState = MutableStateFlow<UiState<TenantProfile>>(UiState.Loading)
    val profileState: StateFlow<UiState<TenantProfile>> = _profileState.asStateFlow()

    private val _logoState = MutableStateFlow<UiState<String?>>(UiState.Idle)
    val logoState: StateFlow<UiState<String?>> = _logoState.asStateFlow()

    private val _logoActionState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val logoActionState: StateFlow<UiState<Unit>> = _logoActionState.asStateFlow()

    private val _subscriptionState = MutableStateFlow<UiState<TenantSubscription>>(UiState.Loading)
    val subscriptionState: StateFlow<UiState<TenantSubscription>> = _subscriptionState.asStateFlow()

    private val _plansState = MutableStateFlow<UiState<List<SubscriptionPlan>>>(UiState.Idle)
    val plansState: StateFlow<UiState<List<SubscriptionPlan>>> = _plansState.asStateFlow()

    private val _updateState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val updateState: StateFlow<UiState<Unit>> = _updateState.asStateFlow()

    init {
        loadProfileData()
    }

    fun loadProfileData() {
        viewModelScope.launch {
            try {
                val res = apiService.getProfile()
                if (res.success && res.data != null) {
                    _profileState.value = UiState.Success(res.data)
                    _logoState.value = UiState.Success(res.data.logoUrl)
                } else {
                    _profileState.value = UiState.Error(res.error?.message ?: "Failed to load profile")
                }
            } catch (e: Exception) {
                _profileState.value = UiState.Error(e.message ?: "Network error")
            }
        }

        viewModelScope.launch {
            try {
                val res = apiService.getSubscription()
                if (res.success && res.data != null) {
                    _subscriptionState.value = UiState.Success(res.data)
                } else {
                    _subscriptionState.value = UiState.Error(res.error?.message ?: "Failed to load subscription")
                }
            } catch (e: Exception) {
                _subscriptionState.value = UiState.Error(e.message ?: "Network error")
            }
        }

        viewModelScope.launch {
            authRepository.getPublicPlans().collect { res ->
                _plansState.value = when (res) {
                    is Resource.Loading -> UiState.Loading
                    is Resource.Success -> UiState.Success(res.data)
                    is Resource.Error -> UiState.Error(res.message)
                }
            }
        }
    }

    fun updateProfile(name: String, email: String, phone: String?) {
        if (name.isBlank() || email.isBlank()) {
            _updateState.value = UiState.Error("Name and email are required")
            return
        }

        viewModelScope.launch {
            _updateState.value = UiState.Loading
            try {
                val res = apiService.updateProfile(UpdateTenantProfileRequest(name, email, phone))
                if (res.success && res.data != null) {
                    _profileState.value = UiState.Success(res.data)
                    _updateState.value = UiState.Success(Unit)
                } else {
                    _updateState.value = UiState.Error(res.error?.message ?: "Update failed")
                }
            } catch (e: Exception) {
                _updateState.value = UiState.Error(e.message ?: "Network error updating profile")
            }
        }
    }

    fun logout(onSuccess: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout().collect {
                onSuccess()
            }
        }
    }

    fun loadLogo() {
        viewModelScope.launch {
            try {
                val res = apiService.getCompanyLogo()
                if (res.success && res.data != null) {
                    _logoState.value = UiState.Success(res.data.logoUrl)
                } else {
                    _logoState.value = UiState.Error(res.error?.message ?: "Failed to load logo")
                }
            } catch (e: Exception) {
                _logoState.value = UiState.Error(e.message ?: "Network error loading logo")
            }
        }
    }

    fun uploadLogo(filename: String, mimeType: String, bytes: ByteArray) {
        if (bytes.isEmpty()) {
            _logoActionState.value = UiState.Error("Selected image file is empty")
            return
        }
        if (bytes.size > 2 * 1024 * 1024) {
            _logoActionState.value = UiState.Error("Logo file must not exceed 2MB in size")
            return
        }
        val normalizedMime = mimeType.lowercase().trim()
        val allowedTypes = listOf("image/jpeg", "image/png", "image/webp", "image/gif")
        if (normalizedMime !in allowedTypes) {
            _logoActionState.value = UiState.Error("Only JPEG, PNG, WEBP, or GIF images are supported")
            return
        }

        viewModelScope.launch {
            _logoActionState.value = UiState.Loading
            try {
                val reqBody = bytes.toRequestBody(normalizedMime.toMediaTypeOrNull())
                val part = MultipartBody.Part.createFormData("logo", filename, reqBody)
                val res = apiService.uploadCompanyLogo(part)
                if (res.success && res.data != null) {
                    LogoMemoryCache.clear()
                    _logoState.value = UiState.Success(res.data.logoUrl)
                    _logoActionState.value = UiState.Success(Unit)
                    refreshCoordinator.notifyMutation(DataMutation.PROFILE)
                    loadProfileData()
                } else {
                    _logoActionState.value = UiState.Error(res.error?.message ?: "Failed to upload logo")
                }
            } catch (e: Exception) {
                _logoActionState.value = UiState.Error(e.message ?: "Network error uploading logo")
            }
        }
    }

    fun deleteLogo() {
        viewModelScope.launch {
            _logoActionState.value = UiState.Loading
            try {
                val res = apiService.deleteCompanyLogo()
                if (res.success) {
                    LogoMemoryCache.clear()
                    _logoState.value = UiState.Success(null)
                    _logoActionState.value = UiState.Success(Unit)
                    refreshCoordinator.notifyMutation(DataMutation.PROFILE)
                    loadProfileData()
                } else {
                    _logoActionState.value = UiState.Error(res.error?.message ?: "Failed to remove logo")
                }
            } catch (e: Exception) {
                _logoActionState.value = UiState.Error(e.message ?: "Network error removing logo")
            }
        }
    }

    fun resetLogoActionState() {
        _logoActionState.value = UiState.Idle
    }

    fun resetUpdateState() {
        _updateState.value = UiState.Idle
    }
}
