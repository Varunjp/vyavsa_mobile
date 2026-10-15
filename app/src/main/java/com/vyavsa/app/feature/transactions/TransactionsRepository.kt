package com.vyavsa.app.feature.transactions

import com.vyavsa.app.core.common.Resource
import com.vyavsa.app.core.model.CounterSale
import com.vyavsa.app.core.model.CreateCounterSaleRequest
import com.vyavsa.app.core.model.CreateExpenseRequest
import com.vyavsa.app.core.model.CreateLineSaleRequest
import com.vyavsa.app.core.model.Customer
import com.vyavsa.app.core.model.Expense
import com.vyavsa.app.core.model.LineSale
import com.vyavsa.app.core.network.VyavsaApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

interface TransactionsRepository {
    fun listLineSales(date: String?, search: String?): Flow<Resource<List<LineSale>>>
    fun createLineSale(request: CreateLineSaleRequest): Flow<Resource<LineSale>>
    fun listCounterSales(date: String?, search: String?): Flow<Resource<List<CounterSale>>>
    fun createCounterSale(request: CreateCounterSaleRequest): Flow<Resource<CounterSale>>
    fun listExpenses(date: String?, search: String?): Flow<Resource<List<Expense>>>
    fun createExpense(request: CreateExpenseRequest): Flow<Resource<Expense>>
    fun listCustomers(): Flow<Resource<List<Customer>>>
}

@Singleton
class TransactionsRepositoryImpl @Inject constructor(
    private val apiService: VyavsaApiService
) : TransactionsRepository {

    override fun listLineSales(date: String?, search: String?): Flow<Resource<List<LineSale>>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.listLineSales(date = date, search = search)
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data))
            } else {
                emit(Resource.Error(response.error?.message ?: "Failed to load line sales"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Network error loading line sales"))
        }
    }.flowOn(Dispatchers.IO)

    override fun createLineSale(request: CreateLineSaleRequest): Flow<Resource<LineSale>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.createLineSale(request)
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data, response.message))
            } else {
                emit(Resource.Error(response.error?.message ?: response.message ?: "Failed to create line sale", response.error?.code))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Network error creating sale"))
        }
    }.flowOn(Dispatchers.IO)

    override fun listCounterSales(date: String?, search: String?): Flow<Resource<List<CounterSale>>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.listCounterSales(date = date, search = search)
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data))
            } else {
                emit(Resource.Error(response.error?.message ?: "Failed to load counter sales"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Network error loading counter sales"))
        }
    }.flowOn(Dispatchers.IO)

    override fun createCounterSale(request: CreateCounterSaleRequest): Flow<Resource<CounterSale>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.createCounterSale(request)
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data, response.message))
            } else {
                emit(Resource.Error(response.error?.message ?: response.message ?: "Failed to record counter sale", response.error?.code))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Network error recording sale"))
        }
    }.flowOn(Dispatchers.IO)

    override fun listExpenses(date: String?, search: String?): Flow<Resource<List<Expense>>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.listExpenses(date = date, search = search)
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data))
            } else {
                emit(Resource.Error(response.error?.message ?: "Failed to load expenses"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Network error loading expenses"))
        }
    }.flowOn(Dispatchers.IO)

    override fun createExpense(request: CreateExpenseRequest): Flow<Resource<Expense>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.createExpense(request)
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data, response.message))
            } else {
                emit(Resource.Error(response.error?.message ?: response.message ?: "Failed to record expense", response.error?.code))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Network error recording expense"))
        }
    }.flowOn(Dispatchers.IO)

    override fun listCustomers(): Flow<Resource<List<Customer>>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.listCustomers()
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data))
            } else {
                emit(Resource.Error(response.error?.message ?: "Failed to load customers"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Network error"))
        }
    }.flowOn(Dispatchers.IO)
}
