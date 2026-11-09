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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import com.vyavsa.app.core.model.Purchase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchasesScreen(
    viewModel: TransactionsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToCreatePurchase: () -> Unit,
    onNavigateToPurchaseDetails: (purchaseId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val purchasesState by viewModel.purchasesState.collectAsState()
    val suppliersState by viewModel.suppliersState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val dateRangeSelection by viewModel.dateRangeSelection.collectAsState()
    val selectedSupplierId by viewModel.selectedPurchaseSupplierId.collectAsState()

    val suppliers = (suppliersState as? UiState.Success)?.data ?: emptyList()
    var showCustomDatePicker by remember { mutableStateOf(false) }
    var supplierMenuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.selectTab(TransactionTab.PURCHASES)
        viewModel.loadSuppliers()
        viewModel.loadPurchasesData()
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
                title = "Purchases & Procurement",
                subtitle = "Vendor invoices, stock intake & dues",
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                onNavigationClick = onNavigateBack
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToCreatePurchase,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Record Purchase", fontWeight = FontWeight.Bold) }
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
                    placeholder = { Text("Search item, vendor name...", fontSize = 14.sp) },
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

                    // Supplier Filter Chip Dropdown
                    Box {
                        val activeSupplier = suppliers.firstOrNull { it.id == selectedSupplierId }
                        FilterChip(
                            selected = selectedSupplierId != null,
                            onClick = { supplierMenuExpanded = true },
                            shape = CircleShape,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Blue50,
                                selectedLabelColor = Blue600
                            ),
                            label = {
                                Text(
                                    text = activeSupplier?.displayName ?: "Filter Supplier",
                                    fontWeight = if (selectedSupplierId != null) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            trailingIcon = {
                                if (selectedSupplierId != null) {
                                    IconButton(
                                        onClick = { viewModel.setPurchaseSupplierFilter(null) },
                                        modifier = Modifier.size(18.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(12.dp))
                                    }
                                }
                            }
                        )

                        DropdownMenu(
                            expanded = supplierMenuExpanded,
                            onDismissRequest = { supplierMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("All Suppliers", fontWeight = FontWeight.Bold) },
                                onClick = {
                                    viewModel.setPurchaseSupplierFilter(null)
                                    supplierMenuExpanded = false
                                }
                            )
                            suppliers.forEach { supp ->
                                DropdownMenuItem(
                                    text = { Text(supp.displayName) },
                                    onClick = {
                                        viewModel.setPurchaseSupplierFilter(supp.id)
                                        supplierMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Financial Summary Banner for Current Filtered List
            if (purchasesState is UiState.Success) {
                val purchases = (purchasesState as UiState.Success).data
                if (purchases.isNotEmpty()) {
                    var totalBillSum = 0.0
                    var totalPaidSum = 0.0
                    var totalDueSum = 0.0
                    purchases.forEach { p ->
                        totalBillSum += p.totalAmount.toDoubleOrNull() ?: 0.0
                        totalPaidSum += p.totalPaid.toDoubleOrNull() ?: 0.0
                        totalDueSum += p.totalPending.toDoubleOrNull() ?: p.outstandingAmount.toDoubleOrNull() ?: 0.0
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
                                    text = "Total Procured",
                                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                                Text(
                                    text = FormatUtils.formatCurrency(totalBillSum.toString()),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold)
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Settled Paid",
                                    style = MaterialTheme.typography.labelSmall.copy(color = Emerald600)
                                )
                                Text(
                                    text = FormatUtils.formatCurrency(totalPaidSum.toString()),
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Emerald600
                                    )
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Pending Dues",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (totalDueSum > 0.0) Crimson600 else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                                Text(
                                    text = FormatUtils.formatCurrency(totalDueSum.toString()),
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (totalDueSum > 0.0) Crimson600 else MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }

            // Main Purchase Content
            when (val state = purchasesState) {
                is UiState.Loading -> {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                is UiState.Empty -> {
                    EmptyStateView(
                        title = "No Purchases Recorded",
                        description = "Record raw materials, vendor invoices, stock intake, and supplier dues",
                        icon = Icons.Default.ShoppingCart,
                        actionText = "Record First Purchase",
                        onActionClick = onNavigateToCreatePurchase
                    )
                }
                is UiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                        ErrorBanner(message = state.message)
                    }
                }
                is UiState.Success -> {
                    val purchases = state.data
                    if (purchases.isEmpty()) {
                        EmptyStateView(
                            title = "No Purchases Found",
                            description = "No purchase records matched the selected date range or search filter",
                            icon = Icons.Default.ShoppingCart,
                            actionText = "Record Purchase",
                            onActionClick = onNavigateToCreatePurchase
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 88.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            itemsIndexed(
                                items = purchases,
                                key = { index, purch -> "purch_${purch.id.ifBlank { "p" }}_$index" },
                                contentType = { _, _ -> "purchase_item" }
                            ) { _, purchase ->
                                PurchaseItemCard(
                                    purchase = purchase,
                                    onClick = { onNavigateToPurchaseDetails(purchase.id) }
                                )
                            }
                        }
                    }
                }
                else -> {}
            }
        }
    }
}

@Composable
fun PurchaseItemCard(
    purchase: Purchase,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pendingVal = purchase.totalPending.toDoubleOrNull() ?: purchase.outstandingAmount.toDoubleOrNull() ?: 0.0
    val contraVal = purchase.contraAmount.toDoubleOrNull() ?: 0.0
    val totalPaidVal = purchase.totalPaid.toDoubleOrNull() ?: 0.0
    val isPaid = purchase.paymentStatus.equals("PAID", ignoreCase = true) || pendingVal <= 0.0
    val isPartial = !isPaid && (purchase.paymentStatus.equals("PARTIAL", ignoreCase = true) || totalPaidVal > 0.0)

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
                            .background(Amber50)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = null,
                            tint = Amber600,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = purchase.item.ifBlank { "Stock Purchase" },
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val supplierLabel = purchase.effectiveSupplierName
                            Text(
                                text = supplierLabel,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = if (supplierLabel != "Direct / Vendor") Blue600 else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = if (supplierLabel != "Direct / Vendor") FontWeight.Bold else FontWeight.Normal
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = " • Qty: ${purchase.quantity}",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = FormatUtils.formatCurrency(purchase.totalAmount),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = FormatUtils.formatDateForDisplay(purchase.createdAt),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Financial & Status Strip
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Paid: ",
                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                        Text(
                            text = FormatUtils.formatCurrency(purchase.totalPaid),
                            style = MaterialTheme.typography.labelMedium.copy(color = Emerald600, fontWeight = FontWeight.Bold)
                        )
                        if (contraVal > 0.0) {
                            Text(
                                text = " (⚡Contra)",
                                style = MaterialTheme.typography.labelSmall.copy(color = Emerald600, fontSize = 10.sp)
                            )
                        }
                    }

                    if (pendingVal > 0.0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Due: ",
                                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                            Text(
                                text = FormatUtils.formatCurrency(purchase.totalPending),
                                style = MaterialTheme.typography.labelMedium.copy(color = Crimson600, fontWeight = FontWeight.Bold)
                            )
                        }
                    }

                    StatBadge(
                        text = when {
                            isPaid && contraVal > 0.0 -> "PAID (CONTRA)"
                            isPaid -> "PAID"
                            isPartial -> "PARTIAL"
                            else -> "UNPAID"
                        },
                        backgroundColor = when {
                            isPaid -> Emerald50
                            isPartial -> Amber50
                            else -> Crimson50
                        },
                        textColor = when {
                            isPaid -> Emerald600
                            isPartial -> Amber600
                            else -> Crimson600
                        }
                    )
                }
            }
        }
    }
}
