package com.vyavsa.app.feature.party

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import com.vyavsa.app.core.designsystem.EmptyStateView
import com.vyavsa.app.core.designsystem.ErrorBanner
import com.vyavsa.app.core.designsystem.StatBadge
import com.vyavsa.app.core.designsystem.VyavsaButton
import com.vyavsa.app.core.designsystem.VyavsaOutlinedButton
import com.vyavsa.app.core.designsystem.VyavsaSearchBar
import com.vyavsa.app.core.designsystem.VyavsaTopAppBar
import com.vyavsa.app.core.designsystem.vyavsaClickable
import com.vyavsa.app.core.model.Party

@Composable
fun PartiesScreen(
    viewModel: PartyViewModel,
    onNavigateBack: (() -> Unit)? = null,
    onNavigateToPartyDetails: (partyId: String) -> Unit,
    onCollectPayment: ((party: Party) -> Unit)? = null,
    onPaySupplier: ((party: Party) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    ObserveAsEvents(viewModel.uiEvents) { msg ->
        snackbarHostState.showSnackbar(msg.text)
    }

    var showAddPartyDialog by remember { mutableStateOf(false) }
    var editingParty by remember { mutableStateOf<Party?>(null) }
    var deletingParty by remember { mutableStateOf<Party?>(null) }

    val actionState by viewModel.actionState.collectAsState()
    val canCreateParty = viewModel.canCreateParty

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            VyavsaTopAppBar(
                title = "Business Parties",
                subtitle = "Unified customer, supplier & net settlement directory",
                navigationIcon = if (onNavigateBack != null) Icons.AutoMirrored.Filled.ArrowBack else null,
                onNavigationClick = onNavigateBack
            )
        },
        floatingActionButton = {
            if (canCreateParty) {
                ExtendedFloatingActionButton(
                    onClick = { showAddPartyDialog = true },
                    shape = RoundedCornerShape(20.dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    icon = { Icon(imageVector = Icons.Default.Add, contentDescription = null) },
                    text = { Text("Add Party", fontWeight = FontWeight.Bold) }
                )
            }
        },
        modifier = modifier
    ) { paddingValues ->
        PartiesListContent(
            viewModel = viewModel,
            onPartyClick = { onNavigateToPartyDetails(it.id) },
            onEditClick = { editingParty = it },
            onDeleteClick = { deletingParty = it },
            onCollectClick = onCollectPayment,
            onPayClick = onPaySupplier,
            onAddNewPartyClick = { showAddPartyDialog = true },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        )

        // Add Party Dialog
        if (showAddPartyDialog) {
            CreatePartyDialog(
                isLoading = actionState is UiState.Loading,
                onDismiss = { showAddPartyDialog = false },
                onSubmit = { name, phone, email, address, role, recv, pay ->
                    viewModel.createParty(
                        name = name,
                        phone = phone,
                        email = email,
                        address = address,
                        role = role,
                        openingReceivable = recv,
                        openingPayable = pay
                    )
                    showAddPartyDialog = false
                }
            )
        }

        // Edit Party Dialog
        editingParty?.let { party ->
            EditPartyDialog(
                party = party,
                isLoading = actionState is UiState.Loading,
                onDismiss = { editingParty = null },
                onSubmit = { name, phone, email, address, role, status ->
                    viewModel.updateParty(
                        id = party.id,
                        name = name,
                        phone = phone,
                        email = email,
                        address = address,
                        role = role,
                        status = status
                    )
                    editingParty = null
                }
            )
        }

        // Delete Confirmation Dialog
        deletingParty?.let { party ->
            AlertDialog(
                onDismissRequest = { deletingParty = null },
                shape = RoundedCornerShape(20.dp),
                title = { Text("Delete Business Party", fontWeight = FontWeight.Bold) },
                text = {
                    Text("Are you sure you want to delete '${party.displayName}'? This action cannot be undone.")
                },
                confirmButton = {
                    VyavsaButton(
                        text = "Delete",
                        onClick = {
                            viewModel.deleteParty(party.id)
                            deletingParty = null
                        }
                    )
                },
                dismissButton = {
                    TextButton(onClick = { deletingParty = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun PartiesListContent(
    viewModel: PartyViewModel,
    onPartyClick: (Party) -> Unit,
    onEditClick: (Party) -> Unit,
    onDeleteClick: (Party) -> Unit,
    onCollectClick: ((Party) -> Unit)? = null,
    onPayClick: ((Party) -> Unit)? = null,
    onAddNewPartyClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val partiesState by viewModel.partiesState.collectAsState()
    val pagination by viewModel.paginationState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val roleFilter by viewModel.selectedRoleFilter.collectAsState()
    val balanceFilter by viewModel.selectedBalanceFilter.collectAsState()
    val canUpdateParty = viewModel.canUpdateParty
    val canDeleteParty = viewModel.canDeleteParty

    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Search bar
        VyavsaSearchBar(
            query = searchQuery,
            onQueryChange = { viewModel.onSearchChange(it) },
            placeholder = "Search party name, phone, email...",
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
        )

        // Filter chips rows
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Role Filter
            FilterChip(
                selected = roleFilter == "all",
                onClick = { viewModel.selectRoleFilter("all") },
                label = { Text("All Roles") }
            )
            FilterChip(
                selected = roleFilter == "both",
                onClick = { viewModel.selectRoleFilter("both") },
                label = { Text("Both (Cust & Supp)") }
            )
            FilterChip(
                selected = roleFilter == "customer",
                onClick = { viewModel.selectRoleFilter("customer") },
                label = { Text("Customers Only") }
            )
            FilterChip(
                selected = roleFilter == "supplier",
                onClick = { viewModel.selectRoleFilter("supplier") },
                label = { Text("Suppliers Only") }
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Balance Filter
            FilterChip(
                selected = balanceFilter == "all",
                onClick = { viewModel.selectBalanceFilter("all") },
                label = { Text("All Balances") }
            )
            FilterChip(
                selected = balanceFilter == "dues",
                onClick = { viewModel.selectBalanceFilter("dues") },
                label = { Text("Has Dues") }
            )
            FilterChip(
                selected = balanceFilter == "settleable",
                onClick = { viewModel.selectBalanceFilter("settleable") },
                label = { Text("Eligible for Contra") }
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        when (val state = partiesState) {
            is UiState.Loading -> {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            is UiState.Empty -> {
                EmptyStateView(
                    title = "No Business Parties Found",
                    description = if (searchQuery.isNotBlank() || roleFilter != "all" || balanceFilter != "all") {
                        "No parties match the selected filters or search terms."
                    } else {
                        "Register a business party to manage customer sales, supplier purchases, and net balance settlements."
                    },
                    icon = Icons.Default.Groups,
                    actionText = "Register Party",
                    onActionClick = onAddNewPartyClick
                )
            }
            is UiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    ErrorBanner(message = state.message)
                }
            }
            is UiState.Success -> {
                val parties = state.data

                LazyColumn(
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(parties, key = { it.id }) { party ->
                        PartyCard(
                            party = party,
                            onClick = { onPartyClick(party) },
                            onEdit = if (canUpdateParty) { { onEditClick(party) } } else null,
                            onDelete = if (canDeleteParty) { { onDeleteClick(party) } } else null,
                            onCollect = if (onCollectClick != null) { { onCollectClick(party) } } else null,
                            onPay = if (onPayClick != null) { { onPayClick(party) } } else null
                        )
                    }

                    // Pagination Footer
                    pagination?.let { pag ->
                        if (pag.totalPages > 1) {
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = { viewModel.previousPage() },
                                        enabled = pag.page > 1
                                    ) {
                                        Text("Previous")
                                    }

                                    Text(
                                        text = "Page ${pag.page} of ${pag.totalPages} (${pag.totalItems} parties)",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )

                                    OutlinedButton(
                                        onClick = { viewModel.nextPage() },
                                        enabled = pag.page < pag.totalPages
                                    ) {
                                        Text("Next")
                                    }
                                }
                            }
                        }
                    }
                }
            }
            else -> Unit
        }
    }
}

@Composable
fun PartyCard(
    party: Party,
    onClick: () -> Unit,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onCollect: (() -> Unit)? = null,
    onPay: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val recv = party.totalReceivable.toDoubleOrNull() ?: 0.0
    val pay = party.totalPayable.toDoubleOrNull() ?: 0.0
    val net = party.netBalance.toDoubleOrNull() ?: 0.0

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .vyavsaClickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Avatar, Name, Role Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                AvatarInitialCircle(
                    name = party.displayName,
                    modifier = Modifier.size(42.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = party.displayName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Role Badge
                        StatBadge(
                            text = when {
                                party.isDualRole -> "Both Roles"
                                party.isSupplierOnly -> "Supplier"
                                else -> "Customer"
                            },
                            backgroundColor = when {
                                party.isDualRole -> Color(0xFFE0E7FF)
                                party.isSupplierOnly -> Color(0xFFFEF3C7)
                                else -> Color(0xFFDBEAFE)
                            },
                            textColor = when {
                                party.isDualRole -> Color(0xFF4338CA)
                                party.isSupplierOnly -> Color(0xFFB45309)
                                else -> Color(0xFF1D4ED8)
                            }
                        )

                        if (!party.phone.isNullOrBlank()) {
                            Text(
                                text = "•  ${party.phone}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            androidx.compose.material3.HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
            )
            Spacer(modifier = Modifier.height(10.dp))

            // Financial Summary Row: Receivable, Payable, Net Balance
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Receivable",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.5.sp
                        )
                    )
                    Text(
                        text = FormatUtils.formatCurrency(recv),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (recv > 0.009) Emerald600 else MaterialTheme.colorScheme.onSurface
                        )
                    )
                }

                Column {
                    Text(
                        text = "Payable",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.5.sp
                        )
                    )
                    Text(
                        text = FormatUtils.formatCurrency(pay),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (pay > 0.009) Crimson600 else MaterialTheme.colorScheme.onSurface
                        )
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Net Position",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.5.sp
                        )
                    )
                    StatBadge(
                        text = when {
                            net > 0.009 -> "Owes: ${FormatUtils.formatCurrency(net)}"
                            net < -0.009 -> "We Owe: ${FormatUtils.formatCurrency(kotlin.math.abs(net))}"
                            else -> "Settled"
                        },
                        backgroundColor = when {
                            net > 0.009 -> Emerald50
                            net < -0.009 -> Crimson50
                            else -> MaterialTheme.colorScheme.surfaceContainer
                        },
                        textColor = when {
                            net > 0.009 -> Emerald600
                            net < -0.009 -> Crimson600
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (recv > 0.009 && onCollect != null) {
                    TextButton(
                        onClick = onCollect,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("Collect", color = Emerald600, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                if (pay > 0.009 && onPay != null) {
                    TextButton(
                        onClick = onPay,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("Pay", color = Crimson600, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                TextButton(
                    onClick = onClick,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text("Details", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                }

                if (onEdit != null) {
                    TextButton(
                        onClick = onEdit,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("Edit", fontSize = 12.sp)
                    }
                }

                if (onDelete != null) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Party",
                            tint = Crimson600,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
