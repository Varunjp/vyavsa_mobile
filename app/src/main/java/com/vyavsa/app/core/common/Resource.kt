package com.vyavsa.app.core.common

sealed class Resource<out T> {
    data class Success<out T>(val data: T, val message: String? = null) : Resource<T>()
    data class Error(val message: String, val code: String? = null, val details: Map<String, String>? = null) : Resource<Nothing>()
    object Loading : Resource<Nothing>()
}

sealed interface UiState<out T> {
    object Idle : UiState<Nothing>
    object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    object Empty : UiState<Nothing>
    data class Error(val message: String, val code: String? = null) : UiState<Nothing>
}
