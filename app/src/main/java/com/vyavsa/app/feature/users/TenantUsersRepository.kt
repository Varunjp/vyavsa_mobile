package com.vyavsa.app.feature.users

import com.vyavsa.app.core.common.Resource
import com.vyavsa.app.core.model.CreateTenantUserRequest
import com.vyavsa.app.core.model.TenantUser
import com.vyavsa.app.core.model.UpdateTenantUserRequest
import com.vyavsa.app.core.network.VyavsaApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

interface TenantUsersRepository {
    fun listUsers(search: String? = null, role: String? = null, status: String? = null): Flow<Resource<List<TenantUser>>>
    fun createUser(req: CreateTenantUserRequest): Flow<Resource<TenantUser>>
    fun getUserById(id: String): Flow<Resource<TenantUser>>
    fun updateUser(id: String, req: UpdateTenantUserRequest): Flow<Resource<TenantUser>>
    fun deleteUser(id: String): Flow<Resource<Unit>>
}

@Singleton
class TenantUsersRepositoryImpl @Inject constructor(
    private val apiService: VyavsaApiService
) : TenantUsersRepository {

    override fun listUsers(search: String?, role: String?, status: String?): Flow<Resource<List<TenantUser>>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.listTenantUsers(
                page = 1,
                pageSize = 100,
                search = search?.ifBlank { null },
                role = role?.ifBlank { null },
                status = status?.ifBlank { null }
            )
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data))
            } else {
                emit(Resource.Error(response.error?.message ?: response.message ?: "Failed to list tenant users"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Network error"))
        }
    }.flowOn(Dispatchers.IO)

    override fun createUser(req: CreateTenantUserRequest): Flow<Resource<TenantUser>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.createTenantUser(req)
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data, response.message ?: "User created successfully"))
            } else {
                emit(Resource.Error(response.error?.message ?: response.message ?: "Failed to create user"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Network error"))
        }
    }.flowOn(Dispatchers.IO)

    override fun getUserById(id: String): Flow<Resource<TenantUser>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.getTenantUserById(id)
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data))
            } else {
                emit(Resource.Error(response.error?.message ?: response.message ?: "Failed to load user details"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Network error"))
        }
    }.flowOn(Dispatchers.IO)

    override fun updateUser(id: String, req: UpdateTenantUserRequest): Flow<Resource<TenantUser>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.updateTenantUser(id, req)
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data, response.message ?: "User updated successfully"))
            } else {
                emit(Resource.Error(response.error?.message ?: response.message ?: "Failed to update user"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Network error"))
        }
    }.flowOn(Dispatchers.IO)

    override fun deleteUser(id: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.deleteTenantUser(id)
            if (response.success) {
                emit(Resource.Success(Unit, response.message ?: "User deleted successfully"))
            } else {
                emit(Resource.Error(response.error?.message ?: response.message ?: "Failed to delete user"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Network error"))
        }
    }.flowOn(Dispatchers.IO)
}
