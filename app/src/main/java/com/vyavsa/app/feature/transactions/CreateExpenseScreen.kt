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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.vyavsa.app.core.common.SaleCalculationUtils
import com.vyavsa.app.core.common.UiState
import com.vyavsa.app.core.designsystem.Amber50
import com.vyavsa.app.core.designsystem.Amber600
import com.vyavsa.app.core.designsystem.Crimson50
import com.vyavsa.app.core.designsystem.Crimson600
import com.vyavsa.app.core.designsystem.Emerald50
import com.vyavsa.app.core.designsystem.Emerald600
import com.vyavsa.app.core.designsystem.ErrorBanner
import com.vyavsa.app.core.designsystem.VyavsaButton
import com.vyavsa.app.core.designsystem.VyavsaSegmentedTabs
import com.vyavsa.app.core.designsystem.VyavsaTextField
import com.vyavsa.app.core.designsystem.VyavsaTopAppBar
import com.vyavsa.app.core.model.BankPaymentSplit
import com.vyavsa.app.core.model.Employee
import com.vyavsa.app.core.model.Expense
import com.vyavsa.app.core.model.TenantBank
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateExpenseScreen(
    viewModel: TransactionsViewModel,
    onExpenseCreated: () -> Unit,
    onNavigateBack: () -> Unit,
    initialExpense: Expense? = null,
    modifier: Modifier = Modifier
) {
    val isEditMode = initialExpense != null
    val createActionState by viewModel.createActionState.collectAsState()
    val updateActionState by viewModel.updateExpenseActionState.collectAsState()
    val employeesState by viewModel.employeesState.collectAsState()
    val banksState by viewModel.banksState.collectAsState()
    val focusManager = LocalFocusManager.current

    val employees = (employeesState as? UiState.Success)?.data ?: emptyList()
    val banks = (banksState as? UiState.Success)?.data ?: emptyList()

    var item by rememberSaveable { mutableStateOf(initialExpense?.item ?: "") }
    var category by rememberSaveable { mutableStateOf(initialExpense?.category ?: "operations") }
    var amount by rememberSaveable {
        mutableStateOf(
            if (initialExpense != null) {
                if (initialExpense.totalAmount != "0.00") initialExpense.totalAmount else initialExpense.amount
            } else ""
        )
    }
    var paymentMethod by rememberSaveable {
        mutableStateOf(
            when (initialExpense?.paymentMethod) {
                "cash_bank" -> "cash_bank"
                "bank" -> "bank"
                else -> "cash"
            }
        )
    }
    var cashAllocated by rememberSaveable {
        mutableStateOf(
            if (initialExpense != null) {
                if (initialExpense.paymentMethod == "cash_bank") {
                    initialExpense.paymentBreakdown?.cashAmount ?: ""
                } else if (initialExpense.paymentMethod == "cash") {
                    if (initialExpense.totalAmount != "0.00") initialExpense.totalAmount else initialExpense.amount
                } else ""
            } else ""
        )
    }
    val bankRows = remember {
        mutableStateListOf<ExpenseBankRowState>().apply {
            if (initialExpense != null) {
                val splits = initialExpense.paymentBreakdown?.banks
                if (!splits.isNullOrEmpty()) {
                    splits.forEach { b ->
                        add(ExpenseBankRowState(bankId = b.bankAccountId, amount = b.amount))
                    }
                } else if (initialExpense.paymentMethod == "bank") {
                    val bId = initialExpense.paymentBreakdown?.banks?.firstOrNull()?.bankAccountId ?: banks.firstOrNull()?.id ?: ""
                    val amt = if (initialExpense.totalAmount != "0.00") initialExpense.totalAmount else initialExpense.amount
                    add(ExpenseBankRowState(bankId = bId, amount = amt))
                }
            }
        }
    }
    var selectedEmployee by remember {
        mutableStateOf<Employee?>(
            if (initialExpense?.employeeId != null) {
                employees.firstOrNull { it.id == initialExpense.employeeId }
            } else null
        )
    }

    LaunchedEffect(employees, initialExpense) {
        if (selectedEmployee == null && initialExpense?.employeeId != null && employees.isNotEmpty()) {
            selectedEmployee = employees.firstOrNull { it.id == initialExpense.employeeId }
        }
    }

    LaunchedEffect(banks) {
        if ((paymentMethod == "bank" || paymentMethod == "cash_bank") && bankRows.isEmpty() && banks.isNotEmpty()) {
            val expenseBd = SaleCalculationUtils.parseAmount(amount)
            val prefill = if (paymentMethod == "bank" && expenseBd > BigDecimal.ZERO) {
                String.format(Locale.US, "%.2f", expenseBd)
            } else ""
            bankRows.add(ExpenseBankRowState(bankId = banks.first().id, amount = prefill))
        }
    }

    var employeeDropdownExpanded by remember { mutableStateOf(false) }
    var expenseDate by rememberSaveable {
        mutableStateOf(
            initialExpense?.effectiveDate?.ifBlank { null } ?: FormatUtils.todayDateString()
        )
    }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var referenceId by rememberSaveable { mutableStateOf("") }
    var localError by rememberSaveable { mutableStateOf<String?>(null) }

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

    LaunchedEffect(createActionState, updateActionState) {
        if (createActionState is UiState.Success || updateActionState is UiState.Success) {
            onExpenseCreated()
            viewModel.resetCreateState()
            viewModel.resetUpdateExpenseState()
        }
    }

    LaunchedEffect(category) {
        if (category == "employee_advance") {
            if (item.isBlank() || item == "Shop rent" || item == "Electricity bill" || item == "Fuel") {
                item = "Staff Advance Payment"
            }
        }
    }

    if (showDatePicker) {
        val initialMillis = remember(expenseDate) {
            FormatUtils.localDateStringToUtcMillis(expenseDate) ?: System.currentTimeMillis()
        }
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialMillis
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            expenseDate = FormatUtils.utcMillisToLocalDateString(millis)
                            localError = null
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

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            VyavsaTopAppBar(
                title = when {
                    isEditMode -> "Edit Expense"
                    category == "employee_advance" -> "Disburse Staff Advance"
                    else -> "Record Expense"
                },
                subtitle = when {
                    isEditMode -> "Update expense details & transaction date"
                    category == "employee_advance" -> "Advance payout adjusting employee salary balance"
                    else -> "Log store operating expenses & payouts"
                },
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                onNavigationClick = onNavigateBack
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .imePadding()
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
                            text = when {
                                isEditMode -> "Edit Expense Entry"
                                category == "employee_advance" -> "Employee Advance Disbursement"
                                else -> "New Expense Entry"
                            },
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = when {
                                isEditMode -> "Modify transaction date, category, amount, or payment method"
                                category == "employee_advance" -> "Disburses funds to staff and deducts from their pending salary ledger"
                                else -> "Deducts directly from your selected Cash Till or Bank balance"
                            },
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        if (createActionState is UiState.Error || updateActionState is UiState.Error || localError != null) {
                            val errorMsg = localError
                                ?: (createActionState as? UiState.Error)?.message
                                ?: (updateActionState as? UiState.Error)?.message
                                ?: ""
                            ErrorBanner(
                                message = errorMsg,
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

                            ExpenseDateField(
                                selectedDate = expenseDate,
                                onClick = { showDatePicker = true },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        if (expenseDate != FormatUtils.todayDateString()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Historical date: ${FormatUtils.formatDateForDisplay(expenseDate)}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                                TextButton(
                                    onClick = {
                                        expenseDate = FormatUtils.todayDateString()
                                        localError = null
                                    }
                                ) {
                                    Text("Reset to Today", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Live Calculations for Payment Allocation
                        val expenseTotalBd = SaleCalculationUtils.parseAmount(amount)
                        val isExpenseAmountValid = expenseTotalBd > BigDecimal.ZERO

                        val cashValBd: BigDecimal = when (paymentMethod) {
                            "cash" -> expenseTotalBd
                            "cash_bank" -> SaleCalculationUtils.parseAmount(cashAllocated)
                            else -> BigDecimal.ZERO
                        }

                        val bankSumBd: BigDecimal = when (paymentMethod) {
                            "cash" -> BigDecimal.ZERO
                            else -> bankRows.fold(BigDecimal.ZERO) { acc, r -> acc.add(SaleCalculationUtils.parseAmount(r.amount)) }
                        }

                        val totalAllocatedBd = (cashValBd + bankSumBd).setScale(2, RoundingMode.HALF_UP)
                        val remainingBd = (expenseTotalBd - totalAllocatedBd).setScale(2, RoundingMode.HALF_UP)
                        val isFullyAllocated = isExpenseAmountValid && remainingBd.compareTo(BigDecimal.ZERO) == 0 && totalAllocatedBd.compareTo(expenseTotalBd) == 0

                        val selectedBankIds = bankRows.map { it.bankId.trim() }.filter { it.isNotBlank() }
                        val hasDuplicateBanks = selectedBankIds.size != selectedBankIds.distinct().size
                        val hasEmptyBankSelection = (paymentMethod == "bank" || paymentMethod == "cash_bank") && (bankRows.isEmpty() || bankRows.any { it.bankId.isBlank() })
                        val hasNonPositiveBankAmount = (paymentMethod == "bank" || paymentMethod == "cash_bank") && bankRows.any { SaleCalculationUtils.parseAmount(it.amount) <= BigDecimal.ZERO }
                        val hasInvalidCashAmount = paymentMethod == "cash_bank" && cashValBd <= BigDecimal.ZERO

                        val isAllocationValid = isExpenseAmountValid &&
                                isFullyAllocated &&
                                !hasDuplicateBanks &&
                                !hasEmptyBankSelection &&
                                !hasNonPositiveBankAmount &&
                                !hasInvalidCashAmount

                        Spacer(modifier = Modifier.height(18.dp))

                        // Payment Allocation Section
                        Text(
                            text = "Payment Source & Allocation",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Specify whether funds are paid from Cash, single/multiple Bank accounts, or combined",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        VyavsaSegmentedTabs(
                            tabs = listOf("Cash Till", "Bank Account(s)", "Cash + Bank"),
                            selectedIndex = when (paymentMethod) {
                                "bank" -> 1
                                "cash_bank" -> 2
                                else -> 0
                            },
                            onTabSelected = { idx ->
                                localError = null
                                when (idx) {
                                    0 -> {
                                        paymentMethod = "cash"
                                    }
                                    1 -> {
                                        paymentMethod = "bank"
                                        if (bankRows.isEmpty() && banks.isNotEmpty()) {
                                            val prefill = if (expenseTotalBd > BigDecimal.ZERO) String.format(Locale.US, "%.2f", expenseTotalBd) else ""
                                            bankRows.add(ExpenseBankRowState(bankId = banks.first().id, amount = prefill))
                                        }
                                    }
                                    2 -> {
                                        paymentMethod = "cash_bank"
                                        if (bankRows.isEmpty() && banks.isNotEmpty()) {
                                            bankRows.add(ExpenseBankRowState(bankId = banks.first().id, amount = ""))
                                        }
                                    }
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        when (paymentMethod) {
                            "cash" -> {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                    modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = "Full expense amount (${FormatUtils.formatCurrency(if (isExpenseAmountValid) String.format(Locale.US, "%.2f", expenseTotalBd) else "0.00")}) will be paid immediately from the Cash Drawer.",
                                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        )
                                    }
                                }
                            }
                            "cash_bank" -> {
                                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                    VyavsaTextField(
                                        value = cashAllocated,
                                        onValueChange = {
                                            cashAllocated = it
                                            localError = null
                                        },
                                        label = "Paid via Cash Drawer (₹) *",
                                        placeholder = "0.00",
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next)
                                    )

                                    BankAllocationList(
                                        bankRows = bankRows,
                                        banks = banks,
                                        remainingBd = remainingBd,
                                        hasDuplicateBanks = hasDuplicateBanks
                                    )
                                }
                            }
                            "bank" -> {
                                BankAllocationList(
                                    bankRows = bankRows,
                                    banks = banks,
                                    remainingBd = remainingBd,
                                    hasDuplicateBanks = hasDuplicateBanks
                                )
                            }
                        }

                        // Live Settlement / Allocation Breakdown Card (Visible for Bank or Cash+Bank, or when amounts are entered)
                        if (paymentMethod != "cash" || isExpenseAmountValid) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        1.dp,
                                        if (isAllocationValid) Emerald600.copy(alpha = 0.5f)
                                        else if (remainingBd < BigDecimal.ZERO || hasDuplicateBanks) Crimson600.copy(alpha = 0.5f)
                                        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                        RoundedCornerShape(16.dp)
                                    )
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Total Expense Amount:", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium))
                                        Text(
                                            FormatUtils.formatCurrency(String.format(Locale.US, "%.2f", expenseTotalBd)),
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Total Allocated:", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium))
                                        Text(
                                            FormatUtils.formatCurrency(String.format(Locale.US, "%.2f", totalAllocatedBd)),
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontWeight = FontWeight.ExtraBold,
                                                color = if (isFullyAllocated) Emerald600 else MaterialTheme.colorScheme.onSurface
                                            )
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (remainingBd < BigDecimal.ZERO) "Over-allocated:" else "Remaining to Allocate:",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                                        )
                                        Text(
                                            text = FormatUtils.formatCurrency(String.format(Locale.US, "%.2f", remainingBd.abs())),
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontWeight = FontWeight.ExtraBold,
                                                color = when {
                                                    remainingBd < BigDecimal.ZERO -> Crimson600
                                                    remainingBd > BigDecimal.ZERO -> Amber600
                                                    else -> Emerald600
                                                }
                                            )
                                        )
                                    }

                                    // Action chip if balance remains
                                    if (remainingBd > BigDecimal.ZERO) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            if (paymentMethod == "cash_bank") {
                                                TextButton(
                                                    onClick = {
                                                        val newCash = cashValBd.add(remainingBd)
                                                        cashAllocated = String.format(Locale.US, "%.2f", newCash)
                                                    }
                                                ) {
                                                    Text(
                                                        "Allocate ${FormatUtils.formatCurrency(String.format(Locale.US, "%.2f", remainingBd))} to Cash",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                                    )
                                                }
                                            } else if (paymentMethod == "bank" && bankRows.isNotEmpty()) {
                                                TextButton(
                                                    onClick = {
                                                        val lastRow = bankRows.last()
                                                        val currentAmt = SaleCalculationUtils.parseAmount(lastRow.amount)
                                                        val updated = currentAmt.add(remainingBd)
                                                        bankRows[bankRows.lastIndex] = lastRow.copy(amount = String.format(Locale.US, "%.2f", updated))
                                                    }
                                                ) {
                                                    Text(
                                                        "Auto-balance to Bank",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Contextual Validation Messages
                                    if (hasDuplicateBanks) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            "⚠️ Duplicate bank account selected. Each bank account can only be selected once per expense.",
                                            style = MaterialTheme.typography.labelSmall.copy(color = Crimson600, fontWeight = FontWeight.SemiBold)
                                        )
                                    } else if (hasNonPositiveBankAmount) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            "⚠️ Every bank allocation amount must be greater than zero.",
                                            style = MaterialTheme.typography.labelSmall.copy(color = Crimson600, fontWeight = FontWeight.SemiBold)
                                        )
                                    } else if (hasInvalidCashAmount) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            "⚠️ Cash allocation must be greater than zero for Cash + Bank payment.",
                                            style = MaterialTheme.typography.labelSmall.copy(color = Crimson600, fontWeight = FontWeight.SemiBold)
                                        )
                                    } else if (isFullyAllocated) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Emerald600, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                "Allocation valid and matches total expense perfectly.",
                                                style = MaterialTheme.typography.labelSmall.copy(color = Emerald600, fontWeight = FontWeight.Bold)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(26.dp))

                        VyavsaButton(
                            text = when {
                                isEditMode -> "Update Expense"
                                category == "employee_advance" -> "Disburse Staff Advance"
                                else -> "Record Expense"
                            },
                            enabled = isAllocationValid && createActionState !is UiState.Loading && updateActionState !is UiState.Loading,
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
                                if (!FormatUtils.isValidIsoDate(expenseDate)) {
                                    localError = "Expense date must be in YYYY-MM-DD format"
                                    return@VyavsaButton
                                }
                                if (hasDuplicateBanks) {
                                    localError = "Each bank account can only be selected once"
                                    return@VyavsaButton
                                }
                                if (hasEmptyBankSelection) {
                                    localError = "Please select a bank account for all bank allocation rows"
                                    return@VyavsaButton
                                }
                                if (hasNonPositiveBankAmount) {
                                    localError = "Each bank allocation amount must be greater than zero"
                                    return@VyavsaButton
                                }
                                if (hasInvalidCashAmount) {
                                    localError = "Cash amount must be greater than zero for Cash + Bank payment"
                                    return@VyavsaButton
                                }
                                if (remainingBd.compareTo(BigDecimal.ZERO) != 0) {
                                    localError = if (remainingBd > BigDecimal.ZERO) {
                                        "Total allocated amount must equal the expense amount (${FormatUtils.formatCurrency(String.format(Locale.US, "%.2f", remainingBd))} remaining)"
                                    } else {
                                        "Total allocated amount cannot exceed expense amount (${FormatUtils.formatCurrency(String.format(Locale.US, "%.2f", remainingBd.abs()))} excess)"
                                    }
                                    return@VyavsaButton
                                }

                                localError = null
                                val bankSplits = if (paymentMethod == "cash") {
                                    emptyList()
                                } else {
                                    bankRows.map {
                                        BankPaymentSplit(
                                            bankId = it.bankId,
                                            bankAccountId = it.bankId,
                                            amount = it.amount.trim().ifBlank { "0.00" }
                                        )
                                    }
                                }
                                val effectiveCash = when (paymentMethod) {
                                    "cash" -> String.format(Locale.US, "%.2f", expenseTotalBd)
                                    "cash_bank" -> cashAllocated.trim()
                                    else -> null
                                }
                                val effectiveBankId = bankSplits.firstOrNull()?.bankId

                                if (isEditMode) {
                                    viewModel.updateExpense(
                                        id = initialExpense!!.id,
                                        item = item,
                                        category = category,
                                        amount = amount,
                                        paymentMethod = paymentMethod,
                                        employeeId = selectedEmployee?.id,
                                        expenseDate = expenseDate,
                                        bankId = effectiveBankId,
                                        cashAmount = effectiveCash,
                                        bankPayments = bankSplits
                                    )
                                } else {
                                    viewModel.createExpense(
                                        item = item,
                                        category = category,
                                        amount = amount,
                                        paymentMethod = paymentMethod,
                                        employeeId = selectedEmployee?.id,
                                        expenseDate = expenseDate,
                                        bankId = effectiveBankId,
                                        referenceId = referenceId.ifBlank { null },
                                        cashAmount = effectiveCash,
                                        bankPayments = bankSplits
                                    )
                                }
                            },
                            isLoading = createActionState is UiState.Loading || updateActionState is UiState.Loading
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpenseDateField(
    selectedDate: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isToday = selectedDate == FormatUtils.todayDateString()
    val displayDate = remember(selectedDate) {
        FormatUtils.formatDateForDisplay(selectedDate)
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        onClick = onClick,
        modifier = modifier
            .height(56.dp)
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
                RoundedCornerShape(16.dp)
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = "Select Date",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(verticalArrangement = Arrangement.Center) {
                    Text(
                        text = "Date *",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    Text(
                        text = displayDate.ifBlank { selectedDate },
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (isToday) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.padding(start = 4.dp)
                ) {
                    Text(
                        text = "Today",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

data class ExpenseBankRowState(
    val id: String = java.util.UUID.randomUUID().toString(),
    val bankId: String = "",
    val amount: String = ""
)

@Composable
private fun BankAllocationList(
    bankRows: androidx.compose.runtime.snapshots.SnapshotStateList<ExpenseBankRowState>,
    banks: List<TenantBank>,
    remainingBd: BigDecimal,
    hasDuplicateBanks: Boolean
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Paid via Bank Account(s) *",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
            )
            if (banks.isNotEmpty()) {
                val canAddMore = bankRows.size < banks.size || hasDuplicateBanks
                OutlinedButton(
                    onClick = {
                        val unusedBank = banks.firstOrNull { b -> bankRows.none { it.bankId == b.id } } ?: banks.firstOrNull()
                        val prefill = if (remainingBd > BigDecimal.ZERO) String.format(Locale.US, "%.2f", remainingBd) else ""
                        bankRows.add(ExpenseBankRowState(bankId = unusedBank?.id ?: "", amount = prefill))
                    },
                    enabled = canAddMore,
                    shape = CircleShape
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Bank", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (banks.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Amber50,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = Amber600, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "No bank accounts registered. Please register accounts in Bank tab or disburse via Cash Till.",
                        style = MaterialTheme.typography.bodySmall.copy(color = Amber600)
                    )
                }
            }
        } else {
            bankRows.forEachIndexed { index, rowState ->
                ExpenseBankPaymentRowItem(
                    rowState = rowState,
                    banks = banks,
                    canDelete = bankRows.size > 1,
                    onDelete = { bankRows.removeAt(index) },
                    onUpdate = { updated -> bankRows[index] = updated }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpenseBankPaymentRowItem(
    rowState: ExpenseBankRowState,
    banks: List<TenantBank>,
    canDelete: Boolean,
    onDelete: () -> Unit,
    onUpdate: (ExpenseBankRowState) -> Unit
) {
    var dropdownExpanded by remember { mutableStateOf(false) }
    val currentBank = banks.firstOrNull { it.id == rowState.bankId } ?: banks.firstOrNull()

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ExposedDropdownMenuBox(
            expanded = dropdownExpanded,
            onExpandedChange = { dropdownExpanded = !dropdownExpanded },
            modifier = Modifier.weight(1.25f)
        ) {
            OutlinedTextField(
                value = currentBank?.bankName ?: "Select Bank",
                onValueChange = {},
                readOnly = true,
                label = { Text("Bank Account *") },
                singleLine = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                shape = RoundedCornerShape(16.dp),
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
                expanded = dropdownExpanded,
                onDismissRequest = { dropdownExpanded = false }
            ) {
                banks.forEach { bank ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(bank.bankName, fontWeight = FontWeight.Bold)
                                if (!bank.accountNumber.isNullOrBlank()) {
                                    Text(
                                        "A/C: ••••${bank.accountNumber.takeLast(4)}",
                                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    )
                                }
                            }
                        },
                        onClick = {
                            onUpdate(rowState.copy(bankId = bank.id))
                            dropdownExpanded = false
                        }
                    )
                }
            }
        }

        OutlinedTextField(
            value = rowState.amount,
            onValueChange = { onUpdate(rowState.copy(amount = it)) },
            label = { Text("Amount (₹) *") },
            placeholder = { Text("0.00") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
            ),
            modifier = Modifier.weight(1f)
        )

        if (canDelete) {
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Remove Bank",
                    tint = Crimson600,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
