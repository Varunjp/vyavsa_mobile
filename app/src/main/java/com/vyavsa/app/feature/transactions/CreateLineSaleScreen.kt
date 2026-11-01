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
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import android.widget.Toast
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.ui.platform.LocalContext
import com.vyavsa.app.core.common.SaleCalculationUtils
import com.vyavsa.app.core.common.SaleCalculationUtils.PosAllocationPreview
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.material3.Surface
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
import com.vyavsa.app.core.designsystem.AvatarInitialCircle
import com.vyavsa.app.core.designsystem.Blue50
import com.vyavsa.app.core.designsystem.CustomerSelectorBottomSheet
import com.vyavsa.app.core.designsystem.QuickAddCustomerDialog
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.ShoppingBag
import com.vyavsa.app.core.designsystem.Blue600
import com.vyavsa.app.core.designsystem.Crimson50
import com.vyavsa.app.core.designsystem.Crimson600
import com.vyavsa.app.core.designsystem.Emerald50
import com.vyavsa.app.core.designsystem.Emerald600
import com.vyavsa.app.core.designsystem.ErrorBanner
import com.vyavsa.app.core.designsystem.ProductSelectorBottomSheet
import com.vyavsa.app.core.designsystem.SaleItemCard
import com.vyavsa.app.core.designsystem.StatBadge
import com.vyavsa.app.core.designsystem.VyavsaButton
import com.vyavsa.app.core.designsystem.VyavsaOutlinedButton
import com.vyavsa.app.core.designsystem.VyavsaSegmentedTabs
import com.vyavsa.app.core.designsystem.VyavsaTextField
import com.vyavsa.app.core.designsystem.VyavsaTopAppBar
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.AlertDialog
import com.vyavsa.app.core.model.LineSale
import com.vyavsa.app.core.model.BankPaymentSplit
import com.vyavsa.app.core.model.Customer
import com.vyavsa.app.core.model.Product
import com.vyavsa.app.core.model.SaleItemRequest
import com.vyavsa.app.core.model.TenantBank

@Immutable
data class BankPaymentEntry(
    val id: String = java.util.UUID.randomUUID().toString(),
    val bank: TenantBank? = null,
    val amount: String = "",
    val note: String? = null
)

@Immutable
data class SaleItemEntry(
    val id: String = java.util.UUID.randomUUID().toString(),
    val productId: String,
    val productName: String,
    val catalogUnitPrice: String,
    val quantity: String = "1",
    val unitPrice: String = catalogUnitPrice
) {
    val lineTotal: String
        get() {
            val q = quantity.toDoubleOrNull() ?: 0.0
            val p = unitPrice.toDoubleOrNull() ?: 0.0
            return String.format(java.util.Locale.US, "%.2f", q * p)
        }
}


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
    lockSaleType: Boolean = false,
    onSaleCreated: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isSalesRole = viewModel.tokenManager?.isSales() == true
    val isOfficeRole = viewModel.tokenManager?.isOffice() == true
    val determinedInitialType = when {
        isSalesRole -> SaleType.LINE_SALE
        isOfficeRole -> SaleType.COUNTER_SALE
        else -> initialSaleType
    }
    var selectedSaleType by remember { mutableStateOf(determinedInitialType) }
    val canSwitchSaleType = !lockSaleType && !isSalesRole && !isOfficeRole

    val createActionState by viewModel.createActionState.collectAsState()
    val customersState by viewModel.customersState.collectAsState()
    val banksState by viewModel.banksState.collectAsState()
    val productsState by viewModel.productsState.collectAsState()
    val focusManager = LocalFocusManager.current

    val customers = (customersState as? UiState.Success)?.data ?: emptyList()
    val availableBanks = remember(banksState) {
        (banksState as? UiState.Success)?.data?.filter { it.status == "active" || it.status.isBlank() } ?: emptyList()
    }
    val isLoadingBanks = banksState is UiState.Loading

    // Form fields for Line Sale
    val context = LocalContext.current
    var saleDate by remember { mutableStateOf(FormatUtils.todayDateString()) }
    var showDatePicker by remember { mutableStateOf(false) }

    var selectedCustomer by remember { mutableStateOf<Customer?>(null) }
    var showCustomerSelectorSheet by remember { mutableStateOf(false) }
    var customerSelectorTarget by remember { mutableStateOf(SaleType.LINE_SALE) }
    var customerSearchQuery by remember { mutableStateOf("") }
    var showQuickAddCustomerDialog by remember { mutableStateOf(false) }
    var route by remember { mutableStateOf("") }
    var salesman by remember { mutableStateOf("") }
    var lineSubtotal by remember { mutableStateOf("") }
    var lineDiscount by remember { mutableStateOf("0.00") }
    var lineTotalAmount by remember { mutableStateOf("") }
    var lineCashIn by remember { mutableStateOf("0.00") }
    var lineIncludeBank by remember { mutableStateOf(false) }
    var lineBankEntries by remember { mutableStateOf<List<BankPaymentEntry>>(emptyList()) }
    var note by remember { mutableStateOf("") }
    var lineItems by remember { mutableStateOf<List<SaleItemEntry>>(emptyList()) }

    // Form fields for Counter Sale
    var counterSelectedCustomer by remember { mutableStateOf<Customer?>(null) }
    var counterItem by remember { mutableStateOf("") }
    var counterPrice by remember { mutableStateOf("") }
    var counterSubtotal by remember { mutableStateOf("") }
    var counterDiscount by remember { mutableStateOf("0.00") }
    var counterTotalAmount by remember { mutableStateOf("") }
    var counterCash by remember { mutableStateOf("0.00") }
    var counterIncludeBank by remember { mutableStateOf(false) }
    var counterBankEntries by remember { mutableStateOf<List<BankPaymentEntry>>(emptyList()) }
    var counterItems by remember { mutableStateOf<List<SaleItemEntry>>(emptyList()) }

    LaunchedEffect(Unit) {
        if (customersState !is UiState.Success || customers.isEmpty()) {
            viewModel.loadCustomers()
        }
        if (productsState !is UiState.Success) {
            viewModel.loadProducts()
        }
        if (banksState !is UiState.Success) {
            viewModel.loadBanks()
        }
    }

    // Dynamic POS allocation calculation
    val lineBankTotal = if (lineIncludeBank) lineBankEntries.sumOf { it.amount.toDoubleOrNull() ?: 0.0 } else 0.0
    val linePosPreview = remember(lineTotalAmount, selectedCustomer, lineCashIn, lineBankTotal) {
        SaleCalculationUtils.calculatePosAllocation(
            saleTotalStr = lineTotalAmount,
            customerCurrentBalanceStr = selectedCustomer?.currentBalance,
            customerAdvanceBalanceStr = selectedCustomer?.advanceBalance,
            directCashStr = lineCashIn,
            directBankStr = lineBankTotal.toString()
        )
    }

    val counterBankTotal = if (counterIncludeBank) counterBankEntries.sumOf { it.amount.toDoubleOrNull() ?: 0.0 } else 0.0
    val counterPosPreview = remember(counterTotalAmount, counterSelectedCustomer, counterCash, counterBankTotal) {
        SaleCalculationUtils.calculatePosAllocation(
            saleTotalStr = counterTotalAmount,
            customerCurrentBalanceStr = counterSelectedCustomer?.currentBalance,
            customerAdvanceBalanceStr = counterSelectedCustomer?.advanceBalance,
            directCashStr = counterCash,
            directBankStr = counterBankTotal.toString()
        )
    }

    // Searchable Product Selector Sheet State
    var showProductSelectorSheet by remember { mutableStateOf(false) }
    var selectorTarget by remember { mutableStateOf(SaleType.LINE_SALE) }
    var productSearchQuery by remember { mutableStateOf("") }

    val activePrintingSaleId by viewModel.activePrintingSaleId.collectAsState()
    val isPrinting = activePrintingSaleId != null

    var lastClickedLineAction by remember { mutableStateOf<String?>(null) } // "record" or "record_and_print"
    var savedSaleForPrintFailure by remember { mutableStateOf<LineSale?>(null) }
    var printFailureMessage by remember { mutableStateOf<String?>(null) }
    var isRetryingPrint by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val allGranted = grants.values.all { it }
        if (allGranted) {
            savedSaleForPrintFailure?.let { sale ->
                isRetryingPrint = true
                viewModel.printLineSaleWithResult(
                    sale = sale,
                    context = context,
                    onResult = { isSuccess, err ->
                        isRetryingPrint = false
                        if (isSuccess) {
                            Toast.makeText(context, "Invoice printed successfully", Toast.LENGTH_SHORT).show()
                            savedSaleForPrintFailure = null
                            onSaleCreated()
                            viewModel.resetCreateState()
                        } else {
                            printFailureMessage = err ?: "Printer communication error"
                        }
                    }
                )
            }
        } else {
            Toast.makeText(context, "Bluetooth permissions are required for printing.", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(createActionState) {
        if (createActionState is UiState.Success) {
            if (lastClickedLineAction != "record_and_print") {
                onSaleCreated()
                viewModel.resetCreateState()
            }
        } else if (createActionState is UiState.Error) {
            lastClickedLineAction = null
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                            saleDate = sdf.format(Date(millis))
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("Select", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.systemBars.union(WindowInsets.ime),
        topBar = {
            VyavsaTopAppBar(
                title = if (selectedSaleType == SaleType.LINE_SALE) "Record Line Sale" else "Record Counter Sale",
                subtitle = if (selectedSaleType == SaleType.LINE_SALE) "Field route delivery & customer ledger update" else "Over-the-counter retail register",
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                onNavigationClick = onNavigateBack
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                shadowElevation = 8.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    val totalVal = if (selectedSaleType == SaleType.LINE_SALE) lineTotalAmount else counterTotalAmount
                    val subVal = if (selectedSaleType == SaleType.LINE_SALE) {
                        lineSubtotal.ifBlank { totalVal }
                    } else {
                        counterSubtotal.ifBlank { counterPrice.ifBlank { totalVal } }
                    }
                    val discVal = (if (selectedSaleType == SaleType.LINE_SALE) lineDiscount else counterDiscount).toDoubleOrNull() ?: 0.0
                    val cashVal = (if (selectedSaleType == SaleType.LINE_SALE) lineCashIn else counterCash).toDoubleOrNull() ?: 0.0
                    val bankEntriesVal = if (selectedSaleType == SaleType.LINE_SALE) {
                        if (lineIncludeBank) lineBankEntries else emptyList()
                    } else {
                        if (counterIncludeBank) counterBankEntries else emptyList()
                    }
                    val bankVal = bankEntriesVal.sumOf { it.amount.toDoubleOrNull() ?: 0.0 }
                    val totVal = totalVal.toDoubleOrNull() ?: 0.0
                    val dueVal = if (totVal > (cashVal + bankVal)) totVal - (cashVal + bankVal) else 0.0

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        Column {
                            if (discVal > 0.0) {
                                Text(
                                    text = "Subtotal: ${FormatUtils.formatCurrency(subVal.ifBlank { "0.00" })} • Disc: -${FormatUtils.formatCurrency(discVal.toString())}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Emerald600,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                            Text(
                                text = "Final Payable Amount",
                                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                            Text(
                                text = FormatUtils.formatCurrency(totalVal.ifBlank { "0.00" }),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                        }

                        if (selectedSaleType == SaleType.LINE_SALE && dueVal > 0.0) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Due to Customer Ledger",
                                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                                Text(
                                    text = FormatUtils.formatCurrency(dueVal.toString()),
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Crimson600
                                    )
                                )
                            }
                        }
                    }

                    if (selectedSaleType == SaleType.LINE_SALE) {
                        val isFormBusy = createActionState is UiState.Loading || isPrinting
                        val areButtonsEnabled = !isFormBusy && savedSaleForPrintFailure == null
                        val isRecordLoading = lastClickedLineAction == "record" && isFormBusy
                        val isRecordAndPrintLoading = (lastClickedLineAction == "record_and_print" && isFormBusy) || (isPrinting && lastClickedLineAction != "record")

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            VyavsaOutlinedButton(
                                text = "Record",
                                onClick = {
                                    focusManager.clearFocus()
                                    if (createActionState is UiState.Loading || isPrinting || savedSaleForPrintFailure != null) return@VyavsaOutlinedButton

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
                                    val itemRequests = lineItems.map {
                                        SaleItemRequest(
                                            productId = it.productId,
                                            quantity = it.quantity.trim(),
                                            unitPrice = it.unitPrice.trim()
                                        )
                                    }
                                    lastClickedLineAction = "record"
                                    viewModel.createLineSale(
                                        customerId = selectedCustomer?.id ?: "",
                                        route = route,
                                        salesman = salesman,
                                        subtotal = lineSubtotal.ifBlank { lineTotalAmount },
                                        discount = lineDiscount.ifBlank { "0.00" },
                                        totalAmount = lineTotalAmount,
                                        cashIn = lineCashIn,
                                        bankAmount = String.format(java.util.Locale.US, "%.2f", bankTotal),
                                        bankId = splits.firstOrNull()?.bankId,
                                        bankPayments = splits,
                                        note = note,
                                        items = itemRequests,
                                        saleDate = saleDate,
                                        advanceToApply = linePosPreview.advanceAppliedString
                                    )
                                },
                                enabled = areButtonsEnabled,
                                isLoading = isRecordLoading,
                                modifier = Modifier.weight(1f)
                            )

                            VyavsaButton(
                                text = if (isPrinting) "Printing..." else "Record & Print",
                                onClick = {
                                    focusManager.clearFocus()
                                    if (createActionState is UiState.Loading || isPrinting || savedSaleForPrintFailure != null) return@VyavsaButton

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
                                    val itemRequests = lineItems.map {
                                        SaleItemRequest(
                                            productId = it.productId,
                                            quantity = it.quantity.trim(),
                                            unitPrice = it.unitPrice.trim()
                                        )
                                    }
                                    lastClickedLineAction = "record_and_print"
                                    viewModel.recordAndPrintLineSale(
                                        customerId = selectedCustomer?.id ?: "",
                                        route = route,
                                        salesman = salesman,
                                        subtotal = lineSubtotal.ifBlank { lineTotalAmount },
                                        discount = lineDiscount.ifBlank { "0.00" },
                                        totalAmount = lineTotalAmount,
                                        cashIn = lineCashIn,
                                        bankAmount = String.format(java.util.Locale.US, "%.2f", bankTotal),
                                        bankId = splits.firstOrNull()?.bankId,
                                        bankPayments = splits,
                                        note = note,
                                        items = itemRequests,
                                        saleDate = saleDate,
                                        advanceToApply = linePosPreview.advanceAppliedString,
                                        customerName = selectedCustomer?.customerName,
                                        customerPhone = selectedCustomer?.phone,
                                        context = context,
                                        onPermissionRequired = { perms ->
                                            permissionLauncher.launch(perms)
                                        },
                                        onPrintResult = { savedSale, isPrintSuccess, printErrorMsg ->
                                            if (isPrintSuccess) {
                                                Toast.makeText(context, "Line sale recorded and invoice printed successfully", Toast.LENGTH_SHORT).show()
                                                onSaleCreated()
                                                viewModel.resetCreateState()
                                            } else {
                                                savedSaleForPrintFailure = savedSale
                                                printFailureMessage = printErrorMsg ?: "Printing invoice failed. Check printer connection or paper roll."
                                                viewModel.resetCreateState()
                                            }
                                            lastClickedLineAction = null
                                        }
                                    )
                                },
                                enabled = areButtonsEnabled,
                                isLoading = isRecordAndPrintLoading,
                                leadingIcon = if (!isRecordAndPrintLoading) Icons.Default.Print else null,
                                modifier = Modifier.weight(1.3f)
                            )
                        }
                    } else {
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
                                val itemRequests = counterItems.map {
                                    SaleItemRequest(
                                        productId = it.productId,
                                        quantity = it.quantity.trim(),
                                        unitPrice = it.unitPrice.trim()
                                    )
                                }
                                val effectiveSub = counterSubtotal.ifBlank { counterPrice.ifBlank { counterTotalAmount } }
                                viewModel.createCounterSale(
                                    item = counterItem.ifBlank { if (counterItems.isNotEmpty()) counterItems.first().productName else "" },
                                    price = counterPrice.ifBlank { effectiveSub },
                                    subtotal = effectiveSub,
                                    discount = counterDiscount.ifBlank { "0.00" },
                                    totalAmount = counterTotalAmount,
                                    cash = counterCash,
                                    bankAmount = String.format(java.util.Locale.US, "%.2f", bankTotal),
                                    bankId = splits.firstOrNull()?.bankId,
                                    bankPayments = splits,
                                    items = itemRequests,
                                    customerId = counterSelectedCustomer?.id,
                                    customerName = counterSelectedCustomer?.customerName,
                                    saleDate = saleDate,
                                    advanceToApply = if (counterSelectedCustomer != null) counterPosPreview.advanceAppliedString else null
                                )
                            },
                            isLoading = createActionState is UiState.Loading
                        )
                    }
                }
            }
        },
        modifier = modifier
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                // Sale Type Segmented Selector (Shown only when role is authorized to switch)
                if (canSwitchSaleType) {
                    VyavsaSegmentedTabs(
                        tabs = listOf("Line / Route Sale", "Counter / POS Sale"),
                        selectedIndex = if (selectedSaleType == SaleType.LINE_SALE) 0 else 1,
                        onTabSelected = { idx ->
                            selectedSaleType = if (idx == 0) SaleType.LINE_SALE else SaleType.COUNTER_SALE
                            viewModel.resetCreateState()
                        },
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

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

                                Spacer(modifier = Modifier.height(14.dp))

                                SaleDateSelectorRow(
                                    saleDate = saleDate,
                                    isAdmin = viewModel.isAdmin,
                                    onSelectDateClick = {
                                        if (viewModel.isAdmin) {
                                            showDatePicker = true
                                        } else {
                                            Toast.makeText(context, "Non-admins can only record sales for today", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                // Customer Selector for Line Sale
                                if (selectedCustomer == null) {
                                    Surface(
                                        shape = RoundedCornerShape(18.dp),
                                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(18.dp))
                                            .clickable {
                                                customerSelectorTarget = SaleType.LINE_SALE
                                                customerSearchQuery = ""
                                                showCustomerSelectorSheet = true
                                            }
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 14.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Person,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(22.dp)
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "Customer Account *",
                                                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                )
                                                Text(
                                                    text = "Select registered route customer",
                                                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
                                                )
                                            }
                                            Icon(
                                                imageVector = Icons.Default.Search,
                                                contentDescription = "Search",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                } else {
                                    Surface(
                                        shape = RoundedCornerShape(18.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.22f),
                                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                        ) {
                                            AvatarInitialCircle(
                                                name = selectedCustomer!!.customerName,
                                                modifier = Modifier.size(42.dp)
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = selectedCustomer!!.customerName,
                                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                                )
                                                if (!selectedCustomer!!.phone.isNullOrBlank()) {
                                                    Text(
                                                        text = selectedCustomer!!.phone!!,
                                                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    )
                                                }
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    modifier = Modifier.padding(top = 4.dp)
                                                ) {
                                                    val due = selectedCustomer!!.currentBalance.toDoubleOrNull() ?: 0.0
                                                    val adv = selectedCustomer!!.advanceBalance.toDoubleOrNull() ?: 0.0
                                                    StatBadge(
                                                        text = "Due: ${FormatUtils.formatCurrency(selectedCustomer!!.currentBalance)}",
                                                        backgroundColor = if (due > 0) Crimson50 else Emerald50,
                                                        textColor = if (due > 0) Crimson600 else Emerald600
                                                    )
                                                    if (adv > 0) {
                                                        StatBadge(
                                                            text = "Adv: ${FormatUtils.formatCurrency(selectedCustomer!!.advanceBalance)}",
                                                            backgroundColor = Blue50,
                                                            textColor = Blue600
                                                        )
                                                    }
                                                }
                                            }
                                            TextButton(
                                                onClick = {
                                                    customerSelectorTarget = SaleType.LINE_SALE
                                                    customerSearchQuery = ""
                                                    showCustomerSelectorSheet = true
                                                }
                                            ) {
                                                Text("Change", fontWeight = FontWeight.Bold)
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
                                        Column {
                                            Spacer(modifier = Modifier.height(12.dp))
                                            CustomerBalancePreviewBox(
                                                customer = cust,
                                                posPreview = linePosPreview
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

                                // Items in Line Sale Section
                                val lineItemsSubtotal = if (lineItems.isNotEmpty()) {
                                    lineItems.sumOf { (it.quantity.toDoubleOrNull() ?: 0.0) * (it.unitPrice.toDoubleOrNull() ?: 0.0) }
                                } else null

                                SaleItemsSection(
                                    items = lineItems,
                                    onAddItemClick = {
                                        selectorTarget = SaleType.LINE_SALE
                                        productSearchQuery = ""
                                        viewModel.loadProducts()
                                        showProductSelectorSheet = true
                                    },
                                    onQuantityChange = { entry, newQty ->
                                        val filtered = newQty.filter { it.isDigit() }
                                        val updated = lineItems.map { if (it.id == entry.id) it.copy(quantity = filtered) else it }
                                        lineItems = updated
                                        val subtotal = updated.sumOf { (it.quantity.toDoubleOrNull() ?: 0.0) * (it.unitPrice.toDoubleOrNull() ?: 0.0) }
                                        lineSubtotal = String.format(java.util.Locale.US, "%.2f", subtotal)
                                        lineTotalAmount = SaleCalculationUtils.calculateSaleTotal(lineSubtotal, lineDiscount)
                                    },
                                    onQuantityIncrement = { entry ->
                                        val currentQ = entry.quantity.toIntOrNull() ?: 1
                                        val updated = lineItems.map { if (it.id == entry.id) it.copy(quantity = (currentQ + 1).toString()) else it }
                                        lineItems = updated
                                        val subtotal = updated.sumOf { (it.quantity.toDoubleOrNull() ?: 0.0) * (it.unitPrice.toDoubleOrNull() ?: 0.0) }
                                        lineSubtotal = String.format(java.util.Locale.US, "%.2f", subtotal)
                                        lineTotalAmount = SaleCalculationUtils.calculateSaleTotal(lineSubtotal, lineDiscount)
                                    },
                                    onQuantityDecrement = { entry ->
                                        val currentQ = entry.quantity.toIntOrNull() ?: 1
                                        if (currentQ > 1) {
                                            val updated = lineItems.map { if (it.id == entry.id) it.copy(quantity = (currentQ - 1).toString()) else it }
                                            lineItems = updated
                                            val subtotal = updated.sumOf { (it.quantity.toDoubleOrNull() ?: 0.0) * (it.unitPrice.toDoubleOrNull() ?: 0.0) }
                                            lineSubtotal = String.format(java.util.Locale.US, "%.2f", subtotal)
                                            lineTotalAmount = SaleCalculationUtils.calculateSaleTotal(lineSubtotal, lineDiscount)
                                        }
                                    },
                                    onUnitPriceChange = { entry, newPrice ->
                                        val updated = lineItems.map { if (it.id == entry.id) it.copy(unitPrice = newPrice) else it }
                                        lineItems = updated
                                        val subtotal = updated.sumOf { (it.quantity.toDoubleOrNull() ?: 0.0) * (it.unitPrice.toDoubleOrNull() ?: 0.0) }
                                        lineSubtotal = String.format(java.util.Locale.US, "%.2f", subtotal)
                                        lineTotalAmount = SaleCalculationUtils.calculateSaleTotal(lineSubtotal, lineDiscount)
                                    },
                                    onRemoveItem = { entry ->
                                        val updated = lineItems.filterNot { it.id == entry.id }
                                        lineItems = updated
                                        val subtotal = updated.sumOf { (it.quantity.toDoubleOrNull() ?: 0.0) * (it.unitPrice.toDoubleOrNull() ?: 0.0) }
                                        if (updated.isNotEmpty()) {
                                            lineSubtotal = String.format(java.util.Locale.US, "%.2f", subtotal)
                                            lineTotalAmount = SaleCalculationUtils.calculateSaleTotal(lineSubtotal, lineDiscount)
                                        }
                                    }
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                // Subtotal & Custom Discount Row
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.Top,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    VyavsaTextField(
                                        value = lineSubtotal,
                                        onValueChange = {
                                            lineSubtotal = it
                                            lineTotalAmount = SaleCalculationUtils.calculateSaleTotal(it, lineDiscount)
                                        },
                                        label = "Subtotal (₹) *",
                                        placeholder = "0.00",
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                                        modifier = Modifier.weight(1f)
                                    )
                                    VyavsaTextField(
                                        value = lineDiscount,
                                        onValueChange = {
                                            lineDiscount = it
                                            val effSub = lineSubtotal.ifBlank {
                                                if (lineItems.isNotEmpty()) {
                                                    String.format(java.util.Locale.US, "%.2f", lineItems.sumOf { (it.quantity.toDoubleOrNull() ?: 0.0) * (it.unitPrice.toDoubleOrNull() ?: 0.0) })
                                                } else lineTotalAmount
                                            }
                                            lineTotalAmount = SaleCalculationUtils.calculateSaleTotal(effSub, it)
                                        },
                                        label = "Discount Amount (₹)",
                                        placeholder = "0.00",
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Final Invoice Amount & Cash In
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.Top,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    VyavsaTextField(
                                        value = lineTotalAmount,
                                        onValueChange = { lineTotalAmount = it },
                                        label = "Final Payable (₹) *",
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
                                    subtotal = lineSubtotal.ifBlank { lineTotalAmount },
                                    discount = lineDiscount,
                                    totalAmount = lineTotalAmount,
                                    cashAmount = lineCashIn,
                                    bankEntries = if (lineIncludeBank) lineBankEntries else emptyList(),
                                    isLineSale = true,
                                    itemsSubtotal = lineItemsSubtotal,
                                    customer = selectedCustomer,
                                    posAllocationPreview = if (selectedCustomer != null) linePosPreview else null
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                VyavsaTextField(
                                    value = note,
                                    onValueChange = { note = it },
                                    label = "Note / Dispatch Reference",
                                    placeholder = "Optional delivery remarks",
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                                )

                                Spacer(modifier = Modifier.height(20.dp))

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

                                Spacer(modifier = Modifier.height(14.dp))

                                SaleDateSelectorRow(
                                    saleDate = saleDate,
                                    isAdmin = viewModel.isAdmin,
                                    onSelectDateClick = {
                                        if (viewModel.isAdmin) {
                                            showDatePicker = true
                                        } else {
                                            Toast.makeText(context, "Non-admins can only record sales for today", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                // Optional Customer Selector for Counter Sale
                                if (counterSelectedCustomer == null) {
                                    Surface(
                                        shape = RoundedCornerShape(18.dp),
                                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(18.dp))
                                            .clickable {
                                                customerSelectorTarget = SaleType.COUNTER_SALE
                                                customerSearchQuery = ""
                                                showCustomerSelectorSheet = true
                                            }
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 14.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Person,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(22.dp)
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "Customer Account (Optional)",
                                                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                )
                                                Text(
                                                    text = "Walk-in Customer (Tap to select registered account)",
                                                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
                                                )
                                            }
                                            Icon(
                                                imageVector = Icons.Default.Search,
                                                contentDescription = "Search",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                } else {
                                    Surface(
                                        shape = RoundedCornerShape(18.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.22f),
                                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                        ) {
                                            AvatarInitialCircle(
                                                name = counterSelectedCustomer!!.customerName,
                                                modifier = Modifier.size(42.dp)
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = counterSelectedCustomer!!.customerName,
                                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                                )
                                                if (!counterSelectedCustomer!!.phone.isNullOrBlank()) {
                                                    Text(
                                                        text = counterSelectedCustomer!!.phone!!,
                                                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    )
                                                }
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    modifier = Modifier.padding(top = 4.dp)
                                                ) {
                                                    val due = counterSelectedCustomer!!.currentBalance.toDoubleOrNull() ?: 0.0
                                                    val adv = counterSelectedCustomer!!.advanceBalance.toDoubleOrNull() ?: 0.0
                                                    StatBadge(
                                                        text = "Due: ${FormatUtils.formatCurrency(counterSelectedCustomer!!.currentBalance)}",
                                                        backgroundColor = if (due > 0) Crimson50 else Emerald50,
                                                        textColor = if (due > 0) Crimson600 else Emerald600
                                                    )
                                                    if (adv > 0) {
                                                        StatBadge(
                                                            text = "Adv: ${FormatUtils.formatCurrency(counterSelectedCustomer!!.advanceBalance)}",
                                                            backgroundColor = Blue50,
                                                            textColor = Blue600
                                                        )
                                                    }
                                                }
                                            }
                                            Column(horizontalAlignment = Alignment.End) {
                                                TextButton(
                                                    onClick = {
                                                        customerSelectorTarget = SaleType.COUNTER_SALE
                                                        customerSearchQuery = ""
                                                        showCustomerSelectorSheet = true
                                                    }
                                                ) {
                                                    Text("Change", fontWeight = FontWeight.Bold)
                                                }
                                                TextButton(
                                                    onClick = {
                                                        counterSelectedCustomer = null
                                                    }
                                                ) {
                                                    Text("Walk-in", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                                                }
                                            }
                                        }
                                    }
                                }

                                AnimatedVisibility(
                                    visible = counterSelectedCustomer != null,
                                    enter = expandVertically() + fadeIn(),
                                    exit = shrinkVertically() + fadeOut()
                                ) {
                                    counterSelectedCustomer?.let { cust ->
                                        Column {
                                            Spacer(modifier = Modifier.height(12.dp))
                                            CustomerBalancePreviewBox(
                                                customer = cust,
                                                posPreview = counterPosPreview
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Items in Counter Sale Section
                                val counterItemsSubtotal = if (counterItems.isNotEmpty()) {
                                    counterItems.sumOf { (it.quantity.toDoubleOrNull() ?: 0.0) * (it.unitPrice.toDoubleOrNull() ?: 0.0) }
                                } else null

                                SaleItemsSection(
                                    items = counterItems,
                                    onAddItemClick = {
                                        selectorTarget = SaleType.COUNTER_SALE
                                        productSearchQuery = ""
                                        viewModel.loadProducts()
                                        showProductSelectorSheet = true
                                    },
                                    onQuantityChange = { entry, newQty ->
                                        val filtered = newQty.filter { it.isDigit() }
                                        val updated = counterItems.map { if (it.id == entry.id) it.copy(quantity = filtered) else it }
                                        counterItems = updated
                                        val subtotal = updated.sumOf { (it.quantity.toDoubleOrNull() ?: 0.0) * (it.unitPrice.toDoubleOrNull() ?: 0.0) }
                                        counterSubtotal = String.format(java.util.Locale.US, "%.2f", subtotal)
                                        counterTotalAmount = SaleCalculationUtils.calculateSaleTotal(counterSubtotal, counterDiscount)
                                    },
                                    onQuantityIncrement = { entry ->
                                        val currentQ = entry.quantity.toIntOrNull() ?: 1
                                        val updated = counterItems.map { if (it.id == entry.id) it.copy(quantity = (currentQ + 1).toString()) else it }
                                        counterItems = updated
                                        val subtotal = updated.sumOf { (it.quantity.toDoubleOrNull() ?: 0.0) * (it.unitPrice.toDoubleOrNull() ?: 0.0) }
                                        counterSubtotal = String.format(java.util.Locale.US, "%.2f", subtotal)
                                        counterTotalAmount = SaleCalculationUtils.calculateSaleTotal(counterSubtotal, counterDiscount)
                                    },
                                    onQuantityDecrement = { entry ->
                                        val currentQ = entry.quantity.toIntOrNull() ?: 1
                                        if (currentQ > 1) {
                                            val updated = counterItems.map { if (it.id == entry.id) it.copy(quantity = (currentQ - 1).toString()) else it }
                                            counterItems = updated
                                            val subtotal = updated.sumOf { (it.quantity.toDoubleOrNull() ?: 0.0) * (it.unitPrice.toDoubleOrNull() ?: 0.0) }
                                            counterSubtotal = String.format(java.util.Locale.US, "%.2f", subtotal)
                                            counterTotalAmount = SaleCalculationUtils.calculateSaleTotal(counterSubtotal, counterDiscount)
                                        }
                                    },
                                    onUnitPriceChange = { entry, newPrice ->
                                        val updated = counterItems.map { if (it.id == entry.id) it.copy(unitPrice = newPrice) else it }
                                        counterItems = updated
                                        val subtotal = updated.sumOf { (it.quantity.toDoubleOrNull() ?: 0.0) * (it.unitPrice.toDoubleOrNull() ?: 0.0) }
                                        counterSubtotal = String.format(java.util.Locale.US, "%.2f", subtotal)
                                        counterTotalAmount = SaleCalculationUtils.calculateSaleTotal(counterSubtotal, counterDiscount)
                                    },
                                    onRemoveItem = { entry ->
                                        val updated = counterItems.filterNot { it.id == entry.id }
                                        counterItems = updated
                                        val subtotal = updated.sumOf { (it.quantity.toDoubleOrNull() ?: 0.0) * (it.unitPrice.toDoubleOrNull() ?: 0.0) }
                                        if (updated.isNotEmpty()) {
                                            counterSubtotal = String.format(java.util.Locale.US, "%.2f", subtotal)
                                            counterTotalAmount = SaleCalculationUtils.calculateSaleTotal(counterSubtotal, counterDiscount)
                                        }
                                    }
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                VyavsaTextField(
                                    value = counterItem,
                                    onValueChange = { counterItem = it },
                                    label = "Item / Description *",
                                    placeholder = "e.g. Retail goods, Bill #108"
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                // Subtotal & Discount Row
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.Top,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    VyavsaTextField(
                                        value = counterSubtotal.ifBlank { counterPrice },
                                        onValueChange = {
                                            counterPrice = it
                                            counterSubtotal = it
                                            counterTotalAmount = SaleCalculationUtils.calculateSaleTotal(it, counterDiscount)
                                        },
                                        label = "Subtotal / Price (₹) *",
                                        placeholder = "0.00",
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                                        modifier = Modifier.weight(1f)
                                    )

                                    VyavsaTextField(
                                        value = counterDiscount,
                                        onValueChange = {
                                            counterDiscount = it
                                            val effSub = counterSubtotal.ifBlank {
                                                if (counterItems.isNotEmpty()) {
                                                    String.format(java.util.Locale.US, "%.2f", counterItems.sumOf { (it.quantity.toDoubleOrNull() ?: 0.0) * (it.unitPrice.toDoubleOrNull() ?: 0.0) })
                                                } else counterPrice
                                            }
                                            counterTotalAmount = SaleCalculationUtils.calculateSaleTotal(effSub, it)
                                        },
                                        label = "Discount Amount (₹)",
                                        placeholder = "0.00",
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.Top,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    VyavsaTextField(
                                        value = counterTotalAmount,
                                        onValueChange = { counterTotalAmount = it },
                                        label = "Final Amount (₹) *",
                                        placeholder = "0.00",
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                                        modifier = Modifier.weight(1f)
                                    )

                                    VyavsaTextField(
                                        value = counterCash,
                                        onValueChange = { counterCash = it },
                                        label = "Physical Cash (₹)",
                                        placeholder = "0.00",
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                                        modifier = Modifier.weight(1f)
                                    )
                                }

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
                                    subtotal = counterSubtotal.ifBlank { counterPrice.ifBlank { counterTotalAmount } },
                                    discount = counterDiscount,
                                    totalAmount = counterTotalAmount,
                                    cashAmount = counterCash,
                                    bankEntries = if (counterIncludeBank) counterBankEntries else emptyList(),
                                    isLineSale = false,
                                    itemsSubtotal = counterItemsSubtotal,
                                    customer = counterSelectedCustomer,
                                    posAllocationPreview = if (counterSelectedCustomer != null) counterPosPreview else null
                                )

                                Spacer(modifier = Modifier.height(20.dp))
                            }
                        }
                    }
                }
            }
        }

        // Searchable Product Selector Sheet
        if (showProductSelectorSheet) {
            ProductSelectorBottomSheet(
                productsState = productsState,
                searchQuery = productSearchQuery,
                onSearchQueryChange = { query ->
                    productSearchQuery = query
                    viewModel.loadProducts(query)
                },
                onProductSelected = { selectedProduct ->
                    if (selectorTarget == SaleType.LINE_SALE) {
                        val existing = lineItems.find { it.productId == selectedProduct.id }
                        val updated = if (existing != null) {
                            val currQ = existing.quantity.toIntOrNull() ?: 1
                            lineItems.map {
                                if (it.productId == selectedProduct.id) it.copy(quantity = (currQ + 1).toString()) else it
                            }
                        } else {
                            lineItems + SaleItemEntry(
                                productId = selectedProduct.id,
                                productName = selectedProduct.name,
                                catalogUnitPrice = selectedProduct.unitPrice,
                                quantity = "1",
                                unitPrice = selectedProduct.unitPrice
                            )
                        }
                        lineItems = updated
                        val subtotal = updated.sumOf { (it.quantity.toDoubleOrNull() ?: 0.0) * (it.unitPrice.toDoubleOrNull() ?: 0.0) }
                        lineSubtotal = String.format(java.util.Locale.US, "%.2f", subtotal)
                        lineTotalAmount = SaleCalculationUtils.calculateSaleTotal(lineSubtotal, lineDiscount)
                    } else {
                        val existing = counterItems.find { it.productId == selectedProduct.id }
                        val updated = if (existing != null) {
                            val currQ = existing.quantity.toIntOrNull() ?: 1
                            counterItems.map {
                                if (it.productId == selectedProduct.id) it.copy(quantity = (currQ + 1).toString()) else it
                            }
                        } else {
                            counterItems + SaleItemEntry(
                                productId = selectedProduct.id,
                                productName = selectedProduct.name,
                                catalogUnitPrice = selectedProduct.unitPrice,
                                quantity = "1",
                                unitPrice = selectedProduct.unitPrice
                            )
                        }
                        counterItems = updated
                        val subtotal = updated.sumOf { (it.quantity.toDoubleOrNull() ?: 0.0) * (it.unitPrice.toDoubleOrNull() ?: 0.0) }
                        counterSubtotal = String.format(java.util.Locale.US, "%.2f", subtotal)
                        counterTotalAmount = SaleCalculationUtils.calculateSaleTotal(counterSubtotal, counterDiscount)
                        if (counterItem.isBlank() || counterItem.startsWith(selectedProduct.name)) {
                            counterItem = if (updated.size == 1) updated.first().productName else "${updated.first().productName} (+${updated.size - 1} items)"
                        }
                        if (updated.size == 1) {
                            counterPrice = updated.first().unitPrice
                        }
                    }
                    showProductSelectorSheet = false
                },
                onDismiss = { showProductSelectorSheet = false }
            )
        }

        // Searchable Customer Selector Sheet
        if (showCustomerSelectorSheet) {
            CustomerSelectorBottomSheet(
                customersState = customersState,
                searchQuery = customerSearchQuery,
                onSearchQueryChange = { query ->
                    customerSearchQuery = query
                    viewModel.setCustomerSearchQuery(query)
                },
                allowWalkIn = customerSelectorTarget == SaleType.COUNTER_SALE,
                onAddNewCustomerClick = {
                    showQuickAddCustomerDialog = true
                },
                onRetry = {
                    viewModel.loadCustomers(customerSearchQuery.ifBlank { null })
                },
                onCustomerSelected = { customer ->
                    if (customerSelectorTarget == SaleType.LINE_SALE) {
                        selectedCustomer = customer
                    } else {
                        counterSelectedCustomer = customer
                    }
                    showCustomerSelectorSheet = false
                },
                onDismiss = { showCustomerSelectorSheet = false }
            )
        }

        // Quick Add Customer Dialog
        if (showQuickAddCustomerDialog) {
            QuickAddCustomerDialog(
                initialName = customerSearchQuery,
                onDismiss = { showQuickAddCustomerDialog = false },
                onConfirm = { name, phone, openingBalance ->
                    showQuickAddCustomerDialog = false
                    viewModel.createCustomer(name, phone, openingBalance) { success, createdCustomer, _ ->
                        if (success && createdCustomer != null) {
                            if (customerSelectorTarget == SaleType.LINE_SALE) {
                                selectedCustomer = createdCustomer
                            } else {
                                counterSelectedCustomer = createdCustomer
                            }
                            showCustomerSelectorSheet = false
                        }
                    }
                }
            )
        }

        // Print failure dialog after successful sale creation
        if (savedSaleForPrintFailure != null) {
            val sale = savedSaleForPrintFailure!!
            AlertDialog(
                onDismissRequest = {
                    savedSaleForPrintFailure = null
                    onSaleCreated()
                    viewModel.resetCreateState()
                },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(32.dp)
                    )
                },
                title = {
                    Text(
                        text = "Sale Saved — Print Failed",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                text = {
                    Column {
                        val invoiceNum = if (!sale.invoiceNumber.isNullOrBlank()) sale.invoiceNumber else sale.id.take(8).uppercase()
                        Text(
                            text = "Line sale $invoiceNum was successfully saved to your records, but printing the POS invoice failed.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (!printFailureMessage.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = printFailureMessage ?: "",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "You can retry printing now, or return to the sales list. The sale is already safely preserved.",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                },
                confirmButton = {
                    VyavsaButton(
                        text = "Retry Print",
                        leadingIcon = Icons.Default.Print,
                        isLoading = isRetryingPrint,
                        enabled = !isRetryingPrint,
                        onClick = {
                            isRetryingPrint = true
                            viewModel.printLineSaleWithResult(
                                sale = sale,
                                context = context,
                                onPermissionRequired = { perms ->
                                    permissionLauncher.launch(perms)
                                },
                                onResult = { isSuccess, err ->
                                    isRetryingPrint = false
                                    if (isSuccess) {
                                        Toast.makeText(context, "Invoice printed successfully", Toast.LENGTH_SHORT).show()
                                        savedSaleForPrintFailure = null
                                        onSaleCreated()
                                        viewModel.resetCreateState()
                                    } else {
                                        printFailureMessage = err ?: "Printer connection failed"
                                    }
                                }
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                dismissButton = {
                    VyavsaOutlinedButton(
                        text = "Done (Back to Sales)",
                        enabled = !isRetryingPrint,
                        onClick = {
                            savedSaleForPrintFailure = null
                            onSaleCreated()
                            viewModel.resetCreateState()
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            )
        }
    }
}

@Composable
private fun SaleItemsSection(
    items: List<SaleItemEntry>,
    onAddItemClick: () -> Unit,
    onQuantityChange: (SaleItemEntry, String) -> Unit,
    onQuantityIncrement: (SaleItemEntry) -> Unit,
    onQuantityDecrement: (SaleItemEntry) -> Unit,
    onUnitPriceChange: (SaleItemEntry, String) -> Unit,
    onRemoveItem: (SaleItemEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)),
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
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingBag,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Items in this Sale",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Catalog products with auto pricing",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                }

                if (items.isNotEmpty()) {
                    StatBadge(
                        text = "${items.size} ${if (items.size == 1) "item" else "items"}",
                        backgroundColor = MaterialTheme.colorScheme.primaryContainer,
                        textColor = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (items.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "No catalog products added to this sale.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        VyavsaOutlinedButton(
                            text = "+ Add Item from Catalog",
                            leadingIcon = Icons.Default.Add,
                            onClick = onAddItemClick,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items.forEach { item ->
                        SaleItemCard(
                            productName = item.productName,
                            catalogUnitPrice = item.catalogUnitPrice,
                            quantity = item.quantity,
                            unitPrice = item.unitPrice,
                            lineTotal = item.lineTotal,
                            onQuantityChange = { onQuantityChange(item, it) },
                            onQuantityIncrement = { onQuantityIncrement(item) },
                            onQuantityDecrement = { onQuantityDecrement(item) },
                            onUnitPriceChange = { onUnitPriceChange(item, it) },
                            onRemove = { onRemoveItem(item) }
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    VyavsaOutlinedButton(
                        text = "+ Add Another Item",
                        leadingIcon = Icons.Default.Add,
                        onClick = onAddItemClick,
                        modifier = Modifier.fillMaxWidth()
                    )

                    val subtotal = items.sumOf { (it.quantity.toDoubleOrNull() ?: 0.0) * (it.unitPrice.toDoubleOrNull() ?: 0.0) }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = "Items Subtotal (${items.size} ${if (items.size == 1) "item" else "items"}):",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                            Text(
                                text = FormatUtils.formatCurrency(subtotal.toString()),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
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
private fun SaleDateSelectorRow(
    saleDate: String,
    isAdmin: Boolean,
    onSelectDateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Sale Date",
                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                    Text(
                        text = saleDate,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
            TextButton(
                onClick = onSelectDateClick
            ) {
                Text(
                    text = if (isAdmin) "Change Date" else "Today Only",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = if (isAdmin) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    }
}

@Composable
private fun CustomerBalancePreviewBox(
    customer: Customer,
    posPreview: PosAllocationPreview,
    modifier: Modifier = Modifier
) {
    val curDue = customer.currentBalance.toDoubleOrNull() ?: 0.0
    val availAdv = customer.advanceBalance.toDoubleOrNull() ?: 0.0
    val advApplied = posPreview.advanceApplied.toDouble()
    val netPayable = posPreview.remainingPayable.toDouble()
    val prevSettled = posPreview.previousDuesSettled.toDouble()
    val newAdv = posPreview.advanceGenerated.toDouble()
    val updatedDue = posPreview.updatedCustomerDue.toDouble()
    val updatedAdv = posPreview.updatedAdvanceBalance.toDouble()

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
                    text = "Customer Outstanding Due:",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
                StatBadge(
                    text = FormatUtils.formatCurrency(curDue.toString()),
                    backgroundColor = if (curDue > 0.0) Crimson50 else Emerald50,
                    textColor = if (curDue > 0.0) Crimson600 else Emerald600
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Available Advance Balance:",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
                StatBadge(
                    text = FormatUtils.formatCurrency(availAdv.toString()),
                    backgroundColor = if (availAdv > 0.0) Blue50 else MaterialTheme.colorScheme.surfaceContainerHigh,
                    textColor = if (availAdv > 0.0) Blue600 else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (advApplied > 0.0) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Advance Auto-Applied to Sale:",
                        style = MaterialTheme.typography.bodySmall.copy(color = Emerald600, fontWeight = FontWeight.SemiBold)
                    )
                    Text(
                        text = "-${FormatUtils.formatCurrency(advApplied.toString())}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = Emerald600
                        )
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Remaining Net Sale Payable:",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = FormatUtils.formatCurrency(netPayable.toString()),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            if (prevSettled > 0.0) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Settles Previous Customer Dues:",
                        style = MaterialTheme.typography.bodySmall.copy(color = Emerald600, fontWeight = FontWeight.SemiBold)
                    )
                    Text(
                        text = "-${FormatUtils.formatCurrency(prevSettled.toString())}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = Emerald600
                        )
                    )
                }
            }

            if (newAdv > 0.0) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Remaining Excess Stored as Advance:",
                        style = MaterialTheme.typography.bodySmall.copy(color = Blue600, fontWeight = FontWeight.SemiBold)
                    )
                    Text(
                        text = "+${FormatUtils.formatCurrency(newAdv.toString())}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = Blue600
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Resulting Outstanding Due:",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = FormatUtils.formatCurrency(updatedDue.toString()),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (updatedDue > 0.0) Crimson600 else Emerald600
                    )
                )
            }

            if (updatedAdv > 0.0) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Resulting Advance Balance:",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = FormatUtils.formatCurrency(updatedAdv.toString()),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = Blue600
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun SaleCalculationBox(
    subtotal: String = "",
    discount: String = "0.00",
    totalAmount: String,
    cashAmount: String,
    bankEntries: List<BankPaymentEntry> = emptyList(),
    isLineSale: Boolean,
    itemsSubtotal: Double? = null,
    customer: Customer? = null,
    posAllocationPreview: PosAllocationPreview? = null,
    modifier: Modifier = Modifier
) {
    val total = totalAmount.toDoubleOrNull() ?: 0.0
    val sub = subtotal.toDoubleOrNull() ?: (itemsSubtotal ?: total)
    val disc = discount.toDoubleOrNull() ?: 0.0
    val cash = cashAmount.toDoubleOrNull() ?: 0.0
    val bankTotal = bankEntries.sumOf { it.amount.toDoubleOrNull() ?: 0.0 }
    val collected = cash + bankTotal
    val due = if (total > collected) total - collected else 0.0
    val invalidDiscount = disc < 0.0 || (sub > 0.0 && disc > sub)

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (invalidDiscount) Crimson50.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceContainerLow
        ),
        modifier = modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (invalidDiscount) Crimson600.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant,
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
                Text(
                    text = if (itemsSubtotal != null && itemsSubtotal > 0.0) "Items Subtotal:" else "Sale Subtotal:",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
                Text(
                    text = FormatUtils.formatCurrency(sub.toString()),
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold)
                )
            }

            if (disc > 0.0 || discount.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Discount Amount:",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (disc > 0.0) Emerald600 else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (disc > 0.0) FontWeight.Bold else FontWeight.Normal
                        )
                    )
                    Text(
                        text = if (disc > 0.0) "-${FormatUtils.formatCurrency(disc.toString())}" else FormatUtils.formatCurrency("0.00"),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = if (disc > 0.0) Emerald600 else MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Sale Total:", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                Text(
                    text = FormatUtils.formatCurrency(total.toString()),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
            }

            if (posAllocationPreview != null && posAllocationPreview.advanceApplied.toDouble() > 0.0) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Paid using Advance:", style = MaterialTheme.typography.bodySmall.copy(color = Emerald600, fontWeight = FontWeight.Bold))
                    Text(
                        text = "-${FormatUtils.formatCurrency(posAllocationPreview.advanceAppliedString)}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = Emerald600
                        )
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Remaining Net Payable:", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                    Text(
                        text = FormatUtils.formatCurrency(posAllocationPreview.remainingPayableString),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

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
                Text("Total Received Directly (Cash + Bank):", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                Text(
                    text = FormatUtils.formatCurrency(collected.toString()),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold,
                        color = Emerald600
                    )
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            if (posAllocationPreview != null) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Balance Due for this Sale:",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                    Text(
                        text = FormatUtils.formatCurrency(posAllocationPreview.remainingSaleDueString),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = if (posAllocationPreview.remainingSaleDue.toDouble() > 0.0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                        )
                    )
                }

                if (posAllocationPreview.excessPayment.toDouble() > 0.0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Excess Payment Collected:", style = MaterialTheme.typography.bodySmall.copy(color = Emerald600, fontWeight = FontWeight.SemiBold))
                        Text(
                            text = FormatUtils.formatCurrency(posAllocationPreview.excessPaymentString),
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = Emerald600)
                        )
                    }

                    if (posAllocationPreview.previousDuesSettled.toDouble() > 0.0) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("↳ Settled Previous Dues:", style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                            Text(
                                text = "-${FormatUtils.formatCurrency(posAllocationPreview.previousDuesSettledString)}",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold, color = Emerald600)
                            )
                        }
                    }

                    if (posAllocationPreview.advanceGenerated.toDouble() > 0.0) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("↳ Added to Advance Balance:", style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                            Text(
                                text = "+${FormatUtils.formatCurrency(posAllocationPreview.advanceGeneratedString)}",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold, color = Blue600)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Updated Customer Total Due:", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                    Text(
                        text = FormatUtils.formatCurrency(posAllocationPreview.updatedCustomerDueString),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (posAllocationPreview.updatedCustomerDue.toDouble() > 0.0) Crimson600 else Emerald600
                        )
                    )
                }
            } else {
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

                if (collected > total && total > 0.0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Change / Excess Collected:", style = MaterialTheme.typography.bodySmall.copy(color = Emerald600, fontWeight = FontWeight.Bold))
                        Text(
                            text = FormatUtils.formatCurrency((collected - total).toString()),
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = Emerald600)
                        )
                    }
                }
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

            if (disc < 0.0) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "⚠️ Discount cannot be negative",
                    style = MaterialTheme.typography.bodySmall.copy(color = Crimson600, fontWeight = FontWeight.Bold)
                )
            } else if (sub > 0.0 && disc > sub) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "⚠️ Discount (${FormatUtils.formatCurrency(disc.toString())}) cannot exceed subtotal (${FormatUtils.formatCurrency(sub.toString())})",
                    style = MaterialTheme.typography.bodySmall.copy(color = Crimson600, fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}
