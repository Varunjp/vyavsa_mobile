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
    MANAGE_SUPPLIERS;

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
                    MANAGE_SUPPLIERS
                )
                UserRole.SALES, UserRole.LINE -> setOf(
                    VIEW_CUSTOMERS,
                    VIEW_CUSTOMER_FINANCIALS,
                    COLLECT_CUSTOMER_PAYMENT,
                    COLLECT_CUSTOMER_ADVANCE,
                    VIEW_PAYMENT_HISTORY,
                    VIEW_INVOICES,
                    CREATE_LINE_SALE,
                    PRINT_DOWNLOAD_INVOICE
                )
                UserRole.USER -> setOf(
                    VIEW_INVOICES,
                    CREATE_LINE_SALE,
                    CREATE_COUNTER_SALE,
                    PRINT_DOWNLOAD_INVOICE,
                    VIEW_REPORTS
                )
            }
        }
    }
}
