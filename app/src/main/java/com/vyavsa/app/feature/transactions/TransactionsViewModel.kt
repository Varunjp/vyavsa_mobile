package com.vyavsa.app.feature.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vyavsa.app.core.common.Resource
import com.vyavsa.app.core.common.UiState
import com.vyavsa.app.core.model.CounterSale
import com.vyavsa.app.core.model.CreateCounterSaleRequest
import com.vyavsa.app.core.model.CreateExpenseRequest
import com.vyavsa.app.core.model.CreateLineSaleRequest
import com.vyavsa.app.core.model.Customer
import com.vyavsa.app.core.model.Expense
import com.vyavsa.app.core.model.LineSale
import com.vyavsa.app.core.model.TenantBank
import com.vyavsa.app.core.sync.DataMutation
import com.vyavsa.app.core.sync.DataRefreshCoordinator
import com.vyavsa.app.feature.bank.BankRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class TransactionTab {
    SALES,
    EXPENSES
}

enum class SaleType {
    LINE_SALE,
    COUNTER_SALE
}

@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val repository: TransactionsRepository,
    private val bankRepository: BankRepository,
    private val refreshCoordinator: DataRefreshCoordinator
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(TransactionTab.SALES)
    val selectedTab: StateFlow<TransactionTab> = _selectedTab.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _lineSalesState = MutableStateFlow<UiState<List<LineSale>>>(UiState.Loading)
    val lineSalesState: StateFlow<UiState<List<LineSale>>> = _lineSalesState.asStateFlow()

    private val _counterSalesState = MutableStateFlow<UiState<List<CounterSale>>>(UiState.Loading)
    val counterSalesState: StateFlow<UiState<List<CounterSale>>> = _counterSalesState.asStateFlow()

    private val _expensesState = MutableStateFlow<UiState<List<Expense>>>(UiState.Loading)
    val expensesState: StateFlow<UiState<List<Expense>>> = _expensesState.asStateFlow()

    private val _customersState = MutableStateFlow<UiState<List<Customer>>>(UiState.Idle)
    val customersState: StateFlow<UiState<List<Customer>>> = _customersState.asStateFlow()

    private val _banksState = MutableStateFlow<UiState<List<TenantBank>>>(UiState.Idle)
    val banksState: StateFlow<UiState<List<TenantBank>>> = _banksState.asStateFlow()

    private val _createActionState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val createActionState: StateFlow<UiState<Unit>> = _createActionState.asStateFlow()

    init {
        loadData()
        loadCustomers()
        loadBanks()
        observeRefreshEvents()
    }

    private fun observeRefreshEvents() {
        viewModelScope.launch {
            refreshCoordinator.refreshEvents.collect { mutation ->
                when (mutation) {
                    DataMutation.SALES, DataMutation.EXPENSES, DataMutation.ALL -> {
                        loadData()
                        loadCustomers()
                        loadBanks()
                    }
                    DataMutation.BANK -> {
                        loadBanks()
                    }
                    DataMutation.DUES -> {
                        loadCustomers()
                    }
                    else -> Unit
                }
            }
        }
    }

    fun selectTab(tab: TransactionTab) {
        _selectedTab.value = tab
    }

    fun onSearchChange(query: String) {
        _searchQuery.value = query
        loadData()
    }

    fun loadData() {
        val query = _searchQuery.value.ifBlank { null }

        viewModelScope.launch {
            repository.listLineSales(date = null, search = query).collect { res ->
                _lineSalesState.value = when (res) {
                    is Resource.Loading -> UiState.Loading
                    is Resource.Success -> if (res.data.isEmpty()) UiState.Empty else UiState.Success(res.data)
                    is Resource.Error -> UiState.Error(res.message)
                }
            }
        }

        viewModelScope.launch {
            repository.listCounterSales(date = null, search = query).collect { res ->
                _counterSalesState.value = when (res) {
                    is Resource.Loading -> UiState.Loading
                    is Resource.Success -> if (res.data.isEmpty()) UiState.Empty else UiState.Success(res.data)
                    is Resource.Error -> UiState.Error(res.message)
                }
            }
        }

        viewModelScope.launch {
            repository.listExpenses(date = null, search = query).collect { res ->
                _expensesState.value = when (res) {
                    is Resource.Loading -> UiState.Loading
                    is Resource.Success -> if (res.data.isEmpty()) UiState.Empty else UiState.Success(res.data)
                    is Resource.Error -> UiState.Error(res.message)
                }
            }
        }
    }

    fun loadCustomers() {
        viewModelScope.launch {
            repository.listCustomers().collect { res ->
                _customersState.value = when (res) {
                    is Resource.Loading -> UiState.Loading
                    is Resource.Success -> UiState.Success(res.data)
                    is Resource.Error -> UiState.Error(res.message)
                }
            }
        }
    }

    fun loadBanks() {
        viewModelScope.launch {
            bankRepository.listBanks().collect { res ->
                _banksState.value = when (res) {
                    is Resource.Loading -> UiState.Loading
                    is Resource.Success -> UiState.Success(res.data)
                    is Resource.Error -> UiState.Error(res.message)
                }
            }
        }
    }

    fun createLineSale(
        customerId: String,
        route: String? = null,
        salesman: String? = null,
        totalAmount: String,
        cashIn: String,
        bankAmount: String = "0.00",
        bankId: String? = null,
        note: String? = null
    ) {
        if (_createActionState.value is UiState.Loading) return

        if (customerId.isBlank() || totalAmount.isBlank()) {
            _createActionState.value = UiState.Error("Customer and total amount are required")
            return
        }

        val total = totalAmount.trim().toDoubleOrNull()
        if (total == null || total <= 0.0) {
            _createActionState.value = UiState.Error("Total invoice amount must be greater than zero")
            return
        }

        val cash = cashIn.trim().ifBlank { "0.00" }.toDoubleOrNull() ?: 0.0
        val bank = bankAmount.trim().ifBlank { "0.00" }.toDoubleOrNull() ?: 0.0

        if (cash < 0.0 || bank < 0.0) {
            _createActionState.value = UiState.Error("Payment amounts cannot be negative")
            return
        }

        val resolvedBankId = bankId?.trim()?.ifBlank { null }
        if (bank > 0.0 && resolvedBankId == null) {
            _createActionState.value = UiState.Error("Please select a bank account for bank payment")
            return
        }

        if (cash + bank > total) {
            _createActionState.value = UiState.Error("Collected amount cannot exceed the total invoice amount")
            return
        }

        viewModelScope.launch {
            val req = CreateLineSaleRequest(
                customerId = customerId,
                route = route?.trim()?.ifBlank { null },
                salesman = salesman?.trim()?.ifBlank { null },
                totalAmount = totalAmount.trim(),
                totalCashIn = cashIn.trim().ifBlank { "0.00" },
                cashAmount = cashIn.trim().ifBlank { "0.00" },
                bankAmount = bankAmount.trim().ifBlank { "0.00" },
                bankId = resolvedBankId,
                note = note?.trim()?.ifBlank { null }
            )
            repository.createLineSale(req).collect { res ->
                _createActionState.value = when (res) {
                    is Resource.Loading -> UiState.Loading
                    is Resource.Success -> {
                        loadData()
                        UiState.Success(Unit)
                    }
                    is Resource.Error -> UiState.Error(res.message, res.code)
                }
            }
        }
    }

    fun createCounterSale(
        item: String,
        price: String?,
        totalAmount: String,
        cash: String,
        bankAmount: String = "0.00",
        bankId: String? = null
    ) {
        if (_createActionState.value is UiState.Loading) return

        val trimmedItem = item.trim()
        if (trimmedItem.isBlank() || totalAmount.isBlank()) {
            _createActionState.value = UiState.Error("Item description and total amount are required")
            return
        }

        val total = totalAmount.trim().toDoubleOrNull()
        if (total == null || total <= 0.0) {
            _createActionState.value = UiState.Error("Total sale amount must be greater than zero")
            return
        }

        val cashVal = cash.trim().ifBlank { "0.00" }.toDoubleOrNull() ?: 0.0
        val bankVal = bankAmount.trim().ifBlank { "0.00" }.toDoubleOrNull() ?: 0.0

        if (cashVal < 0.0 || bankVal < 0.0) {
            _createActionState.value = UiState.Error("Payment amounts cannot be negative")
            return
        }

        val resolvedBankId = bankId?.trim()?.ifBlank { null }
        if (bankVal > 0.0 && resolvedBankId == null) {
            _createActionState.value = UiState.Error("Please select a bank account for bank payment")
            return
        }

        if (cashVal + bankVal > total) {
            _createActionState.value = UiState.Error("Collected amount cannot exceed the total sale amount")
            return
        }

        val method = if (cashVal > 0.0 && bankVal > 0.0) "split" else if (bankVal > 0.0) "bank" else "cash"
        val itemPrice = price?.trim()?.ifBlank { totalAmount.trim() } ?: totalAmount.trim()

        viewModelScope.launch {
            val req = CreateCounterSaleRequest(
                item = trimmedItem,
                price = itemPrice,
                totalAmount = totalAmount.trim(),
                paymentMethod = method,
                cash = cash.trim().ifBlank { "0.00" },
                cashAmount = cash.trim().ifBlank { "0.00" },
                bankAmount = bankAmount.trim().ifBlank { "0.00" },
                bankId = resolvedBankId
            )
            repository.createCounterSale(req).collect { res ->
                _createActionState.value = when (res) {
                    is Resource.Loading -> UiState.Loading
                    is Resource.Success -> {
                        loadData()
                        UiState.Success(Unit)
                    }
                    is Resource.Error -> UiState.Error(res.message, res.code)
                }
            }
        }
    }

    fun createExpense(item: String, category: String?, amount: String, paymentMethod: String) {
        if (_createActionState.value is UiState.Loading) return

        if (item.isBlank() || amount.isBlank()) {
            _createActionState.value = UiState.Error("Expense item name and amount are required")
            return
        }

        val amt = amount.trim().toDoubleOrNull()
        if (amt == null || amt <= 0.0) {
            _createActionState.value = UiState.Error("Expense amount must be greater than zero")
            return
        }

        viewModelScope.launch {
            val req = CreateExpenseRequest(
                item = item.trim(),
                category = category?.ifBlank { null },
                totalAmount = amount.trim(),
                paymentMethod = paymentMethod
            )
            repository.createExpense(req).collect { res ->
                _createActionState.value = when (res) {
                    is Resource.Loading -> UiState.Loading
                    is Resource.Success -> {
                        loadData()
                        UiState.Success(Unit)
                    }
                    is Resource.Error -> UiState.Error(res.message, res.code)
                }
            }
        }
    }

    fun resetCreateState() {
        _createActionState.value = UiState.Idle
    }
}
