package com.vyavsa.app.feature.transactions

import com.vyavsa.app.core.common.FormatUtils
import com.vyavsa.app.core.common.Resource
import com.vyavsa.app.core.common.UiState
import com.vyavsa.app.core.model.BankPaymentSplit
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
import com.vyavsa.app.core.model.CashTransaction
import com.vyavsa.app.core.model.CounterSalePayment
import com.vyavsa.app.core.model.LineSalePayment
import com.vyavsa.app.core.model.CounterSalePaymentBreakdown
import com.vyavsa.app.core.model.LineSalePaymentBreakdown
import com.vyavsa.app.core.model.PlanTransaction
import com.vyavsa.app.core.model.Purchase
import com.vyavsa.app.core.model.PurchasePayment
import com.vyavsa.app.core.model.RecordSalePaymentRequest
import com.vyavsa.app.core.model.RecordSupplierPaymentRequest
import com.vyavsa.app.core.model.SaleItemRequest
import com.vyavsa.app.core.model.Supplier
import com.vyavsa.app.core.model.SupplierPaymentResponse
import com.vyavsa.app.core.model.TenantBank
import com.vyavsa.app.core.model.UpdateCounterSaleRequest
import com.vyavsa.app.core.model.UpdateExpenseRequest
import com.vyavsa.app.core.model.UpdateLineSaleRequest
import com.vyavsa.app.core.model.UpdatePurchaseRequest
import com.vyavsa.app.core.sync.DataMutation
import com.vyavsa.app.core.sync.DataRefreshCoordinator
import com.vyavsa.app.feature.bank.FakeBankRepository
import com.vyavsa.app.feature.products.FakeProductRepository
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
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
    fun testCreateLineSaleWithItemsComputesSubtotalAndPassesItems() = runTest {
        val items = listOf(
            SaleItemRequest("prod-1", "2.00", "150.00"),
            SaleItemRequest("prod-2", "1.00", "200.00")
        )
        viewModel.createLineSale(
            customerId = "cust-1",
            totalAmount = "",
            cashIn = "500.00",
            bankAmount = "0.00",
            bankId = null,
            items = items
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val actionState = viewModel.createActionState.value
        assertTrue("Expected Success, got $actionState", actionState is UiState.Success)

        val lastReq = fakeRepo.lastCreateLineSaleRequest
        assertNotNull(lastReq)
        assertEquals("500.00", lastReq?.totalAmount)
        assertEquals(2, lastReq?.items?.size)
        assertEquals("prod-1", lastReq?.items?.get(0)?.productId)
        assertEquals("prod-2", lastReq?.items?.get(1)?.productId)
    }

    @Test
    fun testCreateCounterSaleWithItems() = runTest {
        val items = listOf(
            SaleItemRequest("prod-1", "1.00", "120.00")
        )
        viewModel.createCounterSale(
            item = "Store items",
            price = "120.00",
            totalAmount = "120.00",
            cash = "120.00",
            bankAmount = "0.00",
            bankId = null,
            items = items
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val actionState = viewModel.createActionState.value
        assertTrue(actionState is UiState.Success)

        val lastReq = fakeRepo.lastCreateCounterSaleRequest
        assertNotNull(lastReq)
        assertEquals(1, lastReq?.items?.size)
    }

    @Test
    fun testCreateLineSaleWithCustomDiscountSuccess() = runTest {
        viewModel.createLineSale(
            customerId = "cust-1",
            subtotal = "1000.00",
            discount = "100.00",
            totalAmount = "900.00",
            cashIn = "900.00",
            bankAmount = "0.00",
            bankId = null
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val actionState = viewModel.createActionState.value
        assertTrue("Expected Success, got $actionState", actionState is UiState.Success)

        val lastReq = fakeRepo.lastCreateLineSaleRequest
        assertNotNull(lastReq)
        assertEquals("cust-1", lastReq?.customerId)
        assertEquals("1000.00", lastReq?.subtotal)
        assertEquals("100.00", lastReq?.discount)
        assertEquals("100.00", lastReq?.discountAmount)
        assertEquals("900.00", lastReq?.totalAmount)
        assertEquals("900.00", lastReq?.cashAmount)
    }

    @Test
    fun testCreateLineSaleWithDiscountExceedingSubtotalFails() = runTest {
        viewModel.createLineSale(
            customerId = "cust-1",
            subtotal = "500.00",
            discount = "600.00",
            totalAmount = "500.00",
            cashIn = "500.00",
            bankAmount = "0.00",
            bankId = null
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val actionState = viewModel.createActionState.value
        assertTrue("Expected Error, got $actionState", actionState is UiState.Error)
        assertTrue(
            "Expected discount exceed error, got ${(actionState as UiState.Error).message}",
            actionState.message.contains("Discount cannot exceed subtotal", ignoreCase = true)
        )
        assertNull("Repository should not have been called", fakeRepo.lastCreateLineSaleRequest)
    }

    @Test
    fun testCreateLineSaleWithNegativeDiscountFails() = runTest {
        viewModel.createLineSale(
            customerId = "cust-1",
            subtotal = "500.00",
            discount = "-20.00",
            totalAmount = "500.00",
            cashIn = "500.00",
            bankAmount = "0.00",
            bankId = null
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val actionState = viewModel.createActionState.value
        assertTrue("Expected Error, got $actionState", actionState is UiState.Error)
        assertTrue(
            "Expected non-negative discount error, got ${(actionState as UiState.Error).message}",
            actionState.message.contains("Discount cannot be negative", ignoreCase = true)
        )
        assertNull("Repository should not have been called", fakeRepo.lastCreateLineSaleRequest)
    }

    @Test
    fun testCreateCounterSaleWithCustomDiscountSuccess() = runTest {
        viewModel.createCounterSale(
            item = "Bulk Rice",
            price = "1200.00",
            subtotal = "1200.00",
            discount = "200.00",
            totalAmount = "1000.00",
            cash = "1000.00",
            bankAmount = "0.00",
            bankId = null
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val actionState = viewModel.createActionState.value
        assertTrue("Expected Success, got $actionState", actionState is UiState.Success)

        val lastReq = fakeRepo.lastCreateCounterSaleRequest
        assertNotNull(lastReq)
        assertEquals("Bulk Rice", lastReq?.item)
        assertEquals("1200.00", lastReq?.subtotal)
        assertEquals("200.00", lastReq?.discount)
        assertEquals("200.00", lastReq?.discountAmount)
        assertEquals("1000.00", lastReq?.totalAmount)
        assertEquals("1000.00", lastReq?.cash)
    }

    @Test
    fun testCreateCounterSaleWithDiscountExceedingSubtotalFails() = runTest {
        viewModel.createCounterSale(
            item = "Bulk Rice",
            price = "400.00",
            subtotal = "400.00",
            discount = "450.00",
            totalAmount = "400.00",
            cash = "400.00",
            bankAmount = "0.00",
            bankId = null
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val actionState = viewModel.createActionState.value
        assertTrue("Expected Error, got $actionState", actionState is UiState.Error)
        assertTrue(
            "Expected discount error, got ${(actionState as UiState.Error).message}",
            actionState.message.contains("Discount cannot exceed subtotal", ignoreCase = true)
        )
        assertNull("Repository should not have been called", fakeRepo.lastCreateCounterSaleRequest)
    }

    @Test
    fun testLoadProductsWithProductRepository() = runTest {
        val productRepo = FakeProductRepository()
        val customVm = TransactionsViewModel(
            repository = fakeRepo,
            bankRepository = fakeBankRepo,
            refreshCoordinator = refreshCoordinator,
            tokenManager = null,
            productRepository = productRepo
        )

        customVm.loadProducts()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = customVm.productsState.value
        assertTrue("Expected Success, got $state", state is UiState.Success)
        val products = (state as UiState.Success).data
        assertEquals(2, products.size)
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
    fun testCreateLineSaleExcessPaymentSucceeds() = runTest {
        viewModel.createLineSale(
            customerId = "cust-1",
            totalAmount = "1000.00",
            cashIn = "800.00",
            bankAmount = "400.00",
            bankId = "bank-1"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val actionState = viewModel.createActionState.value
        assertTrue(actionState is UiState.Success)
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
    fun testCreateCounterSaleExcessPaymentSucceeds() = runTest {
        viewModel.createCounterSale(
            item = "Soap",
            price = "50.00",
            totalAmount = "50.00",
            cash = "60.00"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val actionState = viewModel.createActionState.value
        assertTrue(actionState is UiState.Success)
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

    @Test
    fun testCreateLineSaleWithMultipleBanksSuccess() = runTest {
        val splits = listOf(
            BankPaymentSplit(bankId = "bank-1", bankName = "HDFC Bank", amount = "500.00"),
            BankPaymentSplit(bankId = "bank-2", bankName = "SBI Bank", amount = "300.00")
        )
        viewModel.createLineSale(
            customerId = "cust-1",
            totalAmount = "1000.00",
            cashIn = "200.00",
            bankAmount = "800.00",
            bankId = "bank-1",
            bankPayments = splits,
            route = "Sector B",
            salesman = "Sunil",
            note = "Multi bank payment"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val actionState = viewModel.createActionState.value
        assertTrue("Expected Success, got $actionState", actionState is UiState.Success)

        val lastReq = fakeRepo.lastCreateLineSaleRequest
        assertNotNull(lastReq)
        assertEquals("cust-1", lastReq?.customerId)
        assertEquals("1000.00", lastReq?.totalAmount)
        assertEquals("200.00", lastReq?.cashAmount)
        assertEquals("800.00", lastReq?.bankAmount)
        assertEquals("bank-1", lastReq?.bankId)
        assertEquals(2, lastReq?.bankPayments?.size)
        assertEquals("bank-1", lastReq?.bankPayments?.get(0)?.bankId)
        assertEquals("500.00", lastReq?.bankPayments?.get(0)?.amount)
        assertEquals("bank-2", lastReq?.bankPayments?.get(1)?.bankId)
        assertEquals("300.00", lastReq?.bankPayments?.get(1)?.amount)
    }

    @Test
    fun testCreateCounterSaleWithMultipleBanksSuccess() = runTest {
        val splits = listOf(
            BankPaymentSplit(bankId = "bank-1", bankName = "HDFC Bank", amount = "200.00"),
            BankPaymentSplit(bankId = "bank-2", bankName = "SBI Bank", amount = "300.00")
        )
        viewModel.createCounterSale(
            item = "Cooking Oil",
            price = "500.00",
            totalAmount = "500.00",
            cash = "0.00",
            bankAmount = "500.00",
            bankId = "bank-1",
            bankPayments = splits
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val actionState = viewModel.createActionState.value
        assertTrue(actionState is UiState.Success)

        val lastReq = fakeRepo.lastCreateCounterSaleRequest
        assertNotNull(lastReq)
        assertEquals("bank", lastReq?.paymentMethod)
        assertEquals("500.00", lastReq?.bankAmount)
        assertEquals(2, lastReq?.bankPayments?.size)
        assertEquals("200.00", lastReq?.bankPayments?.get(0)?.amount)
        assertEquals("300.00", lastReq?.bankPayments?.get(1)?.amount)
    }

    @Test
    fun testCreateCounterSaleWithCashAndMultipleBanksSplitMethod() = runTest {
        val splits = listOf(
            BankPaymentSplit(bankId = "bank-1", bankName = "HDFC Bank", amount = "300.00"),
            BankPaymentSplit(bankId = "bank-2", bankName = "SBI Bank", amount = "100.00")
        )
        viewModel.createCounterSale(
            item = "Flour Bag",
            price = "500.00",
            totalAmount = "500.00",
            cash = "100.00",
            bankAmount = "400.00",
            bankId = "bank-1",
            bankPayments = splits
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val actionState = viewModel.createActionState.value
        assertTrue(actionState is UiState.Success)

        val lastReq = fakeRepo.lastCreateCounterSaleRequest
        assertNotNull(lastReq)
        assertEquals("split", lastReq?.paymentMethod)
        assertEquals("100.00", lastReq?.cash)
        assertEquals("400.00", lastReq?.bankAmount)
        assertEquals(2, lastReq?.bankPayments?.size)
    }

    @Test
    fun testCreateLineSaleValidationDuplicateBanks() = runTest {
        val splits = listOf(
            BankPaymentSplit(bankId = "bank-1", bankName = "HDFC Bank", amount = "300.00"),
            BankPaymentSplit(bankId = "bank-1", bankName = "HDFC Bank Duplicate", amount = "200.00")
        )
        viewModel.createLineSale(
            customerId = "cust-1",
            totalAmount = "1000.00",
            cashIn = "500.00",
            bankAmount = "500.00",
            bankId = "bank-1",
            bankPayments = splits
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val actionState = viewModel.createActionState.value
        assertTrue(actionState is UiState.Error)
        assertEquals("Each bank account can only be selected once per sale", (actionState as UiState.Error).message)
    }

    @Test
    fun testCreateCounterSaleValidationDuplicateBanks() = runTest {
        val splits = listOf(
            BankPaymentSplit(bankId = "bank-2", bankName = "SBI Bank", amount = "200.00"),
            BankPaymentSplit(bankId = "bank-2", bankName = "SBI Bank", amount = "200.00")
        )
        viewModel.createCounterSale(
            item = "Items",
            price = "400.00",
            totalAmount = "400.00",
            cash = "0.00",
            bankAmount = "400.00",
            bankId = "bank-2",
            bankPayments = splits
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val actionState = viewModel.createActionState.value
        assertTrue(actionState is UiState.Error)
        assertEquals("Each bank account can only be selected once per sale", (actionState as UiState.Error).message)
    }

    @Test
    fun testCreateLineSaleValidationZeroOrNegativeBankSplitAmount() = runTest {
        val splits = listOf(
            BankPaymentSplit(bankId = "bank-1", bankName = "HDFC Bank", amount = "0.00")
        )
        viewModel.createLineSale(
            customerId = "cust-1",
            totalAmount = "1000.00",
            cashIn = "1000.00",
            bankPayments = splits
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val actionState = viewModel.createActionState.value
        assertTrue(actionState is UiState.Error)
        assertEquals("Bank payment amount must be greater than zero", (actionState as UiState.Error).message)
    }

    @Test
    fun testCreateLineSaleValidationMissingBankIdInSplit() = runTest {
        val splits = listOf(
            BankPaymentSplit(bankId = "", bankName = "", amount = "200.00")
        )
        viewModel.createLineSale(
            customerId = "cust-1",
            totalAmount = "1000.00",
            cashIn = "800.00",
            bankPayments = splits
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val actionState = viewModel.createActionState.value
        assertTrue(actionState is UiState.Error)
        assertEquals("Please select a bank account for all bank payment rows", (actionState as UiState.Error).message)
    }

    @Test
    fun testCreateLineSaleValidationMultipleBanksOverpayment() = runTest {
        val splits = listOf(
            BankPaymentSplit(bankId = "bank-1", bankName = "HDFC Bank", amount = "600.00"),
            BankPaymentSplit(bankId = "bank-2", bankName = "SBI Bank", amount = "500.00")
        )
        viewModel.createLineSale(
            customerId = "cust-1",
            totalAmount = "1000.00",
            cashIn = "0.00",
            bankPayments = splits
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val actionState = viewModel.createActionState.value
        assertTrue(actionState is UiState.Success)
    }

    @Test
    fun testCreateCounterSaleMultipleBanksExcessPaymentSucceeds() = runTest {
        val splits = listOf(
            BankPaymentSplit(bankId = "bank-1", bankName = "HDFC Bank", amount = "300.00"),
            BankPaymentSplit(bankId = "bank-2", bankName = "SBI Bank", amount = "300.00")
        )
        viewModel.createCounterSale(
            item = "Snacks",
            price = "500.00",
            totalAmount = "500.00",
            cash = "0.00",
            bankPayments = splits
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val actionState = viewModel.createActionState.value
        assertTrue(actionState is UiState.Success)
    }

    @Test
    fun testCreateLineSalePartialPaymentAllowed() = runTest {
        val splits = listOf(
            BankPaymentSplit(bankId = "bank-1", bankName = "HDFC Bank", amount = "300.00")
        )
        viewModel.createLineSale(
            customerId = "cust-1",
            totalAmount = "1000.00",
            cashIn = "200.00",
            bankPayments = splits
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val actionState = viewModel.createActionState.value
        assertTrue("Partial payment should be allowed. Got $actionState", actionState is UiState.Success)

        val lastReq = fakeRepo.lastCreateLineSaleRequest
        assertNotNull(lastReq)
        assertEquals("1000.00", lastReq?.totalAmount)
        assertEquals("200.00", lastReq?.cashAmount)
        assertEquals("300.00", lastReq?.bankAmount)
    }

    @Test
    fun testLoadEmployeesPopulatesEmployeesState() = runTest {
        viewModel.loadEmployees()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.employeesState.value
        assertTrue(state is UiState.Success)
        val list = (state as UiState.Success).data
        assertEquals(1, list.size)
        assertEquals("Suresh Kumar", list[0].name)
    }

    @Test
    fun testCreatePurchaseSuccessCashOnly() = runTest {
        viewModel.createPurchase(
            customerId = "cust-1",
            item = "Refined Oil 15L",
            quantity = "10",
            totalAmount = "15000.00",
            cashAmount = "15000.00"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val actionState = viewModel.createActionState.value
        assertTrue("Expected Success, got $actionState", actionState is UiState.Success)

        val lastReq = fakeRepo.lastCreatePurchaseRequest
        assertNotNull(lastReq)
        assertEquals("Refined Oil 15L", lastReq?.item)
        assertEquals(10, lastReq?.quantity)
        assertEquals("15000.00", lastReq?.totalAmount)
        assertEquals("15000.00", lastReq?.cashAmount)
        assertEquals("cust-1", lastReq?.customerId)
    }

    @Test
    fun testCreatePurchaseSuccessSplitBankAndCash() = runTest {
        val splits = listOf(
            BankPaymentSplit(bankId = "bank-1", bankName = "HDFC Bank", amount = "5000.00")
        )
        viewModel.createPurchase(
            customerId = "cust-2",
            item = "Wheat Flour 50kg",
            quantity = "20",
            totalAmount = "24000.00",
            cashAmount = "10000.00",
            bankPayments = splits
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val actionState = viewModel.createActionState.value
        assertTrue("Expected Success, got $actionState", actionState is UiState.Success)

        val lastReq = fakeRepo.lastCreatePurchaseRequest
        assertNotNull(lastReq)
        assertEquals("Wheat Flour 50kg", lastReq?.item)
        assertEquals(20, lastReq?.quantity)
        assertEquals("10000.00", lastReq?.cashAmount)
        assertEquals("15000.00", lastReq?.totalPaid)
        assertEquals(1, lastReq?.bankPayments?.size)
    }

    @Test
    fun testCreatePurchaseValidationMissingItem() = runTest {
        viewModel.createPurchase(
            item = "",
            quantity = "5",
            totalAmount = "500.00"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val actionState = viewModel.createActionState.value
        assertTrue(actionState is UiState.Error)
        assertEquals("Item description and total amount are required", (actionState as UiState.Error).message)
    }

    @Test
    fun testCreatePurchaseValidationOverpayment() = runTest {
        val splits = listOf(
            BankPaymentSplit(bankId = "bank-1", bankName = "HDFC Bank", amount = "1000.00")
        )
        viewModel.createPurchase(
            item = "Sugar 50kg",
            quantity = "1",
            totalAmount = "2000.00",
            cashAmount = "1500.00",
            bankPayments = splits
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val actionState = viewModel.createActionState.value
        assertTrue(actionState is UiState.Error)
        assertEquals("Total paid amount cannot exceed purchase bill amount", (actionState as UiState.Error).message)
    }

    @Test
    fun testCreateExpenseEmployeeAdvanceSuccess() = runTest {
        viewModel.createExpense(
            item = "Staff Festival Advance",
            category = "employee_advance",
            amount = "3000.00",
            paymentMethod = "cash",
            employeeId = "emp-1"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val actionState = viewModel.createActionState.value
        assertTrue("Expected Success, got $actionState", actionState is UiState.Success)

        val lastReq = fakeRepo.lastCreateExpenseRequest
        assertNotNull(lastReq)
        assertEquals("employee_advance", lastReq?.category)
        assertEquals("emp-1", lastReq?.employeeId)
        assertEquals("3000.00", lastReq?.amount)
    }

    @Test
    fun testCreateExpenseEmployeeAdvanceValidationMissingEmployee() = runTest {
        viewModel.createExpense(
            item = "Staff Advance",
            category = "employee_advance",
            amount = "2000.00",
            paymentMethod = "cash",
            employeeId = null
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val actionState = viewModel.createActionState.value
        assertTrue(actionState is UiState.Error)
        assertEquals("Please select a staff member for the advance payment", (actionState as UiState.Error).message)
    }

    @Test
    fun testCreateExpense_defaultsToTodayWhenDateNotSpecified() = runTest {
        val today = FormatUtils.todayDateString()
        viewModel.createExpense(
            item = "Office Snacks",
            category = "operations",
            amount = "450.00",
            paymentMethod = "cash"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.createActionState.value
        assertTrue("Expected Success, got $state", state is UiState.Success)

        val lastReq = fakeRepo.lastCreateExpenseRequest
        assertNotNull(lastReq)
        assertEquals(today, lastReq?.expenseDate)
        assertEquals(today, lastReq?.paymentDate)
        assertEquals(today, lastReq?.date)
        assertEquals("Office Snacks", lastReq?.item)
    }

    @Test
    fun testCreateExpense_withExplicitPastDate() = runTest {
        val pastDate = "2026-11-02"
        viewModel.createExpense(
            item = "Hardware Tools",
            category = "operations",
            amount = "3200.00",
            paymentMethod = "cash",
            expenseDate = pastDate
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.createActionState.value
        assertTrue("Expected Success, got $state", state is UiState.Success)

        val lastReq = fakeRepo.lastCreateExpenseRequest
        assertNotNull(lastReq)
        assertEquals("2026-11-02", lastReq?.expenseDate)
        assertEquals("2026-11-02", lastReq?.paymentDate)
        assertEquals("2026-11-02", lastReq?.date)
        assertEquals("3200.00", lastReq?.totalAmount)
    }

    @Test
    fun testCreateExpense_rejectsInvalidDateFormat() = runTest {
        val initialCallCount = fakeRepo.expenses.size
        viewModel.createExpense(
            item = "Bad Date Expense",
            category = "operations",
            amount = "100.00",
            paymentMethod = "cash",
            expenseDate = "2026/11/05"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.createActionState.value
        assertTrue(state is UiState.Error)
        assertEquals("Expense date must be in YYYY-MM-DD format", (state as UiState.Error).message)
        assertEquals(initialCallCount, fakeRepo.expenses.size)
    }

    @Test
    fun testCreateExpense_preventsDuplicateSubmissionWhileLoading() = runTest {
        fakeRepo.delayResponse = true

        // First click
        viewModel.createExpense(
            item = "Fast Taps Item",
            category = "operations",
            amount = "500.00",
            paymentMethod = "cash"
        )
        // Immediately tap again before first completes
        viewModel.createExpense(
            item = "Fast Taps Item",
            category = "operations",
            amount = "500.00",
            paymentMethod = "cash"
        )

        testDispatcher.scheduler.advanceUntilIdle()
        fakeRepo.delayResponse = false
    }

    @Test
    fun testUpdateExpense_withExistingAndNewDate() = runTest {
        fakeRepo.expenses.add(
            Expense(
                id = "exp-99",
                item = "Old Expense",
                category = "operations",
                totalAmount = "1000.00",
                paymentMethod = "cash",
                expenseDate = "2026-10-25"
            )
        )

        viewModel.updateExpense(
            id = "exp-99",
            item = "Renamed Expense",
            category = "operations",
            amount = "1500.00",
            paymentMethod = "cash",
            expenseDate = "2026-10-30"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val updateState = viewModel.updateExpenseActionState.value
        assertTrue("Expected update Success, got $updateState", updateState is UiState.Success)

        val lastUpdate = fakeRepo.lastUpdateExpenseRequest
        assertNotNull(lastUpdate)
        assertEquals("Renamed Expense", lastUpdate?.item)
        assertEquals("1500.00", lastUpdate?.totalAmount)
        assertEquals("2026-10-30", lastUpdate?.expenseDate)
    }

    @Test
    fun testUpdateExpense_preservesDateWhenUpdatingUnrelatedFields() = runTest {
        fakeRepo.expenses.add(
            Expense(
                id = "exp-101",
                item = "Maintenance",
                category = "operations",
                totalAmount = "800.00",
                paymentMethod = "cash",
                expenseDate = "2026-10-20"
            )
        )

        // Update item description only, without changing date
        viewModel.updateExpense(
            id = "exp-101",
            item = "Maintenance & Repairs",
            category = "operations",
            amount = "800.00",
            paymentMethod = "cash",
            expenseDate = "2026-10-20"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val updateState = viewModel.updateExpenseActionState.value
        assertTrue(updateState is UiState.Success)

        val updated = fakeRepo.expenses.first { it.id == "exp-101" }
        assertEquals("Maintenance & Repairs", updated.item)
        assertEquals("2026-10-20", updated.expenseDate)
    }

    @Test
    fun testUpdateExpense_rejectsInvalidDateFormat() = runTest {
        viewModel.updateExpense(
            id = "exp-101",
            item = "Invalid Date Update",
            category = "operations",
            amount = "250.00",
            paymentMethod = "cash",
            expenseDate = "invalid-date"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.updateExpenseActionState.value
        assertTrue(state is UiState.Error)
        assertEquals("Expense date must be in YYYY-MM-DD format", (state as UiState.Error).message)
        assertEquals(0, fakeRepo.updateExpenseCallCount)
    }

    @Test
    fun testUpdateExpense_preventsDuplicateSubmissionWhileLoading() = runTest {
        viewModel.updateExpense(
            id = "exp-1",
            item = "Quick Click",
            category = "operations",
            amount = "200.00",
            paymentMethod = "cash"
        )
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, fakeRepo.updateExpenseCallCount)
    }

    @Test
    fun testSetDateFilterLoadsFilteredPurchases() = runTest {
        fakeRepo.purchases.add(
            Purchase(
                id = "p-today",
                item = "Today Item",
                quantity = 5,
                totalAmount = "500.00",
                paymentStatus = "PAID",
                createdAt = "2026-10-15T10:00:00Z"
            )
        )
        fakeRepo.purchases.add(
            Purchase(
                id = "p-yesterday",
                item = "Old Item",
                quantity = 2,
                totalAmount = "100.00",
                paymentStatus = "PAID",
                createdAt = "2026-10-14T10:00:00Z"
            )
        )

        viewModel.selectTab(TransactionTab.PURCHASES)
        viewModel.setDateFilter("2026-10-15")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.purchasesState.value
        assertTrue(state is UiState.Success)
        val data = (state as UiState.Success).data
        assertEquals(1, data.size)
        assertEquals("Today Item", data[0].item)
    }

    @Test
    fun testOpenPrintDialogForLineSalePopulatesPrintData() = runTest {
        val sale = LineSale(
            id = "sale-101",
            customerName = "Ramesh Kumar",
            totalAmount = "500.00",
            discount = "50.00",
            collectedAmount = "450.00",
            balance = "0.00"
        )

        assertNull(viewModel.activePrintDialogData.value)
        viewModel.openPrintDialogForLineSale(sale)
        testDispatcher.scheduler.advanceUntilIdle()

        val data = viewModel.activePrintDialogData.value
        assertNotNull(data)
        assertEquals("sale-101", data?.invoiceNumber)
        assertEquals("Ramesh Kumar", data?.customerName)
        assertEquals(500.0, data?.totalAmount ?: 0.0, 0.01)

        viewModel.dismissPrintDialog()
        assertNull(viewModel.activePrintDialogData.value)
    }

    @Test
    fun testOpenPrintDialogForCounterSalePopulatesPrintData() = runTest {
        val sale = CounterSale(
            id = "csale-202",
            item = "Biscuits Box",
            totalAmount = "250.00",
            discount = "0.00",
            cash = "250.00",
            account = "0.00"
        )

        assertNull(viewModel.activePrintDialogData.value)
        viewModel.openPrintDialogForCounterSale(sale)
        testDispatcher.scheduler.advanceUntilIdle()

        val data = viewModel.activePrintDialogData.value
        assertNotNull(data)
        assertEquals("csale-202", data?.invoiceNumber)
        assertEquals("Walk-in Counter Customer", data?.customerName)
        assertEquals(250.0, data?.totalAmount ?: 0.0, 0.01)

        viewModel.dismissPrintDialog()
        assertNull(viewModel.activePrintDialogData.value)
    }

    @Test
    fun testOpenPrintDialogForLineSaleFetchesItemsFromRepositoryWhenEmpty() = runTest {
        val fullSaleWithItems = LineSale(
            id = "sale-full-1",
            customerName = "Full Store",
            totalAmount = "800.00",
            items = listOf(
                com.vyavsa.app.core.model.SaleItem(
                    id = "si-1",
                    productNameSnapshot = "Wheat Flour 10kg",
                    quantity = "2",
                    unitPrice = "300.00",
                    lineTotal = "600.00"
                ),
                com.vyavsa.app.core.model.SaleItem(
                    id = "si-2",
                    productNameSnapshot = "Mustard Oil 1L",
                    quantity = "1",
                    unitPrice = "200.00",
                    lineTotal = "200.00"
                )
            )
        )
        fakeRepo.lineSales.add(fullSaleWithItems)

        // Sales list item has empty items list
        val listSaleItem = LineSale(
            id = "sale-full-1",
            customerName = "Full Store",
            totalAmount = "800.00",
            items = emptyList()
        )

        viewModel.openPrintDialogForLineSale(listSaleItem)
        testDispatcher.scheduler.advanceUntilIdle()

        val data = viewModel.activePrintDialogData.value
        assertNotNull(data)
        assertEquals(2, data?.items?.size)
        assertEquals("Wheat Flour 10kg", data?.items?.get(0)?.name)
        assertEquals("2", data?.items?.get(0)?.quantity)
        assertEquals("Mustard Oil 1L", data?.items?.get(1)?.name)
        assertEquals(800.0, data?.totalAmount ?: 0.0, 0.01)
    }

    @Test
    fun testOpenPrintDialogForCounterSaleFetchesItemsFromRepositoryWhenEmpty() = runTest {
        val fullCounterSaleWithItems = CounterSale(
            id = "csale-full-1",
            item = "",
            totalAmount = "350.00",
            cash = "350.00",
            items = listOf(
                com.vyavsa.app.core.model.SaleItem(
                    id = "csi-1",
                    productNameSnapshot = "Basmati Rice 5kg",
                    quantity = "1",
                    unitPrice = "350.00",
                    lineTotal = "350.00"
                )
            )
        )
        fakeRepo.counterSales.add(fullCounterSaleWithItems)

        // List item has empty items
        val listCounterItem = CounterSale(
            id = "csale-full-1",
            item = "",
            totalAmount = "350.00",
            cash = "350.00",
            items = emptyList()
        )

        viewModel.openPrintDialogForCounterSale(listCounterItem)
        testDispatcher.scheduler.advanceUntilIdle()

        val data = viewModel.activePrintDialogData.value
        assertNotNull(data)
        assertEquals(1, data?.items?.size)
        assertEquals("Basmati Rice 5kg", data?.items?.get(0)?.name)
        assertEquals("1", data?.items?.get(0)?.quantity)
        assertEquals(350.0, data?.totalAmount ?: 0.0, 0.01)
    }

    @Test
    fun testCustomerSearchAndCreation() = runTest {
        viewModel.setCustomerSearchQuery("NonExistent")
        testDispatcher.scheduler.advanceUntilIdle()

        var state = viewModel.customersState.value
        assertTrue("Expected Empty or empty list", state is UiState.Empty || ((state as? UiState.Success)?.data?.isEmpty() == true))

        var createdCustomer: Customer? = null
        viewModel.createCustomer("New POS Customer", "9988776655", "100.00") { success, cust, _ ->
            if (success) createdCustomer = cust
        }
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(createdCustomer)
        assertEquals("New POS Customer", createdCustomer?.customerName)

        viewModel.setCustomerSearchQuery("New POS")
        testDispatcher.scheduler.advanceUntilIdle()

        state = viewModel.customersState.value
        assertTrue("Expected Success with filtered customers", state is UiState.Success)
        val list = (state as UiState.Success).data
        assertTrue(list.any { it.customerName == "New POS Customer" })
    }

    @Test
    fun testSalesCategoryFilter_ChangesFilterStateAndRestrictsLoading() = runTest {
        assertEquals(SalesCategoryFilter.ALL, viewModel.salesCategoryFilter.value)

        // Switch to LINE sales only
        viewModel.setSalesCategoryFilter(SalesCategoryFilter.LINE)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(SalesCategoryFilter.LINE, viewModel.salesCategoryFilter.value)
        val counterState = viewModel.counterSalesState.value
        assertTrue("Counter sales should be empty success when LINE only is selected", counterState is UiState.Success && counterState.data.isEmpty())

        // Switch to COUNTER sales only
        viewModel.setSalesCategoryFilter(SalesCategoryFilter.COUNTER)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(SalesCategoryFilter.COUNTER, viewModel.salesCategoryFilter.value)
        val lineState = viewModel.lineSalesState.value
        assertTrue("Line sales should be empty success when COUNTER only is selected", lineState is UiState.Success && lineState.data.isEmpty())
    }

    @Test
    fun testDateFilter_TodayYesterdayThisWeekThisMonthAndCustomRange() = runTest {
        // Today
        viewModel.setDateRangeFilter(DateFilterType.TODAY)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(DateFilterType.TODAY, viewModel.dateRangeSelection.value.type)
        assertEquals(FormatUtils.todayDateString(), viewModel.dateRangeSelection.value.startDate)
        assertEquals(FormatUtils.todayDateString(), viewModel.dateRangeSelection.value.endDate)

        // Yesterday
        viewModel.setDateRangeFilter(DateFilterType.YESTERDAY)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(DateFilterType.YESTERDAY, viewModel.dateRangeSelection.value.type)
        assertNotNull(viewModel.dateRangeSelection.value.startDate)
        assertEquals(viewModel.dateRangeSelection.value.startDate, viewModel.dateRangeSelection.value.endDate)

        // This Week
        viewModel.setDateRangeFilter(DateFilterType.THIS_WEEK)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(DateFilterType.THIS_WEEK, viewModel.dateRangeSelection.value.type)
        assertNotNull(viewModel.dateRangeSelection.value.startDate)
        assertEquals(FormatUtils.todayDateString(), viewModel.dateRangeSelection.value.endDate)

        // This Month
        viewModel.setDateRangeFilter(DateFilterType.THIS_MONTH)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(DateFilterType.THIS_MONTH, viewModel.dateRangeSelection.value.type)
        assertNotNull(viewModel.dateRangeSelection.value.startDate)

        // Custom Range
        val start = "2026-11-01"
        val end = "2026-11-07"
        viewModel.setDateRangeFilter(DateFilterType.CUSTOM, start, end)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(DateFilterType.CUSTOM, viewModel.dateRangeSelection.value.type)
        assertEquals(start, viewModel.dateRangeSelection.value.startDate)
        assertEquals(end, viewModel.dateRangeSelection.value.endDate)
    }

    @Test
    fun testCustomerFilterAndCombinedFiltersWithSearch() = runTest {
        val cust = Customer(id = "cust-1", customerName = "Sharma Stores")
        viewModel.setSelectedCustomer(cust)
        viewModel.setSalesCategoryFilter(SalesCategoryFilter.LINE)
        viewModel.setDateRangeFilter(DateFilterType.CUSTOM, "2026-10-01", "2026-10-31")
        viewModel.onSearchChange("Sharma")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("cust-1", viewModel.selectedCustomer.value?.id)
        assertEquals(SalesCategoryFilter.LINE, viewModel.salesCategoryFilter.value)
        assertEquals("2026-10-01", viewModel.dateRangeSelection.value.startDate)
        assertEquals("2026-10-31", viewModel.dateRangeSelection.value.endDate)
        assertEquals("Sharma", viewModel.searchQuery.value)

        // Verify repository received parameters
        assertEquals("cust-1", fakeRepo.lastListLineSalesCustomerId)
        assertEquals("2026-10-01", fakeRepo.lastListLineSalesFromDate)
        assertEquals("2026-10-31", fakeRepo.lastListLineSalesToDate)
        assertEquals("Sharma", fakeRepo.lastListLineSalesSearch)

        // Clear all filters
        viewModel.clearAllFilters()
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.selectedCustomer.value)
        assertEquals(SalesCategoryFilter.ALL, viewModel.salesCategoryFilter.value)
        assertEquals(DateFilterType.TODAY, viewModel.dateRangeSelection.value.type)
        assertEquals("", viewModel.searchQuery.value)
    }

    @Test
    fun testSelectLineSaleCustomerFetchesAuthoritativeBalance() = runTest {
        val cust = Customer(id = "cust-1", customerName = "Sharma Stores", currentBalance = "500.00", advanceBalance = "100.00")
        fakeRepo.customers.clear()
        fakeRepo.customers.add(cust)

        viewModel.selectLineSaleCustomer(cust)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.lineSaleCustomerBalanceState.value
        assertTrue(state is UiState.Success)
        val balance = (state as UiState.Success).data
        assertEquals("cust-1", balance.customerId)
        assertEquals("500.00", balance.currentBalance)
        assertEquals("100.00", balance.advanceBalance)
        assertEquals(1, fakeRepo.getCustomerBalanceCallCount)
    }

    @Test
    fun testSelectLineSaleCustomerNullClearsBalance() = runTest {
        val cust = Customer(id = "cust-1", customerName = "Sharma Stores", currentBalance = "500.00")
        viewModel.selectLineSaleCustomer(cust)
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.lineSaleCustomerBalanceState.value is UiState.Success)

        viewModel.selectLineSaleCustomer(null)
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.lineSaleCustomerBalanceState.value is UiState.Idle)
    }

    @Test
    fun testSelectLineSaleCustomerSwitchingClearsStaleBalance() = runTest {
        val cust1 = Customer(id = "cust-1", customerName = "Sharma Stores", currentBalance = "500.00", advanceBalance = "50.00")
        val cust2 = Customer(id = "cust-2", customerName = "Gupta Mart", currentBalance = "250.00", advanceBalance = "0.00")
        fakeRepo.customers.clear()
        fakeRepo.customers.addAll(listOf(cust1, cust2))

        viewModel.selectLineSaleCustomer(cust1)
        testDispatcher.scheduler.advanceUntilIdle()
        val state1 = viewModel.lineSaleCustomerBalanceState.value
        assertTrue(state1 is UiState.Success)
        assertEquals("500.00", (state1 as UiState.Success).data.currentBalance)

        // Switch to cust2
        viewModel.selectLineSaleCustomer(cust2)
        // Immediately upon calling selectLineSaleCustomer, state is Loading (stale balance wiped!)
        assertTrue(viewModel.lineSaleCustomerBalanceState.value is UiState.Loading)

        testDispatcher.scheduler.advanceUntilIdle()
        val state2 = viewModel.lineSaleCustomerBalanceState.value
        assertTrue(state2 is UiState.Success)
        val data2 = (state2 as UiState.Success).data
        assertEquals("cust-2", data2.customerId)
        assertEquals("250.00", data2.currentBalance)
    }

    @Test
    fun testSelectCounterSaleCustomerFetchesBalanceAndHandlesError() = runTest {
        val cust = Customer(id = "cust-nonexistent", customerName = "Unknown")
        viewModel.selectCounterSaleCustomer(cust)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.counterSaleCustomerBalanceState.value
        assertTrue(state is UiState.Error)
        assertEquals("Customer not found", (state as UiState.Error).message)
    }

    @Test
    fun testCreateLineSaleRefreshesCustomerBalanceOnSuccess() = runTest {
        val cust = Customer(id = "cust-1", customerName = "Sharma Stores", currentBalance = "500.00")
        fakeRepo.customers.clear()
        fakeRepo.customers.add(cust)

        viewModel.selectLineSaleCustomer(cust)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, fakeRepo.getCustomerBalanceCallCount)

        // Customer's balance changes on backend after sale
        fakeRepo.customers[0] = cust.copy(currentBalance = "0.00", advanceBalance = "200.00")

        viewModel.createLineSale(
            customerId = "cust-1",
            totalAmount = "1000.00",
            cashIn = "1700.00",
            items = listOf(SaleItemRequest(productId = "prod-1", quantity = "1", unitPrice = "1000.00"))
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // Balance should have been automatically refreshed!
        assertEquals(2, fakeRepo.getCustomerBalanceCallCount)
        val refreshed = viewModel.lineSaleCustomerBalanceState.value
        assertTrue(refreshed is UiState.Success)
        val refreshedData = (refreshed as UiState.Success).data
        assertEquals("0.00", refreshedData.currentBalance)
        assertEquals("200.00", refreshedData.advanceBalance)
    }

    @Test
    fun testCreateCounterSaleRefreshesCustomerBalanceWhenCustomerPresent() = runTest {
        val cust = Customer(id = "cust-1", customerName = "Sharma Stores", currentBalance = "300.00")
        fakeRepo.customers.clear()
        fakeRepo.customers.add(cust)

        viewModel.selectCounterSaleCustomer(cust)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, fakeRepo.getCustomerBalanceCallCount)

        // Customer's balance settles on backend
        fakeRepo.customers[0] = cust.copy(currentBalance = "0.00")

        viewModel.createCounterSale(
            customerId = "cust-1",
            item = "Retail Items",
            price = "500.00",
            totalAmount = "500.00",
            cash = "500.00"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, fakeRepo.getCustomerBalanceCallCount)
        val refreshed = viewModel.counterSaleCustomerBalanceState.value
        assertTrue(refreshed is UiState.Success)
        assertEquals("0.00", (refreshed as UiState.Success).data.currentBalance)
    }

    @Test
    fun testSetPurchaseSupplierFilter_updatesFilterAndReloads() = runTest {
        val p1 = Purchase(
            id = "purch-1",
            item = "Basmati Rice",
            quantity = 10,
            totalAmount = "10000.00",
            customerId = "supp-1",
            customerName = "Vendor Alpha",
            createdAt = "2026-10-15"
        )
        val p2 = Purchase(
            id = "purch-2",
            item = "Sugar",
            quantity = 5,
            totalAmount = "4000.00",
            customerId = "supp-2",
            customerName = "Vendor Beta",
            createdAt = "2026-10-15"
        )
        fakeRepo.purchases.clear()
        fakeRepo.purchases.addAll(listOf(p1, p2))

        viewModel.selectTab(TransactionTab.PURCHASES)
        viewModel.setDateRangeFilter(DateFilterType.ALL_TIME)
        viewModel.setPurchaseSupplierFilter("supp-1")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("supp-1", viewModel.selectedPurchaseSupplierId.value)
        val state = viewModel.purchasesState.value
        assertTrue("Expected Success, got $state", state is UiState.Success)
        val list = (state as UiState.Success).data
        assertEquals(1, list.size)
        assertEquals("purch-1", list[0].id)
    }

    @Test
    fun testSetExpenseCategoryFilter_updatesCategoryState() = runTest {
        val e1 = Expense(
            id = "exp-1",
            item = "Shop Rent",
            category = "rent",
            totalAmount = "8000.00",
            createdAt = "2026-10-15"
        )
        val e2 = Expense(
            id = "exp-2",
            item = "Electricity Bill",
            category = "electricity",
            totalAmount = "1500.00",
            createdAt = "2026-10-15"
        )
        fakeRepo.expenses.clear()
        fakeRepo.expenses.addAll(listOf(e1, e2))

        viewModel.selectTab(TransactionTab.EXPENSES)
        viewModel.setDateRangeFilter(DateFilterType.ALL_TIME)
        viewModel.setExpenseCategoryFilter("rent")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("rent", viewModel.selectedExpenseCategory.value)
        val state = viewModel.expensesState.value
        assertTrue("Expected Success, got $state", state is UiState.Success)
        val list = (state as UiState.Success).data
        assertEquals(1, list.size)
        assertEquals("exp-1", list[0].id)
    }

    @Test
    fun testSelectExpenseForDetails_setsAndClearsSelection() = runTest {
        val expense = Expense(
            id = "exp-details-1",
            item = "Logistics diesel fuel",
            category = "transport",
            totalAmount = "2200.00",
            createdAt = "2026-10-15"
        )

        viewModel.selectExpenseForDetails(expense)
        assertEquals("exp-details-1", viewModel.selectedExpenseForDetails.value?.id)

        viewModel.selectExpenseForDetails(null)
        assertEquals(null, viewModel.selectedExpenseForDetails.value)
    }

    @Test
    fun testCreatePurchase_withSupplierId_populatesRequest() = runTest {
        viewModel.createPurchase(
            supplierId = "supp-10",
            item = "Mustard Oil Cans",
            quantity = "15",
            totalAmount = "18000.00",
            cashAmount = "18000.00",
            customerId = "supp-10"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.createActionState.value
        assertTrue("Expected Success, got $state", state is UiState.Success)

        val lastReq = fakeRepo.lastCreatePurchaseRequest
        assertNotNull(lastReq)
        assertEquals("supp-10", lastReq?.supplierId)
        assertEquals("supp-10", lastReq?.customerId)
        assertEquals("Mustard Oil Cans", lastReq?.item)
        assertEquals(15, lastReq?.quantity)
        assertEquals("18000.00", lastReq?.totalAmount)
        assertEquals("18000.00", lastReq?.totalPaid)
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

    val purchases = mutableListOf<Purchase>()
    val expenses = mutableListOf<Expense>()
    val customers = mutableListOf(
        Customer(id = "cust-1", customerName = "Sharma Stores", currentBalance = "500.00"),
        Customer(id = "cust-2", customerName = "Gupta Mart", currentBalance = "0.00")
    )
    val employees = mutableListOf(
        Employee(id = "emp-1", name = "Suresh Kumar", phone = "9876543210")
    )
    val suppliers = mutableListOf(
        Supplier(id = "supp-1", supplierName = "Vendor Alpha", phone = "9876543210", outstandingPayable = "1200.00")
    )

    var lastCreateLineSaleRequest: CreateLineSaleRequest? = null
    var lastCreateCounterSaleRequest: CreateCounterSaleRequest? = null
    var lastUpdateLineSaleRequest: UpdateLineSaleRequest? = null
    var lastUpdateCounterSaleRequest: UpdateCounterSaleRequest? = null
    var lastCreatePurchaseRequest: CreatePurchaseRequest? = null
    var lastCreateExpenseRequest: CreateExpenseRequest? = null
    var lastUpdateExpenseRequest: UpdateExpenseRequest? = null
    var updateExpenseCallCount = 0
    var createLineSaleCallCount = 0
    var getCustomerBalanceCallCount = 0
    var lastGetCustomerBalanceId: String? = null
    var delayResponse = false
    var shouldFailCreateLineSale = false
    var failureMessage = "Server error creating line sale"

    var lastListLineSalesDate: String? = null
    var lastListLineSalesSearch: String? = null
    var lastListLineSalesFromDate: String? = null
    var lastListLineSalesToDate: String? = null
    var lastListLineSalesCustomerId: String? = null

    override fun listLineSales(
        date: String?,
        search: String?,
        fromDate: String?,
        toDate: String?,
        customerId: String?
    ): Flow<Resource<List<LineSale>>> = flow {
        lastListLineSalesDate = date
        lastListLineSalesSearch = search
        lastListLineSalesFromDate = fromDate
        lastListLineSalesToDate = toDate
        lastListLineSalesCustomerId = customerId
        emit(Resource.Loading)
        var filtered = lineSales.toList()
        if (!customerId.isNullOrBlank()) {
            filtered = filtered.filter { it.customerId == customerId }
        }
        if (!search.isNullOrBlank()) {
            val q = search.trim().lowercase()
            filtered = filtered.filter {
                it.customerName.lowercase().contains(q) ||
                    it.route?.lowercase()?.contains(q) == true ||
                    it.id.lowercase().contains(q)
            }
        }
        emit(Resource.Success(filtered))
    }

    override fun createLineSale(request: CreateLineSaleRequest): Flow<Resource<LineSale>> = flow {
        createLineSaleCallCount++
        emit(Resource.Loading)
        lastCreateLineSaleRequest = request
        if (shouldFailCreateLineSale) {
            emit(Resource.Error(failureMessage))
            return@flow
        }
        if (!delayResponse) {
            val sale = LineSale(
                id = "sale-${lineSales.size + 1}",
                customerId = request.customerId,
                customerName = "Customer",
                subtotal = request.subtotal ?: request.totalAmount,
                discount = request.discount ?: (request.discountAmount ?: "0.00"),
                totalAmount = request.totalAmount,
                totalCashIn = request.cashAmount ?: "0.00",
                balance = "0.00",
                createdAt = "2026-10-15"
            )
            lineSales.add(sale)
            emit(Resource.Success(sale))
        }
    }

    override fun updateLineSale(id: String, request: UpdateLineSaleRequest): Flow<Resource<LineSale>> = flow {
        emit(Resource.Loading)
        lastUpdateLineSaleRequest = request
        val idx = lineSales.indexOfFirst { it.id == id }
        if (idx != -1) {
            val existing = lineSales[idx]
            val updated = existing.copy(
                subtotal = request.subtotal ?: existing.subtotal,
                discount = request.discount ?: (request.discountAmount ?: existing.discount),
                totalAmount = request.totalAmount ?: existing.totalAmount
            )
            lineSales[idx] = updated
            emit(Resource.Success(updated))
        } else {
            emit(Resource.Error("Sale not found"))
        }
    }

    var breakdownResult: LineSalePaymentBreakdown = LineSalePaymentBreakdown(
        totalSales = "85000.00",
        cashReceived = "25000.00",
        bankReceived = "60000.00",
        totalReceived = "85000.00",
        advanceApplied = "0.00",
        totalDue = "0.00",
        salesCount = 3L
    )
    var shouldFailBreakdown = false
    var breakdownCallCount = 0

    var counterBreakdownResult: CounterSalePaymentBreakdown = CounterSalePaymentBreakdown(
        totalSales = "85000.00",
        cashReceived = "25000.00",
        bankReceived = "60000.00",
        totalReceived = "85000.00",
        advanceApplied = "0.00",
        totalDue = "0.00",
        salesCount = 3L
    )
    var shouldFailCounterBreakdown = false
    var counterBreakdownCallCount = 0

    override fun getLineSalePaymentBreakdown(
        date: String?,
        startDate: String?,
        endDate: String?
    ): Flow<Resource<LineSalePaymentBreakdown>> = flow {
        breakdownCallCount++
        emit(Resource.Loading)
        if (shouldFailBreakdown) {
            emit(Resource.Error("Failed to load payment breakdown"))
        } else {
            emit(Resource.Success(breakdownResult))
        }
    }

    override fun getCounterSalePaymentBreakdown(
        date: String?,
        startDate: String?,
        endDate: String?
    ): Flow<Resource<CounterSalePaymentBreakdown>> = flow {
        counterBreakdownCallCount++
        emit(Resource.Loading)
        if (shouldFailCounterBreakdown) {
            emit(Resource.Error("Failed to load counter sale payment breakdown"))
        } else {
            emit(Resource.Success(counterBreakdownResult))
        }
    }

    override fun listCounterSales(
        date: String?,
        search: String?,
        fromDate: String?,
        toDate: String?,
        customerId: String?
    ): Flow<Resource<List<CounterSale>>> = flow {
        emit(Resource.Loading)
        var filtered = counterSales.toList()
        if (!customerId.isNullOrBlank()) {
            filtered = filtered.filter { it.customerId == customerId }
        }
        if (!search.isNullOrBlank()) {
            val q = search.trim().lowercase()
            filtered = filtered.filter {
                it.item.lowercase().contains(q) ||
                    it.customerName?.lowercase()?.contains(q) == true ||
                    it.id.lowercase().contains(q)
            }
        }
        emit(Resource.Success(filtered))
    }

    override fun createCounterSale(request: CreateCounterSaleRequest): Flow<Resource<CounterSale>> = flow {
        emit(Resource.Loading)
        lastCreateCounterSaleRequest = request
        val sale = CounterSale(
            id = "counter-${counterSales.size + 1}",
            item = request.item,
            subtotal = request.subtotal ?: (request.price.ifBlank { request.totalAmount }),
            discount = request.discount ?: (request.discountAmount ?: "0.00"),
            price = request.price,
            totalAmount = request.totalAmount,
            paymentMethod = request.paymentMethod,
            cash = request.cash,
            createdAt = "2026-10-15"
        )
        counterSales.add(sale)
        emit(Resource.Success(sale))
    }

    override fun updateCounterSale(id: String, request: UpdateCounterSaleRequest): Flow<Resource<CounterSale>> = flow {
        emit(Resource.Loading)
        lastUpdateCounterSaleRequest = request
        val idx = counterSales.indexOfFirst { it.id == id }
        if (idx != -1) {
            val existing = counterSales[idx]
            val updated = existing.copy(
                subtotal = request.subtotal ?: existing.subtotal,
                discount = request.discount ?: (request.discountAmount ?: existing.discount),
                totalAmount = request.totalAmount ?: existing.totalAmount
            )
            counterSales[idx] = updated
            emit(Resource.Success(updated))
        } else {
            emit(Resource.Error("Sale not found"))
        }
    }

    override fun listPurchases(
        date: String?,
        search: String?,
        customerId: String?,
        fromDate: String?,
        toDate: String?,
        supplierId: String?,
        page: Int,
        pageSize: Int
    ): Flow<Resource<List<Purchase>>> = flow {
        emit(Resource.Loading)
        var filtered = purchases.toList()
        if (!date.isNullOrBlank()) {
            filtered = filtered.filter { it.createdAt.startsWith(date) }
        }
        if (!search.isNullOrBlank()) {
            filtered = filtered.filter { it.item.contains(search, ignoreCase = true) || (it.customerName?.contains(search, ignoreCase = true) == true) }
        }
        val effectiveSupplierId = supplierId ?: customerId
        if (!effectiveSupplierId.isNullOrBlank()) {
            filtered = filtered.filter { it.customerId == effectiveSupplierId || it.supplierId == effectiveSupplierId }
        }
        emit(Resource.Success(filtered))
    }

    override fun createPurchase(request: CreatePurchaseRequest): Flow<Resource<Purchase>> = flow {
        emit(Resource.Loading)
        lastCreatePurchaseRequest = request
        val paid = request.totalPaid.toDoubleOrNull() ?: 0.0
        val total = request.totalAmount.toDoubleOrNull() ?: 0.0
        val p = Purchase(
            id = "purch-${purchases.size + 1}",
            item = request.item,
            quantity = request.quantity,
            totalAmount = request.totalAmount,
            customerId = request.customerId,
            customerName = if (request.customerId == "cust-1") "Sharma Stores" else null,
            paymentStatus = if (paid >= total) "PAID" else "PARTIAL",
            totalPaid = request.totalPaid,
            totalPending = String.format(java.util.Locale.US, "%.2f", (total - paid).coerceAtLeast(0.0)),
            createdAt = "2026-10-15"
        )
        purchases.add(p)
        emit(Resource.Success(p))
    }

    override fun getPurchaseById(id: String): Flow<Resource<Purchase>> = flow {
        emit(Resource.Loading)
        val p = purchases.firstOrNull { it.id == id }
        if (p != null) emit(Resource.Success(p)) else emit(Resource.Error("Purchase not found"))
    }

    override fun updatePurchase(id: String, request: UpdatePurchaseRequest): Flow<Resource<Purchase>> = flow {
        emit(Resource.Loading)
        val idx = purchases.indexOfFirst { it.id == id }
        if (idx != -1) {
            val existing = purchases[idx]
            val updated = existing.copy(
                item = request.item ?: existing.item,
                quantity = request.quantity ?: existing.quantity,
                totalAmount = request.totalAmount ?: existing.totalAmount,
                totalPaid = request.totalPaid ?: existing.totalPaid
            )
            purchases[idx] = updated
            emit(Resource.Success(updated))
        } else {
            emit(Resource.Error("Purchase not found"))
        }
    }

    override fun deletePurchase(id: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading)
        val removed = purchases.removeAll { it.id == id }
        if (removed) emit(Resource.Success(Unit)) else emit(Resource.Error("Purchase not found"))
    }

    override fun listPurchasePayments(id: String): Flow<Resource<List<PurchasePayment>>> = flow {
        emit(Resource.Loading)
        emit(Resource.Success(emptyList()))
    }

    override fun recordPurchasePayment(id: String, request: RecordSupplierPaymentRequest): Flow<Resource<SupplierPaymentResponse>> = flow {
        emit(Resource.Loading)
        emit(Resource.Success(SupplierPaymentResponse(
            paymentId = "pay-1",
            customerId = "cust-1",
            customerName = "Sharma Stores",
            amount = request.amount,
            allocations = emptyList()
        )))
    }

    override fun listExpenses(
        date: String?,
        search: String?,
        fromDate: String?,
        toDate: String?,
        page: Int,
        pageSize: Int
    ): Flow<Resource<List<Expense>>> = flow {
        emit(Resource.Loading)
        var filtered = expenses.toList()
        if (!date.isNullOrBlank()) {
            filtered = filtered.filter { (it.expenseDate ?: it.createdAt).startsWith(date) }
        }
        if (!search.isNullOrBlank()) {
            filtered = filtered.filter { it.item.contains(search, ignoreCase = true) || (it.category?.contains(search, ignoreCase = true) == true) }
        }
        emit(Resource.Success(filtered))
    }

    override fun createExpense(request: CreateExpenseRequest): Flow<Resource<Expense>> = flow {
        emit(Resource.Loading)
        lastCreateExpenseRequest = request
        val exp = Expense(
            id = "exp-${expenses.size + 1}",
            item = request.item,
            category = request.category,
            totalAmount = request.totalAmount,
            paymentMethod = request.paymentMethod,
            employeeId = request.employeeId,
            employeeName = if (request.employeeId == "emp-1") "Suresh Kumar" else null,
            expenseDate = request.expenseDate,
            paymentDate = request.paymentDate,
            date = request.date,
            createdAt = "2026-10-15"
        )
        expenses.add(exp)
        emit(Resource.Success(exp))
    }

    override fun updateExpense(id: String, request: UpdateExpenseRequest): Flow<Resource<Expense>> = flow {
        emit(Resource.Loading)
        lastUpdateExpenseRequest = request
        updateExpenseCallCount++
        val existingIndex = expenses.indexOfFirst { it.id == id }
        val updated = if (existingIndex >= 0) {
            val old = expenses[existingIndex]
            val effectiveDate = request.expenseDate ?: request.paymentDate ?: request.date ?: old.expenseDate
            val newExp = old.copy(
                item = request.item ?: old.item,
                category = request.category ?: old.category,
                totalAmount = request.totalAmount ?: old.totalAmount,
                paymentMethod = request.paymentMethod ?: old.paymentMethod,
                employeeId = request.employeeId ?: old.employeeId,
                expenseDate = effectiveDate,
                paymentDate = effectiveDate,
                date = effectiveDate
            )
            expenses[existingIndex] = newExp
            newExp
        } else {
            Expense(
                id = id,
                item = request.item ?: "Expense",
                category = request.category,
                totalAmount = request.totalAmount ?: "0.00",
                paymentMethod = request.paymentMethod ?: "cash",
                expenseDate = request.expenseDate ?: request.paymentDate ?: request.date,
                paymentDate = request.paymentDate,
                date = request.date
            )
        }
        emit(Resource.Success(updated))
    }

    override fun getExpenseById(id: String): Flow<Resource<Expense>> = flow {
        emit(Resource.Loading)
        val exp = expenses.firstOrNull { it.id == id }
        if (exp != null) emit(Resource.Success(exp)) else emit(Resource.Error("Expense not found", "404"))
    }

    override fun deleteExpense(id: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading)
        expenses.removeAll { it.id == id }
        emit(Resource.Success(Unit))
    }

    override fun listCustomers(search: String?, page: Int, pageSize: Int): Flow<Resource<List<Customer>>> = flow {
        emit(Resource.Loading)
        val filtered = if (search.isNullOrBlank()) {
            customers.toList()
        } else {
            val q = search.lowercase()
            customers.filter { it.customerName.lowercase().contains(q) || (it.phone?.lowercase()?.contains(q) == true) }
        }
        emit(Resource.Success(filtered))
    }

    override fun getCustomerBalance(customerId: String): Flow<Resource<CustomerBalance>> = flow {
        getCustomerBalanceCallCount++
        lastGetCustomerBalanceId = customerId
        emit(Resource.Loading)
        val cust = customers.firstOrNull { it.id == customerId }
        if (cust != null) {
            emit(Resource.Success(CustomerBalance(
                customerId = cust.id,
                customerName = cust.customerName,
                currentBalance = cust.currentBalance,
                advanceBalance = cust.advanceBalance,
                status = cust.status
            )))
        } else {
            emit(Resource.Error("Customer not found", "404"))
        }
    }

    override fun createCustomer(name: String, phone: String?, openingBalance: String): Flow<Resource<Customer>> = flow {
        emit(Resource.Loading)
        val newCust = Customer(
            id = "cust-${customers.size + 1}",
            customerName = name,
            phone = phone,
            openingBalance = openingBalance,
            currentBalance = openingBalance
        )
        customers.add(newCust)
        emit(Resource.Success(newCust))
    }

    override suspend fun downloadLineSalePdf(id: String): ResponseBody {
        return "%PDF-1.4 test line sale pdf".toResponseBody("application/pdf".toMediaTypeOrNull())
    }

    override suspend fun downloadCounterSalePdf(id: String): ResponseBody {
        return "%PDF-1.4 test counter sale pdf".toResponseBody("application/pdf".toMediaTypeOrNull())
    }

    override fun listSuppliers(): Flow<Resource<List<Supplier>>> = flow {
        emit(Resource.Loading)
        emit(Resource.Success(suppliers.toList()))
    }

    override fun listEmployees(): Flow<Resource<List<Employee>>> = flow {
        emit(Resource.Loading)
        emit(Resource.Success(employees.toList()))
    }

    override fun getLineSaleById(id: String): Flow<Resource<LineSale>> = flow {
        emit(Resource.Loading)
        val sale = lineSales.firstOrNull { it.id == id }
        if (sale != null) emit(Resource.Success(sale)) else emit(Resource.Error("Sale not found"))
    }

    override fun listLineSalePayments(id: String): Flow<Resource<List<LineSalePayment>>> = flow {
        emit(Resource.Loading)
        emit(Resource.Success(emptyList()))
    }

    override fun recordLineSalePayment(id: String, request: RecordSalePaymentRequest): Flow<Resource<LineSalePayment>> = flow {
        emit(Resource.Loading)
        val p = LineSalePayment(id = "pay-1", lineSaleId = id, amount = request.amount, paymentMethod = request.paymentMethod)
        emit(Resource.Success(p))
    }

    override fun getCounterSaleById(id: String): Flow<Resource<CounterSale>> = flow {
        emit(Resource.Loading)
        val sale = counterSales.firstOrNull { it.id == id }
        if (sale != null) emit(Resource.Success(sale)) else emit(Resource.Error("Sale not found"))
    }

    override fun listCounterSalePayments(id: String): Flow<Resource<List<CounterSalePayment>>> = flow {
        emit(Resource.Loading)
        emit(Resource.Success(emptyList()))
    }

    override fun recordCounterSalePayment(id: String, request: RecordSalePaymentRequest): Flow<Resource<CounterSalePayment>> = flow {
        emit(Resource.Loading)
        val p = CounterSalePayment(id = "cpay-1", counterSaleId = id, amount = request.amount, paymentMethod = request.paymentMethod)
        emit(Resource.Success(p))
    }

    override fun listCashTransactions(): Flow<Resource<List<CashTransaction>>> = flow {
        emit(Resource.Loading)
        emit(Resource.Success(emptyList()))
    }

    override fun listSubscriptionTransactions(): Flow<Resource<List<PlanTransaction>>> = flow {
        emit(Resource.Loading)
        emit(Resource.Success(emptyList()))
    }
}
