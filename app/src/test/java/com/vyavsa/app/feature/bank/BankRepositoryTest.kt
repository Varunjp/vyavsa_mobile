package com.vyavsa.app.feature.bank

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.vyavsa.app.core.common.Resource
import com.vyavsa.app.core.model.CreateBankRequest
import com.vyavsa.app.core.model.UpdateBankRequest
import com.vyavsa.app.core.network.VyavsaApiService
import com.vyavsa.app.core.sync.DataMutation
import com.vyavsa.app.core.sync.DataRefreshCoordinator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit

@OptIn(ExperimentalCoroutinesApi::class)
class BankRepositoryTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var apiService: VyavsaApiService
    private lateinit var refreshCoordinator: DataRefreshCoordinator
    private lateinit var repository: BankRepository

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    @Before
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        val contentType = "application/json".toMediaType()
        val retrofit = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .client(OkHttpClient())
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()

        apiService = retrofit.create(VyavsaApiService::class.java)
        refreshCoordinator = DataRefreshCoordinator()
        repository = BankRepositoryImpl(apiService, refreshCoordinator)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun testListBanksSuccess() = runTest {
        val jsonBody = """
            {
              "success": true,
              "message": "banks retrieved",
              "data": [
                {
                  "id": "bank-1",
                  "tenant_id": "tenant-1",
                  "bank_name": "HDFC Bank",
                  "account_number": "123456789",
                  "ifsc": "HDFC0001234",
                  "current_balance": "50000.00",
                  "status": "active"
                }
              ]
            }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(jsonBody))

        val results = repository.listBanks().toList()
        assertTrue(results[0] is Resource.Loading)
        assertTrue(results[1] is Resource.Success)

        val success = results[1] as Resource.Success
        assertEquals(1, success.data.size)
        assertEquals("HDFC Bank", success.data[0].bankName)
        assertEquals("50000.00", success.data[0].currentBalance)

        val recordedRequest = mockWebServer.takeRequest()
        assertEquals("/api/v1/tenant/banks?page=1&page_size=100", recordedRequest.path)
        assertEquals("GET", recordedRequest.method)
    }

    @Test
    fun testCreateBankSuccessTriggersRefresh() = runTest {
        val mutations = mutableListOf<DataMutation>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            refreshCoordinator.refreshEvents.collect { mutations.add(it) }
        }

        val jsonBody = """
            {
              "success": true,
              "message": "bank created",
              "data": {
                "id": "bank-new",
                "bank_name": "ICICI Bank",
                "account_number": "999888777",
                "ifsc": "ICIC0001111",
                "current_balance": "10000.00",
                "status": "active"
              }
            }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(201).setBody(jsonBody))

        val req = CreateBankRequest(
            bankName = "ICICI Bank",
            accountNumber = "999888777",
            ifsc = "ICIC0001111",
            openingBalance = "10000.00"
        )

        val results = repository.createBank(req).toList()
        assertTrue(results[1] is Resource.Success)
        assertEquals("ICICI Bank", (results[1] as Resource.Success).data.bankName)

        val recordedRequest = mockWebServer.takeRequest()
        assertEquals("/api/v1/tenant/banks", recordedRequest.path)
        assertEquals("POST", recordedRequest.method)

        // Verify DataMutation.BANK was emitted for real-time sync
        assertEquals(listOf(DataMutation.BANK), mutations)

        job.cancel()
    }

    @Test
    fun testUpdateBankSuccessTriggersRefresh() = runTest {
        val mutations = mutableListOf<DataMutation>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            refreshCoordinator.refreshEvents.collect { mutations.add(it) }
        }

        val jsonBody = """
            {
              "success": true,
              "message": "bank updated",
              "data": {
                "id": "bank-1",
                "bank_name": "HDFC Bank Corporate",
                "account_number": "123456789",
                "ifsc": "HDFC0001234",
                "current_balance": "50000.00",
                "status": "active"
              }
            }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(jsonBody))

        val req = UpdateBankRequest(
            bankName = "HDFC Bank Corporate",
            accountNumber = "123456789",
            ifsc = "HDFC0001234",
            status = "active"
        )

        val results = repository.updateBank("bank-1", req).toList()
        assertTrue(results[1] is Resource.Success)

        val recordedRequest = mockWebServer.takeRequest()
        assertEquals("/api/v1/tenant/banks/bank-1", recordedRequest.path)
        assertEquals("PUT", recordedRequest.method)
        assertEquals(listOf(DataMutation.BANK), mutations)

        job.cancel()
    }

    @Test
    fun testDeleteBankSuccessTriggersRefresh() = runTest {
        val mutations = mutableListOf<DataMutation>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            refreshCoordinator.refreshEvents.collect { mutations.add(it) }
        }

        val jsonBody = """
            {
              "success": true,
              "message": "bank deleted",
              "data": null
            }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(jsonBody))

        val results = repository.deleteBank("bank-1").toList()
        assertTrue(results[1] is Resource.Success)

        val recordedRequest = mockWebServer.takeRequest()
        assertEquals("/api/v1/tenant/banks/bank-1", recordedRequest.path)
        assertEquals("DELETE", recordedRequest.method)
        assertEquals(listOf(DataMutation.BANK), mutations)

        job.cancel()
    }

    @Test
    fun testListBanksBusinessErrorHandling() = runTest {
        val errorJson = """
            {
              "success": false,
              "message": "Internal server error occurred",
              "error": {
                "code": "SERVER_ERROR",
                "message": "Internal server error occurred"
              }
            }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(errorJson))

        val results = repository.listBanks().toList()
        assertTrue(results[0] is Resource.Loading)
        assertTrue(results[1] is Resource.Error)
        assertEquals("Internal server error occurred", (results[1] as Resource.Error).message)
    }

    @Test
    fun testListBanksHttpErrorHandling() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(500).setBody("Server Error"))

        val results = repository.listBanks().toList()
        assertTrue(results[0] is Resource.Loading)
        assertTrue(results[1] is Resource.Error)
        assertTrue((results[1] as Resource.Error).message.contains("500"))
    }
}
