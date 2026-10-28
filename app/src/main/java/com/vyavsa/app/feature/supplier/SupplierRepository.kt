package com.vyavsa.app.feature.supplier

import com.vyavsa.app.core.common.Resource
import com.vyavsa.app.core.model.CreateSupplierRequest
import com.vyavsa.app.core.model.RecordSupplierPaymentRequest
import com.vyavsa.app.core.model.Supplier
import com.vyavsa.app.core.model.SupplierBalance
import com.vyavsa.app.core.model.SupplierPaymentResponse
import com.vyavsa.app.core.model.UpdateSupplierRequest
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

interface SupplierRepository {
    fun listSuppliers(search: String? = null, status: String? = null): Flow<Resource<List<Supplier>>>
    fun createSupplier(name: String, phone: String? = null, status: String = "active"): Flow<Resource<Supplier>>
    fun getSupplierById(id: String): Flow<Resource<Supplier>>
    fun updateSupplier(id: String, name: String?, phone: String?, status: String?): Flow<Resource<Supplier>>
    fun deleteSupplier(id: String): Flow<Resource<Unit>>
    fun getSupplierBalance(id: String): Flow<Resource<SupplierBalance>>
    fun recordSupplierPayment(
        supplierId: String,
        amount: String,
        paymentMethod: String = "cash",
        bankId: String? = null,
        paymentDate: String? = null,
        referenceId: String? = null,
        note: String? = null,
        purchaseId: String? = null
    ): Flow<Resource<SupplierPaymentResponse>>
}

@Singleton
class SupplierRepositoryImpl @Inject constructor(
    private val apiService: VyavsaApiService,
    private val refreshCoordinator: DataRefreshCoordinator? = null
) : SupplierRepository {

    override fun listSuppliers(search: String?, status: String?): Flow<Resource<List<Supplier>>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.listSuppliers(page = 1, pageSize = 100, search = search, status = status)
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data))
            } else {
                emit(ApiErrorParser.fromApiResponse(response, "Failed to load suppliers"))
            }
        } catch (e: Throwable) {
            emit(ApiErrorParser.parse(e, "Network error loading suppliers"))
        }
    }.flowOn(Dispatchers.IO)

    override fun createSupplier(name: String, phone: String?, status: String): Flow<Resource<Supplier>> = flow {
        emit(Resource.Loading)
        try {
            val req = CreateSupplierRequest(
                supplierName = name.trim(),
                phone = phone?.trim()?.ifBlank { null },
                status = status
            )
            val response = apiService.createSupplier(req)
            if (response.success && response.data != null) {
                refreshCoordinator?.notifyMutation(DataMutation.PURCHASES)
                emit(Resource.Success(response.data, response.message))
            } else {
                emit(ApiErrorParser.fromApiResponse(response, "Failed to create supplier"))
            }
        } catch (e: Throwable) {
            emit(ApiErrorParser.parse(e, "Network error creating supplier"))
        }
    }.flowOn(Dispatchers.IO)

    override fun getSupplierById(id: String): Flow<Resource<Supplier>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.getSupplierById(id)
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data))
            } else {
                emit(ApiErrorParser.fromApiResponse(response, "Failed to fetch supplier"))
            }
        } catch (e: Throwable) {
            emit(ApiErrorParser.parse(e, "Network error fetching supplier"))
        }
    }.flowOn(Dispatchers.IO)

    override fun updateSupplier(id: String, name: String?, phone: String?, status: String?): Flow<Resource<Supplier>> = flow {
        emit(Resource.Loading)
        try {
            val req = UpdateSupplierRequest(
                supplierName = name?.trim()?.ifBlank { null },
                phone = phone?.trim()?.ifBlank { null },
                status = status
            )
            val response = apiService.updateSupplier(id, req)
            if (response.success && response.data != null) {
                refreshCoordinator?.notifyMutation(DataMutation.PURCHASES)
                emit(Resource.Success(response.data, response.message))
            } else {
                emit(ApiErrorParser.fromApiResponse(response, "Failed to update supplier"))
            }
        } catch (e: Throwable) {
            emit(ApiErrorParser.parse(e, "Network error updating supplier"))
        }
    }.flowOn(Dispatchers.IO)

    override fun deleteSupplier(id: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.deleteSupplier(id)
            if (response.success) {
                refreshCoordinator?.notifyMutation(DataMutation.PURCHASES)
                emit(Resource.Success(Unit, response.message))
            } else {
                emit(ApiErrorParser.fromApiResponse(response, "Failed to delete supplier"))
            }
        } catch (e: Throwable) {
            emit(ApiErrorParser.parse(e, "Network error deleting supplier"))
        }
    }.flowOn(Dispatchers.IO)

    override fun getSupplierBalance(id: String): Flow<Resource<SupplierBalance>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.getSupplierBalance(id)
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data))
            } else {
                emit(ApiErrorParser.fromApiResponse(response, "Failed to fetch supplier balance"))
            }
        } catch (e: Throwable) {
            emit(ApiErrorParser.parse(e, "Network error fetching supplier balance"))
        }
    }.flowOn(Dispatchers.IO)

    override fun recordSupplierPayment(
        supplierId: String,
        amount: String,
        paymentMethod: String,
        bankId: String?,
        paymentDate: String?,
        referenceId: String?,
        note: String?,
        purchaseId: String?
    ): Flow<Resource<SupplierPaymentResponse>> = flow {
        emit(Resource.Loading)
        try {
            val normMethod = paymentMethod.lowercase().trim()
            val req = RecordSupplierPaymentRequest(
                supplierId = supplierId,
                purchaseId = purchaseId,
                amount = amount.trim(),
                paymentMethod = normMethod,
                bankId = if (normMethod == "bank") bankId else null,
                paymentDate = paymentDate,
                referenceId = referenceId?.trim()?.ifBlank { null },
                note = note?.trim()?.ifBlank { null }
            )
            val response = apiService.recordSupplierPayment(supplierId, req)
            if (response.success && response.data != null) {
                refreshCoordinator?.notifyMutation(DataMutation.PURCHASES)
                if (normMethod == "bank") {
                    refreshCoordinator?.notifyMutation(DataMutation.BANK)
                } else {
                    refreshCoordinator?.notifyMutation(DataMutation.CASH)
                }
                emit(Resource.Success(response.data, response.message))
            } else {
                emit(ApiErrorParser.fromApiResponse(response, "Failed to record supplier payment"))
            }
        } catch (e: Throwable) {
            emit(ApiErrorParser.parse(e, "Network error recording supplier payment"))
        }
    }.flowOn(Dispatchers.IO)
}
