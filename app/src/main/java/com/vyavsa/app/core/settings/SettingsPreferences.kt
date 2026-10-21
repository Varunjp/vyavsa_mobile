package com.vyavsa.app.core.settings

import android.content.Context
import android.content.SharedPreferences
import com.vyavsa.app.core.navigation.BottomBarDestination
import com.vyavsa.app.core.printer.InvoiceContentSettings
import com.vyavsa.app.core.printer.InvoiceTheme
import com.vyavsa.app.core.printer.PaperWidth
import com.vyavsa.app.core.printer.PrintTypography
import com.vyavsa.app.core.printer.PrinterPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Centralized persistent preferences manager for general app settings, dock customization,
 * and invoice/printing template configurations.
 */
@Singleton
class SettingsPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
    val printerPreferences: PrinterPreferences
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ------------------------------------------------------------------------
    // General Settings
    // ------------------------------------------------------------------------

    private val _themeMode = MutableStateFlow(
        runCatching {
            ThemeMode.valueOf(prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name)
        }.getOrDefault(ThemeMode.SYSTEM)
    )
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _fontSize = MutableStateFlow(
        runCatching {
            AppFontSize.valueOf(prefs.getString(KEY_FONT_SIZE, AppFontSize.MEDIUM.name) ?: AppFontSize.MEDIUM.name)
        }.getOrDefault(AppFontSize.MEDIUM)
    )
    val fontSize: StateFlow<AppFontSize> = _fontSize.asStateFlow()

    private val _language = MutableStateFlow(
        runCatching {
            val code = prefs.getString(KEY_LANGUAGE, AppLanguage.ENGLISH.code) ?: AppLanguage.ENGLISH.code
            AppLanguage.values().firstOrNull { it.code == code } ?: AppLanguage.ENGLISH
        }.getOrDefault(AppLanguage.ENGLISH)
    )
    val language: StateFlow<AppLanguage> = _language.asStateFlow()

    private val _enabledQuickActions = MutableStateFlow(
        prefs.getStringSet(KEY_QUICK_ACTIONS, null)?.toSet()
            ?: QuickActionItem.values().filter { it.defaultEnabled }.map { it.id }.toSet()
    )
    val enabledQuickActions: StateFlow<Set<String>> = _enabledQuickActions.asStateFlow()

    private val _customDockOrder = MutableStateFlow<List<BottomBarDestination>>(
        loadSavedDockOrder()
    )
    val customDockOrder: StateFlow<List<BottomBarDestination>> = _customDockOrder.asStateFlow()

    // ------------------------------------------------------------------------
    // Invoice & Printing Settings
    // ------------------------------------------------------------------------

    private val _invoiceTheme = MutableStateFlow(
        runCatching {
            InvoiceTheme.valueOf(prefs.getString(KEY_INVOICE_THEME, InvoiceTheme.CLASSIC.name) ?: InvoiceTheme.CLASSIC.name)
        }.getOrDefault(InvoiceTheme.CLASSIC)
    )
    val invoiceTheme: StateFlow<InvoiceTheme> = _invoiceTheme.asStateFlow()

    private val _typography = MutableStateFlow(
        runCatching {
            PrintTypography.valueOf(prefs.getString(KEY_TYPOGRAPHY, PrintTypography.STANDARD.name) ?: PrintTypography.STANDARD.name)
        }.getOrDefault(PrintTypography.STANDARD)
    )
    val typography: StateFlow<PrintTypography> = _typography.asStateFlow()

    private val _contentSettings = MutableStateFlow(
        InvoiceContentSettings(
            showBusinessAddress = prefs.getBoolean(KEY_SHOW_ADDR, true),
            showBusinessPhone = prefs.getBoolean(KEY_SHOW_PHONE, true),
            showBusinessEmail = prefs.getBoolean(KEY_SHOW_EMAIL, true),
            showTaxId = prefs.getBoolean(KEY_SHOW_TAX_ID, true),
            showCustomerDetails = prefs.getBoolean(KEY_SHOW_CUSTOMER, true),
            showNotes = prefs.getBoolean(KEY_SHOW_NOTES, true),
            showPaymentInfo = prefs.getBoolean(KEY_SHOW_PAYMENT, true),
            showFooterNote = prefs.getBoolean(KEY_SHOW_FOOTER, true),
            showLogo = prefs.getBoolean(KEY_SHOW_LOGO, true)
        )
    )
    val contentSettings: StateFlow<InvoiceContentSettings> = _contentSettings.asStateFlow()

    // ------------------------------------------------------------------------
    // Mutations
    // ------------------------------------------------------------------------

    fun saveThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
        _themeMode.value = mode
    }

    fun saveFontSize(size: AppFontSize) {
        prefs.edit().putString(KEY_FONT_SIZE, size.name).apply()
        _fontSize.value = size
    }

    fun saveLanguage(lang: AppLanguage) {
        prefs.edit().putString(KEY_LANGUAGE, lang.code).apply()
        _language.value = lang
    }

    fun toggleQuickAction(actionId: String, enabled: Boolean) {
        val current = _enabledQuickActions.value.toMutableSet()
        if (enabled) current.add(actionId) else current.remove(actionId)
        val copy = current.toSet()
        prefs.edit().putStringSet(KEY_QUICK_ACTIONS, copy).apply()
        _enabledQuickActions.value = copy
    }

    fun saveDockOrder(order: List<BottomBarDestination>) {
        val names = order.joinToString(",") { it.name }
        prefs.edit().putString(KEY_DOCK_ORDER, names).apply()
        _customDockOrder.value = order
    }

    fun resetDockOrder() {
        prefs.edit().remove(KEY_DOCK_ORDER).apply()
        _customDockOrder.value = emptyList()
    }

    fun saveInvoiceTheme(theme: InvoiceTheme) {
        prefs.edit().putString(KEY_INVOICE_THEME, theme.name).apply()
        _invoiceTheme.value = theme
    }

    fun saveTypography(typography: PrintTypography) {
        prefs.edit().putString(KEY_TYPOGRAPHY, typography.name).apply()
        _typography.value = typography
    }

    fun updateContentSettings(transform: (InvoiceContentSettings) -> InvoiceContentSettings) {
        val current = _contentSettings.value
        val updated = transform(current)
        saveContentSettings(updated)
    }

    fun saveContentSettings(settings: InvoiceContentSettings) {
        prefs.edit()
            .putBoolean(KEY_SHOW_ADDR, settings.showBusinessAddress)
            .putBoolean(KEY_SHOW_PHONE, settings.showBusinessPhone)
            .putBoolean(KEY_SHOW_EMAIL, settings.showBusinessEmail)
            .putBoolean(KEY_SHOW_TAX_ID, settings.showTaxId)
            .putBoolean(KEY_SHOW_CUSTOMER, settings.showCustomerDetails)
            .putBoolean(KEY_SHOW_NOTES, settings.showNotes)
            .putBoolean(KEY_SHOW_PAYMENT, settings.showPaymentInfo)
            .putBoolean(KEY_SHOW_FOOTER, settings.showFooterNote)
            .putBoolean(KEY_SHOW_LOGO, settings.showLogo)
            .apply()
        _contentSettings.value = settings
    }

    fun toggleShowBusinessAddress(show: Boolean = !_contentSettings.value.showBusinessAddress) = updateContentSettings { it.copy(showBusinessAddress = show) }
    fun toggleShowBusinessPhone(show: Boolean = !_contentSettings.value.showBusinessPhone) = updateContentSettings { it.copy(showBusinessPhone = show) }
    fun toggleShowBusinessEmail(show: Boolean = !_contentSettings.value.showBusinessEmail) = updateContentSettings { it.copy(showBusinessEmail = show) }
    fun toggleShowTaxId(show: Boolean = !_contentSettings.value.showTaxId) = updateContentSettings { it.copy(showTaxId = show) }
    fun toggleShowCustomerDetails(show: Boolean = !_contentSettings.value.showCustomerDetails) = updateContentSettings { it.copy(showCustomerDetails = show) }
    fun toggleShowPaymentInfo(show: Boolean = !_contentSettings.value.showPaymentInfo) = updateContentSettings { it.copy(showPaymentInfo = show) }
    fun toggleShowNotes(show: Boolean = !_contentSettings.value.showNotes) = updateContentSettings { it.copy(showNotes = show) }
    fun toggleShowFooterNote(show: Boolean = !_contentSettings.value.showFooterNote) = updateContentSettings { it.copy(showFooterNote = show) }
    fun toggleShowLogo(show: Boolean = !_contentSettings.value.showLogo) = updateContentSettings { it.copy(showLogo = show) }

    fun savePaperWidth(width: PaperWidth) {
        printerPreferences.savePaperWidth(width)
    }

    fun resetToDefaults() {
        prefs.edit().clear().apply()
        _themeMode.value = ThemeMode.SYSTEM
        _fontSize.value = AppFontSize.MEDIUM
        _language.value = AppLanguage.ENGLISH
        _enabledQuickActions.value = QuickActionItem.values().filter { it.defaultEnabled }.map { it.id }.toSet()
        _customDockOrder.value = emptyList()
        _invoiceTheme.value = InvoiceTheme.CLASSIC
        _typography.value = PrintTypography.STANDARD
        _contentSettings.value = InvoiceContentSettings()
        printerPreferences.savePaperWidth(PaperWidth.WIDTH_58MM)
    }

    private fun loadSavedDockOrder(): List<BottomBarDestination> {
        val raw = prefs.getString(KEY_DOCK_ORDER, null) ?: return emptyList()
        return raw.split(",")
            .mapNotNull { name ->
                runCatching { BottomBarDestination.valueOf(name.trim()) }.getOrNull()
            }
    }

    companion object {
        private const val PREFS_NAME = "vyavsa_general_settings"
        private const val KEY_THEME_MODE = "setting_theme_mode"
        private const val KEY_FONT_SIZE = "setting_font_size"
        private const val KEY_LANGUAGE = "setting_language"
        private const val KEY_QUICK_ACTIONS = "setting_quick_actions"
        private const val KEY_DOCK_ORDER = "setting_dock_order"
        private const val KEY_INVOICE_THEME = "setting_invoice_theme"
        private const val KEY_TYPOGRAPHY = "setting_typography"
        private const val KEY_SHOW_ADDR = "setting_show_address"
        private const val KEY_SHOW_PHONE = "setting_show_phone"
        private const val KEY_SHOW_EMAIL = "setting_show_email"
        private const val KEY_SHOW_TAX_ID = "setting_show_tax_id"
        private const val KEY_SHOW_CUSTOMER = "setting_show_customer"
        private const val KEY_SHOW_NOTES = "setting_show_notes"
        private const val KEY_SHOW_PAYMENT = "setting_show_payment"
        private const val KEY_SHOW_FOOTER = "setting_show_footer"
        private const val KEY_SHOW_LOGO = "setting_show_logo"
    }
}
