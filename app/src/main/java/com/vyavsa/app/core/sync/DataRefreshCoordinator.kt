package com.vyavsa.app.core.sync

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class DataMutation {
    SALES,
    PURCHASES,
    BANK,
    CASH,
    EXPENSES,
    DUES,
    STAFF,
    ALL
}

@Singleton
class DataRefreshCoordinator @Inject constructor() {

    private val _refreshEvents = MutableSharedFlow<DataMutation>(extraBufferCapacity = 64)
    val refreshEvents: SharedFlow<DataMutation> = _refreshEvents.asSharedFlow()

    fun notifyMutation(mutation: DataMutation) {
        _refreshEvents.tryEmit(mutation)
    }
}
