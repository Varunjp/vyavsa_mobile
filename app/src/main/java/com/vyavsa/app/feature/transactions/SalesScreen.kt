@file:Suppress("DEPRECATION")
package com.vyavsa.app.feature.transactions

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CallReceived
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vyavsa.app.core.common.FormatUtils
import com.vyavsa.app.core.common.UiState
import com.vyavsa.app.core.designsystem.Crimson50
import com.vyavsa.app.core.designsystem.Crimson600
import com.vyavsa.app.core.designsystem.Emerald50
import com.vyavsa.app.core.designsystem.Emerald600
import com.vyavsa.app.core.designsystem.EmptyStateView
import com.vyavsa.app.core.designsystem.StatBadge
import com.vyavsa.app.core.designsystem.VyavsaSearchBar
import com.vyavsa.app.core.designsystem.VyavsaSegmentedTabs
import com.vyavsa.app.core.designsystem.VyavsaTopAppBar
import com.vyavsa.app.core.designsystem.vyavsaClickable
import com.vyavsa.app.core.model.CounterSale
import com.vyavsa.app.core.model.LineSale
import com.vyavsa.app.core.printer.PrintJobStatus
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SalesScreen(
    viewModel: TransactionsViewModel,
    onNavigateToCreateSale: () -> Unit,
    onNavigateToSaleDetails: (saleType: String, saleId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val allGranted = grants.values.all { it }
        if (!allGranted) {
            Toast.makeText(context, "Bluetooth permissions are required for printing.", Toast.LENGTH_SHORT).show()
        }
    }

    val searchQuery by viewModel.searchQuery.collectAsState()
    val dateFilter by viewModel.dateFilter.collectAsState()
    val lineSalesState by viewModel.lineSalesState.collectAsState()
    val counterSalesState by viewModel.counterSalesState.collectAsState()

    val activePrintingSaleId by viewModel.activePrintingSaleId.collectAsState()
    val activeSharingSaleId by viewModel.activeSharingSaleId.collectAsState()
    val printJobState by viewModel.printJobState.collectAsState()

    LaunchedEffect(printJobState) {
        when (printJobState.status) {
            PrintJobStatus.SUCCESS -> {
                snackbarHostState.showSnackbar(printJobState.message ?: "Receipt printed successfully")
            }
            PrintJobStatus.ERROR -> {
                snackbarHostState.showSnackbar(printJobState.message ?: "Print failed. Check printer connection.")
            }
            else -> {}
        }
    }

    val isSalesRole = viewModel.tokenManager?.isSales() == true
    val isOfficeRole = viewModel.tokenManager?.isOffice() == true

    var selectedSaleFilterIndex by remember(isSalesRole, isOfficeRole) { mutableIntStateOf(0) }
    val filterTabs = remember(isSalesRole, isOfficeRole) {
        when {
            isSalesRole -> listOf("Line Sales")
            isOfficeRole -> listOf("Counter Sales")
            else -> listOf("All Sales", "Line Sales", "Counter")
        }
    }

    val todayDate = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    val screenTitle = when {
        isSalesRole -> "Line Sales"
        isOfficeRole -> "Counter Sales"
        else -> "Sales"
    }

    val screenSubtitle = when {
        isSalesRole -> "Route deliveries & order billing"
        isOfficeRole -> "Over-the-counter retail bills"
        else -> "Field line sales, POS counter billing & invoices"
    }

    val fabText = when {
        isSalesRole -> "New Line Sale"
        isOfficeRole -> "New Counter Sale"
        else -> "New Sale"
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            VyavsaTopAppBar(
                title = screenTitle,
                subtitle = screenSubtitle
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToCreateSale,
                icon = { Icon(Icons.Default.Add, contentDescription = fabText) },
                text = { Text(fabText, fontWeight = FontWeight.Bold) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Header Controls: Search bar & Date Filter
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                VyavsaSearchBar(
                    query = searchQuery,
                    onQueryChange = { viewModel.onSearchChange(it) },
                    placeholder = "Search customer, route, invoice #..."
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (filterTabs.size > 1) {
                        VyavsaSegmentedTabs(
                            tabs = filterTabs,
                            selectedIndex = selectedSaleFilterIndex,
                            onTabSelected = { selectedSaleFilterIndex = it },
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                    }

                    FilterChip(
                        selected = dateFilter == todayDate,
                        onClick = {
                            if (dateFilter == todayDate) {
                                viewModel.setDateFilter(null)
                            } else {
                                viewModel.setDateFilter(todayDate)
                            }
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        label = {
                            Text(
                                text = "Today",
                                fontWeight = if (dateFilter == todayDate) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            // Sales List
            val lineSales = (lineSalesState as? UiState.Success)?.data ?: emptyList()
            val counterSales = (counterSalesState as? UiState.Success)?.data ?: emptyList()

            val showLine = when {
                isSalesRole -> true
                isOfficeRole -> false
                else -> selectedSaleFilterIndex == 0 || selectedSaleFilterIndex == 1
            }
            val showCounter = when {
                isSalesRole -> false
                isOfficeRole -> true
                else -> selectedSaleFilterIndex == 0 || selectedSaleFilterIndex == 2
            }

            val filteredLineSales = if (showLine) lineSales else emptyList()
            val filteredCounterSales = if (showCounter) counterSales else emptyList()

            val isLoading = (lineSalesState is UiState.Loading && counterSalesState is UiState.Loading)

            if (isLoading) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (filteredLineSales.isEmpty() && filteredCounterSales.isEmpty()) {
                EmptyStateView(
                    title = "No Sales Found",
                    description = if (searchQuery.isNotBlank() || dateFilter != null) {
                        "No sales matched the specified filters"
                    } else {
                        "Record your first line or counter sale to generate receipts and invoices"
                    },
                    icon = Icons.Default.ReceiptLong,
                    actionText = "Record Sale",
                    onActionClick = onNavigateToCreateSale
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Line Sales
                    if (filteredLineSales.isNotEmpty()) {
                        items(
                            items = filteredLineSales,
                            key = { "line_${it.id}" }
                        ) { sale ->
                            SaleItemCard(
                                title = sale.customerName.ifBlank { "Line Customer" },
                                subtitle = if (!sale.route.isNullOrBlank()) "Route: ${sale.route} • ${FormatUtils.formatDateForDisplay(sale.createdAt)}"
                                else FormatUtils.formatDateForDisplay(sale.createdAt),
                                invoiceNumber = sale.id.take(8).uppercase(),
                                totalAmount = sale.totalAmount,
                                discount = sale.discount,
                                balance = sale.balance,
                                isCounter = false,
                                isPrinting = activePrintingSaleId == sale.id,
                                isSharing = activeSharingSaleId == sale.id,
                                onCardClick = { onNavigateToSaleDetails("line", sale.id) },
                                onPrintClick = {
                                    viewModel.printLineSale(sale, context) {
                                        permissionLauncher.launch(it)
                                    }
                                },
                                onShareClick = {
                                    viewModel.shareLineSalePdf(context, sale) { success, err ->
                                        if (!success && err != null) {
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar(err)
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }

                    // Counter Sales
                    if (filteredCounterSales.isNotEmpty()) {
                        items(
                            items = filteredCounterSales,
                            key = { "counter_${it.id}" }
                        ) { sale ->
                            SaleItemCard(
                                title = sale.item.ifBlank { "Counter Sale" },
                                subtitle = "POS Bill • ${FormatUtils.formatDateForDisplay(sale.createdAt)}",
                                invoiceNumber = sale.id.take(8).uppercase(),
                                totalAmount = sale.totalAmount,
                                discount = sale.discount,
                                balance = sale.account,
                                isCounter = true,
                                isPrinting = activePrintingSaleId == sale.id,
                                isSharing = activeSharingSaleId == sale.id,
                                onCardClick = { onNavigateToSaleDetails("counter", sale.id) },
                                onPrintClick = {
                                    viewModel.printCounterSale(sale, context) {
                                        permissionLauncher.launch(it)
                                    }
                                },
                                onShareClick = {
                                    viewModel.shareCounterSalePdf(context, sale) { success, err ->
                                        if (!success && err != null) {
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar(err)
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SaleItemCard(
    title: String,
    subtitle: String,
    invoiceNumber: String,
    totalAmount: String,
    discount: String,
    balance: String,
    isCounter: Boolean,
    isPrinting: Boolean,
    isSharing: Boolean,
    onCardClick: () -> Unit,
    onPrintClick: () -> Unit,
    onShareClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .vyavsaClickable { onCardClick() }
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f), RoundedCornerShape(22.dp))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Main Card Info Header
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
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isCounter) MaterialTheme.colorScheme.secondaryContainer else Emerald50)
                    ) {
                        Icon(
                            imageVector = if (isCounter) Icons.Default.Storefront else Icons.Default.CallReceived,
                            contentDescription = null,
                            tint = if (isCounter) MaterialTheme.colorScheme.onSecondaryContainer else Emerald600,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "#$invoiceNumber • $subtitle",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Amount and status badge
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "+${FormatUtils.formatCurrency(totalAmount)}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = Emerald600
                        )
                    )
                    val balVal = balance.toDoubleOrNull() ?: 0.0
                    val discVal = discount.toDoubleOrNull() ?: 0.0
                    if (discVal > 0.0) {
                        Text(
                            text = "Disc: -${FormatUtils.formatCurrency(discount)}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    if (balVal > 0.0) {
                        StatBadge(
                            text = "Due: ${FormatUtils.formatCurrency(balance)}",
                            backgroundColor = Crimson50,
                            textColor = Crimson600
                        )
                    } else {
                        StatBadge(text = "Paid")
                    }
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                thickness = 0.8.dp
            )

            // One-Tap Quick Actions: Print and PDF Share
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // One-Tap Print Button
                FilledTonalButton(
                    onClick = onPrintClick,
                    enabled = !isPrinting && !isSharing,
                    shape = CircleShape,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 44.dp)
                ) {
                    if (isPrinting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Printing...", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(
                            imageVector = Icons.Default.Print,
                            contentDescription = "Print Invoice",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Print", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // One-Tap Share PDF Button
                OutlinedButton(
                    onClick = onShareClick,
                    enabled = !isPrinting && !isSharing,
                    shape = CircleShape,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 44.dp)
                ) {
                    if (isSharing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Sharing...", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share PDF",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share PDF", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
