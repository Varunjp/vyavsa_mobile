package com.vyavsa.app.feature.staff

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vyavsa.app.core.common.FormatUtils
import com.vyavsa.app.core.common.Resource
import com.vyavsa.app.core.common.UiState
import com.vyavsa.app.core.model.AttendanceRecord
import com.vyavsa.app.core.model.Employee
import com.vyavsa.app.core.model.EmployeeSalary
import com.vyavsa.app.core.security.TokenManager
import com.vyavsa.app.core.sync.DataMutation
import com.vyavsa.app.core.sync.DataRefreshCoordinator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private val refreshCoordinator: DataRefreshCoordinator,
    val tokenManager: TokenManager
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(StaffTab.ATTENDANCE)
    val selectedTab: StateFlow<StaffTab> = _selectedTab.asStateFlow()

    private val _employeesState = MutableStateFlow<UiState<List<Employee>>>(UiState.Loading)
    val employeesState: StateFlow<UiState<List<Employee>>> = _employeesState.asStateFlow()

    private val _attendanceState = MutableStateFlow<UiState<List<AttendanceRecord>>>(UiState.Loading)
    val attendanceState: StateFlow<UiState<List<AttendanceRecord>>> = _attendanceState.asStateFlow()

    private val _salariesState = MutableStateFlow<UiState<List<EmployeeSalary>>>(UiState.Loading)
    val salariesState: StateFlow<UiState<List<EmployeeSalary>>> = _salariesState.asStateFlow()

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
        if (tokenManager.isTenantAdmin()) {
            loadSalaries()
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

    fun markAttendance(employeeId: String, status: String, otAmount: String, advance: String) {
        viewModelScope.launch {
            repository.recordAttendance(
                employeeId = employeeId,
                date = FormatUtils.todayDateString(),
                status = status,
                otAmount = otAmount,
                advance = advance
            ).collect { res ->
                _actionState.value = when (res) {
                    is Resource.Loading -> UiState.Loading
                    is Resource.Success -> {
                        refreshCoordinator.notifyMutation(DataMutation.STAFF)
                        loadAttendance()
                        UiState.Success(Unit)
                    }
                    is Resource.Error -> UiState.Error(res.message)
                }
            }
        }
    }

    fun createEmployee(name: String, phone: String?, salary: String, otRate: String) {
        if (name.isBlank() || salary.isBlank()) {
            _actionState.value = UiState.Error("Employee name and base salary are required")
            return
        }

        viewModelScope.launch {
            repository.createEmployee(name, phone, salary, otRate).collect { res ->
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

    fun paySalary(employeeId: String, amount: String, paymentMethod: String, note: String?) {
        if (amount.isBlank() || amount.toDoubleOrNull() ?: 0.0 <= 0.0) {
            _actionState.value = UiState.Error("Please enter a valid disbursement amount")
            return
        }

        viewModelScope.launch {
            repository.paySalary(
                employeeId = employeeId,
                amount = amount,
                paymentMethod = paymentMethod,
                date = FormatUtils.todayDateString(),
                note = note
            ).collect { res ->
                _actionState.value = when (res) {
                    is Resource.Loading -> UiState.Loading
                    is Resource.Success -> {
                        refreshCoordinator.notifyMutation(DataMutation.STAFF)
                        if (paymentMethod == "bank") {
                            refreshCoordinator.notifyMutation(DataMutation.BANK)
                        } else {
                            refreshCoordinator.notifyMutation(DataMutation.CASH)
                        }
                        loadSalaries()
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
