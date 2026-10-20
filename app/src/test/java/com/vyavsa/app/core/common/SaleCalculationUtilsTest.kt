package com.vyavsa.app.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SaleCalculationUtilsTest {

    @Test
    fun testCalculateSaleTotalStandard() {
        val finalAmount = SaleCalculationUtils.calculateSaleTotal(
            subtotalStr = "1000.00",
            discountStr = "100.00"
        )
        assertEquals("900.00", finalAmount)
    }

    @Test
    fun testCalculateSaleTotalZeroDiscount() {
        val finalAmount = SaleCalculationUtils.calculateSaleTotal(
            subtotalStr = "1000.00",
            discountStr = "0.00"
        )
        assertEquals("1000.00", finalAmount)
    }

    @Test
    fun testCalculateSaleTotalEmptyDiscount() {
        val finalAmount = SaleCalculationUtils.calculateSaleTotal(
            subtotalStr = "1000.00",
            discountStr = ""
        )
        assertEquals("1000.00", finalAmount)
    }

    @Test
    fun testCalculateSaleTotalDiscountEqualsSubtotal() {
        val finalAmount = SaleCalculationUtils.calculateSaleTotal(
            subtotalStr = "500.00",
            discountStr = "500.00"
        )
        assertEquals("0.00", finalAmount)
    }

    @Test
    fun testCalculateSaleTotalDiscountExceedingSubtotalClampedToZero() {
        val finalAmount = SaleCalculationUtils.calculateSaleTotal(
            subtotalStr = "500.00",
            discountStr = "600.00"
        )
        assertEquals("0.00", finalAmount)
    }

    @Test
    fun testCalculateSaleTotalWithCommasAndWhitespace() {
        val finalAmount = SaleCalculationUtils.calculateSaleTotal(
            subtotalStr = " 1,250.50 ",
            discountStr = " 250.50 "
        )
        assertEquals("1000.00", finalAmount)
    }

    @Test
    fun testCalculateDueStandard() {
        val due = SaleCalculationUtils.calculateDue(
            finalTotal = "1000.00",
            cash = "400.00",
            bank = "300.00"
        )
        assertEquals("300.00", due)
    }

    @Test
    fun testCalculateDueFullyPaid() {
        val due = SaleCalculationUtils.calculateDue(
            finalTotal = "1000.00",
            cash = "1000.00",
            bank = "0.00"
        )
        assertEquals("0.00", due)
    }

    @Test
    fun testCalculateDueOverpaidClampedToZero() {
        val due = SaleCalculationUtils.calculateDue(
            finalTotal = "500.00",
            cash = "600.00",
            bank = "0.00"
        )
        assertEquals("0.00", due)
    }

    @Test
    fun testValidateDiscountValid() {
        assertNull(SaleCalculationUtils.validateDiscount(subtotal = "1000.00", discount = "100.00"))
        assertNull(SaleCalculationUtils.validateDiscount(subtotal = "1000.00", discount = "0.00"))
        assertNull(SaleCalculationUtils.validateDiscount(subtotal = "1000.00", discount = ""))
        assertNull(SaleCalculationUtils.validateDiscount(subtotal = "1000.00", discount = "1000.00"))
    }

    @Test
    fun testValidateDiscountNegative() {
        val error = SaleCalculationUtils.validateDiscount(subtotal = "1000.00", discount = "-50.00")
        assertNotNull(error)
        assertTrue(error!!.contains("Discount cannot be negative", ignoreCase = true))
    }

    @Test
    fun testValidateDiscountExceedsSubtotal() {
        val error = SaleCalculationUtils.validateDiscount(subtotal = "1000.00", discount = "1000.01")
        assertNotNull(error)
        assertTrue(error!!.contains("Discount cannot exceed subtotal", ignoreCase = true))
    }

    @Test
    fun testValidateDiscountMalformed() {
        val error = SaleCalculationUtils.validateDiscount(subtotal = "1000.00", discount = "abc")
        assertNotNull(error)
        assertTrue(error!!.contains("Discount must be a valid number", ignoreCase = true))
    }

    @Test
    fun testFormatDecimalRounding() {
        assertEquals("123.46", SaleCalculationUtils.formatDecimal(123.456))
        assertEquals("123.45", SaleCalculationUtils.formatDecimal(123.454))
        assertEquals("0.00", SaleCalculationUtils.formatDecimal(0.0))
    }
}
