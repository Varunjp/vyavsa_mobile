@file:Suppress("DEPRECATION")
package com.vyavsa.app.feature.bank

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.vyavsa.app.core.common.FormatUtils
import com.vyavsa.app.core.common.UiState
import com.vyavsa.app.core.designsystem.Blue600
import com.vyavsa.app.core.designsystem.Emerald50
import com.vyavsa.app.core.designsystem.Emerald600
import com.vyavsa.app.core.designsystem.EmptyStateView
import com.vyavsa.app.core.designsystem.ErrorBanner
import com.vyavsa.app.core.designsystem.StatBadge
import com.vyavsa.app.core.designsystem.VyavsaButton
import com.vyavsa.app.core.designsystem.VyavsaGradients
import com.vyavsa.app.core.designsystem.VyavsaTextField
import com.vyavsa.app.core.designsystem.VyavsaTopAppBar
import com.vyavsa.app.core.model.TenantBank

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BankAccountsScreen(
    viewModel: BankViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val banksState by viewModel.banksState.collectAsState()
    val actionState by viewModel.actionState.collectAsState()
    val isAdmin = viewModel.isTenantAdmin

    var showAddBankDialog by remember { mutableStateOf(false) }
    var editingBank by remember { mutableStateOf<TenantBank?>(null) }
    var deletingBank by remember { mutableStateOf<TenantBank?>(null) }

    LaunchedEffect(actionState) {
        if (actionState is UiState.Success) {
            showAddBankDialog = false
            editingBank = null
            deletingBank = null
            viewModel.resetActionState()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            VyavsaTopAppBar(
                title = "Bank Accounts",
                subtitle = "Manage bank accounts & track ledger balances",
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                onNavigationClick = onNavigateBack,
                actions = {
                    FilledTonalIconButton(
                        onClick = { viewModel.loadBanks() },
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh", modifier = Modifier.size(20.dp))
                    }
                }
            )
        },
        floatingActionButton = {
            if (isAdmin) {
                ExtendedFloatingActionButton(
                    onClick = { showAddBankDialog = true },
                    shape = RoundedCornerShape(20.dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    icon = { Icon(imageVector = Icons.Default.Add, contentDescription = null) },
                    text = { Text("Add Bank", fontWeight = FontWeight.Bold) }
                )
            }
        },
        modifier = modifier
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (val state = banksState) {
                is UiState.Loading -> {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                is UiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        ErrorBanner(message = state.message)
                        Spacer(modifier = Modifier.height(16.dp))
                        VyavsaButton(
                            text = "Retry",
                            onClick = { viewModel.loadBanks() },
                            modifier = Modifier.width(160.dp)
                        )
                    }
                }
                is UiState.Empty -> {
                    EmptyStateView(
                        title = "No Bank Accounts",
                        description = "Add bank accounts to monitor electronic payments, bank collections, and current balances",
                        icon = Icons.Default.AccountBalance,
                        actionText = if (isAdmin) "Add Bank Account" else null,
                        onActionClick = { showAddBankDialog = true }
                    )
                }
                is UiState.Success -> {
                    val banks = state.data
                    val totalBankBalance = remember(banks) {
                        banks.fold(0.0) { acc, b ->
                            acc + (b.currentBalance.toDoubleOrNull() ?: 0.0)
                        }
                    }

                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Total Bank Balance Hero Card
                        item(key = "bank_total_hero") {
                            BankTotalHeroCard(totalBalance = totalBankBalance, totalAccounts = banks.size)
                        }

                        // Bank Accounts List
                        items(banks, key = { it.id }) { bank ->
                            BankCard(
                                bank = bank,
                                isAdmin = isAdmin,
                                onEdit = { editingBank = bank },
                                onDelete = { deletingBank = bank },
                                modifier = Modifier.animateItemPlacement()
                            )
                        }

                        item(key = "bank_fab_spacer") {
                            Spacer(modifier = Modifier.height(80.dp))
                        }
                    }
                }
                is UiState.Idle -> Unit
            }
        }
    }

    // Add Bank Dialog
    if (showAddBankDialog) {
        AddBankDialog(
            actionState = actionState,
            onDismiss = {
                showAddBankDialog = false
                viewModel.resetActionState()
            },
            onSubmit = { name, acc, ifsc, opening ->
                viewModel.createBank(name, acc, ifsc, opening)
            }
        )
    }

    // Edit Bank Dialog
    editingBank?.let { bank ->
        EditBankDialog(
            bank = bank,
            actionState = actionState,
            onDismiss = {
                editingBank = null
                viewModel.resetActionState()
            },
            onSubmit = { name, acc, ifsc, status ->
                viewModel.updateBank(bank.id, name, acc, ifsc, status)
            }
        )
    }

    // Delete Bank Confirmation Dialog
    deletingBank?.let { bank ->
        AlertDialog(
            onDismissRequest = { deletingBank = null },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Delete Bank Account", fontWeight = FontWeight.ExtraBold) },
            text = { Text("Are you sure you want to delete '${bank.bankName}'? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.deleteBank(bank.id) },
                    enabled = actionState !is UiState.Loading
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { deletingBank = null },
                    enabled = actionState !is UiState.Loading
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun BankTotalHeroCard(
    totalBalance: Double,
    totalAccounts: Int,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(VyavsaGradients.BlueHeroBrush)
                .padding(22.dp)
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White.copy(alpha = 0.18f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalance,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Total Bank Liquidity",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                            Text(
                                text = "$totalAccounts active ${if (totalAccounts == 1) "account" else "accounts"}",
                                style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.8f))
                            )
                        }
                    }
                    Surface(
                        color = Color.White.copy(alpha = 0.18f),
                        shape = CircleShape,
                        border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.25f))
                    ) {
                        Text(
                            text = "Bank Ledger",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = FormatUtils.formatCurrency(totalBalance.toString()),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                )
            }
        }
    }
}

@Composable
private fun BankCard(
    bank: TenantBank,
    isAdmin: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f), RoundedCornerShape(22.dp))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = bank.bankName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        StatBadge(
                            text = bank.status.uppercase(),
                            backgroundColor = if (bank.status == "active") Emerald50 else MaterialTheme.colorScheme.surfaceContainerHigh,
                            textColor = if (bank.status == "active") Emerald600 else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (!bank.accountNumber.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "A/C: ${bank.accountNumber}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }

                    if (!bank.ifsc.isNullOrBlank()) {
                        Text(
                            text = "IFSC: ${bank.ifsc}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }

                // Balance display
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Current Balance",
                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = FormatUtils.formatCurrency(bank.currentBalance),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = Blue600
                        )
                    )
                }
            }

            if (isAdmin) {
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(
                        onClick = onEdit,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Edit", fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    TextButton(
                        onClick = onDelete,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun AddBankDialog(
    actionState: UiState<Unit>,
    onDismiss: () -> Unit,
    onSubmit: (name: String, accountNumber: String, ifsc: String, openingBalance: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var account by remember { mutableStateOf("") }
    var ifsc by remember { mutableStateOf("") }
    var opening by remember { mutableStateOf("0.00") }

    AlertDialog(
        onDismissRequest = { if (actionState !is UiState.Loading) onDismiss() },
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = "Add Bank Account",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (actionState is UiState.Error) {
                    ErrorBanner(message = actionState.message)
                }

                VyavsaTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Bank & Branch Name *",
                    placeholder = "e.g. HDFC Bank - Main Branch"
                )

                VyavsaTextField(
                    value = account,
                    onValueChange = { account = it },
                    label = "Account Number",
                    placeholder = "e.g. 50100234567890",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next)
                )

                VyavsaTextField(
                    value = ifsc,
                    onValueChange = { ifsc = it },
                    label = "IFSC Code",
                    placeholder = "e.g. HDFC0001234",
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )

                VyavsaTextField(
                    value = opening,
                    onValueChange = { opening = it },
                    label = "Opening Balance (₹)",
                    placeholder = "0.00",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done)
                )
            }
        },
        confirmButton = {
            VyavsaButton(
                text = "Save Account",
                onClick = { onSubmit(name, account, ifsc, opening) },
                isLoading = actionState is UiState.Loading,
                modifier = Modifier.padding(end = 8.dp)
            )
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = actionState !is UiState.Loading
            ) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditBankDialog(
    bank: TenantBank,
    actionState: UiState<Unit>,
    onDismiss: () -> Unit,
    onSubmit: (name: String, accountNumber: String, ifsc: String, status: String) -> Unit
) {
    var name by remember { mutableStateOf(bank.bankName) }
    var account by remember { mutableStateOf(bank.accountNumber ?: "") }
    var ifsc by remember { mutableStateOf(bank.ifsc ?: "") }
    var status by remember { mutableStateOf(bank.status) }
    var statusDropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (actionState !is UiState.Loading) onDismiss() },
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = "Edit Bank Account",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (actionState is UiState.Error) {
                    ErrorBanner(message = actionState.message)
                }

                VyavsaTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Bank Name *",
                    placeholder = "e.g. HDFC Bank"
                )

                VyavsaTextField(
                    value = account,
                    onValueChange = { account = it },
                    label = "Account Number",
                    placeholder = "e.g. 50100234567890",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next)
                )

                VyavsaTextField(
                    value = ifsc,
                    onValueChange = { ifsc = it },
                    label = "IFSC Code",
                    placeholder = "e.g. HDFC0001234",
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                )

                ExposedDropdownMenuBox(
                    expanded = statusDropdownExpanded,
                    onExpandedChange = { statusDropdownExpanded = !statusDropdownExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = status.replaceFirstChar { it.uppercase() },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Status") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusDropdownExpanded) },
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
                        expanded = statusDropdownExpanded,
                        onDismissRequest = { statusDropdownExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Active") },
                            onClick = {
                                status = "active"
                                statusDropdownExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Inactive") },
                            onClick = {
                                status = "inactive"
                                statusDropdownExpanded = false
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            VyavsaButton(
                text = "Save Changes",
                onClick = { onSubmit(name, account, ifsc, status) },
                isLoading = actionState is UiState.Loading,
                modifier = Modifier.padding(end = 8.dp)
            )
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = actionState !is UiState.Loading
            ) {
                Text("Cancel")
            }
        }
    )
}
