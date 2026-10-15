package com.vyavsa.app.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vyavsa.app.core.common.Resource
import com.vyavsa.app.core.common.UiState
import com.vyavsa.app.core.model.FinancialMetrics
import com.vyavsa.app.core.model.TenantProfile
import com.vyavsa.app.core.model.TenantSubscription
import com.vyavsa.app.core.security.TokenManager
import com.vyavsa.app.core.sync.DataMutation
import com.vyavsa.app.core.sync.DataRefreshCoordinator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: DashboardRepository,
    private val refreshCoordinator: DataRefreshCoordinator,
    val tokenManager: TokenManager
) : ViewModel() {

    private val _metricsState = MutableStateFlow<UiState<FinancialMetrics>>(UiState.Loading)
    val metricsState: StateFlow<UiState<FinancialMetrics>> = _metricsState.asStateFlow()

    private val _profileState = MutableStateFlow<UiState<TenantProfile>>(UiState.Idle)
    val profileState: StateFlow<UiState<TenantProfile>> = _profileState.asStateFlow()

    private val _subscriptionState = MutableStateFlow<UiState<TenantSubscription>>(UiState.Idle)
    val subscriptionState: StateFlow<UiState<TenantSubscription>> = _subscriptionState.asStateFlow()

    init {
        loadDashboard()
        observeRefreshEvents()
    }

    private fun observeRefreshEvents() {
        viewModelScope.launch {
            refreshCoordinator.refreshEvents.collect {
                loadDashboard()
            }
        }
    }

    fun loadDashboard() {
        viewModelScope.launch {
            repository.getDashboardMetrics().collect { resource ->
                _metricsState.value = when (resource) {
                    is Resource.Loading -> UiState.Loading
                    is Resource.Success -> UiState.Success(resource.data)
                    is Resource.Error -> UiState.Error(resource.message, resource.code)
                }
            }
        }
        viewModelScope.launch {
            repository.getSubscription().collect { resource ->
                _subscriptionState.value = when (resource) {
                    is Resource.Loading -> UiState.Loading
                    is Resource.Success -> UiState.Success(resource.data)
                    is Resource.Error -> UiState.Error(resource.message, resource.code)
                }
            }
        }
    }
}
