package com.vyavsa.app.core.common

import android.content.ActivityNotFoundException
import android.content.ContextWrapper
import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DialerUtilsTest {

    private class RecordingContext(private val shouldThrow: Exception? = null) : ContextWrapper(null) {
        var launchedIntent: Intent? = null

        override fun startActivity(intent: Intent?) {
            if (shouldThrow != null) {
                throw shouldThrow
            }
            launchedIntent = intent
        }
    }

    @Test
    fun testCreateDialIntentConfiguresActionAndFlag() {
        val phoneNumber = "+91 9876543210"
        val intent = DialerUtils.createDialIntent(phoneNumber)

        assertNotNull(intent)
    }

    @Test
    fun testDialPhoneNumberWithNullReturnsFalse() {
        val context = RecordingContext()
        val result = DialerUtils.dialPhoneNumber(context, null)

        assertFalse(result)
        assertEquals(null, context.launchedIntent)
    }

    @Test
    fun testDialPhoneNumberWithEmptyOrBlankReturnsFalse() {
        val context = RecordingContext()

        assertFalse(DialerUtils.dialPhoneNumber(context, ""))
        assertEquals(null, context.launchedIntent)

        assertFalse(DialerUtils.dialPhoneNumber(context, "   "))
        assertEquals(null, context.launchedIntent)
    }

    @Test
    fun testDialPhoneNumberSuccessDispatchesIntent() {
        val context = RecordingContext()
        val phoneNumber = "+91 9876543210"
        val result = DialerUtils.dialPhoneNumber(context, phoneNumber)

        assertTrue(result)
        assertNotNull(context.launchedIntent)
    }

    @Test
    fun testDialPhoneNumberHandlesActivityNotFoundGracefully() {
        val context = RecordingContext(shouldThrow = ActivityNotFoundException("No dialer"))
        val result = DialerUtils.dialPhoneNumber(context, "1234567890")

        assertFalse(result)
    }

    @Test
    fun testDialPhoneNumberHandlesGenericExceptionGracefully() {
        val context = RecordingContext(shouldThrow = SecurityException("Permission denied"))
        val result = DialerUtils.dialPhoneNumber(context, "1234567890")

        assertFalse(result)
    }
}
