package com.vyavsa.app.feature.party

import com.vyavsa.app.core.common.Resource
import com.vyavsa.app.core.model.ApiPagination
import com.vyavsa.app.core.model.CreatePartyRequest
import com.vyavsa.app.core.model.Party
import com.vyavsa.app.core.model.PartyDetail
import com.vyavsa.app.core.model.PartyFinancialSummary
import com.vyavsa.app.core.model.UpdatePartyRequest
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

interface PartyRepository {
    fun listParties(
        page: Int = 1,
        pageSize: Int = 20,
        role: String? = "all",
        search: String? = null,
        status: String? = null
    ): Flow<Resource<List<Party>>>

    fun listPartiesPaginated(
        page: Int = 1,
        pageSize: Int = 20,
        role: String? = "all",
        search: String? = null,
        status: String? = null
    ): Flow<Resource<Pair<List<Party>, ApiPagination?>>>

    fun createParty(
        name: String,
        phone: String? = null,
        email: String? = null,
        address: String? = null,
        role: String = "both",
        openingReceivable: Double = 0.0,
        openingPayable: Double = 0.0,
        status: String = "active"
    ): Flow<Resource<Party>>

    fun getPartyById(id: String): Flow<Resource<Party>>

    fun updateParty(
        id: String,
        name: String? = null,
        phone: String? = null,
        email: String? = null,
        address: String? = null,
        role: String? = null,
        status: String? = null
    ): Flow<Resource<Party>>

    fun deleteParty(id: String): Flow<Resource<Unit>>

    fun getPartyDetailedView(id: String): Flow<Resource<PartyDetail>>

    fun getPartyFinancialSummary(id: String): Flow<Resource<PartyFinancialSummary>>
}

@Singleton
class PartyRepositoryImpl @Inject constructor(
    private val apiService: VyavsaApiService,
    private val refreshCoordinator: DataRefreshCoordinator? = null
) : PartyRepository {

    override fun listParties(
        page: Int,
        pageSize: Int,
        role: String?,
        search: String?,
        status: String?
    ): Flow<Resource<List<Party>>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.listParties(
                page = page,
                pageSize = pageSize,
                role = role?.ifBlank { "all" } ?: "all",
                search = search?.trim()?.ifBlank { null },
                status = status?.trim()?.ifBlank { null }
            )
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data))
            } else {
                emit(ApiErrorParser.fromApiResponse(response, "Failed to load business parties"))
            }
        } catch (e: Throwable) {
            emit(ApiErrorParser.parse(e, "Network error loading business parties"))
        }
    }.flowOn(Dispatchers.IO)

    override fun listPartiesPaginated(
        page: Int,
        pageSize: Int,
        role: String?,
        search: String?,
        status: String?
    ): Flow<Resource<Pair<List<Party>, ApiPagination?>>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.listParties(
                page = page,
                pageSize = pageSize,
                role = role?.ifBlank { "all" } ?: "all",
                search = search?.trim()?.ifBlank { null },
                status = status?.trim()?.ifBlank { null }
            )
            if (response.success && response.data != null) {
                emit(Resource.Success(Pair(response.data, response.pagination)))
            } else {
                emit(ApiErrorParser.fromApiResponse(response, "Failed to load business parties"))
            }
        } catch (e: Throwable) {
            emit(ApiErrorParser.parse(e, "Network error loading business parties"))
        }
    }.flowOn(Dispatchers.IO)

    override fun createParty(
        name: String,
        phone: String?,
        email: String?,
        address: String?,
        role: String,
        openingReceivable: Double,
        openingPayable: Double,
        status: String
    ): Flow<Resource<Party>> = flow {
        emit(Resource.Loading)
        try {
            val req = CreatePartyRequest(
                name = name.trim(),
                phone = phone?.trim()?.ifBlank { null },
                email = email?.trim()?.ifBlank { null },
                address = address?.trim()?.ifBlank { null },
                role = role.trim().ifBlank { "both" },
                openingBalance = if (openingReceivable > 0.0) openingReceivable else null,
                openingPayableBalance = if (openingPayable > 0.0) openingPayable else null,
                status = status.trim().ifBlank { "active" }
            )
            val response = apiService.createParty(req)
            if (response.success && response.data != null) {
                refreshCoordinator?.notifyMutation(DataMutation.DUES)
                refreshCoordinator?.notifyMutation(DataMutation.PURCHASES)
                emit(Resource.Success(response.data, response.message))
            } else {
                emit(ApiErrorParser.fromApiResponse(response, "Failed to register business party"))
            }
        } catch (e: Throwable) {
            emit(ApiErrorParser.parse(e, "Network error registering party"))
        }
    }.flowOn(Dispatchers.IO)

    override fun getPartyById(id: String): Flow<Resource<Party>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.getPartyById(id)
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data))
            } else {
                emit(ApiErrorParser.fromApiResponse(response, "Failed to fetch party"))
            }
        } catch (e: Throwable) {
            emit(ApiErrorParser.parse(e, "Network error fetching party"))
        }
    }.flowOn(Dispatchers.IO)

    override fun updateParty(
        id: String,
        name: String?,
        phone: String?,
        email: String?,
        address: String?,
        role: String?,
        status: String?
    ): Flow<Resource<Party>> = flow {
        emit(Resource.Loading)
        try {
            val req = UpdatePartyRequest(
                name = name?.trim()?.ifBlank { null },
                phone = phone?.trim()?.ifBlank { null },
                email = email?.trim()?.ifBlank { null },
                address = address?.trim()?.ifBlank { null },
                role = role?.trim()?.ifBlank { null },
                status = status?.trim()?.ifBlank { null }
            )
            val response = apiService.updateParty(id, req)
            if (response.success && response.data != null) {
                refreshCoordinator?.notifyMutation(DataMutation.DUES)
                refreshCoordinator?.notifyMutation(DataMutation.PURCHASES)
                emit(Resource.Success(response.data, response.message))
            } else {
                emit(ApiErrorParser.fromApiResponse(response, "Failed to update business party"))
            }
        } catch (e: Throwable) {
            emit(ApiErrorParser.parse(e, "Network error updating party"))
        }
    }.flowOn(Dispatchers.IO)

    override fun deleteParty(id: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.deleteParty(id)
            if (response.success) {
                refreshCoordinator?.notifyMutation(DataMutation.DUES)
                refreshCoordinator?.notifyMutation(DataMutation.PURCHASES)
                emit(Resource.Success(Unit, response.message))
            } else {
                emit(ApiErrorParser.fromApiResponse(response, "Failed to delete business party"))
            }
        } catch (e: Throwable) {
            emit(ApiErrorParser.parse(e, "Network error deleting party"))
        }
    }.flowOn(Dispatchers.IO)

    override fun getPartyDetailedView(id: String): Flow<Resource<PartyDetail>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.getPartyDetailedView(id)
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data))
            } else {
                emit(ApiErrorParser.fromApiResponse(response, "Failed to load party profile details"))
            }
        } catch (e: Throwable) {
            emit(ApiErrorParser.parse(e, "Network error loading party details"))
        }
    }.flowOn(Dispatchers.IO)

    override fun getPartyFinancialSummary(id: String): Flow<Resource<PartyFinancialSummary>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.getPartyFinancialSummary(id)
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data))
            } else {
                emit(ApiErrorParser.fromApiResponse(response, "Failed to load party financial summary"))
            }
        } catch (e: Throwable) {
            emit(ApiErrorParser.parse(e, "Network error loading party financial summary"))
        }
    }.flowOn(Dispatchers.IO)
}
