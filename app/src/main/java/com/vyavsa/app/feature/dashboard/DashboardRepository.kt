package com.vyavsa.app.feature.dashboard

import com.vyavsa.app.core.common.Resource
import com.vyavsa.app.core.model.FinancialMetrics
import com.vyavsa.app.core.model.TenantProfile
import com.vyavsa.app.core.model.TenantSubscription
import com.vyavsa.app.core.network.VyavsaApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

interface DashboardRepository {
    fun getDashboardMetrics(): Flow<Resource<FinancialMetrics>>
    fun getProfile(): Flow<Resource<TenantProfile>>
    fun getSubscription(): Flow<Resource<TenantSubscription>>
}

@Singleton
class DashboardRepositoryImpl @Inject constructor(
    private val apiService: VyavsaApiService
) : DashboardRepository {

    override fun getDashboardMetrics(): Flow<Resource<FinancialMetrics>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.getDashboardMetrics()
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data))
            } else {
                emit(Resource.Error(response.error?.message ?: response.message ?: "Failed to load dashboard metrics", response.error?.code))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Network error loading dashboard"))
        }
    }.flowOn(Dispatchers.IO)

    override fun getProfile(): Flow<Resource<TenantProfile>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.getProfile()
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data))
            } else {
                emit(Resource.Error(response.error?.message ?: "Failed to load profile"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Network error loading profile"))
        }
    }.flowOn(Dispatchers.IO)

    override fun getSubscription(): Flow<Resource<TenantSubscription>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.getSubscription()
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data))
            } else {
                emit(Resource.Error(response.error?.message ?: "Failed to load subscription"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Network error loading subscription"))
        }
    }.flowOn(Dispatchers.IO)
}
