package com.vyavsa.app.feature.transactions

import com.vyavsa.app.core.common.UiState
import com.vyavsa.app.core.model.BankPaymentSplit
import com.vyavsa.app.core.model.LineSale
import com.vyavsa.app.core.model.SaleItemRequest
import com.vyavsa.app.core.printer.FakeThermalPrinterManager
import com.vyavsa.app.core.printer.InvoiceContentSettings
import com.vyavsa.app.core.printer.InvoiceTheme
import com.vyavsa.app.core.printer.PaperWidth
import com.vyavsa.app.core.printer.PrintJobStatus
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionsViewModelRecordAndPrintTest {

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
    fun testRecordLineSaleSavesSaleWithoutPrinting() = runTest {
        viewModel.createLineSale(
            customerId = "cust-1",
            totalAmount = "1200.00",
            cashIn = "1200.00",
            route = "Downtown",
            salesman = "John"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // 1. Sale is successfully recorded in repository
        assertEquals(1, fakeRepo.createLineSaleCallCount)
        val lastReq = fakeRepo.lastCreateLineSaleRequest
        assertNotNull(lastReq)
        assertEquals("cust-1", lastReq?.customerId)
        assertEquals("1200.00", lastReq?.totalAmount)
        assertEquals("1200.00", lastReq?.cashAmount)

        // 2. UI state is Success
        assertTrue(viewModel.createActionState.value is UiState.Success)

        // 3. Printing was NOT triggered
        assertTrue(fakePrinterManager.printedPayloads.isEmpty())
        assertNull(viewModel.activePrintingSaleId.value)
    }

    @Test
    fun testRecordAndPrintValidationFailureDoesNotCallApiOrPrint() = runTest {
        var callbackInvoked = false
        viewModel.recordAndPrintLineSale(
            customerId = "", // Invalid: customerId required
            totalAmount = "1000.00",
            cashIn = "1000.00",
            context = testContext,
            onPrintResult = { _, _, _ -> callbackInvoked = true }
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // Validation fails immediately
        assertTrue(viewModel.createActionState.value is UiState.Error)
        assertEquals("Customer and total amount are required", (viewModel.createActionState.value as UiState.Error).message)

        // Never hits API or Printer
        assertEquals(0, fakeRepo.createLineSaleCallCount)
        assertTrue(fakePrinterManager.printedPayloads.isEmpty())
        assertFalse(callbackInvoked)
    }

    @Test
    fun testRecordAndPrintSavesSaleAndPrintsMatchingInvoice() = runTest {
        val printer = PrinterDevice("Thermal-POS-80", "11:22:33:44:55:66", connectionType = PrinterConnectionType.BLUETOOTH)
        fakePrinterManager.pairedPrintersList = listOf(printer)
        viewModel.selectPrinter(printer)
        printerPrefs.saveSelectedPrinter(printer)
        printerPrefs.savePaperWidth(PaperWidth.WIDTH_80MM)

        val items = listOf(
            SaleItemRequest("prod-1", "2.00", "500.00")
        )

        var returnedSale: LineSale? = null
        var printSuccess: Boolean? = null
        var printMsg: String? = null

        viewModel.recordAndPrintLineSale(
            customerId = "cust-1",
            totalAmount = "1000.00",
            cashIn = "1000.00",
            route = "North Route",
            salesman = "Varun",
            items = items,
            customerName = "Ramesh Kumar",
            context = testContext,
            onPrintResult = { sale, success, msg ->
                returnedSale = sale
                printSuccess = success
                printMsg = msg
            }
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // 1. Saved sale in repository
        assertEquals(1, fakeRepo.createLineSaleCallCount)
        assertNotNull(returnedSale)
        assertEquals("cust-1", returnedSale?.customerId)
        assertEquals("1000.00", returnedSale?.totalAmount)
        assertEquals("Ramesh Kumar", returnedSale?.customerName)

        // 2. Printing was triggered and completed successfully
        assertEquals(true, printSuccess)
        assertTrue(printMsg?.contains("Printed successfully") == true)
        assertEquals(1, fakePrinterManager.printedPayloads.size)
        assertEquals(PrintJobStatus.SUCCESS, viewModel.printJobState.value.status)

        // 3. ESC/POS payload format check
        val rawBytes = fakePrinterManager.printedPayloads.first()
        assertTrue(rawBytes.isNotEmpty())
        assertEquals(0x1B.toByte(), rawBytes[0]) // ESC
        assertEquals(0x40.toByte(), rawBytes[1]) // @
    }

    @Test
    fun testRecordAndPrintApiFailureNeverTriggersPrint() = runTest {
        fakeRepo.shouldFailCreateLineSale = true
        fakeRepo.failureMessage = "Database connection timeout"

        var callbackInvoked = false
        viewModel.recordAndPrintLineSale(
            customerId = "cust-1",
            totalAmount = "800.00",
            cashIn = "800.00",
            context = testContext,
            onPrintResult = { _, _, _ -> callbackInvoked = true }
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // API call attempted
        assertEquals(1, fakeRepo.createLineSaleCallCount)

        // UI state captured error
        assertTrue(viewModel.createActionState.value is UiState.Error)
        assertEquals("Database connection timeout", (viewModel.createActionState.value as UiState.Error).message)

        // PRINT MUST NOT BE TRIGGERED
        assertTrue(fakePrinterManager.printedPayloads.isEmpty())
        assertFalse(callbackInvoked)
    }

    @Test
    fun testRecordAndPrintPrinterFailurePreservesSavedSaleWithoutDuplicateSave() = runTest {
        val printer = PrinterDevice("Offline-POS", "AA:BB:CC:DD:EE:99", connectionType = PrinterConnectionType.BLUETOOTH)
        fakePrinterManager.pairedPrintersList = listOf(printer)
        viewModel.selectPrinter(printer)
        printerPrefs.saveSelectedPrinter(printer)

        // Simulate printer hardware communication error / disconnect
        fakePrinterManager.shouldFailPrint = true
        fakePrinterManager.failureMessage = "Printer connection reset by peer"

        var savedSaleResult: LineSale? = null
        var isSuccessResult: Boolean? = null
        var errorMessageResult: String? = null

        viewModel.recordAndPrintLineSale(
            customerId = "cust-1",
            totalAmount = "600.00",
            cashIn = "600.00",
            context = testContext,
            onPrintResult = { sale, isSuccess, errMsg ->
                savedSaleResult = sale
                isSuccessResult = isSuccess
                errorMessageResult = errMsg
            }
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // 1. Sale was saved in repository exactly once
        assertEquals(1, fakeRepo.createLineSaleCallCount)
        assertNotNull(savedSaleResult)
        assertEquals("cust-1", savedSaleResult?.customerId)

        // 2. Printing failed and notified callback
        assertEquals(false, isSuccessResult)
        assertEquals("Printer connection reset by peer", errorMessageResult)
        assertEquals(PrintJobStatus.ERROR, viewModel.printJobState.value.status)

        // 3. Now verify RETRY PRINT:
        // In the UI, the user clicks "Retry Print" which invokes printLineSaleWithResult(savedSale)
        // It must NOT record another sale!
        fakePrinterManager.shouldFailPrint = false // printer reconnected

        var retrySuccess: Boolean? = null
        viewModel.printLineSaleWithResult(
            sale = savedSaleResult!!,
            context = testContext,
            onResult = { ok, _ -> retrySuccess = ok }
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // Verifications:
        assertEquals(true, retrySuccess)
        assertEquals(1, fakePrinterManager.printedPayloads.size)
        // CRITICAL INVARIANT: Repository call count MUST STILL BE 1 (No duplicate sale!)
        assertEquals(1, fakeRepo.createLineSaleCallCount)
    }

    @Test
    fun testRapidClicksPreventDuplicateSubmissions() = runTest {
        fakeRepo.delayResponse = true

        // First click
        viewModel.recordAndPrintLineSale(
            customerId = "cust-1",
            totalAmount = "500.00",
            cashIn = "500.00",
            context = testContext
        )
        testDispatcher.scheduler.runCurrent()

        assertTrue(viewModel.createActionState.value is UiState.Loading)
        assertEquals(1, fakeRepo.createLineSaleCallCount)

        // Rapid second click on Record & Print while loading
        viewModel.recordAndPrintLineSale(
            customerId = "cust-1",
            totalAmount = "500.00",
            cashIn = "500.00",
            context = testContext
        )
        testDispatcher.scheduler.runCurrent()

        // Rapid third click on Record while loading
        viewModel.createLineSale(
            customerId = "cust-1",
            totalAmount = "500.00",
            cashIn = "500.00"
        )
        testDispatcher.scheduler.runCurrent()

        // Call count MUST remain 1
        assertEquals(1, fakeRepo.createLineSaleCallCount)
    }

    @Test
    fun testBluetoothDisabledReportsErrorAndPreservesSavedSale() = runTest {
        val printer = PrinterDevice("BT-Thermal", "00:11:22:33:44:55", connectionType = PrinterConnectionType.BLUETOOTH)
        fakePrinterManager.pairedPrintersList = listOf(printer)
        viewModel.selectPrinter(printer)
        printerPrefs.saveSelectedPrinter(printer)

        // Bluetooth turned off on device
        fakePrinterManager.isEnabled = false

        var savedSaleResult: LineSale? = null
        var isSuccessResult: Boolean? = null
        var errorMsgResult: String? = null

        viewModel.recordAndPrintLineSale(
            customerId = "cust-1",
            totalAmount = "350.00",
            cashIn = "350.00",
            context = testContext,
            onPrintResult = { sale, isSuccess, err ->
                savedSaleResult = sale
                isSuccessResult = isSuccess
                errorMsgResult = err
            }
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // 1. Sale was saved in database
        assertEquals(1, fakeRepo.createLineSaleCallCount)
        assertNotNull(savedSaleResult)

        // 2. Printing failed due to Bluetooth disabled
        assertEquals(false, isSuccessResult)
        assertTrue(errorMsgResult?.contains("Bluetooth is turned off") == true)
        assertEquals(PrintJobStatus.ERROR, viewModel.printJobState.value.status)
        assertTrue(fakePrinterManager.printedPayloads.isEmpty())
    }
}
