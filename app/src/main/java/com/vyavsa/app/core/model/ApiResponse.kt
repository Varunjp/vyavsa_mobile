package com.vyavsa.app.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ApiResponse<T>(
    val success: Boolean = false,
    val data: T? = null,
    val message: String? = null,
    val error: ApiErrorInfo? = null,
    val pagination: ApiPagination? = null
)

@Serializable
data class ApiErrorInfo(
    val code: String = "",
    val message: String = "",
    val details: Map<String, String>? = null,
    val fields: Map<String, String>? = null
)

@Serializable
data class ApiPagination(
    val page: Int = 1,
    @SerialName("page_size") val pageSize: Int = 20,
    @SerialName("total_items") val totalItems: Long = 0,
    @SerialName("total_pages") val totalPages: Int = 0
)
