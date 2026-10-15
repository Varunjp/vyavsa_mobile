package com.vyavsa.app.feature.plans

import com.vyavsa.app.core.common.Resource
import com.vyavsa.app.core.common.UiState
import com.vyavsa.app.core.model.PlanTransaction
import com.vyavsa.app.core.model.SubscriptionPlan
import com.vyavsa.app.core.model.TenantSubscription
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
class PlansViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakePlansRepository
    private lateinit var refreshCoordinator: DataRefreshCoordinator
    private lateinit var viewModel: PlansViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakePlansRepository()
        refreshCoordinator = DataRefreshCoordinator()
        viewModel = PlansViewModel(fakeRepository, refreshCoordinator)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialLoadSuccess() = runTest {
        viewModel.adminOverrideForTesting = true
        viewModel.loadData()
        testDispatcher.scheduler.advanceUntilIdle()

        // Subscription State
        val subState = viewModel.subscriptionState.value
        assertTrue("Expected Success subscription, got $subState", subState is UiState.Success)
        val subscription = (subState as UiState.Success).data
        assertEquals("active", subscription.status)
        assertEquals("Pro Growth", subscription.currentPlanName)

        // Plans State
        val plansState = viewModel.plansState.value
        assertTrue("Expected Success plans, got $plansState", plansState is UiState.Success)
        val plans = (plansState as UiState.Success).data
        assertEquals(2, plans.size)
        assertEquals("Pro Growth", plans[0].planName)

        // Transactions State
        val txState = viewModel.transactionsState.value
        assertTrue("Expected Success transactions, got $txState", txState is UiState.Success)
        val transactions = (txState as UiState.Success).data
        assertEquals(1, transactions.size)
        assertEquals("999.00", transactions[0].amount)
    }

    @Test
    fun testTabSelection() = runTest {
        assertEquals(PlansTab.AVAILABLE_PLANS, viewModel.selectedTab.value)
        viewModel.selectTab(PlansTab.BILLING_HISTORY)
        assertEquals(PlansTab.BILLING_HISTORY, viewModel.selectedTab.value)
    }

    @Test
    fun testAdminPurchasePlanSuccess() = runTest {
        viewModel.adminOverrideForTesting = true
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.purchasePlan("plan-2", "upi")
        testDispatcher.scheduler.advanceUntilIdle()

        val actionState = viewModel.purchaseActionState.value
        assertTrue("Expected Success for plan purchase, got $actionState", actionState is UiState.Success)
        assertEquals("plan-2", fakeRepository.subscription.currentPlanId)
    }

    @Test
    fun testNonAdminPurchasePlanBlockedByRbac() = runTest {
        viewModel.adminOverrideForTesting = false
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.purchasePlan("plan-2", "upi")
        testDispatcher.scheduler.advanceUntilIdle()

        val actionState = viewModel.purchaseActionState.value
        assertTrue("Expected Error for non-admin, got $actionState", actionState is UiState.Error)
        assertEquals(
            "Only tenant administrators can manage subscription plans",
            (actionState as UiState.Error).message
        )
    }
}

class FakePlansRepository : PlansRepository {

    var subscription = TenantSubscription(
        id = "sub-1",
        tenantId = "tenant-1",
        currentPlanId = "plan-1",
        currentPlanName = "Pro Growth",
        price = "999.00",
        status = "active",
        startDate = "2026-01-01T00:00:00Z",
        endDate = "2026-12-31T23:59:59Z",
        daysRemaining = 260
    )

    val plansList = mutableListOf(
        SubscriptionPlan(
            id = "plan-1",
            planName = "Pro Growth",
            name = "Pro Growth",
            note = "For growing retail and wholesale businesses",
            price = "999.00",
            durationDays = 30,
            features = listOf("Unlimited Sales", "Multi-user RBAC", "Inventory Sync")
        ),
        SubscriptionPlan(
            id = "plan-2",
            planName = "Enterprise Annual",
            name = "Enterprise Annual",
            note = "Full featured business suite",
            price = "9999.00",
            durationDays = 365,
            features = listOf("All Pro Features", "Dedicated Manager", "Custom Invoices")
        )
    )

    val transactions = mutableListOf(
        PlanTransaction(
            id = "tx-plan-1",
            tenantId = "tenant-1",
            transactionId = "tx-12345",
            planId = "plan-1",
            planName = "Pro Growth",
            amount = "999.00",
            paymentMethod = "upi",
            status = "completed",
            createdAt = "2026-01-01T10:00:00Z"
        )
    )

    override fun getSubscription(): Flow<Resource<TenantSubscription>> = flow {
        emit(Resource.Loading)
        emit(Resource.Success(subscription))
    }

    override fun listAvailablePlans(): Flow<Resource<List<SubscriptionPlan>>> = flow {
        emit(Resource.Loading)
        emit(Resource.Success(plansList))
    }

    override fun purchasePlan(planId: String, paymentMethod: String): Flow<Resource<TenantSubscription>> = flow {
        emit(Resource.Loading)
        subscription = subscription.copy(currentPlanId = planId)
        emit(Resource.Success(subscription))
    }

    override fun listBillingTransactions(): Flow<Resource<List<PlanTransaction>>> = flow {
        emit(Resource.Loading)
        emit(Resource.Success(transactions))
    }
}
