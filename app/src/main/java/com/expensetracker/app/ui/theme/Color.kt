package com.expensetracker.app.ui.theme

import androidx.compose.ui.graphics.Color

val PrimaryGreen = Color(0xFF10B981)
val PrimaryGreenDark = Color(0xFF059669)
val SecondaryTeal = Color(0xFF14B8A6)
val BackgroundLight = Color(0xFFF8FAFC)
val SurfaceLight = Color(0xFFFFFFFF)
val TextPrimary = Color(0xFF1E293B)
val TextSecondary = Color(0xFF64748B)

val ExpenseRed = Color(0xFFEF4444)
val IncomeGreen = Color(0xFF10B981)
val WarningOrange = Color(0xFFF59E0B)

val CardBackground = Color(0xFFFFFFFF)
val DividerColor = Color(0xFFE2E8F0)
val InputBackground = Color(0xFFF1F5F9)

// Category Colors
val CatFood = Color(0xFFFF5722)
val CatShopping = Color(0xFFE91E63)
val CatTransport = Color(0xFF3F51B5)
val CatBills = Color(0xFFFF9800)
val CatHousing = Color(0xFF795548)
val CatEntertainment = Color(0xFF9C27B0)
val CatHealth = Color(0xFF009688)
val CatEducation = Color(0xFF00BCD4)
val CatOther = Color(0xFF607D8B)

val CatSalary = Color(0xFF10B981)
val CatBonus = Color(0xFF8BC34A)
val CatInvestment = Color(0xFF00ACC1)
val CatSavings = Color(0xFFFFB300)

fun parseColor(hex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        PrimaryGreen
    }
}
