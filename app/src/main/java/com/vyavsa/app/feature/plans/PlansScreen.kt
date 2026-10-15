package com.vyavsa.app.feature.plans

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.vyavsa.app.core.designsystem.VyavsaButton
import com.vyavsa.app.core.designsystem.VyavsaGradients
import com.vyavsa.app.core.designsystem.VyavsaSegmentedTabs
import com.vyavsa.app.core.designsystem.VyavsaTopAppBar
import com.vyavsa.app.core.model.PlanTransaction
import com.vyavsa.app.core.model.SubscriptionPlan

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PlansScreen(
    viewModel: PlansViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val subState by viewModel.subscriptionState.collectAsState()
    val plansState by viewModel.plansState.collectAsState()
    val txState by viewModel.transactionsState.collectAsState()
    val actionState by viewModel.purchaseActionState.collectAsState()
    val isAdmin = viewModel.isAdmin

    var selectedPlanToPurchase by remember { mutableStateOf<SubscriptionPlan?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            VyavsaTopAppBar(
                title = "Plans & Subscription",
                subtitle = "Manage tiers, feature limits & billing ledger",
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
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                )
            }

            // Current Subscription Hero Card with Gradient
            val currentSub = (subState as? UiState.Success)?.data
            if (currentSub != null) {
                Card(
                    shape = RoundedCornerShape(26.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(VyavsaGradients.PrimaryHeroBrush)
                            .padding(20.dp)
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(44.dp)
                                            .background(Color.White.copy(alpha = 0.18f), RoundedCornerShape(14.dp))
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.WorkspacePremium,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = currentSub.currentPlanName.ifBlank { "Standard Business Plan" },
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color.White
                                            )
                                        )
                                        Text(
                                            text = "Current Active Subscription",
                                            style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.8f))
                                        )
                                    }
                                }

                                Surface(
                                    color = if (currentSub.isExpired) Crimson50 else Emerald50,
                                    shape = CircleShape
                                ) {
                                    Text(
                                        text = if (currentSub.isExpired) "EXPIRED" else "${currentSub.daysRemaining} DAYS LEFT",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = if (currentSub.isExpired) Crimson600 else Emerald600,
                                            fontWeight = FontWeight.ExtraBold
                                        ),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color.White.copy(alpha = 0.14f),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = "Plan Rate",
                                            style = MaterialTheme.typography.labelSmall.copy(color = Color.White.copy(alpha = 0.8f))
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "${FormatUtils.formatCurrency(currentSub.price)} / billing cycle",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        )
                                    }

                                    if (!currentSub.endDate.isNullOrBlank()) {
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "Valid Until",
                                                style = MaterialTheme.typography.labelSmall.copy(color = Color.White.copy(alpha = 0.8f))
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = FormatUtils.formatIsoDateTime(currentSub.endDate),
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color.White
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (subState is UiState.Loading) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                }
            }

            // Unified Pill Segmented Tabs
            VyavsaSegmentedTabs(
                tabs = listOf("Available Plans", "Billing History"),
                selectedIndex = selectedTab.ordinal,
                onTabSelected = { idx ->
                    viewModel.selectTab(if (idx == 0) PlansTab.AVAILABLE_PLANS else PlansTab.BILLING_HISTORY)
                },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )

            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    val dir = if (targetState.ordinal >= initialState.ordinal) 1 else -1
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
                label = "PlansSubTabTransition",
                modifier = Modifier.fillMaxSize()
            ) { currentTab ->
                when (currentTab) {
                    PlansTab.AVAILABLE_PLANS -> {
                        when (val state = plansState) {
                            is UiState.Loading -> {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                }
                            }
                            is UiState.Empty -> {
                                EmptyStateView(
                                    title = "No Plans Available",
                                    description = "There are no public subscription plans configured on the platform at this time."
                                )
                            }
                            is UiState.Error -> {
                                Box(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                                    ErrorBanner(message = state.message)
                                }
                            }
                            is UiState.Success -> {
                                val currentPlanId = currentSub?.currentPlanId ?: ""

                                LazyColumn(
                                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(
                                        items = state.data,
                                        key = { it.id.ifBlank { it.displayName } }
                                    ) { plan ->
                                        val isCurrentPlan = plan.id.isNotBlank() && (plan.id == currentPlanId || plan.displayName.equals(currentSub?.currentPlanName, ignoreCase = true))

                                        PlanCard(
                                            plan = plan,
                                            isCurrentPlan = isCurrentPlan,
                                            canUpgrade = isAdmin && !isCurrentPlan,
                                            onUpgradeClick = { selectedPlanToPurchase = plan },
                                            modifier = Modifier.animateItemPlacement()
                                        )
                                    }
                                }
                            }
                            else -> {}
                        }
                    }

                    PlansTab.BILLING_HISTORY -> {
                        when (val state = txState) {
                            is UiState.Loading -> {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                }
                            }
                            is UiState.Empty -> {
                                EmptyStateView(
                                    title = "No Billing History",
                                    description = "No previous subscription purchases or invoice transactions found.",
                                    icon = Icons.Default.History
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
                                    items(
                                        items = state.data,
                                        key = { it.id.ifBlank { it.transactionId } }
                                    ) { tx ->
                                        BillingTransactionCard(
                                            transaction = tx,
                                            modifier = Modifier.animateItemPlacement()
                                        )
                                    }
                                }
                            }
                            else -> {}
                        }
                    }
                }
            }
        }
    }

    // Purchase Dialog
    selectedPlanToPurchase?.let { plan ->
        PurchasePlanDialog(
            plan = plan,
            onDismiss = { selectedPlanToPurchase = null },
            onConfirm = { paymentMethod ->
                selectedPlanToPurchase = null
                viewModel.purchasePlan(plan.id, paymentMethod)
            }
        )
    }
}

@Composable
private fun PlanCard(
    plan: SubscriptionPlan,
    isCurrentPlan: Boolean,
    canUpgrade: Boolean,
    onUpgradeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentPlan) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = if (isCurrentPlan) 2.dp else 1.dp,
                color = if (isCurrentPlan) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                shape = RoundedCornerShape(24.dp)
            )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = plan.displayName,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${plan.durationDays} Days Duration",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }

                if (isCurrentPlan) {
                    StatBadge(
                        text = "ACTIVE PLAN",
                        backgroundColor = Emerald50,
                        textColor = Emerald600
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = FormatUtils.formatCurrency(plan.price),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = plan.displayDescription,
                style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )

            if (plan.features.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    plan.features.forEach { feature ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Emerald600,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = feature,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            if (canUpgrade) {
                VyavsaButton(
                    text = "Upgrade to ${plan.displayName}",
                    leadingIcon = Icons.Default.Star,
                    onClick = onUpgradeClick,
                    modifier = Modifier.fillMaxWidth()
                )
            } else if (isCurrentPlan) {
                OutlinedButton(
                    onClick = {},
                    enabled = false,
                    shape = CircleShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text("Currently Subscribed", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun BillingTransactionCard(
    transaction: PlanTransaction,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f), RoundedCornerShape(20.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = transaction.planName.ifBlank { "Subscription Purchase" },
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "TX: ${transaction.transactionId.ifBlank { transaction.id }}",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }

                Text(
                    text = FormatUtils.formatCurrency(transaction.amount),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Method: ${transaction.paymentMethod.uppercase()} | ${FormatUtils.formatIsoDateTime(transaction.createdAt)}",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )

                val isSuccess = transaction.status.equals("completed", ignoreCase = true) || transaction.status.equals("success", ignoreCase = true)
                StatBadge(
                    text = transaction.status.uppercase(),
                    backgroundColor = if (isSuccess) Emerald50 else Crimson50,
                    textColor = if (isSuccess) Emerald600 else Crimson600
                )
            }
        }
    }
}

@Composable
private fun PurchasePlanDialog(
    plan: SubscriptionPlan,
    onDismiss: () -> Unit,
    onConfirm: (paymentMethod: String) -> Unit
) {
    var selectedMethod by remember { mutableStateOf("upi") }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Upgrade to ${plan.displayName}", fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = "Total Amount: ${FormatUtils.formatCurrency(plan.price)}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Duration: ${plan.durationDays} days access to all premium operational features.",
                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )

                Text(
                    text = "Select Payment Channel:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("upi" to "UPI", "card" to "Card", "netbanking" to "Net Banking").forEach { (key, label) ->
                        FilterChip(
                            selected = selectedMethod == key,
                            onClick = { selectedMethod = key },
                            shape = CircleShape,
                            label = { Text(label, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(selectedMethod) }
            ) {
                Text("Confirm & Activate", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
