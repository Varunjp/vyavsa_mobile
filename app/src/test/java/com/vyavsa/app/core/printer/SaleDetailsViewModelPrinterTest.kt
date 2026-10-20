package com.vyavsa.app.core.printer

import androidx.lifecycle.SavedStateHandle
import com.vyavsa.app.core.common.Resource
import com.vyavsa.app.core.common.UiState
import com.vyavsa.app.core.model.FinancialMetrics
import com.vyavsa.app.core.model.LineSale
import com.vyavsa.app.core.model.TenantProfile
import com.vyavsa.app.core.model.TenantSubscription
import com.vyavsa.app.core.sync.DataRefreshCoordinator
import com.vyavsa.app.feature.bank.FakeBankRepository
import com.vyavsa.app.feature.transactions.FakeTransactionsRepository
import com.vyavsa.app.feature.transactions.SaleDetailData
import com.vyavsa.app.feature.transactions.SaleDetailsViewModel
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeThermalPrinterManager(
    var isSupported: Boolean = true,
    var isEnabled: Boolean = true,
    var hasPermission: Boolean = true,
    var shouldFailPrint: Boolean = false,
    var failureMessage: String = "Socket write failed"
) : ThermalPrinterManager {

    val printedPayloads = mutableListOf<ByteArray>()

    override fun isBluetoothSupported(): Boolean = isSupported
    override fun isBluetoothEnabled(): Boolean = isEnabled
    override fun hasBluetoothPermission(): Boolean = hasPermission

    override fun getPairedPrinters(): List<PrinterDevice> = listOf(
        PrinterDevice(name = "POS-58-Thermal", address = "00:11:22:33:44:55", connectionType = PrinterConnectionType.BLUETOOTH),
        PrinterDevice(name = "Epson-TM-T88", address = "AA:BB:CC:DD:EE:FF", connectionType = PrinterConnectionType.BLUETOOTH)
    )

    override fun getConnectedUsbPrinters(): List<PrinterDevice> = listOf(
        PrinterDevice(name = "USB-POS-Thermal", address = "/dev/bus/usb/001/002", connectionType = PrinterConnectionType.USB)
    )

    override suspend fun printReceipt(printer: PrinterDevice, receiptBytes: ByteArray): Result<Unit> {
        if (printer.connectionType == PrinterConnectionType.BLUETOOTH) {
            if (!isEnabled) return Result.failure(IllegalStateException("Bluetooth is disabled."))
            if (!hasPermission) return Result.failure(SecurityException("Bluetooth permission denied."))
        }
        if (shouldFailPrint) return Result.failure(java.io.IOException(failureMessage))

        printedPayloads.add(receiptBytes)
        return Result.success(Unit)
    }

    override suspend fun printReceipt(printerAddress: String, receiptBytes: ByteArray): Result<Unit> {
        return printReceipt(PrinterDevice(name = "Generic", address = printerAddress), receiptBytes)
    }
}

class FakeDashboardRepository(
    var profile: TenantProfile? = null
) : com.vyavsa.app.feature.dashboard.DashboardRepository {
    override fun getDashboardMetrics(): Flow<Resource<FinancialMetrics>> = flow {}
    override fun getProfile(): Flow<Resource<TenantProfile>> = flow {
        val p = profile
        if (p != null) {
            emit(Resource.Success(p))
        }
    }
    override fun getSubscription(): Flow<Resource<TenantSubscription>> = flow {}
}

@OptIn(ExperimentalCoroutinesApi::class)
class SaleDetailsViewModelPrinterTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepo: FakeTransactionsRepository
    private lateinit var fakeBankRepo: FakeBankRepository
    private lateinit var refreshCoordinator: DataRefreshCoordinator
    private lateinit var fakePrinterManager: FakeThermalPrinterManager

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepo = FakeTransactionsRepository()
        fakeBankRepo = FakeBankRepository()
        refreshCoordinator = DataRefreshCoordinator()
        fakePrinterManager = FakeThermalPrinterManager()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testPrinterSelectionAndPaperWidth() = runTest {
        val handle = SavedStateHandle(mapOf("saleType" to "line", "saleId" to "sale-1"))
        val viewModel = SaleDetailsViewModel(
            repository = fakeRepo,
            bankRepository = fakeBankRepo,
            refreshCoordinator = refreshCoordinator,
            dashboardRepository = null,
            printerManager = fakePrinterManager,
            printerPreferences = null,
            savedStateHandle = handle
        )
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.loadPairedPrinters()
        assertEquals(2, viewModel.pairedPrinters.value.size)

        // Select second printer
        val secondPrinter = viewModel.pairedPrinters.value[1]
        viewModel.selectPrinter(secondPrinter)
        assertEquals(secondPrinter, viewModel.selectedPrinter.value)

        // Switch paper width
        viewModel.setPaperWidth(PaperWidth.WIDTH_80MM)
        assertEquals(PaperWidth.WIDTH_80MM, viewModel.paperWidth.value)
    }

    @Test
    fun testPrintInvoiceSuccess() = runTest {
        val handle = SavedStateHandle(mapOf("saleType" to "line", "saleId" to "sale-1"))
        val viewModel = SaleDetailsViewModel(
            repository = fakeRepo,
            bankRepository = fakeBankRepo,
            refreshCoordinator = refreshCoordinator,
            dashboardRepository = null,
            printerManager = fakePrinterManager,
            printerPreferences = null,
            savedStateHandle = handle
        )
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.loadPairedPrinters()
        viewModel.selectPrinter(viewModel.pairedPrinters.value.first())

        viewModel.printInvoice()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(PrintJobStatus.SUCCESS, viewModel.printJobState.value.status)
        assertEquals(1, fakePrinterManager.printedPayloads.size)

        // Verify ESC/POS header command in printed bytes
        val bytes = fakePrinterManager.printedPayloads.first()
        assertEquals(0x1B.toByte(), bytes[0])
        assertEquals(0x40.toByte(), bytes[1])
    }

    @Test
    fun testPrintErrorWhenBluetoothDisabled() = runTest {
        fakePrinterManager.isEnabled = false

        val handle = SavedStateHandle(mapOf("saleType" to "line", "saleId" to "sale-1"))
        val viewModel = SaleDetailsViewModel(
            repository = fakeRepo,
            bankRepository = fakeBankRepo,
            refreshCoordinator = refreshCoordinator,
            dashboardRepository = null,
            printerManager = fakePrinterManager,
            printerPreferences = null,
            savedStateHandle = handle
        )
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.selectPrinter(PrinterDevice("POS-Printer", "00:11:22:33:44:55"))
        viewModel.printInvoice()

        assertEquals(PrintJobStatus.ERROR, viewModel.printJobState.value.status)
        assertTrue(viewModel.printJobState.value.message?.contains("Bluetooth is disabled") == true)
        assertEquals(0, fakePrinterManager.printedPayloads.size)
    }

    @Test
    fun testPrintErrorWhenPermissionDenied() = runTest {
        fakePrinterManager.hasPermission = false

        val handle = SavedStateHandle(mapOf("saleType" to "line", "saleId" to "sale-1"))
        val viewModel = SaleDetailsViewModel(
            repository = fakeRepo,
            bankRepository = fakeBankRepo,
            refreshCoordinator = refreshCoordinator,
            dashboardRepository = null,
            printerManager = fakePrinterManager,
            printerPreferences = null,
            savedStateHandle = handle
        )
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.selectPrinter(PrinterDevice("POS-Printer", "00:11:22:33:44:55"))
        viewModel.printInvoice()

        assertEquals(PrintJobStatus.ERROR, viewModel.printJobState.value.status)
        assertTrue(viewModel.printJobState.value.message?.contains("permission is required") == true)
        assertEquals(0, fakePrinterManager.printedPayloads.size)
    }

    @Test
    fun testPrintErrorWhenHardwareDisconnects() = runTest {
        fakePrinterManager.shouldFailPrint = true
        fakePrinterManager.failureMessage = "Device connection terminated"

        val handle = SavedStateHandle(mapOf("saleType" to "line", "saleId" to "sale-1"))
        val viewModel = SaleDetailsViewModel(
            repository = fakeRepo,
            bankRepository = fakeBankRepo,
            refreshCoordinator = refreshCoordinator,
            dashboardRepository = null,
            printerManager = fakePrinterManager,
            printerPreferences = null,
            savedStateHandle = handle
        )
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.selectPrinter(PrinterDevice("POS-Printer", "00:11:22:33:44:55"))
        viewModel.printInvoice()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(PrintJobStatus.ERROR, viewModel.printJobState.value.status)
        assertEquals("Device connection terminated", viewModel.printJobState.value.message)

        // Check that sale details remain completely untouched
        assertTrue(viewModel.detailState.value is UiState.Success)
    }

    @Test
    fun testReceiptPreviewTextGeneration() = runTest {
        val handle = SavedStateHandle(mapOf("saleType" to "line", "saleId" to "sale-1"))
        val viewModel = SaleDetailsViewModel(
            repository = fakeRepo,
            bankRepository = fakeBankRepo,
            refreshCoordinator = refreshCoordinator,
            dashboardRepository = null,
            printerManager = fakePrinterManager,
            printerPreferences = null,
            savedStateHandle = handle
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val preview = viewModel.getReceiptPreviewText()
        assertNotNull(preview)
        assertTrue(preview.contains("Sharma Stores"))
        assertTrue(preview.contains("1,500.00"))
    }

    @Test
    fun testPrintViaUsbPrinterSuccess() = runTest {
        val handle = SavedStateHandle(mapOf("saleType" to "line", "saleId" to "sale-1"))
        val viewModel = SaleDetailsViewModel(
            repository = fakeRepo,
            bankRepository = fakeBankRepo,
            refreshCoordinator = refreshCoordinator,
            dashboardRepository = null,
            printerManager = fakePrinterManager,
            printerPreferences = null,
            savedStateHandle = handle
        )
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.setConnectionType(PrinterConnectionType.USB)
        testDispatcher.scheduler.advanceUntilIdle()

        val selected = viewModel.selectedPrinter.value
        assertNotNull(selected)
        assertEquals(PrinterConnectionType.USB, selected?.connectionType)
        assertEquals("/dev/bus/usb/001/002", selected?.address)

        viewModel.printInvoice()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(PrintJobStatus.SUCCESS, viewModel.printJobState.value.status)
        assertEquals(1, fakePrinterManager.printedPayloads.size)
    }

    @Test
    fun testPrintViaNetworkPrinterSuccess() = runTest {
        val handle = SavedStateHandle(mapOf("saleType" to "line", "saleId" to "sale-1"))
        val viewModel = SaleDetailsViewModel(
            repository = fakeRepo,
            bankRepository = fakeBankRepo,
            refreshCoordinator = refreshCoordinator,
            dashboardRepository = null,
            printerManager = fakePrinterManager,
            printerPreferences = null,
            savedStateHandle = handle
        )
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.setConnectionType(PrinterConnectionType.NETWORK)
        viewModel.setNetworkPrinterIp("192.168.1.150:9100")
        testDispatcher.scheduler.advanceUntilIdle()

        val selected = viewModel.selectedPrinter.value
        assertNotNull(selected)
        assertEquals(PrinterConnectionType.NETWORK, selected?.connectionType)
        assertEquals("192.168.1.150:9100", selected?.address)

        viewModel.printInvoice()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(PrintJobStatus.SUCCESS, viewModel.printJobState.value.status)
        assertEquals(1, fakePrinterManager.printedPayloads.size)
    }

    @Test
    fun testNetworkPrinterBlankAddressError() = runTest {
        val handle = SavedStateHandle(mapOf("saleType" to "line", "saleId" to "sale-1"))
        val viewModel = SaleDetailsViewModel(
            repository = fakeRepo,
            bankRepository = fakeBankRepo,
            refreshCoordinator = refreshCoordinator,
            dashboardRepository = null,
            printerManager = fakePrinterManager,
            printerPreferences = null,
            savedStateHandle = handle
        )
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.setConnectionType(PrinterConnectionType.NETWORK)
        viewModel.setNetworkPrinterIp("")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.printInvoice()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(PrintJobStatus.ERROR, viewModel.printJobState.value.status)
        assertEquals("Please enter a valid IP address for the network printer", viewModel.printJobState.value.message)
    }

    @Test
    fun testLogoIncludedInInvoicePrintData() = runTest {
        val handle = SavedStateHandle(mapOf("saleType" to "line", "saleId" to "sale-1"))
        val fakeDashboardRepo = FakeDashboardRepository(
            profile = com.vyavsa.app.core.model.TenantProfile(
                name = "Ramesh General Store",
                logoUrl = "/uploads/logos/store_logo.png"
            )
        )

        val viewModel = SaleDetailsViewModel(
            repository = fakeRepo,
            bankRepository = fakeBankRepo,
            refreshCoordinator = refreshCoordinator,
            dashboardRepository = fakeDashboardRepo,
            printerManager = fakePrinterManager,
            printerPreferences = null,
            savedStateHandle = handle
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val printData = viewModel.getInvoicePrintData()
        assertNotNull(printData)
        assertTrue(printData?.hasLogo == true)
        assertEquals("/uploads/logos/store_logo.png", printData?.logoUrl)

        val preview = viewModel.getReceiptPreviewText()
        assertTrue(preview.contains("[ COMPANY LOGO ]"))
    }
}
