package com.vyavsa.app.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.vyavsa.app.core.navigation.Screen
import com.vyavsa.app.core.security.SessionState
import com.vyavsa.app.feature.auth.AuthViewModel
import com.vyavsa.app.feature.auth.ForgotPasswordScreen
import com.vyavsa.app.feature.auth.LoginScreen
import com.vyavsa.app.feature.auth.RegisterScreen
import com.vyavsa.app.feature.auth.ResetPasswordScreen
import com.vyavsa.app.feature.auth.VerifyOtpScreen
import com.vyavsa.app.feature.bank.BankAccountsScreen
import com.vyavsa.app.feature.bank.BankViewModel
import com.vyavsa.app.feature.cash.CashDrawerScreen
import com.vyavsa.app.feature.cash.CashViewModel
import com.vyavsa.app.feature.dues.CustomerDetailsScreen
import com.vyavsa.app.feature.dues.CustomerStatementScreen
import com.vyavsa.app.feature.dues.DuesViewModel
import com.vyavsa.app.feature.plans.PlansScreen
import com.vyavsa.app.feature.plans.PlansViewModel
import com.vyavsa.app.feature.transactions.CreateExpenseScreen
import com.vyavsa.app.feature.transactions.CreateLineSaleScreen
import com.vyavsa.app.feature.transactions.SaleDetailsScreen
import com.vyavsa.app.feature.transactions.SaleDetailsViewModel
import com.vyavsa.app.feature.transactions.TransactionsViewModel
import com.vyavsa.app.feature.users.TenantUsersScreen
import com.vyavsa.app.feature.users.TenantUsersViewModel

@Composable
fun VyavsaNavGraph(
    navController: NavHostController,
    sessionState: SessionState,
    modifier: Modifier = Modifier
) {
    val startDestination = if (sessionState is SessionState.Authenticated) {
        Screen.MainShell.route
    } else {
        Screen.Login.route
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = {
            fadeIn(animationSpec = tween(240)) + slideInHorizontally(
                initialOffsetX = { it / 6 },
                animationSpec = tween(240)
            )
        },
        exitTransition = {
            fadeOut(animationSpec = tween(200))
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(220))
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(220)) + slideOutHorizontally(
                targetOffsetX = { it / 6 },
                animationSpec = tween(220)
            )
        },
        modifier = modifier
    ) {
        // ----------------------------------------------------
        // Authentication Flow
        // ----------------------------------------------------
        composable(Screen.Login.route) {
            val authViewModel: AuthViewModel = hiltViewModel()
            LoginScreen(
                viewModel = authViewModel,
                onLoginSuccess = {
                    navController.navigate(Screen.MainShell.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                },
                onNavigateToForgotPassword = {
                    navController.navigate(Screen.ForgotPassword.route)
                }
            )
        }

        composable(Screen.Register.route) {
            val authViewModel: AuthViewModel = hiltViewModel()
            RegisterScreen(
                viewModel = authViewModel,
                onRegisterSuccess = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.ForgotPassword.route) {
            val authViewModel: AuthViewModel = hiltViewModel()
            ForgotPasswordScreen(
                viewModel = authViewModel,
                onOtpSent = { email ->
                    navController.navigate(Screen.VerifyOtp.createRoute(email))
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.VerifyOtp.route,
            arguments = listOf(navArgument("email") { type = NavType.StringType })
        ) { backStackEntry ->
            val email = backStackEntry.arguments?.getString("email") ?: ""
            val authViewModel: AuthViewModel = hiltViewModel()
            VerifyOtpScreen(
                email = email,
                viewModel = authViewModel,
                onOtpVerified = { resetToken ->
                    navController.navigate(Screen.ResetPassword.createRoute(resetToken))
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.ResetPassword.route,
            arguments = listOf(navArgument("token") { type = NavType.StringType })
        ) { backStackEntry ->
            val token = backStackEntry.arguments?.getString("token") ?: ""
            val authViewModel: AuthViewModel = hiltViewModel()
            ResetPasswordScreen(
                resetToken = token,
                viewModel = authViewModel,
                onPasswordResetSuccess = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ----------------------------------------------------
        // Authenticated Main Shell
        // ----------------------------------------------------
        composable(Screen.MainShell.route) {
            MainShellScreen(
                onNavigateToCreateSale = {
                    navController.navigate(Screen.CreateLineSale.route)
                },
                onNavigateToCreateExpense = {
                    navController.navigate(Screen.CreateExpense.route)
                },
                onNavigateToCashDrawer = {
                    navController.navigate(Screen.CashDrawer.route)
                },
                onNavigateToBankAccounts = {
                    navController.navigate(Screen.BankAccounts.route)
                },
                onNavigateToStatement = { customerId, customerName ->
                    navController.navigate(Screen.CustomerStatement.createRoute(customerId, customerName))
                },
                onNavigateToSaleDetails = { saleType, saleId ->
                    navController.navigate(Screen.SaleDetails.createRoute(saleType, saleId))
                },
                onNavigateToCustomerDetails = { customerId ->
                    navController.navigate(Screen.CustomerDetails.createRoute(customerId))
                },
                onNavigateToPlans = {
                    navController.navigate(Screen.Plans.route)
                },
                onNavigateToTenantUsers = {
                    navController.navigate(Screen.TenantUsers.route)
                },
                onLogoutSuccess = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.MainShell.route) { inclusive = true }
                    }
                }
            )
        }

        // ----------------------------------------------------
        // Detail & Action Screens
        // ----------------------------------------------------
        composable(Screen.CreateLineSale.route) {
            val transactionsViewModel: TransactionsViewModel = hiltViewModel()
            CreateLineSaleScreen(
                viewModel = transactionsViewModel,
                onSaleCreated = { navController.popBackStack() },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.CreateExpense.route) {
            val transactionsViewModel: TransactionsViewModel = hiltViewModel()
            CreateExpenseScreen(
                viewModel = transactionsViewModel,
                onExpenseCreated = { navController.popBackStack() },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.CashDrawer.route) {
            val cashViewModel: CashViewModel = hiltViewModel()
            CashDrawerScreen(
                viewModel = cashViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.CustomerStatement.route,
            arguments = listOf(
                navArgument("customerId") { type = NavType.StringType },
                navArgument("customerName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val customerId = backStackEntry.arguments?.getString("customerId") ?: ""
            val customerName = backStackEntry.arguments?.getString("customerName") ?: ""
            val duesViewModel: DuesViewModel = hiltViewModel()
            CustomerStatementScreen(
                customerId = customerId,
                customerName = customerName,
                viewModel = duesViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.CustomerDetails.route,
            arguments = listOf(
                navArgument("customerId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val customerId = backStackEntry.arguments?.getString("customerId") ?: ""
            val duesViewModel: DuesViewModel = hiltViewModel()
            CustomerDetailsScreen(
                customerId = customerId,
                viewModel = duesViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToStatement = { cid, cname ->
                    navController.navigate(Screen.CustomerStatement.createRoute(cid, cname))
                }
            )
        }

        composable(
            route = Screen.SaleDetails.route,
            arguments = listOf(
                navArgument("saleType") { type = NavType.StringType },
                navArgument("saleId") { type = NavType.StringType }
            )
        ) { _ ->
            val saleDetailsViewModel: SaleDetailsViewModel = hiltViewModel()
            SaleDetailsScreen(
                viewModel = saleDetailsViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Plans.route) {
            val plansViewModel: PlansViewModel = hiltViewModel()
            PlansScreen(
                viewModel = plansViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.TenantUsers.route) {
            val tenantUsersViewModel: TenantUsersViewModel = hiltViewModel()
            TenantUsersScreen(
                viewModel = tenantUsersViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.BankAccounts.route) {
            val bankViewModel: BankViewModel = hiltViewModel()
            BankAccountsScreen(
                viewModel = bankViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
