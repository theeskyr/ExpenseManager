package com.expensetracker.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class AccountType {
    CASH,           // Tiền mặt
    BANK,           // Tài khoản ngân hàng
    E_WALLET,       // Ví điện tử (Momo, ZaloPay...)
    CREDIT_CARD,    // Thẻ tín dụng
    RESERVE         // Quỹ dự phòng / Tiết kiệm khẩn cấp
}

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: AccountType,
    val initialBalance: Double = 0.0,
    val currentBalance: Double = 0.0,
    val colorHex: String = "#4CAF50",
    val iconName: String = "AccountBalanceWallet",
    val isReserve: Boolean = false // Đánh dấu là nguồn tiền dự trữ
)
