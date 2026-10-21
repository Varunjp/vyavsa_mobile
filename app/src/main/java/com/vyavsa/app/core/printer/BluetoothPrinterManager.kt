package com.vyavsa.app.core.printer

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.IOException
import java.io.OutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

interface ThermalPrinterManager {
    fun isBluetoothSupported(): Boolean
    fun isBluetoothEnabled(): Boolean
    fun hasBluetoothPermission(): Boolean
    fun getPairedPrinters(): List<PrinterDevice>
    fun getConnectedUsbPrinters(): List<PrinterDevice> = emptyList()
    suspend fun printReceipt(printer: PrinterDevice, receiptBytes: ByteArray): Result<Unit> = printReceipt(printer.address, receiptBytes)
    suspend fun printReceipt(printerAddress: String, receiptBytes: ByteArray): Result<Unit>
    suspend fun testNetworkPrinter(address: String): Result<Unit> = Result.success(Unit)
}

@Singleton
class BluetoothPrinterManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val usbPrinterManager: UsbPrinterManager = UsbPrinterManager(context),
    private val networkPrinterManager: NetworkPrinterManager = NetworkPrinterManager()
) : ThermalPrinterManager {

    private val bluetoothManager: BluetoothManager? by lazy {
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    }

    private val bluetoothAdapter: BluetoothAdapter?
        get() = bluetoothManager?.adapter ?: @Suppress("DEPRECATION") BluetoothAdapter.getDefaultAdapter()

    override fun isBluetoothSupported(): Boolean = bluetoothAdapter != null

    override fun isBluetoothEnabled(): Boolean = bluetoothAdapter?.isEnabled == true

    override fun hasBluetoothPermission(): Boolean {
        return BluetoothPermissionHelper.hasConnectPermission(context)
    }

    fun hasBluetoothScanPermission(): Boolean {
        return BluetoothPermissionHelper.hasScanPermission(context)
    }

    override fun getPairedPrinters(): List<PrinterDevice> {
        val adapter = bluetoothAdapter ?: return emptyList()
        if (!hasBluetoothPermission()) return emptyList()

        return try {
            @Suppress("MissingPermission")
            val bondedDevices = adapter.bondedDevices ?: emptySet()
            bondedDevices.map { device ->
                PrinterDevice(
                    name = device.name ?: "Unknown Printer",
                    address = device.address,
                    isBonded = true,
                    connectionType = PrinterConnectionType.BLUETOOTH
                )
            }
        } catch (e: SecurityException) {
            emptyList()
        }
    }

    override fun getConnectedUsbPrinters(): List<PrinterDevice> {
        return usbPrinterManager.getConnectedUsbPrinters()
    }

    override suspend fun testNetworkPrinter(address: String): Result<Unit> {
        return networkPrinterManager.testConnection(address)
    }

    override suspend fun printReceipt(printer: PrinterDevice, receiptBytes: ByteArray): Result<Unit> {
        return when (printer.connectionType) {
            PrinterConnectionType.BLUETOOTH -> printBluetoothReceipt(printer.address, receiptBytes)
            PrinterConnectionType.USB -> usbPrinterManager.printReceipt(printer.address, receiptBytes)
            PrinterConnectionType.NETWORK -> networkPrinterManager.printReceipt(printer.address, receiptBytes)
        }
    }

    override suspend fun printReceipt(printerAddress: String, receiptBytes: ByteArray): Result<Unit> {
        // Smart routing when called with address string
        return if (printerAddress.contains(".") && (printerAddress.contains(":") || printerAddress.split(".").size == 4)) {
            networkPrinterManager.printReceipt(printerAddress, receiptBytes)
        } else if (printerAddress.startsWith("/dev/") || printerAddress.startsWith("usb", ignoreCase = true)) {
            usbPrinterManager.printReceipt(printerAddress, receiptBytes)
        } else {
            printBluetoothReceipt(printerAddress, receiptBytes)
        }
    }

    private suspend fun printBluetoothReceipt(printerAddress: String, receiptBytes: ByteArray): Result<Unit> = withContext(Dispatchers.IO) {
        val adapter = bluetoothAdapter
            ?: return@withContext Result.failure(IllegalStateException("Bluetooth is not supported on this device."))

        if (!adapter.isEnabled) {
            return@withContext Result.failure(IllegalStateException("Bluetooth is disabled. Please turn on Bluetooth."))
        }

        if (!hasBluetoothPermission()) {
            return@withContext Result.failure(SecurityException("Bluetooth permission denied. Please grant Nearby Devices permission."))
        }

        val device: BluetoothDevice = try {
            adapter.getRemoteDevice(printerAddress)
        } catch (e: IllegalArgumentException) {
            return@withContext Result.failure(IllegalArgumentException("Invalid Bluetooth MAC address: $printerAddress"))
        }

        var socket: BluetoothSocket? = null
        var outputStream: OutputStream? = null

        try {
            // Cancel discovery before connecting to preserve bandwidth and stability, only if scan permission is granted
            if (hasBluetoothScanPermission()) {
                try {
                    @Suppress("MissingPermission")
                    if (adapter.isDiscovering) {
                        adapter.cancelDiscovery()
                    }
                } catch (_: SecurityException) {
                    // Ignore scan exception to allow RFCOMM socket connection to proceed
                }
            }

            // Connect with timeout to avoid blocking indefinitely
            withTimeout(8000L) {
                socket = createSocketWithFallback(device)
                @Suppress("MissingPermission")
                socket?.connect()
            }

            val connectedSocket = socket
                ?: return@withContext Result.failure(IOException("Failed to open connection socket to printer."))

            if (!connectedSocket.isConnected) {
                return@withContext Result.failure(IOException("Could not establish connection to printer."))
            }

            outputStream = connectedSocket.outputStream

            // Write in 512-byte chunks with small delay to prevent buffer overflow on small thermal printers
            val chunkSize = 512
            var offset = 0
            while (offset < receiptBytes.size) {
                val length = (receiptBytes.size - offset).coerceAtMost(chunkSize)
                outputStream.write(receiptBytes, offset, length)
                outputStream.flush()
                offset += length
            }

            // Short pause to allow printer internal buffer to flush before closing socket
            kotlinx.coroutines.delay(250L)

            Result.success(Unit)
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            Result.failure(IOException("Printer connection timed out. Ensure the printer is turned on and within range."))
        } catch (e: IOException) {
            Result.failure(IOException("Printing failed: ${e.message ?: "Printer disconnected or unreachable."}"))
        } catch (e: SecurityException) {
            Result.failure(SecurityException("Bluetooth permission denied. Please enable Nearby Devices permission in App Info."))
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            try {
                outputStream?.close()
            } catch (_: Exception) {}
            try {
                socket?.close()
            } catch (_: Exception) {}
        }
    }

    private fun createSocketWithFallback(device: BluetoothDevice): BluetoothSocket {
        return try {
            @Suppress("MissingPermission")
            device.createRfcommSocketToServiceRecord(SPP_UUID)
        } catch (e: Exception) {
            // Fallback to hidden reflection method for printers that do not advertise standard SPP UUID
            val m = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
            @Suppress("MissingPermission")
            m.invoke(device, 1) as BluetoothSocket
        }
    }

    companion object {
        // Standard Serial Port Profile (SPP) UUID used by ESC/POS thermal printers
        private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }
}
