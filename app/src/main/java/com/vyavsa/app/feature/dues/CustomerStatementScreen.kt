@file:Suppress("DEPRECATION")
package com.vyavsa.app.feature.dues

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.vyavsa.app.core.designsystem.VyavsaGradients
import com.vyavsa.app.core.designsystem.VyavsaTopAppBar
import com.vyavsa.app.core.model.CustomerStatementEntry

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CustomerStatementScreen(
    customerId: String,
    customerName: String,
    viewModel: DuesViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statementState by viewModel.statementState.collectAsState()

    LaunchedEffect(customerId) {
        viewModel.loadStatement(customerId)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            VyavsaTopAppBar(
                title = customerName,
                subtitle = "Customer Ledger Statement",
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
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (val state = statementState) {
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
                    val statement = state.data
                    val entries = statement.entries

                    if (entries.isEmpty()) {
                        EmptyStateView(
                            title = "No Statement Entries",
                            description = "No purchases or settlement payments have been recorded for this customer yet",
                            icon = Icons.Default.ReceiptLong
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            // Statement Header Balance with Gradient
                            item(key = "statement_hero") {
                                Card(
                                    shape = RoundedCornerShape(26.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(VyavsaGradients.PrimaryHeroBrush)
                                            .padding(20.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column {
                                                Text(
                                                    text = "Total Purchases",
                                                    style = MaterialTheme.typography.labelMedium.copy(
                                                        color = Color.White.copy(alpha = 0.8f),
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = FormatUtils.formatCurrency(statement.totalPurchases),
                                                    style = MaterialTheme.typography.headlineSmall.copy(
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = Color.White
                                                    )
                                                )
                                            }

                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(
                                                    text = "Outstanding Due",
                                                    style = MaterialTheme.typography.labelMedium.copy(
                                                        color = Color.White.copy(alpha = 0.8f),
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = FormatUtils.formatCurrency(statement.currentBalance),
                                                    style = MaterialTheme.typography.headlineSmall.copy(
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = Color.White
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                            }

                            items(
                                items = entries,
                                key = { it.id.ifBlank { "${it.date}_${it.description}_${it.balance}" } },
                                contentType = { "statement_entry" }
                            ) { entry ->
                                StatementEntryCard(
                                    entry = entry,
                                    modifier = Modifier.animateItemPlacement()
                                )
                            }
                        }
                    }
                }
                else -> {}
            }
        }
    }
}

@Composable
private fun StatementEntryCard(
    entry: CustomerStatementEntry,
    modifier: Modifier = Modifier
) {
    val isPayment = entry.entryType.contains("payment", ignoreCase = true) || entry.paidAmount.toDoubleOrNull() ?: 0.0 > 0.0

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
                    text = entry.description.ifBlank { if (isPayment) "Settlement Payment" else "Purchase / Delivery" },
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${entry.formattedDate.ifBlank { entry.date }} • ${entry.paymentMethod ?: "Cash"}",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (isPayment) "-${FormatUtils.formatCurrency(entry.paidAmount)}" else "+${FormatUtils.formatCurrency(entry.purchaseAmount)}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isPayment) Emerald600 else Crimson600
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                StatBadge(
                    text = "Bal: ${FormatUtils.formatCurrency(entry.balance)}",
                    backgroundColor = if (isPayment) Emerald50 else Crimson50,
                    textColor = if (isPayment) Emerald600 else Crimson600
                )
            }
        }
    }
}
