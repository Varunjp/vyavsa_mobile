package com.vyavsa.app.feature.transactions

import com.vyavsa.app.core.model.BankPaymentSplit
import com.vyavsa.app.core.model.CreateExpenseRequest
import com.vyavsa.app.core.model.CreatePurchaseRequest
import com.vyavsa.app.core.model.Purchase
import com.vyavsa.app.core.model.PurchasePayment
import com.vyavsa.app.core.model.RecordSupplierPaymentRequest
import com.vyavsa.app.core.model.SupplierPaymentResponse
import com.vyavsa.app.core.model.UpdatePurchaseRequest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PurchaseSerializationTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true; encodeDefaults = true }

    @Test
    fun testPurchaseDeserializationFromBackendJson() {
        val payload = """
            {
                "id": "purch-101",
                "tenant_id": "tenant-abc",
                "item": "Basmati Rice 25kg",
                "quantity": 10,
                "total_amount": "12000.00",
                "payment_status": "PARTIAL",
                "customer_id": "supp-99",
                "customer_name": "Agro Supplies Ltd",
                "total_paid": "5000.00",
                "total_pending": "7000.00",
                "outstanding_amount": "7000.00",
                "created_at": "2026-10-15T09:30:00Z",
                "updated_at": "2026-10-15T09:30:00Z"
            }
        """.trimIndent()

        val purchase = json.decodeFromString<Purchase>(payload)
        assertEquals("purch-101", purchase.id)
        assertEquals("tenant-abc", purchase.tenantId)
        assertEquals("Basmati Rice 25kg", purchase.item)
        assertEquals(10, purchase.quantity)
        assertEquals("12000.00", purchase.totalAmount)
        assertEquals("PARTIAL", purchase.paymentStatus)
        assertEquals("supp-99", purchase.customerId)
        assertEquals("Agro Supplies Ltd", purchase.customerName)
        assertEquals("5000.00", purchase.totalPaid)
        assertEquals("7000.00", purchase.totalPending)
        assertEquals("7000.00", purchase.outstandingAmount)
    }

    @Test
    fun testPurchaseWithNestedPaymentsDeserialization() {
        val payload = """
            {
                "id": "purch-102",
                "item": "Cooking Oil 15L",
                "quantity": 5,
                "total_amount": "9000.00",
                "payment_status": "PAID",
                "total_paid": "9000.00",
                "total_pending": "0.00",
                "payments": [
                    {
                        "id": "pay-1",
                        "purchase_id": "purch-102",
                        "amount": "5000.00",
                        "payment_method": "cash",
                        "payment_date": "2026-10-15",
                        "created_at": "2026-10-15T10:00:00Z"
                    },
                    {
                        "id": "pay-2",
                        "purchase_id": "purch-102",
                        "amount": "4000.00",
                        "payment_method": "bank",
                        "bank_id": "bank-hdfc",
                        "bank_name": "HDFC Bank",
                        "payment_date": "2026-10-15",
                        "created_at": "2026-10-15T10:05:00Z"
                    }
                ],
                "created_at": "2026-10-15T10:00:00Z"
            }
        """.trimIndent()

        val purchase = json.decodeFromString<Purchase>(payload)
        assertEquals("purch-102", purchase.id)
        assertEquals(5, purchase.quantity)
        assertEquals(2, purchase.payments.size)
        assertEquals("pay-1", purchase.payments[0].id)
        assertEquals("5000.00", purchase.payments[0].amount)
        assertEquals("cash", purchase.payments[0].paymentMethod)
        assertEquals("pay-2", purchase.payments[1].id)
        assertEquals("bank-hdfc", purchase.payments[1].bankId)
        assertEquals("HDFC Bank", purchase.payments[1].bankName)
    }

    @Test
    fun testCreatePurchaseRequestSerializationWithBankSplits() {
        val splits = listOf(
            BankPaymentSplit(bankId = "b-1", bankName = "SBI Bank", amount = "3000.00", note = "Cheque #401"),
            BankPaymentSplit(bankId = "b-2", bankName = "ICICI Bank", amount = "2000.00")
        )
        val req = CreatePurchaseRequest(
            customerId = "supp-10",
            item = "Wheat Flour 50kg",
            quantity = 10,
            totalAmount = "12000.00",
            totalPaid = "5000.00",
            cashAmount = "5000.00",
            bankPayments = splits
        )

        val serialized = json.encodeToString(req)
        assertTrue(serialized.contains("\"item\":\"Wheat Flour 50kg\""))
        assertTrue(serialized.contains("\"quantity\":10"))
        assertTrue(serialized.contains("\"total_amount\":\"12000.00\""))
        assertTrue(serialized.contains("\"customer_id\":\"supp-10\""))
        assertTrue(serialized.contains("\"total_paid\":\"5000.00\""))
        assertTrue(serialized.contains("\"cash_amount\":\"5000.00\""))
        assertTrue(serialized.contains("\"bank_payments\""))
        assertTrue(serialized.contains("SBI Bank"))
    }

    @Test
    fun testUpdatePurchaseRequestSerialization() {
        val req = UpdatePurchaseRequest(
            item = "Mustard Oil 15L",
            quantity = 8,
            totalAmount = "13200.00",
            totalPaid = "10000.00"
        )

        val serialized = json.encodeToString(req)
        assertTrue(serialized.contains("\"item\":\"Mustard Oil 15L\""))
        assertTrue(serialized.contains("\"quantity\":8"))
        assertTrue(serialized.contains("\"total_amount\":\"13200.00\""))
        assertTrue(serialized.contains("\"total_paid\":\"10000.00\""))
    }

    @Test
    fun testRecordSupplierPaymentRequestSerialization() {
        val req = RecordSupplierPaymentRequest(
            amount = "2500.00",
            paymentMethod = "bank",
            bankId = "bank-axis",
            paymentDate = "2026-10-15",
            note = "Settlement for Invoice #89"
        )

        val serialized = json.encodeToString(req)
        assertTrue(serialized.contains("\"amount\":\"2500.00\""))
        assertTrue(serialized.contains("\"payment_method\":\"bank\""))
        assertTrue(serialized.contains("\"bank_id\":\"bank-axis\""))
        assertTrue(serialized.contains("\"payment_date\":\"2026-10-15\""))
        assertTrue(serialized.contains("\"note\":\"Settlement for Invoice #89\""))
    }

    @Test
    fun testSupplierPaymentResponseDeserialization() {
        val payload = """
            {
                "payment_id": "pay-101",
                "customer_id": "supp-99",
                "customer_name": "Agro Supplies Ltd",
                "amount": "2500.00",
                "payment_method": "cash",
                "previous_outstanding": "7000.00",
                "new_outstanding": "4500.00",
                "allocations": [
                    {
                        "purchase_id": "purch-101",
                        "item": "Basmati Rice 25kg",
                        "previous_pending": "7000.00",
                        "amount_settled": "2500.00",
                        "new_pending": "4500.00",
                        "payment_status": "PARTIAL"
                    }
                ]
            }
        """.trimIndent()

        val resp = json.decodeFromString<SupplierPaymentResponse>(payload)
        assertEquals("pay-101", resp.paymentId)
        assertEquals("supp-99", resp.customerId)
        assertEquals("2500.00", resp.amount)
        assertEquals("7000.00", resp.previousOutstanding)
        assertEquals("4500.00", resp.newOutstanding)
        assertEquals(1, resp.allocations.size)
        assertEquals("purch-101", resp.allocations[0].purchaseId)
        assertEquals("2500.00", resp.allocations[0].amountSettled)
        assertEquals("PARTIAL", resp.allocations[0].paymentStatus)
    }

    @Test
    fun testCreateExpenseRequestSerializationForEmployeeAdvance() {
        val req = CreateExpenseRequest(
            item = "Festival Advance - Suresh",
            category = "employee_advance",
            amount = "4000.00",
            totalAmount = "4000.00",
            cashAmount = "4000.00",
            employeeId = "emp-42",
            paymentMethod = "cash",
            paymentDate = "2026-10-15"
        )

        val serialized = json.encodeToString(req)
        assertTrue(serialized.contains("\"category\":\"employee_advance\""))
        assertTrue(serialized.contains("\"employee_id\":\"emp-42\""))
        assertTrue(serialized.contains("\"amount\":\"4000.00\""))
        assertTrue(serialized.contains("\"total_amount\":\"4000.00\""))
        assertTrue(serialized.contains("\"payment_method\":\"cash\""))
    }
}
