package com.vyavsa.app.feature.transactions

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
import com.vyavsa.app.feature.bank.FakeBankRepository
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepo: FakeTransactionsRepository
    private lateinit var fakeBankRepo: FakeBankRepository
    private lateinit var refreshCoordinator: DataRefreshCoordinator
    private lateinit var viewModel: TransactionsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepo = FakeTransactionsRepository()
        fakeBankRepo = FakeBankRepository()
        refreshCoordinator = DataRefreshCoordinator()
        viewModel = TransactionsViewModel(fakeRepo, fakeBankRepo, refreshCoordinator)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testLoadBanksPopulatesBanksState() = runTest {
        viewModel.loadBanks()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.banksState.value
        assertTrue(state is UiState.Success)
        val banks = (state as UiState.Success).data
        assertEquals(2, banks.size)
        assertEquals("HDFC Bank", banks[0].bankName)
    }

    @Test
    fun testCreateLineSaleSuccessWithBank() = runTest {
        viewModel.createLineSale(
            customerId = "cust-1",
            totalAmount = "1500.00",
            cashIn = "1000.00",
            bankAmount = "500.00",
            bankId = "bank-1",
            route = "Sector A",
            salesman = "Ramesh",
            note = "Morning"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val actionState = viewModel.createActionState.value
        assertTrue("Expected Success, got $actionState", actionState is UiState.Success)

        val lastReq = fakeRepo.lastCreateLineSaleRequest
        assertNotNull(lastReq)
        assertEquals("cust-1", lastReq?.customerId)
        assertEquals("1500.00", lastReq?.totalAmount)
        assertEquals("1000.00", lastReq?.cashAmount)
        assertEquals("500.00", lastReq?.bankAmount)
        assertEquals("bank-1", lastReq?.bankId)
    }

    @Test
    fun testCreateLineSaleValidationMissingCustomer() = runTest {
        viewModel.createLineSale(
            customerId = "",
            totalAmount = "500.00",
            cashIn = "500.00",
            bankAmount = "0.00",
            bankId = null
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val actionState = viewModel.createActionState.value
        assertTrue(actionState is UiState.Error)
        assertEquals("Customer and total amount are required", (actionState as UiState.Error).message)
    }

    @Test
    fun testCreateLineSaleValidationBankRequiredIfBankAmountNonZero() = runTest {
        viewModel.createLineSale(
            customerId = "cust-1",
            totalAmount = "1000.00",
            cashIn = "500.00",
            bankAmount = "500.00",
            bankId = null
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val actionState = viewModel.createActionState.value
        assertTrue(actionState is UiState.Error)
        assertEquals("Please select a bank account for bank payment", (actionState as UiState.Error).message)
    }

    @Test
    fun testCreateLineSaleValidationOverpayment() = runTest {
        viewModel.createLineSale(
            customerId = "cust-1",
            totalAmount = "1000.00",
            cashIn = "800.00",
            bankAmount = "400.00",
            bankId = "bank-1"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val actionState = viewModel.createActionState.value
        assertTrue(actionState is UiState.Error)
        assertEquals("Collected amount cannot exceed the total invoice amount", (actionState as UiState.Error).message)
    }

    @Test
    fun testCreateCounterSaleSuccess() = runTest {
        viewModel.createCounterSale(
            item = "Biscuits",
            price = "50.00",
            totalAmount = "200.00",
            cash = "100.00",
            bankAmount = "100.00",
            bankId = "bank-2"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val actionState = viewModel.createActionState.value
        assertTrue(actionState is UiState.Success)

        val lastReq = fakeRepo.lastCreateCounterSaleRequest
        assertNotNull(lastReq)
        assertEquals("Biscuits", lastReq?.item)
        assertEquals("200.00", lastReq?.totalAmount)
        assertEquals("split", lastReq?.paymentMethod)
        assertEquals("100.00", lastReq?.cash)
        assertEquals("100.00", lastReq?.bankAmount)
        assertEquals("bank-2", lastReq?.bankId)
    }

    @Test
    fun testCreateCounterSaleCashOnlyMethod() = runTest {
        viewModel.createCounterSale(
            item = "Chips",
            price = "20.00",
            totalAmount = "40.00",
            cash = "40.00",
            bankAmount = "0.00",
            bankId = null
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val lastReq = fakeRepo.lastCreateCounterSaleRequest
        assertEquals("cash", lastReq?.paymentMethod)
        assertNull(lastReq?.bankId)
    }

    @Test
    fun testCreateCounterSaleBankOnlyMethod() = runTest {
        viewModel.createCounterSale(
            item = "Cold Drinks",
            price = "150.00",
            totalAmount = "150.00",
            cash = "0.00",
            bankAmount = "150.00",
            bankId = "bank-1"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val lastReq = fakeRepo.lastCreateCounterSaleRequest
        assertEquals("bank", lastReq?.paymentMethod)
        assertEquals("bank-1", lastReq?.bankId)
    }

    @Test
    fun testCreateCounterSaleValidationMissingItem() = runTest {
        viewModel.createCounterSale(
            item = "",
            price = "10.00",
            totalAmount = "10.00",
            cash = "10.00"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val actionState = viewModel.createActionState.value
        assertTrue(actionState is UiState.Error)
        assertEquals("Item description and total amount are required", (actionState as UiState.Error).message)
    }

    @Test
    fun testCreateCounterSaleValidationOverpayment() = runTest {
        viewModel.createCounterSale(
            item = "Soap",
            price = "50.00",
            totalAmount = "50.00",
            cash = "60.00"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val actionState = viewModel.createActionState.value
        assertTrue(actionState is UiState.Error)
        assertEquals("Collected amount cannot exceed the total sale amount", (actionState as UiState.Error).message)
    }

    @Test
    fun testPreventDuplicateSubmissionWhileLoading() = runTest {
        fakeRepo.delayResponse = true
        viewModel.createLineSale(
            customerId = "cust-1",
            totalAmount = "500.00",
            cashIn = "500.00",
            bankAmount = "0.00",
            bankId = null
        )
        testDispatcher.scheduler.runCurrent()

        assertTrue(viewModel.createActionState.value is UiState.Loading)

        val initialCallCount = fakeRepo.createLineSaleCallCount
        // Second call while loading must be ignored
        viewModel.createLineSale(
            customerId = "cust-1",
            totalAmount = "500.00",
            cashIn = "500.00",
            bankAmount = "0.00",
            bankId = null
        )
        assertEquals(initialCallCount, fakeRepo.createLineSaleCallCount)
    }

    @Test
    fun testRealTimeSyncOnCoordinatorSalesMutation() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        val initialSalesCount = (viewModel.lineSalesState.value as UiState.Success).data.size
        assertEquals(1, initialSalesCount)

        fakeRepo.lineSales.add(
            LineSale(
                id = "sale-new",
                customerId = "cust-2",
                customerName = "Gupta Mart",
                totalAmount = "3000.00",
                totalCashIn = "3000.00",
                balance = "0.00",
                createdAt = "2026-10-15"
            )
        )

        refreshCoordinator.notifyMutation(DataMutation.SALES)
        testDispatcher.scheduler.advanceUntilIdle()

        val updatedState = viewModel.lineSalesState.value
        assertTrue(updatedState is UiState.Success)
        assertEquals(2, (updatedState as UiState.Success).data.size)
    }
}

class FakeTransactionsRepository : TransactionsRepository {
    val lineSales = mutableListOf(
        LineSale(
            id = "sale-1",
            customerId = "cust-1",
            customerName = "Sharma Stores",
            totalAmount = "1500.00",
            totalCashIn = "1000.00",
            balance = "500.00",
            createdAt = "2026-10-14"
        )
    )

    val counterSales = mutableListOf(
        CounterSale(
            id = "counter-1",
            item = "Daily Ration",
            price = "500.00",
            totalAmount = "500.00",
            paymentMethod = "cash",
            cash = "500.00",
            createdAt = "2026-10-14"
        )
    )

    val expenses = mutableListOf<Expense>()
    val customers = mutableListOf(
        Customer(id = "cust-1", customerName = "Sharma Stores", currentBalance = "500.00"),
        Customer(id = "cust-2", customerName = "Gupta Mart", currentBalance = "0.00")
    )

    var lastCreateLineSaleRequest: CreateLineSaleRequest? = null
    var lastCreateCounterSaleRequest: CreateCounterSaleRequest? = null
    var createLineSaleCallCount = 0
    var delayResponse = false

    override fun listLineSales(date: String?, search: String?): Flow<Resource<List<LineSale>>> = flow {
        emit(Resource.Loading)
        emit(Resource.Success(lineSales.toList()))
    }

    override fun createLineSale(request: CreateLineSaleRequest): Flow<Resource<LineSale>> = flow {
        createLineSaleCallCount++
        emit(Resource.Loading)
        lastCreateLineSaleRequest = request
        if (!delayResponse) {
            val sale = LineSale(
                id = "sale-${lineSales.size + 1}",
                customerId = request.customerId,
                customerName = "Customer",
                totalAmount = request.totalAmount,
                totalCashIn = request.cashAmount ?: "0.00",
                balance = "0.00",
                createdAt = "2026-10-15"
            )
            lineSales.add(sale)
            emit(Resource.Success(sale))
        }
    }

    override fun listCounterSales(date: String?, search: String?): Flow<Resource<List<CounterSale>>> = flow {
        emit(Resource.Loading)
        emit(Resource.Success(counterSales.toList()))
    }

    override fun createCounterSale(request: CreateCounterSaleRequest): Flow<Resource<CounterSale>> = flow {
        emit(Resource.Loading)
        lastCreateCounterSaleRequest = request
        val sale = CounterSale(
            id = "counter-${counterSales.size + 1}",
            item = request.item,
            price = request.price,
            totalAmount = request.totalAmount,
            paymentMethod = request.paymentMethod,
            cash = request.cash,
            createdAt = "2026-10-15"
        )
        counterSales.add(sale)
        emit(Resource.Success(sale))
    }

    override fun listExpenses(date: String?, search: String?): Flow<Resource<List<Expense>>> = flow {
        emit(Resource.Loading)
        emit(Resource.Success(expenses.toList()))
    }

    override fun createExpense(request: CreateExpenseRequest): Flow<Resource<Expense>> = flow {
        emit(Resource.Loading)
        val exp = Expense(
            id = "exp-${expenses.size + 1}",
            item = request.item,
            category = request.category,
            totalAmount = request.totalAmount,
            paymentMethod = request.paymentMethod,
            createdAt = "2026-10-15"
        )
        expenses.add(exp)
        emit(Resource.Success(exp))
    }

    override fun listCustomers(): Flow<Resource<List<Customer>>> = flow {
        emit(Resource.Loading)
        emit(Resource.Success(customers.toList()))
    }
}
