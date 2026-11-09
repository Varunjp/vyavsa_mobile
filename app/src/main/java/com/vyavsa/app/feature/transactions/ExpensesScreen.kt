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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vyavsa.app.core.common.FormatUtils
import com.vyavsa.app.core.common.UiState
import com.vyavsa.app.core.designsystem.Amber50
import com.vyavsa.app.core.designsystem.Amber600
import com.vyavsa.app.core.designsystem.Blue50
import com.vyavsa.app.core.designsystem.Blue600
import com.vyavsa.app.core.designsystem.Crimson50
import com.vyavsa.app.core.designsystem.Crimson600
import com.vyavsa.app.core.designsystem.Emerald50
import com.vyavsa.app.core.designsystem.Emerald600
import com.vyavsa.app.core.designsystem.EmptyStateView
import com.vyavsa.app.core.designsystem.ErrorBanner
import com.vyavsa.app.core.designsystem.StatBadge
import com.vyavsa.app.core.designsystem.VyavsaTopAppBar
import com.vyavsa.app.core.designsystem.vyavsaClickable
import com.vyavsa.app.core.model.Expense

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesScreen(
    viewModel: TransactionsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToCreateExpense: () -> Unit,
    modifier: Modifier = Modifier
) {
    val expensesState by viewModel.expensesState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val dateRangeSelection by viewModel.dateRangeSelection.collectAsState()
    val selectedCategory by viewModel.selectedExpenseCategory.collectAsState()
    val updateExpenseActionState by viewModel.updateExpenseActionState.collectAsState()

    var showCustomDatePicker by remember { mutableStateOf(false) }
    var selectedExpenseForDetails by remember { mutableStateOf<Expense?>(null) }
    var editingExpense by remember { mutableStateOf<Expense?>(null) }

    val banksState by viewModel.banksState.collectAsState()
    val banks = (banksState as? UiState.Success)?.data ?: emptyList()

    LaunchedEffect(Unit) {
        viewModel.selectTab(TransactionTab.EXPENSES)
        viewModel.loadBanks()
        viewModel.loadEmployees()
        viewModel.loadExpensesData()
    }

    LaunchedEffect(updateExpenseActionState) {
        if (updateExpenseActionState is UiState.Success) {
            editingExpense = null
            viewModel.resetUpdateExpenseState()
        }
    }

    if (showCustomDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showCustomDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val chosenDate = FormatUtils.utcMillisToLocalDateString(millis)
                            viewModel.setDateFilter(chosenDate)
                        }
                        showCustomDatePicker = false
                    }
                ) {
                    Text("Select", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomDatePicker = false }) {
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
                title = "Operating Expenses",
                subtitle = "Utilities, fuel, rent & staff disbursements",
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                onNavigationClick = onNavigateBack
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToCreateExpense,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Record Expense", fontWeight = FontWeight.Bold) }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Search Bar & Filter Controls
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.onSearchChange(it) },
                    placeholder = { Text("Search expense description, staff name...", fontSize = 14.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { viewModel.onSearchChange("") }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Date Filter Chips Row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    val dateOptions = listOf(
                        DateFilterType.TODAY to "Today",
                        DateFilterType.YESTERDAY to "Yesterday",
                        DateFilterType.THIS_WEEK to "This Week",
                        DateFilterType.THIS_MONTH to "This Month",
                        DateFilterType.ALL_TIME to "All Time"
                    )

                    dateOptions.forEach { (type, label) ->
                        val isSelected = dateRangeSelection.type == type
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setDateRangeFilter(type) },
                            shape = CircleShape,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            label = { Text(label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium) }
                        )
                    }

                    // Custom Date Picker Chip
                    FilterChip(
                        selected = dateRangeSelection.type == DateFilterType.CUSTOM,
                        onClick = { showCustomDatePicker = true },
                        shape = CircleShape,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        label = {
                            Text(
                                if (dateRangeSelection.type == DateFilterType.CUSTOM && !dateRangeSelection.startDate.isNullOrBlank()) {
                                    FormatUtils.formatDateForDisplay(dateRangeSelection.startDate)
                                } else "Custom Date",
                                fontWeight = if (dateRangeSelection.type == DateFilterType.CUSTOM) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Category Filter Chips Row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    val categories = listOf(
                        "all" to "All Categories",
                        "operations" to "Operations",
                        "rent" to "Rent",
                        "electricity" to "Utilities",
                        "transport" to "Transport",
                        "employee_advance" to "Staff Advance",
                        "other" to "Other"
                    )

                    categories.forEach { (catKey, catLabel) ->
                        val isSelected = (selectedCategory ?: "all") == catKey
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setExpenseCategoryFilter(catKey) },
                            shape = CircleShape,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = if (catKey == "employee_advance") Amber50 else Blue50,
                                selectedLabelColor = if (catKey == "employee_advance") Amber600 else Blue600
                            ),
                            label = {
                                Text(
                                    text = catLabel,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        )
                    }
                }
            }

            // Financial Summary Banner for Current Filtered Expenses
            if (expensesState is UiState.Success) {
                val expenses = (expensesState as UiState.Success).data
                if (expenses.isNotEmpty()) {
                    var totalSum = 0.0
                    var cashSum = 0.0
                    var bankSum = 0.0

                    expenses.forEach { e ->
                        val amt = e.totalAmount.toDoubleOrNull() ?: e.amount.toDoubleOrNull() ?: 0.0
                        totalSum += amt
                        when (e.paymentMethod) {
                            "cash" -> cashSum += amt
                            "bank" -> bankSum += amt
                            "cash_bank" -> {
                                val cAmt = e.paymentBreakdown?.cashAmount?.toDoubleOrNull() ?: 0.0
                                val bAmt = e.paymentBreakdown?.bankAmount?.toDoubleOrNull() ?: (amt - cAmt)
                                cashSum += cAmt
                                bankSum += bAmt
                            }
                            else -> cashSum += amt
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Column {
                                Text(
                                    text = "Total Expenses",
                                    style = MaterialTheme.typography.labelSmall.copy(color = Crimson600)
                                )
                                Text(
                                    text = FormatUtils.formatCurrency(totalSum.toString()),
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Crimson600
                                    )
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Cash Outflow",
                                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                                Text(
                                    text = FormatUtils.formatCurrency(cashSum.toString()),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Bank Outflow",
                                    style = MaterialTheme.typography.labelSmall.copy(color = Blue600)
                                )
                                Text(
                                    text = FormatUtils.formatCurrency(bankSum.toString()),
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Blue600
                                    )
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }

            // Main Expense Content
            when (val state = expensesState) {
                is UiState.Loading -> {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                is UiState.Empty -> {
                    EmptyStateView(
                        title = "No Expenses Recorded",
                        description = "Record staff advances, utility bills, fuel, and shop maintenance",
                        icon = Icons.AutoMirrored.Filled.CallMade,
                        actionText = "Record First Expense",
                        onActionClick = onNavigateToCreateExpense
                    )
                }
                is UiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                        ErrorBanner(message = state.message)
                    }
                }
                is UiState.Success -> {
                    val expenses = state.data
                    if (expenses.isEmpty()) {
                        EmptyStateView(
                            title = "No Expenses Found",
                            description = "No expense records matched the selected date range or category filter",
                            icon = Icons.AutoMirrored.Filled.CallMade,
                            actionText = "Record Expense",
                            onActionClick = onNavigateToCreateExpense
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 88.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            itemsIndexed(
                                items = expenses,
                                key = { index, exp -> "exp_${exp.id.ifBlank { "e" }}_$index" },
                                contentType = { _, _ -> "expense_item" }
                            ) { _, expense ->
                                ExpenseItemCard(
                                    expense = expense,
                                    onClick = { selectedExpenseForDetails = expense },
                                    onEditClick = { editingExpense = expense }
                                )
                            }
                        }
                    }
                }
                else -> {}
            }
        }
    }

    // Expense Details Dialog
    selectedExpenseForDetails?.let { expense ->
        ExpenseDetailsDialog(
            expense = expense,
            onDismiss = { selectedExpenseForDetails = null },
            onEditClick = {
                selectedExpenseForDetails = null
                editingExpense = expense
            }
        )
    }

    // Edit Expense Dialog
    editingExpense?.let { expense ->
        EditExpenseDialog(
            expense = expense,
            banks = banks,
            isLoading = updateExpenseActionState is UiState.Loading,
            errorMessage = (updateExpenseActionState as? UiState.Error)?.message,
            onDismiss = {
                editingExpense = null
                viewModel.resetUpdateExpenseState()
            },
            onConfirm = { updatedItem, updatedCategory, updatedAmount, updatedPaymentMethod, bId, expDate ->
                viewModel.updateExpense(
                    id = expense.id,
                    item = updatedItem,
                    category = updatedCategory,
                    amount = updatedAmount,
                    paymentMethod = updatedPaymentMethod,
                    employeeId = expense.employeeId,
                    expenseDate = expDate,
                    bankId = bId
                )
            }
        )
    }
}

@Composable
fun ExpenseItemCard(
    expense: Expense,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isAdvance = expense.category.equals("employee_advance", ignoreCase = true) || !expense.employeeName.isNullOrBlank()

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .vyavsaClickable { onClick() }
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f), RoundedCornerShape(22.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(15.dp))
                            .background(if (isAdvance) Amber50 else Crimson50)
                    ) {
                        Icon(
                            imageVector = if (isAdvance) Icons.Default.Person else Icons.AutoMirrored.Filled.CallMade,
                            contentDescription = null,
                            tint = if (isAdvance) Amber600 else Crimson600,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = expense.item.ifBlank { "Operating Expense" },
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isAdvance) {
                                Text(
                                    text = if (!expense.employeeName.isNullOrBlank()) "Staff: ${expense.employeeName}" else "Staff Advance",
                                    style = MaterialTheme.typography.bodySmall.copy(color = Amber600, fontWeight = FontWeight.Bold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            } else {
                                val categoryDisplay = expense.category?.replace('_', ' ')?.replaceFirstChar {
                                    if (it.isLowerCase()) it.titlecase(java.util.Locale.US) else it.toString()
                                } ?: "General"
                                Text(
                                    text = categoryDisplay,
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = " • ${FormatUtils.formatDateForDisplay(expense.effectiveDate)}",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "-${FormatUtils.formatCurrency(if (expense.totalAmount != "0.00") expense.totalAmount else expense.amount)}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = Crimson600
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    when (expense.paymentMethod.lowercase()) {
                        "bank" -> {
                            val bankCount = expense.paymentBreakdown?.banks?.size ?: 1
                            val bankLabel = if (bankCount > 1) "Bank ($bankCount)" else (expense.paymentBreakdown?.banks?.firstOrNull()?.bankName ?: "Bank")
                            StatBadge(
                                text = bankLabel,
                                backgroundColor = Blue50,
                                textColor = Blue600
                            )
                        }
                        "cash_bank" -> {
                            StatBadge(
                                text = "Cash + Bank",
                                backgroundColor = MaterialTheme.colorScheme.secondaryContainer,
                                textColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        else -> {
                            StatBadge(
                                text = "Cash Till",
                                backgroundColor = Emerald50,
                                textColor = Emerald600
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tap to view details",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        fontSize = 11.sp
                    )
                )

                TextButton(
                    onClick = onEditClick,
                    shape = CircleShape,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Edit",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun ExpenseDetailsDialog(
    expense: Expense,
    onDismiss: () -> Unit,
    onEditClick: () -> Unit
) {
    val isAdvance = expense.category.equals("employee_advance", ignoreCase = true) || !expense.employeeName.isNullOrBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = "Expense Details",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold)
                    )
                    Text(
                        text = "Recorded Operational Disbursement",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
                StatBadge(
                    text = (expense.category?.replace('_', ' ')?.uppercase() ?: "GENERAL"),
                    backgroundColor = if (isAdvance) Amber50 else Crimson50,
                    textColor = if (isAdvance) Amber600 else Crimson600
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Amount Display
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Crimson50,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Disbursed Amount",
                            style = MaterialTheme.typography.labelSmall.copy(color = Crimson600)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = FormatUtils.formatCurrency(if (expense.totalAmount != "0.00") expense.totalAmount else expense.amount),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = Crimson600
                            )
                        )
                    }
                }

                // Description
                Column {
                    Text(
                        text = "Item Description / Purpose",
                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = expense.item.ifBlank { "Operating Expense" },
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }

                // Staff Advance Specific Note
                if (isAdvance) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Amber50,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = Amber600, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (!expense.employeeName.isNullOrBlank()) "Beneficiary: ${expense.employeeName}" else "Staff Member Advance",
                                    style = MaterialTheme.typography.labelMedium.copy(color = Amber600, fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "Automatically adjusted in pending salary statement",
                                    style = MaterialTheme.typography.labelSmall.copy(color = Amber600.copy(alpha = 0.85f))
                                )
                            }
                        }
                    }
                }

                // Date & Payment Method Breakdown
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Date",
                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = FormatUtils.formatDateForDisplay(expense.effectiveDate),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Payment Method",
                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = expense.paymentMethod.replace('_', ' ').uppercase(),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }

                // Detailed Bank Splits
                val breakdown = expense.paymentBreakdown
                if (breakdown != null && breakdown.banks.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Settlement Breakdown",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            if (breakdown.cashAmount.toDoubleOrNull() ?: 0.0 > 0.0) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Cash Till", style = MaterialTheme.typography.bodySmall)
                                    Text(
                                        FormatUtils.formatCurrency(breakdown.cashAmount),
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                            breakdown.banks.forEach { b ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Bank: ${b.bankName.ifBlank { "Account" }}", style = MaterialTheme.typography.bodySmall)
                                    Text(
                                        FormatUtils.formatCurrency(b.amount),
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onEditClick,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Edit Expense", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = CircleShape
            ) {
                Text("Close", fontWeight = FontWeight.SemiBold)
            }
        }
    )
}
