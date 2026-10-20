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
}
