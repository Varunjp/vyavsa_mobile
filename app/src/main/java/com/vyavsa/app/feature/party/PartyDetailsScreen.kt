package com.vyavsa.app.feature.party

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vyavsa.app.core.common.FormatUtils
import com.vyavsa.app.core.common.ObserveAsEvents
import com.vyavsa.app.core.common.UiState
import com.vyavsa.app.core.designsystem.AvatarInitialCircle
import com.vyavsa.app.core.designsystem.Crimson50
import com.vyavsa.app.core.designsystem.Crimson600
import com.vyavsa.app.core.designsystem.Emerald50
import com.vyavsa.app.core.designsystem.Emerald600
import com.vyavsa.app.core.designsystem.ErrorBanner
import com.vyavsa.app.core.designsystem.StatBadge
import com.vyavsa.app.core.designsystem.VyavsaButton
import com.vyavsa.app.core.designsystem.VyavsaOutlinedButton
import com.vyavsa.app.core.designsystem.VyavsaTopAppBar
import com.vyavsa.app.core.model.Party
import com.vyavsa.app.core.model.PartyDetail

@Composable
fun PartyDetailsScreen(
    partyId: String,
    viewModel: PartyViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToStatement: ((customerId: String, customerName: String) -> Unit)? = null,
    onCollectPayment: ((party: Party) -> Unit)? = null,
    onPaySupplier: ((party: Party) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val partyDetailState by viewModel.selectedPartyDetail.collectAsState()
    val actionState by viewModel.actionState.collectAsState()
    val canUpdateParty = viewModel.canUpdateParty

    val snackbarHostState = remember { SnackbarHostState() }
    ObserveAsEvents(viewModel.uiEvents) { msg ->
        snackbarHostState.showSnackbar(msg.text)
    }

    var showEditDialog by remember { mutableStateOf(false) }

    LaunchedEffect(partyId) {
        viewModel.loadPartyDetails(partyId)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            VyavsaTopAppBar(
                title = "Party Profile",
                subtitle = "Comprehensive business ledger & mutual balance",
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                onNavigationClick = onNavigateBack,
                actions = {
                    IconButton(onClick = { viewModel.loadPartyDetails(partyId) }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (canUpdateParty && partyDetailState is UiState.Success) {
                        IconButton(onClick = { showEditDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Party",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = partyDetailState) {
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
                            .padding(20.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        ErrorBanner(message = state.message)
                        Spacer(modifier = Modifier.height(16.dp))
                        VyavsaButton(
                            text = "Retry",
                            onClick = { viewModel.loadPartyDetails(partyId) }
                        )
                    }
                }
                is UiState.Success -> {
                    val detail = state.data
                    val net = detail.netBalance.toDoubleOrNull() ?: 0.0
                    val recv = detail.totalReceivable.toDoubleOrNull() ?: 0.0
                    val pay = detail.totalPayable.toDoubleOrNull() ?: 0.0
                    val contra = detail.maxContraSettlement.toDoubleOrNull() ?: 0.0

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            start = 20.dp,
                            end = 20.dp,
                            top = 12.dp,
                            bottom = 32.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Profile Header Card
                        item {
                            Card(
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(20.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        AvatarInitialCircle(
                                            name = detail.name,
                                            modifier = Modifier.size(52.dp)
                                        )
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = detail.name,
                                                style = MaterialTheme.typography.titleLarge.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                // Role Pill
                                                StatBadge(
                                                    text = when {
                                                        detail.isBoth || detail.role == "both" -> "Customer & Supplier"
                                                        detail.isSupplier || detail.role == "supplier" -> "Supplier"
                                                        else -> "Customer"
                                                    },
                                                    backgroundColor = when {
                                                        detail.isBoth || detail.role == "both" -> Color(0xFFE0E7FF)
                                                        detail.isSupplier || detail.role == "supplier" -> Color(0xFFFEF3C7)
                                                        else -> Color(0xFFDBEAFE)
                                                    },
                                                    textColor = when {
                                                        detail.isBoth || detail.role == "both" -> Color(0xFF4338CA)
                                                        detail.isSupplier || detail.role == "supplier" -> Color(0xFFB45309)
                                                        else -> Color(0xFF1D4ED8)
                                                    }
                                                )

                                                // Status Pill
                                                StatBadge(
                                                    text = detail.status.replaceFirstChar { it.uppercase() },
                                                    backgroundColor = if (detail.status == "active") Emerald50 else Crimson50,
                                                    textColor = if (detail.status == "active") Emerald600 else Crimson600
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))
                                    androidx.compose.material3.HorizontalDivider(
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Contact details
                                    if (!detail.phone.isNullOrBlank()) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(vertical = 3.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Phone,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = detail.phone,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            )
                                        }
                                    }

                                    if (!detail.email.isNullOrBlank()) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(vertical = 3.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Email,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = detail.email,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            )
                                        }
                                    }

                                    if (!detail.address.isNullOrBlank()) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(vertical = 3.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.LocationOn,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = detail.address,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Net Balance Highlight Card
                        item {
                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = when {
                                        net > 0.009 -> Emerald50
                                        net < -0.009 -> Crimson50
                                        else -> MaterialTheme.colorScheme.surfaceContainer
                                    }
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    when {
                                        net > 0.009 -> Emerald600.copy(alpha = 0.3f)
                                        net < -0.009 -> Crimson600.copy(alpha = 0.3f)
                                        else -> MaterialTheme.colorScheme.outlineVariant
                                    }
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(18.dp)) {
                                    Text(
                                        text = "Net Balance Position",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = when {
                                                net > 0.009 -> Emerald600
                                                net < -0.009 -> Crimson600
                                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                                            }
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = FormatUtils.formatCurrency(kotlin.math.abs(net)),
                                            style = MaterialTheme.typography.headlineMedium.copy(
                                                fontWeight = FontWeight.ExtraBold,
                                                color = when {
                                                    net > 0.009 -> Emerald600
                                                    net < -0.009 -> Crimson600
                                                    else -> MaterialTheme.colorScheme.onSurface
                                                }
                                            )
                                        )
                                        StatBadge(
                                            text = when {
                                                net > 0.009 -> "Party Owes Us"
                                                net < -0.009 -> "We Owe Party"
                                                else -> "Settled (₹0.00)"
                                            },
                                            backgroundColor = when {
                                                net > 0.009 -> Emerald600
                                                net < -0.009 -> Crimson600
                                                else -> MaterialTheme.colorScheme.outlineVariant
                                            },
                                            textColor = Color.White
                                        )
                                    }

                                    if (contra > 0.009) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = "Eligible for Contra Settlement up to ${FormatUtils.formatCurrency(contra)} to offset mutual dues.",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        // Financial KPI Metrics
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    text = "Accounts Receivable & Payable",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // Total Receivable
                                    Card(
                                        shape = RoundedCornerShape(18.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Text(
                                                text = "Total Receivable",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = Emerald600,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = FormatUtils.formatCurrency(recv),
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = if (recv > 0.009) Emerald600 else MaterialTheme.colorScheme.onSurface
                                                )
                                            )
                                        }
                                    }

                                    // Total Payable
                                    Card(
                                        shape = RoundedCornerShape(18.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Text(
                                                text = "Total Payable",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = Crimson600,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = FormatUtils.formatCurrency(pay),
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = if (pay > 0.009) Crimson600 else MaterialTheme.colorScheme.onSurface
                                                )
                                            )
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // Total Sales
                                    Card(
                                        shape = RoundedCornerShape(18.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Text(
                                                text = "Total Sales Billed",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = FormatUtils.formatCurrency(detail.totalSales),
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                            )
                                            Text(
                                                text = "Paid: ${FormatUtils.formatCurrency(detail.totalSalesPaid)}",
                                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            )
                                        }
                                    }

                                    // Total Purchases
                                    Card(
                                        shape = RoundedCornerShape(18.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Text(
                                                text = "Total Purchases",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = FormatUtils.formatCurrency(detail.totalPurchases),
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                            )
                                            Text(
                                                text = "Paid: ${FormatUtils.formatCurrency(detail.totalPurchasesPaid)}",
                                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Quick Actions
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (recv > 0.009 && onCollectPayment != null) {
                                    val partyObj = Party(
                                        id = detail.id,
                                        name = detail.name,
                                        phone = detail.phone,
                                        totalReceivable = detail.totalReceivable
                                    )
                                    VyavsaButton(
                                        text = "Collect Payment (${FormatUtils.formatCurrency(recv)})",
                                        onClick = { onCollectPayment(partyObj) },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                if (pay > 0.009 && onPaySupplier != null) {
                                    val partyObj = Party(
                                        id = detail.id,
                                        name = detail.name,
                                        phone = detail.phone,
                                        totalPayable = detail.totalPayable
                                    )
                                    VyavsaOutlinedButton(
                                        text = "Pay Supplier (${FormatUtils.formatCurrency(pay)})",
                                        onClick = { onPaySupplier(partyObj) },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                if (onNavigateToStatement != null && (detail.isCustomer || detail.isBoth)) {
                                    VyavsaOutlinedButton(
                                        text = "View Ledger Statement",
                                        onClick = { onNavigateToStatement(detail.id, detail.name) },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }

                    // Edit Dialog
                    if (showEditDialog) {
                        val partyObj = Party(
                            id = detail.id,
                            name = detail.name,
                            phone = detail.phone,
                            email = detail.email,
                            address = detail.address,
                            role = detail.role,
                            isCustomer = detail.isCustomer,
                            isSupplier = detail.isSupplier,
                            isBoth = detail.isBoth,
                            status = detail.status
                        )
                        EditPartyDialog(
                            party = partyObj,
                            isLoading = actionState is UiState.Loading,
                            onDismiss = { showEditDialog = false },
                            onSubmit = { name, phone, email, address, role, status ->
                                viewModel.updateParty(
                                    id = detail.id,
                                    name = name,
                                    phone = phone,
                                    email = email,
                                    address = address,
                                    role = role,
                                    status = status
                                )
                                showEditDialog = false
                            }
                        )
                    }
                }
                else -> Unit
            }
        }
    }
}
