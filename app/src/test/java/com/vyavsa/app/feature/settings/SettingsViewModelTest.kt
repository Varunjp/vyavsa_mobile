package com.vyavsa.app.feature.settings

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.vyavsa.app.core.common.Resource
import com.vyavsa.app.core.common.UiState
import com.vyavsa.app.core.model.FinancialMetrics
import com.vyavsa.app.core.model.OfficeDashboardResponse
import com.vyavsa.app.core.model.SalesDashboardResponse
import com.vyavsa.app.core.model.TenantProfile
import com.vyavsa.app.core.model.TenantSubscription
import com.vyavsa.app.core.network.VyavsaApiService
import com.vyavsa.app.core.printer.FakeThermalPrinterManager
import com.vyavsa.app.core.printer.InvoiceContentSettings
import com.vyavsa.app.core.printer.InvoiceTheme
import com.vyavsa.app.core.printer.PaperWidth
import com.vyavsa.app.core.printer.PrintJobStatus
import com.vyavsa.app.core.printer.PrintTypography
import com.vyavsa.app.core.printer.PrinterConnectionType
import com.vyavsa.app.core.printer.PrinterDevice
import com.vyavsa.app.core.printer.PrinterPreferences
import com.vyavsa.app.core.settings.AppFontSize
import com.vyavsa.app.core.settings.AppLanguage
import com.vyavsa.app.core.settings.FakeSharedPreferences
import com.vyavsa.app.core.settings.SettingsPreferences
import com.vyavsa.app.core.settings.TestContext
import com.vyavsa.app.core.settings.ThemeMode
import com.vyavsa.app.core.sync.DataMutation
import com.vyavsa.app.core.sync.DataRefreshCoordinator
import com.vyavsa.app.feature.dashboard.DashboardRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockWebServer: MockWebServer
    private lateinit var apiService: VyavsaApiService
    private lateinit var fakePrefs: FakeSharedPreferences
    private lateinit var testContext: TestContext
    private lateinit var printerPrefs: PrinterPreferences
    private lateinit var settingsPrefs: SettingsPreferences
    private lateinit var fakePrinterManager: FakeThermalPrinterManager
    private lateinit var refreshCoordinator: DataRefreshCoordinator
    private lateinit var fakeDashboardRepo: FakeSettingsDashboardRepo

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private class FakeSettingsDashboardRepo(
        var profile: TenantProfile? = TenantProfile(
            id = "t-1",
            name = "Vyavsa Hardware & Tools",
            email = "hardware@vyavsa.com",
            phone = "9876543210",
            addressLine1 = "Shop 12, Market Complex",
            city = "Jaipur",
            state = "Rajasthan",
            invoiceFooterHeading = "Thank you for shopping with us!",
            invoiceFooterParagraph = "Goods once sold can only be exchanged within 7 days."
        )
    ) : DashboardRepository {
        override fun getDashboardMetrics(): Flow<Resource<FinancialMetrics>> = flow {}
        override fun getSalesDashboard(date: String?): Flow<Resource<SalesDashboardResponse>> = flow {}
        override fun getOfficeDashboard(date: String?): Flow<Resource<OfficeDashboardResponse>> = flow {}
        override fun getProfile(): Flow<Resource<TenantProfile>> = flow {
            profile?.let { emit(Resource.Success(it)) }
        }
        override fun getSubscription(): Flow<Resource<TenantSubscription>> = flow {}
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockWebServer = MockWebServer()

        val dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return when {
                    request.path == "/api/v1/tenant/profile" && request.method == "PUT" -> {
                        MockResponse()
                            .setResponseCode(200)
                            .setHeader("Content-Type", "application/json")
                            .setBody("""
                            {
                              "success": true,
                              "data": {
                                "id": "t-1",
                                "name": "Vyavsa Hardware & Tools",
                                "email": "hardware@vyavsa.com",
                                "phone": "9876543210",
                                "invoice_footer_heading": "Updated Terms",
                                "invoice_footer_paragraph": "No returns without bill."
                              }
                            }
                        """.trimIndent())
                    }
                    else -> MockResponse().setResponseCode(404)
                }
            }
        }
        mockWebServer.dispatcher = dispatcher
        mockWebServer.start()

        val contentType = "application/json".toMediaType()
        val retrofit = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .client(OkHttpClient.Builder().build())
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
        apiService = retrofit.create(VyavsaApiService::class.java)

        fakePrefs = FakeSharedPreferences()
        testContext = TestContext(fakePrefs)
        printerPrefs = PrinterPreferences(testContext)
        settingsPrefs = SettingsPreferences(testContext, printerPrefs)
        fakePrinterManager = FakeThermalPrinterManager()
        refreshCoordinator = DataRefreshCoordinator()
        fakeDashboardRepo = FakeSettingsDashboardRepo()
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
        Dispatchers.resetMain()
    }

    private fun createViewModel(): SettingsViewModel {
        return SettingsViewModel(
            settingsPreferences = settingsPrefs,
            printerPreferences = printerPrefs,
            printerManager = fakePrinterManager,
            tokenManager = null,
            dashboardRepository = fakeDashboardRepo,
            apiService = apiService,
            refreshCoordinator = refreshCoordinator
        )
    }

    @Test
    fun testInitialSettingsLoaded() = runTest {
        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(ThemeMode.SYSTEM, vm.themeMode.value)
        assertEquals(AppFontSize.MEDIUM, vm.fontSize.value)
        assertEquals(AppLanguage.ENGLISH, vm.language.value)
        assertEquals(InvoiceTheme.CLASSIC, vm.invoiceTheme.value)
        assertEquals(PaperWidth.WIDTH_58MM, vm.paperWidth.value)
        assertEquals(PrintTypography.STANDARD, vm.typography.value)
        assertNotNull(vm.companyProfile.value)
        assertEquals("Vyavsa Hardware & Tools", vm.companyProfile.value?.name)
    }

    @Test
    fun testContentSettingsTogglesUpdateStateAndPersist() = runTest {
        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // Toggle Logo
        assertTrue(vm.contentSettings.value.showLogo)
        vm.toggleShowLogo(false)
        assertFalse(vm.contentSettings.value.showLogo)
        assertFalse(fakePrefs.getBoolean("setting_show_logo", true))

        // Toggle Business Address
        assertTrue(vm.contentSettings.value.showBusinessAddress)
        vm.toggleShowBusinessAddress(false)
        assertFalse(vm.contentSettings.value.showBusinessAddress)
        assertFalse(fakePrefs.getBoolean("setting_show_address", true))

        // Toggle Business Phone
        assertTrue(vm.contentSettings.value.showBusinessPhone)
        vm.toggleShowBusinessPhone(false)
        assertFalse(vm.contentSettings.value.showBusinessPhone)

        // Toggle Notes
        assertTrue(vm.contentSettings.value.showNotes)
        vm.toggleShowNotes(false)
        assertFalse(vm.contentSettings.value.showNotes)
        assertFalse(fakePrefs.getBoolean("setting_show_notes", true))

        // Toggle Footer Note
        assertTrue(vm.contentSettings.value.showFooterNote)
        vm.toggleShowFooterNote(false)
        assertFalse(vm.contentSettings.value.showFooterNote)
        assertFalse(fakePrefs.getBoolean("setting_show_footer", true))
    }

    @Test
    fun testThemeAndPaperWidthCustomization() = runTest {
        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        vm.setInvoiceTheme(InvoiceTheme.MODERN_MINIMAL)
        assertEquals(InvoiceTheme.MODERN_MINIMAL, vm.invoiceTheme.value)

        vm.setPaperWidth(PaperWidth.WIDTH_80MM)
        assertEquals(PaperWidth.WIDTH_80MM, vm.paperWidth.value)

        vm.setTypography(PrintTypography.COMPACT)
        assertEquals(PrintTypography.COMPACT, vm.typography.value)
    }

    @Test
    fun testSampleInvoiceDataIncludesProfileAndFooter() = runTest {
        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val sample = vm.getSampleInvoiceData()
        assertEquals("Vyavsa Hardware & Tools", sample.businessName)
        assertEquals("9876543210", sample.businessPhone)
        assertEquals("Thank you for shopping with us!", sample.footerHeading)
        assertEquals("Goods once sold can only be exchanged within 7 days.", sample.footerParagraph)
    }

    @Test
    fun testReceiptPreviewTextReflectsCustomization() = runTest {
        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val defaultPreview = vm.getReceiptPreviewText()
        assertTrue(defaultPreview.contains("Thank you for shopping with us!"))
        assertTrue(defaultPreview.contains("Shop 12, Market Complex"))

        // Disable footer note in content settings
        vm.toggleShowFooterNote(false)
        val previewWithoutFooter = vm.getReceiptPreviewText()
        assertFalse(previewWithoutFooter.contains("Thank you for shopping with us!"))

        // Disable business address
        vm.toggleShowBusinessAddress(false)
        val previewWithoutAddress = vm.getReceiptPreviewText()
        assertFalse(previewWithoutAddress.contains("Shop 12, Market Complex"))
    }

    @Test
    fun testUpdateInvoiceFooterSuccess() = runTest {
        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        var mutationReceived: DataMutation? = null
        val job = backgroundScope.launch {
            refreshCoordinator.refreshEvents.collect {
                mutationReceived = it
            }
        }

        fakeDashboardRepo.profile = fakeDashboardRepo.profile?.copy(
            invoiceFooterHeading = "Updated Terms",
            invoiceFooterParagraph = "No returns without bill."
        )

        vm.updateInvoiceFooter("Updated Terms", "No returns without bill.")
        testDispatcher.scheduler.advanceUntilIdle()

        var attempts = 0
        while (vm.footerUpdateState.value !is UiState.Success && attempts < 50) {
            Thread.sleep(20)
            testDispatcher.scheduler.advanceUntilIdle()
            attempts++
        }

        assertTrue("Expected Success, got ${vm.footerUpdateState.value}", vm.footerUpdateState.value is UiState.Success)
        assertEquals("Updated Terms", vm.companyProfile.value?.invoiceFooterHeading)
        assertEquals("No returns without bill.", vm.companyProfile.value?.invoiceFooterParagraph)
        assertEquals(DataMutation.PROFILE, mutationReceived)

        job.cancel()
    }

    @Test
    fun testUpdateInvoiceFooterValidationErrors() = runTest {
        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // Heading exceeding 255 chars
        val longHeading = "A".repeat(256)
        vm.updateInvoiceFooter(longHeading, "Normal paragraph")
        assertTrue(vm.footerUpdateState.value is UiState.Error)
        assertEquals("Footer heading cannot exceed 255 characters", (vm.footerUpdateState.value as UiState.Error).message)

        // Paragraph exceeding 1000 chars
        val longParagraph = "B".repeat(1001)
        vm.updateInvoiceFooter("Normal Heading", longParagraph)
        assertTrue(vm.footerUpdateState.value is UiState.Error)
        assertEquals("Footer paragraph cannot exceed 1000 characters", (vm.footerUpdateState.value as UiState.Error).message)
    }

    @Test
    fun testRunTestPrintNoPrinterSelectedReturnsError() = runTest {
        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        vm.runTestPrint()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(PrintJobStatus.ERROR, vm.testPrintState.value.status)
        assertTrue(vm.testPrintState.value.message?.contains("No printer selected") == true)
    }

    @Test
    fun testRunTestPrintSuccessWithPrinter() = runTest {
        val vm = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val printer = PrinterDevice("POS-Thermal-Test", "00:11:22:33:44:55", connectionType = PrinterConnectionType.BLUETOOTH)
        vm.selectPrinter(printer)

        vm.runTestPrint()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(PrintJobStatus.SUCCESS, vm.testPrintState.value.status)
        assertEquals(1, fakePrinterManager.printedPayloads.size)
    }
}
