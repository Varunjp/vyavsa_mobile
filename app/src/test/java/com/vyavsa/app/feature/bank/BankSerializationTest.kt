package com.vyavsa.app.feature.bank

import com.vyavsa.app.core.model.ApiResponse
import com.vyavsa.app.core.model.CreateBankRequest
import com.vyavsa.app.core.model.TenantBank
import com.vyavsa.app.core.model.UpdateBankRequest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BankSerializationTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true; encodeDefaults = true }

    @Test
    fun testTenantBankListDeserialization() {
        val payload = """
            {
              "success": true,
              "message": "bank accounts retrieved",
              "data": [
                {
                  "id": "bank-uuid-1",
                  "tenant_id": "tenant-uuid-1",
                  "bank_name": "State Bank of India",
                  "account_number": "123456789012",
                  "ifsc": "SBIN0001234",
                  "current_balance": "55000.75",
                  "status": "active",
                  "created_at": "2026-10-01T12:00:00Z",
                  "updated_at": "2026-10-15T08:00:00Z"
                },
                {
                  "id": "bank-uuid-2",
                  "tenant_id": "tenant-uuid-1",
                  "bank_name": "HDFC Bank",
                  "account_number": "987654321098",
                  "ifsc": "HDFC0005678",
                  "current_balance": "12500.00",
                  "status": "active"
                }
              ]
            }
        """.trimIndent()

        val response = json.decodeFromString<ApiResponse<List<TenantBank>>>(payload)
        assertTrue(response.success)
        assertNotNull(response.data)
        assertEquals(2, response.data?.size)

        val first = response.data?.get(0)
        assertEquals("bank-uuid-1", first?.id)
        assertEquals("State Bank of India", first?.bankName)
        assertEquals("123456789012", first?.accountNumber)
        assertEquals("SBIN0001234", first?.ifsc)
        assertEquals("55000.75", first?.currentBalance)
        assertEquals(55000.75, first?.currentBalance?.toDoubleOrNull() ?: 0.0, 0.001)

        val second = response.data?.get(1)
        assertEquals("bank-uuid-2", second?.id)
        assertEquals("HDFC Bank", second?.bankName)
        assertEquals("12500.00", second?.currentBalance)
    }

    @Test
    fun testCreateBankRequestSerialization() {
        val req = CreateBankRequest(
            bankName = "Axis Bank",
            accountNumber = "998877665544",
            ifsc = "UTIB0000123",
            openingBalance = "15000.00",
            status = "active"
        )
        val serialized = json.encodeToString(req)
        assertTrue(serialized.contains("\"bank_name\":\"Axis Bank\""))
        assertTrue(serialized.contains("\"account_number\":\"998877665544\""))
        assertTrue(serialized.contains("\"ifsc\":\"UTIB0000123\""))
        assertTrue(serialized.contains("\"opening_balance\":\"15000.00\""))
        assertTrue(serialized.contains("\"status\":\"active\""))
    }

    @Test
    fun testUpdateBankRequestSerialization() {
        val req = UpdateBankRequest(
            bankName = "Axis Bank Current",
            accountNumber = "998877665544",
            ifsc = "UTIB0000123",
            status = "active"
        )
        val serialized = json.encodeToString(req)
        assertTrue(serialized.contains("\"bank_name\":\"Axis Bank Current\""))
        assertTrue(serialized.contains("\"account_number\":\"998877665544\""))
        assertTrue(serialized.contains("\"ifsc\":\"UTIB0000123\""))
        assertTrue(serialized.contains("\"status\":\"active\""))
    }
}
