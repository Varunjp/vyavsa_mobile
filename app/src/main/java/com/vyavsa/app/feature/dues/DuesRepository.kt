package com.vyavsa.app.feature.dues

import com.vyavsa.app.core.common.Resource
import com.vyavsa.app.core.model.AdjustCustomerBalanceRequest
import com.vyavsa.app.core.model.CollectCustomerPaymentRequest
import com.vyavsa.app.core.model.CreateCustomerRequest
import com.vyavsa.app.core.model.Customer
import com.vyavsa.app.core.model.CustomerBalance
import com.vyavsa.app.core.model.CustomerBalanceAdjustment
import com.vyavsa.app.core.model.CustomerPaymentResponse
import com.vyavsa.app.core.model.CustomerStatement
import com.vyavsa.app.core.model.LineSale
import com.vyavsa.app.core.model.RecordPaymentRequest
import com.vyavsa.app.core.model.UpdateCustomerRequest
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

interface DuesRepository {
    fun listCustomers(search: String?): Flow<Resource<List<Customer>>>
    fun createCustomer(name: String, phone: String?, openingBalance: String): Flow<Resource<Customer>>
    fun getCustomerById(id: String): Flow<Resource<Customer>>
    fun updateCustomer(id: String, name: String?, phone: String?, status: String?): Flow<Resource<Customer>>
    fun deleteCustomer(id: String): Flow<Resource<Unit>>
    fun adjustCustomerBalance(id: String, newBalance: String?, adjustmentAmount: String?, reason: String): Flow<Resource<CustomerBalanceAdjustment>>
    fun listCustomerAdjustments(id: String): Flow<Resource<List<CustomerBalanceAdjustment>>>
    fun getCustomerBalance(id: String): Flow<Resource<CustomerBalance>>
    fun getCustomerStatement(id: String): Flow<Resource<CustomerStatement>>
    fun listCustomerSales(customerId: String): Flow<Resource<List<LineSale>>>
    fun recordPayment(id: String, amount: String, paymentMethod: String, date: String?, note: String?): Flow<Resource<Unit>>
    fun collectCustomerPayment(
        id: String,
        amount: String,
        paymentMethod: String,
        bankId: String? = null,
        bankName: String? = null,
        date: String? = null,
        referenceId: String? = null,
        note: String? = null
    ): Flow<Resource<CustomerPaymentResponse>>
    fun listCustomerCollections(customerId: String): Flow<Resource<List<CustomerPaymentResponse>>>
}

@Singleton
class DuesRepositoryImpl @Inject constructor(
    private val apiService: VyavsaApiService,
    private val refreshCoordinator: DataRefreshCoordinator? = null
) : DuesRepository {

    override fun listCustomers(search: String?): Flow<Resource<List<Customer>>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.listCustomers(search = search)
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data))
            } else {
                emit(ApiErrorParser.fromApiResponse(response, "Failed to load customers"))
            }
        } catch (e: Throwable) {
            emit(ApiErrorParser.parse(e, "Network error loading dues"))
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
                emit(ApiErrorParser.fromApiResponse(response, "Failed to add customer"))
            }
        } catch (e: Throwable) {
            emit(ApiErrorParser.parse(e, "Network error adding customer"))
        }
    }.flowOn(Dispatchers.IO)

    override fun getCustomerById(id: String): Flow<Resource<Customer>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.getCustomerById(id)
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data))
            } else {
                emit(ApiErrorParser.fromApiResponse(response, "Failed to load customer profile"))
            }
        } catch (e: Throwable) {
            emit(ApiErrorParser.parse(e, "Network error loading customer"))
        }
    }.flowOn(Dispatchers.IO)

    override fun updateCustomer(id: String, name: String?, phone: String?, status: String?): Flow<Resource<Customer>> = flow {
        emit(Resource.Loading)
        try {
            val req = UpdateCustomerRequest(
                customerName = name?.trim()?.ifBlank { null },
                phone = phone?.trim()?.ifBlank { null },
                status = status
            )
            val response = apiService.updateCustomer(id, req)
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data, response.message))
            } else {
                emit(ApiErrorParser.fromApiResponse(response, "Failed to update customer"))
            }
        } catch (e: Throwable) {
            emit(ApiErrorParser.parse(e, "Network error updating customer"))
        }
    }.flowOn(Dispatchers.IO)

    override fun deleteCustomer(id: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.deleteCustomer(id)
            if (response.success) {
                emit(Resource.Success(Unit, response.message))
            } else {
                emit(ApiErrorParser.fromApiResponse(response, "Failed to delete customer"))
            }
        } catch (e: Throwable) {
            emit(ApiErrorParser.parse(e, "Network error deleting customer"))
        }
    }.flowOn(Dispatchers.IO)

    override fun adjustCustomerBalance(
        id: String,
        newBalance: String?,
        adjustmentAmount: String?,
        reason: String
    ): Flow<Resource<CustomerBalanceAdjustment>> = flow {
        emit(Resource.Loading)
        try {
            val req = AdjustCustomerBalanceRequest(
                newBalance = newBalance?.trim()?.ifBlank { null },
                adjustmentAmount = adjustmentAmount?.trim()?.ifBlank { null },
                reason = reason.trim()
            )
            val response = apiService.adjustCustomerBalance(id, req)
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data, response.message))
            } else {
                emit(ApiErrorParser.fromApiResponse(response, "Failed to adjust balance"))
            }
        } catch (e: Throwable) {
            emit(ApiErrorParser.parse(e, "Network error adjusting balance"))
        }
    }.flowOn(Dispatchers.IO)

    override fun listCustomerAdjustments(id: String): Flow<Resource<List<CustomerBalanceAdjustment>>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.listCustomerAdjustments(id)
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data))
            } else {
                emit(ApiErrorParser.fromApiResponse(response, "Failed to load balance adjustments"))
            }
        } catch (e: Throwable) {
            emit(ApiErrorParser.parse(e, "Network error loading adjustments"))
        }
    }.flowOn(Dispatchers.IO)

    override fun getCustomerBalance(id: String): Flow<Resource<CustomerBalance>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.getCustomerBalance(id)
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data))
            } else {
                emit(ApiErrorParser.fromApiResponse(response, "Failed to load customer balance"))
            }
        } catch (e: Throwable) {
            emit(ApiErrorParser.parse(e, "Network error loading customer balance"))
        }
    }.flowOn(Dispatchers.IO)

    override fun getCustomerStatement(id: String): Flow<Resource<CustomerStatement>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.getCustomerStatement(id)
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data))
            } else {
                emit(ApiErrorParser.fromApiResponse(response, "Failed to load customer statement"))
            }
        } catch (e: Throwable) {
            emit(ApiErrorParser.parse(e, "Network error loading customer statement"))
        }
    }.flowOn(Dispatchers.IO)

    override fun listCustomerSales(customerId: String): Flow<Resource<List<LineSale>>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.listLineSales(customerId = customerId)
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data))
            } else {
                emit(ApiErrorParser.fromApiResponse(response, "Failed to load customer sales"))
            }
        } catch (e: Throwable) {
            emit(ApiErrorParser.parse(e, "Network error loading customer sales"))
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
                emit(ApiErrorParser.fromApiResponse(response, "Failed to record payment"))
            }
        } catch (e: Throwable) {
            emit(ApiErrorParser.parse(e, "Network error recording payment"))
        }
    }.flowOn(Dispatchers.IO)

    override fun collectCustomerPayment(
        id: String,
        amount: String,
        paymentMethod: String,
        bankId: String?,
        bankName: String?,
        date: String?,
        referenceId: String?,
        note: String?
    ): Flow<Resource<CustomerPaymentResponse>> = flow {
        emit(Resource.Loading)
        try {
            val normMethod = paymentMethod.lowercase().trim()
            val req = CollectCustomerPaymentRequest(
                customerId = id,
                amount = amount.trim(),
                paymentMethod = normMethod,
                cashAmount = if (normMethod == "cash") amount.trim() else null,
                bankId = if (normMethod == "bank") bankId else null,
                bankName = if (normMethod == "bank") bankName else null,
                paymentDate = date,
                referenceId = referenceId?.trim()?.ifBlank { null },
                note = note?.trim()?.ifBlank { null },
                idempotencyKey = java.util.UUID.randomUUID().toString()
            )
            val response = apiService.collectCustomerPayment(id, req)
            if (response.success && response.data != null) {
                refreshCoordinator?.notifyMutation(DataMutation.DUES)
                if (normMethod == "bank") {
                    refreshCoordinator?.notifyMutation(DataMutation.BANK)
                } else {
                    refreshCoordinator?.notifyMutation(DataMutation.CASH)
                }
                emit(Resource.Success(response.data, response.message))
            } else {
                emit(ApiErrorParser.fromApiResponse(response, "Failed to collect customer payment"))
            }
        } catch (e: Throwable) {
            emit(ApiErrorParser.parse(e, "Network error collecting customer payment"))
        }
    }.flowOn(Dispatchers.IO)

    override fun listCustomerCollections(customerId: String): Flow<Resource<List<CustomerPaymentResponse>>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.listCustomerCollections(id = customerId)
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data))
            } else {
                emit(ApiErrorParser.fromApiResponse(response, "Failed to load customer collections"))
            }
        } catch (e: Throwable) {
            emit(ApiErrorParser.parse(e, "Network error loading customer collections"))
        }
    }.flowOn(Dispatchers.IO)
}
