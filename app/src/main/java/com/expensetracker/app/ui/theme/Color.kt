package com.expensetracker.app.ui.theme

import androidx.compose.ui.graphics.Color

// Primary Brand Colors (Expressive Vibrant Emerald)
val PrimaryGreen = Color(0xFF059669)
val PrimaryGreenDark = Color(0xFF047857)
val PrimaryGreenLight = Color(0xFF10B981)
val PrimaryContainer = Color(0xFFD1FAE5)
val OnPrimaryContainer = Color(0xFF064E3B)

// Secondary Colors (Expressive Ocean & Teal)
val SecondaryTeal = Color(0xFF0284C7)
val SecondaryContainer = Color(0xFFE0F2FE)
val OnSecondaryContainer = Color(0xFF0369A1)

// Tertiary Colors (Expressive AI Violet & Indigo)
val TertiaryViolet = Color(0xFF7C3AED)
val TertiaryContainer = Color(0xFFEDE9FE)
val OnTertiaryContainer = Color(0xFF5B21B6)

// Surface Container Hierarchy (M3 Expressive)
val BackgroundLight = Color(0xFFF8FAFC)
val SurfaceLight = Color(0xFFFFFFFF)
val SurfaceContainerLowest = Color(0xFFFFFFFF)
val SurfaceContainerLow = Color(0xFFF8FAFC)
val SurfaceContainer = Color(0xFFF1F5F9)
val SurfaceContainerHigh = Color(0xFFE2E8F0)
val SurfaceContainerHighest = Color(0xFFCBD5E1)

// Typography & Content
val TextPrimary = Color(0xFF0F172A)
val TextSecondary = Color(0xFF64748B)
val TextTertiary = Color(0xFF94A3B8)

// Semantic Financial Colors
val ExpenseRed = Color(0xFFE11D48)
val ExpenseRedContainer = Color(0xFFFFE4E6)
val IncomeGreen = Color(0xFF059669)
val IncomeGreenContainer = Color(0xFFD1FAE5)
val WarningOrange = Color(0xFFD97706)
val WarningOrangeContainer = Color(0xFFFEF3C7)

val CardBackground = Color(0xFFFFFFFF)
val DividerColor = Color(0xFFE2E8F0)
val InputBackground = Color(0xFFF8FAFC)

// Category Colors (Expressive & Distinct)
val CatFood = Color(0xFFEA580C)
val CatShopping = Color(0xFFE11D48)
val CatTransport = Color(0xFF2563EB)
val CatBills = Color(0xFFD97706)
val CatHousing = Color(0xFF78350F)
val CatEntertainment = Color(0xFF9333EA)
val CatHealth = Color(0xFF0D9488)
val CatEducation = Color(0xFF0284C7)
val CatOther = Color(0xFF64748B)

val CatSalary = Color(0xFF059669)
val CatBonus = Color(0xFF65A30D)
val CatInvestment = Color(0xFF0284C7)
val CatSavings = Color(0xFFD97706)

private val colorCache = java.util.concurrent.ConcurrentHashMap<String, Color>()

fun parseColor(hex: String): Color {
    return colorCache.getOrPut(hex) {
        try {
            Color(android.graphics.Color.parseColor(hex))
        } catch (e: Exception) {
            PrimaryGreen
        }
    }
}
