package com.vyavsa.app.feature.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vyavsa.app.core.common.Resource
import com.vyavsa.app.core.common.UiState
import com.vyavsa.app.core.model.CreateProductRequest
import com.vyavsa.app.core.model.Product
import com.vyavsa.app.core.model.ProductAnalyticsResponse
import com.vyavsa.app.core.model.UpdateProductRequest
import com.vyavsa.app.core.security.TokenManager
import com.vyavsa.app.core.sync.DataMutation
import com.vyavsa.app.core.sync.DataRefreshCoordinator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

enum class DateRangeFilter(val label: String) {
    TODAY("Today"),
    THIS_WEEK("This Week"),
    THIS_MONTH("This Month"),
    ALL_TIME("All Time")
}

@HiltViewModel
class ProductViewModel @Inject constructor(
    private val repository: ProductRepository,
    private val refreshCoordinator: DataRefreshCoordinator,
    val tokenManager: TokenManager? = null
) : ViewModel() {

    private val isoDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US)

    var adminOverrideForTesting: Boolean? = null

    val isTenantAdmin: Boolean
        get() = adminOverrideForTesting ?: (tokenManager?.isTenantAdmin() ?: false)

    // ----------------------------------------------------
    // Product Catalog State
    // ----------------------------------------------------
    private val _productsState = MutableStateFlow<UiState<List<Product>>>(UiState.Loading)
    val productsState: StateFlow<UiState<List<Product>>> = _productsState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _actionState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val actionState: StateFlow<UiState<Unit>> = _actionState.asStateFlow()

    private var searchDebounceJob: Job? = null

    // ----------------------------------------------------
    // Product Sales Analytics State
    // ----------------------------------------------------
    private val _analyticsState = MutableStateFlow<UiState<ProductAnalyticsResponse>>(UiState.Loading)
    val analyticsState: StateFlow<UiState<ProductAnalyticsResponse>> = _analyticsState.asStateFlow()

    private val _selectedRange = MutableStateFlow(DateRangeFilter.THIS_MONTH)
    val selectedRange: StateFlow<DateRangeFilter> = _selectedRange.asStateFlow()

    init {
        loadProducts()
        loadAnalytics(DateRangeFilter.THIS_MONTH)
        observeRefreshEvents()
    }

    private fun observeRefreshEvents() {
        viewModelScope.launch {
            refreshCoordinator.refreshEvents.collect { mutation ->
                when (mutation) {
                    DataMutation.PRODUCTS,
                    DataMutation.ALL -> {
                        loadProducts(_searchQuery.value)
                        loadAnalytics(_selectedRange.value)
                    }
                    DataMutation.SALES -> {
                        loadAnalytics(_selectedRange.value)
                    }
                    else -> Unit
                }
            }
        }
    }

    // ----------------------------------------------------
    // Product Catalog Operations
    // ----------------------------------------------------

    fun onSearchQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
        searchDebounceJob?.cancel()
        searchDebounceJob = viewModelScope.launch {
            delay(280) // Debounce rapid keystrokes to conserve mobile network bandwidth
            loadProducts(newQuery)
        }
    }

    fun loadProducts(search: String? = _searchQuery.value, status: String? = null) {
        viewModelScope.launch {
            repository.listProducts(
                page = 1,
                pageSize = 100,
                search = search?.ifBlank { null },
                status = status?.ifBlank { null }
            ).collect { res ->
                _productsState.value = when (res) {
                    is Resource.Loading -> UiState.Loading
                    is Resource.Success -> {
                        if (res.data.isEmpty()) UiState.Empty else UiState.Success(res.data)
                    }
                    is Resource.Error -> UiState.Error(res.message, res.code)
                }
            }
        }
    }

    fun createProduct(name: String, unitPrice: String) {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) {
            _actionState.value = UiState.Error("Product name cannot be empty")
            return
        }

        val priceClean = unitPrice.trim().ifBlank { "0.00" }
        val priceVal = priceClean.toDoubleOrNull()
        if (priceVal == null || priceVal < 0.0) {
            _actionState.value = UiState.Error("Please enter a valid price (0.00 or greater)")
            return
        }

        viewModelScope.launch {
            val req = CreateProductRequest(
                name = trimmedName,
                unitPrice = priceClean
            )
            repository.createProduct(req).collect { res ->
                _actionState.value = when (res) {
                    is Resource.Loading -> UiState.Loading
                    is Resource.Success -> {
                        loadProducts(_searchQuery.value)
                        UiState.Success(Unit)
                    }
                    is Resource.Error -> UiState.Error(res.message, res.code)
                }
            }
        }
    }

    fun updateProduct(id: String, name: String, unitPrice: String, status: String = "active") {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) {
            _actionState.value = UiState.Error("Product name cannot be empty")
            return
        }

        val priceClean = unitPrice.trim().ifBlank { "0.00" }
        val priceVal = priceClean.toDoubleOrNull()
        if (priceVal == null || priceVal < 0.0) {
            _actionState.value = UiState.Error("Please enter a valid price (0.00 or greater)")
            return
        }

        viewModelScope.launch {
            val req = UpdateProductRequest(
                name = trimmedName,
                unitPrice = priceClean,
                status = status
            )
            repository.updateProduct(id, req).collect { res ->
                _actionState.value = when (res) {
                    is Resource.Loading -> UiState.Loading
                    is Resource.Success -> {
                        loadProducts(_searchQuery.value)
                        UiState.Success(Unit)
                    }
                    is Resource.Error -> UiState.Error(res.message, res.code)
                }
            }
        }
    }

    fun deleteProduct(id: String) {
        viewModelScope.launch {
            repository.deleteProduct(id).collect { res ->
                _actionState.value = when (res) {
                    is Resource.Loading -> UiState.Loading
                    is Resource.Success -> {
                        loadProducts(_searchQuery.value)
                        UiState.Success(Unit)
                    }
                    is Resource.Error -> UiState.Error(res.message, res.code)
                }
            }
        }
    }

    fun resetActionState() {
        _actionState.value = UiState.Idle
    }

    // ----------------------------------------------------
    // Product Sales Analytics Operations
    // ----------------------------------------------------

    fun selectDateRange(range: DateRangeFilter) {
        _selectedRange.value = range
        loadAnalytics(range)
    }

    fun loadAnalytics(range: DateRangeFilter = _selectedRange.value) {
        val today = LocalDate.now()
        val (startDate, endDate) = when (range) {
            DateRangeFilter.TODAY -> Pair(today.format(isoDateFormatter), today.format(isoDateFormatter))
            DateRangeFilter.THIS_WEEK -> Pair(today.minusDays(6).format(isoDateFormatter), today.format(isoDateFormatter))
            DateRangeFilter.THIS_MONTH -> Pair(today.withDayOfMonth(1).format(isoDateFormatter), today.format(isoDateFormatter))
            DateRangeFilter.ALL_TIME -> Pair(null, null)
        }

        viewModelScope.launch {
            repository.getProductAnalytics(
                startDate = startDate,
                endDate = endDate,
                limit = 15
            ).collect { res ->
                _analyticsState.value = when (res) {
                    is Resource.Loading -> UiState.Loading
                    is Resource.Success -> {
                        val response = res.data
                        if (response.topByQuantity.isEmpty() && response.summary.totalQuantitySold.toDoubleOrNull() ?: 0.0 == 0.0) {
                            UiState.Empty
                        } else {
                            UiState.Success(response)
                        }
                    }
                    is Resource.Error -> UiState.Error(res.message, res.code)
                }
            }
        }
    }
}
