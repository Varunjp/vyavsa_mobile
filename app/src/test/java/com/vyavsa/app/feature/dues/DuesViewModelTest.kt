package com.vyavsa.app.feature.dues

import com.vyavsa.app.core.common.Resource
import com.vyavsa.app.core.common.UiState
import com.vyavsa.app.core.model.ApplyCustomerAdvanceResponse
import com.vyavsa.app.core.model.Customer
import com.vyavsa.app.core.model.CustomerAdvanceResponse
import com.vyavsa.app.core.model.CustomerBalance
import com.vyavsa.app.core.model.CustomerBalanceAdjustment
import com.vyavsa.app.core.model.CustomerPaymentResponse
import com.vyavsa.app.core.model.CustomerStatement
import com.vyavsa.app.core.model.LineSale
import com.vyavsa.app.core.model.Supplier
import com.vyavsa.app.core.model.SupplierBalance
import com.vyavsa.app.core.model.SupplierPaymentResponse
import com.vyavsa.app.core.sync.DataRefreshCoordinator
import com.vyavsa.app.feature.bank.FakeBankRepository
import com.vyavsa.app.feature.supplier.SupplierRepository
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DuesViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeDuesRepository
    private lateinit var fakeSupplierRepository: FakeSupplierRepository
    private lateinit var fakeBankRepository: FakeBankRepository
    private lateinit var refreshCoordinator: DataRefreshCoordinator
    private lateinit var viewModel: DuesViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeDuesRepository()
        fakeSupplierRepository = FakeSupplierRepository()
        fakeBankRepository = FakeBankRepository()
        refreshCoordinator = DataRefreshCoordinator()
        viewModel = DuesViewModel(
            repository = fakeRepository,
            supplierRepository = fakeSupplierRepository,
            bankRepository = fakeBankRepository,
            refreshCoordinator = refreshCoordinator
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialLoadCustomersSuccess() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.customersState.value
        assertTrue("Expected Success state, got $state", state is UiState.Success)
        val customers = (state as UiState.Success).data
        assertEquals(2, customers.size)
        assertEquals("Sharma Stores", customers[0].customerName)
        assertEquals("Gupta Traders", customers[1].customerName)
    }

    @Test
    fun testCustomerSearchFiltersList() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onSearchChange("Gupta")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.customersState.value
        assertTrue("Expected Success state, got $state", state is UiState.Success)
        val customers = (state as UiState.Success).data
        assertEquals(1, customers.size)
        assertEquals("Gupta Traders", customers[0].customerName)
    }

    @Test
    fun testLoadCustomerDetailsAndSales() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.loadCustomerDetails("cust-1")
        testDispatcher.scheduler.advanceUntilIdle()

        val detailState = viewModel.selectedCustomer.value
        assertTrue("Expected Success for customer detail, got $detailState", detailState is UiState.Success)
        assertEquals("Sharma Stores", (detailState as UiState.Success).data.customerName)

        val salesState = viewModel.selectedCustomerSales.value
        assertTrue("Expected Success for sales history, got $salesState", salesState is UiState.Success)
        val sales = (salesState as UiState.Success).data
        assertEquals(1, sales.size)
        assertEquals("sale-1", sales[0].id)
    }

    @Test
    fun testAdminAdjustCustomerBalanceSuccess() = runTest {
        viewModel.adminOverrideForTesting = true
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.adjustBalance(
            id = "cust-1",
            newBalance = "100.00",
            adjustmentAmount = null,
            reason = "Audit reconciliation"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val actionState = viewModel.actionState.value
        assertTrue("Expected Success for adjustBalance, got $actionState", actionState is UiState.Success)
        assertEquals("100.00", fakeRepository.customers[0].currentBalance)
    }

    @Test
    fun testNonAdminAdjustBalanceBlockedByRbac() = runTest {
        viewModel.adminOverrideForTesting = false
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.adjustBalance(
            id = "cust-1",
            newBalance = "100.00",
            adjustmentAmount = null,
            reason = "Audit reconciliation"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val actionState = viewModel.actionState.value
        assertTrue("Expected Error for non-admin adjust balance, got $actionState", actionState is UiState.Error)
        assertEquals(
            "Forbidden: Administrator privileges required",
            (actionState as UiState.Error).message
        )
    }

    @Test
    fun testAdjustBalanceEmptyReasonFails() = runTest {
        viewModel.adminOverrideForTesting = true
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.adjustBalance(
            id = "cust-1",
            newBalance = "100.00",
            adjustmentAmount = null,
            reason = "   "
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val actionState = viewModel.actionState.value
        assertTrue("Expected Error for empty reason, got $actionState", actionState is UiState.Error)
        assertEquals(
            "Reason is required for manual balance adjustment",
            (actionState as UiState.Error).message
        )
    }

    @Test
    fun testRecordPaymentSuccess() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.recordPayment(
            customerId = "cust-1",
            amount = "150.00",
            paymentMethod = "cash",
            note = "Counter payment"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val actionState = viewModel.actionState.value
        assertTrue("Expected Success on recording payment, got $actionState", actionState is UiState.Success)
    }

    @Test
    fun testAdminDeleteCustomerSuccess() = runTest {
        viewModel.adminOverrideForTesting = true
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.deleteCustomer("cust-2")
        testDispatcher.scheduler.advanceUntilIdle()

        val actionState = viewModel.actionState.value
        assertTrue("Expected Success on deleting customer, got $actionState", actionState is UiState.Success)
        assertEquals(1, fakeRepository.customers.size)
    }

    @Test
    fun testNonAdminDeleteCustomerBlocked() = runTest {
        viewModel.adminOverrideForTesting = false
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.deleteCustomer("cust-2")
        testDispatcher.scheduler.advanceUntilIdle()

        val actionState = viewModel.actionState.value
        assertTrue("Expected Error for non-admin delete customer, got $actionState", actionState is UiState.Error)
        assertEquals(
            "Forbidden: Administrator privileges required",
            (actionState as UiState.Error).message
        )
    }

    @Test
    fun testCollectCustomerPaymentCashSuccess() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        var callbackCalled = false
        viewModel.collectCustomerPayment(
            customerId = "cust-1",
            amount = "200.00",
            paymentMethod = "cash",
            note = "Partial balance collection",
            onSuccess = { callbackCalled = true }
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val actionState = viewModel.actionState.value
        assertTrue("Expected Success on collect payment, got $actionState", actionState is UiState.Success)
        assertTrue(callbackCalled)
        assertEquals(1, fakeRepository.customerCollections.size)
        assertEquals("200.00", fakeRepository.customerCollections[0].amount)
        assertEquals("cash", fakeRepository.customerCollections[0].paymentMethod)
        assertEquals("300.00", fakeRepository.customers.find { it.id == "cust-1" }?.currentBalance)
    }

    @Test
    fun testCollectCustomerPaymentBankSuccess() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.collectCustomerPayment(
            customerId = "cust-1",
            amount = "300.00",
            paymentMethod = "bank",
            bankId = "bank-1",
            bankName = "HDFC Bank",
            referenceId = "UPI-REF-12345",
            note = "UPI transfer"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val actionState = viewModel.actionState.value
        assertTrue("Expected Success on bank collect payment, got $actionState", actionState is UiState.Success)
        assertEquals(1, fakeRepository.customerCollections.size)
        assertEquals("bank", fakeRepository.customerCollections[0].paymentMethod)
        assertEquals("UPI-REF-12345", fakeRepository.customerCollections[0].referenceId)
    }

    @Test
    fun testCollectCustomerPaymentBankWithoutBankIdFails() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.collectCustomerPayment(
            customerId = "cust-1",
            amount = "100.00",
            paymentMethod = "bank",
            bankId = null
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val actionState = viewModel.actionState.value
        assertTrue("Expected Error when bank is missing, got $actionState", actionState is UiState.Error)
        assertEquals("Please select a bank account for bank collection", (actionState as UiState.Error).message)
        assertEquals(0, fakeRepository.customerCollections.size)
    }

    @Test
    fun testCollectCustomerPaymentExceedingBalanceFails() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.collectCustomerPayment(
            customerId = "cust-1",
            amount = "9999.00",
            paymentMethod = "cash"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val actionState = viewModel.actionState.value
        assertTrue("Expected Error when amount exceeds balance, got $actionState", actionState is UiState.Error)
        assertTrue((actionState as UiState.Error).message.contains("cannot exceed customer's outstanding balance"))
        assertEquals(0, fakeRepository.customerCollections.size)
    }

    @Test
    fun testSelectTabSwitchesToSuppliers() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.selectTab(DuesTab.SUPPLIERS)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(DuesTab.SUPPLIERS, viewModel.selectedTab.value)
        val state = viewModel.suppliersState.value
        assertTrue("Expected Success state for suppliers, got $state", state is UiState.Success)
        val suppliers = (state as UiState.Success).data
        assertEquals(2, suppliers.size)
        assertEquals("Vendor Alpha", suppliers[0].displayName)
    }

    @Test
    fun testCreateSupplierSuccess() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        var callbackCalled = false
        viewModel.createSupplier(
            name = "Vendor Gamma",
            phone = "9876543299",
            onSuccess = { callbackCalled = true }
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val actionState = viewModel.actionState.value
        assertTrue("Expected Success on creating supplier, got $actionState", actionState is UiState.Success)
        assertTrue(callbackCalled)
        assertEquals(3, fakeSupplierRepository.suppliers.size)
    }

    @Test
    fun testRecordSupplierPaymentCashSuccess() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        var callbackCalled = false
        viewModel.recordSupplierPayment(
            supplierId = "supp-1",
            amount = "500.00",
            paymentMethod = "cash",
            note = "Cash payout",
            onSuccess = { callbackCalled = true }
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val actionState = viewModel.actionState.value
        assertTrue("Expected Success on recording supplier payment, got $actionState", actionState is UiState.Success)
        assertTrue(callbackCalled)
        assertEquals(1, fakeSupplierRepository.recordedPayments.size)
        assertEquals("1000.00", fakeSupplierRepository.suppliers.find { it.id == "supp-1" }?.outstandingPayable)
    }

    @Test
    fun testRecordSupplierPaymentBankRequiresBankId() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.recordSupplierPayment(
            supplierId = "supp-1",
            amount = "500.00",
            paymentMethod = "bank",
            bankId = null
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val actionState = viewModel.actionState.value
        assertTrue("Expected Error on missing bankId for bank payment, got $actionState", actionState is UiState.Error)
        assertEquals("Please select a bank account for bank payout", (actionState as UiState.Error).message)
        assertEquals(0, fakeSupplierRepository.recordedPayments.size)
    }

    @Test
    fun testCollectCustomerAdvanceCashSuccess() = runTest {
        val customer = fakeRepository.customers.find { it.id == "cust-1" }!!
        assertEquals("500.00", customer.currentBalance)
        assertEquals("0.00", customer.advanceBalance)

        var callbackResponse: CustomerAdvanceResponse? = null
        viewModel.collectCustomerAdvance(
            customerId = "cust-1",
            amount = "800.00",
            paymentMethod = "cash",
            note = "Advance collection route 4"
        ) { resp ->
            callbackResponse = resp
        }
        testDispatcher.scheduler.advanceUntilIdle()

        val resp = viewModel.lastAdvanceResponse.value
        assertNotNull("Expected lastAdvanceResponse to be populated", resp)
        assertEquals("800.00", resp?.amount)
        assertEquals("500.00", resp?.settledDues)
        assertEquals("300.00", resp?.availableAmount)
        assertEquals("0.00", resp?.customerBalance)
        assertEquals("300.00", resp?.advanceBalance)
        assertEquals("cash", resp?.paymentMethod)
        assertEquals(resp, callbackResponse)

        val updatedCustomer = fakeRepository.customers.find { it.id == "cust-1" }!!
        assertEquals("0.00", updatedCustomer.currentBalance)
        assertEquals("300.00", updatedCustomer.advanceBalance)

        viewModel.clearLastAdvanceResponse()
        assertNull(viewModel.lastAdvanceResponse.value)
    }

    @Test
    fun testCollectCustomerAdvanceBankRequiresBankId() = runTest {
        viewModel.collectCustomerAdvance(
            customerId = "cust-1",
            amount = "500.00",
            paymentMethod = "bank",
            bankId = null
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val actionState = viewModel.actionState.value
        assertTrue("Expected Error on missing bankId for advance bank collection, got $actionState", actionState is UiState.Error)
        assertEquals("Please select a bank account for bank advance collection", (actionState as UiState.Error).message)
        assertEquals(0, fakeRepository.customerAdvances.size)
    }

    @Test
    fun testCollectCustomerAdvanceInvalidAmountFails() = runTest {
        viewModel.collectCustomerAdvance(
            customerId = "cust-1",
            amount = "0.00",
            paymentMethod = "cash"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        var actionState = viewModel.actionState.value
        assertTrue("Expected Error on zero advance amount, got $actionState", actionState is UiState.Error)
        assertEquals("Advance amount must be strictly greater than zero", (actionState as UiState.Error).message)

        viewModel.collectCustomerAdvance(
            customerId = "cust-1",
            amount = "-50.00",
            paymentMethod = "cash"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        actionState = viewModel.actionState.value
        assertTrue("Expected Error on negative advance amount, got $actionState", actionState is UiState.Error)
        assertEquals("Advance amount must be strictly greater than zero", (actionState as UiState.Error).message)
        assertEquals(0, fakeRepository.customerAdvances.size)
    }

    @Test
    fun testApplyCustomerAdvanceSuccess() = runTest {
        val cust = fakeRepository.customers.find { it.id == "cust-1" }!!
        fakeRepository.customers[0] = cust.copy(currentBalance = "500.00", advanceBalance = "300.00")

        var successCallbackCalled = false
        viewModel.applyCustomerAdvance(
            customerId = "cust-1",
            amount = "200.00",
            note = "Applying 200 from deposit"
        ) {
            successCallbackCalled = true
        }
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(successCallbackCalled)
        val updated = fakeRepository.customers.find { it.id == "cust-1" }!!
        assertEquals("300.00", updated.currentBalance)
        assertEquals("100.00", updated.advanceBalance)
    }

    @Test
    fun testApplyCustomerAdvanceInvalidAmountFails() = runTest {
        viewModel.applyCustomerAdvance(
            customerId = "cust-1",
            amount = "0.00"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val actionState = viewModel.actionState.value
        assertTrue("Expected Error on zero apply amount", actionState is UiState.Error)
        assertEquals("Please enter a valid amount to apply", (actionState as UiState.Error).message)
    }
}

class FakeDuesRepository : DuesRepository {

    val customers = mutableListOf(
        Customer(
            id = "cust-1",
            customerName = "Sharma Stores",
            phone = "9876543210",
            currentBalance = "500.00",
            status = "active",
            createdAt = "2026-01-01T00:00:00Z"
        ),
        Customer(
            id = "cust-2",
            customerName = "Gupta Traders",
            phone = "9876543211",
            currentBalance = "-200.00",
            status = "active",
            createdAt = "2026-01-02T00:00:00Z"
        )
    )

    val adjustments = mutableListOf<CustomerBalanceAdjustment>()

    val sales = mutableListOf(
        LineSale(
            id = "sale-1",
            customerId = "cust-1",
            customerName = "Sharma Stores",
            totalAmount = "1500.00",
            totalCashIn = "1000.00",
            bankAmount = "0.00",
            collectedAmount = "1000.00",
            balance = "500.00",
            createdAt = "2026-01-15T10:00:00Z"
        )
    )

    override fun listCustomers(search: String?): Flow<Resource<List<Customer>>> = flow {
        emit(Resource.Loading)
        val filtered = customers.filter {
            val phone = it.phone
            search == null || it.customerName.contains(search, ignoreCase = true) ||
            (phone != null && phone.contains(search))
        }
        emit(Resource.Success(filtered))
    }

    override fun createCustomer(name: String, phone: String?, openingBalance: String): Flow<Resource<Customer>> = flow {
        emit(Resource.Loading)
        val customer = Customer(
            id = "cust-${customers.size + 1}",
            customerName = name,
            phone = phone,
            currentBalance = openingBalance,
            status = "active",
            createdAt = "2026-01-03T00:00:00Z"
        )
        customers.add(customer)
        emit(Resource.Success(customer))
    }

    override fun getCustomerById(id: String): Flow<Resource<Customer>> = flow {
        emit(Resource.Loading)
        val customer = customers.find { it.id == id }
        if (customer != null) {
            emit(Resource.Success(customer))
        } else {
            emit(Resource.Error("Customer not found"))
        }
    }

    override fun updateCustomer(id: String, name: String?, phone: String?, status: String?): Flow<Resource<Customer>> = flow {
        emit(Resource.Loading)
        val idx = customers.indexOfFirst { it.id == id }
        if (idx >= 0) {
            val old = customers[idx]
            val updated = old.copy(
                customerName = name ?: old.customerName,
                phone = phone ?: old.phone,
                status = status ?: old.status
            )
            customers[idx] = updated
            emit(Resource.Success(updated))
        } else {
            emit(Resource.Error("Customer not found"))
        }
    }

    override fun deleteCustomer(id: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading)
        customers.removeAll { it.id == id }
        emit(Resource.Success(Unit))
    }

    override fun adjustCustomerBalance(
        id: String,
        newBalance: String?,
        adjustmentAmount: String?,
        reason: String
    ): Flow<Resource<CustomerBalanceAdjustment>> = flow {
        emit(Resource.Loading)
        val idx = customers.indexOfFirst { it.id == id }
        if (idx >= 0) {
            val old = customers[idx]
            val prevBal = old.currentBalance
            val targetBal = newBalance ?: "0.00"
            customers[idx] = old.copy(currentBalance = targetBal)

            val adj = CustomerBalanceAdjustment(
                id = "adj-1",
                customerId = id,
                previousBalance = prevBal,
                newBalance = targetBal,
                adjustmentAmount = adjustmentAmount ?: "0.00",
                reason = reason,
                createdAt = "2026-01-16T12:00:00Z"
            )
            adjustments.add(adj)
            emit(Resource.Success(adj))
        } else {
            emit(Resource.Error("Customer not found"))
        }
    }

    override fun listCustomerAdjustments(id: String): Flow<Resource<List<CustomerBalanceAdjustment>>> = flow {
        emit(Resource.Loading)
        emit(Resource.Success(adjustments.filter { it.customerId == id }))
    }

    override fun getCustomerBalance(id: String): Flow<Resource<CustomerBalance>> = flow {
        emit(Resource.Loading)
        val customer = customers.find { it.id == id }
        if (customer != null) {
            emit(Resource.Success(CustomerBalance(
                customerId = id,
                customerName = customer.customerName,
                currentBalance = customer.currentBalance
            )))
        } else {
            emit(Resource.Error("Customer not found"))
        }
    }

    override fun getCustomerStatement(id: String): Flow<Resource<CustomerStatement>> = flow {
        emit(Resource.Loading)
        emit(Resource.Success(CustomerStatement(
            customerId = id,
            customerName = "Sharma Stores",
            openingBalance = "0.00",
            currentBalance = "500.00",
            totalPurchases = "1500.00",
            totalPaid = "1000.00",
            outstandingPayable = "500.00",
            entries = emptyList()
        )))
    }

    override fun listCustomerSales(customerId: String): Flow<Resource<List<LineSale>>> = flow {
        emit(Resource.Loading)
        emit(Resource.Success(sales.filter { it.customerId == customerId }))
    }

    override fun recordPayment(
        id: String,
        amount: String,
        paymentMethod: String,
        date: String?,
        note: String?
    ): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading)
        emit(Resource.Success(Unit))
    }

    val customerCollections = mutableListOf<CustomerPaymentResponse>()

    override fun collectCustomerPayment(
        id: String,
        amount: String,
        paymentMethod: String,
        bankId: String?,
        bankName: String?,
        date: String?,
        referenceId: String?,
        note: String?
    ): Flow<Resource<CustomerPaymentResponse>> = flow {
        emit(Resource.Loading)
        val customer = customers.find { it.id == id }
        val prevBal = customer?.currentBalance?.toDoubleOrNull() ?: 0.0
        val collAmt = amount.toDoubleOrNull() ?: 0.0
        val newBal = (prevBal - collAmt).coerceAtLeast(0.0)
        if (customer != null) {
            val idx = customers.indexOf(customer)
            customers[idx] = customer.copy(currentBalance = "%.2f".format(newBal))
        }
        val response = CustomerPaymentResponse(
            id = "coll-${customerCollections.size + 1}",
            customerId = id,
            customerName = customer?.customerName ?: "Customer",
            amount = amount,
            paymentMethod = paymentMethod,
            previousBalance = "%.2f".format(prevBal),
            customerBalance = "%.2f".format(newBal),
            referenceId = referenceId,
            note = note,
            paymentDate = date ?: "2026-10-28"
        )
        customerCollections.add(response)
        emit(Resource.Success(response))
    }

    override fun listCustomerCollections(customerId: String): Flow<Resource<List<CustomerPaymentResponse>>> = flow {
        emit(Resource.Loading)
        emit(Resource.Success(customerCollections.filter { it.customerId == customerId }))
    }

    val customerAdvances = mutableListOf<CustomerAdvanceResponse>()

    override fun collectCustomerAdvance(
        id: String,
        amount: String,
        paymentMethod: String,
        bankId: String?,
        bankName: String?,
        date: String?,
        referenceId: String?,
        note: String?
    ): Flow<Resource<CustomerAdvanceResponse>> = flow {
        emit(Resource.Loading)
        val customer = customers.find { it.id == id }
        val prevBal = customer?.currentBalance?.toDoubleOrNull() ?: 0.0
        val advAmt = amount.toDoubleOrNull() ?: 0.0
        val settledDues = minOf(prevBal, advAmt)
        val newDue = (prevBal - settledDues).coerceAtLeast(0.0)
        val availableAdvance = (advAmt - settledDues).coerceAtLeast(0.0)
        val prevAdv = customer?.advanceBalance?.toDoubleOrNull() ?: 0.0
        val newAdv = prevAdv + availableAdvance
        if (customer != null) {
            val idx = customers.indexOf(customer)
            customers[idx] = customer.copy(
                currentBalance = "%.2f".format(newDue),
                advanceBalance = "%.2f".format(newAdv)
            )
        }
        val response = CustomerAdvanceResponse(
            id = "adv-${customerAdvances.size + 1}",
            customerId = id,
            customerName = customer?.customerName ?: "Customer",
            receiptNumber = "ADV-20261031-0001",
            amount = amount,
            appliedAmount = "%.2f".format(settledDues),
            availableAmount = "%.2f".format(availableAdvance),
            settledDues = "%.2f".format(settledDues),
            paymentMethod = paymentMethod,
            customerBalance = "%.2f".format(newDue),
            advanceBalance = "%.2f".format(newAdv),
            referenceId = referenceId,
            note = note,
            paymentDate = date ?: "2026-10-31"
        )
        customerAdvances.add(response)
        emit(Resource.Success(response))
    }

    override fun listCustomerAdvances(customerId: String): Flow<Resource<List<CustomerAdvanceResponse>>> = flow {
        emit(Resource.Loading)
        emit(Resource.Success(customerAdvances.filter { it.customerId == customerId }))
    }

    override fun applyCustomerAdvance(
        customerId: String,
        amount: String,
        advanceId: String?,
        note: String?
    ): Flow<Resource<ApplyCustomerAdvanceResponse>> = flow {
        emit(Resource.Loading)
        val amt = amount.toDoubleOrNull() ?: 0.0
        val customer = customers.find { it.id == customerId }
        val prevDue = customer?.currentBalance?.toDoubleOrNull() ?: 0.0
        val prevAdv = customer?.advanceBalance?.toDoubleOrNull() ?: 0.0
        val remDue = (prevDue - amt).coerceAtLeast(0.0)
        val remAdv = (prevAdv - amt).coerceAtLeast(0.0)
        if (customer != null) {
            val idx = customers.indexOf(customer)
            customers[idx] = customer.copy(
                currentBalance = "%.2f".format(remDue),
                advanceBalance = "%.2f".format(remAdv)
            )
        }
        val response = ApplyCustomerAdvanceResponse(
            customerId = customerId,
            customerName = customer?.customerName ?: "Customer",
            amountApplied = amount,
            previousCustomerBalance = "%.2f".format(prevDue),
            updatedCustomerBalance = "%.2f".format(remDue),
            previousAdvanceBalance = "%.2f".format(prevAdv),
            updatedAdvanceBalance = "%.2f".format(remAdv)
        )
        emit(Resource.Success(response))
    }

    override suspend fun downloadAdvanceReceiptPdf(id: String): ResponseBody {
        return "%PDF-1.4 test advance pdf".toResponseBody("application/pdf".toMediaTypeOrNull())
    }
}

class FakeSupplierRepository : SupplierRepository {
    val suppliers = mutableListOf(
        Supplier(
            id = "supp-1",
            supplierName = "Vendor Alpha",
            phone = "9876543210",
            outstandingPayable = "1500.00",
            status = "active"
        ),
        Supplier(
            id = "supp-2",
            supplierName = "Vendor Beta",
            phone = "9876543211",
            outstandingPayable = "800.00",
            status = "active"
        )
    )

    val recordedPayments = mutableListOf<SupplierPaymentResponse>()

    override fun listSuppliers(search: String?, status: String?): Flow<Resource<List<Supplier>>> = flow {
        emit(Resource.Loading)
        val filtered = suppliers.filter {
            val phone = it.phone
            (search == null || it.supplierName.contains(search, ignoreCase = true) || (phone != null && phone.contains(search))) &&
            (status == null || it.status.equals(status, ignoreCase = true))
        }
        emit(Resource.Success(filtered))
    }

    override fun createSupplier(name: String, phone: String?, status: String): Flow<Resource<Supplier>> = flow {
        emit(Resource.Loading)
        val supplier = Supplier(
            id = "supp-${suppliers.size + 1}",
            supplierName = name,
            phone = phone,
            outstandingPayable = "0.00",
            status = status
        )
        suppliers.add(supplier)
        emit(Resource.Success(supplier))
    }

    override fun getSupplierById(id: String): Flow<Resource<Supplier>> = flow {
        emit(Resource.Loading)
        val supplier = suppliers.find { it.id == id }
        if (supplier != null) emit(Resource.Success(supplier)) else emit(Resource.Error("Supplier not found"))
    }

    override fun updateSupplier(id: String, name: String?, phone: String?, status: String?): Flow<Resource<Supplier>> = flow {
        emit(Resource.Loading)
        val idx = suppliers.indexOfFirst { it.id == id }
        if (idx >= 0) {
            val old = suppliers[idx]
            val updated = old.copy(
                supplierName = name ?: old.supplierName,
                phone = phone ?: old.phone,
                status = status ?: old.status
            )
            suppliers[idx] = updated
            emit(Resource.Success(updated))
        } else {
            emit(Resource.Error("Supplier not found"))
        }
    }

    override fun deleteSupplier(id: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading)
        suppliers.removeAll { it.id == id }
        emit(Resource.Success(Unit))
    }

    override fun getSupplierBalance(id: String): Flow<Resource<SupplierBalance>> = flow {
        emit(Resource.Loading)
        val supplier = suppliers.find { it.id == id }
        if (supplier != null) {
            emit(Resource.Success(SupplierBalance(
                supplierId = id,
                supplierName = supplier.supplierName,
                outstandingPayable = supplier.outstandingPayable
            )))
        } else {
            emit(Resource.Error("Supplier not found"))
        }
    }

    override fun recordSupplierPayment(
        supplierId: String,
        amount: String,
        paymentMethod: String,
        bankId: String?,
        paymentDate: String?,
        referenceId: String?,
        note: String?,
        purchaseId: String?
    ): Flow<Resource<SupplierPaymentResponse>> = flow {
        emit(Resource.Loading)
        val supp = suppliers.find { it.id == supplierId }
        val prevPayable = supp?.outstandingPayable?.toDoubleOrNull() ?: 0.0
        val pmtAmt = amount.toDoubleOrNull() ?: 0.0
        val newPayable = (prevPayable - pmtAmt).coerceAtLeast(0.0)
        if (supp != null) {
            val idx = suppliers.indexOf(supp)
            suppliers[idx] = supp.copy(outstandingPayable = "%.2f".format(newPayable))
        }
        val resp = SupplierPaymentResponse(
            paymentId = "pay-${recordedPayments.size + 1}",
            amount = amount,
            paymentMethod = paymentMethod,
            referenceId = referenceId,
            note = note,
            paymentDate = paymentDate ?: "2026-10-28"
        )
        recordedPayments.add(resp)
        emit(Resource.Success(resp))
    }
}
