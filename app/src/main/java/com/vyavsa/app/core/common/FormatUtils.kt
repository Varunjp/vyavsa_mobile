package com.vyavsa.app.core.common

import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
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

    fun formatSalaryForDisplay(salary: String?): String {
        if (salary.isNullOrBlank()) return "No fixed salary"
        return formatCurrency(salary)
    }

    fun resolveMediaUrl(path: String?): String? {
        if (path.isNullOrBlank()) return null
        val trimmed = path.trim()
        if (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)) {
            return trimmed
        }
        val rawBase = com.vyavsa.app.BuildConfig.BASE_URL.ifBlank { "https://vyavsa.varunjp.in/" }
        val base = rawBase.trimEnd('/')
        val relative = if (trimmed.startsWith("/")) trimmed else "/$trimmed"
        return "$base$relative"
    }

    fun todayDateString(): String {
        return LocalDate.now().format(isoDateFormatter)
    }

    /**
     * Converts UTC epoch milliseconds (e.g. from Material 3 DatePicker) to a YYYY-MM-DD date string.
     * Guarantees zero timezone drift across all local device timezones.
     */
    fun utcMillisToLocalDateString(utcMillis: Long): String {
        return Instant.ofEpochMilli(utcMillis)
            .atZone(ZoneOffset.UTC)
            .toLocalDate()
            .format(isoDateFormatter)
    }

    /**
     * Converts a YYYY-MM-DD date string to UTC epoch milliseconds for Material 3 DatePicker initialization.
     * Guarantees zero timezone drift across all local device timezones.
     */
    fun localDateStringToUtcMillis(dateString: String?): Long? {
        if (dateString.isNullOrBlank()) return null
        return try {
            val trimmed = dateString.trim()
            val localDate = when {
                trimmed.length == 10 && trimmed[4] == '-' -> LocalDate.parse(trimmed, isoDateFormatter)
                trimmed.contains("T") -> LocalDate.parse(trimmed.substringBefore("T"), isoDateFormatter)
                else -> parseToLocalDate(trimmed)
            } ?: return null
            localDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Validates whether the given string is in strict YYYY-MM-DD ISO format.
     */
    fun isValidIsoDate(dateString: String?): Boolean {
        if (dateString.isNullOrBlank()) return false
        val trimmed = dateString.trim()
        if (trimmed.length != 10 || trimmed[4] != '-' || trimmed[7] != '-') return false
        return try {
            val parsed = LocalDate.parse(trimmed, isoDateFormatter)
            parsed.format(isoDateFormatter) == trimmed
        } catch (_: Exception) {
            false
        }
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

    /**
     * Converts an amount into words following the Indian numbering system (Crores, Lakhs, Thousands).
     */
    fun numberToWordsINR(amount: Double): String {
        val totalPaise = Math.round(amount * 100.0)
        val rupees = totalPaise / 100
        val paise = totalPaise % 100

        if (rupees == 0L && paise == 0L) return "Zero Rupees Only"

        val ones = arrayOf(
            "", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine",
            "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen",
            "Seventeen", "Eighteen", "Nineteen"
        )
        val tens = arrayOf(
            "", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"
        )

        fun convertUnderThousand(n: Long): String {
            if (n == 0L) return ""
            val sb = java.lang.StringBuilder()
            var rem = n
            if (rem >= 100) {
                sb.append(ones[(rem / 100).toInt()]).append(" Hundred")
                rem %= 100
                if (rem > 0) sb.append(" ")
            }
            if (rem > 0) {
                if (rem < 20) {
                    sb.append(ones[rem.toInt()])
                } else {
                    sb.append(tens[(rem / 10).toInt()])
                    if (rem % 10 > 0) {
                        sb.append(" ").append(ones[(rem % 10).toInt()])
                    }
                }
            }
            return sb.toString()
        }

        val parts = mutableListOf<String>()
        var num = rupees

        val crore = num / 10000000L
        num %= 10000000L

        val lakh = num / 100000L
        num %= 100000L

        val thousand = num / 1000L
        num %= 1000L

        val hundredsAndBelow = num

        if (crore > 0) parts.add("${convertUnderThousand(crore)} Crore")
        if (lakh > 0) parts.add("${convertUnderThousand(lakh)} Lakh")
        if (thousand > 0) parts.add("${convertUnderThousand(thousand)} Thousand")
        if (hundredsAndBelow > 0) parts.add(convertUnderThousand(hundredsAndBelow))

        val rupeeWords = if (parts.isNotEmpty()) parts.joinToString(" ") + " Rupees" else ""
        val paiseWords = if (paise > 0) "${convertUnderThousand(paise)} Paise" else ""

        return when {
            rupeeWords.isNotEmpty() && paiseWords.isNotEmpty() -> "$rupeeWords and $paiseWords Only"
            rupeeWords.isNotEmpty() -> "$rupeeWords Only"
            paiseWords.isNotEmpty() -> "$paiseWords Only"
            else -> "Zero Rupees Only"
        }
    }
}
