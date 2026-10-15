package com.vyavsa.app.feature.staff

import com.vyavsa.app.core.common.Resource
import com.vyavsa.app.core.common.UiState
import com.vyavsa.app.core.model.AttendanceRecord
import com.vyavsa.app.core.model.Employee
import com.vyavsa.app.core.model.EmployeeSalary
import com.vyavsa.app.core.model.EmployeeSalaryStatement
import com.vyavsa.app.core.sync.DataRefreshCoordinator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StaffViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeStaffRepository
    private lateinit var refreshCoordinator: DataRefreshCoordinator
    private lateinit var viewModel: StaffViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeStaffRepository()
        refreshCoordinator = DataRefreshCoordinator()
        viewModel = StaffViewModel(fakeRepository, refreshCoordinator)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialLoadEmployeesAndAttendance() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        val empState = viewModel.employeesState.value
        assertTrue("Expected Success employees, got $empState", empState is UiState.Success)
        val employees = (empState as UiState.Success).data
        assertEquals(2, employees.size)
        assertEquals("Ramesh Singh", employees[0].name)

        val attState = viewModel.attendanceState.value
        assertTrue("Expected Success attendance, got $attState", attState is UiState.Success)
    }

    @Test
    fun testAdminLoadsSalariesOnDataLoad() = runTest {
        viewModel.adminOverrideForTesting = true
        viewModel.loadData()
        testDispatcher.scheduler.advanceUntilIdle()

        val salariesState = viewModel.salariesState.value
        assertTrue("Expected Success salaries for admin, got $salariesState", salariesState is UiState.Success)
        val salaries = (salariesState as UiState.Success).data
        assertEquals(1, salaries.size)
        assertEquals("12000.00", salaries[0].salaryRate)
    }

    @Test
    fun testNonAdminCannotUpdateEmployee() = runTest {
        viewModel.adminOverrideForTesting = false
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateEmployee(
            id = "emp-1",
            name = "Ramesh Kumar Singh",
            phone = null,
            salary = "15000.00",
            otRate = null,
            status = null
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val actionState = viewModel.actionState.value
        assertTrue("Expected Error for non-admin update, got $actionState", actionState is UiState.Error)
        assertEquals("Access Denied: Administrator role required", (actionState as UiState.Error).message)
    }

    @Test
    fun testAdminCanUpdateEmployee() = runTest {
        viewModel.adminOverrideForTesting = true
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateEmployee(
            id = "emp-1",
            name = "Ramesh Kumar Singh",
            phone = "9876543210",
            salary = "15000.00",
            otRate = "60.00",
            status = "active"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val actionState = viewModel.actionState.value
        assertTrue("Expected Success for admin update, got $actionState", actionState is UiState.Success)
        val updated = fakeRepository.employees.find { it.id == "emp-1" }
        assertEquals("Ramesh Kumar Singh", updated?.name)
        assertEquals("15000.00", updated?.salary)
    }

    @Test
    fun testNonAdminCannotDeleteEmployee() = runTest {
        viewModel.adminOverrideForTesting = false
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.deleteEmployee("emp-1")
        testDispatcher.scheduler.advanceUntilIdle()

        val actionState = viewModel.actionState.value
        assertTrue("Expected Error for non-admin delete, got $actionState", actionState is UiState.Error)
        assertEquals("Access Denied: Administrator role required", (actionState as UiState.Error).message)
    }

    @Test
    fun testAdminCanDeleteEmployee() = runTest {
        viewModel.adminOverrideForTesting = true
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.deleteEmployee("emp-2")
        testDispatcher.scheduler.advanceUntilIdle()

        val actionState = viewModel.actionState.value
        assertTrue("Expected Success for admin delete, got $actionState", actionState is UiState.Success)
        assertEquals(1, fakeRepository.employees.size)
    }
}

class FakeStaffRepository : StaffRepository {

    val employees = mutableListOf(
        Employee(
            id = "emp-1",
            name = "Ramesh Singh",
            phone = "9876543210",
            salary = "12000.00",
            otRate = "50.00",
            status = "active",
            createdAt = "2026-01-01T00:00:00Z"
        ),
        Employee(
            id = "emp-2",
            name = "Suresh Verma",
            phone = "9876543212",
            salary = "10000.00",
            otRate = "40.00",
            status = "active",
            createdAt = "2026-01-02T00:00:00Z"
        )
    )

    val attendances = mutableListOf(
        AttendanceRecord(
            id = "att-1",
            employeeId = "emp-1",
            employeeName = "Ramesh Singh",
            date = "2026-01-15",
            status = "present",
            otAmount = "100.00",
            advance = "0.00"
        )
    )

    val pendingSalaries = mutableListOf(
        EmployeeSalary(
            id = "sal-1",
            employeeId = "emp-1",
            employeeName = "Ramesh Singh",
            salaryRate = "12000.00",
            grossSalary = "12500.00",
            totalOvertime = "500.00",
            totalAdvances = "200.00",
            totalPaid = "0.00",
            balance = "12300.00"
        )
    )

    override fun listEmployees(): Flow<Resource<List<Employee>>> = flow {
        emit(Resource.Loading)
        emit(Resource.Success(employees))
    }

    override fun createEmployee(name: String, phone: String?, salary: String, otRate: String): Flow<Resource<Employee>> = flow {
        emit(Resource.Loading)
        val newEmp = Employee(
            id = "emp-${employees.size + 1}",
            name = name,
            phone = phone,
            salary = salary,
            otRate = otRate,
            status = "active",
            createdAt = "2026-01-03T00:00:00Z"
        )
        employees.add(newEmp)
        emit(Resource.Success(newEmp))
    }

    override fun updateEmployee(
        id: String,
        name: String?,
        phone: String?,
        salary: String?,
        otRate: String?,
        status: String?
    ): Flow<Resource<Employee>> = flow {
        emit(Resource.Loading)
        val idx = employees.indexOfFirst { it.id == id }
        if (idx >= 0) {
            val old = employees[idx]
            val updated = old.copy(
                name = name ?: old.name,
                phone = phone ?: old.phone,
                salary = salary ?: old.salary,
                otRate = otRate ?: old.otRate,
                status = status ?: old.status
            )
            employees[idx] = updated
            emit(Resource.Success(updated))
        } else {
            emit(Resource.Error("Employee not found"))
        }
    }

    override fun deleteEmployee(id: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading)
        employees.removeAll { it.id == id }
        emit(Resource.Success(Unit))
    }

    override fun getEmployeeSalaryStatement(id: String): Flow<Resource<EmployeeSalaryStatement>> = flow {
        emit(Resource.Loading)
        emit(Resource.Success(EmployeeSalaryStatement(
            employeeId = id,
            employeeName = "Ramesh Singh",
            baseSalary = "12000.00",
            totalOvertime = "500.00",
            grossSalary = "12500.00",
            totalAdvances = "0.00",
            totalPaid = "0.00",
            netPayable = "12500.00",
            overtimes = emptyList(),
            advances = emptyList(),
            payments = emptyList()
        )))
    }

    override fun listAttendance(date: String?): Flow<Resource<List<AttendanceRecord>>> = flow {
        emit(Resource.Loading)
        emit(Resource.Success(attendances))
    }

    override fun recordAttendance(
        employeeId: String,
        date: String,
        status: String,
        otAmount: String,
        advance: String
    ): Flow<Resource<AttendanceRecord>> = flow {
        emit(Resource.Loading)
        val record = AttendanceRecord(
            id = "att-${attendances.size + 1}",
            employeeId = employeeId,
            employeeName = "Ramesh Singh",
            date = date,
            status = status,
            otAmount = otAmount,
            advance = advance
        )
        attendances.add(record)
        emit(Resource.Success(record))
    }

    override fun listPendingSalaries(): Flow<Resource<List<EmployeeSalary>>> = flow {
        emit(Resource.Loading)
        emit(Resource.Success(pendingSalaries))
    }

    override fun paySalary(
        employeeId: String,
        amount: String,
        paymentMethod: String,
        date: String?,
        note: String?
    ): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading)
        emit(Resource.Success(Unit))
    }
}
