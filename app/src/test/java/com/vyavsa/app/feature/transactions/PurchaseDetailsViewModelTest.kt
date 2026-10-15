package com.vyavsa.app.feature.transactions

import androidx.lifecycle.SavedStateHandle
import com.vyavsa.app.core.common.UiState
import com.vyavsa.app.core.model.Purchase
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
class PurchaseDetailsViewModelTest {

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

        fakeRepo.purchases.add(
            Purchase(
                id = "purch-1",
                item = "Basmati Rice 25kg",
                quantity = 10,
                totalAmount = "12000.00",
                paymentStatus = "PARTIAL",
                customerId = "cust-1",
                customerName = "Sharma Stores",
                totalPaid = "5000.00",
                totalPending = "7000.00",
                createdAt = "2026-10-15T09:00:00Z"
            )
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testLoadPurchaseDetailsSuccess() = runTest {
        val handle = SavedStateHandle(mapOf("purchaseId" to "purch-1"))
        val viewModel = PurchaseDetailsViewModel(fakeRepo, fakeBankRepo, refreshCoordinator, null, handle)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.purchaseState.value
        assertTrue("Expected Success, got $state", state is UiState.Success)
        val purchase = (state as UiState.Success).data
        assertEquals("purch-1", purchase.id)
        assertEquals("Basmati Rice 25kg", purchase.item)
        assertEquals("12000.00", purchase.totalAmount)
        assertEquals("7000.00", purchase.totalPending)
    }

    @Test
    fun testLoadPurchaseDetailsNotFound() = runTest {
        val handle = SavedStateHandle(mapOf("purchaseId" to "purch-nonexistent"))
        val viewModel = PurchaseDetailsViewModel(fakeRepo, fakeBankRepo, refreshCoordinator, null, handle)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.purchaseState.value
        assertTrue("Expected Error, got $state", state is UiState.Error)
        assertEquals("Purchase not found", (state as UiState.Error).message)
    }

    @Test
    fun testRecordPaymentSuccess() = runTest {
        val handle = SavedStateHandle(mapOf("purchaseId" to "purch-1"))
        val viewModel = PurchaseDetailsViewModel(fakeRepo, fakeBankRepo, refreshCoordinator, null, handle)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.recordPayment(
            amount = "3000.00",
            paymentMethod = "cash",
            bankId = null,
            date = "2026-10-15",
            note = "Supplier partial settlement"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val actionState = viewModel.paymentActionState.value
        assertTrue("Expected Success, got $actionState", actionState is UiState.Success)
    }

    @Test
    fun testRecordPaymentValidationZeroOrNegativeAmount() = runTest {
        val handle = SavedStateHandle(mapOf("purchaseId" to "purch-1"))
        val viewModel = PurchaseDetailsViewModel(fakeRepo, fakeBankRepo, refreshCoordinator, null, handle)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.recordPayment(
            amount = "0.00",
            paymentMethod = "cash",
            bankId = null,
            date = "2026-10-15"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val actionState = viewModel.paymentActionState.value
        assertTrue("Expected Error, got $actionState", actionState is UiState.Error)
        assertEquals("Payment amount must be greater than zero", (actionState as UiState.Error).message)
    }

    @Test
    fun testRecordPaymentValidationBankRequiresBankSelection() = runTest {
        val handle = SavedStateHandle(mapOf("purchaseId" to "purch-1"))
        val viewModel = PurchaseDetailsViewModel(fakeRepo, fakeBankRepo, refreshCoordinator, null, handle)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.recordPayment(
            amount = "1500.00",
            paymentMethod = "bank",
            bankId = null,
            date = "2026-10-15"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val actionState = viewModel.paymentActionState.value
        assertTrue("Expected Error, got $actionState", actionState is UiState.Error)
        assertEquals("Please select a bank account for bank payment", (actionState as UiState.Error).message)
    }

    @Test
    fun testUpdatePurchaseSuccess() = runTest {
        val handle = SavedStateHandle(mapOf("purchaseId" to "purch-1"))
        val viewModel = PurchaseDetailsViewModel(fakeRepo, fakeBankRepo, refreshCoordinator, null, handle)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updatePurchase(
            item = "Basmati Premium Rice 25kg",
            quantity = "12",
            totalAmount = "15600.00",
            totalPaid = "5000.00"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val actionState = viewModel.updateActionState.value
        assertTrue("Expected Success, got $actionState", actionState is UiState.Success)

        val updatedPurchase = fakeRepo.purchases.first { it.id == "purch-1" }
        assertEquals("Basmati Premium Rice 25kg", updatedPurchase.item)
        assertEquals(12, updatedPurchase.quantity)
        assertEquals("15600.00", updatedPurchase.totalAmount)
    }

    @Test
    fun testUpdatePurchaseValidationMissingItem() = runTest {
        val handle = SavedStateHandle(mapOf("purchaseId" to "purch-1"))
        val viewModel = PurchaseDetailsViewModel(fakeRepo, fakeBankRepo, refreshCoordinator, null, handle)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updatePurchase(
            item = "",
            quantity = "10",
            totalAmount = "1000.00",
            totalPaid = "500.00"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val actionState = viewModel.updateActionState.value
        assertTrue("Expected Error, got $actionState", actionState is UiState.Error)
        assertEquals("Item description is required", (actionState as UiState.Error).message)
    }

    @Test
    fun testDeletePurchaseSuccess() = runTest {
        val handle = SavedStateHandle(mapOf("purchaseId" to "purch-1"))
        val viewModel = PurchaseDetailsViewModel(fakeRepo, fakeBankRepo, refreshCoordinator, null, handle)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.deletePurchase()
        testDispatcher.scheduler.advanceUntilIdle()

        val actionState = viewModel.deleteActionState.value
        assertTrue("Expected Success, got $actionState", actionState is UiState.Success)
        assertTrue(fakeRepo.purchases.none { it.id == "purch-1" })
    }
}
