package com.expensetracker.app.data.local.dao

import androidx.room.*
import com.expensetracker.app.data.local.entity.TripEntity
import com.expensetracker.app.data.local.entity.TripExpenseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {

    @Query("SELECT * FROM trips ORDER BY createdAt DESC")
    fun getAllTrips(): Flow<List<TripEntity>>

    @Query("SELECT * FROM trips WHERE id = :tripId")
    suspend fun getTripById(tripId: String): TripEntity?

    @Query("SELECT * FROM trips WHERE id = :tripId")
    fun getTripByIdFlow(tripId: String): Flow<TripEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrip(trip: TripEntity)

    @Update
    suspend fun updateTrip(trip: TripEntity)

    @Delete
    suspend fun deleteTrip(trip: TripEntity)

    @Query("SELECT * FROM trip_expenses WHERE tripId = :tripId ORDER BY dayIndex ASC, dateTimestamp ASC")
    fun getExpensesForTrip(tripId: String): Flow<List<TripExpenseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: TripExpenseEntity)

    @Update
    suspend fun updateExpense(expense: TripExpenseEntity)

    @Delete
    suspend fun deleteExpense(expense: TripExpenseEntity)

    @Query("DELETE FROM trip_expenses WHERE tripId = :tripId")
    suspend fun deleteExpensesForTrip(tripId: String)
}
