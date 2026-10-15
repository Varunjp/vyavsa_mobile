package com.vyavsa.app.feature.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.vyavsa.app.core.common.UiState
import com.vyavsa.app.core.designsystem.ErrorBanner
import com.vyavsa.app.core.designsystem.VyavsaButton
import com.vyavsa.app.core.designsystem.VyavsaTextField
import com.vyavsa.app.core.designsystem.VyavsaTopAppBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateExpenseScreen(
    viewModel: TransactionsViewModel,
    onExpenseCreated: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val createActionState by viewModel.createActionState.collectAsState()
    val focusManager = LocalFocusManager.current

    var item by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("operations") }
    var amount by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("cash") }

    val categories = listOf("operations", "transport", "office", "salary", "rent", "misc")

    LaunchedEffect(createActionState) {
        if (createActionState is UiState.Success) {
            onExpenseCreated()
            viewModel.resetCreateState()
        }
    }

    Scaffold(
        topBar = {
            VyavsaTopAppBar(
                title = "Record Expense",
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "New Expense Entry",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        if (createActionState is UiState.Error) {
                            ErrorBanner(
                                message = (createActionState as UiState.Error).message,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                        }

                        // Expense Item Name
                        VyavsaTextField(
                            value = item,
                            onValueChange = { item = it },
                            label = "Expense Name / Reason *",
                            placeholder = "e.g. Fuel, Tea, Office Electricity",
                            leadingIcon = Icons.Default.Description,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Amount
                        VyavsaTextField(
                            value = amount,
                            onValueChange = { amount = it },
                            label = "Amount (₹) *",
                            placeholder = "0.00",
                            leadingIcon = Icons.Default.AttachMoney,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Done
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Category Chips
                        Text(
                            text = "Category",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            categories.take(3).forEach { cat ->
                                FilterChip(
                                    selected = category == cat,
                                    onClick = { category = cat },
                                    label = { Text(cat.replaceFirstChar { it.uppercase() }) }
                                )
                            }
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            categories.drop(3).forEach { cat ->
                                FilterChip(
                                    selected = category == cat,
                                    onClick = { category = cat },
                                    label = { Text(cat.replaceFirstChar { it.uppercase() }) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Payment Method
                        Text(
                            text = "Paid From",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            FilterChip(
                                selected = paymentMethod == "cash",
                                onClick = { paymentMethod = "cash" },
                                label = { Text("Cash Till") }
                            )
                            FilterChip(
                                selected = paymentMethod == "bank",
                                onClick = { paymentMethod = "bank" },
                                label = { Text("Bank Account") }
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        VyavsaButton(
                            text = "Record Expense",
                            onClick = {
                                focusManager.clearFocus()
                                viewModel.createExpense(
                                    item = item,
                                    category = category,
                                    amount = amount,
                                    paymentMethod = paymentMethod
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
