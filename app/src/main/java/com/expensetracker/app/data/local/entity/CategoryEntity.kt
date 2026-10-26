package com.expensetracker.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val type: TransactionType, // EXPENSE or INCOME
    val iconName: String,
    val colorHex: String,
    val isDefault: Boolean = true
)
