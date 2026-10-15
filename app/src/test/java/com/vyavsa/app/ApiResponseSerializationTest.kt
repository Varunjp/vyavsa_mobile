package com.vyavsa.app

import com.vyavsa.app.core.model.ApiResponse
import com.vyavsa.app.core.model.FinancialMetrics
import com.vyavsa.app.core.model.TokenResponse
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiResponseSerializationTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    @Test
    fun testTokenResponseDeserialization() {
        val payload = """
            {
              "success": true,
              "message": "tenant user authenticated successfully",
              "data": {
                "access_token": "mock.access.token",
                "refresh_token": "mock.refresh.token",
                "token_type": "Bearer",
                "expires_in": 3600,
                "user": {
                  "id": "11111111-2222-3333-4444-555555555555",
                  "name": "Varun",
                  "email": "owner@vyavsa.com",
                  "role": "tenant_admin",
                  "user_type": "tenant",
                  "tenant_id": "99999999-8888-7777-6666-555555555555"
                }
              }
            }
        """.trimIndent()

        val resp = json.decodeFromString<ApiResponse<TokenResponse>>(payload)
        assertTrue(resp.success)
        assertNotNull(resp.data)
        assertEquals("mock.access.token", resp.data?.accessToken)
        assertEquals("tenant_admin", resp.data?.user?.role)
        assertEquals("Varun", resp.data?.user?.name)
    }

    @Test
    fun testFinancialMetricsDeserialization() {
        val payload = """
            {
              "success": true,
              "message": "financial summary and dashboard metrics retrieved",
              "data": {
                "cash_balance": "14500.50",
                "bank_balance": "85000.00",
                "total_receivable": "12000.00",
                "total_payable": "5400.00",
                "net_dues": "6600.00",
                "pending_salary": "15000.00",
                "today_overview": {
                  "attendance": {
                    "present": 4,
                    "total": 5,
                    "absent": 1,
                    "has_records": true
                  },
                  "line_sale": "3500.00",
                  "counter_sale": "1200.00",
                  "employee_advance": "500.00",
                  "outstanding_payable": "5400.00",
                  "date": "2026-10-14"
                }
              }
            }
        """.trimIndent()

        val resp = json.decodeFromString<ApiResponse<FinancialMetrics>>(payload)
        assertTrue(resp.success)
        assertNotNull(resp.data)
        assertEquals("14500.50", resp.data?.cashBalance)
        assertEquals("85000.00", resp.data?.bankBalance)
        assertEquals(4, resp.data?.todayOverview?.attendance?.present)
        assertEquals("3500.00", resp.data?.todayOverview?.lineSale)
    }

    @Test
    fun testErrorResponseDeserialization() {
        val payload = """
            {
              "success": false,
              "message": "invalid credentials",
              "error": {
                "code": "INVALID_CREDENTIALS",
                "message": "invalid credentials",
                "details": {
                  "email": "user does not exist"
                }
              }
            }
        """.trimIndent()

        val resp = json.decodeFromString<ApiResponse<TokenResponse>>(payload)
        assertFalse(resp.success)
        assertNotNull(resp.error)
        assertEquals("INVALID_CREDENTIALS", resp.error?.code)
        assertEquals("user does not exist", resp.error?.details?.get("email"))
    }
}
