package com.vyavsa.app.core.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class FinancialMetrics(
    @SerialName("cash_balance") val cashBalance: String = "0.00",
    @SerialName("bank_balance") val bankBalance: String = "0.00",
    @SerialName("total_receivable") val totalReceivable: String = "0.00",
    @SerialName("total_payable") val totalPayable: String = "0.00",
    @SerialName("net_dues") val netDues: String = "0.00",
    @SerialName("net_receivables") val netReceivables: String = "0.00",
    @SerialName("pending_salary") val pendingSalary: String = "0.00",
    @SerialName("today_employee_salary") val todayEmployeeSalary: String = "0.00",
    @SerialName("bank_balances") val bankBalances: List<TenantBank> = emptyList(),
    @SerialName("today_overview") val todayOverview: TodayOverview? = null,
    @SerialName("is_current") val isCurrent: Boolean = true
)

@Immutable
@Serializable
data class TodayOverview(
    val attendance: TodayAttendanceOverview = TodayAttendanceOverview(),
    @SerialName("line_sale") val lineSale: String = "0.00",
    @SerialName("counter_sale") val counterSale: String = "0.00",
    @SerialName("employee_advance") val employeeAdvance: String = "0.00",
    @SerialName("outstanding_payable") val outstandingPayable: String = "0.00",
    val date: String = ""
)

@Immutable
@Serializable
data class TodayAttendanceOverview(
    val present: Int = 0,
    val total: Int = 0,
    val absent: Int = 0,
    @SerialName("has_records") val hasRecords: Boolean = false
)

@Immutable
@Serializable
data class TenantBank(
    val id: String = "",
    @SerialName("tenant_id") val tenantId: String = "",
    @SerialName("bank_name") val bankName: String = "",
    @SerialName("account_number") val accountNumber: String? = null,
    val ifsc: String? = null,
    @SerialName("current_balance") val currentBalance: String = "0.00",
    val status: String = "active",
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = ""
)

@Serializable
data class CreateBankRequest(
    @SerialName("bank_name") val bankName: String,
    @SerialName("account_number") val accountNumber: String? = null,
    val ifsc: String? = null,
    @SerialName("opening_balance") val openingBalance: String = "0.00",
    val status: String = "active"
)

@Serializable
data class UpdateBankRequest(
    @SerialName("bank_name") val bankName: String,
    @SerialName("account_number") val accountNumber: String? = null,
    val ifsc: String? = null,
    val status: String = "active"
)

@Immutable
@Serializable
data class TenantSubscription(
    val id: String = "",
    @SerialName("tenant_id") val tenantId: String = "",
    @SerialName("current_plan_id") val currentPlanId: String = "",
    @SerialName("current_plan_name") val currentPlanName: String = "",
    val price: String = "0.00",
    val note: String? = null,
    val status: String = "active",
    @SerialName("start_date") val startDate: String? = null,
    @SerialName("end_date") val endDate: String? = null,
    @SerialName("is_expired") val isExpired: Boolean = false,
    @SerialName("days_remaining") val daysRemaining: Int = 0
)

@Immutable
@Serializable
data class SubscriptionPlan(
    val id: String = "",
    @SerialName("plan_name") val planName: String = "",
    val name: String = "",
    val note: String? = null,
    val price: String = "0.00",
    @SerialName("duration_days") val durationDays: Int = 30,
    val features: List<String> = emptyList(),
    val status: String = "active"
) {
    val displayName: String
        get() = planName.ifBlank { name.ifBlank { "Subscription Plan" } }

    val displayDescription: String
        get() = note ?: "Full-featured small business billing & operations."
}

@Serializable
data class PurchasePlanRequest(
    @SerialName("plan_id") val planId: String,
    @SerialName("payment_method") val paymentMethod: String = "upi"
)

@Serializable
data class InitiateCheckoutRequest(
    @SerialName("plan_id") val planId: String,
    @SerialName("activation_mode") val activationMode: String? = "instant" // "instant" or "queue"
)

@Immutable
@Serializable
data class CheckoutCustomerInfo(
    val name: String = "",
    val email: String = "",
    val phone: String = ""
)

@Immutable
@Serializable
data class InitiateCheckoutResponse(
    val provider: String = "razorpay",
    @SerialName("key_id") val keyId: String = "",
    @SerialName("subscription_id") val subscriptionId: String = "",
    @SerialName("plan_id") val planId: String = "",
    @SerialName("plan_name") val planName: String = "",
    val amount: String = "0.00",
    val currency: String = "INR",
    @SerialName("billing_interval") val billingInterval: String = "monthly",
    @SerialName("activation_mode") val activationMode: String? = null,
    val customer: CheckoutCustomerInfo = CheckoutCustomerInfo()
)

@Serializable
data class VerifyPaymentRequest(
    @SerialName("plan_id") val planId: String,
    @SerialName("razorpay_payment_id") val razorpayPaymentId: String,
    @SerialName("razorpay_subscription_id") val razorpaySubscriptionId: String,
    @SerialName("razorpay_signature") val razorpaySignature: String,
    @SerialName("activation_mode") val activationMode: String? = "instant"
)

@Immutable
@Serializable
data class VerifyPaymentResponse(
    val subscription: TenantSubscription? = null,
    val transaction: PlanTransaction? = null
)

@Immutable
@Serializable
data class PlanTransaction(
    val id: String = "",
    @SerialName("tenant_id") val tenantId: String = "",
    @SerialName("transaction_id") val transactionId: String = "",
    @SerialName("payment_method") val paymentMethod: String = "",
    @SerialName("plan_id") val planId: String = "",
    @SerialName("plan_name") val planName: String = "",
    val amount: String = "0.00",
    val status: String = "completed",
    @SerialName("failure_reason") val failureReason: String? = null,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = ""
)

@Immutable
@Serializable
data class TenantLogoResponse(
    @SerialName("tenant_id") val tenantId: String? = null,
    @SerialName("logo_url") val logoUrl: String? = null,
    @SerialName("has_custom_logo") val hasCustomLogo: Boolean = false
)

@Immutable
@Serializable
data class TenantProfile(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String? = null,
    @SerialName("logo_url") val logoUrl: String? = null,
    @SerialName("address_line1") val addressLine1: String? = null,
    @SerialName("address_line2") val addressLine2: String? = null,
    val city: String? = null,
    val state: String? = null,
    @SerialName("postal_code") val postalCode: String? = null,
    val country: String? = null,
    @SerialName("formatted_address") val formattedAddress: String? = null,
    val address: String? = null,
    @SerialName("invoice_footer_heading") val invoiceFooterHeading: String? = null,
    @SerialName("invoice_footer_paragraph") val invoiceFooterParagraph: String? = null,
    val status: String = "active",
    @SerialName("created_at") val createdAt: String = ""
) {
    /**
     * Resolves the complete human-readable address with graceful fallbacks.
     */
    fun getEffectiveAddress(): String? {
        if (!formattedAddress.isNullOrBlank()) return formattedAddress.trim()
        if (!address.isNullOrBlank()) return address.trim()
        val parts = mutableListOf<String>()
        if (!addressLine1.isNullOrBlank()) parts.add(addressLine1.trim())
        if (!addressLine2.isNullOrBlank()) parts.add(addressLine2.trim())

        var cityStateZip = ""
        if (!city.isNullOrBlank()) cityStateZip = city.trim()
        if (!state.isNullOrBlank()) {
            cityStateZip = if (cityStateZip.isNotEmpty()) "$cityStateZip, ${state.trim()}" else state.trim()
        }
        if (!postalCode.isNullOrBlank()) {
            cityStateZip = if (cityStateZip.isNotEmpty()) "$cityStateZip - ${postalCode.trim()}" else postalCode.trim()
        }
        if (cityStateZip.isNotEmpty()) parts.add(cityStateZip)

        if (!country.isNullOrBlank()) parts.add(country.trim())

        return if (parts.isNotEmpty()) parts.joinToString(", ") else null
    }

    fun getEffectiveFooterHeading(): String {
        return invoiceFooterHeading?.trim()?.ifBlank { null } ?: "Thank You for Your Business!"
    }

    fun getEffectiveFooterParagraph(): String {
        return invoiceFooterParagraph?.trim()?.ifBlank { null } ?: "Thank you for choosing us. We appreciate your trust and look forward to serving you again."
    }
}

@Serializable
data class UpdateTenantProfileRequest(
    val name: String,
    val email: String,
    val phone: String? = null,
    @SerialName("logo_url") val logoUrl: String? = null,
    @SerialName("address_line1") val addressLine1: String? = null,
    @SerialName("address_line2") val addressLine2: String? = null,
    val city: String? = null,
    val state: String? = null,
    @SerialName("postal_code") val postalCode: String? = null,
    val country: String? = null,
    @SerialName("invoice_footer_heading") val invoiceFooterHeading: String? = null,
    @SerialName("invoice_footer_paragraph") val invoiceFooterParagraph: String? = null
)

@Immutable
@Serializable
data class SalesDashboardResponse(
    @SerialName("business_date") val businessDate: String = "",
    val currency: String = "INR",
    @SerialName("total_cash_collected") val totalCashCollected: String = "0.00",
    @SerialName("total_bank_received") val totalBankReceived: String = "0.00",
    @SerialName("total_sales_count") val totalSalesCount: Long = 0,
    @SerialName("total_sales_amount") val totalSalesAmount: String = "0.00",
    @SerialName("outstanding_dues") val outstandingDues: String = "0.00"
)

@Immutable
@Serializable
data class OfficeDashboardResponse(
    @SerialName("business_date") val businessDate: String = "",
    val currency: String = "INR",
    @SerialName("total_cash_collected") val totalCashCollected: String = "0.00",
    @SerialName("total_bank_received") val totalBankReceived: String = "0.00",
    @SerialName("total_attendance") val totalAttendance: Int = 0,
    @SerialName("present_count") val presentCount: Int = 0,
    @SerialName("absent_count") val absentCount: Int = 0,
    @SerialName("half_day_count") val halfDayCount: Int = 0,
    @SerialName("total_active_employees") val totalActiveEmployees: Int = 0,
    @SerialName("total_sales_count") val totalSalesCount: Long = 0,
    @SerialName("total_sales_amount") val totalSalesAmount: String = "0.00"
)
