package com.vyavsa.app.feature.dues

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.vyavsa.app.core.designsystem.Crimson50
import com.vyavsa.app.core.designsystem.Crimson600
import com.vyavsa.app.core.designsystem.Emerald50
import com.vyavsa.app.core.designsystem.Emerald600
import com.vyavsa.app.core.designsystem.EmptyStateView
import com.vyavsa.app.core.designsystem.ErrorBanner
import com.vyavsa.app.core.designsystem.StatBadge
import com.vyavsa.app.core.designsystem.VyavsaGradients
import com.vyavsa.app.core.designsystem.VyavsaSearchBar
import com.vyavsa.app.core.designsystem.VyavsaSegmentedTabs
import com.vyavsa.app.core.designsystem.VyavsaTextField
import com.vyavsa.app.core.designsystem.VyavsaTopAppBar
import com.vyavsa.app.core.designsystem.vyavsaClickable
import com.vyavsa.app.core.designsystem.Blue50
import com.vyavsa.app.core.designsystem.Blue600
import com.vyavsa.app.core.designsystem.VyavsaButton
import androidx.compose.material3.HorizontalDivider
import com.vyavsa.app.core.model.CustomerAdvanceResponse
import com.vyavsa.app.core.model.CustomerPaymentResponse
import com.vyavsa.app.core.model.Customer
import com.vyavsa.app.core.model.Party
import com.vyavsa.app.core.model.Supplier
import com.vyavsa.app.core.model.TenantBank
import com.vyavsa.app.feature.party.CreatePartyDialog
import com.vyavsa.app.feature.party.EditPartyDialog
import com.vyavsa.app.feature.party.PartiesListContent
import com.vyavsa.app.feature.party.PartyCard

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DuesScreen(
    viewModel: DuesViewModel,
    onNavigateToStatement: (customerId: String, customerName: String) -> Unit,
    onNavigateToCustomerDetails: (customerId: String) -> Unit = {},
    onNavigateToPartyDetails: (partyId: String) -> Unit = {},
    initialTab: DuesTab? = null,
    modifier: Modifier = Modifier
) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val partiesState by viewModel.partiesState.collectAsState()
    val partiesPagination by viewModel.partiesPagination.collectAsState()
    val partyRoleFilter by viewModel.partyRoleFilter.collectAsState()
    val partyBalanceFilter by viewModel.partyBalanceFilter.collectAsState()
    val customersState by viewModel.customersState.collectAsState()
    val suppliersState by viewModel.suppliersState.collectAsState()
    val banksState by viewModel.banksState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val actionState by viewModel.actionState.collectAsState()
    val canCreateParty = viewModel.canCreateParty
    val canUpdateParty = viewModel.canUpdateParty
    val canDeleteParty = viewModel.canDeleteParty

    val snackbarHostState = remember { SnackbarHostState() }
    ObserveAsEvents(viewModel.uiEvents) { msg ->
        snackbarHostState.showSnackbar(msg.text)
    }

    LaunchedEffect(initialTab) {
        if (initialTab != null) {
            viewModel.selectTab(initialTab)
        }
    }

    var showAddPartyDialog by remember { mutableStateOf(false) }
    var editingParty by remember { mutableStateOf<Party?>(null) }
    var deletingParty by remember { mutableStateOf<Party?>(null) }
    var showAddCustomerDialog by remember { mutableStateOf(false) }
    var showAddSupplierDialog by remember { mutableStateOf(false) }
    var collectingCustomer by remember { mutableStateOf<Customer?>(null) }
    var collectingAdvanceCustomer by remember { mutableStateOf<Customer?>(null) }
    var applyingAdvanceCustomer by remember { mutableStateOf<Customer?>(null) }
    var payingSupplier by remember { mutableStateOf<Supplier?>(null) }

    val lastPaymentResponse by viewModel.lastPaymentResponse.collectAsState()
    val lastAdvanceResponse by viewModel.lastAdvanceResponse.collectAsState()

    val banks = (banksState as? UiState.Success)?.data ?: emptyList()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            VyavsaTopAppBar(
                title = "Khata & Ledger Accounts",
                subtitle = "Manage unified parties, customer receivables & supplier payables"
            )
        },
        floatingActionButton = {
            val canShowFab = when (selectedTab) {
                DuesTab.PARTIES -> canCreateParty
                else -> true
            }
            if (canShowFab) {
                ExtendedFloatingActionButton(
                    onClick = {
                        when (selectedTab) {
                            DuesTab.PARTIES -> showAddPartyDialog = true
                            DuesTab.CUSTOMERS -> showAddCustomerDialog = true
                            DuesTab.SUPPLIERS -> showAddSupplierDialog = true
                        }
                    },
                    shape = RoundedCornerShape(20.dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    icon = { Icon(imageVector = Icons.Default.Add, contentDescription = null) },
                    text = {
                        Text(
                            when (selectedTab) {
                                DuesTab.PARTIES -> "Add Party"
                                DuesTab.CUSTOMERS -> "Add Customer"
                                DuesTab.SUPPLIERS -> "Add Supplier"
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
            // Segmented Switcher for Parties vs Customers vs Suppliers
            VyavsaSegmentedTabs(
                tabs = listOf("Parties (Unified)", "Customers (Receivables)", "Suppliers (Payables)"),
                selectedIndex = when (selectedTab) {
                    DuesTab.PARTIES -> 0
                    DuesTab.CUSTOMERS -> 1
                    DuesTab.SUPPLIERS -> 2
                },
                onTabSelected = { idx ->
                    viewModel.selectTab(
                        when (idx) {
                            0 -> DuesTab.PARTIES
                            1 -> DuesTab.CUSTOMERS
                            else -> DuesTab.SUPPLIERS
                        }
                    )
                },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )

            // Pill Search Input
            VyavsaSearchBar(
                query = searchQuery,
                onQueryChange = { viewModel.onSearchChange(it) },
                placeholder = when (selectedTab) {
                    DuesTab.PARTIES -> "Search party name, phone, email..."
                    DuesTab.CUSTOMERS -> "Search customer name or phone..."
                    DuesTab.SUPPLIERS -> "Search supplier name or phone..."
                },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )

            if (actionState is UiState.Error) {
                ErrorBanner(
                    message = (actionState as UiState.Error).message,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )
            }

            if (selectedTab == DuesTab.PARTIES) {
                // Unified Business Parties Tab
                Column(modifier = Modifier.fillMaxSize()) {
                    // Filter Chips Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = partyRoleFilter == "all",
                            onClick = { viewModel.selectPartyRoleFilter("all") },
                            label = { Text("All Roles") }
                        )
                        FilterChip(
                            selected = partyRoleFilter == "both",
                            onClick = { viewModel.selectPartyRoleFilter("both") },
                            label = { Text("Both (Cust & Supp)") }
                        )
                        FilterChip(
                            selected = partyRoleFilter == "customer",
                            onClick = { viewModel.selectPartyRoleFilter("customer") },
                            label = { Text("Customers Only") }
                        )
                        FilterChip(
                            selected = partyRoleFilter == "supplier",
                            onClick = { viewModel.selectPartyRoleFilter("supplier") },
                            label = { Text("Suppliers Only") }
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = partyBalanceFilter == "all",
                            onClick = { viewModel.selectPartyBalanceFilter("all") },
                            label = { Text("All Balances") }
                        )
                        FilterChip(
                            selected = partyBalanceFilter == "dues",
                            onClick = { viewModel.selectPartyBalanceFilter("dues") },
                            label = { Text("Has Dues") }
                        )
                        FilterChip(
                            selected = partyBalanceFilter == "settleable",
                            onClick = { viewModel.selectPartyBalanceFilter("settleable") },
                            label = { Text("Eligible for Contra") }
                        )
                    }

                    when (val state = partiesState) {
                        is UiState.Loading -> {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        is UiState.Empty -> {
                            EmptyStateView(
                                title = "No Business Parties Found",
                                description = "Register business parties to manage sales, purchases, and mutual balance settlements",
                                icon = Icons.Default.Groups,
                                actionText = "Add Business Party",
                                onActionClick = { showAddPartyDialog = true }
                            )
                        }
                        is UiState.Error -> {
                            Box(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                                ErrorBanner(message = state.message)
                            }
                        }
                        is UiState.Success -> {
                            val parties = state.data
                            val totalReceivable = remember(parties) {
                                parties.sumOf { it.totalReceivable.toDoubleOrNull() ?: 0.0 }
                            }
                            val totalPayable = remember(parties) {
                                parties.sumOf { it.totalPayable.toDoubleOrNull() ?: 0.0 }
                            }
                            val netTotal = totalReceivable - totalPayable

                            LazyColumn(
                                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 96.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                item {
                                    Card(
                                        shape = RoundedCornerShape(20.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(14.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = "Net Directory Position",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                )
                                                Text(
                                                    text = FormatUtils.formatCurrency(kotlin.math.abs(netTotal)),
                                                    style = MaterialTheme.typography.titleMedium.copy(
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = if (netTotal > 0) Emerald600 else if (netTotal < 0) Crimson600 else MaterialTheme.colorScheme.onSurface
                                                    )
                                                )
                                            }
                                            StatBadge(
                                                text = if (netTotal > 0) "Net Due: +${FormatUtils.formatCurrency(netTotal)}" else if (netTotal < 0) "Net Payable: -${FormatUtils.formatCurrency(kotlin.math.abs(netTotal))}" else "Settled",
                                                backgroundColor = if (netTotal > 0) Emerald50 else if (netTotal < 0) Crimson50 else MaterialTheme.colorScheme.surfaceContainer,
                                                textColor = if (netTotal > 0) Emerald600 else if (netTotal < 0) Crimson600 else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                items(parties, key = { it.id }) { party ->
                                    val recv = party.totalReceivable.toDoubleOrNull() ?: 0.0
                                    val pay = party.totalPayable.toDoubleOrNull() ?: 0.0
                                    PartyCard(
                                        party = party,
                                        onClick = { onNavigateToPartyDetails(party.id) },
                                        onEdit = if (canUpdateParty) { { editingParty = party } } else null,
                                        onDelete = if (canDeleteParty) { { deletingParty = party } } else null,
                                        onCollect = if (recv > 0.009) {
                                            {
                                                collectingCustomer = Customer(
                                                    id = party.id,
                                                    customerName = party.displayName,
                                                    phone = party.phone,
                                                    currentBalance = party.totalReceivable
                                                )
                                            }
                                        } else null,
                                        onPay = if (pay > 0.009) {
                                            {
                                                payingSupplier = Supplier(
                                                    id = party.id,
                                                    supplierName = party.displayName,
                                                    phone = party.phone,
                                                    outstandingPayable = party.totalPayable
                                                )
                                            }
                                        } else null
                                    )
                                }

                                partiesPagination?.let { pag ->
                                    if (pag.totalPages > 1) {
                                        item {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 8.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                androidx.compose.material3.OutlinedButton(
                                                    onClick = { viewModel.previousPartyPage() },
                                                    enabled = pag.page > 1
                                                ) {
                                                    Text("Previous")
                                                }
                                                Text(
                                                    text = "Page ${pag.page} of ${pag.totalPages}",
                                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                )
                                                androidx.compose.material3.OutlinedButton(
                                                    onClick = { viewModel.nextPartyPage() },
                                                    enabled = pag.page < pag.totalPages
                                                ) {
                                                    Text("Next")
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        else -> Unit
                    }
                }
            } else if (selectedTab == DuesTab.CUSTOMERS) {
                when (val state = customersState) {
                    is UiState.Loading -> {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    is UiState.Empty -> {
                        EmptyStateView(
                            title = "No Customers Found",
                            description = "Add customers to start managing dues, receipts, and ledger statements",
                            icon = Icons.Default.People,
                            actionText = "Add New Customer",
                            onActionClick = { showAddCustomerDialog = true }
                        )
                    }
                    is UiState.Error -> {
                        Box(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                            ErrorBanner(message = state.message)
                        }
                    }
                    is UiState.Success -> {
                        val customers = state.data
                        val totalDue = remember(customers) {
                            customers.sumOf { it.currentBalance.toDoubleOrNull() ?: 0.0 }
                        }

                        LazyColumn(
                            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 88.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            item(key = "dues_summary_banner") {
                                Card(
                                    shape = RoundedCornerShape(26.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(brush = VyavsaGradients.PrimaryHeroBrush)
                                            .padding(20.dp)
                                    ) {
                                        Column {
                                            Text(
                                                text = "Total Outstanding Receivables",
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    color = Color.White.copy(alpha = 0.9f),
                                                    fontWeight = FontWeight.Medium
                                                )
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = FormatUtils.formatCurrency(totalDue.toString()),
                                                style = MaterialTheme.typography.headlineLarge.copy(
                                                    fontWeight = FontWeight.Black,
                                                    color = Color.White
                                                )
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Surface(
                                                shape = CircleShape,
                                                color = Color.White.copy(alpha = 0.22f)
                                            ) {
                                                Text(
                                                    text = "${customers.size} Customer Accounts Registered",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        color = Color.White,
                                                        fontWeight = FontWeight.SemiBold
                                                    ),
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            itemsIndexed(
                                items = customers,
                                key = { index, it -> "cust_${it.id.ifBlank { "c" }}_$index" },
                                contentType = { _, _ -> "customer_item" }
                            ) { _, customer ->
                                CustomerDuesCard(
                                    customer = customer,
                                    canCollectPayment = viewModel.canCollectPayment,
                                    canCollectAdvance = viewModel.canCollectAdvance,
                                    onCollectPaymentClick = { collectingCustomer = customer },
                                    onCollectAdvanceClick = { collectingAdvanceCustomer = customer },
                                    onApplyAdvanceClick = { applyingAdvanceCustomer = customer },
                                    onStatementClick = { onNavigateToStatement(customer.id, customer.customerName) },
                                    onClick = { onNavigateToCustomerDetails(customer.id) }
                                )
                            }
                        }
                    }
                    is UiState.Idle -> {}
                }
            } else {
                when (val state = suppliersState) {
                    is UiState.Loading -> {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    is UiState.Empty -> {
                        EmptyStateView(
                            title = "No Suppliers Found",
                            description = "Add vendors & suppliers to manage payables and purchase ledgers",
                            icon = Icons.Default.LocalShipping,
                            actionText = "Add New Supplier",
                            onActionClick = { showAddSupplierDialog = true }
                        )
                    }
                    is UiState.Error -> {
                        Box(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                            ErrorBanner(message = state.message)
                        }
                    }
                    is UiState.Success -> {
                        val suppliers = state.data
                        val totalPayable = remember(suppliers) {
                            suppliers.sumOf { it.outstandingPayable.toDoubleOrNull() ?: 0.0 }
                        }

                        LazyColumn(
                            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 88.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            item(key = "supplier_summary_banner") {
                                Card(
                                    shape = RoundedCornerShape(26.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(brush = VyavsaGradients.BlueHeroBrush)
                                            .padding(20.dp)
                                    ) {
                                        Column {
                                            Text(
                                                text = "Total Outstanding Payables",
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    color = Color.White.copy(alpha = 0.9f),
                                                    fontWeight = FontWeight.Medium
                                                )
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = FormatUtils.formatCurrency(totalPayable.toString()),
                                                style = MaterialTheme.typography.headlineLarge.copy(
                                                    fontWeight = FontWeight.Black,
                                                    color = Color.White
                                                )
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Surface(
                                                shape = CircleShape,
                                                color = Color.White.copy(alpha = 0.22f)
                                            ) {
                                                Text(
                                                    text = "${suppliers.size} Supplier Accounts Registered",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        color = Color.White,
                                                        fontWeight = FontWeight.SemiBold
                                                    ),
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            itemsIndexed(
                                items = suppliers,
                                key = { index, it -> "supp_${it.id.ifBlank { "s" }}_$index" },
                                contentType = { _, _ -> "supplier_item" }
                            ) { _, supplier ->
                                SupplierPayableCard(
                                    supplier = supplier,
                                    onPaySupplierClick = { payingSupplier = supplier }
                                )
                            }
                        }
                    }
                    is UiState.Idle -> {}
                }
            }
        }
    }

    // Add Party Dialog
    if (showAddPartyDialog) {
        CreatePartyDialog(
            isLoading = actionState is UiState.Loading,
            onDismiss = { showAddPartyDialog = false },
            onSubmit = { name, phone, email, address, role, recv, pay ->
                viewModel.createParty(
                    name = name,
                    phone = phone,
                    email = email,
                    address = address,
                    role = role,
                    openingReceivable = recv,
                    openingPayable = pay,
                    onSuccess = { showAddPartyDialog = false }
                )
            }
        )
    }

    // Edit Party Dialog
    editingParty?.let { party ->
        EditPartyDialog(
            party = party,
            isLoading = actionState is UiState.Loading,
            onDismiss = { editingParty = null },
            onSubmit = { name, phone, email, address, role, status ->
                viewModel.updateParty(
                    id = party.id,
                    name = name,
                    phone = phone,
                    email = email,
                    address = address,
                    role = role,
                    status = status,
                    onSuccess = { editingParty = null }
                )
            }
        )
    }

    // Delete Party Confirmation Dialog
    deletingParty?.let { party ->
        AlertDialog(
            onDismissRequest = { deletingParty = null },
            shape = RoundedCornerShape(20.dp),
            title = { Text("Delete Business Party", fontWeight = FontWeight.Bold) },
            text = {
                Text("Are you sure you want to delete '${party.displayName}'? This action cannot be undone.")
            },
            confirmButton = {
                VyavsaButton(
                    text = "Delete",
                    onClick = {
                        viewModel.deleteParty(party.id, onSuccess = { deletingParty = null })
                    }
                )
            },
            dismissButton = {
                TextButton(onClick = { deletingParty = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Add Customer Dialog
    if (showAddCustomerDialog) {
        AddCustomerDialog(
            onDismiss = { showAddCustomerDialog = false },
            onConfirm = { name, phone, openingBalance ->
                showAddCustomerDialog = false
                viewModel.createCustomer(name, phone, openingBalance)
            }
        )
    }

    // Add Supplier Dialog
    if (showAddSupplierDialog) {
        AddSupplierDialog(
            onDismiss = { showAddSupplierDialog = false },
            onConfirm = { name, phone ->
                showAddSupplierDialog = false
                viewModel.createSupplier(name, phone)
            }
        )
    }

    // Collect Customer Payment Dialog
    if (collectingCustomer != null) {
        CollectCustomerPaymentDialog(
            customer = collectingCustomer!!,
            banks = banks,
            onDismiss = { collectingCustomer = null },
            onConfirm = { amount, method, bankId, bankName, refId, note ->
                viewModel.collectCustomerPayment(
                    customerId = collectingCustomer!!.id,
                    amount = amount,
                    paymentMethod = method,
                    bankId = bankId,
                    bankName = bankName,
                    referenceId = refId,
                    note = note,
                    maxBalance = collectingCustomer!!.currentBalance
                )
                collectingCustomer = null
            }
        )
    }

    // Collect Customer Advance Dialog
    if (collectingAdvanceCustomer != null) {
        CollectCustomerAdvanceDialog(
            customer = collectingAdvanceCustomer!!,
            banks = banks,
            onDismiss = { collectingAdvanceCustomer = null },
            onConfirm = { amount, method, bankId, bankName, refId, note ->
                viewModel.collectCustomerAdvance(
                    customerId = collectingAdvanceCustomer!!.id,
                    amount = amount,
                    paymentMethod = method,
                    bankId = bankId,
                    bankName = bankName,
                    referenceId = refId,
                    note = note
                )
                collectingAdvanceCustomer = null
            }
        )
    }

    if (applyingAdvanceCustomer != null) {
        ApplyCustomerAdvanceDialog(
            customer = applyingAdvanceCustomer!!,
            onDismiss = { applyingAdvanceCustomer = null },
            onConfirm = { amount, note ->
                val cust = applyingAdvanceCustomer!!
                applyingAdvanceCustomer = null
                viewModel.applyCustomerAdvance(
                    customerId = cust.id,
                    amount = amount,
                    note = note
                )
            }
        )
    }

    val curPaymentResp = lastPaymentResponse
    if (curPaymentResp != null) {
        CustomerPaymentReceiptDialog(
            response = curPaymentResp,
            onDismiss = { viewModel.clearLastPaymentResponse() }
        )
    }

    val curAdvanceResp = lastAdvanceResponse
    if (curAdvanceResp != null) {
        CustomerAdvanceReceiptDialog(
            response = curAdvanceResp,
            onDismiss = { viewModel.clearLastAdvanceResponse() }
        )
    }

    // Pay Supplier Dialog
    if (payingSupplier != null) {
        RecordSupplierPaymentDialog(
            supplier = payingSupplier!!,
            banks = banks,
            onDismiss = { payingSupplier = null },
            onConfirm = { amount, method, bankId, _, refId, note ->
                viewModel.recordSupplierPayment(
                    supplierId = payingSupplier!!.id,
                    amount = amount,
                    paymentMethod = method,
                    bankId = bankId,
                    referenceId = refId,
                    note = note
                )
                payingSupplier = null
            }
        )
    }
}

@Composable
private fun CustomerDuesCard(
    customer: Customer,
    canCollectPayment: Boolean = true,
    canCollectAdvance: Boolean = true,
    onCollectPaymentClick: () -> Unit,
    onCollectAdvanceClick: () -> Unit,
    onApplyAdvanceClick: (() -> Unit)? = null,
    onStatementClick: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val balanceAmount = customer.currentBalance.toDoubleOrNull() ?: 0.0
    val advanceAmount = customer.advanceBalance.toDoubleOrNull() ?: 0.0

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .vyavsaClickable { onClick() }
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f), RoundedCornerShape(24.dp))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    AvatarInitialCircle(name = customer.customerName)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = customer.customerName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (!customer.phone.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = customer.phone,
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = FormatUtils.formatCurrency(customer.currentBalance),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = if (balanceAmount > 0) Crimson600 else Emerald600
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (advanceAmount > 0) {
                            StatBadge(
                                text = "Adv: ${FormatUtils.formatCurrency(customer.advanceBalance)}",
                                backgroundColor = Blue50,
                                textColor = Blue600
                            )
                        }
                        StatBadge(
                            text = if (balanceAmount > 0) "Due" else "Clear",
                            backgroundColor = if (balanceAmount > 0) Crimson50 else Emerald50,
                            textColor = if (balanceAmount > 0) Crimson600 else Emerald600
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (balanceAmount > 0 && canCollectPayment) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier
                            .clip(CircleShape)
                            .vyavsaClickable { onCollectPaymentClick() }
                    ) {
                        Text(
                            text = "Collect Payment",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }

                if (canCollectAdvance) {
                    Surface(
                        shape = CircleShape,
                        color = Blue50,
                        modifier = Modifier
                            .clip(CircleShape)
                            .vyavsaClickable { onCollectAdvanceClick() }
                    ) {
                        Text(
                            text = "Collect Advance",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = Blue600,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }

                if (canCollectAdvance && balanceAmount > 0.0 && advanceAmount > 0.0 && onApplyAdvanceClick != null) {
                    Surface(
                        shape = CircleShape,
                        color = Blue50,
                        border = BorderStroke(1.dp, Blue600.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .clip(CircleShape)
                            .vyavsaClickable { onApplyAdvanceClick() }
                    ) {
                        Text(
                            text = "Apply Adv",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = Blue600,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier
                        .clip(CircleShape)
                        .vyavsaClickable { onStatementClick() }
                ) {
                    Text(
                        text = "Statement →",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        ),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SupplierPayableCard(
    supplier: Supplier,
    onPaySupplierClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val payableAmount = supplier.outstandingPayable.toDoubleOrNull() ?: 0.0

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f), RoundedCornerShape(24.dp))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    AvatarInitialCircle(name = supplier.displayName)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = supplier.displayName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (!supplier.phone.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = supplier.phone,
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = FormatUtils.formatCurrency(supplier.outstandingPayable),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = if (payableAmount > 0) Crimson600 else Emerald600
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    StatBadge(
                        text = if (payableAmount > 0) "Payable" else "Settled",
                        backgroundColor = if (payableAmount > 0) Crimson50 else Emerald50,
                        textColor = if (payableAmount > 0) Crimson600 else Emerald600
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Purchases: ${FormatUtils.formatCurrency(supplier.totalPurchases)}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier
                        .clip(CircleShape)
                        .vyavsaClickable { onPaySupplierClick() }
                ) {
                    Text(
                        text = "Pay Supplier",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AddCustomerDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, phone: String?, openingBalance: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var openingBalance by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Add Customer Account", fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                VyavsaTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Customer Name *"
                )
                VyavsaTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = "Phone Number",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )
                VyavsaTextField(
                    value = openingBalance,
                    onValueChange = { openingBalance = it },
                    label = "Opening Due Balance (₹)",
                    placeholder = "0.00",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name, phone, openingBalance) },
                enabled = name.isNotBlank()
            ) {
                Text("Add Customer", fontWeight = FontWeight.Bold)
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
fun AddSupplierDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, phone: String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Add Supplier / Vendor", fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                VyavsaTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Supplier / Vendor Name *"
                )
                VyavsaTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = "Phone Number",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name.trim(), phone.trim().ifBlank { null }) },
                enabled = name.isNotBlank()
            ) {
                Text("Add Supplier", fontWeight = FontWeight.Bold)
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
fun CollectCustomerPaymentDialog(
    customer: Customer,
    banks: List<TenantBank> = emptyList(),
    onDismiss: () -> Unit,
    onConfirm: (amount: String, method: String, bankId: String?, bankName: String?, referenceId: String?, note: String?) -> Unit
) {
    var amount by remember { mutableStateOf(customer.currentBalance) }
    var method by remember { mutableStateOf("cash") }
    var selectedBankId by remember(banks) { mutableStateOf(banks.firstOrNull()?.id) }
    var referenceId by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }

    val currentDueVal = customer.currentBalance.toDoubleOrNull() ?: 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Collect Payment: ${customer.customerName}", fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Crimson50,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Current Due Balance: ${FormatUtils.formatCurrency(customer.currentBalance)}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = Crimson600),
                        modifier = Modifier.padding(12.dp)
                    )
                }

                VyavsaTextField(
                    value = amount,
                    onValueChange = {
                        amount = it
                        validationError = null
                    },
                    label = "Collection Amount (₹) *",
                    placeholder = "0.00",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )

                Text(
                    text = "Deposit Mode *",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )

                VyavsaSegmentedTabs(
                    tabs = listOf("Cash Till", "Bank Account"),
                    selectedIndex = if (method == "cash") 0 else 1,
                    onTabSelected = { idx ->
                        method = if (idx == 0) "cash" else "bank"
                        validationError = null
                        if (method == "bank" && selectedBankId == null && banks.isNotEmpty()) {
                            selectedBankId = banks.first().id
                        }
                    }
                )

                AnimatedVisibility(
                    visible = method == "bank",
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Deposit Into Bank Account *",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )

                        if (banks.isEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Amber50,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccountBalance,
                                        contentDescription = null,
                                        tint = Amber600,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "No bank accounts available. Please configure a bank in Bank Accounts.",
                                        style = MaterialTheme.typography.bodySmall.copy(color = Amber600)
                                    )
                                }
                            }
                        } else {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                            ) {
                                banks.forEach { bank ->
                                    val isSelected = selectedBankId == bank.id
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            selectedBankId = bank.id
                                            validationError = null
                                        },
                                        shape = CircleShape,
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.AccountBalance,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        ),
                                        label = {
                                            val labelText = if (!bank.accountNumber.isNullOrBlank()) {
                                                "${bank.bankName} (..${bank.accountNumber.takeLast(4)})"
                                            } else {
                                                bank.bankName
                                            }
                                            Text(labelText, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                        }
                                    )
                                }
                            }
                        }

                        VyavsaTextField(
                            value = referenceId,
                            onValueChange = { referenceId = it },
                            label = "Reference / UTR ID (Optional)",
                            placeholder = "e.g. UTR55443322"
                        )
                    }
                }

                VyavsaTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = "Note (Optional)",
                    placeholder = "e.g. Partial due settlement"
                )

                if (validationError != null) {
                    Text(
                        text = validationError!!,
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.error)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val trimmed = amount.trim()
                    val parsed = trimmed.toDoubleOrNull()
                    if (trimmed.isBlank() || parsed == null || parsed <= 0.0) {
                        validationError = "Please enter a valid positive collection amount"
                        return@TextButton
                    }
                    if (currentDueVal > 0.0 && parsed > currentDueVal) {
                        validationError = "Collection amount cannot exceed customer's outstanding balance (₹${customer.currentBalance})"
                        return@TextButton
                    }
                    if (method == "bank" && (selectedBankId.isNullOrBlank() || banks.isEmpty())) {
                        validationError = "Please select a bank account for bank collection"
                        return@TextButton
                    }
                    val selectedBank = if (method == "bank") banks.firstOrNull { it.id == selectedBankId } else null
                    onConfirm(
                        trimmed,
                        method,
                        if (method == "bank") selectedBankId else null,
                        selectedBank?.bankName,
                        referenceId.trim().ifBlank { null },
                        note.trim().ifBlank { null }
                    )
                },
                enabled = amount.isNotBlank() && (method == "cash" || (banks.isNotEmpty() && selectedBankId != null))
            ) {
                Text("Record Collection", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun RecordSupplierPaymentDialog(
    supplier: Supplier,
    banks: List<TenantBank> = emptyList(),
    onDismiss: () -> Unit,
    onConfirm: (amount: String, method: String, bankId: String?, bankName: String?, referenceId: String?, note: String?) -> Unit
) {
    var amount by remember { mutableStateOf(supplier.outstandingPayable) }
    var method by remember { mutableStateOf("cash") }
    var selectedBankId by remember(banks) { mutableStateOf(banks.firstOrNull()?.id) }
    var referenceId by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Pay Supplier: ${supplier.displayName}", fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Crimson50,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Outstanding Payable: ${FormatUtils.formatCurrency(supplier.outstandingPayable)}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = Crimson600),
                        modifier = Modifier.padding(12.dp)
                    )
                }

                VyavsaTextField(
                    value = amount,
                    onValueChange = {
                        amount = it
                        validationError = null
                    },
                    label = "Payment Amount (₹) *",
                    placeholder = "0.00",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )

                Text(
                    text = "Payment Mode *",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )

                VyavsaSegmentedTabs(
                    tabs = listOf("Cash Till", "Bank Account"),
                    selectedIndex = if (method == "cash") 0 else 1,
                    onTabSelected = { idx ->
                        method = if (idx == 0) "cash" else "bank"
                        validationError = null
                        if (method == "bank" && selectedBankId == null && banks.isNotEmpty()) {
                            selectedBankId = banks.first().id
                        }
                    }
                )

                AnimatedVisibility(
                    visible = method == "bank",
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Paid From Bank Account *",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )

                        if (banks.isEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Amber50,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccountBalance,
                                        contentDescription = null,
                                        tint = Amber600,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "No bank accounts available. Please configure a bank in Bank Accounts.",
                                        style = MaterialTheme.typography.bodySmall.copy(color = Amber600)
                                    )
                                }
                            }
                        } else {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                            ) {
                                banks.forEach { bank ->
                                    val isSelected = selectedBankId == bank.id
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            selectedBankId = bank.id
                                            validationError = null
                                        },
                                        shape = CircleShape,
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.AccountBalance,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        ),
                                        label = {
                                            val labelText = if (!bank.accountNumber.isNullOrBlank()) {
                                                "${bank.bankName} (..${bank.accountNumber.takeLast(4)})"
                                            } else {
                                                bank.bankName
                                            }
                                            Text(labelText, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                        }
                                    )
                                }
                            }
                        }

                        VyavsaTextField(
                            value = referenceId,
                            onValueChange = { referenceId = it },
                            label = "Reference / UTR ID (Optional)",
                            placeholder = "e.g. UTR99887766"
                        )
                    }
                }

                VyavsaTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = "Note (Optional)",
                    placeholder = "e.g. Supplier invoice payment"
                )

                if (validationError != null) {
                    Text(
                        text = validationError!!,
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.error)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val trimmed = amount.trim()
                    val parsed = trimmed.toDoubleOrNull()
                    if (trimmed.isBlank() || parsed == null || parsed <= 0.0) {
                        validationError = "Please enter a valid positive payment amount"
                        return@TextButton
                    }
                    if (method == "bank" && (selectedBankId.isNullOrBlank() || banks.isEmpty())) {
                        validationError = "Please select a bank account for bank payment"
                        return@TextButton
                    }
                    val selectedBank = if (method == "bank") banks.firstOrNull { it.id == selectedBankId } else null
                    onConfirm(
                        trimmed,
                        method,
                        if (method == "bank") selectedBankId else null,
                        selectedBank?.bankName,
                        referenceId.trim().ifBlank { null },
                        note.trim().ifBlank { null }
                    )
                },
                enabled = amount.isNotBlank() && (method == "cash" || (banks.isNotEmpty() && selectedBankId != null))
            ) {
                Text("Disburse Payment", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun CollectCustomerAdvanceDialog(
    customer: Customer,
    banks: List<TenantBank> = emptyList(),
    onDismiss: () -> Unit,
    onConfirm: (amount: String, method: String, bankId: String?, bankName: String?, referenceId: String?, note: String?) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var method by remember { mutableStateOf("cash") }
    var selectedBankId by remember(banks) { mutableStateOf(banks.firstOrNull()?.id) }
    var referenceId by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }

    val currentDueVal = customer.currentBalance.toDoubleOrNull() ?: 0.0
    val currentAdvVal = customer.advanceBalance.toDoubleOrNull() ?: 0.0
    val enteredAmt = amount.trim().toDoubleOrNull() ?: 0.0
    val settlesDues = minOf(currentDueVal, enteredAmt)
    val remExcess = (enteredAmt - settlesDues).coerceAtLeast(0.0)
    val resultingDue = (currentDueVal - settlesDues).coerceAtLeast(0.0)
    val resultingAdv = currentAdvVal + remExcess

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Collect Advance: ${customer.customerName}", fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Balance Status Banner
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Current Outstanding Due:", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                            Text(FormatUtils.formatCurrency(currentDueVal.toString()), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = if (currentDueVal > 0) Crimson600 else Emerald600))
                        }
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Available Advance Balance:", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                            Text(FormatUtils.formatCurrency(currentAdvVal.toString()), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = Blue600))
                        }
                    }
                }

                VyavsaTextField(
                    value = amount,
                    onValueChange = {
                        amount = it
                        validationError = null
                    },
                    label = "Advance Amount (₹) *",
                    placeholder = "0.00",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )

                if (enteredAmt > 0.0) {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Emerald50.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Emerald600.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Backend Allocation Preview:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Emerald600))
                            if (settlesDues > 0.0) {
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("↳ Settles Existing Dues FIFO:", style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                                    Text(FormatUtils.formatCurrency(settlesDues.toString()), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Emerald600))
                                }
                            }
                            if (remExcess > 0.0) {
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("↳ Added to Advance Balance:", style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                                    Text(FormatUtils.formatCurrency(remExcess.toString()), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Blue600))
                                }
                            }
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("Resulting Outstanding Due:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold))
                                Text(FormatUtils.formatCurrency(resultingDue.toString()), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                            }
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("Resulting Advance Balance:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold))
                                Text(FormatUtils.formatCurrency(resultingAdv.toString()), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Blue600))
                            }
                        }
                    }
                }

                Text(
                    text = "Deposit Mode *",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )

                VyavsaSegmentedTabs(
                    tabs = listOf("Cash Till", "Bank Account"),
                    selectedIndex = if (method == "cash") 0 else 1,
                    onTabSelected = { idx ->
                        method = if (idx == 0) "cash" else "bank"
                        validationError = null
                        if (method == "bank" && selectedBankId == null && banks.isNotEmpty()) {
                            selectedBankId = banks.first().id
                        }
                    }
                )

                AnimatedVisibility(
                    visible = method == "bank",
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Deposit Into Bank Account *",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )

                        if (banks.isEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Amber50,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.AccountBalance, contentDescription = null, tint = Amber600, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = "No bank accounts found.", style = MaterialTheme.typography.bodySmall.copy(color = Amber600))
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                banks.forEach { bank ->
                                    val isSelected = bank.id == selectedBankId
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { selectedBankId = bank.id },
                                        label = {
                                            val labelText = if (!bank.accountNumber.isNullOrBlank()) {
                                                "${bank.bankName} (..${bank.accountNumber.takeLast(4)})"
                                            } else {
                                                bank.bankName
                                            }
                                            Text(
                                                text = labelText,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
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
                    }
                }

                VyavsaTextField(
                    value = referenceId,
                    onValueChange = { referenceId = it },
                    label = "Reference / UTR ID (Optional)",
                    placeholder = "e.g. UTR-9876543210"
                )

                VyavsaTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = "Note (Optional)",
                    placeholder = "e.g. Advance for upcoming route supply"
                )

                if (validationError != null) {
                    ErrorBanner(message = validationError!!)
                }
            }
        },
        confirmButton = {
            VyavsaButton(
                text = "Record Advance",
                onClick = {
                    val parsedAmt = amount.trim().toDoubleOrNull()
                    if (parsedAmt == null || parsedAmt <= 0.0) {
                        validationError = "Please enter a valid advance amount greater than ₹0"
                        return@VyavsaButton
                    }
                    if (method == "bank" && selectedBankId.isNullOrBlank()) {
                        validationError = "Please select a bank account"
                        return@VyavsaButton
                    }
                    val bankObj = banks.find { it.id == selectedBankId }
                    onConfirm(
                        amount.trim(),
                        method,
                        if (method == "bank") selectedBankId else null,
                        if (method == "bank") bankObj?.bankName else null,
                        referenceId.trim().ifBlank { null },
                        note.trim().ifBlank { null }
                    )
                }
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun CustomerAdvanceReceiptDialog(
    response: CustomerAdvanceResponse,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Column {
                Text("Advance Collection Receipt", fontWeight = FontWeight.ExtraBold)
                if (!response.receiptNumber.isNullOrBlank()) {
                    Text("Receipt #${response.receiptNumber}", style = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold))
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Customer:", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                            Text(response.customerName, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                        }
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Date:", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                            Text(FormatUtils.formatDateForDisplay(response.paymentDate), style = MaterialTheme.typography.bodySmall)
                        }
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Payment Mode:", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                            Text(response.paymentMethod.uppercase(), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                        }
                        if (!response.referenceId.isNullOrBlank()) {
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("Ref / UTR:", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                                Text(response.referenceId, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Emerald50.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Emerald600.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Total Advance Collected:", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                            Text(FormatUtils.formatCurrency(response.amount), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.ExtraBold, color = Emerald600))
                        }
                        val settled = response.settledDues.toDoubleOrNull() ?: 0.0
                        if (settled > 0.0) {
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("↳ Settled Existing Dues:", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                                Text("-${FormatUtils.formatCurrency(settled.toString())}", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = Emerald600))
                            }
                        }
                        val avail = response.availableAmount.toDoubleOrNull() ?: 0.0
                        if (avail > 0.0) {
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("↳ Added to Advance Balance:", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                                Text("+${FormatUtils.formatCurrency(avail.toString())}", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = Blue600))
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Updated Customer Due:", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                            Text(FormatUtils.formatCurrency(response.customerBalance), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                        }
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Updated Advance Balance:", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                            Text(FormatUtils.formatCurrency(response.advanceBalance), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = Blue600))
                        }
                    }
                }
            }
        },
        confirmButton = {
            VyavsaButton(text = "Done", onClick = onDismiss)
        }
    )
}

@Composable
fun CustomerPaymentReceiptDialog(
    response: CustomerPaymentResponse,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Column {
                Text("Payment Receipt", fontWeight = FontWeight.ExtraBold)
                if (!response.receiptNumber.isNullOrBlank()) {
                    Text("Receipt #${response.receiptNumber}", style = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold))
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Customer:", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                            Text(response.customerName, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                        }
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Date:", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                            Text(FormatUtils.formatDateForDisplay(response.paymentDate), style = MaterialTheme.typography.bodySmall)
                        }
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Payment Mode:", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                            Text(response.paymentMethod.uppercase(), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                        }
                        if (!response.referenceId.isNullOrBlank()) {
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("Ref / UTR:", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                                Text(response.referenceId, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Emerald50.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Emerald600.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Amount Collected:", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                            Text(FormatUtils.formatCurrency(response.amount), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.ExtraBold, color = Emerald600))
                        }
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Previous Due Balance:", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                            Text(FormatUtils.formatCurrency(response.previousBalance), style = MaterialTheme.typography.bodySmall)
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Updated Outstanding Due:", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                            Text(FormatUtils.formatCurrency(response.customerBalance), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = if ((response.customerBalance.toDoubleOrNull() ?: 0.0) > 0) Crimson600 else Emerald600))
                        }
                    }
                }
            }
        },
        confirmButton = {
            VyavsaButton(text = "Done", onClick = onDismiss)
        }
    )
}
