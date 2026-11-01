package com.vyavsa.app.core.common

import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

/**
 * Authoritative financial calculation utility for sales in Vyavsa.
 *
 * Rules:
 * - Final Amount = Eligible Subtotal - Discount Amount
 * - Subtotal must be >= 0.00
 * - Discount must be non-negative (>= 0.00)
 * - Discount must not exceed eligible subtotal (discount <= subtotal)
 * - Rounds all values to 2 decimal places with HALF_UP precision
 * - Missing discount defaults to 0.00
 */
object SaleCalculationUtils {

    data class SaleCalculationResult(
        val subtotal: BigDecimal,
        val discount: BigDecimal,
        val finalAmount: BigDecimal,
        val isValid: Boolean,
        val errorMessage: String? = null
    ) {
        val subtotalString: String
            get() = String.format(Locale.US, "%.2f", subtotal)

        val discountString: String
            get() = String.format(Locale.US, "%.2f", discount)

        val finalAmountString: String
            get() = String.format(Locale.US, "%.2f", finalAmount)
    }

    /**
     * Parses a string representation of an amount into a 2-decimal-place BigDecimal.
     * Empty, blank, or malformed strings safely return 0.00.
     */
    fun parseAmount(amountStr: String?): BigDecimal {
        if (amountStr.isNullOrBlank()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
        }
        return try {
            val sanitized = amountStr.replace(",", "").trim()
            BigDecimal(sanitized).setScale(2, RoundingMode.HALF_UP)
        } catch (_: Exception) {
            BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
        }
    }

    /**
     * Calculates structured result with validation details.
     */
    fun calculateResult(subtotalStr: String?, discountStr: String?): SaleCalculationResult {
        val subtotal = parseAmount(subtotalStr)
        val discount = parseAmount(discountStr)

        if (subtotal < BigDecimal.ZERO) {
            return SaleCalculationResult(
                subtotal = subtotal,
                discount = discount,
                finalAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                isValid = false,
                errorMessage = "Subtotal cannot be negative"
            )
        }

        if (discount < BigDecimal.ZERO) {
            return SaleCalculationResult(
                subtotal = subtotal,
                discount = discount,
                finalAmount = subtotal,
                isValid = false,
                errorMessage = "Discount cannot be negative"
            )
        }

        if (discount > subtotal) {
            return SaleCalculationResult(
                subtotal = subtotal,
                discount = discount,
                finalAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                isValid = false,
                errorMessage = "Discount cannot exceed subtotal"
            )
        }

        val finalAmount = (subtotal - discount).setScale(2, RoundingMode.HALF_UP)
        return SaleCalculationResult(
            subtotal = subtotal,
            discount = discount,
            finalAmount = finalAmount,
            isValid = true,
            errorMessage = null
        )
    }

    /**
     * Calculates Final Amount = Subtotal - Discount (clamped to >= 0.00).
     * Returns formatted string with 2 decimal places.
     */
    fun calculateSaleTotal(subtotalStr: String?, discountStr: String?): String {
        return calculateResult(subtotalStr, discountStr).finalAmountString
    }

    /**
     * Calculates customer ledger due: max(0.00, finalAmount - (cash + bank))
     */
    fun calculateDue(finalTotal: String?, cash: String?, bank: String?): String {
        val tot = parseAmount(finalTotal)
        val c = parseAmount(cash)
        val b = parseAmount(bank)
        val paid = c + b
        val due = if (tot > paid) (tot - paid).setScale(2, RoundingMode.HALF_UP) else BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
        return formatDecimal(due)
    }

    fun calculateDue(finalAmountStr: String?, collectedAmountStr: String?): BigDecimal {
        val finalAmount = parseAmount(finalAmountStr)
        val collected = parseAmount(collectedAmountStr)
        return if (finalAmount > collected) {
            (finalAmount - collected).setScale(2, RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
        }
    }

    fun validateDiscount(subtotal: String?, discount: String?): String? {
        if (discount.isNullOrBlank()) return null
        val sub = parseAmount(subtotal)
        val sanitized = discount.replace(",", "").trim()
        val disc = try {
            BigDecimal(sanitized).setScale(2, RoundingMode.HALF_UP)
        } catch (_: Exception) {
            return "Discount must be a valid number"
        }
        if (disc < BigDecimal.ZERO) {
            return "Discount cannot be negative"
        }
        if (disc > sub) {
            return "Discount cannot exceed subtotal"
        }
        return null
    }

    fun formatDecimal(value: Double): String {
        return String.format(Locale.US, "%.2f", BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP))
    }

    fun formatDecimal(value: BigDecimal): String {
        return String.format(Locale.US, "%.2f", value.setScale(2, RoundingMode.HALF_UP))
    }

    /**
     * Preview breakdown of POS financial allocation consistent with backend rules.
     */
    data class PosAllocationPreview(
        val saleTotal: BigDecimal,
        val availableAdvance: BigDecimal,
        val advanceApplied: BigDecimal,
        val remainingPayable: BigDecimal,
        val directPaymentCollected: BigDecimal,
        val excessPayment: BigDecimal,
        val previousDuesSettled: BigDecimal,
        val advanceGenerated: BigDecimal,
        val remainingSaleDue: BigDecimal,
        val updatedCustomerDue: BigDecimal,
        val updatedAdvanceBalance: BigDecimal,
        val amountAllocatedToSale: BigDecimal = directPaymentCollected.min(remainingPayable),
        val previousDues: BigDecimal = BigDecimal.ZERO,
        val duesSettledFromExcess: BigDecimal = BigDecimal.ZERO,
        val duesSettledFromAdvance: BigDecimal = BigDecimal.ZERO
    ) {
        val currentSale: BigDecimal get() = saleTotal
        val previousDue: BigDecimal get() = previousDues
        val amountCollected: BigDecimal get() = directPaymentCollected
        val currentInvoicePaid: BigDecimal get() = amountAllocatedToSale
        val previousDueSettled: BigDecimal get() = previousDuesSettled
        val remainingDue: BigDecimal get() = updatedCustomerDue
        val availableAdvanceBalance: BigDecimal get() = availableAdvance
        val newAdvance: BigDecimal get() = advanceGenerated
        val currentInvoiceDue: BigDecimal get() = remainingSaleDue
        val totalSettledForSale: BigDecimal get() = (amountAllocatedToSale + advanceApplied).min(saleTotal)

        val saleTotalString: String get() = String.format(Locale.US, "%.2f", saleTotal)
        val availableAdvanceString: String get() = String.format(Locale.US, "%.2f", availableAdvance)
        val advanceAppliedString: String get() = String.format(Locale.US, "%.2f", advanceApplied)
        val remainingPayableString: String get() = String.format(Locale.US, "%.2f", remainingPayable)
        val directPaymentCollectedString: String get() = String.format(Locale.US, "%.2f", directPaymentCollected)
        val excessPaymentString: String get() = String.format(Locale.US, "%.2f", excessPayment)
        val previousDuesSettledString: String get() = String.format(Locale.US, "%.2f", previousDuesSettled)
        val advanceGeneratedString: String get() = String.format(Locale.US, "%.2f", advanceGenerated)
        val remainingSaleDueString: String get() = String.format(Locale.US, "%.2f", remainingSaleDue)
        val updatedCustomerDueString: String get() = String.format(Locale.US, "%.2f", updatedCustomerDue)
        val updatedAdvanceBalanceString: String get() = String.format(Locale.US, "%.2f", updatedAdvanceBalance)
        val amountAllocatedToSaleString: String get() = String.format(Locale.US, "%.2f", amountAllocatedToSale)
        val previousDuesString: String get() = String.format(Locale.US, "%.2f", previousDues)
        val totalSettledForSaleString: String get() = String.format(Locale.US, "%.2f", totalSettledForSale)
    }

    /**
     * Computes real-time POS allocation preview:
     * 1. Automatically applies available advance up to sale total
     * 2. Direct payment (cash + bank) settles remaining sale payable
     * 3. Any excess payment automatically settles eligible previous customer dues FIFO
     * 4. Remaining advance after sale also automatically settles remaining previous dues FIFO
     * 5. Remaining excess payment becomes new available customer advance
     *
     * Consistent with backend tenant operations (Line Sale & Counter Sale).
     */
    fun calculatePosAllocation(
        saleTotalStr: String?,
        customerCurrentBalanceStr: String?,
        customerAdvanceBalanceStr: String?,
        directCashStr: String?,
        directBankStr: String?
    ): PosAllocationPreview {
        val saleTotal = parseAmount(saleTotalStr).max(BigDecimal.ZERO)
        val prevDue = parseAmount(customerCurrentBalanceStr).max(BigDecimal.ZERO)
        val availAdv = parseAmount(customerAdvanceBalanceStr).max(BigDecimal.ZERO)
        val cash = parseAmount(directCashStr).max(BigDecimal.ZERO)
        val bank = parseAmount(directBankStr).max(BigDecimal.ZERO)
        val directPaid = cash + bank

        // 1. Apply existing advance to current sale
        val advanceApplied = availAdv.min(saleTotal)
        val netSalePayable = (saleTotal - advanceApplied).max(BigDecimal.ZERO)

        // 2. Direct payment applied towards current sale
        val saleAlloc = directPaid.min(netSalePayable)
        val dueOnThisSale = (netSalePayable - saleAlloc).max(BigDecimal.ZERO)

        // 3. Excess payment above net sale payable
        val excess = (directPaid - saleAlloc).max(BigDecimal.ZERO)

        // 4. Automatic previous dues settlement from excess payment
        val duesSettledFromExcess = excess.min(prevDue)
        val remDuesAfterExcess = (prevDue - duesSettledFromExcess).max(BigDecimal.ZERO)

        // 5. Automatically apply any remaining available advance to previous dues
        val remAdvAfterSale = (availAdv - advanceApplied).max(BigDecimal.ZERO)
        val duesSettledFromAdv = if (remAdvAfterSale > BigDecimal.ZERO && remDuesAfterExcess > BigDecimal.ZERO) {
            remAdvAfterSale.min(remDuesAfterExcess)
        } else {
            BigDecimal.ZERO
        }
        val totalDuesSettled = duesSettledFromExcess + duesSettledFromAdv

        // 6. Remaining excess payment stored as customer advance
        val advanceGenerated = (excess - duesSettledFromExcess).max(BigDecimal.ZERO)

        // 7. Updated customer dues and advance balances
        val updatedCustomerDue = (prevDue + dueOnThisSale - totalDuesSettled).max(BigDecimal.ZERO)
        val updatedAdvanceBalance = (availAdv - advanceApplied - duesSettledFromAdv + advanceGenerated).max(BigDecimal.ZERO)

        return PosAllocationPreview(
            saleTotal = saleTotal,
            availableAdvance = availAdv,
            advanceApplied = advanceApplied,
            remainingPayable = netSalePayable,
            directPaymentCollected = directPaid,
            excessPayment = excess,
            previousDuesSettled = totalDuesSettled,
            advanceGenerated = advanceGenerated,
            remainingSaleDue = dueOnThisSale,
            updatedCustomerDue = updatedCustomerDue,
            updatedAdvanceBalance = updatedAdvanceBalance,
            amountAllocatedToSale = saleAlloc,
            previousDues = prevDue,
            duesSettledFromExcess = duesSettledFromExcess,
            duesSettledFromAdvance = duesSettledFromAdv
        )
    }
}
