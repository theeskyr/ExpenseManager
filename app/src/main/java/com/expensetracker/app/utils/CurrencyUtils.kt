package com.expensetracker.app.utils

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object CurrencyUtils {
    private val formatter = DecimalFormat("#,###", DecimalFormatSymbols(Locale("vi", "VN")))

    fun formatVnd(amount: Double): String {
        return "${formatter.format(amount)} ₫"
    }

    fun parseAmount(text: String): Double {
        val cleanText = text.replace("[^0-9.]".toRegex(), "")
        return cleanText.toDoubleOrNull() ?: 0.0
    }
}
