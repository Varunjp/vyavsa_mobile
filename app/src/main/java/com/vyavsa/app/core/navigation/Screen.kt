package com.vyavsa.app.core.navigation

sealed class Screen(val route: String) {
    // Auth routes
    object Login : Screen("login")
    object Register : Screen("register")
    object ForgotPassword : Screen("forgot_password")
    object VerifyOtp : Screen("verify_otp/{email}") {
        fun createRoute(email: String) = "verify_otp/$email"
    }
    object ResetPassword : Screen("reset_password/{token}") {
        fun createRoute(token: String) = "reset_password/$token"
    }

    // Main App Shell
    object MainShell : Screen("main_shell")

    // Sub-screens
    object CreateLineSale : Screen("create_line_sale")
    object CreateExpense : Screen("create_expense")
    object CustomerStatement : Screen("customer_statement/{customerId}/{customerName}") {
        fun createRoute(customerId: String, customerName: String) =
            "customer_statement/$customerId/$customerName"
    }
    object CashDrawer : Screen("cash_drawer")
}

enum class BottomBarDestination(
    val title: String,
    val iconName: String
) {
    HOME("Home", "home"),
    TRANSACTIONS("Ledger", "receipt_long"),
    DUES("Dues", "account_balance_wallet"),
    STAFF("Staff", "badge"),
    MORE("More", "menu")
}
