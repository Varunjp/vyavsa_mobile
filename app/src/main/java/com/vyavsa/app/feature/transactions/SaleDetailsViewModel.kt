package com.vyavsa.app.feature.transactions

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vyavsa.app.core.common.Resource
import com.vyavsa.app.core.common.UiState
import com.vyavsa.app.core.model.CounterSale
import com.vyavsa.app.core.model.LineSale
import com.vyavsa.app.core.model.RecordSalePaymentRequest
import com.vyavsa.app.core.model.TenantBank
import com.vyavsa.app.core.model.TenantProfile
import com.vyavsa.app.core.sync.DataMutation
import com.vyavsa.app.core.sync.DataRefreshCoordinator
import com.vyavsa.app.feature.bank.BankRepository
import com.vyavsa.app.feature.dashboard.DashboardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.vyavsa.app.core.common.FormatUtils
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.vyavsa.app.core.designsystem.LogoMemoryCache
import com.vyavsa.app.core.printer.InvoiceContentSettings
import com.vyavsa.app.core.printer.InvoiceDataMapper
import com.vyavsa.app.core.printer.InvoicePrintData
import com.vyavsa.app.core.printer.InvoiceTheme
import com.vyavsa.app.core.printer.PaperWidth
import com.vyavsa.app.core.printer.PrintItem
import com.vyavsa.app.core.printer.PrintJobState
import com.vyavsa.app.core.printer.PrintJobStatus
import com.vyavsa.app.core.printer.PrintTypography
import com.vyavsa.app.core.printer.PrinterConnectionType
import com.vyavsa.app.core.printer.PrinterDevice
import com.vyavsa.app.core.printer.PrinterPreferences
import com.vyavsa.app.core.printer.ThermalInvoiceFormatter
import com.vyavsa.app.core.printer.ThermalPrinterManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject

sealed class SaleDetailData {
    data class Line(val sale: LineSale) : SaleDetailData()
    data class Counter(val sale: CounterSale) : SaleDetailData()
}

@HiltViewModel
class SaleDetailsViewModel @Inject constructor(
    private val repository: TransactionsRepository,
    private val bankRepository: BankRepository,
    private val refreshCoordinator: DataRefreshCoordinator,
    private val dashboardRepository: DashboardRepository? = null,
    val printerManager: ThermalPrinterManager? = null,
    val printerPreferences: PrinterPreferences? = null,
    val settingsPreferences: com.vyavsa.app.core.settings.SettingsPreferences? = null,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    constructor(
        repository: TransactionsRepository,
        bankRepository: BankRepository,
        refreshCoordinator: DataRefreshCoordinator,
        dashboardRepository: DashboardRepository?,
        printerManager: ThermalPrinterManager?,
        printerPreferences: PrinterPreferences?,
        savedStateHandle: SavedStateHandle
    ) : this(repository, bankRepository, refreshCoordinator, dashboardRepository, printerManager, printerPreferences, null, savedStateHandle)

    constructor(
        repository: TransactionsRepository,
        bankRepository: BankRepository,
        refreshCoordinator: DataRefreshCoordinator,
        savedStateHandle: SavedStateHandle
    ) : this(repository, bankRepository, refreshCoordinator, null, null, null, null, savedStateHandle)

    val saleType: String = savedStateHandle["saleType"] ?: "line"
    val saleId: String = savedStateHandle["saleId"] ?: ""

    private val _detailState = MutableStateFlow<UiState<SaleDetailData>>(UiState.Idle)
    val detailState: StateFlow<UiState<SaleDetailData>> = _detailState.asStateFlow()

    private val _paymentActionState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val paymentActionState: StateFlow<UiState<Unit>> = _paymentActionState.asStateFlow()

    private val _banksState = MutableStateFlow<List<TenantBank>>(emptyList())
    val banksState: StateFlow<List<TenantBank>> = _banksState.asStateFlow()

    private val _companyProfile = MutableStateFlow<TenantProfile?>(null)
    val companyProfile: StateFlow<TenantProfile?> = _companyProfile.asStateFlow()

    // ------------------------------------------------------------------------
    // Thermal Receipt Printing State
    // ------------------------------------------------------------------------
    private val _printJobState = MutableStateFlow(PrintJobState())
    val printJobState: StateFlow<PrintJobState> = _printJobState.asStateFlow()

    private val _pairedPrinters = MutableStateFlow<List<PrinterDevice>>(emptyList())
    val pairedPrinters: StateFlow<List<PrinterDevice>> = _pairedPrinters.asStateFlow()

    private val _usbPrinters = MutableStateFlow<List<PrinterDevice>>(emptyList())
    val usbPrinters: StateFlow<List<PrinterDevice>> = _usbPrinters.asStateFlow()

    private val _selectedConnectionType = MutableStateFlow(
        printerPreferences?.selectedConnectionType?.value ?: PrinterConnectionType.BLUETOOTH
    )
    val selectedConnectionType: StateFlow<PrinterConnectionType> = _selectedConnectionType.asStateFlow()

    private val _networkPrinterIp = MutableStateFlow(
        printerPreferences?.savedNetworkPrinterIp?.value ?: "192.168.1.100:9100"
    )
    val networkPrinterIp: StateFlow<String> = _networkPrinterIp.asStateFlow()

    private val _selectedPrinter = MutableStateFlow<PrinterDevice?>(null)
    val selectedPrinter: StateFlow<PrinterDevice?> = _selectedPrinter.asStateFlow()

    private val _paperWidth = MutableStateFlow(printerPreferences?.paperWidth?.value ?: PaperWidth.WIDTH_58MM)
    val paperWidth: StateFlow<PaperWidth> = _paperWidth.asStateFlow()

    private var cachedLogoBitmap: Bitmap? = null

    init {
        loadSaleDetails()
        loadBanks()
        loadCompanyProfile()
        loadPrinterSettings()
        observeMutations()
    }

    private fun loadCompanyProfile() {
        dashboardRepository?.let { repo ->
            viewModelScope.launch {
                repo.getProfile().collectLatest { res ->
                    if (res is Resource.Success) {
                        _companyProfile.value = res.data
                        preloadLogoBitmap(res.data.logoUrl)
                    }
                }
            }
        }
    }

    private fun preloadLogoBitmap(logoUrl: String?) {
        if (logoUrl.isNullOrBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            ensureLogoBitmapLoaded(logoUrl)
        }
    }

    suspend fun ensureLogoBitmapLoaded(logoUrl: String? = _companyProfile.value?.logoUrl) {
        if (cachedLogoBitmap != null) return
        if (logoUrl.isNullOrBlank()) return

        withContext(Dispatchers.IO) {
            try {
                val resolvedUrl = FormatUtils.resolveMediaUrl(logoUrl) ?: return@withContext
                val cached = LogoMemoryCache.get(resolvedUrl)
                if (cached != null) {
                    cachedLogoBitmap = cached.asAndroidBitmap()
                    return@withContext
                }
                val conn = java.net.URL(resolvedUrl).openConnection()
                conn.connectTimeout = 4000
                conn.readTimeout = 6000
                conn.getInputStream().use { stream ->
                    val bmp = BitmapFactory.decodeStream(stream)
                    if (bmp != null) {
                        LogoMemoryCache.put(resolvedUrl, bmp.asImageBitmap())
                        cachedLogoBitmap = bmp
                    }
                }
            } catch (_: Exception) {
                // Graceful fallback: invoice prints without logo if offline or network error
            }
        }
    }

    fun loadSaleDetails() {
        if (saleId.isBlank()) {
            _detailState.value = UiState.Error("Invalid sale identifier")
            return
        }

        viewModelScope.launch {
            if (saleType == "counter") {
                repository.getCounterSaleById(saleId).collectLatest { res ->
                    when (res) {
                        is Resource.Loading -> _detailState.value = UiState.Loading
                        is Resource.Success -> {
                            val sale = res.data
                            _detailState.value = UiState.Success(SaleDetailData.Counter(sale))
                        }
                        is Resource.Error -> _detailState.value = UiState.Error(res.message)
                    }
                }
            } else {
                repository.getLineSaleById(saleId).collectLatest { res ->
                    when (res) {
                        is Resource.Loading -> _detailState.value = UiState.Loading
                        is Resource.Success -> {
                            val sale = res.data
                            _detailState.value = UiState.Success(SaleDetailData.Line(sale))
                        }
                        is Resource.Error -> _detailState.value = UiState.Error(res.message)
                    }
                }
            }
        }
    }

    private fun loadBanks() {
        viewModelScope.launch {
            bankRepository.listBanks().collectLatest { res ->
                if (res is Resource.Success) {
                    _banksState.value = res.data
                }
            }
        }
    }

    private fun observeMutations() {
        viewModelScope.launch {
            refreshCoordinator.refreshEvents.collectLatest { mutation ->
                if (mutation == DataMutation.SALES || mutation == DataMutation.ALL) {
                    loadSaleDetails()
                }
                if (mutation == DataMutation.PROFILE || mutation == DataMutation.ALL) {
                    loadCompanyProfile()
                }
            }
        }
    }

    fun recordPayment(
        amount: String,
        paymentMethod: String,
        bankId: String? = null,
        note: String? = null
    ) {
        val amountVal = amount.toDoubleOrNull()
        if (amountVal == null || amountVal <= 0.0) {
            _paymentActionState.value = UiState.Error("Payment amount must be greater than zero")
            return
        }

        if (paymentMethod == "bank" && bankId.isNullOrBlank()) {
            _paymentActionState.value = UiState.Error("Please select a bank account for bank payment")
            return
        }

        if (_paymentActionState.value is UiState.Loading) return

        val bankObj = _banksState.value.firstOrNull { it.id == bankId }
        val req = RecordSalePaymentRequest(
            amount = String.format(Locale.US, "%.2f", amountVal),
            paymentMethod = paymentMethod,
            bankId = bankId,
            bankName = bankObj?.bankName,
            note = note
        )

        viewModelScope.launch {
            _paymentActionState.value = UiState.Loading
            if (saleType == "counter") {
                repository.recordCounterSalePayment(saleId, req).collectLatest { res ->
                    when (res) {
                        is Resource.Loading -> _paymentActionState.value = UiState.Loading
                        is Resource.Success -> {
                            _paymentActionState.value = UiState.Success(Unit)
                            loadSaleDetails()
                        }
                        is Resource.Error -> _paymentActionState.value = UiState.Error(res.message)
                    }
                }
            } else {
                repository.recordLineSalePayment(saleId, req).collectLatest { res ->
                    when (res) {
                        is Resource.Loading -> _paymentActionState.value = UiState.Loading
                        is Resource.Success -> {
                            _paymentActionState.value = UiState.Success(Unit)
                            loadSaleDetails()
                        }
                        is Resource.Error -> _paymentActionState.value = UiState.Error(res.message)
                    }
                }
            }
        }
    }

    fun clearPaymentActionState() {
        _paymentActionState.value = UiState.Idle
    }

    // ------------------------------------------------------------------------
    // Thermal Printing Actions & Helpers
    // ------------------------------------------------------------------------

    fun loadPrinterSettings() {
        printerPreferences?.let { prefs ->
            _paperWidth.value = prefs.paperWidth.value
            _selectedConnectionType.value = prefs.selectedConnectionType.value
            _networkPrinterIp.value = prefs.savedNetworkPrinterIp.value ?: "192.168.1.100:9100"
            val addr = prefs.selectedPrinterAddress.value
            val name = prefs.selectedPrinterName.value
            val connType = prefs.selectedConnectionType.value
            if (!addr.isNullOrBlank()) {
                _selectedPrinter.value = PrinterDevice(
                    name = name ?: "Saved Thermal Printer",
                    address = addr,
                    connectionType = connType
                )
            }
        }
        refreshPrinters()
    }

    fun loadPairedPrinters() {
        val printers = printerManager?.getPairedPrinters() ?: emptyList()
        _pairedPrinters.value = printers
        if (_selectedConnectionType.value == PrinterConnectionType.BLUETOOTH) {
            val savedAddr = printerPreferences?.selectedPrinterAddress?.value
            if (!savedAddr.isNullOrBlank()) {
                val matching = printers.firstOrNull { it.address == savedAddr }
                if (matching != null) {
                    _selectedPrinter.value = matching
                }
            } else if (_selectedPrinter.value == null && printers.isNotEmpty()) {
                _selectedPrinter.value = printers.first()
            }
        }
    }

    fun loadUsbPrinters() {
        val usbList = printerManager?.getConnectedUsbPrinters() ?: emptyList()
        _usbPrinters.value = usbList
        if (_selectedConnectionType.value == PrinterConnectionType.USB) {
            val savedAddr = printerPreferences?.selectedPrinterAddress?.value
            if (!savedAddr.isNullOrBlank()) {
                val matching = usbList.firstOrNull { it.address == savedAddr }
                if (matching != null) {
                    _selectedPrinter.value = matching
                }
            } else if (_selectedPrinter.value == null && usbList.isNotEmpty()) {
                _selectedPrinter.value = usbList.first()
            } else if (usbList.isEmpty() && _selectedPrinter.value?.connectionType == PrinterConnectionType.USB) {
                _selectedPrinter.value = null
            }
        }
    }

    fun setConnectionType(type: PrinterConnectionType) {
        _selectedConnectionType.value = type
        printerPreferences?.saveConnectionType(type)
        when (type) {
            PrinterConnectionType.BLUETOOTH -> {
                loadPairedPrinters()
                val current = _selectedPrinter.value
                if (current?.connectionType != PrinterConnectionType.BLUETOOTH) {
                    _selectedPrinter.value = _pairedPrinters.value.firstOrNull()
                    printerPreferences?.saveSelectedPrinter(_selectedPrinter.value)
                }
            }
            PrinterConnectionType.USB -> {
                loadUsbPrinters()
                val current = _selectedPrinter.value
                if (current?.connectionType != PrinterConnectionType.USB) {
                    _selectedPrinter.value = _usbPrinters.value.firstOrNull()
                    printerPreferences?.saveSelectedPrinter(_selectedPrinter.value)
                }
            }
            PrinterConnectionType.NETWORK -> {
                setNetworkPrinterIp(_networkPrinterIp.value)
            }
        }
    }

    fun setNetworkPrinterIp(ip: String) {
        val cleanIp = ip.trim()
        _networkPrinterIp.value = cleanIp
        printerPreferences?.saveNetworkPrinterIp(cleanIp)
        val dev = PrinterDevice(
            name = "Network Thermal Printer",
            address = cleanIp,
            isBonded = true,
            connectionType = PrinterConnectionType.NETWORK
        )
        _selectedPrinter.value = dev
        printerPreferences?.saveSelectedPrinter(dev)
    }

    fun refreshPrinters() {
        loadPairedPrinters()
        loadUsbPrinters()
        if (_selectedConnectionType.value == PrinterConnectionType.NETWORK) {
            setNetworkPrinterIp(_networkPrinterIp.value)
        }
    }

    fun selectPrinter(printer: PrinterDevice?) {
        _selectedPrinter.value = printer
        printerPreferences?.saveSelectedPrinter(printer)
    }

    fun setPaperWidth(width: PaperWidth) {
        _paperWidth.value = width
        printerPreferences?.savePaperWidth(width)
    }

    fun resetPrintJobState() {
        _printJobState.value = PrintJobState(status = PrintJobStatus.IDLE)
    }

    fun getInvoicePrintData(): InvoicePrintData? {
        val state = _detailState.value
        if (state !is UiState.Success) return null
        val profile = _companyProfile.value
        return when (val data = state.data) {
            is SaleDetailData.Line -> InvoiceDataMapper.fromLineSale(data.sale, profile, cachedLogoBitmap)
            is SaleDetailData.Counter -> InvoiceDataMapper.fromCounterSale(data.sale, profile, cachedLogoBitmap)
        }
    }

    fun getCachedLogoBitmap(): Bitmap? = cachedLogoBitmap

    fun getReceiptPreviewText(): String {
        val printData = getInvoicePrintData() ?: return "No invoice data available"
        val theme = settingsPreferences?.invoiceTheme?.value ?: InvoiceTheme.CLASSIC
        val content = settingsPreferences?.contentSettings?.value ?: InvoiceContentSettings()
        val typo = settingsPreferences?.typography?.value ?: PrintTypography.STANDARD
        return ThermalInvoiceFormatter.formatPlainTextReceipt(
            data = printData,
            paperWidth = _paperWidth.value,
            theme = theme,
            contentSettings = content,
            typography = typo
        )
    }

    fun printInvoice(onPermissionRequired: ((Array<String>) -> Unit)? = null) {
        if (_printJobState.value.status == PrintJobStatus.CONNECTING || _printJobState.value.status == PrintJobStatus.PRINTING) {
            return
        }

        // Re-read latest saved printer settings
        loadPrinterSettings()

        val printer = _selectedPrinter.value
        if (printer == null) {
            _printJobState.value = PrintJobState(
                status = PrintJobStatus.ERROR,
                message = "No printer configured. Please configure your default printer in Settings."
            )
            return
        }

        val manager = printerManager
        if (manager == null) {
            _printJobState.value = PrintJobState(
                status = PrintJobStatus.ERROR,
                message = "Printer service is unavailable on this device."
            )
            return
        }

        when (printer.connectionType) {
            PrinterConnectionType.BLUETOOTH -> {
                if (!manager.isBluetoothSupported()) {
                    _printJobState.value = PrintJobState(
                        status = PrintJobStatus.ERROR,
                        message = "Bluetooth is not supported on this device."
                    )
                    return
                }
                if (!manager.isBluetoothEnabled()) {
                    _printJobState.value = PrintJobState(
                        status = PrintJobStatus.ERROR,
                        message = "Bluetooth is disabled. Please turn on Bluetooth in settings."
                    )
                    return
                }
                if (!manager.hasBluetoothPermission()) {
                    _printJobState.value = PrintJobState(
                        status = PrintJobStatus.ERROR,
                        message = "Bluetooth permission is required to connect to ${printer.name}."
                    )
                    onPermissionRequired?.invoke(com.vyavsa.app.core.printer.BluetoothPermissionHelper.getRequiredPermissions())
                    return
                }
            }
            PrinterConnectionType.USB -> {
                if (printer.address.isBlank()) {
                    _printJobState.value = PrintJobState(
                        status = PrintJobStatus.ERROR,
                        message = "USB printer address is invalid or disconnected."
                    )
                    return
                }
            }
            PrinterConnectionType.NETWORK -> {
                if (printer.address.isBlank()) {
                    _printJobState.value = PrintJobState(
                        status = PrintJobStatus.ERROR,
                        message = "Please enter a valid IP address for the network printer"
                    )
                    return
                }
            }
        }

        _printJobState.value = PrintJobState(
            status = PrintJobStatus.CONNECTING,
            message = "Connecting to ${printer.name}..."
        )

        viewModelScope.launch {
            // Ensure company logo bitmap is loaded before generating ESC/POS raster commands
            ensureLogoBitmapLoaded()

            val printData = getInvoicePrintData()
            if (printData == null) {
                _printJobState.value = PrintJobState(
                    status = PrintJobStatus.ERROR,
                    message = "Invoice details could not be loaded."
                )
                return@launch
            }

            val theme = settingsPreferences?.invoiceTheme?.value ?: InvoiceTheme.CLASSIC
            val content = settingsPreferences?.contentSettings?.value ?: InvoiceContentSettings()
            val typo = settingsPreferences?.typography?.value ?: PrintTypography.STANDARD

            val rawBytes = ThermalInvoiceFormatter.formatEscPosReceipt(
                data = printData,
                paperWidth = _paperWidth.value,
                theme = theme,
                contentSettings = content,
                typography = typo
            )

            _printJobState.value = PrintJobState(
                status = PrintJobStatus.PRINTING,
                message = "Printing invoice on ${printer.name}..."
            )
            val result = manager.printReceipt(printer, rawBytes)

            result.fold(
                onSuccess = {
                    _printJobState.value = PrintJobState(
                        status = PrintJobStatus.SUCCESS,
                        message = "Invoice printed successfully on ${printer.name}!"
                    )
                },
                onFailure = { err ->
                    _printJobState.value = PrintJobState(
                        status = PrintJobStatus.ERROR,
                        message = err.message ?: "Printing failed. Please check printer connection."
                    )
                }
            )
        }
    }
}
