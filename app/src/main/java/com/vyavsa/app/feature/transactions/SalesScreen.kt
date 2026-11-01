@file:Suppress("DEPRECATION")
package com.vyavsa.app.feature.transactions

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CallReceived
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FilterAltOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vyavsa.app.core.common.FormatUtils
import com.vyavsa.app.core.common.MessageType
import com.vyavsa.app.core.common.ObserveAsEvents
import com.vyavsa.app.core.common.UiState
import com.vyavsa.app.core.designsystem.Amber50
import com.vyavsa.app.core.designsystem.Amber600
import com.vyavsa.app.core.designsystem.Blue50
import com.vyavsa.app.core.designsystem.Blue600
import com.vyavsa.app.core.designsystem.Crimson50
import com.vyavsa.app.core.designsystem.Crimson600
import com.vyavsa.app.core.designsystem.CustomerSelectorBottomSheet
import com.vyavsa.app.core.designsystem.Emerald50
import com.vyavsa.app.core.designsystem.Emerald600
import com.vyavsa.app.core.designsystem.EmptyStateView
import com.vyavsa.app.core.designsystem.StatBadge
import com.vyavsa.app.core.designsystem.VyavsaSearchBar
import com.vyavsa.app.core.designsystem.VyavsaTopAppBar
import com.vyavsa.app.core.designsystem.vyavsaClickable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesScreen(
    viewModel: TransactionsViewModel,
    onNavigateToCreateSale: () -> Unit,
    onNavigateToSaleDetails: (saleType: String, saleId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val allGranted = grants.values.all { it }
        if (!allGranted) {
            Toast.makeText(context, "Bluetooth permissions are required for printing.", Toast.LENGTH_SHORT).show()
        }
    }

    val searchQuery by viewModel.searchQuery.collectAsState()
    val lineSalesState by viewModel.lineSalesState.collectAsState()
    val counterSalesState by viewModel.counterSalesState.collectAsState()
    val salesCategory by viewModel.salesCategoryFilter.collectAsState()
    val dateSelection by viewModel.dateRangeSelection.collectAsState()
    val selectedCustomer by viewModel.selectedCustomer.collectAsState()
    val customersState by viewModel.customersState.collectAsState()
    val customerSearchQuery by viewModel.customerSearchQuery.collectAsState()

    val activePrintingSaleId by viewModel.activePrintingSaleId.collectAsState()
    val activeSharingSaleId by viewModel.activeSharingSaleId.collectAsState()

    ObserveAsEvents(viewModel.uiEvents) { event ->
        snackbarHostState.showSnackbar(event.text)
    }

    val isSalesRole = viewModel.tokenManager?.isSales() == true
    val isOfficeRole = viewModel.tokenManager?.isOffice() == true

    var showDateRangePicker by remember { mutableStateOf(false) }
    var showCustomerPicker by remember { mutableStateOf(false) }

    // Date Range Picker Dialog
    if (showDateRangePicker) {
        val dateRangePickerState = rememberDateRangePickerState()
        DatePickerDialog(
            onDismissRequest = { showDateRangePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val startMillis = dateRangePickerState.selectedStartDateMillis
                        val endMillis = dateRangePickerState.selectedEndDateMillis ?: startMillis
                        if (startMillis != null && endMillis != null) {
                            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                            val startStr = sdf.format(Date(startMillis))
                            val endStr = sdf.format(Date(endMillis))
                            val finalStart = if (startStr <= endStr) startStr else endStr
                            val finalEnd = if (startStr <= endStr) endStr else startStr
                            viewModel.setDateRangeFilter(DateFilterType.CUSTOM, finalStart, finalEnd)
                        }
                        showDateRangePicker = false
                    }
                ) {
                    Text("Apply Range", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDateRangePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DateRangePicker(
                state = dateRangePickerState,
                title = {
                    Text(
                        text = "Select Sales Date Range",
                        modifier = Modifier.padding(start = 24.dp, top = 16.dp),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                modifier = Modifier.heightIn(max = 500.dp)
            )
        }
    }

    // Customer Selection Bottom Sheet
    if (showCustomerPicker) {
        CustomerSelectorBottomSheet(
            customersState = customersState,
            searchQuery = customerSearchQuery,
            onSearchQueryChange = { viewModel.setCustomerSearchQuery(it) },
            onCustomerSelected = { customer ->
                viewModel.setSelectedCustomer(customer)
                showCustomerPicker = false
            },
            onDismiss = { showCustomerPicker = false },
            allowWalkIn = false
        )
    }

    val screenTitle = when {
        isSalesRole -> "Line Sales"
        isOfficeRole -> "Counter Sales"
        else -> "Sales"
    }

    val screenSubtitle = when {
        isSalesRole -> "Route deliveries & order billing"
        isOfficeRole -> "Over-the-counter retail bills"
        else -> "Field line sales, POS counter billing & invoices"
    }

    val fabText = when {
        isSalesRole -> "New Line Sale"
        isOfficeRole -> "New Counter Sale"
        else -> "New Sale"
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            VyavsaTopAppBar(
                title = screenTitle,
                subtitle = screenSubtitle
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToCreateSale,
                icon = { Icon(Icons.Default.Add, contentDescription = fabText) },
                text = { Text(fabText, fontWeight = FontWeight.Bold) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Section 1: Search Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
            ) {
                VyavsaSearchBar(
                    query = searchQuery,
                    onQueryChange = { viewModel.onSearchChange(it) },
                    placeholder = "Search customer, route, invoice #..."
                )
            }

            // Section 2: Sales Category Filter (Segmented control on full width)
            if (!isSalesRole && !isOfficeRole) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val categories = listOf(
                        SalesCategoryFilter.ALL to "All Sales",
                        SalesCategoryFilter.LINE to "Line Sales",
                        SalesCategoryFilter.COUNTER to "Counter Sales"
                    )
                    categories.forEach { (cat, label) ->
                        val isSelected = salesCategory == cat
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setSalesCategoryFilter(cat) },
                            label = {
                                Text(
                                    text = label,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Section 3: Date Filter Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Date:",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                FilterChip(
                    selected = dateSelection.type == DateFilterType.TODAY,
                    onClick = { viewModel.setDateRangeFilter(DateFilterType.TODAY) },
                    label = { Text("Today") },
                    shape = RoundedCornerShape(12.dp)
                )

                FilterChip(
                    selected = dateSelection.type == DateFilterType.YESTERDAY,
                    onClick = { viewModel.setDateRangeFilter(DateFilterType.YESTERDAY) },
                    label = { Text("Yesterday") },
                    shape = RoundedCornerShape(12.dp)
                )

                FilterChip(
                    selected = dateSelection.type == DateFilterType.THIS_WEEK,
                    onClick = { viewModel.setDateRangeFilter(DateFilterType.THIS_WEEK) },
                    label = { Text("This Week") },
                    shape = RoundedCornerShape(12.dp)
                )

                FilterChip(
                    selected = dateSelection.type == DateFilterType.THIS_MONTH,
                    onClick = { viewModel.setDateRangeFilter(DateFilterType.THIS_MONTH) },
                    label = { Text("This Month") },
                    shape = RoundedCornerShape(12.dp)
                )

                val isCustom = dateSelection.type == DateFilterType.CUSTOM
                val customLabel = if (isCustom && dateSelection.startDate != null) {
                    if (dateSelection.startDate == dateSelection.endDate) {
                        FormatUtils.formatDateForDisplay(dateSelection.startDate)
                    } else {
                        "${FormatUtils.formatDateForDisplay(dateSelection.startDate)} - ${FormatUtils.formatDateForDisplay(dateSelection.endDate)}"
                    }
                } else {
                    "Custom Range"
                }

                FilterChip(
                    selected = isCustom,
                    onClick = { showDateRangePicker = true },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    label = { Text(customLabel) },
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Section 4: Customer Filter & Reset Actions Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (selectedCustomer != null) {
                    InputChip(
                        selected = true,
                        onClick = { showCustomerPicker = true },
                        label = {
                            Text(
                                text = "Customer: ${selectedCustomer!!.customerName}",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear Customer",
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable { viewModel.clearCustomerFilter() }
                            )
                        },
                        colors = InputChipDefaults.inputChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                } else {
                    AssistChip(
                        onClick = { showCustomerPicker = true },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.PersonOutline,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = { Text("Filter by Customer") },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                val hasActiveFilters = selectedCustomer != null ||
                    dateSelection.type != DateFilterType.TODAY ||
                    salesCategory != SalesCategoryFilter.ALL ||
                    searchQuery.isNotBlank()

                if (hasActiveFilters) {
                    SuggestionChip(
                        onClick = { viewModel.clearAllFilters() },
                        label = {
                            Text(
                                text = "Reset Filters",
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.FilterAltOff,
                                contentDescription = "Reset Filters",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = Crimson50
                        ),
                        border = SuggestionChipDefaults.suggestionChipBorder(
                            enabled = true,
                            borderColor = Crimson600.copy(alpha = 0.4f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Section 5: Sales List & State Handling
            val showLine = when {
                isSalesRole -> true
                isOfficeRole -> false
                else -> salesCategory == SalesCategoryFilter.ALL || salesCategory == SalesCategoryFilter.LINE
            }
            val showCounter = when {
                isSalesRole -> false
                isOfficeRole -> true
                else -> salesCategory == SalesCategoryFilter.ALL || salesCategory == SalesCategoryFilter.COUNTER
            }

            val isLineLoading = showLine && lineSalesState is UiState.Loading
            val isCounterLoading = showCounter && counterSalesState is UiState.Loading
            val isLoading = isLineLoading || isCounterLoading

            val lineError = if (showLine && lineSalesState is UiState.Error) (lineSalesState as UiState.Error).message else null
            val counterError = if (showCounter && counterSalesState is UiState.Error) (counterSalesState as UiState.Error).message else null
            val activeError = lineError ?: counterError

            val filteredLineSales = if (showLine) (lineSalesState as? UiState.Success)?.data ?: emptyList() else emptyList()
            val filteredCounterSales = if (showCounter) (counterSalesState as? UiState.Success)?.data ?: emptyList() else emptyList()

            if (isLoading && filteredLineSales.isEmpty() && filteredCounterSales.isEmpty()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (activeError != null && filteredLineSales.isEmpty() && filteredCounterSales.isEmpty()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = activeError,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = { viewModel.loadFilterableData(activeTabOnly = true) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Retry")
                        }
                    }
                }
            } else if (filteredLineSales.isEmpty() && filteredCounterSales.isEmpty()) {
                val hasFilter = selectedCustomer != null || searchQuery.isNotBlank() || dateSelection.type != DateFilterType.TODAY
                val description = if (searchQuery.isNotBlank()) {
                    "No sales matched \"$searchQuery\""
                } else if (selectedCustomer != null) {
                    "No sales found for customer ${selectedCustomer!!.customerName}"
                } else {
                    "No sales recorded for the selected filter criteria"
                }

                EmptyStateView(
                    title = "No Sales Found",
                    description = description,
                    icon = Icons.Default.ReceiptLong,
                    actionText = if (hasFilter) "Reset Filters" else "Record Sale",
                    onActionClick = {
                        if (hasFilter) {
                            viewModel.clearAllFilters()
                        } else {
                            onNavigateToCreateSale()
                        }
                    }
                )
            } else {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Line Sales
                    if (filteredLineSales.isNotEmpty()) {
                        itemsIndexed(
                            items = filteredLineSales,
                            key = { index, sale -> "line_${sale.id.ifBlank { "ls" }}_$index" },
                            contentType = { _, _ -> "line_sale" }
                        ) { _, sale ->
                            SaleItemCard(
                                title = sale.customerName.ifBlank { "Line Customer" },
                                invoiceNumber = sale.id.take(8).uppercase(),
                                dateString = sale.createdAt,
                                route = sale.route,
                                totalAmount = sale.totalAmount,
                                discount = sale.discount,
                                balance = sale.balance,
                                isCounter = false,
                                isPrinting = activePrintingSaleId == sale.id,
                                isSharing = activeSharingSaleId == sale.id,
                                onCardClick = { onNavigateToSaleDetails("line", sale.id) },
                                onPrintClick = {
                                    viewModel.printLineSale(sale, context) {
                                        permissionLauncher.launch(it)
                                    }
                                },
                                onShareClick = {
                                    viewModel.shareLineSalePdf(context, sale) { success, err ->
                                        if (!success && err != null) {
                                            viewModel.sendUiMessage(err, MessageType.ERROR)
                                        }
                                    }
                                }
                            )
                        }
                    }

                    // Counter Sales
                    if (filteredCounterSales.isNotEmpty()) {
                        itemsIndexed(
                            items = filteredCounterSales,
                            key = { index, sale -> "counter_${sale.id.ifBlank { "cs" }}_$index" },
                            contentType = { _, _ -> "counter_sale" }
                        ) { _, sale ->
                            SaleItemCard(
                                title = sale.item.ifBlank { "Counter Sale" },
                                invoiceNumber = sale.id.take(8).uppercase(),
                                dateString = sale.createdAt,
                                route = null,
                                totalAmount = sale.totalAmount,
                                discount = sale.discount,
                                balance = sale.account,
                                isCounter = true,
                                isPrinting = activePrintingSaleId == sale.id,
                                isSharing = activeSharingSaleId == sale.id,
                                onCardClick = { onNavigateToSaleDetails("counter", sale.id) },
                                onPrintClick = {
                                    viewModel.printCounterSale(sale, context) {
                                        permissionLauncher.launch(it)
                                    }
                                },
                                onShareClick = {
                                    viewModel.shareCounterSalePdf(context, sale) { success, err ->
                                        if (!success && err != null) {
                                            viewModel.sendUiMessage(err, MessageType.ERROR)
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SaleItemCard(
    title: String,
    invoiceNumber: String,
    dateString: String,
    route: String? = null,
    totalAmount: String,
    discount: String,
    balance: String,
    isCounter: Boolean,
    isPrinting: Boolean,
    isSharing: Boolean,
    onCardClick: () -> Unit,
    onPrintClick: () -> Unit,
    onShareClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .vyavsaClickable { onCardClick() }
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f), RoundedCornerShape(22.dp))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row: Avatar, Title, Invoice info and Amounts
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isCounter) Amber50 else Blue50)
                    ) {
                        Icon(
                            imageVector = if (isCounter) Icons.Default.Storefront else Icons.Default.CallReceived,
                            contentDescription = null,
                            tint = if (isCounter) Amber600 else Blue600,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Invoice: #$invoiceNumber",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Text(
                            text = "Date: ${FormatUtils.formatDateForDisplay(dateString)}${if (!route.isNullOrBlank()) " • Route: $route" else ""}",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Amounts and Status Badges
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "+${FormatUtils.formatCurrency(totalAmount)}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = Emerald600
                        )
                    )
                    val balVal = balance.toDoubleOrNull() ?: 0.0
                    val discVal = discount.toDoubleOrNull() ?: 0.0
                    if (discVal > 0.0) {
                        Text(
                            text = "Disc: -${FormatUtils.formatCurrency(discount)}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        StatBadge(
                            text = if (isCounter) "Counter" else "Line",
                            backgroundColor = if (isCounter) Amber50 else Blue50,
                            textColor = if (isCounter) Amber600 else Blue600
                        )
                        if (balVal > 0.0) {
                            StatBadge(
                                text = "Due: ${FormatUtils.formatCurrency(balance)}",
                                backgroundColor = Crimson50,
                                textColor = Crimson600
                            )
                        } else {
                            StatBadge(
                                text = "Paid",
                                backgroundColor = Emerald50,
                                textColor = Emerald600
                            )
                        }
                    }
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                thickness = 0.8.dp
            )

            // One-Tap Actions: Print & Share PDF
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                FilledTonalButton(
                    onClick = onPrintClick,
                    enabled = !isPrinting && !isSharing,
                    shape = CircleShape,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 44.dp)
                ) {
                    if (isPrinting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Printing...", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(
                            imageVector = Icons.Default.Print,
                            contentDescription = "Print Invoice",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Print", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }

                OutlinedButton(
                    onClick = onShareClick,
                    enabled = !isPrinting && !isSharing,
                    shape = CircleShape,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 44.dp)
                ) {
                    if (isSharing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Sharing...", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share PDF",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share PDF", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
