package com.vyavsa.app.feature.plans

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vyavsa.app.core.common.Resource
import com.vyavsa.app.core.common.UiState
import com.vyavsa.app.core.model.PlanTransaction
import com.vyavsa.app.core.model.SubscriptionPlan
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

enum class PlansTab {
    AVAILABLE_PLANS,
    BILLING_HISTORY
}

@HiltViewModel
class PlansViewModel @Inject constructor(
    private val repository: PlansRepository,
    private val refreshCoordinator: DataRefreshCoordinator,
    val tokenManager: TokenManager? = null
) : ViewModel() {

    var adminOverrideForTesting: Boolean? = null

    private val _selectedTab = MutableStateFlow(PlansTab.AVAILABLE_PLANS)
    val selectedTab: StateFlow<PlansTab> = _selectedTab.asStateFlow()

    private val _subscriptionState = MutableStateFlow<UiState<TenantSubscription>>(UiState.Loading)
    val subscriptionState: StateFlow<UiState<TenantSubscription>> = _subscriptionState.asStateFlow()

    private val _plansState = MutableStateFlow<UiState<List<SubscriptionPlan>>>(UiState.Loading)
    val plansState: StateFlow<UiState<List<SubscriptionPlan>>> = _plansState.asStateFlow()

    private val _transactionsState = MutableStateFlow<UiState<List<PlanTransaction>>>(UiState.Loading)
    val transactionsState: StateFlow<UiState<List<PlanTransaction>>> = _transactionsState.asStateFlow()

    private val _purchaseActionState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val purchaseActionState: StateFlow<UiState<Unit>> = _purchaseActionState.asStateFlow()

    val isAdmin: Boolean
        get() = adminOverrideForTesting ?: (tokenManager?.isTenantAdmin() ?: false)

    init {
        loadData()
        observeRefreshEvents()
    }

    private fun observeRefreshEvents() {
        viewModelScope.launch {
            refreshCoordinator.refreshEvents.collect { mutation ->
                when (mutation) {
                    DataMutation.ALL -> loadData()
                    else -> Unit
                }
            }
        }
    }

    fun selectTab(tab: PlansTab) {
        _selectedTab.value = tab
    }

    fun loadData() {
        loadSubscription()
        loadAvailablePlans()
        if (isAdmin) {
            loadBillingTransactions()
        }
    }

    fun loadSubscription() {
        viewModelScope.launch {
            repository.getSubscription().collect { res ->
                _subscriptionState.value = when (res) {
                    is Resource.Loading -> UiState.Loading
                    is Resource.Success -> UiState.Success(res.data)
                    is Resource.Error -> UiState.Error(res.message)
                }
            }
        }
    }

    fun loadAvailablePlans() {
        viewModelScope.launch {
            repository.listAvailablePlans().collect { res ->
                _plansState.value = when (res) {
                    is Resource.Loading -> UiState.Loading
                    is Resource.Success -> if (res.data.isEmpty()) UiState.Empty else UiState.Success(res.data)
                    is Resource.Error -> UiState.Error(res.message)
                }
            }
        }
    }

    fun loadBillingTransactions() {
        viewModelScope.launch {
            repository.listBillingTransactions().collect { res ->
                _transactionsState.value = when (res) {
                    is Resource.Loading -> UiState.Loading
                    is Resource.Success -> if (res.data.isEmpty()) UiState.Empty else UiState.Success(res.data)
                    is Resource.Error -> UiState.Error(res.message)
                }
            }
        }
    }

    fun purchasePlan(planId: String, paymentMethod: String = "upi") {
        if (!isAdmin) {
            _purchaseActionState.value = UiState.Error("Only tenant administrators can manage subscription plans")
            return
        }

        viewModelScope.launch {
            repository.purchasePlan(planId, paymentMethod).collect { res ->
                _purchaseActionState.value = when (res) {
                    is Resource.Loading -> UiState.Loading
                    is Resource.Success -> {
                        refreshCoordinator.notifyMutation(DataMutation.ALL)
                        loadData()
                        UiState.Success(Unit)
                    }
                    is Resource.Error -> UiState.Error(res.message)
                }
            }
        }
    }

    fun resetActionState() {
        _purchaseActionState.value = UiState.Idle
    }
}
