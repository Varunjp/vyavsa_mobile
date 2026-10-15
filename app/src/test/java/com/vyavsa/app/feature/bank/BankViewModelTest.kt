package com.vyavsa.app.feature.bank

import com.vyavsa.app.core.common.Resource
import com.vyavsa.app.core.common.UiState
import com.vyavsa.app.core.model.CreateBankRequest
import com.vyavsa.app.core.model.TenantBank
import com.vyavsa.app.core.model.UpdateBankRequest
import com.vyavsa.app.core.sync.DataMutation
import com.vyavsa.app.core.sync.DataRefreshCoordinator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BankViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeBankRepository
    private lateinit var refreshCoordinator: DataRefreshCoordinator
    private lateinit var viewModel: BankViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeBankRepository()
        refreshCoordinator = DataRefreshCoordinator()
        viewModel = BankViewModel(fakeRepository, refreshCoordinator)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialLoadBanksSuccess() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.banksState.value
        assertTrue("Expected Success state, but was $state", state is UiState.Success)
        val banks = (state as UiState.Success).data
        assertEquals(2, banks.size)
        assertEquals("HDFC Bank", banks[0].bankName)
        assertEquals("SBI Bank", banks[1].bankName)
    }

    @Test
    fun testEmptyBankListDisplaysEmptyState() = runTest {
        fakeRepository.banksList.clear()
        viewModel.loadBanks()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.banksState.value
        assertTrue("Expected Empty state when bank list is empty", state is UiState.Empty)
    }

    @Test
    fun testCreateBankValidationMissingName() = runTest {
        viewModel.createBank("", "123456", "HDFC0001", "100.00")
        testDispatcher.scheduler.advanceUntilIdle()

        val actionState = viewModel.actionState.value
        assertTrue(actionState is UiState.Error)
        assertEquals("Bank and branch name is required", (actionState as UiState.Error).message)
    }

    @Test
    fun testCreateBankValidationNegativeBalance() = runTest {
        viewModel.createBank("Axis Bank", "123456", "UTIB0001", "-50.00")
        testDispatcher.scheduler.advanceUntilIdle()

        val actionState = viewModel.actionState.value
        assertTrue(actionState is UiState.Error)
        assertEquals("Please enter a valid opening balance (0.00 or greater)", (actionState as UiState.Error).message)
    }

    @Test
    fun testCreateBankSuccessReloadsList() = runTest {
        viewModel.createBank("Kotak Bank", "778899", "KKBK0001", "5000.00")
        testDispatcher.scheduler.advanceUntilIdle()

        val actionState = viewModel.actionState.value
        assertTrue(actionState is UiState.Success)

        val banksState = viewModel.banksState.value
        assertTrue(banksState is UiState.Success)
        val banks = (banksState as UiState.Success).data
        assertEquals(3, banks.size)
        assertTrue(banks.any { it.bankName == "Kotak Bank" })
    }

    @Test
    fun testPreventDuplicateSubmissionWhileLoading() = runTest {
        fakeRepository.delayResponse = true
        viewModel.createBank("Delayed Bank", "111", "IFSC", "100.00")
        testDispatcher.scheduler.runCurrent()

        assertTrue(viewModel.actionState.value is UiState.Loading)

        // Try submitting again while loading
        val initialCallCount = fakeRepository.createCallCount
        viewModel.createBank("Duplicate Bank", "222", "IFSC", "200.00")
        assertEquals(initialCallCount, fakeRepository.createCallCount)
    }

    @Test
    fun testRealTimeSyncOnCoordinatorMutation() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(2, (viewModel.banksState.value as UiState.Success).data.size)

        // Simulate a mutation occurring from another screen (e.g. sale or transaction recorded)
        fakeRepository.banksList.add(
            TenantBank(
                id = "bank-3",
                tenantId = "tenant-1",
                bankName = "ICICI Bank",
                currentBalance = "75000.00",
                status = "active"
            )
        )

        refreshCoordinator.notifyMutation(DataMutation.BANK)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.banksState.value
        assertTrue(state is UiState.Success)
        assertEquals(3, (state as UiState.Success).data.size)
    }
}

class FakeBankRepository : BankRepository {
    val banksList = mutableListOf(
        TenantBank(
            id = "bank-1",
            tenantId = "tenant-1",
            bankName = "HDFC Bank",
            accountNumber = "123456",
            ifsc = "HDFC0001",
            currentBalance = "25000.00",
            status = "active"
        ),
        TenantBank(
            id = "bank-2",
            tenantId = "tenant-1",
            bankName = "SBI Bank",
            accountNumber = "654321",
            ifsc = "SBIN0001",
            currentBalance = "15000.00",
            status = "active"
        )
    )

    var createCallCount = 0
    var delayResponse = false

    override fun listBanks(search: String?, status: String?): Flow<Resource<List<TenantBank>>> = flow {
        emit(Resource.Loading)
        val filtered = banksList.filter {
            (search == null || it.bankName.contains(search, ignoreCase = true)) &&
            (status == null || it.status.equals(status, ignoreCase = true))
        }
        emit(Resource.Success(filtered))
    }

    override fun createBank(request: CreateBankRequest): Flow<Resource<TenantBank>> = flow {
        createCallCount++
        emit(Resource.Loading)
        if (!delayResponse) {
            val newBank = TenantBank(
                id = "bank-new-${banksList.size + 1}",
                tenantId = "tenant-1",
                bankName = request.bankName,
                accountNumber = request.accountNumber,
                ifsc = request.ifsc,
                currentBalance = request.openingBalance,
                status = request.status
            )
            banksList.add(newBank)
            emit(Resource.Success(newBank))
        }
    }

    override fun updateBank(id: String, request: UpdateBankRequest): Flow<Resource<TenantBank>> = flow {
        emit(Resource.Loading)
        val index = banksList.indexOfFirst { it.id == id }
        if (index >= 0) {
            val updated = banksList[index].copy(
                bankName = request.bankName,
                accountNumber = request.accountNumber,
                ifsc = request.ifsc,
                status = request.status
            )
            banksList[index] = updated
            emit(Resource.Success(updated))
        } else {
            emit(Resource.Error("Bank not found"))
        }
    }

    override fun deleteBank(id: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading)
        banksList.removeAll { it.id == id }
        emit(Resource.Success(Unit))
    }
}
