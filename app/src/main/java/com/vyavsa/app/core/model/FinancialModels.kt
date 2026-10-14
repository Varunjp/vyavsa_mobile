package com.vyavsa.app.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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

@Serializable
data class TodayOverview(
    val attendance: TodayAttendanceOverview = TodayAttendanceOverview(),
    @SerialName("line_sale") val lineSale: String = "0.00",
    @SerialName("counter_sale") val counterSale: String = "0.00",
    @SerialName("employee_advance") val employeeAdvance: String = "0.00",
    @SerialName("outstanding_payable") val outstandingPayable: String = "0.00",
    val date: String = ""
)

@Serializable
data class TodayAttendanceOverview(
    val present: Int = 0,
    val total: Int = 0,
    val absent: Int = 0,
    @SerialName("has_records") val hasRecords: Boolean = false
)

@Serializable
data class TenantBank(
    val id: String = "",
    @SerialName("bank_name") val bankName: String = "",
    @SerialName("account_number") val accountNumber: String? = null,
    val ifsc: String? = null,
    @SerialName("current_balance") val currentBalance: String = "0.00",
    val status: String = "active"
)

@Serializable
data class TenantSubscription(
    val id: String = "",
    @SerialName("tenant_id") val tenantId: String = "",
    @SerialName("current_plan_id") val currentPlanId: String = "",
    @SerialName("current_plan_name") val currentPlanName: String = "",
    val price: String = "0.00",
    val status: String = "active",
    @SerialName("start_date") val startDate: String? = null,
    @SerialName("end_date") val endDate: String? = null,
    @SerialName("is_expired") val isExpired: Boolean = false,
    @SerialName("days_remaining") val daysRemaining: Int = 0
)

@Serializable
data class SubscriptionPlan(
    val id: String = "",
    val name: String = "",
    val price: String = "0.00",
    @SerialName("duration_days") val durationDays: Int = 30,
    val features: List<String> = emptyList(),
    val status: String = "active"
)

@Serializable
data class TenantProfile(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String? = null,
    val status: String = "active",
    @SerialName("created_at") val createdAt: String = ""
)

@Serializable
data class UpdateTenantProfileRequest(
    val name: String,
    val email: String,
    val phone: String? = null
)
