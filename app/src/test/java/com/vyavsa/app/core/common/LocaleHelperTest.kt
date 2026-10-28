package com.vyavsa.app.core.common

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Proxy
import java.util.Locale

class LocaleHelperTest {

    @Test
    fun testGetLocaleForAllSupportedLanguages() {
        assertEquals("hi", LocaleHelper.getLocale("hi").language)
        assertEquals("IN", LocaleHelper.getLocale("hi").country)

        assertEquals("ml", LocaleHelper.getLocale("ml").language)
        assertEquals("IN", LocaleHelper.getLocale("ml").country)

        assertEquals("ta", LocaleHelper.getLocale("ta").language)
        assertEquals("IN", LocaleHelper.getLocale("ta").country)

        assertEquals("kn", LocaleHelper.getLocale("kn").language)
        assertEquals("IN", LocaleHelper.getLocale("kn").country)

        assertEquals("te", LocaleHelper.getLocale("te").language)
        assertEquals("IN", LocaleHelper.getLocale("te").country)

        assertEquals("bn", LocaleHelper.getLocale("bn").language)
        assertEquals("IN", LocaleHelper.getLocale("bn").country)

        assertEquals("mr", LocaleHelper.getLocale("mr").language)
        assertEquals("IN", LocaleHelper.getLocale("mr").country)

        assertEquals("gu", LocaleHelper.getLocale("gu").language)
        assertEquals("IN", LocaleHelper.getLocale("gu").country)

        assertEquals("pa", LocaleHelper.getLocale("pa").language)
        assertEquals("IN", LocaleHelper.getLocale("pa").country)

        assertEquals("or", LocaleHelper.getLocale("or").language)
        assertEquals("IN", LocaleHelper.getLocale("or").country)

        assertEquals("as", LocaleHelper.getLocale("as").language)
        assertEquals("IN", LocaleHelper.getLocale("as").country)

        assertEquals("en", LocaleHelper.getLocale("en").language)
        assertEquals("US", LocaleHelper.getLocale("en").country)

        // Case insensitivity & fallback
        assertEquals("hi", LocaleHelper.getLocale("HI").language)
        assertEquals("en", LocaleHelper.getLocale("xyz").language)
    }

    private class FakeActivity : Activity()

    private class FakeContext : ContextWrapper(null)

    private class FakeContextWrapper(private val target: Context) : ContextWrapper(target) {
        override fun getBaseContext(): Context = target
    }

    @Test
    fun testFindActivityDirectAndWrapped() {
        val fakeActivity = FakeActivity()
        assertEquals(fakeActivity, fakeActivity.findActivity())

        val config = Configuration()
        val localizedWrapper = LocalizedActivityContext(fakeActivity, config)
        assertEquals(fakeActivity, localizedWrapper.findActivity())

        val outerWrapper = FakeContextWrapper(localizedWrapper)
        assertEquals(fakeActivity, outerWrapper.findActivity())

        val nonActivityContext = FakeContext()
        assertNull(nonActivityContext.findActivity())
    }

    @Test
    fun testHiltContextUnwrappingCompatibility() {
        val fakeActivity = FakeActivity()
        val config = Configuration()
        val localizedWrapper = LocalizedActivityContext(fakeActivity, config)

        // Emulate Dagger Hilt's Contexts.getActivity(Context context) algorithm
        var current: Context? = localizedWrapper
        var resolvedActivity: Activity? = null
        while (current is ContextWrapper) {
            if (current is Activity) {
                resolvedActivity = current
                break
            }
            current = current.baseContext
        }

        assertNotNull("Hilt must resolve the activity from the ContextWrapper chain", resolvedActivity)
        assertEquals(fakeActivity, resolvedActivity)
    }
}
