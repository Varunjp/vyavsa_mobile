package com.vyavsa.app.feature.transactions

import androidx.lifecycle.SavedStateHandle
import com.vyavsa.app.core.common.UiState
import com.vyavsa.app.core.model.CounterSale
import com.vyavsa.app.core.model.LineSale
import com.vyavsa.app.core.sync.DataRefreshCoordinator
import com.vyavsa.app.feature.bank.FakeBankRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class SaleDetailsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepo: FakeTransactionsRepository
    private lateinit var fakeBankRepo: FakeBankRepository
    private lateinit var refreshCoordinator: DataRefreshCoordinator

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepo = FakeTransactionsRepository()
        fakeBankRepo = FakeBankRepository()
        refreshCoordinator = DataRefreshCoordinator()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testLoadLineSaleDetailsSuccess() = runTest {
        val handle = SavedStateHandle(mapOf("saleType" to "line", "saleId" to "sale-1"))
        val viewModel = SaleDetailsViewModel(fakeRepo, fakeBankRepo, refreshCoordinator, handle)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.detailState.value
        assertTrue(state is UiState.Success)
        val data = (state as UiState.Success).data
        assertTrue(data is SaleDetailData.Line)
        val lineSale = (data as SaleDetailData.Line).sale
        assertEquals("Sharma Stores", lineSale.customerName)
        assertEquals("1500.00", lineSale.totalAmount)
    }

    @Test
    fun testLoadCounterSaleDetailsSuccess() = runTest {
        val handle = SavedStateHandle(mapOf("saleType" to "counter", "saleId" to "counter-1"))
        val viewModel = SaleDetailsViewModel(fakeRepo, fakeBankRepo, refreshCoordinator, handle)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.detailState.value
        assertTrue(state is UiState.Success)
        val data = (state as UiState.Success).data
        assertTrue(data is SaleDetailData.Counter)
        val counterSale = (data as SaleDetailData.Counter).sale
        assertEquals("Daily Ration", counterSale.item)
        assertEquals("500.00", counterSale.totalAmount)
    }

    @Test
    fun testRecordLinePaymentSuccess() = runTest {
        val handle = SavedStateHandle(mapOf("saleType" to "line", "saleId" to "sale-1"))
        val viewModel = SaleDetailsViewModel(fakeRepo, fakeBankRepo, refreshCoordinator, handle)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.recordPayment(
            amount = "500.00",
            paymentMethod = "cash",
            bankId = null,
            note = "Settlement"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val actionState = viewModel.paymentActionState.value
        assertTrue("Expected success got $actionState", actionState is UiState.Success)
    }

    @Test
    fun testRecordPaymentInvalidAmountValidation() = runTest {
        val handle = SavedStateHandle(mapOf("saleType" to "line", "saleId" to "sale-1"))
        val viewModel = SaleDetailsViewModel(fakeRepo, fakeBankRepo, refreshCoordinator, handle)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.recordPayment(
            amount = "-50.00",
            paymentMethod = "cash",
            bankId = null,
            note = null
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val actionState = viewModel.paymentActionState.value
        assertTrue(actionState is UiState.Error)
        assertEquals("Payment amount must be greater than zero", (actionState as UiState.Error).message)
    }

    @Test
    fun testRecordPaymentBankRequiresBankSelection() = runTest {
        val handle = SavedStateHandle(mapOf("saleType" to "line", "saleId" to "sale-1"))
        val viewModel = SaleDetailsViewModel(fakeRepo, fakeBankRepo, refreshCoordinator, handle)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.recordPayment(
            amount = "500.00",
            paymentMethod = "bank",
            bankId = null,
            note = null
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val actionState = viewModel.paymentActionState.value
        assertTrue(actionState is UiState.Error)
        assertEquals("Please select a bank account for bank payment", (actionState as UiState.Error).message)
    }
}
