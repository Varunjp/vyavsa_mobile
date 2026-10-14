@file:Suppress("DEPRECATION")
package com.vyavsa.app.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
    onNavigateToCashDrawer: () -> Unit,
    onNavigateToStatement: (customerId: String, customerName: String) -> Unit,
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
        bottomBar = {
            NavigationBar {
                BottomBarDestination.values().forEach { destination ->
                    val icon = when (destination) {
                        BottomBarDestination.HOME -> Icons.Default.Home
                        BottomBarDestination.TRANSACTIONS -> Icons.Default.ReceiptLong
                        BottomBarDestination.DUES -> Icons.Default.People
                        BottomBarDestination.STAFF -> Icons.Default.Badge
                        BottomBarDestination.MORE -> Icons.Default.Menu
                    }

                    NavigationBarItem(
                        selected = selectedDestination == destination,
                        onClick = { selectedDestination = destination },
                        icon = { Icon(imageVector = icon, contentDescription = destination.title) },
                        label = { Text(destination.title) }
                    )
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
            when (selectedDestination) {
                BottomBarDestination.HOME -> {
                    DashboardScreen(
                        viewModel = dashboardViewModel,
                        onNavigateToCreateSale = onNavigateToCreateSale,
                        onNavigateToCreateExpense = onNavigateToCreateExpense,
                        onNavigateToCashDrawer = onNavigateToCashDrawer,
                        onNavigateToStaff = { selectedDestination = BottomBarDestination.STAFF }
                    )
                }
                BottomBarDestination.TRANSACTIONS -> {
                    TransactionsScreen(
                        viewModel = transactionsViewModel,
                        onNavigateToCreateSale = onNavigateToCreateSale,
                        onNavigateToCreateExpense = onNavigateToCreateExpense
                    )
                }
                BottomBarDestination.DUES -> {
                    DuesScreen(
                        viewModel = duesViewModel,
                        onNavigateToStatement = onNavigateToStatement
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
                        onLogoutSuccess = onLogoutSuccess
                    )
                }
            }
        }
    }
}
