@file:Suppress("DEPRECATION")
package com.vyavsa.app.feature.transactions

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CallMade
import androidx.compose.material.icons.filled.CallReceived
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vyavsa.app.core.common.FormatUtils
import com.vyavsa.app.core.common.UiState
import com.vyavsa.app.core.designsystem.Crimson50
import com.vyavsa.app.core.designsystem.Crimson600
import com.vyavsa.app.core.designsystem.Emerald50
import com.vyavsa.app.core.designsystem.Emerald600
import com.vyavsa.app.core.designsystem.EmptyStateView
import com.vyavsa.app.core.designsystem.ErrorBanner
import com.vyavsa.app.core.designsystem.StatBadge
import com.vyavsa.app.core.designsystem.VyavsaSearchBar
import com.vyavsa.app.core.designsystem.VyavsaSectionHeader
import com.vyavsa.app.core.designsystem.VyavsaSegmentedTabs
import com.vyavsa.app.core.designsystem.VyavsaTopAppBar
import com.vyavsa.app.core.model.CounterSale
import com.vyavsa.app.core.model.Expense
import com.vyavsa.app.core.model.LineSale

@Composable
fun TransactionsScreen(
    viewModel: TransactionsViewModel,
    onNavigateToCreateSale: () -> Unit,
    onNavigateToCreateExpense: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val lineSalesState by viewModel.lineSalesState.collectAsState()
    val counterSalesState by viewModel.counterSalesState.collectAsState()
    val expensesState by viewModel.expensesState.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            VyavsaTopAppBar(
                title = "Business Ledger",
                subtitle = "Daily sales inflows & operating expenses"
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    if (selectedTab == TransactionTab.SALES) onNavigateToCreateSale()
                    else onNavigateToCreateExpense()
                },
                shape = RoundedCornerShape(20.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                icon = { Icon(imageVector = Icons.Default.Add, contentDescription = null) },
                text = {
                    Text(
                        text = if (selectedTab == TransactionTab.SALES) "New Sale" else "New Expense",
                        fontWeight = FontWeight.Bold
                    )
                }
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
            // Pill Segmented Tabs: Sales vs Expenses
            VyavsaSegmentedTabs(
                tabs = listOf("Sales & Inflow", "Expenses & Outflow"),
                selectedIndex = selectedTab.ordinal,
                onTabSelected = { idx ->
                    viewModel.selectTab(if (idx == 0) TransactionTab.SALES else TransactionTab.EXPENSES)
                },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )

            // Modern Pill Search Bar
            VyavsaSearchBar(
                query = searchQuery,
                onQueryChange = { viewModel.onSearchChange(it) },
                placeholder = "Search transactions, customer, item...",
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )

            // Content List based on Tab
            when (selectedTab) {
                TransactionTab.SALES -> {
                    SalesListContent(
                        lineSalesState = lineSalesState,
                        counterSalesState = counterSalesState,
                        onAddSale = onNavigateToCreateSale
                    )
                }
                TransactionTab.EXPENSES -> {
                    ExpensesListContent(
                        expensesState = expensesState,
                        onAddExpense = onNavigateToCreateExpense
                    )
                }
            }
        }
    }
}

@Composable
private fun SalesListContent(
    lineSalesState: UiState<List<LineSale>>,
    counterSalesState: UiState<List<CounterSale>>,
    onAddSale: () -> Unit
) {
    if (lineSalesState is UiState.Loading && counterSalesState is UiState.Loading) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    val lineSales = (lineSalesState as? UiState.Success)?.data ?: emptyList()
    val counterSales = (counterSalesState as? UiState.Success)?.data ?: emptyList()

    if (lineSales.isEmpty() && counterSales.isEmpty()) {
        EmptyStateView(
            title = "No Sales Recorded",
            description = "Start recording your field line sales or counter sales to track daily income",
            icon = Icons.Default.ReceiptLong,
            actionText = "Record First Sale",
            onActionClick = onAddSale
        )
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        if (lineSales.isNotEmpty()) {
            item {
                VyavsaSectionHeader(
                    title = "Line / Field Sales",
                    badgeText = "${lineSales.size}",
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            items(lineSales) { sale ->
                LineSaleCard(sale)
            }
        }

        if (counterSales.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                VyavsaSectionHeader(
                    title = "Counter / POS Sales",
                    badgeText = "${counterSales.size}",
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            items(counterSales) { sale ->
                CounterSaleCard(sale)
            }
        }
    }
}

@Composable
private fun LineSaleCard(sale: LineSale) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f), RoundedCornerShape(22.dp))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.padding(16.dp)
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
                        .background(Emerald50)
                ) {
                    Icon(
                        imageVector = Icons.Default.CallReceived,
                        contentDescription = null,
                        tint = Emerald600,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = sale.customerName.ifBlank { "Line Customer" },
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (!sale.route.isNullOrBlank()) "Route: ${sale.route}" else FormatUtils.formatDateForDisplay(sale.createdAt),
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "+${FormatUtils.formatCurrency(sale.totalAmount)}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Emerald600
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (sale.balance.toDoubleOrNull() ?: 0.0 > 0.0) {
                    StatBadge(
                        text = "Due: ${FormatUtils.formatCurrency(sale.balance)}",
                        backgroundColor = Crimson50,
                        textColor = Crimson600
                    )
                } else {
                    StatBadge(text = "Paid")
                }
            }
        }
    }
}

@Composable
private fun CounterSaleCard(sale: CounterSale) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f), RoundedCornerShape(22.dp))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.padding(16.dp)
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
                        .background(Emerald50)
                ) {
                    Icon(
                        imageVector = Icons.Default.CallReceived,
                        contentDescription = null,
                        tint = Emerald600,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = sale.item.ifBlank { "Store Sale" },
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Paid via ${sale.paymentMethod.uppercase()}",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "+${FormatUtils.formatCurrency(sale.totalAmount)}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Emerald600
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = FormatUtils.formatDateForDisplay(sale.createdAt),
                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
            }
        }
    }
}

@Composable
private fun ExpensesListContent(
    expensesState: UiState<List<Expense>>,
    onAddExpense: () -> Unit
) {
    when (expensesState) {
        is UiState.Loading -> {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }
        is UiState.Empty -> {
            EmptyStateView(
                title = "No Expenses Logged",
                description = "Record operating expenses like rent, fuel, stock and petty cash",
                icon = Icons.Default.ReceiptLong,
                actionText = "Add Expense",
                onActionClick = onAddExpense
            )
        }
        is UiState.Error -> {
            Box(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                ErrorBanner(message = expensesState.message)
            }
        }
        is UiState.Success -> {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(expensesState.data) { expense ->
                    ExpenseCard(expense)
                }
            }
        }
        else -> {}
    }
}

@Composable
private fun ExpenseCard(expense: Expense) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f), RoundedCornerShape(22.dp))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.padding(16.dp)
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
                        .background(Crimson50)
                ) {
                    Icon(
                        imageVector = Icons.Default.CallMade,
                        contentDescription = null,
                        tint = Crimson600,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = expense.item,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (!expense.category.isNullOrBlank()) "${expense.category} • ${expense.paymentMethod}" else expense.paymentMethod,
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "-${FormatUtils.formatCurrency(if (expense.totalAmount != "0.00") expense.totalAmount else expense.amount)}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Crimson600
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = FormatUtils.formatDateForDisplay(expense.createdAt),
                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
            }
        }
    }
}
