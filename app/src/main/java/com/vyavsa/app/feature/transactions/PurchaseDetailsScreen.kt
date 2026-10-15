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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
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
import com.vyavsa.app.core.designsystem.ErrorBanner
import com.vyavsa.app.core.designsystem.StatBadge
import com.vyavsa.app.core.designsystem.VyavsaButton
import com.vyavsa.app.core.designsystem.VyavsaSectionHeader
import com.vyavsa.app.core.designsystem.VyavsaTextField
import com.vyavsa.app.core.model.Purchase
import com.vyavsa.app.core.model.PurchasePayment
import com.vyavsa.app.core.model.TenantBank

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseDetailsScreen(
    viewModel: PurchaseDetailsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val purchaseState by viewModel.purchaseState.collectAsState()
    val paymentsState by viewModel.paymentsState.collectAsState()
    val banks by viewModel.banksState.collectAsState()

    val paymentActionState by viewModel.paymentActionState.collectAsState()
    val updateActionState by viewModel.updateActionState.collectAsState()
    val deleteActionState by viewModel.deleteActionState.collectAsState()

    var showPaymentDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(deleteActionState) {
        if (deleteActionState is UiState.Success) {
            onNavigateBack()
        }
    }

    LaunchedEffect(paymentActionState) {
        if (paymentActionState is UiState.Success) {
            showPaymentDialog = false
            viewModel.clearActionStates()
        }
    }

    LaunchedEffect(updateActionState) {
        if (updateActionState is UiState.Success) {
            showEditDialog = false
            viewModel.clearActionStates()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Purchase Order Details",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Procurement & vendor payment audit record",
                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (viewModel.isAdmin) {
                        IconButton(onClick = { showEditDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Purchase",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Purchase",
                                tint = Crimson600
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = purchaseState) {
                is UiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                is UiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        ErrorBanner(message = state.message)
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedButton(onClick = { viewModel.loadPurchaseDetails() }) {
                            Text("Retry")
                        }
                    }
                }
                is UiState.Success -> {
                    val purchase = state.data
                    PurchaseDetailsContent(
                        purchase = purchase,
                        paymentsState = paymentsState,
                        isAdmin = viewModel.isAdmin,
                        onRecordPaymentClick = { showPaymentDialog = true },
                        onEditClick = { showEditDialog = true }
                    )

                    if (showPaymentDialog) {
                        val pending = purchase.totalPending.toDoubleOrNull() ?: 0.0
                        RecordPurchasePaymentDialog(
                            maxDue = pending,
                            banks = banks,
                            isLoading = paymentActionState is UiState.Loading,
                            errorMessage = (paymentActionState as? UiState.Error)?.message,
                            onDismiss = {
                                showPaymentDialog = false
                                viewModel.clearActionStates()
                            },
                            onConfirm = { amt, method, bankId, note ->
                                viewModel.recordPayment(amt, method, bankId, note)
                            }
                        )
                    }

                    if (showEditDialog) {
                        EditPurchaseDialog(
                            purchase = purchase,
                            isLoading = updateActionState is UiState.Loading,
                            errorMessage = (updateActionState as? UiState.Error)?.message,
                            onDismiss = {
                                showEditDialog = false
                                viewModel.clearActionStates()
                            },
                            onConfirm = { item, qty, total, paid ->
                                viewModel.updatePurchase(item, qty, total, paid, purchase.customerId)
                            }
                        )
                    }

                    if (showDeleteDialog) {
                        AlertDialog(
                            onDismissRequest = { showDeleteDialog = false },
                            title = { Text("Delete Purchase Order", fontWeight = FontWeight.Bold) },
                            text = {
                                Text("Are you sure you want to delete this purchase for \"${purchase.item}\"? This will reverse all supplier dues and associated cash/bank disbursements.")
                            },
                            confirmButton = {
                                Button(
                                    onClick = { viewModel.deletePurchase() },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Text("Delete Order")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDeleteDialog = false }) {
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
private fun PurchaseDetailsContent(
    purchase: Purchase,
    paymentsState: UiState<List<PurchasePayment>>,
    isAdmin: Boolean,
    onRecordPaymentClick: () -> Unit,
    onEditClick: () -> Unit
) {
    val pendingVal = purchase.totalPending.toDoubleOrNull() ?: 0.0

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Card
        item {
            Card(
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f), RoundedCornerShape(26.dp))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Amber50)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ShoppingCart,
                                    contentDescription = null,
                                    tint = Amber600,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = purchase.item,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                                )
                                Text(
                                    text = if (!purchase.customerName.isNullOrBlank()) "Vendor: ${purchase.customerName}" else "Direct / Walk-in Vendor",
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                            }
                        }

                        val isPaid = purchase.paymentStatus.equals("PAID", ignoreCase = true) || pendingVal <= 0.0
                        val isPartial = purchase.paymentStatus.equals("PARTIALLY_PAID", ignoreCase = true) ||
                                (purchase.totalPaid.toDoubleOrNull() ?: 0.0 > 0.0 && pendingVal > 0.0)

                        StatBadge(
                            text = if (isPaid) "PAID" else if (isPartial) "PARTIAL" else "UNPAID",
                            backgroundColor = if (isPaid) Emerald50 else if (isPartial) Amber50 else Crimson50,
                            textColor = if (isPaid) Emerald600 else if (isPartial) Amber600 else Crimson600
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        DetailChip(
                            icon = Icons.Default.Inventory2,
                            label = "Quantity",
                            value = "${purchase.quantity} units",
                            modifier = Modifier.weight(1f)
                        )
                        DetailChip(
                            icon = Icons.Default.CalendarToday,
                            label = "Date",
                            value = FormatUtils.formatDateForDisplay(purchase.createdAt),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Financial Summary Breakdown
                    FinancialRow(
                        label = "Total Purchase Amount",
                        value = FormatUtils.formatCurrency(purchase.totalAmount),
                        isBold = true
                    )
                    FinancialRow(
                        label = "Total Amount Paid",
                        value = FormatUtils.formatCurrency(purchase.totalPaid),
                        color = Emerald600,
                        isBold = true
                    )
                    FinancialRow(
                        label = "Outstanding Balance",
                        value = FormatUtils.formatCurrency(purchase.totalPending),
                        color = if (pendingVal > 0) Crimson600 else MaterialTheme.colorScheme.onSurface,
                        isBold = true
                    )

                    if (pendingVal > 0.0) {
                        Spacer(modifier = Modifier.height(18.dp))
                        VyavsaButton(
                            text = "Record Supplier Settlement Payment",
                            onClick = onRecordPaymentClick,
                            leadingIcon = Icons.Default.Payment
                        )
                    }

                    if (isAdmin) {
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = onEditClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Edit Purchase")
                        }
                    }
                }
            }
        }

        // Supplier Info Card
        if (!purchase.customerName.isNullOrBlank()) {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f), RoundedCornerShape(20.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Blue50)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = Blue600,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Registered Supplier",
                                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                            Text(
                                text = purchase.customerName,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
        }

        // Payment History Section
        item {
            VyavsaSectionHeader(
                title = "Payment & Settlement Records",
                badgeText = when (paymentsState) {
                    is UiState.Success -> "${paymentsState.data.size}"
                    else -> null
                }
            )
        }

        when (paymentsState) {
            is UiState.Loading -> {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                    }
                }
            }
            is UiState.Empty -> {
                item {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No Payments Logged",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "No settlement transactions have been recorded for this purchase yet.",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                    }
                }
            }
            is UiState.Error -> {
                item {
                    ErrorBanner(message = paymentsState.message)
                }
            }
            is UiState.Success -> {
                items(paymentsState.data) { payment ->
                    PaymentRecordCard(payment)
                }
            }
            else -> {}
        }
    }
}

@Composable
private fun PaymentRecordCard(payment: PurchasePayment) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f), RoundedCornerShape(18.dp))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Emerald50)
                ) {
                    Icon(
                        imageVector = Icons.Default.Payment,
                        contentDescription = null,
                        tint = Emerald600,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = if (payment.paymentMethod.equals("bank", ignoreCase = true)) {
                            if (!payment.bankName.isNullOrBlank()) "Bank: ${payment.bankName}" else "Bank Settlement"
                        } else {
                            "Cash Payment"
                        },
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = payment.note?.ifBlank { null } ?: if (!payment.paymentDate.isNullOrBlank()) payment.paymentDate else FormatUtils.formatDateForDisplay(payment.createdAt),
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = FormatUtils.formatCurrency(payment.amount),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold, color = Emerald600)
                )
                if (payment.isSettlement) {
                    Spacer(modifier = Modifier.height(2.dp))
                    StatBadge(
                        text = "SETTLEMENT",
                        backgroundColor = Blue50,
                        textColor = Blue600
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailChip(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

@Composable
private fun FinancialRow(
    label: String,
    value: String,
    color: Color = Color.Unspecified,
    isBold: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (isBold) FontWeight.Bold else FontWeight.SemiBold,
                color = if (color != Color.Unspecified) color else MaterialTheme.colorScheme.onSurface
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecordPurchasePaymentDialog(
    maxDue: Double,
    banks: List<TenantBank>,
    isLoading: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onConfirm: (amount: String, method: String, bankId: String?, note: String?) -> Unit
) {
    var amount by remember { mutableStateOf(if (maxDue > 0) String.format(java.util.Locale.US, "%.2f", maxDue) else "") }
    var selectedMethod by remember { mutableStateOf("cash") }
    var selectedBankId by remember { mutableStateOf<String?>(banks.firstOrNull()?.id) }
    var note by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Purchase Settlement", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (errorMessage != null || localError != null) {
                    ErrorBanner(message = localError ?: errorMessage ?: "")
                }

                VyavsaTextField(
                    value = amount,
                    onValueChange = {
                        amount = it
                        localError = null
                    },
                    label = "Settlement Amount (₹) *",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )

                Text(
                    text = "Payment Method",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedMethod == "cash",
                        onClick = { selectedMethod = "cash" },
                        label = { Text("Cash Till") }
                    )
                    FilterChip(
                        selected = selectedMethod == "bank",
                        onClick = { selectedMethod = "bank" },
                        label = { Text("Bank Account") }
                    )
                }

                if (selectedMethod == "bank" && banks.isNotEmpty()) {
                    Text(
                        text = "Select Bank Account *",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    banks.forEach { bank ->
                        FilterChip(
                            selected = selectedBankId == bank.id,
                            onClick = { selectedBankId = bank.id },
                            label = { Text(bank.bankName) },
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }
                }

                VyavsaTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = "Note / Cheque Ref (Optional)"
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amount.toDoubleOrNull()
                    if (amt == null || amt <= 0.0) {
                        localError = "Enter a valid settlement amount"
                        return@Button
                    }
                    if (amt > maxDue) {
                        localError = "Amount cannot exceed outstanding balance"
                        return@Button
                    }
                    if (selectedMethod == "bank" && selectedBankId.isNullOrBlank()) {
                        localError = "Please select a bank account"
                        return@Button
                    }
                    onConfirm(amount, selectedMethod, selectedBankId, note)
                },
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
                } else {
                    Text("Record Payment")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditPurchaseDialog(
    purchase: Purchase,
    isLoading: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onConfirm: (item: String, quantity: String, totalAmount: String, totalPaid: String) -> Unit
) {
    var item by remember { mutableStateOf(purchase.item) }
    var quantity by remember { mutableStateOf(purchase.quantity.toString()) }
    var totalAmount by remember { mutableStateOf(purchase.totalAmount) }
    var totalPaid by remember { mutableStateOf(purchase.totalPaid) }
    var localError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Purchase Order", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (errorMessage != null || localError != null) {
                    ErrorBanner(message = localError ?: errorMessage ?: "")
                }

                VyavsaTextField(
                    value = item,
                    onValueChange = {
                        item = it
                        localError = null
                    },
                    label = "Item Description *"
                )

                VyavsaTextField(
                    value = quantity,
                    onValueChange = {
                        quantity = it
                        localError = null
                    },
                    label = "Quantity *",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                VyavsaTextField(
                    value = totalAmount,
                    onValueChange = {
                        totalAmount = it
                        localError = null
                    },
                    label = "Total Bill Amount (₹) *",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )

                VyavsaTextField(
                    value = totalPaid,
                    onValueChange = {
                        totalPaid = it
                        localError = null
                    },
                    label = "Total Paid (₹)",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (item.isBlank()) {
                        localError = "Item description is required"
                        return@Button
                    }
                    val qty = quantity.toIntOrNull() ?: 0
                    if (qty < 1) {
                        localError = "Quantity must be at least 1"
                        return@Button
                    }
                    val tot = totalAmount.toDoubleOrNull() ?: 0.0
                    if (tot <= 0.0) {
                        localError = "Total amount must be greater than zero"
                        return@Button
                    }
                    val pd = totalPaid.toDoubleOrNull() ?: 0.0
                    if (pd < 0.0 || pd > tot) {
                        localError = "Paid amount cannot be negative or exceed total amount"
                        return@Button
                    }
                    onConfirm(item, quantity, totalAmount, totalPaid)
                },
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
                } else {
                    Text("Save Changes")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text("Cancel")
            }
        }
    )
}
