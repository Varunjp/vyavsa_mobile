package com.vyavsa.app.feature.transactions

import com.vyavsa.app.core.common.FormatUtils
import com.vyavsa.app.core.model.CreateExpenseRequest
import com.vyavsa.app.core.model.Expense
import com.vyavsa.app.core.model.UpdateExpenseRequest
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExpenseDateSerializationTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = false
    }

    @Test
    fun testCreateExpenseRequestSerialization_withExpenseDate() {
        val req = CreateExpenseRequest(
            item = "Printer Paper",
            category = "office",
            totalAmount = "350.00",
            paymentMethod = "cash",
            expenseDate = "2026-11-04",
            paymentDate = "2026-11-04",
            date = "2026-11-04"
        )
        val encoded = json.encodeToString(req)
        assertTrue(encoded.contains("\"expense_date\":\"2026-11-04\""))
        assertTrue(encoded.contains("\"payment_date\":\"2026-11-04\""))
        assertTrue(encoded.contains("\"item\":\"Printer Paper\""))
    }

    @Test
    fun testUpdateExpenseRequestSerialization_withPastExpenseDate() {
        val req = UpdateExpenseRequest(
            item = "Hardware Maintenance",
            totalAmount = "1200.00",
            expenseDate = "2026-10-15",
            paymentDate = "2026-10-15"
        )
        val encoded = json.encodeToString(req)
        assertTrue(encoded.contains("\"expense_date\":\"2026-10-15\""))
        assertTrue(encoded.contains("\"item\":\"Hardware Maintenance\""))
    }

    @Test
    fun testUpdateExpenseRequestSerialization_omittedDateDoesNotOverwriteFile() {
        val req = UpdateExpenseRequest(
            item = "Updated Name Only",
            totalAmount = "500.00"
        )
        val encoded = json.encodeToString(req)
        assertTrue(!encoded.contains("\"expense_date\""))
        assertTrue(encoded.contains("\"item\":\"Updated Name Only\""))
    }

    @Test
    fun testExpenseDeserialization_persistedExpenseDatePrioritized() {
        val rawJson = """
            {
                "id": "e-123",
                "item": "Courier Charges",
                "total_amount": "150.00",
                "category": "operations",
                "payment_method": "cash",
                "expense_date": "2026-11-03",
                "payment_date": "2026-11-03",
                "created_at": "2026-11-09T10:15:30Z",
                "updated_at": "2026-11-09T10:15:30Z"
            }
        """.trimIndent()

        val expense = json.decodeFromString<Expense>(rawJson)
        assertEquals("e-123", expense.id)
        assertEquals("Courier Charges", expense.item)
        assertEquals("2026-11-03", expense.expenseDate)
        assertEquals("2026-11-03", expense.effectiveDate)

        // Verifying display formatting reflects transaction date, not created_at
        val formatted = FormatUtils.formatDateForDisplay(expense.effectiveDate)
        assertEquals("03 Nov 2026", formatted)
    }

    @Test
    fun testExpenseDeserialization_legacyDataFallback() {
        // Legacy data where expense_date was null, but created_at is present
        val rawJson = """
            {
                "id": "e-legacy",
                "item": "Old Expense",
                "total_amount": "200.00",
                "category": "misc",
                "payment_method": "cash",
                "created_at": "2026-10-20T08:00:00Z"
            }
        """.trimIndent()

        val expense = json.decodeFromString<Expense>(rawJson)
        assertNull(expense.expenseDate)
        assertEquals("2026-10-20", expense.effectiveDate)
        assertEquals("20 Oct 2026", FormatUtils.formatDateForDisplay(expense.effectiveDate))
    }
}
