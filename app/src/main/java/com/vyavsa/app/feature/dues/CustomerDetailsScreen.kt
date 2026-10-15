package com.vyavsa.app.feature.dues

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.vyavsa.app.core.common.FormatUtils
import com.vyavsa.app.core.common.UiState
import com.vyavsa.app.core.designsystem.AvatarInitialCircle
import com.vyavsa.app.core.designsystem.Blue600
import com.vyavsa.app.core.designsystem.Crimson50
import com.vyavsa.app.core.designsystem.Crimson600
import com.vyavsa.app.core.designsystem.Emerald50
import com.vyavsa.app.core.designsystem.Emerald600
import com.vyavsa.app.core.designsystem.EmptyStateView
import com.vyavsa.app.core.designsystem.ErrorBanner
import com.vyavsa.app.core.designsystem.StatBadge
import com.vyavsa.app.core.designsystem.VyavsaButton
import com.vyavsa.app.core.designsystem.VyavsaOutlinedButton
import com.vyavsa.app.core.designsystem.VyavsaSegmentedTabs
import com.vyavsa.app.core.designsystem.VyavsaTextField
import com.vyavsa.app.core.designsystem.VyavsaTopAppBar
import com.vyavsa.app.core.model.Customer
import com.vyavsa.app.core.model.CustomerBalanceAdjustment
import com.vyavsa.app.core.model.LineSale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CustomerDetailsScreen(
    customerId: String,
    viewModel: DuesViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToStatement: (customerId: String, customerName: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val customerState by viewModel.selectedCustomer.collectAsState()
    val salesState by viewModel.selectedCustomerSales.collectAsState()
    val adjustmentsState by viewModel.adjustmentsState.collectAsState()
    val actionState by viewModel.actionState.collectAsState()
    val isAdmin = viewModel.isAdmin

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showPaymentDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showAdjustDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    LaunchedEffect(customerId) {
        viewModel.loadCustomerDetails(customerId)
        viewModel.loadCustomerSales(customerId)
        viewModel.loadAdjustments(customerId)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            VyavsaTopAppBar(
                title = "Customer Details",
                subtitle = "Profile, dues ledger & sales history",
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                onNavigationClick = onNavigateBack,
                actions = {
                    if (isAdmin) {
                        FilledTonalIconButton(
                            onClick = { showEditDialog = true },
                            shape = CircleShape,
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Customer",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        FilledTonalIconButton(
                            onClick = { showDeleteConfirmDialog = true },
                            shape = CircleShape,
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = Crimson50,
                                contentColor = Crimson600
                            ),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Customer",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
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
            when (val state = customerState) {
                is UiState.Loading -> {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                is UiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                        ErrorBanner(message = state.message)
                    }
                }
                is UiState.Success -> {
                    val customer = state.data

                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item(key = "customer_hero_card") {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                if (actionState is UiState.Error) {
                                    ErrorBanner(message = (actionState as UiState.Error).message)
                                }

                                // Customer Hero Card
                                Card(
                                    shape = RoundedCornerShape(26.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), RoundedCornerShape(26.dp))
                                ) {
                                    Column(modifier = Modifier.padding(20.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            AvatarInitialCircle(
                                                name = customer.customerName,
                                                modifier = Modifier.size(54.dp)
                                            )
                                            Spacer(modifier = Modifier.width(16.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = customer.customerName,
                                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                if (!customer.phone.isNullOrBlank()) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(
                                                            imageVector = Icons.Default.Phone,
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text(
                                                            text = customer.phone,
                                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        )
                                                    }
                                                }
                                            }

                                            StatBadge(
                                                text = customer.status.uppercase(),
                                                backgroundColor = if (customer.status.equals("active", ignoreCase = true)) Emerald50 else Crimson50,
                                                textColor = if (customer.status.equals("active", ignoreCase = true)) Emerald600 else Crimson600
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(18.dp))

                                        // Financial balances banner
                                        Surface(
                                            shape = RoundedCornerShape(20.dp),
                                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(16.dp)
                                            ) {
                                                Column {
                                                    Text(
                                                        text = "Current Due Balance",
                                                        style = MaterialTheme.typography.labelMedium.copy(
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    )
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    val balanceNum = customer.currentBalance.toDoubleOrNull() ?: 0.0
                                                    Text(
                                                        text = FormatUtils.formatCurrency(balanceNum),
                                                        style = MaterialTheme.typography.headlineMedium.copy(
                                                            fontWeight = FontWeight.ExtraBold,
                                                            color = if (balanceNum > 0) Crimson600 else Emerald600
                                                        )
                                                    )
                                                }

                                                Column(horizontalAlignment = Alignment.End) {
                                                    Text(
                                                        text = "Opening Balance",
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    )
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        text = FormatUtils.formatCurrency(customer.openingBalance),
                                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                                    )
                                                }
                                            }
                                        }

                                        // Procurement / Payable Info if applicable
                                        val totalPurch = customer.totalPurchases.toDoubleOrNull() ?: 0.0
                                        if (totalPurch > 0) {
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = "Procurement Purchases: ${FormatUtils.formatCurrency(totalPurch)}",
                                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                )
                                                val outstandingPayable = customer.outstandingPayable.toDoubleOrNull() ?: 0.0
                                                Text(
                                                    text = "Payable Due: ${FormatUtils.formatCurrency(outstandingPayable)}",
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (outstandingPayable > 0) Blue600 else MaterialTheme.colorScheme.onSurface
                                                    )
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(18.dp))

                                        // Action buttons row (Naturally aligned equal heights)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            VyavsaButton(
                                                text = "Record Payment",
                                                leadingIcon = Icons.Default.Payment,
                                                onClick = { showPaymentDialog = true },
                                                modifier = Modifier.weight(1f)
                                            )

                                            VyavsaOutlinedButton(
                                                text = "Statement",
                                                leadingIcon = Icons.Default.Receipt,
                                                onClick = { onNavigateToStatement(customer.id, customer.customerName) },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }

                                        if (isAdmin) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            TextButton(
                                                onClick = { showAdjustDialog = true },
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Tune,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Adjust Ledger Balance Manually", fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Unified Pill Segmented Tab Switcher
                        item(key = "customer_tabs") {
                            VyavsaSegmentedTabs(
                                tabs = listOf("Sales History", "Balance Adjustments"),
                                selectedIndex = selectedTabIndex,
                                onTabSelected = { selectedTabIndex = it },
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        // Tab Content
                        if (selectedTabIndex == 0) {
                            when (val sState = salesState) {
                                is UiState.Loading -> {
                                    item(key = "sales_loading") {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.fillMaxWidth().padding(32.dp)
                                        ) {
                                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                }
                                is UiState.Empty -> {
                                    item(key = "sales_empty") {
                                        EmptyStateView(
                                            title = "No Prior Sales",
                                            description = "No sale transactions recorded for this customer yet."
                                        )
                                    }
                                }
                                is UiState.Error -> {
                                    item(key = "sales_error") {
                                        Box(modifier = Modifier.padding(20.dp)) {
                                            ErrorBanner(message = sState.message)
                                        }
                                    }
                                }
                                is UiState.Success -> {
                                    items(
                                        items = sState.data,
                                        key = { "cust_sale_${it.id}" }
                                    ) { sale ->
                                        CustomerSaleItemCard(
                                            sale = sale,
                                            modifier = Modifier
                                                .padding(horizontal = 20.dp, vertical = 6.dp)
                                                .animateItemPlacement()
                                        )
                                    }
                                }
                                else -> {}
                            }
                        } else {
                            when (val aState = adjustmentsState) {
                                is UiState.Loading -> {
                                    item(key = "adj_loading") {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.fillMaxWidth().padding(32.dp)
                                        ) {
                                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                }
                                is UiState.Empty -> {
                                    item(key = "adj_empty") {
                                        EmptyStateView(
                                            title = "No Balance Adjustments",
                                            description = "No manual ledger balance adjustments have been made for this customer."
                                        )
                                    }
                                }
                                is UiState.Error -> {
                                    item(key = "adj_error") {
                                        Box(modifier = Modifier.padding(20.dp)) {
                                            ErrorBanner(message = aState.message)
                                        }
                                    }
                                }
                                is UiState.Success -> {
                                    items(
                                        items = aState.data,
                                        key = { "cust_adj_${it.id}" }
                                    ) { adj ->
                                        CustomerAdjustmentItemCard(
                                            adjustment = adj,
                                            modifier = Modifier
                                                .padding(horizontal = 20.dp, vertical = 6.dp)
                                                .animateItemPlacement()
                                        )
                                    }
                                }
                                else -> {}
                            }
                        }

                        item(key = "bottom_spacer") {
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }

                    // Record Payment Dialog
                    if (showPaymentDialog) {
                        CustomerRecordPaymentDialog(
                            customer = customer,
                            onDismiss = { showPaymentDialog = false },
                            onConfirm = { amount, method, note ->
                                showPaymentDialog = false
                                viewModel.recordPayment(customer.id, amount, method, note)
                            }
                        )
                    }

                    // Edit Customer Dialog
                    if (showEditDialog) {
                        EditCustomerDialog(
                            customer = customer,
                            onDismiss = { showEditDialog = false },
                            onConfirm = { name, phone, status ->
                                showEditDialog = false
                                viewModel.updateCustomer(customer.id, name, phone, status)
                            }
                        )
                    }

                    // Adjust Balance Dialog
                    if (showAdjustDialog) {
                        AdjustBalanceDialog(
                            customer = customer,
                            onDismiss = { showAdjustDialog = false },
                            onConfirm = { newBal, deltaAmt, reason ->
                                showAdjustDialog = false
                                viewModel.adjustBalance(customer.id, newBal, deltaAmt, reason)
                            }
                        )
                    }

                    // Delete Customer Confirmation Dialog
                    if (showDeleteConfirmDialog) {
                        AlertDialog(
                            onDismissRequest = { showDeleteConfirmDialog = false },
                            shape = RoundedCornerShape(28.dp),
                            containerColor = MaterialTheme.colorScheme.surface,
                            title = { Text("Delete Customer Account", fontWeight = FontWeight.ExtraBold) },
                            text = {
                                Text("Are you sure you want to delete ${customer.customerName}? This action cannot be undone if ledger transactions are active.")
                            },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        showDeleteConfirmDialog = false
                                        viewModel.deleteCustomer(customer.id) {
                                            onNavigateBack()
                                        }
                                    }
                                ) {
                                    Text("Delete Permanently", color = Crimson600, fontWeight = FontWeight.Bold)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                                    Text("Cancel")
                                }
                            }
                        )
                    }
                }
                else -> {}
            }
        }
    }
}

@Composable
private fun CustomerSaleItemCard(
    sale: LineSale,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (!sale.route.isNullOrBlank()) "Route: ${sale.route}" else "Line Sale",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = FormatUtils.formatIsoDateTime(sale.createdAt),
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }

                Text(
                    text = FormatUtils.formatCurrency(sale.totalAmount),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Cash: ${FormatUtils.formatCurrency(sale.totalCashIn)} | Bank: ${FormatUtils.formatCurrency(sale.bankAmount)}",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )

                val balanceNum = sale.balance.toDoubleOrNull() ?: 0.0
                StatBadge(
                    text = if (balanceNum <= 0) "PAID" else "DUE ${FormatUtils.formatCurrency(balanceNum)}",
                    backgroundColor = if (balanceNum <= 0) Emerald50 else Crimson50,
                    textColor = if (balanceNum <= 0) Emerald600 else Crimson600
                )
            }
        }
    }
}

@Composable
private fun CustomerAdjustmentItemCard(
    adjustment: CustomerBalanceAdjustment,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = adjustment.reason,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = FormatUtils.formatIsoDateTime(adjustment.createdAt),
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Old: ${FormatUtils.formatCurrency(adjustment.oldBalance)}",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
                Text(
                    text = "New: ${FormatUtils.formatCurrency(adjustment.newBalance)}",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

@Composable
private fun CustomerRecordPaymentDialog(
    customer: Customer,
    onDismiss: () -> Unit,
    onConfirm: (amount: String, method: String, note: String?) -> Unit
) {
    var amount by remember { mutableStateOf(customer.currentBalance) }
    var selectedMethod by remember { mutableStateOf("cash") }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Receive Payment from ${customer.customerName}", fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                VyavsaTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = "Settlement Amount (₹)",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedMethod == "cash",
                        onClick = { selectedMethod = "cash" },
                        shape = CircleShape,
                        label = { Text("Cash Till") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Money, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )

                    FilterChip(
                        selected = selectedMethod == "bank",
                        onClick = { selectedMethod = "bank" },
                        shape = CircleShape,
                        label = { Text("Bank Transfer") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.AccountBalance, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }

                VyavsaTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = "Reference / Note (Optional)"
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (amount.isNotBlank()) {
                        onConfirm(amount.trim(), selectedMethod, note.trim().ifBlank { null })
                    }
                }
            ) {
                Text("Confirm Payment", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun EditCustomerDialog(
    customer: Customer,
    onDismiss: () -> Unit,
    onConfirm: (name: String, phone: String?, status: String) -> Unit
) {
    var name by remember { mutableStateOf(customer.customerName) }
    var phone by remember { mutableStateOf(customer.phone ?: "") }
    var status by remember { mutableStateOf(customer.status) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Edit Customer Profile", fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                VyavsaTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Customer Name"
                )

                VyavsaTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = "Phone Number",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = status == "active",
                        onClick = { status = "active" },
                        shape = CircleShape,
                        label = { Text("Active") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Emerald50,
                            selectedLabelColor = Emerald600
                        )
                    )

                    FilterChip(
                        selected = status == "inactive",
                        onClick = { status = "inactive" },
                        shape = CircleShape,
                        label = { Text("Inactive") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Crimson50,
                            selectedLabelColor = Crimson600
                        )
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name.trim(), phone.trim().ifBlank { null }, status)
                    }
                }
            ) {
                Text("Save Changes", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun AdjustBalanceDialog(
    customer: Customer,
    onDismiss: () -> Unit,
    onConfirm: (newBalance: String?, deltaAmount: String?, reason: String) -> Unit
) {
    var adjustType by remember { mutableStateOf("new_balance") } // "new_balance" or "delta"
    var amountValue by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Adjust Customer Balance", fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Current balance: ${FormatUtils.formatCurrency(customer.currentBalance)}",
                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = adjustType == "new_balance",
                        onClick = { adjustType = "new_balance" }
                    )
                    Text("Set Exact New Balance", style = MaterialTheme.typography.bodySmall)

                    Spacer(modifier = Modifier.width(12.dp))

                    RadioButton(
                        selected = adjustType == "delta",
                        onClick = { adjustType = "delta" }
                    )
                    Text("Delta (+/-)", style = MaterialTheme.typography.bodySmall)
                }

                VyavsaTextField(
                    value = amountValue,
                    onValueChange = { amountValue = it },
                    label = if (adjustType == "new_balance") "New Balance (₹)" else "Adjustment Amount (+/- ₹)",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )

                VyavsaTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = "Reason (Required, e.g. Audit correction)"
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (amountValue.isNotBlank() && reason.isNotBlank()) {
                        if (adjustType == "new_balance") {
                            onConfirm(amountValue.trim(), null, reason.trim())
                        } else {
                            onConfirm(null, amountValue.trim(), reason.trim())
                        }
                    }
                }
            ) {
                Text("Apply Adjustment", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
