package com.vyavsa.app.feature.transactions

import com.vyavsa.app.core.common.FormatUtils
import com.vyavsa.app.core.common.SaleCalculationUtils
import com.vyavsa.app.core.model.ApiResponse
import com.vyavsa.app.core.model.LineSale
import com.vyavsa.app.core.model.LineSaleBankBreakdown
import com.vyavsa.app.core.model.LineSalePaymentBreakdown
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

/**
 * Targeted verification test suite ensuring financial correctness and allocation breakup display
 * across all 6 core scenarios:
 * 1. Entire sale adjusted against advance.
 * 2. Entire sale adjusted against payable.
 * 3. Partial advance and partial payable adjustment.
 * 4. Sale without advance or payable adjustment.
 * 5. Customer with both advance and outstanding payable.
 * 6. Zero-value and missing allocation fields.
 */
class LineSaleBreakupAllocationTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true; encodeDefaults = true }

    // -------------------------------------------------------------
    // Scenario 1: Entire sale adjusted against advance
    // -------------------------------------------------------------
    @Test
    fun testScenario1_EntireSaleAdjustedAgainstAdvance() {
        val payload = """
            {
                "success": true,
                "data": {
                    "total_sales": "10000.00",
                    "cash_received": "0.00",
                    "bank_received": "0.00",
                    "total_received": "0.00",
                    "advance_applied": "10000.00",
                    "payable_applied": "0.00",
                    "contra_amount": "0.00",
                    "total_due": "0.00",
                    "sales_count": 1,
                    "is_reconciled": true,
                    "bank_breakdown": []
                }
            }
        """.trimIndent()

        val parsed = json.decodeFromString<ApiResponse<LineSalePaymentBreakdown>>(payload)
        val data = parsed.data!!

        // Assert 4 core breakup fields
        assertEquals("10000.00", data.totalSales)
        assertEquals("10000.00", data.advanceApplied)
        assertEquals("0.00", data.effectivePayableApplied)
        assertEquals("0.00", data.totalDue)

        // Mathematical reconciliation
        val total = BigDecimal(data.totalSales)
        val advance = BigDecimal(data.advanceApplied)
        val payable = BigDecimal(data.effectivePayableApplied)
        val due = BigDecimal(data.totalDue)
        val direct = BigDecimal(data.totalReceived)

        assertEquals(total, advance + payable + due + direct)
        assertTrue(data.isReconciled)

        // Format validation
        assertEquals("₹10,000.00", FormatUtils.formatCurrency(data.totalSales))
        assertEquals("₹10,000.00", FormatUtils.formatCurrency(data.advanceApplied))
        assertEquals("₹0.00", FormatUtils.formatCurrency(data.effectivePayableApplied))
        assertEquals("₹0.00", FormatUtils.formatCurrency(data.totalDue))
    }

    // -------------------------------------------------------------
    // Scenario 2: Entire sale adjusted against payable
    // -------------------------------------------------------------
    @Test
    fun testScenario2_EntireSaleAdjustedAgainstPayable() {
        val payload = """
            {
                "success": true,
                "data": {
                    "total_sales": "12500.00",
                    "cash_received": "0.00",
                    "bank_received": "0.00",
                    "total_received": "0.00",
                    "advance_applied": "0.00",
                    "payable_applied": "12500.00",
                    "contra_amount": "12500.00",
                    "total_due": "0.00",
                    "sales_count": 1,
                    "is_reconciled": true,
                    "bank_breakdown": []
                }
            }
        """.trimIndent()

        val parsed = json.decodeFromString<ApiResponse<LineSalePaymentBreakdown>>(payload)
        val data = parsed.data!!

        // Assert 4 core breakup fields
        assertEquals("12500.00", data.totalSales)
        assertEquals("0.00", data.advanceApplied)
        assertEquals("12500.00", data.effectivePayableApplied)
        assertEquals("0.00", data.totalDue)

        // Mathematical reconciliation
        val total = BigDecimal(data.totalSales)
        val advance = BigDecimal(data.advanceApplied)
        val payable = BigDecimal(data.effectivePayableApplied)
        val due = BigDecimal(data.totalDue)
        val direct = BigDecimal(data.totalReceived)

        assertEquals(total, advance + payable + due + direct)
        assertTrue(data.isReconciled)

        // Single LineSale entity domain mapping verification
        val sale = LineSale(
            id = "sale-p-1",
            totalAmount = "12500.00",
            contraAmount = "12500.00",
            payableApplied = "12500.00",
            advanceApplied = "0.00",
            balance = "0.00"
        )
        assertEquals("12500.00", sale.effectivePayableAdjustment)
        assertEquals("0.00", sale.balance)
    }

    // -------------------------------------------------------------
    // Scenario 3: Partial advance and partial payable adjustment
    // -------------------------------------------------------------
    @Test
    fun testScenario3_PartialAdvanceAndPartialPayableAdjustment() {
        val payload = """
            {
                "success": true,
                "data": {
                    "total_sales": "50000.00",
                    "cash_received": "5000.00",
                    "bank_received": "10000.00",
                    "total_received": "15000.00",
                    "advance_applied": "15000.00",
                    "payable_applied": "12000.00",
                    "contra_amount": "12000.00",
                    "total_due": "8000.00",
                    "sales_count": 3,
                    "is_reconciled": true,
                    "bank_breakdown": [
                        {
                            "bank_id": "b-sbi",
                            "bank_name": "SBI",
                            "amount": "10000.00",
                            "percentage": 20.0
                        }
                    ]
                }
            }
        """.trimIndent()

        val parsed = json.decodeFromString<ApiResponse<LineSalePaymentBreakdown>>(payload)
        val data = parsed.data!!

        // Assert 4 core breakup fields
        assertEquals("50000.00", data.totalSales)
        assertEquals("15000.00", data.advanceApplied)
        assertEquals("12000.00", data.effectivePayableApplied)
        assertEquals("8000.00", data.totalDue)

        // Mathematical reconciliation: Total = Advance + Payable + Cash + Bank + Due
        val total = BigDecimal(data.totalSales)
        val advance = BigDecimal(data.advanceApplied)
        val payable = BigDecimal(data.effectivePayableApplied)
        val cash = BigDecimal(data.cashReceived)
        val bank = BigDecimal(data.bankReceived)
        val due = BigDecimal(data.totalDue)

        assertEquals(total, advance + payable + cash + bank + due)
        assertTrue(data.isReconciled)
    }

    // -------------------------------------------------------------
    // Scenario 4: Sale without advance or payable adjustment
    // -------------------------------------------------------------
    @Test
    fun testScenario4_SaleWithoutAdvanceOrPayableAdjustment() {
        val payload = """
            {
                "success": true,
                "data": {
                    "total_sales": "25000.00",
                    "cash_received": "10000.00",
                    "bank_received": "5000.00",
                    "total_received": "15000.00",
                    "advance_applied": "0.00",
                    "payable_applied": "0.00",
                    "contra_amount": "0.00",
                    "total_due": "10000.00",
                    "sales_count": 1,
                    "is_reconciled": true,
                    "bank_breakdown": []
                }
            }
        """.trimIndent()

        val parsed = json.decodeFromString<ApiResponse<LineSalePaymentBreakdown>>(payload)
        val data = parsed.data!!

        assertEquals("25000.00", data.totalSales)
        assertEquals("0.00", data.advanceApplied)
        assertEquals("0.00", data.effectivePayableApplied)
        assertEquals("10000.00", data.totalDue)
        assertEquals("10000.00", data.cashReceived)
        assertEquals("5000.00", data.bankReceived)

        val total = BigDecimal(data.totalSales)
        val direct = BigDecimal(data.totalReceived)
        val due = BigDecimal(data.totalDue)
        assertEquals(total, direct + due)
    }

    // -------------------------------------------------------------
    // Scenario 5: Customer with both advance and outstanding payable
    // -------------------------------------------------------------
    @Test
    fun testScenario5_CustomerWithBothAdvanceAndOutstandingPayable() {
        // Customer has advance balance ₹4,000 and supplier payable ₹6,000
        // Sale total is ₹10,000. Cash/Bank is 0.
        // POS preview should allocate:
        // 1. Advance applied: min(10000, 4000) = 4000
        // 2. Remaining payable on sale: 6000
        // 3. Contra adjustment: min(6000, 6000) = 6000
        // 4. Remaining sale due: 0
        val preview = SaleCalculationUtils.calculatePosAllocation(
            saleTotalStr = "10000.00",
            customerCurrentBalanceStr = "0.00",
            customerAdvanceBalanceStr = "4000.00",
            directCashStr = "0.00",
            directBankStr = "0.00",
            supplierPayableStr = "6000.00"
        )

        assertEquals("10000.00", preview.saleTotalString)
        assertEquals("4000.00", preview.advanceAppliedString)
        assertEquals("6000.00", preview.contraAdjustmentString)
        assertEquals("0.00", preview.remainingSaleDueString)
        assertEquals("10000.00", preview.totalSettledForSaleString)

        // Matching breakdown response from backend
        val payload = """
            {
                "success": true,
                "data": {
                    "total_sales": "10000.00",
                    "cash_received": "0.00",
                    "bank_received": "0.00",
                    "total_received": "0.00",
                    "advance_applied": "4000.00",
                    "payable_applied": "6000.00",
                    "contra_amount": "6000.00",
                    "total_due": "0.00",
                    "sales_count": 1,
                    "is_reconciled": true,
                    "bank_breakdown": []
                }
            }
        """.trimIndent()

        val parsed = json.decodeFromString<ApiResponse<LineSalePaymentBreakdown>>(payload)
        val data = parsed.data!!
        assertEquals(preview.saleTotalString, data.totalSales)
        assertEquals(preview.advanceAppliedString, data.advanceApplied)
        assertEquals(preview.contraAdjustmentString, data.effectivePayableApplied)
        assertEquals(preview.remainingSaleDueString, data.totalDue)
    }

    // -------------------------------------------------------------
    // Scenario 6: Zero-value and missing allocation fields
    // -------------------------------------------------------------
    @Test
    fun testScenario6_ZeroValueAndMissingAllocationFields() {
        // Missing advance_applied, payable_applied, contra_amount, is_reconciled
        val payload = """
            {
                "success": true,
                "data": {
                    "total_sales": "0.00",
                    "cash_received": "0.00",
                    "bank_received": "0.00",
                    "total_received": "0.00"
                }
            }
        """.trimIndent()

        val parsed = json.decodeFromString<ApiResponse<LineSalePaymentBreakdown>>(payload)
        val data = parsed.data!!

        // Safe defaults should kick in without throwing exceptions
        assertEquals("0.00", data.totalSales)
        assertEquals("0.00", data.advanceApplied)
        assertEquals("0.00", data.payableApplied)
        assertEquals("0.00", data.effectivePayableApplied)
        assertEquals("0.00", data.totalDue)
        assertTrue(data.isReconciled)
        assertTrue(data.bankBreakdown.isEmpty())

        // Ensure formatting doesn't fail on zeroes
        assertEquals("₹0.00", FormatUtils.formatCurrency(data.totalSales))
        assertEquals("₹0.00", FormatUtils.formatCurrency(data.advanceApplied))
        assertEquals("₹0.00", FormatUtils.formatCurrency(data.effectivePayableApplied))
        assertEquals("₹0.00", FormatUtils.formatCurrency(data.totalDue))
    }
}
