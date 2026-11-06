package com.expensetracker.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "trip_expenses",
    indices = [Index(value = ["tripId"])]
)
data class TripExpenseEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val tripId: String,
    val dayIndex: Int = 1, // Ngày 1, Ngày 2, Ngày 3...
    val title: String,
    val amount: Double,
    val payerMemberId: String,
    val payerMemberName: String,
    val sharerMemberIdsJson: String, // JSON List<String>
    val dateTimestamp: Long = System.currentTimeMillis(),
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
