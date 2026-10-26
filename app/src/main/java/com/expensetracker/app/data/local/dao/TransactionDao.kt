package com.expensetracker.app.data.local.dao

import androidx.room.*
import com.expensetracker.app.data.local.entity.TransactionEntity
import com.expensetracker.app.data.local.entity.TransactionType
import kotlinx.coroutines.flow.Flow

data class CategorySummary(
    val categoryId: String,
    val totalAmount: Double
)

data class DailySummary(
    val dayTimestamp: Long,
    val totalExpense: Double,
    val totalIncome: Double
)

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    fun getTransactionsInRange(startTime: Long, endTime: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE accountId = :accountId ORDER BY timestamp DESC")
    fun getTransactionsByAccount(accountId: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE categoryId = :categoryId ORDER BY timestamp DESC")
    fun getTransactionsByCategory(categoryId: String): Flow<List<TransactionEntity>>

    @Query("""
        SELECT categoryId, SUM(amount) as totalAmount 
        FROM transactions 
        WHERE type = :type AND timestamp BETWEEN :startTime AND :endTime 
        GROUP BY categoryId 
        ORDER BY totalAmount DESC
    """)
    fun getCategorySummaries(type: TransactionType, startTime: Long, endTime: Long): Flow<List<CategorySummary>>

    @Query("""
        SELECT SUM(amount) 
        FROM transactions 
        WHERE type = :type AND timestamp BETWEEN :startTime AND :endTime
    """)
    fun getTotalAmountInRange(type: TransactionType, startTime: Long, endTime: Long): Flow<Double?>

    @Query("""
        SELECT SUM(amount) 
        FROM transactions 
        WHERE categoryId = :categoryId AND type = 'EXPENSE' AND timestamp BETWEEN :startTime AND :endTime
    """)
    fun getExpenseForCategoryInRange(categoryId: String, startTime: Long, endTime: Long): Flow<Double?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()
}
