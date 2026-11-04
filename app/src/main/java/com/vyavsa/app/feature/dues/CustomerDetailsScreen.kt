package com.vyavsa.app.feature.dues

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AddCard
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vyavsa.app.core.common.FormatUtils
import com.vyavsa.app.core.common.ObserveAsEvents
import com.vyavsa.app.core.common.UiState
import com.vyavsa.app.core.designsystem.Amber50
import com.vyavsa.app.core.designsystem.Amber600
import com.vyavsa.app.core.designsystem.AvatarInitialCircle
import com.vyavsa.app.core.designsystem.Blue50
import com.vyavsa.app.core.designsystem.Blue600
import com.vyavsa.app.core.designsystem.Crimson50
import com.vyavsa.app.core.designsystem.Crimson600
import com.vyavsa.app.core.designsystem.Emerald50
import com.vyavsa.app.core.designsystem.Emerald600
import com.vyavsa.app.core.designsystem.Purple50
import com.vyavsa.app.core.designsystem.Purple600
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
import com.vyavsa.app.core.model.CustomerStatementEntry
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
    val statementState by viewModel.statementState.collectAsState()
    val adjustmentsState by viewModel.adjustmentsState.collectAsState()
    val banksState by viewModel.banksState.collectAsState()
    val actionState by viewModel.actionState.collectAsState()
    val lastPaymentResponse by viewModel.lastPaymentResponse.collectAsState()
    val lastAdvanceResponse by viewModel.lastAdvanceResponse.collectAsState()
    val isAdmin = viewModel.isAdmin

    val snackbarHostState = remember { SnackbarHostState() }
    ObserveAsEvents(viewModel.uiEvents) { msg ->
        snackbarHostState.showSnackbar(msg.text)
    }

    val banks = (banksState as? UiState.Success)?.data ?: emptyList()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showPaymentDialog by remember { mutableStateOf(false) }
    var showAdvanceDialog by remember { mutableStateOf(false) }
    var showApplyAdvanceDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showAdjustDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    LaunchedEffect(customerId) {
        viewModel.loadCustomerDetails(customerId)
        viewModel.loadCustomerSales(customerId)
        viewModel.loadStatement(customerId)
        viewModel.loadAdjustments(customerId)
        viewModel.loadBanks()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
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

                                            Column(
                                                horizontalAlignment = Alignment.End,
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                StatBadge(
                                                    text = customer.status.uppercase(),
                                                    backgroundColor = if (customer.status.equals("active", ignoreCase = true)) Emerald50 else Crimson50,
                                                    textColor = if (customer.status.equals("active", ignoreCase = true)) Emerald600 else Crimson600
                                                )
                                                val isBoth = (customer.isCustomer && customer.isSupplier) || customer.role.equals("both", ignoreCase = true)
                                                val isSupp = customer.isSupplier || customer.role.equals("supplier", ignoreCase = true)
                                                if (isBoth) {
                                                    StatBadge(
                                                        text = "DUAL ROLE",
                                                        backgroundColor = Purple50,
                                                        textColor = Purple600
                                                    )
                                                } else if (isSupp) {
                                                    StatBadge(
                                                        text = "SUPPLIER",
                                                        backgroundColor = Blue50,
                                                        textColor = Blue600
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(18.dp))

                                        // 4-Card Financial Summary Hero
                                        val dueNum = customer.currentBalance.toDoubleOrNull() ?: 0.0
                                        val advNum = customer.advanceBalance.toDoubleOrNull() ?: 0.0
                                        val billedVal = if (customer.totalBilled.isNotBlank() && customer.totalBilled != "0.00") customer.totalBilled else customer.totalPurchases
                                        val paidVal = if (customer.amountPaid.isNotBlank() && customer.amountPaid != "0.00") customer.amountPaid else customer.totalPaid

                                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                FinancialMetricCard(
                                                    title = "Outstanding Due",
                                                    amount = FormatUtils.formatCurrency(customer.currentBalance),
                                                    amountColor = if (dueNum > 0) Crimson600 else Emerald600,
                                                    containerColor = if (dueNum > 0) Crimson50 else Emerald50,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                FinancialMetricCard(
                                                    title = "Available Advance",
                                                    amount = FormatUtils.formatCurrency(customer.advanceBalance),
                                                    amountColor = if (advNum > 0) Blue600 else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    containerColor = if (advNum > 0) Blue50 else MaterialTheme.colorScheme.surfaceContainerLow,
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                FinancialMetricCard(
                                                    title = "Total Sales Billed",
                                                    amount = FormatUtils.formatCurrency(billedVal),
                                                    amountColor = MaterialTheme.colorScheme.onSurface,
                                                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                FinancialMetricCard(
                                                    title = "Payments Received",
                                                    amount = FormatUtils.formatCurrency(paidVal),
                                                    amountColor = Emerald600,
                                                    containerColor = Emerald50,
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                        }

                                        // Procurement / Payable Info if applicable
                                        val totalPurch = customer.totalPurchases.toDoubleOrNull() ?: 0.0
                                        val outstandingPayable = customer.outstandingPayable.toDoubleOrNull() ?: 0.0
                                        val totalRec = customer.currentBalance.toDoubleOrNull() ?: dueNum
                                        val hasDualRole = (customer.isCustomer && customer.isSupplier) || customer.role.equals("both", ignoreCase = true) || outstandingPayable > 0.0 || totalPurch > 0.0

                                        if (hasDualRole) {
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Surface(
                                                shape = RoundedCornerShape(18.dp),
                                                color = Purple50.copy(alpha = 0.5f),
                                                border = BorderStroke(1.dp, Purple600.copy(alpha = 0.35f)),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(modifier = Modifier.padding(14.dp)) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                        ) {
                                                            Text(
                                                                text = "⚡",
                                                                style = MaterialTheme.typography.titleSmall
                                                            )
                                                            Text(
                                                                text = "Unified Party Settlement",
                                                                style = MaterialTheme.typography.titleSmall.copy(
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = Purple600
                                                                )
                                                            )
                                                        }
                                                        val netStatusText = when {
                                                            customer.netStatus.equals("settled", ignoreCase = true) || (totalRec == outstandingPayable) -> "SETTLED"
                                                            customer.netStatus.equals("we_owe", ignoreCase = true) || (outstandingPayable > totalRec) -> "WE OWE PARTY"
                                                            else -> "PARTY OWES US"
                                                        }
                                                        val netStatusBg = when (netStatusText) {
                                                            "SETTLED" -> Emerald50
                                                            "WE OWE PARTY" -> Blue50
                                                            else -> Crimson50
                                                        }
                                                        val netStatusFg = when (netStatusText) {
                                                            "SETTLED" -> Emerald600
                                                            "WE OWE PARTY" -> Blue600
                                                            else -> Crimson600
                                                        }
                                                        StatBadge(
                                                            text = netStatusText,
                                                            backgroundColor = netStatusBg,
                                                            textColor = netStatusFg
                                                        )
                                                    }

                                                    Spacer(modifier = Modifier.height(10.dp))

                                                    Row(
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Column {
                                                            Text(
                                                                text = "Customer Receivable",
                                                                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                            )
                                                            Text(
                                                                text = FormatUtils.formatCurrency(totalRec.toString()),
                                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = if (totalRec > 0.0) Crimson600 else Emerald600
                                                                )
                                                            )
                                                        }

                                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                            Text(
                                                                text = "Supplier Payable",
                                                                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                            )
                                                            Text(
                                                                text = FormatUtils.formatCurrency(outstandingPayable.toString()),
                                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = if (outstandingPayable > 0.0) Purple600 else MaterialTheme.colorScheme.onSurface
                                                                )
                                                            )
                                                        }

                                                        Column(horizontalAlignment = Alignment.End) {
                                                            Text(
                                                                text = "Net Balance",
                                                                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                            )
                                                            val computedNet = (customer.netBalance?.toDoubleOrNull()) ?: (totalRec - outstandingPayable)
                                                            Text(
                                                                text = FormatUtils.formatCurrency(kotlin.math.abs(computedNet).toString()),
                                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                                    fontWeight = FontWeight.ExtraBold,
                                                                    color = if (computedNet > 0.0) Crimson600 else if (computedNet < 0.0) Blue600 else Emerald600
                                                                )
                                                            )
                                                        }
                                                    }

                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    Text(
                                                        text = "Auto contra adjustment dynamically settles sales against pending supplier purchases FIFO on creation.",
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    )
                                                }
                                            }
                                        }

                                        // Highlight banner if customer has both dues and available advance
                                        if (dueNum > 0.0 && advNum > 0.0) {
                                            Spacer(modifier = Modifier.height(14.dp))
                                            Surface(
                                                shape = RoundedCornerShape(16.dp),
                                                color = Blue50,
                                                border = BorderStroke(1.dp, Blue600.copy(alpha = 0.4f)),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 14.dp, vertical = 10.dp)
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = "Customer has Available Advance",
                                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Blue600)
                                                        )
                                                        Text(
                                                            text = "Apply deposit (${FormatUtils.formatCurrency(customer.advanceBalance)}) directly against due (${FormatUtils.formatCurrency(customer.currentBalance)})",
                                                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    VyavsaButton(
                                                        text = "Apply Now",
                                                        onClick = { showApplyAdvanceDialog = true }
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(18.dp))

                                        // Action buttons row
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (viewModel.canCollectPayment && dueNum > 0.0) {
                                                VyavsaButton(
                                                    text = "Collect Due",
                                                    leadingIcon = Icons.Default.Payment,
                                                    onClick = { showPaymentDialog = true },
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }

                                            if (viewModel.canCollectAdvance) {
                                                VyavsaButton(
                                                    text = "Collect Advance",
                                                    leadingIcon = Icons.Default.AddCard,
                                                    onClick = { showAdvanceDialog = true },
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }

                                            VyavsaOutlinedButton(
                                                text = "Statement",
                                                leadingIcon = Icons.AutoMirrored.Filled.ReceiptLong,
                                                onClick = { onNavigateToStatement(customer.id, customer.customerName) },
                                                modifier = if (!viewModel.canCollectAdvance || dueNum <= 0.0) Modifier.weight(1f) else Modifier
                                            )
                                        }

                                        if (isAdmin || viewModel.canAdjustBalance) {
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

                        // Unified Pill Segmented Tab Switcher (3 Tabs)
                        item(key = "customer_tabs") {
                            VyavsaSegmentedTabs(
                                tabs = listOf("Transactions Ledger", "Sales History", "Balance Adjustments"),
                                selectedIndex = selectedTabIndex,
                                onTabSelected = { selectedTabIndex = it },
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        // Tab Content
                        if (selectedTabIndex == 0) {
                            when (val stState = statementState) {
                                is UiState.Loading -> {
                                    item(key = "stmt_loading") {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.fillMaxWidth().padding(32.dp)
                                        ) {
                                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                }
                                is UiState.Empty -> {
                                    item(key = "stmt_empty") {
                                        EmptyStateView(
                                            title = "No Ledger Transactions",
                                            description = "No statement transactions recorded for this customer yet."
                                        )
                                    }
                                }
                                is UiState.Error -> {
                                    item(key = "stmt_error") {
                                        Box(modifier = Modifier.padding(20.dp)) {
                                            ErrorBanner(message = stState.message)
                                        }
                                    }
                                }
                                is UiState.Success -> {
                                    val entries = stState.data.entries
                                    if (entries.isEmpty()) {
                                        item(key = "stmt_entries_empty") {
                                            EmptyStateView(
                                                title = "No Transactions Found",
                                                description = "No statement transactions recorded for this customer yet."
                                            )
                                        }
                                    } else {
                                        itemsIndexed(
                                            items = entries,
                                            key = { index, item -> "cust_stmt_${item.id.ifBlank { "entry" }}_${item.entryType}_${item.date}_$index" }
                                        ) { _, entry ->
                                            CustomerTransactionItemCard(
                                                entry = entry,
                                                modifier = Modifier
                                                    .padding(horizontal = 20.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                }
                                else -> {}
                            }
                        } else if (selectedTabIndex == 1) {
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
                                    itemsIndexed(
                                        items = sState.data,
                                        key = { index, sale -> "cust_sale_${sale.id.ifBlank { "s" }}_$index" }
                                    ) { _, sale ->
                                        CustomerSaleItemCard(
                                            sale = sale,
                                            modifier = Modifier
                                                .padding(horizontal = 20.dp, vertical = 6.dp)
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
                                    itemsIndexed(
                                        items = aState.data,
                                        key = { index, adj -> "cust_adj_${adj.id.ifBlank { "a" }}_$index" }
                                    ) { _, adj ->
                                        CustomerAdjustmentItemCard(
                                            adjustment = adj,
                                            modifier = Modifier
                                                .padding(horizontal = 20.dp, vertical = 6.dp)
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

                    // Collect Payment Dialog
                    if (showPaymentDialog) {
                        CollectCustomerPaymentDialog(
                            customer = customer,
                            banks = banks,
                            onDismiss = { showPaymentDialog = false },
                            onConfirm = { amount, method, bankId, bankName, refId, note ->
                                showPaymentDialog = false
                                viewModel.collectCustomerPayment(
                                    customerId = customer.id,
                                    amount = amount,
                                    paymentMethod = method,
                                    bankId = bankId,
                                    bankName = bankName,
                                    referenceId = refId,
                                    note = note,
                                    maxBalance = customer.currentBalance
                                )
                            }
                        )
                    }

                    // Collect Advance Dialog
                    if (showAdvanceDialog) {
                        CollectCustomerAdvanceDialog(
                            customer = customer,
                            banks = banks,
                            onDismiss = { showAdvanceDialog = false },
                            onConfirm = { amount, method, bankId, bankName, refId, note ->
                                showAdvanceDialog = false
                                viewModel.collectCustomerAdvance(
                                    customerId = customer.id,
                                    amount = amount,
                                    paymentMethod = method,
                                    bankId = bankId,
                                    bankName = bankName,
                                    paymentDate = null,
                                    referenceId = refId,
                                    note = note
                                )
                            }
                        )
                    }

                    // Apply Advance to Dues Dialog
                    if (showApplyAdvanceDialog) {
                        ApplyCustomerAdvanceDialog(
                            customer = customer,
                            onDismiss = { showApplyAdvanceDialog = false },
                            onConfirm = { amount, note ->
                                showApplyAdvanceDialog = false
                                viewModel.applyCustomerAdvance(
                                    customerId = customer.id,
                                    amount = amount,
                                    note = note
                                )
                            }
                        )
                    }

                    // Customer Payment Receipt Dialog
                    val curPaymentResp = lastPaymentResponse
                    if (curPaymentResp != null) {
                        CustomerPaymentReceiptDialog(
                            response = curPaymentResp,
                            onDismiss = { viewModel.clearLastPaymentResponse() }
                        )
                    }

                    // Customer Advance Receipt Dialog
                    val curAdvanceResp = lastAdvanceResponse
                    if (curAdvanceResp != null) {
                        CustomerAdvanceReceiptDialog(
                            response = curAdvanceResp,
                            onDismiss = { viewModel.clearLastAdvanceResponse() }
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

@Composable
private fun FinancialMetricCard(
    title: String,
    amount: String,
    amountColor: Color,
    containerColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = amount,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = amountColor
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CustomerTransactionItemCard(
    entry: CustomerStatementEntry,
    modifier: Modifier = Modifier
) {
    val billedAmt = (entry.billedAmount.ifBlank { entry.purchaseAmount }).toDoubleOrNull() ?: 0.0
    val paidAmt = entry.paidAmount.toDoubleOrNull() ?: 0.0
    val isDebit = billedAmt > 0.0
    val isCredit = paidAmt > 0.0

    val badgeText: String
    val badgeBg: Color
    val badgeFg: Color

    when (entry.entryType.lowercase()) {
        "line_sale" -> {
            badgeText = "LINE SALE"
            badgeBg = Crimson50
            badgeFg = Crimson600
        }
        "counter_sale" -> {
            badgeText = "COUNTER SALE"
            badgeBg = Crimson50
            badgeFg = Crimson600
        }
        "sale" -> {
            badgeText = "SALE"
            badgeBg = Crimson50
            badgeFg = Crimson600
        }
        "collection", "payment" -> {
            badgeText = "PAYMENT"
            badgeBg = Emerald50
            badgeFg = Emerald600
        }
        "advance_receipt", "advance" -> {
            badgeText = "ADVANCE"
            badgeBg = Blue50
            badgeFg = Blue600
        }
        "advance_settlement", "advance_applied" -> {
            badgeText = "ADV SETTLED"
            badgeBg = Amber50
            badgeFg = Amber600
        }
        "contra_settlement", "contra_adjustment", "contra" -> {
            badgeText = "CONTRA ADJUST"
            badgeBg = Purple50
            badgeFg = Purple600
        }
        "adjustment" -> {
            badgeText = "ADJUSTMENT"
            badgeBg = MaterialTheme.colorScheme.surfaceContainerHigh
            badgeFg = MaterialTheme.colorScheme.onSurfaceVariant
        }
        "opening_balance" -> {
            badgeText = "OPENING"
            badgeBg = MaterialTheme.colorScheme.surfaceContainerHigh
            badgeFg = MaterialTheme.colorScheme.onSurfaceVariant
        }
        else -> {
            badgeText = entry.entryType.uppercase().ifBlank { "TRANSACTION" }
            badgeBg = MaterialTheme.colorScheme.surfaceContainerHigh
            badgeFg = MaterialTheme.colorScheme.onSurfaceVariant
        }
    }

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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    StatBadge(text = badgeText, backgroundColor = badgeBg, textColor = badgeFg)
                    Text(
                        text = entry.description.ifBlank { "Transaction #${entry.id.takeLast(6)}" },
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                val displayAmt = if (isDebit) "+${FormatUtils.formatCurrency(billedAmt)}" else if (isCredit) "-${FormatUtils.formatCurrency(paidAmt)}" else FormatUtils.formatCurrency(0.0)
                val amtColor = if (isDebit) Crimson600 else if (isCredit) Emerald600 else MaterialTheme.colorScheme.onSurface
                Text(
                    text = displayAmt,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = amtColor
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = entry.formattedDate.ifBlank { FormatUtils.formatDateForDisplay(entry.date) },
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )

                Text(
                    text = "Running Bal: ${FormatUtils.formatCurrency(entry.balance)}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }

            val method = entry.paymentMethod
            val bank = entry.bankName
            val ref = entry.referenceId
            if (!method.isNullOrBlank() || !ref.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val methodText = buildString {
                        if (!method.isNullOrBlank()) append(method.uppercase())
                        if (!bank.isNullOrBlank()) append(" ($bank)")
                    }
                    if (methodText.isNotBlank()) {
                        Text(
                            text = methodText,
                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                    if (!ref.isNullOrBlank()) {
                        Text(
                            text = "Ref: $ref",
                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Apply Customer Advance against Outstanding Dues Dialog.
 */
@Composable
fun ApplyCustomerAdvanceDialog(
    customer: Customer,
    onDismiss: () -> Unit,
    onConfirm: (amount: String, note: String?) -> Unit
) {
    val dueNum = customer.currentBalance.toDoubleOrNull() ?: 0.0
    val advNum = customer.advanceBalance.toDoubleOrNull() ?: 0.0
    val maxApplicable = java.lang.Double.min(dueNum, advNum)
    var amount by remember { mutableStateOf(String.format(java.util.Locale.US, "%.2f", maxApplicable)) }
    var note by remember { mutableStateOf("") }
    var amountError by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Apply Advance to Dues",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold)
                    )
                    androidx.compose.material3.IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Settle customer outstanding receivables using available advance deposit",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Current Due:", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                FormatUtils.formatCurrency(customer.currentBalance),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = Crimson600)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Available Advance:", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                FormatUtils.formatCurrency(customer.advanceBalance),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = Blue600)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                VyavsaTextField(
                    value = amount,
                    onValueChange = {
                        amount = it
                        val v = it.toDoubleOrNull() ?: 0.0
                        amountError = when {
                            v <= 0.0 -> "Enter valid positive amount"
                            v > advNum -> "Cannot exceed available advance (${FormatUtils.formatCurrency(customer.advanceBalance)})"
                            v > dueNum -> "Cannot exceed outstanding due (${FormatUtils.formatCurrency(customer.currentBalance)})"
                            else -> null
                        }
                    },
                    label = "Amount to Apply (₹) *",
                    placeholder = "0.00",
                    isError = amountError != null,
                    errorMessage = amountError,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal,
                        imeAction = androidx.compose.ui.text.input.ImeAction.Next
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                VyavsaTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = "Note / Remarks (Optional)",
                    placeholder = "e.g. Settle bill dues from advance deposit",
                    keyboardOptions = KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Done),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(22.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    VyavsaOutlinedButton(
                        text = "Cancel",
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    )
                    VyavsaButton(
                        text = "Apply Advance",
                        onClick = {
                            val v = amount.toDoubleOrNull() ?: 0.0
                            if (v <= 0.0 || v > advNum || v > dueNum) {
                                amountError = "Please enter valid settlement amount"
                                return@VyavsaButton
                            }
                            onConfirm(amount.trim(), note.trim().ifBlank { null })
                        },
                        modifier = Modifier.weight(1.2f)
                    )
                }
            }
        }
    }
}
