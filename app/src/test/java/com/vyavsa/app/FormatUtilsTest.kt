package com.vyavsa.app

import com.vyavsa.app.core.common.FormatUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

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
}
