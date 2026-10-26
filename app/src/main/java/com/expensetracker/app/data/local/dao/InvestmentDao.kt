package com.expensetracker.app.data.local.dao

import androidx.room.*
import com.expensetracker.app.data.local.entity.InvestmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InvestmentDao {
    @Query("SELECT * FROM investments ORDER BY id DESC")
    fun getAllInvestments(): Flow<List<InvestmentEntity>>

    @Query("SELECT SUM(amountInvested) FROM investments")
    fun getTotalInvested(): Flow<Double?>

    @Query("SELECT SUM(currentValue) FROM investments")
    fun getTotalCurrentValue(): Flow<Double?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvestment(investment: InvestmentEntity): Long

    @Update
    suspend fun updateInvestment(investment: InvestmentEntity)

    @Delete
    suspend fun deleteInvestment(investment: InvestmentEntity)
}
