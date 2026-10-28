package com.vyavsa.app.core.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ==========================================
// 1. Sales Models & Payments
// ==========================================

@Immutable
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

@Immutable
@Serializable
data class SaleItem(
    val id: String = "",
    @SerialName("tenant_id") val tenantId: String = "",
    @SerialName("sale_type") val saleType: String = "",
    @SerialName("sale_id") val saleId: String = "",
    @SerialName("line_sale_id") val lineSaleId: String? = null,
    @SerialName("counter_sale_id") val counterSaleId: String? = null,
    @SerialName("product_id") val productId: String? = null,
    @SerialName("product_name_snapshot") val productNameSnapshot: String = "",
    val quantity: String = "1",
    @SerialName("unit_price") val unitPrice: String = "0.00",
    @SerialName("line_total") val lineTotal: String = "0.00",
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = ""
)

@Serializable
data class SaleItemRequest(
    @SerialName("product_id") val productId: String,
    val quantity: String,
    @SerialName("unit_price") val unitPrice: String = "0.00"
)

@Immutable
@Serializable
data class LineSale(
    val id: String = "",
    @SerialName("customer_id") val customerId: String = "",
    @SerialName("customer_name") val customerName: String = "",
    @SerialName("customer_phone") val customerPhone: String? = null,
    @SerialName("invoice_number") val invoiceNumber: String? = null,
    val route: String? = null,
    val salesman: String? = null,
    val note: String? = null,
    val subtotal: String = "0.00",
    val discount: String = "0.00",
    @SerialName("total_amount") val totalAmount: String = "0.00",
    @SerialName("total_cash_in") val totalCashIn: String = "0.00",
    @SerialName("bank_amount") val bankAmount: String = "0.00",
    @SerialName("collected_amount") val collectedAmount: String = "0.00",
    val balance: String = "0.00",
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = "",
    val payments: List<LineSalePayment> = emptyList(),
    val items: List<SaleItem> = emptyList()
) {
    val cash: String
        get() = totalCashIn
}

@Immutable
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
    val subtotal: String? = null,
    val discount: String? = null,
    @SerialName("discount_amount") val discountAmount: String? = null,
    @SerialName("total_amount") val totalAmount: String = "0.00",
    @SerialName("total_cash_in") val totalCashIn: String = "0.00",
    @SerialName("cash_amount") val cashAmount: String? = null,
    @SerialName("bank_amount") val bankAmount: String = "0.00",
    @SerialName("bank_id") val bankId: String? = null,
    @SerialName("bank_payments") val bankPayments: List<BankPaymentSplit> = emptyList(),
    val items: List<SaleItemRequest> = emptyList()
)

@Serializable
data class UpdateLineSaleRequest(
    val route: String? = null,
    val salesman: String? = null,
    val note: String? = null,
    val subtotal: String? = null,
    val discount: String? = null,
    @SerialName("discount_amount") val discountAmount: String? = null,
    @SerialName("total_amount") val totalAmount: String? = null,
    @SerialName("total_cash_in") val totalCashIn: String? = null,
    val items: List<SaleItemRequest> = emptyList()
)

@Immutable
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

@Immutable
@Serializable
data class CounterSale(
    val id: String = "",
    @SerialName("invoice_number") val invoiceNumber: String? = null,
    val item: String = "",
    val price: String = "0.00",
    val subtotal: String = "0.00",
    val discount: String = "0.00",
    @SerialName("total_amount") val totalAmount: String = "0.00",
    @SerialName("payment_method") val paymentMethod: String = "cash",
    val cash: String = "0.00",
    @SerialName("bank_amount") val bankAmount: String = "0.00",
    @SerialName("collected_amount") val collectedAmount: String = "0.00",
    val account: String = "0.00",
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = "",
    val payments: List<CounterSalePayment> = emptyList(),
    val items: List<SaleItem> = emptyList()
)

@Serializable
data class CreateCounterSaleRequest(
    val item: String = "",
    val price: String = "0.00",
    val subtotal: String? = null,
    val discount: String? = null,
    @SerialName("discount_amount") val discountAmount: String? = null,
    @SerialName("total_amount") val totalAmount: String = "0.00",
    @SerialName("payment_method") val paymentMethod: String = "cash",
    val cash: String = "0.00",
    @SerialName("cash_amount") val cashAmount: String? = null,
    @SerialName("bank_amount") val bankAmount: String = "0.00",
    @SerialName("bank_id") val bankId: String? = null,
    val account: String? = null,
    @SerialName("bank_payments") val bankPayments: List<BankPaymentSplit> = emptyList(),
    val items: List<SaleItemRequest> = emptyList()
)

@Serializable
data class UpdateCounterSaleRequest(
    val item: String? = null,
    val price: String? = null,
    val subtotal: String? = null,
    val discount: String? = null,
    @SerialName("discount_amount") val discountAmount: String? = null,
    @SerialName("total_amount") val totalAmount: String? = null,
    @SerialName("payment_method") val paymentMethod: String? = null,
    val cash: String? = null,
    @SerialName("bank_amount") val bankAmount: String? = null,
    val account: String? = null,
    val items: List<SaleItemRequest> = emptyList()
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

@Immutable
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

@Immutable
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
    @SerialName("supplier_id") val supplierId: String? = null,
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
    @SerialName("supplier_id") val supplierId: String? = null,
    val item: String? = null,
    val quantity: Int? = null,
    @SerialName("total_amount") val totalAmount: String? = null,
    @SerialName("total_paid") val totalPaid: String? = null
)

@Serializable
data class RecordSupplierPaymentRequest(
    @SerialName("customer_id") val customerId: String? = null,
    @SerialName("supplier_id") val supplierId: String? = null,
    @SerialName("purchase_id") val purchaseId: String? = null,
    val amount: String,
    @SerialName("payment_method") val paymentMethod: String = "cash",
    @SerialName("bank_id") val bankId: String? = null,
    @SerialName("bank_name") val bankName: String? = null,
    @SerialName("bank_payments") val bankPayments: List<BankPaymentSplit> = emptyList(),
    @SerialName("payment_date") val paymentDate: String? = null,
    @SerialName("reference_id") val referenceId: String? = null,
    val note: String? = null
)

@Immutable
@Serializable
data class SupplierPaymentAllocation(
    @SerialName("purchase_id") val purchaseId: String = "",
    val item: String = "",
    @SerialName("previous_pending") val previousPending: String = "0.00",
    @SerialName("amount_settled") val amountSettled: String = "0.00",
    @SerialName("new_pending") val newPending: String = "0.00",
    @SerialName("payment_status") val paymentStatus: String = "PAID"
)

@Immutable
@Serializable
data class SupplierPaymentResponse(
    @SerialName("payment_id") val paymentId: String = "",
    @SerialName("customer_id") val customerId: String = "",
    @SerialName("customer_name") val customerName: String = "",
    val amount: String = "0.00",
    @SerialName("payment_method") val paymentMethod: String = "cash",
    @SerialName("previous_outstanding") val previousOutstanding: String = "0.00",
    @SerialName("new_outstanding") val newOutstanding: String = "0.00",
    @SerialName("payment_date") val paymentDate: String = "",
    @SerialName("reference_id") val referenceId: String? = null,
    val note: String? = null,
    val allocations: List<SupplierPaymentAllocation> = emptyList()
)

@Immutable
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

@Immutable
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

@Immutable
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

@Immutable
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

@Immutable
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

@Serializable
data class CollectCustomerPaymentRequest(
    @SerialName("customer_id") val customerId: String? = null,
    val amount: String,
    @SerialName("payment_method") val paymentMethod: String = "cash", // cash, bank
    @SerialName("cash_amount") val cashAmount: String? = null,
    @SerialName("bank_id") val bankId: String? = null,
    @SerialName("bank_name") val bankName: String? = null,
    @SerialName("payment_date") val paymentDate: String? = null,
    @SerialName("reference_id") val referenceId: String? = null,
    val note: String? = null,
    @SerialName("idempotency_key") val idempotencyKey: String? = null
)

@Immutable
@Serializable
data class CustomerPaymentResponse(
    val id: String = "",
    @SerialName("tenant_id") val tenantId: String = "",
    @SerialName("customer_id") val customerId: String = "",
    @SerialName("customer_name") val customerName: String = "",
    @SerialName("collection_number") val collectionNumber: String = "",
    val amount: String = "0.00",
    @SerialName("payment_method") val paymentMethod: String = "",
    @SerialName("cash_amount") val cashAmount: String = "0.00",
    @SerialName("bank_amount") val bankAmount: String = "0.00",
    @SerialName("customer_balance") val customerBalance: String = "0.00",
    @SerialName("previous_balance") val previousBalance: String = "0.00",
    @SerialName("reference_id") val referenceId: String? = null,
    val note: String? = null,
    @SerialName("payment_date") val paymentDate: String = "",
    @SerialName("created_at") val createdAt: String = ""
)

// ==========================================
// 2b. Supplier Models
// ==========================================

@Immutable
@Serializable
data class Supplier(
    val id: String = "",
    @SerialName("tenant_id") val tenantId: String = "",
    @SerialName("supplier_name") val supplierName: String = "",
    @SerialName("customer_name") val customerName: String? = null,
    val phone: String? = null,
    @SerialName("total_purchases") val totalPurchases: String = "0.00",
    @SerialName("total_paid") val totalPaid: String = "0.00",
    @SerialName("outstanding_payable") val outstandingPayable: String = "0.00",
    val status: String = "active",
    @SerialName("entity_type") val entityType: String = "supplier",
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = ""
) {
    val displayName: String
        get() = supplierName.ifBlank { customerName ?: "Unnamed Supplier" }
}

@Serializable
data class CreateSupplierRequest(
    @SerialName("supplier_name") val supplierName: String,
    val phone: String? = null,
    val status: String = "active"
)

@Serializable
data class UpdateSupplierRequest(
    @SerialName("supplier_name") val supplierName: String? = null,
    val phone: String? = null,
    val status: String? = null
)

@Immutable
@Serializable
data class SupplierBalance(
    @SerialName("supplier_id") val supplierId: String = "",
    @SerialName("supplier_name") val supplierName: String = "",
    @SerialName("total_purchases") val totalPurchases: String = "0.00",
    @SerialName("total_paid") val totalPaid: String = "0.00",
    @SerialName("outstanding_payable") val outstandingPayable: String = "0.00",
    val status: String = "active"
)

// ==========================================
// 3. Expenses Models
// ==========================================

@Immutable
@Serializable
data class ExpenseBankBreakdown(
    @SerialName("bank_account_id") val bankAccountId: String = "",
    @SerialName("bank_name") val bankName: String = "",
    val amount: String = "0.00"
)

@Immutable
@Serializable
data class ExpensePaymentBreakdown(
    @SerialName("cash_amount") val cashAmount: String = "0.00",
    @SerialName("bank_amount") val bankAmount: String = "0.00",
    val banks: List<ExpenseBankBreakdown> = emptyList()
)

@Immutable
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

@Immutable
@Serializable
data class Employee(
    val id: String = "",
    val name: String = "",
    val phone: String? = null,
    val salary: String? = null,
    @SerialName("ot_rate") val otRate: String = "0.00",
    val status: String = "active",
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = ""
)

@Serializable
data class CreateEmployeeRequest(
    val name: String,
    val phone: String? = null,
    val salary: String? = null,
    @SerialName("ot_rate") val otRate: String = "0.00",
    val status: String = "active"
)

@Serializable
data class UpdateEmployeeRequest(
    val name: String? = null,
    val phone: String? = null,
    val salary: String? = null,
    @SerialName("clear_salary") val clearSalary: Boolean = false,
    @SerialName("ot_rate") val otRate: String? = null,
    val status: String? = null
)

@Immutable
@Serializable
data class AttendanceRecord(
    val id: String = "",
    @SerialName("tenant_id") val tenantId: String = "",
    @SerialName("employee_id") val employeeId: String = "",
    @SerialName("employee_name") val employeeName: String = "",
    val date: String = "",
    val status: String = "present", // present, absent, half_day, leave
    @SerialName("daily_salary") val dailySalary: String? = null,
    @SerialName("salary_source") val salarySource: String? = null,
    val ot: String = "0.00",
    @SerialName("ot_amount") val otAmount: String = "0.00",
    val advance: String = "0.00",
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = ""
)

@Serializable
data class RecordAttendanceRequest(
    @SerialName("employee_id") val employeeId: String,
    val date: String? = null,
    val status: String,
    @SerialName("daily_salary") val dailySalary: String? = null,
    val ot: String = "0.00",
    @SerialName("ot_amount") val otAmount: String = "0.00",
    val advance: String = "0.00"
)

@Serializable
data class UpdateAttendanceRequest(
    val status: String? = null,
    @SerialName("daily_salary") val dailySalary: String? = null,
    @SerialName("clear_daily_salary") val clearDailySalary: Boolean = false,
    val ot: String? = null,
    @SerialName("ot_amount") val otAmount: String? = null,
    val advance: String? = null
)

@Serializable
data class RecordAdvanceRequest(
    @SerialName("employee_id") val employeeId: String,
    val date: String? = null,
    val amount: String,
    @SerialName("payment_method") val paymentMethod: String = "cash",
    @SerialName("bank_id") val bankId: String? = null,
    @SerialName("bank_name") val bankName: String? = null,
    @SerialName("reference_id") val referenceId: String? = null,
    val note: String? = null
)

@Serializable
data class RecordOvertimeRequest(
    @SerialName("employee_id") val employeeId: String,
    val date: String? = null,
    val amount: String = "0.00",
    val ot: String? = null,
    @SerialName("reference_id") val referenceId: String? = null,
    val note: String? = null
)

@Immutable
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

@Immutable
@Serializable
data class EmployeeOvertime(
    val id: String = "",
    @SerialName("employee_id") val employeeId: String = "",
    @SerialName("employee_name") val employeeName: String? = null,
    val amount: String = "0.00",
    @SerialName("overtime_date") val overtimeDate: String = "",
    val notes: String? = null
)

@Immutable
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

@Immutable
@Serializable
data class EmployeeSalaryStatement(
    @SerialName("employee_id") val employeeId: String = "",
    @SerialName("employee_name") val employeeName: String = "",
    @SerialName("base_salary") val baseSalary: String? = null,
    @SerialName("total_overtime") val totalOvertime: String = "0.00",
    @SerialName("gross_salary") val grossSalary: String = "0.00",
    @SerialName("total_advances") val totalAdvances: String = "0.00",
    @SerialName("total_paid") val totalPaid: String = "0.00",
    @SerialName("net_payable") val netPayable: String = "0.00",
    val overtimes: List<EmployeeOvertime> = emptyList(),
    val advances: List<EmployeeAdvance> = emptyList(),
    val payments: List<EmployeeSalaryPayment> = emptyList()
)

@Immutable
@Serializable
data class EmployeeSalary(
    val id: String = "",
    @SerialName("employee_id") val employeeId: String = "",
    @SerialName("employee_name") val employeeName: String = "",
    @SerialName("salary_rate") val salaryRate: String? = null,
    @SerialName("gross_salary") val grossSalary: String = "0.00",
    @SerialName("total_overtime") val totalOvertime: String = "0.00",
    @SerialName("total_advances") val totalAdvances: String = "0.00",
    @SerialName("total_paid") val totalPaid: String = "0.00",
    val balance: String = "0.00"
)

@Serializable
data class PaySalaryRequest(
    @SerialName("payment_method") val paymentMethod: String = "cash",
    @SerialName("bank_id") val bankId: String? = null,
    @SerialName("bank_name") val bankName: String? = null,
    val amount: String,
    @SerialName("payment_date") val paymentDate: String? = null,
    @SerialName("reference_id") val referenceId: String? = null,
    val note: String? = null
)

// ==========================================
// 5. Tenant User Management Models
// ==========================================

@Immutable
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

@Immutable
@Serializable
data class CashBalanceResponse(
    @SerialName("cash_balance") val cashBalance: String = "0.00"
)

@Immutable
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

@Immutable
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

// ==========================================
// 7. Product Catalog & Inventory Models
// ==========================================

@Immutable
@Serializable
data class Product(
    val id: String = "",
    @SerialName("tenant_id") val tenantId: String = "",
    val name: String = "",
    @SerialName("unit_price") val unitPrice: String = "0.00",
    val status: String = "active", // active, inactive, archived
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = ""
)

@Serializable
data class CreateProductRequest(
    val name: String,
    @SerialName("unit_price") val unitPrice: String = "0.00"
)

@Serializable
data class UpdateProductRequest(
    val name: String? = null,
    @SerialName("unit_price") val unitPrice: String? = null,
    val status: String? = null
)

// ==========================================
// 8. Product Sales Analytics Models
// ==========================================

@Immutable
@Serializable
data class ProductSalesSummary(
    @SerialName("total_quantity_sold") val totalQuantitySold: String = "0.00",
    @SerialName("total_revenue") val totalRevenue: String = "0.00",
    @SerialName("distinct_products_count") val distinctProductsCount: Long = 0,
    @SerialName("top_product_name") val topProductName: String? = null
)

@Immutable
@Serializable
data class TopSellingProduct(
    val rank: Int = 1,
    @SerialName("product_id") val productId: String? = null,
    @SerialName("product_name") val productName: String = "",
    @SerialName("quantity_sold") val quantitySold: String = "0.00",
    val revenue: String = "0.00"
)

@Immutable
@Serializable
data class ProductDailySales(
    @SerialName("sale_date") val saleDate: String = "",
    @SerialName("daily_quantity") val dailyQuantity: String = "0.00",
    @SerialName("daily_revenue") val dailyRevenue: String = "0.00"
)

@Immutable
@Serializable
data class ProductAnalyticsResponse(
    val summary: ProductSalesSummary = ProductSalesSummary(),
    @SerialName("top_by_quantity") val topByQuantity: List<TopSellingProduct> = emptyList(),
    @SerialName("top_by_revenue") val topByRevenue: List<TopSellingProduct> = emptyList(),
    @SerialName("daily_sales") val dailySales: List<ProductDailySales> = emptyList(),
    @SerialName("start_date") val startDate: String = "",
    @SerialName("end_date") val endDate: String = ""
)

