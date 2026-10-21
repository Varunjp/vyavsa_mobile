package com.vyavsa.app.core.printer

import android.Manifest
import android.os.Build
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BluetoothPermissionHelperTest {

    @Test
    fun testRequiredPermissionsForAndroid12Plus() {
        val permissions = BluetoothPermissionHelper.getRequiredPermissions(Build.VERSION_CODES.S)
        assertEquals(2, permissions.size)
        assertTrue(permissions.contains(Manifest.permission.BLUETOOTH_CONNECT))
        assertTrue(permissions.contains(Manifest.permission.BLUETOOTH_SCAN))
    }

    @Test
    fun testRequiredPermissionsForAndroid13Plus() {
        val permissions = BluetoothPermissionHelper.getRequiredPermissions(Build.VERSION_CODES.TIRAMISU)
        assertEquals(2, permissions.size)
        assertTrue(permissions.contains(Manifest.permission.BLUETOOTH_CONNECT))
        assertTrue(permissions.contains(Manifest.permission.BLUETOOTH_SCAN))
    }

    @Test
    fun testRequiredPermissionsForLegacyAndroid11AndLower() {
        val permissions = BluetoothPermissionHelper.getRequiredPermissions(Build.VERSION_CODES.R)
        assertEquals(2, permissions.size)
        assertTrue(permissions.contains(Manifest.permission.BLUETOOTH))
        assertTrue(permissions.contains(Manifest.permission.BLUETOOTH_ADMIN))
    }

    @Test
    fun testRequiredPermissionsForAndroid10() {
        val permissions = BluetoothPermissionHelper.getRequiredPermissions(Build.VERSION_CODES.Q)
        assertEquals(2, permissions.size)
        assertTrue(permissions.contains(Manifest.permission.BLUETOOTH))
        assertTrue(permissions.contains(Manifest.permission.BLUETOOTH_ADMIN))
    }
}
