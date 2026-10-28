package com.expensetracker.app.utils

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object CurrencyUtils {
    private val formatter = DecimalFormat("#,###", DecimalFormatSymbols(Locale("vi", "VN")))

    fun formatVnd(amount: Double): String {
        return "${formatter.format(amount)} ₫"
    }

    fun formatNumber(amount: Double): String {
        return if (amount <= 0.0) "" else formatter.format(amount)
    }

    fun formatNumber(amount: Long): String {
        return if (amount <= 0L) "" else formatter.format(amount)
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
            formatter.format(num)
        } catch (e: Exception) {
            safeDigits
        }
    }
}
