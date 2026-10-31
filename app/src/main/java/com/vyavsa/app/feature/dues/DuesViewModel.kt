package com.vyavsa.app.feature.dues

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vyavsa.app.core.common.FormatUtils
import com.vyavsa.app.core.common.MessageType
import com.vyavsa.app.core.common.OneTimeEventEmitter
import com.vyavsa.app.core.common.Resource
import com.vyavsa.app.core.common.UiMessage
import com.vyavsa.app.core.common.UiState
import com.vyavsa.app.core.model.CreateBankRequest
import com.vyavsa.app.core.model.Customer
import com.vyavsa.app.core.model.CustomerAdvanceResponse
import com.vyavsa.app.core.model.CustomerBalanceAdjustment
import com.vyavsa.app.core.model.CustomerPaymentResponse
import com.vyavsa.app.core.model.CustomerStatement
import com.vyavsa.app.core.model.LineSale
import com.vyavsa.app.core.model.Supplier
import com.vyavsa.app.core.model.TenantBank
import com.vyavsa.app.core.model.UpdateBankRequest
import com.vyavsa.app.core.security.AppPermission
import com.vyavsa.app.core.security.TokenManager
import com.vyavsa.app.core.sync.DataMutation
import com.vyavsa.app.core.sync.DataRefreshCoordinator
import com.vyavsa.app.feature.bank.BankRepository
import com.vyavsa.app.feature.supplier.SupplierRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class DuesTab {
    CUSTOMERS,
    SUPPLIERS
}

@HiltViewModel
class DuesViewModel @Inject constructor(
    private val repository: DuesRepository,
    private val supplierRepository: SupplierRepository,
    private val bankRepository: BankRepository,
    private val refreshCoordinator: DataRefreshCoordinator,
    val tokenManager: TokenManager? = null
) : ViewModel() {

    // Overload for testing or fallback
    constructor(
        repository: DuesRepository,
        refreshCoordinator: DataRefreshCoordinator,
        tokenManager: TokenManager? = null
    ) : this(
        repository = repository,
        supplierRepository = object : SupplierRepository {
            override fun listSuppliers(search: String?, status: String?): Flow<Resource<List<Supplier>>> = flowOf(Resource.Success(emptyList()))
            override fun createSupplier(name: String, phone: String?, status: String): Flow<Resource<Supplier>> = flowOf(Resource.Error("Not implemented"))
            override fun getSupplierById(id: String): Flow<Resource<Supplier>> = flowOf(Resource.Error("Not implemented"))
            override fun updateSupplier(id: String, name: String?, phone: String?, status: String?): Flow<Resource<Supplier>> = flowOf(Resource.Error("Not implemented"))
            override fun deleteSupplier(id: String): Flow<Resource<Unit>> = flowOf(Resource.Error("Not implemented"))
            override fun getSupplierBalance(id: String) = flowOf(Resource.Error("Not implemented"))
            override fun recordSupplierPayment(
                supplierId: String,
                amount: String,
                paymentMethod: String,
                bankId: String?,
                paymentDate: String?,
                referenceId: String?,
                note: String?,
                purchaseId: String?
            ) = flowOf(Resource.Error("Not implemented"))
        },
        bankRepository = object : BankRepository {
            override fun listBanks(search: String?, status: String?): Flow<Resource<List<TenantBank>>> = flowOf(Resource.Success(emptyList()))
            override fun createBank(request: CreateBankRequest): Flow<Resource<TenantBank>> = flowOf(Resource.Error("Not implemented"))
            override fun updateBank(id: String, request: UpdateBankRequest): Flow<Resource<TenantBank>> = flowOf(Resource.Error("Not implemented"))
            override fun deleteBank(id: String): Flow<Resource<Unit>> = flowOf(Resource.Error("Not implemented"))
        },
        refreshCoordinator = refreshCoordinator,
        tokenManager = tokenManager
    )

    private val _selectedTab = MutableStateFlow(DuesTab.CUSTOMERS)
    val selectedTab: StateFlow<DuesTab> = _selectedTab.asStateFlow()

    private val _customersState = MutableStateFlow<UiState<List<Customer>>>(UiState.Loading)
    val customersState: StateFlow<UiState<List<Customer>>> = _customersState.asStateFlow()

    private val _suppliersState = MutableStateFlow<UiState<List<Supplier>>>(UiState.Loading)
    val suppliersState: StateFlow<UiState<List<Supplier>>> = _suppliersState.asStateFlow()

    private val _banksState = MutableStateFlow<UiState<List<TenantBank>>>(UiState.Idle)
    val banksState: StateFlow<UiState<List<TenantBank>>> = _banksState.asStateFlow()

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

    private val _customerAdvancesState = MutableStateFlow<UiState<List<CustomerAdvanceResponse>>>(UiState.Idle)
    val customerAdvancesState: StateFlow<UiState<List<CustomerAdvanceResponse>>> = _customerAdvancesState.asStateFlow()

    private val _lastAdvanceResponse = MutableStateFlow<CustomerAdvanceResponse?>(null)
    val lastAdvanceResponse: StateFlow<CustomerAdvanceResponse?> = _lastAdvanceResponse.asStateFlow()

    private val _lastPaymentResponse = MutableStateFlow<CustomerPaymentResponse?>(null)
    val lastPaymentResponse: StateFlow<CustomerPaymentResponse?> = _lastPaymentResponse.asStateFlow()

    fun clearLastAdvanceResponse() {
        _lastAdvanceResponse.value = null
    }

    fun clearLastPaymentResponse() {
        _lastPaymentResponse.value = null
    }

    private val _actionState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val actionState: StateFlow<UiState<Unit>> = _actionState.asStateFlow()

    private val eventEmitter = OneTimeEventEmitter()
    val uiEvents: Flow<UiMessage> = eventEmitter.events

    fun sendUiMessage(text: String, type: MessageType = MessageType.INFO) {
        eventEmitter.emit(text, type)
    }

    private var searchJob: Job? = null

    var adminOverrideForTesting: Boolean? = null

    val isAdmin: Boolean
        get() = adminOverrideForTesting ?: (tokenManager?.isTenantAdmin() ?: false)

    val canViewCustomers: Boolean
        get() = tokenManager?.hasPermission(AppPermission.VIEW_CUSTOMERS) ?: true

    val canCreateCustomer: Boolean
        get() = tokenManager?.hasPermission(AppPermission.CREATE_CUSTOMERS) ?: isAdmin

    val canUpdateCustomer: Boolean
        get() = tokenManager?.hasPermission(AppPermission.UPDATE_CUSTOMERS) ?: isAdmin

    val canDeleteCustomer: Boolean
        get() = tokenManager?.hasPermission(AppPermission.DELETE_CUSTOMERS) ?: isAdmin

    val canAdjustBalance: Boolean
        get() = tokenManager?.hasPermission(AppPermission.ADJUST_CUSTOMER_BALANCE) ?: isAdmin

    val canCollectPayment: Boolean
        get() = tokenManager?.hasPermission(AppPermission.COLLECT_CUSTOMER_PAYMENT) ?: true

    val canCollectAdvance: Boolean
        get() = tokenManager?.hasPermission(AppPermission.COLLECT_CUSTOMER_ADVANCE) ?: true

    val canViewFinancials: Boolean
        get() = tokenManager?.hasPermission(AppPermission.VIEW_CUSTOMER_FINANCIALS) ?: true

    init {
        loadCustomers()
        loadSuppliers()
        loadBanks()
        observeRefreshEvents()
    }

    fun selectTab(tab: DuesTab) {
        _selectedTab.value = tab
        _searchQuery.value = ""
        if (tab == DuesTab.CUSTOMERS) {
            loadCustomers()
        } else {
            loadSuppliers()
        }
    }

    private fun observeRefreshEvents() {
        viewModelScope.launch {
            refreshCoordinator.refreshEvents.collect { mutation ->
                when (mutation) {
                    DataMutation.DUES,
                    DataMutation.SALES,
                    DataMutation.ALL -> {
                        loadCustomers()
                        loadSuppliers()
                    }
                    DataMutation.PURCHASES -> {
                        loadSuppliers()
                    }
                    DataMutation.BANK -> {
                        loadBanks()
                    }
                    else -> Unit
                }
            }
        }
    }

    fun loadBanks() {
        viewModelScope.launch {
            bankRepository.listBanks(status = "active").collect { res ->
                _banksState.value = when (res) {
                    is Resource.Loading -> if (_banksState.value is UiState.Success) _banksState.value else UiState.Loading
                    is Resource.Success -> UiState.Success(res.data)
                    is Resource.Error -> UiState.Error(res.message)
                }
            }
        }
    }

    fun loadSuppliers(search: String? = null) {
        viewModelScope.launch {
            supplierRepository.listSuppliers(search = search).collect { res ->
                _suppliersState.value = when (res) {
                    is Resource.Loading -> if (_suppliersState.value is UiState.Success) _suppliersState.value else UiState.Loading
                    is Resource.Success -> UiState.Success(res.data)
                    is Resource.Error -> UiState.Error(res.message)
                }
            }
        }
    }

    fun createSupplier(name: String, phone: String? = null, onSuccess: (() -> Unit)? = null) {
        if (_actionState.value is UiState.Loading) return
        val trimmed = name.trim()
        if (trimmed.isBlank()) {
            val err = "Supplier name is required"
            _actionState.value = UiState.Error(err)
            sendUiMessage(err, MessageType.ERROR)
            return
        }

        viewModelScope.launch {
            supplierRepository.createSupplier(name = trimmed, phone = phone?.trim()?.ifBlank { null }).collect { res ->
                _actionState.value = when (res) {
                    is Resource.Loading -> UiState.Loading
                    is Resource.Success -> {
                        loadSuppliers()
                        sendUiMessage("Supplier created successfully", MessageType.SUCCESS)
                        onSuccess?.invoke()
                        UiState.Success(Unit)
                    }
                    is Resource.Error -> {
                        sendUiMessage(res.message, MessageType.ERROR)
                        UiState.Error(res.message)
                    }
                }
            }
        }
    }

    fun recordSupplierPayment(
        supplierId: String,
        amount: String,
        paymentMethod: String = "cash",
        bankId: String? = null,
        referenceId: String? = null,
        note: String? = null,
        onSuccess: (() -> Unit)? = null
    ) {
        if (_actionState.value is UiState.Loading) return

        val parsedAmount = amount.trim().toDoubleOrNull() ?: 0.0
        if (amount.isBlank() || parsedAmount <= 0.0) {
            val err = "Please enter a valid payment amount"
            _actionState.value = UiState.Error(err)
            sendUiMessage(err, MessageType.ERROR)
            return
        }

        val normMethod = paymentMethod.lowercase().trim()
        if (normMethod != "cash" && normMethod != "bank") {
            val err = "Payment method must be Cash or Bank"
            _actionState.value = UiState.Error(err)
            sendUiMessage(err, MessageType.ERROR)
            return
        }

        if (normMethod == "bank" && bankId.isNullOrBlank()) {
            val err = "Please select a bank account for bank payout"
            _actionState.value = UiState.Error(err)
            sendUiMessage(err, MessageType.ERROR)
            return
        }

        viewModelScope.launch {
            supplierRepository.recordSupplierPayment(
                supplierId = supplierId,
                amount = amount.trim(),
                paymentMethod = normMethod,
                bankId = if (normMethod == "bank") bankId else null,
                paymentDate = FormatUtils.todayDateString(),
                referenceId = referenceId?.trim()?.ifBlank { null },
                note = note?.trim()?.ifBlank { null }
            ).collect { res ->
                _actionState.value = when (res) {
                    is Resource.Loading -> UiState.Loading
                    is Resource.Success -> {
                        loadSuppliers()
                        sendUiMessage("Supplier payment recorded successfully via ${normMethod.uppercase()}", MessageType.SUCCESS)
                        onSuccess?.invoke()
                        UiState.Success(Unit)
                    }
                    is Resource.Error -> {
                        sendUiMessage(res.message, MessageType.ERROR)
                        UiState.Error(res.message)
                    }
                }
            }
        }
    }

    fun collectCustomerPayment(
        customerId: String,
        amount: String,
        paymentMethod: String = "cash",
        bankId: String? = null,
        bankName: String? = null,
        paymentDate: String? = null,
        referenceId: String? = null,
        note: String? = null,
        maxBalance: String? = null,
        onSuccess: (() -> Unit)? = null
    ) {
        if (_actionState.value is UiState.Loading) return

        val parsedAmount = amount.trim().toDoubleOrNull() ?: 0.0
        if (amount.isBlank() || parsedAmount <= 0.0) {
            val err = "Please enter a valid collection amount"
            _actionState.value = UiState.Error(err)
            sendUiMessage(err, MessageType.ERROR)
            return
        }

        val max = maxBalance?.trim()?.toDoubleOrNull()
            ?: (_selectedCustomer.value as? UiState.Success)?.data?.takeIf { it.id == customerId }?.currentBalance?.toDoubleOrNull()
            ?: (_customersState.value as? UiState.Success)?.data?.find { it.id == customerId }?.currentBalance?.toDoubleOrNull()

        if (max != null && max > 0.0 && parsedAmount > max) {
            val err = "Collection amount cannot exceed customer's outstanding balance"
            _actionState.value = UiState.Error(err)
            sendUiMessage(err, MessageType.ERROR)
            return
        }

        val normMethod = paymentMethod.lowercase().trim()
        if (normMethod != "cash" && normMethod != "bank") {
            val err = "Payment method must be Cash or Bank"
            _actionState.value = UiState.Error(err)
            sendUiMessage(err, MessageType.ERROR)
            return
        }

        if (normMethod == "bank" && bankId.isNullOrBlank()) {
            val err = "Please select a bank account for bank collection"
            _actionState.value = UiState.Error(err)
            sendUiMessage(err, MessageType.ERROR)
            return
        }

        viewModelScope.launch {
            repository.collectCustomerPayment(
                id = customerId,
                amount = amount.trim(),
                paymentMethod = normMethod,
                bankId = if (normMethod == "bank") bankId else null,
                bankName = if (normMethod == "bank") bankName else null,
                date = paymentDate ?: FormatUtils.todayDateString(),
                referenceId = referenceId?.trim()?.ifBlank { null },
                note = note?.trim()?.ifBlank { null }
            ).collect { res ->
                _actionState.value = when (res) {
                    is Resource.Loading -> UiState.Loading
                    is Resource.Success -> {
                        _lastPaymentResponse.value = res.data
                        loadCustomers()
                        loadCustomerDetails(customerId)
                        loadStatement(customerId)
                        sendUiMessage("Payment collected successfully via ${normMethod.uppercase()}", MessageType.SUCCESS)
                        onSuccess?.invoke()
                        UiState.Success(Unit)
                    }
                    is Resource.Error -> {
                        sendUiMessage(res.message, MessageType.ERROR)
                        UiState.Error(res.message)
                    }
                }
            }
        }
    }

    // Overload for backward compatibility
    fun recordPayment(customerId: String, amount: String, paymentMethod: String, note: String?) {
        collectCustomerPayment(
            customerId = customerId,
            amount = amount,
            paymentMethod = paymentMethod,
            note = note
        )
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300)
            if (_selectedTab.value == DuesTab.CUSTOMERS) {
                loadCustomers(query.ifBlank { null })
            } else {
                loadSuppliers(query.ifBlank { null })
            }
        }
    }

    fun onSearchChange(query: String) = onSearchQueryChanged(query)

    fun loadCustomers(search: String? = null) {
        viewModelScope.launch {
            repository.listCustomers(search).collect { res ->
                _customersState.value = when (res) {
                    is Resource.Loading -> if (_customersState.value is UiState.Success) _customersState.value else UiState.Loading
                    is Resource.Success -> UiState.Success(res.data)
                    is Resource.Error -> UiState.Error(res.message)
                }
            }
        }
    }

    fun loadCustomerDetails(id: String) {
        viewModelScope.launch {
            repository.getCustomerById(id).collect { res ->
                _selectedCustomer.value = when (res) {
                    is Resource.Loading -> UiState.Loading
                    is Resource.Success -> {
                        loadCustomerSales(id)
                        loadStatement(id)
                        loadCustomerAdvances(id)
                        loadAdjustments(id)
                        UiState.Success(res.data)
                    }
                    is Resource.Error -> UiState.Error(res.message)
                }
            }
        }
    }

    fun collectCustomerAdvance(
        customerId: String,
        amount: String,
        paymentMethod: String,
        bankId: String? = null,
        bankName: String? = null,
        paymentDate: String? = null,
        referenceId: String? = null,
        note: String? = null,
        onSuccess: ((CustomerAdvanceResponse) -> Unit)? = null
    ) {
        if (_actionState.value is UiState.Loading) return

        val amt = amount.trim().toDoubleOrNull()
        if (amt == null || amt <= 0.0) {
            val err = "Advance amount must be strictly greater than zero"
            _actionState.value = UiState.Error(err)
            sendUiMessage(err, MessageType.ERROR)
            return
        }

        val normMethod = paymentMethod.lowercase().trim()
        if (normMethod != "cash" && normMethod != "bank") {
            val err = "Please select a valid payment method (Cash or Bank)"
            _actionState.value = UiState.Error(err)
            sendUiMessage(err, MessageType.ERROR)
            return
        }

        if (normMethod == "bank" && bankId.isNullOrBlank()) {
            val err = "Please select a bank account for bank advance collection"
            _actionState.value = UiState.Error(err)
            sendUiMessage(err, MessageType.ERROR)
            return
        }

        viewModelScope.launch {
            _actionState.value = UiState.Loading
            repository.collectCustomerAdvance(
                id = customerId,
                amount = amount.trim(),
                paymentMethod = normMethod,
                bankId = if (normMethod == "bank") bankId else null,
                bankName = if (normMethod == "bank") bankName else null,
                date = paymentDate ?: FormatUtils.todayDateString(),
                referenceId = referenceId?.trim()?.ifBlank { null },
                note = note?.trim()?.ifBlank { null }
            ).collect { res ->
                _actionState.value = when (res) {
                    is Resource.Loading -> UiState.Loading
                    is Resource.Success -> {
                        _lastAdvanceResponse.value = res.data
                        loadCustomers()
                        loadCustomerDetails(customerId)
                        loadStatement(customerId)
                        loadCustomerAdvances(customerId)
                        val settled = res.data.settledDues.toDoubleOrNull() ?: 0.0
                        val avail = res.data.availableAmount.toDoubleOrNull() ?: 0.0
                        val msg = if (settled > 0.0 && avail > 0.0) {
                            "Advance recorded: ${FormatUtils.formatCurrency(settled)} settled towards dues, ${FormatUtils.formatCurrency(avail)} added to advance balance"
                        } else if (settled > 0.0) {
                            "Advance recorded: ${FormatUtils.formatCurrency(settled)} settled existing customer dues in full"
                        } else {
                            "Advance of ${FormatUtils.formatCurrency(avail)} credited to customer balance"
                        }
                        sendUiMessage(msg, MessageType.SUCCESS)
                        onSuccess?.invoke(res.data)
                        UiState.Success(Unit)
                    }
                    is Resource.Error -> {
                        sendUiMessage(res.message, MessageType.ERROR)
                        UiState.Error(res.message)
                    }
                }
            }
        }
    }

    fun applyCustomerAdvance(
        customerId: String,
        amount: String,
        advanceId: String? = null,
        note: String? = null,
        onSuccess: (() -> Unit)? = null
    ) {
        if (_actionState.value is UiState.Loading) return

        val parsedAmount = amount.trim().toDoubleOrNull() ?: 0.0
        if (amount.isBlank() || parsedAmount <= 0.0) {
            val err = "Please enter a valid amount to apply"
            _actionState.value = UiState.Error(err)
            sendUiMessage(err, MessageType.ERROR)
            return
        }

        viewModelScope.launch {
            repository.applyCustomerAdvance(
                customerId = customerId,
                amount = amount.trim(),
                advanceId = advanceId,
                note = note?.trim()?.ifBlank { null }
            ).collect { res ->
                _actionState.value = when (res) {
                    is Resource.Loading -> UiState.Loading
                    is Resource.Success -> {
                        loadCustomers()
                        loadCustomerDetails(customerId)
                        loadStatement(customerId)
                        loadCustomerAdvances(customerId)
                        sendUiMessage("Applied ${FormatUtils.formatCurrency(res.data.amountApplied)} advance towards customer dues", MessageType.SUCCESS)
                        onSuccess?.invoke()
                        UiState.Success(Unit)
                    }
                    is Resource.Error -> {
                        sendUiMessage(res.message, MessageType.ERROR)
                        UiState.Error(res.message)
                    }
                }
            }
        }
    }

    fun loadCustomerAdvances(customerId: String) {
        viewModelScope.launch {
            repository.listCustomerAdvances(customerId).collect { res ->
                _customerAdvancesState.value = when (res) {
                    is Resource.Loading -> UiState.Loading
                    is Resource.Success -> UiState.Success(res.data)
                    is Resource.Error -> UiState.Error(res.message)
                }
            }
        }
    }

    fun loadCustomerSales(customerId: String) {
        viewModelScope.launch {
            repository.listCustomerSales(customerId).collect { res ->
                _selectedCustomerSales.value = when (res) {
                    is Resource.Loading -> UiState.Loading
                    is Resource.Success -> UiState.Success(res.data)
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
                    is Resource.Success -> UiState.Success(res.data)
                    is Resource.Error -> UiState.Error(res.message)
                }
            }
        }
    }

    fun createCustomer(name: String, phone: String?, openingBalance: String) {
        if (_actionState.value is UiState.Loading) return
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) {
            val err = "Customer name is required"
            _actionState.value = UiState.Error(err)
            sendUiMessage(err, MessageType.ERROR)
            return
        }

        val parsedBalance = openingBalance.trim().toDoubleOrNull()
        if (parsedBalance == null || parsedBalance < 0.0) {
            val err = "Opening balance must be a non-negative amount"
            _actionState.value = UiState.Error(err)
            sendUiMessage(err, MessageType.ERROR)
            return
        }

        viewModelScope.launch {
            repository.createCustomer(
                name = trimmedName,
                phone = phone?.trim()?.ifBlank { null },
                openingBalance = openingBalance.trim()
            ).collect { res ->
                _actionState.value = when (res) {
                    is Resource.Loading -> UiState.Loading
                    is Resource.Success -> {
                        refreshCoordinator.notifyMutation(DataMutation.DUES)
                        loadCustomers()
                        sendUiMessage("Customer created successfully", MessageType.SUCCESS)
                        UiState.Success(Unit)
                    }
                    is Resource.Error -> {
                        sendUiMessage(res.message, MessageType.ERROR)
                        UiState.Error(res.message)
                    }
                }
            }
        }
    }

    fun updateCustomer(id: String, name: String?, phone: String?, status: String?) {
        if (_actionState.value is UiState.Loading) return
        if (name != null && name.trim().isBlank()) {
            val err = "Customer name cannot be empty"
            _actionState.value = UiState.Error(err)
            sendUiMessage(err, MessageType.ERROR)
            return
        }

        viewModelScope.launch {
            repository.updateCustomer(
                id = id,
                name = name?.trim()?.ifBlank { null },
                phone = phone?.trim()?.ifBlank { null },
                status = status
            ).collect { res ->
                _actionState.value = when (res) {
                    is Resource.Loading -> UiState.Loading
                    is Resource.Success -> {
                        refreshCoordinator.notifyMutation(DataMutation.DUES)
                        loadCustomers()
                        loadCustomerDetails(id)
                        sendUiMessage("Customer updated successfully", MessageType.SUCCESS)
                        UiState.Success(Unit)
                    }
                    is Resource.Error -> {
                        sendUiMessage(res.message, MessageType.ERROR)
                        UiState.Error(res.message)
                    }
                }
            }
        }
    }

    fun deleteCustomer(id: String, onSuccess: () -> Unit = {}) {
        if (!isAdmin) {
            val err = "Forbidden: Administrator privileges required"
            _actionState.value = UiState.Error(err)
            sendUiMessage(err, MessageType.ERROR)
            return
        }

        viewModelScope.launch {
            repository.deleteCustomer(id).collect { res ->
                _actionState.value = when (res) {
                    is Resource.Loading -> UiState.Loading
                    is Resource.Success -> {
                        refreshCoordinator.notifyMutation(DataMutation.DUES)
                        loadCustomers()
                        sendUiMessage("Customer deleted successfully", MessageType.SUCCESS)
                        onSuccess()
                        UiState.Success(Unit)
                    }
                    is Resource.Error -> {
                        sendUiMessage(res.message, MessageType.ERROR)
                        UiState.Error(res.message)
                    }
                }
            }
        }
    }

    fun adjustBalance(id: String, newBalance: String?, adjustmentAmount: String?, reason: String) {
        if (!isAdmin) {
            val err = "Forbidden: Administrator privileges required"
            _actionState.value = UiState.Error(err)
            sendUiMessage(err, MessageType.ERROR)
            return
        }

        if (reason.isBlank()) {
            val err = "Reason is required for manual balance adjustment"
            _actionState.value = UiState.Error(err)
            sendUiMessage(err, MessageType.ERROR)
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
                        sendUiMessage("Customer balance adjusted successfully", MessageType.SUCCESS)
                        UiState.Success(Unit)
                    }
                    is Resource.Error -> {
                        sendUiMessage(res.message, MessageType.ERROR)
                        UiState.Error(res.message)
                    }
                }
            }
        }
    }

    fun resetActionState() {
        _actionState.value = UiState.Idle
    }
}
