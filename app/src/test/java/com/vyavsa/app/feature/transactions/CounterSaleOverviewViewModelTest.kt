package com.vyavsa.app.feature.transactions

import com.vyavsa.app.core.common.UiState
import com.vyavsa.app.core.model.CounterSaleBankBreakdown
import com.vyavsa.app.core.model.CounterSalePaymentBreakdown
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
class CounterSaleOverviewViewModelTest {

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
        val expected = CounterSalePaymentBreakdown(
            totalSales = "85000.00",
            cashReceived = "25000.00",
            bankReceived = "60000.00",
            totalReceived = "85000.00",
            advanceApplied = "0.00",
            payableApplied = "0.00",
            totalDue = "0.00",
            salesCount = 3L,
            bankBreakdown = listOf(
                CounterSaleBankBreakdown(bankId = "b1", bankName = "SBI", amount = "30000.00", percentage = 50.0),
                CounterSaleBankBreakdown(bankId = "b2", bankName = "HDFC", amount = "20000.00", percentage = 33.33),
                CounterSaleBankBreakdown(bankId = "b3", bankName = "ICICI", amount = "10000.00", percentage = 16.67)
            )
        )
        fakeRepo.counterBreakdownResult = expected

        val viewModel = CounterSaleOverviewViewModel(fakeRepo, refreshCoordinator)
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
        val expected = CounterSalePaymentBreakdown(
            totalSales = "25000.00",
            cashReceived = "25000.00",
            bankReceived = "0.00",
            totalReceived = "25000.00",
            totalDue = "0.00",
            salesCount = 1L,
            bankBreakdown = emptyList()
        )
        fakeRepo.counterBreakdownResult = expected

        val viewModel = CounterSaleOverviewViewModel(fakeRepo, refreshCoordinator)
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
        val expected = CounterSalePaymentBreakdown(
            totalSales = "30000.00",
            cashReceived = "0.00",
            bankReceived = "30000.00",
            totalReceived = "30000.00",
            totalDue = "0.00",
            salesCount = 1L,
            bankBreakdown = listOf(
                CounterSaleBankBreakdown(bankId = "b1", bankName = "Axis Bank", amount = "30000.00", percentage = 100.0)
            )
        )
        fakeRepo.counterBreakdownResult = expected

        val viewModel = CounterSaleOverviewViewModel(fakeRepo, refreshCoordinator)
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
    fun testLoadBreakdownMixedPaymentAndDues() = runTest {
        val expected = CounterSalePaymentBreakdown(
            totalSales = "50000.00",
            cashReceived = "20000.00",
            bankReceived = "15000.00",
            totalReceived = "35000.00",
            advanceApplied = "0.00",
            payableApplied = "0.00",
            totalDue = "15000.00",
            salesCount = 2L,
            bankBreakdown = listOf(
                CounterSaleBankBreakdown(bankId = "b1", bankName = "HDFC", amount = "15000.00", percentage = 30.0)
            )
        )
        fakeRepo.counterBreakdownResult = expected

        val viewModel = CounterSaleOverviewViewModel(fakeRepo, refreshCoordinator)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue(state is UiState.Success)
        val data = (state as UiState.Success).data
        assertEquals("50000.00", data.totalSales)
        assertEquals("20000.00", data.cashReceived)
        assertEquals("15000.00", data.bankReceived)
        assertEquals("15000.00", data.totalDue)
    }

    @Test
    fun testLoadBreakdownEmptySales() = runTest {
        val expected = CounterSalePaymentBreakdown(
            totalSales = "0.00",
            cashReceived = "0.00",
            bankReceived = "0.00",
            totalReceived = "0.00",
            totalDue = "0.00",
            salesCount = 0L,
            bankBreakdown = emptyList()
        )
        fakeRepo.counterBreakdownResult = expected

        val viewModel = CounterSaleOverviewViewModel(fakeRepo, refreshCoordinator)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue(state is UiState.Success)
        val data = (state as UiState.Success).data
        assertEquals("0.00", data.totalSales)
        assertEquals(0L, data.salesCount)
        assertTrue(data.bankBreakdown.isEmpty())
    }

    @Test
    fun testLoadBreakdownErrorAndRetry() = runTest {
        fakeRepo.shouldFailCounterBreakdown = true
        val viewModel = CounterSaleOverviewViewModel(fakeRepo, refreshCoordinator)
        testDispatcher.scheduler.advanceUntilIdle()

        val errorState = viewModel.state.value
        assertTrue(errorState is UiState.Error)
        assertEquals("Failed to load counter sale payment breakdown", (errorState as UiState.Error).message)

        // Retry successfully
        fakeRepo.shouldFailCounterBreakdown = false
        val expected = CounterSalePaymentBreakdown(totalSales = "10000.00", salesCount = 1L)
        fakeRepo.counterBreakdownResult = expected
        viewModel.refresh()
        testDispatcher.scheduler.advanceUntilIdle()

        val successState = viewModel.state.value
        assertTrue(successState is UiState.Success)
        assertEquals("10000.00", (successState as UiState.Success).data.totalSales)
    }

    @Test
    fun testAutoRefreshOnSalesMutation() = runTest {
        val initial = CounterSalePaymentBreakdown(totalSales = "50000.00", salesCount = 1L)
        fakeRepo.counterBreakdownResult = initial
        val viewModel = CounterSaleOverviewViewModel(fakeRepo, refreshCoordinator)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, fakeRepo.counterBreakdownCallCount)

        // Update repository data and broadcast SALES mutation
        val updated = CounterSalePaymentBreakdown(totalSales = "75000.00", salesCount = 2L)
        fakeRepo.counterBreakdownResult = updated
        refreshCoordinator.notifyMutation(DataMutation.SALES)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, fakeRepo.counterBreakdownCallCount)
        val state = viewModel.state.value
        assertTrue(state is UiState.Success)
        assertEquals("75000.00", (state as UiState.Success).data.totalSales)
    }

    @Test
    fun testUnrelatedMutationDoesNotTriggerReload() = runTest {
        val viewModel = CounterSaleOverviewViewModel(fakeRepo, refreshCoordinator)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, fakeRepo.counterBreakdownCallCount)
        assertTrue(viewModel.state.value is UiState.Success)

        // Non-sales mutation (e.g. STAFF) should NOT trigger counter sale breakdown refresh
        refreshCoordinator.notifyMutation(DataMutation.STAFF)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, fakeRepo.counterBreakdownCallCount)
        assertTrue(viewModel.state.value is UiState.Success)
    }

    @Test
    fun testLoadBreakdownWithPayableAllocation() = runTest {
        val expected = CounterSalePaymentBreakdown(
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
        fakeRepo.counterBreakdownResult = expected

        val viewModel = CounterSaleOverviewViewModel(fakeRepo, refreshCoordinator)
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
        val expected = CounterSalePaymentBreakdown(
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
        fakeRepo.counterBreakdownResult = expected

        val viewModel = CounterSaleOverviewViewModel(fakeRepo, refreshCoordinator)
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

    @Test
    fun testConcurrencyDuplicateRequestsPrevented() = runTest {
        val viewModel = CounterSaleOverviewViewModel(fakeRepo, refreshCoordinator)
        // Multiple rapid calls without force while job is executing
        viewModel.loadBreakdown(force = false)
        viewModel.loadBreakdown(force = false)
        testDispatcher.scheduler.advanceUntilIdle()

        // Should only be called once during initial init
        assertEquals(1, fakeRepo.counterBreakdownCallCount)
    }
}
