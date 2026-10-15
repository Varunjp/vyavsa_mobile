package com.vyavsa.app.feature.dues

import com.vyavsa.app.core.common.Resource
import com.vyavsa.app.core.model.CreateCustomerRequest
import com.vyavsa.app.core.model.Customer
import com.vyavsa.app.core.model.CustomerBalance
import com.vyavsa.app.core.model.CustomerStatement
import com.vyavsa.app.core.model.RecordPaymentRequest
import com.vyavsa.app.core.network.VyavsaApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

interface DuesRepository {
    fun listCustomers(search: String?): Flow<Resource<List<Customer>>>
    fun createCustomer(name: String, phone: String?, openingBalance: String): Flow<Resource<Customer>>
    fun getCustomerBalance(id: String): Flow<Resource<CustomerBalance>>
    fun getCustomerStatement(id: String): Flow<Resource<CustomerStatement>>
    fun recordPayment(id: String, amount: String, paymentMethod: String, date: String?, note: String?): Flow<Resource<Unit>>
}

@Singleton
class DuesRepositoryImpl @Inject constructor(
    private val apiService: VyavsaApiService
) : DuesRepository {

    override fun listCustomers(search: String?): Flow<Resource<List<Customer>>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.listCustomers(search = search)
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data))
            } else {
                emit(Resource.Error(response.error?.message ?: "Failed to load customers"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Network error loading dues"))
        }
    }.flowOn(Dispatchers.IO)

    override fun createCustomer(name: String, phone: String?, openingBalance: String): Flow<Resource<Customer>> = flow {
        emit(Resource.Loading)
        try {
            val req = CreateCustomerRequest(
                customerName = name.trim(),
                phone = phone?.trim()?.ifBlank { null },
                openingBalance = openingBalance.trim().ifBlank { "0.00" }
            )
            val response = apiService.createCustomer(req)
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data, response.message))
            } else {
                emit(Resource.Error(response.error?.message ?: response.message ?: "Failed to add customer"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Network error adding customer"))
        }
    }.flowOn(Dispatchers.IO)

    override fun getCustomerBalance(id: String): Flow<Resource<CustomerBalance>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.getCustomerBalance(id)
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data))
            } else {
                emit(Resource.Error(response.error?.message ?: "Failed to load customer balance"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Network error"))
        }
    }.flowOn(Dispatchers.IO)

    override fun getCustomerStatement(id: String): Flow<Resource<CustomerStatement>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.getCustomerStatement(id)
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data))
            } else {
                emit(Resource.Error(response.error?.message ?: "Failed to load customer statement"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Network error"))
        }
    }.flowOn(Dispatchers.IO)

    override fun recordPayment(
        id: String,
        amount: String,
        paymentMethod: String,
        date: String?,
        note: String?
    ): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading)
        try {
            val req = RecordPaymentRequest(
                amount = amount.trim(),
                paymentMethod = paymentMethod,
                paymentDate = date,
                note = note
            )
            val response = apiService.recordCustomerPayment(id, req)
            if (response.success) {
                emit(Resource.Success(Unit, response.message))
            } else {
                emit(Resource.Error(response.error?.message ?: response.message ?: "Failed to record payment"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Network error recording payment"))
        }
    }.flowOn(Dispatchers.IO)
}
