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
        val advApplied = sale.advanceApplied.toDoubleOrNull() ?: 0.0
        val prevBal = sale.previousBalance.toDoubleOrNull() ?: 0.0
        val updatedBal = sale.updatedBalance.toDoubleOrNull() ?: (prevBal + due)
        val directReceived = sale.collectedAmount.toDoubleOrNull() ?: (cash + bank)
        val prevDuesSettled = sale.previousDuesSettled.toDoubleOrNull() ?: 0.0
        val advGenerated = sale.advanceGenerated.toDoubleOrNull() ?: 0.0
        val parsedAlloc = sale.amountAllocatedToSale.toDoubleOrNull()
        val allocatedToSale = if (parsedAlloc != null && parsedAlloc > 0.0) {
            parsedAlloc
        } else {
            directReceived.coerceAtMost((tot - advApplied).coerceAtLeast(0.0))
        }
        val totalSettled = (allocatedToSale + advApplied).coerceAtMost(tot)

        val paymentsList = sale.payments.filter { p ->
            val n = (p.note ?: "").lowercase()
            !n.startsWith("customer collection")
        }.map { p ->
            InvoicePaymentEntry(
                method = p.paymentMethod,
                amount = p.amount.toDoubleOrNull() ?: 0.0,
                bankName = p.bankName,
                referenceId = p.referenceId,
                note = p.note
            )
        }

        return InvoicePrintData(
            businessName = busName,
            businessAddress = busAddr,
            businessPhone = busPhone,
            businessEmail = busEmail,
            logoUrl = profile?.logoUrl,
            logoBitmap = logoBitmap,
            invoiceTitle = "Line Sale Invoice",
            invoiceNumber = if (!sale.invoiceNumber.isNullOrBlank()) sale.invoiceNumber else sale.id,
            invoiceDate = if (!sale.saleDate.isNullOrBlank()) sale.saleDate else FormatUtils.formatDateTimeForDisplay(sale.createdAt),
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
            amountAllocatedToSale = allocatedToSale,
            balanceDue = due,
            previousBalance = prevBal,
            advanceApplied = advApplied,
            amountReceivedDirect = directReceived,
            totalSettledForSale = totalSettled,
            previousDuesSettled = prevDuesSettled,
            advanceGenerated = advGenerated,
            updatedCustomerBalance = updatedBal,
            hasCustomerLedger = true,
            cashAmount = cash,
            bankAmount = bank,
            paymentMethod = if (bank > 0.0 && cash > 0.0) "split" else if (bank > 0.0) "bank" else "cash",
            payments = paymentsList,
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
        val advApplied = sale.advanceApplied.toDoubleOrNull() ?: 0.0
        val prevBal = sale.previousBalance.toDoubleOrNull() ?: 0.0
        val updatedBal = sale.updatedBalance.toDoubleOrNull() ?: 0.0
        val cash = sale.cash.toDoubleOrNull() ?: 0.0
        val bank = sale.bankAmount.toDoubleOrNull() ?: 0.0
        val directReceived = sale.collectedAmount.toDoubleOrNull() ?: (cash + bank)
        val due = sale.account.toDoubleOrNull() ?: 0.0
        val prevDuesSettled = sale.previousDuesSettled.toDoubleOrNull() ?: 0.0
        val advGenerated = sale.advanceGenerated.toDoubleOrNull() ?: 0.0
        val parsedAlloc = sale.amountAllocatedToSale.toDoubleOrNull()
        val allocatedToSale = if (parsedAlloc != null && parsedAlloc > 0.0) {
            parsedAlloc
        } else {
            directReceived.coerceAtMost((tot - advApplied).coerceAtLeast(0.0))
        }
        val totalSettled = if (advApplied > 0.0 || directReceived > 0.0) {
            (allocatedToSale + advApplied).coerceAtMost(tot)
        } else tot
        val hasCustomer = !sale.customerId.isNullOrBlank() || !sale.customerName.isNullOrBlank() || prevBal > 0.0 || advApplied > 0.0

        val paymentsList = sale.payments.filter { p ->
            val n = (p.note ?: "").lowercase()
            !n.startsWith("customer collection")
        }.map { p ->
            InvoicePaymentEntry(
                method = p.paymentMethod,
                amount = p.amount.toDoubleOrNull() ?: 0.0,
                bankName = p.bankName,
                referenceId = p.referenceId,
                note = p.note
            )
        }

        return InvoicePrintData(
            businessName = busName,
            businessAddress = busAddr,
            businessPhone = busPhone,
            businessEmail = busEmail,
            logoUrl = profile?.logoUrl,
            logoBitmap = logoBitmap,
            invoiceTitle = "Counter POS Receipt",
            invoiceNumber = if (!sale.invoiceNumber.isNullOrBlank()) sale.invoiceNumber else sale.id,
            invoiceDate = if (!sale.saleDate.isNullOrBlank()) sale.saleDate else FormatUtils.formatDateTimeForDisplay(sale.createdAt),
            customerName = if (!sale.customerName.isNullOrBlank()) sale.customerName else "Walk-in Counter Customer",
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
            amountPaid = if (directReceived > 0.0 || advApplied > 0.0) directReceived else tot,
            amountAllocatedToSale = allocatedToSale,
            balanceDue = due,
            previousBalance = prevBal,
            advanceApplied = advApplied,
            amountReceivedDirect = directReceived,
            totalSettledForSale = totalSettled,
            previousDuesSettled = prevDuesSettled,
            advanceGenerated = advGenerated,
            updatedCustomerBalance = updatedBal,
            hasCustomerLedger = hasCustomer,
            cashAmount = cash,
            bankAmount = bank,
            paymentMethod = sale.paymentMethod.ifBlank { if (bank > 0.0 && cash > 0.0) "split" else if (bank > 0.0) "bank" else "cash" },
            bankName = paymentsList.firstOrNull { !it.bankName.isNullOrBlank() }?.bankName,
            payments = paymentsList,
            paymentSummary = "Method: ${sale.paymentMethod.uppercase(Locale.US)} | Cash: ${FormatUtils.formatCurrency(sale.cash)} | Bank: ${FormatUtils.formatCurrency(sale.bankAmount.ifBlank { sale.account })}",
            footerHeading = profile?.getEffectiveFooterHeading(),
            footerParagraph = profile?.getEffectiveFooterParagraph()
        )
    }
}
