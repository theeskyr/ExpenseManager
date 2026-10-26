package com.expensetracker.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val categoryId: String? = null, // null nghĩa là ngân sách tổng cho toàn bộ chi tiêu
    val month: Int, // 1 - 12
    val year: Int,
    val amountLimit: Double
)
