package com.vyavsa.app.feature.transactions

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vyavsa.app.core.common.FormatUtils
import com.vyavsa.app.core.common.UiState
import com.vyavsa.app.core.designsystem.Amber50
import com.vyavsa.app.core.designsystem.Amber600
import com.vyavsa.app.core.designsystem.ErrorBanner
import com.vyavsa.app.core.designsystem.VyavsaButton
import com.vyavsa.app.core.designsystem.VyavsaSegmentedTabs
import com.vyavsa.app.core.designsystem.VyavsaTextField
import com.vyavsa.app.core.designsystem.VyavsaTopAppBar
import com.vyavsa.app.core.model.Employee

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateExpenseScreen(
    viewModel: TransactionsViewModel,
    onExpenseCreated: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val createActionState by viewModel.createActionState.collectAsState()
    val employeesState by viewModel.employeesState.collectAsState()
    val banksState by viewModel.banksState.collectAsState()
    val focusManager = LocalFocusManager.current

    val employees = (employeesState as? UiState.Success)?.data ?: emptyList()
    val banks = (banksState as? UiState.Success)?.data ?: emptyList()

    var item by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("operations") }
    var amount by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("cash") }
    var selectedBankId by remember { mutableStateOf<String?>(null) }
    var selectedEmployee by remember { mutableStateOf<Employee?>(null) }
    var employeeDropdownExpanded by remember { mutableStateOf(false) }
    var paymentDate by remember { mutableStateOf("") }
    var referenceId by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    val categories = remember {
        listOf(
            "operations" to "Operations",
            "employee_advance" to "Staff Advance",
            "transport" to "Transport",
            "office" to "Office",
            "salary" to "Salary",
            "rent" to "Rent",
            "misc" to "Misc"
        )
    }

    LaunchedEffect(createActionState) {
        if (createActionState is UiState.Success) {
            onExpenseCreated()
            viewModel.resetCreateState()
        }
    }

    LaunchedEffect(category) {
        if (category == "employee_advance") {
            if (item.isBlank() || item == "Shop rent" || item == "Electricity bill" || item == "Fuel") {
                item = "Staff Advance Payment"
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            VyavsaTopAppBar(
                title = if (category == "employee_advance") "Disburse Staff Advance" else "Record Expense",
                subtitle = if (category == "employee_advance") "Advance payout adjusting employee salary balance" else "Log store operating expenses & payouts",
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                onNavigationClick = onNavigateBack
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f), RoundedCornerShape(28.dp))
                ) {
                    Column(modifier = Modifier.padding(22.dp)) {
                        Text(
                            text = if (category == "employee_advance") "Employee Advance Disbursement" else "New Expense Entry",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (category == "employee_advance")
                                "Disburses funds to staff and deducts from their pending salary ledger"
                            else
                                "Deducts directly from your selected Cash Till or Bank balance",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        if (createActionState is UiState.Error || localError != null) {
                            ErrorBanner(
                                message = localError ?: (createActionState as? UiState.Error)?.message ?: "",
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                        }

                        // Category Selection (Cleanly aligned 2-row scrollable + balanced grid)
                        Text(
                            text = "Expense Category",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            categories.chunked(3).forEach { rowItems ->
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    rowItems.forEach { (catKey, labelText) ->
                                        val isSelected = category == catKey
                                        val isAdv = catKey == "employee_advance"
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = {
                                                category = catKey
                                                localError = null
                                            },
                                            shape = CircleShape,
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = if (isAdv) Amber50 else MaterialTheme.colorScheme.primaryContainer,
                                                selectedLabelColor = if (isAdv) Amber600 else MaterialTheme.colorScheme.onPrimaryContainer
                                            ),
                                            border = if (isSelected && isAdv) {
                                                FilterChipDefaults.filterChipBorder(enabled = true, selected = true, borderColor = Amber600)
                                            } else {
                                                FilterChipDefaults.filterChipBorder(
                                                    enabled = true,
                                                    selected = isSelected,
                                                    borderColor = MaterialTheme.colorScheme.outlineVariant
                                                )
                                            },
                                            label = {
                                                Text(
                                                    text = labelText,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    textAlign = TextAlign.Center,
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    if (rowItems.size < 3) {
                                        repeat(3 - rowItems.size) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Employee Advance Selection Section (Smooth AnimatedVisibility)
                        AnimatedVisibility(
                            visible = category == "employee_advance",
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = Amber50,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, Amber600.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = null,
                                            tint = Amber600,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = "Selecting an employee advance records the disbursement and automatically adjusts their pending salary balance.",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = Amber600,
                                                fontWeight = FontWeight.Medium
                                            )
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                ExposedDropdownMenuBox(
                                    expanded = employeeDropdownExpanded,
                                    onExpandedChange = { employeeDropdownExpanded = !employeeDropdownExpanded },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    OutlinedTextField(
                                        value = selectedEmployee?.name ?: "",
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Select Staff Member *") },
                                        placeholder = { Text("Tap to choose staff member...") },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = employeeDropdownExpanded) },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Badge,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        },
                                        shape = RoundedCornerShape(18.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .menuAnchor()
                                    )

                                    ExposedDropdownMenu(
                                        expanded = employeeDropdownExpanded,
                                        onDismissRequest = { employeeDropdownExpanded = false }
                                    ) {
                                        if (employees.isEmpty()) {
                                            DropdownMenuItem(
                                                text = { Text("No active staff found. Register employees in Staff tab.") },
                                                onClick = { employeeDropdownExpanded = false }
                                            )
                                        } else {
                                            employees.forEach { emp ->
                                                DropdownMenuItem(
                                                    text = {
                                                        Column {
                                                            Text(emp.name, fontWeight = FontWeight.Bold)
                                                            Text(
                                                                "Base Rate: ${FormatUtils.formatCurrency(emp.salary)} • Status: ${emp.status.uppercase()}",
                                                                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                            )
                                                        }
                                                    },
                                                    onClick = {
                                                        selectedEmployee = emp
                                                        employeeDropdownExpanded = false
                                                        localError = null
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))
                            }
                        }

                        // Expense Item Name
                        VyavsaTextField(
                            value = item,
                            onValueChange = {
                                item = it
                                localError = null
                            },
                            label = if (category == "employee_advance") "Advance Purpose / Note *" else "Expense Name / Reason *",
                            placeholder = if (category == "employee_advance") "e.g. Emergency advance, Festival advance" else "e.g. Fuel, Tea, Office Electricity",
                            leadingIcon = Icons.Default.Description,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Amount & Date Row
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            VyavsaTextField(
                                value = amount,
                                onValueChange = {
                                    amount = it
                                    localError = null
                                },
                                label = if (category == "employee_advance") "Advance (₹) *" else "Amount (₹) *",
                                placeholder = "0.00",
                                leadingIcon = Icons.Default.AttachMoney,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Decimal,
                                    imeAction = ImeAction.Next
                                ),
                                modifier = Modifier.weight(1f)
                            )

                            VyavsaTextField(
                                value = paymentDate,
                                onValueChange = { paymentDate = it },
                                label = "Date (Optional)",
                                placeholder = "YYYY-MM-DD",
                                leadingIcon = Icons.Default.CalendarToday,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Payment Method (Aligned Segmented Switcher)
                        Text(
                            text = "Paid From Account",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        VyavsaSegmentedTabs(
                            tabs = listOf("Cash Till", "Bank Account"),
                            selectedIndex = if (paymentMethod == "cash") 0 else 1,
                            onTabSelected = { idx ->
                                if (idx == 0) {
                                    paymentMethod = "cash"
                                } else {
                                    paymentMethod = "bank"
                                    if (selectedBankId == null && banks.isNotEmpty()) {
                                        selectedBankId = banks.first().id
                                    }
                                }
                            }
                        )

                        AnimatedVisibility(
                            visible = paymentMethod == "bank" && banks.isNotEmpty(),
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column(modifier = Modifier.padding(top = 12.dp)) {
                                Text(
                                    text = "Select Bank Account *",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState())
                                ) {
                                    banks.forEach { bank ->
                                        val isSelected = selectedBankId == bank.id
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { selectedBankId = bank.id },
                                            shape = CircleShape,
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Default.AccountBalance,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                            ),
                                            label = {
                                                Text(
                                                    text = bank.bankName,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(26.dp))

                        VyavsaButton(
                            text = if (category == "employee_advance") "Disburse Staff Advance" else "Record Expense",
                            onClick = {
                                focusManager.clearFocus()
                                val amt = amount.trim().toDoubleOrNull()
                                if (item.isBlank()) {
                                    localError = "Expense item name is required"
                                    return@VyavsaButton
                                }
                                if (amt == null || amt <= 0.0) {
                                    localError = "Please enter an amount greater than zero"
                                    return@VyavsaButton
                                }
                                if (category == "employee_advance" && selectedEmployee == null) {
                                    localError = "Please select a staff member for the advance payment"
                                    return@VyavsaButton
                                }
                                if (paymentMethod == "bank" && selectedBankId.isNullOrBlank()) {
                                    localError = "Please select a bank account"
                                    return@VyavsaButton
                                }

                                localError = null
                                viewModel.createExpense(
                                    item = item,
                                    category = category,
                                    amount = amount,
                                    paymentMethod = paymentMethod,
                                    employeeId = selectedEmployee?.id,
                                    paymentDate = paymentDate.ifBlank { null },
                                    bankId = if (paymentMethod == "bank") selectedBankId else null,
                                    referenceId = referenceId.ifBlank { null }
                                )
                            },
                            isLoading = createActionState is UiState.Loading
                        )
                    }
                }
            }
        }
    }
}
