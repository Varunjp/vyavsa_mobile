package com.vyavsa.app.feature.transactions

import com.vyavsa.app.core.model.ApiResponse
import com.vyavsa.app.core.model.CounterSaleBankBreakdown
import com.vyavsa.app.core.model.CounterSalePaymentBreakdown
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CounterSalePaymentBreakdownSerializationTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true; encodeDefaults = true }

    @Test
    fun testSuccessfulBreakdownSerializationWithMultipleBanks() {
        val payload = """
            {
                "success": true,
                "message": "counter sale payment breakdown retrieved",
                "data": {
                    "total_sales": "85000.00",
                    "cash_received": "25000.00",
                    "bank_received": "60000.00",
                    "total_received": "85000.00",
                    "advance_applied": "0.00",
                    "payable_applied": "0.00",
                    "contra_amount": "0.00",
                    "total_due": "0.00",
                    "sales_count": 3,
                    "start_date": "2026-11-04",
                    "end_date": "2026-11-04",
                    "is_reconciled": true,
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

        val parsed = json.decodeFromString<ApiResponse<CounterSalePaymentBreakdown>>(payload)
        assertTrue(parsed.success)
        assertEquals("counter sale payment breakdown retrieved", parsed.message)
        assertNotNull(parsed.data)

        val data = parsed.data!!
        assertEquals("85000.00", data.totalSales)
        assertEquals("25000.00", data.cashReceived)
        assertEquals("60000.00", data.bankReceived)
        assertEquals("85000.00", data.totalReceived)
        assertEquals("0.00", data.advanceApplied)
        assertEquals("0.00", data.payableApplied)
        assertEquals("0.00", data.totalDue)
        assertEquals(3L, data.salesCount)
        assertTrue(data.isReconciled)
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
                "message": "counter sale payment breakdown retrieved",
                "data": {
                    "total_sales": "25000.00",
                    "cash_received": "25000.00",
                    "bank_received": "0.00",
                    "total_received": "25000.00",
                    "advance_applied": "0.00",
                    "payable_applied": "0.00",
                    "total_due": "0.00",
                    "sales_count": 1,
                    "start_date": "2026-11-04",
                    "end_date": "2026-11-04",
                    "is_reconciled": true,
                    "bank_breakdown": []
                }
            }
        """.trimIndent()

        val parsed = json.decodeFromString<ApiResponse<CounterSalePaymentBreakdown>>(payload)
        val data = parsed.data!!
        assertEquals("25000.00", data.totalSales)
        assertEquals("25000.00", data.cashReceived)
        assertEquals("0.00", data.bankReceived)
        assertEquals("0.00", data.totalDue)
        assertTrue(data.bankBreakdown.isEmpty())
        assertTrue(data.isReconciled)
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
                    "payable_applied": "0.00",
                    "total_due": "0.00",
                    "sales_count": 1,
                    "is_reconciled": true,
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

        val parsed = json.decodeFromString<ApiResponse<CounterSalePaymentBreakdown>>(payload)
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
                    "payable_applied": "0.00",
                    "total_due": "20000.00",
                    "sales_count": 4,
                    "is_reconciled": true,
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

        val parsed = json.decodeFromString<ApiResponse<CounterSalePaymentBreakdown>>(payload)
        val data = parsed.data!!
        assertEquals("100000.00", data.totalSales)
        assertEquals("40000.00", data.cashReceived)
        assertEquals("35000.00", data.bankReceived)
        assertEquals("75000.00", data.totalReceived)
        assertEquals("5000.00", data.advanceApplied)
        assertEquals("20000.00", data.totalDue)
        assertEquals(4L, data.salesCount)
        assertEquals(1, data.bankBreakdown.size)
        assertEquals("Axis Bank", data.bankBreakdown[0].bankName)
        assertEquals("35000.00", data.bankBreakdown[0].amount)
        assertEquals(35.0, data.bankBreakdown[0].percentage, 0.01)
    }

    @Test
    fun testAdvanceAdjustmentBreakdown() {
        val payload = """
            {
                "success": true,
                "data": {
                    "total_sales": "50000.00",
                    "cash_received": "0.00",
                    "bank_received": "0.00",
                    "total_received": "0.00",
                    "advance_applied": "50000.00",
                    "payable_applied": "0.00",
                    "total_due": "0.00",
                    "sales_count": 1,
                    "is_reconciled": true,
                    "bank_breakdown": []
                }
            }
        """.trimIndent()

        val parsed = json.decodeFromString<ApiResponse<CounterSalePaymentBreakdown>>(payload)
        val data = parsed.data!!
        assertEquals("50000.00", data.totalSales)
        assertEquals("50000.00", data.advanceApplied)
        assertEquals("0.00", data.totalDue)
    }

    @Test
    fun testPayableAdjustmentBreakdown() {
        val payload = """
            {
                "success": true,
                "data": {
                    "total_sales": "60000.00",
                    "cash_received": "0.00",
                    "bank_received": "0.00",
                    "total_received": "0.00",
                    "advance_applied": "0.00",
                    "payable_applied": "60000.00",
                    "contra_amount": "60000.00",
                    "total_due": "0.00",
                    "sales_count": 1,
                    "is_reconciled": true,
                    "bank_breakdown": []
                }
            }
        """.trimIndent()

        val parsed = json.decodeFromString<ApiResponse<CounterSalePaymentBreakdown>>(payload)
        val data = parsed.data!!
        assertEquals("60000.00", data.totalSales)
        assertEquals("60000.00", data.payableApplied)
        assertEquals("60000.00", data.effectivePayableApplied)
        assertEquals("0.00", data.totalDue)
    }

    @Test
    fun testMixedAdvanceAndPayableAdjustmentBreakdown() {
        val payload = """
            {
                "success": true,
                "data": {
                    "total_sales": "100000.00",
                    "cash_received": "20000.00",
                    "bank_received": "30000.00",
                    "total_received": "50000.00",
                    "advance_applied": "25000.00",
                    "payable_applied": "25000.00",
                    "total_due": "0.00",
                    "sales_count": 2,
                    "is_reconciled": true,
                    "bank_breakdown": [
                        {
                            "bank_id": "b1",
                            "bank_name": "Canara Bank",
                            "amount": "30000.00",
                            "percentage": 30.0
                        }
                    ]
                }
            }
        """.trimIndent()

        val parsed = json.decodeFromString<ApiResponse<CounterSalePaymentBreakdown>>(payload)
        val data = parsed.data!!
        assertEquals("100000.00", data.totalSales)
        assertEquals("20000.00", data.cashReceived)
        assertEquals("30000.00", data.bankReceived)
        assertEquals("25000.00", data.advanceApplied)
        assertEquals("25000.00", data.payableApplied)
        assertEquals("25000.00", data.effectivePayableApplied)
        assertEquals("0.00", data.totalDue)
    }

    @Test
    fun testContraAmountFallbackWhenPayableAppliedIsZero() {
        val payload = """
            {
                "success": true,
                "data": {
                    "total_sales": "45000.00",
                    "cash_received": "0.00",
                    "bank_received": "0.00",
                    "total_received": "0.00",
                    "advance_applied": "0.00",
                    "payable_applied": "0.00",
                    "contra_amount": "45000.00",
                    "total_due": "0.00",
                    "sales_count": 1,
                    "is_reconciled": true,
                    "bank_breakdown": []
                }
            }
        """.trimIndent()

        val parsed = json.decodeFromString<ApiResponse<CounterSalePaymentBreakdown>>(payload)
        val data = parsed.data!!
        assertEquals("0.00", data.payableApplied)
        assertEquals("45000.00", data.effectivePayableApplied)
    }

    @Test
    fun testEmptySalesBreakdown() {
        val payload = """
            {
                "success": true,
                "message": "counter sale payment breakdown retrieved",
                "data": {
                    "total_sales": "0.00",
                    "cash_received": "0.00",
                    "bank_received": "0.00",
                    "total_received": "0.00",
                    "advance_applied": "0.00",
                    "payable_applied": "0.00",
                    "contra_amount": "0.00",
                    "total_due": "0.00",
                    "sales_count": 0,
                    "is_reconciled": true,
                    "bank_breakdown": []
                }
            }
        """.trimIndent()

        val parsed = json.decodeFromString<ApiResponse<CounterSalePaymentBreakdown>>(payload)
        val data = parsed.data!!
        assertEquals("0.00", data.totalSales)
        assertEquals("0.00", data.cashReceived)
        assertEquals("0.00", data.bankReceived)
        assertEquals(0L, data.salesCount)
        assertTrue(data.bankBreakdown.isEmpty())
        assertTrue(data.isReconciled)
    }

    @Test
    fun testMissingOptionalFieldsSerialization() {
        val payload = """
            {
                "success": true,
                "data": {
                    "total_sales": "1000.00"
                }
            }
        """.trimIndent()

        val parsed = json.decodeFromString<ApiResponse<CounterSalePaymentBreakdown>>(payload)
        val data = parsed.data!!
        assertEquals("1000.00", data.totalSales)
        assertEquals("0.00", data.cashReceived)
        assertEquals("0.00", data.bankReceived)
        assertEquals("0.00", data.totalDue)
        assertEquals(0L, data.salesCount)
        assertTrue(data.bankBreakdown.isEmpty())
    }

    @Test
    fun testRoundtripSerialization() {
        val original = CounterSalePaymentBreakdown(
            totalSales = "50000.00",
            cashReceived = "20000.00",
            bankReceived = "30000.00",
            totalReceived = "50000.00",
            advanceApplied = "0.00",
            payableApplied = "0.00",
            totalDue = "0.00",
            salesCount = 2L,
            startDate = "2026-11-04",
            endDate = "2026-11-04",
            isReconciled = true,
            bankBreakdown = listOf(
                CounterSaleBankBreakdown(
                    bankId = "bank-1",
                    bankName = "Kotak Bank",
                    accountNumber = "998877",
                    amount = "30000.00",
                    percentage = 60.0
                )
            )
        )

        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<CounterSalePaymentBreakdown>(encoded)
        assertEquals(original, decoded)
    }
}
