package com.vyavsa.app.core.printer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThermalInvoiceFormatterTest {

    private val sampleData = InvoicePrintData(
        businessName = "Vyavsa Retailers Pvt Ltd",
        businessAddress = "Shop 12, Market Complex, MG Road, Bangalore 560001",
        businessPhone = "9876543210",
        businessEmail = "support@vyavsa.com",
        taxId = "29AAAAA0000A1Z5",
        invoiceTitle = "Tax Invoice",
        invoiceNumber = "INV-2026-0089",
        invoiceDate = "20 Oct 2026, 11:30 AM",
        customerName = "Sharma General Stores",
        customerPhone = "9123456780",
        route = "North Route",
        salesman = "Ramesh Kumar",
        items = listOf(
            PrintItem(
                name = "Aashirvaad Superior MP Whole Wheat Atta 5kg",
                quantity = "2",
                unitPrice = "260.00",
                lineTotal = "520.00"
            ),
            PrintItem(
                name = "Fortune Sunlite Refined Sunflower Oil 1L",
                quantity = "3",
                unitPrice = "140.00",
                lineTotal = "420.00"
            )
        ),
        subtotal = 940.00,
        discount = 40.00,
        totalAmount = 900.00,
        amountPaid = 700.00,
        balanceDue = 200.00,
        paymentSummary = "Cash: Rs. 500.00 | Bank: Rs. 200.00",
        note = "Deliver before 5 PM"
    )

    @Test
    fun testWrapTextStandard() {
        val text = "Aashirvaad Superior MP Whole Wheat Atta 5kg"
        val lines = ThermalInvoiceFormatter.wrapText(text, 20)
        assertTrue(lines.size > 1)
        lines.forEach { line ->
            assertTrue(line.length <= 20)
        }
    }

    @Test
    fun testWrapTextWithLongWordExceedingWidth() {
        val text = "Supercalifragilisticexpialidocious Item"
        val lines = ThermalInvoiceFormatter.wrapText(text, 15)
        assertTrue(lines.size >= 3)
        lines.forEach { line ->
            assertTrue(line.length <= 15)
        }
    }

    @Test
    fun testFormatKeyValueAlignment() {
        val result = ThermalInvoiceFormatter.formatKeyValue("Subtotal:", "Rs. 940.00", 32)
        assertEquals(32, result.length)
        assertTrue(result.startsWith("Subtotal:"))
        assertTrue(result.endsWith("Rs. 940.00"))
    }

    @Test
    fun testCenterText() {
        val centered = ThermalInvoiceFormatter.centerText("VYAVSA", 32)
        assertEquals(32, centered.length)
        assertTrue(centered.startsWith(" "))
        assertTrue(centered.endsWith(" "))
        assertTrue(centered.contains("VYAVSA"))
    }

    @Test
    fun testFormatPlainTextReceipt58mm() {
        val receipt = ThermalInvoiceFormatter.formatPlainTextReceipt(sampleData, PaperWidth.WIDTH_58MM)

        // Verify key headers and values appear
        assertTrue(receipt.contains("VYAVSA RETAILERS PVT LTD"))
        assertTrue(receipt.contains("TAX INVOICE"))
        assertTrue(receipt.contains("INV-2026-0089"))
        assertTrue(receipt.contains("ITEM"))
        assertTrue(receipt.contains("QTY"))
        assertTrue(receipt.contains("PRICE"))
        assertTrue(receipt.contains("TOTAL"))
        assertTrue(receipt.contains("Aashirvaad"))
        assertTrue(receipt.contains("520.00"))
        assertTrue(receipt.contains("Subtotal:"))
        assertTrue(receipt.contains("Rs. 940.00"))
        assertTrue(receipt.contains("Discount:"))
        assertTrue(receipt.contains("-Rs. 40.00"))
        assertTrue(receipt.contains("TOTAL AMOUNT:"))
        assertTrue(receipt.contains("Rs. 900.00"))
        assertTrue(receipt.contains("Amount Paid:"))
        assertTrue(receipt.contains("Rs. 700.00"))
        assertTrue(receipt.contains("BALANCE DUE:"))
        assertTrue(receipt.contains("Rs. 200.00"))
        assertTrue(receipt.contains("Deliver before 5 PM"))
        assertTrue(receipt.contains("Thank you for your business!"))

        // Check line width consistency (no line exceeds 32 chars)
        receipt.lines().forEach { line ->
            assertTrue("Line exceeds 32 chars: '$line' (${line.length})", line.length <= 32)
        }
    }

    @Test
    fun testFormatPlainTextReceipt80mm() {
        val receipt = ThermalInvoiceFormatter.formatPlainTextReceipt(sampleData, PaperWidth.WIDTH_80MM)

        assertTrue(receipt.contains("VYAVSA RETAILERS PVT LTD"))
        assertTrue(receipt.contains("TAX INVOICE"))
        assertTrue(receipt.contains("ITEM"))
        assertTrue(receipt.contains("QTY"))
        assertTrue(receipt.contains("PRICE"))
        assertTrue(receipt.contains("TOTAL"))
        assertTrue(receipt.contains("Rs. 900.00"))
        assertTrue(receipt.contains("BALANCE DUE:"))
        assertTrue(receipt.contains("Rs. 200.00"))

        // Check line width consistency for 80mm (no line exceeds 48 chars)
        receipt.lines().forEach { line ->
            assertTrue("Line exceeds 48 chars: '$line' (${line.length})", line.length <= 48)
        }
    }

    @Test
    fun testPaidInFullStatusWhenBalanceIsZero() {
        val paidData = sampleData.copy(
            amountPaid = 900.00,
            balanceDue = 0.00
        )
        val receipt = ThermalInvoiceFormatter.formatPlainTextReceipt(paidData, PaperWidth.WIDTH_58MM)
        assertTrue(receipt.contains("*** PAID IN FULL ***"))
        assertFalse(receipt.contains("BALANCE DUE:"))
    }

    @Test
    fun testSingleItemCounterSale() {
        val counterData = InvoicePrintData(
            businessName = "Vyavsa Counter Register",
            invoiceTitle = "Counter POS Receipt",
            invoiceNumber = "POS-9981",
            invoiceDate = "20 Oct 2026, 12:00 PM",
            singleItemName = "Retail Grocery Pack",
            subtotal = 350.00,
            discount = 0.00,
            totalAmount = 350.00,
            amountPaid = 350.00,
            balanceDue = 0.00,
            paymentSummary = "Cash: Rs. 350.00"
        )

        val receipt = ThermalInvoiceFormatter.formatPlainTextReceipt(counterData, PaperWidth.WIDTH_58MM)
        assertTrue(receipt.contains("Retail"))
        assertTrue(receipt.contains("350.00"))
        assertTrue(receipt.contains("*** PAID IN FULL ***"))
        assertFalse(receipt.contains("Discount:"))
    }

    @Test
    fun testSixDigitAmountsOnSingleLine58mmAnd80mm() {
        val largeData = sampleData.copy(
            items = listOf(
                PrintItem(
                    name = "Industrial Heavy Duty Diesel Generator 50kVA",
                    quantity = "1",
                    unitPrice = "450000.00",
                    lineTotal = "450000.00"
                )
            ),
            subtotal = 450000.00,
            discount = 0.00,
            totalAmount = 450000.00,
            amountPaid = 450000.00,
            balanceDue = 0.00
        )

        val receipt58 = ThermalInvoiceFormatter.formatPlainTextReceipt(largeData, PaperWidth.WIDTH_58MM)
        receipt58.lines().forEach { line ->
            assertTrue("58mm Line exceeds 32 chars: '$line' (${line.length})", line.length <= 32)
        }
        assertTrue(receipt58.contains("450,000.00"))

        val receipt80 = ThermalInvoiceFormatter.formatPlainTextReceipt(largeData, PaperWidth.WIDTH_80MM)
        receipt80.lines().forEach { line ->
            assertTrue("80mm Line exceeds 48 chars: '$line' (${line.length})", line.length <= 48)
        }
        assertTrue(receipt80.contains("450,000.00"))
    }

    @Test
    fun testEscPosByteSequenceGeneration() {
        val bytes = ThermalInvoiceFormatter.formatEscPosReceipt(sampleData, PaperWidth.WIDTH_58MM)

        assertTrue(bytes.isNotEmpty())
        // Should start with ESC @ (0x1B, 0x40)
        assertEquals(0x1B.toByte(), bytes[0])
        assertEquals(0x40.toByte(), bytes[1])

        // Should contain partial cut command near the end: 0x1D, 0x56, 0x42, 0x00
        val lastBytes = bytes.takeLast(4)
        assertEquals(0x1D.toByte(), lastBytes[0])
        assertEquals(0x56.toByte(), lastBytes[1])
        assertEquals(0x42.toByte(), lastBytes[2])
        assertEquals(0x00.toByte(), lastBytes[3])
    }

    @Test
    fun testLogoPlaintextNoPlaceholderAndCustomFooter() {
        val withLogoData = sampleData.copy(
            logoUrl = "/uploads/logos/store.png",
            footerHeading = "Custom Business Thanks!",
            footerParagraph = "Goods once sold can be returned within 7 days."
        )
        assertTrue(withLogoData.hasLogo)
        val receipt = ThermalInvoiceFormatter.formatPlainTextReceipt(withLogoData, PaperWidth.WIDTH_58MM)
        // Ensure bracketed placeholder text is never rendered in output
        assertFalse(receipt.contains("[ COMPANY LOGO ]"))
        // Verify custom footer is correctly rendered
        assertTrue(receipt.contains("Custom Business Thanks!"))
        assertTrue(receipt.contains("Goods once sold can be returned"))
    }

    @Test
    fun testEscPosRasterBitImageGeneration() {
        // Create an 8x4 test raster with top-left black pixel
        val testRaster = object : RasterImage {
            override val width: Int = 8
            override val height: Int = 4
            override fun getPixel(x: Int, y: Int): Int {
                // Black pixel at (0, 0), white elsewhere (ARGB format)
                return if (x == 0 && y == 0) -0x1000000 else -0x1
            }
        }

        val rasterBytes = EscPosCommands.generateRasterBitImage(testRaster, 8)
        assertTrue(rasterBytes.isNotEmpty())

        // Must start with ALIGN_CENTER (0x1B, 0x61, 0x01)
        assertEquals(0x1B.toByte(), rasterBytes[0])
        assertEquals(0x61.toByte(), rasterBytes[1])
        assertEquals(0x01.toByte(), rasterBytes[2])

        // Must have GS v 0 m xL xH yL yH (0x1D, 0x76, 0x30, 0x00, 0x01, 0x00, 0x04, 0x00)
        assertEquals(0x1D.toByte(), rasterBytes[3])
        assertEquals(0x76.toByte(), rasterBytes[4])
        assertEquals(0x30.toByte(), rasterBytes[5])
        assertEquals(0x00.toByte(), rasterBytes[6])
        assertEquals(0x01.toByte(), rasterBytes[7]) // 1 byte per row (8 dots)
        assertEquals(0x00.toByte(), rasterBytes[8])
        assertEquals(0x04.toByte(), rasterBytes[9]) // 4 dots high
        assertEquals(0x00.toByte(), rasterBytes[10])

        // First data byte (row 0): bit 7 set for black pixel at (0, 0) -> 0x80 (128)
        assertEquals(0x80.toByte(), rasterBytes[11])
    }

    @Test
    fun testFormatThermalReceiptWithCustomerAdvanceAndBalanceBreakdown() {
        val invoiceData = sampleData.copy(
            subtotal = 1000.00,
            discount = 0.00,
            totalAmount = 1000.00,
            amountPaid = 1000.00,
            amountReceivedDirect = 700.00,
            advanceApplied = 300.00,
            totalSettledForSale = 1000.00,
            balanceDue = 0.00,
            previousBalance = 500.00,
            previousDuesSettled = 0.00,
            advanceGenerated = 0.00,
            updatedCustomerBalance = 500.00,
            hasCustomerLedger = true
        )

        val receipt = ThermalInvoiceFormatter.formatPlainTextReceipt(invoiceData, PaperWidth.WIDTH_58MM)

        assertTrue(receipt.contains("Prev Customer Due:"))
        assertTrue(receipt.contains("Rs. 500.00"))
        assertTrue(receipt.contains("TOTAL AMOUNT:"))
        assertTrue(receipt.contains("Rs. 1,000.00"))
        assertTrue(receipt.contains("Amount Paid:"))
        assertTrue(receipt.contains("Rs. 700.00"))
        assertTrue(receipt.contains("Paid via Advance:"))
        assertTrue(receipt.contains("Rs. 300.00"))
        assertTrue(receipt.contains("Total Settled:"))
        assertTrue(receipt.contains("Rs. 1,000.00"))
        assertTrue(receipt.contains("Updated Customer Due:"))
        assertTrue(receipt.contains("Rs. 500.00"))

        val escPosBytes = ThermalInvoiceFormatter.formatEscPosReceipt(invoiceData, PaperWidth.WIDTH_58MM)
        val escPosText = String(escPosBytes, Charsets.US_ASCII)
        assertTrue(escPosText.contains("Prev Customer Due:"))
        assertTrue(escPosText.contains("Paid via Advance:"))
        assertTrue(escPosText.contains("Updated Customer Due:"))
    }

    @Test
    fun testFormatThermalReceiptWithFullContraAdjustment58mmAnd80mm() {
        val invoiceData = sampleData.copy(
            totalAmount = 20000.00,
            amountPaid = 0.00,
            amountReceivedDirect = 0.00,
            contraAmount = 20000.00,
            previousPayable = 50000.00,
            remainingPayable = 30000.00,
            balanceDue = 0.00,
            totalSettledForSale = 20000.00
        )

        // 58mm verification
        val receipt58 = ThermalInvoiceFormatter.formatPlainTextReceipt(invoiceData, PaperWidth.WIDTH_58MM)
        assertTrue(receipt58.contains("Prev Supplier Payable:"))
        assertTrue(receipt58.contains("Rs. 50,000.00"))
        assertTrue(receipt58.contains("Adjusted Against Supplier"))
        assertTrue(receipt58.contains("-Rs. 20,000.00"))
        assertTrue(receipt58.contains("Remaining Supplier"))
        assertTrue(receipt58.contains("Rs. 30,000.00"))
        assertFalse(receipt58.contains("Payment Received: Rs. 20,000.00")) // Must not confuse contra with cash/bank payment

        // 80mm verification
        val receipt80 = ThermalInvoiceFormatter.formatPlainTextReceipt(invoiceData, PaperWidth.WIDTH_80MM)
        assertTrue(receipt80.contains("Prev Supplier Payable:"))
        assertTrue(receipt80.contains("Adjusted Against Supplier Payable:"))
        assertTrue(receipt80.contains("Remaining Supplier Payable:"))

        // EscPos output verification
        val escPos58 = ThermalInvoiceFormatter.formatEscPosReceipt(invoiceData, PaperWidth.WIDTH_58MM)
        val escPosText58 = String(escPos58, Charsets.US_ASCII)
        assertTrue(escPosText58.contains("Adjusted Against Supplier"))
        assertTrue(escPosText58.contains("-Rs. 20,000.00"))
    }

    @Test
    fun testFormatThermalReceiptWithPartialContraAdjustmentAndDirectPayment() {
        // Sale ₹25,000 | Cash Paid ₹5,000 | Contra ₹15,000 | Remaining Sale Due ₹5,000
        val invoiceData = sampleData.copy(
            totalAmount = 25000.00,
            amountPaid = 5000.00,
            amountReceivedDirect = 5000.00,
            contraAmount = 15000.00,
            previousPayable = 15000.00,
            remainingPayable = 0.00,
            balanceDue = 5000.00,
            totalSettledForSale = 20000.00
        )

        val receipt = ThermalInvoiceFormatter.formatPlainTextReceipt(invoiceData, PaperWidth.WIDTH_58MM)
        assertTrue(receipt.contains("Actual Payment Received:"))
        assertTrue(receipt.contains("Rs. 5,000.00"))
        assertTrue(receipt.contains("Adjusted Against Supplier"))
        assertTrue(receipt.contains("-Rs. 15,000.00"))
        assertTrue(receipt.contains("BALANCE DUE:"))
        assertTrue(receipt.contains("Rs. 5,000.00"))
        assertTrue(receipt.contains("Remaining Supplier"))
        assertTrue(receipt.contains("Rs. 0.00"))
    }

    @Test
    fun testFormatThermalReceiptHistoricalWithoutContraAdjustment() {
        val invoiceData = sampleData.copy(
            totalAmount = 1000.00,
            amountPaid = 1000.00,
            contraAmount = 0.0,
            previousPayable = 0.0,
            remainingPayable = 0.0
        )

        val receipt = ThermalInvoiceFormatter.formatPlainTextReceipt(invoiceData, PaperWidth.WIDTH_58MM)
        assertFalse(receipt.contains("Adjusted Against Supplier"))
        assertFalse(receipt.contains("Prev Supplier Payable:"))
        assertFalse(receipt.contains("Remaining Supplier"))
    }

    @Test
    fun testFormatThermalReceiptWithLongPartyAndLargeMonetaryValues() {
        val invoiceData = sampleData.copy(
            customerName = "Sri Venkateshwara Agro Industrial Feed & Fertilizer Corporation Limited",
            totalAmount = 1500000.00,
            contraAmount = 750000.00,
            previousPayable = 1200000.00,
            remainingPayable = 450000.00,
            amountReceivedDirect = 250000.00,
            balanceDue = 500000.00,
            totalSettledForSale = 1000000.00
        )

        val receipt58 = ThermalInvoiceFormatter.formatPlainTextReceipt(invoiceData, PaperWidth.WIDTH_58MM)
        assertTrue(receipt58.contains("Sri Venkateshwara"))
        assertTrue(receipt58.contains("Adjusted Against Supplier"))
        assertTrue(receipt58.contains("-Rs. 750,000.00"))
        assertTrue(receipt58.contains("Remaining Supplier"))
        assertTrue(receipt58.contains("Rs. 450,000.00"))

        // Ensure each line in 58mm doesn't overflow 32 columns
        receipt58.lines().forEach { line ->
            // EscPos commands or formatting might add non-printing chars, plain text should fit neatly
            assertTrue("Line exceeds 32 chars: '$line'", line.length <= 32)
        }
    }
}
