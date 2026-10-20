package com.vyavsa.app.feature.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vyavsa.app.core.common.Resource
import com.vyavsa.app.core.common.SaleCalculationUtils
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
import com.vyavsa.app.core.model.Product
import com.vyavsa.app.core.model.Purchase
import com.vyavsa.app.core.model.SaleItemRequest
import com.vyavsa.app.core.model.TenantBank
import com.vyavsa.app.core.security.TokenManager
import com.vyavsa.app.core.sync.DataMutation
import com.vyavsa.app.core.sync.DataRefreshCoordinator
import com.vyavsa.app.feature.bank.BankRepository
import com.vyavsa.app.feature.products.ProductRepository
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.vyavsa.app.core.common.FormatUtils
import com.vyavsa.app.core.designsystem.LogoMemoryCache
import com.vyavsa.app.core.document.InvoiceShareManager
import com.vyavsa.app.core.model.TenantProfile
import com.vyavsa.app.core.printer.InvoiceDataMapper
import com.vyavsa.app.core.printer.InvoicePrintAdapter
import com.vyavsa.app.core.printer.InvoicePrintData
import com.vyavsa.app.core.printer.PaperWidth
import com.vyavsa.app.core.printer.PrintJobState
import com.vyavsa.app.core.printer.PrintJobStatus
import com.vyavsa.app.core.printer.PrinterConnectionType
import com.vyavsa.app.core.printer.PrinterDevice
import com.vyavsa.app.core.printer.PrinterPreferences
import com.vyavsa.app.core.printer.ThermalInvoiceFormatter
import com.vyavsa.app.core.printer.ThermalPrinterManager
import com.vyavsa.app.feature.dashboard.DashboardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    val tokenManager: TokenManager? = null,
    private val productRepository: ProductRepository? = null,
    val dashboardRepository: DashboardRepository? = null,
    val printerManager: ThermalPrinterManager? = null,
    val printerPreferences: PrinterPreferences? = null
) : ViewModel() {

    constructor(
        repository: TransactionsRepository,
        bankRepository: BankRepository,
        refreshCoordinator: DataRefreshCoordinator
    ) : this(repository, bankRepository, refreshCoordinator, null, null, null, null, null)

    constructor(
        repository: TransactionsRepository,
        bankRepository: BankRepository,
        refreshCoordinator: DataRefreshCoordinator,
        tokenManager: TokenManager?,
        productRepository: ProductRepository?
    ) : this(repository, bankRepository, refreshCoordinator, tokenManager, productRepository, null, null, null)

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

    private val _productsState = MutableStateFlow<UiState<List<Product>>>(UiState.Idle)
    val productsState: StateFlow<UiState<List<Product>>> = _productsState.asStateFlow()

    private val _dateFilter = MutableStateFlow<String?>(null)
    val dateFilter: StateFlow<String?> = _dateFilter.asStateFlow()

    private val _createActionState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val createActionState: StateFlow<UiState<Unit>> = _createActionState.asStateFlow()

    private val _companyProfile = MutableStateFlow<TenantProfile?>(null)
    val companyProfile: StateFlow<TenantProfile?> = _companyProfile.asStateFlow()

    private val _activePrintingSaleId = MutableStateFlow<String?>(null)
    val activePrintingSaleId: StateFlow<String?> = _activePrintingSaleId.asStateFlow()

    private val _activeSharingSaleId = MutableStateFlow<String?>(null)
    val activeSharingSaleId: StateFlow<String?> = _activeSharingSaleId.asStateFlow()

    private val _printJobState = MutableStateFlow(PrintJobState())
    val printJobState: StateFlow<PrintJobState> = _printJobState.asStateFlow()

    private val _activePrintDialogData = MutableStateFlow<InvoicePrintData?>(null)
    val activePrintDialogData: StateFlow<InvoicePrintData?> = _activePrintDialogData.asStateFlow()

    private val _pairedPrinters = MutableStateFlow<List<PrinterDevice>>(emptyList())
    val pairedPrinters: StateFlow<List<PrinterDevice>> = _pairedPrinters.asStateFlow()

    private val _usbPrinters = MutableStateFlow<List<PrinterDevice>>(emptyList())
    val usbPrinters: StateFlow<List<PrinterDevice>> = _usbPrinters.asStateFlow()

    private val _selectedPrinter = MutableStateFlow<PrinterDevice?>(null)
    val selectedPrinter: StateFlow<PrinterDevice?> = _selectedPrinter.asStateFlow()

    private val _selectedConnectionType = MutableStateFlow(
        printerPreferences?.selectedConnectionType?.value ?: PrinterConnectionType.BLUETOOTH
    )
    val selectedConnectionType: StateFlow<PrinterConnectionType> = _selectedConnectionType.asStateFlow()

    private val _networkPrinterIp = MutableStateFlow(
        printerPreferences?.savedNetworkPrinterIp?.value ?: "192.168.1.100:9100"
    )
    val networkPrinterIp: StateFlow<String> = _networkPrinterIp.asStateFlow()

    private val _paperWidth = MutableStateFlow(
        printerPreferences?.paperWidth?.value ?: PaperWidth.WIDTH_58MM
    )
    val paperWidth: StateFlow<PaperWidth> = _paperWidth.asStateFlow()

    private var searchJob: Job? = null

    val isAdmin: Boolean
        get() = tokenManager?.isTenantAdmin() ?: false

    init {
        loadData()
        loadCustomers()
        loadBanks()
        loadEmployees()
        loadProducts()
        loadCompanyProfile()
        loadPrinters()
        observeRefreshEvents()
    }

    fun loadProducts(search: String? = null) {
        if (productRepository == null) return
        viewModelScope.launch {
            productRepository.listProducts(page = 1, pageSize = 100, search = search).collect { res ->
                _productsState.value = when (res) {
                    is Resource.Loading -> UiState.Loading
                    is Resource.Success -> if (res.data.isEmpty()) UiState.Empty else UiState.Success(res.data)
                    is Resource.Error -> UiState.Error(res.message, res.code)
                }
            }
        }
    }

    private fun observeRefreshEvents() {
        viewModelScope.launch {
            refreshCoordinator.refreshEvents.collect { mutation ->
                val query = _searchQuery.value.ifBlank { null }
                val date = _dateFilter.value?.ifBlank { null }
                when (mutation) {
                    DataMutation.SALES -> {
                        loadLineSalesData(date, query)
                        loadCounterSalesData(date, query)
                        loadCashTransactionsData()
                        loadCustomers()
                    }
                    DataMutation.PURCHASES -> {
                        loadPurchasesData(date, query)
                        loadCashTransactionsData()
                    }
                    DataMutation.EXPENSES -> {
                        loadExpensesData(date, query)
                        loadCashTransactionsData()
                    }
                    DataMutation.CASH -> {
                        loadCashTransactionsData()
                    }
                    DataMutation.PRODUCTS -> {
                        loadProducts()
                    }
                    DataMutation.BANK -> {
                        loadBanks()
                    }
                    DataMutation.DUES -> {
                        loadCustomers()
                        loadLineSalesData(date, query)
                    }
                    DataMutation.STAFF -> {
                        loadEmployees()
                    }
                    DataMutation.PROFILE -> {
                        loadCompanyProfile()
                    }
                    DataMutation.ALL -> {
                        loadData()
                        loadCustomers()
                        loadBanks()
                        loadEmployees()
                        loadProducts()
                        loadCompanyProfile()
                    }
                    else -> {}
                }
            }
        }
    }

    fun selectTab(tab: TransactionTab) {
        val prev = _selectedTab.value
        _selectedTab.value = tab
        if (prev != tab && (_searchQuery.value.isNotBlank() || _dateFilter.value != null)) {
            loadFilterableData(activeTabOnly = true)
        }
    }

    fun onSearchChange(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(220L)
            loadFilterableData(activeTabOnly = true)
        }
    }

    fun setDateFilter(date: String?) {
        _dateFilter.value = date
        searchJob?.cancel()
        loadFilterableData(activeTabOnly = true)
    }

    fun loadLineSalesData(date: String? = null, query: String? = null) {
        val q = query ?: _searchQuery.value.ifBlank { null }
        val d = date ?: _dateFilter.value?.ifBlank { null }
        viewModelScope.launch {
            repository.listLineSales(date = d, search = q).collect { res ->
                _lineSalesState.value = when (res) {
                    is Resource.Loading -> if (_lineSalesState.value is UiState.Success) _lineSalesState.value else UiState.Loading
                    is Resource.Success -> if (res.data.isEmpty()) UiState.Empty else UiState.Success(res.data)
                    is Resource.Error -> UiState.Error(res.message)
                }
            }
        }
    }

    fun loadCounterSalesData(date: String? = null, query: String? = null) {
        val q = query ?: _searchQuery.value.ifBlank { null }
        val d = date ?: _dateFilter.value?.ifBlank { null }
        viewModelScope.launch {
            repository.listCounterSales(date = d, search = q).collect { res ->
                _counterSalesState.value = when (res) {
                    is Resource.Loading -> if (_counterSalesState.value is UiState.Success) _counterSalesState.value else UiState.Loading
                    is Resource.Success -> if (res.data.isEmpty()) UiState.Empty else UiState.Success(res.data)
                    is Resource.Error -> UiState.Error(res.message)
                }
            }
        }
    }

    fun loadPurchasesData(date: String? = null, query: String? = null) {
        val q = query ?: _searchQuery.value.ifBlank { null }
        val d = date ?: _dateFilter.value?.ifBlank { null }
        viewModelScope.launch {
            repository.listPurchases(date = d, search = q).collect { res ->
                _purchasesState.value = when (res) {
                    is Resource.Loading -> if (_purchasesState.value is UiState.Success) _purchasesState.value else UiState.Loading
                    is Resource.Success -> if (res.data.isEmpty()) UiState.Empty else UiState.Success(res.data)
                    is Resource.Error -> UiState.Error(res.message)
                }
            }
        }
    }

    fun loadExpensesData(date: String? = null, query: String? = null) {
        val q = query ?: _searchQuery.value.ifBlank { null }
        val d = date ?: _dateFilter.value?.ifBlank { null }
        viewModelScope.launch {
            repository.listExpenses(date = d, search = q).collect { res ->
                _expensesState.value = when (res) {
                    is Resource.Loading -> if (_expensesState.value is UiState.Success) _expensesState.value else UiState.Loading
                    is Resource.Success -> if (res.data.isEmpty()) UiState.Empty else UiState.Success(res.data)
                    is Resource.Error -> UiState.Error(res.message)
                }
            }
        }
    }

    fun loadCashTransactionsData() {
        viewModelScope.launch {
            repository.listCashTransactions().collect { res ->
                _cashTransactionsState.value = when (res) {
                    is Resource.Loading -> if (_cashTransactionsState.value is UiState.Success) _cashTransactionsState.value else UiState.Loading
                    is Resource.Success -> if (res.data.isEmpty()) UiState.Empty else UiState.Success(res.data)
                    is Resource.Error -> UiState.Error(res.message)
                }
            }
        }
    }

    fun loadBillingTransactionsData() {
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

    private fun loadFilterableData(activeTabOnly: Boolean = false) {
        val query = _searchQuery.value.ifBlank { null }
        val date = _dateFilter.value?.ifBlank { null }

        if (!activeTabOnly) {
            loadLineSalesData(date, query)
            loadCounterSalesData(date, query)
            loadPurchasesData(date, query)
            loadExpensesData(date, query)
            return
        }

        when (_selectedTab.value) {
            TransactionTab.SALES -> {
                loadLineSalesData(date, query)
                loadCounterSalesData(date, query)
            }
            TransactionTab.PURCHASES -> {
                loadPurchasesData(date, query)
            }
            TransactionTab.EXPENSES -> {
                loadExpensesData(date, query)
            }
            TransactionTab.CASH_BOOK -> {
                loadCashTransactionsData()
            }
            TransactionTab.BILLING -> {
                loadBillingTransactionsData()
            }
        }
    }

    fun loadData() {
        loadFilterableData(activeTabOnly = false)
        loadCashTransactionsData()
        loadBillingTransactionsData()
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
        note: String? = null,
        items: List<SaleItemRequest> = emptyList(),
        subtotal: String? = null,
        discount: String? = null
    ) {
        if (_createActionState.value is UiState.Loading) return

        if (customerId.isBlank()) {
            _createActionState.value = UiState.Error("Customer and total amount are required")
            return
        }

        val calculatedItemsSubtotal = if (items.isNotEmpty()) {
            items.sumOf { (it.quantity.toDoubleOrNull() ?: 0.0) * (it.unitPrice.toDoubleOrNull() ?: 0.0) }
        } else null

        val rawSubtotal = when {
            calculatedItemsSubtotal != null -> String.format(java.util.Locale.US, "%.2f", calculatedItemsSubtotal)
            !subtotal.isNullOrBlank() -> subtotal.trim()
            else -> totalAmount.trim()
        }

        if (rawSubtotal.isBlank()) {
            _createActionState.value = UiState.Error("Customer and total amount are required")
            return
        }

        val calcResult = SaleCalculationUtils.calculateResult(rawSubtotal, discount)
        if (!calcResult.isValid) {
            _createActionState.value = UiState.Error(calcResult.errorMessage ?: "Invalid discount or subtotal")
            return
        }

        val subtotalVal = calcResult.subtotal.toDouble()
        if (subtotalVal <= 0.0) {
            _createActionState.value = UiState.Error("Total invoice amount must be greater than zero")
            return
        }

        val effectiveTotalAmount = calcResult.finalAmountString
        val total = calcResult.finalAmount.toDouble()

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
                subtotal = calcResult.subtotalString,
                discount = calcResult.discountString,
                discountAmount = calcResult.discountString,
                totalAmount = effectiveTotalAmount,
                totalCashIn = cashIn.trim().ifBlank { "0.00" },
                cashAmount = cashIn.trim().ifBlank { "0.00" },
                bankAmount = formattedBankAmount,
                bankId = resolvedBankId,
                bankPayments = effectiveBankSplits,
                note = note?.trim()?.ifBlank { null },
                items = items
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
        bankPayments: List<BankPaymentSplit> = emptyList(),
        items: List<SaleItemRequest> = emptyList(),
        subtotal: String? = null,
        discount: String? = null
    ) {
        if (_createActionState.value is UiState.Loading) return

        val calculatedItemsSubtotal = if (items.isNotEmpty()) {
            items.sumOf { (it.quantity.toDoubleOrNull() ?: 0.0) * (it.unitPrice.toDoubleOrNull() ?: 0.0) }
        } else null

        val rawSubtotal = when {
            calculatedItemsSubtotal != null -> String.format(java.util.Locale.US, "%.2f", calculatedItemsSubtotal)
            !subtotal.isNullOrBlank() -> subtotal.trim()
            else -> totalAmount.trim()
        }

        val trimmedItem = item.trim()
        if ((trimmedItem.isBlank() && items.isEmpty()) || rawSubtotal.isBlank()) {
            _createActionState.value = UiState.Error("Item description and total amount are required")
            return
        }

        val calcResult = SaleCalculationUtils.calculateResult(rawSubtotal, discount)
        if (!calcResult.isValid) {
            _createActionState.value = UiState.Error(calcResult.errorMessage ?: "Invalid discount or subtotal")
            return
        }

        val subtotalVal = calcResult.subtotal.toDouble()
        if (subtotalVal <= 0.0) {
            _createActionState.value = UiState.Error("Total sale amount must be greater than zero")
            return
        }

        val effectiveTotalAmount = calcResult.finalAmountString
        val total = calcResult.finalAmount.toDouble()

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
        val itemPrice = price?.trim()?.ifBlank { calcResult.subtotalString } ?: calcResult.subtotalString
        val formattedBankAmount = String.format(java.util.Locale.US, "%.2f", effectiveBankTotal)

        viewModelScope.launch {
            val req = CreateCounterSaleRequest(
                item = trimmedItem.ifBlank { if (items.isNotEmpty()) items.first().productId else "Sale Item" },
                price = itemPrice,
                subtotal = calcResult.subtotalString,
                discount = calcResult.discountString,
                discountAmount = calcResult.discountString,
                totalAmount = effectiveTotalAmount,
                paymentMethod = method,
                cash = cash.trim().ifBlank { "0.00" },
                cashAmount = cash.trim().ifBlank { "0.00" },
                bankAmount = formattedBankAmount,
                bankId = resolvedBankId,
                account = formattedBankAmount,
                bankPayments = effectiveBankSplits,
                items = items
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

    fun loadCompanyProfile() {
        dashboardRepository?.let { repo ->
            viewModelScope.launch {
                repo.getProfile().collectLatest { res ->
                    if (res is Resource.Success) {
                        _companyProfile.value = res.data
                    }
                }
            }
        }
    }

    fun getCachedLogoBitmap(): Bitmap? {
        val url = _companyProfile.value?.logoUrl
        if (url.isNullOrBlank()) return null
        val resolved = FormatUtils.resolveMediaUrl(url) ?: return null
        return LogoMemoryCache.get(resolved)?.asAndroidBitmap()
    }

    suspend fun ensureCompanyProfile(): TenantProfile? {
        val current = _companyProfile.value
        if (current != null) return current
        val repo = dashboardRepository ?: return null
        var prof: TenantProfile? = null
        try {
            repo.getProfile().collect { res ->
                if (res is Resource.Success) {
                    prof = res.data
                    _companyProfile.value = res.data
                }
            }
        } catch (_: Exception) {}
        return prof
    }

    suspend fun resolveFullLineSale(sale: LineSale): LineSale {
        if (sale.items.isNotEmpty()) return sale
        var fullSale: LineSale? = null
        try {
            repository.getLineSaleById(sale.id).collect { res ->
                if (res is Resource.Success) {
                    fullSale = res.data
                }
            }
        } catch (_: Exception) {}
        val resolved = fullSale ?: sale
        val currentListState = _lineSalesState.value
        if (currentListState is UiState.Success && fullSale != null) {
            val updatedList = currentListState.data.map {
                if (it.id == resolved.id) resolved else it
            }
            _lineSalesState.value = UiState.Success(updatedList)
        }
        return resolved
    }

    suspend fun resolveFullCounterSale(sale: CounterSale): CounterSale {
        if (sale.items.isNotEmpty()) return sale
        var fullSale: CounterSale? = null
        try {
            repository.getCounterSaleById(sale.id).collect { res ->
                if (res is Resource.Success) {
                    fullSale = res.data
                }
            }
        } catch (_: Exception) {}
        val resolved = fullSale ?: sale
        val currentListState = _counterSalesState.value
        if (currentListState is UiState.Success && fullSale != null) {
            val updatedList = currentListState.data.map {
                if (it.id == resolved.id) resolved else it
            }
            _counterSalesState.value = UiState.Success(updatedList)
        }
        return resolved
    }

    fun loadPrinters() {
        _pairedPrinters.value = printerManager?.getPairedPrinters() ?: emptyList()
        _usbPrinters.value = printerManager?.getConnectedUsbPrinters() ?: emptyList()
        val savedAddr = printerPreferences?.selectedPrinterAddress?.value
        val savedType = printerPreferences?.selectedConnectionType?.value ?: PrinterConnectionType.BLUETOOTH
        _selectedConnectionType.value = savedType
        if (savedType == PrinterConnectionType.BLUETOOTH) {
            _selectedPrinter.value = _pairedPrinters.value.firstOrNull { it.address == savedAddr } ?: _pairedPrinters.value.firstOrNull()
        } else if (savedType == PrinterConnectionType.USB) {
            _selectedPrinter.value = _usbPrinters.value.firstOrNull { it.address == savedAddr } ?: _usbPrinters.value.firstOrNull()
        } else if (savedType == PrinterConnectionType.NETWORK) {
            val netIp = _networkPrinterIp.value
            _selectedPrinter.value = PrinterDevice("Network Thermal Printer", netIp, true, PrinterConnectionType.NETWORK)
        }
    }

    fun selectPrinter(printer: PrinterDevice) {
        _selectedPrinter.value = printer
        printerPreferences?.saveSelectedPrinter(printer)
    }

    fun setConnectionType(type: PrinterConnectionType) {
        _selectedConnectionType.value = type
        printerPreferences?.saveConnectionType(type)
        when (type) {
            PrinterConnectionType.BLUETOOTH -> {
                _pairedPrinters.value = printerManager?.getPairedPrinters() ?: emptyList()
                val savedAddr = printerPreferences?.selectedPrinterAddress?.value
                _selectedPrinter.value = _pairedPrinters.value.firstOrNull { it.address == savedAddr } ?: _pairedPrinters.value.firstOrNull()
                printerPreferences?.saveSelectedPrinter(_selectedPrinter.value)
            }
            PrinterConnectionType.USB -> {
                _usbPrinters.value = printerManager?.getConnectedUsbPrinters() ?: emptyList()
                val savedAddr = printerPreferences?.selectedPrinterAddress?.value
                _selectedPrinter.value = _usbPrinters.value.firstOrNull { it.address == savedAddr } ?: _usbPrinters.value.firstOrNull()
                printerPreferences?.saveSelectedPrinter(_selectedPrinter.value)
            }
            PrinterConnectionType.NETWORK -> {
                val dev = PrinterDevice("Network Thermal Printer", _networkPrinterIp.value, true, PrinterConnectionType.NETWORK)
                _selectedPrinter.value = dev
                printerPreferences?.saveSelectedPrinter(dev)
            }
        }
    }

    fun setNetworkPrinterIp(ip: String) {
        val clean = ip.trim()
        _networkPrinterIp.value = clean
        printerPreferences?.saveNetworkPrinterIp(clean)
        val dev = PrinterDevice("Network Thermal Printer", clean, true, PrinterConnectionType.NETWORK)
        _selectedPrinter.value = dev
        printerPreferences?.saveSelectedPrinter(dev)
    }

    fun setPaperWidth(width: PaperWidth) {
        _paperWidth.value = width
        printerPreferences?.savePaperWidth(width)
    }

    fun openPrintDialogForLineSale(sale: LineSale) {
        if (_activePrintingSaleId.value != null) return
        _activePrintingSaleId.value = sale.id
        viewModelScope.launch {
            try {
                val fullSale = resolveFullLineSale(sale)
                val profile = ensureCompanyProfile() ?: _companyProfile.value
                val logoBmp = getCachedLogoBitmap()
                val data = InvoiceDataMapper.fromLineSale(fullSale, profile, logoBmp)
                _activePrintDialogData.value = data
                _printJobState.value = PrintJobState(status = PrintJobStatus.IDLE)
                loadPrinters()
            } finally {
                _activePrintingSaleId.value = null
            }
        }
    }

    fun openPrintDialogForCounterSale(sale: CounterSale) {
        if (_activePrintingSaleId.value != null) return
        _activePrintingSaleId.value = sale.id
        viewModelScope.launch {
            try {
                val fullSale = resolveFullCounterSale(sale)
                val profile = ensureCompanyProfile() ?: _companyProfile.value
                val logoBmp = getCachedLogoBitmap()
                val data = InvoiceDataMapper.fromCounterSale(fullSale, profile, logoBmp)
                _activePrintDialogData.value = data
                _printJobState.value = PrintJobState(status = PrintJobStatus.IDLE)
                loadPrinters()
            } finally {
                _activePrintingSaleId.value = null
            }
        }
    }

    fun dismissPrintDialog() {
        _activePrintDialogData.value = null
        _activePrintingSaleId.value = null
    }

    fun printCurrentDialogReceipt(context: Context) {
        val printData = _activePrintDialogData.value ?: return
        val printer = _selectedPrinter.value
        val width = _paperWidth.value

        if (printer == null) {
            InvoicePrintAdapter.print(context, printData)
            dismissPrintDialog()
            return
        }

        val saleId = printData.invoiceNumber
        _activePrintingSaleId.value = saleId
        _printJobState.value = PrintJobState(status = PrintJobStatus.CONNECTING, message = "Connecting to ${printer.name}...")

        viewModelScope.launch {
            try {
                val rawBytes = ThermalInvoiceFormatter.formatEscPosReceipt(printData, width)
                _printJobState.value = PrintJobState(status = PrintJobStatus.PRINTING, message = "Printing receipt...")
                val res = printerManager?.printReceipt(printer, rawBytes)
                if (res != null && res.isSuccess) {
                    _printJobState.value = PrintJobState(status = PrintJobStatus.SUCCESS, message = "Printed successfully")
                    delay(800)
                    dismissPrintDialog()
                } else {
                    _printJobState.value = PrintJobState(
                        status = PrintJobStatus.ERROR,
                        message = res?.exceptionOrNull()?.message ?: "Thermal print failed. Check printer connection or paper."
                    )
                }
            } catch (e: Exception) {
                _printJobState.value = PrintJobState(
                    status = PrintJobStatus.ERROR,
                    message = e.message ?: "Printer error"
                )
            } finally {
                _activePrintingSaleId.value = null
            }
        }
    }

    fun shareLineSalePdf(context: Context, sale: LineSale, onComplete: ((Boolean, String?) -> Unit)? = null) {
        if (_activeSharingSaleId.value != null) return
        _activeSharingSaleId.value = sale.id

        viewModelScope.launch {
            try {
                val fullSale = resolveFullLineSale(sale)
                val profile = ensureCompanyProfile() ?: _companyProfile.value
                val logoBmp = getCachedLogoBitmap()
                val printData = InvoiceDataMapper.fromLineSale(fullSale, profile, logoBmp)
                val res = InvoiceShareManager.shareInvoicePdf(context, printData)
                if (res.isSuccess) {
                    onComplete?.invoke(true, null)
                } else {
                    onComplete?.invoke(false, res.exceptionOrNull()?.message ?: "Failed to share PDF")
                }
            } catch (e: Exception) {
                onComplete?.invoke(false, e.message ?: "Error sharing PDF")
            } finally {
                _activeSharingSaleId.value = null
            }
        }
    }

    fun shareCounterSalePdf(context: Context, sale: CounterSale, onComplete: ((Boolean, String?) -> Unit)? = null) {
        if (_activeSharingSaleId.value != null) return
        _activeSharingSaleId.value = sale.id

        viewModelScope.launch {
            try {
                val fullSale = resolveFullCounterSale(sale)
                val profile = ensureCompanyProfile() ?: _companyProfile.value
                val logoBmp = getCachedLogoBitmap()
                val printData = InvoiceDataMapper.fromCounterSale(fullSale, profile, logoBmp)
                val res = InvoiceShareManager.shareInvoicePdf(context, printData)
                if (res.isSuccess) {
                    onComplete?.invoke(true, null)
                } else {
                    onComplete?.invoke(false, res.exceptionOrNull()?.message ?: "Failed to share PDF")
                }
            } catch (e: Exception) {
                onComplete?.invoke(false, e.message ?: "Error sharing PDF")
            } finally {
                _activeSharingSaleId.value = null
            }
        }
    }
}
