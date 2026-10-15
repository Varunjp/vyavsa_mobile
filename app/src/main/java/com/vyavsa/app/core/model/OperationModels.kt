package com.vyavsa.app.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ==========================================
// 1. Sales Models & Payments
// ==========================================

@Serializable
data class LineSalePayment(
    val id: String = "",
    @SerialName("tenant_id") val tenantId: String = "",
    @SerialName("line_sale_id") val lineSaleId: String = "",
    @SerialName("payment_method") val paymentMethod: String = "cash",
    @SerialName("bank_id") val bankId: String? = null,
    @SerialName("bank_name") val bankName: String? = null,
    val amount: String = "0.00",
    @SerialName("payment_date") val paymentDate: String? = null,
    @SerialName("reference_id") val referenceId: String? = null,
    val status: String? = null,
    val note: String? = null,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = ""
)

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
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = "",
    val payments: List<LineSalePayment> = emptyList()
)

@Serializable
data class BankPaymentSplit(
    @SerialName("bank_id") val bankId: String,
    @SerialName("bank_name") val bankName: String? = null,
    val amount: String,
    val note: String? = null
)

@Serializable
data class CreateLineSaleRequest(
    @SerialName("customer_id") val customerId: String,
    val route: String? = null,
    val salesman: String? = null,
    val note: String? = null,
    @SerialName("total_amount") val totalAmount: String,
    @SerialName("total_cash_in") val totalCashIn: String = "0.00",
    @SerialName("cash_amount") val cashAmount: String? = null,
    @SerialName("bank_amount") val bankAmount: String = "0.00",
    @SerialName("bank_id") val bankId: String? = null,
    @SerialName("bank_payments") val bankPayments: List<BankPaymentSplit> = emptyList()
)

@Serializable
data class CounterSalePayment(
    val id: String = "",
    @SerialName("tenant_id") val tenantId: String = "",
    @SerialName("counter_sale_id") val counterSaleId: String = "",
    @SerialName("payment_method") val paymentMethod: String = "cash",
    @SerialName("bank_id") val bankId: String? = null,
    @SerialName("bank_name") val bankName: String? = null,
    val amount: String = "0.00",
    @SerialName("payment_date") val paymentDate: String? = null,
    @SerialName("reference_id") val referenceId: String? = null,
    val status: String? = null,
    val note: String? = null,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = ""
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
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = "",
    val payments: List<CounterSalePayment> = emptyList()
)

@Serializable
data class CreateCounterSaleRequest(
    val item: String,
    val price: String,
    @SerialName("total_amount") val totalAmount: String,
    @SerialName("payment_method") val paymentMethod: String = "cash",
    val cash: String = "0.00",
    @SerialName("cash_amount") val cashAmount: String? = null,
    @SerialName("bank_amount") val bankAmount: String = "0.00",
    @SerialName("bank_id") val bankId: String? = null,
    val account: String? = null,
    @SerialName("bank_payments") val bankPayments: List<BankPaymentSplit> = emptyList()
)

@Serializable
data class RecordSalePaymentRequest(
    val amount: String,
    @SerialName("payment_method") val paymentMethod: String = "cash",
    @SerialName("bank_id") val bankId: String? = null,
    @SerialName("bank_name") val bankName: String? = null,
    @SerialName("payment_date") val paymentDate: String? = null,
    @SerialName("reference_id") val referenceId: String? = null,
    val note: String? = null
)

// ==========================================
// 2. Purchases & Customer / Dues Models
// ==========================================

@Serializable
data class PurchasePayment(
    val id: String = "",
    @SerialName("tenant_id") val tenantId: String = "",
    @SerialName("purchase_id") val purchaseId: String = "",
    @SerialName("customer_id") val customerId: String? = null,
    @SerialName("payment_method") val paymentMethod: String = "cash",
    @SerialName("bank_id") val bankId: String? = null,
    @SerialName("bank_name") val bankName: String? = null,
    val amount: String = "0.00",
    @SerialName("payment_date") val paymentDate: String? = null,
    @SerialName("reference_id") val referenceId: String? = null,
    val status: String? = null,
    val note: String? = null,
    @SerialName("is_settlement") val isSettlement: Boolean = false,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = ""
)

@Serializable
data class Purchase(
    val id: String = "",
    @SerialName("tenant_id") val tenantId: String = "",
    @SerialName("customer_id") val customerId: String? = null,
    @SerialName("customer_name") val customerName: String? = null,
    val item: String = "",
    val quantity: Int = 1,
    @SerialName("total_amount") val totalAmount: String = "0.00",
    @SerialName("total_paid") val totalPaid: String = "0.00",
    @SerialName("total_pending") val totalPending: String = "0.00",
    @SerialName("outstanding_amount") val outstandingAmount: String = "0.00",
    @SerialName("payment_status") val paymentStatus: String = "UNPAID",
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = "",
    val payments: List<PurchasePayment> = emptyList()
)

@Serializable
data class CreatePurchaseRequest(
    @SerialName("customer_id") val customerId: String? = null,
    val item: String,
    val quantity: Int = 1,
    @SerialName("total_amount") val totalAmount: String,
    @SerialName("total_paid") val totalPaid: String = "0.00",
    @SerialName("cash_amount") val cashAmount: String? = null,
    @SerialName("bank_payments") val bankPayments: List<BankPaymentSplit> = emptyList()
)

@Serializable
data class UpdatePurchaseRequest(
    @SerialName("customer_id") val customerId: String? = null,
    val item: String? = null,
    val quantity: Int? = null,
    @SerialName("total_amount") val totalAmount: String? = null,
    @SerialName("total_paid") val totalPaid: String? = null
)

@Serializable
data class RecordSupplierPaymentRequest(
    val amount: String,
    @SerialName("payment_method") val paymentMethod: String = "cash",
    @SerialName("bank_id") val bankId: String? = null,
    @SerialName("bank_name") val bankName: String? = null,
    @SerialName("bank_payments") val bankPayments: List<BankPaymentSplit> = emptyList(),
    @SerialName("payment_date") val paymentDate: String? = null,
    @SerialName("reference_id") val referenceId: String? = null,
    val note: String? = null
)

@Serializable
data class SupplierPaymentAllocation(
    @SerialName("purchase_id") val purchaseId: String = "",
    val item: String = "",
    @SerialName("previous_pending") val previousPending: String = "0.00",
    @SerialName("amount_settled") val amountSettled: String = "0.00",
    @SerialName("new_pending") val newPending: String = "0.00",
    @SerialName("payment_status") val paymentStatus: String = "PAID"
)

@Serializable
data class SupplierPaymentResponse(
    @SerialName("payment_id") val paymentId: String = "",
    @SerialName("customer_id") val customerId: String = "",
    @SerialName("customer_name") val customerName: String = "",
    val amount: String = "0.00",
    @SerialName("payment_method") val paymentMethod: String = "cash",
    @SerialName("previous_outstanding") val previousOutstanding: String = "0.00",
    @SerialName("new_outstanding") val newOutstanding: String = "0.00",
    val allocations: List<SupplierPaymentAllocation> = emptyList()
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
    val status: String = "active",
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = ""
)

@Serializable
data class CreateCustomerRequest(
    @SerialName("customer_name") val customerName: String,
    val phone: String? = null,
    @SerialName("opening_balance") val openingBalance: String = "0.00",
    val status: String = "active"
)

@Serializable
data class UpdateCustomerRequest(
    @SerialName("customer_name") val customerName: String? = null,
    val phone: String? = null,
    val status: String? = null
)

@Serializable
data class AdjustCustomerBalanceRequest(
    @SerialName("new_balance") val newBalance: String? = null,
    @SerialName("adjustment_amount") val adjustmentAmount: String? = null,
    val reason: String
)

@Serializable
data class CustomerBalanceAdjustment(
    val id: String = "",
    @SerialName("tenant_id") val tenantId: String = "",
    @SerialName("customer_id") val customerId: String = "",
    @SerialName("customer_name") val customerName: String? = null,
    @SerialName("previous_balance") val previousBalance: String = "0.00",
    @SerialName("new_balance") val newBalance: String = "0.00",
    @SerialName("adjustment_amount") val adjustmentAmount: String = "0.00",
    val reason: String = "",
    @SerialName("created_at") val createdAt: String = ""
) {
    val oldBalance: String
        get() = previousBalance
}

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
data class ExpenseBankBreakdown(
    @SerialName("bank_account_id") val bankAccountId: String = "",
    @SerialName("bank_name") val bankName: String = "",
    val amount: String = "0.00"
)

@Serializable
data class ExpensePaymentBreakdown(
    @SerialName("cash_amount") val cashAmount: String = "0.00",
    @SerialName("bank_amount") val bankAmount: String = "0.00",
    val banks: List<ExpenseBankBreakdown> = emptyList()
)

@Serializable
data class Expense(
    val id: String = "",
    val item: String = "",
    val category: String? = null,
    @SerialName("employee_id") val employeeId: String? = null,
    @SerialName("employee_name") val employeeName: String? = null,
    @SerialName("total_amount") val totalAmount: String = "0.00",
    val amount: String = "0.00",
    @SerialName("payment_method") val paymentMethod: String = "cash",
    @SerialName("payment_breakdown") val paymentBreakdown: ExpensePaymentBreakdown? = null,
    @SerialName("created_at") val createdAt: String = ""
)

@Serializable
data class CreateExpenseRequest(
    val item: String,
    val category: String? = null,
    @SerialName("employee_id") val employeeId: String? = null,
    @SerialName("total_amount") val totalAmount: String,
    val amount: String? = null,
    @SerialName("payment_method") val paymentMethod: String = "cash",
    @SerialName("cash_amount") val cashAmount: String? = null,
    @SerialName("bank_id") val bankId: String? = null,
    @SerialName("bank_name") val bankName: String? = null,
    @SerialName("payment_date") val paymentDate: String? = null,
    @SerialName("reference_id") val referenceId: String? = null,
    @SerialName("bank_payments") val bankPayments: List<BankPaymentSplit> = emptyList()
)

// ==========================================
// 4. Employee & Attendance Models
// ==========================================

@Serializable
data class Employee(
    val id: String = "",
    val name: String = "",
    val phone: String? = null,
    val salary: String = "0.00",
    @SerialName("ot_rate") val otRate: String = "0.00",
    val status: String = "active",
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = ""
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
data class UpdateEmployeeRequest(
    val name: String? = null,
    val phone: String? = null,
    val salary: String? = null,
    @SerialName("ot_rate") val otRate: String? = null,
    val status: String? = null
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
data class EmployeeAdvance(
    val id: String = "",
    @SerialName("employee_id") val employeeId: String = "",
    @SerialName("employee_name") val employeeName: String? = null,
    val amount: String = "0.00",
    @SerialName("payment_method") val paymentMethod: String = "cash",
    @SerialName("advance_date") val advanceDate: String = "",
    val notes: String? = null
)

@Serializable
data class EmployeeOvertime(
    val id: String = "",
    @SerialName("employee_id") val employeeId: String = "",
    @SerialName("employee_name") val employeeName: String? = null,
    val amount: String = "0.00",
    @SerialName("overtime_date") val overtimeDate: String = "",
    val notes: String? = null
)

@Serializable
data class EmployeeSalaryPayment(
    val id: String = "",
    @SerialName("employee_id") val employeeId: String = "",
    @SerialName("employee_name") val employeeName: String? = null,
    val amount: String = "0.00",
    @SerialName("payment_method") val paymentMethod: String = "cash",
    @SerialName("payment_date") val paymentDate: String? = null,
    val note: String? = null,
    @SerialName("created_at") val createdAt: String = ""
)

@Serializable
data class EmployeeSalaryStatement(
    @SerialName("employee_id") val employeeId: String = "",
    @SerialName("employee_name") val employeeName: String = "",
    @SerialName("base_salary") val baseSalary: String = "0.00",
    @SerialName("total_overtime") val totalOvertime: String = "0.00",
    @SerialName("gross_salary") val grossSalary: String = "0.00",
    @SerialName("total_advances") val totalAdvances: String = "0.00",
    @SerialName("total_paid") val totalPaid: String = "0.00",
    @SerialName("net_payable") val netPayable: String = "0.00",
    val overtimes: List<EmployeeOvertime> = emptyList(),
    val advances: List<EmployeeAdvance> = emptyList(),
    val payments: List<EmployeeSalaryPayment> = emptyList()
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
// 5. Tenant User Management Models
// ==========================================

@Serializable
data class TenantUser(
    val id: String = "",
    @SerialName("tenant_id") val tenantId: String = "",
    val name: String = "",
    val role: String = "user", // admin or user
    val email: String = "",
    val status: String = "active", // active, inactive, suspended
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = ""
)

@Serializable
data class CreateTenantUserRequest(
    val name: String,
    val email: String,
    val password: String,
    val role: String = "user",
    val status: String = "active"
)

@Serializable
data class UpdateTenantUserRequest(
    val name: String? = null,
    val role: String? = null,
    val status: String? = null,
    val password: String? = null
)

// ==========================================
// 6. Cash Book & Bank Transaction Models
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

@Serializable
data class BankTransaction(
    val id: String = "",
    @SerialName("tenant_id") val tenantId: String = "",
    @SerialName("bank_id") val bankId: String = "",
    val amount: String = "0.00",
    @SerialName("transaction_type") val transactionType: String = "", // credit, debit
    val reason: String = "",
    @SerialName("sale_type") val saleType: String? = null,
    @SerialName("sale_id") val saleId: String? = null,
    @SerialName("created_at") val createdAt: String = ""
)
