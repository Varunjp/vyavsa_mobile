package com.vyavsa.app.core.settings

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import com.vyavsa.app.core.navigation.BottomBarDestination
import com.vyavsa.app.core.printer.InvoiceContentSettings
import com.vyavsa.app.core.printer.InvoiceTheme
import com.vyavsa.app.core.printer.PaperWidth
import com.vyavsa.app.core.printer.PrintTypography
import com.vyavsa.app.core.printer.PrinterPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeSharedPreferences : SharedPreferences {
    private val values = mutableMapOf<String, Any?>()

    override fun getAll(): Map<String, *> = values
    override fun getString(key: String?, defValue: String?): String? = values[key] as? String ?: defValue
    @Suppress("UNCHECKED_CAST")
    override fun getStringSet(key: String?, defValues: Set<String>?): Set<String>? = values[key] as? Set<String> ?: defValues
    override fun getInt(key: String?, defValue: Int): Int = values[key] as? Int ?: defValue
    override fun getLong(key: String?, defValue: Long): Long = values[key] as? Long ?: defValue
    override fun getFloat(key: String?, defValue: Float): Float = values[key] as? Float ?: defValue
    override fun getBoolean(key: String?, defValue: Boolean): Boolean = values[key] as? Boolean ?: defValue
    override fun contains(key: String?): Boolean = values.containsKey(key)
    override fun edit(): SharedPreferences.Editor = FakeEditor(values)
    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

    class FakeEditor(private val storage: MutableMap<String, Any?>) : SharedPreferences.Editor {
        private val temp = mutableMapOf<String, Any?>()
        private val removes = mutableSetOf<String>()
        private var clear = false

        override fun putString(key: String?, value: String?): SharedPreferences.Editor {
            if (key != null) temp[key] = value
            return this
        }
        override fun putStringSet(key: String?, values: Set<String>?): SharedPreferences.Editor {
            if (key != null) temp[key] = values
            return this
        }
        override fun putInt(key: String?, value: Int): SharedPreferences.Editor {
            if (key != null) temp[key] = value
            return this
        }
        override fun putLong(key: String?, value: Long): SharedPreferences.Editor {
            if (key != null) temp[key] = value
            return this
        }
        override fun putFloat(key: String?, value: Float): SharedPreferences.Editor {
            if (key != null) temp[key] = value
            return this
        }
        override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor {
            if (key != null) temp[key] = value
            return this
        }
        override fun remove(key: String?): SharedPreferences.Editor {
            if (key != null) removes.add(key)
            return this
        }
        override fun clear(): SharedPreferences.Editor {
            clear = true
            return this
        }
        override fun commit(): Boolean {
            apply()
            return true
        }
        override fun apply() {
            if (clear) storage.clear()
            removes.forEach { storage.remove(it) }
            storage.putAll(temp)
        }
    }
}

class TestContext(private val fakePrefs: SharedPreferences) : ContextWrapper(null) {
    override fun getSharedPreferences(name: String?, mode: Int): SharedPreferences = fakePrefs
    override fun getPackageName(): String = "com.vyavsa.app"
}

class SettingsPreferencesTest {

    private lateinit var fakePrefs: FakeSharedPreferences
    private lateinit var testContext: Context
    private lateinit var printerPrefs: PrinterPreferences
    private lateinit var settingsPreferences: SettingsPreferences

    @Before
    fun setUp() {
        fakePrefs = FakeSharedPreferences()
        testContext = TestContext(fakePrefs)
        printerPrefs = PrinterPreferences(testContext)
        settingsPreferences = SettingsPreferences(testContext, printerPrefs)
    }

    @Test
    fun testDefaultSettings() {
        assertEquals(ThemeMode.SYSTEM, settingsPreferences.themeMode.value)
        assertEquals(AppFontSize.MEDIUM, settingsPreferences.fontSize.value)
        assertEquals(AppLanguage.ENGLISH, settingsPreferences.language.value)
        assertEquals(InvoiceTheme.CLASSIC, settingsPreferences.invoiceTheme.value)
        assertEquals(PrintTypography.STANDARD, settingsPreferences.typography.value)
        assertTrue(settingsPreferences.customDockOrder.value.isEmpty())
        assertTrue(settingsPreferences.enabledQuickActions.value.isNotEmpty())
    }

    @Test
    fun testUpdateThemeMode() {
        settingsPreferences.saveThemeMode(ThemeMode.DARK)
        assertEquals(ThemeMode.DARK, settingsPreferences.themeMode.value)
        assertEquals("DARK", fakePrefs.getString("setting_theme_mode", null))

        settingsPreferences.saveThemeMode(ThemeMode.LIGHT)
        assertEquals(ThemeMode.LIGHT, settingsPreferences.themeMode.value)
        assertEquals("LIGHT", fakePrefs.getString("setting_theme_mode", null))
    }

    @Test
    fun testUpdateFontSize() {
        settingsPreferences.saveFontSize(AppFontSize.LARGE)
        assertEquals(AppFontSize.LARGE, settingsPreferences.fontSize.value)
        assertEquals("LARGE", fakePrefs.getString("setting_font_size", null))

        settingsPreferences.saveFontSize(AppFontSize.SMALL)
        assertEquals(AppFontSize.SMALL, settingsPreferences.fontSize.value)
        assertEquals("SMALL", fakePrefs.getString("setting_font_size", null))
    }

    @Test
    fun testUpdateLanguage() {
        settingsPreferences.saveLanguage(AppLanguage.HINDI)
        assertEquals(AppLanguage.HINDI, settingsPreferences.language.value)
        assertEquals("hi", fakePrefs.getString("setting_language", null))
    }

    @Test
    fun testDockOrderCustomization() {
        val newOrder = listOf(
            BottomBarDestination.SALES,
            BottomBarDestination.HOME,
            BottomBarDestination.DUES,
            BottomBarDestination.MORE
        )
        settingsPreferences.saveDockOrder(newOrder)
        assertEquals(newOrder, settingsPreferences.customDockOrder.value)
        val persisted = fakePrefs.getString("setting_dock_order", null)
        assertEquals("SALES,HOME,DUES,MORE", persisted)
    }

    @Test
    fun testQuickActionToggling() {
        val actionId = QuickActionItem.CREATE_LINE_SALE.id
        assertTrue(settingsPreferences.enabledQuickActions.value.contains(actionId))

        settingsPreferences.toggleQuickAction(actionId, false)
        assertFalse(settingsPreferences.enabledQuickActions.value.contains(actionId))

        settingsPreferences.toggleQuickAction(actionId, true)
        assertTrue(settingsPreferences.enabledQuickActions.value.contains(actionId))
    }

    @Test
    fun testInvoiceThemeAndTypography() {
        settingsPreferences.saveInvoiceTheme(InvoiceTheme.MODERN_MINIMAL)
        assertEquals(InvoiceTheme.MODERN_MINIMAL, settingsPreferences.invoiceTheme.value)
        assertEquals("MODERN_MINIMAL", fakePrefs.getString("setting_invoice_theme", null))

        settingsPreferences.saveTypography(PrintTypography.COMPACT)
        assertEquals(PrintTypography.COMPACT, settingsPreferences.typography.value)
        assertEquals("COMPACT", fakePrefs.getString("setting_typography", null))
    }

    @Test
    fun testInvoiceContentSettings() {
        val updatedContent = InvoiceContentSettings(
            showBusinessAddress = false,
            showBusinessPhone = false,
            showBusinessEmail = false,
            showTaxId = false,
            showCustomerDetails = true,
            showNotes = false,
            showPaymentInfo = true,
            showFooterNote = true,
            showLogo = false
        )
        settingsPreferences.saveContentSettings(updatedContent)
        assertEquals(updatedContent, settingsPreferences.contentSettings.value)

        assertFalse(fakePrefs.getBoolean("setting_show_address", true))
        assertFalse(fakePrefs.getBoolean("setting_show_phone", true))
        assertFalse(fakePrefs.getBoolean("setting_show_logo", true))
        assertTrue(fakePrefs.getBoolean("setting_show_customer", false))
    }

    @Test
    fun testUpdateContentSettingsAtomic() {
        val original = settingsPreferences.contentSettings.value
        assertTrue(original.showLogo)
        assertTrue(original.showBusinessAddress)

        settingsPreferences.updateContentSettings { current ->
            current.copy(showLogo = false, showNotes = true)
        }

        assertFalse(settingsPreferences.contentSettings.value.showLogo)
        assertTrue(settingsPreferences.contentSettings.value.showNotes)
        assertFalse(fakePrefs.getBoolean("setting_show_logo", true))
        assertTrue(fakePrefs.getBoolean("setting_show_notes", false))
    }

    @Test
    fun testIndividualContentToggles() {
        // Toggle Logo
        assertTrue(settingsPreferences.contentSettings.value.showLogo)
        settingsPreferences.toggleShowLogo(false)
        assertFalse(settingsPreferences.contentSettings.value.showLogo)
        assertFalse(fakePrefs.getBoolean("setting_show_logo", true))

        // Inverted toggle without explicit parameter
        settingsPreferences.toggleShowLogo()
        assertTrue(settingsPreferences.contentSettings.value.showLogo)

        // Toggle Business Address
        settingsPreferences.toggleShowBusinessAddress(false)
        assertFalse(settingsPreferences.contentSettings.value.showBusinessAddress)

        // Toggle Business Phone
        settingsPreferences.toggleShowBusinessPhone(false)
        assertFalse(settingsPreferences.contentSettings.value.showBusinessPhone)

        // Toggle Business Email
        settingsPreferences.toggleShowBusinessEmail(false)
        assertFalse(settingsPreferences.contentSettings.value.showBusinessEmail)

        // Toggle Tax ID
        settingsPreferences.toggleShowTaxId(false)
        assertFalse(settingsPreferences.contentSettings.value.showTaxId)

        // Toggle Customer Details
        settingsPreferences.toggleShowCustomerDetails(true)
        assertTrue(settingsPreferences.contentSettings.value.showCustomerDetails)

        // Toggle Payment Info
        settingsPreferences.toggleShowPaymentInfo(false)
        assertFalse(settingsPreferences.contentSettings.value.showPaymentInfo)

        // Toggle Notes
        settingsPreferences.toggleShowNotes(true)
        assertTrue(settingsPreferences.contentSettings.value.showNotes)

        // Toggle Footer Note
        settingsPreferences.toggleShowFooterNote(false)
        assertFalse(settingsPreferences.contentSettings.value.showFooterNote)
    }

    @Test
    fun testResetToDefaults() {
        settingsPreferences.saveThemeMode(ThemeMode.DARK)
        settingsPreferences.saveFontSize(AppFontSize.LARGE)
        settingsPreferences.saveInvoiceTheme(InvoiceTheme.DETAILED_GST)
        settingsPreferences.saveDockOrder(listOf(BottomBarDestination.DUES))

        settingsPreferences.resetToDefaults()

        assertEquals(ThemeMode.SYSTEM, settingsPreferences.themeMode.value)
        assertEquals(AppFontSize.MEDIUM, settingsPreferences.fontSize.value)
        assertEquals(InvoiceTheme.CLASSIC, settingsPreferences.invoiceTheme.value)
        assertTrue(settingsPreferences.customDockOrder.value.isEmpty())
    }
}
