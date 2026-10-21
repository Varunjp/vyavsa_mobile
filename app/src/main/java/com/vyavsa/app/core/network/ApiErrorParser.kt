package com.vyavsa.app.core.network

import android.util.Log
import com.vyavsa.app.core.common.Resource
import com.vyavsa.app.core.model.ApiResponse
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import retrofit2.HttpException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Centralized parser for mapping network, HTTP, and API responses to human-readable Resource.Error objects.
 * Handles HTTP status codes, Go backend API error envelopes, validation errors, timeouts, and connection issues
 * while ensuring sensitive technical details (e.g., credentials, database internals) are safely redacted.
 */
object ApiErrorParser {

    private const val TAG = "VyavsaApiError"
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private fun logW(tag: String, msg: String) {
        try {
            Log.w(tag, msg)
        } catch (_: Throwable) {
            // Ignored during testing
        }
    }

    private fun logE(tag: String, msg: String) {
        try {
            Log.e(tag, msg)
        } catch (_: Throwable) {
            // Ignored during testing
        }
    }

    fun sanitizeErrorMessage(msg: String): String {
        return msg.replace(Regex("(?i)bearer\\s+[A-Za-z0-9_\\-\\.]+"), "Bearer [REDACTED]")
            .replace(Regex("(?i)password\\s*=\\s*[^\\s&]+"), "password=[REDACTED]")
    }

    /**
     * Parses any caught Throwable from a network or repository operation into a standardized Resource.Error.
     */
    fun parse(throwable: Throwable, fallbackMessage: String = "An unexpected error occurred"): Resource.Error {
        return when (throwable) {
            is HttpException -> parseHttpException(throwable, fallbackMessage)
            is SocketTimeoutException -> {
                logW(TAG, "Connection timeout: ${throwable.message}")
                Resource.Error(
                    message = "Connection timed out. Please check your internet connection and try again.",
                    code = "TIMEOUT"
                )
            }
            is UnknownHostException, is ConnectException -> {
                logW(TAG, "Server connection failed: ${throwable.message}")
                Resource.Error(
                    message = "Unable to connect to the server. Please check your network connection.",
                    code = "NETWORK_ERROR"
                )
            }
            is IOException -> {
                logW(TAG, "Network I/O failure: ${throwable.message}")
                Resource.Error(
                    message = "Network error occurred. Please check your connection and retry.",
                    code = "IO_ERROR"
                )
            }
            is kotlinx.serialization.SerializationException -> {
                logE(TAG, "Serialization error: ${throwable.message}")
                Resource.Error(
                    message = "Unable to process server response. Please ensure your app is up to date.",
                    code = "DESERIALIZATION_ERROR"
                )
            }
            else -> {
                logE(TAG, "Unexpected exception: ${throwable.javaClass.simpleName}: ${throwable.message}")
                val rawMsg = throwable.message
                val sanitized = if (!rawMsg.isNullOrBlank() &&
                    !rawMsg.contains("Exception", ignoreCase = true) &&
                    !rawMsg.contains("NullPointer", ignoreCase = true) &&
                    !rawMsg.contains("@") &&
                    !rawMsg.contains("com.vyavsa")
                ) {
                    sanitizeErrorMessage(rawMsg)
                } else {
                    fallbackMessage
                }
                Resource.Error(message = sanitized, code = "UNKNOWN")
            }
        }
    }

    /**
     * Convenience method to parse an Exception into a Resource.Error.
     */
    fun parseException(e: Exception, fallbackMessage: String = "An unexpected error occurred"): Resource.Error =
        parse(e, fallbackMessage)

    /**
     * Convenience method to parse a Throwable directly into a human-readable message string.
     */
    fun parseToMessage(throwable: Throwable, fallbackMessage: String = "An unexpected error occurred"): String =
        parse(throwable, fallbackMessage).message

    /**
     * Parses an HTTP error (non-2xx response), extracting backend envelope errors if present.
     */
    fun parseHttpException(e: HttpException, fallbackMessage: String = "Operation failed"): Resource.Error {
        val statusCode = e.code()
        val errorBodyStr = try {
            e.response()?.errorBody()?.string()
        } catch (_: Exception) {
            null
        }

        // Diagnostic log without leaking authorization tokens or customer credentials
        logW(TAG, "HTTP $statusCode errorBody: ${errorBodyStr?.take(250)}")

        var extractedMessage: String? = null
        var extractedCode: String? = null
        var extractedFields: Map<String, String>? = null

        if (!errorBodyStr.isNullOrBlank()) {
            try {
                val element = runCatching { json.parseToJsonElement(errorBodyStr) }.getOrNull()
                if (element is JsonObject) {
                    val errElem = element["error"]
                    if (errElem is JsonObject) {
                        extractedCode = (errElem["code"] as? JsonPrimitive)?.contentOrNull
                        extractedMessage = (errElem["message"] as? JsonPrimitive)?.contentOrNull
                        val fieldsObj = errElem["fields"] as? JsonObject
                        if (fieldsObj != null) {
                            extractedFields = fieldsObj.mapNotNull { (k, v) ->
                                (v as? JsonPrimitive)?.contentOrNull?.let { k to it }
                            }.toMap()
                        }
                    } else if (errElem is JsonPrimitive && errElem.isString) {
                        extractedMessage = errElem.contentOrNull
                    }

                    if (extractedMessage.isNullOrBlank()) {
                        extractedMessage = (element["message"] as? JsonPrimitive)?.contentOrNull
                    }

                    if (extractedMessage.isNullOrBlank()) {
                        val detailElem = element["detail"]
                        if (detailElem is JsonPrimitive) {
                            extractedMessage = detailElem.contentOrNull
                        }
                    }

                    val errorsObj = (element["errors"] as? JsonObject) ?: (element["fields"] as? JsonObject)
                    if (errorsObj != null && extractedFields == null) {
                        val mapped = mutableMapOf<String, String>()
                        for ((k, v) in errorsObj) {
                            when (v) {
                                is JsonArray -> {
                                    val items = v.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
                                    if (items.isNotEmpty()) mapped[k] = items.joinToString(", ")
                                }
                                is JsonPrimitive -> {
                                    v.contentOrNull?.let { mapped[k] = it }
                                }
                                else -> Unit
                            }
                        }
                        if (mapped.isNotEmpty()) {
                            extractedFields = mapped
                        }
                    }
                }
            } catch (jsonEx: Exception) {
                logW(TAG, "Could not deserialize API error body: ${jsonEx.message}")
            }
        }

        val formattedMessage = when {
            !extractedFields.isNullOrEmpty() -> {
                val fieldDetails = extractedFields.entries.joinToString("; ") { "${it.key}: ${it.value}" }
                if (!extractedMessage.isNullOrBlank()) "$extractedMessage ($fieldDetails)" else fieldDetails
            }
            !extractedMessage.isNullOrBlank() -> extractedMessage
            else -> null
        }

        val mappedMessage = formattedMessage ?: when (statusCode) {
            400 -> "Invalid request. Please check the entered data."
            401 -> "Session expired or invalid credentials. Please log in again."
            403 -> "You do not have permission to perform this action."
            404 -> "The requested resource was not found."
            408 -> "Request timed out. Please try again."
            409 -> "Conflict detected. The operation could not be completed."
            422 -> "Validation failed. Please verify your entries."
            429 -> "Too many requests. Please slow down and try again shortly."
            500 -> "Server error (500). Please try again later."
            502, 503 -> "Vyavsa service is temporarily unavailable. Please try again shortly."
            504 -> "Server request timed out. Please check your connection and retry."
            in 500..599 -> "Server error ($statusCode). Please try again later."
            else -> "$fallbackMessage (Error $statusCode)"
        }

        return Resource.Error(
            message = sanitizeErrorMessage(mappedMessage),
            code = extractedCode ?: "HTTP_$statusCode",
            details = extractedFields
        )
    }

    /**
     * Extracts a human-readable Resource.Error from a successful HTTP response whose 'success' flag is false.
     */
    fun fromApiResponse(response: ApiResponse<*>, fallbackMessage: String = "Operation failed"): Resource.Error {
        val errInfo = response.error
        val fieldsMap = errInfo?.fields
        val detailsMap = errInfo?.details
        val errMessage = errInfo?.message?.trim()?.ifBlank { null }

        val msg = when {
            !fieldsMap.isNullOrEmpty() -> {
                val fields = fieldsMap.entries.joinToString("; ") { "${it.key}: ${it.value}" }
                if (!errMessage.isNullOrBlank()) "$errMessage ($fields)" else fields
            }
            !detailsMap.isNullOrEmpty() -> {
                val details = detailsMap.entries.joinToString("; ") { "${it.key}: ${it.value}" }
                if (!errMessage.isNullOrBlank()) "$errMessage ($details)" else details
            }
            !errMessage.isNullOrBlank() -> errMessage
            !response.message.isNullOrBlank() -> response.message
            else -> fallbackMessage
        }
        return Resource.Error(
            message = sanitizeErrorMessage(msg),
            code = errInfo?.code?.ifBlank { null },
            details = errInfo?.details ?: errInfo?.fields
        )
    }
}
