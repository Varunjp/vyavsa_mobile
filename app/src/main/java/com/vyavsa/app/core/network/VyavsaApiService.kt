package com.vyavsa.app.core.network

import com.vyavsa.app.core.model.AddCashRequest
import com.vyavsa.app.core.model.AdjustCashRequest
import com.vyavsa.app.core.model.AdjustCustomerBalanceRequest
import com.vyavsa.app.core.model.ApiResponse
import com.vyavsa.app.core.model.AttendanceRecord
import com.vyavsa.app.core.model.BankTransaction
import com.vyavsa.app.core.model.CashBalanceResponse
import com.vyavsa.app.core.model.CashTransaction
import com.vyavsa.app.core.model.CounterSale
import com.vyavsa.app.core.model.CounterSalePayment
import com.vyavsa.app.core.model.CreateBankRequest
import com.vyavsa.app.core.model.CreateCounterSaleRequest
import com.vyavsa.app.core.model.CreateCustomerRequest
import com.vyavsa.app.core.model.CreateEmployeeRequest
import com.vyavsa.app.core.model.CreateExpenseRequest
import com.vyavsa.app.core.model.CreateLineSaleRequest
import com.vyavsa.app.core.model.CreatePurchaseRequest
import com.vyavsa.app.core.model.CreateTenantUserRequest
import com.vyavsa.app.core.model.Customer
import com.vyavsa.app.core.model.CustomerBalance
import com.vyavsa.app.core.model.CustomerBalanceAdjustment
import com.vyavsa.app.core.model.CustomerStatement
import com.vyavsa.app.core.model.Employee
import com.vyavsa.app.core.model.EmployeeSalary
import com.vyavsa.app.core.model.EmployeeSalaryStatement
import com.vyavsa.app.core.model.Expense
import com.vyavsa.app.core.model.FinancialMetrics
import com.vyavsa.app.core.model.ForgotPasswordRequest
import com.vyavsa.app.core.model.LineSale
import com.vyavsa.app.core.model.LineSalePayment
import com.vyavsa.app.core.model.PaySalaryRequest
import com.vyavsa.app.core.model.PlanTransaction
import com.vyavsa.app.core.model.Purchase
import com.vyavsa.app.core.model.PurchasePayment
import com.vyavsa.app.core.model.PurchasePlanRequest
import com.vyavsa.app.core.model.RecordSupplierPaymentRequest
import com.vyavsa.app.core.model.SupplierPaymentResponse
import com.vyavsa.app.core.model.UpdatePurchaseRequest
import com.vyavsa.app.core.model.RecordAttendanceRequest
import com.vyavsa.app.core.model.RecordPaymentRequest
import com.vyavsa.app.core.model.RecordSalePaymentRequest
import com.vyavsa.app.core.model.RefreshTokenRequest
import com.vyavsa.app.core.model.ResetPasswordRequest
import com.vyavsa.app.core.model.SubscriptionPlan
import com.vyavsa.app.core.model.TenantBank
import com.vyavsa.app.core.model.TenantLoginRequest
import com.vyavsa.app.core.model.TenantProfile
import com.vyavsa.app.core.model.TenantRegisterRequest
import com.vyavsa.app.core.model.TenantSubscription
import com.vyavsa.app.core.model.TenantUser
import com.vyavsa.app.core.model.TokenResponse
import com.vyavsa.app.core.model.UpdateBankRequest
import com.vyavsa.app.core.model.UpdateCustomerRequest
import com.vyavsa.app.core.model.UpdateEmployeeRequest
import com.vyavsa.app.core.model.UpdateTenantProfileRequest
import com.vyavsa.app.core.model.UpdateTenantUserRequest
import com.vyavsa.app.core.model.UserProfile
import com.vyavsa.app.core.model.VerifyResetOtpRequest
import com.vyavsa.app.core.model.VerifyResetOtpResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
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

    @GET("api/v1/tenant/subscription/plans")
    suspend fun listAvailablePlans(): ApiResponse<List<SubscriptionPlan>>

    @POST("api/v1/tenant/subscription/purchase")
    suspend fun purchasePlan(@Body req: PurchasePlanRequest): ApiResponse<TenantSubscription>

    @GET("api/v1/tenant/subscription/transactions")
    suspend fun listSubscriptionTransactions(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 50
    ): ApiResponse<List<PlanTransaction>>

    // ==========================================
    // 3. Line Sales & Counter Sales
    // ==========================================

    @GET("api/v1/tenant/line-sales")
    suspend fun listLineSales(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20,
        @Query("date") date: String? = null,
        @Query("search") search: String? = null,
        @Query("customer_id") customerId: String? = null
    ): ApiResponse<List<LineSale>>

    @POST("api/v1/tenant/line-sales")
    suspend fun createLineSale(@Body req: CreateLineSaleRequest): ApiResponse<LineSale>

    @GET("api/v1/tenant/line-sales/{id}")
    suspend fun getLineSaleById(@Path("id") id: String): ApiResponse<LineSale>

    @GET("api/v1/tenant/line-sales/{id}/payments")
    suspend fun listLineSalePayments(@Path("id") id: String): ApiResponse<List<LineSalePayment>>

    @POST("api/v1/tenant/line-sales/{id}/payments")
    suspend fun recordLineSalePayment(
        @Path("id") id: String,
        @Body req: RecordSalePaymentRequest
    ): ApiResponse<LineSalePayment>

    @GET("api/v1/tenant/counter-sales")
    suspend fun listCounterSales(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20,
        @Query("date") date: String? = null,
        @Query("search") search: String? = null
    ): ApiResponse<List<CounterSale>>

    @POST("api/v1/tenant/counter-sales")
    suspend fun createCounterSale(@Body req: CreateCounterSaleRequest): ApiResponse<CounterSale>

    @GET("api/v1/tenant/counter-sales/{id}")
    suspend fun getCounterSaleById(@Path("id") id: String): ApiResponse<CounterSale>

    @GET("api/v1/tenant/counter-sales/{id}/payments")
    suspend fun listCounterSalePayments(@Path("id") id: String): ApiResponse<List<CounterSalePayment>>

    @POST("api/v1/tenant/counter-sales/{id}/payments")
    suspend fun recordCounterSalePayment(
        @Path("id") id: String,
        @Body req: RecordSalePaymentRequest
    ): ApiResponse<CounterSalePayment>

    // ==========================================
    // 4. Purchases & Customers / Dues
    // ==========================================

    @GET("api/v1/tenant/purchases")
    suspend fun listPurchases(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20,
        @Query("date") date: String? = null,
        @Query("search") search: String? = null,
        @Query("customer_id") customerId: String? = null
    ): ApiResponse<List<Purchase>>

    @POST("api/v1/tenant/purchases")
    suspend fun createPurchase(@Body req: CreatePurchaseRequest): ApiResponse<Purchase>

    @GET("api/v1/tenant/purchases/{id}")
    suspend fun getPurchaseById(@Path("id") id: String): ApiResponse<Purchase>

    @PUT("api/v1/tenant/purchases/{id}")
    suspend fun updatePurchase(
        @Path("id") id: String,
        @Body req: UpdatePurchaseRequest
    ): ApiResponse<Purchase>

    @DELETE("api/v1/tenant/purchases/{id}")
    suspend fun deletePurchase(@Path("id") id: String): ApiResponse<Map<String, Boolean>>

    @GET("api/v1/tenant/purchases/{id}/payments")
    suspend fun listPurchasePayments(@Path("id") id: String): ApiResponse<List<PurchasePayment>>

    @POST("api/v1/tenant/purchases/{id}/payments")
    suspend fun recordPurchaseSettlementPayment(
        @Path("id") id: String,
        @Body req: RecordSupplierPaymentRequest
    ): ApiResponse<SupplierPaymentResponse>

    @GET("api/v1/tenant/customers")
    suspend fun listCustomers(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 50,
        @Query("search") search: String? = null
    ): ApiResponse<List<Customer>>

    @POST("api/v1/tenant/customers")
    suspend fun createCustomer(@Body req: CreateCustomerRequest): ApiResponse<Customer>

    @GET("api/v1/tenant/customers/{id}")
    suspend fun getCustomerById(@Path("id") id: String): ApiResponse<Customer>

    @PUT("api/v1/tenant/customers/{id}")
    suspend fun updateCustomer(
        @Path("id") id: String,
        @Body req: UpdateCustomerRequest
    ): ApiResponse<Customer>

    @DELETE("api/v1/tenant/customers/{id}")
    suspend fun deleteCustomer(@Path("id") id: String): ApiResponse<Map<String, Boolean>>

    @POST("api/v1/tenant/customers/{id}/adjust-balance")
    suspend fun adjustCustomerBalance(
        @Path("id") id: String,
        @Body req: AdjustCustomerBalanceRequest
    ): ApiResponse<CustomerBalanceAdjustment>

    @GET("api/v1/tenant/customers/{id}/adjustments")
    suspend fun listCustomerAdjustments(@Path("id") id: String): ApiResponse<List<CustomerBalanceAdjustment>>

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

    @GET("api/v1/tenant/employees/{id}")
    suspend fun getEmployeeById(@Path("id") id: String): ApiResponse<Employee>

    @PUT("api/v1/tenant/employees/{id}")
    suspend fun updateEmployee(
        @Path("id") id: String,
        @Body req: UpdateEmployeeRequest
    ): ApiResponse<Employee>

    @DELETE("api/v1/tenant/employees/{id}")
    suspend fun deleteEmployee(@Path("id") id: String): ApiResponse<Map<String, Boolean>>

    @GET("api/v1/tenant/attendance")
    suspend fun listAttendance(@Query("date") date: String? = null): ApiResponse<List<AttendanceRecord>>

    @POST("api/v1/tenant/attendance")
    suspend fun recordAttendance(@Body req: RecordAttendanceRequest): ApiResponse<AttendanceRecord>

    @GET("api/v1/tenant/salaries/pending")
    suspend fun listPendingSalaries(): ApiResponse<List<EmployeeSalary>>

    @GET("api/v1/tenant/salaries/{employee_id}/statement")
    suspend fun getEmployeeSalaryStatement(@Path("employee_id") employeeId: String): ApiResponse<EmployeeSalaryStatement>

    @POST("api/v1/tenant/salaries/{employee_id}/pay")
    suspend fun paySalary(
        @Path("employee_id") employeeId: String,
        @Body req: PaySalaryRequest
    ): ApiResponse<Map<String, String>>

    // ==========================================
    // 7. Tenant Users Management (Admin only)
    // ==========================================

    @GET("api/v1/tenant/users")
    suspend fun listTenantUsers(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 50,
        @Query("search") search: String? = null,
        @Query("role") role: String? = null,
        @Query("status") status: String? = null
    ): ApiResponse<List<TenantUser>>

    @POST("api/v1/tenant/users")
    suspend fun createTenantUser(@Body req: CreateTenantUserRequest): ApiResponse<TenantUser>

    @GET("api/v1/tenant/users/{id}")
    suspend fun getTenantUserById(@Path("id") id: String): ApiResponse<TenantUser>

    @PUT("api/v1/tenant/users/{id}")
    suspend fun updateTenantUser(
        @Path("id") id: String,
        @Body req: UpdateTenantUserRequest
    ): ApiResponse<TenantUser>

    @DELETE("api/v1/tenant/users/{id}")
    suspend fun deleteTenantUser(@Path("id") id: String): ApiResponse<Map<String, Boolean>>

    // ==========================================
    // 8. Cash Drawer Management
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

    // ==========================================
    // 9. Bank Management & Transactions
    // ==========================================

    @GET("api/v1/tenant/banks")
    suspend fun listBanks(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 100,
        @Query("search") search: String? = null,
        @Query("status") status: String? = null
    ): ApiResponse<List<TenantBank>>

    @POST("api/v1/tenant/banks")
    suspend fun createBank(@Body req: CreateBankRequest): ApiResponse<TenantBank>

    @GET("api/v1/tenant/banks/{id}")
    suspend fun getBankById(@Path("id") id: String): ApiResponse<TenantBank>

    @PUT("api/v1/tenant/banks/{id}")
    suspend fun updateBank(
        @Path("id") id: String,
        @Body req: UpdateBankRequest
    ): ApiResponse<TenantBank>

    @DELETE("api/v1/tenant/banks/{id}")
    suspend fun deleteBank(@Path("id") id: String): ApiResponse<Map<String, Boolean>>

    @GET("api/v1/tenant/banks/{id}/transactions")
    suspend fun listBankTransactions(@Path("id") bankId: String): ApiResponse<List<BankTransaction>>
}
