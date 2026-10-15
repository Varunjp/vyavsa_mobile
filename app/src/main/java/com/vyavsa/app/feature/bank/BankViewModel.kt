package com.vyavsa.app.feature.bank

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vyavsa.app.core.common.Resource
import com.vyavsa.app.core.common.UiState
import com.vyavsa.app.core.model.CreateBankRequest
import com.vyavsa.app.core.model.TenantBank
import com.vyavsa.app.core.model.UpdateBankRequest
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
class BankViewModel @Inject constructor(
    private val repository: BankRepository,
    private val refreshCoordinator: DataRefreshCoordinator,
    private val tokenManager: TokenManager? = null
) : ViewModel() {

    val isTenantAdmin: Boolean
        get() = tokenManager?.isTenantAdmin() ?: true

    private val _banksState = MutableStateFlow<UiState<List<TenantBank>>>(UiState.Loading)
    val banksState: StateFlow<UiState<List<TenantBank>>> = _banksState.asStateFlow()

    private val _actionState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val actionState: StateFlow<UiState<Unit>> = _actionState.asStateFlow()

    init {
        loadBanks()
        observeRefreshEvents()
    }

    private fun observeRefreshEvents() {
        viewModelScope.launch {
            refreshCoordinator.refreshEvents.collect { mutation ->
                when (mutation) {
                    DataMutation.BANK,
                    DataMutation.SALES,
                    DataMutation.EXPENSES,
                    DataMutation.ALL -> {
                        loadBanks()
                    }
                    else -> Unit
                }
            }
        }
    }

    fun loadBanks() {
        viewModelScope.launch {
            repository.listBanks().collect { res ->
                _banksState.value = when (res) {
                    is Resource.Loading -> UiState.Loading
                    is Resource.Success -> if (res.data.isEmpty()) UiState.Empty else UiState.Success(res.data)
                    is Resource.Error -> UiState.Error(res.message, res.code)
                }
            }
        }
    }

    fun createBank(
        name: String,
        accountNumber: String?,
        ifsc: String?,
        openingBalance: String
    ) {
        if (_actionState.value is UiState.Loading) return

        val trimmedName = name.trim()
        if (trimmedName.isBlank()) {
            _actionState.value = UiState.Error("Bank and branch name is required")
            return
        }

        val balanceVal = openingBalance.trim().toDoubleOrNull()
        if (balanceVal == null || balanceVal < 0.0) {
            _actionState.value = UiState.Error("Please enter a valid opening balance (0.00 or greater)")
            return
        }

        viewModelScope.launch {
            val req = CreateBankRequest(
                bankName = trimmedName,
                accountNumber = accountNumber?.trim()?.ifBlank { null },
                ifsc = ifsc?.trim()?.ifBlank { null },
                openingBalance = openingBalance.trim().ifBlank { "0.00" },
                status = "active"
            )
            repository.createBank(req).collect { res ->
                _actionState.value = when (res) {
                    is Resource.Loading -> UiState.Loading
                    is Resource.Success -> {
                        loadBanks()
                        UiState.Success(Unit)
                    }
                    is Resource.Error -> UiState.Error(res.message, res.code)
                }
            }
        }
    }

    fun updateBank(
        id: String,
        name: String,
        accountNumber: String?,
        ifsc: String?,
        status: String
    ) {
        if (_actionState.value is UiState.Loading) return

        val trimmedName = name.trim()
        if (trimmedName.isBlank()) {
            _actionState.value = UiState.Error("Bank name cannot be empty")
            return
        }

        viewModelScope.launch {
            val req = UpdateBankRequest(
                bankName = trimmedName,
                accountNumber = accountNumber?.trim()?.ifBlank { null },
                ifsc = ifsc?.trim()?.ifBlank { null },
                status = if (status.isNotBlank()) status else "active"
            )
            repository.updateBank(id, req).collect { res ->
                _actionState.value = when (res) {
                    is Resource.Loading -> UiState.Loading
                    is Resource.Success -> {
                        loadBanks()
                        UiState.Success(Unit)
                    }
                    is Resource.Error -> UiState.Error(res.message, res.code)
                }
            }
        }
    }

    fun deleteBank(id: String) {
        if (_actionState.value is UiState.Loading) return

        viewModelScope.launch {
            repository.deleteBank(id).collect { res ->
                _actionState.value = when (res) {
                    is Resource.Loading -> UiState.Loading
                    is Resource.Success -> {
                        loadBanks()
                        UiState.Success(Unit)
                    }
                    is Resource.Error -> UiState.Error(res.message, res.code)
                }
            }
        }
    }

    fun resetActionState() {
        _actionState.value = UiState.Idle
    }
}
