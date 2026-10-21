package com.vyavsa.app.feature.products

import com.vyavsa.app.core.common.Resource
import com.vyavsa.app.core.model.CreateProductRequest
import com.vyavsa.app.core.model.Product
import com.vyavsa.app.core.model.ProductAnalyticsResponse
import com.vyavsa.app.core.model.UpdateProductRequest
import com.vyavsa.app.core.network.ApiErrorParser
import com.vyavsa.app.core.network.VyavsaApiService
import com.vyavsa.app.core.sync.DataMutation
import com.vyavsa.app.core.sync.DataRefreshCoordinator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

interface ProductRepository {
    fun listProducts(
        page: Int = 1,
        pageSize: Int = 100,
        search: String? = null,
        status: String? = null
    ): Flow<Resource<List<Product>>>

    fun getProductById(id: String): Flow<Resource<Product>>

    fun createProduct(request: CreateProductRequest): Flow<Resource<Product>>

    fun updateProduct(id: String, request: UpdateProductRequest): Flow<Resource<Product>>

    fun deleteProduct(id: String): Flow<Resource<Unit>>

    fun getProductAnalytics(
        startDate: String? = null,
        endDate: String? = null,
        limit: Int = 10
    ): Flow<Resource<ProductAnalyticsResponse>>
}

@Singleton
class ProductRepositoryImpl @Inject constructor(
    private val apiService: VyavsaApiService,
    private val refreshCoordinator: DataRefreshCoordinator
) : ProductRepository {

    override fun listProducts(
        page: Int,
        pageSize: Int,
        search: String?,
        status: String?
    ): Flow<Resource<List<Product>>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.listProducts(
                page = page,
                pageSize = pageSize,
                search = search?.ifBlank { null },
                status = status?.ifBlank { null }
            )
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data, response.message))
            } else {
                emit(ApiErrorParser.fromApiResponse(response, "Failed to load products"))
            }
        } catch (e: Throwable) {
            emit(ApiErrorParser.parse(e, "Network error loading product catalog"))
        }
    }.flowOn(Dispatchers.IO)

    override fun getProductById(id: String): Flow<Resource<Product>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.getProductById(id)
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data, response.message))
            } else {
                emit(ApiErrorParser.fromApiResponse(response, "Failed to load product details"))
            }
        } catch (e: Throwable) {
            emit(ApiErrorParser.parse(e, "Network error loading product details"))
        }
    }.flowOn(Dispatchers.IO)

    override fun createProduct(request: CreateProductRequest): Flow<Resource<Product>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.createProduct(request)
            if (response.success && response.data != null) {
                refreshCoordinator.notifyMutation(DataMutation.PRODUCTS)
                emit(Resource.Success(response.data, response.message))
            } else {
                emit(ApiErrorParser.fromApiResponse(response, "Failed to create product"))
            }
        } catch (e: Throwable) {
            emit(ApiErrorParser.parse(e, "Network error creating product"))
        }
    }.flowOn(Dispatchers.IO)

    override fun updateProduct(id: String, request: UpdateProductRequest): Flow<Resource<Product>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.updateProduct(id, request)
            if (response.success && response.data != null) {
                refreshCoordinator.notifyMutation(DataMutation.PRODUCTS)
                emit(Resource.Success(response.data, response.message))
            } else {
                emit(ApiErrorParser.fromApiResponse(response, "Failed to update product"))
            }
        } catch (e: Throwable) {
            emit(ApiErrorParser.parse(e, "Network error updating product"))
        }
    }.flowOn(Dispatchers.IO)

    override fun deleteProduct(id: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.deleteProduct(id)
            if (response.success) {
                refreshCoordinator.notifyMutation(DataMutation.PRODUCTS)
                emit(Resource.Success(Unit, response.message))
            } else {
                emit(ApiErrorParser.fromApiResponse(response, "Failed to delete product"))
            }
        } catch (e: Throwable) {
            emit(ApiErrorParser.parse(e, "Network error deleting product"))
        }
    }.flowOn(Dispatchers.IO)

    override fun getProductAnalytics(
        startDate: String?,
        endDate: String?,
        limit: Int
    ): Flow<Resource<ProductAnalyticsResponse>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.getProductAnalytics(
                startDate = startDate?.ifBlank { null },
                endDate = endDate?.ifBlank { null },
                limit = limit
            )
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data, response.message))
            } else {
                emit(ApiErrorParser.fromApiResponse(response, "Failed to load product analytics"))
            }
        } catch (e: Throwable) {
            emit(ApiErrorParser.parse(e, "Network error loading product sales analytics"))
        }
    }.flowOn(Dispatchers.IO)
}
