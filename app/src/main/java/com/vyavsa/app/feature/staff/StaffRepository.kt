package com.vyavsa.app.feature.staff

import com.vyavsa.app.core.common.Resource
import com.vyavsa.app.core.model.AttendanceRecord
import com.vyavsa.app.core.model.CreateEmployeeRequest
import com.vyavsa.app.core.model.Employee
import com.vyavsa.app.core.model.EmployeeSalary
import com.vyavsa.app.core.model.PaySalaryRequest
import com.vyavsa.app.core.model.RecordAttendanceRequest
import com.vyavsa.app.core.network.ApiErrorParser
import com.vyavsa.app.core.network.VyavsaApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import com.vyavsa.app.core.model.UpdateAttendanceRequest
import com.vyavsa.app.core.model.UpdateEmployeeRequest
import com.vyavsa.app.core.model.EmployeeSalaryStatement
import javax.inject.Inject
import javax.inject.Singleton

interface StaffRepository {
    fun listEmployees(): Flow<Resource<List<Employee>>>
    fun createEmployee(name: String, phone: String?, salary: String?, otRate: String): Flow<Resource<Employee>>
    fun updateEmployee(
        id: String,
        name: String?,
        phone: String?,
        salary: String?,
        clearSalary: Boolean = false,
        otRate: String?,
        status: String?
    ): Flow<Resource<Employee>>
    fun deleteEmployee(id: String): Flow<Resource<Unit>>
    fun getEmployeeSalaryStatement(id: String): Flow<Resource<EmployeeSalaryStatement>>
    fun listAttendance(date: String?): Flow<Resource<List<AttendanceRecord>>>
    fun recordAttendance(
        employeeId: String,
        date: String,
        status: String,
        dailySalary: String?,
        otAmount: String,
        advance: String
    ): Flow<Resource<AttendanceRecord>>
    fun updateAttendance(
        id: String,
        status: String?,
        dailySalary: String?,
        clearDailySalary: Boolean = false,
        otAmount: String?,
        advance: String?
    ): Flow<Resource<AttendanceRecord>>
    fun listPendingSalaries(): Flow<Resource<List<EmployeeSalary>>>
    fun paySalary(
        employeeId: String,
        amount: String,
        paymentMethod: String,
        bankId: String? = null,
        bankName: String? = null,
        date: String? = null,
        referenceId: String? = null,
        note: String? = null
    ): Flow<Resource<Unit>>
    fun recordAdvance(
        employeeId: String,
        amount: String,
        date: String? = null,
        paymentMethod: String = "cash",
        bankId: String? = null,
        bankName: String? = null,
        referenceId: String? = null,
        note: String? = null
    ): Flow<Resource<AttendanceRecord>>
    fun recordOvertime(
        employeeId: String,
        amount: String,
        date: String? = null,
        ot: String? = null,
        referenceId: String? = null,
        note: String? = null
    ): Flow<Resource<AttendanceRecord>>
    fun listAdvances(
        page: Int = 1,
        pageSize: Int = 50,
        date: String? = null,
        employeeId: String? = null
    ): Flow<Resource<List<com.vyavsa.app.core.model.EmployeeAdvance>>>
    fun listOvertime(
        page: Int = 1,
        pageSize: Int = 50,
        date: String? = null,
        employeeId: String? = null
    ): Flow<Resource<List<com.vyavsa.app.core.model.EmployeeOvertime>>>
}

@Singleton
class StaffRepositoryImpl @Inject constructor(
    private val apiService: VyavsaApiService
) : StaffRepository {

    override fun listEmployees(): Flow<Resource<List<Employee>>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.listEmployees()
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data))
            } else {
                emit(ApiErrorParser.fromApiResponse(response, "Failed to load staff list"))
            }
        } catch (e: Throwable) {
            emit(ApiErrorParser.parse(e, "Network error loading employees"))
        }
    }.flowOn(Dispatchers.IO)

    override fun createEmployee(name: String, phone: String?, salary: String?, otRate: String): Flow<Resource<Employee>> = flow {
        emit(Resource.Loading)
        try {
            val req = CreateEmployeeRequest(
                name = name.trim(),
                phone = phone?.trim()?.ifBlank { null },
                salary = salary?.trim()?.ifBlank { null },
                otRate = otRate.trim().ifBlank { "0.00" }
            )
            val response = apiService.createEmployee(req)
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data, response.message))
            } else {
                emit(ApiErrorParser.fromApiResponse(response, "Failed to add employee"))
            }
        } catch (e: Throwable) {
            emit(ApiErrorParser.parse(e, "Network error adding employee"))
        }
    }.flowOn(Dispatchers.IO)

    override fun listAttendance(date: String?): Flow<Resource<List<AttendanceRecord>>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.listAttendance(date)
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data))
            } else {
                emit(ApiErrorParser.fromApiResponse(response, "Failed to load attendance"))
            }
        } catch (e: Throwable) {
            emit(ApiErrorParser.parse(e, "Network error loading attendance"))
        }
    }.flowOn(Dispatchers.IO)

    override fun recordAttendance(
        employeeId: String,
        date: String,
        status: String,
        dailySalary: String?,
        otAmount: String,
        advance: String
    ): Flow<Resource<AttendanceRecord>> = flow {
        emit(Resource.Loading)
        try {
            val req = RecordAttendanceRequest(
                employeeId = employeeId,
                date = date,
                status = status,
                dailySalary = dailySalary?.trim()?.ifBlank { null },
                otAmount = otAmount.ifBlank { "0.00" },
                advance = advance.ifBlank { "0.00" }
            )
            val response = apiService.recordAttendance(req)
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data, response.message))
            } else {
                emit(ApiErrorParser.fromApiResponse(response, "Failed to mark attendance"))
            }
        } catch (e: Throwable) {
            emit(ApiErrorParser.parse(e, "Network error recording attendance"))
        }
    }.flowOn(Dispatchers.IO)

    override fun updateAttendance(
        id: String,
        status: String?,
        dailySalary: String?,
        clearDailySalary: Boolean,
        otAmount: String?,
        advance: String?
    ): Flow<Resource<AttendanceRecord>> = flow {
        emit(Resource.Loading)
        try {
            val req = UpdateAttendanceRequest(
                status = status?.ifBlank { null },
                dailySalary = if (clearDailySalary) null else dailySalary?.trim()?.ifBlank { null },
                clearDailySalary = clearDailySalary,
                otAmount = otAmount?.ifBlank { null },
                advance = advance?.ifBlank { null }
            )
            val response = apiService.updateAttendance(id, req)
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data, response.message))
            } else {
                emit(ApiErrorParser.fromApiResponse(response, "Failed to update attendance"))
            }
        } catch (e: Throwable) {
            emit(ApiErrorParser.parse(e, "Network error updating attendance"))
        }
    }.flowOn(Dispatchers.IO)

    override fun listPendingSalaries(): Flow<Resource<List<EmployeeSalary>>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.listPendingSalaries()
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data))
            } else {
                emit(ApiErrorParser.fromApiResponse(response, "Failed to load pending salaries"))
            }
        } catch (e: Throwable) {
            emit(ApiErrorParser.parse(e, "Network error loading salaries"))
        }
    }.flowOn(Dispatchers.IO)

    override fun paySalary(
        employeeId: String,
        amount: String,
        paymentMethod: String,
        bankId: String?,
        bankName: String?,
        date: String?,
        referenceId: String?,
        note: String?
    ): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading)
        try {
            val req = PaySalaryRequest(
                paymentMethod = paymentMethod,
                bankId = if (paymentMethod == "bank") bankId?.trim()?.ifBlank { null } else null,
                bankName = if (paymentMethod == "bank") bankName?.trim()?.ifBlank { null } else null,
                amount = amount.trim(),
                paymentDate = date,
                referenceId = referenceId?.trim()?.ifBlank { null },
                note = note?.trim()?.ifBlank { null }
            )
            val response = apiService.paySalary(employeeId, req)
            if (response.success) {
                emit(Resource.Success(Unit, response.message))
            } else {
                emit(ApiErrorParser.fromApiResponse(response, "Failed to disburse salary"))
            }
        } catch (e: Throwable) {
            emit(ApiErrorParser.parse(e, "Network error paying salary"))
        }
    }.flowOn(Dispatchers.IO)

    override fun updateEmployee(
        id: String,
        name: String?,
        phone: String?,
        salary: String?,
        clearSalary: Boolean,
        otRate: String?,
        status: String?
    ): Flow<Resource<Employee>> = flow {
        emit(Resource.Loading)
        try {
            val req = UpdateEmployeeRequest(
                name = name?.trim()?.ifBlank { null },
                phone = phone?.trim()?.ifBlank { null },
                salary = if (clearSalary) null else salary?.trim()?.ifBlank { null },
                clearSalary = clearSalary,
                otRate = otRate?.trim()?.ifBlank { null },
                status = status?.ifBlank { null }
            )
            val response = apiService.updateEmployee(id, req)
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data, response.message ?: "Employee updated successfully"))
            } else {
                emit(ApiErrorParser.fromApiResponse(response, "Failed to update employee"))
            }
        } catch (e: Throwable) {
            emit(ApiErrorParser.parse(e, "Network error updating employee"))
        }
    }.flowOn(Dispatchers.IO)

    override fun deleteEmployee(id: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.deleteEmployee(id)
            if (response.success) {
                emit(Resource.Success(Unit, response.message ?: "Employee deleted successfully"))
            } else {
                emit(ApiErrorParser.fromApiResponse(response, "Failed to delete employee"))
            }
        } catch (e: Throwable) {
            emit(ApiErrorParser.parse(e, "Network error deleting employee"))
        }
    }.flowOn(Dispatchers.IO)

    override fun getEmployeeSalaryStatement(id: String): Flow<Resource<EmployeeSalaryStatement>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.getEmployeeSalaryStatement(id)
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data))
            } else {
                emit(ApiErrorParser.fromApiResponse(response, "Failed to load salary statement"))
            }
        } catch (e: Throwable) {
            emit(ApiErrorParser.parse(e, "Network error loading salary statement"))
        }
    }.flowOn(Dispatchers.IO)

    override fun recordAdvance(
        employeeId: String,
        amount: String,
        date: String?,
        paymentMethod: String,
        bankId: String?,
        bankName: String?,
        referenceId: String?,
        note: String?
    ): Flow<Resource<AttendanceRecord>> = flow {
        emit(Resource.Loading)
        try {
            val req = com.vyavsa.app.core.model.RecordAdvanceRequest(
                employeeId = employeeId,
                date = date,
                amount = amount.trim(),
                paymentMethod = paymentMethod,
                bankId = bankId,
                bankName = bankName,
                referenceId = referenceId,
                note = note
            )
            val response = apiService.recordAdvance(req)
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data, response.message ?: "Advance recorded successfully"))
            } else {
                emit(ApiErrorParser.fromApiResponse(response, "Failed to record advance"))
            }
        } catch (e: Throwable) {
            emit(ApiErrorParser.parse(e, "Network error recording advance"))
        }
    }.flowOn(Dispatchers.IO)

    override fun recordOvertime(
        employeeId: String,
        amount: String,
        date: String?,
        ot: String?,
        referenceId: String?,
        note: String?
    ): Flow<Resource<AttendanceRecord>> = flow {
        emit(Resource.Loading)
        try {
            val req = com.vyavsa.app.core.model.RecordOvertimeRequest(
                employeeId = employeeId,
                date = date,
                amount = amount.trim(),
                ot = ot,
                referenceId = referenceId,
                note = note
            )
            val response = apiService.recordOvertime(req)
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data, response.message ?: "Overtime recorded successfully"))
            } else {
                emit(ApiErrorParser.fromApiResponse(response, "Failed to record overtime"))
            }
        } catch (e: Throwable) {
            emit(ApiErrorParser.parse(e, "Network error recording overtime"))
        }
    }.flowOn(Dispatchers.IO)

    override fun listAdvances(
        page: Int,
        pageSize: Int,
        date: String?,
        employeeId: String?
    ): Flow<Resource<List<com.vyavsa.app.core.model.EmployeeAdvance>>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.listAdvances(page = page, pageSize = pageSize, date = date, employeeId = employeeId)
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data))
            } else {
                emit(ApiErrorParser.fromApiResponse(response, "Failed to load advances"))
            }
        } catch (e: Throwable) {
            emit(ApiErrorParser.parse(e, "Network error loading advances"))
        }
    }.flowOn(Dispatchers.IO)

    override fun listOvertime(
        page: Int,
        pageSize: Int,
        date: String?,
        employeeId: String?
    ): Flow<Resource<List<com.vyavsa.app.core.model.EmployeeOvertime>>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.listOvertime(page = page, pageSize = pageSize, date = date, employeeId = employeeId)
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data))
            } else {
                emit(ApiErrorParser.fromApiResponse(response, "Failed to load overtime"))
            }
        } catch (e: Throwable) {
            emit(ApiErrorParser.parse(e, "Network error loading overtime"))
        }
    }.flowOn(Dispatchers.IO)
}
