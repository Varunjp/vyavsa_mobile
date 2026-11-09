package com.vyavsa.app.core.security

import com.vyavsa.app.core.model.UserRole

/**
 * Authoritative fine-grained permissions matching backend RBAC endpoints and policies.
 */
enum class AppPermission {
    VIEW_CUSTOMERS,
    CREATE_CUSTOMERS,
    UPDATE_CUSTOMERS,
    DELETE_CUSTOMERS,
    ADJUST_CUSTOMER_BALANCE,
    VIEW_CUSTOMER_FINANCIALS,
    COLLECT_CUSTOMER_PAYMENT,
    COLLECT_CUSTOMER_ADVANCE,
    VIEW_PAYMENT_HISTORY,
    VIEW_INVOICES,
    CREATE_LINE_SALE,
    CREATE_COUNTER_SALE,
    PRINT_DOWNLOAD_INVOICE,
    VIEW_REPORTS,
    MANAGE_STAFF,
    MANAGE_SUPPLIERS,
    VIEW_PARTIES,
    CREATE_PARTIES,
    UPDATE_PARTIES,
    DELETE_PARTIES,
    VIEW_PURCHASES,
    CREATE_PURCHASE,
    UPDATE_PURCHASE,
    DELETE_PURCHASE,
    VIEW_EXPENSES,
    CREATE_EXPENSE,
    UPDATE_EXPENSE,
    DELETE_EXPENSE;

    companion object {
        fun permissionsForRole(role: UserRole): Set<AppPermission> {
            return when (role) {
                UserRole.ADMIN -> values().toSet()
                UserRole.OFFICE -> setOf(
                    VIEW_CUSTOMERS,
                    VIEW_CUSTOMER_FINANCIALS,
                    COLLECT_CUSTOMER_PAYMENT,
                    COLLECT_CUSTOMER_ADVANCE,
                    VIEW_PAYMENT_HISTORY,
                    VIEW_INVOICES,
                    CREATE_LINE_SALE,
                    CREATE_COUNTER_SALE,
                    PRINT_DOWNLOAD_INVOICE,
                    VIEW_REPORTS,
                    MANAGE_STAFF,
                    MANAGE_SUPPLIERS,
                    VIEW_PARTIES,
                    VIEW_PURCHASES,
                    CREATE_PURCHASE,
                    UPDATE_PURCHASE,
                    VIEW_EXPENSES,
                    CREATE_EXPENSE,
                    UPDATE_EXPENSE
                )
                UserRole.SALES -> setOf(
                    VIEW_CUSTOMERS,
                    VIEW_CUSTOMER_FINANCIALS,
                    COLLECT_CUSTOMER_PAYMENT,
                    COLLECT_CUSTOMER_ADVANCE,
                    VIEW_PAYMENT_HISTORY,
                    VIEW_INVOICES,
                    CREATE_LINE_SALE,
                    PRINT_DOWNLOAD_INVOICE,
                    VIEW_PARTIES,
                    CREATE_PARTIES,
                    UPDATE_PARTIES,
                    VIEW_PURCHASES,
                    CREATE_PURCHASE,
                    UPDATE_PURCHASE,
                    VIEW_EXPENSES,
                    CREATE_EXPENSE,
                    UPDATE_EXPENSE
                )
                UserRole.LINE -> setOf(
                    VIEW_CUSTOMERS,
                    VIEW_CUSTOMER_FINANCIALS,
                    COLLECT_CUSTOMER_PAYMENT,
                    COLLECT_CUSTOMER_ADVANCE,
                    VIEW_PAYMENT_HISTORY,
                    VIEW_INVOICES,
                    CREATE_LINE_SALE,
                    PRINT_DOWNLOAD_INVOICE,
                    VIEW_PARTIES
                )
                UserRole.USER -> setOf(
                    VIEW_INVOICES,
                    CREATE_LINE_SALE,
                    CREATE_COUNTER_SALE,
                    PRINT_DOWNLOAD_INVOICE,
                    VIEW_REPORTS,
                    VIEW_PARTIES
                )
            }
        }
    }
}
