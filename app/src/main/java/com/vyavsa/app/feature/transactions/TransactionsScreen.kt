@file:Suppress("DEPRECATION")
package com.vyavsa.app.feature.transactions

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CallMade
import androidx.compose.material.icons.filled.CallReceived
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import com.vyavsa.app.core.designsystem.vyavsaClickable
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
    onNavigateToCreatePurchase: () -> Unit = {},
    onNavigateToPurchaseDetails: (purchaseId: String) -> Unit = {},
    onNavigateToSaleDetails: (saleType: String, saleId: String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val dateFilter by viewModel.dateFilter.collectAsState()
    val lineSalesState by viewModel.lineSalesState.collectAsState()
    val counterSalesState by viewModel.counterSalesState.collectAsState()
    val purchasesState by viewModel.purchasesState.collectAsState()
    val expensesState by viewModel.expensesState.collectAsState()
    val cashState by viewModel.cashTransactionsState.collectAsState()
    val billingState by viewModel.billingTransactionsState.collectAsState()

    val isAdmin = viewModel.isAdmin
    val availableTabs = remember(isAdmin) {
        if (isAdmin) {
            listOf(
                TransactionTab.SALES,
                TransactionTab.PURCHASES,
                TransactionTab.EXPENSES,
                TransactionTab.CASH_BOOK,
                TransactionTab.BILLING
            )
        } else {
            listOf(
                TransactionTab.SALES,
                TransactionTab.PURCHASES,
                TransactionTab.EXPENSES,
                TransactionTab.CASH_BOOK
            )
        }
    }

    val tabTitles = remember(availableTabs) {
        availableTabs.map {
            when (it) {
                TransactionTab.SALES -> "Sales"
                TransactionTab.PURCHASES -> "Purchases"
                TransactionTab.EXPENSES -> "Expenses"
                TransactionTab.CASH_BOOK -> "Cash Book"
                TransactionTab.BILLING -> "Billing"
            }
        }
    }

    val selectedTabIndex = availableTabs.indexOf(selectedTab).coerceAtLeast(0)
    val todayDate = remember { FormatUtils.todayDateString() }
    val showFab = selectedTab == TransactionTab.SALES ||
        selectedTab == TransactionTab.PURCHASES ||
        selectedTab == TransactionTab.EXPENSES

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            VyavsaTopAppBar(
                title = "Business Ledger",
                subtitle = "Complete financial transactions & operational records"
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = showFab,
                enter = scaleIn(tween(200)) + fadeIn(tween(180)),
                exit = scaleOut(tween(160)) + fadeOut(tween(150))
            ) {
                ExtendedFloatingActionButton(
                    onClick = {
                        when (selectedTab) {
                            TransactionTab.SALES -> onNavigateToCreateSale()
                            TransactionTab.PURCHASES -> onNavigateToCreatePurchase()
                            else -> onNavigateToCreateExpense()
                        }
                    },
                    shape = RoundedCornerShape(20.dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    icon = { Icon(imageVector = Icons.Default.Add, contentDescription = null) },
                    text = {
                        Text(
                            text = when (selectedTab) {
                                TransactionTab.SALES -> "New Sale"
                                TransactionTab.PURCHASES -> "New Purchase"
                                else -> "New Expense"
                            },
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
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )

            // Search Bar & Quick Filters (Aligned for filterable tabs)
            AnimatedVisibility(
                visible = selectedTab != TransactionTab.CASH_BOOK && selectedTab != TransactionTab.BILLING
            ) {
                Column {
                    VyavsaSearchBar(
                        query = searchQuery,
                        onQueryChange = { viewModel.onSearchChange(it) },
                        placeholder = when (selectedTab) {
                            TransactionTab.SALES -> "Search sales by customer, route or item..."
                            TransactionTab.PURCHASES -> "Search purchases by item or supplier..."
                            TransactionTab.EXPENSES -> "Search expenses by item or category..."
                            TransactionTab.CASH_BOOK -> "Search cash book transactions..."
                            TransactionTab.BILLING -> "Search billing history..."
                        },
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = dateFilter == null,
                            onClick = { viewModel.setDateFilter(null) },
                            shape = CircleShape,
                            label = {
                                Text(
                                    text = "All Dates",
                                    fontWeight = if (dateFilter == null) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                        FilterChip(
                            selected = dateFilter == todayDate,
                            onClick = { viewModel.setDateFilter(if (dateFilter == todayDate) null else todayDate) },
                            shape = CircleShape,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = "Today Only",
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
            }

            // Directional Animated Content based on Selected Tab
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    val direction = if (targetState.ordinal >= initialState.ordinal) 1 else -1
                    (fadeIn(tween(220, easing = FastOutSlowInEasing)) +
                        slideInHorizontally(
                            initialOffsetX = { (it * 0.08f * direction).toInt() },
                            animationSpec = tween(240, easing = FastOutSlowInEasing)
                        )) togetherWith
                        (fadeOut(tween(160, easing = FastOutSlowInEasing)) +
                            slideOutHorizontally(
                                targetOffsetX = { (-it * 0.06f * direction).toInt() },
                                animationSpec = tween(180, easing = FastOutSlowInEasing)
                            ))
                },
                label = "LedgerSubTabTransition",
                modifier = Modifier.fillMaxSize()
            ) { currentTab ->
                when (currentTab) {
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
                            purchasesState = purchasesState,
                            onAddPurchase = onNavigateToCreatePurchase,
                            onPurchaseClick = onNavigateToPurchaseDetails
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
}

@OptIn(ExperimentalFoundationApi::class)
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
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        if (lineSales.isNotEmpty()) {
            item(key = "header_line_sales") {
                VyavsaSectionHeader(
                    title = "Line / Field Sales",
                    badgeText = "${lineSales.size}",
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }

            items(
                items = lineSales,
                key = { "line_${it.id}" },
                contentType = { "line_sale" }
            ) { sale ->
                LineSaleCard(
                    sale = sale,
                    onClick = { onSaleClick("line", sale.id) },
                    modifier = Modifier.animateItemPlacement()
                )
            }
        }

        if (counterSales.isNotEmpty()) {
            item(key = "header_counter_sales") {
                Spacer(modifier = Modifier.height(6.dp))
                VyavsaSectionHeader(
                    title = "Counter / POS Sales",
                    badgeText = "${counterSales.size}",
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }

            items(
                items = counterSales,
                key = { "counter_${it.id}" },
                contentType = { "counter_sale" }
            ) { sale ->
                CounterSaleCard(
                    sale = sale,
                    onClick = { onSaleClick("counter", sale.id) },
                    modifier = Modifier.animateItemPlacement()
                )
            }
        }
    }
}

@Composable
private fun LineSaleCard(
    sale: LineSale,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
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
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (!sale.route.isNullOrBlank()) "Route: ${sale.route} • ${FormatUtils.formatDateForDisplay(sale.createdAt)}"
                        else FormatUtils.formatDateForDisplay(sale.createdAt),
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

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
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
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
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = FormatUtils.formatDateForDisplay(sale.createdAt),
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PurchasesListContent(
    purchasesState: UiState<List<Purchase>>,
    onAddPurchase: () -> Unit = {},
    onPurchaseClick: (purchaseId: String) -> Unit = {}
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
                icon = Icons.Default.ShoppingCart,
                actionText = "Record First Purchase",
                onActionClick = onAddPurchase
            )
        }
        is UiState.Error -> {
            Box(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                ErrorBanner(message = purchasesState.message)
            }
        }
        is UiState.Success -> {
            val purchases = purchasesState.data
            if (purchases.isEmpty()) {
                EmptyStateView(
                    title = "No Purchases Recorded",
                    description = "Inventory stock intakes and supplier purchases will appear here",
                    icon = Icons.Default.ShoppingCart,
                    actionText = "Record First Purchase",
                    onActionClick = onAddPurchase
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item(key = "header_purchases") {
                        VyavsaSectionHeader(
                            title = "Inventory Purchases & Procurement",
                            badgeText = "${purchases.size}",
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }
                    items(
                        items = purchases,
                        key = { it.id },
                        contentType = { "purchase" }
                    ) { purchase ->
                        PurchaseCard(
                            purchase = purchase,
                            onClick = { onPurchaseClick(purchase.id) },
                            modifier = Modifier.animateItemPlacement()
                        )
                    }
                }
            }
        }
        else -> {}
    }
}

@Composable
private fun PurchaseCard(
    purchase: Purchase,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
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
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (!purchase.customerName.isNullOrBlank()) "Supplier: ${purchase.customerName} • Qty: ${purchase.quantity}" else "Qty: ${purchase.quantity}",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val outstanding = purchase.outstandingAmount.toDoubleOrNull() ?: 0.0
                    if (outstanding > 0.0) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Due: ${FormatUtils.formatCurrency(purchase.outstandingAmount)}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Crimson600,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

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
                val isPartial = purchase.paymentStatus.equals("PARTIAL", ignoreCase = true)
                StatBadge(
                    text = purchase.paymentStatus.uppercase(),
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

@OptIn(ExperimentalFoundationApi::class)
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
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item(key = "header_expenses") {
                    VyavsaSectionHeader(
                        title = "Operational Outflow & Expenses",
                        badgeText = "${expenses.size}",
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
                items(
                    items = expenses,
                    key = { it.id },
                    contentType = { "expense" }
                ) { expense ->
                    ExpenseCard(
                        expense = expense,
                        modifier = Modifier.animateItemPlacement()
                    )
                }
            }
        }
        else -> {}
    }
}

@Composable
private fun ExpenseCard(
    expense: Expense,
    modifier: Modifier = Modifier
) {
    val isAdvance = expense.category.equals("employee_advance", ignoreCase = true) || !expense.employeeName.isNullOrBlank()
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
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
                        .background(if (isAdvance) Amber50 else Crimson50)
                ) {
                    Icon(
                        imageVector = if (isAdvance) Icons.Default.Payments else Icons.Default.CallMade,
                        contentDescription = null,
                        tint = if (isAdvance) Amber600 else Crimson600,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = if (isAdvance && expense.item.isBlank()) "Staff Advance" else expense.item.ifBlank { "Expense" },
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = when {
                            !expense.employeeName.isNullOrBlank() -> "Staff: ${expense.employeeName} • ${FormatUtils.formatDateForDisplay(expense.createdAt)}"
                            !expense.category.isNullOrBlank() -> "${expense.category.replace('_', ' ').uppercase()} • ${FormatUtils.formatDateForDisplay(expense.createdAt)}"
                            else -> FormatUtils.formatDateForDisplay(expense.createdAt)
                        },
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

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
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (isAdvance) {
                        StatBadge(
                            text = "ADVANCE",
                            backgroundColor = Amber50,
                            textColor = Amber600
                        )
                    }
                    StatBadge(
                        text = expense.paymentMethod.uppercase(),
                        backgroundColor = Blue50,
                        textColor = Blue600
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
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
                item(key = "header_cash_book") {
                    VyavsaSectionHeader(
                        title = "Cash Till Ledger Entries",
                        badgeText = "${transactions.size}",
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
                items(
                    items = transactions,
                    key = { it.id.ifBlank { "${it.transactionDate}_${it.amount}_${it.newBalance}" } },
                    contentType = { "cash_txn" }
                ) { txn ->
                    CashTransactionCard(
                        txn = txn,
                        modifier = Modifier.animateItemPlacement()
                    )
                }
            }
        }
        else -> {}
    }
}

@Composable
private fun CashTransactionCard(
    txn: CashTransaction,
    modifier: Modifier = Modifier
) {
    val isAdd = txn.transactionType == "add_cash"
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
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

            Spacer(modifier = Modifier.width(8.dp))

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

@OptIn(ExperimentalFoundationApi::class)
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
                item(key = "header_billing") {
                    VyavsaSectionHeader(
                        title = "Plan Purchase Ledger",
                        badgeText = "${transactions.size}",
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
                items(
                    items = transactions,
                    key = { it.id.ifBlank { it.transactionId } },
                    contentType = { "billing_txn" }
                ) { txn ->
                    PlanTransactionCard(
                        txn = txn,
                        modifier = Modifier.animateItemPlacement()
                    )
                }
            }
        }
        else -> {}
    }
}

@Composable
private fun PlanTransactionCard(
    txn: PlanTransaction,
    modifier: Modifier = Modifier
) {
    val isCompleted = txn.status.equals("completed", ignoreCase = true)
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
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
