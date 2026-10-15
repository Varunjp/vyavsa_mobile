package com.vyavsa.app.feature.cash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vyavsa.app.core.common.Resource
import com.vyavsa.app.core.common.UiState
import com.vyavsa.app.core.model.CashBalanceResponse
import com.vyavsa.app.core.model.CashTransaction
import com.vyavsa.app.core.sync.DataMutation
import com.vyavsa.app.core.sync.DataRefreshCoordinator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CashViewModel @Inject constructor(
    private val repository: CashRepository,
    private val refreshCoordinator: DataRefreshCoordinator
) : ViewModel() {

    private val _balanceState = MutableStateFlow<UiState<CashBalanceResponse>>(UiState.Loading)
    val balanceState: StateFlow<UiState<CashBalanceResponse>> = _balanceState.asStateFlow()

    private val _transactionsState = MutableStateFlow<UiState<List<CashTransaction>>>(UiState.Loading)
    val transactionsState: StateFlow<UiState<List<CashTransaction>>> = _transactionsState.asStateFlow()

    private val _actionState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val actionState: StateFlow<UiState<Unit>> = _actionState.asStateFlow()

    init {
        loadData()
        observeRefreshEvents()
    }

    private fun observeRefreshEvents() {
        viewModelScope.launch {
            refreshCoordinator.refreshEvents.collect { mutation ->
                when (mutation) {
                    DataMutation.CASH,
                    DataMutation.SALES,
                    DataMutation.EXPENSES,
                    DataMutation.ALL -> {
                        loadData()
                    }
                    else -> Unit
                }
            }
        }
    }

    fun loadData() {
        viewModelScope.launch {
            repository.getCashBalance().collect { res ->
                _balanceState.value = when (res) {
                    is Resource.Loading -> UiState.Loading
                    is Resource.Success -> UiState.Success(res.data)
                    is Resource.Error -> UiState.Error(res.message)
                }
            }
        }

        viewModelScope.launch {
            repository.listCashTransactions().collect { res ->
                _transactionsState.value = when (res) {
                    is Resource.Loading -> UiState.Loading
                    is Resource.Success -> if (res.data.isEmpty()) UiState.Empty else UiState.Success(res.data)
                    is Resource.Error -> UiState.Error(res.message)
                }
            }
        }
    }

    fun addCash(amount: String, description: String?) {
        if (amount.isBlank() || amount.toDoubleOrNull() ?: 0.0 <= 0.0) {
            _actionState.value = UiState.Error("Please enter a valid amount")
            return
        }

        viewModelScope.launch {
            repository.addCash(amount, description).collect { res ->
                _actionState.value = when (res) {
                    is Resource.Loading -> UiState.Loading
                    is Resource.Success -> {
                        refreshCoordinator.notifyMutation(DataMutation.CASH)
                        loadData()
                        UiState.Success(Unit)
                    }
                    is Resource.Error -> UiState.Error(res.message)
                }
            }
        }
    }

    fun adjustCash(actualBalance: String, reason: String) {
        if (actualBalance.isBlank() || reason.isBlank()) {
            _actionState.value = UiState.Error("Actual cash balance and reconciliation reason are required")
            return
        }

        viewModelScope.launch {
            repository.adjustCash(actualBalance, reason).collect { res ->
                _actionState.value = when (res) {
                    is Resource.Loading -> UiState.Loading
                    is Resource.Success -> {
                        refreshCoordinator.notifyMutation(DataMutation.CASH)
                        loadData()
                        UiState.Success(Unit)
                    }
                    is Resource.Error -> UiState.Error(res.message)
                }
            }
        }
    }

    fun resetActionState() {
        _actionState.value = UiState.Idle
    }
}
