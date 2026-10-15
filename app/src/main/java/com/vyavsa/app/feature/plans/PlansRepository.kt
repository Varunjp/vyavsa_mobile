package com.vyavsa.app.feature.plans

import com.vyavsa.app.core.common.Resource
import com.vyavsa.app.core.model.PlanTransaction
import com.vyavsa.app.core.model.PurchasePlanRequest
import com.vyavsa.app.core.model.SubscriptionPlan
import com.vyavsa.app.core.model.TenantSubscription
import com.vyavsa.app.core.network.VyavsaApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

interface PlansRepository {
    fun getSubscription(): Flow<Resource<TenantSubscription>>
    fun listAvailablePlans(): Flow<Resource<List<SubscriptionPlan>>>
    fun purchasePlan(planId: String, paymentMethod: String): Flow<Resource<TenantSubscription>>
    fun listBillingTransactions(): Flow<Resource<List<PlanTransaction>>>
}

@Singleton
class PlansRepositoryImpl @Inject constructor(
    private val apiService: VyavsaApiService
) : PlansRepository {

    override fun getSubscription(): Flow<Resource<TenantSubscription>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.getSubscription()
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data))
            } else {
                emit(Resource.Error(response.error?.message ?: response.message ?: "Failed to load subscription details"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Network error"))
        }
    }.flowOn(Dispatchers.IO)

    override fun listAvailablePlans(): Flow<Resource<List<SubscriptionPlan>>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.listAvailablePlans()
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data))
            } else {
                emit(Resource.Error(response.error?.message ?: response.message ?: "Failed to load subscription plans"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Network error"))
        }
    }.flowOn(Dispatchers.IO)

    override fun purchasePlan(planId: String, paymentMethod: String): Flow<Resource<TenantSubscription>> = flow {
        emit(Resource.Loading)
        try {
            val req = PurchasePlanRequest(
                planId = planId,
                paymentMethod = paymentMethod
            )
            val response = apiService.purchasePlan(req)
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data, response.message ?: "Plan upgraded successfully"))
            } else {
                emit(Resource.Error(response.error?.message ?: response.message ?: "Failed to purchase plan"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Network error"))
        }
    }.flowOn(Dispatchers.IO)

    override fun listBillingTransactions(): Flow<Resource<List<PlanTransaction>>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.listSubscriptionTransactions()
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data))
            } else {
                emit(Resource.Error(response.error?.message ?: response.message ?: "Failed to load billing history"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Network error"))
        }
    }.flowOn(Dispatchers.IO)
}
