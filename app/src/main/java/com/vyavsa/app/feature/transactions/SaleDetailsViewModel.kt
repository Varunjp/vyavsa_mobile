package com.vyavsa.app.feature.transactions

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vyavsa.app.core.common.Resource
import com.vyavsa.app.core.common.UiState
import com.vyavsa.app.core.model.CounterSale
import com.vyavsa.app.core.model.LineSale
import com.vyavsa.app.core.model.RecordSalePaymentRequest
import com.vyavsa.app.core.model.TenantBank
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

sealed class SaleDetailData {
    data class Line(val sale: LineSale) : SaleDetailData()
    data class Counter(val sale: CounterSale) : SaleDetailData()
}

@HiltViewModel
class SaleDetailsViewModel @Inject constructor(
    private val repository: TransactionsRepository,
    private val bankRepository: BankRepository,
    private val refreshCoordinator: DataRefreshCoordinator,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val saleType: String = savedStateHandle["saleType"] ?: "line"
    val saleId: String = savedStateHandle["saleId"] ?: ""

    private val _detailState = MutableStateFlow<UiState<SaleDetailData>>(UiState.Idle)
    val detailState: StateFlow<UiState<SaleDetailData>> = _detailState.asStateFlow()

    private val _paymentActionState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val paymentActionState: StateFlow<UiState<Unit>> = _paymentActionState.asStateFlow()

    private val _banksState = MutableStateFlow<List<TenantBank>>(emptyList())
    val banksState: StateFlow<List<TenantBank>> = _banksState.asStateFlow()

    init {
        loadSaleDetails()
        loadBanks()
        observeMutations()
    }

    fun loadSaleDetails() {
        if (saleId.isBlank()) {
            _detailState.value = UiState.Error("Invalid sale identifier")
            return
        }

        viewModelScope.launch {
            if (saleType == "counter") {
                repository.getCounterSaleById(saleId).collectLatest { res ->
                    when (res) {
                        is Resource.Loading -> _detailState.value = UiState.Loading
                        is Resource.Success -> {
                            val sale = res.data
                            _detailState.value = UiState.Success(SaleDetailData.Counter(sale))
                        }
                        is Resource.Error -> _detailState.value = UiState.Error(res.message)
                    }
                }
            } else {
                repository.getLineSaleById(saleId).collectLatest { res ->
                    when (res) {
                        is Resource.Loading -> _detailState.value = UiState.Loading
                        is Resource.Success -> {
                            val sale = res.data
                            _detailState.value = UiState.Success(SaleDetailData.Line(sale))
                        }
                        is Resource.Error -> _detailState.value = UiState.Error(res.message)
                    }
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
                if (mutation == DataMutation.SALES || mutation == DataMutation.ALL) {
                    loadSaleDetails()
                }
            }
        }
    }

    fun recordPayment(
        amount: String,
        paymentMethod: String,
        bankId: String? = null,
        note: String? = null
    ) {
        val amountVal = amount.toDoubleOrNull()
        if (amountVal == null || amountVal <= 0.0) {
            _paymentActionState.value = UiState.Error("Payment amount must be greater than zero")
            return
        }

        if (paymentMethod == "bank" && bankId.isNullOrBlank()) {
            _paymentActionState.value = UiState.Error("Please select a bank account for bank payment")
            return
        }

        if (_paymentActionState.value is UiState.Loading) return

        val bankObj = _banksState.value.firstOrNull { it.id == bankId }
        val req = RecordSalePaymentRequest(
            amount = String.format("%.2f", amountVal),
            paymentMethod = paymentMethod,
            bankId = bankId,
            bankName = bankObj?.bankName,
            note = note
        )

        viewModelScope.launch {
            _paymentActionState.value = UiState.Loading
            if (saleType == "counter") {
                repository.recordCounterSalePayment(saleId, req).collectLatest { res ->
                    when (res) {
                        is Resource.Loading -> _paymentActionState.value = UiState.Loading
                        is Resource.Success -> {
                            _paymentActionState.value = UiState.Success(Unit)
                            loadSaleDetails()
                        }
                        is Resource.Error -> _paymentActionState.value = UiState.Error(res.message)
                    }
                }
            } else {
                repository.recordLineSalePayment(saleId, req).collectLatest { res ->
                    when (res) {
                        is Resource.Loading -> _paymentActionState.value = UiState.Loading
                        is Resource.Success -> {
                            _paymentActionState.value = UiState.Success(Unit)
                            loadSaleDetails()
                        }
                        is Resource.Error -> _paymentActionState.value = UiState.Error(res.message)
                    }
                }
            }
        }
    }

    fun clearPaymentActionState() {
        _paymentActionState.value = UiState.Idle
    }
}
