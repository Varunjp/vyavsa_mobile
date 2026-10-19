package com.vyavsa.app.feature.products

import com.vyavsa.app.core.common.Resource
import com.vyavsa.app.core.common.UiState
import com.vyavsa.app.core.model.CreateProductRequest
import com.vyavsa.app.core.model.Product
import com.vyavsa.app.core.model.ProductAnalyticsResponse
import com.vyavsa.app.core.model.ProductDailySales
import com.vyavsa.app.core.model.ProductSalesSummary
import com.vyavsa.app.core.model.TopSellingProduct
import com.vyavsa.app.core.model.UpdateProductRequest
import com.vyavsa.app.core.sync.DataMutation
import com.vyavsa.app.core.sync.DataRefreshCoordinator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProductViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeProductRepository
    private lateinit var refreshCoordinator: DataRefreshCoordinator
    private lateinit var viewModel: ProductViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeProductRepository()
        refreshCoordinator = DataRefreshCoordinator()
        viewModel = ProductViewModel(
            repository = fakeRepository,
            refreshCoordinator = refreshCoordinator,
            tokenManager = null
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialLoadPopulatesProductsAndAnalytics() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        val productsState = viewModel.productsState.value
        assertTrue("Expected Success state for products, got $productsState", productsState is UiState.Success)
        val products = (productsState as UiState.Success).data
        assertEquals(2, products.size)
        assertEquals("Basmati Rice 5kg", products[0].name)

        val analyticsState = viewModel.analyticsState.value
        assertTrue("Expected Success state for analytics, got $analyticsState", analyticsState is UiState.Success)
        val analytics = (analyticsState as UiState.Success).data
        assertEquals("12000.00", analytics.summary.totalRevenue)
        assertEquals(2, analytics.topByQuantity.size)
    }

    @Test
    fun testSearchQueryDebounceAndFiltering() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onSearchQueryChanged("Basmati")
        // Before debounce elapsed (e.g., 100ms), search shouldn't have executed
        testDispatcher.scheduler.advanceTimeBy(100)
        assertEquals(2, (viewModel.productsState.value as UiState.Success).data.size)

        // After debounce period (280ms)
        testDispatcher.scheduler.advanceTimeBy(200)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.productsState.value
        assertTrue(state is UiState.Success)
        val filtered = (state as UiState.Success).data
        assertEquals(1, filtered.size)
        assertEquals("Basmati Rice 5kg", filtered[0].name)
    }

    @Test
    fun testCreateProductValidationEmptyName() = runTest {
        viewModel.createProduct("   ", "50.00")
        testDispatcher.scheduler.advanceUntilIdle()

        val actionState = viewModel.actionState.value
        assertTrue(actionState is UiState.Error)
        assertEquals("Product name cannot be empty", (actionState as UiState.Error).message)
    }

    @Test
    fun testCreateProductValidationInvalidPrice() = runTest {
        viewModel.createProduct("Milk", "-10.00")
        testDispatcher.scheduler.advanceUntilIdle()

        val actionState = viewModel.actionState.value
        assertTrue(actionState is UiState.Error)
        assertEquals("Please enter a valid price (0.00 or greater)", (actionState as UiState.Error).message)
    }

    @Test
    fun testCreateProductSuccess() = runTest {
        viewModel.createProduct("Amul Butter 100g", "58.00")
        testDispatcher.scheduler.advanceUntilIdle()

        val actionState = viewModel.actionState.value
        assertTrue("Expected Success, got $actionState", actionState is UiState.Success)
        assertEquals("Amul Butter 100g", fakeRepository.lastCreateProductRequest?.name)
        assertEquals("58.00", fakeRepository.lastCreateProductRequest?.unitPrice)

        // Verify products state updated
        val productsState = viewModel.productsState.value
        assertTrue(productsState is UiState.Success)
        val items = (productsState as UiState.Success).data
        assertEquals(3, items.size)
        assertTrue(items.any { it.name == "Amul Butter 100g" })
    }

    @Test
    fun testUpdateProductValidationAndSuccess() = runTest {
        // Validation: Empty name
        viewModel.updateProduct("prod-1", "   ", "500.00")
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.actionState.value is UiState.Error)

        // Validation: Invalid price
        viewModel.updateProduct("prod-1", "Valid Name", "abc")
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.actionState.value is UiState.Error)

        // Success update
        viewModel.updateProduct("prod-1", "Basmati Rice Premium 5kg", "480.00", status = "active")
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.actionState.value is UiState.Success)
        assertEquals("Basmati Rice Premium 5kg", fakeRepository.lastUpdateProductRequest?.name)
        assertEquals("480.00", fakeRepository.lastUpdateProductRequest?.unitPrice)

        val productsState = viewModel.productsState.value
        assertTrue(productsState is UiState.Success)
        val updatedItem = (productsState as UiState.Success).data.first { it.id == "prod-1" }
        assertEquals("Basmati Rice Premium 5kg", updatedItem.name)
        assertEquals("480.00", updatedItem.unitPrice)
    }

    @Test
    fun testDeleteProductSuccess() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(2, (viewModel.productsState.value as UiState.Success).data.size)

        viewModel.deleteProduct("prod-1")
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.actionState.value is UiState.Success)
        assertEquals("prod-1", fakeRepository.lastDeletedProductId)

        val productsState = viewModel.productsState.value
        assertTrue(productsState is UiState.Success)
        val remaining = (productsState as UiState.Success).data
        assertEquals(1, remaining.size)
        assertEquals("prod-2", remaining[0].id)
    }

    @Test
    fun testDateRangeFilterUpdates() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(DateRangeFilter.THIS_MONTH, viewModel.selectedRange.value)

        viewModel.selectDateRange(DateRangeFilter.TODAY)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(DateRangeFilter.TODAY, viewModel.selectedRange.value)
        assertNotNull(fakeRepository.lastAnalyticsStartDate)
        assertEquals(fakeRepository.lastAnalyticsStartDate, fakeRepository.lastAnalyticsEndDate)

        viewModel.selectDateRange(DateRangeFilter.ALL_TIME)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(DateRangeFilter.ALL_TIME, viewModel.selectedRange.value)
        assertEquals(null, fakeRepository.lastAnalyticsStartDate)
        assertEquals(null, fakeRepository.lastAnalyticsEndDate)
    }

    @Test
    fun testEmptyProductsYieldsUiStateEmpty() = runTest {
        fakeRepository.products.clear()
        viewModel.loadProducts()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.productsState.value
        assertTrue("Expected Empty state when products list is empty, got $state", state is UiState.Empty)
    }

    @Test
    fun testEmptyAnalyticsYieldsUiStateEmpty() = runTest {
        fakeRepository.analyticsResponse = ProductAnalyticsResponse(
            summary = ProductSalesSummary("0.00", "0.00", 0L, null),
            topByQuantity = emptyList(),
            topByRevenue = emptyList(),
            dailySales = emptyList()
        )
        viewModel.loadAnalytics()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.analyticsState.value
        assertTrue("Expected Empty state for zero analytics, got $state", state is UiState.Empty)
    }

    @Test
    fun testRefreshEventsTriggerReload() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        // Add item directly to fake repo
        fakeRepository.products.add(
            Product(id = "prod-ext", name = "External Product", unitPrice = "99.00")
        )

        // Emit refresh event
        refreshCoordinator.notifyMutation(DataMutation.PRODUCTS)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.productsState.value
        assertTrue(state is UiState.Success)
        assertEquals(3, (state as UiState.Success).data.size)
    }

    @Test
    fun testAdminAuthorizationCheck() {
        viewModel.adminOverrideForTesting = true
        assertTrue(viewModel.isTenantAdmin)

        viewModel.adminOverrideForTesting = false
        assertFalse(viewModel.isTenantAdmin)
    }

    @Test
    fun testResetActionState() {
        viewModel.createProduct("   ", "10.00")
        assertTrue(viewModel.actionState.value is UiState.Error)

        viewModel.resetActionState()
        assertTrue(viewModel.actionState.value is UiState.Idle)
    }
}

class FakeProductRepository : ProductRepository {
    val products = mutableListOf(
        Product(id = "prod-1", name = "Basmati Rice 5kg", unitPrice = "450.00", status = "active"),
        Product(id = "prod-2", name = "Sunflower Oil 1L", unitPrice = "120.00", status = "active")
    )

    var lastCreateProductRequest: CreateProductRequest? = null
    var lastUpdateProductRequest: UpdateProductRequest? = null
    var lastDeletedProductId: String? = null
    var lastAnalyticsStartDate: String? = null
    var lastAnalyticsEndDate: String? = null

    var analyticsResponse = ProductAnalyticsResponse(
        summary = ProductSalesSummary(
            totalQuantitySold = "80.00",
            totalRevenue = "12000.00",
            distinctProductsCount = 2L,
            topProductName = "Basmati Rice 5kg"
        ),
        topByQuantity = listOf(
            TopSellingProduct(1, "prod-1", "Basmati Rice 5kg", "20.00", "9000.00"),
            TopSellingProduct(2, "prod-2", "Sunflower Oil 1L", "25.00", "3000.00")
        ),
        topByRevenue = listOf(
            TopSellingProduct(1, "prod-1", "Basmati Rice 5kg", "20.00", "9000.00")
        ),
        dailySales = listOf(
            ProductDailySales("2026-10-18", "5.00", "2250.00")
        )
    )

    override fun listProducts(
        page: Int,
        pageSize: Int,
        search: String?,
        status: String?
    ): Flow<Resource<List<Product>>> = flow {
        emit(Resource.Loading)
        val filtered = products.filter {
            (search.isNullOrBlank() || it.name.contains(search, ignoreCase = true)) &&
            (status.isNullOrBlank() || it.status.equals(status, ignoreCase = true))
        }
        emit(Resource.Success(filtered))
    }

    override fun getProductById(id: String): Flow<Resource<Product>> = flow {
        emit(Resource.Loading)
        val item = products.firstOrNull { it.id == id }
        if (item != null) emit(Resource.Success(item)) else emit(Resource.Error("Product not found"))
    }

    override fun createProduct(request: CreateProductRequest): Flow<Resource<Product>> = flow {
        emit(Resource.Loading)
        lastCreateProductRequest = request
        val created = Product(
            id = "prod-${products.size + 1}",
            name = request.name,
            unitPrice = request.unitPrice,
            status = "active"
        )
        products.add(created)
        emit(Resource.Success(created))
    }

    override fun updateProduct(id: String, request: UpdateProductRequest): Flow<Resource<Product>> = flow {
        emit(Resource.Loading)
        lastUpdateProductRequest = request
        val index = products.indexOfFirst { it.id == id }
        if (index != -1) {
            val current = products[index]
            val updated = current.copy(
                name = request.name ?: current.name,
                unitPrice = request.unitPrice ?: current.unitPrice,
                status = request.status ?: current.status
            )
            products[index] = updated
            emit(Resource.Success(updated))
        } else {
            emit(Resource.Error("Product not found"))
        }
    }

    override fun deleteProduct(id: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading)
        lastDeletedProductId = id
        products.removeAll { it.id == id }
        emit(Resource.Success(Unit))
    }

    override fun getProductAnalytics(
        startDate: String?,
        endDate: String?,
        limit: Int
    ): Flow<Resource<ProductAnalyticsResponse>> = flow {
        emit(Resource.Loading)
        lastAnalyticsStartDate = startDate
        lastAnalyticsEndDate = endDate
        emit(Resource.Success(analyticsResponse))
    }
}
