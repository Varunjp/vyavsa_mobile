package com.vyavsa.app.feature.transactions

import com.vyavsa.app.core.model.CounterSale
import com.vyavsa.app.core.model.LineSale
import com.vyavsa.app.core.printer.FakeThermalPrinterManager
import com.vyavsa.app.core.printer.InvoiceContentSettings
import com.vyavsa.app.core.printer.InvoiceTheme
import com.vyavsa.app.core.printer.PaperWidth
import com.vyavsa.app.core.printer.PrintJobStatus
import com.vyavsa.app.core.printer.PrintTypography
import com.vyavsa.app.core.printer.PrinterConnectionType
import com.vyavsa.app.core.printer.PrinterDevice
import com.vyavsa.app.core.printer.PrinterPreferences
import com.vyavsa.app.core.settings.FakeSharedPreferences
import com.vyavsa.app.core.settings.SettingsPreferences
import com.vyavsa.app.core.settings.TestContext
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionsViewModelDirectPrintTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepo: FakeTransactionsRepository
    private lateinit var fakeBankRepo: FakeBankRepository
    private lateinit var refreshCoordinator: DataRefreshCoordinator
    private lateinit var fakePrinterManager: FakeThermalPrinterManager
    private lateinit var fakePrefs: FakeSharedPreferences
    private lateinit var testContext: TestContext
    private lateinit var printerPrefs: PrinterPreferences
    private lateinit var settingsPrefs: SettingsPreferences
    private lateinit var viewModel: TransactionsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepo = FakeTransactionsRepository()
        fakeBankRepo = FakeBankRepository()
        refreshCoordinator = DataRefreshCoordinator()
        fakePrinterManager = FakeThermalPrinterManager()

        fakePrefs = FakeSharedPreferences()
        testContext = TestContext(fakePrefs)
        printerPrefs = PrinterPreferences(testContext)
        settingsPrefs = SettingsPreferences(testContext, printerPrefs)

        viewModel = TransactionsViewModel(
            repository = fakeRepo,
            bankRepository = fakeBankRepo,
            refreshCoordinator = refreshCoordinator,
            tokenManager = null,
            dashboardRepository = null,
            printerManager = fakePrinterManager,
            printerPreferences = printerPrefs,
            settingsPreferences = settingsPrefs
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testPrintLineSaleUsesSavedPreferencesDirectly() = runTest {
        // Configure saved printer & settings preferences
        val printer = PrinterDevice("Saved-POS-80", "AA:BB:CC:DD:EE:01", connectionType = PrinterConnectionType.BLUETOOTH)
        fakePrinterManager.pairedPrintersList = listOf(printer)
        viewModel.selectPrinter(printer)
        printerPrefs.savePaperWidth(PaperWidth.WIDTH_80MM)

        settingsPrefs.saveInvoiceTheme(InvoiceTheme.MODERN_MINIMAL)
        settingsPrefs.saveTypography(PrintTypography.COMPACT)
        settingsPrefs.saveContentSettings(
            InvoiceContentSettings(
                showBusinessAddress = true,
                showFooterNote = true,
                showCustomerDetails = true
            )
        )

        val testLineSale = LineSale(
            id = "sale-101",
            customerId = "cust-1",
            customerName = "Ramesh Kumar",
            totalAmount = "2500.00",
            totalCashIn = "2500.00",
            createdAt = "2026-10-21T10:00:00Z"
        )

        viewModel.printLineSale(testLineSale, testContext)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(PrintJobStatus.SUCCESS, viewModel.printJobState.value.status)
        assertTrue(viewModel.printJobState.value.message?.contains("Saved-POS-80") == true)
        assertEquals(1, fakePrinterManager.printedPayloads.size)

        // Verify ESC/POS payload format
        val bytes = fakePrinterManager.printedPayloads.first()
        assertEquals(0x1B.toByte(), bytes[0])
        assertEquals(0x40.toByte(), bytes[1])
    }

    @Test
    fun testPrintCounterSaleUsesSavedPreferencesDirectly() = runTest {
        val printer = PrinterDevice("USB-Counter-Printer", "/dev/bus/usb/001/003", connectionType = PrinterConnectionType.USB)
        viewModel.selectPrinter(printer)
        printerPrefs.saveConnectionType(PrinterConnectionType.USB)
        printerPrefs.savePaperWidth(PaperWidth.WIDTH_58MM)

        settingsPrefs.saveInvoiceTheme(InvoiceTheme.CLASSIC)

        val testCounterSale = CounterSale(
            id = "counter-202",
            item = "Stationery Bundle",
            price = "450.00",
            totalAmount = "450.00",
            createdAt = "2026-10-21T11:00:00Z"
        )

        viewModel.printCounterSale(testCounterSale, testContext)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(PrintJobStatus.SUCCESS, viewModel.printJobState.value.status)
        assertEquals(1, fakePrinterManager.printedPayloads.size)
    }

    @Test
    fun testDirectPrintPermissionHandling() = runTest {
        val printer = PrinterDevice("BT-Printer", "00:11:22:33:44:55", connectionType = PrinterConnectionType.BLUETOOTH)
        printerPrefs.saveSelectedPrinter(printer)
        fakePrinterManager.hasPermission = false

        val testLineSale = LineSale(
            id = "sale-103",
            totalAmount = "100.00",
            createdAt = "2026-10-21T10:00:00Z"
        )

        var requestedPerms: Array<String>? = null
        viewModel.printLineSale(testLineSale, testContext) { perms ->
            requestedPerms = perms
        }
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(requestedPerms)
        assertEquals(PrintJobStatus.ERROR, viewModel.printJobState.value.status)
        assertTrue(viewModel.printJobState.value.message?.contains("permission is required") == true)
        assertEquals(0, fakePrinterManager.printedPayloads.size)
    }
}
