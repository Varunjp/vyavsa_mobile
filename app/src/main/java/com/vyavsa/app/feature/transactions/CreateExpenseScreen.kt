package com.vyavsa.app.feature.transactions

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.unit.dp
import com.vyavsa.app.core.common.FormatUtils
import com.vyavsa.app.core.common.UiState
import com.vyavsa.app.core.designsystem.Amber50
import com.vyavsa.app.core.designsystem.Amber600
import com.vyavsa.app.core.designsystem.Blue50
import com.vyavsa.app.core.designsystem.Blue600
import com.vyavsa.app.core.designsystem.ErrorBanner
import com.vyavsa.app.core.designsystem.VyavsaButton
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

    val categories = listOf("operations", "employee_advance", "transport", "office", "salary", "rent", "misc")

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
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f), RoundedCornerShape(28.dp))
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
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

                        Spacer(modifier = Modifier.height(20.dp))

                        if (createActionState is UiState.Error || localError != null) {
                            ErrorBanner(
                                message = localError ?: (createActionState as? UiState.Error)?.message ?: "",
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                        }

                        // Category Selection Chips
                        Text(
                            text = "Expense Category",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Category Chips Row 1
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            categories.take(3).forEach { cat ->
                                val labelText = if (cat == "employee_advance") "Staff Advance" else cat.replaceFirstChar { it.uppercase() }
                                val isSelected = category == cat
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        category = cat
                                        localError = null
                                    },
                                    shape = CircleShape,
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = if (cat == "employee_advance") Amber50 else MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = if (cat == "employee_advance") Amber600 else MaterialTheme.colorScheme.onPrimaryContainer
                                    ),
                                    border = if (isSelected && cat == "employee_advance") FilterChipDefaults.filterChipBorder(enabled = true, selected = true, borderColor = Amber600) else null,
                                    label = { Text(labelText, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) }
                                )
                            }
                        }

                        // Category Chips Row 2
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            categories.drop(3).forEach { cat ->
                                val labelText = cat.replaceFirstChar { it.uppercase() }
                                val isSelected = category == cat
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        category = cat
                                        localError = null
                                    },
                                    shape = CircleShape,
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    ),
                                    label = { Text(labelText, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Employee Advance Selection Section
                        if (category == "employee_advance") {
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

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Select Staff Member *",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            ExposedDropdownMenuBox(
                                expanded = employeeDropdownExpanded,
                                onExpandedChange = { employeeDropdownExpanded = !employeeDropdownExpanded },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = selectedEmployee?.name ?: "Tap to choose staff member...",
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = employeeDropdownExpanded) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Badge,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
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

                        // Amount
                        VyavsaTextField(
                            value = amount,
                            onValueChange = {
                                amount = it
                                localError = null
                            },
                            label = if (category == "employee_advance") "Advance Amount (₹) *" else "Amount (₹) *",
                            placeholder = "0.00",
                            leadingIcon = Icons.Default.AttachMoney,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Next
                            )
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Transaction Date (Optional)
                        VyavsaTextField(
                            value = paymentDate,
                            onValueChange = { paymentDate = it },
                            label = "Transaction Date (YYYY-MM-DD, Optional)",
                            placeholder = "Defaults to today",
                            leadingIcon = Icons.Default.CalendarToday,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // Payment Method
                        Text(
                            text = "Paid From",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            FilterChip(
                                selected = paymentMethod == "cash",
                                onClick = { paymentMethod = "cash" },
                                shape = CircleShape,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                label = { Text("Cash Till") }
                            )
                            FilterChip(
                                selected = paymentMethod == "bank",
                                onClick = {
                                    paymentMethod = "bank"
                                    if (selectedBankId == null && banks.isNotEmpty()) {
                                        selectedBankId = banks.first().id
                                    }
                                },
                                shape = CircleShape,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                label = { Text("Bank Account") }
                            )
                        }

                        if (paymentMethod == "bank" && banks.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Select Bank Account *",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            banks.forEach { bank ->
                                FilterChip(
                                    selected = selectedBankId == bank.id,
                                    onClick = { selectedBankId = bank.id },
                                    label = { Text(bank.bankName) },
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
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
