package com.vyavsa.app.feature.plans

import com.vyavsa.app.core.common.Resource
import com.vyavsa.app.core.common.UiState
import com.vyavsa.app.core.model.CheckoutCustomerInfo
import com.vyavsa.app.core.model.InitiateCheckoutResponse
import com.vyavsa.app.core.model.PlanTransaction
import com.vyavsa.app.core.model.SubscriptionPlan
import com.vyavsa.app.core.model.TenantSubscription
import com.vyavsa.app.core.model.VerifyPaymentResponse
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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

    @Test
    fun testStartPlanPurchaseInitiatesCheckoutSuccess() = runTest {
        viewModel.adminOverrideForTesting = true
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.startPlanPurchase("plan-2", "instant")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.checkoutState.value
        assertTrue("Expected AwaitingPayment state, got $state", state is CheckoutUiState.AwaitingPayment)
        val checkoutInfo = (state as CheckoutUiState.AwaitingPayment).checkoutInfo
        assertEquals("plan-2", checkoutInfo.planId)
        assertEquals("rzp_test_1234567890", checkoutInfo.keyId)
        assertEquals("sub_razorpay_999", checkoutInfo.subscriptionId)
    }

    @Test
    fun testNonAdminStartPlanPurchaseBlockedByRbac() = runTest {
        viewModel.adminOverrideForTesting = false
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.startPlanPurchase("plan-2", "instant")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.checkoutState.value
        assertTrue("Expected Failed state for non-admin, got $state", state is CheckoutUiState.Failed)
        assertEquals("Only tenant administrators can manage subscription plans", (state as CheckoutUiState.Failed).message)
    }

    @Test
    fun testVerifyPaymentSuccess() = runTest {
        viewModel.adminOverrideForTesting = true
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.verifyPayment(
            planId = "plan-2",
            paymentId = "pay_test_001",
            subscriptionId = "sub_razorpay_999",
            signature = "valid_sig_xyz",
            activationMode = "instant"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.checkoutState.value
        assertTrue("Expected Success state, got $state", state is CheckoutUiState.Success)
        assertEquals("plan-2", (state as CheckoutUiState.Success).subscription?.currentPlanId)
        assertNull(viewModel.pendingVerification)
    }

    @Test
    fun testVerifyPaymentFailureRetainsPendingVerificationForRetry() = runTest {
        viewModel.adminOverrideForTesting = true
        fakeRepository.verifySuccess = false
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.verifyPayment(
            planId = "plan-2",
            paymentId = "pay_test_002",
            subscriptionId = "sub_razorpay_999",
            signature = "invalid_sig_abc",
            activationMode = "instant"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.checkoutState.value
        assertTrue("Expected Failed state, got $state", state is CheckoutUiState.Failed)
        assertTrue((state as CheckoutUiState.Failed).canRetryVerification)
        assertNotNull(viewModel.pendingVerification)
        assertEquals("pay_test_002", viewModel.pendingVerification?.paymentId)
    }

    @Test
    fun testRetryPendingVerificationSuccess() = runTest {
        viewModel.adminOverrideForTesting = true
        fakeRepository.verifySuccess = false
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.verifyPayment(
            planId = "plan-2",
            paymentId = "pay_test_003",
            subscriptionId = "sub_razorpay_999",
            signature = "valid_sig_after_retry",
            activationMode = "instant"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // Now backend recovers and verification succeeds
        fakeRepository.verifySuccess = true
        viewModel.retryPendingVerification()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.checkoutState.value
        assertTrue("Expected Success state after retry, got $state", state is CheckoutUiState.Success)
        assertEquals("plan-2", (state as CheckoutUiState.Success).subscription?.currentPlanId)
        assertNull(viewModel.pendingVerification)
    }

    @Test
    fun testCancelCheckoutResetsState() = runTest {
        viewModel.adminOverrideForTesting = true
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.startPlanPurchase("plan-2", "instant")
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.checkoutState.value is CheckoutUiState.AwaitingPayment)

        viewModel.cancelCheckout()
        assertEquals(CheckoutUiState.Idle, viewModel.checkoutState.value)
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

    var checkoutResponse = InitiateCheckoutResponse(
        provider = "razorpay",
        keyId = "rzp_test_1234567890",
        subscriptionId = "sub_razorpay_999",
        planId = "plan-2",
        planName = "Enterprise Annual",
        amount = "9999.00",
        currency = "INR",
        billingInterval = "yearly",
        customer = CheckoutCustomerInfo(
            name = "Test Admin",
            email = "admin@example.com",
            phone = "9876543210"
        )
    )

    var verifySuccess = true

    override fun initiateCheckout(planId: String, activationMode: String): Flow<Resource<InitiateCheckoutResponse>> = flow {
        emit(Resource.Loading)
        emit(Resource.Success(checkoutResponse.copy(planId = planId, activationMode = activationMode)))
    }

    override fun verifyPayment(
        planId: String,
        paymentId: String,
        subscriptionId: String,
        signature: String,
        activationMode: String
    ): Flow<Resource<VerifyPaymentResponse>> = flow {
        emit(Resource.Loading)
        if (verifySuccess) {
            subscription = subscription.copy(currentPlanId = planId)
            emit(Resource.Success(VerifyPaymentResponse(subscription = subscription)))
        } else {
            emit(Resource.Error("Payment verification failed on server: signature mismatch"))
        }
    }
}
