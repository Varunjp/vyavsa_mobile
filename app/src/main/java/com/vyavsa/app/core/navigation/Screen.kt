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
    object CreateSale : Screen("create_line_sale")
    object CreateLineSale : Screen("create_line_sale")
    object CreateCounterSale : Screen("create_counter_sale")
    object CreateExpense : Screen("create_expense")
    object CreatePurchase : Screen("create_purchase")
    object CustomerStatement : Screen("customer_statement/{customerId}/{customerName}") {
        fun createRoute(customerId: String, customerName: String) =
            "customer_statement/$customerId/$customerName"
    }
    object CashDrawer : Screen("cash_drawer")
    object BankAccounts : Screen("bank_accounts")
    object SaleDetails : Screen("sale_details/{saleType}/{saleId}") {
        fun createRoute(saleType: String, saleId: String) = "sale_details/$saleType/$saleId"
    }
    object PurchaseDetails : Screen("purchase_details/{purchaseId}") {
        fun createRoute(purchaseId: String) = "purchase_details/$purchaseId"
    }
    object CustomerDetails : Screen("customer_details/{customerId}") {
        fun createRoute(customerId: String) = "customer_details/$customerId"
    }
    object Plans : Screen("plans")
    object TenantUsers : Screen("tenant_users")
    object Products : Screen("products")
    object ProductAnalytics : Screen("product_analytics")
}


enum class BottomBarDestination(
    val title: String,
    val iconName: String
) {
    HOME("Home", "home"),
    SALES("Sales", "shopping_cart"),
    COUNTER_SALES("Counter", "point_of_sale"),
    TRANSACTIONS("Ledger", "receipt_long"),
    DUES("Dues", "account_balance_wallet"),
    STAFF("Staff", "badge"),
    PRODUCTS("Catalog", "inventory_2"),
    MORE("More", "menu");

    companion object {
        fun destinationsForRole(role: com.vyavsa.app.core.model.UserRole): List<BottomBarDestination> {
            return when (role) {
                com.vyavsa.app.core.model.UserRole.SALES -> listOf(HOME, SALES, PRODUCTS, MORE)
                com.vyavsa.app.core.model.UserRole.OFFICE -> listOf(HOME, COUNTER_SALES, TRANSACTIONS, STAFF, MORE)
                com.vyavsa.app.core.model.UserRole.ADMIN,
                com.vyavsa.app.core.model.UserRole.USER -> listOf(HOME, SALES, TRANSACTIONS, DUES, STAFF, MORE)
            }
        }
    }
}
