package com.vyavsa.app.feature.transactions

import com.vyavsa.app.core.model.ApiResponse
import com.vyavsa.app.core.model.BankPaymentSplit
import com.vyavsa.app.core.model.CounterSale
import com.vyavsa.app.core.model.CreateCounterSaleRequest
import com.vyavsa.app.core.model.CreateLineSaleRequest
import com.vyavsa.app.core.model.LineSale
import com.vyavsa.app.core.model.SaleItem
import com.vyavsa.app.core.model.SaleItemRequest
import com.vyavsa.app.core.model.UpdateCounterSaleRequest
import com.vyavsa.app.core.model.UpdateLineSaleRequest
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
    fun testCreateLineSaleRequestSerializationWithMultipleBanks() {
        val splits = listOf(
            BankPaymentSplit(bankId = "bank-1", bankName = "HDFC Bank", amount = "600.00", note = "Cheque"),
            BankPaymentSplit(bankId = "bank-2", bankName = "SBI Bank", amount = "400.00")
        )
        val req = CreateLineSaleRequest(
            customerId = "cust-123",
            route = "East Sector",
            salesman = "Sunil",
            totalAmount = "1500.00",
            totalCashIn = "500.00",
            cashAmount = "500.00",
            bankAmount = "1000.00",
            bankId = "bank-1",
            bankPayments = splits,
            note = "Multiple bank allocations"
        )

        val serialized = json.encodeToString(req)
        assertTrue(serialized.contains("\"customer_id\":\"cust-123\""))
        assertTrue(serialized.contains("\"bank_amount\":\"1000.00\""))
        assertTrue(serialized.contains("\"bank_payments\":["))
        assertTrue(serialized.contains("\"bank_id\":\"bank-1\""))
        assertTrue(serialized.contains("\"bank_name\":\"HDFC Bank\""))
        assertTrue(serialized.contains("\"amount\":\"600.00\""))
        assertTrue(serialized.contains("\"note\":\"Cheque\""))
        assertTrue(serialized.contains("\"bank_id\":\"bank-2\""))
        assertTrue(serialized.contains("\"bank_name\":\"SBI Bank\""))
        assertTrue(serialized.contains("\"amount\":\"400.00\""))
    }

    @Test
    fun testCreateCounterSaleRequestSerializationWithMultipleBanks() {
        val splits = listOf(
            BankPaymentSplit(bankId = "bank-1", bankName = "HDFC", amount = "150.00"),
            BankPaymentSplit(bankId = "bank-3", bankName = "ICICI", amount = "250.00")
        )
        val req = CreateCounterSaleRequest(
            item = "Bulk Rice",
            price = "500.00",
            totalAmount = "500.00",
            paymentMethod = "split",
            cash = "100.00",
            cashAmount = "100.00",
            bankAmount = "400.00",
            bankId = "bank-1",
            bankPayments = splits,
            account = "counter"
        )

        val serialized = json.encodeToString(req)
        assertTrue(serialized.contains("\"item\":\"Bulk Rice\""))
        assertTrue(serialized.contains("\"payment_method\":\"split\""))
        assertTrue(serialized.contains("\"account\":\"counter\""))
        assertTrue(serialized.contains("\"bank_amount\":\"400.00\""))
        assertTrue(serialized.contains("\"bank_payments\":["))
        assertTrue(serialized.contains("\"bank_id\":\"bank-1\""))
        assertTrue(serialized.contains("\"bank_id\":\"bank-3\""))
        assertTrue(serialized.contains("\"amount\":\"250.00\""))
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

    @Test
    fun testCreateLineSaleRequestSerializationWithItems() {
        val req = CreateLineSaleRequest(
            customerId = "cust-1",
            totalAmount = "680.00",
            totalCashIn = "680.00",
            items = listOf(
                SaleItemRequest(
                    productId = "prod-1",
                    quantity = "1.00",
                    unitPrice = "450.00"
                ),
                SaleItemRequest(
                    productId = "prod-2",
                    quantity = "2.00",
                    unitPrice = "115.00"
                )
            )
        )

        val serialized = json.encodeToString(req)
        assertTrue(serialized.contains("\"items\":["))
        assertTrue(serialized.contains("\"product_id\":\"prod-1\""))
        assertTrue(serialized.contains("\"quantity\":\"1.00\""))
        assertTrue(serialized.contains("\"unit_price\":\"450.00\""))
        assertTrue(serialized.contains("\"product_id\":\"prod-2\""))
        assertTrue(serialized.contains("\"unit_price\":\"115.00\""))
    }

    @Test
    fun testCreateCounterSaleRequestSerializationWithItems() {
        val req = CreateCounterSaleRequest(
            totalAmount = "350.00",
            paymentMethod = "cash",
            cash = "350.00",
            items = listOf(
                SaleItemRequest(
                    productId = "prod-3",
                    quantity = "1.00",
                    unitPrice = "350.00"
                )
            )
        )

        val serialized = json.encodeToString(req)
        assertTrue(serialized.contains("\"items\":["))
        assertTrue(serialized.contains("\"product_id\":\"prod-3\""))
        assertTrue(serialized.contains("\"quantity\":\"1.00\""))
        assertTrue(serialized.contains("\"unit_price\":\"350.00\""))
    }

    @Test
    fun testLineSaleWithItemsDeserialization() {
        val payload = """
            {
              "success": true,
              "message": "line sale retrieved",
              "data": {
                "id": "sale-101",
                "customer_id": "cust-1",
                "customer_name": "Sharma Stores",
                "total_amount": "560.00",
                "total_cash_in": "560.00",
                "balance": "0.00",
                "date": "2026-10-18",
                "items": [
                  {
                    "id": "item-1",
                    "sale_id": "sale-101",
                    "sale_type": "line_sale",
                    "product_id": "prod-1",
                    "product_name_snapshot": "Tata Tea 250g",
                    "quantity": "2.00",
                    "unit_price": "140.00",
                    "line_total": "280.00"
                  },
                  {
                    "id": "item-2",
                    "sale_id": "sale-101",
                    "sale_type": "line_sale",
                    "product_id": "prod-2",
                    "product_name_snapshot": "Sugar 5kg",
                    "quantity": "1.00",
                    "unit_price": "280.00",
                    "line_total": "280.00"
                  }
                ]
              }
            }
        """.trimIndent()

        val response = json.decodeFromString<ApiResponse<LineSale>>(payload)
        assertTrue(response.success)
        val sale = response.data
        assertNotNull(sale)
        assertEquals("sale-101", sale?.id)
        assertEquals(2, sale?.items?.size)
        assertEquals("Tata Tea 250g", sale?.items?.get(0)?.productNameSnapshot)
        assertEquals("2.00", sale?.items?.get(0)?.quantity)
        assertEquals("140.00", sale?.items?.get(0)?.unitPrice)
        assertEquals("280.00", sale?.items?.get(0)?.lineTotal)
        assertEquals("Sugar 5kg", sale?.items?.get(1)?.productNameSnapshot)
    }

    @Test
    fun testCreateLineSaleRequestWithDiscountSerialization() {
        val req = CreateLineSaleRequest(
            customerId = "cust-1",
            subtotal = "1000.00",
            discount = "100.00",
            discountAmount = "100.00",
            totalAmount = "900.00",
            totalCashIn = "900.00",
            cashAmount = "900.00"
        )

        val serialized = json.encodeToString(req)
        assertTrue(serialized.contains("\"subtotal\":\"1000.00\""))
        assertTrue(serialized.contains("\"discount\":\"100.00\""))
        assertTrue(serialized.contains("\"discount_amount\":\"100.00\""))
        assertTrue(serialized.contains("\"total_amount\":\"900.00\""))
    }

    @Test
    fun testCreateCounterSaleRequestWithDiscountSerialization() {
        val req = CreateCounterSaleRequest(
            item = "Notebook",
            subtotal = "250.00",
            discount = "25.00",
            discountAmount = "25.00",
            totalAmount = "225.00",
            paymentMethod = "cash",
            cash = "225.00"
        )

        val serialized = json.encodeToString(req)
        assertTrue(serialized.contains("\"subtotal\":\"250.00\""))
        assertTrue(serialized.contains("\"discount\":\"25.00\""))
        assertTrue(serialized.contains("\"discount_amount\":\"25.00\""))
        assertTrue(serialized.contains("\"total_amount\":\"225.00\""))
    }

    @Test
    fun testLineSaleWithDiscountDeserialization() {
        val payload = """
            {
              "success": true,
              "message": "line sale retrieved",
              "data": {
                "id": "sale-200",
                "customer_id": "cust-2",
                "customer_name": "Agrawal Mart",
                "subtotal": "1200.00",
                "discount": "150.00",
                "total_amount": "1050.00",
                "total_cash_in": "1050.00",
                "balance": "0.00",
                "date": "2026-10-19"
              }
            }
        """.trimIndent()

        val response = json.decodeFromString<ApiResponse<LineSale>>(payload)
        assertTrue(response.success)
        val sale = response.data
        assertNotNull(sale)
        assertEquals("sale-200", sale?.id)
        assertEquals("1200.00", sale?.subtotal)
        assertEquals("150.00", sale?.discount)
        assertEquals("1050.00", sale?.totalAmount)
    }

    @Test
    fun testCounterSaleWithDiscountDeserialization() {
        val payload = """
            {
              "success": true,
              "message": "counter sale retrieved",
              "data": {
                "id": "counter-200",
                "item": "Snacks Box",
                "subtotal": "500.00",
                "discount": "50.00",
                "price": "500.00",
                "total_amount": "450.00",
                "payment_method": "cash",
                "cash": "450.00"
              }
            }
        """.trimIndent()

        val response = json.decodeFromString<ApiResponse<CounterSale>>(payload)
        assertTrue(response.success)
        val sale = response.data
        assertNotNull(sale)
        assertEquals("counter-200", sale?.id)
        assertEquals("500.00", sale?.subtotal)
        assertEquals("500.00", sale?.price)
        assertEquals("50.00", sale?.discount)
        assertEquals("450.00", sale?.totalAmount)
    }

    @Test
    fun testLegacySaleWithoutDiscountDeserializationDefaultsGracefully() {
        val payload = """
            {
              "success": true,
              "message": "legacy sale retrieved",
              "data": {
                "id": "sale-legacy-1",
                "customer_id": "cust-old",
                "customer_name": "Old Customer",
                "total_amount": "800.00",
                "total_cash_in": "800.00",
                "balance": "0.00"
              }
            }
        """.trimIndent()

        val response = json.decodeFromString<ApiResponse<LineSale>>(payload)
        assertTrue(response.success)
        val sale = response.data
        assertNotNull(sale)
        assertEquals("sale-legacy-1", sale?.id)
        assertEquals("0.00", sale?.discount)
        assertEquals("0.00", sale?.subtotal)
        assertEquals("800.00", sale?.totalAmount)
    }

    @Test
    fun testUpdateLineSaleRequestSerialization() {
        val req = UpdateLineSaleRequest(
            subtotal = "1500.00",
            discount = "200.00",
            discountAmount = "200.00",
            totalAmount = "1300.00",
            note = "Updated invoice discount"
        )

        val serialized = json.encodeToString(req)
        assertTrue(serialized.contains("\"subtotal\":\"1500.00\""))
        assertTrue(serialized.contains("\"discount\":\"200.00\""))
        assertTrue(serialized.contains("\"discount_amount\":\"200.00\""))
        assertTrue(serialized.contains("\"total_amount\":\"1300.00\""))
        assertTrue(serialized.contains("\"note\":\"Updated invoice discount\""))
    }

    @Test
    fun testUpdateCounterSaleRequestSerialization() {
        val req = UpdateCounterSaleRequest(
            item = "Edited Item",
            subtotal = "300.00",
            discount = "30.00",
            discountAmount = "30.00",
            totalAmount = "270.00"
        )

        val serialized = json.encodeToString(req)
        assertTrue(serialized.contains("\"item\":\"Edited Item\""))
        assertTrue(serialized.contains("\"subtotal\":\"300.00\""))
        assertTrue(serialized.contains("\"discount\":\"30.00\""))
        assertTrue(serialized.contains("\"discount_amount\":\"30.00\""))
        assertTrue(serialized.contains("\"total_amount\":\"270.00\""))
    }
}
