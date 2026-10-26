package com.expensetracker.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class InvestmentType {
    GOLD,           // Vàng
    STOCK,          // Cổ phiếu
    FUND,           // Chứng chỉ quỹ
    SAVINGS,        // Tiết kiệm sinh lời
    REAL_ESTATE,    // Bất động sản
    CRYPTO,         // Tiền mã hóa
    OTHER           // Khác
}

@Entity(tableName = "investments")
data class InvestmentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: InvestmentType,
    val amountInvested: Double, // Vốn ban đầu
    val currentValue: Double,   // Giá trị thị trường hiện tại
    val startDate: Long = System.currentTimeMillis(),
    val note: String = ""
)
