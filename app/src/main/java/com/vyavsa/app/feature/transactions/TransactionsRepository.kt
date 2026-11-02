package com.vyavsa.app.feature.transactions

import com.vyavsa.app.core.common.Resource
import com.vyavsa.app.core.model.CashTransaction
import com.vyavsa.app.core.model.CounterSale
import com.vyavsa.app.core.model.CounterSalePayment
import com.vyavsa.app.core.model.CreateCounterSaleRequest
import com.vyavsa.app.core.model.CreateCustomerRequest
import com.vyavsa.app.core.model.CreateExpenseRequest
import com.vyavsa.app.core.model.CreateLineSaleRequest
import com.vyavsa.app.core.model.CreatePurchaseRequest
import com.vyavsa.app.core.model.Customer
import com.vyavsa.app.core.model.CustomerBalance
import com.vyavsa.app.core.model.Employee
import com.vyavsa.app.core.model.Expense
import com.vyavsa.app.core.model.LineSale
import com.vyavsa.app.core.model.LineSalePayment
import com.vyavsa.app.core.model.LineSalePaymentBreakdown
import com.vyavsa.app.core.model.PlanTransaction
import com.vyavsa.app.core.model.Purchase
import com.vyavsa.app.core.model.PurchasePayment
import com.vyavsa.app.core.model.RecordSalePaymentRequest
import com.vyavsa.app.core.model.RecordSupplierPaymentRequest
import com.vyavsa.app.core.model.Supplier
import com.vyavsa.app.core.model.SupplierPaymentResponse
import com.vyavsa.app.core.model.UpdateCounterSaleRequest
import com.vyavsa.app.core.model.UpdateLineSaleRequest
import com.vyavsa.app.core.model.UpdatePurchaseRequest
import com.vyavsa.app.core.network.ApiErrorParser
import com.vyavsa.app.core.network.VyavsaApiService
import com.vyavsa.app.core.sync.DataMutation
import com.vyavsa.app.core.sync.DataRefreshCoordinator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.ResponseBody
import javax.inject.Inject
import javax.inject.Singleton

interface TransactionsRepository {
    fun listLineSales(
        date: String? = null,
        search: String? = null,
        fromDate: String? = null,
        toDate: String? = null,
        customerId: String? = null
    ): Flow<Resource<List<LineSale>>>
    fun createLineSale(request: CreateLineSaleRequest): Flow<Resource<LineSale>>
    fun updateLineSale(id: String, request: UpdateLineSaleRequest): Flow<Resource<LineSale>>
    fun getLineSaleById(id: String): Flow<Resource<LineSale>>
    fun listLineSalePayments(id: String): Flow<Resource<List<LineSalePayment>>>
    fun recordLineSalePayment(id: String, request: RecordSalePaymentRequest): Flow<Resource<LineSalePayment>>
    fun getLineSalePaymentBreakdown(
        date: String? = null,
        startDate: String? = null,
        endDate: String? = null
    ): Flow<Resource<LineSalePaymentBreakdown>>

    fun listCounterSales(
        date: String? = null,
        search: String? = null,
        fromDate: String? = null,
        toDate: String? = null,
        customerId: String? = null
    ): Flow<Resource<List<CounterSale>>>
    fun createCounterSale(request: CreateCounterSaleRequest): Flow<Resource<CounterSale>>
    fun updateCounterSale(id: String, request: UpdateCounterSaleRequest): Flow<Resource<CounterSale>>
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
    fun listCustomers(search: String? = null, page: Int = 1, pageSize: Int = 50): Flow<Resource<List<Customer>>>
    fun getCustomerBalance(customerId: String): Flow<Resource<CustomerBalance>>
    fun createCustomer(name: String, phone: String?, openingBalance: String): Flow<Resource<Customer>>
    suspend fun downloadLineSalePdf(id: String): ResponseBody
    suspend fun downloadCounterSalePdf(id: String): ResponseBody
    fun listSuppliers(): Flow<Resource<List<Supplier>>>
    fun listEmployees(): Flow<Resource<List<Employee>>>
}

@Singleton
class TransactionsRepositoryImpl @Inject constructor(
    private val apiService: VyavsaApiService,
    private val refreshCoordinator: DataRefreshCoordinator
) : TransactionsRepository {

    override fun listLineSales(
        date: String?,
        search: String?,
        fromDate: String?,
        toDate: String?,
        customerId: String?
    ): Flow<Resource<List<LineSale>>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.listLineSales(
                date = date,
                fromDate = fromDate,
                toDate = toDate,
                search = search,
                customerId = customerId
            )
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data))
            } else {
                emit(ApiErrorParser.fromApiResponse(response, "Failed to load line sales"))
            }
        } catch (e: Throwable) {
            emit(ApiErrorParser.parse(e, "Network error loading line sales"))
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
                if (request.items.isNotEmpty()) {
                    refreshCoordinator.notifyMutation(DataMutation.PRODUCTS)
                }
                emit(Resource.Success(response.data, response.message))
            } else {
                emit(ApiErrorParser.fromApiResponse(response, "Failed to create line sale"))
            }
        } catch (e: Throwable) {
            emit(ApiErrorParser.parse(e, "Network error creating sale"))
        }
    }.flowOn(Dispatchers.IO)

    override fun updateLineSale(id: String, request: UpdateLineSaleRequest): Flow<Resource<LineSale>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.updateLineSale(id, request)
            if (response.success && response.data != null) {
                refreshCoordinator.notifyMutation(DataMutation.SALES)
                refreshCoordinator.notifyMutation(DataMutation.DUES)
                if (request.items.isNotEmpty()) {
                    refreshCoordinator.notifyMutation(DataMutation.PRODUCTS)
                }
                emit(Resource.Success(response.data, response.message))
            } else {
                emit(ApiErrorParser.fromApiResponse(response, "Failed to update line sale"))
            }
        } catch (e: Throwable) {
            emit(ApiErrorParser.parse(e, "Network error updating line sale"))
        }
    }.flowOn(Dispatchers.IO)

    override fun getLineSaleById(id: String): Flow<Resource<LineSale>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.getLineSaleById(id)
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data))
            } else {
                emit(ApiErrorParser.fromApiResponse(response, "Failed to load line sale details"))
            }
        } catch (e: Throwable) {
            emit(ApiErrorParser.parse(e, "Network error loading sale details"))
        }
    }.flowOn(Dispatchers.IO)

    override fun listLineSalePayments(id: String): Flow<Resource<List<LineSalePayment>>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.listLineSalePayments(id)
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data))
            } else {
                emit(ApiErrorParser.fromApiResponse(response, "Failed to load line sale payments"))
            }
        } catch (e: Throwable) {
            emit(ApiErrorParser.parse(e, "Network error loading payments"))
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
                emit(ApiErrorParser.fromApiResponse(response, "Failed to record payment"))
            }
        } catch (e: Throwable) {
            emit(ApiErrorParser.parse(e, "Network error recording payment"))
        }
    }.flowOn(Dispatchers.IO)

    override fun getLineSalePaymentBreakdown(
        date: String?,
        startDate: String?,
        endDate: String?
    ): Flow<Resource<LineSalePaymentBreakdown>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.getLineSalePaymentBreakdown(
                date = date,
                startDate = startDate,
                endDate = endDate
            )
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data))
            } else {
                emit(ApiErrorParser.fromApiResponse(response, "Failed to load line sale payment breakdown"))
            }
        } catch (e: Throwable) {
            emit(ApiErrorParser.parse(e, "Network error loading line sale payment breakdown"))
        }
    }.flowOn(Dispatchers.IO)

    override fun listCounterSales(
        date: String?,
        search: String?,
        fromDate: String?,
        toDate: String?,
        customerId: String?
    ): Flow<Resource<List<CounterSale>>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.listCounterSales(
                date = date,
                fromDate = fromDate,
                toDate = toDate,
                search = search,
                customerId = customerId
            )
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data))
            } else {
                emit(ApiErrorParser.fromApiResponse(response, "Failed to load counter sales"))
            }
        } catch (e: Throwable) {
            emit(ApiErrorParser.parse(e, "Network error loading counter sales"))
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
                if (!request.customerId.isNullOrBlank()) {
                    refreshCoordinator.notifyMutation(DataMutation.DUES)
                }
                if (request.items.isNotEmpty()) {
                    refreshCoordinator.notifyMutation(DataMutation.PRODUCTS)
                }
                emit(Resource.Success(response.data, response.message))
            } else {
                emit(ApiErrorParser.fromApiResponse(response, "Failed to record counter sale"))
            }
        } catch (e: Throwable) {
            emit(ApiErrorParser.parse(e, "Network error recording sale"))
        }
    }.flowOn(Dispatchers.IO)

    override fun updateCounterSale(id: String, request: UpdateCounterSaleRequest): Flow<Resource<CounterSale>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.updateCounterSale(id, request)
            if (response.success && response.data != null) {
                refreshCoordinator.notifyMutation(DataMutation.SALES)
                if (request.items.isNotEmpty()) {
                    refreshCoordinator.notifyMutation(DataMutation.PRODUCTS)
                }
                emit(Resource.Success(response.data, response.message))
            } else {
                emit(ApiErrorParser.fromApiResponse(response, "Failed to update counter sale"))
            }
        } catch (e: Throwable) {
            emit(ApiErrorParser.parse(e, "Network error updating counter sale"))
        }
    }.flowOn(Dispatchers.IO)

    override fun getCounterSaleById(id: String): Flow<Resource<CounterSale>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.getCounterSaleById(id)
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data))
            } else {
                emit(ApiErrorParser.fromApiResponse(response, "Failed to load counter sale details"))
            }
        } catch (e: Throwable) {
            emit(ApiErrorParser.parse(e, "Network error loading counter sale details"))
        }
    }.flowOn(Dispatchers.IO)

    override fun listCounterSalePayments(id: String): Flow<Resource<List<CounterSalePayment>>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.listCounterSalePayments(id)
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data))
            } else {
                emit(ApiErrorParser.fromApiResponse(response, "Failed to load counter sale payments"))
            }
        } catch (e: Throwable) {
            emit(ApiErrorParser.parse(e, "Network error loading payments"))
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
                emit(ApiErrorParser.fromApiResponse(response, "Failed to record payment"))
            }
        } catch (e: Throwable) {
            emit(ApiErrorParser.parse(e, "Network error recording payment"))
        }
    }.flowOn(Dispatchers.IO)

    override fun listPurchases(date: String?, search: String?, customerId: String?): Flow<Resource<List<Purchase>>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.listPurchases(date = date, search = search, customerId = customerId)
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data))
            } else {
                emit(ApiErrorParser.fromApiResponse(response, "Failed to load purchases"))
            }
        } catch (e: Throwable) {
            emit(ApiErrorParser.parse(e, "Network error loading purchases"))
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
                emit(ApiErrorParser.fromApiResponse(response, "Failed to record purchase"))
            }
        } catch (e: Throwable) {
            emit(ApiErrorParser.parse(e, "Network error recording purchase"))
        }
    }.flowOn(Dispatchers.IO)

    override fun getPurchaseById(id: String): Flow<Resource<Purchase>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.getPurchaseById(id)
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data))
            } else {
                emit(ApiErrorParser.fromApiResponse(response, "Failed to load purchase details"))
            }
        } catch (e: Throwable) {
            emit(ApiErrorParser.parse(e, "Network error loading purchase details"))
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
                emit(ApiErrorParser.fromApiResponse(response, "Failed to update purchase"))
            }
        } catch (e: Throwable) {
            emit(ApiErrorParser.parse(e, "Network error updating purchase"))
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
                emit(ApiErrorParser.fromApiResponse(response, "Failed to delete purchase"))
            }
        } catch (e: Throwable) {
            emit(ApiErrorParser.parse(e, "Network error deleting purchase"))
        }
    }.flowOn(Dispatchers.IO)

    override fun listPurchasePayments(id: String): Flow<Resource<List<PurchasePayment>>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.listPurchasePayments(id)
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data))
            } else {
                emit(ApiErrorParser.fromApiResponse(response, "Failed to load purchase payments"))
            }
        } catch (e: Throwable) {
            emit(ApiErrorParser.parse(e, "Network error loading payments"))
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
                emit(ApiErrorParser.fromApiResponse(response, "Failed to record purchase settlement"))
            }
        } catch (e: Throwable) {
            emit(ApiErrorParser.parse(e, "Network error recording payment"))
        }
    }.flowOn(Dispatchers.IO)

    override fun listExpenses(date: String?, search: String?): Flow<Resource<List<Expense>>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.listExpenses(date = date, search = search)
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data))
            } else {
                emit(ApiErrorParser.fromApiResponse(response, "Failed to load expenses"))
            }
        } catch (e: Throwable) {
            emit(ApiErrorParser.parse(e, "Network error loading expenses"))
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
                emit(ApiErrorParser.fromApiResponse(response, "Failed to record expense"))
            }
        } catch (e: Throwable) {
            emit(ApiErrorParser.parse(e, "Network error recording expense"))
        }
    }.flowOn(Dispatchers.IO)

    override fun listCashTransactions(): Flow<Resource<List<CashTransaction>>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.listCashTransactions()
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data))
            } else {
                emit(ApiErrorParser.fromApiResponse(response, "Failed to load cash transactions"))
            }
        } catch (e: Throwable) {
            emit(ApiErrorParser.parse(e, "Network error loading cash transactions"))
        }
    }.flowOn(Dispatchers.IO)

    override fun listSubscriptionTransactions(): Flow<Resource<List<PlanTransaction>>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.listSubscriptionTransactions()
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data))
            } else {
                emit(ApiErrorParser.fromApiResponse(response, "Failed to load billing history"))
            }
        } catch (e: Throwable) {
            emit(ApiErrorParser.parse(e, "Network error loading billing history"))
        }
    }.flowOn(Dispatchers.IO)

    override fun listCustomers(search: String?, page: Int, pageSize: Int): Flow<Resource<List<Customer>>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.listCustomers(page = page, pageSize = pageSize, search = search)
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data))
            } else {
                emit(ApiErrorParser.fromApiResponse(response, "Failed to load customers"))
            }
        } catch (e: Throwable) {
            emit(ApiErrorParser.parse(e, "Network error loading customers"))
        }
    }.flowOn(Dispatchers.IO)

    override fun getCustomerBalance(customerId: String): Flow<Resource<CustomerBalance>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.getCustomerBalance(customerId)
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data))
            } else {
                emit(ApiErrorParser.fromApiResponse(response, "Failed to load customer balance"))
            }
        } catch (e: Throwable) {
            emit(ApiErrorParser.parse(e, "Network error loading customer balance"))
        }
    }.flowOn(Dispatchers.IO)

    override fun createCustomer(name: String, phone: String?, openingBalance: String): Flow<Resource<Customer>> = flow {
        emit(Resource.Loading)
        try {
            val req = CreateCustomerRequest(
                customerName = name,
                phone = phone?.ifBlank { null },
                openingBalance = openingBalance.ifBlank { "0.00" }
            )
            val response = apiService.createCustomer(req)
            if (response.success && response.data != null) {
                refreshCoordinator.notifyMutation(DataMutation.DUES)
                emit(Resource.Success(response.data))
            } else {
                emit(ApiErrorParser.fromApiResponse(response, "Failed to create customer"))
            }
        } catch (e: Throwable) {
            emit(ApiErrorParser.parse(e, "Network error creating customer"))
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun downloadLineSalePdf(id: String): ResponseBody {
        return apiService.downloadLineSalePdf(id)
    }

    override suspend fun downloadCounterSalePdf(id: String): ResponseBody {
        return apiService.downloadCounterSalePdf(id)
    }

    override fun listSuppliers(): Flow<Resource<List<Supplier>>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.listSuppliers(page = 1, pageSize = 100)
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data))
            } else {
                emit(ApiErrorParser.fromApiResponse(response, "Failed to load suppliers"))
            }
        } catch (e: Throwable) {
            emit(ApiErrorParser.parse(e, "Network error loading suppliers"))
        }
    }.flowOn(Dispatchers.IO)

    override fun listEmployees(): Flow<Resource<List<Employee>>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.listEmployees()
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data))
            } else {
                emit(ApiErrorParser.fromApiResponse(response, "Failed to load employees"))
            }
        } catch (e: Throwable) {
            emit(ApiErrorParser.parse(e, "Network error loading employees"))
        }
    }.flowOn(Dispatchers.IO)
}
