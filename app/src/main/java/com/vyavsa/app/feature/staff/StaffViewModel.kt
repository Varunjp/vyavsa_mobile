package com.vyavsa.app.feature.staff

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vyavsa.app.core.common.FormatUtils
import com.vyavsa.app.core.common.MessageType
import com.vyavsa.app.core.common.OneTimeEventEmitter
import com.vyavsa.app.core.common.Resource
import com.vyavsa.app.core.common.UiMessage
import com.vyavsa.app.core.common.UiState
import com.vyavsa.app.core.model.AttendanceRecord
import com.vyavsa.app.core.model.Employee
import com.vyavsa.app.core.model.EmployeeSalary
import com.vyavsa.app.core.model.TenantBank
import com.vyavsa.app.core.security.TokenManager
import com.vyavsa.app.core.sync.DataMutation
import com.vyavsa.app.core.sync.DataRefreshCoordinator
import com.vyavsa.app.feature.bank.BankRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class StaffTab {
    ATTENDANCE,
    SALARIES,
    ROSTER
}

@HiltViewModel
class StaffViewModel @Inject constructor(
    private val repository: StaffRepository,
    private val bankRepository: BankRepository,
    private val refreshCoordinator: DataRefreshCoordinator,
    val tokenManager: TokenManager? = null
) : ViewModel() {

    // Overload for testing or default instances
    constructor(
        repository: StaffRepository,
        refreshCoordinator: DataRefreshCoordinator,
        tokenManager: TokenManager? = null
    ) : this(
        repository = repository,
        bankRepository = object : BankRepository {
            override fun listBanks(search: String?, status: String?): Flow<Resource<List<TenantBank>>> = flowOf(Resource.Success(emptyList()))
            override fun createBank(request: com.vyavsa.app.core.model.CreateBankRequest): Flow<Resource<TenantBank>> = flowOf(Resource.Error("Not implemented"))
            override fun updateBank(id: String, request: com.vyavsa.app.core.model.UpdateBankRequest): Flow<Resource<TenantBank>> = flowOf(Resource.Error("Not implemented"))
            override fun deleteBank(id: String): Flow<Resource<Unit>> = flowOf(Resource.Error("Not implemented"))
        },
        refreshCoordinator = refreshCoordinator,
        tokenManager = tokenManager
    )

    private val eventEmitter = OneTimeEventEmitter()
    val uiEvents: Flow<UiMessage> = eventEmitter.events

    fun sendUiMessage(text: String, type: MessageType = MessageType.INFO) {
        eventEmitter.emit(text, type)
    }

    var adminOverrideForTesting: Boolean? = null

    val isAdmin: Boolean
        get() = adminOverrideForTesting ?: (tokenManager?.isTenantAdmin() ?: false)

    private val _selectedTab = MutableStateFlow(StaffTab.ATTENDANCE)
    val selectedTab: StateFlow<StaffTab> = _selectedTab.asStateFlow()

    private val _employeesState = MutableStateFlow<UiState<List<Employee>>>(UiState.Loading)
    val employeesState: StateFlow<UiState<List<Employee>>> = _employeesState.asStateFlow()

    private val _attendanceState = MutableStateFlow<UiState<List<AttendanceRecord>>>(UiState.Loading)
    val attendanceState: StateFlow<UiState<List<AttendanceRecord>>> = _attendanceState.asStateFlow()

    private val _salariesState = MutableStateFlow<UiState<List<EmployeeSalary>>>(UiState.Loading)
    val salariesState: StateFlow<UiState<List<EmployeeSalary>>> = _salariesState.asStateFlow()

    private val _banksState = MutableStateFlow<UiState<List<TenantBank>>>(UiState.Loading)
    val banksState: StateFlow<UiState<List<TenantBank>>> = _banksState.asStateFlow()

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
                    DataMutation.STAFF,
                    DataMutation.ALL -> {
                        loadData()
                    }
                    DataMutation.BANK -> {
                        loadBanks()
                    }
                    else -> Unit
                }
            }
        }
    }

    fun selectTab(tab: StaffTab) {
        _selectedTab.value = tab
    }

    fun loadData() {
        loadEmployees()
        loadAttendance()
        loadBanks()
        if (isAdmin) {
            loadSalaries()
        }
    }

    fun loadBanks() {
        viewModelScope.launch {
            bankRepository.listBanks().collect { res ->
                _banksState.value = when (res) {
                    is Resource.Loading -> UiState.Loading
                    is Resource.Success -> if (res.data.isEmpty()) UiState.Empty else UiState.Success(res.data)
                    is Resource.Error -> UiState.Error(res.message)
                }
            }
        }
    }

    fun loadEmployees() {
        viewModelScope.launch {
            repository.listEmployees().collect { res ->
                _employeesState.value = when (res) {
                    is Resource.Loading -> UiState.Loading
                    is Resource.Success -> if (res.data.isEmpty()) UiState.Empty else UiState.Success(res.data)
                    is Resource.Error -> UiState.Error(res.message)
                }
            }
        }
    }

    fun loadAttendance() {
        viewModelScope.launch {
            repository.listAttendance(FormatUtils.todayDateString()).collect { res ->
                _attendanceState.value = when (res) {
                    is Resource.Loading -> UiState.Loading
                    is Resource.Success -> if (res.data.isEmpty()) UiState.Empty else UiState.Success(res.data)
                    is Resource.Error -> UiState.Error(res.message)
                }
            }
        }
    }

    fun loadSalaries() {
        viewModelScope.launch {
            repository.listPendingSalaries().collect { res ->
                _salariesState.value = when (res) {
                    is Resource.Loading -> UiState.Loading
                    is Resource.Success -> if (res.data.isEmpty()) UiState.Empty else UiState.Success(res.data)
                    is Resource.Error -> UiState.Error(res.message)
                }
            }
        }
    }

    fun markAttendance(
        employeeId: String,
        status: String,
        dailySalary: String? = null,
        otAmount: String = "",
        advance: String = "",
        date: String = FormatUtils.todayDateString()
    ) {
        val trimmedSalary = dailySalary?.trim()
        if (!trimmedSalary.isNullOrBlank()) {
            val parsed = trimmedSalary.toDoubleOrNull()
            if (parsed == null || parsed < 0.0) {
                _actionState.value = UiState.Error("Daily salary must be a non-negative amount")
                return
            }
        }

        viewModelScope.launch {
            repository.recordAttendance(
                employeeId = employeeId,
                date = date,
                status = status,
                dailySalary = trimmedSalary?.ifBlank { null },
                otAmount = otAmount,
                advance = advance
            ).collect { res ->
                _actionState.value = when (res) {
                    is Resource.Loading -> UiState.Loading
                    is Resource.Success -> {
                        refreshCoordinator.notifyMutation(DataMutation.STAFF)
                        loadAttendance()
                        if (isAdmin) {
                            loadSalaries()
                        }
                        UiState.Success(Unit)
                    }
                    is Resource.Error -> UiState.Error(res.message)
                }
            }
        }
    }

    fun updateAttendance(
        attendanceId: String,
        status: String?,
        dailySalary: String?,
        clearDailySalary: Boolean = false,
        otAmount: String? = null,
        advance: String? = null
    ) {
        val trimmedSalary = dailySalary?.trim()
        if (!clearDailySalary && !trimmedSalary.isNullOrBlank()) {
            val parsed = trimmedSalary.toDoubleOrNull()
            if (parsed == null || parsed < 0.0) {
                _actionState.value = UiState.Error("Daily salary must be a non-negative amount")
                return
            }
        }

        viewModelScope.launch {
            repository.updateAttendance(
                id = attendanceId,
                status = status,
                dailySalary = if (clearDailySalary) null else trimmedSalary?.ifBlank { null },
                clearDailySalary = clearDailySalary,
                otAmount = otAmount,
                advance = advance
            ).collect { res ->
                _actionState.value = when (res) {
                    is Resource.Loading -> UiState.Loading
                    is Resource.Success -> {
                        refreshCoordinator.notifyMutation(DataMutation.STAFF)
                        loadAttendance()
                        if (isAdmin) {
                            loadSalaries()
                        }
                        UiState.Success(Unit)
                    }
                    is Resource.Error -> UiState.Error(res.message)
                }
            }
        }
    }

    fun createEmployee(name: String, phone: String?, salary: String?, otRate: String) {
        if (name.isBlank()) {
            _actionState.value = UiState.Error("Employee name is required")
            return
        }

        val trimmedSalary = salary?.trim()
        if (!trimmedSalary.isNullOrBlank()) {
            val parsed = trimmedSalary.toDoubleOrNull()
            if (parsed == null || parsed < 0.0) {
                _actionState.value = UiState.Error("Salary must be a non-negative amount")
                return
            }
        }

        viewModelScope.launch {
            repository.createEmployee(name, phone, trimmedSalary?.ifBlank { null }, otRate).collect { res ->
                _actionState.value = when (res) {
                    is Resource.Loading -> UiState.Loading
                    is Resource.Success -> {
                        refreshCoordinator.notifyMutation(DataMutation.STAFF)
                        loadEmployees()
                        UiState.Success(Unit)
                    }
                    is Resource.Error -> UiState.Error(res.message)
                }
            }
        }
    }

    fun paySalary(
        employeeId: String,
        amount: String,
        paymentMethod: String,
        bankId: String? = null,
        bankName: String? = null,
        referenceId: String? = null,
        note: String? = null
    ) {
        if (_actionState.value is UiState.Loading) return

        val parsedAmount = amount.toDoubleOrNull() ?: 0.0
        if (amount.isBlank() || parsedAmount <= 0.0) {
            val err = "Please enter a valid disbursement amount"
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
            val err = "Please select a bank account for bank disbursement"
            _actionState.value = UiState.Error(err)
            sendUiMessage(err, MessageType.ERROR)
            return
        }

        viewModelScope.launch {
            repository.paySalary(
                employeeId = employeeId,
                amount = amount,
                paymentMethod = normMethod,
                bankId = if (normMethod == "bank") bankId else null,
                bankName = if (normMethod == "bank") bankName else null,
                date = FormatUtils.todayDateString(),
                referenceId = referenceId,
                note = note
            ).collect { res ->
                _actionState.value = when (res) {
                    is Resource.Loading -> UiState.Loading
                    is Resource.Success -> {
                        refreshCoordinator.notifyMutation(DataMutation.STAFF)
                        if (normMethod == "bank") {
                            refreshCoordinator.notifyMutation(DataMutation.BANK)
                        } else {
                            refreshCoordinator.notifyMutation(DataMutation.CASH)
                        }
                        loadSalaries()
                        sendUiMessage("Salary disbursed successfully via ${normMethod.uppercase()}", MessageType.SUCCESS)
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
    fun paySalary(employeeId: String, amount: String, paymentMethod: String, note: String?) {
        paySalary(employeeId, amount, paymentMethod, null, null, null, note)
    }

    fun updateEmployee(
        id: String,
        name: String?,
        phone: String?,
        salary: String?,
        clearSalary: Boolean = false,
        otRate: String?,
        status: String?
    ) {
        if (!isAdmin) {
            _actionState.value = UiState.Error("Access Denied: Administrator role required")
            return
        }

        val trimmedSalary = salary?.trim()
        if (!clearSalary && !trimmedSalary.isNullOrBlank()) {
            val parsed = trimmedSalary.toDoubleOrNull()
            if (parsed == null || parsed < 0.0) {
                _actionState.value = UiState.Error("Salary must be a non-negative amount")
                return
            }
        }

        viewModelScope.launch {
            repository.updateEmployee(
                id = id,
                name = name,
                phone = phone,
                salary = if (clearSalary) null else trimmedSalary?.ifBlank { null },
                clearSalary = clearSalary,
                otRate = otRate,
                status = status
            ).collect { res ->
                _actionState.value = when (res) {
                    is Resource.Loading -> UiState.Loading
                    is Resource.Success -> {
                        refreshCoordinator.notifyMutation(DataMutation.STAFF)
                        loadEmployees()
                        UiState.Success(Unit)
                    }
                    is Resource.Error -> UiState.Error(res.message)
                }
            }
        }
    }

    fun deleteEmployee(id: String) {
        if (!isAdmin) {
            _actionState.value = UiState.Error("Access Denied: Administrator role required")
            return
        }

        viewModelScope.launch {
            repository.deleteEmployee(id).collect { res ->
                _actionState.value = when (res) {
                    is Resource.Loading -> UiState.Loading
                    is Resource.Success -> {
                        refreshCoordinator.notifyMutation(DataMutation.STAFF)
                        loadEmployees()
                        UiState.Success(Unit)
                    }
                    is Resource.Error -> UiState.Error(res.message)
                }
            }
        }
    }

    fun recordStandaloneAdvance(
        employeeId: String,
        amount: String,
        date: String = FormatUtils.todayDateString(),
        paymentMethod: String = "cash",
        note: String? = null
    ) {
        val trimmedAmount = amount.trim()
        val parsed = trimmedAmount.toDoubleOrNull()
        if (parsed == null || parsed <= 0.0) {
            _actionState.value = UiState.Error("Advance amount must be greater than zero")
            return
        }

        viewModelScope.launch {
            repository.recordAdvance(
                employeeId = employeeId,
                amount = trimmedAmount,
                date = date,
                paymentMethod = paymentMethod,
                note = note?.trim()?.ifBlank { null }
            ).collect { res ->
                _actionState.value = when (res) {
                    is Resource.Loading -> UiState.Loading
                    is Resource.Success -> {
                        refreshCoordinator.notifyMutation(DataMutation.STAFF)
                        loadAttendance()
                        if (isAdmin) {
                            loadSalaries()
                        }
                        UiState.Success(Unit)
                    }
                    is Resource.Error -> UiState.Error(res.message)
                }
            }
        }
    }

    fun recordStandaloneOvertime(
        employeeId: String,
        amount: String,
        date: String = FormatUtils.todayDateString(),
        note: String? = null
    ) {
        val trimmedAmount = amount.trim()
        val parsed = trimmedAmount.toDoubleOrNull()
        if (parsed == null || parsed <= 0.0) {
            _actionState.value = UiState.Error("Overtime amount must be greater than zero")
            return
        }

        viewModelScope.launch {
            repository.recordOvertime(
                employeeId = employeeId,
                amount = trimmedAmount,
                date = date,
                note = note?.trim()?.ifBlank { null }
            ).collect { res ->
                _actionState.value = when (res) {
                    is Resource.Loading -> UiState.Loading
                    is Resource.Success -> {
                        refreshCoordinator.notifyMutation(DataMutation.STAFF)
                        loadAttendance()
                        if (isAdmin) {
                            loadSalaries()
                        }
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
