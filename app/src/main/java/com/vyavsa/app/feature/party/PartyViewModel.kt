package com.vyavsa.app.feature.party

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vyavsa.app.core.common.MessageType
import com.vyavsa.app.core.common.OneTimeEventEmitter
import com.vyavsa.app.core.common.Resource
import com.vyavsa.app.core.common.UiMessage
import com.vyavsa.app.core.common.UiState
import com.vyavsa.app.core.model.ApiPagination
import com.vyavsa.app.core.model.Party
import com.vyavsa.app.core.model.PartyDetail
import com.vyavsa.app.core.model.PartyFinancialSummary
import com.vyavsa.app.core.security.AppPermission
import com.vyavsa.app.core.security.TokenManager
import com.vyavsa.app.core.sync.DataMutation
import com.vyavsa.app.core.sync.DataRefreshCoordinator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PartyViewModel @Inject constructor(
    private val repository: PartyRepository,
    private val refreshCoordinator: DataRefreshCoordinator,
    val tokenManager: TokenManager? = null
) : ViewModel() {

    private val _rawParties = MutableStateFlow<List<Party>>(emptyList())

    private val _partiesState = MutableStateFlow<UiState<List<Party>>>(UiState.Loading)
    val partiesState: StateFlow<UiState<List<Party>>> = _partiesState.asStateFlow()

    private val _paginationState = MutableStateFlow<ApiPagination?>(null)
    val paginationState: StateFlow<ApiPagination?> = _paginationState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedRoleFilter = MutableStateFlow("all")
    val selectedRoleFilter: StateFlow<String> = _selectedRoleFilter.asStateFlow()

    private val _selectedBalanceFilter = MutableStateFlow("all")
    val selectedBalanceFilter: StateFlow<String> = _selectedBalanceFilter.asStateFlow()

    private val _currentPage = MutableStateFlow(1)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()

    private val _selectedPartyDetail = MutableStateFlow<UiState<PartyDetail>>(UiState.Idle)
    val selectedPartyDetail: StateFlow<UiState<PartyDetail>> = _selectedPartyDetail.asStateFlow()

    private val _selectedPartyFinancialSummary = MutableStateFlow<UiState<PartyFinancialSummary>>(UiState.Idle)
    val selectedPartyFinancialSummary: StateFlow<UiState<PartyFinancialSummary>> = _selectedPartyFinancialSummary.asStateFlow()

    private val _actionState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val actionState: StateFlow<UiState<Unit>> = _actionState.asStateFlow()

    private val eventEmitter = OneTimeEventEmitter()
    val uiEvents: Flow<UiMessage> = eventEmitter.events

    private val emailRegex = "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}".toRegex()

    private var searchJob: Job? = null

    var adminOverrideForTesting: Boolean? = null

    val isAdmin: Boolean
        get() = adminOverrideForTesting ?: (tokenManager?.isTenantAdmin() ?: false)

    val canCreateParty: Boolean
        get() = tokenManager?.canCreateParties() ?: isAdmin

    val canUpdateParty: Boolean
        get() = tokenManager?.canUpdateParties() ?: isAdmin

    val canDeleteParty: Boolean
        get() = tokenManager?.canDeleteParties() ?: isAdmin

    init {
        loadParties(page = 1)
        observeRefreshEvents()
    }

    private fun observeRefreshEvents() {
        viewModelScope.launch {
            refreshCoordinator.refreshEvents.collect { mutation ->
                when (mutation) {
                    DataMutation.DUES,
                    DataMutation.PURCHASES,
                    DataMutation.ALL -> {
                        loadParties(page = _currentPage.value, isBackgroundRefresh = true)
                    }
                    else -> Unit
                }
            }
        }
    }

    fun onSearchChange(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300)
            loadParties(page = 1)
        }
    }

    fun setSearchQuery(query: String) = onSearchChange(query)

    fun selectRoleFilter(role: String) {
        if (_selectedRoleFilter.value == role) return
        _selectedRoleFilter.value = role
        loadParties(page = 1)
    }

    fun setRoleFilter(role: String) = selectRoleFilter(role)

    fun selectBalanceFilter(balance: String) {
        _selectedBalanceFilter.value = balance
        applyBalanceFilter()
    }

    fun setBalanceFilter(balance: String) = selectBalanceFilter(balance)

    fun loadParties(page: Int = 1, isBackgroundRefresh: Boolean = false) {
        _currentPage.value = page
        viewModelScope.launch {
            if (!isBackgroundRefresh) {
                _partiesState.value = UiState.Loading
            }
            repository.listPartiesPaginated(
                page = page,
                pageSize = 20,
                role = _selectedRoleFilter.value,
                search = _searchQuery.value,
                status = null
            ).collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        if (!isBackgroundRefresh) {
                            _partiesState.value = UiState.Loading
                        }
                    }
                    is Resource.Success -> {
                        val (parties, pagination) = resource.data
                        _rawParties.value = parties
                        _paginationState.value = pagination
                        applyBalanceFilter()
                    }
                    is Resource.Error -> {
                        _partiesState.value = UiState.Error(resource.message)
                    }
                }
            }
        }
    }

    private fun applyBalanceFilter() {
        val raw = _rawParties.value
        val filter = _selectedBalanceFilter.value
        val filtered = when (filter) {
            "dues" -> raw.filter { p ->
                val recv = p.totalReceivable.toDoubleOrNull() ?: 0.0
                val pay = p.totalPayable.toDoubleOrNull() ?: 0.0
                recv > 0.009 || pay > 0.009
            }
            "settleable" -> raw.filter { p ->
                val recv = p.totalReceivable.toDoubleOrNull() ?: 0.0
                val pay = p.totalPayable.toDoubleOrNull() ?: 0.0
                recv > 0.009 && pay > 0.009
            }
            else -> raw
        }

        _partiesState.value = if (filtered.isEmpty()) {
            UiState.Empty
        } else {
            UiState.Success(filtered)
        }
    }

    fun nextPage() {
        val current = _paginationState.value ?: return
        if (current.page < current.totalPages) {
            loadParties(current.page + 1)
        }
    }

    fun previousPage() {
        val current = _paginationState.value ?: return
        if (current.page > 1) {
            loadParties(current.page - 1)
        }
    }

    fun loadPartyDetails(partyId: String) {
        if (partyId.isBlank()) return
        viewModelScope.launch {
            _selectedPartyDetail.value = UiState.Loading
            repository.getPartyDetailedView(partyId).collect { resource ->
                _selectedPartyDetail.value = when (resource) {
                    is Resource.Loading -> UiState.Loading
                    is Resource.Success -> UiState.Success(resource.data)
                    is Resource.Error -> UiState.Error(resource.message)
                }
            }
        }
    }

    fun loadPartyFinancialSummary(partyId: String) {
        if (partyId.isBlank()) return
        viewModelScope.launch {
            _selectedPartyFinancialSummary.value = UiState.Loading
            repository.getPartyFinancialSummary(partyId).collect { resource ->
                _selectedPartyFinancialSummary.value = when (resource) {
                    is Resource.Loading -> UiState.Loading
                    is Resource.Success -> UiState.Success(resource.data)
                    is Resource.Error -> UiState.Error(resource.message)
                }
            }
        }
    }

    fun createParty(
        name: String,
        phone: String? = null,
        email: String? = null,
        address: String? = null,
        role: String = "both",
        openingReceivable: Double = 0.0,
        openingPayable: Double = 0.0,
        onSuccess: (() -> Unit)? = null
    ) {
        if (_actionState.value is UiState.Loading) return

        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) {
            _actionState.value = UiState.Error("Party name is required")
            eventEmitter.emit("Party name is required", MessageType.ERROR)
            return
        }
        if (trimmedName.length < 2) {
            _actionState.value = UiState.Error("Party name must be at least 2 characters")
            eventEmitter.emit("Party name must be at least 2 characters", MessageType.ERROR)
            return
        }

        val trimmedPhone = phone?.trim()?.ifBlank { null }
        if (trimmedPhone != null && (trimmedPhone.length < 10 || !trimmedPhone.all { it.isDigit() || it == '+' })) {
            _actionState.value = UiState.Error("Phone number must be at least 10 digits")
            eventEmitter.emit("Phone number must be at least 10 digits", MessageType.ERROR)
            return
        }

        val trimmedEmail = email?.trim()?.ifBlank { null }
        if (trimmedEmail != null && !emailRegex.matches(trimmedEmail)) {
            _actionState.value = UiState.Error("Please enter a valid email address")
            eventEmitter.emit("Please enter a valid email address", MessageType.ERROR)
            return
        }

        if (openingReceivable < 0.0 || openingPayable < 0.0) {
            _actionState.value = UiState.Error("Opening balances cannot be negative")
            eventEmitter.emit("Opening balances cannot be negative", MessageType.ERROR)
            return
        }

        viewModelScope.launch {
            _actionState.value = UiState.Loading
            repository.createParty(
                name = trimmedName,
                phone = trimmedPhone,
                email = trimmedEmail,
                address = address?.trim()?.ifBlank { null },
                role = role,
                openingReceivable = openingReceivable,
                openingPayable = openingPayable,
                status = "active"
            ).collect { resource ->
                when (resource) {
                    is Resource.Loading -> _actionState.value = UiState.Loading
                    is Resource.Success -> {
                        _actionState.value = UiState.Success(Unit)
                        eventEmitter.emit("Business party '${resource.data.displayName}' created successfully", MessageType.SUCCESS)
                        loadParties(page = 1)
                        onSuccess?.invoke()
                    }
                    is Resource.Error -> {
                        _actionState.value = UiState.Error(resource.message)
                        eventEmitter.emit(resource.message, MessageType.ERROR)
                    }
                }
            }
        }
    }

    fun updateParty(
        id: String,
        name: String? = null,
        phone: String? = null,
        email: String? = null,
        address: String? = null,
        role: String? = null,
        status: String? = null,
        onSuccess: (() -> Unit)? = null
    ) {
        if (_actionState.value is UiState.Loading) return

        val trimmedName = name?.trim()
        if (trimmedName != null && trimmedName.length < 2) {
            _actionState.value = UiState.Error("Party name must be at least 2 characters")
            eventEmitter.emit("Party name must be at least 2 characters", MessageType.ERROR)
            return
        }

        val trimmedEmail = email?.trim()?.ifBlank { null }
        if (trimmedEmail != null && !emailRegex.matches(trimmedEmail)) {
            _actionState.value = UiState.Error("Please enter a valid email address")
            eventEmitter.emit("Please enter a valid email address", MessageType.ERROR)
            return
        }

        viewModelScope.launch {
            _actionState.value = UiState.Loading
            repository.updateParty(
                id = id,
                name = trimmedName,
                phone = phone?.trim()?.ifBlank { null },
                email = trimmedEmail,
                address = address?.trim()?.ifBlank { null },
                role = role?.trim(),
                status = status?.trim()
            ).collect { resource ->
                when (resource) {
                    is Resource.Loading -> _actionState.value = UiState.Loading
                    is Resource.Success -> {
                        _actionState.value = UiState.Success(Unit)
                        eventEmitter.emit("Business party updated successfully", MessageType.SUCCESS)
                        loadParties(page = _currentPage.value)
                        loadPartyDetails(id)
                        onSuccess?.invoke()
                    }
                    is Resource.Error -> {
                        _actionState.value = UiState.Error(resource.message)
                        eventEmitter.emit(resource.message, MessageType.ERROR)
                    }
                }
            }
        }
    }

    fun deleteParty(id: String, onSuccess: (() -> Unit)? = null) {
        if (_actionState.value is UiState.Loading) return
        viewModelScope.launch {
            _actionState.value = UiState.Loading
            repository.deleteParty(id).collect { resource ->
                when (resource) {
                    is Resource.Loading -> _actionState.value = UiState.Loading
                    is Resource.Success -> {
                        _actionState.value = UiState.Success(Unit)
                        eventEmitter.emit("Business party deleted successfully", MessageType.SUCCESS)
                        loadParties(page = 1)
                        onSuccess?.invoke()
                    }
                    is Resource.Error -> {
                        _actionState.value = UiState.Error(resource.message)
                        eventEmitter.emit(resource.message, MessageType.ERROR)
                    }
                }
            }
        }
    }

    fun resetActionState() {
        _actionState.value = UiState.Idle
    }
}
