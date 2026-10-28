package com.vyavsa.app.feature.dues

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
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
import com.vyavsa.app.core.model.Customer
import com.vyavsa.app.core.model.Supplier
import com.vyavsa.app.core.model.TenantBank

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DuesScreen(
    viewModel: DuesViewModel,
    onNavigateToStatement: (customerId: String, customerName: String) -> Unit,
    onNavigateToCustomerDetails: (customerId: String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val customersState by viewModel.customersState.collectAsState()
    val suppliersState by viewModel.suppliersState.collectAsState()
    val banksState by viewModel.banksState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val actionState by viewModel.actionState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    ObserveAsEvents(viewModel.uiEvents) { msg ->
        snackbarHostState.showSnackbar(msg.text)
    }

    var showAddCustomerDialog by remember { mutableStateOf(false) }
    var showAddSupplierDialog by remember { mutableStateOf(false) }
    var collectingCustomer by remember { mutableStateOf<Customer?>(null) }
    var payingSupplier by remember { mutableStateOf<Supplier?>(null) }

    val banks = (banksState as? UiState.Success)?.data ?: emptyList()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            VyavsaTopAppBar(
                title = "Khata & Ledger Accounts",
                subtitle = "Manage customer receivables & supplier payables"
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    if (selectedTab == DuesTab.CUSTOMERS) {
                        showAddCustomerDialog = true
                    } else {
                        showAddSupplierDialog = true
                    }
                },
                shape = RoundedCornerShape(20.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                icon = { Icon(imageVector = Icons.Default.Add, contentDescription = null) },
                text = {
                    Text(
                        if (selectedTab == DuesTab.CUSTOMERS) "Add Customer" else "Add Supplier",
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
            // Segmented Switcher for Customers vs Suppliers
            VyavsaSegmentedTabs(
                tabs = listOf("Customers (Receivables)", "Suppliers (Payables)"),
                selectedIndex = if (selectedTab == DuesTab.CUSTOMERS) 0 else 1,
                onTabSelected = { idx ->
                    viewModel.selectTab(if (idx == 0) DuesTab.CUSTOMERS else DuesTab.SUPPLIERS)
                },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )

            // Pill Search Input
            VyavsaSearchBar(
                query = searchQuery,
                onQueryChange = { viewModel.onSearchChange(it) },
                placeholder = if (selectedTab == DuesTab.CUSTOMERS) "Search customer name or phone..." else "Search supplier name or phone...",
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )

            if (actionState is UiState.Error) {
                ErrorBanner(
                    message = (actionState as UiState.Error).message,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )
            }

            if (selectedTab == DuesTab.CUSTOMERS) {
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

                            items(
                                items = customers,
                                key = { "cust_${it.id}" },
                                contentType = { "customer_item" }
                            ) { customer ->
                                CustomerDuesCard(
                                    customer = customer,
                                    onCollectPaymentClick = { collectingCustomer = customer },
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

                            items(
                                items = suppliers,
                                key = { "supp_${it.id}" },
                                contentType = { "supplier_item" }
                            ) { supplier ->
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
    onCollectPaymentClick: () -> Unit,
    onStatementClick: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val balanceAmount = customer.currentBalance.toDoubleOrNull() ?: 0.0

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
                    StatBadge(
                        text = if (balanceAmount > 0) "Due" else "Clear",
                        backgroundColor = if (balanceAmount > 0) Crimson50 else Emerald50,
                        textColor = if (balanceAmount > 0) Crimson600 else Emerald600
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (balanceAmount > 0) {
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
