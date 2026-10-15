package com.vyavsa.app.feature.staff

import com.vyavsa.app.core.common.Resource
import com.vyavsa.app.core.model.AttendanceRecord
import com.vyavsa.app.core.model.CreateEmployeeRequest
import com.vyavsa.app.core.model.Employee
import com.vyavsa.app.core.model.EmployeeSalary
import com.vyavsa.app.core.model.PaySalaryRequest
import com.vyavsa.app.core.model.RecordAttendanceRequest
import com.vyavsa.app.core.network.VyavsaApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

interface StaffRepository {
    fun listEmployees(): Flow<Resource<List<Employee>>>
    fun createEmployee(name: String, phone: String?, salary: String, otRate: String): Flow<Resource<Employee>>
    fun listAttendance(date: String?): Flow<Resource<List<AttendanceRecord>>>
    fun recordAttendance(employeeId: String, date: String, status: String, otAmount: String, advance: String): Flow<Resource<AttendanceRecord>>
    fun listPendingSalaries(): Flow<Resource<List<EmployeeSalary>>>
    fun paySalary(employeeId: String, amount: String, paymentMethod: String, date: String?, note: String?): Flow<Resource<Unit>>
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
                emit(Resource.Error(response.error?.message ?: "Failed to load staff list"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Network error"))
        }
    }.flowOn(Dispatchers.IO)

    override fun createEmployee(name: String, phone: String?, salary: String, otRate: String): Flow<Resource<Employee>> = flow {
        emit(Resource.Loading)
        try {
            val req = CreateEmployeeRequest(
                name = name.trim(),
                phone = phone?.trim()?.ifBlank { null },
                salary = salary.trim(),
                otRate = otRate.trim().ifBlank { "0.00" }
            )
            val response = apiService.createEmployee(req)
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data, response.message))
            } else {
                emit(Resource.Error(response.error?.message ?: response.message ?: "Failed to add employee"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Network error"))
        }
    }.flowOn(Dispatchers.IO)

    override fun listAttendance(date: String?): Flow<Resource<List<AttendanceRecord>>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.listAttendance(date)
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data))
            } else {
                emit(Resource.Error(response.error?.message ?: "Failed to load attendance"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Network error"))
        }
    }.flowOn(Dispatchers.IO)

    override fun recordAttendance(
        employeeId: String,
        date: String,
        status: String,
        otAmount: String,
        advance: String
    ): Flow<Resource<AttendanceRecord>> = flow {
        emit(Resource.Loading)
        try {
            val req = RecordAttendanceRequest(
                employeeId = employeeId,
                date = date,
                status = status,
                otAmount = otAmount.ifBlank { "0.00" },
                advance = advance.ifBlank { "0.00" }
            )
            val response = apiService.recordAttendance(req)
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data, response.message))
            } else {
                emit(Resource.Error(response.error?.message ?: response.message ?: "Failed to mark attendance"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Network error"))
        }
    }.flowOn(Dispatchers.IO)

    override fun listPendingSalaries(): Flow<Resource<List<EmployeeSalary>>> = flow {
        emit(Resource.Loading)
        try {
            val response = apiService.listPendingSalaries()
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data))
            } else {
                emit(Resource.Error(response.error?.message ?: "Failed to load pending salaries"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Network error"))
        }
    }.flowOn(Dispatchers.IO)

    override fun paySalary(
        employeeId: String,
        amount: String,
        paymentMethod: String,
        date: String?,
        note: String?
    ): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading)
        try {
            val req = PaySalaryRequest(
                paymentMethod = paymentMethod,
                amount = amount.trim(),
                paymentDate = date,
                note = note
            )
            val response = apiService.paySalary(employeeId, req)
            if (response.success) {
                emit(Resource.Success(Unit, response.message))
            } else {
                emit(Resource.Error(response.error?.message ?: response.message ?: "Failed to disburse salary"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Network error"))
        }
    }.flowOn(Dispatchers.IO)
}
