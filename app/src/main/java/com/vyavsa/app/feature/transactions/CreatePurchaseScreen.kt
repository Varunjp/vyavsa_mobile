package com.vyavsa.app.feature.transactions

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.vyavsa.app.core.common.FormatUtils
import com.vyavsa.app.core.common.UiState
import com.vyavsa.app.core.designsystem.Amber50
import com.vyavsa.app.core.designsystem.Amber600
import com.vyavsa.app.core.designsystem.Blue50
import com.vyavsa.app.core.designsystem.Blue600
import com.vyavsa.app.core.designsystem.Crimson600
import com.vyavsa.app.core.designsystem.Emerald600
import com.vyavsa.app.core.designsystem.ErrorBanner
import com.vyavsa.app.core.designsystem.VyavsaButton
import com.vyavsa.app.core.designsystem.VyavsaTextField
import com.vyavsa.app.core.designsystem.VyavsaTopAppBar
import com.vyavsa.app.core.model.BankPaymentSplit
import com.vyavsa.app.core.model.Customer
import com.vyavsa.app.core.model.TenantBank

data class BankPaymentRowState(
    var bankId: String = "",
    var amount: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePurchaseScreen(
    viewModel: TransactionsViewModel,
    onPurchaseCreated: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val createActionState by viewModel.createActionState.collectAsState()
    val customersState by viewModel.customersState.collectAsState()
    val banksState by viewModel.banksState.collectAsState()
    val focusManager = LocalFocusManager.current

    var selectedCustomer by remember { mutableStateOf<Customer?>(null) }
    var customerDropdownExpanded by remember { mutableStateOf(false) }

    var item by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("1") }
    var unitPrice by remember { mutableStateOf("") }
    var totalAmount by remember { mutableStateOf("") }

    var cashAmount by remember { mutableStateOf("0.00") }
    val bankRows = remember { mutableStateListOf<BankPaymentRowState>() }

    val customers = (customersState as? UiState.Success)?.data ?: emptyList()
    val banks = (banksState as? UiState.Success)?.data ?: emptyList()

    LaunchedEffect(createActionState) {
        if (createActionState is UiState.Success) {
            onPurchaseCreated()
            viewModel.resetCreateState()
        }
    }

    // Auto-calculate total if unit price and quantity change
    fun updateCalculatedTotal() {
        val qty = quantity.toIntOrNull() ?: 0
        val price = unitPrice.toDoubleOrNull()
        if (qty > 0 && price != null && price > 0.0) {
            totalAmount = String.format(java.util.Locale.US, "%.2f", qty * price)
        }
    }

    val totalBillVal = totalAmount.toDoubleOrNull() ?: 0.0
    val cashVal = cashAmount.toDoubleOrNull() ?: 0.0
    val bankSumVal = bankRows.sumOf { it.amount.toDoubleOrNull() ?: 0.0 }
    val totalPaidVal = cashVal + bankSumVal
    val pendingVal = (totalBillVal - totalPaidVal).coerceAtLeast(0.0)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            VyavsaTopAppBar(
                title = "Record Purchase",
                subtitle = "Log vendor invoice, raw materials & stock intake",
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
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (createActionState is UiState.Error) {
                    ErrorBanner(
                        message = (createActionState as UiState.Error).message,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                Card(
                    shape = RoundedCornerShape(26.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f), RoundedCornerShape(26.dp))
                ) {
                    Column(modifier = Modifier.padding(22.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Amber50)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ShoppingCart,
                                    contentDescription = null,
                                    tint = Amber600,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Purchase & Vendor Information",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                                )
                                Text(
                                    text = "Records payable to supplier & updates inventory",
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Supplier Dropdown (Naturally aligned with floating label matching VyavsaTextField)
                        ExposedDropdownMenuBox(
                            expanded = customerDropdownExpanded,
                            onExpandedChange = { customerDropdownExpanded = !customerDropdownExpanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = selectedCustomer?.customerName ?: "Walk-in Supplier / Direct Vendor",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Supplier / Vendor") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = customerDropdownExpanded) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
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
                                DropdownMenuItem(
                                    text = { Text("Walk-in Supplier / Direct Vendor (No Account)") },
                                    onClick = {
                                        selectedCustomer = null
                                        customerDropdownExpanded = false
                                    }
                                )
                                customers.forEach { cust ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(cust.customerName, fontWeight = FontWeight.Bold)
                                                if (cust.outstandingPayable != "0.00") {
                                                    Text(
                                                        "Payable: ${FormatUtils.formatCurrency(cust.outstandingPayable)}",
                                                        style = MaterialTheme.typography.labelSmall.copy(color = Crimson600)
                                                    )
                                                }
                                            }
                                        },
                                        onClick = {
                                            selectedCustomer = cust
                                            customerDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        AnimatedVisibility(
                            visible = selectedCustomer != null,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column {
                                Spacer(modifier = Modifier.height(10.dp))
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = Blue50,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Current Outstanding Payable",
                                            style = MaterialTheme.typography.bodySmall.copy(color = Blue600, fontWeight = FontWeight.Medium)
                                        )
                                        Text(
                                            text = FormatUtils.formatCurrency(selectedCustomer?.outstandingPayable ?: "0.00"),
                                            style = MaterialTheme.typography.bodySmall.copy(color = Blue600, fontWeight = FontWeight.ExtraBold)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Item Description
                        VyavsaTextField(
                            value = item,
                            onValueChange = { item = it },
                            label = "Item Description / Raw Material *",
                            placeholder = "e.g. Cotton Yarn 40s, Packing Boxes, Steel Rods",
                            leadingIcon = Icons.Default.Description,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Quantity & Unit Price Row
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            VyavsaTextField(
                                value = quantity,
                                onValueChange = {
                                    quantity = it
                                    updateCalculatedTotal()
                                },
                                label = "Quantity *",
                                placeholder = "1",
                                leadingIcon = Icons.Default.Inventory2,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                                modifier = Modifier.weight(1f)
                            )

                            VyavsaTextField(
                                value = unitPrice,
                                onValueChange = {
                                    unitPrice = it
                                    updateCalculatedTotal()
                                },
                                label = "Unit Price (₹)",
                                placeholder = "0.00",
                                leadingIcon = Icons.Default.AttachMoney,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Total Bill Amount
                        VyavsaTextField(
                            value = totalAmount,
                            onValueChange = { totalAmount = it },
                            label = "Total Purchase Bill Amount (₹) *",
                            placeholder = "0.00",
                            leadingIcon = Icons.Default.AttachMoney,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done)
                        )
                    }
                }

                // Payment Breakdown Card
                Card(
                    shape = RoundedCornerShape(26.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f), RoundedCornerShape(26.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .padding(22.dp)
                            .animateContentSize()
                    ) {
                        Text(
                            text = "Payment Settlement Breakdown",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                        )
                        Text(
                            text = "Specify amount paid immediately via cash or bank accounts",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // Cash Payment
                        VyavsaTextField(
                            value = cashAmount,
                            onValueChange = { cashAmount = it },
                            label = "Paid via Cash Drawer (₹)",
                            placeholder = "0.00",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Bank Payments Split Section
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Paid via Bank Account(s)",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            )
                            if (banks.isNotEmpty()) {
                                OutlinedButton(
                                    onClick = {
                                        bankRows.add(BankPaymentRowState(bankId = banks.firstOrNull()?.id ?: "", amount = ""))
                                    },
                                    shape = CircleShape
                                ) {
                                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add Bank", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                }
                            }
                        }

                        if (bankRows.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            bankRows.forEachIndexed { index, rowState ->
                                BankPaymentRowItem(
                                    rowState = rowState,
                                    banks = banks,
                                    onDelete = { bankRows.removeAt(index) },
                                    onUpdate = { updated -> bankRows[index] = updated }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(16.dp))

                        // Live Settlement Summary
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Paid Now:", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                            Text(
                                FormatUtils.formatCurrency(String.format(java.util.Locale.US, "%.2f", totalPaidVal)),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.ExtraBold, color = Emerald600)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Remaining Due (Payable):", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                            Text(
                                FormatUtils.formatCurrency(String.format(java.util.Locale.US, "%.2f", pendingVal)),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (pendingVal > 0) Crimson600 else MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        VyavsaButton(
                            text = "Record Purchase",
                            onClick = {
                                focusManager.clearFocus()
                                val bankSplits = bankRows.map {
                                    BankPaymentSplit(
                                        bankId = it.bankId,
                                        amount = it.amount.ifBlank { "0.00" }
                                    )
                                }
                                viewModel.createPurchase(
                                    customerId = selectedCustomer?.id,
                                    item = item,
                                    quantity = quantity,
                                    totalAmount = totalAmount,
                                    cashAmount = cashAmount,
                                    bankPayments = bankSplits
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BankPaymentRowItem(
    rowState: BankPaymentRowState,
    banks: List<TenantBank>,
    onDelete: () -> Unit,
    onUpdate: (BankPaymentRowState) -> Unit
) {
    var dropdownExpanded by remember { mutableStateOf(false) }
    val currentBank = banks.firstOrNull { it.id == rowState.bankId } ?: banks.firstOrNull()

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ExposedDropdownMenuBox(
            expanded = dropdownExpanded,
            onExpandedChange = { dropdownExpanded = !dropdownExpanded },
            modifier = Modifier.weight(1.25f)
        ) {
            OutlinedTextField(
                value = currentBank?.bankName ?: "Select Bank",
                onValueChange = {},
                readOnly = true,
                label = { Text("Bank Account") },
                singleLine = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                shape = RoundedCornerShape(16.dp),
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
                expanded = dropdownExpanded,
                onDismissRequest = { dropdownExpanded = false }
            ) {
                banks.forEach { bank ->
                    DropdownMenuItem(
                        text = { Text(bank.bankName) },
                        onClick = {
                            onUpdate(rowState.copy(bankId = bank.id))
                            dropdownExpanded = false
                        }
                    )
                }
            }
        }

        OutlinedTextField(
            value = rowState.amount,
            onValueChange = { onUpdate(rowState.copy(amount = it)) },
            label = { Text("Amount (₹)") },
            placeholder = { Text("0.00") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
            ),
            modifier = Modifier.weight(1f)
        )

        IconButton(onClick = onDelete) {
            Icon(imageVector = Icons.Default.Close, contentDescription = "Remove Bank", tint = Crimson600)
        }
    }
}
