package com.vyavsa.app.feature.party

import com.vyavsa.app.core.model.CreatePartyRequest
import com.vyavsa.app.core.model.Party
import com.vyavsa.app.core.model.PartyDetail
import com.vyavsa.app.core.model.PartyFinancialSummary
import com.vyavsa.app.core.model.UpdatePartyRequest
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalSerializationApi::class)
class PartySerializationTest {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
        isLenient = true
        explicitNulls = false
    }

    @Test
    fun testPartyDeserialization_dualRole() {
        val rawJson = """
            {
                "id": "pty-001",
                "name": "Acme Traders",
                "phone": "9876543210",
                "email": "acme@example.com",
                "address": "12 Commercial Street",
                "role": "both",
                "is_customer": true,
                "is_supplier": true,
                "is_both": true,
                "opening_balance": "5000.00",
                "opening_payable_balance": "2000.00",
                "total_receivable": "3500.00",
                "total_payable": "1000.00",
                "net_balance": "2500.00",
                "created_at": "2026-11-01T10:00:00Z",
                "updated_at": "2026-11-08T15:30:00Z"
            }
        """.trimIndent()

        val party = json.decodeFromString<Party>(rawJson)
        assertEquals("pty-001", party.id)
        assertEquals("Acme Traders", party.name)
        assertEquals("9876543210", party.phone)
        assertEquals("acme@example.com", party.email)
        assertEquals("12 Commercial Street", party.address)
        assertEquals("both", party.role)
        assertTrue(party.isDualRole)
        assertFalse(party.isSupplierOnly)
        assertFalse(party.isCustomerOnly)
        assertEquals("3500.00", party.totalReceivable)
        assertEquals("1000.00", party.totalPayable)
        assertEquals("2500.00", party.netBalance)
    }

    @Test
    fun testPartyDeserialization_customerOnly() {
        val rawJson = """
            {
                "id": "pty-002",
                "name": "Kavita Retail",
                "phone": "9123456780",
                "role": "customer",
                "is_customer": true,
                "is_supplier": false,
                "is_both": false,
                "total_receivable": "1200.00",
                "total_payable": "0.00"
            }
        """.trimIndent()

        val party = json.decodeFromString<Party>(rawJson)
        assertEquals("pty-002", party.id)
        assertEquals("customer", party.role)
        assertTrue(party.isCustomerOnly)
        assertFalse(party.isDualRole)
        assertFalse(party.isSupplierOnly)
        assertEquals("1200.00", party.totalReceivable)
        assertEquals("0.00", party.totalPayable)
    }

    @Test
    fun testPartyDeserialization_supplierOnly() {
        val rawJson = """
            {
                "id": "pty-003",
                "name": "Metro Wholesalers",
                "phone": "9988776655",
                "role": "supplier",
                "is_customer": false,
                "is_supplier": true,
                "is_both": false,
                "total_receivable": "0.00",
                "total_payable": "4800.00"
            }
        """.trimIndent()

        val party = json.decodeFromString<Party>(rawJson)
        assertEquals("pty-003", party.id)
        assertEquals("supplier", party.role)
        assertTrue(party.isSupplierOnly)
        assertFalse(party.isDualRole)
        assertFalse(party.isCustomerOnly)
        assertEquals("0.00", party.totalReceivable)
        assertEquals("4800.00", party.totalPayable)
    }

    @Test
    fun testCreatePartyRequestSerialization_customer() {
        val req = CreatePartyRequest(
            name = "Ramesh Kumar",
            phone = "9876501234",
            email = "ramesh@test.com",
            address = "Civil Lines",
            role = "customer",
            openingBalance = 1500.0
        )
        val encoded = json.encodeToString(req)
        assertTrue(encoded.contains("\"name\":\"Ramesh Kumar\""))
        assertTrue(encoded.contains("\"role\":\"customer\""))
        assertTrue(encoded.contains("\"opening_balance\":1500"))
        assertFalse(encoded.contains("\"opening_payable_balance\""))
    }

    @Test
    fun testCreatePartyRequestSerialization_supplier() {
        val req = CreatePartyRequest(
            name = "National Mills",
            phone = "9112233445",
            role = "supplier",
            openingPayableBalance = 8500.0
        )
        val encoded = json.encodeToString(req)
        assertTrue(encoded.contains("\"name\":\"National Mills\""))
        assertTrue(encoded.contains("\"role\":\"supplier\""))
        assertTrue(encoded.contains("\"opening_payable_balance\":8500"))
        assertFalse(encoded.contains("\"opening_balance\""))
    }

    @Test
    fun testCreatePartyRequestSerialization_dualRole() {
        val req = CreatePartyRequest(
            name = "Apex Distributing",
            phone = "9900112233",
            role = "both",
            openingBalance = 3000.0,
            openingPayableBalance = 1500.0
        )
        val encoded = json.encodeToString(req)
        assertTrue(encoded.contains("\"role\":\"both\""))
        assertTrue(encoded.contains("\"opening_balance\":3000"))
        assertTrue(encoded.contains("\"opening_payable_balance\":1500"))
    }

    @Test
    fun testUpdatePartyRequestSerialization() {
        val req = UpdatePartyRequest(
            name = "Apex Distributing Updated",
            phone = "9900112233",
            role = "both"
        )
        val encoded = json.encodeToString(req)
        assertTrue(encoded.contains("\"name\":\"Apex Distributing Updated\""))
        assertTrue(encoded.contains("\"role\":\"both\""))
    }

    @Test
    fun testPartyDetailDeserialization() {
        val rawJson = """
            {
                "id": "pty-999",
                "name": "Global Traders",
                "phone": "9811223344",
                "email": "global@example.com",
                "address": "44 Ring Road",
                "role": "both",
                "is_customer": true,
                "is_supplier": true,
                "is_both": true,
                "opening_balance": "1000.00",
                "opening_payable_balance": "500.00",
                "total_receivable": "4000.00",
                "total_payable": "1500.00",
                "net_balance": "2500.00",
                "total_sales": "12000.00",
                "total_purchases": "8000.00",
                "total_sales_paid": "9000.00",
                "total_purchases_paid": "7000.00",
                "created_at": "2026-10-01T00:00:00Z",
                "updated_at": "2026-11-05T00:00:00Z"
            }
        """.trimIndent()

        val detail = json.decodeFromString<PartyDetail>(rawJson)
        assertEquals("pty-999", detail.id)
        assertEquals("Global Traders", detail.name)
        assertEquals("4000.00", detail.totalReceivable)
        assertEquals("1500.00", detail.totalPayable)
        assertEquals("2500.00", detail.netBalance)
        assertEquals("12000.00", detail.totalSales)
        assertEquals("8000.00", detail.totalPurchases)
    }

    @Test
    fun testPartyFinancialSummaryDeserialization() {
        val rawJson = """
            {
                "party_id": "pty-888",
                "party_name": "Financial Test Party",
                "role": "both",
                "total_receivable": "6000.00",
                "total_payable": "2000.00",
                "net_balance": "4000.00",
                "total_sales": "15000.00",
                "total_purchases": "5000.00",
                "total_sales_paid": "9000.00",
                "total_purchases_paid": "3000.00"
            }
        """.trimIndent()

        val summary = json.decodeFromString<PartyFinancialSummary>(rawJson)
        assertEquals("pty-888", summary.partyId)
        assertEquals("Financial Test Party", summary.partyName)
        assertEquals("6000.00", summary.totalReceivable)
        assertEquals("2000.00", summary.totalPayable)
        assertEquals("4000.00", summary.netBalance)
    }
}
