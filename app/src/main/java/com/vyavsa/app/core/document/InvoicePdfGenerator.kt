package com.vyavsa.app.core.document

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.vyavsa.app.core.common.FormatUtils
import com.vyavsa.app.core.printer.InvoicePrintData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.URL
import java.util.Locale

/**
 * Native Android PDF Generator for Vyavsa sales invoices and receipts.
 * Generates standards-compliant A4 PDF documents with company branding,
 * high-resolution logo rendering, tabular line items, financial totals,
 * payment breakdown, and configured invoice footer.
 *
 * Implements strict column alignments, pixel-perfect right-anchored numerical
 * metrics, collision-free totals cards, and automatic page budgeting.
 */
object InvoicePdfGenerator {

    private const val PAGE_WIDTH = 595 // A4 standard width in PostScript points (72 dpi)
    private const val PAGE_HEIGHT = 842 // A4 standard height in PostScript points (72 dpi)
    private const val MARGIN = 36f // 0.5 inch margins

    suspend fun generateInvoicePdf(
        context: Context,
        data: InvoicePrintData
    ): Result<File> = withContext(Dispatchers.IO) {
        val pdfDocument = PdfDocument()

        try {
            // 1. Prepare target cache directory and clean up stale temporary files
            val invoicesDir = File(context.cacheDir, "invoices").apply {
                if (!exists()) mkdirs()
            }
            cleanupOldInvoicePdfs(invoicesDir)

            val safeInvoiceNumber = data.invoiceNumber
                .replace(Regex("[^a-zA-Z0-9_-]"), "_")
                .take(32)
                .ifBlank { "INV_${System.currentTimeMillis()}" }
            val outputFile = File(invoicesDir, "invoice-$safeInvoiceNumber.pdf")

            // 2. Load or resolve logo bitmap if present
            val logoBitmap = resolveLogoBitmap(data)

            // 3. Render Invoice Pages
            renderDocument(pdfDocument, data, logoBitmap)

            // 4. Write to File
            FileOutputStream(outputFile).use { out ->
                pdfDocument.writeTo(out)
            }

            Result.success(outputFile)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            pdfDocument.close()
        }
    }

    private fun renderDocument(
        pdfDocument: PdfDocument,
        data: InvoicePrintData,
        logoBitmap: Bitmap?
    ) {
        // Paints
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42) // Slate 900
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        val textRightPaint = Paint(textPaint).apply { textAlign = Paint.Align.RIGHT }
        val textCenterPaint = Paint(textPaint).apply { textAlign = Paint.Align.CENTER }

        val boldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42)
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val boldRightPaint = Paint(boldPaint).apply { textAlign = Paint.Align.RIGHT }

        val mutedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(100, 116, 139) // Slate 500
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        val mutedCenterPaint = Paint(mutedPaint).apply { textAlign = Paint.Align.CENTER }

        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(226, 232, 240) // Slate 200
            strokeWidth = 1f
            style = Paint.Style.STROKE
        }

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(248, 250, 252) // Slate 50
            style = Paint.Style.FILL
        }

        // Structured Column Coordinates (A4 page: MARGIN=36, width=595, right edge=559)
        // Col 0 (#): 36f to 58f | Col 1 (Desc): 58f to 300f | Col 2 (Qty): 300f to 365f | Col 3 (Price): 365f to 460f | Col 4 (Total): 460f to 559f
        val colNoCenterX = 47f
        val colDescX = 64f
        val colDescMaxWidth = 230f
        val colQtyCenterX = 332.5f
        val colRateRightX = 452f
        val colTotalRightX = PAGE_WIDTH - MARGIN - 8f // 551f
        val dividerXPositions = floatArrayOf(58f, 300f, 365f, 460f)

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        // Draw initial page header
        var curY = drawHeader(canvas, data, logoBitmap, textPaint, mutedPaint, linePaint)

        // Draw Bill-To Card
        curY = drawBillToCard(canvas, data, curY, boldPaint, mutedPaint, linePaint, bgPaint)

        // Draw Table Header with crisp column dividers
        curY = drawTableHeader(canvas, curY, colNoCenterX, colDescX, colQtyCenterX, colRateRightX, colTotalRightX, dividerXPositions, linePaint)

        // Resolve line items to render
        val itemsToRender = if (data.items.isNotEmpty()) {
            data.items
        } else if (!data.singleItemName.isNullOrBlank()) {
            listOf(
                com.vyavsa.app.core.printer.PrintItem(
                    name = data.singleItemName,
                    quantity = "1",
                    unitPrice = data.subtotal.toString(),
                    lineTotal = data.subtotal.toString()
                )
            )
        } else {
            listOf(
                com.vyavsa.app.core.printer.PrintItem(
                    name = "Sale Item",
                    quantity = "1",
                    unitPrice = data.subtotal.toString(),
                    lineTotal = data.subtotal.toString()
                )
            )
        }

        // Draw Line Items
        itemsToRender.forEachIndexed { index, item ->
            val nameLines = wrapText(item.name, textPaint, colDescMaxWidth)
            val effectiveLines = if (nameLines.isEmpty()) listOf(item.name) else nameLines
            val rowHeight = maxOf(20f, effectiveLines.size * 12f + 8f)

            // Multi-page check
            if (curY + rowHeight > PAGE_HEIGHT - MARGIN - 180f) {
                drawFooter(canvas, data, linePaint)
                pdfDocument.finishPage(page)

                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas

                curY = MARGIN + 10f
                canvas.drawText("${data.invoiceTitle} - ${data.invoiceNumber} (Continued)", MARGIN, curY, mutedPaint)
                curY += 12f
                curY = drawTableHeader(canvas, curY, colNoCenterX, colDescX, colQtyCenterX, colRateRightX, colTotalRightX, dividerXPositions, linePaint)
            }

            val rowTopY = curY

            // Draw row cells
            canvas.drawText("${index + 1}", colNoCenterX, curY + 13f, mutedCenterPaint)

            effectiveLines.forEachIndexed { lIdx, lineText ->
                canvas.drawText(lineText, colDescX, curY + 13f + (lIdx * 12f), textPaint)
            }

            canvas.drawText(item.quantity, colQtyCenterX, curY + 13f, textCenterPaint)
            val formattedRate = FormatUtils.formatCurrency(item.unitPrice.toDoubleOrNull() ?: 0.0)
            canvas.drawText(formattedRate, colRateRightX, curY + 13f, textRightPaint)
            val formattedTotal = FormatUtils.formatCurrency(item.lineTotal.toDoubleOrNull() ?: 0.0)
            canvas.drawText(formattedTotal, colTotalRightX, curY + 13f, boldRightPaint)

            curY += rowHeight

            // Draw vertical column dividers for this row
            canvas.drawLine(MARGIN, rowTopY, MARGIN, curY, linePaint)
            dividerXPositions.forEach { x ->
                canvas.drawLine(x, rowTopY, x, curY, linePaint)
            }
            canvas.drawLine(PAGE_WIDTH - MARGIN, rowTopY, PAGE_WIDTH - MARGIN, curY, linePaint)

            // Draw horizontal dividing line at the bottom of the row
            canvas.drawLine(MARGIN, curY, PAGE_WIDTH - MARGIN, curY, linePaint)
        }

        // Draw Financial Totals & Payment Breakdown
        drawTotalsAndNotesSection(canvas, data, curY, textPaint, textRightPaint, boldPaint, boldRightPaint, mutedPaint, linePaint, bgPaint)

        // Draw Footer
        drawFooter(canvas, data, linePaint)

        pdfDocument.finishPage(page)
    }

    private fun drawHeader(
        canvas: Canvas,
        data: InvoicePrintData,
        logoBitmap: Bitmap?,
        textPaint: Paint,
        mutedPaint: Paint,
        linePaint: Paint
    ): Float {
        var curY = MARGIN + 6f
        val logoSize = 46f

        if (logoBitmap != null) {
            val srcRect = Rect(0, 0, logoBitmap.width, logoBitmap.height)
            val destRect = RectF(MARGIN, curY, MARGIN + logoSize, curY + logoSize)
            canvas.drawBitmap(logoBitmap, srcRect, destRect, null)
        } else {
            // Monogram fallback
            val rect = RectF(MARGIN, curY, MARGIN + logoSize, curY + logoSize)
            val monogramBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(238, 242, 255) // Indigo 50
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(rect, 8f, 8f, monogramBg)
            val borderP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(99, 102, 241) // Indigo 500
                style = Paint.Style.STROKE
                strokeWidth = 1.2f
            }
            canvas.drawRoundRect(rect, 8f, 8f, borderP)
            val letterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(79, 70, 229)
                textSize = 18f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            val initial = data.businessName.trim().take(1).uppercase(Locale.US).ifBlank { "V" }
            canvas.drawText(initial, rect.centerX(), rect.centerY() + 6.5f, letterPaint)
        }

        val bizTextX = MARGIN + logoSize + 10f
        val maxBizWidth = 250f
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42)
            textSize = 13.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val nameLines = wrapText(data.businessName, titlePaint, maxBizWidth)
        var subBizY = curY + 13f
        nameLines.forEach { line ->
            canvas.drawText(line, bizTextX, subBizY, titlePaint)
            subBizY += 14f
        }

        if (!data.businessAddress.isNullOrBlank()) {
            val addrLines = wrapText(data.businessAddress, mutedPaint, maxBizWidth)
            addrLines.take(2).forEach { line ->
                canvas.drawText(line, bizTextX, subBizY, mutedPaint)
                subBizY += 10.5f
            }
        }

        val contactLine = listOfNotNull(
            data.businessPhone?.let { "Ph: $it" },
            data.businessEmail?.let { "Email: $it" }
        ).joinToString("  •  ")
        if (contactLine.isNotBlank()) {
            val contactWrapped = wrapText(contactLine, mutedPaint, maxBizWidth)
            contactWrapped.take(1).forEach { line ->
                canvas.drawText(line, bizTextX, subBizY, mutedPaint)
                subBizY += 10.5f
            }
        }

        if (!data.taxId.isNullOrBlank()) {
            canvas.drawText("Tax/GSTIN: ${data.taxId}", bizTextX, subBizY, mutedPaint)
            subBizY += 10.5f
        }

        // Top-Right Header Metadata
        val rightX = PAGE_WIDTH - MARGIN
        val docTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(30, 58, 138) // Blue 900
            textSize = 14.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText(data.invoiceTitle.uppercase(Locale.US), rightX, curY + 14f, docTitlePaint)

        val metaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42)
            textSize = 9.5f
            textAlign = Paint.Align.RIGHT
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        canvas.drawText("Invoice #: ${data.invoiceNumber}", rightX, curY + 28f, metaPaint)
        canvas.drawText("Date: ${data.invoiceDate}", rightX, curY + 41f, metaPaint)

        // Payment Status Badge
        val statusText = if (data.balanceDue <= 0.0) "PAID IN FULL" else "DUE: ${FormatUtils.formatCurrency(data.balanceDue)}"
        val badgeColor = if (data.balanceDue <= 0.0) Color.rgb(5, 150, 105) else Color.rgb(220, 38, 38)
        val badgeBg = if (data.balanceDue <= 0.0) Color.rgb(236, 253, 245) else Color.rgb(254, 242, 242)

        val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = badgeColor
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val badgeWidth = badgePaint.measureText(statusText) + 16f
        val badgeRect = RectF(rightX - badgeWidth, curY + 48f, rightX, curY + 64f)
        val badgeBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = badgeBg
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(badgeRect, 4f, 4f, badgeBgPaint)
        val badgeBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = badgeColor
            style = Paint.Style.STROKE
            strokeWidth = 0.8f
        }
        canvas.drawRoundRect(badgeRect, 4f, 4f, badgeBorderPaint)
        val fontMetrics = badgePaint.fontMetrics
        val badgeTextY = badgeRect.centerY() - (fontMetrics.descent + fontMetrics.ascent) / 2f
        canvas.drawText(statusText, badgeRect.centerX(), badgeTextY, badgePaint)

        val headerBottomY = maxOf(subBizY + 8f, curY + 70f)
        canvas.drawLine(MARGIN, headerBottomY, PAGE_WIDTH - MARGIN, headerBottomY, linePaint)
        return headerBottomY + 8f
    }

    private fun drawBillToCard(
        canvas: Canvas,
        data: InvoicePrintData,
        curY: Float,
        boldPaint: Paint,
        mutedPaint: Paint,
        linePaint: Paint,
        bgPaint: Paint
    ): Float {
        val cardHeight = 46f
        val billToRect = RectF(MARGIN, curY, PAGE_WIDTH - MARGIN, curY + cardHeight)
        canvas.drawRoundRect(billToRect, 5f, 5f, bgPaint)
        canvas.drawRoundRect(billToRect, 5f, 5f, linePaint)

        val billToHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(100, 116, 139)
            textSize = 8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("BILLED TO", MARGIN + 10f, curY + 13f, billToHeaderPaint)

        val customerName = data.customerName?.ifBlank { "Counter Customer (Walk-in)" } ?: "Customer"
        canvas.drawText(customerName, MARGIN + 10f, curY + 26f, boldPaint)

        val custDetails = listOfNotNull(
            data.customerPhone?.let { "Phone: $it" },
            data.route?.let { "Route: $it" },
            data.salesman?.let { "Salesman: $it" }
        ).joinToString("   •   ")
        if (custDetails.isNotBlank()) {
            canvas.drawText(custDetails, MARGIN + 10f, curY + 38f, mutedPaint)
        }

        return curY + cardHeight + 10f
    }

    private fun drawTableHeader(
        canvas: Canvas,
        curY: Float,
        colNoCenterX: Float,
        colDescX: Float,
        colQtyCenterX: Float,
        colRateRightX: Float,
        colTotalRightX: Float,
        dividerXPositions: FloatArray,
        linePaint: Paint
    ): Float {
        val headerHeight = 20f
        val thRect = RectF(MARGIN, curY, PAGE_WIDTH - MARGIN, curY + headerHeight)
        val thBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(241, 245, 249) // Slate 100
            style = Paint.Style.FILL
        }
        canvas.drawRect(thRect, thBgPaint)

        // Draw header border box
        canvas.drawRect(thRect, linePaint)

        // Draw column divider lines in header
        dividerXPositions.forEach { x ->
            canvas.drawLine(x, curY, x, curY + headerHeight, linePaint)
        }

        val thPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(71, 85, 105)
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val thCenterPaint = Paint(thPaint).apply { textAlign = Paint.Align.CENTER }
        val thRightPaint = Paint(thPaint).apply { textAlign = Paint.Align.RIGHT }

        val textY = curY + 13.5f
        canvas.drawText("#", colNoCenterX, textY, thCenterPaint)
        canvas.drawText("ITEM DESCRIPTION", colDescX, textY, thPaint)
        canvas.drawText("QTY", colQtyCenterX, textY, thCenterPaint)
        canvas.drawText("UNIT PRICE", colRateRightX, textY, thRightPaint)
        canvas.drawText("TOTAL", colTotalRightX, textY, thRightPaint)

        return curY + headerHeight
    }

    private fun drawTotalsAndNotesSection(
        canvas: Canvas,
        data: InvoicePrintData,
        curY: Float,
        textPaint: Paint,
        textRightPaint: Paint,
        boldPaint: Paint,
        boldRightPaint: Paint,
        mutedPaint: Paint,
        linePaint: Paint,
        bgPaint: Paint
    ) {
        val startY = curY + 10f

        // Right-Side Totals Box (width = 230pt)
        val totalsBoxRight = PAGE_WIDTH - MARGIN
        val totalsBoxLeft = totalsBoxRight - 230f
        val totalsLabelX = totalsBoxLeft + 8f
        val totalsValX = totalsBoxRight - 8f

        var tY = startY

        // Subtotal
        canvas.drawText("Subtotal:", totalsLabelX, tY + 10f, textPaint)
        canvas.drawText(FormatUtils.formatCurrency(data.subtotal), totalsValX, tY + 10f, textRightPaint)
        tY += 15f

        // Discount
        if (data.discount > 0.0) {
            val greenPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(5, 150, 105)
                textSize = 9.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val greenRight = Paint(greenPaint).apply { textAlign = Paint.Align.RIGHT }
            canvas.drawText("Discount:", totalsLabelX, tY + 10f, greenPaint)
            canvas.drawText("-${FormatUtils.formatCurrency(data.discount)}", totalsValX, tY + 10f, greenRight)
            tY += 15f
        }

        // Total Payable Card Highlight
        val totalHighlight = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(238, 242, 255) // Indigo 50
            style = Paint.Style.FILL
        }
        val totalBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(199, 210, 254) // Indigo 200
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        val totalRowRect = RectF(totalsBoxLeft, tY, totalsBoxRight, tY + 22f)
        canvas.drawRoundRect(totalRowRect, 4f, 4f, totalHighlight)
        canvas.drawRoundRect(totalRowRect, 4f, 4f, totalBorder)

        val totalGrandLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(30, 58, 138)
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val totalGrandValPaint = Paint(totalGrandLabelPaint).apply { textAlign = Paint.Align.RIGHT }

        canvas.drawText("TOTAL PAYABLE:", totalsLabelX, tY + 14.5f, totalGrandLabelPaint)
        canvas.drawText(FormatUtils.formatCurrency(data.totalAmount), totalsValX, tY + 14.5f, totalGrandValPaint)
        tY += 27f

        // Amount Paid
        canvas.drawText("Amount Paid:", totalsLabelX, tY + 10f, textPaint)
        canvas.drawText(FormatUtils.formatCurrency(data.amountPaid), totalsValX, tY + 10f, textRightPaint)
        tY += 15f

        // Balance Due / Paid in Full
        if (data.balanceDue > 0.0) {
            val duePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(220, 38, 38)
                textSize = 9.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val dueRight = Paint(duePaint).apply { textAlign = Paint.Align.RIGHT }
            canvas.drawText("Balance Due:", totalsLabelX, tY + 10f, duePaint)
            canvas.drawText(FormatUtils.formatCurrency(data.balanceDue), totalsValX, tY + 10f, dueRight)
        } else {
            val paidFullPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(5, 150, 105)
                textSize = 9.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.RIGHT
            }
            canvas.drawText("Status:", totalsLabelX, tY + 10f, mutedPaint)
            canvas.drawText("✓ PAID IN FULL", totalsValX, tY + 10f, paidFullPaint)
        }
        tY += 16f

        // Left-Side Payment & Notes Box
        val notesBoxLeft = MARGIN
        val notesBoxRight = totalsBoxLeft - 14f
        val notesHeight = maxOf(tY - startY, 60f)
        val notesRect = RectF(notesBoxLeft, startY, notesBoxRight, startY + notesHeight)
        canvas.drawRoundRect(notesRect, 5f, 5f, bgPaint)
        canvas.drawRoundRect(notesRect, 5f, 5f, linePaint)

        val notesHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(100, 116, 139)
            textSize = 8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("PAYMENT & TRANSACTION DETAILS", notesBoxLeft + 10f, startY + 13f, notesHeaderPaint)

        var nY = startY + 26f
        val maxNotesWidth = notesBoxRight - notesBoxLeft - 20f

        if (data.paymentSummary.isNotBlank()) {
            val payLines = wrapText("Payment: ${data.paymentSummary}", mutedPaint, maxNotesWidth)
            payLines.take(2).forEach { line ->
                canvas.drawText(line, notesBoxLeft + 10f, nY, mutedPaint)
                nY += 11f
            }
        }

        if (!data.note.isNullOrBlank()) {
            val noteLines = wrapText("Note: ${data.note}", mutedPaint, maxNotesWidth)
            noteLines.take(2).forEach { line ->
                canvas.drawText(line, notesBoxLeft + 10f, nY, mutedPaint)
                nY += 11f
            }
        }
    }

    private fun drawFooter(canvas: Canvas, data: InvoicePrintData, linePaint: Paint) {
        val footerY = PAGE_HEIGHT - MARGIN - 42f
        canvas.drawLine(MARGIN, footerY, PAGE_WIDTH - MARGIN, footerY, linePaint)

        val footerHeadingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42)
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(data.effectiveFooterHeading, PAGE_WIDTH / 2f, footerY + 14f, footerHeadingPaint)

        val footerParaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(100, 116, 139)
            textSize = 8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        val wrappedFooter = wrapText(data.effectiveFooterParagraph, footerParaPaint, 480f)
        wrappedFooter.take(2).forEachIndexed { idx, line ->
            canvas.drawText(line, PAGE_WIDTH / 2f, footerY + 25f + (idx * 10f), footerParaPaint)
        }
    }

    private suspend fun resolveLogoBitmap(data: InvoicePrintData): Bitmap? = withContext(Dispatchers.IO) {
        if (data.logoBitmap != null) return@withContext data.logoBitmap
        val resolvedUrl = FormatUtils.resolveMediaUrl(data.logoUrl) ?: return@withContext null
        try {
            val conn = URL(resolvedUrl).openConnection()
            conn.connectTimeout = 4000
            conn.readTimeout = 6000
            conn.getInputStream().use { stream: InputStream ->
                BitmapFactory.decodeStream(stream)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        if (text.isBlank()) return emptyList()
        val words = text.split(Regex("\\s+"))
        val lines = mutableListOf<String>()
        var currentLine = StringBuilder()

        for (word in words) {
            val candidate = if (currentLine.isEmpty()) word else "${currentLine} $word"
            if (paint.measureText(candidate) <= maxWidth) {
                currentLine = StringBuilder(candidate)
            } else {
                if (currentLine.isNotEmpty()) {
                    lines.add(currentLine.toString())
                    currentLine = StringBuilder()
                }
                // Handle single word longer than maxWidth
                if (paint.measureText(word) > maxWidth) {
                    var remaining = word
                    while (remaining.isNotEmpty()) {
                        var count = remaining.length
                        while (count > 0 && paint.measureText(remaining.substring(0, count)) > maxWidth) {
                            count--
                        }
                        if (count == 0) count = 1
                        lines.add(remaining.substring(0, count))
                        remaining = remaining.substring(count)
                    }
                } else {
                    currentLine = StringBuilder(word)
                }
            }
        }
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine.toString())
        }
        return lines
    }

    private fun cleanupOldInvoicePdfs(invoicesDir: File) {
        try {
            val files = invoicesDir.listFiles() ?: return
            val oneDayAgo = System.currentTimeMillis() - (24 * 60 * 60 * 1000)
            files.forEach { file ->
                if (file.lastModified() < oneDayAgo || files.size > 25) {
                    file.delete()
                }
            }
        } catch (_: Exception) {}
    }
}
