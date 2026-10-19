package com.vyavsa.app.feature.products

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vyavsa.app.core.common.FormatUtils
import com.vyavsa.app.core.common.UiState
import com.vyavsa.app.core.designsystem.Blue50
import com.vyavsa.app.core.designsystem.Blue600
import com.vyavsa.app.core.designsystem.Emerald50
import com.vyavsa.app.core.designsystem.Emerald600
import com.vyavsa.app.core.designsystem.EmptyStateView
import com.vyavsa.app.core.designsystem.ErrorBanner
import com.vyavsa.app.core.designsystem.ProductSalesBarChart
import com.vyavsa.app.core.designsystem.TopSellingProductRow
import com.vyavsa.app.core.designsystem.VyavsaButton
import com.vyavsa.app.core.designsystem.VyavsaOutlinedButton
import com.vyavsa.app.core.designsystem.VyavsaSectionHeader
import com.vyavsa.app.core.designsystem.VyavsaSegmentedTabs
import com.vyavsa.app.core.designsystem.VyavsaTopAppBar
import com.vyavsa.app.core.model.ProductAnalyticsResponse

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductAnalyticsScreen(
    viewModel: ProductViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val analyticsState by viewModel.analyticsState.collectAsState()
    val selectedRange by viewModel.selectedRange.collectAsState()

    val ranges = remember { DateRangeFilter.values().toList() }
    val rangeIndex = remember(selectedRange) { ranges.indexOf(selectedRange).coerceAtLeast(0) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            VyavsaTopAppBar(
                title = "Product Sales Analytics",
                subtitle = "Turnover, unit volumes & ranked performers",
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                onNavigationClick = onNavigateBack,
                actions = {
                    IconButton(onClick = { viewModel.loadAnalytics() }) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh Analytics")
                    }
                }
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
            // Date Filter Tabs
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                VyavsaSegmentedTabs(
                    tabs = ranges.map { it.label },
                    selectedIndex = rangeIndex,
                    onTabSelected = { idx ->
                        viewModel.selectDateRange(ranges[idx])
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Dashboard Content
            when (val state = analyticsState) {
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
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                    ) {
                        ErrorBanner(message = state.message)
                        Spacer(modifier = Modifier.height(16.dp))
                        VyavsaOutlinedButton(
                            text = "Retry",
                            onClick = { viewModel.loadAnalytics() }
                        )
                    }
                }
                is UiState.Empty -> {
                    EmptyStateView(
                        icon = Icons.Default.Leaderboard,
                        title = "No Sales for ${selectedRange.label}",
                        description = "No itemized sales transactions were recorded for this selected time window. Switch date range or create a sale.",
                        actionText = "View All Time",
                        onActionClick = { viewModel.selectDateRange(DateRangeFilter.ALL_TIME) },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                is UiState.Success -> {
                    ProductAnalyticsContent(
                        analytics = state.data,
                        selectedRange = selectedRange
                    )
                }
                else -> Unit
            }
        }
    }
}

@Composable
private fun ProductAnalyticsContent(
    analytics: ProductAnalyticsResponse,
    selectedRange: DateRangeFilter
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        // Date range indicator subtitle
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            val periodText = if (analytics.startDate.isNotBlank() && analytics.endDate.isNotBlank()) {
                "${selectedRange.label} (${FormatUtils.formatDateForDisplay(analytics.startDate)} – ${FormatUtils.formatDateForDisplay(analytics.endDate)})"
            } else {
                "Showing data for: ${selectedRange.label}"
            }
            Text(
                text = periodText,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            )
        }

        // 4 Summary Metrics (2x2 Responsive Grid)
        VyavsaSectionHeader(title = "Sales Volume & Revenue")

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Metric 1: Total Units Sold
            ProductMetricCard(
                icon = Icons.Default.ShoppingBag,
                iconBg = MaterialTheme.colorScheme.primaryContainer,
                iconTint = MaterialTheme.colorScheme.primary,
                title = "UNITS SOLD",
                value = analytics.summary.totalQuantitySold,
                modifier = Modifier.weight(1f)
            )

            // Metric 2: Product Sales Revenue
            ProductMetricCard(
                icon = Icons.Default.CurrencyRupee,
                iconBg = Emerald50,
                iconTint = Emerald600,
                title = "PRODUCT REVENUE",
                value = FormatUtils.formatCurrency(analytics.summary.totalRevenue),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Metric 3: Distinct Products Sold
            ProductMetricCard(
                icon = Icons.Default.Category,
                iconBg = Blue50,
                iconTint = Blue600,
                title = "PRODUCTS SOLD",
                value = "${analytics.summary.distinctProductsCount} items",
                modifier = Modifier.weight(1f)
            )

            // Metric 4: Top-Selling Product
            ProductMetricCard(
                icon = Icons.Default.Star,
                iconBg = Color(0xFFFEF3C7),
                iconTint = Color(0xFFB45309),
                title = "TOP PERFORMER",
                value = analytics.summary.topProductName?.ifBlank { "N/A" } ?: "N/A",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Visual Bar Chart of Top Products
        if (analytics.topByRevenue.isNotEmpty()) {
            VyavsaSectionHeader(title = "Top Sellers Chart")
            Spacer(modifier = Modifier.height(10.dp))
            ProductSalesBarChart(
                items = analytics.topByRevenue,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Ranked Top-Selling Products List
        VyavsaSectionHeader(
            title = "Ranked Top Sellers",
            badgeText = "Volume & Revenue"
        )

        Spacer(modifier = Modifier.height(10.dp))

        val topProducts = if (analytics.topByQuantity.isNotEmpty()) {
            analytics.topByQuantity
        } else {
            analytics.topByRevenue
        }

        if (topProducts.isEmpty()) {
            Text(
                text = "No ranked products recorded for this date range.",
                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                modifier = Modifier.padding(vertical = 12.dp)
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                topProducts.forEach { product ->
                    TopSellingProductRow(
                        rank = product.rank,
                        productName = product.productName,
                        quantitySold = product.quantitySold,
                        revenue = product.revenue
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun ProductMetricCard(
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(iconBg)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    fontSize = 10.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    letterSpacing = (-0.2).sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
