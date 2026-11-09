package com.vyavsa.app

import com.vyavsa.app.core.common.FormatUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.TimeZone

class FormatUtilsTest {

    @Test
    fun testFormatCurrency() {
        val formatted = FormatUtils.formatCurrency(1500.50)
        // Should contain 1,500 and currency symbol
        assertTrue(formatted.contains("1,500") || formatted.contains("1500"))
    }

    @Test
    fun testFormatCurrencyFromString() {
        val formatted = FormatUtils.formatCurrency("25000.00")
        assertTrue(formatted.contains("25,000") || formatted.contains("25000"))
    }

    @Test
    fun testFormatDateForDisplay() {
        val formatted = FormatUtils.formatDateForDisplay("2026-10-14T10:30:00Z")
        assertTrue(formatted.contains("14 Oct 2026"))
    }

    @Test
    fun testFormatSimpleDateForDisplay() {
        val formatted = FormatUtils.formatDateForDisplay("2026-10-14")
        assertTrue(formatted.contains("14 Oct 2026"))
    }

    @Test
    fun testTodayDateStringFormat() {
        val today = FormatUtils.todayDateString()
        assertTrue(today.matches(Regex("""^\d{4}-\d{2}-\d{2}$""")))
        assertTrue(FormatUtils.isValidIsoDate(today))
    }

    @Test
    fun testUtcMillisToLocalDateStringAndRoundTrip() {
        val originalTz = TimeZone.getDefault()
        try {
            // Test across contrasting time zones (negative offset US/Eastern, zero UTC, positive offset IST)
            val testTimezones = listOf(
                TimeZone.getTimeZone("America/New_York"), // UTC-5 / UTC-4
                TimeZone.getTimeZone("UTC"),
                TimeZone.getTimeZone("Asia/Kolkata"),     // UTC+5:30
                TimeZone.getTimeZone("Pacific/Auckland")  // UTC+12 / UTC+13
            )

            val testDates = listOf(
                "2026-11-09",
                "2026-01-01",
                "2026-02-28",
                "2024-02-29", // leap day
                "2020-12-31"  // historical past date
            )

            for (tz in testTimezones) {
                TimeZone.setDefault(tz)
                for (dateStr in testDates) {
                    val millis = FormatUtils.localDateStringToUtcMillis(dateStr)
                    assertNotNull("Millis should not be null for $dateStr in $tz", millis)
                    val formattedBack = FormatUtils.utcMillisToLocalDateString(millis!!)
                    assertEquals("Date must not drift in timezone $tz", dateStr, formattedBack)
                }
            }
        } finally {
            TimeZone.setDefault(originalTz)
        }
    }

    @Test
    fun testIsValidIsoDate() {
        assertTrue(FormatUtils.isValidIsoDate("2026-11-09"))
        assertTrue(FormatUtils.isValidIsoDate("2020-01-01"))
        assertTrue(FormatUtils.isValidIsoDate("2024-02-29")) // Leap day

        assertFalse(FormatUtils.isValidIsoDate(null))
        assertFalse(FormatUtils.isValidIsoDate(""))
        assertFalse(FormatUtils.isValidIsoDate("   "))
        assertFalse(FormatUtils.isValidIsoDate("2026/11/09"))
        assertFalse(FormatUtils.isValidIsoDate("09-11-2026"))
        assertFalse(FormatUtils.isValidIsoDate("2026-13-01")) // invalid month
        assertFalse(FormatUtils.isValidIsoDate("2026-02-30")) // invalid day
        assertFalse(FormatUtils.isValidIsoDate("not-a-date"))
    }
}
