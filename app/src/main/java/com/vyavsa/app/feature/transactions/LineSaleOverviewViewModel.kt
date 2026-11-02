package com.vyavsa.app.feature.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vyavsa.app.core.common.Resource
import com.vyavsa.app.core.common.UiState
import com.vyavsa.app.core.model.LineSalePaymentBreakdown
import com.vyavsa.app.core.sync.DataMutation
import com.vyavsa.app.core.sync.DataRefreshCoordinator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Lifecycle-aware ViewModel managing today's Line Sale payment breakdown overview.
 * Automatically synchronizes on [DataMutation.SALES] mutations and prevents duplicate network requests.
 */
@HiltViewModel
class LineSaleOverviewViewModel @Inject constructor(
    private val repository: TransactionsRepository,
    private val refreshCoordinator: DataRefreshCoordinator
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<LineSalePaymentBreakdown>>(UiState.Loading)
    val state: StateFlow<UiState<LineSalePaymentBreakdown>> = _state.asStateFlow()

    private var loadJob: Job? = null

    init {
        loadBreakdown()
        observeDataMutations()
    }

    private fun observeDataMutations() {
        viewModelScope.launch {
            refreshCoordinator.refreshEvents.collectLatest { mutation ->
                if (mutation == DataMutation.SALES) {
                    loadBreakdown(force = true)
                }
            }
        }
    }

    fun loadBreakdown(force: Boolean = false) {
        if (!force && loadJob?.isActive == true) return
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            repository.getLineSalePaymentBreakdown(date = "today").collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        if (_state.value !is UiState.Success || force) {
                            _state.value = UiState.Loading
                        }
                    }
                    is Resource.Success -> {
                        _state.value = UiState.Success(resource.data)
                    }
                    is Resource.Error -> {
                        _state.value = UiState.Error(
                            message = resource.message,
                            code = resource.code
                        )
                    }
                }
            }
        }
    }

    fun refresh() {
        loadBreakdown(force = true)
    }
}
