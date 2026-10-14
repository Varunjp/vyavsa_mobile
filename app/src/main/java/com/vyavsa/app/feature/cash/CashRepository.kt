package com.vyavsa.app.feature.cash

import com.vyavsa.app.core.common.Resource
import com.vyavsa.app.core.model.AddCashRequest
import com.vyavsa.app.core.model.AdjustCashRequest
import com.vyavsa.app.core.model.CashBalanceResponse
import com.vyavsa.app.core.model.CashTransaction
import com.vyavsa.app.core.network.VyavsaApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

interface CashRepository {
    fun getCashBalance(): Flow<Resource<CashBalanceResponse>>
    fun listCashTransactions(): Flow<Resource<List<CashTransaction>>>
    fun addCash(amount: String, description: String?): Flow<Resource<CashTransaction>>
    fun adjustCash(actualBalance: String, reason: String): Flow<Resource<CashTransaction>>
}

@Singleton
class CashRepositoryImpl @Inject constructor(
    private val apiService: VyavsaApiService
) : CashRepository {

    override fun getCashBalance(): Flow<Resource<CashBalanceResponse>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.getCashBalance()
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data))
            } else {
                emit(Resource.Error(response.error?.message ?: "Failed to fetch cash balance"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Network error"))
        }
    }.flowOn(Dispatchers.IO)

    override fun listCashTransactions(): Flow<Resource<List<CashTransaction>>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.listCashTransactions()
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data))
            } else {
                emit(Resource.Error(response.error?.message ?: "Failed to load cash transactions"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Network error"))
        }
    }.flowOn(Dispatchers.IO)

    override fun addCash(amount: String, description: String?): Flow<Resource<CashTransaction>> = flow {
        emit(Resource.Loading)
        try {
            val req = AddCashRequest(
                amount = amount.trim(),
                description = description?.trim()?.ifBlank { null }
            )
            val response = apiService.addCash(req)
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data, response.message))
            } else {
                emit(Resource.Error(response.error?.message ?: response.message ?: "Failed to add cash"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Network error adding cash"))
        }
    }.flowOn(Dispatchers.IO)

    override fun adjustCash(actualBalance: String, reason: String): Flow<Resource<CashTransaction>> = flow {
        emit(Resource.Loading)
        try {
            val req = AdjustCashRequest(
                actualBalance = actualBalance.trim(),
                reason = reason.trim()
            )
            val response = apiService.adjustCash(req)
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data, response.message))
            } else {
                emit(Resource.Error(response.error?.message ?: response.message ?: "Failed to adjust balance"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Network error adjusting cash"))
        }
    }.flowOn(Dispatchers.IO)
}
