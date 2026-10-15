package com.vyavsa.app.feature.bank

import com.vyavsa.app.core.common.Resource
import com.vyavsa.app.core.model.CreateBankRequest
import com.vyavsa.app.core.model.TenantBank
import com.vyavsa.app.core.model.UpdateBankRequest
import com.vyavsa.app.core.network.VyavsaApiService
import com.vyavsa.app.core.sync.DataMutation
import com.vyavsa.app.core.sync.DataRefreshCoordinator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

interface BankRepository {
    fun listBanks(search: String? = null, status: String? = null): Flow<Resource<List<TenantBank>>>
    fun createBank(request: CreateBankRequest): Flow<Resource<TenantBank>>
    fun updateBank(id: String, request: UpdateBankRequest): Flow<Resource<TenantBank>>
    fun deleteBank(id: String): Flow<Resource<Unit>>
}

@Singleton
class BankRepositoryImpl @Inject constructor(
    private val apiService: VyavsaApiService,
    private val refreshCoordinator: DataRefreshCoordinator
) : BankRepository {

    override fun listBanks(search: String?, status: String?): Flow<Resource<List<TenantBank>>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.listBanks(page = 1, pageSize = 100, search = search, status = status)
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data))
            } else {
                emit(Resource.Error(response.error?.message ?: response.message ?: "Failed to load bank accounts"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Network error loading bank accounts"))
        }
    }.flowOn(Dispatchers.IO)

    override fun createBank(request: CreateBankRequest): Flow<Resource<TenantBank>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.createBank(request)
            if (response.success && response.data != null) {
                refreshCoordinator.notifyMutation(DataMutation.BANK)
                emit(Resource.Success(response.data, response.message))
            } else {
                emit(Resource.Error(response.error?.message ?: response.message ?: "Failed to create bank account", response.error?.code))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Network error creating bank account"))
        }
    }.flowOn(Dispatchers.IO)

    override fun updateBank(id: String, request: UpdateBankRequest): Flow<Resource<TenantBank>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.updateBank(id, request)
            if (response.success && response.data != null) {
                refreshCoordinator.notifyMutation(DataMutation.BANK)
                emit(Resource.Success(response.data, response.message))
            } else {
                emit(Resource.Error(response.error?.message ?: response.message ?: "Failed to update bank account", response.error?.code))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Network error updating bank account"))
        }
    }.flowOn(Dispatchers.IO)

    override fun deleteBank(id: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.deleteBank(id)
            if (response.success) {
                refreshCoordinator.notifyMutation(DataMutation.BANK)
                emit(Resource.Success(Unit, response.message))
            } else {
                emit(Resource.Error(response.error?.message ?: response.message ?: "Failed to delete bank account", response.error?.code))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Network error deleting bank account"))
        }
    }.flowOn(Dispatchers.IO)
}
