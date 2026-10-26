package com.expensetracker.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

object IconMapping {
    fun getIcon(name: String): ImageVector {
        return when (name) {
            "Fastfood" -> Icons.Default.Fastfood
            "ShoppingCart" -> Icons.Default.ShoppingCart
            "DirectionsCar" -> Icons.Default.DirectionsCar
            "Receipt" -> Icons.Default.Receipt
            "Home" -> Icons.Default.Home
            "SportsEsports" -> Icons.Default.SportsEsports
            "MedicalServices" -> Icons.Default.MedicalServices
            "School" -> Icons.Default.School
            "MoreHoriz" -> Icons.Default.MoreHoriz
            "AttachMoney" -> Icons.Default.AttachMoney
            "CardGiftcard" -> Icons.Default.CardGiftcard
            "TrendingUp" -> Icons.Default.TrendingUp
            "Savings" -> Icons.Default.Savings
            "Payments" -> Icons.Default.Payments
            "AccountBalance" -> Icons.Default.AccountBalance
            "AccountBalanceWallet" -> Icons.Default.AccountBalanceWallet
            "CreditCard" -> Icons.Default.CreditCard
            else -> Icons.Default.Category
        }
    }
}

@Composable
fun CategoryIcon(
    name: String,
    tint: Color = Color.White,
    modifier: Modifier = Modifier
) {
    Icon(
        imageVector = IconMapping.getIcon(name),
        contentDescription = name,
        tint = tint,
        modifier = modifier
    )
}
