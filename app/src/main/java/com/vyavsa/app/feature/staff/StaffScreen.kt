package com.vyavsa.app.feature.staff

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vyavsa.app.core.common.FormatUtils
import com.vyavsa.app.core.common.UiState
import com.vyavsa.app.core.designsystem.Amber50
import com.vyavsa.app.core.designsystem.Amber600
import com.vyavsa.app.core.designsystem.Crimson50
import com.vyavsa.app.core.designsystem.Crimson600
import com.vyavsa.app.core.designsystem.Emerald50
import com.vyavsa.app.core.designsystem.Emerald600
import com.vyavsa.app.core.designsystem.EmptyStateView
import com.vyavsa.app.core.designsystem.ErrorBanner
import com.vyavsa.app.core.designsystem.StatBadge
import com.vyavsa.app.core.designsystem.VyavsaTextField
import com.vyavsa.app.core.designsystem.VyavsaTopAppBar
import com.vyavsa.app.core.model.AttendanceRecord
import com.vyavsa.app.core.model.Employee
import com.vyavsa.app.core.model.EmployeeSalary

@Composable
fun StaffScreen(
    viewModel: StaffViewModel,
    modifier: Modifier = Modifier
) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val employeesState by viewModel.employeesState.collectAsState()
    val attendanceState by viewModel.attendanceState.collectAsState()
    val salariesState by viewModel.salariesState.collectAsState()
    val actionState by viewModel.actionState.collectAsState()
    val isAdmin = viewModel.tokenManager.isTenantAdmin()

    var showAddEmployeeDialog by remember { mutableStateOf(false) }
    var markingAttendanceEmployee by remember { mutableStateOf<Employee?>(null) }
    var disbursingSalaryItem by remember { mutableStateOf<EmployeeSalary?>(null) }

    Scaffold(
        topBar = {
            VyavsaTopAppBar(title = "Staff & Payroll")
        },
        floatingActionButton = {
            if (isAdmin) {
                FloatingActionButton(
                    onClick = { showAddEmployeeDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Staff")
                }
            }
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Tab Header
            PrimaryTabRow(
                selectedTabIndex = selectedTab.ordinal,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = selectedTab == StaffTab.ATTENDANCE,
                    onClick = { viewModel.selectTab(StaffTab.ATTENDANCE) },
                    text = { Text("Daily Attendance") }
                )
                if (isAdmin) {
                    Tab(
                        selected = selectedTab == StaffTab.SALARIES,
                        onClick = { viewModel.selectTab(StaffTab.SALARIES) },
                        text = { Text("Salaries Due") }
                    )
                }
                Tab(
                    selected = selectedTab == StaffTab.ROSTER,
                    onClick = { viewModel.selectTab(StaffTab.ROSTER) },
                    text = { Text("Staff Roster") }
                )
            }

            if (actionState is UiState.Error) {
                ErrorBanner(
                    message = (actionState as UiState.Error).message,
                    modifier = Modifier.padding(16.dp)
                )
            }

            when (selectedTab) {
                StaffTab.ATTENDANCE -> {
                    AttendanceTabContent(
                        employeesState = employeesState,
                        attendanceState = attendanceState,
                        onMarkAttendance = { markingAttendanceEmployee = it }
                    )
                }
                StaffTab.SALARIES -> {
                    SalariesTabContent(
                        salariesState = salariesState,
                        onDisburseSalary = { disbursingSalaryItem = it }
                    )
                }
                StaffTab.ROSTER -> {
                    RosterTabContent(
                        employeesState = employeesState,
                        onAddEmployee = { showAddEmployeeDialog = true }
                    )
                }
            }
        }
    }

    // Mark Attendance Dialog
    if (markingAttendanceEmployee != null) {
        MarkAttendanceDialog(
            employee = markingAttendanceEmployee!!,
            onDismiss = { markingAttendanceEmployee = null },
            onConfirm = { status, otAmount, advance ->
                viewModel.markAttendance(
                    employeeId = markingAttendanceEmployee!!.id,
                    status = status,
                    otAmount = otAmount,
                    advance = advance
                )
                markingAttendanceEmployee = null
            }
        )
    }

    // Add Employee Dialog
    if (showAddEmployeeDialog) {
        AddEmployeeDialog(
            onDismiss = { showAddEmployeeDialog = false },
            onConfirm = { name, phone, salary, otRate ->
                viewModel.createEmployee(name, phone, salary, otRate)
                showAddEmployeeDialog = false
            }
        )
    }

    // Disburse Salary Dialog
    if (disbursingSalaryItem != null) {
        DisburseSalaryDialog(
            salary = disbursingSalaryItem!!,
            onDismiss = { disbursingSalaryItem = null },
            onConfirm = { amount, method, note ->
                viewModel.paySalary(
                    employeeId = disbursingSalaryItem!!.employeeId,
                    amount = amount,
                    paymentMethod = method,
                    note = note
                )
                disbursingSalaryItem = null
            }
        )
    }
}

@Composable
private fun AttendanceTabContent(
    employeesState: UiState<List<Employee>>,
    attendanceState: UiState<List<AttendanceRecord>>,
    onMarkAttendance: (Employee) -> Unit
) {
    val employees = (employeesState as? UiState.Success)?.data ?: emptyList()
    val attendance = (attendanceState as? UiState.Success)?.data ?: emptyList()
    val attendanceMap = attendance.associateBy { it.employeeId }

    if (employees.isEmpty()) {
        EmptyStateView(
            title = "No Staff Added",
            description = "Add your business employees to start logging attendance and advances",
            icon = Icons.Default.Badge
        )
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(employees) { employee ->
            val record = attendanceMap[employee.id]
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = employee.name,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                text = "Base Salary: ${FormatUtils.formatCurrency(employee.salary)}/mo",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                    }

                    if (record != null) {
                        Column(horizontalAlignment = Alignment.End) {
                            val (badgeBg, badgeText, textCol) = when (record.status.lowercase()) {
                                "present" -> Triple(Emerald50, "Present", Emerald600)
                                "absent" -> Triple(Crimson50, "Absent", Crimson600)
                                "half_day" -> Triple(Amber50, "Half Day", Amber600)
                                else -> Triple(MaterialTheme.colorScheme.surfaceVariant, record.status, MaterialTheme.colorScheme.primary)
                            }
                            StatBadge(text = badgeText, backgroundColor = badgeBg, textColor = textCol)
                            if (record.advance.toDoubleOrNull() ?: 0.0 > 0.0) {
                                Text(
                                    text = "Adv: ${FormatUtils.formatCurrency(record.advance)}",
                                    style = MaterialTheme.typography.labelSmall.copy(color = Crimson600)
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "Mark Today",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onMarkAttendance(employee) }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SalariesTabContent(
    salariesState: UiState<List<EmployeeSalary>>,
    onDisburseSalary: (EmployeeSalary) -> Unit
) {
    when (salariesState) {
        is UiState.Loading -> {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }
        is UiState.Empty -> {
            EmptyStateView(
                title = "No Pending Salaries",
                description = "All employee salaries and wages are up to date",
                icon = Icons.Default.CheckCircle
            )
        }
        is UiState.Error -> {
            Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                ErrorBanner(message = salariesState.message)
            }
        }
        is UiState.Success -> {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(salariesState.data) { salary ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = salary.employeeName,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "Pending Net",
                                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    )
                                    Text(
                                        text = FormatUtils.formatCurrency(salary.balance),
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Crimson600
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Gross: ${FormatUtils.formatCurrency(salary.grossSalary)}",
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                                Text(
                                    text = "Advances Paid: ${FormatUtils.formatCurrency(salary.totalAdvances)}",
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                                Text(
                                    text = "Disburse Salary Payout →",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable { onDisburseSalary(salary) }
                                        .padding(4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
        else -> {}
    }
}

@Composable
private fun RosterTabContent(
    employeesState: UiState<List<Employee>>,
    onAddEmployee: () -> Unit
) {
    val employees = (employeesState as? UiState.Success)?.data ?: emptyList()

    if (employees.isEmpty()) {
        EmptyStateView(
            title = "No Staff Registered",
            description = "Add staff to manage attendance, overtime, advances, and payroll",
            icon = Icons.Default.Badge,
            actionText = "Add First Employee",
            onActionClick = onAddEmployee
        )
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(employees) { employee ->
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.padding(14.dp)
                ) {
                    Column {
                        Text(
                            text = employee.name,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        if (!employee.phone.isNullOrBlank()) {
                            Text(
                                text = employee.phone,
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = FormatUtils.formatCurrency(employee.salary),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        StatBadge(text = employee.status.uppercase())
                    }
                }
            }
        }
    }
}

@Composable
fun MarkAttendanceDialog(
    employee: Employee,
    onDismiss: () -> Unit,
    onConfirm: (status: String, otAmount: String, advance: String) -> Unit
) {
    var status by remember { mutableStateOf("present") }
    var otAmount by remember { mutableStateOf("") }
    var advance by remember { mutableStateOf("") }

    val statusOptions = listOf("present", "half_day", "absent", "leave")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mark Attendance: ${employee.name}", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Select Status", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    statusOptions.take(2).forEach { s ->
                        FilterChip(
                            selected = status == s,
                            onClick = { status = s },
                            label = { Text(s.replace("_", " ").replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    statusOptions.drop(2).forEach { s ->
                        FilterChip(
                            selected = status == s,
                            onClick = { status = s },
                            label = { Text(s.replace("_", " ").replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }

                VyavsaTextField(
                    value = otAmount,
                    onValueChange = { otAmount = it },
                    label = "Overtime Amount (₹)",
                    placeholder = "0.00",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )

                VyavsaTextField(
                    value = advance,
                    onValueChange = { advance = it },
                    label = "Cash Advance Paid (₹)",
                    placeholder = "0.00",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(status, otAmount, advance) }) {
                Text("Submit Attendance", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AddEmployeeDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, phone: String?, salary: String, otRate: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var salary by remember { mutableStateOf("") }
    var otRate by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Employee", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                VyavsaTextField(value = name, onValueChange = { name = it }, label = "Full Name *")
                VyavsaTextField(value = phone, onValueChange = { phone = it }, label = "Phone Number", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
                VyavsaTextField(value = salary, onValueChange = { salary = it }, label = "Monthly Base Salary (₹) *", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                VyavsaTextField(value = otRate, onValueChange = { otRate = it }, label = "Overtime Rate (₹/hr)", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name, phone, salary, otRate) },
                enabled = name.isNotBlank() && salary.isNotBlank()
            ) {
                Text("Add Staff", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun DisburseSalaryDialog(
    salary: EmployeeSalary,
    onDismiss: () -> Unit,
    onConfirm: (amount: String, method: String, note: String?) -> Unit
) {
    var amount by remember { mutableStateOf(salary.balance) }
    var method by remember { mutableStateOf("cash") }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Disburse Salary: ${salary.employeeName}", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Pending Payable: ${FormatUtils.formatCurrency(salary.balance)}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, color = Crimson600)
                )
                VyavsaTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = "Disbursement Amount (₹) *",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                VyavsaTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = "Note / Reference",
                    placeholder = "Salary payout"
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(amount, method, note) },
                enabled = amount.isNotBlank()
            ) {
                Text("Disburse Payout", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
