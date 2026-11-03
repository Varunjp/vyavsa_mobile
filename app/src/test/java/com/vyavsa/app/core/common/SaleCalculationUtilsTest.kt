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

    @Test
    fun testPosAllocationWithAdvanceApplication() {
        // Sale total: 1000, Advance: 300, Prev due: 500, Direct payment: 700
        val preview = SaleCalculationUtils.calculatePosAllocation(
            saleTotalStr = "1000.00",
            customerCurrentBalanceStr = "500.00",
            customerAdvanceBalanceStr = "300.00",
            directCashStr = "700.00",
            directBankStr = "0.00"
        )
        assertEquals("300.00", preview.advanceAppliedString)
        assertEquals("700.00", preview.remainingPayableString)
        assertEquals("700.00", preview.directPaymentCollectedString)
        assertEquals("0.00", preview.remainingSaleDueString)
        assertEquals("0.00", preview.excessPaymentString)
        assertEquals("0.00", preview.previousDuesSettledString)
        assertEquals("0.00", preview.advanceGeneratedString)
        assertEquals("500.00", preview.updatedCustomerDueString)
        assertEquals("0.00", preview.updatedAdvanceBalanceString)
    }

    @Test
    fun testPosAllocationExcessPaymentSettlesPreviousDuesAndGeneratesAdvance() {
        // Previous due: 1000, Sale: 600, Advance: 0, Collected: 2000
        val preview = SaleCalculationUtils.calculatePosAllocation(
            saleTotalStr = "600.00",
            customerCurrentBalanceStr = "1000.00",
            customerAdvanceBalanceStr = "0.00",
            directCashStr = "2000.00",
            directBankStr = "0.00"
        )
        assertEquals("0.00", preview.advanceAppliedString)
        assertEquals("600.00", preview.remainingPayableString)
        assertEquals("2000.00", preview.directPaymentCollectedString)
        assertEquals("0.00", preview.remainingSaleDueString)
        assertEquals("1400.00", preview.excessPaymentString)
        assertEquals("1000.00", preview.previousDuesSettledString)
        assertEquals("400.00", preview.advanceGeneratedString)
        assertEquals("0.00", preview.updatedCustomerDueString)
        assertEquals("400.00", preview.updatedAdvanceBalanceString)
    }

    @Test
    fun testPosAllocationAdvanceExceedsSale() {
        // Sale: 400, Advance: 1000, Prev due: 200, Direct payment: 0
        // Advance applied to sale: 400. Remaining advance: 600.
        // Remaining advance settles eligible previous dues: 200.
        // Dues settled: 200. Remaining dues: 0. Remaining advance: 400.
        val preview = SaleCalculationUtils.calculatePosAllocation(
            saleTotalStr = "400.00",
            customerCurrentBalanceStr = "200.00",
            customerAdvanceBalanceStr = "1000.00",
            directCashStr = "0.00",
            directBankStr = "0.00"
        )
        assertEquals("400.00", preview.advanceAppliedString)
        assertEquals("0.00", preview.remainingPayableString)
        assertEquals("0.00", preview.remainingSaleDueString)
        assertEquals("200.00", preview.previousDuesSettledString)
        assertEquals("0.00", preview.updatedCustomerDueString)
        assertEquals("400.00", preview.updatedAdvanceBalanceString)
    }

    @Test
    fun testPosAllocationScenarioA() {
        // Current sale: ₹1,000 | Previous due: ₹500 | Collected: ₹700 | Advance: ₹0
        // Expected: Current invoice outstanding: ₹300 | Prev due remaining: ₹500 | Total outstanding: ₹800 | Advance: ₹0
        val preview = SaleCalculationUtils.calculatePosAllocation(
            saleTotalStr = "1000.00",
            customerCurrentBalanceStr = "500.00",
            customerAdvanceBalanceStr = "0.00",
            directCashStr = "700.00",
            directBankStr = "0.00"
        )
        assertEquals("1000.00", preview.saleTotalString)
        assertEquals("500.00", preview.previousDuesString)
        assertEquals("700.00", preview.directPaymentCollectedString)
        assertEquals("700.00", preview.amountAllocatedToSaleString)
        assertEquals("300.00", preview.remainingSaleDueString)
        assertEquals("0.00", preview.previousDuesSettledString)
        assertEquals("800.00", preview.updatedCustomerDueString)
        assertEquals("0.00", preview.advanceGeneratedString)
        assertEquals("0.00", preview.updatedAdvanceBalanceString)
    }

    @Test
    fun testPosAllocationScenarioB() {
        // Current sale: ₹1,000 | Previous due: ₹500 | Collected: ₹1,200 | Advance: ₹0
        // Expected: Current invoice outstanding: ₹0 | Prev due remaining: ₹300 | Total outstanding: ₹300 | Advance: ₹0
        val preview = SaleCalculationUtils.calculatePosAllocation(
            saleTotalStr = "1000.00",
            customerCurrentBalanceStr = "500.00",
            customerAdvanceBalanceStr = "0.00",
            directCashStr = "1200.00",
            directBankStr = "0.00"
        )
        assertEquals("1000.00", preview.saleTotalString)
        assertEquals("500.00", preview.previousDuesString)
        assertEquals("1200.00", preview.directPaymentCollectedString)
        assertEquals("1000.00", preview.amountAllocatedToSaleString)
        assertEquals("0.00", preview.remainingSaleDueString)
        assertEquals("200.00", preview.previousDuesSettledString)
        assertEquals("300.00", preview.updatedCustomerDueString)
        assertEquals("0.00", preview.advanceGeneratedString)
        assertEquals("0.00", preview.updatedAdvanceBalanceString)
    }

    @Test
    fun testPosAllocationScenarioC() {
        // Current sale: ₹1,000 | Previous due: ₹500 | Collected: ₹1,700 | Advance: ₹0
        // Expected: Current invoice outstanding: ₹0 | Prev due remaining: ₹0 | Total outstanding: ₹0 | Advance: ₹200
        val preview = SaleCalculationUtils.calculatePosAllocation(
            saleTotalStr = "1000.00",
            customerCurrentBalanceStr = "500.00",
            customerAdvanceBalanceStr = "0.00",
            directCashStr = "1700.00",
            directBankStr = "0.00"
        )
        assertEquals("1000.00", preview.saleTotalString)
        assertEquals("500.00", preview.previousDuesString)
        assertEquals("1700.00", preview.directPaymentCollectedString)
        assertEquals("1000.00", preview.amountAllocatedToSaleString)
        assertEquals("0.00", preview.remainingSaleDueString)
        assertEquals("500.00", preview.previousDuesSettledString)
        assertEquals("0.00", preview.updatedCustomerDueString)
        assertEquals("200.00", preview.advanceGeneratedString)
        assertEquals("200.00", preview.updatedAdvanceBalanceString)
    }

    @Test
    fun testPosAllocationExistingAdvanceAndExcessPayment() {
        // Matches backend: New sale ₹1,000 | Existing Advance ₹200 | Existing Dues ₹300 | Collected ₹1,200
        // Advance applied to sale: ₹200 (sale remaining: ₹800)
        // Collected allocated to sale: ₹800 (sale now fully paid)
        // Remaining collected: ₹400
        // Dues settled from excess: ₹300 (dues now ₹0)
        // Remaining collected becomes new advance: ₹100
        val preview = SaleCalculationUtils.calculatePosAllocation(
            saleTotalStr = "1000.00",
            customerCurrentBalanceStr = "300.00",
            customerAdvanceBalanceStr = "200.00",
            directCashStr = "1200.00",
            directBankStr = "0.00"
        )
        assertEquals("200.00", preview.advanceAppliedString)
        assertEquals("800.00", preview.remainingPayableString)
        assertEquals("800.00", preview.amountAllocatedToSaleString)
        assertEquals("0.00", preview.remainingSaleDueString)
        assertEquals("400.00", preview.excessPaymentString)
        assertEquals("300.00", preview.previousDuesSettledString)
        assertEquals("100.00", preview.advanceGeneratedString)
        assertEquals("0.00", preview.updatedCustomerDueString)
        assertEquals("100.00", preview.updatedAdvanceBalanceString)
    }

    @Test
    fun testPosAllocationPartialPayment() {
        // Sale: 1000, Advance: 200, Prev due: 0, Cash: 300
        val preview = SaleCalculationUtils.calculatePosAllocation(
            saleTotalStr = "1000.00",
            customerCurrentBalanceStr = "0.00",
            customerAdvanceBalanceStr = "200.00",
            directCashStr = "300.00",
            directBankStr = "0.00"
        )
        assertEquals("200.00", preview.advanceAppliedString)
        assertEquals("800.00", preview.remainingPayableString)
        assertEquals("300.00", preview.directPaymentCollectedString)
        assertEquals("500.00", preview.remainingSaleDueString)
        assertEquals("500.00", preview.updatedCustomerDueString)
        assertEquals("0.00", preview.updatedAdvanceBalanceString)
    }

    @Test
    fun testPosAllocationContraAdjustmentPartyWithoutSupplierPayable() {
        // Sale: ₹20,000 | Supplier Payable: ₹0
        val preview = SaleCalculationUtils.calculatePosAllocation(
            saleTotalStr = "20000.00",
            customerCurrentBalanceStr = "0.00",
            customerAdvanceBalanceStr = "0.00",
            directCashStr = "0.00",
            directBankStr = "0.00",
            supplierPayableStr = "0.00"
        )
        assertEquals("20000.00", preview.saleTotalString)
        assertEquals("0.00", preview.contraAdjustmentString)
        assertEquals("0.00", preview.remainingSupplierPayableString)
        assertEquals("20000.00", preview.remainingSaleDueString)
        assertEquals("20000.00", preview.updatedCustomerDueString)
    }

    @Test
    fun testPosAllocationContraAdjustmentSaleSmallerThanPayable() {
        // Example from spec:
        // Before sale: Supplier payable: ₹50,000 | New sale: ₹20,000
        // Expected: Automatic adjustment: ₹20,000 | Remaining payable: ₹30,000 | Remaining receivable: ₹0
        val preview = SaleCalculationUtils.calculatePosAllocation(
            saleTotalStr = "20000.00",
            customerCurrentBalanceStr = "0.00",
            customerAdvanceBalanceStr = "0.00",
            directCashStr = "0.00",
            directBankStr = "0.00",
            supplierPayableStr = "50000.00"
        )
        assertEquals("20000.00", preview.saleTotalString)
        assertEquals("20000.00", preview.contraAdjustmentString)
        assertEquals("30000.00", preview.remainingSupplierPayableString)
        assertEquals("0.00", preview.remainingSaleDueString)
        assertEquals("0.00", preview.updatedCustomerDueString)
    }

    @Test
    fun testPosAllocationContraAdjustmentSaleEqualToPayable() {
        // Sale: ₹20,000 | Supplier Payable: ₹20,000
        val preview = SaleCalculationUtils.calculatePosAllocation(
            saleTotalStr = "20000.00",
            customerCurrentBalanceStr = "0.00",
            customerAdvanceBalanceStr = "0.00",
            directCashStr = "0.00",
            directBankStr = "0.00",
            supplierPayableStr = "20000.00"
        )
        assertEquals("20000.00", preview.contraAdjustmentString)
        assertEquals("0.00", preview.remainingSupplierPayableString)
        assertEquals("0.00", preview.remainingSaleDueString)
        assertEquals("0.00", preview.updatedCustomerDueString)
    }

    @Test
    fun testPosAllocationContraAdjustmentSaleGreaterThanPayable() {
        // Example from spec:
        // Before sale: Supplier payable: ₹15,000 | New sale: ₹25,000
        // Expected: Automatic adjustment: ₹15,000 | Remaining payable: ₹0 | Remaining receivable: ₹10,000
        val preview = SaleCalculationUtils.calculatePosAllocation(
            saleTotalStr = "25000.00",
            customerCurrentBalanceStr = "0.00",
            customerAdvanceBalanceStr = "0.00",
            directCashStr = "0.00",
            directBankStr = "0.00",
            supplierPayableStr = "15000.00"
        )
        assertEquals("25000.00", preview.saleTotalString)
        assertEquals("15000.00", preview.contraAdjustmentString)
        assertEquals("0.00", preview.remainingSupplierPayableString)
        assertEquals("10000.00", preview.remainingSaleDueString)
        assertEquals("10000.00", preview.updatedCustomerDueString)
    }

    @Test
    fun testPosAllocationContraAdjustmentWithCashPaymentAndAdvance() {
        // Sale: ₹25,000 | Advance: ₹2,000 | Cash: ₹5,000 | Supplier Payable: ₹12,000
        // Net sale after advance: ₹23,000
        // Cash allocated: ₹5,000 -> Due before contra: ₹18,000
        // Contra: min(18000, 12000) = ₹12,000
        // Remaining sale due: ₹6,000 | Remaining supplier payable: ₹0
        val preview = SaleCalculationUtils.calculatePosAllocation(
            saleTotalStr = "25000.00",
            customerCurrentBalanceStr = "0.00",
            customerAdvanceBalanceStr = "2000.00",
            directCashStr = "5000.00",
            directBankStr = "0.00",
            supplierPayableStr = "12000.00"
        )
        assertEquals("2000.00", preview.advanceAppliedString)
        assertEquals("5000.00", preview.amountAllocatedToSaleString)
        assertEquals("12000.00", preview.contraAdjustmentString)
        assertEquals("0.00", preview.remainingSupplierPayableString)
        assertEquals("6000.00", preview.remainingSaleDueString)
        assertEquals("6000.00", preview.updatedCustomerDueString)
    }
}
