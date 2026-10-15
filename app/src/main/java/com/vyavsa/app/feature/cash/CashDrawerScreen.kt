@file:Suppress("DEPRECATION")
package com.vyavsa.app.feature.cash

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CallMade
import androidx.compose.material.icons.filled.CallReceived
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vyavsa.app.core.common.FormatUtils
import com.vyavsa.app.core.common.UiState
import com.vyavsa.app.core.designsystem.Amber50
import com.vyavsa.app.core.designsystem.Amber600
import com.vyavsa.app.core.designsystem.Emerald50
import com.vyavsa.app.core.designsystem.Emerald600
import com.vyavsa.app.core.designsystem.EmptyStateView
import com.vyavsa.app.core.designsystem.ErrorBanner
import com.vyavsa.app.core.designsystem.StatBadge
import com.vyavsa.app.core.designsystem.VyavsaButton
import com.vyavsa.app.core.designsystem.VyavsaOutlinedButton
import com.vyavsa.app.core.designsystem.VyavsaSectionHeader
import com.vyavsa.app.core.designsystem.VyavsaTextField
import com.vyavsa.app.core.designsystem.VyavsaTopAppBar
import com.vyavsa.app.core.model.CashTransaction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashDrawerScreen(
    viewModel: CashViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val balanceState by viewModel.balanceState.collectAsState()
    val transactionsState by viewModel.transactionsState.collectAsState()
    val actionState by viewModel.actionState.collectAsState()

    var showAddCashDialog by remember { mutableStateOf(false) }
    var showAdjustDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            VyavsaTopAppBar(
                title = "Cash Till Management",
                subtitle = "Physical drawer balance & audit log",
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                onNavigationClick = onNavigateBack
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
            if (actionState is UiState.Error) {
                ErrorBanner(
                    message = (actionState as UiState.Error).message,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }

            // Cash In Drawer Hero Card
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f), RoundedCornerShape(28.dp))
            ) {
                Column(modifier = Modifier.padding(22.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Authoritative Physical Cash",
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        StatBadge(text = "Live Till Balance")
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val balance = (balanceState as? UiState.Success)?.data?.cashBalance ?: "0.00"
                    Text(
                        text = FormatUtils.formatCurrency(balance),
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 34.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        VyavsaButton(
                            text = "Add Cash",
                            onClick = { showAddCashDialog = true },
                            leadingIcon = Icons.Default.Add,
                            modifier = Modifier.weight(1f)
                        )

                        VyavsaOutlinedButton(
                            text = "Reconcile",
                            onClick = { showAdjustDialog = true },
                            leadingIcon = Icons.Default.Tune,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            VyavsaSectionHeader(
                title = "Cash Drawer Activity Log",
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )

            // Transactions History
            when (val state = transactionsState) {
                is UiState.Loading -> {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                is UiState.Empty -> {
                    EmptyStateView(
                        title = "No Cash Movements Recorded",
                        description = "Cash entries from sales, expenses, additions and adjustments appear here",
                        icon = Icons.Default.AttachMoney
                    )
                }
                is UiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                        ErrorBanner(message = state.message)
                    }
                }
                is UiState.Success -> {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(state.data) { tx ->
                            CashTransactionCard(tx)
                        }
                    }
                }
                else -> {}
            }
        }
    }

    if (showAddCashDialog) {
        AddCashDialog(
            onDismiss = { showAddCashDialog = false },
            onConfirm = { amount, description ->
                viewModel.addCash(amount, description)
                showAddCashDialog = false
            }
        )
    }

    if (showAdjustDialog) {
        val currentBalance = (balanceState as? UiState.Success)?.data?.cashBalance ?: "0.00"
        AdjustCashDialog(
            currentBalance = currentBalance,
            onDismiss = { showAdjustDialog = false },
            onConfirm = { actualBalance, reason ->
                viewModel.adjustCash(actualBalance, reason)
                showAdjustDialog = false
            }
        )
    }
}

@Composable
private fun CashTransactionCard(tx: CashTransaction) {
    val isAddition = tx.transactionType == "add_cash" || tx.newBalance.toDoubleOrNull() ?: 0.0 >= tx.previousBalance.toDoubleOrNull() ?: 0.0

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
                        .size(42.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isAddition) Emerald50 else Amber50)
                ) {
                    Icon(
                        imageVector = if (isAddition) Icons.Default.CallReceived else Icons.Default.CallMade,
                        contentDescription = null,
                        tint = if (isAddition) Emerald600 else Amber600,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = if (!tx.description.isNullOrBlank()) tx.description
                        else tx.transactionType.replace("_", " ").replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = FormatUtils.formatDateForDisplay(tx.createdAt.ifBlank { tx.transactionDate }),
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${if (isAddition) "+" else ""}${FormatUtils.formatCurrency(tx.amount)}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isAddition) Emerald600 else MaterialTheme.colorScheme.onSurface
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                StatBadge(
                    text = "Till: ${FormatUtils.formatCurrency(tx.newBalance)}",
                    backgroundColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    textColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun AddCashDialog(
    onDismiss: () -> Unit,
    onConfirm: (amount: String, description: String?) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Add Cash into Till", fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                VyavsaTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = "Amount to Add (₹) *",
                    placeholder = "0.00",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                VyavsaTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = "Description / Reason",
                    placeholder = "Owner cash injection, morning till cash"
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(amount, description) },
                enabled = amount.isNotBlank()
            ) {
                Text("Add Cash", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AdjustCashDialog(
    currentBalance: String,
    onDismiss: () -> Unit,
    onConfirm: (actualBalance: String, reason: String) -> Unit
) {
    var actualBalance by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Reconcile Cash Balance", fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Currently Recorded: ${FormatUtils.formatCurrency(currentBalance)}",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.padding(12.dp)
                    )
                }
                VyavsaTextField(
                    value = actualBalance,
                    onValueChange = { actualBalance = it },
                    label = "Actual Physical Count (₹) *",
                    placeholder = "0.00",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                VyavsaTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = "Audit Reason / Tally Note *",
                    placeholder = "End of day physical drawer count"
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(actualBalance, reason) },
                enabled = actualBalance.isNotBlank() && reason.isNotBlank()
            ) {
                Text("Reconcile Balance", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
