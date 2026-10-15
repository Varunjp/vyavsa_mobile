@file:Suppress("DEPRECATION")
package com.vyavsa.app.feature.transactions

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.vyavsa.app.core.common.FormatUtils
import com.vyavsa.app.core.common.UiState
import com.vyavsa.app.core.designsystem.Blue50
import com.vyavsa.app.core.designsystem.Blue600
import com.vyavsa.app.core.designsystem.Crimson50
import com.vyavsa.app.core.designsystem.Crimson600
import com.vyavsa.app.core.designsystem.Emerald50
import com.vyavsa.app.core.designsystem.Emerald600
import com.vyavsa.app.core.designsystem.ErrorBanner
import com.vyavsa.app.core.designsystem.StatBadge
import com.vyavsa.app.core.designsystem.VyavsaButton
import com.vyavsa.app.core.designsystem.VyavsaOutlinedButton
import com.vyavsa.app.core.designsystem.VyavsaSegmentedTabs
import com.vyavsa.app.core.designsystem.VyavsaTextField
import com.vyavsa.app.core.designsystem.VyavsaTopAppBar
import com.vyavsa.app.core.model.BankPaymentSplit
import com.vyavsa.app.core.model.Customer
import com.vyavsa.app.core.model.TenantBank

@Immutable
data class BankPaymentEntry(
    val id: String = java.util.UUID.randomUUID().toString(),
    val bank: TenantBank? = null,
    val amount: String = "",
    val note: String? = null
)

@Composable
fun CreateLineSaleScreen(
    viewModel: TransactionsViewModel,
    onSaleCreated: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    CreateSaleScreen(
        viewModel = viewModel,
        initialSaleType = SaleType.LINE_SALE,
        onSaleCreated = onSaleCreated,
        onNavigateBack = onNavigateBack,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateSaleScreen(
    viewModel: TransactionsViewModel,
    initialSaleType: SaleType = SaleType.LINE_SALE,
    onSaleCreated: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedSaleType by remember { mutableStateOf(initialSaleType) }

    val createActionState by viewModel.createActionState.collectAsState()
    val customersState by viewModel.customersState.collectAsState()
    val banksState by viewModel.banksState.collectAsState()
    val focusManager = LocalFocusManager.current

    val customers = (customersState as? UiState.Success)?.data ?: emptyList()
    val availableBanks = remember(banksState) {
        (banksState as? UiState.Success)?.data?.filter { it.status == "active" || it.status.isBlank() } ?: emptyList()
    }
    val isLoadingBanks = banksState is UiState.Loading

    // Form fields for Line Sale
    var selectedCustomer by remember { mutableStateOf<Customer?>(null) }
    var customerDropdownExpanded by remember { mutableStateOf(false) }
    var route by remember { mutableStateOf("") }
    var salesman by remember { mutableStateOf("") }
    var lineTotalAmount by remember { mutableStateOf("") }
    var lineCashIn by remember { mutableStateOf("0.00") }
    var lineIncludeBank by remember { mutableStateOf(false) }
    var lineBankEntries by remember { mutableStateOf<List<BankPaymentEntry>>(emptyList()) }
    var note by remember { mutableStateOf("") }

    // Form fields for Counter Sale
    var counterItem by remember { mutableStateOf("") }
    var counterPrice by remember { mutableStateOf("") }
    var counterTotalAmount by remember { mutableStateOf("") }
    var counterCash by remember { mutableStateOf("0.00") }
    var counterIncludeBank by remember { mutableStateOf(false) }
    var counterBankEntries by remember { mutableStateOf<List<BankPaymentEntry>>(emptyList()) }

    LaunchedEffect(createActionState) {
        if (createActionState is UiState.Success) {
            onSaleCreated()
            viewModel.resetCreateState()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            VyavsaTopAppBar(
                title = if (selectedSaleType == SaleType.LINE_SALE) "Record Line Sale" else "Record Counter Sale",
                subtitle = if (selectedSaleType == SaleType.LINE_SALE) "Field route delivery & customer ledger update" else "Over-the-counter retail register",
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
                .imePadding()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                // Sale Type Segmented Selector
                VyavsaSegmentedTabs(
                    tabs = listOf("Line / Route Sale", "Counter / POS Sale"),
                    selectedIndex = if (selectedSaleType == SaleType.LINE_SALE) 0 else 1,
                    onTabSelected = { idx ->
                        selectedSaleType = if (idx == 0) SaleType.LINE_SALE else SaleType.COUNTER_SALE
                        viewModel.resetCreateState()
                    },
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                if (createActionState is UiState.Error) {
                    ErrorBanner(
                        message = (createActionState as UiState.Error).message,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                // Card container with smooth directional transition between Line and Counter Sale forms
                Card(
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f), RoundedCornerShape(28.dp))
                ) {
                    AnimatedContent(
                        targetState = selectedSaleType,
                        transitionSpec = {
                            val dir = if (targetState == SaleType.COUNTER_SALE) 1 else -1
                            (fadeIn(tween(220, easing = FastOutSlowInEasing)) +
                                slideInHorizontally(
                                    initialOffsetX = { (it * 0.08f * dir).toInt() },
                                    animationSpec = tween(240, easing = FastOutSlowInEasing)
                                )) togetherWith
                                (fadeOut(tween(160, easing = FastOutSlowInEasing)) +
                                    slideOutHorizontally(
                                        targetOffsetX = { (-it * 0.06f * dir).toInt() },
                                        animationSpec = tween(180, easing = FastOutSlowInEasing)
                                    ))
                        },
                        label = "SaleFormSwitchTransition"
                    ) { saleType ->
                        Column(modifier = Modifier.padding(22.dp)) {
                            if (saleType == SaleType.LINE_SALE) {
                                // ==========================================
                                // 1. Line Sale Form
                                // ==========================================
                                Text(
                                    text = "Line Sale Entry",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Deliveries & collections for route accounts. Difference is added to customer balance.",
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )

                                Spacer(modifier = Modifier.height(18.dp))

                                // Customer Selector (Aligned with VyavsaTextField style)
                                ExposedDropdownMenuBox(
                                    expanded = customerDropdownExpanded,
                                    onExpandedChange = { customerDropdownExpanded = !customerDropdownExpanded },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    OutlinedTextField(
                                        value = selectedCustomer?.customerName ?: "",
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Customer Account *") },
                                        placeholder = { Text("Select registered customer") },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Person,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = customerDropdownExpanded) },
                                        shape = RoundedCornerShape(18.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .menuAnchor()
                                    )

                                    ExposedDropdownMenu(
                                        expanded = customerDropdownExpanded,
                                        onDismissRequest = { customerDropdownExpanded = false }
                                    ) {
                                        if (customers.isEmpty()) {
                                            DropdownMenuItem(
                                                text = { Text("No customers found") },
                                                onClick = { customerDropdownExpanded = false }
                                            )
                                        } else {
                                            customers.forEach { customer ->
                                                DropdownMenuItem(
                                                    text = {
                                                        Column {
                                                            Text(customer.customerName, fontWeight = FontWeight.Bold)
                                                            Text(
                                                                text = "Balance: ${FormatUtils.formatCurrency(customer.currentBalance)}",
                                                                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                            )
                                                        }
                                                    },
                                                    onClick = {
                                                        selectedCustomer = customer
                                                        customerDropdownExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                // Customer Balance Preview Card
                                AnimatedVisibility(
                                    visible = selectedCustomer != null,
                                    enter = expandVertically() + fadeIn(),
                                    exit = shrinkVertically() + fadeOut()
                                ) {
                                    selectedCustomer?.let { cust ->
                                        val lineBankTotal = if (lineIncludeBank) lineBankEntries.sumOf { it.amount.toDoubleOrNull() ?: 0.0 } else 0.0
                                        Column {
                                            Spacer(modifier = Modifier.height(12.dp))
                                            CustomerBalancePreviewBox(
                                                currentBalance = cust.currentBalance,
                                                totalAmount = lineTotalAmount,
                                                cashIn = lineCashIn,
                                                bankAmount = lineBankTotal.toString()
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Route & Salesman Row
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.Top,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    VyavsaTextField(
                                        value = route,
                                        onValueChange = { route = it },
                                        label = "Delivery Route",
                                        placeholder = "e.g. North Route",
                                        modifier = Modifier.weight(1f)
                                    )
                                    VyavsaTextField(
                                        value = salesman,
                                        onValueChange = { salesman = it },
                                        label = "Salesman",
                                        placeholder = "e.g. Ramesh",
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Invoice Amount & Cash In
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.Top,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    VyavsaTextField(
                                        value = lineTotalAmount,
                                        onValueChange = { lineTotalAmount = it },
                                        label = "Invoice Total (₹) *",
                                        placeholder = "0.00",
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                                        modifier = Modifier.weight(1f)
                                    )
                                    VyavsaTextField(
                                        value = lineCashIn,
                                        onValueChange = { lineCashIn = it },
                                        label = "Cash Collected (₹)",
                                        placeholder = "0.00",
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Multi-Bank Payment Toggle & Selection
                                MultiBankPaymentSection(
                                    includeBank = lineIncludeBank,
                                    onIncludeBankChange = { lineIncludeBank = it },
                                    bankEntries = lineBankEntries,
                                    onBankEntriesChange = { lineBankEntries = it },
                                    availableBanks = availableBanks,
                                    isLoadingBanks = isLoadingBanks
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                // Line Sale Calculations Box
                                SaleCalculationBox(
                                    totalAmount = lineTotalAmount,
                                    cashAmount = lineCashIn,
                                    bankEntries = if (lineIncludeBank) lineBankEntries else emptyList(),
                                    isLineSale = true
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                VyavsaTextField(
                                    value = note,
                                    onValueChange = { note = it },
                                    label = "Note / Dispatch Reference",
                                    placeholder = "Optional delivery remarks",
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                                )

                                Spacer(modifier = Modifier.height(24.dp))

                                VyavsaButton(
                                    text = "Record Line Sale",
                                    onClick = {
                                        focusManager.clearFocus()
                                        val splits = if (lineIncludeBank) {
                                            lineBankEntries.map {
                                                BankPaymentSplit(
                                                    bankId = it.bank?.id ?: "",
                                                    bankName = it.bank?.bankName,
                                                    amount = it.amount.trim()
                                                )
                                            }
                                        } else {
                                            emptyList()
                                        }
                                        val bankTotal = splits.sumOf { it.amount.toDoubleOrNull() ?: 0.0 }
                                        viewModel.createLineSale(
                                            customerId = selectedCustomer?.id ?: "",
                                            route = route,
                                            salesman = salesman,
                                            totalAmount = lineTotalAmount,
                                            cashIn = lineCashIn,
                                            bankAmount = String.format(java.util.Locale.US, "%.2f", bankTotal),
                                            bankId = splits.firstOrNull()?.bankId,
                                            bankPayments = splits,
                                            note = note
                                        )
                                    },
                                    isLoading = createActionState is UiState.Loading
                                )

                            } else {
                                // ==========================================
                                // 2. Counter Sale Form
                                // ==========================================
                                Text(
                                    text = "Counter Sale Entry",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Instant over-the-counter sale. Payment via cash, bank transfer / UPI, or split.",
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )

                                Spacer(modifier = Modifier.height(18.dp))

                                VyavsaTextField(
                                    value = counterItem,
                                    onValueChange = { counterItem = it },
                                    label = "Item / Description *",
                                    placeholder = "e.g. Retail goods, Bill #108"
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.Top,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    VyavsaTextField(
                                        value = counterPrice,
                                        onValueChange = {
                                            counterPrice = it
                                            if (counterTotalAmount.isBlank()) counterTotalAmount = it
                                        },
                                        label = "Unit Price (₹)",
                                        placeholder = "0.00",
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                                        modifier = Modifier.weight(1f)
                                    )

                                    VyavsaTextField(
                                        value = counterTotalAmount,
                                        onValueChange = { counterTotalAmount = it },
                                        label = "Total Amount (₹) *",
                                        placeholder = "0.00",
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                VyavsaTextField(
                                    value = counterCash,
                                    onValueChange = { counterCash = it },
                                    label = "Physical Cash Received (₹)",
                                    placeholder = "0.00",
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next)
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                // Counter Bank Payment Section
                                MultiBankPaymentSection(
                                    includeBank = counterIncludeBank,
                                    onIncludeBankChange = { counterIncludeBank = it },
                                    bankEntries = counterBankEntries,
                                    onBankEntriesChange = { counterBankEntries = it },
                                    availableBanks = availableBanks,
                                    isLoadingBanks = isLoadingBanks
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                // Counter Sale Calculations Box
                                SaleCalculationBox(
                                    totalAmount = counterTotalAmount,
                                    cashAmount = counterCash,
                                    bankEntries = if (counterIncludeBank) counterBankEntries else emptyList(),
                                    isLineSale = false
                                )

                                Spacer(modifier = Modifier.height(24.dp))

                                VyavsaButton(
                                    text = "Record Counter Sale",
                                    onClick = {
                                        focusManager.clearFocus()
                                        val splits = if (counterIncludeBank) {
                                            counterBankEntries.map {
                                                BankPaymentSplit(
                                                    bankId = it.bank?.id ?: "",
                                                    bankName = it.bank?.bankName,
                                                    amount = it.amount.trim()
                                                )
                                            }
                                        } else {
                                            emptyList()
                                        }
                                        val bankTotal = splits.sumOf { it.amount.toDoubleOrNull() ?: 0.0 }
                                        viewModel.createCounterSale(
                                            item = counterItem,
                                            price = counterPrice.ifBlank { counterTotalAmount },
                                            totalAmount = counterTotalAmount,
                                            cash = counterCash,
                                            bankAmount = String.format(java.util.Locale.US, "%.2f", bankTotal),
                                            bankId = splits.firstOrNull()?.bankId,
                                            bankPayments = splits
                                        )
                                    },
                                    isLoading = createActionState is UiState.Loading
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MultiBankPaymentSection(
    includeBank: Boolean,
    onIncludeBankChange: (Boolean) -> Unit,
    bankEntries: List<BankPaymentEntry>,
    onBankEntriesChange: (List<BankPaymentEntry>) -> Unit,
    availableBanks: List<TenantBank>,
    isLoadingBanks: Boolean = false,
    modifier: Modifier = Modifier
) {
    val duplicateBankIds = remember(bankEntries) {
        bankEntries.mapNotNull { it.bank?.id?.ifBlank { null } }
            .groupingBy { it }
            .eachCount()
            .filter { it.value > 1 }
            .keys
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Blue50)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalance,
                            contentDescription = null,
                            tint = Blue600,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Bank / UPI Payments",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = if (includeBank) "Deposit directly to bank ledger accounts" else "Toggle to record bank / online collection",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                }

                Switch(
                    checked = includeBank,
                    onCheckedChange = { checked ->
                        onIncludeBankChange(checked)
                        if (checked && bankEntries.isEmpty() && availableBanks.isNotEmpty()) {
                            onBankEntriesChange(listOf(BankPaymentEntry(bank = availableBanks.first())))
                        } else if (!checked) {
                            onBankEntriesChange(emptyList())
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }

            AnimatedVisibility(
                visible = includeBank,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.padding(top = 14.dp)) {
                    if (isLoadingBanks) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Loading bank accounts...",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                    } else if (availableBanks.isEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
                                .padding(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "No registered bank accounts found. Please register a bank account in Bank Management first.",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.error)
                            )
                        }
                    } else {
                        // Render bank entries
                        bankEntries.forEachIndexed { index, entry ->
                            val isDuplicate = entry.bank?.id != null && entry.bank.id in duplicateBankIds
                            BankPaymentRowItem(
                                index = index,
                                entry = entry,
                                availableBanks = availableBanks,
                                isDuplicate = isDuplicate,
                                onSelectBank = { selected ->
                                    val updated = bankEntries.toMutableList()
                                    updated[index] = entry.copy(bank = selected)
                                    onBankEntriesChange(updated)
                                },
                                onAmountChange = { newAmount ->
                                    val updated = bankEntries.toMutableList()
                                    updated[index] = entry.copy(amount = newAmount)
                                    onBankEntriesChange(updated)
                                },
                                onRemove = {
                                    val updated = bankEntries.toMutableList()
                                    updated.removeAt(index)
                                    onBankEntriesChange(updated)
                                }
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        // Duplicate Warning Banner
                        if (duplicateBankIds.isNotEmpty()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Crimson50, RoundedCornerShape(12.dp))
                                    .border(1.dp, Crimson600.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Crimson600,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Each bank account can only be selected once per sale.",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Crimson600,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        // Add Bank Button
                        VyavsaOutlinedButton(
                            text = "+ Add Bank",
                            leadingIcon = Icons.Default.Add,
                            onClick = {
                                val usedBankIds = bankEntries.mapNotNull { it.bank?.id }.toSet()
                                val nextBank = availableBanks.firstOrNull { it.id !in usedBankIds } ?: availableBanks.firstOrNull()
                                onBankEntriesChange(bankEntries + BankPaymentEntry(bank = nextBank))
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Bank Allocation Total Summary
                        val bankTotal = bankEntries.sumOf { it.amount.toDoubleOrNull() ?: 0.0 }
                        if (bankTotal > 0.0) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp)
                            ) {
                                Text(
                                    text = "Total Bank Received (${bankEntries.size} ${if (bankEntries.size == 1) "account" else "accounts"}):",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                                Text(
                                    text = FormatUtils.formatCurrency(bankTotal.toString()),
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Blue600
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BankPaymentRowItem(
    index: Int,
    entry: BankPaymentEntry,
    availableBanks: List<TenantBank>,
    isDuplicate: Boolean,
    onSelectBank: (TenantBank) -> Unit,
    onAmountChange: (String) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            1.dp,
            if (isDuplicate) Crimson600.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(if (isDuplicate) Crimson50 else Blue50)
                    ) {
                        Text(
                            text = "${index + 1}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isDuplicate) Crimson600 else Blue600
                            )
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Bank Allocation #${index + 1}",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }

                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove Bank Entry",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Bank Account Dropdown
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = entry.bank?.let { "${it.bankName} (${FormatUtils.formatCurrency(it.currentBalance)})" } ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Select Bank Account *") },
                    placeholder = { Text("Choose receiving bank") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    shape = RoundedCornerShape(18.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        focusedBorderColor = if (isDuplicate) Crimson600 else MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = if (isDuplicate) Crimson600.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    availableBanks.forEach { bank ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(bank.bankName, fontWeight = FontWeight.Bold)
                                    Row {
                                        if (!bank.accountNumber.isNullOrBlank()) {
                                            Text(
                                                text = "A/C: ${bank.accountNumber} • ",
                                                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            )
                                        }
                                        Text(
                                            text = "Balance: ${FormatUtils.formatCurrency(bank.currentBalance)}",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Blue600
                                            )
                                        )
                                    }
                                }
                            },
                            onClick = {
                                onSelectBank(bank)
                                expanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            VyavsaTextField(
                value = entry.amount,
                onValueChange = onAmountChange,
                label = "Bank Amount Received (₹) *",
                placeholder = "0.00",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done)
            )
        }
    }
}

@Composable
private fun CustomerBalancePreviewBox(
    currentBalance: String,
    totalAmount: String,
    cashIn: String,
    bankAmount: String,
    modifier: Modifier = Modifier
) {
    val cur = currentBalance.toDoubleOrNull() ?: 0.0
    val total = totalAmount.toDoubleOrNull() ?: 0.0
    val cash = cashIn.toDoubleOrNull() ?: 0.0
    val bank = bankAmount.toDoubleOrNull() ?: 0.0
    val collected = cash + bank
    val saleDue = if (total > collected) total - collected else 0.0
    val resulting = cur + saleDue

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), RoundedCornerShape(18.dp))
    ) {
        Column(
            modifier = Modifier
                .padding(14.dp)
                .animateContentSize()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Current Customer Balance:",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
                StatBadge(
                    text = FormatUtils.formatCurrency(cur.toString()),
                    backgroundColor = if (cur > 0.0) Crimson50 else Emerald50,
                    textColor = if (cur > 0.0) Crimson600 else Emerald600
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "This Invoice Due (Unpaid):",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
                Text(
                    text = "+${FormatUtils.formatCurrency(saleDue.toString())}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = if (saleDue > 0.0) Crimson600 else Emerald600
                    )
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Resulting Account Balance:",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = FormatUtils.formatCurrency(resulting.toString()),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }
    }
}

@Composable
private fun SaleCalculationBox(
    totalAmount: String,
    cashAmount: String,
    bankEntries: List<BankPaymentEntry> = emptyList(),
    isLineSale: Boolean,
    modifier: Modifier = Modifier
) {
    val total = totalAmount.toDoubleOrNull() ?: 0.0
    val cash = cashAmount.toDoubleOrNull() ?: 0.0
    val bankTotal = bankEntries.sumOf { it.amount.toDoubleOrNull() ?: 0.0 }
    val collected = cash + bankTotal
    val due = if (total > collected) total - collected else 0.0
    val exceeds = total > 0.0 && collected > total

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (exceeds) Crimson50.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceContainerLow
        ),
        modifier = modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (exceeds) Crimson600.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant,
                RoundedCornerShape(18.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .padding(14.dp)
                .animateContentSize()
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cash Payment:", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                Text(FormatUtils.formatCurrency(cash.toString()), style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold))
            }

            Spacer(modifier = Modifier.height(4.dp))

            if (bankEntries.size <= 1) {
                val bankLabel = if (bankEntries.isNotEmpty() && bankEntries[0].bank != null) {
                    "Bank / UPI (${bankEntries[0].bank?.bankName}):"
                } else {
                    "Bank / UPI Payment:"
                }
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(bankLabel, style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                    Text(FormatUtils.formatCurrency(bankTotal.toString()), style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold))
                }
            } else {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Bank Payments Total (${bankEntries.size} accounts):",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                    )
                    Text(
                        FormatUtils.formatCurrency(bankTotal.toString()),
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = Blue600)
                    )
                }

                bankEntries.forEach { entry ->
                    val amt = entry.amount.toDoubleOrNull() ?: 0.0
                    val name = entry.bank?.bankName ?: "Unselected Bank"
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp, top = 2.dp)
                    ) {
                        Text(
                            text = "• $name:",
                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                        Text(
                            text = FormatUtils.formatCurrency(amt.toString()),
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Total Received (Cash + Bank):", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                Text(
                    text = FormatUtils.formatCurrency(collected.toString()),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (exceeds) Crimson600 else Emerald600
                    )
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isLineSale) "Due Balance (Customer Ledger):" else "Due / Credit Balance:",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
                Text(
                    text = FormatUtils.formatCurrency(due.toString()),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = if (due > 0.0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                )
            }

            if (!isLineSale) {
                val method = if (cash > 0.0 && bankTotal > 0.0) {
                    "SPLIT (Cash + Banks)"
                } else if (bankTotal > 0.0) {
                    "BANK / UPI"
                } else {
                    "CASH"
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Payment Method:", style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                    StatBadge(text = method, backgroundColor = Blue50, textColor = Blue600)
                }
            }

            if (exceeds) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "⚠️ Collected amount cannot exceed total sale amount (${FormatUtils.formatCurrency(total.toString())})",
                    style = MaterialTheme.typography.bodySmall.copy(color = Crimson600, fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}
