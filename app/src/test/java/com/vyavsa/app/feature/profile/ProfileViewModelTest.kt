package com.vyavsa.app.feature.profile

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.vyavsa.app.core.common.Resource
import com.vyavsa.app.core.common.UiState
import com.vyavsa.app.core.model.SubscriptionPlan
import com.vyavsa.app.core.model.TenantProfile
import com.vyavsa.app.core.model.TokenResponse
import com.vyavsa.app.core.network.VyavsaApiService
import com.vyavsa.app.core.sync.DataRefreshCoordinator
import com.vyavsa.app.feature.auth.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockWebServer: MockWebServer
    private lateinit var apiService: VyavsaApiService
    private lateinit var fakeAuthRepo: FakeAuthRepository
    private lateinit var refreshCoordinator: DataRefreshCoordinator
    private lateinit var viewModel: ProfileViewModel

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private class FakeAuthRepository : AuthRepository {
        override fun login(email: String, password: String): Flow<Resource<TokenResponse>> = flow { }
        override fun register(name: String, adminName: String?, email: String, password: String, phone: String?, planId: String?): Flow<Resource<TenantProfile>> = flow { }
        override fun forgotPassword(email: String): Flow<Resource<String>> = flow { }
        override fun verifyResetOtp(email: String, otp: String): Flow<Resource<String>> = flow { }
        override fun resetPassword(resetToken: String, newPassword: String): Flow<Resource<Unit>> = flow { }
        override fun logout(): Flow<Resource<Unit>> = flow { emit(Resource.Success(Unit)) }
        override fun getPublicPlans(): Flow<Resource<List<SubscriptionPlan>>> = flow { emit(Resource.Success(emptyList())) }
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockWebServer = MockWebServer()

        val dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return when {
                    request.path == "/api/v1/tenant/profile" && request.method == "GET" -> {
                        MockResponse().setResponseCode(200).setBody("""
                            {
                              "success": true,
                              "data": {
                                "id": "t-1",
                                "name": "Vyavsa Hardware",
                                "email": "hardware@vyavsa.com",
                                "phone": "9876543210"
                              }
                            }
                        """.trimIndent())
                    }
                    request.path == "/api/v1/tenant/subscription" && request.method == "GET" -> {
                        MockResponse().setResponseCode(200).setBody("""
                            {
                              "success": true,
                              "data": {
                                "id": "sub-1",
                                "plan_name": "Standard",
                                "status": "active"
                              }
                            }
                        """.trimIndent())
                    }
                    request.path == "/api/v1/tenant/profile" && request.method == "PUT" -> {
                        val body = request.body.clone().readUtf8()
                        val hasCustomFooter = body.contains("invoice_footer_heading")
                        val heading = if (hasCustomFooter) "Custom Thank You" else null
                        val paragraph = if (hasCustomFooter) "Goods once sold cannot be returned." else null
                        MockResponse().setResponseCode(200).setBody("""
                            {
                              "success": true,
                              "message": "profile updated successfully",
                              "data": {
                                "id": "t-1",
                                "name": "Vyavsa Hardware & Tools",
                                "email": "hardware@vyavsa.com",
                                "phone": "9876543210",
                                "address_line1": "Shop 4, Main Bazaar",
                                "address_line2": "Behind Town Hall",
                                "city": "Jaipur",
                                "state": "Rajasthan",
                                "postal_code": "302001",
                                "country": "India",
                                "invoice_footer_heading": ${if (heading != null) "\"$heading\"" else "null"},
                                "invoice_footer_paragraph": ${if (paragraph != null) "\"$paragraph\"" else "null"},
                                "formatted_address": "Shop 4, Main Bazaar, Behind Town Hall, Jaipur, Rajasthan, 302001, India"
                              }
                            }
                        """.trimIndent())
                    }
                    else -> MockResponse().setResponseCode(404)
                }
            }
        }
        mockWebServer.dispatcher = dispatcher
        mockWebServer.start()

        val contentType = "application/json".toMediaType()
        val retrofit = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .client(OkHttpClient())
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()

        apiService = retrofit.create(VyavsaApiService::class.java)
        fakeAuthRepo = FakeAuthRepository()
        refreshCoordinator = DataRefreshCoordinator()

        viewModel = ProfileViewModel(
            apiService = apiService,
            authRepository = fakeAuthRepo,
            refreshCoordinator = refreshCoordinator,
            tokenManager = null
        )
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
        Dispatchers.resetMain()
    }

    @Test
    fun testUpdateProfileWithCompanyAddressSuccess() = runTest {
        var initAttempts = 0
        while (viewModel.profileState.value !is UiState.Success && initAttempts < 50) {
            Thread.sleep(20)
            testDispatcher.scheduler.advanceUntilIdle()
            initAttempts++
        }

        viewModel.updateProfile(
            name = "Vyavsa Hardware & Tools",
            email = "hardware@vyavsa.com",
            phone = "9876543210",
            addressLine1 = "Shop 4, Main Bazaar",
            addressLine2 = "Behind Town Hall",
            city = "Jaipur",
            state = "Rajasthan",
            postalCode = "302001",
            country = "India"
        )

        testDispatcher.scheduler.advanceUntilIdle()

        var attempts = 0
        while (viewModel.updateState.value !is UiState.Success && attempts < 50) {
            Thread.sleep(20)
            testDispatcher.scheduler.advanceUntilIdle()
            attempts++
        }

        val updateState = viewModel.updateState.value
        assertTrue("Expected Success, got $updateState", updateState is UiState.Success)

        val profileState = viewModel.profileState.value
        assertTrue("Expected Success, got $profileState", profileState is UiState.Success)
        val profile = (profileState as UiState.Success).data
        assertEquals("Vyavsa Hardware & Tools", profile.name)
        assertEquals("Shop 4, Main Bazaar", profile.addressLine1)
        assertEquals("Behind Town Hall", profile.addressLine2)
        assertEquals("Jaipur", profile.city)
        assertEquals("Rajasthan", profile.state)
        assertEquals("302001", profile.postalCode)
        assertEquals("India", profile.country)
        assertEquals("Shop 4, Main Bazaar, Behind Town Hall, Jaipur, Rajasthan, 302001, India", profile.getEffectiveAddress())
    }

    @Test
    fun testUpdateProfileValidationPostalCodeTooLong() = runTest {
        viewModel.updateProfile(
            name = "Vyavsa",
            email = "test@vyavsa.com",
            postalCode = "123456789012345678901" // 21 chars
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val updateState = viewModel.updateState.value
        assertTrue("Expected Error state, got $updateState", updateState is UiState.Error)
        assertEquals("Postal code cannot exceed 20 characters", (updateState as UiState.Error).message)
    }

    @Test
    fun testUpdateProfileValidationAddressLine1TooLong() = runTest {
        viewModel.updateProfile(
            name = "Vyavsa",
            email = "test@vyavsa.com",
            addressLine1 = "A".repeat(256)
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val updateState = viewModel.updateState.value
        assertTrue("Expected Error state, got $updateState", updateState is UiState.Error)
        assertEquals("Address line 1 cannot exceed 255 characters", (updateState as UiState.Error).message)
    }

    @Test
    fun testUpdateProfileValidationBlankName() = runTest {
        viewModel.updateProfile(
            name = "   ",
            email = "test@vyavsa.com"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val updateState = viewModel.updateState.value
        assertTrue("Expected Error state, got $updateState", updateState is UiState.Error)
        assertEquals("Name and email are required", (updateState as UiState.Error).message)
    }

    @Test
    fun testUpdateProfileValidationBlankEmail() = runTest {
        viewModel.updateProfile(
            name = "Vyavsa",
            email = "   "
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val updateState = viewModel.updateState.value
        assertTrue("Expected Error state, got $updateState", updateState is UiState.Error)
        assertEquals("Name and email are required", (updateState as UiState.Error).message)
    }

    @Test
    fun testUpdateInvoiceFooterValidationHeadingTooLong() = runTest {
        viewModel.updateInvoiceFooter(
            heading = "A".repeat(256),
            paragraph = "Valid footer paragraph"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val footerState = viewModel.footerUpdateState.value
        assertTrue("Expected Error state, got $footerState", footerState is UiState.Error)
        assertEquals("Footer heading cannot exceed 255 characters", (footerState as UiState.Error).message)
    }

    @Test
    fun testUpdateInvoiceFooterValidationParagraphTooLong() = runTest {
        viewModel.updateInvoiceFooter(
            heading = "Valid heading",
            paragraph = "B".repeat(1001)
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val footerState = viewModel.footerUpdateState.value
        assertTrue("Expected Error state, got $footerState", footerState is UiState.Error)
        assertEquals("Footer paragraph cannot exceed 1000 characters", (footerState as UiState.Error).message)
    }

    @Test
    fun testUpdateInvoiceFooterSuccess() = runTest {
        var initAttempts = 0
        while (viewModel.profileState.value !is UiState.Success && initAttempts < 50) {
            Thread.sleep(20)
            testDispatcher.scheduler.advanceUntilIdle()
            initAttempts++
        }

        viewModel.updateInvoiceFooter(
            heading = "Custom Thank You",
            paragraph = "Goods once sold cannot be returned."
        )
        testDispatcher.scheduler.advanceUntilIdle()

        var attempts = 0
        while (viewModel.footerUpdateState.value !is UiState.Success && attempts < 50) {
            Thread.sleep(20)
            testDispatcher.scheduler.advanceUntilIdle()
            attempts++
        }

        val footerState = viewModel.footerUpdateState.value
        assertTrue("Expected Success, got $footerState", footerState is UiState.Success)

        val profileState = viewModel.profileState.value
        assertTrue("Expected Success, got $profileState", profileState is UiState.Success)
        val profile = (profileState as UiState.Success).data
        assertEquals("Custom Thank You", profile.getEffectiveFooterHeading())
        assertEquals("Goods once sold cannot be returned.", profile.getEffectiveFooterParagraph())
    }
}
