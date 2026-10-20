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
        assertTrue(receipt.contains("Aashirvaad Superior MP"))
        assertTrue(receipt.contains("Wheat Atta 5kg"))
        assertTrue(receipt.contains("2 x Rs. 260.00"))
        assertTrue(receipt.contains("Rs. 520.00"))
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
        assertTrue(receipt.contains("Retail Grocery Pack"))
        assertTrue(receipt.contains("1 x Rs. 350.00"))
        assertTrue(receipt.contains("*** PAID IN FULL ***"))
        assertFalse(receipt.contains("Discount:"))
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
    fun testLogoPlaintextIndicatorWhenLogoPresent() {
        val withLogoData = sampleData.copy(logoUrl = "/uploads/logos/store.png")
        val receipt = ThermalInvoiceFormatter.formatPlainTextReceipt(withLogoData, PaperWidth.WIDTH_58MM)
        assertTrue(receipt.contains("[ COMPANY LOGO ]"))
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
}
