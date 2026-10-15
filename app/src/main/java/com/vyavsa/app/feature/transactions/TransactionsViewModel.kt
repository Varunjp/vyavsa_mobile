package com.vyavsa.app.feature.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vyavsa.app.core.common.Resource
import com.vyavsa.app.core.common.UiState
import com.vyavsa.app.core.model.BankPaymentSplit
import com.vyavsa.app.core.model.CashTransaction
import com.vyavsa.app.core.model.CounterSale
import com.vyavsa.app.core.model.CreateCounterSaleRequest
import com.vyavsa.app.core.model.CreateExpenseRequest
import com.vyavsa.app.core.model.CreateLineSaleRequest
import com.vyavsa.app.core.model.CreatePurchaseRequest
import com.vyavsa.app.core.model.Customer
import com.vyavsa.app.core.model.Employee
import com.vyavsa.app.core.model.Expense
import com.vyavsa.app.core.model.LineSale
import com.vyavsa.app.core.model.PlanTransaction
import com.vyavsa.app.core.model.Purchase
import com.vyavsa.app.core.model.TenantBank
import com.vyavsa.app.core.security.TokenManager
import com.vyavsa.app.core.sync.DataMutation
import com.vyavsa.app.core.sync.DataRefreshCoordinator
import com.vyavsa.app.feature.bank.BankRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class TransactionTab {
    SALES,
    PURCHASES,
    EXPENSES,
    CASH_BOOK,
    BILLING
}

enum class SaleType {
    LINE_SALE,
    COUNTER_SALE
}

@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val repository: TransactionsRepository,
    private val bankRepository: BankRepository,
    private val refreshCoordinator: DataRefreshCoordinator,
    val tokenManager: TokenManager? = null
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(TransactionTab.SALES)
    val selectedTab: StateFlow<TransactionTab> = _selectedTab.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _lineSalesState = MutableStateFlow<UiState<List<LineSale>>>(UiState.Loading)
    val lineSalesState: StateFlow<UiState<List<LineSale>>> = _lineSalesState.asStateFlow()

    private val _counterSalesState = MutableStateFlow<UiState<List<CounterSale>>>(UiState.Loading)
    val counterSalesState: StateFlow<UiState<List<CounterSale>>> = _counterSalesState.asStateFlow()

    private val _purchasesState = MutableStateFlow<UiState<List<Purchase>>>(UiState.Loading)
    val purchasesState: StateFlow<UiState<List<Purchase>>> = _purchasesState.asStateFlow()

    private val _expensesState = MutableStateFlow<UiState<List<Expense>>>(UiState.Loading)
    val expensesState: StateFlow<UiState<List<Expense>>> = _expensesState.asStateFlow()

    private val _cashTransactionsState = MutableStateFlow<UiState<List<CashTransaction>>>(UiState.Loading)
    val cashTransactionsState: StateFlow<UiState<List<CashTransaction>>> = _cashTransactionsState.asStateFlow()

    private val _billingTransactionsState = MutableStateFlow<UiState<List<PlanTransaction>>>(UiState.Loading)
    val billingTransactionsState: StateFlow<UiState<List<PlanTransaction>>> = _billingTransactionsState.asStateFlow()

    private val _customersState = MutableStateFlow<UiState<List<Customer>>>(UiState.Idle)
    val customersState: StateFlow<UiState<List<Customer>>> = _customersState.asStateFlow()

    private val _banksState = MutableStateFlow<UiState<List<TenantBank>>>(UiState.Idle)
    val banksState: StateFlow<UiState<List<TenantBank>>> = _banksState.asStateFlow()

    private val _employeesState = MutableStateFlow<UiState<List<Employee>>>(UiState.Idle)
    val employeesState: StateFlow<UiState<List<Employee>>> = _employeesState.asStateFlow()

    private val _dateFilter = MutableStateFlow<String?>(null)
    val dateFilter: StateFlow<String?> = _dateFilter.asStateFlow()

    private val _createActionState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val createActionState: StateFlow<UiState<Unit>> = _createActionState.asStateFlow()

    private var searchJob: Job? = null

    val isAdmin: Boolean
        get() = tokenManager?.isTenantAdmin() ?: false

    init {
        loadData()
        loadCustomers()
        loadBanks()
        loadEmployees()
        observeRefreshEvents()
    }

    private fun observeRefreshEvents() {
        viewModelScope.launch {
            refreshCoordinator.refreshEvents.collect { mutation ->
                when (mutation) {
                    DataMutation.SALES, DataMutation.EXPENSES, DataMutation.PURCHASES, DataMutation.CASH, DataMutation.ALL -> {
                        loadData()
                        loadCustomers()
                        loadBanks()
                        loadEmployees()
                    }
                    DataMutation.BANK -> {
                        loadBanks()
                    }
                    DataMutation.DUES -> {
                        loadCustomers()
                    }
                    DataMutation.STAFF -> {
                        loadEmployees()
                        loadData()
                    }
                }
            }
        }
    }

    fun selectTab(tab: TransactionTab) {
        _selectedTab.value = tab
    }

    fun onSearchChange(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(220L)
            loadFilterableData()
        }
    }

    fun setDateFilter(date: String?) {
        _dateFilter.value = date
        searchJob?.cancel()
        loadFilterableData()
    }

    private fun loadFilterableData() {
        val query = _searchQuery.value.ifBlank { null }
        val date = _dateFilter.value?.ifBlank { null }

        viewModelScope.launch {
            repository.listLineSales(date = date, search = query).collect { res ->
                _lineSalesState.value = when (res) {
                    is Resource.Loading -> if (_lineSalesState.value is UiState.Success) _lineSalesState.value else UiState.Loading
                    is Resource.Success -> if (res.data.isEmpty()) UiState.Empty else UiState.Success(res.data)
                    is Resource.Error -> UiState.Error(res.message)
                }
            }
        }

        viewModelScope.launch {
            repository.listCounterSales(date = date, search = query).collect { res ->
                _counterSalesState.value = when (res) {
                    is Resource.Loading -> if (_counterSalesState.value is UiState.Success) _counterSalesState.value else UiState.Loading
                    is Resource.Success -> if (res.data.isEmpty()) UiState.Empty else UiState.Success(res.data)
                    is Resource.Error -> UiState.Error(res.message)
                }
            }
        }

        viewModelScope.launch {
            repository.listPurchases(date = date, search = query).collect { res ->
                _purchasesState.value = when (res) {
                    is Resource.Loading -> if (_purchasesState.value is UiState.Success) _purchasesState.value else UiState.Loading
                    is Resource.Success -> if (res.data.isEmpty()) UiState.Empty else UiState.Success(res.data)
                    is Resource.Error -> UiState.Error(res.message)
                }
            }
        }

        viewModelScope.launch {
            repository.listExpenses(date = date, search = query).collect { res ->
                _expensesState.value = when (res) {
                    is Resource.Loading -> if (_expensesState.value is UiState.Success) _expensesState.value else UiState.Loading
                    is Resource.Success -> if (res.data.isEmpty()) UiState.Empty else UiState.Success(res.data)
                    is Resource.Error -> UiState.Error(res.message)
                }
            }
        }
    }

    fun loadData() {
        loadFilterableData()

        viewModelScope.launch {
            repository.listCashTransactions().collect { res ->
                _cashTransactionsState.value = when (res) {
                    is Resource.Loading -> if (_cashTransactionsState.value is UiState.Success) _cashTransactionsState.value else UiState.Loading
                    is Resource.Success -> if (res.data.isEmpty()) UiState.Empty else UiState.Success(res.data)
                    is Resource.Error -> UiState.Error(res.message)
                }
            }
        }

        if (isAdmin) {
            viewModelScope.launch {
                repository.listSubscriptionTransactions().collect { res ->
                    _billingTransactionsState.value = when (res) {
                        is Resource.Loading -> if (_billingTransactionsState.value is UiState.Success) _billingTransactionsState.value else UiState.Loading
                        is Resource.Success -> if (res.data.isEmpty()) UiState.Empty else UiState.Success(res.data)
                        is Resource.Error -> UiState.Error(res.message)
                    }
                }
            }
        }
    }

    fun loadCustomers() {
        viewModelScope.launch {
            repository.listCustomers().collect { res ->
                _customersState.value = when (res) {
                    is Resource.Loading -> if (_customersState.value is UiState.Success) _customersState.value else UiState.Loading
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
                    is Resource.Loading -> if (_banksState.value is UiState.Success) _banksState.value else UiState.Loading
                    is Resource.Success -> UiState.Success(res.data)
                    is Resource.Error -> UiState.Error(res.message)
                }
            }
        }
    }

    fun loadEmployees() {
        viewModelScope.launch {
            repository.listEmployees().collect { res ->
                _employeesState.value = when (res) {
                    is Resource.Loading -> if (_employeesState.value is UiState.Success) _employeesState.value else UiState.Loading
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
        bankPayments: List<BankPaymentSplit> = emptyList(),
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
        if (cash < 0.0) {
            _createActionState.value = UiState.Error("Payment amounts cannot be negative")
            return
        }

        val effectiveBankSplits: List<BankPaymentSplit>
        val effectiveBankTotal: Double
        val resolvedBankId: String?

        if (bankPayments.isNotEmpty()) {
            for (bp in bankPayments) {
                if (bp.bankId.isBlank()) {
                    _createActionState.value = UiState.Error("Please select a bank account for all bank payment rows")
                    return
                }
                val amt = bp.amount.trim().toDoubleOrNull()
                if (amt == null || amt <= 0.0) {
                    _createActionState.value = UiState.Error("Bank payment amount must be greater than zero")
                    return
                }
            }

            val seenBanks = mutableSetOf<String>()
            for (bp in bankPayments) {
                if (!seenBanks.add(bp.bankId.trim())) {
                    _createActionState.value = UiState.Error("Each bank account can only be selected once per sale")
                    return
                }
            }

            effectiveBankTotal = bankPayments.sumOf { it.amount.trim().toDoubleOrNull() ?: 0.0 }
            effectiveBankSplits = bankPayments
            resolvedBankId = bankPayments.first().bankId.trim()
        } else {
            val bank = bankAmount.trim().ifBlank { "0.00" }.toDoubleOrNull() ?: 0.0
            if (bank < 0.0) {
                _createActionState.value = UiState.Error("Payment amounts cannot be negative")
                return
            }

            val bId = bankId?.trim()?.ifBlank { null }
            if (bank > 0.0 && bId == null) {
                _createActionState.value = UiState.Error("Please select a bank account for bank payment")
                return
            }

            effectiveBankTotal = bank
            resolvedBankId = bId
            effectiveBankSplits = if (bId != null && bank > 0.0) {
                listOf(BankPaymentSplit(bankId = bId, amount = bankAmount.trim().ifBlank { "0.00" }))
            } else {
                emptyList()
            }
        }

        if (cash + effectiveBankTotal > total) {
            _createActionState.value = UiState.Error("Collected amount cannot exceed the total invoice amount")
            return
        }

        val formattedBankAmount = String.format(java.util.Locale.US, "%.2f", effectiveBankTotal)

        viewModelScope.launch {
            val req = CreateLineSaleRequest(
                customerId = customerId,
                route = route?.trim()?.ifBlank { null },
                salesman = salesman?.trim()?.ifBlank { null },
                totalAmount = totalAmount.trim(),
                totalCashIn = cashIn.trim().ifBlank { "0.00" },
                cashAmount = cashIn.trim().ifBlank { "0.00" },
                bankAmount = formattedBankAmount,
                bankId = resolvedBankId,
                bankPayments = effectiveBankSplits,
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
        bankId: String? = null,
        bankPayments: List<BankPaymentSplit> = emptyList()
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
        if (cashVal < 0.0) {
            _createActionState.value = UiState.Error("Payment amounts cannot be negative")
            return
        }

        val effectiveBankSplits: List<BankPaymentSplit>
        val effectiveBankTotal: Double
        val resolvedBankId: String?

        if (bankPayments.isNotEmpty()) {
            for (bp in bankPayments) {
                if (bp.bankId.isBlank()) {
                    _createActionState.value = UiState.Error("Please select a bank account for all bank payment rows")
                    return
                }
                val amt = bp.amount.trim().toDoubleOrNull()
                if (amt == null || amt <= 0.0) {
                    _createActionState.value = UiState.Error("Bank payment amount must be greater than zero")
                    return
                }
            }

            val seenBanks = mutableSetOf<String>()
            for (bp in bankPayments) {
                if (!seenBanks.add(bp.bankId.trim())) {
                    _createActionState.value = UiState.Error("Each bank account can only be selected once per sale")
                    return
                }
            }

            effectiveBankTotal = bankPayments.sumOf { it.amount.trim().toDoubleOrNull() ?: 0.0 }
            effectiveBankSplits = bankPayments
            resolvedBankId = bankPayments.first().bankId.trim()
        } else {
            val bankVal = bankAmount.trim().ifBlank { "0.00" }.toDoubleOrNull() ?: 0.0
            if (bankVal < 0.0) {
                _createActionState.value = UiState.Error("Payment amounts cannot be negative")
                return
            }

            val bId = bankId?.trim()?.ifBlank { null }
            if (bankVal > 0.0 && bId == null) {
                _createActionState.value = UiState.Error("Please select a bank account for bank payment")
                return
            }

            effectiveBankTotal = bankVal
            resolvedBankId = bId
            effectiveBankSplits = if (bId != null && bankVal > 0.0) {
                listOf(BankPaymentSplit(bankId = bId, amount = bankAmount.trim().ifBlank { "0.00" }))
            } else {
                emptyList()
            }
        }

        if (cashVal + effectiveBankTotal > total) {
            _createActionState.value = UiState.Error("Collected amount cannot exceed the total sale amount")
            return
        }

        val method = if (cashVal > 0.0 && effectiveBankTotal > 0.0) "split" else if (effectiveBankTotal > 0.0) "bank" else "cash"
        val itemPrice = price?.trim()?.ifBlank { totalAmount.trim() } ?: totalAmount.trim()
        val formattedBankAmount = String.format(java.util.Locale.US, "%.2f", effectiveBankTotal)

        viewModelScope.launch {
            val req = CreateCounterSaleRequest(
                item = trimmedItem,
                price = itemPrice,
                totalAmount = totalAmount.trim(),
                paymentMethod = method,
                cash = cash.trim().ifBlank { "0.00" },
                cashAmount = cash.trim().ifBlank { "0.00" },
                bankAmount = formattedBankAmount,
                bankId = resolvedBankId,
                account = formattedBankAmount,
                bankPayments = effectiveBankSplits
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

    fun createPurchase(
        customerId: String? = null,
        item: String,
        quantity: String,
        totalAmount: String,
        cashAmount: String = "0.00",
        bankPayments: List<BankPaymentSplit> = emptyList()
    ) {
        if (_createActionState.value is UiState.Loading) return

        val trimmedItem = item.trim()
        if (trimmedItem.isBlank() || totalAmount.isBlank()) {
            _createActionState.value = UiState.Error("Item description and total amount are required")
            return
        }

        val qty = quantity.trim().toIntOrNull() ?: 1
        if (qty < 1) {
            _createActionState.value = UiState.Error("Quantity must be at least 1")
            return
        }

        val total = totalAmount.trim().toDoubleOrNull()
        if (total == null || total <= 0.0) {
            _createActionState.value = UiState.Error("Total purchase amount must be greater than zero")
            return
        }

        val cashVal = cashAmount.trim().ifBlank { "0.00" }.toDoubleOrNull() ?: 0.0
        if (cashVal < 0.0) {
            _createActionState.value = UiState.Error("Cash payment cannot be negative")
            return
        }

        val effectiveBankSplits = mutableListOf<BankPaymentSplit>()
        var bankSum = 0.0
        if (bankPayments.isNotEmpty()) {
            val seenBanks = mutableSetOf<String>()
            for (bp in bankPayments) {
                if (bp.bankId.isBlank()) {
                    _createActionState.value = UiState.Error("Please select a bank account for all bank payment rows")
                    return
                }
                val amt = bp.amount.trim().toDoubleOrNull()
                if (amt == null || amt <= 0.0) {
                    _createActionState.value = UiState.Error("Bank payment amount must be greater than zero")
                    return
                }
                if (!seenBanks.add(bp.bankId.trim())) {
                    _createActionState.value = UiState.Error("Each bank account can only be selected once per purchase")
                    return
                }
                bankSum += amt
                effectiveBankSplits.add(bp)
            }
        }

        val totalPaidVal = cashVal + bankSum
        if (totalPaidVal > total) {
            _createActionState.value = UiState.Error("Total paid amount cannot exceed purchase bill amount")
            return
        }

        val formattedTotalPaid = String.format(java.util.Locale.US, "%.2f", totalPaidVal)
        val formattedCash = String.format(java.util.Locale.US, "%.2f", cashVal)

        viewModelScope.launch {
            val req = CreatePurchaseRequest(
                customerId = customerId?.trim()?.ifBlank { null },
                item = trimmedItem,
                quantity = qty,
                totalAmount = totalAmount.trim(),
                totalPaid = formattedTotalPaid,
                cashAmount = formattedCash,
                bankPayments = effectiveBankSplits
            )
            repository.createPurchase(req).collect { res ->
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

    fun createExpense(
        item: String,
        category: String?,
        amount: String,
        paymentMethod: String,
        employeeId: String? = null,
        paymentDate: String? = null,
        bankId: String? = null,
        referenceId: String? = null,
        bankPayments: List<BankPaymentSplit> = emptyList()
    ) {
        if (_createActionState.value is UiState.Loading) return

        val trimmedItem = item.trim()
        if (trimmedItem.isBlank() || amount.isBlank()) {
            _createActionState.value = UiState.Error("Expense item name and amount are required")
            return
        }

        val amt = amount.trim().toDoubleOrNull()
        if (amt == null || amt <= 0.0) {
            _createActionState.value = UiState.Error("Expense amount must be greater than zero")
            return
        }

        val isAdvance = category == "employee_advance"
        if (isAdvance && employeeId.isNullOrBlank()) {
            _createActionState.value = UiState.Error("Please select a staff member for the advance payment")
            return
        }

        if (paymentMethod == "bank" && bankId.isNullOrBlank() && bankPayments.isEmpty()) {
            _createActionState.value = UiState.Error("Please select a bank account for bank payment")
            return
        }

        viewModelScope.launch {
            val req = CreateExpenseRequest(
                item = trimmedItem,
                category = category?.ifBlank { null },
                employeeId = employeeId?.trim()?.ifBlank { null },
                totalAmount = amount.trim(),
                amount = amount.trim(),
                paymentMethod = paymentMethod,
                cashAmount = if (paymentMethod == "cash") amount.trim() else null,
                bankId = bankId?.trim()?.ifBlank { null },
                paymentDate = paymentDate?.trim()?.ifBlank { null },
                referenceId = referenceId?.trim()?.ifBlank { null },
                bankPayments = bankPayments
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
