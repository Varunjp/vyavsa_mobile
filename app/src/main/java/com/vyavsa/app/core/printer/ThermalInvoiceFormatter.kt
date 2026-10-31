package com.vyavsa.app.core.printer

import java.io.ByteArrayOutputStream
import java.nio.charset.Charset
import java.util.Locale

/**
 * Dedicated engine for formatting sales invoices for thermal receipt printers (58 mm, 80 mm, and A4).
 * Handles alignment, columnar tables, wrapping of long product names, right-aligned totals,
 * multiple built-in themes (Classic, Modern Minimal, Detailed GST), content toggles,
 * and standard ESC/POS hardware control sequences.
 */
object ThermalInvoiceFormatter {

    private val CP437: Charset = runCatching { Charset.forName("CP437") }.getOrDefault(Charsets.US_ASCII)

    /**
     * Formats an invoice into a human-readable plain text receipt string matching the selected theme.
     */
    fun formatPlainTextReceipt(
        data: InvoicePrintData,
        paperWidth: PaperWidth = PaperWidth.WIDTH_58MM,
        theme: InvoiceTheme = InvoiceTheme.CLASSIC,
        contentSettings: InvoiceContentSettings = InvoiceContentSettings(),
        typography: PrintTypography = PrintTypography.STANDARD
    ): String {
        return when (theme) {
            InvoiceTheme.CLASSIC -> formatClassicPlainText(data, paperWidth, contentSettings, typography)
            InvoiceTheme.MODERN_MINIMAL -> formatModernMinimalPlainText(data, paperWidth, contentSettings, typography)
            InvoiceTheme.DETAILED_GST -> formatDetailedGstPlainText(data, paperWidth, contentSettings, typography)
        }
    }

    /**
     * Formats an invoice into an ESC/POS byte sequence suitable for raw thermal transmission matching the selected theme.
     */
    fun formatEscPosReceipt(
        data: InvoicePrintData,
        paperWidth: PaperWidth = PaperWidth.WIDTH_58MM,
        theme: InvoiceTheme = InvoiceTheme.CLASSIC,
        contentSettings: InvoiceContentSettings = InvoiceContentSettings(),
        typography: PrintTypography = PrintTypography.STANDARD
    ): ByteArray {
        return when (theme) {
            InvoiceTheme.CLASSIC -> formatClassicEscPos(data, paperWidth, contentSettings, typography)
            InvoiceTheme.MODERN_MINIMAL -> formatModernMinimalEscPos(data, paperWidth, contentSettings, typography)
            InvoiceTheme.DETAILED_GST -> formatDetailedGstEscPos(data, paperWidth, contentSettings, typography)
        }
    }

    // ========================================================================
    // 1. CLASSIC RETAIL THEME
    // ========================================================================

    private fun formatClassicPlainText(
        data: InvoicePrintData,
        paperWidth: PaperWidth,
        content: InvoiceContentSettings,
        typography: PrintTypography
    ): String {
        val width = paperWidth.charsPerLine
        val sb = StringBuilder()

        // 1. Business Header (Centered)
        sb.appendLine(centerText(data.businessName.uppercase(Locale.US), width))
        if (content.showBusinessAddress && !data.businessAddress.isNullOrBlank()) {
            wrapText(data.businessAddress, width).forEach { line ->
                sb.appendLine(centerText(line, width))
            }
        }
        if (content.showBusinessPhone && !data.businessPhone.isNullOrBlank()) {
            sb.appendLine(centerText("Ph: ${data.businessPhone}", width))
        }
        if (content.showBusinessEmail && !data.businessEmail.isNullOrBlank()) {
            sb.appendLine(centerText("Email: ${data.businessEmail}", width))
        }
        if (content.showTaxId && !data.taxId.isNullOrBlank()) {
            sb.appendLine(centerText("GSTIN / Tax: ${data.taxId}", width))
        }

        // Section Separator
        sb.appendLine("=".repeat(width))

        // 2. Invoice Metadata
        sb.appendLine(centerText(data.invoiceTitle.uppercase(Locale.US), width))
        sb.appendLine(formatKeyValue("Invoice #:", data.invoiceNumber, width))
        sb.appendLine(formatKeyValue("Date:", data.invoiceDate, width))

        if (content.showCustomerDetails) {
            if (!data.customerName.isNullOrBlank()) {
                sb.appendLine(formatKeyValue("Customer:", data.customerName, width))
            }
            if (!data.customerPhone.isNullOrBlank()) {
                sb.appendLine(formatKeyValue("Phone:", data.customerPhone, width))
            }
            if (!data.route.isNullOrBlank()) {
                sb.appendLine(formatKeyValue("Route:", data.route, width))
            }
            if (!data.salesman.isNullOrBlank()) {
                sb.appendLine(formatKeyValue("Salesman:", data.salesman, width))
            }
        }

        // Item Header Separator
        sb.appendLine("-".repeat(width))

        // 3. Line Items
        formatItemsByWidth(data, sb, paperWidth)

        // Financial Totals Separator
        sb.appendLine("-".repeat(width))

        // 4. Financial Summary
        sb.appendLine(formatKeyValue("Subtotal:", formatCurrency(data.subtotal), width))
        if (data.discount > 0.0) {
            sb.appendLine(formatKeyValue("Discount:", "-${formatCurrency(data.discount)}", width))
        }
        if (data.previousBalance > 0.0) {
            sb.appendLine(formatKeyValue("Prev Customer Due:", formatCurrency(data.previousBalance), width))
        }
        sb.appendLine(formatKeyValue("TOTAL AMOUNT:", formatCurrency(data.totalAmount), width))
        val directReceived = if (data.amountReceivedDirect > 0.0) data.amountReceivedDirect else data.amountPaid
        sb.appendLine(formatKeyValue("Amount Paid:", formatCurrency(directReceived), width))
        if (data.advanceApplied > 0.0) {
            sb.appendLine(formatKeyValue("Paid via Advance:", formatCurrency(data.advanceApplied), width))
            val totalSettled = if (data.totalSettledForSale > 0.0) data.totalSettledForSale else (directReceived + data.advanceApplied)
            sb.appendLine(formatKeyValue("Total Settled:", formatCurrency(totalSettled), width))
        }

        if (data.balanceDue > 0.0) {
            sb.appendLine(formatKeyValue("BALANCE DUE:", formatCurrency(data.balanceDue), width))
        } else {
            sb.appendLine(centerText("*** PAID IN FULL ***", width))
        }

        if (data.hasCustomerLedger && (data.updatedCustomerBalance > 0.0 || data.previousBalance > 0.0)) {
            sb.appendLine(formatKeyValue("Updated Customer Due:", formatCurrency(data.updatedCustomerBalance), width))
        }

        // 5. Payment Details & Notes
        if (content.showPaymentInfo && data.paymentSummary.isNotBlank()) {
            sb.appendLine(formatKeyValue("Payment Mode:", data.paymentSummary, width))
        }
        if (content.showNotes && !data.note.isNullOrBlank()) {
            sb.appendLine("Note: ${data.note}")
        }

        // Footer Separator
        sb.appendLine("=".repeat(width))

        // 6. Dynamic Footer
        if (content.showFooterNote) {
            wrapText(data.effectiveFooterHeading, width).forEach { line ->
                sb.appendLine(centerText(line, width))
            }
            wrapText(data.effectiveFooterParagraph, width).forEach { line ->
                sb.appendLine(centerText(line, width))
            }
        }

        return sb.toString()
    }

    private fun formatClassicEscPos(
        data: InvoicePrintData,
        paperWidth: PaperWidth,
        content: InvoiceContentSettings,
        typography: PrintTypography
    ): ByteArray {
        val width = paperWidth.charsPerLine
        val out = ByteArrayOutputStream()

        out.write(EscPosCommands.INIT)
        applyTypographyCommand(out, typography)

        // Logo
        if (content.showLogo) {
            writeLogoRaster(data, paperWidth, out)
        }

        // Header
        out.write(EscPosCommands.ALIGN_CENTER)
        out.write(EscPosCommands.DOUBLE_HEIGHT_ON)
        out.write(EscPosCommands.BOLD_ON)
        out.write(encodeString("${data.businessName.uppercase(Locale.US)}\n"))
        out.write(EscPosCommands.NORMAL_SIZE)
        out.write(EscPosCommands.BOLD_OFF)

        if (content.showBusinessAddress && !data.businessAddress.isNullOrBlank()) {
            wrapText(data.businessAddress, width).forEach { line ->
                out.write(encodeString("${line}\n"))
            }
        }
        if (content.showBusinessPhone && !data.businessPhone.isNullOrBlank()) {
            out.write(encodeString("Ph: ${data.businessPhone}\n"))
        }
        if (content.showBusinessEmail && !data.businessEmail.isNullOrBlank()) {
            out.write(encodeString("Email: ${data.businessEmail}\n"))
        }
        if (content.showTaxId && !data.taxId.isNullOrBlank()) {
            out.write(encodeString("GSTIN / Tax: ${data.taxId}\n"))
        }

        out.write(encodeString("=".repeat(width) + "\n"))

        // Title
        out.write(EscPosCommands.BOLD_ON)
        out.write(encodeString("${data.invoiceTitle.uppercase(Locale.US)}\n"))
        out.write(EscPosCommands.BOLD_OFF)

        out.write(EscPosCommands.ALIGN_LEFT)
        out.write(encodeString(formatKeyValue("Invoice #:", data.invoiceNumber, width) + "\n"))
        out.write(encodeString(formatKeyValue("Date:", data.invoiceDate, width) + "\n"))

        if (content.showCustomerDetails) {
            if (!data.customerName.isNullOrBlank()) {
                out.write(encodeString(formatKeyValue("Customer:", data.customerName, width) + "\n"))
            }
            if (!data.customerPhone.isNullOrBlank()) {
                out.write(encodeString(formatKeyValue("Phone:", data.customerPhone, width) + "\n"))
            }
            if (!data.route.isNullOrBlank()) {
                out.write(encodeString(formatKeyValue("Route:", data.route, width) + "\n"))
            }
            if (!data.salesman.isNullOrBlank()) {
                out.write(encodeString(formatKeyValue("Salesman:", data.salesman, width) + "\n"))
            }
        }

        out.write(encodeString("-".repeat(width) + "\n"))

        // Line Items
        val itemsSb = StringBuilder()
        formatItemsByWidth(data, itemsSb, paperWidth)
        out.write(encodeString(itemsSb.toString()))

        out.write(encodeString("-".repeat(width) + "\n"))

        // Totals
        out.write(encodeString(formatKeyValue("Subtotal:", formatCurrency(data.subtotal), width) + "\n"))
        if (data.discount > 0.0) {
            out.write(encodeString(formatKeyValue("Discount:", "-${formatCurrency(data.discount)}", width) + "\n"))
        }

        if (data.previousBalance > 0.0) {
            out.write(encodeString(formatKeyValue("Prev Customer Due:", formatCurrency(data.previousBalance), width) + "\n"))
        }

        out.write(EscPosCommands.BOLD_ON)
        out.write(encodeString(formatKeyValue("TOTAL AMOUNT:", formatCurrency(data.totalAmount), width) + "\n"))
        out.write(EscPosCommands.BOLD_OFF)

        val directReceived = if (data.amountReceivedDirect > 0.0) data.amountReceivedDirect else data.amountPaid
        out.write(encodeString(formatKeyValue("Amount Paid:", formatCurrency(directReceived), width) + "\n"))

        if (data.advanceApplied > 0.0) {
            out.write(encodeString(formatKeyValue("Paid via Advance:", formatCurrency(data.advanceApplied), width) + "\n"))
            val totalSettled = if (data.totalSettledForSale > 0.0) data.totalSettledForSale else (directReceived + data.advanceApplied)
            out.write(encodeString(formatKeyValue("Total Settled:", formatCurrency(totalSettled), width) + "\n"))
        }

        if (data.balanceDue > 0.0) {
            out.write(EscPosCommands.BOLD_ON)
            out.write(encodeString(formatKeyValue("BALANCE DUE:", formatCurrency(data.balanceDue), width) + "\n"))
            out.write(EscPosCommands.BOLD_OFF)
        } else {
            out.write(EscPosCommands.ALIGN_CENTER)
            out.write(EscPosCommands.BOLD_ON)
            out.write(encodeString("*** PAID IN FULL ***\n"))
            out.write(EscPosCommands.BOLD_OFF)
            out.write(EscPosCommands.ALIGN_LEFT)
        }

        if (data.hasCustomerLedger && (data.updatedCustomerBalance > 0.0 || data.previousBalance > 0.0)) {
            out.write(encodeString(formatKeyValue("Updated Customer Due:", formatCurrency(data.updatedCustomerBalance), width) + "\n"))
        }

        if (content.showPaymentInfo && data.paymentSummary.isNotBlank()) {
            out.write(encodeString(formatKeyValue("Payment Mode:", data.paymentSummary, width) + "\n"))
        }
        if (content.showNotes && !data.note.isNullOrBlank()) {
            out.write(encodeString("Note: ${data.note}\n"))
        }

        out.write(encodeString("=".repeat(width) + "\n"))

        // Footer
        if (content.showFooterNote) {
            out.write(EscPosCommands.ALIGN_CENTER)
            wrapText(data.effectiveFooterHeading, width).forEach { line ->
                out.write(encodeString("${line}\n"))
            }
            wrapText(data.effectiveFooterParagraph, width).forEach { line ->
                out.write(encodeString("${line}\n"))
            }
        }

        out.write(EscPosCommands.feedLines(4))
        out.write(EscPosCommands.CUT_PARTIAL)

        return out.toByteArray()
    }

    // ========================================================================
    // 2. MODERN MINIMAL THEME
    // ========================================================================

    private fun formatModernMinimalPlainText(
        data: InvoicePrintData,
        paperWidth: PaperWidth,
        content: InvoiceContentSettings,
        typography: PrintTypography
    ): String {
        val width = paperWidth.charsPerLine
        val sb = StringBuilder()

        // Left aligned clean header
        sb.appendLine(data.businessName.uppercase(Locale.US))
        if (content.showBusinessAddress && !data.businessAddress.isNullOrBlank()) {
            wrapText(data.businessAddress, width).forEach { line ->
                sb.appendLine("  $line")
            }
        }
        val contactList = mutableListOf<String>()
        if (content.showBusinessPhone && !data.businessPhone.isNullOrBlank()) contactList.add("Tel: ${data.businessPhone}")
        if (content.showBusinessEmail && !data.businessEmail.isNullOrBlank()) contactList.add(data.businessEmail)
        if (contactList.isNotEmpty()) {
            sb.appendLine("  " + contactList.joinToString(" | "))
        }
        if (content.showTaxId && !data.taxId.isNullOrBlank()) {
            sb.appendLine("  Tax ID: ${data.taxId}")
        }

        sb.appendLine("-".repeat(width))

        // Metadata
        sb.appendLine("INV: ${data.invoiceNumber}   DATE: ${data.invoiceDate}")
        if (content.showCustomerDetails && !data.customerName.isNullOrBlank()) {
            sb.appendLine("CLIENT: ${data.customerName}${if (!data.customerPhone.isNullOrBlank()) " (${data.customerPhone})" else ""}")
        }

        sb.appendLine("-".repeat(width))

        // Modern compact items
        if (data.items.isNotEmpty()) {
            data.items.forEachIndexed { idx, item ->
                sb.appendLine("${idx + 1}. ${item.name}")
                val rateQty = "   ${item.quantity} x ${formatCurrency(item.unitPrice.toDoubleOrNull() ?: 0.0)}"
                val lineTotal = formatCurrency(item.lineTotal.toDoubleOrNull() ?: 0.0)
                sb.appendLine(formatKeyValue(rateQty, lineTotal, width))
            }
        } else {
            val name = data.singleItemName ?: "Standard Order"
            sb.appendLine("1. $name")
            sb.appendLine(formatKeyValue("   1 x ${formatCurrency(data.subtotal)}", formatCurrency(data.subtotal), width))
        }

        sb.appendLine("-".repeat(width))

        // Financials
        sb.appendLine(formatKeyValue("Subtotal", formatCurrency(data.subtotal), width))
        if (data.discount > 0.0) {
            sb.appendLine(formatKeyValue("Discount", "-${formatCurrency(data.discount)}", width))
        }
        sb.appendLine(formatKeyValue("TOTAL DUE", formatCurrency(data.totalAmount), width))
        sb.appendLine(formatKeyValue("Paid", formatCurrency(data.amountPaid), width))
        if (data.balanceDue > 0.0) {
            sb.appendLine(formatKeyValue("Balance", formatCurrency(data.balanceDue), width))
        } else {
            sb.appendLine(formatKeyValue("Status", "SETTLED IN FULL", width))
        }

        if (content.showPaymentInfo && data.paymentSummary.isNotBlank()) {
            sb.appendLine(formatKeyValue("Payment via", data.paymentSummary, width))
        }
        if (content.showNotes && !data.note.isNullOrBlank()) {
            sb.appendLine("Memo: ${data.note}")
        }

        sb.appendLine("-".repeat(width))

        if (content.showFooterNote) {
            sb.appendLine(centerText("~ ${data.effectiveFooterHeading} ~", width))
        }

        return sb.toString()
    }

    private fun formatModernMinimalEscPos(
        data: InvoicePrintData,
        paperWidth: PaperWidth,
        content: InvoiceContentSettings,
        typography: PrintTypography
    ): ByteArray {
        val width = paperWidth.charsPerLine
        val out = ByteArrayOutputStream()

        out.write(EscPosCommands.INIT)
        applyTypographyCommand(out, typography)

        if (content.showLogo) {
            writeLogoRaster(data, paperWidth, out)
        }

        out.write(EscPosCommands.ALIGN_LEFT)
        out.write(EscPosCommands.BOLD_ON)
        out.write(encodeString("${data.businessName.uppercase(Locale.US)}\n"))
        out.write(EscPosCommands.BOLD_OFF)

        if (content.showBusinessAddress && !data.businessAddress.isNullOrBlank()) {
            wrapText(data.businessAddress, width).forEach { line ->
                out.write(encodeString("  $line\n"))
            }
        }
        val contactList = mutableListOf<String>()
        if (content.showBusinessPhone && !data.businessPhone.isNullOrBlank()) contactList.add("Tel: ${data.businessPhone}")
        if (content.showBusinessEmail && !data.businessEmail.isNullOrBlank()) contactList.add(data.businessEmail)
        if (contactList.isNotEmpty()) {
            out.write(encodeString("  " + contactList.joinToString(" | ") + "\n"))
        }
        if (content.showTaxId && !data.taxId.isNullOrBlank()) {
            out.write(encodeString("  Tax ID: ${data.taxId}\n"))
        }

        out.write(encodeString("-".repeat(width) + "\n"))

        out.write(encodeString("INV: ${data.invoiceNumber}   DATE: ${data.invoiceDate}\n"))
        if (content.showCustomerDetails && !data.customerName.isNullOrBlank()) {
            out.write(encodeString("CLIENT: ${data.customerName}${if (!data.customerPhone.isNullOrBlank()) " (${data.customerPhone})" else ""}\n"))
        }

        out.write(encodeString("-".repeat(width) + "\n"))

        // Items
        if (data.items.isNotEmpty()) {
            data.items.forEachIndexed { idx, item ->
                out.write(encodeString("${idx + 1}. ${item.name}\n"))
                val rateQty = "   ${item.quantity} x ${formatCurrency(item.unitPrice.toDoubleOrNull() ?: 0.0)}"
                val lineTotal = formatCurrency(item.lineTotal.toDoubleOrNull() ?: 0.0)
                out.write(encodeString(formatKeyValue(rateQty, lineTotal, width) + "\n"))
            }
        } else {
            val name = data.singleItemName ?: "Standard Order"
            out.write(encodeString("1. $name\n"))
            out.write(encodeString(formatKeyValue("   1 x ${formatCurrency(data.subtotal)}", formatCurrency(data.subtotal), width) + "\n"))
        }

        out.write(encodeString("-".repeat(width) + "\n"))

        out.write(encodeString(formatKeyValue("Subtotal", formatCurrency(data.subtotal), width) + "\n"))
        if (data.discount > 0.0) {
            out.write(encodeString(formatKeyValue("Discount", "-${formatCurrency(data.discount)}", width) + "\n"))
        }

        out.write(EscPosCommands.BOLD_ON)
        out.write(encodeString(formatKeyValue("TOTAL DUE", formatCurrency(data.totalAmount), width) + "\n"))
        out.write(EscPosCommands.BOLD_OFF)

        out.write(encodeString(formatKeyValue("Paid", formatCurrency(data.amountPaid), width) + "\n"))
        if (data.balanceDue > 0.0) {
            out.write(EscPosCommands.BOLD_ON)
            out.write(encodeString(formatKeyValue("Balance", formatCurrency(data.balanceDue), width) + "\n"))
            out.write(EscPosCommands.BOLD_OFF)
        } else {
            out.write(encodeString(formatKeyValue("Status", "SETTLED IN FULL", width) + "\n"))
        }

        if (content.showPaymentInfo && data.paymentSummary.isNotBlank()) {
            out.write(encodeString(formatKeyValue("Payment via", data.paymentSummary, width) + "\n"))
        }
        if (content.showNotes && !data.note.isNullOrBlank()) {
            out.write(encodeString("Memo: ${data.note}\n"))
        }

        out.write(encodeString("-".repeat(width) + "\n"))

        if (content.showFooterNote) {
            out.write(EscPosCommands.ALIGN_CENTER)
            out.write(encodeString("~ ${data.effectiveFooterHeading} ~\n"))
        }

        out.write(EscPosCommands.feedLines(4))
        out.write(EscPosCommands.CUT_PARTIAL)

        return out.toByteArray()
    }

    // ========================================================================
    // 3. DETAILED GST TAX INVOICE THEME
    // ========================================================================

    private fun formatDetailedGstPlainText(
        data: InvoicePrintData,
        paperWidth: PaperWidth,
        content: InvoiceContentSettings,
        typography: PrintTypography
    ): String {
        val width = paperWidth.charsPerLine
        val sb = StringBuilder()

        // Formal Header
        sb.appendLine(centerText("*** TAX INVOICE ***", width))
        sb.appendLine(centerText(data.businessName.uppercase(Locale.US), width))

        if (content.showBusinessAddress && !data.businessAddress.isNullOrBlank()) {
            wrapText(data.businessAddress, width).forEach { line ->
                sb.appendLine(centerText(line, width))
            }
        }
        if (content.showBusinessPhone && !data.businessPhone.isNullOrBlank()) {
            sb.appendLine(centerText("Phone: ${data.businessPhone}", width))
        }

        // Prominent Boxed GSTIN
        if (content.showTaxId && !data.taxId.isNullOrBlank()) {
            val innerBoxWidth = (width - 4).coerceAtLeast(10)
            sb.appendLine("+" + "-".repeat(width - 2) + "+")
            sb.appendLine("| " + centerText("GSTIN / TAX ID: ${data.taxId}", innerBoxWidth) + " |")
            sb.appendLine("+" + "-".repeat(width - 2) + "+")
        } else {
            sb.appendLine("=".repeat(width))
        }

        // Invoice Metadata
        sb.appendLine(formatKeyValue("Tax Invoice #:", data.invoiceNumber, width))
        sb.appendLine(formatKeyValue("Date of Issue:", data.invoiceDate, width))

        // Billed To Section
        if (content.showCustomerDetails) {
            sb.appendLine("-".repeat(width))
            sb.appendLine("BILLED TO (BUYER):")
            sb.appendLine("  Name: ${data.customerName ?: "Cash Counter Customer"}")
            if (!data.customerPhone.isNullOrBlank()) {
                sb.appendLine("  Phone: ${data.customerPhone}")
            }
            if (!data.route.isNullOrBlank()) {
                sb.appendLine("  Place of Supply / Route: ${data.route}")
            }
        }

        sb.appendLine("-".repeat(width))

        // Table
        formatItemsByWidth(data, sb, paperWidth)

        sb.appendLine("-".repeat(width))

        // Tax & Totals Breakdown
        sb.appendLine(formatKeyValue("Taxable Value:", formatCurrency(data.subtotal), width))
        if (data.discount > 0.0) {
            sb.appendLine(formatKeyValue("Trade Discount:", "-${formatCurrency(data.discount)}", width))
        }
        sb.appendLine(formatKeyValue("Taxes (CGST+SGST):", "Included", width))
        sb.appendLine(formatKeyValue("NET INVOICE TOTAL:", formatCurrency(data.totalAmount), width))
        sb.appendLine(formatKeyValue("Amount Received:", formatCurrency(data.amountPaid), width))

        if (data.balanceDue > 0.0) {
            sb.appendLine(formatKeyValue("OUTSTANDING BALANCE:", formatCurrency(data.balanceDue), width))
        } else {
            sb.appendLine(centerText("*** INVOICE FULLY SETTLED ***", width))
        }

        if (content.showPaymentInfo && data.paymentSummary.isNotBlank()) {
            sb.appendLine(formatKeyValue("Payment Mode:", data.paymentSummary, width))
        }
        if (content.showNotes && !data.note.isNullOrBlank()) {
            sb.appendLine("Remarks: ${data.note}")
        }

        sb.appendLine("=".repeat(width))

        // Legal & Signatory
        sb.appendLine("Terms: Goods once sold will not be returned.")
        sb.appendLine("Subject to local jurisdiction.")
        sb.appendLine("")
        sb.appendLine(formatKeyValue("", "[ Authorized Signatory ]", width))

        if (content.showFooterNote) {
            sb.appendLine("-".repeat(width))
            sb.appendLine(centerText(data.effectiveFooterParagraph, width))
        }

        return sb.toString()
    }

    private fun formatDetailedGstEscPos(
        data: InvoicePrintData,
        paperWidth: PaperWidth,
        content: InvoiceContentSettings,
        typography: PrintTypography
    ): ByteArray {
        val width = paperWidth.charsPerLine
        val out = ByteArrayOutputStream()

        out.write(EscPosCommands.INIT)
        applyTypographyCommand(out, typography)

        if (content.showLogo) {
            writeLogoRaster(data, paperWidth, out)
        }

        out.write(EscPosCommands.ALIGN_CENTER)
        out.write(EscPosCommands.BOLD_ON)
        out.write(encodeString("*** TAX INVOICE ***\n"))
        out.write(EscPosCommands.DOUBLE_HEIGHT_ON)
        out.write(encodeString("${data.businessName.uppercase(Locale.US)}\n"))
        out.write(EscPosCommands.NORMAL_SIZE)
        out.write(EscPosCommands.BOLD_OFF)

        if (content.showBusinessAddress && !data.businessAddress.isNullOrBlank()) {
            wrapText(data.businessAddress, width).forEach { line ->
                out.write(encodeString("${line}\n"))
            }
        }
        if (content.showBusinessPhone && !data.businessPhone.isNullOrBlank()) {
            out.write(encodeString("Phone: ${data.businessPhone}\n"))
        }

        if (content.showTaxId && !data.taxId.isNullOrBlank()) {
            val innerBoxWidth = (width - 4).coerceAtLeast(10)
            out.write(encodeString("+" + "-".repeat(width - 2) + "+\n"))
            out.write(encodeString("| " + centerText("GSTIN / TAX ID: ${data.taxId}", innerBoxWidth) + " |\n"))
            out.write(encodeString("+" + "-".repeat(width - 2) + "+\n"))
        } else {
            out.write(encodeString("=".repeat(width) + "\n"))
        }

        out.write(EscPosCommands.ALIGN_LEFT)
        out.write(encodeString(formatKeyValue("Tax Invoice #:", data.invoiceNumber, width) + "\n"))
        out.write(encodeString(formatKeyValue("Date of Issue:", data.invoiceDate, width) + "\n"))

        if (content.showCustomerDetails) {
            out.write(encodeString("-".repeat(width) + "\n"))
            out.write(EscPosCommands.BOLD_ON)
            out.write(encodeString("BILLED TO (BUYER):\n"))
            out.write(EscPosCommands.BOLD_OFF)
            out.write(encodeString("  Name: ${data.customerName ?: "Cash Counter Customer"}\n"))
            if (!data.customerPhone.isNullOrBlank()) {
                out.write(encodeString("  Phone: ${data.customerPhone}\n"))
            }
            if (!data.route.isNullOrBlank()) {
                out.write(encodeString("  Place of Supply / Route: ${data.route}\n"))
            }
        }

        out.write(encodeString("-".repeat(width) + "\n"))

        // Table
        val itemsSb = StringBuilder()
        formatItemsByWidth(data, itemsSb, paperWidth)
        out.write(encodeString(itemsSb.toString()))

        out.write(encodeString("-".repeat(width) + "\n"))

        out.write(encodeString(formatKeyValue("Taxable Value:", formatCurrency(data.subtotal), width) + "\n"))
        if (data.discount > 0.0) {
            out.write(encodeString(formatKeyValue("Trade Discount:", "-${formatCurrency(data.discount)}", width) + "\n"))
        }
        out.write(encodeString(formatKeyValue("Taxes (CGST+SGST):", "Included", width) + "\n"))

        out.write(EscPosCommands.BOLD_ON)
        out.write(encodeString(formatKeyValue("NET INVOICE TOTAL:", formatCurrency(data.totalAmount), width) + "\n"))
        out.write(EscPosCommands.BOLD_OFF)

        out.write(encodeString(formatKeyValue("Amount Received:", formatCurrency(data.amountPaid), width) + "\n"))

        if (data.balanceDue > 0.0) {
            out.write(EscPosCommands.BOLD_ON)
            out.write(encodeString(formatKeyValue("OUTSTANDING BALANCE:", formatCurrency(data.balanceDue), width) + "\n"))
            out.write(EscPosCommands.BOLD_OFF)
        } else {
            out.write(EscPosCommands.ALIGN_CENTER)
            out.write(EscPosCommands.BOLD_ON)
            out.write(encodeString("*** INVOICE FULLY SETTLED ***\n"))
            out.write(EscPosCommands.BOLD_OFF)
            out.write(EscPosCommands.ALIGN_LEFT)
        }

        if (content.showPaymentInfo && data.paymentSummary.isNotBlank()) {
            out.write(encodeString(formatKeyValue("Payment Mode:", data.paymentSummary, width) + "\n"))
        }
        if (content.showNotes && !data.note.isNullOrBlank()) {
            out.write(encodeString("Remarks: ${data.note}\n"))
        }

        out.write(encodeString("=".repeat(width) + "\n"))

        out.write(encodeString("Terms: Goods once sold will not be returned.\n"))
        out.write(encodeString("Subject to local jurisdiction.\n\n"))
        out.write(encodeString(formatKeyValue("", "[ Authorized Signatory ]", width) + "\n"))

        if (content.showFooterNote) {
            out.write(encodeString("-".repeat(width) + "\n"))
            out.write(EscPosCommands.ALIGN_CENTER)
            out.write(encodeString("${data.effectiveFooterParagraph}\n"))
        }

        out.write(EscPosCommands.feedLines(4))
        out.write(EscPosCommands.CUT_PARTIAL)

        return out.toByteArray()
    }

    // ========================================================================
    // Table Item Layout Helpers
    // ========================================================================

    private fun formatItemsByWidth(data: InvoicePrintData, sb: StringBuilder, paperWidth: PaperWidth) {
        when (paperWidth) {
            PaperWidth.WIDTH_58MM -> formatItems58mm(data, sb, paperWidth.charsPerLine)
            PaperWidth.WIDTH_80MM -> formatItems80mm(data, sb, paperWidth.charsPerLine)
            PaperWidth.WIDTH_A4 -> formatItemsA4(data, sb, paperWidth.charsPerLine)
        }
    }

    /**
     * Helper to format table amounts so that numeric columns never overflow their allocated column width.
     */
    fun formatTableAmount(amount: Double, maxChars: Int, includeCurrency: Boolean = true): String {
        val standard = if (includeCurrency) formatCurrency(amount) else String.format(Locale.US, "%,.2f", amount)
        if (standard.length <= maxChars) {
            return standard
        }
        val withoutSymbol = String.format(Locale.US, "%,.2f", amount)
        if (withoutSymbol.length <= maxChars) {
            return withoutSymbol
        }
        return String.format(Locale.US, "%.2f", amount)
    }

    private fun formatItems58mm(data: InvoicePrintData, sb: StringBuilder, width: Int) {
        val qtyColWidth = 4
        val priceColWidth = 8
        val totalColWidth = 10
        val baseItemWidth = width - (qtyColWidth + priceColWidth + totalColWidth) // 10

        val header = "ITEM".padEnd(baseItemWidth) +
            "QTY".padStart(qtyColWidth) +
            "PRICE".padStart(priceColWidth) +
            "TOTAL".padStart(totalColWidth)
        sb.appendLine(header)
        sb.appendLine("-".repeat(width))

        if (data.items.isNotEmpty()) {
            data.items.forEach { item ->
                val qtyStr = item.quantity
                val priceVal = item.unitPrice.toDoubleOrNull() ?: 0.0
                val totalVal = item.lineTotal.toDoubleOrNull() ?: 0.0

                val totalStr = formatTableAmount(totalVal, totalColWidth)
                val priceStr = formatTableAmount(priceVal, priceColWidth)

                val dynamicTotalW = maxOf(totalColWidth, totalStr.length)
                val dynamicPriceW = maxOf(priceColWidth, priceStr.length)
                val dynamicQtyW = maxOf(qtyColWidth, qtyStr.length)
                val dynamicItemW = maxOf(6, width - (dynamicQtyW + dynamicPriceW + dynamicTotalW))

                val nameChunks = wrapText(item.name, dynamicItemW)
                val firstChunk = nameChunks.firstOrNull() ?: ""
                val line = firstChunk.padEnd(dynamicItemW) +
                    qtyStr.padStart(dynamicQtyW) +
                    priceStr.padStart(dynamicPriceW) +
                    totalStr.padStart(dynamicTotalW)
                sb.appendLine(line)

                for (i in 1 until nameChunks.size) {
                    sb.appendLine(nameChunks[i].padEnd(dynamicItemW))
                }
            }
        } else if (!data.singleItemName.isNullOrBlank()) {
            val nameChunks = wrapText(data.singleItemName, baseItemWidth)
            val qtyStr = "1"
            val priceStr = formatTableAmount(data.subtotal, priceColWidth)
            val totalStr = formatTableAmount(data.subtotal, totalColWidth)

            val firstChunk = nameChunks.firstOrNull() ?: ""
            sb.appendLine(firstChunk.padEnd(baseItemWidth) + qtyStr.padStart(qtyColWidth) + priceStr.padStart(priceColWidth) + totalStr.padStart(totalColWidth))
            for (i in 1 until nameChunks.size) {
                sb.appendLine(nameChunks[i].padEnd(baseItemWidth))
            }
        } else {
            val totalStr = formatTableAmount(data.subtotal, totalColWidth)
            val priceStr = formatTableAmount(data.subtotal, priceColWidth)
            sb.appendLine("Item".padEnd(baseItemWidth) + "1".padStart(qtyColWidth) + priceStr.padStart(priceColWidth) + totalStr.padStart(totalColWidth))
        }
    }

    private fun formatItems80mm(data: InvoicePrintData, sb: StringBuilder, width: Int) {
        val qtyColWidth = 5
        val priceColWidth = 13
        val totalColWidth = 14
        val baseItemWidth = width - (qtyColWidth + priceColWidth + totalColWidth) // 16

        val header = "ITEM".padEnd(baseItemWidth) +
            "QTY".padStart(qtyColWidth) +
            "PRICE".padStart(priceColWidth) +
            "TOTAL".padStart(totalColWidth)
        sb.appendLine(header)
        sb.appendLine("-".repeat(width))

        if (data.items.isNotEmpty()) {
            data.items.forEach { item ->
                val qtyStr = item.quantity
                val priceVal = item.unitPrice.toDoubleOrNull() ?: 0.0
                val totalVal = item.lineTotal.toDoubleOrNull() ?: 0.0

                val totalStr = formatTableAmount(totalVal, totalColWidth)
                val priceStr = formatTableAmount(priceVal, priceColWidth)

                val dynamicTotalW = maxOf(totalColWidth, totalStr.length)
                val dynamicPriceW = maxOf(priceColWidth, priceStr.length)
                val dynamicQtyW = maxOf(qtyColWidth, qtyStr.length)
                val dynamicItemW = maxOf(8, width - (dynamicQtyW + dynamicPriceW + dynamicTotalW))

                val nameChunks = wrapText(item.name, dynamicItemW)
                val firstChunk = nameChunks.firstOrNull() ?: ""
                val line = firstChunk.padEnd(dynamicItemW) +
                    qtyStr.padStart(dynamicQtyW) +
                    priceStr.padStart(dynamicPriceW) +
                    totalStr.padStart(dynamicTotalW)
                sb.appendLine(line)

                for (i in 1 until nameChunks.size) {
                    sb.appendLine(nameChunks[i].padEnd(dynamicItemW))
                }
            }
        } else if (!data.singleItemName.isNullOrBlank()) {
            val nameChunks = wrapText(data.singleItemName, baseItemWidth)
            val qtyStr = "1"
            val priceStr = formatTableAmount(data.subtotal, priceColWidth)
            val totalStr = formatTableAmount(data.subtotal, totalColWidth)

            val firstChunk = nameChunks.firstOrNull() ?: ""
            sb.appendLine(firstChunk.padEnd(baseItemWidth) + qtyStr.padStart(qtyColWidth) + priceStr.padStart(priceColWidth) + totalStr.padStart(totalColWidth))

            for (i in 1 until nameChunks.size) {
                sb.appendLine(nameChunks[i].padEnd(baseItemWidth))
            }
        } else {
            val priceStr = formatTableAmount(data.subtotal, priceColWidth)
            val totalStr = formatTableAmount(data.subtotal, totalColWidth)
            sb.appendLine("Sale Item".padEnd(baseItemWidth) + "1".padStart(qtyColWidth) + priceStr.padStart(priceColWidth) + totalStr.padStart(totalColWidth))
        }
    }

    private fun formatItemsA4(data: InvoicePrintData, sb: StringBuilder, width: Int) {
        val qtyColWidth = 6
        val priceColWidth = 18
        val totalColWidth = 22
        val baseItemWidth = width - (qtyColWidth + priceColWidth + totalColWidth) // 26

        val header = "ITEM DESCRIPTION".padEnd(baseItemWidth) +
            "QTY".padStart(qtyColWidth) +
            "UNIT RATE".padStart(priceColWidth) +
            "AMOUNT".padStart(totalColWidth)
        sb.appendLine(header)
        sb.appendLine("-".repeat(width))

        if (data.items.isNotEmpty()) {
            data.items.forEach { item ->
                val qtyStr = item.quantity
                val priceVal = item.unitPrice.toDoubleOrNull() ?: 0.0
                val totalVal = item.lineTotal.toDoubleOrNull() ?: 0.0

                val totalStr = formatTableAmount(totalVal, totalColWidth)
                val priceStr = formatTableAmount(priceVal, priceColWidth)

                val dynamicTotalW = maxOf(totalColWidth, totalStr.length)
                val dynamicPriceW = maxOf(priceColWidth, priceStr.length)
                val dynamicQtyW = maxOf(qtyColWidth, qtyStr.length)
                val dynamicItemW = maxOf(12, width - (dynamicQtyW + dynamicPriceW + dynamicTotalW))

                val nameChunks = wrapText(item.name, dynamicItemW)
                val firstChunk = nameChunks.firstOrNull() ?: ""
                val line = firstChunk.padEnd(dynamicItemW) +
                    qtyStr.padStart(dynamicQtyW) +
                    priceStr.padStart(dynamicPriceW) +
                    totalStr.padStart(dynamicTotalW)
                sb.appendLine(line)

                for (i in 1 until nameChunks.size) {
                    sb.appendLine(nameChunks[i].padEnd(dynamicItemW))
                }
            }
        } else {
            val name = data.singleItemName ?: "Sales Product"
            val nameChunks = wrapText(name, baseItemWidth)
            val qtyStr = "1"
            val priceStr = formatTableAmount(data.subtotal, priceColWidth)
            val totalStr = formatTableAmount(data.subtotal, totalColWidth)

            val firstChunk = nameChunks.firstOrNull() ?: ""
            sb.appendLine(firstChunk.padEnd(baseItemWidth) + qtyStr.padStart(qtyColWidth) + priceStr.padStart(priceColWidth) + totalStr.padStart(totalColWidth))
            for (i in 1 until nameChunks.size) {
                sb.appendLine(nameChunks[i].padEnd(baseItemWidth))
            }
        }
    }

    private fun writeLogoRaster(data: InvoicePrintData, paperWidth: PaperWidth, out: ByteArrayOutputStream) {
        val logoBmp = data.logoBitmap ?: return
        val (paperDots, maxLogoDots) = when (paperWidth) {
            PaperWidth.WIDTH_58MM -> 384 to 200
            PaperWidth.WIDTH_80MM -> 576 to 320
            PaperWidth.WIDTH_A4 -> 800 to 400
        }
        val rasterBytes = EscPosCommands.generateRasterBitImage(
            raster = AndroidBitmapRaster(logoBmp),
            maxWidthDots = maxLogoDots,
            paperWidthDots = paperDots,
            maxHeightDots = 120
        )
        if (rasterBytes.isNotEmpty()) {
            out.write(rasterBytes)
        }
    }

    private fun applyTypographyCommand(out: ByteArrayOutputStream, typography: PrintTypography) {
        when (typography) {
            PrintTypography.COMPACT -> out.write(EscPosCommands.FONT_B)
            PrintTypography.STANDARD -> out.write(EscPosCommands.FONT_A)
            PrintTypography.LARGE -> {
                out.write(EscPosCommands.FONT_A)
            }
        }
    }

    /**
     * Aligns key on the left and value on the right within [width] characters.
     * If key and value exceed [width], wraps key and aligns value cleanly.
     */
    fun formatKeyValue(key: String, value: String, width: Int): String {
        val safeWidth = width.coerceAtLeast(1)
        val totalLen = key.length + value.length
        if (totalLen + 1 <= safeWidth) {
            val spaceCount = safeWidth - totalLen
            return key + " ".repeat(spaceCount) + value
        }

        val sb = StringBuilder()
        val keyLines = wrapText(key, safeWidth)
        keyLines.forEach { sb.appendLine(it) }

        if (value.length <= safeWidth) {
            val spaces = safeWidth - value.length
            sb.append(" ".repeat(spaces)).append(value)
        } else {
            val valueLines = wrapText(value, safeWidth)
            valueLines.forEachIndexed { idx, vLine ->
                if (idx < valueLines.size - 1) {
                    sb.appendLine(vLine)
                } else {
                    val spaces = safeWidth - vLine.length
                    sb.append(" ".repeat(spaces)).append(vLine)
                }
            }
        }
        return sb.toString()
    }

    /**
     * Centers text within [width] characters.
     */
    fun centerText(text: String, width: Int): String {
        val safeWidth = width.coerceAtLeast(1)
        val trimmed = text.trim()
        if (trimmed.length >= safeWidth) return trimmed.take(safeWidth)
        val leftPadding = (safeWidth - trimmed.length) / 2
        val rightPadding = safeWidth - trimmed.length - leftPadding
        return " ".repeat(leftPadding) + trimmed + " ".repeat(rightPadding)
    }

    /**
     * Wraps a string into lines no longer than [maxWidth] characters without breaking words if possible.
     */
    fun wrapText(text: String, maxWidth: Int): List<String> {
        val safeWidth = maxWidth.coerceAtLeast(1)
        val clean = text.trim()
        if (clean.length <= safeWidth) return listOf(clean)

        val words = clean.split(Regex("\\s+"))
        val lines = mutableListOf<String>()
        var currentLine = StringBuilder()

        for (word in words) {
            if (word.length > safeWidth) {
                if (currentLine.isNotEmpty()) {
                    lines.add(currentLine.toString())
                    currentLine = StringBuilder()
                }
                var remaining = word
                while (remaining.length > safeWidth) {
                    lines.add(remaining.substring(0, safeWidth))
                    remaining = remaining.substring(safeWidth)
                }
                if (remaining.isNotEmpty()) {
                    currentLine.append(remaining)
                }
            } else if (currentLine.isEmpty()) {
                currentLine.append(word)
            } else if (currentLine.length + 1 + word.length <= safeWidth) {
                currentLine.append(" ").append(word)
            } else {
                lines.add(currentLine.toString())
                currentLine = StringBuilder(word)
            }
        }

        if (currentLine.isNotEmpty()) {
            lines.add(currentLine.toString())
        }

        return if (lines.isEmpty()) listOf("") else lines
    }

    /**
     * Formats currency in ASCII-friendly notation for thermal printers (e.g. "Rs. 1,250.00").
     */
    fun formatCurrency(amount: Double): String {
        return String.format(Locale.US, "Rs. %,.2f", amount)
    }

    private fun encodeString(str: String): ByteArray {
        val sanitized = str.replace("₹", "Rs. ")
        return sanitized.toByteArray(CP437)
    }
}
