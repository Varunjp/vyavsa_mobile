package com.vyavsa.app.feature.settings

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vyavsa.app.core.common.FormatUtils
import com.vyavsa.app.core.common.Resource
import com.vyavsa.app.core.common.UiState
import com.vyavsa.app.core.designsystem.LogoMemoryCache
import com.vyavsa.app.core.model.TenantProfile
import com.vyavsa.app.core.model.UpdateTenantProfileRequest
import com.vyavsa.app.core.model.UserRole
import com.vyavsa.app.core.navigation.BottomBarDestination
import com.vyavsa.app.core.network.ApiErrorParser
import com.vyavsa.app.core.network.VyavsaApiService
import com.vyavsa.app.core.printer.InvoiceContentSettings
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
import com.vyavsa.app.core.security.TokenManager
import com.vyavsa.app.core.settings.AppFontSize
import com.vyavsa.app.core.settings.AppLanguage
import com.vyavsa.app.core.settings.SettingsPreferences
import com.vyavsa.app.core.settings.ThemeMode
import com.vyavsa.app.core.sync.DataMutation
import com.vyavsa.app.core.sync.DataRefreshCoordinator
import com.vyavsa.app.feature.dashboard.DashboardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    val settingsPreferences: SettingsPreferences,
    val printerPreferences: PrinterPreferences,
    val printerManager: ThermalPrinterManager,
    val tokenManager: TokenManager? = null,
    private val dashboardRepository: DashboardRepository? = null,
    private val apiService: VyavsaApiService? = null,
    private val refreshCoordinator: DataRefreshCoordinator? = null
) : ViewModel() {

    constructor(
        settingsPreferences: SettingsPreferences,
        printerPreferences: PrinterPreferences,
        printerManager: ThermalPrinterManager,
        tokenManager: TokenManager?,
        dashboardRepository: DashboardRepository?
    ) : this(settingsPreferences, printerPreferences, printerManager, tokenManager, dashboardRepository, null, null)

    // General Preferences
    val themeMode: StateFlow<ThemeMode> = settingsPreferences.themeMode
    val fontSize: StateFlow<AppFontSize> = settingsPreferences.fontSize
    val language: StateFlow<AppLanguage> = settingsPreferences.language
    val enabledQuickActions: StateFlow<Set<String>> = settingsPreferences.enabledQuickActions
    val customDockOrder: StateFlow<List<BottomBarDestination>> = settingsPreferences.customDockOrder

    // Invoice & Printing Preferences
    val invoiceTheme: StateFlow<InvoiceTheme> = settingsPreferences.invoiceTheme
    val typography: StateFlow<PrintTypography> = settingsPreferences.typography
    val contentSettings: StateFlow<InvoiceContentSettings> = settingsPreferences.contentSettings
    val paperWidth: StateFlow<PaperWidth> = printerPreferences.paperWidth

    // Printer Hardware State
    private val _pairedPrinters = MutableStateFlow<List<PrinterDevice>>(emptyList())
    val pairedPrinters: StateFlow<List<PrinterDevice>> = _pairedPrinters.asStateFlow()

    private val _usbPrinters = MutableStateFlow<List<PrinterDevice>>(emptyList())
    val usbPrinters: StateFlow<List<PrinterDevice>> = _usbPrinters.asStateFlow()

    private val _selectedPrinter = MutableStateFlow<PrinterDevice?>(null)
    val selectedPrinter: StateFlow<PrinterDevice?> = _selectedPrinter.asStateFlow()

    private val _selectedConnectionType = MutableStateFlow(printerPreferences.selectedConnectionType.value)
    val selectedConnectionType: StateFlow<PrinterConnectionType> = _selectedConnectionType.asStateFlow()

    private val _networkPrinterIp = MutableStateFlow(printerPreferences.savedNetworkPrinterIp.value ?: "192.168.1.100:9100")
    val networkPrinterIp: StateFlow<String> = _networkPrinterIp.asStateFlow()

    private val _testPrintState = MutableStateFlow(PrintJobState())
    val testPrintState: StateFlow<PrintJobState> = _testPrintState.asStateFlow()

    private val _companyProfile = MutableStateFlow<TenantProfile?>(null)
    val companyProfile: StateFlow<TenantProfile?> = _companyProfile.asStateFlow()

    private val _footerUpdateState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val footerUpdateState: StateFlow<UiState<Unit>> = _footerUpdateState.asStateFlow()

    private var cachedLogoUrl: String? = null
    private var cachedLogoBitmap: Bitmap? = null

    init {
        loadInitialPrinterState()
        loadCompanyProfile()
        refreshPrinters()
        observeMutations()
    }

    private fun loadInitialPrinterState() {
        val addr = printerPreferences.selectedPrinterAddress.value
        val name = printerPreferences.selectedPrinterName.value
        val type = printerPreferences.selectedConnectionType.value
        if (!addr.isNullOrBlank()) {
            _selectedPrinter.value = PrinterDevice(
                name = name ?: "Configured Printer",
                address = addr,
                connectionType = type
            )
        }
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

    private fun observeMutations() {
        refreshCoordinator?.let { coordinator ->
            viewModelScope.launch {
                coordinator.refreshEvents.collectLatest { mutation ->
                    if (mutation == DataMutation.PROFILE || mutation == DataMutation.ALL) {
                        loadCompanyProfile()
                    }
                }
            }
        }
    }

    private fun preloadLogoBitmap(logoUrl: String?) {
        if (logoUrl.isNullOrBlank()) {
            cachedLogoBitmap = null
            cachedLogoUrl = null
            return
        }
        if (cachedLogoBitmap != null && cachedLogoUrl == logoUrl) return

        cachedLogoUrl = logoUrl
        viewModelScope.launch(Dispatchers.IO) {
            ensureLogoBitmapLoaded(logoUrl)
        }
    }

    suspend fun ensureLogoBitmapLoaded(logoUrl: String? = _companyProfile.value?.logoUrl) {
        if (logoUrl.isNullOrBlank()) {
            cachedLogoBitmap = null
            cachedLogoUrl = null
            return
        }
        if (cachedLogoBitmap != null && cachedLogoUrl == logoUrl) return

        withContext(Dispatchers.IO) {
            try {
                val resolvedUrl = FormatUtils.resolveMediaUrl(logoUrl) ?: return@withContext
                val cached = LogoMemoryCache.get(resolvedUrl)
                if (cached != null) {
                    cachedLogoBitmap = cached.asAndroidBitmap()
                    cachedLogoUrl = logoUrl
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
                        cachedLogoUrl = logoUrl
                    }
                }
            } catch (_: Exception) {
                // Graceful fallback
            }
        }
    }

    fun getCachedLogoBitmap(): Bitmap? = cachedLogoBitmap

    fun resetFooterUpdateState() {
        _footerUpdateState.value = UiState.Idle
    }

    fun updateInvoiceFooter(heading: String, paragraph: String) {
        val trimmedHeading = heading.trim()
        val trimmedParagraph = paragraph.trim()

        if (trimmedHeading.length > 255) {
            _footerUpdateState.value = UiState.Error("Footer heading cannot exceed 255 characters")
            return
        }
        if (trimmedParagraph.length > 1000) {
            _footerUpdateState.value = UiState.Error("Footer paragraph cannot exceed 1000 characters")
            return
        }

        val currentProfile = _companyProfile.value
        if (currentProfile == null) {
            _footerUpdateState.value = UiState.Error("Profile data not loaded yet")
            return
        }

        viewModelScope.launch {
            _footerUpdateState.value = UiState.Loading
            try {
                val api = apiService
                if (api == null) {
                    val updated = currentProfile.copy(
                        invoiceFooterHeading = trimmedHeading.ifBlank { null },
                        invoiceFooterParagraph = trimmedParagraph.ifBlank { null }
                    )
                    _companyProfile.value = updated
                    _footerUpdateState.value = UiState.Success(Unit)
                    return@launch
                }
                val req = UpdateTenantProfileRequest(
                    name = currentProfile.name,
                    email = currentProfile.email,
                    phone = currentProfile.phone,
                    logoUrl = currentProfile.logoUrl,
                    addressLine1 = currentProfile.addressLine1,
                    addressLine2 = currentProfile.addressLine2,
                    city = currentProfile.city,
                    state = currentProfile.state,
                    postalCode = currentProfile.postalCode,
                    country = currentProfile.country,
                    invoiceFooterHeading = trimmedHeading.ifBlank { null },
                    invoiceFooterParagraph = trimmedParagraph.ifBlank { null }
                )
                val res = api.updateProfile(req)
                if (res.success && res.data != null) {
                    _companyProfile.value = res.data
                    _footerUpdateState.value = UiState.Success(Unit)
                    refreshCoordinator?.notifyMutation(DataMutation.PROFILE)
                } else {
                    _footerUpdateState.value = UiState.Error(res.error?.message ?: "Failed to update invoice footer")
                }
            } catch (e: Exception) {
                _footerUpdateState.value = UiState.Error(ApiErrorParser.parseException(e).message)
            }
        }
    }

    // ------------------------------------------------------------------------
    // General Settings Actions
    // ------------------------------------------------------------------------

    fun setThemeMode(mode: ThemeMode) = settingsPreferences.saveThemeMode(mode)
    fun setFontSize(size: AppFontSize) = settingsPreferences.saveFontSize(size)
    fun setLanguage(lang: AppLanguage) = settingsPreferences.saveLanguage(lang)
    fun toggleQuickAction(id: String, enabled: Boolean) = settingsPreferences.toggleQuickAction(id, enabled)

    fun getEffectiveDockOrder(): List<BottomBarDestination> {
        val userRole = tokenManager?.getUserRole() ?: UserRole.ADMIN
        val defaultList = BottomBarDestination.destinationsForRole(userRole)
        val saved = customDockOrder.value
        if (saved.isEmpty()) return defaultList

        // Filter saved items so only destinations valid for this role are preserved, and append any missing ones
        val validSaved = saved.filter { it in defaultList }
        val missing = defaultList.filter { it !in validSaved }
        return validSaved + missing
    }

    fun moveDockItemUp(index: Int) {
        val current = getEffectiveDockOrder().toMutableList()
        if (index > 0 && index < current.size) {
            val item = current.removeAt(index)
            current.add(index - 1, item)
            settingsPreferences.saveDockOrder(current)
        }
    }

    fun moveDockItemDown(index: Int) {
        val current = getEffectiveDockOrder().toMutableList()
        if (index >= 0 && index < current.size - 1) {
            val item = current.removeAt(index)
            current.add(index + 1, item)
            settingsPreferences.saveDockOrder(current)
        }
    }

    fun resetDockOrder() = settingsPreferences.resetDockOrder()

    // ------------------------------------------------------------------------
    // Invoice & Printing Actions
    // ------------------------------------------------------------------------

    fun setInvoiceTheme(theme: InvoiceTheme) = settingsPreferences.saveInvoiceTheme(theme)
    fun setPaperWidth(width: PaperWidth) = settingsPreferences.savePaperWidth(width)
    fun setTypography(typography: PrintTypography) = settingsPreferences.saveTypography(typography)
    fun updateContentSettings(settings: InvoiceContentSettings) = settingsPreferences.saveContentSettings(settings)

    fun toggleShowBusinessAddress(show: Boolean = !contentSettings.value.showBusinessAddress) = settingsPreferences.toggleShowBusinessAddress(show)
    fun toggleShowBusinessPhone(show: Boolean = !contentSettings.value.showBusinessPhone) = settingsPreferences.toggleShowBusinessPhone(show)
    fun toggleShowBusinessEmail(show: Boolean = !contentSettings.value.showBusinessEmail) = settingsPreferences.toggleShowBusinessEmail(show)
    fun toggleShowTaxId(show: Boolean = !contentSettings.value.showTaxId) = settingsPreferences.toggleShowTaxId(show)
    fun toggleShowCustomerDetails(show: Boolean = !contentSettings.value.showCustomerDetails) = settingsPreferences.toggleShowCustomerDetails(show)
    fun toggleShowPaymentInfo(show: Boolean = !contentSettings.value.showPaymentInfo) = settingsPreferences.toggleShowPaymentInfo(show)
    fun toggleShowNotes(show: Boolean = !contentSettings.value.showNotes) = settingsPreferences.toggleShowNotes(show)
    fun toggleShowFooterNote(show: Boolean = !contentSettings.value.showFooterNote) = settingsPreferences.toggleShowFooterNote(show)
    fun toggleShowLogo(show: Boolean = !contentSettings.value.showLogo) = settingsPreferences.toggleShowLogo(show)

    fun resetAllSettingsToDefaults() {
        settingsPreferences.resetToDefaults()
        loadInitialPrinterState()
    }

    // ------------------------------------------------------------------------
    // Printer Management
    // ------------------------------------------------------------------------

    fun refreshPrinters() {
        _pairedPrinters.value = printerManager.getPairedPrinters()
        _usbPrinters.value = printerManager.getConnectedUsbPrinters()
    }

    fun selectPrinter(printer: PrinterDevice) {
        _selectedPrinter.value = printer
        printerPreferences.saveSelectedPrinter(printer)
    }

    fun setConnectionType(type: PrinterConnectionType) {
        _selectedConnectionType.value = type
        printerPreferences.saveConnectionType(type)
        when (type) {
            PrinterConnectionType.BLUETOOTH -> {
                refreshPrinters()
                val current = _selectedPrinter.value
                if (current?.connectionType != PrinterConnectionType.BLUETOOTH) {
                    val fallback = _pairedPrinters.value.firstOrNull()
                    _selectedPrinter.value = fallback
                    printerPreferences.saveSelectedPrinter(fallback)
                }
            }
            PrinterConnectionType.USB -> {
                refreshPrinters()
                val current = _selectedPrinter.value
                if (current?.connectionType != PrinterConnectionType.USB) {
                    val fallback = _usbPrinters.value.firstOrNull()
                    _selectedPrinter.value = fallback
                    printerPreferences.saveSelectedPrinter(fallback)
                }
            }
            PrinterConnectionType.NETWORK -> {
                setNetworkPrinterIp(_networkPrinterIp.value)
            }
        }
    }

    fun setNetworkPrinterIp(ip: String) {
        val clean = ip.trim()
        _networkPrinterIp.value = clean
        printerPreferences.saveNetworkPrinterIp(clean)
        val dev = PrinterDevice(
            name = "Network Thermal Printer",
            address = clean,
            isBonded = true,
            connectionType = PrinterConnectionType.NETWORK
        )
        _selectedPrinter.value = dev
        printerPreferences.saveSelectedPrinter(dev)
    }

    fun resetTestPrintState() {
        _testPrintState.value = PrintJobState(status = PrintJobStatus.IDLE)
    }

    fun getSampleInvoiceData(): InvoicePrintData {
        val profile = _companyProfile.value
        return InvoicePrintData(
            businessName = profile?.name?.ifBlank { null } ?: "Vyavsa Enterprises",
            businessAddress = profile?.getEffectiveAddress() ?: "Main Market, Commercial Center, Jaipur",
            businessPhone = profile?.phone ?: "+91 98765 43210",
            businessEmail = profile?.email ?: "sales@vyavsa.com",
            taxId = "08AAAAA0000A1Z5",
            logoUrl = profile?.logoUrl,
            logoBitmap = cachedLogoBitmap,
            invoiceTitle = "Tax Invoice / Cash Bill",
            invoiceNumber = "INV-2026-0042",
            invoiceDate = "21 Oct 2026, 11:30 AM",
            customerName = "Sharma Trading Co.",
            customerPhone = "+91 98290 12345",
            route = "Bazaar Central Route",
            salesman = "Varun Sharma",
            items = listOf(
                PrintItem(name = "Premium Cotton Fabric 50m", quantity = "2.0", unitPrice = "1250.00", lineTotal = "2500.00"),
                PrintItem(name = "Industrial Thread Spools", quantity = "10.0", unitPrice = "45.00", lineTotal = "450.00"),
                PrintItem(name = "Packaging Bags (Heavy)", quantity = "5.0", unitPrice = "80.00", lineTotal = "400.00")
            ),
            subtotal = 3350.00,
            discount = 150.00,
            totalAmount = 3200.00,
            amountPaid = 3200.00,
            balanceDue = 0.00,
            paymentSummary = "Cash / Counter Settlement",
            note = "Goods inspected and received in good condition.",
            footerHeading = profile?.invoiceFooterHeading?.ifBlank { null } ?: "Thank you for your business!",
            footerParagraph = profile?.invoiceFooterParagraph?.ifBlank { null } ?: "Powered by Vyavsa Small Business Solutions"
        )
    }

    fun getReceiptPreviewText(): String {
        val sample = getSampleInvoiceData()
        return ThermalInvoiceFormatter.formatPlainTextReceipt(
            data = sample,
            paperWidth = paperWidth.value,
            theme = invoiceTheme.value,
            contentSettings = contentSettings.value,
            typography = typography.value
        )
    }

    fun runTestPrint(onPermissionRequired: ((Array<String>) -> Unit)? = null) {
        val printer = _selectedPrinter.value
        if (printer == null) {
            _testPrintState.value = PrintJobState(status = PrintJobStatus.ERROR, message = "No printer selected. Please choose a printer first.")
            return
        }

        if (printer.connectionType == PrinterConnectionType.BLUETOOTH) {
            if (!printerManager.isBluetoothSupported()) {
                _testPrintState.value = PrintJobState(status = PrintJobStatus.ERROR, message = "Bluetooth is not supported on this device.")
                return
            }
            if (!printerManager.isBluetoothEnabled()) {
                _testPrintState.value = PrintJobState(status = PrintJobStatus.ERROR, message = "Bluetooth is turned off. Please turn on Bluetooth.")
                return
            }
            if (!printerManager.hasBluetoothPermission()) {
                _testPrintState.value = PrintJobState(status = PrintJobStatus.ERROR, message = "Bluetooth permission is required.")
                onPermissionRequired?.invoke(com.vyavsa.app.core.printer.BluetoothPermissionHelper.getRequiredPermissions())
                return
            }
        }

        viewModelScope.launch {
            _testPrintState.value = PrintJobState(status = PrintJobStatus.CONNECTING, message = "Connecting to ${printer.name}...")

            if (contentSettings.value.showLogo && cachedLogoBitmap == null && !_companyProfile.value?.logoUrl.isNullOrBlank()) {
                ensureLogoBitmapLoaded(_companyProfile.value?.logoUrl)
            }

            val testData = getSampleInvoiceData()
            val rawBytes = ThermalInvoiceFormatter.formatEscPosReceipt(
                data = testData,
                paperWidth = paperWidth.value,
                theme = invoiceTheme.value,
                contentSettings = contentSettings.value,
                typography = typography.value
            )

            _testPrintState.value = PrintJobState(status = PrintJobStatus.PRINTING, message = "Transmitting test print data...")
            val result = printerManager.printReceipt(printer, rawBytes)

            result.fold(
                onSuccess = {
                    _testPrintState.value = PrintJobState(status = PrintJobStatus.SUCCESS, message = "Test print succeeded on ${printer.name}!")
                },
                onFailure = { error ->
                    _testPrintState.value = PrintJobState(status = PrintJobStatus.ERROR, message = error.message ?: "Test print failed.")
                }
            )
        }
    }
}
