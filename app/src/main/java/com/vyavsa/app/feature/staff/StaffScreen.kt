package com.vyavsa.app.feature.staff

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vyavsa.app.core.common.FormatUtils
import com.vyavsa.app.core.common.UiState
import com.vyavsa.app.core.designsystem.Amber50
import com.vyavsa.app.core.designsystem.Amber600
import com.vyavsa.app.core.designsystem.AvatarInitialCircle
import com.vyavsa.app.core.designsystem.Blue50
import com.vyavsa.app.core.designsystem.Blue600
import com.vyavsa.app.core.designsystem.Crimson50
import com.vyavsa.app.core.designsystem.Crimson600
import com.vyavsa.app.core.designsystem.Emerald50
import com.vyavsa.app.core.designsystem.Emerald600
import com.vyavsa.app.core.designsystem.EmptyStateView
import com.vyavsa.app.core.designsystem.ErrorBanner
import com.vyavsa.app.core.designsystem.StatBadge
import com.vyavsa.app.core.designsystem.VyavsaSegmentedTabs
import com.vyavsa.app.core.designsystem.VyavsaTextField
import com.vyavsa.app.core.designsystem.VyavsaTopAppBar
import com.vyavsa.app.core.designsystem.vyavsaClickable
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
    val isAdmin = viewModel.isAdmin

    var showAddEmployeeDialog by remember { mutableStateOf(false) }
    var markingAttendanceEmployee by remember { mutableStateOf<Employee?>(null) }
    var editingAttendancePair by remember { mutableStateOf<Pair<Employee, AttendanceRecord>?>(null) }
    var disbursingSalaryItem by remember { mutableStateOf<EmployeeSalary?>(null) }
    var employeeToEdit by remember { mutableStateOf<Employee?>(null) }
    var employeeToDelete by remember { mutableStateOf<Employee?>(null) }

    val visibleTabs = remember(isAdmin) {
        if (isAdmin) {
            listOf(StaffTab.ATTENDANCE, StaffTab.SALARIES, StaffTab.ROSTER)
        } else {
            listOf(StaffTab.ATTENDANCE, StaffTab.ROSTER)
        }
    }
    val tabTitles = remember(visibleTabs) {
        visibleTabs.map {
            when (it) {
                StaffTab.ATTENDANCE -> "Attendance"
                StaffTab.SALARIES -> "Salaries"
                StaffTab.ROSTER -> "Roster"
            }
        }
    }
    val currentTabIndex = visibleTabs.indexOf(selectedTab).coerceAtLeast(0)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            VyavsaTopAppBar(
                title = "Staff & Payroll",
                subtitle = "Daily attendance, advances & salary payouts"
            )
        },
        floatingActionButton = {
            if (isAdmin) {
                ExtendedFloatingActionButton(
                    onClick = { showAddEmployeeDialog = true },
                    shape = RoundedCornerShape(20.dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    icon = { Icon(imageVector = Icons.Default.Add, contentDescription = null) },
                    text = { Text("Add Staff", fontWeight = FontWeight.Bold) }
                )
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
            // Pill Segmented Tab Header
            VyavsaSegmentedTabs(
                tabs = tabTitles,
                selectedIndex = currentTabIndex,
                onTabSelected = { index ->
                    viewModel.selectTab(visibleTabs[index])
                },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )

            if (actionState is UiState.Error) {
                ErrorBanner(
                    message = (actionState as UiState.Error).message,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                )
            }

            // Directional Animated Sub-Tab Transition
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    val direction = if (targetState.ordinal >= initialState.ordinal) 1 else -1
                    (fadeIn(tween(220, easing = FastOutSlowInEasing)) +
                        slideInHorizontally(
                            initialOffsetX = { (it * 0.08f * direction).toInt() },
                            animationSpec = tween(240, easing = FastOutSlowInEasing)
                        )) togetherWith
                        (fadeOut(tween(160, easing = FastOutSlowInEasing)) +
                            slideOutHorizontally(
                                targetOffsetX = { (-it * 0.06f * direction).toInt() },
                                animationSpec = tween(180, easing = FastOutSlowInEasing)
                            ))
                },
                label = "StaffSubTabTransition",
                modifier = Modifier.fillMaxSize()
            ) { currentTab ->
                when (currentTab) {
                    StaffTab.ATTENDANCE -> {
                        AttendanceTabContent(
                            employeesState = employeesState,
                            attendanceState = attendanceState,
                            isAdmin = isAdmin,
                            onMarkAttendance = { markingAttendanceEmployee = it },
                            onEditAttendance = { emp, record -> editingAttendancePair = emp to record }
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
                            isAdmin = isAdmin,
                            onAddEmployee = { showAddEmployeeDialog = true },
                            onEditEmployee = { employeeToEdit = it },
                            onDeleteEmployee = { employeeToDelete = it }
                        )
                    }
                }
            }
        }
    }

    // Mark Attendance Dialog
    if (markingAttendanceEmployee != null) {
        MarkAttendanceDialog(
            employee = markingAttendanceEmployee!!,
            onDismiss = { markingAttendanceEmployee = null },
            onConfirm = { status, dailySalary, otAmount, advance ->
                viewModel.markAttendance(
                    employeeId = markingAttendanceEmployee!!.id,
                    status = status,
                    dailySalary = dailySalary,
                    otAmount = otAmount,
                    advance = advance
                )
                markingAttendanceEmployee = null
            }
        )
    }

    // Edit Attendance Dialog
    if (editingAttendancePair != null) {
        val (emp, record) = editingAttendancePair!!
        EditAttendanceDialog(
            employee = emp,
            record = record,
            onDismiss = { editingAttendancePair = null },
            onConfirm = { status, dailySalary, clearDailySalary, otAmount, advance ->
                viewModel.updateAttendance(
                    attendanceId = record.id,
                    status = status,
                    dailySalary = dailySalary,
                    clearDailySalary = clearDailySalary,
                    otAmount = otAmount,
                    advance = advance
                )
                editingAttendancePair = null
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

    // Edit Employee Dialog
    if (employeeToEdit != null) {
        EditEmployeeDialog(
            employee = employeeToEdit!!,
            onDismiss = { employeeToEdit = null },
            onConfirm = { name, phone, salary, clearSalary, otRate, status ->
                viewModel.updateEmployee(
                    id = employeeToEdit!!.id,
                    name = name,
                    phone = phone,
                    salary = salary,
                    clearSalary = clearSalary,
                    otRate = otRate,
                    status = status
                )
                employeeToEdit = null
            }
        )
    }

    // Delete Employee Confirmation Dialog
    if (employeeToDelete != null) {
        AlertDialog(
            onDismissRequest = { employeeToDelete = null },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Delete Employee Record", fontWeight = FontWeight.ExtraBold) },
            text = {
                Text("Are you sure you want to remove ${employeeToDelete!!.name}? This will remove the employee from the roster.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteEmployee(employeeToDelete!!.id)
                        employeeToDelete = null
                    }
                ) {
                    Text("Delete Permanently", color = Crimson600, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { employeeToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AttendanceTabContent(
    employeesState: UiState<List<Employee>>,
    attendanceState: UiState<List<AttendanceRecord>>,
    isAdmin: Boolean = false,
    onMarkAttendance: (Employee) -> Unit,
    onEditAttendance: (Employee, AttendanceRecord) -> Unit = { _, _ -> }
) {
    val employees = (employeesState as? UiState.Success)?.data ?: emptyList()
    val attendance = (attendanceState as? UiState.Success)?.data ?: emptyList()
    val attendanceMap = remember(attendance) { attendance.associateBy { it.employeeId } }

    if (employeesState is UiState.Loading && employees.isEmpty()) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    if (employees.isEmpty()) {
        EmptyStateView(
            title = "No Staff Added",
            description = "Add your business employees to start logging attendance and advances",
            icon = Icons.Default.Badge
        )
        return
    }

    val presentCount = remember(attendance) {
        attendance.count { it.status.equals("present", ignoreCase = true) || it.status.equals("half_day", ignoreCase = true) }
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item(key = "attendance_summary_header") {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = "Today's Roll Call (${FormatUtils.formatDateForDisplay(FormatUtils.todayDateString())})",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    StatBadge(
                        text = "$presentCount / ${employees.size} Present",
                        backgroundColor = Emerald50,
                        textColor = Emerald600
                    )
                }
            }
        }

        items(
            items = employees,
            key = { "att_${it.id}" },
            contentType = { "employee_attendance" }
        ) { employee ->
            val record = attendanceMap[employee.id]
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .animateItemPlacement()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f), RoundedCornerShape(24.dp))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        AvatarInitialCircle(name = employee.name)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = employee.name,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            val salaryText = if (!employee.salary.isNullOrBlank()) {
                                "Default Wage: ${FormatUtils.formatCurrency(employee.salary)}/day"
                            } else {
                                "No fixed salary • Daily wage"
                            }
                            Text(
                                text = salaryText,
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    if (record != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.End) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    val (badgeBg, badgeText, textCol) = when (record.status.lowercase()) {
                                        "present" -> Triple(Emerald50, "Present", Emerald600)
                                        "absent" -> Triple(Crimson50, "Absent", Crimson600)
                                        "half_day" -> Triple(Amber50, "Half Day", Amber600)
                                        else -> Triple(MaterialTheme.colorScheme.surfaceVariant, record.status, MaterialTheme.colorScheme.primary)
                                    }
                                    StatBadge(text = badgeText, backgroundColor = badgeBg, textColor = textCol)

                                    val sourceBadge = when (record.salarySource?.lowercase()) {
                                        "override" -> Triple(Amber50, "Override", Amber600)
                                        "manual" -> Triple(Emerald50, "Manual", Emerald600)
                                        "default" -> Triple(Blue50, "Default", Blue600)
                                        "pending" -> Triple(Amber50, "Pending", Amber600)
                                        "unpaid" -> Triple(MaterialTheme.colorScheme.surfaceVariant, "Unpaid", MaterialTheme.colorScheme.onSurfaceVariant)
                                        else -> null
                                    }
                                    if (sourceBadge != null) {
                                        StatBadge(text = sourceBadge.second, backgroundColor = sourceBadge.first, textColor = sourceBadge.third)
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                if (!record.dailySalary.isNullOrBlank()) {
                                    Text(
                                        text = "Wage: ${FormatUtils.formatCurrency(record.dailySalary)}",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                } else {
                                    Text(
                                        text = "Wage Pending",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = Amber600
                                        )
                                    )
                                }

                                if (record.advance.toDoubleOrNull() ?: 0.0 > 0.0) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Adv: ${FormatUtils.formatCurrency(record.advance)}",
                                        style = MaterialTheme.typography.labelSmall.copy(color = Crimson600, fontWeight = FontWeight.SemiBold)
                                    )
                                }
                            }

                            if (isAdmin) {
                                FilledTonalIconButton(
                                    onClick = { onEditAttendance(employee, record) },
                                    shape = CircleShape,
                                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        contentColor = MaterialTheme.colorScheme.onSurface
                                    ),
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit Attendance",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier
                                .clip(CircleShape)
                                .vyavsaClickable { onMarkAttendance(employee) }
                        ) {
                            Text(
                                text = "Mark Today",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
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
            Box(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                ErrorBanner(message = salariesState.message)
            }
        }
        is UiState.Success -> {
            LazyColumn(
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(
                    items = salariesState.data,
                    key = { it.id.ifBlank { it.employeeId } },
                    contentType = { "salary_item" }
                ) { salary ->
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItemPlacement()
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f), RoundedCornerShape(24.dp))
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    AvatarInitialCircle(name = salary.employeeName)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = salary.employeeName,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "Pending Net",
                                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    )
                                    Text(
                                        text = FormatUtils.formatCurrency(salary.balance),
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Crimson600
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
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
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .vyavsaClickable { onDisburseSalary(salary) }
                                ) {
                                    Text(
                                        text = "Disburse Salary Payout →",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        else -> {}
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RosterTabContent(
    employeesState: UiState<List<Employee>>,
    isAdmin: Boolean,
    onAddEmployee: () -> Unit,
    onEditEmployee: (Employee) -> Unit,
    onDeleteEmployee: (Employee) -> Unit
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
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(
            items = employees,
            key = { "roster_${it.id}" },
            contentType = { "employee_roster" }
        ) { employee ->
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .animateItemPlacement()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f), RoundedCornerShape(24.dp))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        AvatarInitialCircle(name = employee.name)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = employee.name,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (!employee.phone.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = employee.phone,
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.End) {
                            if (employee.salary.isNullOrBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                ) {
                                    Text(
                                        text = "Daily Wage",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            } else {
                                Text(
                                    text = FormatUtils.formatCurrency(employee.salary),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            StatBadge(text = employee.status.uppercase())
                        }

                        if (isAdmin) {
                            Spacer(modifier = Modifier.width(4.dp))
                            FilledTonalIconButton(
                                onClick = { onEditEmployee(employee) },
                                shape = CircleShape,
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                ),
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Employee",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            FilledTonalIconButton(
                                onClick = { onDeleteEmployee(employee) },
                                shape = CircleShape,
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = Crimson50,
                                    contentColor = Crimson600
                                ),
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Employee",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
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
    onConfirm: (status: String, dailySalary: String?, otAmount: String, advance: String) -> Unit
) {
    var status by remember { mutableStateOf("present") }

    val baseSalary = employee.salary?.toDoubleOrNull()
    val initialCalculatedSalary = remember(employee.salary) {
        if (baseSalary != null) {
            String.format(java.util.Locale.US, "%.2f", baseSalary)
        } else {
            ""
        }
    }
    var dailySalary by remember { mutableStateOf(initialCalculatedSalary) }
    var isManualOverride by remember { mutableStateOf(false) }
    var otAmount by remember { mutableStateOf("") }
    var advance by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }

    fun updateStatus(newStatus: String) {
        status = newStatus
        if (!isManualOverride && baseSalary != null) {
            val autoAmount = when (newStatus) {
                "present" -> baseSalary
                "half_day" -> Math.round((baseSalary / 2.0) * 100.0) / 100.0
                "absent", "leave" -> 0.0
                else -> baseSalary
            }
            dailySalary = String.format(java.util.Locale.US, "%.2f", autoAmount)
        }
    }

    val statusOptions = listOf(
        "present" to "Present",
        "half_day" to "Half Day",
        "absent" to "Absent",
        "leave" to "Leave"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Mark Attendance: ${employee.name}", fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Select Status", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                statusOptions.chunked(2).forEach { rowItems ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        rowItems.forEach { (key, label) ->
                            val isSelected = status == key
                            FilterChip(
                                selected = isSelected,
                                onClick = { updateStatus(key) },
                                shape = CircleShape,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                label = {
                                    Text(
                                        text = label,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Today's Wage / Daily Salary",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    if (baseSalary != null) {
                        Surface(
                            shape = CircleShape,
                            color = if (isManualOverride) Amber50 else Blue50
                        ) {
                            Text(
                                text = if (isManualOverride) "Override" else "Default: ₹${FormatUtils.formatCurrency(employee.salary)}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isManualOverride) Amber600 else Blue600
                                ),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    } else {
                        Surface(
                            shape = CircleShape,
                            color = Amber50
                        ) {
                            Text(
                                text = "Daily Wage / Flexible",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Amber600
                                ),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                VyavsaTextField(
                    value = dailySalary,
                    onValueChange = {
                        dailySalary = it
                        isManualOverride = true
                        validationError = null
                    },
                    label = "Daily Wage (₹)",
                    placeholder = if (baseSalary == null) "Leave blank if pending/unpaid" else "0.00",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )

                Text(
                    text = if (baseSalary != null) {
                        "Applies to this date only without modifying employee's default salary."
                    } else {
                        "No fixed salary configured. Enter today's wage or leave blank if unpaid/pending."
                    },
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )

                if (validationError != null) {
                    Text(
                        text = validationError!!,
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.error)
                    )
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
            TextButton(
                onClick = {
                    val trimmed = dailySalary.trim()
                    if (trimmed.isNotEmpty()) {
                        val parsed = trimmed.toDoubleOrNull()
                        if (parsed == null || parsed < 0.0) {
                            validationError = "Daily salary must be a valid non-negative amount"
                            return@TextButton
                        }
                    }
                    onConfirm(status, trimmed.ifBlank { null }, otAmount.trim(), advance.trim())
                }
            ) {
                Text("Submit Attendance", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun EditAttendanceDialog(
    employee: Employee,
    record: AttendanceRecord,
    onDismiss: () -> Unit,
    onConfirm: (status: String, dailySalary: String?, clearDailySalary: Boolean, otAmount: String, advance: String) -> Unit
) {
    var status by remember { mutableStateOf(record.status) }
    var dailySalary by remember {
        mutableStateOf(
            record.dailySalary?.let {
                val d = it.toDoubleOrNull()
                if (d != null) String.format(java.util.Locale.US, "%.2f", d) else it
            } ?: ""
        )
    }
    var otAmount by remember {
        mutableStateOf(
            if ((record.otAmount?.toDoubleOrNull() ?: 0.0) > 0) {
                record.otAmount ?: ""
            } else {
                record.ot ?: ""
            }
        )
    }
    var advance by remember { mutableStateOf(record.advance ?: "") }
    var validationError by remember { mutableStateOf<String?>(null) }

    val statusOptions = listOf(
        "present" to "Present",
        "half_day" to "Half Day",
        "absent" to "Absent",
        "leave" to "Leave"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Edit Attendance: ${employee.name}", fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Status", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                statusOptions.chunked(2).forEach { rowItems ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        rowItems.forEach { (key, label) ->
                            val isSelected = status == key
                            FilterChip(
                                selected = isSelected,
                                onClick = { status = key },
                                shape = CircleShape,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                label = {
                                    Text(
                                        text = label,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Daily Salary / Wage (₹)",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    if (!record.salarySource.isNullOrBlank()) {
                        Surface(
                            shape = CircleShape,
                            color = when (record.salarySource.lowercase()) {
                                "override" -> Amber50
                                "manual" -> Emerald50
                                "default" -> Blue50
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                        ) {
                            Text(
                                text = "Source: ${record.salarySource.uppercase()}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = when (record.salarySource.lowercase()) {
                                        "override" -> Amber600
                                        "manual" -> Emerald600
                                        "default" -> Blue600
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                ),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                VyavsaTextField(
                    value = dailySalary,
                    onValueChange = {
                        dailySalary = it
                        validationError = null
                    },
                    label = "Daily Wage (₹)",
                    placeholder = "Leave empty to clear daily salary",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )

                Text(
                    text = "Clear to reset daily wage or leave as unpaid/pending.",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )

                if (validationError != null) {
                    Text(
                        text = validationError!!,
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.error)
                    )
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
            TextButton(
                onClick = {
                    val trimmed = dailySalary.trim()
                    if (trimmed.isNotEmpty()) {
                        val parsed = trimmed.toDoubleOrNull()
                        if (parsed == null || parsed < 0.0) {
                            validationError = "Daily salary must be a valid non-negative amount"
                            return@TextButton
                        }
                    }
                    val clearDailySalary = trimmed.isEmpty() && record.dailySalary != null
                    val finalDailySalary = if (trimmed.isEmpty()) null else trimmed
                    onConfirm(status, finalDailySalary, clearDailySalary, otAmount.trim(), advance.trim())
                }
            ) {
                Text("Save Changes", fontWeight = FontWeight.Bold)
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
    onConfirm: (name: String, phone: String?, salary: String?, otRate: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var salary by remember { mutableStateOf("") }
    var otRate by remember { mutableStateOf("") }
    var salaryError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Add New Employee", fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                VyavsaTextField(value = name, onValueChange = { name = it }, label = "Full Name *")
                VyavsaTextField(value = phone, onValueChange = { phone = it }, label = "Phone Number", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
                
                Column {
                    VyavsaTextField(
                        value = salary,
                        onValueChange = {
                            salary = it
                            salaryError = null
                        },
                        label = "Monthly Base Salary (₹) (Optional)",
                        placeholder = "Enter default salary (optional)",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Daily wage can also be entered while marking attendance",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                    if (salaryError != null) {
                        Text(
                            text = salaryError!!,
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.error)
                        )
                    }
                }

                VyavsaTextField(value = otRate, onValueChange = { otRate = it }, label = "Overtime Rate (₹/hr)", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val trimmedSalary = salary.trim()
                    if (trimmedSalary.isNotEmpty()) {
                        val parsed = trimmedSalary.toDoubleOrNull()
                        if (parsed == null || parsed < 0.0) {
                            salaryError = "Salary must be a non-negative amount"
                            return@TextButton
                        }
                    }
                    onConfirm(name.trim(), phone.trim().ifBlank { null }, trimmedSalary.ifBlank { null }, otRate.trim())
                },
                enabled = name.isNotBlank()
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
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Disburse Salary: ${salary.employeeName}", fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Crimson50,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Pending Payable: ${FormatUtils.formatCurrency(salary.balance)}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = Crimson600),
                        modifier = Modifier.padding(12.dp)
                    )
                }
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

@Composable
fun EditEmployeeDialog(
    employee: Employee,
    onDismiss: () -> Unit,
    onConfirm: (name: String, phone: String?, salary: String?, clearSalary: Boolean, otRate: String, status: String) -> Unit
) {
    var name by remember { mutableStateOf(employee.name) }
    var phone by remember { mutableStateOf(employee.phone ?: "") }
    var salary by remember { mutableStateOf(employee.salary ?: "") }
    var otRate by remember { mutableStateOf(employee.otRate ?: "0.00") }
    var status by remember { mutableStateOf(employee.status) }
    var salaryError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Edit Employee", fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                VyavsaTextField(value = name, onValueChange = { name = it }, label = "Full Name *")
                VyavsaTextField(value = phone, onValueChange = { phone = it }, label = "Phone Number", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
                
                Column {
                    VyavsaTextField(
                        value = salary,
                        onValueChange = {
                            salary = it
                            salaryError = null
                        },
                        label = "Monthly Base Salary (₹) (Optional)",
                        placeholder = "Leave empty for daily wage / variable",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Leave blank to remove default fixed salary",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                    if (salaryError != null) {
                        Text(
                            text = salaryError!!,
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.error)
                        )
                    }
                }

                VyavsaTextField(value = otRate, onValueChange = { otRate = it }, label = "Overtime Rate (₹/hr)", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = status == "active",
                        onClick = { status = "active" },
                        shape = CircleShape,
                        label = { Text("Active", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Emerald50,
                            selectedLabelColor = Emerald600
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = status == "inactive",
                        onClick = { status = "inactive" },
                        shape = CircleShape,
                        label = { Text("Inactive", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Crimson50,
                            selectedLabelColor = Crimson600
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val trimmedSalary = salary.trim()
                    if (trimmedSalary.isNotEmpty()) {
                        val parsed = trimmedSalary.toDoubleOrNull()
                        if (parsed == null || parsed < 0.0) {
                            salaryError = "Salary must be a non-negative amount"
                            return@TextButton
                        }
                    }
                    val clearSalary = trimmedSalary.isEmpty() && employee.salary != null
                    val finalSalary = if (trimmedSalary.isEmpty()) null else trimmedSalary
                    onConfirm(name.trim(), phone.trim().ifBlank { null }, finalSalary, clearSalary, otRate.trim(), status)
                },
                enabled = name.isNotBlank()
            ) {
                Text("Save Changes", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
