package com.vyavsa.app.feature.transactions

import com.vyavsa.app.core.model.ApiResponse
import com.vyavsa.app.core.model.LineSaleBankBreakdown
import com.vyavsa.app.core.model.LineSalePaymentBreakdown
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LineSalePaymentBreakdownSerializationTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true; encodeDefaults = true }

    @Test
    fun testSuccessfulBreakdownSerializationWithMultipleBanks() {
        val payload = """
            {
                "success": true,
                "message": "line sale payment breakdown retrieved",
                "data": {
                    "total_sales": "85000.00",
                    "cash_received": "25000.00",
                    "bank_received": "60000.00",
                    "total_received": "85000.00",
                    "advance_applied": "0.00",
                    "total_due": "0.00",
                    "sales_count": 3,
                    "start_date": "2026-11-02",
                    "end_date": "2026-11-02",
                    "bank_breakdown": [
                        {
                            "bank_id": "bank-101",
                            "bank_name": "State Bank of India",
                            "account_number": "1234567890",
                            "amount": "30000.00",
                            "percentage": 50.0
                        },
                        {
                            "bank_id": "bank-102",
                            "bank_name": "HDFC Bank",
                            "account_number": "9876543210",
                            "amount": "20000.00",
                            "percentage": 33.33
                        },
                        {
                            "bank_id": "bank-103",
                            "bank_name": "ICICI Bank",
                            "account_number": "5555444433",
                            "amount": "10000.00",
                            "percentage": 16.67
                        }
                    ]
                }
            }
        """.trimIndent()

        val parsed = json.decodeFromString<ApiResponse<LineSalePaymentBreakdown>>(payload)
        assertTrue(parsed.success)
        assertEquals("line sale payment breakdown retrieved", parsed.message)
        assertNotNull(parsed.data)

        val data = parsed.data!!
        assertEquals("85000.00", data.totalSales)
        assertEquals("25000.00", data.cashReceived)
        assertEquals("60000.00", data.bankReceived)
        assertEquals("85000.00", data.totalReceived)
        assertEquals("0.00", data.advanceApplied)
        assertEquals("0.00", data.totalDue)
        assertEquals(3L, data.salesCount)
        assertEquals(3, data.bankBreakdown.size)

        assertEquals("State Bank of India", data.bankBreakdown[0].bankName)
        assertEquals("30000.00", data.bankBreakdown[0].amount)
        assertEquals(50.0, data.bankBreakdown[0].percentage, 0.01)

        assertEquals("HDFC Bank", data.bankBreakdown[1].bankName)
        assertEquals("20000.00", data.bankBreakdown[1].amount)
        assertEquals(33.33, data.bankBreakdown[1].percentage, 0.01)

        assertEquals("ICICI Bank", data.bankBreakdown[2].bankName)
        assertEquals("10000.00", data.bankBreakdown[2].amount)
        assertEquals(16.67, data.bankBreakdown[2].percentage, 0.01)
    }

    @Test
    fun testCashOnlySalesBreakdown() {
        val payload = """
            {
                "success": true,
                "message": "line sale payment breakdown retrieved",
                "data": {
                    "total_sales": "25000.00",
                    "cash_received": "25000.00",
                    "bank_received": "0.00",
                    "total_received": "25000.00",
                    "advance_applied": "0.00",
                    "total_due": "0.00",
                    "sales_count": 1,
                    "start_date": "2026-11-02",
                    "end_date": "2026-11-02",
                    "bank_breakdown": []
                }
            }
        """.trimIndent()

        val parsed = json.decodeFromString<ApiResponse<LineSalePaymentBreakdown>>(payload)
        val data = parsed.data!!
        assertEquals("25000.00", data.totalSales)
        assertEquals("25000.00", data.cashReceived)
        assertEquals("0.00", data.bankReceived)
        assertEquals("0.00", data.totalDue)
        assertTrue(data.bankBreakdown.isEmpty())
    }

    @Test
    fun testBankOnlySalesBreakdown() {
        val payload = """
            {
                "success": true,
                "data": {
                    "total_sales": "30000.00",
                    "cash_received": "0.00",
                    "bank_received": "30000.00",
                    "total_received": "30000.00",
                    "advance_applied": "0.00",
                    "total_due": "0.00",
                    "sales_count": 1,
                    "bank_breakdown": [
                        {
                            "bank_id": "bank-sbi",
                            "bank_name": "SBI Bank",
                            "amount": "30000.00",
                            "percentage": 100.0
                        }
                    ]
                }
            }
        """.trimIndent()

        val parsed = json.decodeFromString<ApiResponse<LineSalePaymentBreakdown>>(payload)
        val data = parsed.data!!
        assertEquals("30000.00", data.totalSales)
        assertEquals("0.00", data.cashReceived)
        assertEquals("30000.00", data.bankReceived)
        assertEquals(1, data.bankBreakdown.size)
        assertEquals("SBI Bank", data.bankBreakdown[0].bankName)
        assertEquals(100.0, data.bankBreakdown[0].percentage, 0.01)
    }

    @Test
    fun testMixedPaymentAndOutstandingDueBreakdown() {
        val payload = """
            {
                "success": true,
                "data": {
                    "total_sales": "100000.00",
                    "cash_received": "40000.00",
                    "bank_received": "35000.00",
                    "total_received": "75000.00",
                    "advance_applied": "5000.00",
                    "total_due": "20000.00",
                    "sales_count": 4,
                    "bank_breakdown": [
                        {
                            "bank_id": "bank-axis",
                            "bank_name": "Axis Bank",
                            "account_number": "11223344",
                            "amount": "35000.00",
                            "percentage": 35.0
                        }
                    ]
                }
            }
        """.trimIndent()

        val parsed = json.decodeFromString<ApiResponse<LineSalePaymentBreakdown>>(payload)
        val data = parsed.data!!
        assertEquals("100000.00", data.totalSales)
        assertEquals("40000.00", data.cashReceived)
        assertEquals("35000.00", data.bankReceived)
        assertEquals("75000.00", data.totalReceived)
        assertEquals("5000.00", data.advanceApplied)
        assertEquals("20000.00", data.totalDue)
        assertEquals(4L, data.salesCount)
        assertEquals(1, data.bankBreakdown.size)
    }

    @Test
    fun testEmptySalesBreakdown() {
        val payload = """
            {
                "success": true,
                "message": "line sale payment breakdown retrieved",
                "data": {
                    "total_sales": "0.00",
                    "cash_received": "0.00",
                    "bank_received": "0.00",
                    "total_received": "0.00",
                    "advance_applied": "0.00",
                    "total_due": "0.00",
                    "sales_count": 0,
                    "bank_breakdown": []
                }
            }
        """.trimIndent()

        val parsed = json.decodeFromString<ApiResponse<LineSalePaymentBreakdown>>(payload)
        val data = parsed.data!!
        assertEquals("0.00", data.totalSales)
        assertEquals(0L, data.salesCount)
        assertTrue(data.bankBreakdown.isEmpty())
    }

    @Test
    fun testLineSalePaymentBreakdownSerializationRoundtrip() {
        val breakdown = LineSalePaymentBreakdown(
            totalSales = "50000.00",
            cashReceived = "20000.00",
            bankReceived = "30000.00",
            totalReceived = "50000.00",
            advanceApplied = "0.00",
            totalDue = "0.00",
            salesCount = 2L,
            startDate = "2026-11-02",
            endDate = "2026-11-02",
            bankBreakdown = listOf(
                LineSaleBankBreakdown(
                    bankId = "bank-1",
                    bankName = "Punjab National Bank",
                    accountNumber = "9988776655",
                    amount = "30000.00",
                    percentage = 60.0
                )
            )
        )

        val serialized = json.encodeToString(breakdown)
        assertTrue(serialized.contains("\"total_sales\":\"50000.00\""))
        assertTrue(serialized.contains("\"cash_received\":\"20000.00\""))
        assertTrue(serialized.contains("\"bank_name\":\"Punjab National Bank\""))
        assertTrue(serialized.contains("\"percentage\":60.0"))

        val deserialized = json.decodeFromString<LineSalePaymentBreakdown>(serialized)
        assertEquals(breakdown, deserialized)
    }

    @Test
    fun testEntireSaleAdjustedAgainstAdvance() {
        val payload = """
            {
                "success": true,
                "data": {
                    "total_sales": "15000.00",
                    "cash_received": "0.00",
                    "bank_received": "0.00",
                    "total_received": "0.00",
                    "advance_applied": "15000.00",
                    "payable_applied": "0.00",
                    "total_due": "0.00",
                    "sales_count": 1,
                    "is_reconciled": true,
                    "bank_breakdown": []
                }
            }
        """.trimIndent()

        val parsed = json.decodeFromString<ApiResponse<LineSalePaymentBreakdown>>(payload)
        val data = parsed.data!!
        assertEquals("15000.00", data.totalSales)
        assertEquals("15000.00", data.advanceApplied)
        assertEquals("0.00", data.payableApplied)
        assertEquals("0.00", data.effectivePayableApplied)
        assertEquals("0.00", data.totalDue)
        assertTrue(data.isReconciled)
    }

    @Test
    fun testEntireSaleAdjustedAgainstPayable() {
        val payload = """
            {
                "success": true,
                "data": {
                    "total_sales": "20000.00",
                    "cash_received": "0.00",
                    "bank_received": "0.00",
                    "total_received": "0.00",
                    "advance_applied": "0.00",
                    "payable_applied": "20000.00",
                    "contra_amount": "20000.00",
                    "total_due": "0.00",
                    "sales_count": 1,
                    "is_reconciled": true,
                    "bank_breakdown": []
                }
            }
        """.trimIndent()

        val parsed = json.decodeFromString<ApiResponse<LineSalePaymentBreakdown>>(payload)
        val data = parsed.data!!
        assertEquals("20000.00", data.totalSales)
        assertEquals("0.00", data.advanceApplied)
        assertEquals("20000.00", data.payableApplied)
        assertEquals("20000.00", data.effectivePayableApplied)
        assertEquals("0.00", data.totalDue)
        assertTrue(data.isReconciled)
    }

    @Test
    fun testPartialAdvanceAndPartialPayableAdjustment() {
        val payload = """
            {
                "success": true,
                "data": {
                    "total_sales": "50000.00",
                    "cash_received": "10000.00",
                    "bank_received": "10000.00",
                    "total_received": "20000.00",
                    "advance_applied": "15000.00",
                    "payable_applied": "10000.00",
                    "total_due": "5000.00",
                    "sales_count": 2,
                    "is_reconciled": true,
                    "bank_breakdown": [
                        {
                            "bank_id": "b1",
                            "bank_name": "HDFC",
                            "amount": "10000.00",
                            "percentage": 20.0
                        }
                    ]
                }
            }
        """.trimIndent()

        val parsed = json.decodeFromString<ApiResponse<LineSalePaymentBreakdown>>(payload)
        val data = parsed.data!!
        assertEquals("50000.00", data.totalSales)
        assertEquals("15000.00", data.advanceApplied)
        assertEquals("10000.00", data.payableApplied)
        assertEquals("10000.00", data.effectivePayableApplied)
        assertEquals("5000.00", data.totalDue)
        assertEquals("10000.00", data.cashReceived)
        assertEquals("10000.00", data.bankReceived)
        assertTrue(data.isReconciled)
    }

    @Test
    fun testSaleWithoutAdvanceOrPayableAdjustment() {
        val payload = """
            {
                "success": true,
                "data": {
                    "total_sales": "30000.00",
                    "cash_received": "15000.00",
                    "bank_received": "0.00",
                    "total_received": "15000.00",
                    "advance_applied": "0.00",
                    "payable_applied": "0.00",
                    "total_due": "15000.00",
                    "sales_count": 1,
                    "is_reconciled": true,
                    "bank_breakdown": []
                }
            }
        """.trimIndent()

        val parsed = json.decodeFromString<ApiResponse<LineSalePaymentBreakdown>>(payload)
        val data = parsed.data!!
        assertEquals("30000.00", data.totalSales)
        assertEquals("0.00", data.advanceApplied)
        assertEquals("0.00", data.payableApplied)
        assertEquals("0.00", data.effectivePayableApplied)
        assertEquals("15000.00", data.totalDue)
        assertEquals("15000.00", data.cashReceived)
    }

    @Test
    fun testZeroValueAndMissingAllocationFields() {
        // Missing payable_applied and contra_amount entirely, nulls
        val payload = """
            {
                "success": true,
                "data": {
                    "total_sales": "10000.00",
                    "cash_received": "10000.00",
                    "bank_received": "0.00",
                    "total_received": "10000.00"
                }
            }
        """.trimIndent()

        val parsed = json.decodeFromString<ApiResponse<LineSalePaymentBreakdown>>(payload)
        val data = parsed.data!!
        assertEquals("10000.00", data.totalSales)
        assertEquals("0.00", data.advanceApplied)
        assertEquals("0.00", data.payableApplied)
        assertEquals("0.00", data.effectivePayableApplied)
        assertEquals("0.00", data.totalDue)
    }

    @Test
    fun testContraAmountFallbackWhenPayableAppliedMissing() {
        val payload = """
            {
                "success": true,
                "data": {
                    "total_sales": "25000.00",
                    "cash_received": "0.00",
                    "bank_received": "0.00",
                    "total_received": "0.00",
                    "contra_amount": "25000.00",
                    "total_due": "0.00"
                }
            }
        """.trimIndent()

        val parsed = json.decodeFromString<ApiResponse<LineSalePaymentBreakdown>>(payload)
        val data = parsed.data!!
        assertEquals("25000.00", data.totalSales)
        assertEquals("25000.00", data.contraAmount)
        assertEquals("25000.00", data.effectivePayableApplied)
    }

    @Test
    fun testFullBreakdownWithPayableRoundtrip() {
        val breakdown = LineSalePaymentBreakdown(
            totalSales = "75000.00",
            cashReceived = "25000.00",
            bankReceived = "25000.00",
            totalReceived = "50000.00",
            advanceApplied = "10000.00",
            payableApplied = "15000.00",
            contraAmount = "15000.00",
            totalDue = "0.00",
            salesCount = 3L,
            startDate = "2026-11-03",
            endDate = "2026-11-03",
            isReconciled = true,
            bankBreakdown = listOf(
                LineSaleBankBreakdown(
                    bankId = "bank-1",
                    bankName = "Axis Bank",
                    amount = "25000.00",
                    percentage = 33.33
                )
            )
        )

        val serialized = json.encodeToString(breakdown)
        assertTrue(serialized.contains("\"payable_applied\":\"15000.00\""))
        assertTrue(serialized.contains("\"contra_amount\":\"15000.00\""))
        assertTrue(serialized.contains("\"is_reconciled\":true"))

        val deserialized = json.decodeFromString<LineSalePaymentBreakdown>(serialized)
        assertEquals(breakdown, deserialized)
        assertEquals("15000.00", deserialized.effectivePayableApplied)
    }
}
