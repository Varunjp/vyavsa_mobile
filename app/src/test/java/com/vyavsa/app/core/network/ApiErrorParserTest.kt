package com.vyavsa.app.core.network

import com.vyavsa.app.core.common.Resource
import com.vyavsa.app.core.model.ApiResponse
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class ApiErrorParserTest {

    private fun createHttpException(statusCode: Int, jsonBody: String): HttpException {
        val mediaType = "application/json".toMediaTypeOrNull()
        val responseBody = jsonBody.toResponseBody(mediaType)
        val response = Response.error<Any>(statusCode, responseBody)
        return HttpException(response)
    }

    @Test
    fun testParseNetworkUnknownHostException() {
        val exception = UnknownHostException("Unable to resolve host api.vyavsa.com")
        val errorMsg = ApiErrorParser.parseToMessage(exception)
        assertEquals("Unable to connect to the server. Please check your network connection.", errorMsg)

        val resource = ApiErrorParser.parseException(exception)
        assertEquals("Unable to connect to the server. Please check your network connection.", resource.message)
    }

    @Test
    fun testParseNetworkSocketTimeoutException() {
        val exception = SocketTimeoutException("connect timed out")
        val errorMsg = ApiErrorParser.parseToMessage(exception)
        assertEquals("Connection timed out. Please check your internet connection and try again.", errorMsg)
    }

    @Test
    fun testParseNetworkConnectException() {
        val exception = ConnectException("Connection refused")
        val errorMsg = ApiErrorParser.parseToMessage(exception)
        assertEquals("Unable to connect to the server. Please check your network connection.", errorMsg)
    }

    @Test
    fun testParseGenericIOException() {
        val exception = IOException("Software caused connection abort")
        val errorMsg = ApiErrorParser.parseToMessage(exception)
        assertEquals("Network error occurred. Please check your connection and retry.", errorMsg)
    }

    @Test
    fun testParseHttp400WithDirectMessage() {
        val json = """{"message": "Invalid customer identifier"}"""
        val exception = createHttpException(400, json)
        val message = ApiErrorParser.parseToMessage(exception)
        assertEquals("Invalid customer identifier", message)
    }

    @Test
    fun testParseHttp400WithErrorsFieldMap() {
        val json = """
            {
                "message": "Validation failed",
                "errors": {
                    "email": ["Invalid email address format"],
                    "phone": ["Phone number must be 10 digits"]
                }
            }
        """.trimIndent()
        val exception = createHttpException(400, json)
        val message = ApiErrorParser.parseToMessage(exception)
        assertTrue(message.contains("email: Invalid email address format"))
        assertTrue(message.contains("phone: Phone number must be 10 digits"))
    }

    @Test
    fun testParseHttp401Unauthorized() {
        val json = """{"error": "Token has expired or is invalid"}"""
        val exception = createHttpException(401, json)
        val message = ApiErrorParser.parseToMessage(exception)
        assertEquals("Token has expired or is invalid", message)
    }

    @Test
    fun testParseHttp401WithDefaultFallback() {
        val exception = createHttpException(401, "")
        val message = ApiErrorParser.parseToMessage(exception)
        assertEquals("Session expired or invalid credentials. Please log in again.", message)
    }

    @Test
    fun testParseHttp403Forbidden() {
        val json = """{"detail": "You do not have permission to delete sales."}"""
        val exception = createHttpException(403, json)
        val message = ApiErrorParser.parseToMessage(exception)
        assertEquals("You do not have permission to delete sales.", message)
    }

    @Test
    fun testParseHttp403WithDefaultFallback() {
        val exception = createHttpException(403, "")
        val message = ApiErrorParser.parseToMessage(exception)
        assertEquals("You do not have permission to perform this action.", message)
    }

    @Test
    fun testParseHttp404NotFound() {
        val exception = createHttpException(404, "")
        val message = ApiErrorParser.parseToMessage(exception)
        assertEquals("The requested resource was not found.", message)
    }

    @Test
    fun testParseHttp409Conflict() {
        val json = """{"message": "Invoice number already exists"}"""
        val exception = createHttpException(409, json)
        val message = ApiErrorParser.parseToMessage(exception)
        assertEquals("Invoice number already exists", message)
    }

    @Test
    fun testParseHttp422UnprocessableEntity() {
        val json = """
            {
                "errors": {
                    "amount": ["Amount must be greater than zero"]
                }
            }
        """.trimIndent()
        val exception = createHttpException(422, json)
        val message = ApiErrorParser.parseToMessage(exception)
        assertTrue(message.contains("amount: Amount must be greater than zero"))
    }

    @Test
    fun testParseHttp429TooManyRequests() {
        val exception = createHttpException(429, "")
        val message = ApiErrorParser.parseToMessage(exception)
        assertEquals("Too many requests. Please slow down and try again shortly.", message)
    }

    @Test
    fun testParseHttp500InternalServerError() {
        val exception = createHttpException(500, "")
        val message = ApiErrorParser.parseToMessage(exception)
        assertEquals("Server error (500). Please try again later.", message)
    }

    @Test
    fun testParseHttp502BadGateway() {
        val exception = createHttpException(502, "")
        val message = ApiErrorParser.parseToMessage(exception)
        assertEquals("Vyavsa service is temporarily unavailable. Please try again shortly.", message)
    }

    @Test
    fun testParseHttp503ServiceUnavailable() {
        val exception = createHttpException(503, "")
        val message = ApiErrorParser.parseToMessage(exception)
        assertEquals("Vyavsa service is temporarily unavailable. Please try again shortly.", message)
    }

    @Test
    fun testParseHttp504GatewayTimeout() {
        val exception = createHttpException(504, "")
        val message = ApiErrorParser.parseToMessage(exception)
        assertEquals("Server request timed out. Please check your connection and retry.", message)
    }

    @Test
    fun testRedactsSensitiveDataInErrorMessage() {
        val json = """{"message": "Failed connecting with Authorization: Bearer secret_token_12345"}"""
        val exception = createHttpException(500, json)
        val message = ApiErrorParser.parseToMessage(exception)
        assertFalse(message.contains("secret_token_12345"))
    }

    @Test
    fun testFromApiResponseFailureWithMessage() {
        val response = ApiResponse<String>(
            success = false,
            message = "Tenant subscription has expired",
            data = null
        )
        val resource = ApiErrorParser.fromApiResponse(response)
        assertEquals("Tenant subscription has expired", resource.message)
    }

    @Test
    fun testFromApiResponseWithApiErrorInfoFields() {
        val response = ApiResponse<String>(
            success = false,
            message = "Validation failed",
            error = com.vyavsa.app.core.model.ApiErrorInfo(
                message = "Invalid input",
                fields = mapOf("amount" to "Amount must be positive")
            ),
            data = null
        )
        val resource = ApiErrorParser.fromApiResponse(response)
        assertTrue(resource.message.contains("Invalid input"))
        assertTrue(resource.message.contains("amount: Amount must be positive"))
    }

    @Test
    fun testFromApiResponseFallbackMessage() {
        val response = ApiResponse<String>(
            success = false,
            message = null,
            error = null,
            data = null
        )
        val resource = ApiErrorParser.fromApiResponse(response, "Custom operation failed")
        assertEquals("Custom operation failed", resource.message)
    }
}
