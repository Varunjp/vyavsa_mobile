package com.vyavsa.app.core.printer

import android.graphics.Bitmap
import com.vyavsa.app.core.common.FormatUtils
import com.vyavsa.app.core.model.CounterSale
import com.vyavsa.app.core.model.LineSale
import com.vyavsa.app.core.model.TenantProfile
import java.util.Locale

/**
 * Authoritative mapper that transforms LineSale and CounterSale domain entities
 * into InvoicePrintData for thermal printing, live previews, and PDF document generation.
 */
object InvoiceDataMapper {

    fun fromLineSale(
        sale: LineSale,
        profile: TenantProfile?,
        logoBitmap: Bitmap? = null
    ): InvoicePrintData {
        val busName = profile?.name?.ifBlank { "Vyavsa Business" } ?: "Vyavsa Business"
        val busAddr = profile?.getEffectiveAddress()
        val busPhone = profile?.phone
        val busEmail = profile?.email

        val disc = sale.discount.toDoubleOrNull() ?: 0.0
        val sub = sale.subtotal.toDoubleOrNull() ?: (sale.totalAmount.toDoubleOrNull() ?: 0.0)
        val effectiveSub = if (sub > 0.0) sub else (sale.totalAmount.toDoubleOrNull() ?: 0.0) + disc
        val tot = sale.totalAmount.toDoubleOrNull() ?: 0.0
        val cash = sale.totalCashIn.toDoubleOrNull() ?: 0.0
        val bank = sale.bankAmount.toDoubleOrNull() ?: 0.0
        val paid = sale.collectedAmount.toDoubleOrNull() ?: (cash + bank)
        val due = sale.balance.toDoubleOrNull() ?: 0.0

        return InvoicePrintData(
            businessName = busName,
            businessAddress = busAddr,
            businessPhone = busPhone,
            businessEmail = busEmail,
            logoUrl = profile?.logoUrl,
            logoBitmap = logoBitmap,
            invoiceTitle = "Line Sale Invoice",
            invoiceNumber = if (!sale.invoiceNumber.isNullOrBlank()) sale.invoiceNumber else sale.id,
            invoiceDate = FormatUtils.formatDateTimeForDisplay(sale.createdAt),
            customerName = sale.customerName.ifBlank { "Line Customer" },
            customerPhone = sale.customerPhone?.ifBlank { null },
            route = sale.route?.ifBlank { null },
            salesman = sale.salesman?.ifBlank { null },
            items = sale.items.map {
                PrintItem(
                    name = it.productNameSnapshot,
                    quantity = it.quantity,
                    unitPrice = it.unitPrice,
                    lineTotal = it.lineTotal
                )
            },
            subtotal = effectiveSub,
            discount = disc,
            totalAmount = tot,
            amountPaid = paid,
            balanceDue = due,
            paymentSummary = "Cash: ${FormatUtils.formatCurrency(sale.totalCashIn)} | Bank: ${FormatUtils.formatCurrency(sale.bankAmount)}",
            note = sale.note,
            footerHeading = profile?.getEffectiveFooterHeading(),
            footerParagraph = profile?.getEffectiveFooterParagraph()
        )
    }

    fun fromCounterSale(
        sale: CounterSale,
        profile: TenantProfile?,
        logoBitmap: Bitmap? = null
    ): InvoicePrintData {
        val busName = profile?.name?.ifBlank { "Vyavsa Business" } ?: "Vyavsa Business"
        val busAddr = profile?.getEffectiveAddress()
        val busPhone = profile?.phone
        val busEmail = profile?.email

        val disc = sale.discount.toDoubleOrNull() ?: 0.0
        val sub = sale.subtotal.toDoubleOrNull() ?: (sale.price.toDoubleOrNull() ?: (sale.totalAmount.toDoubleOrNull() ?: 0.0))
        val effectiveSub = if (sub > 0.0) sub else (sale.totalAmount.toDoubleOrNull() ?: 0.0) + disc
        val tot = sale.totalAmount.toDoubleOrNull() ?: 0.0

        return InvoicePrintData(
            businessName = busName,
            businessAddress = busAddr,
            businessPhone = busPhone,
            businessEmail = busEmail,
            logoUrl = profile?.logoUrl,
            logoBitmap = logoBitmap,
            invoiceTitle = "Counter POS Receipt",
            invoiceNumber = if (!sale.invoiceNumber.isNullOrBlank()) sale.invoiceNumber else sale.id,
            invoiceDate = FormatUtils.formatDateTimeForDisplay(sale.createdAt),
            customerName = "Walk-in Counter Customer",
            customerPhone = null,
            items = sale.items.map {
                PrintItem(
                    name = it.productNameSnapshot,
                    quantity = it.quantity,
                    unitPrice = it.unitPrice,
                    lineTotal = it.lineTotal
                )
            },
            singleItemName = if (sale.items.isEmpty()) sale.item.ifBlank { "Counter Item" } else null,
            subtotal = effectiveSub,
            discount = disc,
            totalAmount = tot,
            amountPaid = tot,
            balanceDue = 0.0,
            paymentSummary = "Method: ${sale.paymentMethod.uppercase(Locale.US)} | Cash: ${FormatUtils.formatCurrency(sale.cash)} | Bank: ${FormatUtils.formatCurrency(sale.bankAmount.ifBlank { sale.account })}",
            footerHeading = profile?.getEffectiveFooterHeading(),
            footerParagraph = profile?.getEffectiveFooterParagraph()
        )
    }
}
