package com.vyavsa.app.feature.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MoreTime
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vyavsa.app.core.common.FormatUtils
import com.vyavsa.app.core.common.UiState
import com.vyavsa.app.core.designsystem.Amber50
import com.vyavsa.app.core.designsystem.Amber600
import com.vyavsa.app.core.designsystem.Blue50
import com.vyavsa.app.core.designsystem.Blue600
import com.vyavsa.app.core.designsystem.CompanyLogoImage
import com.vyavsa.app.core.designsystem.Crimson50
import com.vyavsa.app.core.designsystem.Crimson600
import com.vyavsa.app.core.designsystem.Emerald50
import com.vyavsa.app.core.designsystem.Emerald600
import com.vyavsa.app.core.designsystem.ErrorBanner
import com.vyavsa.app.core.designsystem.MetricCard
import com.vyavsa.app.core.designsystem.StatBadge
import com.vyavsa.app.core.designsystem.VyavsaGradients
import com.vyavsa.app.core.designsystem.VyavsaSectionHeader
import com.vyavsa.app.core.designsystem.vyavsaClickable
import com.vyavsa.app.core.model.OfficeDashboardResponse
import com.vyavsa.app.feature.staff.RecordAdvanceDialog
import com.vyavsa.app.feature.staff.RecordOvertimeDialog
import com.vyavsa.app.feature.staff.StaffViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfficeDashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToCreateCounterSale: () -> Unit,
    onNavigateToCreatePurchase: () -> Unit,
    onNavigateToCreateExpense: () -> Unit,
    onNavigateToStaff: () -> Unit,
    onNavigateToCounterSales: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val officeState by viewModel.officeDashboardState.collectAsState()
    val profileState by viewModel.profileState.collectAsState()
    val dateFilter by viewModel.officeDateFilter.collectAsState()
    val sessionState by viewModel.tokenManager.sessionState.collectAsState()

    val staffViewModel: StaffViewModel = hiltViewModel()
    val employeesState by staffViewModel.employeesState.collectAsState()
    val employees = (employeesState as? UiState.Success)?.data ?: emptyList()

    var showAdvanceDialog by remember { mutableStateOf(false) }
    var showOvertimeDialog by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    val userName = remember(sessionState) {
        viewModel.tokenManager.getUserProfile()?.name?.ifBlank { "Office Manager" } ?: "Office Manager"
    }

    val todayDate = remember { FormatUtils.todayDateString() }
    val effectiveDate = if (dateFilter.isNotBlank()) dateFilter else todayDate
    val displayDateLabel = remember(effectiveDate) {
        if (effectiveDate == todayDate) {
            "Today • ${FormatUtils.formatDateForDisplay(effectiveDate)}"
        } else {
            FormatUtils.formatDateForDisplay(effectiveDate)
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                            val chosenDate = sdf.format(Date(millis))
                            viewModel.loadOfficeDashboard(chosenDate)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("Select", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Direct Employee Advance Dialog from Office Dashboard
    if (showAdvanceDialog && employees.isNotEmpty()) {
        RecordAdvanceDialog(
            employees = employees,
            onDismiss = { showAdvanceDialog = false },
            onConfirm = { empId, amount, date, method, note ->
                staffViewModel.recordStandaloneAdvance(
                    employeeId = empId,
                    amount = amount,
                    date = date,
                    paymentMethod = method,
                    note = note
                )
                showAdvanceDialog = false
                viewModel.loadOfficeDashboard()
            }
        )
    }

    // Direct Overtime Dialog from Office Dashboard
    if (showOvertimeDialog && employees.isNotEmpty()) {
        RecordOvertimeDialog(
            employees = employees,
            onDismiss = { showOvertimeDialog = false },
            onConfirm = { empId, amount, date, note ->
                staffViewModel.recordStandaloneOvertime(
                    employeeId = empId,
                    amount = amount,
                    date = date,
                    note = note
                )
                showOvertimeDialog = false
                viewModel.loadOfficeDashboard()
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Modern Header with greeting, branding, and live badge
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(VyavsaGradients.SoftHeaderGlowBrush)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    val companyProfile = (profileState as? UiState.Success)?.data
                    CompanyLogoImage(
                        logoUrl = companyProfile?.logoUrl,
                        companyName = companyProfile?.name ?: "Vyavsa",
                        size = 44.dp,
                        shape = RoundedCornerShape(14.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Hello, $userName",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onBackground,
                                letterSpacing = (-0.3).sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(Blue600)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Office Terminal • $displayDateLabel",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                FilledTonalIconButton(
                    onClick = { viewModel.loadOfficeDashboard() },
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh office metrics",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Scrollable content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Date Filter Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = dateFilter.isBlank() || dateFilter == todayDate,
                        onClick = { viewModel.loadOfficeDashboard(todayDate) },
                        label = { Text("Today", fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                    FilterChip(
                        selected = dateFilter.isNotBlank() && dateFilter != todayDate,
                        onClick = { showDatePicker = true },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        label = {
                            Text(
                                text = if (dateFilter.isNotBlank() && dateFilter != todayDate) dateFilter else "Pick Date",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    )
                }

                StatBadge(
                    text = "Live Authoritative",
                    backgroundColor = Emerald50,
                    textColor = Emerald600
                )
            }

            // Error Banner if failed
            if (officeState is UiState.Error) {
                ErrorBanner(
                    message = (officeState as UiState.Error).message,
                    onDismiss = { viewModel.loadOfficeDashboard() }
                )
            }

            val officeData = (officeState as? UiState.Success)?.data ?: OfficeDashboardResponse(
                businessDate = effectiveDate
            )

            // Primary 3 Metrics: Counter Sale Cash, Counter Sale Bank, Total Attendance
            VyavsaSectionHeader(
                title = "Today's Core Operations"
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                MetricCard(
                    title = "Counter Cash",
                    amount = FormatUtils.formatCurrency(officeData.totalCashCollected),
                    subtitle = "Store drawer cash",
                    icon = Icons.Default.Payments,
                    iconTint = Emerald600,
                    iconBackground = Emerald50.copy(alpha = 0.5f),
                    onClick = onNavigateToCounterSales,
                    modifier = Modifier.weight(1f)
                )

                MetricCard(
                    title = "Counter Bank",
                    amount = FormatUtils.formatCurrency(officeData.totalBankReceived),
                    subtitle = "POS bank / UPI",
                    icon = Icons.Default.AccountBalance,
                    iconTint = Blue600,
                    iconBackground = Blue50.copy(alpha = 0.5f),
                    onClick = onNavigateToCounterSales,
                    modifier = Modifier.weight(1f)
                )
            }

            // Attendance Metric Card
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                        RoundedCornerShape(24.dp)
                    )
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Badge,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Today's Attendance",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "${officeData.totalAttendance} of ${officeData.totalActiveEmployees.coerceAtLeast(officeData.totalAttendance)} logged",
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                            }
                        }

                        StatBadge(
                            text = "${officeData.presentCount} Present",
                            backgroundColor = Emerald50,
                            textColor = Emerald600
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        AttendanceStatPill(
                            label = "Present",
                            count = officeData.presentCount,
                            color = Emerald600,
                            bg = Emerald50,
                            modifier = Modifier.weight(1f)
                        )
                        AttendanceStatPill(
                            label = "Half Day",
                            count = officeData.halfDayCount,
                            color = Amber600,
                            bg = Amber50,
                            modifier = Modifier.weight(1f)
                        )
                        AttendanceStatPill(
                            label = "Absent",
                            count = officeData.absentCount,
                            color = Crimson600,
                            bg = Crimson50,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Quick Operational Action Grid (6 Key Authorized Actions)
            VyavsaSectionHeader(
                title = "Office Workflows"
            )

            // Row 1: Record Counter Sale & Mark Attendance
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OfficeActionCard(
                    title = "Record Counter Sale",
                    subtitle = "POS retail bill",
                    icon = Icons.Default.PointOfSale,
                    accentColor = MaterialTheme.colorScheme.primary,
                    onClick = onNavigateToCreateCounterSale,
                    modifier = Modifier.weight(1f)
                )

                OfficeActionCard(
                    title = "Mark Attendance",
                    subtitle = "Roll call & wages",
                    icon = Icons.Default.CheckCircle,
                    accentColor = Emerald600,
                    onClick = onNavigateToStaff,
                    modifier = Modifier.weight(1f)
                )
            }

            // Row 2: Add Advance & Record Overtime
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OfficeActionCard(
                    title = "Add Advance",
                    subtitle = "Cash / bank payout",
                    icon = Icons.Default.Payments,
                    accentColor = Amber600,
                    onClick = {
                        if (employees.isNotEmpty()) {
                            showAdvanceDialog = true
                        } else {
                            onNavigateToStaff()
                        }
                    },
                    modifier = Modifier.weight(1f)
                )

                OfficeActionCard(
                    title = "Record Overtime",
                    subtitle = "Extra hours / OT",
                    icon = Icons.Default.MoreTime,
                    accentColor = Blue600,
                    onClick = {
                        if (employees.isNotEmpty()) {
                            showOvertimeDialog = true
                        } else {
                            onNavigateToStaff()
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            // Row 3: Add Purchase & Add Expense
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OfficeActionCard(
                    title = "Add Purchase",
                    subtitle = "Supplier inventory",
                    icon = Icons.Default.ShoppingCart,
                    accentColor = MaterialTheme.colorScheme.secondary,
                    onClick = onNavigateToCreatePurchase,
                    modifier = Modifier.weight(1f)
                )

                OfficeActionCard(
                    title = "Add Expense",
                    subtitle = "Operational bills",
                    icon = Icons.Default.Receipt,
                    accentColor = Crimson600,
                    onClick = onNavigateToCreateExpense,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun AttendanceStatPill(
    label: String,
    count: Int,
    color: Color,
    bg: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bg,
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = color
                )
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Medium,
                    color = color,
                    fontSize = 10.sp
                )
            )
        }
    }
}

@Composable
private fun OfficeActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
        ),
        modifier = modifier.vyavsaClickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor.copy(alpha = 0.12f))
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
