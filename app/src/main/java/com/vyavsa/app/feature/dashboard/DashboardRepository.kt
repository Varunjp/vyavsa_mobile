package com.vyavsa.app.feature.dashboard

import com.vyavsa.app.core.common.Resource
import com.vyavsa.app.core.model.FinancialMetrics
import com.vyavsa.app.core.model.TenantProfile
import com.vyavsa.app.core.model.TenantSubscription
import com.vyavsa.app.core.network.ApiErrorParser
import com.vyavsa.app.core.network.VyavsaApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

interface DashboardRepository {
    fun getDashboardMetrics(): Flow<Resource<FinancialMetrics>>
    fun getSalesDashboard(date: String? = null): Flow<Resource<com.vyavsa.app.core.model.SalesDashboardResponse>>
    fun getOfficeDashboard(date: String? = null): Flow<Resource<com.vyavsa.app.core.model.OfficeDashboardResponse>>
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
                emit(ApiErrorParser.fromApiResponse(response, "Failed to load dashboard metrics"))
            }
        } catch (e: Throwable) {
            emit(ApiErrorParser.parse(e, "Network error loading dashboard"))
        }
    }.flowOn(Dispatchers.IO)

    override fun getSalesDashboard(date: String?): Flow<Resource<com.vyavsa.app.core.model.SalesDashboardResponse>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.getSalesDashboard(date)
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data))
            } else {
                emit(ApiErrorParser.fromApiResponse(response, "Failed to load sales dashboard"))
            }
        } catch (e: Throwable) {
            emit(ApiErrorParser.parse(e, "Network error loading sales dashboard"))
        }
    }.flowOn(Dispatchers.IO)

    override fun getOfficeDashboard(date: String?): Flow<Resource<com.vyavsa.app.core.model.OfficeDashboardResponse>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.getOfficeDashboard(date)
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data))
            } else {
                emit(ApiErrorParser.fromApiResponse(response, "Failed to load office dashboard"))
            }
        } catch (e: Throwable) {
            emit(ApiErrorParser.parse(e, "Network error loading office dashboard"))
        }
    }.flowOn(Dispatchers.IO)

    override fun getProfile(): Flow<Resource<TenantProfile>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.getProfile()
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data))
            } else {
                emit(ApiErrorParser.fromApiResponse(response, "Failed to load profile"))
            }
        } catch (e: Throwable) {
            emit(ApiErrorParser.parse(e, "Network error loading profile"))
        }
    }.flowOn(Dispatchers.IO)

    override fun getSubscription(): Flow<Resource<TenantSubscription>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.getSubscription()
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data))
            } else {
                emit(ApiErrorParser.fromApiResponse(response, "Failed to load subscription"))
            }
        } catch (e: Throwable) {
            emit(ApiErrorParser.parse(e, "Network error loading subscription"))
        }
    }.flowOn(Dispatchers.IO)
}
