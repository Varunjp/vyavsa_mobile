package com.vyavsa.app.feature.transactions

import com.vyavsa.app.core.common.UiState
import com.vyavsa.app.core.model.LineSaleBankBreakdown
import com.vyavsa.app.core.model.LineSalePaymentBreakdown
import com.vyavsa.app.core.sync.DataMutation
import com.vyavsa.app.core.sync.DataRefreshCoordinator
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
class LineSaleOverviewViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepo: FakeTransactionsRepository
    private lateinit var refreshCoordinator: DataRefreshCoordinator

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepo = FakeTransactionsRepository()
        refreshCoordinator = DataRefreshCoordinator()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testLoadBreakdownSuccessWithMultipleBanks() = runTest {
        val expected = LineSalePaymentBreakdown(
            totalSales = "85000.00",
            cashReceived = "25000.00",
            bankReceived = "60000.00",
            totalReceived = "85000.00",
            advanceApplied = "0.00",
            totalDue = "0.00",
            salesCount = 3L,
            bankBreakdown = listOf(
                LineSaleBankBreakdown(bankId = "b1", bankName = "SBI", amount = "30000.00", percentage = 50.0),
                LineSaleBankBreakdown(bankId = "b2", bankName = "HDFC", amount = "20000.00", percentage = 33.33),
                LineSaleBankBreakdown(bankId = "b3", bankName = "ICICI", amount = "10000.00", percentage = 16.67)
            )
        )
        fakeRepo.breakdownResult = expected

        val viewModel = LineSaleOverviewViewModel(fakeRepo, refreshCoordinator)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue(state is UiState.Success)
        val data = (state as UiState.Success).data
        assertEquals("85000.00", data.totalSales)
        assertEquals("25000.00", data.cashReceived)
        assertEquals("60000.00", data.bankReceived)
        assertEquals(3, data.bankBreakdown.size)
        assertEquals("SBI", data.bankBreakdown[0].bankName)
        assertEquals(50.0, data.bankBreakdown[0].percentage, 0.01)
    }

    @Test
    fun testLoadBreakdownCashOnly() = runTest {
        val expected = LineSalePaymentBreakdown(
            totalSales = "25000.00",
            cashReceived = "25000.00",
            bankReceived = "0.00",
            totalReceived = "25000.00",
            totalDue = "0.00",
            salesCount = 1L,
            bankBreakdown = emptyList()
        )
        fakeRepo.breakdownResult = expected

        val viewModel = LineSaleOverviewViewModel(fakeRepo, refreshCoordinator)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue(state is UiState.Success)
        val data = (state as UiState.Success).data
        assertEquals("25000.00", data.totalSales)
        assertEquals("25000.00", data.cashReceived)
        assertEquals("0.00", data.bankReceived)
        assertTrue(data.bankBreakdown.isEmpty())
    }

    @Test
    fun testLoadBreakdownBankOnly() = runTest {
        val expected = LineSalePaymentBreakdown(
            totalSales = "30000.00",
            cashReceived = "0.00",
            bankReceived = "30000.00",
            totalReceived = "30000.00",
            totalDue = "0.00",
            salesCount = 1L,
            bankBreakdown = listOf(
                LineSaleBankBreakdown(bankId = "b-axis", bankName = "Axis Bank", amount = "30000.00", percentage = 100.0)
            )
        )
        fakeRepo.breakdownResult = expected

        val viewModel = LineSaleOverviewViewModel(fakeRepo, refreshCoordinator)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue(state is UiState.Success)
        val data = (state as UiState.Success).data
        assertEquals("30000.00", data.totalSales)
        assertEquals("0.00", data.cashReceived)
        assertEquals("30000.00", data.bankReceived)
        assertEquals(1, data.bankBreakdown.size)
        assertEquals("Axis Bank", data.bankBreakdown[0].bankName)
    }

    @Test
    fun testLoadBreakdownMixedWithOutstandingDues() = runTest {
        val expected = LineSalePaymentBreakdown(
            totalSales = "100000.00",
            cashReceived = "40000.00",
            bankReceived = "35000.00",
            totalReceived = "75000.00",
            advanceApplied = "5000.00",
            totalDue = "20000.00",
            salesCount = 4L,
            bankBreakdown = listOf(
                LineSaleBankBreakdown(bankId = "b1", bankName = "Canara Bank", amount = "35000.00", percentage = 35.0)
            )
        )
        fakeRepo.breakdownResult = expected

        val viewModel = LineSaleOverviewViewModel(fakeRepo, refreshCoordinator)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue(state is UiState.Success)
        val data = (state as UiState.Success).data
        assertEquals("100000.00", data.totalSales)
        assertEquals("20000.00", data.totalDue)
        assertEquals("5000.00", data.advanceApplied)
        assertEquals(4L, data.salesCount)
    }

    @Test
    fun testLoadBreakdownEmptySales() = runTest {
        val expected = LineSalePaymentBreakdown(
            totalSales = "0.00",
            cashReceived = "0.00",
            bankReceived = "0.00",
            totalReceived = "0.00",
            totalDue = "0.00",
            salesCount = 0L,
            bankBreakdown = emptyList()
        )
        fakeRepo.breakdownResult = expected

        val viewModel = LineSaleOverviewViewModel(fakeRepo, refreshCoordinator)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue(state is UiState.Success)
        val data = (state as UiState.Success).data
        assertEquals("0.00", data.totalSales)
        assertEquals(0L, data.salesCount)
        assertTrue(data.bankBreakdown.isEmpty())
    }

    @Test
    fun testLoadBreakdownApiError() = runTest {
        fakeRepo.shouldFailBreakdown = true

        val viewModel = LineSaleOverviewViewModel(fakeRepo, refreshCoordinator)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue(state is UiState.Error)
        assertEquals("Failed to load payment breakdown", (state as UiState.Error).message)
    }

    @Test
    fun testRetryAfterErrorSucceeds() = runTest {
        fakeRepo.shouldFailBreakdown = true
        val viewModel = LineSaleOverviewViewModel(fakeRepo, refreshCoordinator)
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.state.value is UiState.Error)

        // Reset failure and retry
        fakeRepo.shouldFailBreakdown = false
        viewModel.refresh()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue(state is UiState.Success)
        assertEquals("85000.00", (state as UiState.Success).data.totalSales)
    }

    @Test
    fun testAutoRefreshOnSalesMutation() = runTest {
        val initial = LineSalePaymentBreakdown(totalSales = "50000.00", salesCount = 1L)
        fakeRepo.breakdownResult = initial
        val viewModel = LineSaleOverviewViewModel(fakeRepo, refreshCoordinator)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, fakeRepo.breakdownCallCount)

        // Update repository data and broadcast SALES mutation
        val updated = LineSalePaymentBreakdown(totalSales = "75000.00", salesCount = 2L)
        fakeRepo.breakdownResult = updated
        refreshCoordinator.notifyMutation(DataMutation.SALES)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, fakeRepo.breakdownCallCount)
        val state = viewModel.state.value
        assertTrue(state is UiState.Success)
        assertEquals("75000.00", (state as UiState.Success).data.totalSales)
    }

    @Test
    fun testUnrelatedMutationDoesNotTriggerReload() = runTest {
        val viewModel = LineSaleOverviewViewModel(fakeRepo, refreshCoordinator)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, fakeRepo.breakdownCallCount)
        assertTrue(viewModel.state.value is UiState.Success)

        // Non-sales mutation (e.g. STAFF) should NOT trigger line sale breakdown refresh
        refreshCoordinator.notifyMutation(DataMutation.STAFF)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, fakeRepo.breakdownCallCount)
        assertTrue(viewModel.state.value is UiState.Success)
    }

    @Test
    fun testLoadBreakdownWithPayableAllocation() = runTest {
        val expected = LineSalePaymentBreakdown(
            totalSales = "40000.00",
            cashReceived = "0.00",
            bankReceived = "0.00",
            totalReceived = "0.00",
            advanceApplied = "0.00",
            payableApplied = "40000.00",
            contraAmount = "40000.00",
            totalDue = "0.00",
            salesCount = 1L,
            isReconciled = true,
            bankBreakdown = emptyList()
        )
        fakeRepo.breakdownResult = expected

        val viewModel = LineSaleOverviewViewModel(fakeRepo, refreshCoordinator)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue(state is UiState.Success)
        val data = (state as UiState.Success).data
        assertEquals("40000.00", data.totalSales)
        assertEquals("40000.00", data.payableApplied)
        assertEquals("40000.00", data.effectivePayableApplied)
        assertEquals("0.00", data.totalDue)
        assertTrue(data.isReconciled)
    }

    @Test
    fun testLoadBreakdownWithMixedAdvanceAndPayableAllocation() = runTest {
        val expected = LineSalePaymentBreakdown(
            totalSales = "60000.00",
            cashReceived = "10000.00",
            bankReceived = "10000.00",
            totalReceived = "20000.00",
            advanceApplied = "20000.00",
            payableApplied = "20000.00",
            totalDue = "0.00",
            salesCount = 2L,
            isReconciled = true,
            bankBreakdown = emptyList()
        )
        fakeRepo.breakdownResult = expected

        val viewModel = LineSaleOverviewViewModel(fakeRepo, refreshCoordinator)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue(state is UiState.Success)
        val data = (state as UiState.Success).data
        assertEquals("60000.00", data.totalSales)
        assertEquals("20000.00", data.advanceApplied)
        assertEquals("20000.00", data.effectivePayableApplied)
        assertEquals("20000.00", data.totalReceived)
        assertEquals("0.00", data.totalDue)
        assertTrue(data.isReconciled)
    }
}
