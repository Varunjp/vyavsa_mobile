package com.vyavsa.app.core.printer

import android.graphics.Bitmap
import androidx.compose.runtime.Immutable

/**
 * Supported paper roll widths for small thermal receipt printers.
 */
enum class PaperWidth(val widthMm: Int, val charsPerLine: Int, val label: String) {
    WIDTH_58MM(58, 32, "58 mm (Standard)"),
    WIDTH_80MM(80, 48, "80 mm (Wide POS)"),
    WIDTH_A4(210, 72, "A4 (Sheet)");

    companion object {
        fun fromWidthMm(mm: Int): PaperWidth {
            return when {
                mm >= 150 -> WIDTH_A4
                mm >= 80 -> WIDTH_80MM
                else -> WIDTH_58MM
            }
        }
    }
}

/**
 * Built-in invoice themes suitable for thermal and standard POS receipts.
 */
enum class InvoiceTheme(val title: String, val description: String) {
    CLASSIC("Classic Retail", "Centered business header, clear bordered dividers, and classic receipt totals."),
    MODERN_MINIMAL("Modern Minimal", "Clean left-aligned typography, subtle divider lines, and uncluttered compact layout."),
    DETAILED_GST("Detailed GST", "Tax invoice layout with tax identification box, customer details, and terms/signature block.")
}

/**
 * Font sizing / density for invoice printing.
 */
enum class PrintTypography(val label: String, val scaleFactor: Float) {
    STANDARD("Standard (Normal)", 1.0f),
    COMPACT("Compact (Dense)", 0.85f),
    LARGE("Large (Readable)", 1.15f)
}

/**
 * Content visibility switches for invoices.
 */
@Immutable
data class InvoiceContentSettings(
    val showBusinessAddress: Boolean = true,
    val showBusinessPhone: Boolean = true,
    val showBusinessEmail: Boolean = true,
    val showTaxId: Boolean = true,
    val showCustomerDetails: Boolean = true,
    val showNotes: Boolean = true,
    val showPaymentInfo: Boolean = true,
    val showFooterNote: Boolean = true,
    val showLogo: Boolean = true
)

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
