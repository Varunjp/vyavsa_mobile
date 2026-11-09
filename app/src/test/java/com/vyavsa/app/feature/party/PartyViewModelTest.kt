package com.vyavsa.app.feature.party

import com.vyavsa.app.core.common.UiState
import com.vyavsa.app.core.model.Party
import com.vyavsa.app.core.sync.DataMutation
import com.vyavsa.app.core.sync.DataRefreshCoordinator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PartyViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakePartyRepository
    private lateinit var refreshCoordinator: DataRefreshCoordinator
    private lateinit var viewModel: PartyViewModel

    private val sampleParty1 = Party(
        id = "p-1",
        name = "Alpha Stores",
        phone = "9876543210",
        email = "alpha@test.com",
        address = "12 Main Street",
        role = "customer",
        isCustomer = true,
        isSupplier = false,
        isBoth = false,
        totalReceivable = "1500.00",
        totalPayable = "0.00",
        netBalance = "1500.00"
    )

    private val sampleParty2 = Party(
        id = "p-2",
        name = "Beta Wholesalers",
        phone = "9123456780",
        email = "beta@test.com",
        address = "45 Wholesale Market",
        role = "supplier",
        isCustomer = false,
        isSupplier = true,
        isBoth = false,
        totalReceivable = "0.00",
        totalPayable = "3000.00",
        netBalance = "-3000.00"
    )

    private val sampleParty3 = Party(
        id = "p-3",
        name = "Gamma Enterprises",
        phone = "9988776655",
        email = "gamma@test.com",
        address = "88 Industrial Area",
        role = "both",
        isCustomer = true,
        isSupplier = true,
        isBoth = true,
        totalReceivable = "4000.00",
        totalPayable = "1500.00",
        netBalance = "2500.00"
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakePartyRepository()
        refreshCoordinator = DataRefreshCoordinator()

        fakeRepository.parties.addAll(listOf(sampleParty1, sampleParty2, sampleParty3))

        viewModel = PartyViewModel(
            repository = fakeRepository,
            refreshCoordinator = refreshCoordinator,
            tokenManager = null
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialLoadParties_success() = runTest {
        advanceUntilIdle()
        val state = viewModel.partiesState.value
        assertTrue("Expected Success state", state is UiState.Success)
        val list = (state as UiState.Success).data
        assertEquals(3, list.size)
        assertEquals("Alpha Stores", list[0].name)
    }

    @Test
    fun testLoadParties_failure() = runTest {
        fakeRepository.shouldFail = true
        fakeRepository.failureMessage = "Server unreachable"

        viewModel.loadParties()
        advanceUntilIdle()

        val state = viewModel.partiesState.value
        assertTrue("Expected Error state", state is UiState.Error)
        assertEquals("Server unreachable", (state as UiState.Error).message)
    }

    @Test
    fun testRoleFilter_customer() = runTest {
        advanceUntilIdle()
        viewModel.setRoleFilter("customer")
        advanceUntilIdle()

        assertEquals("customer", viewModel.selectedRoleFilter.value)
        val state = viewModel.partiesState.value
        assertTrue(state is UiState.Success)
        val list = (state as UiState.Success).data
        assertEquals(1, list.size)
        assertEquals("Alpha Stores", list[0].name)
    }

    @Test
    fun testRoleFilter_supplier() = runTest {
        advanceUntilIdle()
        viewModel.setRoleFilter("supplier")
        advanceUntilIdle()

        assertEquals("supplier", viewModel.selectedRoleFilter.value)
        val state = viewModel.partiesState.value
        assertTrue(state is UiState.Success)
        val list = (state as UiState.Success).data
        assertEquals(1, list.size)
        assertEquals("Beta Wholesalers", list[0].name)
    }

    @Test
    fun testRoleFilter_both() = runTest {
        advanceUntilIdle()
        viewModel.setRoleFilter("both")
        advanceUntilIdle()

        assertEquals("both", viewModel.selectedRoleFilter.value)
        val state = viewModel.partiesState.value
        assertTrue(state is UiState.Success)
        val list = (state as UiState.Success).data
        assertEquals(1, list.size)
        assertEquals("Gamma Enterprises", list[0].name)
    }

    @Test
    fun testBalanceFilter_dues() = runTest {
        advanceUntilIdle()
        viewModel.setBalanceFilter("dues")
        advanceUntilIdle()

        val state = viewModel.partiesState.value
        assertTrue(state is UiState.Success)
        val list = (state as UiState.Success).data
        // sampleParty1 (receivable 1500) and sampleParty2 (payable 3000) and sampleParty3 (receivable 4000) have dues
        assertEquals(3, list.size)
    }

    @Test
    fun testBalanceFilter_settleable() = runTest {
        advanceUntilIdle()
        viewModel.setBalanceFilter("settleable")
        advanceUntilIdle()

        val state = viewModel.partiesState.value
        assertTrue(state is UiState.Success)
        val list = (state as UiState.Success).data
        // Only Gamma Enterprises has both receivable > 0 AND payable > 0
        assertEquals(1, list.size)
        assertEquals("Gamma Enterprises", list[0].name)
    }

    @Test
    fun testSearchQuery_debouncedFiltering() = runTest {
        advanceUntilIdle()
        viewModel.setSearchQuery("Beta")
        // Before debounce period (300ms)
        advanceTimeBy(100)
        // Advance past debounce
        advanceTimeBy(300)
        advanceUntilIdle()

        val state = viewModel.partiesState.value
        assertTrue(state is UiState.Success)
        val list = (state as UiState.Success).data
        assertEquals(1, list.size)
        assertEquals("Beta Wholesalers", list[0].name)
    }

    @Test
    fun testCreateParty_validationEmptyName() = runTest {
        advanceUntilIdle()
        viewModel.createParty(
            name = "",
            phone = "9876543210",
            role = "both"
        )
        advanceUntilIdle()

        val actionState = viewModel.actionState.value
        assertTrue(actionState is UiState.Error)
        assertEquals("Party name is required", (actionState as UiState.Error).message)
    }

    @Test
    fun testCreateParty_validationInvalidPhone() = runTest {
        advanceUntilIdle()
        viewModel.createParty(
            name = "Test Partner",
            phone = "123", // less than 10 digits
            role = "both"
        )
        advanceUntilIdle()

        val actionState = viewModel.actionState.value
        assertTrue(actionState is UiState.Error)
        assertEquals("Phone number must be at least 10 digits", (actionState as UiState.Error).message)
    }

    @Test
    fun testCreateParty_validationInvalidEmail() = runTest {
        advanceUntilIdle()
        viewModel.createParty(
            name = "Test Partner",
            phone = "9876543210",
            email = "invalid-email",
            role = "both"
        )
        advanceUntilIdle()

        val actionState = viewModel.actionState.value
        assertTrue(actionState is UiState.Error)
        assertEquals("Please enter a valid email address", (actionState as UiState.Error).message)
    }

    @Test
    fun testCreateParty_validationNegativeBalance() = runTest {
        advanceUntilIdle()
        viewModel.createParty(
            name = "Test Partner",
            phone = "9876543210",
            role = "both",
            openingReceivable = -50.0
        )
        advanceUntilIdle()

        val actionState = viewModel.actionState.value
        assertTrue(actionState is UiState.Error)
        assertEquals("Opening balances cannot be negative", (actionState as UiState.Error).message)
    }

    @Test
    fun testCreateParty_success() = runTest {
        advanceUntilIdle()
        var callbackInvoked = false

        viewModel.createParty(
            name = "Delta Corp",
            phone = "9112233445",
            email = "delta@test.com",
            address = "77 Station Road",
            role = "both",
            openingReceivable = 1000.0,
            openingPayable = 500.0,
            onSuccess = { callbackInvoked = true }
        )
        advanceUntilIdle()

        assertTrue(callbackInvoked)
        val actionState = viewModel.actionState.value
        assertTrue(actionState is UiState.Success)
        assertNotNull(fakeRepository.createdParty)
        assertEquals("Delta Corp", fakeRepository.createdParty?.name)

        // Verifying list was refreshed
        val parties = (viewModel.partiesState.value as UiState.Success).data
        assertTrue(parties.any { it.name == "Delta Corp" })
    }

    @Test
    fun testUpdateParty_success() = runTest {
        advanceUntilIdle()
        var callbackInvoked = false

        viewModel.updateParty(
            id = "p-1",
            name = "Alpha Stores Updated",
            phone = "9876543210",
            role = "customer",
            onSuccess = { callbackInvoked = true }
        )
        advanceUntilIdle()

        assertTrue(callbackInvoked)
        assertEquals("Alpha Stores Updated", fakeRepository.updatedParty?.name)
    }

    @Test
    fun testDeleteParty_success() = runTest {
        advanceUntilIdle()
        var callbackInvoked = false

        viewModel.deleteParty(
            id = "p-2",
            onSuccess = { callbackInvoked = true }
        )
        advanceUntilIdle()

        assertTrue(callbackInvoked)
        assertEquals("p-2", fakeRepository.deletedPartyId)

        val parties = (viewModel.partiesState.value as UiState.Success).data
        assertFalse(parties.any { it.id == "p-2" })
    }

    @Test
    fun testLoadPartyDetails_success() = runTest {
        advanceUntilIdle()
        viewModel.loadPartyDetails("p-3")
        advanceUntilIdle()

        val detailState = viewModel.selectedPartyDetail.value
        assertTrue(detailState is UiState.Success)
        val detail = (detailState as UiState.Success).data
        assertEquals("p-3", detail.id)
        assertEquals("Gamma Enterprises", detail.name)
    }

    @Test
    fun testLoadPartyFinancialSummary_success() = runTest {
        advanceUntilIdle()
        viewModel.loadPartyFinancialSummary("p-3")
        advanceUntilIdle()

        val summaryState = viewModel.selectedPartyFinancialSummary.value
        assertTrue(summaryState is UiState.Success)
        val summary = (summaryState as UiState.Success).data
        assertEquals("p-3", summary.partyId)
        assertEquals("Gamma Enterprises", summary.partyName)
    }

    @Test
    fun testDataRefreshCoordinator_triggersReload() = runTest {
        advanceUntilIdle()
        assertEquals(3, (viewModel.partiesState.value as UiState.Success).data.size)

        // Add a new party directly in fake repository
        fakeRepository.parties.add(
            Party(
                id = "p-4",
                name = "Zeta Logistics",
                role = "both",
                totalReceivable = "500.00",
                totalPayable = "0.00"
            )
        )

        // Notify mutation through coordinator
        refreshCoordinator.notifyMutation(DataMutation.DUES)
        advanceUntilIdle()

        // ViewModel should have automatically reloaded
        val parties = (viewModel.partiesState.value as UiState.Success).data
        assertEquals(4, parties.size)
        assertTrue(parties.any { it.name == "Zeta Logistics" })
    }
}
