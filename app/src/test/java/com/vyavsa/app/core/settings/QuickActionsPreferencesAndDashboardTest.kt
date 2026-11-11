package com.vyavsa.app.core.settings

import android.content.Context
import com.vyavsa.app.core.common.Resource
import com.vyavsa.app.core.model.FinancialMetrics
import com.vyavsa.app.core.model.OfficeDashboardResponse
import com.vyavsa.app.core.model.SalesDashboardResponse
import com.vyavsa.app.core.model.TenantProfile
import com.vyavsa.app.core.model.TenantSubscription
import com.vyavsa.app.core.model.UserRole
import com.vyavsa.app.core.printer.PrinterPreferences
import com.vyavsa.app.core.security.TokenManager
import com.vyavsa.app.core.sync.DataRefreshCoordinator
import com.vyavsa.app.feature.dashboard.DashboardRepository
import com.vyavsa.app.feature.dashboard.DashboardViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class QuickActionsPreferencesAndDashboardTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakePrefs: FakeSharedPreferences
    private lateinit var testContext: Context
    private lateinit var printerPrefs: PrinterPreferences
    private lateinit var settingsPreferences: SettingsPreferences

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakePrefs = FakeSharedPreferences()
        testContext = TestContext(fakePrefs)
        printerPrefs = PrinterPreferences(testContext)
        settingsPreferences = SettingsPreferences(testContext, printerPrefs)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialDefaults_containExpectedActions() {
        val enabled = settingsPreferences.enabledQuickActions.value
        val expectedDefaults = QuickActionItem.values().filter { it.defaultEnabled }.map { it.id }.toSet()

        assertEquals(expectedDefaults, enabled)
        assertTrue(enabled.contains(QuickActionItem.CREATE_LINE_SALE.id))
        assertTrue(enabled.contains(QuickActionItem.CREATE_COUNTER_SALE.id))
        assertTrue(enabled.contains(QuickActionItem.CREATE_EXPENSE.id))
        assertTrue(enabled.contains(QuickActionItem.CREATE_PURCHASE.id))
        assertTrue(enabled.contains(QuickActionItem.CASH_DRAWER.id))
        assertTrue(enabled.contains(QuickActionItem.PRODUCTS.id))
        assertFalse(enabled.contains(QuickActionItem.BANK_ACCOUNTS.id))
        assertTrue(settingsPreferences.showQuickActions.value)
    }

    @Test
    fun testEnableAndDisableIndividualAction() {
        val saleId = QuickActionItem.CREATE_LINE_SALE.id
        val bankId = QuickActionItem.BANK_ACCOUNTS.id

        // Disable an enabled action
        settingsPreferences.toggleQuickAction(saleId, false)
        assertFalse(settingsPreferences.enabledQuickActions.value.contains(saleId))

        // Enable a disabled action
        settingsPreferences.toggleQuickAction(bankId, true)
        assertTrue(settingsPreferences.enabledQuickActions.value.contains(bankId))

        // Re-enable the first action
        settingsPreferences.toggleQuickAction(saleId, true)
        assertTrue(settingsPreferences.enabledQuickActions.value.contains(saleId))
    }

    @Test
    fun testToggleMultipleIndependentActions() {
        val counterSaleId = QuickActionItem.CREATE_COUNTER_SALE.id
        val purchaseId = QuickActionItem.CREATE_PURCHASE.id
        val expenseId = QuickActionItem.CREATE_EXPENSE.id

        // Toggle Counter Sale off without affecting others
        settingsPreferences.toggleQuickAction(counterSaleId, false)
        assertFalse(settingsPreferences.enabledQuickActions.value.contains(counterSaleId))
        assertTrue(settingsPreferences.enabledQuickActions.value.contains(purchaseId))
        assertTrue(settingsPreferences.enabledQuickActions.value.contains(expenseId))

        // Toggle Purchase off
        settingsPreferences.toggleQuickAction(purchaseId, false)
        assertFalse(settingsPreferences.enabledQuickActions.value.contains(counterSaleId))
        assertFalse(settingsPreferences.enabledQuickActions.value.contains(purchaseId))
        assertTrue(settingsPreferences.enabledQuickActions.value.contains(expenseId))

        // Toggle Counter Sale back on
        settingsPreferences.toggleQuickAction(counterSaleId, true)
        assertTrue(settingsPreferences.enabledQuickActions.value.contains(counterSaleId))
        assertFalse(settingsPreferences.enabledQuickActions.value.contains(purchaseId))
        assertTrue(settingsPreferences.enabledQuickActions.value.contains(expenseId))
    }

    @Test
    fun testPersistenceAndStateRestorationAcrossAppRestart() {
        val counterSaleId = QuickActionItem.CREATE_COUNTER_SALE.id
        val bankAccountsId = QuickActionItem.BANK_ACCOUNTS.id

        // User disables counter sale and enables bank accounts
        settingsPreferences.toggleQuickAction(counterSaleId, false)
        settingsPreferences.toggleQuickAction(bankAccountsId, true)

        // Verify persisted in SharedPreferences storage
        val persisted = fakePrefs.getStringSet("setting_quick_actions", null)
        assertFalse(persisted!!.contains(counterSaleId))
        assertTrue(persisted.contains(bankAccountsId))

        // Simulate application restart with new SettingsPreferences instance sharing the same storage
        val restoredPrefs = SettingsPreferences(testContext, printerPrefs)
        val restoredSet = restoredPrefs.enabledQuickActions.value

        assertFalse(restoredSet.contains(counterSaleId))
        assertTrue(restoredSet.contains(bankAccountsId))
        assertTrue(restoredSet.contains(QuickActionItem.CREATE_LINE_SALE.id))
        assertTrue(restoredSet.contains(QuickActionItem.CREATE_EXPENSE.id))
    }

    @Test
    fun testDisablingAllActions_persistsEmptySet_andDoesNotRevertToDefaults() {
        // Disable every action
        QuickActionItem.values().forEach { action ->
            settingsPreferences.toggleQuickAction(action.id, false)
        }

        assertTrue(settingsPreferences.enabledQuickActions.value.isEmpty())

        // Persisted storage must record the empty set
        val persisted = fakePrefs.getStringSet("setting_quick_actions", null)
        assertTrue(persisted != null && persisted.isEmpty())

        // Recreate on app restart - MUST remain empty, not revert to defaults!
        val restartedPrefs = SettingsPreferences(testContext, printerPrefs)
        assertTrue(restartedPrefs.enabledQuickActions.value.isEmpty())
    }

    @Test
    fun testCorruptedOrInvalidPreferences_safelyHandled() {
        // Inject corrupt / obsolete action keys into preferences
        fakePrefs.edit().putStringSet(
            "setting_quick_actions",
            setOf("unknown_legacy_action", QuickActionItem.CREATE_LINE_SALE.id, "corrupted_uuid_999")
        ).apply()

        val freshPrefs = SettingsPreferences(testContext, printerPrefs)
        val sanitized = freshPrefs.enabledQuickActions.value

        // Only valid enum ID should remain
        assertEquals(setOf(QuickActionItem.CREATE_LINE_SALE.id), sanitized)
        assertFalse(sanitized.contains("unknown_legacy_action"))
        assertFalse(sanitized.contains("corrupted_uuid_999"))
    }

    @Test
    fun testResetToDefaults_restoresDefaultQuickActions() {
        settingsPreferences.toggleQuickAction(QuickActionItem.CREATE_LINE_SALE.id, false)
        settingsPreferences.toggleQuickAction(QuickActionItem.BANK_ACCOUNTS.id, true)
        settingsPreferences.saveShowQuickActions(false)

        assertFalse(settingsPreferences.showQuickActions.value)

        settingsPreferences.resetToDefaults()

        assertTrue(settingsPreferences.showQuickActions.value)
        val expected = QuickActionItem.values().filter { it.defaultEnabled }.map { it.id }.toSet()
        assertEquals(expected, settingsPreferences.enabledQuickActions.value)
    }

    @Test
    fun testDashboardViewModel_reflectsSettingsUpdatesImmediately() = runTest {
        val fakeDashboardRepo = object : DashboardRepository {
            override fun getDashboardMetrics(): Flow<Resource<FinancialMetrics>> = flowOf(Resource.Success(FinancialMetrics()))
            override fun getSalesDashboard(date: String?): Flow<Resource<SalesDashboardResponse>> = flowOf(Resource.Loading)
            override fun getOfficeDashboard(date: String?): Flow<Resource<OfficeDashboardResponse>> = flowOf(Resource.Loading)
            override fun getProfile(): Flow<Resource<TenantProfile>> = flowOf(Resource.Loading)
            override fun getSubscription(): Flow<Resource<TenantSubscription>> = flowOf(Resource.Loading)
        }
        val fakeTokenManager = object : TokenManager() {
            override fun getUserRole(): UserRole = UserRole.ADMIN
        }
        val coordinator = DataRefreshCoordinator()

        val dashboardVm = DashboardViewModel(
            repository = fakeDashboardRepo,
            refreshCoordinator = coordinator,
            tokenManager = fakeTokenManager,
            settingsPreferences = settingsPreferences
        )

        // Initially matches
        assertTrue(dashboardVm.showQuickActions.value)
        assertTrue(dashboardVm.enabledQuickActions.value.contains(QuickActionItem.CREATE_COUNTER_SALE.id))

        // Toggle in settingsPreferences
        settingsPreferences.toggleQuickAction(QuickActionItem.CREATE_COUNTER_SALE.id, false)
        assertFalse(dashboardVm.enabledQuickActions.value.contains(QuickActionItem.CREATE_COUNTER_SALE.id))

        // Toggle showQuickActions
        settingsPreferences.saveShowQuickActions(false)
        assertFalse(dashboardVm.showQuickActions.value)

        // Toggle showQuickActions back on
        settingsPreferences.saveShowQuickActions(true)
        assertTrue(dashboardVm.showQuickActions.value)
    }
}
