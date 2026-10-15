package com.vyavsa.app.feature.dues

import com.vyavsa.app.core.common.Resource
import com.vyavsa.app.core.common.UiState
import com.vyavsa.app.core.model.Customer
import com.vyavsa.app.core.model.CustomerBalance
import com.vyavsa.app.core.model.CustomerBalanceAdjustment
import com.vyavsa.app.core.model.CustomerStatement
import com.vyavsa.app.core.model.LineSale
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
class DuesViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeDuesRepository
    private lateinit var refreshCoordinator: DataRefreshCoordinator
    private lateinit var viewModel: DuesViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeDuesRepository()
        refreshCoordinator = DataRefreshCoordinator()
        viewModel = DuesViewModel(fakeRepository, refreshCoordinator)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialLoadCustomersSuccess() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.customersState.value
        assertTrue("Expected Success state, got $state", state is UiState.Success)
        val customers = (state as UiState.Success).data
        assertEquals(2, customers.size)
        assertEquals("Sharma Stores", customers[0].customerName)
        assertEquals("Gupta Traders", customers[1].customerName)
    }

    @Test
    fun testCustomerSearchFiltersList() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onSearchChange("Gupta")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.customersState.value
        assertTrue("Expected Success state, got $state", state is UiState.Success)
        val customers = (state as UiState.Success).data
        assertEquals(1, customers.size)
        assertEquals("Gupta Traders", customers[0].customerName)
    }

    @Test
    fun testLoadCustomerDetailsAndSales() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.loadCustomerDetails("cust-1")
        testDispatcher.scheduler.advanceUntilIdle()

        val detailState = viewModel.selectedCustomer.value
        assertTrue("Expected Success for customer detail, got $detailState", detailState is UiState.Success)
        assertEquals("Sharma Stores", (detailState as UiState.Success).data.customerName)

        val salesState = viewModel.selectedCustomerSales.value
        assertTrue("Expected Success for sales history, got $salesState", salesState is UiState.Success)
        val sales = (salesState as UiState.Success).data
        assertEquals(1, sales.size)
        assertEquals("sale-1", sales[0].id)
    }

    @Test
    fun testAdminAdjustCustomerBalanceSuccess() = runTest {
        viewModel.adminOverrideForTesting = true
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.adjustBalance(
            id = "cust-1",
            newBalance = "100.00",
            adjustmentAmount = null,
            reason = "Audit reconciliation"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val actionState = viewModel.actionState.value
        assertTrue("Expected Success for adjustBalance, got $actionState", actionState is UiState.Success)
        assertEquals("100.00", fakeRepository.customers[0].currentBalance)
    }

    @Test
    fun testNonAdminAdjustBalanceBlockedByRbac() = runTest {
        viewModel.adminOverrideForTesting = false
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.adjustBalance(
            id = "cust-1",
            newBalance = "100.00",
            adjustmentAmount = null,
            reason = "Audit reconciliation"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val actionState = viewModel.actionState.value
        assertTrue("Expected Error for non-admin adjust balance, got $actionState", actionState is UiState.Error)
        assertEquals(
            "Forbidden: Administrator privileges required",
            (actionState as UiState.Error).message
        )
    }

    @Test
    fun testAdjustBalanceEmptyReasonFails() = runTest {
        viewModel.adminOverrideForTesting = true
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.adjustBalance(
            id = "cust-1",
            newBalance = "100.00",
            adjustmentAmount = null,
            reason = "   "
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val actionState = viewModel.actionState.value
        assertTrue("Expected Error for empty reason, got $actionState", actionState is UiState.Error)
        assertEquals(
            "Reason is required for manual balance adjustment",
            (actionState as UiState.Error).message
        )
    }

    @Test
    fun testRecordPaymentSuccess() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.recordPayment(
            customerId = "cust-1",
            amount = "150.00",
            paymentMethod = "cash",
            note = "Counter payment"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val actionState = viewModel.actionState.value
        assertTrue("Expected Success on recording payment, got $actionState", actionState is UiState.Success)
    }

    @Test
    fun testAdminDeleteCustomerSuccess() = runTest {
        viewModel.adminOverrideForTesting = true
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.deleteCustomer("cust-2")
        testDispatcher.scheduler.advanceUntilIdle()

        val actionState = viewModel.actionState.value
        assertTrue("Expected Success on deleting customer, got $actionState", actionState is UiState.Success)
        assertEquals(1, fakeRepository.customers.size)
    }

    @Test
    fun testNonAdminDeleteCustomerBlocked() = runTest {
        viewModel.adminOverrideForTesting = false
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.deleteCustomer("cust-2")
        testDispatcher.scheduler.advanceUntilIdle()

        val actionState = viewModel.actionState.value
        assertTrue("Expected Error for non-admin delete customer, got $actionState", actionState is UiState.Error)
        assertEquals(
            "Forbidden: Administrator privileges required",
            (actionState as UiState.Error).message
        )
    }
}

class FakeDuesRepository : DuesRepository {

    val customers = mutableListOf(
        Customer(
            id = "cust-1",
            customerName = "Sharma Stores",
            phone = "9876543210",
            currentBalance = "500.00",
            status = "active",
            createdAt = "2026-01-01T00:00:00Z"
        ),
        Customer(
            id = "cust-2",
            customerName = "Gupta Traders",
            phone = "9876543211",
            currentBalance = "-200.00",
            status = "active",
            createdAt = "2026-01-02T00:00:00Z"
        )
    )

    val adjustments = mutableListOf<CustomerBalanceAdjustment>()

    val sales = mutableListOf(
        LineSale(
            id = "sale-1",
            customerId = "cust-1",
            customerName = "Sharma Stores",
            totalAmount = "1500.00",
            totalCashIn = "1000.00",
            bankAmount = "0.00",
            collectedAmount = "1000.00",
            balance = "500.00",
            createdAt = "2026-01-15T10:00:00Z"
        )
    )

    override fun listCustomers(search: String?): Flow<Resource<List<Customer>>> = flow {
        emit(Resource.Loading)
        val filtered = customers.filter {
            val phone = it.phone
            search == null || it.customerName.contains(search, ignoreCase = true) ||
            (phone != null && phone.contains(search))
        }
        emit(Resource.Success(filtered))
    }

    override fun createCustomer(name: String, phone: String?, openingBalance: String): Flow<Resource<Customer>> = flow {
        emit(Resource.Loading)
        val customer = Customer(
            id = "cust-${customers.size + 1}",
            customerName = name,
            phone = phone,
            currentBalance = openingBalance,
            status = "active",
            createdAt = "2026-01-03T00:00:00Z"
        )
        customers.add(customer)
        emit(Resource.Success(customer))
    }

    override fun getCustomerById(id: String): Flow<Resource<Customer>> = flow {
        emit(Resource.Loading)
        val customer = customers.find { it.id == id }
        if (customer != null) {
            emit(Resource.Success(customer))
        } else {
            emit(Resource.Error("Customer not found"))
        }
    }

    override fun updateCustomer(id: String, name: String?, phone: String?, status: String?): Flow<Resource<Customer>> = flow {
        emit(Resource.Loading)
        val idx = customers.indexOfFirst { it.id == id }
        if (idx >= 0) {
            val old = customers[idx]
            val updated = old.copy(
                customerName = name ?: old.customerName,
                phone = phone ?: old.phone,
                status = status ?: old.status
            )
            customers[idx] = updated
            emit(Resource.Success(updated))
        } else {
            emit(Resource.Error("Customer not found"))
        }
    }

    override fun deleteCustomer(id: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading)
        customers.removeAll { it.id == id }
        emit(Resource.Success(Unit))
    }

    override fun adjustCustomerBalance(
        id: String,
        newBalance: String?,
        adjustmentAmount: String?,
        reason: String
    ): Flow<Resource<CustomerBalanceAdjustment>> = flow {
        emit(Resource.Loading)
        val idx = customers.indexOfFirst { it.id == id }
        if (idx >= 0) {
            val old = customers[idx]
            val prevBal = old.currentBalance ?: "0.00"
            val targetBal = newBalance ?: "0.00"
            customers[idx] = old.copy(currentBalance = targetBal)

            val adj = CustomerBalanceAdjustment(
                id = "adj-1",
                customerId = id,
                previousBalance = prevBal,
                newBalance = targetBal,
                adjustmentAmount = adjustmentAmount ?: "0.00",
                reason = reason,
                createdAt = "2026-01-16T12:00:00Z"
            )
            adjustments.add(adj)
            emit(Resource.Success(adj))
        } else {
            emit(Resource.Error("Customer not found"))
        }
    }

    override fun listCustomerAdjustments(id: String): Flow<Resource<List<CustomerBalanceAdjustment>>> = flow {
        emit(Resource.Loading)
        emit(Resource.Success(adjustments.filter { it.customerId == id }))
    }

    override fun getCustomerBalance(id: String): Flow<Resource<CustomerBalance>> = flow {
        emit(Resource.Loading)
        val customer = customers.find { it.id == id }
        if (customer != null) {
            emit(Resource.Success(CustomerBalance(
                customerId = id,
                customerName = customer.customerName,
                currentBalance = customer.currentBalance ?: "0.00"
            )))
        } else {
            emit(Resource.Error("Customer not found"))
        }
    }

    override fun getCustomerStatement(id: String): Flow<Resource<CustomerStatement>> = flow {
        emit(Resource.Loading)
        emit(Resource.Success(CustomerStatement(
            customerId = id,
            customerName = "Sharma Stores",
            openingBalance = "0.00",
            currentBalance = "500.00",
            totalPurchases = "1500.00",
            totalPaid = "1000.00",
            outstandingPayable = "500.00",
            entries = emptyList()
        )))
    }

    override fun listCustomerSales(customerId: String): Flow<Resource<List<LineSale>>> = flow {
        emit(Resource.Loading)
        emit(Resource.Success(sales.filter { it.customerId == customerId }))
    }

    override fun recordPayment(
        id: String,
        amount: String,
        paymentMethod: String,
        date: String?,
        note: String?
    ): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading)
        emit(Resource.Success(Unit))
    }
}
