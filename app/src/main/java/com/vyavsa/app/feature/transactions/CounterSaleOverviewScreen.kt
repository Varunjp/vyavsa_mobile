package com.vyavsa.app.feature.transactions

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vyavsa.app.core.common.FormatUtils
import com.vyavsa.app.core.common.UiState
import com.vyavsa.app.core.designsystem.Amber50
import com.vyavsa.app.core.designsystem.Amber600
import com.vyavsa.app.core.designsystem.BankBreakdownCard
import com.vyavsa.app.core.designsystem.Blue600
import com.vyavsa.app.core.designsystem.Crimson50
import com.vyavsa.app.core.designsystem.Crimson600
import com.vyavsa.app.core.designsystem.Emerald50
import com.vyavsa.app.core.designsystem.Emerald600
import com.vyavsa.app.core.designsystem.MetricCard
import com.vyavsa.app.core.designsystem.PaymentDistributionChart
import com.vyavsa.app.core.designsystem.Purple50
import com.vyavsa.app.core.designsystem.Purple600
import com.vyavsa.app.core.designsystem.VyavsaSectionHeader
import com.vyavsa.app.core.designsystem.VyavsaTopAppBar
import com.vyavsa.app.core.model.CounterSalePaymentBreakdown

/**
 * Dedicated Counter Sales Overview Screen displaying today's payment breakdown,
 * four key financial summary cards, dynamic multi-bank allocations, payment distribution chart,
 * and a prominent bottom action to inspect today's detailed counter sales list.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CounterSaleOverviewScreen(
    viewModel: CounterSaleOverviewViewModel,
    onNavigateBack: () -> Unit,
    onViewTodaySales: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    var isNavigating by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            VyavsaTopAppBar(
                title = "Counter Sales Overview",
                subtitle = "Today's payment breakdown & bank splits",
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                onNavigationClick = onNavigateBack,
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh data"
                        )
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                shadowElevation = 8.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    Button(
                        onClick = {
                            if (!isNavigating) {
                                isNavigating = true
                                onViewTodaySales()
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "View Today's Sales",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val s = state) {
                is UiState.Loading -> {
                    CounterSaleOverviewLoading(modifier = Modifier.fillMaxSize())
                }
                is UiState.Error -> {
                    CounterSaleOverviewError(
                        message = s.message,
                        onRetry = { viewModel.refresh() },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                is UiState.Empty -> {
                    CounterSaleOverviewEmpty(
                        onRefresh = { viewModel.refresh() },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                is UiState.Success -> {
                    CounterSaleOverviewContent(
                        breakdown = s.data,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                is UiState.Idle -> {
                    // Handled automatically via ViewModel init
                }
            }
        }
    }
}

@Composable
private fun CounterSaleOverviewContent(
    breakdown: CounterSalePaymentBreakdown,
    modifier: Modifier = Modifier
) {
    val cashVal = breakdown.cashReceived.toDoubleOrNull() ?: 0.0
    val bankVal = breakdown.bankReceived.toDoubleOrNull() ?: 0.0
    val dueVal = breakdown.totalDue.toDoubleOrNull() ?: 0.0
    val advanceVal = breakdown.advanceApplied.toDoubleOrNull() ?: 0.0
    val payableVal = breakdown.effectivePayableApplied.toDoubleOrNull() ?: 0.0

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
    ) {
        // Section A: Summary Metric Cards (2x2 Grid + Dynamic Advance / Payable Rows)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    MetricCard(
                        title = "Total Counter Sales",
                        amount = FormatUtils.formatCurrency(breakdown.totalSales),
                        subtitle = "${breakdown.salesCount} sale${if (breakdown.salesCount == 1L) "" else "s"} today",
                        icon = Icons.Default.Storefront,
                        iconTint = MaterialTheme.colorScheme.primary,
                        iconBackground = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Cash Received",
                        amount = FormatUtils.formatCurrency(breakdown.cashReceived),
                        subtitle = "Drawer cash",
                        icon = Icons.Default.Payments,
                        iconTint = Emerald600,
                        iconBackground = Emerald50,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    MetricCard(
                        title = "Bank Received",
                        amount = FormatUtils.formatCurrency(breakdown.bankReceived),
                        subtitle = "${breakdown.bankBreakdown.size} bank account${if (breakdown.bankBreakdown.size == 1) "" else "s"}",
                        icon = Icons.Default.AccountBalance,
                        iconTint = Blue600,
                        iconBackground = Color(0xFFEFF6FF),
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Outstanding Due",
                        amount = FormatUtils.formatCurrency(breakdown.totalDue),
                        subtitle = "Customer khata dues",
                        icon = Icons.Default.Receipt,
                        iconTint = Crimson600,
                        iconBackground = Crimson50,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Dynamic Advance & Payable Adjustment Cards
                if (advanceVal > 0.0 || payableVal > 0.0) {
                    if (advanceVal > 0.0 && payableVal > 0.0) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            MetricCard(
                                title = "Adjusted Against Advance",
                                amount = FormatUtils.formatCurrency(breakdown.advanceApplied),
                                subtitle = "Prepaid advance applied",
                                icon = Icons.Default.AccountBalanceWallet,
                                iconTint = Amber600,
                                iconBackground = Amber50,
                                modifier = Modifier.weight(1f)
                            )
                            MetricCard(
                                title = "Adjusted Against Payable",
                                amount = FormatUtils.formatCurrency(breakdown.effectivePayableApplied),
                                subtitle = "Supplier payable offset",
                                icon = Icons.Default.SwapHoriz,
                                iconTint = Purple600,
                                iconBackground = Purple50,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    } else if (advanceVal > 0.0) {
                        MetricCard(
                            title = "Adjusted Against Advance",
                            amount = FormatUtils.formatCurrency(breakdown.advanceApplied),
                            subtitle = "Prepaid advance applied",
                            icon = Icons.Default.AccountBalanceWallet,
                            iconTint = Amber600,
                            iconBackground = Amber50,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        MetricCard(
                            title = "Adjusted Against Payable",
                            amount = FormatUtils.formatCurrency(breakdown.effectivePayableApplied),
                            subtitle = "Supplier payable offset",
                            icon = Icons.Default.SwapHoriz,
                            iconTint = Purple600,
                            iconBackground = Purple50,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // Section B: Dedicated Counter Sale Allocation Breakup Card (if adjustments present)
        if (advanceVal > 0.0 || payableVal > 0.0) {
            item {
                VyavsaSectionHeader(
                    title = "Sale Allocation Breakup",
                    badgeText = if (breakdown.isReconciled) "Reconciled ✓" else null
                )
                Spacer(modifier = Modifier.height(4.dp))
                CounterSaleAllocationBreakupCard(breakdown = breakdown)
            }
        }

        // Section C: Payment Distribution Chart
        item {
            VyavsaSectionHeader(title = "Payment Distribution")
            Spacer(modifier = Modifier.height(4.dp))
            PaymentDistributionChart(
                cashAmount = cashVal,
                bankPayments = breakdown.bankBreakdown,
                outstandingDue = dueVal,
                advanceApplied = advanceVal,
                payableAdjustment = payableVal
            )
        }

        // Section D: Bank-wise Payment Breakdown
        item {
            VyavsaSectionHeader(
                title = "Bank Breakdown",
                badgeText = "${breakdown.bankBreakdown.size} Accounts"
            )
        }

        if (breakdown.bankBreakdown.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(18.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "No bank payments recorded for today's counter sales. All receipts were cash or customer khata dues.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        } else {
            items(
                items = breakdown.bankBreakdown,
                key = { it.bankId ?: it.bankName }
            ) { bank ->
                BankBreakdownCard(
                    bank = bank,
                    totalBankReceived = bankVal
                )
            }
        }
    }
}

@Composable
private fun CounterSaleOverviewLoading(modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.padding(24.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                strokeWidth = 3.dp,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Loading Counter Sale Breakdown…",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

@Composable
private fun CounterSaleOverviewError(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.padding(24.dp)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, Crimson600.copy(alpha = 0.25f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Crimson50)
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = Crimson600,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Failed to Load Breakdown",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = message.ifBlank { "An unexpected error occurred while communicating with the server." },
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onRetry,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Retry", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun CounterSaleOverviewEmpty(
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.padding(24.dp)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(28.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Icon(
                        imageVector = Icons.Default.Storefront,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No Counter Sales Recorded Today",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Transactions created today will automatically appear here with complete cash, dynamic bank allocations, and dues breakdown.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                )
                Spacer(modifier = Modifier.height(18.dp))
                FilledTonalButton(
                    onClick = onRefresh,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Check Again", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun CounterSaleAllocationBreakupCard(
    breakdown: CounterSalePaymentBreakdown,
    modifier: Modifier = Modifier
) {
    val totalSalesVal = breakdown.totalSales.toDoubleOrNull() ?: 0.0
    val advanceVal = breakdown.advanceApplied.toDoubleOrNull() ?: 0.0
    val payableVal = breakdown.effectivePayableApplied.toDoubleOrNull() ?: 0.0
    val dueVal = breakdown.totalDue.toDoubleOrNull() ?: 0.0
    val cashVal = breakdown.cashReceived.toDoubleOrNull() ?: 0.0
    val bankVal = breakdown.bankReceived.toDoubleOrNull() ?: 0.0

    val isEntirelyPayable = totalSalesVal > 0.0 && payableVal >= totalSalesVal && advanceVal <= 0.0 && dueVal <= 0.0
    val isEntirelyAdvance = totalSalesVal > 0.0 && advanceVal >= totalSalesVal && payableVal <= 0.0 && dueVal <= 0.0
    val isMixed = advanceVal > 0.0 && payableVal > 0.0

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header with icon and status pill
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
                            imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Counter Sale Allocation Breakup",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Text(
                            text = "Advance, payable & balance settlement",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }

                if (isEntirelyPayable) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Purple50,
                        border = BorderStroke(1.dp, Purple600.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = "100% Payable",
                            color = Purple600,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                } else if (isEntirelyAdvance) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Amber50,
                        border = BorderStroke(1.dp, Amber600.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = "100% Advance",
                            color = Amber600,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                } else if (isMixed) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = "Mixed Allocation",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                } else if (breakdown.isReconciled && totalSalesVal > 0.0) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Emerald50,
                        border = BorderStroke(1.dp, Emerald600.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = "Balanced ✓",
                            color = Emerald600,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Breakdown Line Items Table
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        RoundedCornerShape(16.dp)
                    )
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AllocationRow(
                    label = "Total Sale Amount",
                    amount = breakdown.totalSales,
                    isProminent = true
                )

                if (advanceVal > 0.0) {
                    AllocationRow(
                        label = "Adjusted Against Advance",
                        amount = "- ${FormatUtils.formatCurrency(breakdown.advanceApplied)}",
                        color = Amber600,
                        isCustomFormatted = true
                    )
                }

                if (payableVal > 0.0) {
                    AllocationRow(
                        label = "Adjusted Against Payable",
                        amount = "- ${FormatUtils.formatCurrency(breakdown.effectivePayableApplied)}",
                        color = Purple600,
                        isCustomFormatted = true
                    )
                }

                if (cashVal > 0.0) {
                    AllocationRow(
                        label = "Cash Collection",
                        amount = breakdown.cashReceived,
                        color = Emerald600
                    )
                }

                if (bankVal > 0.0) {
                    AllocationRow(
                        label = "Bank / UPI Collection",
                        amount = breakdown.bankReceived,
                        color = Blue600
                    )
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    thickness = 1.dp
                )

                AllocationRow(
                    label = "Remaining Amount (Due)",
                    amount = breakdown.totalDue,
                    color = if (dueVal > 0.0) Crimson600 else Emerald600,
                    isProminent = true
                )
            }

            // Reconciliation Audit Indicator
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (breakdown.isReconciled) Emerald50 else Crimson50)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = if (breakdown.isReconciled) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = if (breakdown.isReconciled) Emerald600 else Crimson600,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (breakdown.isReconciled) {
                        "Financial totals fully verified and balanced with backend ledger."
                    } else {
                        "Discrepancy detected: Sales amount does not match collections & adjustments."
                    },
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = if (breakdown.isReconciled) Emerald600 else Crimson600,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp
                    )
                )
            }
        }
    }
}

@Composable
private fun AllocationRow(
    label: String,
    amount: String,
    color: Color = MaterialTheme.colorScheme.onSurface,
    isProminent: Boolean = false,
    isCustomFormatted: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = if (isProminent) MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
            else MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
        )
        Text(
            text = if (isCustomFormatted) amount else FormatUtils.formatCurrency(amount),
            style = if (isProminent) MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                color = color
            ) else MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.SemiBold,
                color = color
            ),
            fontFamily = FontFamily.Monospace
        )
    }
}
