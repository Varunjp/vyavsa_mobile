package com.vyavsa.app.core.printer

import java.io.ByteArrayOutputStream
import java.nio.charset.Charset
import java.util.Locale

/**
 * Dedicated engine for formatting sales invoices for thermal receipt printers (58 mm and 80 mm).
 * Handles alignment, columnar tables, wrapping of long product names, right-aligned totals,
 * and standard ESC/POS hardware control sequences.
 */
object ThermalInvoiceFormatter {

    private val CP437: Charset = runCatching { Charset.forName("CP437") }.getOrDefault(Charsets.US_ASCII)

    /**
     * Formats an invoice into a human-readable plain text receipt string.
     */
    fun formatPlainTextReceipt(data: InvoicePrintData, paperWidth: PaperWidth): String {
        val width = paperWidth.charsPerLine
        val sb = StringBuilder()

        // 1. Business Header (Centered)
        sb.appendLine(centerText(data.businessName.uppercase(Locale.US), width))
        if (!data.businessAddress.isNullOrBlank()) {
            wrapText(data.businessAddress, width).forEach { line ->
                sb.appendLine(centerText(line, width))
            }
        }
        if (!data.businessPhone.isNullOrBlank()) {
            sb.appendLine(centerText("Ph: ${data.businessPhone}", width))
        }
        if (!data.businessEmail.isNullOrBlank()) {
            sb.appendLine(centerText("Email: ${data.businessEmail}", width))
        }
        if (!data.taxId.isNullOrBlank()) {
            sb.appendLine(centerText("GSTIN / Tax: ${data.taxId}", width))
        }

        // Section Separator
        sb.appendLine("=".repeat(width))

        // 2. Invoice Metadata
        sb.appendLine(centerText(data.invoiceTitle.uppercase(Locale.US), width))
        sb.appendLine(formatKeyValue("Invoice #:", data.invoiceNumber, width))
        sb.appendLine(formatKeyValue("Date:", data.invoiceDate, width))

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

        // Item Header Separator
        sb.appendLine("-".repeat(width))

        // 3. Line Items
        if (paperWidth == PaperWidth.WIDTH_58MM) {
            formatItems58mm(data, sb, width)
        } else {
            formatItems80mm(data, sb, width)
        }

        // Financial Totals Separator
        sb.appendLine("-".repeat(width))

        // 4. Financial Summary
        sb.appendLine(formatKeyValue("Subtotal:", formatCurrency(data.subtotal), width))
        if (data.discount > 0.0) {
            sb.appendLine(formatKeyValue("Discount:", "-${formatCurrency(data.discount)}", width))
        }
        sb.appendLine(formatKeyValue("TOTAL AMOUNT:", formatCurrency(data.totalAmount), width))
        sb.appendLine(formatKeyValue("Amount Paid:", formatCurrency(data.amountPaid), width))

        if (data.balanceDue > 0.0) {
            sb.appendLine(formatKeyValue("BALANCE DUE:", formatCurrency(data.balanceDue), width))
        } else {
            sb.appendLine(centerText("*** PAID IN FULL ***", width))
        }

        // 5. Payment Details & Notes
        if (data.paymentSummary.isNotBlank()) {
            sb.appendLine(formatKeyValue("Payment Mode:", data.paymentSummary, width))
        }
        if (!data.note.isNullOrBlank()) {
            sb.appendLine("Note: ${data.note}")
        }

        // Footer Separator
        sb.appendLine("=".repeat(width))

        // 6. Dynamic Footer
        wrapText(data.effectiveFooterHeading, width).forEach { line ->
            sb.appendLine(centerText(line, width))
        }
        wrapText(data.effectiveFooterParagraph, width).forEach { line ->
            sb.appendLine(centerText(line, width))
        }

        return sb.toString()
    }

    /**
     * Formats an invoice into an ESC/POS byte sequence suitable for raw thermal transmission.
     */
    fun formatEscPosReceipt(data: InvoicePrintData, paperWidth: PaperWidth): ByteArray {
        val width = paperWidth.charsPerLine
        val out = ByteArrayOutputStream()

        // 1. Initialize Printer
        out.write(EscPosCommands.INIT)

        // 2. Company Logo (Raster Bit Image centered on thermal paper roll)
        val logoBmp = data.logoBitmap
        if (logoBmp != null) {
            val (paperDots, maxLogoDots) = if (paperWidth == PaperWidth.WIDTH_58MM) {
                384 to 200
            } else {
                576 to 320
            }
            val rasterBytes = EscPosCommands.generateRasterBitImage(
                raster = AndroidBitmapRaster(logoBmp),
                maxWidthDots = maxLogoDots,
                paperWidthDots = paperDots,
                maxHeightDots = 110
            )
            if (rasterBytes.isNotEmpty()) {
                out.write(rasterBytes)
            }
        }

        // 3. Business Header (Centered, Emphasized)
        out.write(EscPosCommands.ALIGN_CENTER)
        out.write(EscPosCommands.DOUBLE_HEIGHT_ON)
        out.write(EscPosCommands.BOLD_ON)
        out.write(encodeString("${data.businessName.uppercase(Locale.US)}\n"))
        out.write(EscPosCommands.NORMAL_SIZE)
        out.write(EscPosCommands.BOLD_OFF)

        if (!data.businessAddress.isNullOrBlank()) {
            wrapText(data.businessAddress, width).forEach { line ->
                out.write(encodeString("${line}\n"))
            }
        }
        if (!data.businessPhone.isNullOrBlank()) {
            out.write(encodeString("Ph: ${data.businessPhone}\n"))
        }
        if (!data.businessEmail.isNullOrBlank()) {
            out.write(encodeString("Email: ${data.businessEmail}\n"))
        }
        if (!data.taxId.isNullOrBlank()) {
            out.write(encodeString("GSTIN / Tax: ${data.taxId}\n"))
        }

        out.write(encodeString("=".repeat(width) + "\n"))

        // 3. Invoice Title & Metadata
        out.write(EscPosCommands.BOLD_ON)
        out.write(encodeString("${data.invoiceTitle.uppercase(Locale.US)}\n"))
        out.write(EscPosCommands.BOLD_OFF)

        out.write(EscPosCommands.ALIGN_LEFT)
        out.write(encodeString(formatKeyValue("Invoice #:", data.invoiceNumber, width) + "\n"))
        out.write(encodeString(formatKeyValue("Date:", data.invoiceDate, width) + "\n"))

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

        out.write(encodeString("-".repeat(width) + "\n"))

        // 4. Line Items Table
        val itemsSb = StringBuilder()
        if (paperWidth == PaperWidth.WIDTH_58MM) {
            formatItems58mm(data, itemsSb, width)
        } else {
            formatItems80mm(data, itemsSb, width)
        }
        out.write(encodeString(itemsSb.toString()))

        out.write(encodeString("-".repeat(width) + "\n"))

        // 5. Financial Totals
        out.write(encodeString(formatKeyValue("Subtotal:", formatCurrency(data.subtotal), width) + "\n"))
        if (data.discount > 0.0) {
            out.write(encodeString(formatKeyValue("Discount:", "-${formatCurrency(data.discount)}", width) + "\n"))
        }

        // Emphasized Grand Total
        out.write(EscPosCommands.BOLD_ON)
        out.write(encodeString(formatKeyValue("TOTAL AMOUNT:", formatCurrency(data.totalAmount), width) + "\n"))
        out.write(EscPosCommands.BOLD_OFF)

        out.write(encodeString(formatKeyValue("Amount Paid:", formatCurrency(data.amountPaid), width) + "\n"))

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

        if (data.paymentSummary.isNotBlank()) {
            out.write(encodeString(formatKeyValue("Payment Mode:", data.paymentSummary, width) + "\n"))
        }
        if (!data.note.isNullOrBlank()) {
            out.write(encodeString("Note: ${data.note}\n"))
        }

        out.write(encodeString("=".repeat(width) + "\n"))

        // 6. Dynamic Footer
        out.write(EscPosCommands.ALIGN_CENTER)
        wrapText(data.effectiveFooterHeading, width).forEach { line ->
            out.write(encodeString("${line}\n"))
        }
        wrapText(data.effectiveFooterParagraph, width).forEach { line ->
            out.write(encodeString("${line}\n"))
        }

        // 7. Feed lines & Partial Cut
        out.write(EscPosCommands.feedLines(4))
        out.write(EscPosCommands.CUT_PARTIAL)

        return out.toByteArray()
    }

    private fun formatItems58mm(data: InvoicePrintData, sb: StringBuilder, width: Int) {
        sb.appendLine("ITEM / QTY x RATE          TOTAL")
        if (data.items.isNotEmpty()) {
            data.items.forEach { item ->
                // Wrap item name so it never collides with quantities or prices
                wrapText(item.name, width).forEach { line ->
                    sb.appendLine(line)
                }
                val qtyRate = "  ${item.quantity} x ${formatCurrency(item.unitPrice.toDoubleOrNull() ?: 0.0)}"
                val lineTotal = formatCurrency(item.lineTotal.toDoubleOrNull() ?: 0.0)
                sb.appendLine(formatKeyValue(qtyRate, lineTotal, width))
            }
        } else if (!data.singleItemName.isNullOrBlank()) {
            wrapText(data.singleItemName, width).forEach { line ->
                sb.appendLine(line)
            }
            sb.appendLine(formatKeyValue("  1 x ${formatCurrency(data.subtotal)}", formatCurrency(data.subtotal), width))
        } else {
            sb.appendLine("Sale Item")
            sb.appendLine(formatKeyValue("  1 x ${formatCurrency(data.subtotal)}", formatCurrency(data.subtotal), width))
        }
    }

    private fun formatItems80mm(data: InvoicePrintData, sb: StringBuilder, width: Int) {
        // 48 columns total: Item (18) + Qty (6) + Sp (2) + Price (10) + Sp (2) + Total (10) = 48
        val itemColWidth = 18
        val qtyColWidth = 6
        val priceColWidth = 10
        val totalColWidth = 10

        val header = "ITEM".padEnd(itemColWidth) +
            "QTY".padStart(qtyColWidth) +
            "  " +
            "PRICE".padStart(priceColWidth) +
            "  " +
            "TOTAL".padStart(totalColWidth)
        sb.appendLine(header)
        sb.appendLine("-".repeat(width))

        if (data.items.isNotEmpty()) {
            data.items.forEach { item ->
                val nameChunks = wrapText(item.name, itemColWidth)
                val qtyStr = item.quantity.padStart(qtyColWidth)
                val priceStr = formatCurrency(item.unitPrice.toDoubleOrNull() ?: 0.0).padStart(priceColWidth)
                val totalStr = formatCurrency(item.lineTotal.toDoubleOrNull() ?: 0.0).padStart(totalColWidth)

                val firstChunk = nameChunks.firstOrNull() ?: ""
                sb.appendLine(firstChunk.padEnd(itemColWidth) + qtyStr + "  " + priceStr + "  " + totalStr)

                // If name wrapped, print remaining chunks indented in the item column
                for (i in 1 until nameChunks.size) {
                    sb.appendLine(nameChunks[i].padEnd(itemColWidth))
                }
            }
        } else if (!data.singleItemName.isNullOrBlank()) {
            val nameChunks = wrapText(data.singleItemName, itemColWidth)
            val qtyStr = "1".padStart(qtyColWidth)
            val priceStr = formatCurrency(data.subtotal).padStart(priceColWidth)
            val totalStr = formatCurrency(data.subtotal).padStart(totalColWidth)

            val firstChunk = nameChunks.firstOrNull() ?: ""
            sb.appendLine(firstChunk.padEnd(itemColWidth) + qtyStr + "  " + priceStr + "  " + totalStr)

            for (i in 1 until nameChunks.size) {
                sb.appendLine(nameChunks[i].padEnd(itemColWidth))
            }
        } else {
            val nameChunks = wrapText("Sale Item", itemColWidth)
            val qtyStr = "1".padStart(qtyColWidth)
            val priceStr = formatCurrency(data.subtotal).padStart(priceColWidth)
            val totalStr = formatCurrency(data.subtotal).padStart(totalColWidth)

            val firstChunk = nameChunks.firstOrNull() ?: ""
            sb.appendLine(firstChunk.padEnd(itemColWidth) + qtyStr + "  " + priceStr + "  " + totalStr)
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
        // Sanitize string for ASCII/CP437 receipt printer (replace Unicode ₹ with Rs.)
        val sanitized = str.replace("₹", "Rs. ")
        return sanitized.toByteArray(CP437)
    }
}
