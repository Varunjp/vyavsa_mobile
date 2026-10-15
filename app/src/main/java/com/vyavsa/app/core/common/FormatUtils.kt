package com.vyavsa.app.core.common

import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

object FormatUtils {
    private val inrLocale = Locale("en", "IN")

    private val currencyFormatThreadLocal = object : ThreadLocal<NumberFormat>() {
        override fun initialValue(): NumberFormat = NumberFormat.getCurrencyInstance(inrLocale)
    }

    private val displayDateFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH)

    private val displayDateTimeFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a", Locale.ENGLISH)

    private val isoDateFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US)

    // Fast in-memory caches to eliminate repeated parsing/formatting during LazyColumn scrolling
    private val currencyCache = ConcurrentHashMap<String, String>(256)
    private val dateDisplayCache = ConcurrentHashMap<String, String>(256)
    private val dateTimeDisplayCache = ConcurrentHashMap<String, String>(256)

    fun formatCurrency(amount: Double): String {
        val key = String.format(Locale.US, "%.2f", amount)
        return currencyCache.getOrPut(key) {
            currencyFormatThreadLocal.get()?.format(amount) ?: "₹$key"
        }
    }

    fun formatCurrency(amountString: String?): String {
        if (amountString.isNullOrBlank()) return formatCurrency(0.0)
        val trimmed = amountString.trim()
        currencyCache[trimmed]?.let { return it }
        val amount = trimmed.toDoubleOrNull() ?: 0.0
        val formatted = currencyFormatThreadLocal.get()?.format(amount) ?: "₹$trimmed"
        if (currencyCache.size < 1024) {
            currencyCache[trimmed] = formatted
        }
        return formatted
    }

    fun todayDateString(): String {
        return LocalDate.now().format(isoDateFormatter)
    }

    fun formatDateForDisplay(dateString: String?): String {
        if (dateString.isNullOrBlank()) return ""
        val key = dateString.trim()
        dateDisplayCache[key]?.let { return it }

        val formatted = parseToLocalDate(key)?.format(displayDateFormatter) ?: key
        if (dateDisplayCache.size < 1024) {
            dateDisplayCache[key] = formatted
        }
        return formatted
    }

    fun formatDateTimeForDisplay(dateTimeString: String?): String {
        if (dateTimeString.isNullOrBlank()) return ""
        val key = dateTimeString.trim()
        dateTimeDisplayCache[key]?.let { return it }

        val formatted = try {
            when {
                key.endsWith("Z") -> {
                    val instant = Instant.parse(key)
                    LocalDateTime.ofInstant(instant, ZoneId.systemDefault()).format(displayDateFormatter)
                }
                key.contains("T") -> {
                    val clean = key.substringBefore(".").substringBefore("+")
                    LocalDateTime.parse(clean).format(displayDateFormatter)
                }
                else -> formatDateForDisplay(key)
            }
        } catch (_: Exception) {
            formatDateForDisplay(key)
        }

        if (dateTimeDisplayCache.size < 1024) {
            dateTimeDisplayCache[key] = formatted
        }
        return formatted
    }

    fun formatIsoDateTime(dateTimeString: String?): String {
        return formatDateForDisplay(dateTimeString)
    }

    private fun parseToLocalDate(raw: String): LocalDate? {
        return try {
            when {
                raw.length == 10 && raw[4] == '-' -> LocalDate.parse(raw, isoDateFormatter)
                raw.endsWith("Z") -> Instant.parse(raw).atZone(ZoneId.systemDefault()).toLocalDate()
                raw.contains("T") -> {
                    val datePart = raw.substringBefore("T")
                    LocalDate.parse(datePart, isoDateFormatter)
                }
                else -> OffsetDateTime.parse(raw).toLocalDate()
            }
        } catch (_: Exception) {
            null
        }
    }
}
