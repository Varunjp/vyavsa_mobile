@file:Suppress("DEPRECATION")
package com.vyavsa.app.feature.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import com.vyavsa.app.core.designsystem.VyavsaSearchBar
import com.vyavsa.app.core.designsystem.VyavsaSectionHeader
import com.vyavsa.app.core.designsystem.VyavsaSegmentedTabs
import com.vyavsa.app.core.designsystem.VyavsaTopAppBar
import com.vyavsa.app.core.model.CashTransaction
import com.vyavsa.app.core.model.CounterSale
import com.vyavsa.app.core.model.Expense
import com.vyavsa.app.core.model.LineSale
import com.vyavsa.app.core.model.PlanTransaction
import com.vyavsa.app.core.model.Purchase

@Composable
fun TransactionsScreen(
    viewModel: TransactionsViewModel,
    onNavigateToCreateSale: () -> Unit,
    onNavigateToCreateExpense: () -> Unit,
    onNavigateToSaleDetails: (saleType: String, saleId: String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val lineSalesState by viewModel.lineSalesState.collectAsState()
    val counterSalesState by viewModel.counterSalesState.collectAsState()
    val purchasesState by viewModel.purchasesState.collectAsState()
    val expensesState by viewModel.expensesState.collectAsState()
    val cashState by viewModel.cashTransactionsState.collectAsState()
    val billingState by viewModel.billingTransactionsState.collectAsState()

    val availableTabs = if (viewModel.isAdmin) {
        listOf(TransactionTab.SALES, TransactionTab.PURCHASES, TransactionTab.EXPENSES, TransactionTab.CASH_BOOK, TransactionTab.BILLING)
    } else {
        listOf(TransactionTab.SALES, TransactionTab.PURCHASES, TransactionTab.EXPENSES, TransactionTab.CASH_BOOK)
    }

    val tabTitles = availableTabs.map {
        when (it) {
            TransactionTab.SALES -> "Sales"
            TransactionTab.PURCHASES -> "Purchases"
            TransactionTab.EXPENSES -> "Expenses"
            TransactionTab.CASH_BOOK -> "Cash Book"
            TransactionTab.BILLING -> "Billing"
        }
    }

    val selectedTabIndex = availableTabs.indexOf(selectedTab).coerceAtLeast(0)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            VyavsaTopAppBar(
                title = "Business Ledger",
                subtitle = "Complete financial transactions & operational records"
            )
        },
        floatingActionButton = {
            if (selectedTab == TransactionTab.SALES || selectedTab == TransactionTab.EXPENSES) {
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
            }
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Segmented Tabs
            VyavsaSegmentedTabs(
                tabs = tabTitles,
                selectedIndex = selectedTabIndex,
                onTabSelected = { idx ->
                    if (idx in availableTabs.indices) {
                        viewModel.selectTab(availableTabs[idx])
                    }
                },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )

            // Search Bar
            VyavsaSearchBar(
                query = searchQuery,
                onQueryChange = { viewModel.onSearchChange(it) },
                placeholder = when (selectedTab) {
                    TransactionTab.SALES -> "Search sales by customer or item..."
                    TransactionTab.PURCHASES -> "Search purchases by item or supplier..."
                    TransactionTab.EXPENSES -> "Search expenses by item or category..."
                    TransactionTab.CASH_BOOK -> "Search cash book transactions..."
                    TransactionTab.BILLING -> "Search billing history..."
                },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )

            // Content List based on Selected Tab
            when (selectedTab) {
                TransactionTab.SALES -> {
                    SalesListContent(
                        lineSalesState = lineSalesState,
                        counterSalesState = counterSalesState,
                        onSaleClick = onNavigateToSaleDetails,
                        onAddSale = onNavigateToCreateSale
                    )
                }
                TransactionTab.PURCHASES -> {
                    PurchasesListContent(
                        purchasesState = purchasesState
                    )
                }
                TransactionTab.EXPENSES -> {
                    ExpensesListContent(
                        expensesState = expensesState,
                        onAddExpense = onNavigateToCreateExpense
                    )
                }
                TransactionTab.CASH_BOOK -> {
                    CashBookListContent(
                        cashState = cashState
                    )
                }
                TransactionTab.BILLING -> {
                    BillingListContent(
                        billingState = billingState
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
    onSaleClick: (saleType: String, saleId: String) -> Unit,
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
                    title = "Line / Field Sales (Tap for details)",
                    badgeText = "${lineSales.size}",
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            items(lineSales) { sale ->
                LineSaleCard(
                    sale = sale,
                    onClick = { onSaleClick("line", sale.id) }
                )
            }
        }

        if (counterSales.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                VyavsaSectionHeader(
                    title = "Counter / POS Sales (Tap for details)",
                    badgeText = "${counterSales.size}",
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            items(counterSales) { sale ->
                CounterSaleCard(
                    sale = sale,
                    onClick = { onSaleClick("counter", sale.id) }
                )
            }
        }
    }
}

@Composable
private fun LineSaleCard(
    sale: LineSale,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f), RoundedCornerShape(22.dp))
            .clickable { onClick() }
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
                val bal = sale.balance.toDoubleOrNull() ?: 0.0
                if (bal > 0.0) {
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
private fun CounterSaleCard(
    sale: CounterSale,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f), RoundedCornerShape(22.dp))
            .clickable { onClick() }
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
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = null,
                        tint = Emerald600,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = sale.item.ifBlank { "Counter Sale" },
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = FormatUtils.formatDateForDisplay(sale.createdAt),
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
                StatBadge(
                    text = sale.paymentMethod.uppercase(),
                    backgroundColor = Blue50,
                    textColor = Blue600
                )
            }
        }
    }
}

@Composable
private fun PurchasesListContent(
    purchasesState: UiState<List<Purchase>>
) {
    when (purchasesState) {
        is UiState.Loading -> {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }
        is UiState.Empty -> {
            EmptyStateView(
                title = "No Purchases Recorded",
                description = "Inventory stock intakes and supplier purchases will appear here",
                icon = Icons.Default.ShoppingCart
            )
        }
        is UiState.Error -> {
            Box(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                ErrorBanner(message = purchasesState.message)
            }
        }
        is UiState.Success -> {
            val purchases = purchasesState.data
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    VyavsaSectionHeader(
                        title = "Inventory Purchases & Procurement",
                        badgeText = "${purchases.size}",
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                items(purchases) { purchase ->
                    PurchaseCard(purchase)
                }
            }
        }
        else -> {}
    }
}

@Composable
private fun PurchaseCard(purchase: Purchase) {
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
                        .background(Amber50)
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = null,
                        tint = Amber600,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = purchase.item,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (!purchase.customerName.isNullOrBlank()) "Supplier: ${purchase.customerName} • Qty: ${purchase.quantity}" else "Qty: ${purchase.quantity}",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = FormatUtils.formatCurrency(purchase.totalAmount),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                val isPaid = purchase.paymentStatus.equals("PAID", ignoreCase = true)
                StatBadge(
                    text = purchase.paymentStatus.uppercase(),
                    backgroundColor = if (isPaid) Emerald50 else Crimson50,
                    textColor = if (isPaid) Emerald600 else Crimson600
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
                title = "No Expenses Recorded",
                description = "Record staff advances, utility bills, fuel, and shop maintenance",
                icon = Icons.Default.CallMade,
                actionText = "Record First Expense",
                onActionClick = onAddExpense
            )
        }
        is UiState.Error -> {
            Box(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                ErrorBanner(message = expensesState.message)
            }
        }
        is UiState.Success -> {
            val expenses = expensesState.data
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    VyavsaSectionHeader(
                        title = "Operational Outflow & Expenses",
                        badgeText = "${expenses.size}",
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                items(expenses) { expense ->
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
                        text = expense.item.ifBlank { "Expense" },
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (!expense.employeeName.isNullOrBlank()) "Staff: ${expense.employeeName}" else if (!expense.category.isNullOrBlank()) expense.category.replace('_', ' ').uppercase() else FormatUtils.formatDateForDisplay(expense.createdAt),
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                val amt = if (expense.totalAmount != "0.00") expense.totalAmount else expense.amount
                Text(
                    text = "-${FormatUtils.formatCurrency(amt)}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Crimson600
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                StatBadge(
                    text = expense.paymentMethod.uppercase(),
                    backgroundColor = Blue50,
                    textColor = Blue600
                )
            }
        }
    }
}

@Composable
private fun CashBookListContent(
    cashState: UiState<List<CashTransaction>>
) {
    when (cashState) {
        is UiState.Loading -> {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }
        is UiState.Empty -> {
            EmptyStateView(
                title = "No Cash Drawer Logs",
                description = "Cash till additions, adjustments and tallies will appear here",
                icon = Icons.Default.Payments
            )
        }
        is UiState.Error -> {
            Box(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                ErrorBanner(message = cashState.message)
            }
        }
        is UiState.Success -> {
            val transactions = cashState.data
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    VyavsaSectionHeader(
                        title = "Cash Till Ledger Entries",
                        badgeText = "${transactions.size}",
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                items(transactions) { txn ->
                    CashTransactionCard(txn)
                }
            }
        }
        else -> {}
    }
}

@Composable
private fun CashTransactionCard(txn: CashTransaction) {
    val isAdd = txn.transactionType == "add_cash"
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
                        .background(if (isAdd) Emerald50 else Blue50)
                ) {
                    Icon(
                        imageVector = if (isAdd) Icons.Default.CallReceived else Icons.Default.Payments,
                        contentDescription = null,
                        tint = if (isAdd) Emerald600 else Blue600,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = if (isAdd) "Cash Added to Till" else "Balance Adjustment",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = txn.description?.ifBlank { null } ?: "Tally on ${FormatUtils.formatDateForDisplay(txn.transactionDate.ifBlank { txn.createdAt })}",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${if (isAdd) "+" else ""}${FormatUtils.formatCurrency(txn.amount)}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isAdd) Emerald600 else MaterialTheme.colorScheme.onSurface
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "New: ${FormatUtils.formatCurrency(txn.newBalance)}",
                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
            }
        }
    }
}

@Composable
private fun BillingListContent(
    billingState: UiState<List<PlanTransaction>>
) {
    when (billingState) {
        is UiState.Loading -> {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }
        is UiState.Empty -> {
            EmptyStateView(
                title = "No Billing Transactions",
                description = "Audit records of plan subscriptions and platform renewals",
                icon = Icons.Default.ReceiptLong
            )
        }
        is UiState.Error -> {
            Box(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                ErrorBanner(message = billingState.message)
            }
        }
        is UiState.Success -> {
            val transactions = billingState.data
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    VyavsaSectionHeader(
                        title = "Plan Purchase Ledger",
                        badgeText = "${transactions.size}",
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                items(transactions) { txn ->
                    PlanTransactionCard(txn)
                }
            }
        }
        else -> {}
    }
}

@Composable
private fun PlanTransactionCard(txn: PlanTransaction) {
    val isCompleted = txn.status.equals("completed", ignoreCase = true)
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = txn.planName.ifBlank { "Standard Plan" },
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Txn: ${txn.transactionId.take(12)}... • ${txn.paymentMethod.uppercase()}",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
                if (!txn.failureReason.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Reason: ${txn.failureReason}",
                        style = MaterialTheme.typography.labelSmall.copy(color = Crimson600)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = FormatUtils.formatCurrency(txn.amount),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                StatBadge(
                    text = txn.status.uppercase(),
                    backgroundColor = if (isCompleted) Emerald50 else Crimson50,
                    textColor = if (isCompleted) Emerald600 else Crimson600
                )
            }
        }
    }
}
