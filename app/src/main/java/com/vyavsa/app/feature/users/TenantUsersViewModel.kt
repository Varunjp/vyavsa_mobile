package com.vyavsa.app.feature.users

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vyavsa.app.core.common.Resource
import com.vyavsa.app.core.common.UiState
import com.vyavsa.app.core.model.CreateTenantUserRequest
import com.vyavsa.app.core.model.TenantUser
import com.vyavsa.app.core.model.UpdateTenantUserRequest
import com.vyavsa.app.core.security.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TenantUsersViewModel @Inject constructor(
    private val repository: TenantUsersRepository,
    val tokenManager: TokenManager? = null
) : ViewModel() {

    var adminOverrideForTesting: Boolean? = null

    private val _usersState = MutableStateFlow<UiState<List<TenantUser>>>(UiState.Loading)
    val usersState: StateFlow<UiState<List<TenantUser>>> = _usersState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _actionState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val actionState: StateFlow<UiState<Unit>> = _actionState.asStateFlow()

    private var searchJob: Job? = null

    val isAdmin: Boolean
        get() = adminOverrideForTesting ?: (tokenManager?.isTenantAdmin() ?: false)

    init {
        loadUsers()
    }

    fun onSearchChange(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(200L)
            loadUsers()
        }
    }

    fun loadUsers() {
        if (!isAdmin) {
            _usersState.value = UiState.Error("Access Denied: Only administrators can view tenant user accounts")
            return
        }

        val query = _searchQuery.value.trim().ifBlank { null }
        viewModelScope.launch {
            repository.listUsers(search = query).collect { res ->
                _usersState.value = when (res) {
                    is Resource.Loading -> if (_usersState.value is UiState.Success) _usersState.value else UiState.Loading
                    is Resource.Success -> if (res.data.isEmpty()) UiState.Empty else UiState.Success(res.data)
                    is Resource.Error -> UiState.Error(res.message)
                }
            }
        }
    }

    fun createUser(name: String, email: String, password: String, role: String) {
        if (!isAdmin) {
            _actionState.value = UiState.Error("Access Denied: Administrator role required")
            return
        }

        if (name.isBlank() || email.isBlank() || password.isBlank()) {
            _actionState.value = UiState.Error("Full name, email, and password are required")
            return
        }

        if (password.length < 6) {
            _actionState.value = UiState.Error("Password must be at least 6 characters")
            return
        }

        viewModelScope.launch {
            val req = CreateTenantUserRequest(
                name = name.trim(),
                email = email.trim(),
                password = password,
                role = role
            )
            repository.createUser(req).collect { res ->
                _actionState.value = when (res) {
                    is Resource.Loading -> UiState.Loading
                    is Resource.Success -> {
                        loadUsers()
                        UiState.Success(Unit)
                    }
                    is Resource.Error -> UiState.Error(res.message)
                }
            }
        }
    }

    fun updateUser(id: String, name: String?, role: String?, status: String?, password: String?) {
        if (!isAdmin) {
            _actionState.value = UiState.Error("Access Denied: Administrator role required")
            return
        }

        viewModelScope.launch {
            val req = UpdateTenantUserRequest(
                name = name?.trim()?.ifBlank { null },
                role = role?.ifBlank { null },
                status = status?.ifBlank { null },
                password = password?.ifBlank { null }
            )
            repository.updateUser(id, req).collect { res ->
                _actionState.value = when (res) {
                    is Resource.Loading -> UiState.Loading
                    is Resource.Success -> {
                        loadUsers()
                        UiState.Success(Unit)
                    }
                    is Resource.Error -> UiState.Error(res.message)
                }
            }
        }
    }

    fun deleteUser(id: String) {
        if (!isAdmin) {
            _actionState.value = UiState.Error("Access Denied: Administrator role required")
            return
        }

        viewModelScope.launch {
            repository.deleteUser(id).collect { res ->
                _actionState.value = when (res) {
                    is Resource.Loading -> UiState.Loading
                    is Resource.Success -> {
                        loadUsers()
                        UiState.Success(Unit)
                    }
                    is Resource.Error -> UiState.Error(res.message)
                }
            }
        }
    }

    fun resetActionState() {
        _actionState.value = UiState.Idle
    }
}
