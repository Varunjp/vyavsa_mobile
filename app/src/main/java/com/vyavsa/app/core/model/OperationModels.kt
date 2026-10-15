package com.vyavsa.app.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ==========================================
// 1. Sales Models
// ==========================================

@Serializable
data class LineSale(
    val id: String = "",
    @SerialName("customer_id") val customerId: String = "",
    @SerialName("customer_name") val customerName: String = "",
    val route: String? = null,
    val salesman: String? = null,
    val note: String? = null,
    @SerialName("total_amount") val totalAmount: String = "0.00",
    @SerialName("total_cash_in") val totalCashIn: String = "0.00",
    @SerialName("bank_amount") val bankAmount: String = "0.00",
    @SerialName("collected_amount") val collectedAmount: String = "0.00",
    val balance: String = "0.00",
    @SerialName("created_at") val createdAt: String = ""
)

@Serializable
data class CreateLineSaleRequest(
    @SerialName("customer_id") val customerId: String,
    val route: String? = null,
    val salesman: String? = null,
    val note: String? = null,
    @SerialName("total_amount") val totalAmount: String,
    @SerialName("total_cash_in") val totalCashIn: String = "0.00",
    @SerialName("bank_amount") val bankAmount: String = "0.00",
    @SerialName("bank_id") val bankId: String? = null
)

@Serializable
data class CounterSale(
    val id: String = "",
    val item: String = "",
    val price: String = "0.00",
    @SerialName("total_amount") val totalAmount: String = "0.00",
    @SerialName("payment_method") val paymentMethod: String = "cash",
    val cash: String = "0.00",
    @SerialName("bank_amount") val bankAmount: String = "0.00",
    @SerialName("collected_amount") val collectedAmount: String = "0.00",
    val account: String = "0.00",
    @SerialName("created_at") val createdAt: String = ""
)

@Serializable
data class CreateCounterSaleRequest(
    val item: String,
    val price: String,
    @SerialName("total_amount") val totalAmount: String,
    @SerialName("payment_method") val paymentMethod: String = "cash",
    val cash: String = "0.00",
    @SerialName("bank_amount") val bankAmount: String = "0.00",
    @SerialName("bank_id") val bankId: String? = null
)

// ==========================================
// 2. Purchases & Customer / Dues Models
// ==========================================

@Serializable
data class Purchase(
    val id: String = "",
    @SerialName("customer_id") val customerId: String? = null,
    @SerialName("customer_name") val customerName: String? = null,
    val item: String = "",
    val quantity: Int = 1,
    @SerialName("total_amount") val totalAmount: String = "0.00",
    @SerialName("total_paid") val totalPaid: String = "0.00",
    @SerialName("total_pending") val totalPending: String = "0.00",
    @SerialName("payment_status") val paymentStatus: String = "UNPAID",
    @SerialName("created_at") val createdAt: String = ""
)

@Serializable
data class CreatePurchaseRequest(
    @SerialName("customer_id") val customerId: String? = null,
    val item: String,
    val quantity: Int = 1,
    @SerialName("total_amount") val totalAmount: String,
    @SerialName("total_paid") val totalPaid: String = "0.00"
)

@Serializable
data class Customer(
    val id: String = "",
    @SerialName("customer_name") val customerName: String = "",
    val phone: String? = null,
    @SerialName("opening_balance") val openingBalance: String = "0.00",
    @SerialName("current_balance") val currentBalance: String = "0.00",
    @SerialName("total_purchases") val totalPurchases: String = "0.00",
    @SerialName("total_paid") val totalPaid: String = "0.00",
    @SerialName("outstanding_payable") val outstandingPayable: String = "0.00",
    val status: String = "active"
)

@Serializable
data class CreateCustomerRequest(
    @SerialName("customer_name") val customerName: String,
    val phone: String? = null,
    @SerialName("opening_balance") val openingBalance: String = "0.00",
    val status: String = "active"
)

@Serializable
data class CustomerBalance(
    @SerialName("customer_id") val customerId: String = "",
    @SerialName("customer_name") val customerName: String = "",
    @SerialName("current_balance") val currentBalance: String = "0.00",
    @SerialName("total_purchases") val totalPurchases: String = "0.00",
    @SerialName("total_paid") val totalPaid: String = "0.00",
    @SerialName("outstanding_payable") val outstandingPayable: String = "0.00",
    val status: String = "active"
)

@Serializable
data class CustomerStatementEntry(
    val id: String = "",
    val date: String = "",
    @SerialName("formatted_date") val formattedDate: String = "",
    val description: String = "",
    @SerialName("entry_type") val entryType: String = "",
    @SerialName("purchase_amount") val purchaseAmount: String = "0.00",
    @SerialName("paid_amount") val paidAmount: String = "0.00",
    val balance: String = "0.00",
    @SerialName("payment_method") val paymentMethod: String? = null
)

@Serializable
data class CustomerStatement(
    @SerialName("customer_id") val customerId: String = "",
    @SerialName("customer_name") val customerName: String = "",
    val phone: String? = null,
    @SerialName("opening_balance") val openingBalance: String = "0.00",
    @SerialName("current_balance") val currentBalance: String = "0.00",
    @SerialName("total_purchases") val totalPurchases: String = "0.00",
    @SerialName("total_paid") val totalPaid: String = "0.00",
    @SerialName("outstanding_payable") val outstandingPayable: String = "0.00",
    val entries: List<CustomerStatementEntry> = emptyList()
)

@Serializable
data class RecordPaymentRequest(
    val amount: String,
    @SerialName("payment_method") val paymentMethod: String = "cash",
    @SerialName("payment_date") val paymentDate: String? = null,
    val note: String? = null
)

// ==========================================
// 3. Expenses Models
// ==========================================

@Serializable
data class Expense(
    val id: String = "",
    val item: String = "",
    val category: String? = null,
    @SerialName("total_amount") val totalAmount: String = "0.00",
    val amount: String = "0.00",
    @SerialName("payment_method") val paymentMethod: String = "cash",
    @SerialName("created_at") val createdAt: String = ""
)

@Serializable
data class CreateExpenseRequest(
    val item: String,
    val category: String? = null,
    @SerialName("total_amount") val totalAmount: String,
    @SerialName("payment_method") val paymentMethod: String = "cash"
)

// ==========================================
// 4. Staff, Attendance & Salary Models
// ==========================================

@Serializable
data class Employee(
    val id: String = "",
    val name: String = "",
    val phone: String? = null,
    val salary: String = "0.00",
    @SerialName("ot_rate") val otRate: String = "0.00",
    val status: String = "active"
)

@Serializable
data class CreateEmployeeRequest(
    val name: String,
    val phone: String? = null,
    val salary: String,
    @SerialName("ot_rate") val otRate: String = "0.00",
    val status: String = "active"
)

@Serializable
data class AttendanceRecord(
    val id: String = "",
    @SerialName("employee_id") val employeeId: String = "",
    @SerialName("employee_name") val employeeName: String = "",
    val date: String = "",
    val status: String = "present", // present, absent, half_day, leave
    @SerialName("daily_salary") val dailySalary: String = "0.00",
    @SerialName("ot_amount") val otAmount: String = "0.00",
    val advance: String = "0.00"
)

@Serializable
data class RecordAttendanceRequest(
    @SerialName("employee_id") val employeeId: String,
    val date: String,
    val status: String,
    @SerialName("ot_amount") val otAmount: String = "0.00",
    val advance: String = "0.00"
)

@Serializable
data class EmployeeSalary(
    val id: String = "",
    @SerialName("employee_id") val employeeId: String = "",
    @SerialName("employee_name") val employeeName: String = "",
    @SerialName("salary_rate") val salaryRate: String = "0.00",
    @SerialName("gross_salary") val grossSalary: String = "0.00",
    @SerialName("total_overtime") val totalOvertime: String = "0.00",
    @SerialName("total_advances") val totalAdvances: String = "0.00",
    @SerialName("total_paid") val totalPaid: String = "0.00",
    val balance: String = "0.00"
)

@Serializable
data class PaySalaryRequest(
    @SerialName("payment_method") val paymentMethod: String = "cash",
    val amount: String,
    @SerialName("payment_date") val paymentDate: String? = null,
    val note: String? = null
)

// ==========================================
// 5. Cash Book Models
// ==========================================

@Serializable
data class CashBalanceResponse(
    @SerialName("cash_balance") val cashBalance: String = "0.00"
)

@Serializable
data class CashTransaction(
    val id: String = "",
    @SerialName("transaction_type") val transactionType: String = "",
    val amount: String = "0.00",
    @SerialName("previous_balance") val previousBalance: String = "0.00",
    @SerialName("new_balance") val newBalance: String = "0.00",
    val description: String? = null,
    @SerialName("transaction_date") val transactionDate: String = "",
    @SerialName("created_at") val createdAt: String = ""
)

@Serializable
data class AddCashRequest(
    val amount: String,
    val description: String? = null,
    @SerialName("transaction_date") val transactionDate: String? = null
)

@Serializable
data class AdjustCashRequest(
    @SerialName("actual_balance") val actualBalance: String,
    val reason: String
)
