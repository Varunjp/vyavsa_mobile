package com.vyavsa.app.feature.party

import com.vyavsa.app.core.common.Resource
import com.vyavsa.app.core.model.ApiPagination
import com.vyavsa.app.core.model.Party
import com.vyavsa.app.core.model.PartyDetail
import com.vyavsa.app.core.model.PartyFinancialSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class FakePartyRepository : PartyRepository {

    val parties = mutableListOf<Party>()
    var shouldFail = false
    var failureMessage = "Network error"
    var createdParty: Party? = null
    var updatedParty: Party? = null
    var deletedPartyId: String? = null

    override fun listParties(
        page: Int,
        pageSize: Int,
        role: String?,
        search: String?,
        status: String?
    ): Flow<Resource<List<Party>>> = flow {
        if (shouldFail) {
            emit(Resource.Error(failureMessage))
        } else {
            var filtered = parties.toList()
            if (!role.isNullOrBlank() && role != "all") {
                filtered = filtered.filter { it.role.equals(role, ignoreCase = true) }
            }
            if (!search.isNullOrBlank()) {
                filtered = filtered.filter { it.name.contains(search, ignoreCase = true) || it.phone?.contains(search) == true }
            }
            emit(Resource.Success(filtered))
        }
    }

    override fun listPartiesPaginated(
        page: Int,
        pageSize: Int,
        role: String?,
        search: String?,
        status: String?
    ): Flow<Resource<Pair<List<Party>, ApiPagination?>>> = flow {
        if (shouldFail) {
            emit(Resource.Error(failureMessage))
        } else {
            var filtered = parties.toList()
            if (!role.isNullOrBlank() && role != "all") {
                filtered = filtered.filter { it.role.equals(role, ignoreCase = true) }
            }
            if (!search.isNullOrBlank()) {
                filtered = filtered.filter { it.name.contains(search, ignoreCase = true) || it.phone?.contains(search) == true }
            }
            val pagination = ApiPagination(
                page = page,
                pageSize = pageSize,
                totalItems = filtered.size.toLong(),
                totalPages = 1
            )
            emit(Resource.Success(Pair(filtered, pagination)))
        }
    }

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
        if (shouldFail) {
            emit(Resource.Error(failureMessage))
        } else {
            val party = Party(
                id = "party-${System.currentTimeMillis()}",
                name = name,
                phone = phone,
                email = email,
                address = address,
                role = role,
                isCustomer = role == "customer" || role == "both",
                isSupplier = role == "supplier" || role == "both",
                isBoth = role == "both",
                openingBalance = openingReceivable.toString(),
                openingPayableBalance = openingPayable.toString(),
                totalReceivable = openingReceivable.toString(),
                totalPayable = openingPayable.toString(),
                netBalance = (openingReceivable - openingPayable).toString(),
                createdAt = "2026-11-09T10:00:00Z"
            )
            parties.add(party)
            createdParty = party
            emit(Resource.Success(party))
        }
    }

    override fun getPartyById(id: String): Flow<Resource<Party>> = flow {
        if (shouldFail) {
            emit(Resource.Error(failureMessage))
        } else {
            val party = parties.find { it.id == id }
            if (party != null) {
                emit(Resource.Success(party))
            } else {
                emit(Resource.Error("Party not found"))
            }
        }
    }

    override fun updateParty(
        id: String,
        name: String?,
        phone: String?,
        email: String?,
        address: String?,
        role: String?,
        status: String?
    ): Flow<Resource<Party>> = flow {
        if (shouldFail) {
            emit(Resource.Error(failureMessage))
        } else {
            val index = parties.indexOfFirst { it.id == id }
            if (index != -1) {
                val existing = parties[index]
                val updated = existing.copy(
                    name = name ?: existing.name,
                    phone = phone ?: existing.phone,
                    email = email ?: existing.email,
                    address = address ?: existing.address,
                    role = role ?: existing.role
                )
                parties[index] = updated
                updatedParty = updated
                emit(Resource.Success(updated))
            } else {
                emit(Resource.Error("Party not found"))
            }
        }
    }

    override fun deleteParty(id: String): Flow<Resource<Unit>> = flow {
        if (shouldFail) {
            emit(Resource.Error(failureMessage))
        } else {
            deletedPartyId = id
            parties.removeAll { it.id == id }
            emit(Resource.Success(Unit))
        }
    }

    override fun getPartyDetailedView(id: String): Flow<Resource<PartyDetail>> = flow {
        if (shouldFail) {
            emit(Resource.Error(failureMessage))
        } else {
            val party = parties.find { it.id == id }
            if (party != null) {
                val detail = PartyDetail(
                    id = party.id,
                    name = party.name,
                    phone = party.phone,
                    email = party.email,
                    address = party.address,
                    role = party.role,
                    totalReceivable = party.totalReceivable,
                    totalPayable = party.totalPayable,
                    netBalance = party.netBalance,
                    totalSales = "5000.00",
                    totalPurchases = "2000.00",
                    totalSalesPaid = "4000.00",
                    totalPurchasesPaid = "1000.00"
                )
                emit(Resource.Success(detail))
            } else {
                emit(Resource.Error("Party detail not found"))
            }
        }
    }

    override fun getPartyFinancialSummary(id: String): Flow<Resource<PartyFinancialSummary>> = flow {
        if (shouldFail) {
            emit(Resource.Error(failureMessage))
        } else {
            val party = parties.find { it.id == id }
            if (party != null) {
                val summary = PartyFinancialSummary(
                    partyId = party.id,
                    partyName = party.name,
                    phone = party.phone,
                    email = party.email,
                    address = party.address,
                    role = party.role,
                    totalReceivable = party.totalReceivable,
                    totalPayable = party.totalPayable,
                    netBalance = party.netBalance
                )
                emit(Resource.Success(summary))
            } else {
                emit(Resource.Error("Party financial summary not found"))
            }
        }
    }
}
