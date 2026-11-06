package com.expensetracker.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "trips")
data class TripEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val destination: String = "",
    val startDate: Long = System.currentTimeMillis(),
    val endDate: Long? = null,
    val membersJson: String, // JSON List<SplitMember>
    val status: String = STATUS_ONGOING, // ONGOING or COMPLETED
    val totalExpense: Double = 0.0,
    val settlementSummary: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val STATUS_ONGOING = "ONGOING"
        const val STATUS_COMPLETED = "COMPLETED"
    }

    val isOngoing: Boolean
        get() = status == STATUS_ONGOING
}
