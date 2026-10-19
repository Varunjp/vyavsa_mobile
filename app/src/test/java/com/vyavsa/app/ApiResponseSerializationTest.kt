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

    @Test
    fun testTenantLogoResponseSerialization() {
        val payload = """
            {
              "success": true,
              "message": "company logo retrieved successfully",
              "data": {
                "logo_url": "/uploads/logos/tenant_99999999.png"
              }
            }
        """.trimIndent()

        val resp = json.decodeFromString<ApiResponse<com.vyavsa.app.core.model.TenantLogoResponse>>(payload)
        assertTrue(resp.success)
        assertEquals("/uploads/logos/tenant_99999999.png", resp.data?.logoUrl)

        val nullLogoPayload = """
            {
              "success": true,
              "data": {
                "logo_url": null
              }
            }
        """.trimIndent()
        val nullResp = json.decodeFromString<ApiResponse<com.vyavsa.app.core.model.TenantLogoResponse>>(nullLogoPayload)
        assertTrue(nullResp.success)
        assertEquals(null, nullResp.data?.logoUrl)
    }

    @Test
    fun testEmployeeWithOptionalSalarySerialization() {
        val withSalaryPayload = """
            {
              "id": "emp-1",
              "name": "Ramesh Singh",
              "salary": "12000.00",
              "ot_rate": "50.00",
              "status": "active"
            }
        """.trimIndent()
        val emp1 = json.decodeFromString<com.vyavsa.app.core.model.Employee>(withSalaryPayload)
        assertEquals("12000.00", emp1.salary)

        val withoutSalaryPayload = """
            {
              "id": "emp-2",
              "name": "Suresh Verma",
              "salary": null,
              "ot_rate": "0.00",
              "status": "active"
            }
        """.trimIndent()
        val emp2 = json.decodeFromString<com.vyavsa.app.core.model.Employee>(withoutSalaryPayload)
        assertEquals(null, emp2.salary)

        val createReq = com.vyavsa.app.core.model.CreateEmployeeRequest(
            name = "Kavita",
            salary = "500.00",
            otRate = "0.00"
        )
        val encodedCreate = json.encodeToString(com.vyavsa.app.core.model.CreateEmployeeRequest.serializer(), createReq)
        assertTrue(encodedCreate.contains("\"salary\":\"500.00\""))

        val updateReq = com.vyavsa.app.core.model.UpdateEmployeeRequest(
            name = "Kavita",
            clearSalary = true
        )
        val encodedUpdate = json.encodeToString(com.vyavsa.app.core.model.UpdateEmployeeRequest.serializer(), updateReq)
        assertTrue(encodedUpdate.contains("\"clear_salary\":true"))
    }

    @Test
    fun testAttendanceRecordWithDailySalaryAndSourceSerialization() {
        val payload = """
            {
              "id": "att-101",
              "employee_id": "emp-1",
              "employee_name": "Ramesh Singh",
              "date": "2026-10-19",
              "status": "present",
              "daily_salary": "950.00",
              "salary_source": "override",
              "ot": "0",
              "ot_amount": "150.00",
              "advance": "0.00"
            }
        """.trimIndent()

        val record = json.decodeFromString<com.vyavsa.app.core.model.AttendanceRecord>(payload)
        assertEquals("att-101", record.id)
        assertEquals("950.00", record.dailySalary)
        assertEquals("override", record.salarySource)

        val updateReq = com.vyavsa.app.core.model.UpdateAttendanceRequest(
            status = "half_day",
            dailySalary = "400.00",
            clearDailySalary = true,
            otAmount = "0.00"
        )
        val encodedUpdate = json.encodeToString(com.vyavsa.app.core.model.UpdateAttendanceRequest.serializer(), updateReq)
        assertTrue(encodedUpdate.contains("\"daily_salary\":\"400.00\""))
        assertTrue(encodedUpdate.contains("\"clear_daily_salary\":true"))
    }

    @Test
    fun testTenantProfileWithLogoUrlSerialization() {
        val payload = """
            {
              "id": "t-1",
              "name": "Vyavsa Store",
              "email": "store@vyavsa.com",
              "logo_url": "/uploads/logos/store.png",
              "currency": "INR",
              "timezone": "Asia/Kolkata"
            }
        """.trimIndent()

        val profile = json.decodeFromString<com.vyavsa.app.core.model.TenantProfile>(payload)
        assertEquals("/uploads/logos/store.png", profile.logoUrl)
        assertEquals("Vyavsa Store", profile.name)
    }
}
