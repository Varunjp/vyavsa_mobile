package com.vyavsa.app.feature.dues

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vyavsa.app.core.common.Resource
import com.vyavsa.app.core.common.UiState
import com.vyavsa.app.core.model.Customer
import com.vyavsa.app.core.model.CustomerBalanceAdjustment
import com.vyavsa.app.core.model.CustomerStatement
import com.vyavsa.app.core.model.LineSale
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
class DuesViewModel @Inject constructor(
    private val repository: DuesRepository,
    private val refreshCoordinator: DataRefreshCoordinator,
    val tokenManager: TokenManager? = null
) : ViewModel() {

    private val _customersState = MutableStateFlow<UiState<List<Customer>>>(UiState.Loading)
    val customersState: StateFlow<UiState<List<Customer>>> = _customersState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCustomer = MutableStateFlow<UiState<Customer>>(UiState.Idle)
    val selectedCustomer: StateFlow<UiState<Customer>> = _selectedCustomer.asStateFlow()

    private val _selectedCustomerSales = MutableStateFlow<UiState<List<LineSale>>>(UiState.Idle)
    val selectedCustomerSales: StateFlow<UiState<List<LineSale>>> = _selectedCustomerSales.asStateFlow()

    private val _statementState = MutableStateFlow<UiState<CustomerStatement>>(UiState.Idle)
    val statementState: StateFlow<UiState<CustomerStatement>> = _statementState.asStateFlow()

    private val _adjustmentsState = MutableStateFlow<UiState<List<CustomerBalanceAdjustment>>>(UiState.Idle)
    val adjustmentsState: StateFlow<UiState<List<CustomerBalanceAdjustment>>> = _adjustmentsState.asStateFlow()

    private val _actionState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val actionState: StateFlow<UiState<Unit>> = _actionState.asStateFlow()

    var adminOverrideForTesting: Boolean? = null

    val isAdmin: Boolean
        get() = adminOverrideForTesting ?: (tokenManager?.isTenantAdmin() ?: false)

    init {
        loadCustomers()
        observeRefreshEvents()
    }

    private fun observeRefreshEvents() {
        viewModelScope.launch {
            refreshCoordinator.refreshEvents.collect { mutation ->
                when (mutation) {
                    DataMutation.DUES,
                    DataMutation.SALES,
                    DataMutation.ALL -> {
                        loadCustomers()
                    }
                    else -> Unit
                }
            }
        }
    }

    fun onSearchChange(query: String) {
        _searchQuery.value = query
        loadCustomers()
    }

    fun loadCustomers() {
        val query = _searchQuery.value.ifBlank { null }
        viewModelScope.launch {
            repository.listCustomers(query).collect { res ->
                _customersState.value = when (res) {
                    is Resource.Loading -> UiState.Loading
                    is Resource.Success -> if (res.data.isEmpty()) UiState.Empty else UiState.Success(res.data)
                    is Resource.Error -> UiState.Error(res.message)
                }
            }
        }
    }

    fun loadCustomerDetails(customerId: String) {
        viewModelScope.launch {
            repository.getCustomerById(customerId).collect { res ->
                _selectedCustomer.value = when (res) {
                    is Resource.Loading -> UiState.Loading
                    is Resource.Success -> UiState.Success(res.data)
                    is Resource.Error -> UiState.Error(res.message)
                }
            }
        }
        loadCustomerSales(customerId)
    }

    fun loadCustomerSales(customerId: String) {
        viewModelScope.launch {
            repository.listCustomerSales(customerId).collect { res ->
                _selectedCustomerSales.value = when (res) {
                    is Resource.Loading -> UiState.Loading
                    is Resource.Success -> if (res.data.isEmpty()) UiState.Empty else UiState.Success(res.data)
                    is Resource.Error -> UiState.Error(res.message)
                }
            }
        }
    }

    fun loadStatement(customerId: String) {
        viewModelScope.launch {
            repository.getCustomerStatement(customerId).collect { res ->
                _statementState.value = when (res) {
                    is Resource.Loading -> UiState.Loading
                    is Resource.Success -> UiState.Success(res.data)
                    is Resource.Error -> UiState.Error(res.message)
                }
            }
        }
    }

    fun loadAdjustments(customerId: String) {
        viewModelScope.launch {
            repository.listCustomerAdjustments(customerId).collect { res ->
                _adjustmentsState.value = when (res) {
                    is Resource.Loading -> UiState.Loading
                    is Resource.Success -> if (res.data.isEmpty()) UiState.Empty else UiState.Success(res.data)
                    is Resource.Error -> UiState.Error(res.message)
                }
            }
        }
    }

    fun createCustomer(name: String, phone: String?, openingBalance: String) {
        if (name.isBlank()) {
            _actionState.value = UiState.Error("Customer name is required")
            return
        }

        viewModelScope.launch {
            repository.createCustomer(name, phone, openingBalance).collect { res ->
                _actionState.value = when (res) {
                    is Resource.Loading -> UiState.Loading
                    is Resource.Success -> {
                        refreshCoordinator.notifyMutation(DataMutation.DUES)
                        loadCustomers()
                        UiState.Success(Unit)
                    }
                    is Resource.Error -> UiState.Error(res.message)
                }
            }
        }
    }

    fun updateCustomer(id: String, name: String?, phone: String?, status: String?) {
        if (!isAdmin) {
            _actionState.value = UiState.Error("Forbidden: Administrator privileges required")
            return
        }

        if (name != null && name.isBlank()) {
            _actionState.value = UiState.Error("Customer name cannot be empty")
            return
        }

        viewModelScope.launch {
            repository.updateCustomer(id, name, phone, status).collect { res ->
                _actionState.value = when (res) {
                    is Resource.Loading -> UiState.Loading
                    is Resource.Success -> {
                        refreshCoordinator.notifyMutation(DataMutation.DUES)
                        loadCustomers()
                        loadCustomerDetails(id)
                        UiState.Success(Unit)
                    }
                    is Resource.Error -> UiState.Error(res.message)
                }
            }
        }
    }

    fun deleteCustomer(id: String, onSuccess: () -> Unit = {}) {
        if (!isAdmin) {
            _actionState.value = UiState.Error("Forbidden: Administrator privileges required")
            return
        }

        viewModelScope.launch {
            repository.deleteCustomer(id).collect { res ->
                _actionState.value = when (res) {
                    is Resource.Loading -> UiState.Loading
                    is Resource.Success -> {
                        refreshCoordinator.notifyMutation(DataMutation.DUES)
                        loadCustomers()
                        onSuccess()
                        UiState.Success(Unit)
                    }
                    is Resource.Error -> UiState.Error(res.message)
                }
            }
        }
    }

    fun adjustBalance(id: String, newBalance: String?, adjustmentAmount: String?, reason: String) {
        if (!isAdmin) {
            _actionState.value = UiState.Error("Forbidden: Administrator privileges required")
            return
        }

        if (reason.isBlank()) {
            _actionState.value = UiState.Error("Reason is required for manual balance adjustment")
            return
        }

        viewModelScope.launch {
            repository.adjustCustomerBalance(id, newBalance, adjustmentAmount, reason).collect { res ->
                _actionState.value = when (res) {
                    is Resource.Loading -> UiState.Loading
                    is Resource.Success -> {
                        refreshCoordinator.notifyMutation(DataMutation.DUES)
                        loadCustomers()
                        loadCustomerDetails(id)
                        loadAdjustments(id)
                        UiState.Success(Unit)
                    }
                    is Resource.Error -> UiState.Error(res.message)
                }
            }
        }
    }

    fun recordPayment(customerId: String, amount: String, paymentMethod: String, note: String?) {
        if (amount.isBlank() || amount.toDoubleOrNull() ?: 0.0 <= 0.0) {
            _actionState.value = UiState.Error("Please enter a valid payment amount")
            return
        }

        viewModelScope.launch {
            repository.recordPayment(
                id = customerId,
                amount = amount,
                paymentMethod = paymentMethod,
                date = null,
                note = note
            ).collect { res ->
                _actionState.value = when (res) {
                    is Resource.Loading -> UiState.Loading
                    is Resource.Success -> {
                        refreshCoordinator.notifyMutation(DataMutation.DUES)
                        if (paymentMethod == "bank") {
                            refreshCoordinator.notifyMutation(DataMutation.BANK)
                        } else {
                            refreshCoordinator.notifyMutation(DataMutation.CASH)
                        }
                        loadCustomers()
                        loadCustomerDetails(customerId)
                        loadStatement(customerId)
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
