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
}
