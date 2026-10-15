package com.vyavsa.app.feature.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.vyavsa.app.core.model.Customer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateLineSaleScreen(
    viewModel: TransactionsViewModel,
    onSaleCreated: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val createActionState by viewModel.createActionState.collectAsState()
    val customersState by viewModel.customersState.collectAsState()
    val focusManager = LocalFocusManager.current

    val customers = (customersState as? UiState.Success)?.data ?: emptyList()

    var selectedCustomer by remember { mutableStateOf<Customer?>(null) }
    var customerDropdownExpanded by remember { mutableStateOf(false) }
    var route by remember { mutableStateOf("") }
    var salesman by remember { mutableStateOf("") }
    var totalAmount by remember { mutableStateOf("") }
    var cashIn by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

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
                title = "Record Field Sale",
                subtitle = "Log customer route delivery & cash collected",
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
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f), RoundedCornerShape(28.dp))
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text(
                            text = "Line Sale Entry",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Any unpaid difference is automatically added to customer dues",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        if (createActionState is UiState.Error) {
                            ErrorBanner(
                                message = (createActionState as UiState.Error).message,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                        }

                        // Customer Selection
                        ExposedDropdownMenuBox(
                            expanded = customerDropdownExpanded,
                            onExpandedChange = { customerDropdownExpanded = !customerDropdownExpanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = selectedCustomer?.customerName ?: "",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Select Customer *") },
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
                                        text = { Text("No customers available") },
                                        onClick = { customerDropdownExpanded = false }
                                    )
                                } else {
                                    customers.forEach { customer ->
                                        DropdownMenuItem(
                                            text = { Text(customer.customerName) },
                                            onClick = {
                                                selectedCustomer = customer
                                                customerDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Route
                        VyavsaTextField(
                            value = route,
                            onValueChange = { route = it },
                            label = "Delivery Route",
                            placeholder = "e.g. North Sector, Main Market",
                            leadingIcon = Icons.Default.Route,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Salesman
                        VyavsaTextField(
                            value = salesman,
                            onValueChange = { salesman = it },
                            label = "Salesman Name (Optional)",
                            leadingIcon = Icons.Default.Person,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Total Bill Amount
                        VyavsaTextField(
                            value = totalAmount,
                            onValueChange = { totalAmount = it },
                            label = "Total Sale Amount (₹) *",
                            placeholder = "0.00",
                            leadingIcon = Icons.Default.AttachMoney,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Next
                            )
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Cash Collected In
                        VyavsaTextField(
                            value = cashIn,
                            onValueChange = { cashIn = it },
                            label = "Cash Collected Today (₹)",
                            placeholder = "0.00",
                            leadingIcon = Icons.Default.AttachMoney,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Next
                            )
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Note
                        VyavsaTextField(
                            value = note,
                            onValueChange = { note = it },
                            label = "Remark / Note",
                            placeholder = "Optional remarks",
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                        )

                        Spacer(modifier = Modifier.height(26.dp))

                        VyavsaButton(
                            text = "Save Sale Entry",
                            onClick = {
                                focusManager.clearFocus()
                                val custId = selectedCustomer?.id ?: ""
                                viewModel.createLineSale(
                                    customerId = custId,
                                    route = route,
                                    salesman = salesman,
                                    totalAmount = totalAmount,
                                    cashIn = cashIn,
                                    note = note
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
