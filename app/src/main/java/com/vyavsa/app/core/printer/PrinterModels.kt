package com.vyavsa.app.core.printer

import android.graphics.Bitmap
import androidx.compose.runtime.Immutable

/**
 * Supported paper roll widths for small thermal receipt printers.
 */
enum class PaperWidth(val widthMm: Int, val charsPerLine: Int, val label: String) {
    WIDTH_58MM(58, 32, "58 mm (Standard)"),
    WIDTH_80MM(80, 48, "80 mm (Wide POS)");

    companion object {
        fun fromWidthMm(mm: Int): PaperWidth {
            return if (mm >= 80) WIDTH_80MM else WIDTH_58MM
        }
    }
}

/**
 * Physical or network transport for thermal receipt printers.
 */
enum class PrinterConnectionType {
    BLUETOOTH,
    USB,
    NETWORK
}

@Immutable
data class PrinterDevice(
    val name: String,
    val address: String,
    val isBonded: Boolean = true,
    val connectionType: PrinterConnectionType = PrinterConnectionType.BLUETOOTH
)

enum class PrintJobStatus {
    IDLE,
    CONNECTING,
    PRINTING,
    SUCCESS,
    ERROR
}

@Immutable
data class PrintJobState(
    val status: PrintJobStatus = PrintJobStatus.IDLE,
    val message: String? = null
)

@Immutable
data class PrintItem(
    val name: String,
    val quantity: String,
    val unitPrice: String,
    val lineTotal: String
)

/**
 * 2D raster image interface decoupling bitmap conversion from Android framework classes,
 * enabling robust JVM unit testing of ESC/POS raster image generation.
 */
interface RasterImage {
    val width: Int
    val height: Int
    fun getPixel(x: Int, y: Int): Int
}

class AndroidBitmapRaster(private val bitmap: Bitmap) : RasterImage {
    override val width: Int get() = bitmap.width
    override val height: Int get() = bitmap.height
    override fun getPixel(x: Int, y: Int): Int = bitmap.getPixel(x, y)
}

@Immutable
data class InvoicePrintData(
    val businessName: String,
    val businessAddress: String? = null,
    val businessPhone: String? = null,
    val businessEmail: String? = null,
    val taxId: String? = null,
    val logoUrl: String? = null,
    val logoBitmap: Bitmap? = null,
    val invoiceTitle: String,
    val invoiceNumber: String,
    val invoiceDate: String,
    val customerName: String? = null,
    val customerPhone: String? = null,
    val route: String? = null,
    val salesman: String? = null,
    val items: List<PrintItem> = emptyList(),
    val singleItemName: String? = null,
    val subtotal: Double,
    val discount: Double = 0.0,
    val totalAmount: Double,
    val amountPaid: Double,
    val balanceDue: Double = 0.0,
    val paymentSummary: String,
    val note: String? = null,
    val footerHeading: String? = null,
    val footerParagraph: String? = null
) {
    val hasLogo: Boolean
        get() = logoBitmap != null || !logoUrl.isNullOrBlank()

    val effectiveFooterHeading: String
        get() = footerHeading?.trim()?.ifBlank { null } ?: "Thank you for your business!"

    val effectiveFooterParagraph: String
        get() = footerParagraph?.trim()?.ifBlank { null } ?: "Generated via Vyavsa Bill Book"
}
