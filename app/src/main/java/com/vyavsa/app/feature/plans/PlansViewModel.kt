package com.vyavsa.app.feature.plans

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vyavsa.app.core.common.MessageType
import com.vyavsa.app.core.common.OneTimeEventEmitter
import com.vyavsa.app.core.common.Resource
import com.vyavsa.app.core.common.UiMessage
import com.vyavsa.app.core.common.UiState
import com.vyavsa.app.core.model.InitiateCheckoutResponse
import com.vyavsa.app.core.model.PlanTransaction
import com.vyavsa.app.core.model.SubscriptionPlan
import com.vyavsa.app.core.model.TenantSubscription
import com.vyavsa.app.core.security.TokenManager
import com.vyavsa.app.core.sync.DataMutation
import com.vyavsa.app.core.sync.DataRefreshCoordinator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class PlansTab {
    AVAILABLE_PLANS,
    BILLING_HISTORY
}

sealed interface CheckoutUiState {
    object Idle : CheckoutUiState
    data class InitiatingCheckout(val planId: String) : CheckoutUiState
    data class AwaitingPayment(val checkoutInfo: InitiateCheckoutResponse) : CheckoutUiState
    data class VerifyingPayment(
        val planId: String,
        val paymentId: String,
        val subscriptionId: String,
        val signature: String,
        val activationMode: String
    ) : CheckoutUiState
    data class Success(val message: String, val subscription: TenantSubscription?) : CheckoutUiState
    data class Failed(
        val message: String,
        val canRetryVerification: Boolean = false,
        val pendingVerification: PendingPaymentVerification? = null
    ) : CheckoutUiState
}

data class PendingPaymentVerification(
    val planId: String,
    val paymentId: String,
    val subscriptionId: String,
    val signature: String,
    val activationMode: String
)

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

    private val _checkoutState = MutableStateFlow<CheckoutUiState>(CheckoutUiState.Idle)
    val checkoutState: StateFlow<CheckoutUiState> = _checkoutState.asStateFlow()

    internal var pendingVerification: PendingPaymentVerification? = null

    private val eventEmitter = OneTimeEventEmitter()
    val uiEvents: Flow<UiMessage> = eventEmitter.events

    fun sendUiMessage(text: String, type: MessageType = MessageType.INFO) {
        eventEmitter.emit(text, type)
    }

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

    /**
     * Initiates checkout with backend Razorpay subscription creation
     */
    fun startPlanPurchase(planId: String, activationMode: String = "instant") {
        if (!isAdmin) {
            val err = "Only tenant administrators can manage subscription plans"
            _checkoutState.value = CheckoutUiState.Failed(err)
            sendUiMessage(err, MessageType.ERROR)
            return
        }

        _checkoutState.value = CheckoutUiState.InitiatingCheckout(planId)
        viewModelScope.launch {
            repository.initiateCheckout(planId, activationMode).collect { res ->
                when (res) {
                    is Resource.Loading -> {
                        _checkoutState.value = CheckoutUiState.InitiatingCheckout(planId)
                    }
                    is Resource.Success -> {
                        _checkoutState.value = CheckoutUiState.AwaitingPayment(res.data)
                    }
                    is Resource.Error -> {
                        val errMsg = res.message
                        _checkoutState.value = CheckoutUiState.Failed(errMsg, canRetryVerification = false)
                        sendUiMessage(errMsg, MessageType.ERROR)
                    }
                }
            }
        }
    }

    /**
     * Verifies payment credentials from Razorpay modal/SDK
     */
    fun verifyPayment(
        planId: String,
        paymentId: String,
        subscriptionId: String,
        signature: String,
        activationMode: String = "instant"
    ) {
        val pending = PendingPaymentVerification(
            planId = planId,
            paymentId = paymentId,
            subscriptionId = subscriptionId,
            signature = signature,
            activationMode = activationMode
        )
        pendingVerification = pending

        _checkoutState.value = CheckoutUiState.VerifyingPayment(
            planId = planId,
            paymentId = paymentId,
            subscriptionId = subscriptionId,
            signature = signature,
            activationMode = activationMode
        )

        viewModelScope.launch {
            repository.verifyPayment(
                planId = planId,
                paymentId = paymentId,
                subscriptionId = subscriptionId,
                signature = signature,
                activationMode = activationMode
            ).collect { res ->
                when (res) {
                    is Resource.Loading -> {
                        _checkoutState.value = CheckoutUiState.VerifyingPayment(
                            planId = planId,
                            paymentId = paymentId,
                            subscriptionId = subscriptionId,
                            signature = signature,
                            activationMode = activationMode
                        )
                    }
                    is Resource.Success -> {
                        pendingVerification = null
                        refreshCoordinator.notifyMutation(DataMutation.ALL)
                        loadData()
                        val msg = res.message ?: "Subscription plan successfully activated!"
                        _checkoutState.value = CheckoutUiState.Success(msg, res.data.subscription)
                        sendUiMessage(msg, MessageType.SUCCESS)
                    }
                    is Resource.Error -> {
                        val errMsg = "Payment verification failed: ${res.message}"
                        _checkoutState.value = CheckoutUiState.Failed(
                            message = errMsg,
                            canRetryVerification = true,
                            pendingVerification = pending
                        )
                        sendUiMessage(errMsg, MessageType.ERROR)
                    }
                }
            }
        }
    }

    /**
     * Retries pending verification in case of connection drop
     */
    fun retryPendingVerification() {
        val pending = pendingVerification ?: return
        verifyPayment(
            planId = pending.planId,
            paymentId = pending.paymentId,
            subscriptionId = pending.subscriptionId,
            signature = pending.signature,
            activationMode = pending.activationMode
        )
    }

    /**
     * Cancel checkout or dismiss dialog gracefully without error
     */
    fun cancelCheckout() {
        _checkoutState.value = CheckoutUiState.Idle
    }

    /**
     * Legacy direct purchase fallback
     */
    fun purchasePlan(planId: String, paymentMethod: String = "upi") {
        if (!isAdmin) {
            val err = "Only tenant administrators can manage subscription plans"
            _purchaseActionState.value = UiState.Error(err)
            sendUiMessage(err, MessageType.ERROR)
            return
        }

        viewModelScope.launch {
            repository.purchasePlan(planId, paymentMethod).collect { res ->
                _purchaseActionState.value = when (res) {
                    is Resource.Loading -> UiState.Loading
                    is Resource.Success -> {
                        refreshCoordinator.notifyMutation(DataMutation.ALL)
                        loadData()
                        sendUiMessage("Plan upgraded successfully", MessageType.SUCCESS)
                        UiState.Success(Unit)
                    }
                    is Resource.Error -> {
                        sendUiMessage(res.message, MessageType.ERROR)
                        UiState.Error(res.message)
                    }
                }
            }
        }
    }

    fun resetActionState() {
        _purchaseActionState.value = UiState.Idle
        _checkoutState.value = CheckoutUiState.Idle
    }
}
