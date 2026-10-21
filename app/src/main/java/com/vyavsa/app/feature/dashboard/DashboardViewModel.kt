package com.vyavsa.app.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vyavsa.app.core.common.Resource
import com.vyavsa.app.core.common.UiState
import com.vyavsa.app.core.model.FinancialMetrics
import com.vyavsa.app.core.model.OfficeDashboardResponse
import com.vyavsa.app.core.model.SalesDashboardResponse
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

    private val _salesDashboardState = MutableStateFlow<UiState<SalesDashboardResponse>>(UiState.Loading)
    val salesDashboardState: StateFlow<UiState<SalesDashboardResponse>> = _salesDashboardState.asStateFlow()

    private val _officeDashboardState = MutableStateFlow<UiState<OfficeDashboardResponse>>(UiState.Loading)
    val officeDashboardState: StateFlow<UiState<OfficeDashboardResponse>> = _officeDashboardState.asStateFlow()

    private val _profileState = MutableStateFlow<UiState<TenantProfile>>(UiState.Idle)
    val profileState: StateFlow<UiState<TenantProfile>> = _profileState.asStateFlow()

    private val _subscriptionState = MutableStateFlow<UiState<TenantSubscription>>(UiState.Idle)
    val subscriptionState: StateFlow<UiState<TenantSubscription>> = _subscriptionState.asStateFlow()

    private val _salesDateFilter = MutableStateFlow<String>("")
    val salesDateFilter: StateFlow<String> = _salesDateFilter.asStateFlow()

    private val _officeDateFilter = MutableStateFlow<String>("")
    val officeDateFilter: StateFlow<String> = _officeDateFilter.asStateFlow()

    init {
        loadDashboard()
        observeRefreshEvents()
    }

    private fun observeRefreshEvents() {
        viewModelScope.launch {
            refreshCoordinator.refreshEvents.collect { mutation ->
                when (mutation) {
                    DataMutation.SALES, DataMutation.EXPENSES, DataMutation.PURCHASES, DataMutation.CASH, DataMutation.DUES, DataMutation.STAFF -> {
                        loadRoleMetrics()
                    }
                    DataMutation.PROFILE -> {
                        loadProfile()
                    }
                    DataMutation.ALL -> {
                        loadDashboard()
                    }
                    else -> Unit
                }
            }
        }
    }

    fun loadMetrics() {
        viewModelScope.launch {
            repository.getDashboardMetrics().collect { resource ->
                _metricsState.value = when (resource) {
                    is Resource.Loading -> UiState.Loading
                    is Resource.Success -> UiState.Success(resource.data)
                    is Resource.Error -> UiState.Error(resource.message, resource.code)
                }
            }
        }
    }

    fun loadSalesDashboard(date: String? = _salesDateFilter.value.ifBlank { null }) {
        if (date != null) {
            _salesDateFilter.value = date
        }
        viewModelScope.launch {
            repository.getSalesDashboard(date).collect { resource ->
                _salesDashboardState.value = when (resource) {
                    is Resource.Loading -> UiState.Loading
                    is Resource.Success -> UiState.Success(resource.data)
                    is Resource.Error -> UiState.Error(resource.message, resource.code)
                }
            }
        }
    }

    fun loadOfficeDashboard(date: String? = _officeDateFilter.value.ifBlank { null }) {
        if (date != null) {
            _officeDateFilter.value = date
        }
        viewModelScope.launch {
            repository.getOfficeDashboard(date).collect { resource ->
                _officeDashboardState.value = when (resource) {
                    is Resource.Loading -> UiState.Loading
                    is Resource.Success -> UiState.Success(resource.data)
                    is Resource.Error -> UiState.Error(resource.message, resource.code)
                }
            }
        }
    }

    fun loadSubscription() {
        if (tokenManager.isSales() || tokenManager.isOffice()) return
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

    fun loadProfile() {
        viewModelScope.launch {
            repository.getProfile().collect { resource ->
                _profileState.value = when (resource) {
                    is Resource.Loading -> UiState.Loading
                    is Resource.Success -> UiState.Success(resource.data)
                    is Resource.Error -> UiState.Error(resource.message, resource.code)
                }
            }
        }
    }

    fun loadRoleMetrics() {
        when {
            tokenManager.isSales() -> loadSalesDashboard()
            tokenManager.isOffice() -> loadOfficeDashboard()
            else -> loadMetrics()
        }
    }

    fun loadDashboard() {
        when {
            tokenManager.isSales() -> {
                loadSalesDashboard()
                loadProfile()
            }
            tokenManager.isOffice() -> {
                loadOfficeDashboard()
                loadProfile()
            }
            else -> {
                loadMetrics()
                loadSubscription()
                loadProfile()
            }
        }
    }
}
