package com.vyavsa.app.feature.transactions

import com.vyavsa.app.core.common.Resource
import com.vyavsa.app.core.common.SaleCalculationUtils
import com.vyavsa.app.core.common.UiState
import com.vyavsa.app.core.model.BankPaymentSplit
import com.vyavsa.app.core.model.CreateExpenseRequest
import com.vyavsa.app.core.model.Expense
import com.vyavsa.app.core.model.UpdateExpenseRequest
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
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class ExpensePaymentAllocationTest {

    private val testDispatcher = StandardTestDispatcher()
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = false
    }

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
        viewModel = TransactionsViewModel(
            repository = fakeRepo,
            bankRepository = fakeBankRepo,
            refreshCoordinator = refreshCoordinator
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // =========================================================================
    // 1. Business Logic & Allocation Validation Tests
    // =========================================================================

    @Test
    fun testScenarioA_cashOnlyAllocation_succeeds() = runTest {
        viewModel.createExpense(
            item = "Office Electricity",
            category = "operations",
            amount = "1000.00",
            paymentMethod = "cash",
            cashAmount = "1000.00"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.createActionState.value is UiState.Success)
        val req = fakeRepo.lastCreateExpenseRequest
        assertNotNull(req)
        assertEquals("1000.00", req?.totalAmount)
        assertEquals("cash", req?.paymentMethod)
        assertEquals("1000.00", req?.cashAmount)
        assertTrue(req?.bankPayments.isNullOrEmpty())
        assertNull(req?.bankId)
    }

    @Test
    fun testScenarioB_singleBankAllocation_succeeds() = runTest {
        viewModel.createExpense(
            item = "Shop Rent",
            category = "rent",
            amount = "1000.00",
            paymentMethod = "bank",
            bankPayments = listOf(
                BankPaymentSplit(bankId = "bank-hdfc", bankName = "HDFC Bank", amount = "1000.00")
            )
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.createActionState.value is UiState.Success)
        val req = fakeRepo.lastCreateExpenseRequest
        assertNotNull(req)
        assertEquals("1000.00", req?.totalAmount)
        assertEquals("bank", req?.paymentMethod)
        assertNull(req?.cashAmount)
        assertEquals(1, req?.bankPayments?.size)
        assertEquals("bank-hdfc", req?.bankPayments?.first()?.bankId)
        assertEquals("bank-hdfc", req?.bankPayments?.first()?.bankAccountId)
        assertEquals("1000.00", req?.bankPayments?.first()?.amount)
        assertEquals("bank-hdfc", req?.bankId)
    }

    @Test
    fun testScenarioC_multipleBanksAllocation_succeeds() = runTest {
        viewModel.createExpense(
            item = "Annual Software License",
            category = "operations",
            amount = "1000.00",
            paymentMethod = "bank",
            bankPayments = listOf(
                BankPaymentSplit(bankId = "bank-hdfc", bankName = "HDFC Bank", amount = "600.00"),
                BankPaymentSplit(bankId = "bank-sbi", bankName = "SBI Bank", amount = "400.00")
            )
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.createActionState.value is UiState.Success)
        val req = fakeRepo.lastCreateExpenseRequest
        assertNotNull(req)
        assertEquals("1000.00", req?.totalAmount)
        assertEquals("bank", req?.paymentMethod)
        assertNull(req?.cashAmount)
        assertEquals(2, req?.bankPayments?.size)
        assertEquals("600.00", req?.bankPayments?.get(0)?.amount)
        assertEquals("400.00", req?.bankPayments?.get(1)?.amount)
    }

    @Test
    fun testScenarioD_cashAndMultipleBanksAllocation_succeeds() = runTest {
        viewModel.createExpense(
            item = "Quarterly Server Infra",
            category = "operations",
            amount = "2000.00",
            paymentMethod = "cash_bank",
            cashAmount = "500.00",
            bankPayments = listOf(
                BankPaymentSplit(bankId = "bank-hdfc", bankName = "HDFC Bank", amount = "1000.00"),
                BankPaymentSplit(bankId = "bank-sbi", bankName = "SBI Bank", amount = "500.00")
            )
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.createActionState.value is UiState.Success)
        val req = fakeRepo.lastCreateExpenseRequest
        assertNotNull(req)
        assertEquals("2000.00", req?.totalAmount)
        assertEquals("cash_bank", req?.paymentMethod)
        assertEquals("500.00", req?.cashAmount)
        assertEquals(2, req?.bankPayments?.size)
        assertEquals("1000.00", req?.bankPayments?.get(0)?.amount)
        assertEquals("500.00", req?.bankPayments?.get(1)?.amount)
    }

    @Test
    fun testAllocationUnderallocated_rejected() = runTest {
        // Total 1000, allocated only 800
        viewModel.createExpense(
            item = "Supplies",
            category = "office",
            amount = "1000.00",
            paymentMethod = "bank",
            bankPayments = listOf(
                BankPaymentSplit(bankId = "bank-hdfc", amount = "800.00")
            )
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.createActionState.value is UiState.Error)
        val err = (viewModel.createActionState.value as UiState.Error).message
        assertEquals("Total allocated amount must equal the expense amount", err)
        assertNull(fakeRepo.lastCreateExpenseRequest)
    }

    @Test
    fun testAllocationOverallocated_rejected() = runTest {
        // Total 1000, allocated 1200
        viewModel.createExpense(
            item = "Supplies",
            category = "office",
            amount = "1000.00",
            paymentMethod = "cash_bank",
            cashAmount = "500.00",
            bankPayments = listOf(
                BankPaymentSplit(bankId = "bank-hdfc", amount = "700.00")
            )
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.createActionState.value is UiState.Error)
        val err = (viewModel.createActionState.value as UiState.Error).message
        assertEquals("Total allocated amount cannot exceed expense amount", err)
        assertNull(fakeRepo.lastCreateExpenseRequest)
    }

    @Test
    fun testDuplicateBankAllocation_rejected() = runTest {
        // Same bank selected twice
        viewModel.createExpense(
            item = "Transport",
            category = "transport",
            amount = "1000.00",
            paymentMethod = "bank",
            bankPayments = listOf(
                BankPaymentSplit(bankId = "bank-hdfc", amount = "600.00"),
                BankPaymentSplit(bankId = "bank-hdfc", amount = "400.00")
            )
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.createActionState.value is UiState.Error)
        val err = (viewModel.createActionState.value as UiState.Error).message
        assertTrue(err.contains("Duplicate bank account selected"))
        assertNull(fakeRepo.lastCreateExpenseRequest)
    }

    @Test
    fun testZeroAndNegativeAllocation_rejected() = runTest {
        // Bank row with 0.00
        viewModel.createExpense(
            item = "Tea & Snacks",
            category = "operations",
            amount = "500.00",
            paymentMethod = "bank",
            bankPayments = listOf(
                BankPaymentSplit(bankId = "bank-hdfc", amount = "0.00")
            )
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.createActionState.value is UiState.Error)
        val err = (viewModel.createActionState.value as UiState.Error).message
        assertTrue(err.contains("greater than zero"))

        // Cash + Bank with negative/zero cash
        viewModel.createExpense(
            item = "Tea & Snacks",
            category = "operations",
            amount = "500.00",
            paymentMethod = "cash_bank",
            cashAmount = "0.00",
            bankPayments = listOf(
                BankPaymentSplit(bankId = "bank-hdfc", amount = "500.00")
            )
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.createActionState.value is UiState.Error)
    }

    @Test
    fun testDecimalPrecisionHandling() = runTest {
        // Total ₹99.99 split into Cash ₹33.33 + Bank 1 ₹33.33 + Bank 2 ₹33.33
        viewModel.createExpense(
            item = "Hardware Parts",
            category = "operations",
            amount = "99.99",
            paymentMethod = "cash_bank",
            cashAmount = "33.33",
            bankPayments = listOf(
                BankPaymentSplit(bankId = "bank-hdfc", amount = "33.33"),
                BankPaymentSplit(bankId = "bank-sbi", amount = "33.33")
            )
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.createActionState.value is UiState.Success)
        val req = fakeRepo.lastCreateExpenseRequest
        assertNotNull(req)
        assertEquals("99.99", req?.totalAmount)
        assertEquals("33.33", req?.cashAmount)
        assertEquals(2, req?.bankPayments?.size)
        assertEquals("33.33", req?.bankPayments?.get(0)?.amount)
        assertEquals("33.33", req?.bankPayments?.get(1)?.amount)
    }

    @Test
    fun testLegacySingleBankFallback_succeeds() = runTest {
        // Calling with legacy bankId and empty bankPayments
        viewModel.createExpense(
            item = "Legacy Expense",
            category = "operations",
            amount = "450.00",
            paymentMethod = "bank",
            bankId = "bank-icici",
            bankPayments = emptyList()
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.createActionState.value is UiState.Success)
        val req = fakeRepo.lastCreateExpenseRequest
        assertNotNull(req)
        assertEquals("bank", req?.paymentMethod)
        assertEquals(1, req?.bankPayments?.size)
        assertEquals("bank-icici", req?.bankPayments?.first()?.bankId)
        assertEquals("bank-icici", req?.bankPayments?.first()?.bankAccountId)
        assertEquals("450.00", req?.bankPayments?.first()?.amount)
    }

    // =========================================================================
    // 2. API Request Serialization Contract Tests
    // =========================================================================

    @Test
    fun testCreateExpenseRequestSerialization_cashBankMultiBank() {
        val req = CreateExpenseRequest(
            item = "Quarterly Server Infra",
            category = "operations",
            totalAmount = "2000.00",
            amount = "2000.00",
            paymentMethod = "cash_bank",
            cashAmount = "500.00",
            expenseDate = "2026-11-11",
            bankPayments = listOf(
                BankPaymentSplit(bankId = "b-1", bankAccountId = "b-1", bankName = "HDFC", amount = "1000.00"),
                BankPaymentSplit(bankId = "b-2", bankAccountId = "b-2", bankName = "SBI", amount = "500.00")
            )
        )

        val encoded = json.encodeToString(req)
        assertTrue(encoded.contains("\"payment_method\":\"cash_bank\""))
        assertTrue(encoded.contains("\"cash_amount\":\"500.00\""))
        assertTrue(encoded.contains("\"bank_payments\""))
        assertTrue(encoded.contains("\"bank_id\":\"b-1\""))
        assertTrue(encoded.contains("\"bank_account_id\":\"b-1\""))
        assertTrue(encoded.contains("\"bank_id\":\"b-2\""))
        assertTrue(encoded.contains("\"bank_account_id\":\"b-2\""))
        assertTrue(encoded.contains("\"total_amount\":\"2000.00\""))
    }

    @Test
    fun testUpdateExpenseRequestSerialization_multiBank() {
        val req = UpdateExpenseRequest(
            item = "Quarterly Server Infra Updated",
            totalAmount = "1500.00",
            amount = "1500.00",
            paymentMethod = "bank",
            bankPayments = listOf(
                BankPaymentSplit(bankId = "b-1", bankAccountId = "b-1", amount = "1000.00"),
                BankPaymentSplit(bankId = "b-2", bankAccountId = "b-2", amount = "500.00")
            )
        )

        val encoded = json.encodeToString(req)
        assertTrue(encoded.contains("\"payment_method\":\"bank\""))
        assertTrue(encoded.contains("\"bank_payments\""))
        assertTrue(encoded.contains("\"bank_id\":\"b-1\""))
        assertTrue(encoded.contains("\"bank_id\":\"b-2\""))
    }
}
