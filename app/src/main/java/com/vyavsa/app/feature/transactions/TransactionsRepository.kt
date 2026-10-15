package com.vyavsa.app.feature.transactions

import com.vyavsa.app.core.common.Resource
import com.vyavsa.app.core.model.CashTransaction
import com.vyavsa.app.core.model.CounterSale
import com.vyavsa.app.core.model.CounterSalePayment
import com.vyavsa.app.core.model.CreateCounterSaleRequest
import com.vyavsa.app.core.model.CreateExpenseRequest
import com.vyavsa.app.core.model.CreateLineSaleRequest
import com.vyavsa.app.core.model.CreatePurchaseRequest
import com.vyavsa.app.core.model.Customer
import com.vyavsa.app.core.model.Employee
import com.vyavsa.app.core.model.Expense
import com.vyavsa.app.core.model.LineSale
import com.vyavsa.app.core.model.LineSalePayment
import com.vyavsa.app.core.model.PlanTransaction
import com.vyavsa.app.core.model.Purchase
import com.vyavsa.app.core.model.PurchasePayment
import com.vyavsa.app.core.model.RecordSalePaymentRequest
import com.vyavsa.app.core.model.RecordSupplierPaymentRequest
import com.vyavsa.app.core.model.SupplierPaymentResponse
import com.vyavsa.app.core.model.UpdatePurchaseRequest
import com.vyavsa.app.core.network.VyavsaApiService
import com.vyavsa.app.core.sync.DataMutation
import com.vyavsa.app.core.sync.DataRefreshCoordinator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

interface TransactionsRepository {
    fun listLineSales(date: String?, search: String?): Flow<Resource<List<LineSale>>>
    fun createLineSale(request: CreateLineSaleRequest): Flow<Resource<LineSale>>
    fun getLineSaleById(id: String): Flow<Resource<LineSale>>
    fun listLineSalePayments(id: String): Flow<Resource<List<LineSalePayment>>>
    fun recordLineSalePayment(id: String, request: RecordSalePaymentRequest): Flow<Resource<LineSalePayment>>

    fun listCounterSales(date: String?, search: String?): Flow<Resource<List<CounterSale>>>
    fun createCounterSale(request: CreateCounterSaleRequest): Flow<Resource<CounterSale>>
    fun getCounterSaleById(id: String): Flow<Resource<CounterSale>>
    fun listCounterSalePayments(id: String): Flow<Resource<List<CounterSalePayment>>>
    fun recordCounterSalePayment(id: String, request: RecordSalePaymentRequest): Flow<Resource<CounterSalePayment>>

    fun listPurchases(date: String?, search: String?, customerId: String? = null): Flow<Resource<List<Purchase>>>
    fun createPurchase(request: CreatePurchaseRequest): Flow<Resource<Purchase>>
    fun getPurchaseById(id: String): Flow<Resource<Purchase>>
    fun updatePurchase(id: String, request: UpdatePurchaseRequest): Flow<Resource<Purchase>>
    fun deletePurchase(id: String): Flow<Resource<Unit>>
    fun listPurchasePayments(id: String): Flow<Resource<List<PurchasePayment>>>
    fun recordPurchasePayment(id: String, request: RecordSupplierPaymentRequest): Flow<Resource<SupplierPaymentResponse>>

    fun listExpenses(date: String?, search: String?): Flow<Resource<List<Expense>>>
    fun createExpense(request: CreateExpenseRequest): Flow<Resource<Expense>>
    fun listCashTransactions(): Flow<Resource<List<CashTransaction>>>
    fun listSubscriptionTransactions(): Flow<Resource<List<PlanTransaction>>>
    fun listCustomers(): Flow<Resource<List<Customer>>>
    fun listEmployees(): Flow<Resource<List<Employee>>>
}

@Singleton
class TransactionsRepositoryImpl @Inject constructor(
    private val apiService: VyavsaApiService,
    private val refreshCoordinator: DataRefreshCoordinator
) : TransactionsRepository {

    override fun listLineSales(date: String?, search: String?): Flow<Resource<List<LineSale>>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.listLineSales(date = date, search = search)
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data))
            } else {
                emit(Resource.Error(response.error?.message ?: "Failed to load line sales"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Network error loading line sales"))
        }
    }.flowOn(Dispatchers.IO)

    override fun createLineSale(request: CreateLineSaleRequest): Flow<Resource<LineSale>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.createLineSale(request)
            if (response.success && response.data != null) {
                refreshCoordinator.notifyMutation(DataMutation.SALES)
                val bankAmountVal = request.bankAmount.toDoubleOrNull() ?: 0.0
                if (!request.bankId.isNullOrBlank() || bankAmountVal > 0.0 || request.bankPayments.isNotEmpty()) {
                    refreshCoordinator.notifyMutation(DataMutation.BANK)
                }
                val cashIn = request.cashAmount?.toDoubleOrNull() ?: request.totalCashIn.toDoubleOrNull() ?: 0.0
                if (cashIn > 0.0) {
                    refreshCoordinator.notifyMutation(DataMutation.CASH)
                }
                refreshCoordinator.notifyMutation(DataMutation.DUES)
                emit(Resource.Success(response.data, response.message))
            } else {
                emit(Resource.Error(response.error?.message ?: response.message ?: "Failed to create line sale", response.error?.code))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Network error creating sale"))
        }
    }.flowOn(Dispatchers.IO)

    override fun getLineSaleById(id: String): Flow<Resource<LineSale>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.getLineSaleById(id)
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data))
            } else {
                emit(Resource.Error(response.error?.message ?: "Failed to load line sale details"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Network error loading sale details"))
        }
    }.flowOn(Dispatchers.IO)

    override fun listLineSalePayments(id: String): Flow<Resource<List<LineSalePayment>>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.listLineSalePayments(id)
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data))
            } else {
                emit(Resource.Error(response.error?.message ?: "Failed to load line sale payments"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Network error loading payments"))
        }
    }.flowOn(Dispatchers.IO)

    override fun recordLineSalePayment(id: String, request: RecordSalePaymentRequest): Flow<Resource<LineSalePayment>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.recordLineSalePayment(id, request)
            if (response.success && response.data != null) {
                refreshCoordinator.notifyMutation(DataMutation.SALES)
                refreshCoordinator.notifyMutation(DataMutation.DUES)
                if (request.paymentMethod == "cash") {
                    refreshCoordinator.notifyMutation(DataMutation.CASH)
                } else if (!request.bankId.isNullOrBlank()) {
                    refreshCoordinator.notifyMutation(DataMutation.BANK)
                }
                emit(Resource.Success(response.data, response.message))
            } else {
                emit(Resource.Error(response.error?.message ?: response.message ?: "Failed to record payment", response.error?.code))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Network error recording payment"))
        }
    }.flowOn(Dispatchers.IO)

    override fun listCounterSales(date: String?, search: String?): Flow<Resource<List<CounterSale>>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.listCounterSales(date = date, search = search)
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data))
            } else {
                emit(Resource.Error(response.error?.message ?: "Failed to load counter sales"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Network error loading counter sales"))
        }
    }.flowOn(Dispatchers.IO)

    override fun createCounterSale(request: CreateCounterSaleRequest): Flow<Resource<CounterSale>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.createCounterSale(request)
            if (response.success && response.data != null) {
                refreshCoordinator.notifyMutation(DataMutation.SALES)
                val bankAmountVal = request.bankAmount.toDoubleOrNull() ?: 0.0
                if (!request.bankId.isNullOrBlank() || bankAmountVal > 0.0 || request.bankPayments.isNotEmpty()) {
                    refreshCoordinator.notifyMutation(DataMutation.BANK)
                }
                val cashIn = request.cashAmount?.toDoubleOrNull() ?: request.cash.toDoubleOrNull() ?: 0.0
                if (cashIn > 0.0) {
                    refreshCoordinator.notifyMutation(DataMutation.CASH)
                }
                emit(Resource.Success(response.data, response.message))
            } else {
                emit(Resource.Error(response.error?.message ?: response.message ?: "Failed to record counter sale", response.error?.code))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Network error recording sale"))
        }
    }.flowOn(Dispatchers.IO)

    override fun getCounterSaleById(id: String): Flow<Resource<CounterSale>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.getCounterSaleById(id)
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data))
            } else {
                emit(Resource.Error(response.error?.message ?: "Failed to load counter sale details"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Network error loading counter sale details"))
        }
    }.flowOn(Dispatchers.IO)

    override fun listCounterSalePayments(id: String): Flow<Resource<List<CounterSalePayment>>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.listCounterSalePayments(id)
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data))
            } else {
                emit(Resource.Error(response.error?.message ?: "Failed to load counter sale payments"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Network error loading payments"))
        }
    }.flowOn(Dispatchers.IO)

    override fun recordCounterSalePayment(id: String, request: RecordSalePaymentRequest): Flow<Resource<CounterSalePayment>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.recordCounterSalePayment(id, request)
            if (response.success && response.data != null) {
                refreshCoordinator.notifyMutation(DataMutation.SALES)
                if (request.paymentMethod == "cash") {
                    refreshCoordinator.notifyMutation(DataMutation.CASH)
                } else if (!request.bankId.isNullOrBlank()) {
                    refreshCoordinator.notifyMutation(DataMutation.BANK)
                }
                emit(Resource.Success(response.data, response.message))
            } else {
                emit(Resource.Error(response.error?.message ?: response.message ?: "Failed to record payment", response.error?.code))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Network error recording payment"))
        }
    }.flowOn(Dispatchers.IO)

    override fun listPurchases(date: String?, search: String?, customerId: String?): Flow<Resource<List<Purchase>>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.listPurchases(date = date, search = search, customerId = customerId)
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data))
            } else {
                emit(Resource.Error(response.error?.message ?: "Failed to load purchases"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Network error loading purchases"))
        }
    }.flowOn(Dispatchers.IO)

    override fun createPurchase(request: CreatePurchaseRequest): Flow<Resource<Purchase>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.createPurchase(request)
            if (response.success && response.data != null) {
                refreshCoordinator.notifyMutation(DataMutation.PURCHASES)
                refreshCoordinator.notifyMutation(DataMutation.DUES)
                val cashVal = request.cashAmount?.toDoubleOrNull() ?: request.totalPaid.toDoubleOrNull() ?: 0.0
                if (cashVal > 0.0) {
                    refreshCoordinator.notifyMutation(DataMutation.CASH)
                }
                if (request.bankPayments.isNotEmpty()) {
                    refreshCoordinator.notifyMutation(DataMutation.BANK)
                }
                emit(Resource.Success(response.data, response.message))
            } else {
                emit(Resource.Error(response.error?.message ?: response.message ?: "Failed to record purchase", response.error?.code))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Network error recording purchase"))
        }
    }.flowOn(Dispatchers.IO)

    override fun getPurchaseById(id: String): Flow<Resource<Purchase>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.getPurchaseById(id)
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data))
            } else {
                emit(Resource.Error(response.error?.message ?: "Failed to load purchase details"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Network error loading purchase details"))
        }
    }.flowOn(Dispatchers.IO)

    override fun updatePurchase(id: String, request: UpdatePurchaseRequest): Flow<Resource<Purchase>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.updatePurchase(id, request)
            if (response.success && response.data != null) {
                refreshCoordinator.notifyMutation(DataMutation.PURCHASES)
                refreshCoordinator.notifyMutation(DataMutation.DUES)
                refreshCoordinator.notifyMutation(DataMutation.CASH)
                refreshCoordinator.notifyMutation(DataMutation.BANK)
                emit(Resource.Success(response.data, response.message))
            } else {
                emit(Resource.Error(response.error?.message ?: response.message ?: "Failed to update purchase", response.error?.code))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Network error updating purchase"))
        }
    }.flowOn(Dispatchers.IO)

    override fun deletePurchase(id: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.deletePurchase(id)
            if (response.success) {
                refreshCoordinator.notifyMutation(DataMutation.PURCHASES)
                refreshCoordinator.notifyMutation(DataMutation.DUES)
                refreshCoordinator.notifyMutation(DataMutation.CASH)
                refreshCoordinator.notifyMutation(DataMutation.BANK)
                emit(Resource.Success(Unit, response.message))
            } else {
                emit(Resource.Error(response.error?.message ?: response.message ?: "Failed to delete purchase", response.error?.code))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Network error deleting purchase"))
        }
    }.flowOn(Dispatchers.IO)

    override fun listPurchasePayments(id: String): Flow<Resource<List<PurchasePayment>>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.listPurchasePayments(id)
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data))
            } else {
                emit(Resource.Error(response.error?.message ?: "Failed to load purchase payments"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Network error loading payments"))
        }
    }.flowOn(Dispatchers.IO)

    override fun recordPurchasePayment(id: String, request: RecordSupplierPaymentRequest): Flow<Resource<SupplierPaymentResponse>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.recordPurchaseSettlementPayment(id, request)
            if (response.success && response.data != null) {
                refreshCoordinator.notifyMutation(DataMutation.PURCHASES)
                refreshCoordinator.notifyMutation(DataMutation.DUES)
                if (request.paymentMethod == "cash") {
                    refreshCoordinator.notifyMutation(DataMutation.CASH)
                } else if (!request.bankId.isNullOrBlank() || request.bankPayments.isNotEmpty()) {
                    refreshCoordinator.notifyMutation(DataMutation.BANK)
                }
                emit(Resource.Success(response.data, response.message))
            } else {
                emit(Resource.Error(response.error?.message ?: response.message ?: "Failed to record purchase settlement", response.error?.code))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Network error recording payment"))
        }
    }.flowOn(Dispatchers.IO)

    override fun listExpenses(date: String?, search: String?): Flow<Resource<List<Expense>>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.listExpenses(date = date, search = search)
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data))
            } else {
                emit(Resource.Error(response.error?.message ?: "Failed to load expenses"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Network error loading expenses"))
        }
    }.flowOn(Dispatchers.IO)

    override fun createExpense(request: CreateExpenseRequest): Flow<Resource<Expense>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.createExpense(request)
            if (response.success && response.data != null) {
                refreshCoordinator.notifyMutation(DataMutation.EXPENSES)
                if (request.category == "employee_advance" || !request.employeeId.isNullOrBlank()) {
                    refreshCoordinator.notifyMutation(DataMutation.STAFF)
                }
                if (request.paymentMethod == "bank" || !request.bankId.isNullOrBlank() || request.bankPayments.isNotEmpty()) {
                    refreshCoordinator.notifyMutation(DataMutation.BANK)
                } else if (request.paymentMethod == "cash") {
                    refreshCoordinator.notifyMutation(DataMutation.CASH)
                }
                emit(Resource.Success(response.data, response.message))
            } else {
                emit(Resource.Error(response.error?.message ?: response.message ?: "Failed to record expense", response.error?.code))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Network error recording expense"))
        }
    }.flowOn(Dispatchers.IO)

    override fun listCashTransactions(): Flow<Resource<List<CashTransaction>>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.listCashTransactions()
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data))
            } else {
                emit(Resource.Error(response.error?.message ?: "Failed to load cash transactions"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Network error loading cash transactions"))
        }
    }.flowOn(Dispatchers.IO)

    override fun listSubscriptionTransactions(): Flow<Resource<List<PlanTransaction>>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.listSubscriptionTransactions()
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data))
            } else {
                emit(Resource.Error(response.error?.message ?: "Failed to load billing history"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Network error loading billing history"))
        }
    }.flowOn(Dispatchers.IO)

    override fun listCustomers(): Flow<Resource<List<Customer>>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.listCustomers()
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data))
            } else {
                emit(Resource.Error(response.error?.message ?: "Failed to load customers"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Network error"))
        }
    }.flowOn(Dispatchers.IO)

    override fun listEmployees(): Flow<Resource<List<Employee>>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.listEmployees()
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data))
            } else {
                emit(Resource.Error(response.error?.message ?: "Failed to load employees"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Network error loading employees"))
        }
    }.flowOn(Dispatchers.IO)
}
