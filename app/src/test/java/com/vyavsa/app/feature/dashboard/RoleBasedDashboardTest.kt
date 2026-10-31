package com.vyavsa.app.feature.dashboard

import com.vyavsa.app.core.common.Resource
import com.vyavsa.app.core.common.UiState
import com.vyavsa.app.core.model.ApiResponse
import com.vyavsa.app.core.model.FinancialMetrics
import com.vyavsa.app.core.model.OfficeDashboardResponse
import com.vyavsa.app.core.model.RecordAdvanceRequest
import com.vyavsa.app.core.model.RecordOvertimeRequest
import com.vyavsa.app.core.model.SalesDashboardResponse
import com.vyavsa.app.core.model.TenantProfile
import com.vyavsa.app.core.model.TenantSubscription
import com.vyavsa.app.core.model.UserProfile
import com.vyavsa.app.core.model.UserRole
import com.vyavsa.app.core.navigation.BottomBarDestination
import com.vyavsa.app.core.security.TokenManager
import com.vyavsa.app.core.sync.DataRefreshCoordinator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RoleBasedDashboardTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true; encodeDefaults = true; prettyPrint = false }
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // -------------------------------------------------------------------------
    // 1. Role Parsing & Permission Tests
    // -------------------------------------------------------------------------

    @Test
    fun testUserRoleParsing() {
        assertEquals(UserRole.SALES, UserRole.fromString("sales"))
        assertEquals(UserRole.SALES, UserRole.fromString("SALES"))
        assertEquals(UserRole.SALES, UserRole.fromString("Sales"))

        assertEquals(UserRole.OFFICE, UserRole.fromString("office"))
        assertEquals(UserRole.OFFICE, UserRole.fromString("OFFICE"))
        assertEquals(UserRole.OFFICE, UserRole.fromString("Office"))

        assertEquals(UserRole.ADMIN, UserRole.fromString("admin"))
        assertEquals(UserRole.ADMIN, UserRole.fromString("tenant_admin"))
        assertEquals(UserRole.ADMIN, UserRole.fromString("ADMIN"))

        assertEquals(UserRole.USER, UserRole.fromString("tenant_user"))
        assertEquals(UserRole.USER, UserRole.fromString("user"))
        // Legacy fallback defaults to ADMIN
        assertEquals(UserRole.ADMIN, UserRole.fromString("unknown_role"))
    }

    @Test
    fun testDestinationsForRoles() {
        val salesDestinations = BottomBarDestination.destinationsForRole(UserRole.SALES)
        assertEquals(5, salesDestinations.size)
        assertTrue(salesDestinations.contains(BottomBarDestination.HOME))
        assertTrue(salesDestinations.contains(BottomBarDestination.SALES))
        assertTrue(salesDestinations.contains(BottomBarDestination.DUES))
        assertTrue(salesDestinations.contains(BottomBarDestination.PRODUCTS))
        assertTrue(salesDestinations.contains(BottomBarDestination.MORE))
        assertFalse(salesDestinations.contains(BottomBarDestination.COUNTER_SALES))
        assertFalse(salesDestinations.contains(BottomBarDestination.STAFF))

        val officeDestinations = BottomBarDestination.destinationsForRole(UserRole.OFFICE)
        assertEquals(6, officeDestinations.size)
        assertTrue(officeDestinations.contains(BottomBarDestination.HOME))
        assertTrue(officeDestinations.contains(BottomBarDestination.COUNTER_SALES))
        assertTrue(officeDestinations.contains(BottomBarDestination.TRANSACTIONS))
        assertTrue(officeDestinations.contains(BottomBarDestination.DUES))
        assertTrue(officeDestinations.contains(BottomBarDestination.STAFF))
        assertTrue(officeDestinations.contains(BottomBarDestination.MORE))
        assertFalse(officeDestinations.contains(BottomBarDestination.SALES))

        val adminDestinations = BottomBarDestination.destinationsForRole(UserRole.ADMIN)
        assertEquals(6, adminDestinations.size)
        assertTrue(adminDestinations.contains(BottomBarDestination.HOME))
        assertTrue(adminDestinations.contains(BottomBarDestination.SALES))
        assertTrue(adminDestinations.contains(BottomBarDestination.TRANSACTIONS))
        assertTrue(adminDestinations.contains(BottomBarDestination.DUES))
        assertTrue(adminDestinations.contains(BottomBarDestination.STAFF))
        assertTrue(adminDestinations.contains(BottomBarDestination.MORE))
    }

    // -------------------------------------------------------------------------
    // 2. Serialization & Contract Parity Tests (Go Backend -> Android DTO)
    // -------------------------------------------------------------------------

    @Test
    fun testSalesDashboardResponseSerialization() {
        val payload = """
            {
              "success": true,
              "message": "sales dashboard retrieved",
              "data": {
                "business_date": "2026-10-20",
                "currency": "INR",
                "total_cash_collected": "15200.00",
                "total_bank_received": "8400.00",
                "total_sales_count": 12,
                "total_sales_amount": "25000.00",
                "outstanding_dues": "1400.00"
              }
            }
        """.trimIndent()

        val resp = json.decodeFromString<ApiResponse<SalesDashboardResponse>>(payload)
        assertTrue(resp.success)
        val data = resp.data
        assertNotNull(data)
        assertEquals("2026-10-20", data?.businessDate)
        assertEquals("INR", data?.currency)
        assertEquals("15200.00", data?.totalCashCollected)
        assertEquals("8400.00", data?.totalBankReceived)
        assertEquals(12L, data?.totalSalesCount)
        assertEquals("25000.00", data?.totalSalesAmount)
        assertEquals("1400.00", data?.outstandingDues)
    }

    @Test
    fun testOfficeDashboardResponseSerialization() {
        val payload = """
            {
              "success": true,
              "message": "office dashboard retrieved",
              "data": {
                "business_date": "2026-10-20",
                "currency": "INR",
                "total_cash_collected": "4500.00",
                "total_bank_received": "3200.00",
                "total_attendance": 11,
                "present_count": 8,
                "half_day_count": 1,
                "absent_count": 2,
                "total_active_employees": 11,
                "total_sales_count": 25,
                "total_sales_amount": "7700.00"
              }
            }
        """.trimIndent()

        val resp = json.decodeFromString<ApiResponse<OfficeDashboardResponse>>(payload)
        assertTrue(resp.success)
        val data = resp.data
        assertNotNull(data)
        assertEquals("2026-10-20", data?.businessDate)
        assertEquals("INR", data?.currency)
        assertEquals("4500.00", data?.totalCashCollected)
        assertEquals("3200.00", data?.totalBankReceived)
        assertEquals(11, data?.totalAttendance)
        assertEquals(8, data?.presentCount)
        assertEquals(1, data?.halfDayCount)
        assertEquals(2, data?.absentCount)
        assertEquals(11, data?.totalActiveEmployees)
        assertEquals(25L, data?.totalSalesCount)
        assertEquals("7700.00", data?.totalSalesAmount)
    }

    @Test
    fun testRecordAdvanceAndOvertimeRequestsSerialization() {
        val advReq = RecordAdvanceRequest(
            employeeId = "emp-101",
            amount = "1200.00",
            date = "2026-10-20",
            paymentMethod = "cash",
            note = "Festival advance"
        )
        val advJson = json.encodeToString(advReq)
        assertTrue(advJson.contains(""""employee_id":"emp-101""""))
        assertTrue(advJson.contains(""""amount":"1200.00""""))
        assertTrue(advJson.contains(""""payment_method":"cash""""))
        assertTrue(advJson.contains(""""note":"Festival advance""""))

        val otReq = RecordOvertimeRequest(
            employeeId = "emp-102",
            amount = "350.00",
            ot = "4.0",
            date = "2026-10-20",
            note = "Sunday inventory packing"
        )
        val otJson = json.encodeToString(otReq)
        assertTrue(otJson.contains(""""employee_id":"emp-102""""))
        assertTrue(otJson.contains(""""amount":"350.00""""))
        assertTrue(otJson.contains(""""ot":"4.0""""))
        assertTrue(otJson.contains(""""note":"Sunday inventory packing""""))
    }

    // -------------------------------------------------------------------------
    // 3. ViewModel Role-Based Dispatching Tests
    // -------------------------------------------------------------------------

    @Test
    fun testDashboardViewModelDispatchesSalesRole() = runTest {
        val fakeRepo = TestRoleDashboardRepository()
        val fakeTokenManager = FakeTestTokenManager(
            role = UserRole.SALES,
            profile = UserProfile(id = "1", name = "Ravi Sales", email = "ravi@vyavsa.com", role = "sales")
        )
        val coordinator = DataRefreshCoordinator()

        val vm = DashboardViewModel(fakeRepo, coordinator, fakeTokenManager)
        testDispatcher.scheduler.advanceUntilIdle()

        // Sales state must be populated
        val salesState = vm.salesDashboardState.value
        assertTrue("Expected Sales state success, got $salesState", salesState is UiState.Success)
        val salesData = (salesState as UiState.Success).data
        assertEquals("15000.00", salesData.totalCashCollected)
        assertEquals("5000.00", salesData.totalBankReceived)
    }

    @Test
    fun testDashboardViewModelDispatchesOfficeRole() = runTest {
        val fakeRepo = TestRoleDashboardRepository()
        val fakeTokenManager = FakeTestTokenManager(
            role = UserRole.OFFICE,
            profile = UserProfile(id = "2", name = "Pooja Office", email = "pooja@vyavsa.com", role = "office")
        )
        val coordinator = DataRefreshCoordinator()

        val vm = DashboardViewModel(fakeRepo, coordinator, fakeTokenManager)
        testDispatcher.scheduler.advanceUntilIdle()

        // Office state must be populated
        val officeState = vm.officeDashboardState.value
        assertTrue("Expected Office state success, got $officeState", officeState is UiState.Success)
        val officeData = (officeState as UiState.Success).data
        assertEquals("6000.00", officeData.totalCashCollected)
        assertEquals("4000.00", officeData.totalBankReceived)
        assertEquals(7, officeData.presentCount)
    }

    @Test
    fun testDashboardViewModelDispatchesAdminRole() = runTest {
        val fakeRepo = TestRoleDashboardRepository()
        val fakeTokenManager = FakeTestTokenManager(
            role = UserRole.ADMIN,
            profile = UserProfile(id = "3", name = "Admin Owner", email = "admin@vyavsa.com", role = "admin")
        )
        val coordinator = DataRefreshCoordinator()

        val vm = DashboardViewModel(fakeRepo, coordinator, fakeTokenManager)
        testDispatcher.scheduler.advanceUntilIdle()

        // Metrics state must be populated
        val metricsState = vm.metricsState.value
        assertTrue("Expected Admin metrics success, got $metricsState", metricsState is UiState.Success)
        val metrics = (metricsState as UiState.Success).data
        assertEquals("25000.00", metrics.cashBalance)
    }

    @Test
    fun testDashboardViewModelQuickActionsObservation() = runTest {
        val fakeRepo = TestRoleDashboardRepository()
        val fakeTokenManager = FakeTestTokenManager(role = UserRole.ADMIN)
        val coordinator = DataRefreshCoordinator()

        val vm = DashboardViewModel(fakeRepo, coordinator, fakeTokenManager)
        testDispatcher.scheduler.advanceUntilIdle()

        // Default Quick Actions state should be true and enabled actions populated
        assertTrue(vm.showQuickActions.value)
        assertTrue(vm.enabledQuickActions.value.isNotEmpty())
    }
}

private class FakeTestTokenManager(
    var role: UserRole = UserRole.ADMIN,
    var profile: UserProfile? = null
) : TokenManager() {
    override fun getUserRole(): UserRole = role
    override fun isSales(): Boolean = role == UserRole.SALES
    override fun isOffice(): Boolean = role == UserRole.OFFICE
    override fun isAdminOrUser(): Boolean = role == UserRole.ADMIN || role == UserRole.USER
    override fun getUserProfile(): UserProfile? = profile
}

private class TestRoleDashboardRepository : DashboardRepository {
    override fun getDashboardMetrics(): Flow<Resource<FinancialMetrics>> = flowOf(
        Resource.Success(FinancialMetrics(cashBalance = "25000.00", bankBalance = "100000.00"))
    )

    override fun getSalesDashboard(date: String?): Flow<Resource<SalesDashboardResponse>> = flowOf(
        Resource.Success(
            SalesDashboardResponse(
                businessDate = date ?: "2026-10-20",
                totalCashCollected = "15000.00",
                totalBankReceived = "5000.00"
            )
        )
    )

    override fun getOfficeDashboard(date: String?): Flow<Resource<OfficeDashboardResponse>> = flowOf(
        Resource.Success(
            OfficeDashboardResponse(
                businessDate = date ?: "2026-10-20",
                totalCashCollected = "6000.00",
                totalBankReceived = "4000.00",
                presentCount = 7
            )
        )
    )

    override fun getProfile(): Flow<Resource<TenantProfile>> = flowOf(Resource.Success(TenantProfile(id = "1", name = "Test Company")))
    override fun getSubscription(): Flow<Resource<TenantSubscription>> = flowOf(Resource.Success(TenantSubscription(id = "sub-1")))
}
