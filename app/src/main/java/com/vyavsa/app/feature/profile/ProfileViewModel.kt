package com.vyavsa.app.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vyavsa.app.core.common.Resource
import com.vyavsa.app.core.common.UiState
import com.vyavsa.app.core.model.SubscriptionPlan
import com.vyavsa.app.core.model.TenantProfile
import com.vyavsa.app.core.model.TenantSubscription
import com.vyavsa.app.core.model.UpdateTenantProfileRequest
import com.vyavsa.app.core.network.VyavsaApiService
import com.vyavsa.app.core.security.TokenManager
import com.vyavsa.app.feature.auth.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val apiService: VyavsaApiService,
    private val authRepository: AuthRepository,
    val tokenManager: TokenManager
) : ViewModel() {

    private val _profileState = MutableStateFlow<UiState<TenantProfile>>(UiState.Loading)
    val profileState: StateFlow<UiState<TenantProfile>> = _profileState.asStateFlow()

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

    fun resetUpdateState() {
        _updateState.value = UiState.Idle
    }
}
