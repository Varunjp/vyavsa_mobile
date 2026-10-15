package com.vyavsa.app.feature.dues

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vyavsa.app.core.common.Resource
import com.vyavsa.app.core.common.UiState
import com.vyavsa.app.core.model.Customer
import com.vyavsa.app.core.model.CustomerStatement
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DuesViewModel @Inject constructor(
    private val repository: DuesRepository
) : ViewModel() {

    private val _customersState = MutableStateFlow<UiState<List<Customer>>>(UiState.Loading)
    val customersState: StateFlow<UiState<List<Customer>>> = _customersState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _statementState = MutableStateFlow<UiState<CustomerStatement>>(UiState.Idle)
    val statementState: StateFlow<UiState<CustomerStatement>> = _statementState.asStateFlow()

    private val _actionState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val actionState: StateFlow<UiState<Unit>> = _actionState.asStateFlow()

    init {
        loadCustomers()
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
                        loadCustomers()
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
                        loadCustomers()
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
