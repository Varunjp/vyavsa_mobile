package com.vyavsa.app.core.printer

import android.content.Context
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages direct USB OTG thermal receipt printer detection, communication, and ESC/POS data transfer.
 */
@Singleton
class UsbPrinterManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val usbManager: UsbManager? by lazy {
        context.getSystemService(Context.USB_SERVICE) as? UsbManager
    }

    /**
     * Determines whether a given USB device represents an ESC/POS receipt printer.
     */
    fun isPrinterDevice(device: UsbDevice): Boolean {
        // Direct USB Printer Class (Class 7)
        if (device.deviceClass == UsbConstants.USB_CLASS_PRINTER) return true

        // Check interfaces for Printer Class or Vendor-Specific Bulk Out endpoint (standard in budget POS printers)
        for (i in 0 until device.interfaceCount) {
            val intf = device.getInterface(i)
            if (intf.interfaceClass == UsbConstants.USB_CLASS_PRINTER) return true
            if (intf.interfaceClass == UsbConstants.USB_CLASS_VENDOR_SPEC || intf.interfaceClass == UsbConstants.USB_CLASS_PER_INTERFACE) {
                for (e in 0 until intf.endpointCount) {
                    val ep = intf.getEndpoint(e)
                    if (ep.type == UsbConstants.USB_ENDPOINT_XFER_BULK && ep.direction == UsbConstants.USB_DIR_OUT) {
                        return true
                    }
                }
            }
        }
        return false
    }

    /**
     * Finds the primary printer interface and bulk OUT endpoint for raw ESC/POS byte streaming.
     */
    fun findPrinterEndpoint(device: UsbDevice): Pair<UsbInterface, UsbEndpoint>? {
        // 1. Try standard printer class interface first
        for (i in 0 until device.interfaceCount) {
            val intf = device.getInterface(i)
            if (intf.interfaceClass == UsbConstants.USB_CLASS_PRINTER) {
                for (e in 0 until intf.endpointCount) {
                    val ep = intf.getEndpoint(e)
                    if (ep.type == UsbConstants.USB_ENDPOINT_XFER_BULK && ep.direction == UsbConstants.USB_DIR_OUT) {
                        return Pair(intf, ep)
                    }
                }
            }
        }

        // 2. Fallback to vendor-specific bulk OUT endpoint
        for (i in 0 until device.interfaceCount) {
            val intf = device.getInterface(i)
            for (e in 0 until intf.endpointCount) {
                val ep = intf.getEndpoint(e)
                if (ep.type == UsbConstants.USB_ENDPOINT_XFER_BULK && ep.direction == UsbConstants.USB_DIR_OUT) {
                    return Pair(intf, ep)
                }
            }
        }
        return null
    }

    /**
     * Returns a list of all currently connected and recognized USB thermal printers.
     */
    fun getConnectedUsbPrinters(): List<PrinterDevice> {
        val manager = usbManager ?: return emptyList()
        val deviceList = manager.deviceList ?: return emptyList()

        return deviceList.values.filter { isPrinterDevice(it) }.map { device ->
            val name = (device.productName?.takeIf { it.isNotBlank() }
                ?: "USB Thermal Printer (${device.vendorId}:${device.productId})")
            PrinterDevice(
                name = name,
                address = device.deviceName,
                isBonded = true,
                connectionType = PrinterConnectionType.USB
            )
        }
    }

    /**
     * Checks if the app has been granted runtime permission to communicate with the USB device.
     */
    fun hasUsbPermission(deviceAddress: String): Boolean {
        val manager = usbManager ?: return false
        val device = manager.deviceList.values.firstOrNull { it.deviceName == deviceAddress } ?: return false
        return manager.hasPermission(device)
    }

    /**
     * Sends raw ESC/POS receipt byte sequence to the specified USB printer using bulk transfers.
     */
    suspend fun printReceipt(deviceAddress: String, receiptBytes: ByteArray): Result<Unit> = withContext(Dispatchers.IO) {
        val manager = usbManager
            ?: return@withContext Result.failure(IllegalStateException("USB service is not available on this device"))

        val deviceList = manager.deviceList ?: emptyMap()
        val device = deviceList[deviceAddress] ?: deviceList.values.firstOrNull { it.deviceName == deviceAddress }
            ?: return@withContext Result.failure(IOException("USB printer '$deviceAddress' is disconnected or unavailable"))

        if (!manager.hasPermission(device)) {
            return@withContext Result.failure(
                SecurityException("USB permission denied for ${device.productName ?: device.deviceName}. Please grant USB access in system settings.")
            )
        }

        val endpointPair = findPrinterEndpoint(device)
            ?: return@withContext Result.failure(IOException("No compatible bulk OUT printing endpoint found for USB device"))
        val (intf, endpoint) = endpointPair

        val connection = manager.openDevice(device)
            ?: return@withContext Result.failure(IOException("Unable to open connection to USB printer"))

        try {
            if (!connection.claimInterface(intf, true)) {
                return@withContext Result.failure(IOException("Unable to claim USB printer interface. Another app may be using it."))
            }

            var offset = 0
            while (offset < receiptBytes.size) {
                val chunkSize = minOf(4096, receiptBytes.size - offset)
                val transferred = connection.bulkTransfer(endpoint, receiptBytes, offset, chunkSize, 5000)
                if (transferred < 0) {
                    return@withContext Result.failure(IOException("USB bulk transfer failed while transmitting print data"))
                }
                offset += transferred
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(IOException("USB print failure: ${e.message}", e))
        } finally {
            try {
                connection.releaseInterface(intf)
            } catch (_: Exception) {}
            try {
                connection.close()
            } catch (_: Exception) {}
        }
    }
}
