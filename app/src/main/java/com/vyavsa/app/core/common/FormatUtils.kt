package com.vyavsa.app.core.common

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FormatUtils {
    private val inrFormat: NumberFormat by lazy {
        NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    }

    fun formatCurrency(amount: Double): String {
        return inrFormat.format(amount)
    }

    fun formatCurrency(amountString: String?): String {
        val amount = amountString?.toDoubleOrNull() ?: 0.0
        return inrFormat.format(amount)
    }

    fun todayDateString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    fun formatDateForDisplay(dateString: String?): String {
        if (dateString.isNullOrBlank()) return ""
        return try {
            val inputFormat = if (dateString.contains("T")) {
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            } else {
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            }
            val date = inputFormat.parse(dateString.substringBefore("Z").substringBefore("."))
            val outputFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            outputFormat.format(date ?: Date())
        } catch (e: Exception) {
            dateString
        }
    }

    fun formatDateTimeForDisplay(dateTimeString: String?): String {
        return formatDateForDisplay(dateTimeString)
    }

    fun formatIsoDateTime(dateTimeString: String?): String {
        return formatDateForDisplay(dateTimeString)
    }
}
