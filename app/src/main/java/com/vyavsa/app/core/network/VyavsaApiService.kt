package com.vyavsa.app.core.network

import com.vyavsa.app.core.model.AddCashRequest
import com.vyavsa.app.core.model.AdjustCashRequest
import com.vyavsa.app.core.model.ApiResponse
import com.vyavsa.app.core.model.AttendanceRecord
import com.vyavsa.app.core.model.CashBalanceResponse
import com.vyavsa.app.core.model.CashTransaction
import com.vyavsa.app.core.model.CounterSale
import com.vyavsa.app.core.model.CreateCounterSaleRequest
import com.vyavsa.app.core.model.CreateCustomerRequest
import com.vyavsa.app.core.model.CreateEmployeeRequest
import com.vyavsa.app.core.model.CreateExpenseRequest
import com.vyavsa.app.core.model.CreateLineSaleRequest
import com.vyavsa.app.core.model.CreatePurchaseRequest
import com.vyavsa.app.core.model.Customer
import com.vyavsa.app.core.model.CustomerBalance
import com.vyavsa.app.core.model.CustomerStatement
import com.vyavsa.app.core.model.Employee
import com.vyavsa.app.core.model.EmployeeSalary
import com.vyavsa.app.core.model.Expense
import com.vyavsa.app.core.model.FinancialMetrics
import com.vyavsa.app.core.model.ForgotPasswordRequest
import com.vyavsa.app.core.model.LineSale
import com.vyavsa.app.core.model.PaySalaryRequest
import com.vyavsa.app.core.model.Purchase
import com.vyavsa.app.core.model.RecordAttendanceRequest
import com.vyavsa.app.core.model.RecordPaymentRequest
import com.vyavsa.app.core.model.RefreshTokenRequest
import com.vyavsa.app.core.model.ResetPasswordRequest
import com.vyavsa.app.core.model.SubscriptionPlan
import com.vyavsa.app.core.model.TenantLoginRequest
import com.vyavsa.app.core.model.TenantProfile
import com.vyavsa.app.core.model.TenantRegisterRequest
import com.vyavsa.app.core.model.TenantSubscription
import com.vyavsa.app.core.model.TokenResponse
import com.vyavsa.app.core.model.UpdateTenantProfileRequest
import com.vyavsa.app.core.model.UserProfile
import com.vyavsa.app.core.model.VerifyResetOtpRequest
import com.vyavsa.app.core.model.VerifyResetOtpResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface VyavsaApiService {

    // ==========================================
    // 1. Authentication
    // ==========================================

    @POST("api/v1/auth/tenant/login")
    suspend fun loginTenant(@Body req: TenantLoginRequest): ApiResponse<TokenResponse>

    @POST("api/v1/auth/tenant/register")
    suspend fun registerTenant(@Body req: TenantRegisterRequest): ApiResponse<TenantProfile>

    @POST("api/v1/auth/refresh")
    suspend fun refreshToken(@Body req: RefreshTokenRequest): ApiResponse<TokenResponse>

    @POST("api/v1/auth/logout")
    suspend fun logout(@Body req: RefreshTokenRequest? = null): ApiResponse<Map<String, Boolean>>

    @GET("api/v1/auth/me")
    suspend fun getMe(): ApiResponse<UserProfile>

    @POST("api/v1/auth/forgot-password")
    suspend fun forgotPassword(@Body req: ForgotPasswordRequest): ApiResponse<String?>

    @POST("api/v1/auth/verify-reset-otp")
    suspend fun verifyResetOtp(@Body req: VerifyResetOtpRequest): ApiResponse<VerifyResetOtpResponse>

    @POST("api/v1/auth/reset-password")
    suspend fun resetPassword(@Body req: ResetPasswordRequest): ApiResponse<String?>

    @GET("api/v1/plans")
    suspend fun getPublicPlans(): ApiResponse<List<SubscriptionPlan>>

    // ==========================================
    // 2. Dashboard, Profile & Subscription
    // ==========================================

    @GET("api/v1/tenant/dashboard")
    suspend fun getDashboardMetrics(): ApiResponse<FinancialMetrics>

    @GET("api/v1/tenant/profile")
    suspend fun getProfile(): ApiResponse<TenantProfile>

    @PUT("api/v1/tenant/profile")
    suspend fun updateProfile(@Body req: UpdateTenantProfileRequest): ApiResponse<TenantProfile>

    @GET("api/v1/tenant/subscription")
    suspend fun getSubscription(): ApiResponse<TenantSubscription>

    // ==========================================
    // 3. Line Sales & Counter Sales
    // ==========================================

    @GET("api/v1/tenant/line-sales")
    suspend fun listLineSales(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20,
        @Query("date") date: String? = null,
        @Query("search") search: String? = null
    ): ApiResponse<List<LineSale>>

    @POST("api/v1/tenant/line-sales")
    suspend fun createLineSale(@Body req: CreateLineSaleRequest): ApiResponse<LineSale>

    @GET("api/v1/tenant/counter-sales")
    suspend fun listCounterSales(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20,
        @Query("date") date: String? = null,
        @Query("search") search: String? = null
    ): ApiResponse<List<CounterSale>>

    @POST("api/v1/tenant/counter-sales")
    suspend fun createCounterSale(@Body req: CreateCounterSaleRequest): ApiResponse<CounterSale>

    // ==========================================
    // 4. Purchases & Customers / Dues
    // ==========================================

    @GET("api/v1/tenant/purchases")
    suspend fun listPurchases(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20,
        @Query("date") date: String? = null,
        @Query("search") search: String? = null
    ): ApiResponse<List<Purchase>>

    @POST("api/v1/tenant/purchases")
    suspend fun createPurchase(@Body req: CreatePurchaseRequest): ApiResponse<Purchase>

    @GET("api/v1/tenant/customers")
    suspend fun listCustomers(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 50,
        @Query("search") search: String? = null
    ): ApiResponse<List<Customer>>

    @POST("api/v1/tenant/customers")
    suspend fun createCustomer(@Body req: CreateCustomerRequest): ApiResponse<Customer>

    @GET("api/v1/tenant/customers/{id}/balance")
    suspend fun getCustomerBalance(@Path("id") id: String): ApiResponse<CustomerBalance>

    @GET("api/v1/tenant/customers/{id}/statement")
    suspend fun getCustomerStatement(@Path("id") id: String): ApiResponse<CustomerStatement>

    @POST("api/v1/tenant/customers/{id}/payments")
    suspend fun recordCustomerPayment(
        @Path("id") id: String,
        @Body req: RecordPaymentRequest
    ): ApiResponse<Map<String, String>>

    // ==========================================
    // 5. Expenses
    // ==========================================

    @GET("api/v1/tenant/expenses")
    suspend fun listExpenses(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20,
        @Query("date") date: String? = null,
        @Query("search") search: String? = null
    ): ApiResponse<List<Expense>>

    @POST("api/v1/tenant/expenses")
    suspend fun createExpense(@Body req: CreateExpenseRequest): ApiResponse<Expense>

    // ==========================================
    // 6. Employees, Attendance & Salary
    // ==========================================

    @GET("api/v1/tenant/employees")
    suspend fun listEmployees(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 50,
        @Query("search") search: String? = null
    ): ApiResponse<List<Employee>>

    @POST("api/v1/tenant/employees")
    suspend fun createEmployee(@Body req: CreateEmployeeRequest): ApiResponse<Employee>

    @GET("api/v1/tenant/attendance")
    suspend fun listAttendance(@Query("date") date: String? = null): ApiResponse<List<AttendanceRecord>>

    @POST("api/v1/tenant/attendance")
    suspend fun recordAttendance(@Body req: RecordAttendanceRequest): ApiResponse<AttendanceRecord>

    @GET("api/v1/tenant/salaries/pending")
    suspend fun listPendingSalaries(): ApiResponse<List<EmployeeSalary>>

    @POST("api/v1/tenant/salaries/{employee_id}/pay")
    suspend fun paySalary(
        @Path("employee_id") employeeId: String,
        @Body req: PaySalaryRequest
    ): ApiResponse<Map<String, String>>

    // ==========================================
    // 7. Cash Drawer Management
    // ==========================================

    @GET("api/v1/tenant/cash/balance")
    suspend fun getCashBalance(): ApiResponse<CashBalanceResponse>

    @GET("api/v1/tenant/cash/transactions")
    suspend fun listCashTransactions(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20
    ): ApiResponse<List<CashTransaction>>

    @POST("api/v1/tenant/cash/add")
    suspend fun addCash(@Body req: AddCashRequest): ApiResponse<CashTransaction>

    @POST("api/v1/tenant/cash/adjust")
    suspend fun adjustCash(@Body req: AdjustCashRequest): ApiResponse<CashTransaction>
}
