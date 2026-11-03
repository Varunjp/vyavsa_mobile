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
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vyavsa.app.core.common.FormatUtils
import com.vyavsa.app.core.common.UiState
import com.vyavsa.app.core.designsystem.Amber50
import com.vyavsa.app.core.designsystem.Amber600
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
import com.vyavsa.app.core.model.LineSaleBankBreakdown
import com.vyavsa.app.core.model.LineSalePaymentBreakdown
import java.util.Locale

/**
 * Dedicated Line Sales Overview Screen displaying today's payment breakdown,
 * four key financial summary cards, dynamic multi-bank allocations, payment distribution chart,
 * and a prominent bottom action to inspect today's detailed line sales list.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LineSaleOverviewScreen(
    viewModel: LineSaleOverviewViewModel,
    onNavigateBack: () -> Unit,
    onViewTodaySales: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    var isNavigating by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            VyavsaTopAppBar(
                title = "Line Sales Overview",
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
                    LineSaleOverviewLoading(modifier = Modifier.fillMaxSize())
                }
                is UiState.Error -> {
                    LineSaleOverviewError(
                        message = s.message,
                        onRetry = { viewModel.refresh() },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                is UiState.Empty -> {
                    LineSaleOverviewEmpty(
                        onRefresh = { viewModel.refresh() },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                is UiState.Success -> {
                    LineSaleOverviewContent(
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
private fun LineSaleOverviewContent(
    breakdown: LineSalePaymentBreakdown,
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
                        title = "Total Line Sales",
                        amount = FormatUtils.formatCurrency(breakdown.totalSales),
                        subtitle = "${breakdown.salesCount} sale${if (breakdown.salesCount == 1L) "" else "s"} today",
                        icon = Icons.AutoMirrored.Filled.TrendingUp,
                        iconTint = MaterialTheme.colorScheme.primary,
                        iconBackground = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Cash Received",
                        amount = FormatUtils.formatCurrency(breakdown.cashReceived),
                        subtitle = "Cash in hand",
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
                        subtitle = "Pending dues",
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

        // Section B: Dedicated Line Sale Allocation Breakup Card
        item {
            VyavsaSectionHeader(
                title = "Sale Allocation Breakup",
                badgeText = if (breakdown.isReconciled) "Reconciled ✓" else null
            )
            Spacer(modifier = Modifier.height(4.dp))
            LineSaleAllocationBreakupCard(breakdown = breakdown)
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
                            text = "No bank payments recorded for today\'s line sales. All receipts were cash or ledger dues.",
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
private fun BankBreakdownCard(
    bank: LineSaleBankBreakdown,
    totalBankReceived: Double,
    modifier: Modifier = Modifier
) {
    val bankAmt = bank.amount.toDoubleOrNull() ?: 0.0
    val bankShare = if (totalBankReceived > 0.0) {
        (bankAmt / totalBankReceived).toFloat().coerceIn(0f, 1f)
    } else 0f

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
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
                            .size(42.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFFEFF6FF))
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalance,
                            contentDescription = null,
                            tint = Blue600,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = bank.bankName.ifBlank { "Bank Account" },
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (!bank.accountNumber.isNullOrBlank()) {
                            Text(
                                text = "A/C: •••• ${bank.accountNumber.takeLast(4)}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = FormatUtils.formatCurrency(bank.amount),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFEFF6FF)
                    ) {
                        Text(
                            text = String.format(Locale.US, "%.1f%% of sales", bank.percentage),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Blue600,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Bank share progress bar
            LinearProgressIndicator(
                progress = { bankShare },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = Blue600,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round
            )
        }
    }
}

@Composable
private fun LineSaleOverviewLoading(modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.padding(24.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 3.dp,
                modifier = Modifier.size(44.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Loading payment breakdown...",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }
}

@Composable
private fun LineSaleOverviewError(
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
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
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
                    text = "Unable to Load Breakdown",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                )
                Spacer(modifier = Modifier.height(18.dp))
                FilledTonalButton(
                    onClick = onRetry,
                    shape = RoundedCornerShape(12.dp)
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
private fun LineSaleOverviewEmpty(
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "No Line Sales Recorded Today",
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
private fun LineSaleAllocationBreakupCard(
    breakdown: LineSalePaymentBreakdown,
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
                            text = "Line Sale Allocation Breakup",
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

            // 1. Total Sale Amount
            BreakupRow(
                label = "Total Sale Amount",
                amount = FormatUtils.formatCurrency(breakdown.totalSales),
                isBold = true,
                labelColor = MaterialTheme.colorScheme.onSurface,
                valueColor = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(8.dp))

            // 2. Adjusted Against Advance
            val advancePct = if (totalSalesVal > 0.0) String.format(Locale.US, "%.1f%%", (advanceVal / totalSalesVal) * 100.0) else null
            BreakupRow(
                label = "Adjusted Against Advance",
                amount = if (advanceVal > 0.0) "-${FormatUtils.formatCurrency(breakdown.advanceApplied)}" else FormatUtils.formatCurrency("0.00"),
                badge = advancePct,
                badgeColor = Amber600,
                badgeBg = Amber50,
                valueColor = if (advanceVal > 0.0) Amber600 else MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 3. Adjusted Against Payable
            val payablePct = if (totalSalesVal > 0.0) String.format(Locale.US, "%.1f%%", (payableVal / totalSalesVal) * 100.0) else null
            BreakupRow(
                label = "Adjusted Against Payable",
                amount = if (payableVal > 0.0) "-${FormatUtils.formatCurrency(breakdown.effectivePayableApplied)}" else FormatUtils.formatCurrency("0.00"),
                badge = payablePct,
                badgeColor = Purple600,
                badgeBg = Purple50,
                valueColor = if (payableVal > 0.0) Purple600 else MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Direct collections if present
            if (cashVal > 0.0) {
                Spacer(modifier = Modifier.height(8.dp))
                BreakupRow(
                    label = "Cash Payment Received",
                    amount = "-${FormatUtils.formatCurrency(breakdown.cashReceived)}",
                    valueColor = Emerald600
                )
            }
            if (bankVal > 0.0) {
                Spacer(modifier = Modifier.height(8.dp))
                BreakupRow(
                    label = "Bank / Online Received",
                    amount = "-${FormatUtils.formatCurrency(breakdown.bankReceived)}",
                    valueColor = Blue600
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(8.dp))

            // 4. Remaining Amount
            BreakupRow(
                label = "Remaining Amount",
                subtitle = if (dueVal > 0.0) "Customer outstanding due" else "Fully settled",
                amount = FormatUtils.formatCurrency(breakdown.totalDue),
                isBold = true,
                valueColor = if (dueVal > 0.0) Crimson600 else Emerald600
            )

            // Informational callout pills for special scenarios
            if (isEntirelyPayable) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Purple50,
                    border = BorderStroke(1.dp, Purple600.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = null,
                            tint = Purple600,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Entire sale amount was adjusted against customer payable (contra settlement).",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Purple600,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
            } else if (isEntirelyAdvance) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Amber50,
                    border = BorderStroke(1.dp, Amber600.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = Amber600,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Entire sale amount was settled from prepaid advance deposit.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Amber600,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BreakupRow(
    label: String,
    amount: String,
    subtitle: String? = null,
    badge: String? = null,
    badgeColor: Color = MaterialTheme.colorScheme.primary,
    badgeBg: Color = MaterialTheme.colorScheme.primaryContainer,
    isBold: Boolean = false,
    labelColor: Color = MaterialTheme.colorScheme.onSurface,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = label,
                    style = if (isBold) MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = labelColor
                    ) else MaterialTheme.typography.bodyMedium.copy(
                        color = labelColor
                    )
                )
                if (badge != null) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = badgeBg
                    ) {
                        Text(
                            text = badge,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = badgeColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            ),
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                }
            }
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }

        Text(
            text = amount,
            style = MaterialTheme.typography.titleSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = if (isBold) FontWeight.ExtraBold else FontWeight.SemiBold,
                color = valueColor
            )
        )
    }
}
