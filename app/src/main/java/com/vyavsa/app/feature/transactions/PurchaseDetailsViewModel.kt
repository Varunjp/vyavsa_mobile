package com.vyavsa.app.feature.transactions

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vyavsa.app.core.common.Resource
import com.vyavsa.app.core.common.UiState
import com.vyavsa.app.core.model.Purchase
import com.vyavsa.app.core.model.PurchasePayment
import com.vyavsa.app.core.model.RecordSupplierPaymentRequest
import com.vyavsa.app.core.model.TenantBank
import com.vyavsa.app.core.model.UpdatePurchaseRequest
import com.vyavsa.app.core.security.TokenManager
import com.vyavsa.app.core.sync.DataMutation
import com.vyavsa.app.core.sync.DataRefreshCoordinator
import com.vyavsa.app.feature.bank.BankRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PurchaseDetailsViewModel @Inject constructor(
    private val repository: TransactionsRepository,
    private val bankRepository: BankRepository,
    private val refreshCoordinator: DataRefreshCoordinator,
    val tokenManager: TokenManager? = null,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val purchaseId: String = savedStateHandle["purchaseId"] ?: ""

    private val _purchaseState = MutableStateFlow<UiState<Purchase>>(UiState.Loading)
    val purchaseState: StateFlow<UiState<Purchase>> = _purchaseState.asStateFlow()

    private val _paymentsState = MutableStateFlow<UiState<List<PurchasePayment>>>(UiState.Loading)
    val paymentsState: StateFlow<UiState<List<PurchasePayment>>> = _paymentsState.asStateFlow()

    private val _banksState = MutableStateFlow<List<TenantBank>>(emptyList())
    val banksState: StateFlow<List<TenantBank>> = _banksState.asStateFlow()

    private val _paymentActionState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val paymentActionState: StateFlow<UiState<Unit>> = _paymentActionState.asStateFlow()

    private val _updateActionState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val updateActionState: StateFlow<UiState<Unit>> = _updateActionState.asStateFlow()

    private val _deleteActionState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val deleteActionState: StateFlow<UiState<Unit>> = _deleteActionState.asStateFlow()

    val isAdmin: Boolean
        get() = tokenManager?.isTenantAdmin() ?: false

    init {
        loadPurchaseDetails()
        loadBanks()
        observeMutations()
    }

    fun loadPurchaseDetails() {
        if (purchaseId.isBlank()) {
            _purchaseState.value = UiState.Error("Invalid purchase identifier")
            return
        }

        viewModelScope.launch {
            repository.getPurchaseById(purchaseId).collectLatest { res ->
                _purchaseState.value = when (res) {
                    is Resource.Loading -> UiState.Loading
                    is Resource.Success -> UiState.Success(res.data)
                    is Resource.Error -> UiState.Error(res.message)
                }
            }
        }

        viewModelScope.launch {
            repository.listPurchasePayments(purchaseId).collectLatest { res ->
                _paymentsState.value = when (res) {
                    is Resource.Loading -> UiState.Loading
                    is Resource.Success -> if (res.data.isEmpty()) UiState.Empty else UiState.Success(res.data)
                    is Resource.Error -> UiState.Error(res.message)
                }
            }
        }
    }

    private fun loadBanks() {
        viewModelScope.launch {
            bankRepository.listBanks().collectLatest { res ->
                if (res is Resource.Success) {
                    _banksState.value = res.data
                }
            }
        }
    }

    private fun observeMutations() {
        viewModelScope.launch {
            refreshCoordinator.refreshEvents.collectLatest { mutation ->
                if (mutation == DataMutation.PURCHASES || mutation == DataMutation.ALL) {
                    loadPurchaseDetails()
                }
            }
        }
    }

    fun recordPayment(
        amount: String,
        paymentMethod: String,
        bankId: String? = null,
        note: String? = null,
        date: String? = null
    ) {
        val amountVal = amount.trim().toDoubleOrNull()
        if (amountVal == null || amountVal <= 0.0) {
            _paymentActionState.value = UiState.Error("Payment amount must be greater than zero")
            return
        }

        val currentPurchase = (_purchaseState.value as? UiState.Success)?.data
        val pendingVal = currentPurchase?.totalPending?.toDoubleOrNull() ?: Double.MAX_VALUE
        if (amountVal > pendingVal) {
            _paymentActionState.value = UiState.Error("Payment amount cannot exceed pending purchase balance")
            return
        }

        if (paymentMethod == "bank" && bankId.isNullOrBlank()) {
            _paymentActionState.value = UiState.Error("Please select a bank account for bank payment")
            return
        }

        if (_paymentActionState.value is UiState.Loading) return

        val bankObj = _banksState.value.firstOrNull { it.id == bankId }
        val req = RecordSupplierPaymentRequest(
            amount = String.format(java.util.Locale.US, "%.2f", amountVal),
            paymentMethod = paymentMethod,
            bankId = bankId?.trim()?.ifBlank { null },
            bankName = bankObj?.bankName,
            paymentDate = date?.trim()?.ifBlank { null },
            note = note?.trim()?.ifBlank { null }
        )

        viewModelScope.launch {
            _paymentActionState.value = UiState.Loading
            repository.recordPurchasePayment(purchaseId, req).collectLatest { res ->
                when (res) {
                    is Resource.Loading -> _paymentActionState.value = UiState.Loading
                    is Resource.Success -> {
                        _paymentActionState.value = UiState.Success(Unit)
                        loadPurchaseDetails()
                    }
                    is Resource.Error -> _paymentActionState.value = UiState.Error(res.message)
                }
            }
        }
    }

    fun updatePurchase(
        item: String,
        quantity: String,
        totalAmount: String,
        totalPaid: String,
        customerId: String? = null
    ) {
        val trimmedItem = item.trim()
        if (trimmedItem.isBlank()) {
            _updateActionState.value = UiState.Error("Item description is required")
            return
        }

        val qty = quantity.trim().toIntOrNull() ?: 1
        if (qty < 1) {
            _updateActionState.value = UiState.Error("Quantity must be at least 1")
            return
        }

        val totalVal = totalAmount.trim().toDoubleOrNull()
        if (totalVal == null || totalVal <= 0.0) {
            _updateActionState.value = UiState.Error("Total amount must be greater than zero")
            return
        }

        val paidVal = totalPaid.trim().ifBlank { "0.00" }.toDoubleOrNull() ?: 0.0
        if (paidVal < 0.0) {
            _updateActionState.value = UiState.Error("Paid amount cannot be negative")
            return
        }

        if (paidVal > totalVal) {
            _updateActionState.value = UiState.Error("Paid amount cannot exceed total purchase amount")
            return
        }

        if (_updateActionState.value is UiState.Loading) return

        val req = UpdatePurchaseRequest(
            customerId = customerId?.trim()?.ifBlank { null },
            item = trimmedItem,
            quantity = qty,
            totalAmount = String.format(java.util.Locale.US, "%.2f", totalVal),
            totalPaid = String.format(java.util.Locale.US, "%.2f", paidVal)
        )

        viewModelScope.launch {
            _updateActionState.value = UiState.Loading
            repository.updatePurchase(purchaseId, req).collectLatest { res ->
                when (res) {
                    is Resource.Loading -> _updateActionState.value = UiState.Loading
                    is Resource.Success -> {
                        _updateActionState.value = UiState.Success(Unit)
                        loadPurchaseDetails()
                    }
                    is Resource.Error -> _updateActionState.value = UiState.Error(res.message)
                }
            }
        }
    }

    fun deletePurchase() {
        if (_deleteActionState.value is UiState.Loading) return

        viewModelScope.launch {
            _deleteActionState.value = UiState.Loading
            repository.deletePurchase(purchaseId).collectLatest { res ->
                when (res) {
                    is Resource.Loading -> _deleteActionState.value = UiState.Loading
                    is Resource.Success -> _deleteActionState.value = UiState.Success(Unit)
                    is Resource.Error -> _deleteActionState.value = UiState.Error(res.message)
                }
            }
        }
    }

    fun clearActionStates() {
        _paymentActionState.value = UiState.Idle
        _updateActionState.value = UiState.Idle
        _deleteActionState.value = UiState.Idle
    }
}
