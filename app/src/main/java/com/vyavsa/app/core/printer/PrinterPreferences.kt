package com.vyavsa.app.core.printer

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persists thermal printer preferences such as selected device, connection transport, paper width, and network IP.
 */
@Singleton
class PrinterPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("vyavsa_printer_prefs", Context.MODE_PRIVATE)

    private val _selectedPrinterAddress = MutableStateFlow<String?>(prefs.getString(KEY_PRINTER_ADDRESS, null))
    val selectedPrinterAddress: StateFlow<String?> = _selectedPrinterAddress.asStateFlow()

    private val _selectedPrinterName = MutableStateFlow<String?>(prefs.getString(KEY_PRINTER_NAME, null))
    val selectedPrinterName: StateFlow<String?> = _selectedPrinterName.asStateFlow()

    private val _selectedConnectionType = MutableStateFlow(
        try {
            val typeStr = prefs.getString(KEY_CONNECTION_TYPE, PrinterConnectionType.BLUETOOTH.name)
            PrinterConnectionType.valueOf(typeStr ?: PrinterConnectionType.BLUETOOTH.name)
        } catch (_: Exception) {
            PrinterConnectionType.BLUETOOTH
        }
    )
    val selectedConnectionType: StateFlow<PrinterConnectionType> = _selectedConnectionType.asStateFlow()

    private val _savedNetworkPrinterIp = MutableStateFlow<String?>(prefs.getString(KEY_NETWORK_IP, "192.168.1.100:9100"))
    val savedNetworkPrinterIp: StateFlow<String?> = _savedNetworkPrinterIp.asStateFlow()

    private val _paperWidth = MutableStateFlow(
        PaperWidth.fromWidthMm(prefs.getInt(KEY_PAPER_WIDTH, 58))
    )
    val paperWidth: StateFlow<PaperWidth> = _paperWidth.asStateFlow()

    fun saveSelectedPrinter(printer: PrinterDevice?) {
        prefs.edit().apply {
            if (printer != null) {
                putString(KEY_PRINTER_ADDRESS, printer.address)
                putString(KEY_PRINTER_NAME, printer.name)
                putString(KEY_CONNECTION_TYPE, printer.connectionType.name)
            } else {
                remove(KEY_PRINTER_ADDRESS)
                remove(KEY_PRINTER_NAME)
            }
            apply()
        }
        _selectedPrinterAddress.value = printer?.address
        _selectedPrinterName.value = printer?.name
        if (printer != null) {
            _selectedConnectionType.value = printer.connectionType
        }
    }

    fun saveConnectionType(type: PrinterConnectionType) {
        prefs.edit().putString(KEY_CONNECTION_TYPE, type.name).apply()
        _selectedConnectionType.value = type
    }

    fun saveNetworkPrinterIp(ip: String) {
        prefs.edit().putString(KEY_NETWORK_IP, ip).apply()
        _savedNetworkPrinterIp.value = ip
    }

    fun savePaperWidth(width: PaperWidth) {
        prefs.edit().putInt(KEY_PAPER_WIDTH, width.widthMm).apply()
        _paperWidth.value = width
    }

    companion object {
        private const val KEY_PRINTER_ADDRESS = "selected_printer_address"
        private const val KEY_PRINTER_NAME = "selected_printer_name"
        private const val KEY_CONNECTION_TYPE = "selected_connection_type"
        private const val KEY_NETWORK_IP = "saved_network_printer_ip"
        private const val KEY_PAPER_WIDTH = "selected_paper_width"
    }
}
