package com.vyavsa.app.feature.products

import com.vyavsa.app.core.model.ApiResponse
import com.vyavsa.app.core.model.CreateProductRequest
import com.vyavsa.app.core.model.Product
import com.vyavsa.app.core.model.ProductAnalyticsResponse
import com.vyavsa.app.core.model.ProductDailySales
import com.vyavsa.app.core.model.ProductSalesSummary
import com.vyavsa.app.core.model.SaleItem
import com.vyavsa.app.core.model.SaleItemRequest
import com.vyavsa.app.core.model.TopSellingProduct
import com.vyavsa.app.core.model.UpdateProductRequest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductSerializationTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true; encodeDefaults = true }

    @Test
    fun testCreateProductRequestSerialization() {
        val req = CreateProductRequest(
            name = "Amul Milk 500ml",
            unitPrice = "32.00"
        )
        val serialized = json.encodeToString(req)
        assertTrue(serialized.contains("\"name\":\"Amul Milk 500ml\""))
        assertTrue(serialized.contains("\"unit_price\":\"32.00\""))
    }

    @Test
    fun testUpdateProductRequestSerialization() {
        val req = UpdateProductRequest(
            name = "Amul Milk 500ml Gold",
            unitPrice = "34.00",
            status = "active"
        )
        val serialized = json.encodeToString(req)
        assertTrue(serialized.contains("\"name\":\"Amul Milk 500ml Gold\""))
        assertTrue(serialized.contains("\"unit_price\":\"34.00\""))
        assertTrue(serialized.contains("\"status\":\"active\""))
    }

    @Test
    fun testProductResponseDeserialization() {
        val payload = """
            {
              "success": true,
              "message": "product retrieved successfully",
              "data": {
                "id": "prod-101",
                "name": "Tata Tea Gold 250g",
                "unit_price": "140.00",
                "status": "active",
                "created_at": "2026-10-18T10:00:00Z",
                "updated_at": "2026-10-18T10:00:00Z"
              }
            }
        """.trimIndent()

        val resp = json.decodeFromString<ApiResponse<Product>>(payload)
        assertTrue(resp.success)
        assertNotNull(resp.data)
        assertEquals("prod-101", resp.data?.id)
        assertEquals("Tata Tea Gold 250g", resp.data?.name)
        assertEquals("140.00", resp.data?.unitPrice)
        assertEquals("active", resp.data?.status)
    }

    @Test
    fun testProductListResponseDeserialization() {
        val payload = """
            {
              "success": true,
              "message": "products retrieved",
              "data": [
                {
                  "id": "prod-1",
                  "name": "Basmati Rice 5kg",
                  "unit_price": "450.00",
                  "status": "active"
                },
                {
                  "id": "prod-2",
                  "name": "Sunflower Oil 1L",
                  "unit_price": "135.50",
                  "status": "archived"
                }
              ]
            }
        """.trimIndent()

        val resp = json.decodeFromString<ApiResponse<List<Product>>>(payload)
        assertTrue(resp.success)
        assertEquals(2, resp.data?.size)
        assertEquals("Basmati Rice 5kg", resp.data?.get(0)?.name)
        assertEquals("450.00", resp.data?.get(0)?.unitPrice)
        assertEquals("archived", resp.data?.get(1)?.status)
    }

    @Test
    fun testSaleItemRequestSerialization() {
        val itemReq = SaleItemRequest(
            productId = "prod-101",
            quantity = "3.00",
            unitPrice = "140.00"
        )
        val serialized = json.encodeToString(itemReq)
        assertTrue(serialized.contains("\"product_id\":\"prod-101\""))
        assertTrue(serialized.contains("\"quantity\":\"3.00\""))
        assertTrue(serialized.contains("\"unit_price\":\"140.00\""))
    }

    @Test
    fun testSaleItemDeserialization() {
        val itemJson = """
            {
              "id": "item-1",
              "sale_id": "sale-101",
              "sale_type": "line_sale",
              "product_id": "prod-101",
              "product_name_snapshot": "Tata Tea Gold",
              "quantity": "2.00",
              "unit_price": "140.00",
              "line_total": "280.00",
              "created_at": "2026-10-18T10:05:00Z"
            }
        """.trimIndent()

        val item = json.decodeFromString<SaleItem>(itemJson)
        assertEquals("item-1", item.id)
        assertEquals("sale-101", item.saleId)
        assertEquals("line_sale", item.saleType)
        assertEquals("prod-101", item.productId)
        assertEquals("Tata Tea Gold", item.productNameSnapshot)
        assertEquals("2.00", item.quantity)
        assertEquals("140.00", item.unitPrice)
        assertEquals("280.00", item.lineTotal)
    }

    @Test
    fun testProductAnalyticsResponseDeserialization() {
        val payload = """
            {
              "success": true,
              "message": "product analytics retrieved",
              "data": {
                "summary": {
                  "total_quantity_sold": "450.00",
                  "total_revenue": "54200.00",
                  "distinct_products_count": 18,
                  "top_product_name": "Basmati Rice 5kg"
                },
                "top_by_quantity": [
                  {
                    "rank": 1,
                    "product_id": "prod-1",
                    "product_name": "Basmati Rice 5kg",
                    "quantity_sold": "120.00",
                    "revenue": "54000.00"
                  },
                  {
                    "rank": 2,
                    "product_id": "prod-2",
                    "product_name": "Sunflower Oil 1L",
                    "quantity_sold": "80.00",
                    "revenue": "10800.00"
                  }
                ],
                "top_by_revenue": [
                  {
                    "rank": 1,
                    "product_id": "prod-1",
                    "product_name": "Basmati Rice 5kg",
                    "quantity_sold": "120.00",
                    "revenue": "54000.00"
                  }
                ],
                "daily_sales": [
                  {
                    "sale_date": "2026-10-18",
                    "daily_quantity": "15.00",
                    "daily_revenue": "6750.00"
                  }
                ]
              }
            }
        """.trimIndent()

        val resp = json.decodeFromString<ApiResponse<ProductAnalyticsResponse>>(payload)
        assertTrue(resp.success)
        val data = resp.data
        assertNotNull(data)
        assertEquals("450.00", data?.summary?.totalQuantitySold)
        assertEquals("54200.00", data?.summary?.totalRevenue)
        assertEquals(18L, data?.summary?.distinctProductsCount)
        assertEquals("Basmati Rice 5kg", data?.summary?.topProductName)

        assertEquals(2, data?.topByQuantity?.size)
        assertEquals("prod-1", data?.topByQuantity?.get(0)?.productId)
        assertEquals("120.00", data?.topByQuantity?.get(0)?.quantitySold)

        assertEquals(1, data?.topByRevenue?.size)
        assertEquals("54000.00", data?.topByRevenue?.get(0)?.revenue)

        assertEquals(1, data?.dailySales?.size)
        assertEquals("2026-10-18", data?.dailySales?.get(0)?.saleDate)
        assertEquals("6750.00", data?.dailySales?.get(0)?.dailyRevenue)
    }
}
