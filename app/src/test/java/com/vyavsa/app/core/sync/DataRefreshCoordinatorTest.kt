package com.vyavsa.app.core.sync

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DataRefreshCoordinatorTest {

    @Test
    fun testNotifyDataChangedEmitsToSubscribers() = runTest {
        val coordinator = DataRefreshCoordinator()
        val received = mutableListOf<DataMutation>()

        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            coordinator.refreshEvents.collect { mutation ->
                received.add(mutation)
            }
        }

        coordinator.notifyMutation(DataMutation.BANK)
        coordinator.notifyMutation(DataMutation.SALES)
        coordinator.notifyMutation(DataMutation.CASH)
        coordinator.notifyMutation(DataMutation.ALL)

        assertEquals(listOf(DataMutation.BANK, DataMutation.SALES, DataMutation.CASH, DataMutation.ALL), received)
        job.cancel()
    }

    @Test
    fun testNotifyMultipleSubscribersConcurrently() = runTest {
        val coordinator = DataRefreshCoordinator()
        val sub1 = mutableListOf<DataMutation>()
        val sub2 = mutableListOf<DataMutation>()

        val job1 = launch(UnconfinedTestDispatcher(testScheduler)) {
            coordinator.refreshEvents.collect { sub1.add(it) }
        }
        val job2 = launch(UnconfinedTestDispatcher(testScheduler)) {
            coordinator.refreshEvents.collect { sub2.add(it) }
        }

        coordinator.notifyMutation(DataMutation.BANK)

        assertEquals(listOf(DataMutation.BANK), sub1)
        assertEquals(listOf(DataMutation.BANK), sub2)

        job1.cancel()
        job2.cancel()
    }
}
