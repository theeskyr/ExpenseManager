package com.expensetracker.app.utils

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object CurrencyUtils {
    private val formatter = ThreadLocal.withInitial {
        DecimalFormat("#,###", DecimalFormatSymbols(Locale("vi", "VN")))
    }

    fun formatVnd(amount: Double): String {
        val longVal = amount.toLong()
        if (amount == longVal.toDouble() && longVal in -999_999_999_999L..999_999_999_999L) {
            return formatVndFast(longVal)
        }
        return "${formatter.get()!!.format(amount)} ₫"
    }

    private fun formatVndFast(amount: Long): String {
        if (amount == 0L) return "0 ₫"
        val isNeg = amount < 0
        val v = if (isNeg) -amount else amount
        val s = v.toString()
        val len = s.length
        val commaCount = (len - 1) / 3
        val outLen = (if (isNeg) 1 else 0) + len + commaCount + 2
        val chars = CharArray(outLen)
        var outIdx = outLen - 1
        chars[outIdx--] = '₫'
        chars[outIdx--] = ' '
        var group = 0
        for (i in len - 1 downTo 0) {
            if (group == 3) {
                chars[outIdx--] = '.'
                group = 0
            }
            chars[outIdx--] = s[i]
            group++
        }
        if (isNeg) {
            chars[outIdx] = '-'
        }
        return String(chars)
    }

    fun formatNumber(amount: Double): String {
        return if (amount <= 0.0) "" else formatter.get()!!.format(amount)
    }

    fun formatNumber(amount: Long): String {
        return if (amount <= 0L) "" else formatter.get()!!.format(amount)
    }

    fun parseAmount(text: String): Double {
        val digitsOnly = text.filter { it.isDigit() }
        return digitsOnly.toDoubleOrNull() ?: 0.0
    }

    fun formatInputAmount(input: String): String {
        val cleanDigits = input.filter { it.isDigit() }.trimStart('0')
        if (cleanDigits.isEmpty()) return ""
        val safeDigits = if (cleanDigits.length > 15) cleanDigits.substring(0, 15) else cleanDigits
        return try {
            val num = safeDigits.toLong()
            formatter.get()!!.format(num)
        } catch (e: Exception) {
            safeDigits
        }
    }

    /**
     * Hỗ trợ đọc nhanh các định dạng số tiền tiếng Việt phổ biến:
     * - "300k", "50k", "15.5k" -> nhân 1.000
     * - "1.5tr", "2tr", "2.2tr" -> nhân 1.000.000
     * - "500000", "500.000" -> 500000
     */
    fun parseMoneyInput(raw: String): Double? {
        val clean = raw.trim().lowercase()
        if (clean.isBlank()) return null

        val kIdx = clean.indexOf('k')
        if (kIdx != -1) {
            val num = clean.substring(0, kIdx).filter { it.isDigit() || it == '.' || it == ',' }
                .replace(",", ".").toDoubleOrNull()
            if (num != null) return num * 1000
        }

        val trIdx = clean.indexOf("tr")
        if (trIdx != -1) {
            val num = clean.substring(0, trIdx).filter { it.isDigit() || it == '.' || it == ',' }
                .replace(",", ".").toDoubleOrNull()
            if (num != null) return num * 1000000
        }

        val cuIdx = clean.indexOf("củ")
        if (cuIdx != -1) {
            val num = clean.substring(0, cuIdx).filter { it.isDigit() || it == '.' || it == ',' }
                .replace(",", ".").toDoubleOrNull()
            if (num != null) return num * 1000000
        }

        val digitsOnly = clean.filter { it.isDigit() }
        return digitsOnly.toDoubleOrNull()
    }

    /**
     * Định dạng số tiền rút gọn dễ nhìn trên biểu đồ cột:
     * - 45.000 -> 45k
     * - 500.333 -> 500k
     * - 1.800.000 -> 1.8tr
     * - 15.000.000 -> 15tr
     */
    fun formatCompactVnd(amount: Double): String {
        if (amount <= 0.0) return "0"
        return when {
            amount >= 1_000_000_000.0 -> {
                val v = amount / 1_000_000_000.0
                if (v >= 10.0 || v % 1.0 == 0.0) "${v.toInt()}tỷ" else String.format(Locale.US, "%.1ftỷ", v)
            }
            amount >= 1_000_000.0 -> {
                val v = amount / 1_000_000.0
                if (v >= 10.0 || v % 1.0 == 0.0) "${v.toInt()}tr" else String.format(Locale.US, "%.1ftr", v)
            }
            amount >= 1_000.0 -> {
                val v = amount / 1_000.0
                if (v >= 100.0 || v % 1.0 == 0.0) "${v.toInt()}k" else String.format(Locale.US, "%.0fk", v)
            }
            else -> "${amount.toInt()}"
        }
    }
}
