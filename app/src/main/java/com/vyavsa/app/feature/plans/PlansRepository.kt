package com.vyavsa.app.feature.plans

import com.vyavsa.app.core.common.Resource
import com.vyavsa.app.core.model.InitiateCheckoutRequest
import com.vyavsa.app.core.model.InitiateCheckoutResponse
import com.vyavsa.app.core.model.PlanTransaction
import com.vyavsa.app.core.model.PurchasePlanRequest
import com.vyavsa.app.core.model.SubscriptionPlan
import com.vyavsa.app.core.model.TenantSubscription
import com.vyavsa.app.core.model.VerifyPaymentRequest
import com.vyavsa.app.core.model.VerifyPaymentResponse
import com.vyavsa.app.core.network.ApiErrorParser
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
    fun initiateCheckout(planId: String, activationMode: String = "instant"): Flow<Resource<InitiateCheckoutResponse>>
    fun verifyPayment(
        planId: String,
        paymentId: String,
        subscriptionId: String,
        signature: String,
        activationMode: String = "instant"
    ): Flow<Resource<VerifyPaymentResponse>>
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
                emit(ApiErrorParser.fromApiResponse(response, "Failed to load subscription details"))
            }
        } catch (e: Throwable) {
            emit(ApiErrorParser.parse(e, "Network error loading subscription"))
        }
    }.flowOn(Dispatchers.IO)

    override fun listAvailablePlans(): Flow<Resource<List<SubscriptionPlan>>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.listAvailablePlans()
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data))
            } else {
                emit(ApiErrorParser.fromApiResponse(response, "Failed to load subscription plans"))
            }
        } catch (e: Throwable) {
            emit(ApiErrorParser.parse(e, "Network error loading plans"))
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
                emit(ApiErrorParser.fromApiResponse(response, "Failed to purchase plan"))
            }
        } catch (e: Throwable) {
            emit(ApiErrorParser.parse(e, "Network error purchasing plan"))
        }
    }.flowOn(Dispatchers.IO)

    override fun initiateCheckout(planId: String, activationMode: String): Flow<Resource<InitiateCheckoutResponse>> = flow {
        emit(Resource.Loading)
        try {
            val req = InitiateCheckoutRequest(planId = planId, activationMode = activationMode)
            val response = apiService.initiateSubscriptionCheckout(req)
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data, response.message))
            } else {
                emit(ApiErrorParser.fromApiResponse(response, "Failed to initiate checkout session"))
            }
        } catch (e: Throwable) {
            emit(ApiErrorParser.parse(e, "Network error starting checkout"))
        }
    }.flowOn(Dispatchers.IO)

    override fun verifyPayment(
        planId: String,
        paymentId: String,
        subscriptionId: String,
        signature: String,
        activationMode: String
    ): Flow<Resource<VerifyPaymentResponse>> = flow {
        emit(Resource.Loading)
        try {
            val req = VerifyPaymentRequest(
                planId = planId,
                razorpayPaymentId = paymentId,
                razorpaySubscriptionId = subscriptionId,
                razorpaySignature = signature,
                activationMode = activationMode
            )
            val response = apiService.verifySubscriptionPayment(req)
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data, response.message))
            } else {
                emit(ApiErrorParser.fromApiResponse(response, "Failed to verify subscription payment"))
            }
        } catch (e: Throwable) {
            emit(ApiErrorParser.parse(e, "Network error verifying payment"))
        }
    }.flowOn(Dispatchers.IO)

    override fun listBillingTransactions(): Flow<Resource<List<PlanTransaction>>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.listSubscriptionTransactions()
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data))
            } else {
                emit(ApiErrorParser.fromApiResponse(response, "Failed to load billing history"))
            }
        } catch (e: Throwable) {
            emit(ApiErrorParser.parse(e, "Network error loading billing history"))
        }
    }.flowOn(Dispatchers.IO)
}
