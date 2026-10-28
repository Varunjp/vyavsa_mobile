package com.vyavsa.app.core.printer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ThermalInvoiceFormatterThemesTest {

    private val sampleData = InvoicePrintData(
        businessName = "Vyavsa Enterprises",
        businessAddress = "123 Business Park, Outer Ring Road, Bangalore 560103",
        businessPhone = "9988776655",
        businessEmail = "billing@vyavsa.in",
        taxId = "29ABCDE1234F1Z5",
        invoiceTitle = "Tax Invoice",
        invoiceNumber = "INV-2026-9999",
        invoiceDate = "21 Oct 2026, 10:00 AM",
        customerName = "Sri Krishna Traders",
        customerPhone = "9845012345",
        route = "South Route",
        salesman = "Suresh",
        items = listOf(
            PrintItem(
                name = "Basmati Rice 5kg",
                quantity = "4",
                unitPrice = "450.00",
                lineTotal = "1800.00"
            ),
            PrintItem(
                name = "Toor Dal 1kg",
                quantity = "5",
                unitPrice = "160.00",
                lineTotal = "800.00"
            )
        ),
        subtotal = 2600.00,
        discount = 100.00,
        totalAmount = 2500.00,
        amountPaid = 1500.00,
        balanceDue = 1000.00,
        paymentSummary = "Cash: Rs. 1500.00",
        note = "Payment terms: net 7 days"
    )

    @Test
    fun testClassicThemeReceipt58mm() {
        val receipt = ThermalInvoiceFormatter.formatPlainTextReceipt(
            data = sampleData,
            paperWidth = PaperWidth.WIDTH_58MM,
            theme = InvoiceTheme.CLASSIC
        )

        assertTrue(receipt.contains("VYAVSA ENTERPRISES"))
        assertTrue(receipt.contains("TAX INVOICE"))
        assertTrue(receipt.contains("INV-2026-9999"))
        assertTrue(receipt.contains("Basmati"))
        assertTrue(receipt.contains("Rice 5kg"))
        assertTrue(receipt.contains("TOTAL AMOUNT:"))
        assertTrue(receipt.contains("Rs. 2,500.00"))
        assertTrue(receipt.contains("BALANCE DUE:"))
        assertTrue(receipt.contains("Rs. 1,000.00"))
        assertTrue(receipt.contains("Thank you for your business!"))

        receipt.lines().forEach { line ->
            assertTrue("Line exceeds 32 chars: '$line' (${line.length})", line.length <= 32)
        }
    }

    @Test
    fun testModernMinimalThemeReceipt80mm() {
        val receipt = ThermalInvoiceFormatter.formatPlainTextReceipt(
            data = sampleData,
            paperWidth = PaperWidth.WIDTH_80MM,
            theme = InvoiceTheme.MODERN_MINIMAL
        )

        assertTrue(receipt.contains("VYAVSA ENTERPRISES"))
        assertTrue(receipt.contains("INV: INV-2026-9999"))
        assertTrue(receipt.contains("CLIENT: Sri Krishna Traders"))
        assertTrue(receipt.contains("Basmati Rice 5kg"))
        assertTrue(receipt.contains("TOTAL DUE"))
        assertTrue(receipt.contains("Rs. 2,500.00"))
        assertTrue(receipt.contains("Balance"))
        assertTrue(receipt.contains("Rs. 1,000.00"))

        receipt.lines().forEach { line ->
            assertTrue("Line exceeds 48 chars: '$line' (${line.length})", line.length <= 48)
        }
    }

    @Test
    fun testDetailedGstThemeReceiptA4Width() {
        val receipt = ThermalInvoiceFormatter.formatPlainTextReceipt(
            data = sampleData,
            paperWidth = PaperWidth.WIDTH_A4,
            theme = InvoiceTheme.DETAILED_GST
        )

        assertTrue(receipt.contains("TAX INVOICE"))
        assertTrue(receipt.contains("GSTIN / TAX ID:"))
        assertTrue(receipt.contains("29ABCDE1234F1Z5"))
        assertTrue(receipt.contains("Sri Krishna Traders"))
        assertTrue(receipt.contains("Authorized Signatory"))
        assertTrue(receipt.contains("Subject to local jurisdiction."))

        receipt.lines().forEach { line ->
            assertTrue("Line exceeds 72 chars: '$line' (${line.length})", line.length <= 72)
        }
    }

    @Test
    fun testContentSettingsFilterElements() {
        val filteredSettings = InvoiceContentSettings(
            showBusinessAddress = false,
            showBusinessPhone = false,
            showBusinessEmail = false,
            showTaxId = false,
            showCustomerDetails = false,
            showNotes = false,
            showPaymentInfo = false
        )

        val receipt = ThermalInvoiceFormatter.formatPlainTextReceipt(
            data = sampleData,
            paperWidth = PaperWidth.WIDTH_58MM,
            theme = InvoiceTheme.CLASSIC,
            contentSettings = filteredSettings
        )

        assertFalse(receipt.contains("123 Business Park"))
        assertFalse(receipt.contains("9988776655"))
        assertFalse(receipt.contains("billing@vyavsa.in"))
        assertFalse(receipt.contains("GSTIN/TAX:"))
        assertFalse(receipt.contains("Customer: Sri Krishna Traders"))
        assertFalse(receipt.contains("Note: Payment terms: net 7 days"))
        assertFalse(receipt.contains("Payment Mode:"))

        // Financial items and totals MUST remain present
        assertTrue(receipt.contains("TOTAL AMOUNT:"))
        assertTrue(receipt.contains("Rs. 2,500.00"))
    }

    @Test
    fun testGenerateEscPosBytesForAllThemes() {
        for (theme in InvoiceTheme.values()) {
            for (paperWidth in PaperWidth.values()) {
                val bytes = ThermalInvoiceFormatter.formatEscPosReceipt(
                    data = sampleData,
                    paperWidth = paperWidth,
                    theme = theme,
                    typography = PrintTypography.STANDARD
                )

                assertNotNull(bytes)
                assertTrue("Bytes should not be empty for $theme on $paperWidth", bytes.isNotEmpty())

                // Verify ESC @ (initialize) command starts the sequence
                assertEquals(0x1B.toByte(), bytes[0])
                assertEquals(0x40.toByte(), bytes[1])

                // Verify cut command is present near the end (0x1D 0x56)
                var hasCutCommand = false
                for (i in 0 until bytes.size - 1) {
                    if (bytes[i] == 0x1D.toByte() && bytes[i + 1] == 0x56.toByte()) {
                        hasCutCommand = true
                        break
                    }
                }
                assertTrue("Receipt ESC/POS bytes should contain paper cut command for $theme", hasCutCommand)
            }
        }
    }

    @Test
    fun testTypographyFontSelectionInEscPos() {
        val compactBytes = ThermalInvoiceFormatter.formatEscPosReceipt(
            data = sampleData,
            paperWidth = PaperWidth.WIDTH_58MM,
            typography = PrintTypography.COMPACT
        )
        // Verify ESC M 1 (Font B) is emitted
        var hasFontB = false
        for (i in 0 until compactBytes.size - 2) {
            if (compactBytes[i] == 0x1B.toByte() && compactBytes[i + 1] == 0x4D.toByte() && compactBytes[i + 2] == 0x01.toByte()) {
                hasFontB = true
                break
            }
        }
        assertTrue("Compact typography should emit ESC M 1 for smaller font", hasFontB)
    }
}
