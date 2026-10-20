package com.vyavsa.app.feature.transactions

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun CreateCounterSaleScreen(
    viewModel: TransactionsViewModel,
    onSaleCreated: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    CreateSaleScreen(
        viewModel = viewModel,
        initialSaleType = SaleType.COUNTER_SALE,
        lockSaleType = true,
        onSaleCreated = onSaleCreated,
        onNavigateBack = onNavigateBack,
        modifier = modifier
    )
}
