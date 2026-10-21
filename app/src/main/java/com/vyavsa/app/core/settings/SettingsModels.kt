package com.vyavsa.app.core.settings

import androidx.compose.runtime.Immutable
import com.vyavsa.app.core.navigation.BottomBarDestination

/**
 * App color theme selection.
 */
enum class ThemeMode(val label: String) {
    SYSTEM("System Default"),
    LIGHT("Light"),
    DARK("Dark")
}

/**
 * App typography scaling factor for accessibility.
 */
enum class AppFontSize(val label: String, val scaleFactor: Float) {
    SMALL("Small (90%)", 0.90f),
    MEDIUM("Medium (Default)", 1.0f),
    LARGE("Large (115%)", 1.15f)
}

/**
 * Supported app language / localization options.
 */
enum class AppLanguage(val code: String, val displayName: String, val nativeName: String) {
    ENGLISH("en", "English", "English"),
    HINDI("hi", "Hindi", "हिन्दी"),
    GUJARATI("gu", "Gujarati", "ગુજરાતી"),
    MARATHI("mr", "Marathi", "मराठी")
}

/**
 * Quick action shortcuts available on dashboards.
 */
enum class QuickActionItem(
    val id: String,
    val title: String,
    val description: String,
    val defaultEnabled: Boolean
) {
    CREATE_LINE_SALE("create_line_sale", "Record Sale", "Create a new line sale with items and rates", true),
    CREATE_COUNTER_SALE("create_counter_sale", "Counter Sale", "Fast direct retail sale at the counter", true),
    CREATE_EXPENSE("create_expense", "Record Expense", "Track operational expenses and bills", true),
    CREATE_PURCHASE("create_purchase", "Record Purchase", "Log supplier inventory purchases", true),
    CASH_DRAWER("cash_drawer", "Cash Drawer", "Manage float, cash in, and daily reconciliation", true),
    BANK_ACCOUNTS("bank_accounts", "Bank Accounts", "Audit business bank accounts and settlements", false),
    PRODUCTS("products", "Product Catalog", "Manage items, pricing, and stock levels", true)
}

/**
 * Container representing the full state of user settings.
 */
@Immutable
data class UserSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val fontSize: AppFontSize = AppFontSize.MEDIUM,
    val language: AppLanguage = AppLanguage.ENGLISH,
    val enabledQuickActions: Set<String> = QuickActionItem.values().filter { it.defaultEnabled }.map { it.id }.toSet(),
    val dockOrder: List<BottomBarDestination> = emptyList()
)
