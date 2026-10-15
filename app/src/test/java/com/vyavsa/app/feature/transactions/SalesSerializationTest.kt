package com.vyavsa.app.feature.transactions

import com.vyavsa.app.core.model.ApiResponse
import com.vyavsa.app.core.model.CounterSale
import com.vyavsa.app.core.model.CreateCounterSaleRequest
import com.vyavsa.app.core.model.CreateLineSaleRequest
import com.vyavsa.app.core.model.LineSale
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SalesSerializationTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true; encodeDefaults = true }

    @Test
    fun testCreateLineSaleRequestSerializationWithBank() {
        val req = CreateLineSaleRequest(
            customerId = "cust-123",
            route = "East Sector",
            salesman = "Sunil",
            totalAmount = "1500.00",
            totalCashIn = "1000.00",
            cashAmount = "1000.00",
            bankAmount = "500.00",
            bankId = "bank-456",
            note = "Invoice #101"
        )

        val serialized = json.encodeToString(req)
        assertTrue(serialized.contains("\"customer_id\":\"cust-123\""))
        assertTrue(serialized.contains("\"route\":\"East Sector\""))
        assertTrue(serialized.contains("\"salesman\":\"Sunil\""))
        assertTrue(serialized.contains("\"total_amount\":\"1500.00\""))
        assertTrue(serialized.contains("\"total_cash_in\":\"1000.00\""))
        assertTrue(serialized.contains("\"bank_amount\":\"500.00\""))
        assertTrue(serialized.contains("\"bank_id\":\"bank-456\""))
    }

    @Test
    fun testCreateLineSaleRequestSerializationWithoutBank() {
        val req = CreateLineSaleRequest(
            customerId = "cust-123",
            totalAmount = "500.00",
            totalCashIn = "500.00",
            cashAmount = "500.00",
            bankAmount = "0.00",
            bankId = null
        )

        val serialized = json.encodeToString(req)
        assertTrue(serialized.contains("\"bank_amount\":\"0.00\""))
        assertTrue(serialized.contains("\"bank_id\":null"))
    }

    @Test
    fun testCreateCounterSaleRequestSerialization() {
        val req = CreateCounterSaleRequest(
            item = "Biscuits Box",
            price = "120.00",
            totalAmount = "240.00",
            paymentMethod = "split",
            cash = "140.00",
            cashAmount = "140.00",
            bankAmount = "100.00",
            bankId = "bank-789"
        )

        val serialized = json.encodeToString(req)
        assertTrue(serialized.contains("\"item\":\"Biscuits Box\""))
        assertTrue(serialized.contains("\"price\":\"120.00\""))
        assertTrue(serialized.contains("\"total_amount\":\"240.00\""))
        assertTrue(serialized.contains("\"payment_method\":\"split\""))
        assertTrue(serialized.contains("\"cash\":\"140.00\""))
        assertTrue(serialized.contains("\"bank_amount\":\"100.00\""))
        assertTrue(serialized.contains("\"bank_id\":\"bank-789\""))
    }

    @Test
    fun testLineSaleResponseDeserialization() {
        val payload = """
            {
              "success": true,
              "message": "line sale created",
              "data": {
                "id": "sale-1",
                "customer_id": "cust-1",
                "customer_name": "Sharma Stores",
                "route": "North Route",
                "salesman": "Ramesh",
                "total_amount": "2500.00",
                "total_cash_in": "2000.00",
                "balance": "500.00",
                "date": "2026-10-15",
                "status": "partial"
              }
            }
        """.trimIndent()

        val response = json.decodeFromString<ApiResponse<LineSale>>(payload)
        assertTrue(response.success)
        assertNotNull(response.data)
        assertEquals("sale-1", response.data?.id)
        assertEquals("Sharma Stores", response.data?.customerName)
        assertEquals("2500.00", response.data?.totalAmount)
        assertEquals("500.00", response.data?.balance)
    }

    @Test
    fun testCounterSaleResponseDeserialization() {
        val payload = """
            {
              "success": true,
              "message": "counter sale created",
              "data": {
                "id": "counter-1",
                "item": "Bread Packets",
                "price": "40.00",
                "total_amount": "200.00",
                "payment_method": "cash",
                "cash": "200.00",
                "date": "2026-10-15"
              }
            }
        """.trimIndent()

        val response = json.decodeFromString<ApiResponse<CounterSale>>(payload)
        assertTrue(response.success)
        assertNotNull(response.data)
        assertEquals("counter-1", response.data?.id)
        assertEquals("Bread Packets", response.data?.item)
        assertEquals("200.00", response.data?.totalAmount)
    }
}
