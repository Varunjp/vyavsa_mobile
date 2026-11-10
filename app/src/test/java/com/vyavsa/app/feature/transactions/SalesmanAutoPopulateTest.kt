package com.vyavsa.app.feature.transactions

import com.vyavsa.app.core.model.CounterSale
import com.vyavsa.app.core.model.CreateCounterSaleRequest
import com.vyavsa.app.core.model.CreateLineSaleRequest
import com.vyavsa.app.core.model.UpdateCounterSaleRequest
import com.vyavsa.app.core.model.UserProfile
import com.vyavsa.app.core.model.UserRole
import com.vyavsa.app.core.security.TokenManager
import com.vyavsa.app.core.sync.DataRefreshCoordinator
import com.vyavsa.app.feature.bank.FakeBankRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Dedicated unit test suite for Salesman Name Auto-population and Form State management
 * across Line Sale and Counter Sale.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SalesmanAutoPopulateTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepo: FakeTransactionsRepository
    private lateinit var fakeBankRepo: FakeBankRepository
    private lateinit var refreshCoordinator: DataRefreshCoordinator
    private val json = Json { ignoreUnknownKeys = true; isLenient = true; encodeDefaults = true }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepo = FakeTransactionsRepository()
        fakeBankRepo = FakeBankRepository()
        refreshCoordinator = DataRefreshCoordinator()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(userProfile: UserProfile?): TransactionsViewModel {
        val fakeTokenManager = FakeTransactionsTokenManager(profileData = userProfile)
        return TransactionsViewModel(
            repository = fakeRepo,
            bankRepository = fakeBankRepo,
            refreshCoordinator = refreshCoordinator,
            tokenManager = fakeTokenManager,
            productRepository = null
        )
    }

    @Test
    fun testLoggedInUserNameAutoPopulatesDefaultSalesman() {
        val user = UserProfile(
            id = "u-100",
            name = "Rohan Sharma",
            email = "rohan@vyavsa.com",
            role = "admin"
        )
        val vm = createViewModel(user)
        assertEquals("Rohan Sharma", vm.defaultSalesmanName)
    }

    @Test
    fun testMissingUserProfileHandledGracefullyWithoutCrash() {
        val vm = createViewModel(null)
        assertEquals("", vm.defaultSalesmanName)

        val vmWithoutTokenManager = TransactionsViewModel(
            repository = fakeRepo,
            bankRepository = fakeBankRepo,
            refreshCoordinator = refreshCoordinator
        )
        assertEquals("", vmWithoutTokenManager.defaultSalesmanName)
    }

    @Test
    fun testBlankUserNameProfileHandledGracefully() {
        val user = UserProfile(
            id = "u-101",
            name = "   ",
            email = "blank@vyavsa.com",
            role = "sales"
        )
        val vm = createViewModel(user)
        assertEquals("", vm.defaultSalesmanName)
    }

    @Test
    fun testSalesmanNameWithLeadingAndTrailingWhitespaceIsTrimmed() {
        val user = UserProfile(
            id = "u-102",
            name = "  Pooja Verma  ",
            email = "pooja@vyavsa.com",
            role = "sales"
        )
        val vm = createViewModel(user)
        assertEquals("Pooja Verma", vm.defaultSalesmanName)
    }

    @Test
    fun testLineSaleValidationIncludesSalesmanName() {
        val vm = createViewModel(UserProfile(id = "u-1", name = "Rohan Sharma"))
        val defaultSalesman = vm.defaultSalesmanName

        // Case 1: Populated with default salesman
        val resultDefault = vm.validateAndBuildLineSaleRequest(
            customerId = "cust-1",
            totalAmount = "1200.00",
            cashIn = "1200.00",
            route = "Route North",
            salesman = defaultSalesman
        )
        assertTrue(resultDefault.isSuccess)
        assertEquals("Rohan Sharma", resultDefault.getOrNull()?.salesman)

        // Case 2: Field edited by user to another salesperson
        val resultCustom = vm.validateAndBuildLineSaleRequest(
            customerId = "cust-1",
            totalAmount = "1200.00",
            cashIn = "1200.00",
            route = "Route North",
            salesman = "Suresh Field Rep"
        )
        assertTrue(resultCustom.isSuccess)
        assertEquals("Suresh Field Rep", resultCustom.getOrNull()?.salesman)

        // Case 3: Field cleared by user
        val resultCleared = vm.validateAndBuildLineSaleRequest(
            customerId = "cust-1",
            totalAmount = "1200.00",
            cashIn = "1200.00",
            route = "Route North",
            salesman = ""
        )
        assertTrue(resultCleared.isSuccess)
        assertNull(resultCleared.getOrNull()?.salesman)
    }

    @Test
    fun testCounterSaleValidationIncludesSalesmanName() {
        val vm = createViewModel(UserProfile(id = "u-1", name = "Amitabh Bachchan"))
        val defaultSalesman = vm.defaultSalesmanName

        // Case 1: Populated with default salesman
        val resultDefault = vm.validateAndBuildCounterSaleRequest(
            item = "Stationery Pack",
            price = "350.00",
            totalAmount = "350.00",
            cash = "350.00",
            salesman = defaultSalesman
        )
        assertTrue(resultDefault.isSuccess)
        assertEquals("Amitabh Bachchan", resultDefault.getOrNull()?.salesman)

        // Case 2: Field edited by user to another salesperson
        val resultCustom = vm.validateAndBuildCounterSaleRequest(
            item = "Stationery Pack",
            price = "350.00",
            totalAmount = "350.00",
            cash = "350.00",
            salesman = "Pooja Counter Rep"
        )
        assertTrue(resultCustom.isSuccess)
        assertEquals("Pooja Counter Rep", resultCustom.getOrNull()?.salesman)

        // Case 3: Field cleared by user
        val resultCleared = vm.validateAndBuildCounterSaleRequest(
            item = "Stationery Pack",
            price = "350.00",
            totalAmount = "350.00",
            cash = "350.00",
            salesman = "   "
        )
        assertTrue(resultCleared.isSuccess)
        assertNull(resultCleared.getOrNull()?.salesman)
    }

    @Test
    fun testCreateLineSaleSubmitsSalesmanToBackend() = runTest {
        val vm = createViewModel(UserProfile(id = "u-1", name = "Rohan Sharma"))

        // Create sale with manual override
        vm.createLineSale(
            customerId = "cust-1",
            totalAmount = "1000.00",
            cashIn = "1000.00",
            salesman = "Deepak Route Rider"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val submittedReq = fakeRepo.lastCreateLineSaleRequest
        assertNotNull(submittedReq)
        assertEquals("Deepak Route Rider", submittedReq?.salesman)
    }

    @Test
    fun testCreateCounterSaleSubmitsSalesmanToBackend() = runTest {
        val vm = createViewModel(UserProfile(id = "u-1", name = "Kavita Raman"))

        // Create counter sale with default autofill
        vm.createCounterSale(
            item = "Beverage Crate",
            price = "800.00",
            totalAmount = "800.00",
            cash = "800.00",
            salesman = vm.defaultSalesmanName
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val submittedReq = fakeRepo.lastCreateCounterSaleRequest
        assertNotNull(submittedReq)
        assertEquals("Kavita Raman", submittedReq?.salesman)
    }

    @Test
    fun testLineSaleAndCounterSaleHaveIndependentSalesmanState() = runTest {
        val vm = createViewModel(UserProfile(id = "u-1", name = "Default User"))

        // Simulate Line Sale with custom salesman
        val lineReqResult = vm.validateAndBuildLineSaleRequest(
            customerId = "cust-1",
            totalAmount = "500.00",
            cashIn = "500.00",
            salesman = "Line Salesman A"
        )
        // Simulate Counter Sale with custom salesman
        val counterReqResult = vm.validateAndBuildCounterSaleRequest(
            item = "Snack Item",
            price = "100.00",
            totalAmount = "100.00",
            cash = "100.00",
            salesman = "Counter Cashier B"
        )

        assertEquals("Line Salesman A", lineReqResult.getOrNull()?.salesman)
        assertEquals("Counter Cashier B", counterReqResult.getOrNull()?.salesman)
    }

    @Test
    fun testJsonSerializationOfSalesmanInRequestsAndResponses() {
        // Line Sale Request
        val lineReq = CreateLineSaleRequest(
            customerId = "c-1",
            totalAmount = "500.00",
            totalCashIn = "500.00",
            salesman = "Rohan Sharma"
        )
        val lineJson = json.encodeToString(lineReq)
        assertTrue(lineJson.contains("\"salesman\":\"Rohan Sharma\""))

        // Counter Sale Request
        val counterReq = CreateCounterSaleRequest(
            item = "Grocery",
            price = "250.00",
            totalAmount = "250.00",
            cash = "250.00",
            salesman = "Kavita Raman"
        )
        val counterJson = json.encodeToString(counterReq)
        assertTrue(counterJson.contains("\"salesman\":\"Kavita Raman\""))

        // Counter Sale Update Request
        val updateCounterReq = UpdateCounterSaleRequest(
            salesman = "Updated Rep"
        )
        val updateJson = json.encodeToString(updateCounterReq)
        assertTrue(updateJson.contains("\"salesman\":\"Updated Rep\""))

        // Counter Sale API Response deserialization
        val responseJson = """
            {
                "id": "cs-42",
                "invoice_number": "INV-CS-042",
                "salesman": "Pooja Counter Rep",
                "item": "Rice 10kg",
                "total_amount": "600.00",
                "cash": "600.00"
            }
        """.trimIndent()
        val parsed = json.decodeFromString<CounterSale>(responseJson)
        assertEquals("Pooja Counter Rep", parsed.salesman)
        assertEquals("INV-CS-042", parsed.invoiceNumber)
    }
}
