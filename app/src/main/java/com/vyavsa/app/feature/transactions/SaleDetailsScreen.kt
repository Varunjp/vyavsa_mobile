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
import android.content.Intent
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import com.vyavsa.app.core.printer.ThermalPrintDialog
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import com.vyavsa.app.core.document.InvoiceShareManager
import com.vyavsa.app.core.printer.InvoiceDataMapper
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.vyavsa.app.core.common.FormatUtils
import com.vyavsa.app.core.common.UiState
import com.vyavsa.app.core.designsystem.Blue50
import com.vyavsa.app.core.designsystem.Blue600
import com.vyavsa.app.core.designsystem.CompanyLogoImage
import com.vyavsa.app.core.designsystem.Crimson50
import com.vyavsa.app.core.designsystem.Crimson600
import com.vyavsa.app.core.designsystem.Emerald50
import com.vyavsa.app.core.designsystem.Emerald600
import com.vyavsa.app.core.designsystem.ErrorBanner
import com.vyavsa.app.core.designsystem.StatBadge
import com.vyavsa.app.core.designsystem.VyavsaButton
import com.vyavsa.app.core.designsystem.VyavsaOutlinedButton
import com.vyavsa.app.core.designsystem.VyavsaTextField
import com.vyavsa.app.core.designsystem.VyavsaTopAppBar
import com.vyavsa.app.core.model.CounterSale
import com.vyavsa.app.core.model.CounterSalePayment
import com.vyavsa.app.core.model.LineSale
import com.vyavsa.app.core.model.LineSalePayment
import com.vyavsa.app.core.model.SaleItem
import com.vyavsa.app.core.model.TenantBank
import com.vyavsa.app.core.model.TenantProfile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaleDetailsScreen(
    viewModel: SaleDetailsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val detailState by viewModel.detailState.collectAsState()
    val banks by viewModel.banksState.collectAsState()
    val companyProfile by viewModel.companyProfile.collectAsState()

    val printJobState by viewModel.printJobState.collectAsState()
    val pairedPrinters by viewModel.pairedPrinters.collectAsState()
    val usbPrinters by viewModel.usbPrinters.collectAsState()
    val selectedConnectionType by viewModel.selectedConnectionType.collectAsState()
    val networkPrinterIp by viewModel.networkPrinterIp.collectAsState()
    val selectedPrinter by viewModel.selectedPrinter.collectAsState()
    val paperWidth by viewModel.paperWidth.collectAsState()

    var showPaymentDialog by remember { mutableStateOf(false) }
    var showReceiptDialog by remember { mutableStateOf(false) }
    var showPrintDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            VyavsaTopAppBar(
                title = if (viewModel.saleType == "counter") "Counter Sale Details" else "Line Sale Details",
                subtitle = "Transaction & payment audit record",
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
                                onRecordPaymentClick = { showPaymentDialog = true },
                                onShowReceiptClick = { showReceiptDialog = true },
                                onPrintClick = { showPrintDialog = true }
                            )
                        }
                        is SaleDetailData.Counter -> {
                            CounterSaleDetailsContent(
                                sale = data.sale,
                                onRecordPaymentClick = { showPaymentDialog = true },
                                onShowReceiptClick = { showReceiptDialog = true },
                                onPrintClick = { showPrintDialog = true }
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

    if (showReceiptDialog) {
        when (val state = detailState) {
            is UiState.Success -> {
                InvoiceReceiptDialog(
                    saleData = state.data,
                    companyProfile = companyProfile,
                    onPrintClick = { showPrintDialog = true },
                    onDismiss = { showReceiptDialog = false }
                )
            }
            else -> {}
        }
    }

    if (showPrintDialog) {
        ThermalPrintDialog(
            printJobState = printJobState,
            pairedPrinters = pairedPrinters,
            usbPrinters = usbPrinters,
            selectedConnectionType = selectedConnectionType,
            networkPrinterIp = networkPrinterIp,
            selectedPrinter = selectedPrinter,
            paperWidth = paperWidth,
            hasCompanyLogo = !companyProfile?.logoUrl.isNullOrBlank(),
            receiptPreviewText = viewModel.getReceiptPreviewText(),
            logoUrl = companyProfile?.logoUrl,
            businessName = companyProfile?.name,
            logoBitmap = viewModel.getCachedLogoBitmap(),
            onSelectConnectionType = { viewModel.setConnectionType(it) },
            onSetNetworkPrinterIp = { viewModel.setNetworkPrinterIp(it) },
            onSelectPrinter = { viewModel.selectPrinter(it) },
            onSelectPaperWidth = { viewModel.setPaperWidth(it) },
            onRefreshPrinters = { viewModel.refreshPrinters() },
            onPrintClick = { viewModel.printInvoice() },
            onDismiss = {
                showPrintDialog = false
                viewModel.resetPrintJobState()
            }
        )
    }
}

@Composable
private fun LineSaleDetailsContent(
    sale: LineSale,
    onRecordPaymentClick: () -> Unit,
    onShowReceiptClick: () -> Unit,
    onPrintClick: () -> Unit
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

        // Sale Items Breakdown Card (if items were attached)
        if (sale.items.isNotEmpty()) {
            SaleItemsBreakdownCard(items = sale.items)
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

                val disc = sale.discount.toDoubleOrNull() ?: 0.0
                val sub = sale.subtotal.toDoubleOrNull() ?: (sale.totalAmount.toDoubleOrNull() ?: 0.0)
                val effectiveSub = if (sub > 0.0) sub else (sale.totalAmount.toDoubleOrNull() ?: 0.0) + disc

                FinancialRow(label = "Sale Subtotal", value = FormatUtils.formatCurrency(effectiveSub.toString()))
                if (disc > 0.0) {
                    FinancialRow(
                        label = "Discount",
                        value = "-${FormatUtils.formatCurrency(disc.toString())}",
                        color = Emerald600,
                        isBold = true
                    )
                }
                FinancialRow(label = "Final Payable Amount", value = FormatUtils.formatCurrency(sale.totalAmount), isBold = true)
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

                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    VyavsaOutlinedButton(
                        text = "View Bill",
                        leadingIcon = Icons.Default.ReceiptLong,
                        onClick = onShowReceiptClick,
                        modifier = Modifier.weight(1f)
                    )
                    VyavsaButton(
                        text = "Print Invoice",
                        leadingIcon = Icons.Default.Print,
                        onClick = onPrintClick,
                        modifier = Modifier.weight(1.2f)
                    )
                }

                if (!isFullyPaid) {
                    Spacer(modifier = Modifier.height(12.dp))
                    VyavsaButton(
                        text = "Record Customer Payment",
                        leadingIcon = Icons.Default.Payment,
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
    onRecordPaymentClick: () -> Unit,
    onShowReceiptClick: () -> Unit,
    onPrintClick: () -> Unit
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

                val disc = sale.discount.toDoubleOrNull() ?: 0.0
                val sub = sale.subtotal.toDoubleOrNull() ?: (sale.price.toDoubleOrNull() ?: (sale.totalAmount.toDoubleOrNull() ?: 0.0))
                val effectiveSub = if (sub > 0.0) sub else (sale.totalAmount.toDoubleOrNull() ?: 0.0) + disc

                FinancialRow(label = "Sale Subtotal", value = FormatUtils.formatCurrency(effectiveSub.toString()))
                if (disc > 0.0) {
                    FinancialRow(
                        label = "Discount",
                        value = "-${FormatUtils.formatCurrency(disc.toString())}",
                        color = Emerald600,
                        isBold = true
                    )
                }
                FinancialRow(label = "Final Total Amount", value = FormatUtils.formatCurrency(sale.totalAmount), isBold = true)
                FinancialRow(label = "Cash Portion", value = FormatUtils.formatCurrency(sale.cash))
                FinancialRow(label = "Bank / Online Portion", value = FormatUtils.formatCurrency(sale.bankAmount.ifBlank { sale.account }))

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Billed: ${FormatUtils.formatDateTimeForDisplay(sale.createdAt)}",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )

                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    VyavsaOutlinedButton(
                        text = "View Bill",
                        leadingIcon = Icons.Default.ReceiptLong,
                        onClick = onShowReceiptClick,
                        modifier = Modifier.weight(1f)
                    )
                    VyavsaButton(
                        text = "Print Invoice",
                        leadingIcon = Icons.Default.Print,
                        onClick = onPrintClick,
                        modifier = Modifier.weight(1.2f)
                    )
                }
            }
        }

        // Sale Items Breakdown Card (if items were attached)
        if (sale.items.isNotEmpty()) {
            SaleItemsBreakdownCard(items = sale.items)
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
    var amount by remember { mutableStateOf(if (maxDue > 0) String.format(java.util.Locale.US, "%.2f", maxDue) else "") }
    var selectedMethod by remember { mutableStateOf("cash") }
    var selectedBankId by remember { mutableStateOf<String?>(banks.firstOrNull()?.id) }
    var note by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Record Payment on Sale", fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                VyavsaTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = "Amount (₹)",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )

                Text("Payment Method", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf("cash", "bank", "cheque", "upi").forEach { method ->
                        FilterChip(
                            selected = selectedMethod == method,
                            onClick = { selectedMethod = method },
                            shape = CircleShape,
                            label = { Text(method.uppercase()) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (selectedMethod != "cash" && banks.isNotEmpty()) {
                    Text("Select Bank Account", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                    banks.forEach { bank ->
                        FilterChip(
                            selected = selectedBankId == bank.id,
                            onClick = { selectedBankId = bank.id },
                            shape = CircleShape,
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
            Button(
                onClick = {
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
                },
                shape = CircleShape
            ) {
                Text("Record Payment", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun SaleItemsBreakdownCard(
    items: List<SaleItem>,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f), RoundedCornerShape(24.dp))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Inventory2,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Sale Items Breakdown",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                StatBadge(
                    text = "${items.size} ${if (items.size == 1) "item" else "items"}",
                    backgroundColor = MaterialTheme.colorScheme.primaryContainer,
                    textColor = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            items.forEachIndexed { index, item ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.productNameSnapshot.ifBlank { "Item #${index + 1}" },
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Qty: ${item.quantity}   •   Price: ${FormatUtils.formatCurrency(item.unitPrice)}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }

                        Text(
                            text = FormatUtils.formatCurrency(item.lineTotal),
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
                if (index < items.lastIndex) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            val subtotal = items.sumOf { it.lineTotal.toDoubleOrNull() ?: 0.0 }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Items Subtotal",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Text(
                    text = FormatUtils.formatCurrency(subtotal.toString()),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        }
    }
}

private data class ReceiptDisplayData(
    val title: String,
    val saleId: String,
    val date: String,
    val customerInfo: String,
    val items: List<SaleItem>,
    val singleItemName: String?,
    val subtotal: Double,
    val discount: Double,
    val total: Double,
    val paid: Double,
    val due: Double,
    val paymentMethodSummary: String
)

@Composable
private fun InvoiceReceiptDialog(
    saleData: SaleDetailData,
    companyProfile: TenantProfile?,
    onPrintClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val receiptData = when (saleData) {
        is SaleDetailData.Line -> {
            val sale = saleData.sale
            val disc = sale.discount.toDoubleOrNull() ?: 0.0
            val sub = sale.subtotal.toDoubleOrNull() ?: 0.0
            val effectiveSub = if (sub > 0.0) sub else (sale.totalAmount.toDoubleOrNull() ?: 0.0) + disc
            val tot = sale.totalAmount.toDoubleOrNull() ?: 0.0
            val bal = sale.balance.toDoubleOrNull() ?: 0.0
            val paidAmt = (tot - bal).coerceAtLeast(0.0)
            val customer = buildString {
                append(sale.customerName.ifBlank { "Line Customer" })
                if (!sale.route.isNullOrBlank()) append(" (Route: ${sale.route})")
            }
            ReceiptDisplayData(
                title = "Tax Invoice / Bill",
                saleId = sale.id,
                date = FormatUtils.formatDateTimeForDisplay(sale.createdAt),
                customerInfo = customer,
                items = sale.items,
                singleItemName = null,
                subtotal = effectiveSub,
                discount = disc,
                total = tot,
                paid = paidAmt,
                due = bal,
                paymentMethodSummary = "Cash: ${FormatUtils.formatCurrency(sale.cash)} | Bank: ${FormatUtils.formatCurrency(sale.bankAmount)}"
            )
        }
        is SaleDetailData.Counter -> {
            val sale = saleData.sale
            val disc = sale.discount.toDoubleOrNull() ?: 0.0
            val sub = sale.subtotal.toDoubleOrNull() ?: (sale.price.toDoubleOrNull() ?: (sale.totalAmount.toDoubleOrNull() ?: 0.0))
            val effectiveSub = if (sub > 0.0) sub else (sale.totalAmount.toDoubleOrNull() ?: 0.0) + disc
            val tot = sale.totalAmount.toDoubleOrNull() ?: 0.0
            ReceiptDisplayData(
                title = "Counter Sale Receipt",
                saleId = sale.id,
                date = FormatUtils.formatDateTimeForDisplay(sale.createdAt),
                customerInfo = "Counter Customer (Walk-in)",
                items = sale.items,
                singleItemName = sale.item.ifBlank { "Counter Item" },
                subtotal = effectiveSub,
                discount = disc,
                total = tot,
                paid = tot,
                due = 0.0,
                paymentMethodSummary = "Method: ${sale.paymentMethod.uppercase()} | Cash: ${FormatUtils.formatCurrency(sale.cash)} | Bank: ${FormatUtils.formatCurrency(sale.bankAmount.ifBlank { sale.account })}"
            )
        }
    }

    val coroutineScope = rememberCoroutineScope()
    var isSharingPdf by remember { mutableStateOf(false) }

    fun buildTextReceipt(): String = buildString {
        appendLine("==================================")
        appendLine(companyProfile?.name?.ifBlank { "VYAVSA BUSINESS" } ?: "VYAVSA BUSINESS")
        val address = companyProfile?.getEffectiveAddress()
        if (!address.isNullOrBlank()) {
            appendLine(address)
        }
        val phone = companyProfile?.phone
        if (!phone.isNullOrBlank()) {
            appendLine("Phone: $phone")
        }
        val email = companyProfile?.email
        if (!email.isNullOrBlank()) {
            appendLine("Email: $email")
        }
        appendLine("==================================")
        appendLine(receiptData.title.uppercase())
        appendLine("Bill ID: ${receiptData.saleId}")
        appendLine("Date: ${receiptData.date}")
        appendLine("Customer: ${receiptData.customerInfo}")
        appendLine("----------------------------------")
        if (receiptData.items.isNotEmpty()) {
            appendLine("ITEMS:")
            receiptData.items.forEach { item ->
                appendLine(" - ${item.productNameSnapshot}: ${item.quantity} x ${FormatUtils.formatCurrency(item.unitPrice)} = ${FormatUtils.formatCurrency(item.lineTotal)}")
            }
            appendLine("----------------------------------")
        } else if (!receiptData.singleItemName.isNullOrBlank()) {
            appendLine("Item: ${receiptData.singleItemName}")
            appendLine("----------------------------------")
        }
        appendLine("Subtotal: ${FormatUtils.formatCurrency(receiptData.subtotal.toString())}")
        if (receiptData.discount > 0.0) {
            appendLine("Discount: -${FormatUtils.formatCurrency(receiptData.discount.toString())}")
        }
        appendLine("Total Payable: ${FormatUtils.formatCurrency(receiptData.total.toString())}")
        appendLine("Amount Paid: ${FormatUtils.formatCurrency(receiptData.paid.toString())}")
        if (receiptData.due > 0.0) {
            appendLine("Balance Due: ${FormatUtils.formatCurrency(receiptData.due.toString())}")
        } else {
            appendLine("Payment Status: PAID IN FULL")
        }
        appendLine("Payment: ${receiptData.paymentMethodSummary}")
        appendLine("==================================")
        appendLine(companyProfile?.getEffectiveFooterHeading() ?: "Thank You for Your Business!")
        appendLine(companyProfile?.getEffectiveFooterParagraph() ?: "Generated via Vyavsa Bill Book")
    }

    fun shareReceipt() {
        if (isSharingPdf) return
        coroutineScope.launch {
            isSharingPdf = true
            try {
                val printData = when (saleData) {
                    is SaleDetailData.Line -> InvoiceDataMapper.fromLineSale(saleData.sale, companyProfile, null)
                    is SaleDetailData.Counter -> InvoiceDataMapper.fromCounterSale(saleData.sale, companyProfile, null)
                }
                val shareResult = InvoiceShareManager.shareInvoicePdf(context, printData)
                if (shareResult.isFailure) {
                    val text = buildTextReceipt()
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, text)
                        type = "text/plain"
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Share Bill Receipt via"))
                }
            } catch (_: Exception) {
                val text = buildTextReceipt()
                val sendIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, text)
                    type = "text/plain"
                }
                context.startActivity(Intent.createChooser(sendIntent, "Share Bill Receipt via"))
            } finally {
                isSharingPdf = false
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 20.dp)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(20.dp)
            ) {
                // Company Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        CompanyLogoImage(
                            logoUrl = companyProfile?.logoUrl,
                            companyName = companyProfile?.name ?: "Vyavsa Business",
                            size = 48.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = companyProfile?.name?.ifBlank { "Vyavsa Business" } ?: "Vyavsa Business",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            val phone = companyProfile?.phone
                            if (!phone.isNullOrBlank()) {
                                Text(
                                    text = "Ph: $phone",
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                            }
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // Company Address Header
                val effectiveAddress = companyProfile?.getEffectiveAddress()
                if (!effectiveAddress.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = effectiveAddress,
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                                maxLines = 2,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(12.dp))

                // Title & Status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = receiptData.title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    if (receiptData.due <= 0.0) {
                        StatBadge(text = "PAID IN FULL", backgroundColor = Emerald50, textColor = Emerald600)
                    } else {
                        StatBadge(text = "DUE: ${FormatUtils.formatCurrency(receiptData.due.toString())}", backgroundColor = Crimson50, textColor = Crimson600)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Invoice #: ${receiptData.saleId.take(16)}...",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                Text(
                    text = "Date: ${receiptData.date}",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
                Text(
                    text = "Customer: ${receiptData.customerInfo}",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Items list or single item
                if (receiptData.items.isNotEmpty()) {
                    Text(
                        text = "Items Billed",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    receiptData.items.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.productNameSnapshot,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Qty: ${item.quantity}   •   Price: ${FormatUtils.formatCurrency(item.unitPrice)}",
                                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                            }
                            Text(
                                text = FormatUtils.formatCurrency(item.lineTotal),
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                } else if (!receiptData.singleItemName.isNullOrBlank()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = receiptData.singleItemName,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                        )
                        Text(
                            text = FormatUtils.formatCurrency(receiptData.subtotal.toString()),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(10.dp))

                // Financial Totals
                FinancialRow(label = "Subtotal", value = FormatUtils.formatCurrency(receiptData.subtotal.toString()))

                if (receiptData.discount > 0.0) {
                    FinancialRow(
                        label = "Discount",
                        value = "-${FormatUtils.formatCurrency(receiptData.discount.toString())}",
                        color = Emerald600,
                        isBold = true
                    )
                }

                FinancialRow(
                    label = "Total Payable Amount",
                    value = FormatUtils.formatCurrency(receiptData.total.toString()),
                    isBold = true
                )

                FinancialRow(
                    label = "Amount Paid",
                    value = FormatUtils.formatCurrency(receiptData.paid.toString())
                )

                if (receiptData.due > 0.0) {
                    FinancialRow(
                        label = "Balance Due",
                        value = FormatUtils.formatCurrency(receiptData.due.toString()),
                        color = Crimson600,
                        isBold = true
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = receiptData.paymentMethodSummary,
                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )

                // Configured Invoice Footer
                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(10.dp))

                val footerHeading = companyProfile?.getEffectiveFooterHeading() ?: "Thank You for Your Business!"
                val footerParagraph = companyProfile?.getEffectiveFooterParagraph() ?: "Thank you for choosing us. We appreciate your trust and look forward to serving you again."

                Text(
                    text = footerHeading,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = footerParagraph,
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    VyavsaOutlinedButton(
                        text = "Close",
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    )
                    VyavsaOutlinedButton(
                        text = "Share",
                        leadingIcon = Icons.Default.Share,
                        onClick = { shareReceipt() },
                        modifier = Modifier.weight(1.1f)
                    )
                    VyavsaButton(
                        text = "Print",
                        leadingIcon = Icons.Default.Print,
                        onClick = {
                            onDismiss()
                            onPrintClick()
                        },
                        modifier = Modifier.weight(1.1f)
                    )
                }
            }
        }
    }
}


