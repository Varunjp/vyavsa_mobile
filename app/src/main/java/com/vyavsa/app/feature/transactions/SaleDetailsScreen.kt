@file:Suppress("DEPRECATION")
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
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Route
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
import com.vyavsa.app.core.common.FormatUtils
import com.vyavsa.app.core.common.UiState
import com.vyavsa.app.core.designsystem.Blue50
import com.vyavsa.app.core.designsystem.Blue600
import com.vyavsa.app.core.designsystem.Crimson50
import com.vyavsa.app.core.designsystem.Crimson600
import com.vyavsa.app.core.designsystem.Emerald50
import com.vyavsa.app.core.designsystem.Emerald600
import com.vyavsa.app.core.designsystem.ErrorBanner
import com.vyavsa.app.core.designsystem.StatBadge
import com.vyavsa.app.core.designsystem.VyavsaButton
import com.vyavsa.app.core.designsystem.VyavsaTextField
import com.vyavsa.app.core.model.CounterSale
import com.vyavsa.app.core.model.CounterSalePayment
import com.vyavsa.app.core.model.LineSale
import com.vyavsa.app.core.model.LineSalePayment
import com.vyavsa.app.core.model.TenantBank

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaleDetailsScreen(
    viewModel: SaleDetailsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val detailState by viewModel.detailState.collectAsState()
    val paymentActionState by viewModel.paymentActionState.collectAsState()
    val banks by viewModel.banksState.collectAsState()

    var showPaymentDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (viewModel.saleType == "counter") "Counter Sale Details" else "Line Sale Details",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Transaction & payment audit record",
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
            when (val state = detailState) {
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
                        OutlinedButton(onClick = { viewModel.loadSaleDetails() }) {
                            Text("Retry")
                        }
                    }
                }
                is UiState.Success -> {
                    when (val data = state.data) {
                        is SaleDetailData.Line -> {
                            LineSaleDetailsContent(
                                sale = data.sale,
                                onRecordPaymentClick = { showPaymentDialog = true }
                            )
                        }
                        is SaleDetailData.Counter -> {
                            CounterSaleDetailsContent(
                                sale = data.sale,
                                onRecordPaymentClick = { showPaymentDialog = true }
                            )
                        }
                    }
                }
                else -> {}
            }
        }
    }

    if (showPaymentDialog) {
        val maxDue = when (val state = detailState) {
            is UiState.Success -> when (val data = state.data) {
                is SaleDetailData.Line -> data.sale.balance.toDoubleOrNull() ?: 0.0
                is SaleDetailData.Counter -> 0.0
            }
            else -> 0.0
        }

        RecordSalePaymentDialog(
            maxDue = maxDue,
            banks = banks,
            onDismiss = {
                showPaymentDialog = false
                viewModel.clearPaymentActionState()
            },
            onConfirm = { amount, method, bankId, note ->
                viewModel.recordPayment(amount, method, bankId, note)
                showPaymentDialog = false
            }
        )
    }
}

@Composable
private fun LineSaleDetailsContent(
    sale: LineSale,
    onRecordPaymentClick: () -> Unit
) {
    val scrollState = rememberScrollState()
    val balanceDue = sale.balance.toDoubleOrNull() ?: 0.0
    val isFullyPaid = balanceDue <= 0.0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Customer Overview Card
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f), RoundedCornerShape(24.dp))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = sale.customerName.ifBlank { "Line Customer" },
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Customer ID: ${sale.customerId.take(8)}...",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DetailChip(
                        icon = Icons.Default.Route,
                        label = "Route",
                        value = sale.route?.ifBlank { null } ?: "Standard Route",
                        modifier = Modifier.weight(1f)
                    )
                    DetailChip(
                        icon = Icons.Default.Person,
                        label = "Salesman",
                        value = sale.salesman?.ifBlank { null } ?: "Direct Delivery",
                        modifier = Modifier.weight(1f)
                    )
                }

                if (!sale.note.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Note: ${sale.note}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }

        // Financial Position Card
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f), RoundedCornerShape(24.dp))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Sale Summary",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    StatBadge(
                        text = if (isFullyPaid) "PAID" else "DUE: ${FormatUtils.formatCurrency(balanceDue)}",
                        backgroundColor = if (isFullyPaid) Emerald50 else Crimson50,
                        textColor = if (isFullyPaid) Emerald600 else Crimson600
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                FinancialRow(label = "Total Invoice Amount", value = FormatUtils.formatCurrency(sale.totalAmount), isBold = true)
                FinancialRow(label = "Cash In Drawer", value = FormatUtils.formatCurrency(sale.totalCashIn))
                FinancialRow(label = "Bank / Online Received", value = FormatUtils.formatCurrency(sale.bankAmount))
                FinancialRow(label = "Total Collected", value = FormatUtils.formatCurrency(sale.collectedAmount), color = Emerald600)
                FinancialRow(
                    label = "Remaining Balance Due",
                    value = FormatUtils.formatCurrency(sale.balance),
                    color = if (balanceDue > 0) Crimson600 else MaterialTheme.colorScheme.onSurface,
                    isBold = true
                )

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Recorded: ${FormatUtils.formatDateTimeForDisplay(sale.createdAt)}",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )

                if (!isFullyPaid) {
                    Spacer(modifier = Modifier.height(16.dp))
                    VyavsaButton(
                        text = "Record Customer Payment",
                        onClick = onRecordPaymentClick,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Payments Breakdown Card
        PaymentsBreakdownCard(payments = sale.payments)
    }
}

@Composable
private fun CounterSaleDetailsContent(
    sale: CounterSale,
    onRecordPaymentClick: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Item Details Card
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f), RoundedCornerShape(24.dp))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Emerald50)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = null,
                            tint = Emerald600,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = sale.item.ifBlank { "Counter Item" },
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "POS Bill ID: ${sale.id.take(8)}...",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Payment Method",
                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                    StatBadge(
                        text = sale.paymentMethod.uppercase(),
                        backgroundColor = Blue50,
                        textColor = Blue600
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                FinancialRow(label = "Price", value = FormatUtils.formatCurrency(sale.price))
                FinancialRow(label = "Total Bill Amount", value = FormatUtils.formatCurrency(sale.totalAmount), isBold = true)
                FinancialRow(label = "Cash Portion", value = FormatUtils.formatCurrency(sale.cash))
                FinancialRow(label = "Bank / Online Portion", value = FormatUtils.formatCurrency(sale.bankAmount.ifBlank { sale.account }))

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Billed: ${FormatUtils.formatDateTimeForDisplay(sale.createdAt)}",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
            }
        }

        // Counter Payments Breakdown
        if (sale.payments.isNotEmpty()) {
            CounterPaymentsBreakdownCard(payments = sale.payments)
        }
    }
}

@Composable
private fun PaymentsBreakdownCard(payments: List<LineSalePayment>) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f), RoundedCornerShape(24.dp))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Payment Transactions Breakdown",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (payments.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No separate payment records attached. Initial payment was recorded at time of order creation.",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                        modifier = Modifier.padding(14.dp)
                    )
                }
            } else {
                payments.forEachIndexed { index, payment ->
                    PaymentRowItem(
                        method = payment.paymentMethod,
                        bankName = payment.bankName,
                        amount = payment.amount,
                        date = payment.paymentDate ?: payment.createdAt,
                        note = payment.note,
                        referenceId = payment.referenceId
                    )
                    if (index < payments.size - 1) {
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun CounterPaymentsBreakdownCard(payments: List<CounterSalePayment>) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f), RoundedCornerShape(24.dp))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Payment Allocations",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(12.dp))

            payments.forEachIndexed { index, payment ->
                PaymentRowItem(
                    method = payment.paymentMethod,
                    bankName = payment.bankName,
                    amount = payment.amount,
                    date = payment.paymentDate ?: payment.createdAt,
                    note = payment.note,
                    referenceId = payment.referenceId
                )
                if (index < payments.size - 1) {
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
private fun PaymentRowItem(
    method: String,
    bankName: String?,
    amount: String,
    date: String,
    note: String?,
    referenceId: String?
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatBadge(
                        text = method.uppercase(),
                        backgroundColor = Blue50,
                        textColor = Blue600
                    )
                    if (!bankName.isNullOrBlank()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = bankName,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                }
                if (!note.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = note,
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
                if (!referenceId.isNullOrBlank()) {
                    Text(
                        text = "Ref: $referenceId",
                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            }
            Text(
                text = FormatUtils.formatCurrency(amount),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Emerald600)
            )
        }
    }
}

@Composable
private fun DetailChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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
private fun RecordSalePaymentDialog(
    maxDue: Double,
    banks: List<TenantBank>,
    onDismiss: () -> Unit,
    onConfirm: (amount: String, method: String, bankId: String?, note: String?) -> Unit
) {
    var amount by remember { mutableStateOf(if (maxDue > 0) String.format("%.2f", maxDue) else "") }
    var selectedMethod by remember { mutableStateOf("cash") }
    var selectedBankId by remember { mutableStateOf<String?>(banks.firstOrNull()?.id) }
    var note by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Payment on Sale", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                VyavsaTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = "Amount (₹)",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )

                Text("Payment Method", style = MaterialTheme.typography.labelSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("cash", "bank", "cheque", "upi").forEach { method ->
                        FilterChip(
                            selected = selectedMethod == method,
                            onClick = { selectedMethod = method },
                            label = { Text(method.uppercase()) }
                        )
                    }
                }

                if (selectedMethod != "cash" && banks.isNotEmpty()) {
                    Text("Select Bank Account", style = MaterialTheme.typography.labelSmall)
                    banks.forEach { bank ->
                        FilterChip(
                            selected = selectedBankId == bank.id,
                            onClick = { selectedBankId = bank.id },
                            label = { Text(bank.bankName) },
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                }

                VyavsaTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = "Note / Cheque Ref (Optional)"
                )

                if (errorText != null) {
                    Text(errorText!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val amt = amount.toDoubleOrNull()
                if (amt == null || amt <= 0.0) {
                    errorText = "Enter a valid payment amount"
                    return@Button
                }
                if (selectedMethod != "cash" && selectedBankId.isNullOrBlank()) {
                    errorText = "Please select a bank account"
                    return@Button
                }
                onConfirm(amount, selectedMethod, selectedBankId, note.ifBlank { null })
            }) {
                Text("Record Payment")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
