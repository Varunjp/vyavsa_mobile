package com.vyavsa.app.feature.profile

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.vyavsa.app.core.model.UpdateAttendanceRequest
import com.vyavsa.app.core.network.VyavsaApiService
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit

class CompanyLogoApiTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var apiService: VyavsaApiService

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    @Before
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        val contentType = "application/json".toMediaType()
        val retrofit = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .client(OkHttpClient())
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()

        apiService = retrofit.create(VyavsaApiService::class.java)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun testGetCompanyLogoSuccess() = runTest {
        val responseBody = """
            {
              "success": true,
              "message": "company logo retrieved successfully",
              "data": {
                "logo_url": "/uploads/logos/tenant_logo_12345.png"
              }
            }
        """.trimIndent()
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(responseBody))

        val resp = apiService.getCompanyLogo()
        assertTrue(resp.success)
        assertNotNull(resp.data)
        assertEquals("/uploads/logos/tenant_logo_12345.png", resp.data?.logoUrl)

        val recordedReq = mockWebServer.takeRequest()
        assertEquals("/api/v1/tenant/logo", recordedReq.path)
        assertEquals("GET", recordedReq.method)
    }

    @Test
    fun testUploadCompanyLogoSuccess() = runTest {
        val responseBody = """
            {
              "success": true,
              "message": "company logo uploaded successfully",
              "data": {
                "logo_url": "/uploads/logos/tenant_logo_updated.png"
              }
            }
        """.trimIndent()
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(responseBody))

        val fakeBytes = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
        val body = fakeBytes.toRequestBody("image/png".toMediaType())
        val part = MultipartBody.Part.createFormData("logo", "test_logo.png", body)

        val resp = apiService.uploadCompanyLogo(part)
        assertTrue(resp.success)
        assertEquals("/uploads/logos/tenant_logo_updated.png", resp.data?.logoUrl)

        val recordedReq = mockWebServer.takeRequest()
        assertEquals("/api/v1/tenant/logo", recordedReq.path)
        assertEquals("POST", recordedReq.method)
        assertTrue(recordedReq.headers["Content-Type"]?.contains("multipart/form-data") == true)
    }

    @Test
    fun testDeleteCompanyLogoSuccess() = runTest {
        val responseBody = """
            {
              "success": true,
              "message": "company logo removed successfully",
              "data": {
                "logo_url": null
              }
            }
        """.trimIndent()
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(responseBody))

        val resp = apiService.deleteCompanyLogo()
        assertTrue(resp.success)
        assertEquals(null, resp.data?.logoUrl)

        val recordedReq = mockWebServer.takeRequest()
        assertEquals("/api/v1/tenant/logo", recordedReq.path)
        assertEquals("DELETE", recordedReq.method)
    }

    @Test
    fun testUpdateAttendanceApiEndpoint() = runTest {
        val responseBody = """
            {
              "success": true,
              "message": "attendance updated successfully",
              "data": {
                "id": "att-456",
                "employee_id": "emp-789",
                "date": "2026-10-19",
                "status": "half_day",
                "daily_salary": "450.00",
                "salary_source": "override",
                "ot_amount": "50.00",
                "advance": "0.00"
              }
            }
        """.trimIndent()
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(responseBody))

        val req = UpdateAttendanceRequest(
            status = "half_day",
            dailySalary = "450.00",
            clearDailySalary = false,
            otAmount = "50.00",
            advance = "0.00"
        )

        val resp = apiService.updateAttendance("att-456", req)
        assertTrue(resp.success)
        assertNotNull(resp.data)
        assertEquals("att-456", resp.data?.id)
        assertEquals("450.00", resp.data?.dailySalary)
        assertEquals("override", resp.data?.salarySource)

        val recordedReq = mockWebServer.takeRequest()
        assertEquals("/api/v1/tenant/attendance/att-456", recordedReq.path)
        assertEquals("PUT", recordedReq.method)
    }
}
