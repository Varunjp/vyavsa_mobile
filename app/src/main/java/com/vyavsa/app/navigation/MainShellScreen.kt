@file:Suppress("DEPRECATION")
package com.vyavsa.app.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vyavsa.app.core.navigation.BottomBarDestination
import com.vyavsa.app.feature.dashboard.DashboardScreen
import com.vyavsa.app.feature.dashboard.DashboardViewModel
import com.vyavsa.app.feature.dues.DuesScreen
import com.vyavsa.app.feature.dues.DuesViewModel
import com.vyavsa.app.feature.profile.ProfileScreen
import com.vyavsa.app.feature.profile.ProfileViewModel
import com.vyavsa.app.feature.staff.StaffScreen
import com.vyavsa.app.feature.staff.StaffViewModel
import com.vyavsa.app.feature.transactions.TransactionsScreen
import com.vyavsa.app.feature.transactions.TransactionsViewModel

@Composable
fun MainShellScreen(
    onNavigateToCreateSale: () -> Unit,
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
    onLogoutSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedDestination by rememberSaveable { mutableStateOf(BottomBarDestination.HOME) }

    val dashboardViewModel: DashboardViewModel = hiltViewModel()
    val transactionsViewModel: TransactionsViewModel = hiltViewModel()
    val duesViewModel: DuesViewModel = hiltViewModel()
    val staffViewModel: StaffViewModel = hiltViewModel()
    val profileViewModel: ProfileViewModel = hiltViewModel()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Surface(
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp,
                shadowElevation = 10.dp
            ) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp,
                    modifier = Modifier.clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                ) {
                    BottomBarDestination.values().forEach { destination ->
                        val isSelected = selectedDestination == destination
                        val icon = when (destination) {
                            BottomBarDestination.HOME -> if (isSelected) Icons.Filled.Home else Icons.Outlined.Home
                            BottomBarDestination.TRANSACTIONS -> if (isSelected) Icons.Filled.ReceiptLong else Icons.Outlined.ReceiptLong
                            BottomBarDestination.DUES -> if (isSelected) Icons.Filled.People else Icons.Outlined.People
                            BottomBarDestination.STAFF -> if (isSelected) Icons.Filled.Badge else Icons.Outlined.Badge
                            BottomBarDestination.MORE -> if (isSelected) Icons.Filled.Menu else Icons.Outlined.Menu
                        }

                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { selectedDestination = destination },
                            icon = {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = destination.title
                                )
                            },
                            label = {
                                Text(
                                    text = destination.title,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
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
                .padding(paddingValues)
        ) {
            AnimatedContent(
                targetState = selectedDestination,
                transitionSpec = {
                    fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(180))
                },
                label = "BottomNavDestinationTransition"
            ) { destination ->
                when (destination) {
                    BottomBarDestination.HOME -> {
                        DashboardScreen(
                            viewModel = dashboardViewModel,
                            onNavigateToCreateSale = onNavigateToCreateSale,
                            onNavigateToCreateExpense = onNavigateToCreateExpense,
                            onNavigateToCashDrawer = onNavigateToCashDrawer,
                            onNavigateToStaff = { selectedDestination = BottomBarDestination.STAFF },
                            onNavigateToBankAccounts = onNavigateToBankAccounts
                        )
                    }
                    BottomBarDestination.TRANSACTIONS -> {
                        TransactionsScreen(
                            viewModel = transactionsViewModel,
                            onNavigateToCreateSale = onNavigateToCreateSale,
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
                    BottomBarDestination.MORE -> {
                        ProfileScreen(
                            viewModel = profileViewModel,
                            onLogoutSuccess = onLogoutSuccess,
                            onNavigateToBankAccounts = onNavigateToBankAccounts,
                            onNavigateToPlans = onNavigateToPlans,
                            onNavigateToTenantUsers = onNavigateToTenantUsers
                        )
                    }
                }
            }
        }
    }
}
