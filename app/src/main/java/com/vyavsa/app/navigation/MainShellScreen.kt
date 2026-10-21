@file:Suppress("DEPRECATION")
package com.vyavsa.app.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vyavsa.app.core.model.UserRole
import com.vyavsa.app.core.navigation.BottomBarDestination
import com.vyavsa.app.feature.dashboard.DashboardScreen
import com.vyavsa.app.feature.dashboard.DashboardViewModel
import com.vyavsa.app.feature.dashboard.OfficeDashboardScreen
import com.vyavsa.app.feature.dashboard.SalesDashboardScreen
import com.vyavsa.app.feature.dues.DuesScreen
import com.vyavsa.app.feature.dues.DuesViewModel
import com.vyavsa.app.feature.products.ProductViewModel
import com.vyavsa.app.feature.products.ProductsScreen
import com.vyavsa.app.feature.profile.ProfileScreen
import com.vyavsa.app.feature.profile.ProfileViewModel
import com.vyavsa.app.feature.staff.StaffScreen
import com.vyavsa.app.feature.staff.StaffViewModel
import com.vyavsa.app.feature.transactions.SalesScreen
import com.vyavsa.app.feature.transactions.TransactionsScreen
import com.vyavsa.app.feature.transactions.TransactionsViewModel

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.collectAsState
import com.vyavsa.app.core.settings.SettingsPreferences

import androidx.compose.ui.graphics.Color

@Composable
fun MainShellScreen(
    onNavigateToCreateSale: () -> Unit,
    onNavigateToCreateCounterSale: () -> Unit = {},
    onNavigateToCreateExpense: () -> Unit,
    onNavigateToCreatePurchase: () -> Unit = {},
    onNavigateToPurchaseDetails: (purchaseId: String) -> Unit = {},
    onNavigateToCashDrawer: () -> Unit,
    onNavigateToBankAccounts: () -> Unit,
    onNavigateToStatement: (customerId: String, customerName: String) -> Unit,
    onNavigateToSaleDetails: (saleType: String, saleId: String) -> Unit = { _, _ -> },
    onNavigateToCustomerDetails: (customerId: String) -> Unit = {},
    onNavigateToPlans: () -> Unit = {},
    onNavigateToTenantUsers: () -> Unit = {},
    onNavigateToProducts: () -> Unit = {},
    onNavigateToProductAnalytics: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onLogoutSuccess: () -> Unit,
    settingsPreferences: SettingsPreferences? = null,
    modifier: Modifier = Modifier
) {
    val dashboardViewModel: DashboardViewModel = hiltViewModel()
    val transactionsViewModel: TransactionsViewModel = hiltViewModel()
    val duesViewModel: DuesViewModel = hiltViewModel()
    val staffViewModel: StaffViewModel = hiltViewModel()
    val profileViewModel: ProfileViewModel = hiltViewModel()

    val userRole = dashboardViewModel.tokenManager.getUserRole()
    val customDockOrder = settingsPreferences?.customDockOrder?.collectAsState()?.value ?: emptyList()
    val destinations = remember(userRole, customDockOrder) {
        val defaultList = BottomBarDestination.destinationsForRole(userRole)
        if (customDockOrder.isEmpty()) {
            defaultList
        } else {
            val valid = customDockOrder.filter { it in defaultList }
            val missing = defaultList.filter { it !in valid }
            valid + missing
        }
    }

    var selectedDestination by rememberSaveable { mutableStateOf(BottomBarDestination.HOME) }

    // Ensure selected destination is valid for current role
    LaunchedEffect(destinations) {
        if (selectedDestination !in destinations) {
            selectedDestination = destinations.firstOrNull() ?: BottomBarDestination.HOME
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(start = 16.dp, end = 16.dp, bottom = 12.dp, top = 4.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 4.dp,
                    shadowElevation = 10.dp,
                    border = BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.50f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    NavigationBar(
                        containerColor = Color.Transparent,
                        tonalElevation = 0.dp,
                        windowInsets = WindowInsets(0.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                    ) {
                        destinations.forEach { destination ->
                        val isSelected = selectedDestination == destination
                        val iconScale by animateFloatAsState(
                            targetValue = if (isSelected) 1.12f else 1.0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            ),
                            label = "BottomNavIconScale"
                        )
                        val icon = when (destination) {
                            BottomBarDestination.HOME -> if (isSelected) Icons.Filled.Home else Icons.Outlined.Home
                            BottomBarDestination.SALES -> if (isSelected) Icons.Filled.ShoppingCart else Icons.Outlined.ShoppingCart
                            BottomBarDestination.COUNTER_SALES -> if (isSelected) Icons.Filled.PointOfSale else Icons.Outlined.PointOfSale
                            BottomBarDestination.TRANSACTIONS -> if (isSelected) Icons.Filled.ReceiptLong else Icons.Outlined.ReceiptLong
                            BottomBarDestination.DUES -> if (isSelected) Icons.Filled.People else Icons.Outlined.People
                            BottomBarDestination.STAFF -> if (isSelected) Icons.Filled.Badge else Icons.Outlined.Badge
                            BottomBarDestination.PRODUCTS -> if (isSelected) Icons.Filled.Inventory2 else Icons.Outlined.Inventory2
                            BottomBarDestination.MORE -> if (isSelected) Icons.Filled.Menu else Icons.Outlined.Menu
                        }

                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { selectedDestination = destination },
                            alwaysShowLabel = true,
                            icon = {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = destination.title,
                                    modifier = Modifier.graphicsLayer {
                                        scaleX = iconScale
                                        scaleY = iconScale
                                    }
                                )
                            },
                            label = {
                                Text(
                                    text = destination.title,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                            )
                        )
                    }
                }
            }
        }
    },
    modifier = modifier
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AnimatedContent(
                targetState = selectedDestination,
                transitionSpec = {
                    val direction = if (targetState.ordinal >= initialState.ordinal) 1 else -1
                    (fadeIn(animationSpec = tween(230, easing = FastOutSlowInEasing)) +
                        slideInHorizontally(
                            initialOffsetX = { (it * 0.08f * direction).toInt() },
                            animationSpec = tween(250, easing = FastOutSlowInEasing)
                        )) togetherWith
                        (fadeOut(animationSpec = tween(170, easing = FastOutSlowInEasing)) +
                            slideOutHorizontally(
                                targetOffsetX = { (-it * 0.06f * direction).toInt() },
                                animationSpec = tween(200, easing = FastOutSlowInEasing)
                            ))
                },
                label = "BottomNavDestinationTransition"
            ) { destination ->
                when (destination) {
                    BottomBarDestination.HOME -> {
                        when (userRole) {
                            UserRole.SALES -> {
                                SalesDashboardScreen(
                                    viewModel = dashboardViewModel,
                                    onNavigateToCreateLineSale = onNavigateToCreateSale,
                                    onNavigateToLineSales = { selectedDestination = BottomBarDestination.SALES },
                                    onNavigateToProducts = { selectedDestination = BottomBarDestination.PRODUCTS }
                                )
                            }
                            UserRole.OFFICE -> {
                                OfficeDashboardScreen(
                                    viewModel = dashboardViewModel,
                                    onNavigateToCreateCounterSale = onNavigateToCreateCounterSale,
                                    onNavigateToCreatePurchase = onNavigateToCreatePurchase,
                                    onNavigateToCreateExpense = onNavigateToCreateExpense,
                                    onNavigateToStaff = { selectedDestination = BottomBarDestination.STAFF },
                                    onNavigateToCounterSales = { selectedDestination = BottomBarDestination.COUNTER_SALES }
                                )
                            }
                            UserRole.ADMIN, UserRole.USER -> {
                                DashboardScreen(
                                    viewModel = dashboardViewModel,
                                    onNavigateToCreateSale = onNavigateToCreateSale,
                                    onNavigateToCreateExpense = onNavigateToCreateExpense,
                                    onNavigateToCashDrawer = onNavigateToCashDrawer,
                                    onNavigateToStaff = { selectedDestination = BottomBarDestination.STAFF },
                                    onNavigateToBankAccounts = onNavigateToBankAccounts,
                                    onNavigateToProducts = onNavigateToProducts,
                                    onNavigateToProductAnalytics = onNavigateToProductAnalytics
                                )
                            }
                        }
                    }
                    BottomBarDestination.SALES -> {
                        SalesScreen(
                            viewModel = transactionsViewModel,
                            onNavigateToCreateSale = onNavigateToCreateSale,
                            onNavigateToSaleDetails = onNavigateToSaleDetails
                        )
                    }
                    BottomBarDestination.COUNTER_SALES -> {
                        SalesScreen(
                            viewModel = transactionsViewModel,
                            onNavigateToCreateSale = onNavigateToCreateCounterSale,
                            onNavigateToSaleDetails = onNavigateToSaleDetails
                        )
                    }
                    BottomBarDestination.TRANSACTIONS -> {
                        TransactionsScreen(
                            viewModel = transactionsViewModel,
                            onNavigateToCreateSale = if (userRole == UserRole.OFFICE) onNavigateToCreateCounterSale else onNavigateToCreateSale,
                            onNavigateToCreateExpense = onNavigateToCreateExpense,
                            onNavigateToCreatePurchase = onNavigateToCreatePurchase,
                            onNavigateToPurchaseDetails = onNavigateToPurchaseDetails,
                            onNavigateToSaleDetails = onNavigateToSaleDetails
                        )
                    }
                    BottomBarDestination.DUES -> {
                        DuesScreen(
                            viewModel = duesViewModel,
                            onNavigateToStatement = onNavigateToStatement,
                            onNavigateToCustomerDetails = onNavigateToCustomerDetails
                        )
                    }
                    BottomBarDestination.STAFF -> {
                        StaffScreen(
                            viewModel = staffViewModel
                        )
                    }
                    BottomBarDestination.PRODUCTS -> {
                        val productViewModel: ProductViewModel = hiltViewModel()
                        ProductsScreen(
                            viewModel = productViewModel,
                            onNavigateBack = { selectedDestination = BottomBarDestination.HOME },
                            onNavigateToAnalytics = onNavigateToProductAnalytics
                        )
                    }
                    BottomBarDestination.MORE -> {
                        ProfileScreen(
                            viewModel = profileViewModel,
                            onLogoutSuccess = onLogoutSuccess,
                            onNavigateToBankAccounts = onNavigateToBankAccounts,
                            onNavigateToPlans = onNavigateToPlans,
                            onNavigateToTenantUsers = onNavigateToTenantUsers,
                            onNavigateToProducts = onNavigateToProducts,
                            onNavigateToProductAnalytics = onNavigateToProductAnalytics,
                            onNavigateToSettings = onNavigateToSettings
                        )
                    }
                }
            }
        }
    }
}
