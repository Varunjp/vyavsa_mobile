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
import com.vyavsa.app.core.model.CustomerBalance
import com.vyavsa.app.core.model.Employee
import com.vyavsa.app.core.model.Expense
import com.vyavsa.app.core.model.LineSale
import com.vyavsa.app.core.model.PlanTransaction
import com.vyavsa.app.core.model.Product
import com.vyavsa.app.core.model.Purchase
import com.vyavsa.app.core.model.SaleItemRequest
import com.vyavsa.app.core.model.Supplier
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
import com.vyavsa.app.core.common.MessageType
import com.vyavsa.app.core.common.OneTimeEventEmitter
import com.vyavsa.app.core.common.UiMessage
import com.vyavsa.app.core.designsystem.LogoMemoryCache
import java.time.LocalDate
import java.time.DayOfWeek
import java.time.temporal.TemporalAdjusters
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.Flow
import com.vyavsa.app.core.document.InvoiceShareManager
import com.vyavsa.app.core.model.TenantProfile
import com.vyavsa.app.core.printer.InvoiceContentSettings
import com.vyavsa.app.core.printer.InvoiceDataMapper
import com.vyavsa.app.core.printer.InvoicePrintAdapter
import com.vyavsa.app.core.printer.InvoicePrintData
import com.vyavsa.app.core.printer.InvoiceTheme
import com.vyavsa.app.core.printer.PaperWidth
import com.vyavsa.app.core.printer.PrintJobState
import com.vyavsa.app.core.printer.PrintJobStatus
import com.vyavsa.app.core.printer.PrintTypography
import com.vyavsa.app.core.printer.PrinterConnectionType
import com.vyavsa.app.core.printer.PrinterDevice
import com.vyavsa.app.core.printer.PrinterPreferences
import com.vyavsa.app.core.printer.ThermalInvoiceFormatter
import com.vyavsa.app.core.printer.ThermalPrinterManager
import com.vyavsa.app.core.settings.SettingsPreferences
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

enum class SalesCategoryFilter(val label: String) {
    ALL("All Sales"),
    LINE("Line Sales"),
    COUNTER("Counter Sales")
}

enum class DateFilterType(val label: String) {
    TODAY("Today"),
    YESTERDAY("Yesterday"),
    THIS_WEEK("This Week"),
    THIS_MONTH("This Month"),
    CUSTOM("Custom Range"),
    ALL_TIME("All Time")
}

data class DateRangeSelection(
    val type: DateFilterType = DateFilterType.TODAY,
    val startDate: String? = FormatUtils.todayDateString(),
    val endDate: String? = FormatUtils.todayDateString()
) {
    val isCustom: Boolean get() = type == DateFilterType.CUSTOM
    val isAllTime: Boolean get() = type == DateFilterType.ALL_TIME
}

object DateFilterHelper {
    private val isoFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", java.util.Locale.US)

    fun calculateDateRange(type: DateFilterType, customStart: String? = null, customEnd: String? = null): DateRangeSelection {
        val today = LocalDate.now()
        return when (type) {
            DateFilterType.TODAY -> {
                val str = today.format(isoFormatter)
                DateRangeSelection(DateFilterType.TODAY, str, str)
            }
            DateFilterType.YESTERDAY -> {
                val str = today.minusDays(1).format(isoFormatter)
                DateRangeSelection(DateFilterType.YESTERDAY, str, str)
            }
            DateFilterType.THIS_WEEK -> {
                val start = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).format(isoFormatter)
                val end = today.format(isoFormatter)
                DateRangeSelection(DateFilterType.THIS_WEEK, start, end)
            }
            DateFilterType.THIS_MONTH -> {
                val start = today.withDayOfMonth(1).format(isoFormatter)
                val end = today.format(isoFormatter)
                DateRangeSelection(DateFilterType.THIS_MONTH, start, end)
            }
            DateFilterType.CUSTOM -> {
                DateRangeSelection(DateFilterType.CUSTOM, customStart, customEnd)
            }
            DateFilterType.ALL_TIME -> {
                DateRangeSelection(DateFilterType.ALL_TIME, null, null)
            }
        }
    }
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
    val printerPreferences: PrinterPreferences? = null,
    val settingsPreferences: SettingsPreferences? = null
) : ViewModel() {

    constructor(
        repository: TransactionsRepository,
        bankRepository: BankRepository,
        refreshCoordinator: DataRefreshCoordinator
    ) : this(repository, bankRepository, refreshCoordinator, null, null, null, null, null, null)

    constructor(
        repository: TransactionsRepository,
        bankRepository: BankRepository,
        refreshCoordinator: DataRefreshCoordinator,
        tokenManager: TokenManager?,
        productRepository: ProductRepository?
    ) : this(repository, bankRepository, refreshCoordinator, tokenManager, productRepository, null, null, null, null)

    constructor(
        repository: TransactionsRepository,
        bankRepository: BankRepository,
        refreshCoordinator: DataRefreshCoordinator,
        tokenManager: TokenManager?,
        productRepository: ProductRepository?,
        dashboardRepository: DashboardRepository?,
        printerManager: ThermalPrinterManager?,
        printerPreferences: PrinterPreferences?
    ) : this(repository, bankRepository, refreshCoordinator, tokenManager, productRepository, dashboardRepository, printerManager, printerPreferences, null)

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

    private val _customerSearchQuery = MutableStateFlow("")
    val customerSearchQuery: StateFlow<String> = _customerSearchQuery.asStateFlow()

    private val _suppliersState = MutableStateFlow<UiState<List<Supplier>>>(UiState.Idle)
    val suppliersState: StateFlow<UiState<List<Supplier>>> = _suppliersState.asStateFlow()

    private val _banksState = MutableStateFlow<UiState<List<TenantBank>>>(UiState.Idle)
    val banksState: StateFlow<UiState<List<TenantBank>>> = _banksState.asStateFlow()

    private val _employeesState = MutableStateFlow<UiState<List<Employee>>>(UiState.Idle)
    val employeesState: StateFlow<UiState<List<Employee>>> = _employeesState.asStateFlow()

    private val _productsState = MutableStateFlow<UiState<List<Product>>>(UiState.Idle)
    val productsState: StateFlow<UiState<List<Product>>> = _productsState.asStateFlow()

    private val _salesCategoryFilter = MutableStateFlow(SalesCategoryFilter.ALL)
    val salesCategoryFilter: StateFlow<SalesCategoryFilter> = _salesCategoryFilter.asStateFlow()

    private val _dateRangeSelection = MutableStateFlow(DateFilterHelper.calculateDateRange(DateFilterType.TODAY))
    val dateRangeSelection: StateFlow<DateRangeSelection> = _dateRangeSelection.asStateFlow()

    private val _selectedCustomer = MutableStateFlow<Customer?>(null)
    val selectedCustomer: StateFlow<Customer?> = _selectedCustomer.asStateFlow()

    private val _dateFilter = MutableStateFlow<String?>(FormatUtils.todayDateString())
    val dateFilter: StateFlow<String?> = _dateFilter.asStateFlow()

    private val eventEmitter = OneTimeEventEmitter()
    val uiEvents: Flow<UiMessage> = eventEmitter.events

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
    private var lineSalesJob: Job? = null
    private var counterSalesJob: Job? = null
    private var purchasesJob: Job? = null
    private var expensesJob: Job? = null
    private var lineSaleCustomerBalanceJob: Job? = null
    private var counterSaleCustomerBalanceJob: Job? = null

    private val _lineSaleCustomerBalanceState = MutableStateFlow<UiState<CustomerBalance>>(UiState.Idle)
    val lineSaleCustomerBalanceState: StateFlow<UiState<CustomerBalance>> = _lineSaleCustomerBalanceState.asStateFlow()

    private val _counterSaleCustomerBalanceState = MutableStateFlow<UiState<CustomerBalance>>(UiState.Idle)
    val counterSaleCustomerBalanceState: StateFlow<UiState<CustomerBalance>> = _counterSaleCustomerBalanceState.asStateFlow()

    fun sendUiMessage(text: String, type: MessageType = MessageType.INFO) {
        eventEmitter.emit(text, type)
    }

    fun resetPrintJobState() {
        _printJobState.value = PrintJobState(status = PrintJobStatus.IDLE)
    }

    val isAdmin: Boolean
        get() = tokenManager?.isTenantAdmin() ?: false

    init {
        loadData()
        loadCustomers()
        loadSuppliers()
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
                    is Resource.Success -> {
                        val distinct = res.data.distinctBy { it.id }
                        if (distinct.isEmpty()) UiState.Empty else UiState.Success(distinct)
                    }
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

    fun setSalesCategoryFilter(filter: SalesCategoryFilter) {
        if (_salesCategoryFilter.value == filter) return
        _salesCategoryFilter.value = filter
        loadFilterableData(activeTabOnly = true)
    }

    fun setDateRangeFilter(type: DateFilterType, customStart: String? = null, customEnd: String? = null) {
        val newRange = DateFilterHelper.calculateDateRange(type, customStart, customEnd)
        _dateRangeSelection.value = newRange
        _dateFilter.value = if (newRange.startDate == newRange.endDate) newRange.startDate else null
        searchJob?.cancel()
        loadFilterableData(activeTabOnly = true)
    }

    fun setSelectedCustomer(customer: Customer?) {
        if (_selectedCustomer.value == customer) return
        _selectedCustomer.value = customer
        searchJob?.cancel()
        loadFilterableData(activeTabOnly = true)
    }

    fun selectLineSaleCustomer(customer: Customer?) {
        lineSaleCustomerBalanceJob?.cancel()
        if (customer == null) {
            _lineSaleCustomerBalanceState.value = UiState.Idle
            return
        }
        // Immediately clear previous customer's balance to prevent stale cross-customer data
        _lineSaleCustomerBalanceState.value = UiState.Loading
        lineSaleCustomerBalanceJob = viewModelScope.launch {
            repository.getCustomerBalance(customer.id).collect { res ->
                _lineSaleCustomerBalanceState.value = when (res) {
                    is Resource.Loading -> UiState.Loading
                    is Resource.Success -> UiState.Success(res.data)
                    is Resource.Error -> UiState.Error(res.message, res.code)
                }
            }
        }
    }

    fun selectCounterSaleCustomer(customer: Customer?) {
        counterSaleCustomerBalanceJob?.cancel()
        if (customer == null) {
            _counterSaleCustomerBalanceState.value = UiState.Idle
            return
        }
        // Immediately clear previous customer's balance to prevent stale cross-customer data
        _counterSaleCustomerBalanceState.value = UiState.Loading
        counterSaleCustomerBalanceJob = viewModelScope.launch {
            repository.getCustomerBalance(customer.id).collect { res ->
                _counterSaleCustomerBalanceState.value = when (res) {
                    is Resource.Loading -> UiState.Loading
                    is Resource.Success -> UiState.Success(res.data)
                    is Resource.Error -> UiState.Error(res.message, res.code)
                }
            }
        }
    }

    fun retryFetchLineSaleCustomerBalance(customerId: String) {
        if (customerId.isBlank()) return
        lineSaleCustomerBalanceJob?.cancel()
        _lineSaleCustomerBalanceState.value = UiState.Loading
        lineSaleCustomerBalanceJob = viewModelScope.launch {
            repository.getCustomerBalance(customerId).collect { res ->
                _lineSaleCustomerBalanceState.value = when (res) {
                    is Resource.Loading -> UiState.Loading
                    is Resource.Success -> UiState.Success(res.data)
                    is Resource.Error -> UiState.Error(res.message, res.code)
                }
            }
        }
    }

    fun retryFetchCounterSaleCustomerBalance(customerId: String) {
        if (customerId.isBlank()) return
        counterSaleCustomerBalanceJob?.cancel()
        _counterSaleCustomerBalanceState.value = UiState.Loading
        counterSaleCustomerBalanceJob = viewModelScope.launch {
            repository.getCustomerBalance(customerId).collect { res ->
                _counterSaleCustomerBalanceState.value = when (res) {
                    is Resource.Loading -> UiState.Loading
                    is Resource.Success -> UiState.Success(res.data)
                    is Resource.Error -> UiState.Error(res.message, res.code)
                }
            }
        }
    }

    fun refreshCustomerBalance(customerId: String, saleType: SaleType = SaleType.LINE_SALE) {
        if (customerId.isBlank()) return
        if (saleType == SaleType.LINE_SALE) {
            lineSaleCustomerBalanceJob?.cancel()
            lineSaleCustomerBalanceJob = viewModelScope.launch {
                repository.getCustomerBalance(customerId).collect { res ->
                    if (res is Resource.Success) {
                        _lineSaleCustomerBalanceState.value = UiState.Success(res.data)
                    }
                }
            }
        } else {
            counterSaleCustomerBalanceJob?.cancel()
            counterSaleCustomerBalanceJob = viewModelScope.launch {
                repository.getCustomerBalance(customerId).collect { res ->
                    if (res is Resource.Success) {
                        _counterSaleCustomerBalanceState.value = UiState.Success(res.data)
                    }
                }
            }
        }
    }

    fun clearCustomerFilter() {
        setSelectedCustomer(null)
    }

    fun clearAllFilters() {
        _salesCategoryFilter.value = SalesCategoryFilter.ALL
        _selectedCustomer.value = null
        _searchQuery.value = ""
        val todayRange = DateFilterHelper.calculateDateRange(DateFilterType.TODAY)
        _dateRangeSelection.value = todayRange
        _dateFilter.value = todayRange.startDate
        searchJob?.cancel()
        loadFilterableData(activeTabOnly = true)
    }

    fun setDateFilter(date: String?) {
        _dateFilter.value = date
        if (date != null) {
            _dateRangeSelection.value = DateRangeSelection(DateFilterType.CUSTOM, date, date)
        } else {
            _dateRangeSelection.value = DateRangeSelection(DateFilterType.ALL_TIME, null, null)
        }
        searchJob?.cancel()
        loadFilterableData(activeTabOnly = true)
    }

    fun loadLineSalesData(
        date: String? = null,
        query: String? = null,
        fromDate: String? = null,
        toDate: String? = null,
        customerId: String? = null
    ) {
        val q = query ?: _searchQuery.value.ifBlank { null }
        val range = _dateRangeSelection.value
        val fDate = fromDate ?: if (date == null) range.startDate else null
        val tDate = toDate ?: if (date == null) range.endDate else null
        val d = date ?: if (fDate != null && fDate == tDate) fDate else null
        val cId = customerId ?: _selectedCustomer.value?.id

        lineSalesJob?.cancel()
        _lineSalesState.value = UiState.Loading
        lineSalesJob = viewModelScope.launch {
            repository.listLineSales(
                date = d,
                fromDate = if (d != null) null else fDate,
                toDate = if (d != null) null else tDate,
                search = q,
                customerId = cId
            ).collect { res ->
                _lineSalesState.value = when (res) {
                    is Resource.Loading -> UiState.Loading
                    is Resource.Success -> {
                        val distinct = res.data.distinctBy { it.id }
                        if (distinct.isEmpty()) UiState.Empty else UiState.Success(distinct)
                    }
                    is Resource.Error -> UiState.Error(res.message, res.code)
                }
            }
        }
    }

    fun loadCounterSalesData(
        date: String? = null,
        query: String? = null,
        fromDate: String? = null,
        toDate: String? = null,
        customerId: String? = null
    ) {
        val q = query ?: _searchQuery.value.ifBlank { null }
        val range = _dateRangeSelection.value
        val fDate = fromDate ?: if (date == null) range.startDate else null
        val tDate = toDate ?: if (date == null) range.endDate else null
        val d = date ?: if (fDate != null && fDate == tDate) fDate else null
        val cId = customerId ?: _selectedCustomer.value?.id

        counterSalesJob?.cancel()
        _counterSalesState.value = UiState.Loading
        counterSalesJob = viewModelScope.launch {
            repository.listCounterSales(
                date = d,
                fromDate = if (d != null) null else fDate,
                toDate = if (d != null) null else tDate,
                search = q,
                customerId = cId
            ).collect { res ->
                _counterSalesState.value = when (res) {
                    is Resource.Loading -> UiState.Loading
                    is Resource.Success -> {
                        val distinct = res.data.distinctBy { it.id }
                        if (distinct.isEmpty()) UiState.Empty else UiState.Success(distinct)
                    }
                    is Resource.Error -> UiState.Error(res.message, res.code)
                }
            }
        }
    }

    fun loadPurchasesData(date: String? = null, query: String? = null) {
        val q = query ?: _searchQuery.value.ifBlank { null }
        val d = date ?: _dateFilter.value?.ifBlank { null }
        purchasesJob?.cancel()
        _purchasesState.value = UiState.Loading
        purchasesJob = viewModelScope.launch {
            repository.listPurchases(date = d, search = q).collect { res ->
                _purchasesState.value = when (res) {
                    is Resource.Loading -> UiState.Loading
                    is Resource.Success -> {
                        val distinct = res.data.distinctBy { it.id }
                        if (distinct.isEmpty()) UiState.Empty else UiState.Success(distinct)
                    }
                    is Resource.Error -> UiState.Error(res.message, res.code)
                }
            }
        }
    }

    fun loadExpensesData(date: String? = null, query: String? = null) {
        val q = query ?: _searchQuery.value.ifBlank { null }
        val d = date ?: _dateFilter.value?.ifBlank { null }
        expensesJob?.cancel()
        _expensesState.value = UiState.Loading
        expensesJob = viewModelScope.launch {
            repository.listExpenses(date = d, search = q).collect { res ->
                _expensesState.value = when (res) {
                    is Resource.Loading -> UiState.Loading
                    is Resource.Success -> {
                        val distinct = res.data.distinctBy { it.id }
                        if (distinct.isEmpty()) UiState.Empty else UiState.Success(distinct)
                    }
                    is Resource.Error -> UiState.Error(res.message, res.code)
                }
            }
        }
    }

    fun loadCashTransactionsData() {
        viewModelScope.launch {
            repository.listCashTransactions().collect { res ->
                _cashTransactionsState.value = when (res) {
                    is Resource.Loading -> if (_cashTransactionsState.value is UiState.Success) _cashTransactionsState.value else UiState.Loading
                    is Resource.Success -> {
                        val distinct = res.data.distinctBy { it.id }
                        if (distinct.isEmpty()) UiState.Empty else UiState.Success(distinct)
                    }
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
                        is Resource.Success -> {
                        val distinct = res.data.distinctBy { it.id }
                        if (distinct.isEmpty()) UiState.Empty else UiState.Success(distinct)
                    }
                        is Resource.Error -> UiState.Error(res.message)
                    }
                }
            }
        }
    }

    fun loadFilterableData(activeTabOnly: Boolean = false) {
        val query = _searchQuery.value.ifBlank { null }
        val range = _dateRangeSelection.value
        val fromDate = range.startDate
        val toDate = range.endDate
        val date = if (fromDate == toDate && fromDate != null) fromDate else null
        val customerId = _selectedCustomer.value?.id
        val category = _salesCategoryFilter.value

        if (!activeTabOnly) {
            when (category) {
                SalesCategoryFilter.ALL -> {
                    loadLineSalesData(date = date, fromDate = fromDate, toDate = toDate, query = query, customerId = customerId)
                    loadCounterSalesData(date = date, fromDate = fromDate, toDate = toDate, query = query, customerId = customerId)
                }
                SalesCategoryFilter.LINE -> {
                    counterSalesJob?.cancel()
                    _counterSalesState.value = UiState.Success(emptyList())
                    loadLineSalesData(date = date, fromDate = fromDate, toDate = toDate, query = query, customerId = customerId)
                }
                SalesCategoryFilter.COUNTER -> {
                    lineSalesJob?.cancel()
                    _lineSalesState.value = UiState.Success(emptyList())
                    loadCounterSalesData(date = date, fromDate = fromDate, toDate = toDate, query = query, customerId = customerId)
                }
            }
            loadPurchasesData(date, query)
            loadExpensesData(date, query)
            return
        }

        when (_selectedTab.value) {
            TransactionTab.SALES -> {
                when (category) {
                    SalesCategoryFilter.ALL -> {
                        loadLineSalesData(date = date, fromDate = fromDate, toDate = toDate, query = query, customerId = customerId)
                        loadCounterSalesData(date = date, fromDate = fromDate, toDate = toDate, query = query, customerId = customerId)
                    }
                    SalesCategoryFilter.LINE -> {
                        counterSalesJob?.cancel()
                        _counterSalesState.value = UiState.Success(emptyList())
                        loadLineSalesData(date = date, fromDate = fromDate, toDate = toDate, query = query, customerId = customerId)
                    }
                    SalesCategoryFilter.COUNTER -> {
                        lineSalesJob?.cancel()
                        _lineSalesState.value = UiState.Success(emptyList())
                        loadCounterSalesData(date = date, fromDate = fromDate, toDate = toDate, query = query, customerId = customerId)
                    }
                }
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

    fun setCustomerSearchQuery(query: String) {
        _customerSearchQuery.value = query
        loadCustomers(search = query.ifBlank { null })
    }

    fun loadCustomers(search: String? = null) {
        viewModelScope.launch {
            repository.listCustomers(search = search).collect { res ->
                _customersState.value = when (res) {
                    is Resource.Loading -> if (_customersState.value is UiState.Success && !search.isNullOrBlank()) _customersState.value else UiState.Loading
                    is Resource.Success -> {
                        val distinct = res.data.distinctBy { it.id }
                        if (distinct.isEmpty()) UiState.Empty else UiState.Success(distinct)
                    }
                    is Resource.Error -> UiState.Error(res.message, res.code)
                }
            }
        }
    }

    fun createCustomer(
        name: String,
        phone: String?,
        openingBalance: String = "0.00",
        onComplete: ((Boolean, Customer?, String?) -> Unit)? = null
    ) {
        viewModelScope.launch {
            repository.createCustomer(name = name, phone = phone, openingBalance = openingBalance).collect { res ->
                when (res) {
                    is Resource.Loading -> {}
                    is Resource.Success -> {
                        loadCustomers()
                        sendUiMessage("Customer '${res.data.customerName}' created successfully", MessageType.SUCCESS)
                        onComplete?.invoke(true, res.data, null)
                    }
                    is Resource.Error -> {
                        sendUiMessage(res.message, MessageType.ERROR)
                        onComplete?.invoke(false, null, res.message)
                    }
                }
            }
        }
    }

    fun loadSuppliers() {
        viewModelScope.launch {
            repository.listSuppliers().collect { res ->
                _suppliersState.value = when (res) {
                    is Resource.Loading -> if (_suppliersState.value is UiState.Success) _suppliersState.value else UiState.Loading
                    is Resource.Success -> UiState.Success(res.data)
                    is Resource.Error -> UiState.Error(res.message, res.code)
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

    fun validateAndBuildLineSaleRequest(
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
        discount: String? = null,
        saleDate: String? = null,
        advanceToApply: String? = null
    ): Result<CreateLineSaleRequest> {
        if (customerId.isBlank()) {
            return Result.failure(IllegalArgumentException("Customer and total amount are required"))
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
            return Result.failure(IllegalArgumentException("Customer and total amount are required"))
        }

        val calcResult = SaleCalculationUtils.calculateResult(rawSubtotal, discount)
        if (!calcResult.isValid) {
            return Result.failure(IllegalArgumentException(calcResult.errorMessage ?: "Invalid discount or subtotal"))
        }

        val subtotalVal = calcResult.subtotal.toDouble()
        if (subtotalVal <= 0.0) {
            return Result.failure(IllegalArgumentException("Total invoice amount must be greater than zero"))
        }

        val effectiveTotalAmount = calcResult.finalAmountString

        val cash = cashIn.trim().ifBlank { "0.00" }.toDoubleOrNull() ?: 0.0
        if (cash < 0.0) {
            return Result.failure(IllegalArgumentException("Payment amounts cannot be negative"))
        }

        val effectiveBankSplits: List<BankPaymentSplit>
        val effectiveBankTotal: Double
        val resolvedBankId: String?

        if (bankPayments.isNotEmpty()) {
            for (bp in bankPayments) {
                if (bp.bankId.isBlank()) {
                    return Result.failure(IllegalArgumentException("Please select a bank account for all bank payment rows"))
                }
                val amt = bp.amount.trim().toDoubleOrNull()
                if (amt == null || amt <= 0.0) {
                    return Result.failure(IllegalArgumentException("Bank payment amount must be greater than zero"))
                }
            }

            val seenBanks = mutableSetOf<String>()
            for (bp in bankPayments) {
                if (!seenBanks.add(bp.bankId.trim())) {
                    return Result.failure(IllegalArgumentException("Each bank account can only be selected once per sale"))
                }
            }

            effectiveBankTotal = bankPayments.sumOf { it.amount.trim().toDoubleOrNull() ?: 0.0 }
            effectiveBankSplits = bankPayments
            resolvedBankId = bankPayments.first().bankId.trim()
        } else {
            val bank = bankAmount.trim().ifBlank { "0.00" }.toDoubleOrNull() ?: 0.0
            if (bank < 0.0) {
                return Result.failure(IllegalArgumentException("Payment amounts cannot be negative"))
            }

            val bId = bankId?.trim()?.ifBlank { null }
            if (bank > 0.0 && bId == null) {
                return Result.failure(IllegalArgumentException("Please select a bank account for bank payment"))
            }

            effectiveBankTotal = bank
            resolvedBankId = bId
            effectiveBankSplits = if (bId != null && bank > 0.0) {
                listOf(BankPaymentSplit(bankId = bId, amount = bankAmount.trim().ifBlank { "0.00" }))
            } else {
                emptyList()
            }
        }

        val formattedBankAmount = String.format(java.util.Locale.US, "%.2f", effectiveBankTotal)

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
            items = items,
            saleDate = saleDate?.trim()?.ifBlank { null },
            advanceToApply = advanceToApply?.trim()?.ifBlank { null }
        )
        return Result.success(req)
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
        discount: String? = null,
        saleDate: String? = null,
        advanceToApply: String? = null,
        onSuccess: ((LineSale) -> Unit)? = null
    ) {
        if (_createActionState.value is UiState.Loading || _activePrintingSaleId.value != null) return

        val validationResult = validateAndBuildLineSaleRequest(
            customerId = customerId,
            route = route,
            salesman = salesman,
            totalAmount = totalAmount,
            cashIn = cashIn,
            bankAmount = bankAmount,
            bankId = bankId,
            bankPayments = bankPayments,
            note = note,
            items = items,
            subtotal = subtotal,
            discount = discount,
            saleDate = saleDate,
            advanceToApply = advanceToApply
        )

        val req = validationResult.getOrElse { error ->
            _createActionState.value = UiState.Error(error.message ?: "Validation failed")
            return
        }

        viewModelScope.launch {
            repository.createLineSale(req).collect { res ->
                _createActionState.value = when (res) {
                    is Resource.Loading -> UiState.Loading
                    is Resource.Success -> {
                        loadData()
                        loadCustomers()
                        refreshCustomerBalance(customerId, SaleType.LINE_SALE)
                        onSuccess?.invoke(res.data)
                        UiState.Success(Unit)
                    }
                    is Resource.Error -> UiState.Error(res.message, res.code)
                }
            }
        }
    }

    fun recordAndPrintLineSale(
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
        discount: String? = null,
        saleDate: String? = null,
        advanceToApply: String? = null,
        customerName: String? = null,
        customerPhone: String? = null,
        context: Context,
        onPermissionRequired: ((Array<String>) -> Unit)? = null,
        onPrintResult: ((savedSale: LineSale, isPrintSuccess: Boolean, errorMessage: String?) -> Unit)? = null
    ) {
        if (_createActionState.value is UiState.Loading || _activePrintingSaleId.value != null) return

        val validationResult = validateAndBuildLineSaleRequest(
            customerId = customerId,
            route = route,
            salesman = salesman,
            totalAmount = totalAmount,
            cashIn = cashIn,
            bankAmount = bankAmount,
            bankId = bankId,
            bankPayments = bankPayments,
            note = note,
            items = items,
            subtotal = subtotal,
            discount = discount,
            saleDate = saleDate,
            advanceToApply = advanceToApply
        )

        val req = validationResult.getOrElse { error ->
            _createActionState.value = UiState.Error(error.message ?: "Validation failed")
            return
        }

        viewModelScope.launch {
            repository.createLineSale(req).collect { res ->
                when (res) {
                    is Resource.Loading -> {
                        _createActionState.value = UiState.Loading
                    }
                    is Resource.Error -> {
                        _createActionState.value = UiState.Error(res.message, res.code)
                    }
                    is Resource.Success -> {
                        loadData()
                        loadCustomers()
                        refreshCustomerBalance(customerId, SaleType.LINE_SALE)
                        var savedSale = res.data
                        val resolvedCustomerName = if (!customerName.isNullOrBlank()) customerName else savedSale.customerName
                        val resolvedCustomerPhone = if (!customerPhone.isNullOrBlank()) customerPhone else savedSale.customerPhone
                        savedSale = savedSale.copy(
                            customerName = resolvedCustomerName,
                            customerPhone = resolvedCustomerPhone
                        )
                        printLineSaleWithResult(
                            sale = savedSale,
                            context = context,
                            onPermissionRequired = onPermissionRequired,
                            onResult = { isPrintSuccess, printErrorMsg ->
                                onPrintResult?.invoke(savedSale, isPrintSuccess, printErrorMsg)
                            }
                        )
                    }
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
        discount: String? = null,
        customerId: String? = null,
        customerName: String? = null,
        saleDate: String? = null,
        advanceToApply: String? = null,
        onSuccess: ((CounterSale) -> Unit)? = null
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

        val method = if (cashVal > 0.0 && effectiveBankTotal > 0.0) "split" else if (effectiveBankTotal > 0.0) "bank" else "cash"
        val itemPrice = price?.trim()?.ifBlank { calcResult.subtotalString } ?: calcResult.subtotalString
        val formattedBankAmount = String.format(java.util.Locale.US, "%.2f", effectiveBankTotal)

        viewModelScope.launch {
            val req = CreateCounterSaleRequest(
                customerId = customerId?.trim()?.ifBlank { null },
                customerName = customerName?.trim()?.ifBlank { null },
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
                items = items,
                saleDate = saleDate?.trim()?.ifBlank { null },
                advanceToApply = advanceToApply?.trim()?.ifBlank { null }
            )
            repository.createCounterSale(req).collect { res ->
                _createActionState.value = when (res) {
                    is Resource.Loading -> UiState.Loading
                    is Resource.Success -> {
                        loadData()
                        loadCustomers()
                        if (!customerId.isNullOrBlank()) {
                            refreshCustomerBalance(customerId, SaleType.COUNTER_SALE)
                        }
                        onSuccess?.invoke(res.data)
                        UiState.Success(Unit)
                    }
                    is Resource.Error -> UiState.Error(res.message, res.code)
                }
            }
        }
    }

    fun createPurchase(
        supplierId: String? = null,
        item: String,
        quantity: String,
        totalAmount: String,
        cashAmount: String = "0.00",
        bankPayments: List<BankPaymentSplit> = emptyList(),
        customerId: String? = null
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

        val resolvedSupplierId = supplierId?.trim()?.ifBlank { null }
        val resolvedCustomerId = customerId?.trim()?.ifBlank { null }

        viewModelScope.launch {
            val req = CreatePurchaseRequest(
                supplierId = resolvedSupplierId,
                customerId = resolvedCustomerId,
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

    suspend fun ensureLogoBitmapLoaded(logoUrl: String? = _companyProfile.value?.logoUrl) {
        if (logoUrl.isNullOrBlank()) return
        val resolved = FormatUtils.resolveMediaUrl(logoUrl) ?: return
        if (LogoMemoryCache.get(resolved) != null) return
        withContext(Dispatchers.IO) {
            try {
                val conn = java.net.URL(resolved).openConnection()
                conn.connectTimeout = 4000
                conn.readTimeout = 6000
                conn.getInputStream().use { stream ->
                    val bmp = BitmapFactory.decodeStream(stream)
                    if (bmp != null) {
                        LogoMemoryCache.put(resolved, bmp.asImageBitmap())
                    }
                }
            } catch (_: Exception) {}
        }
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

    fun printLineSale(
        sale: LineSale,
        context: Context,
        onPermissionRequired: ((Array<String>) -> Unit)? = null
    ) {
        printLineSaleWithResult(sale, context, onPermissionRequired, null)
    }

    fun printLineSaleWithResult(
        sale: LineSale,
        context: Context,
        onPermissionRequired: ((Array<String>) -> Unit)? = null,
        onResult: ((isSuccess: Boolean, message: String?) -> Unit)? = null
    ) {
        if (_activePrintingSaleId.value != null) {
            onResult?.invoke(false, "Another print job is in progress")
            return
        }
        _activePrintingSaleId.value = sale.id
        viewModelScope.launch {
            try {
                val fullSale = resolveFullLineSale(sale)
                val profile = ensureCompanyProfile() ?: _companyProfile.value
                val content = settingsPreferences?.contentSettings?.value ?: InvoiceContentSettings()
                if (content.showLogo && getCachedLogoBitmap() == null && !profile?.logoUrl.isNullOrBlank()) {
                    ensureLogoBitmapLoaded(profile?.logoUrl)
                }
                val logoBmp = getCachedLogoBitmap()
                val data = InvoiceDataMapper.fromLineSale(fullSale, profile, logoBmp)
                executeDirectPrint(data, context, onPermissionRequired, onResult)
            } catch (e: Exception) {
                onResult?.invoke(false, e.message ?: "Print failed")
            } finally {
                _activePrintingSaleId.value = null
            }
        }
    }

    fun printCounterSale(
        sale: CounterSale,
        context: Context,
        onPermissionRequired: ((Array<String>) -> Unit)? = null
    ) {
        if (_activePrintingSaleId.value != null) return
        _activePrintingSaleId.value = sale.id
        viewModelScope.launch {
            try {
                val fullSale = resolveFullCounterSale(sale)
                val profile = ensureCompanyProfile() ?: _companyProfile.value
                val content = settingsPreferences?.contentSettings?.value ?: InvoiceContentSettings()
                if (content.showLogo && getCachedLogoBitmap() == null && !profile?.logoUrl.isNullOrBlank()) {
                    ensureLogoBitmapLoaded(profile?.logoUrl)
                }
                val logoBmp = getCachedLogoBitmap()
                val data = InvoiceDataMapper.fromCounterSale(fullSale, profile, logoBmp)
                executeDirectPrint(data, context, onPermissionRequired)
            } finally {
                _activePrintingSaleId.value = null
            }
        }
    }

    private suspend fun executeDirectPrint(
        printData: InvoicePrintData,
        context: Context,
        onPermissionRequired: ((Array<String>) -> Unit)? = null,
        onResult: ((isSuccess: Boolean, message: String?) -> Unit)? = null
    ) {
        val manager = printerManager
        val prefs = printerPreferences

        // 1. Resolve configured or saved printer
        val savedAddr = prefs?.selectedPrinterAddress?.value
        val savedName = prefs?.selectedPrinterName?.value ?: "Saved Thermal Printer"
        val savedConnType = prefs?.selectedConnectionType?.value ?: PrinterConnectionType.BLUETOOTH

        var targetPrinter: PrinterDevice? = _selectedPrinter.value
        if (targetPrinter == null && !savedAddr.isNullOrBlank()) {
            targetPrinter = PrinterDevice(name = savedName, address = savedAddr, connectionType = savedConnType)
            _selectedPrinter.value = targetPrinter
        }
        if (targetPrinter == null && manager != null) {
            val paired = manager.getPairedPrinters()
            targetPrinter = paired.firstOrNull { it.address == savedAddr } ?: paired.firstOrNull()
            if (targetPrinter != null) {
                _selectedPrinter.value = targetPrinter
            }
        }

        // 2. If no hardware printer is available or configured, fallback gracefully to Android System Print
        if (targetPrinter == null || manager == null) {
            InvoicePrintAdapter.print(context, printData)
            val msg = "Sent to system print service"
            _printJobState.value = PrintJobState(status = PrintJobStatus.SUCCESS, message = msg)
            sendUiMessage(msg, MessageType.SUCCESS)
            onResult?.invoke(true, msg)
            return
        }

        // 3. Hardware permission & connection checks for Bluetooth
        if (targetPrinter.connectionType == PrinterConnectionType.BLUETOOTH) {
            if (!manager.isBluetoothSupported()) {
                val msg = "Bluetooth is not supported on this device."
                _printJobState.value = PrintJobState(status = PrintJobStatus.ERROR, message = msg)
                sendUiMessage(msg, MessageType.ERROR)
                onResult?.invoke(false, msg)
                return
            }
            if (!manager.isBluetoothEnabled()) {
                val msg = "Bluetooth is turned off. Please enable Bluetooth."
                _printJobState.value = PrintJobState(status = PrintJobStatus.ERROR, message = msg)
                sendUiMessage(msg, MessageType.ERROR)
                onResult?.invoke(false, msg)
                return
            }
            if (!manager.hasBluetoothPermission()) {
                val msg = "Bluetooth permission is required."
                _printJobState.value = PrintJobState(status = PrintJobStatus.ERROR, message = msg)
                sendUiMessage(msg, MessageType.ERROR)
                onPermissionRequired?.invoke(com.vyavsa.app.core.printer.BluetoothPermissionHelper.getRequiredPermissions())
                onResult?.invoke(false, msg)
                return
            }
        }

        // 4. Format ESC/POS using saved Settings preferences
        val paperWidth = prefs?.paperWidth?.value ?: PaperWidth.WIDTH_58MM
        val theme = settingsPreferences?.invoiceTheme?.value ?: InvoiceTheme.CLASSIC
        val content = settingsPreferences?.contentSettings?.value ?: InvoiceContentSettings()
        val typo = settingsPreferences?.typography?.value ?: PrintTypography.STANDARD

        _printJobState.value = PrintJobState(status = PrintJobStatus.CONNECTING, message = "Connecting to ${targetPrinter.name}...")

        try {
            val rawBytes = ThermalInvoiceFormatter.formatEscPosReceipt(
                data = printData,
                paperWidth = paperWidth,
                theme = theme,
                contentSettings = content,
                typography = typo
            )

            _printJobState.value = PrintJobState(status = PrintJobStatus.PRINTING, message = "Printing receipt...")
            val res = manager.printReceipt(targetPrinter, rawBytes)
            if (res.isSuccess) {
                val msg = "Printed successfully on ${targetPrinter.name}"
                _printJobState.value = PrintJobState(status = PrintJobStatus.SUCCESS, message = msg)
                sendUiMessage(msg, MessageType.SUCCESS)
                onResult?.invoke(true, msg)
            } else {
                val msg = res.exceptionOrNull()?.message ?: "Print failed. Check printer connection or paper roll."
                _printJobState.value = PrintJobState(
                    status = PrintJobStatus.ERROR,
                    message = msg
                )
                sendUiMessage(msg, MessageType.ERROR)
                onResult?.invoke(false, msg)
            }
        } catch (e: Exception) {
            val msg = e.message ?: "Printer error"
            _printJobState.value = PrintJobState(
                status = PrintJobStatus.ERROR,
                message = msg
            )
            sendUiMessage(msg, MessageType.ERROR)
            onResult?.invoke(false, msg)
        }
    }

    fun openPrintDialogForLineSale(sale: LineSale) {
        if (_activePrintingSaleId.value != null) return
        _activePrintingSaleId.value = sale.id
        viewModelScope.launch {
            try {
                val fullSale = resolveFullLineSale(sale)
                val profile = ensureCompanyProfile() ?: _companyProfile.value
                val content = settingsPreferences?.contentSettings?.value ?: InvoiceContentSettings()
                if (content.showLogo && getCachedLogoBitmap() == null && !profile?.logoUrl.isNullOrBlank()) {
                    ensureLogoBitmapLoaded(profile?.logoUrl)
                }
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
                val content = settingsPreferences?.contentSettings?.value ?: InvoiceContentSettings()
                if (content.showLogo && getCachedLogoBitmap() == null && !profile?.logoUrl.isNullOrBlank()) {
                    ensureLogoBitmapLoaded(profile?.logoUrl)
                }
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
        resetPrintJobState()
    }

    fun printCurrentDialogReceipt(context: Context) {
        val printData = _activePrintDialogData.value ?: return
        val printer = _selectedPrinter.value
        val width = _paperWidth.value

        if (printer == null) {
            InvoicePrintAdapter.print(context, printData)
            val msg = "Sent to system print service"
            _printJobState.value = PrintJobState(status = PrintJobStatus.SUCCESS, message = msg)
            sendUiMessage(msg, MessageType.SUCCESS)
            dismissPrintDialog()
            return
        }

        val saleId = printData.invoiceNumber
        _activePrintingSaleId.value = saleId
        _printJobState.value = PrintJobState(status = PrintJobStatus.CONNECTING, message = "Connecting to ${printer.name}...")

        val theme = settingsPreferences?.invoiceTheme?.value ?: InvoiceTheme.CLASSIC
        val content = settingsPreferences?.contentSettings?.value ?: InvoiceContentSettings()
        val typo = settingsPreferences?.typography?.value ?: PrintTypography.STANDARD

        viewModelScope.launch {
            try {
                val rawBytes = ThermalInvoiceFormatter.formatEscPosReceipt(
                    data = printData,
                    paperWidth = width,
                    theme = theme,
                    contentSettings = content,
                    typography = typo
                )
                _printJobState.value = PrintJobState(status = PrintJobStatus.PRINTING, message = "Printing receipt...")
                val res = printerManager?.printReceipt(printer, rawBytes)
                if (res != null && res.isSuccess) {
                    val msg = "Printed successfully"
                    _printJobState.value = PrintJobState(status = PrintJobStatus.SUCCESS, message = msg)
                    sendUiMessage(msg, MessageType.SUCCESS)
                    delay(800)
                    dismissPrintDialog()
                } else {
                    val msg = res?.exceptionOrNull()?.message ?: "Thermal print failed. Check printer connection or paper."
                    _printJobState.value = PrintJobState(
                        status = PrintJobStatus.ERROR,
                        message = msg
                    )
                    sendUiMessage(msg, MessageType.ERROR)
                }
            } catch (e: Exception) {
                val msg = e.message ?: "Printer error"
                _printJobState.value = PrintJobState(
                    status = PrintJobStatus.ERROR,
                    message = msg
                )
                sendUiMessage(msg, MessageType.ERROR)
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
                val res = InvoiceShareManager.shareSalePdf(
                    context = context,
                    invoiceNumber = printData.invoiceNumber,
                    fallbackPrintData = printData,
                    pdfDownloader = { repository.downloadLineSalePdf(sale.id) }
                )
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
                val res = InvoiceShareManager.shareSalePdf(
                    context = context,
                    invoiceNumber = printData.invoiceNumber,
                    fallbackPrintData = printData,
                    pdfDownloader = { repository.downloadCounterSalePdf(sale.id) }
                )
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
