package com.vyavsa.app.core.security

import com.vyavsa.app.core.model.UserRole
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PurchasesAndExpensesRbacTest {

    @Test
    fun testSalesRolePermissions() {
        val perms = AppPermission.permissionsForRole(UserRole.SALES)
        
        // SALES can view, create, update
        assertTrue("SALES should have VIEW_PURCHASES", perms.contains(AppPermission.VIEW_PURCHASES))
        assertTrue("SALES should have CREATE_PURCHASE", perms.contains(AppPermission.CREATE_PURCHASE))
        assertTrue("SALES should have UPDATE_PURCHASE", perms.contains(AppPermission.UPDATE_PURCHASE))
        assertTrue("SALES should have VIEW_EXPENSES", perms.contains(AppPermission.VIEW_EXPENSES))
        assertTrue("SALES should have CREATE_EXPENSE", perms.contains(AppPermission.CREATE_EXPENSE))
        assertTrue("SALES should have UPDATE_EXPENSE", perms.contains(AppPermission.UPDATE_EXPENSE))

        // SALES cannot delete
        assertFalse("SALES should NOT have DELETE_PURCHASE", perms.contains(AppPermission.DELETE_PURCHASE))
        assertFalse("SALES should NOT have DELETE_EXPENSE", perms.contains(AppPermission.DELETE_EXPENSE))
    }

    @Test
    fun testOfficeRolePermissions() {
        val perms = AppPermission.permissionsForRole(UserRole.OFFICE)
        
        assertTrue(perms.contains(AppPermission.VIEW_PURCHASES))
        assertTrue(perms.contains(AppPermission.CREATE_PURCHASE))
        assertTrue(perms.contains(AppPermission.UPDATE_PURCHASE))
        assertTrue(perms.contains(AppPermission.VIEW_EXPENSES))
        assertTrue(perms.contains(AppPermission.CREATE_EXPENSE))
        assertTrue(perms.contains(AppPermission.UPDATE_EXPENSE))

        assertFalse(perms.contains(AppPermission.DELETE_PURCHASE))
        assertFalse(perms.contains(AppPermission.DELETE_EXPENSE))
    }

    @Test
    fun testAdminRolePermissions() {
        val perms = AppPermission.permissionsForRole(UserRole.ADMIN)
        
        assertTrue(perms.contains(AppPermission.VIEW_PURCHASES))
        assertTrue(perms.contains(AppPermission.CREATE_PURCHASE))
        assertTrue(perms.contains(AppPermission.UPDATE_PURCHASE))
        assertTrue(perms.contains(AppPermission.DELETE_PURCHASE))
        assertTrue(perms.contains(AppPermission.VIEW_EXPENSES))
        assertTrue(perms.contains(AppPermission.CREATE_EXPENSE))
        assertTrue(perms.contains(AppPermission.UPDATE_EXPENSE))
        assertTrue(perms.contains(AppPermission.DELETE_EXPENSE))
    }

    @Test
    fun testLineRolePermissions() {
        val perms = AppPermission.permissionsForRole(UserRole.LINE)
        
        // LINE is strictly restricted to field line sales; no access to purchases or expenses
        assertFalse(perms.contains(AppPermission.VIEW_PURCHASES))
        assertFalse(perms.contains(AppPermission.CREATE_PURCHASE))
        assertFalse(perms.contains(AppPermission.UPDATE_PURCHASE))
        assertFalse(perms.contains(AppPermission.DELETE_PURCHASE))
        assertFalse(perms.contains(AppPermission.VIEW_EXPENSES))
        assertFalse(perms.contains(AppPermission.CREATE_EXPENSE))
        assertFalse(perms.contains(AppPermission.UPDATE_EXPENSE))
        assertFalse(perms.contains(AppPermission.DELETE_EXPENSE))
    }
}
