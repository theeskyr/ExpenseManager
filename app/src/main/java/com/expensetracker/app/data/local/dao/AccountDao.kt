package com.expensetracker.app.data.local.dao

import androidx.room.*
import com.expensetracker.app.data.local.entity.AccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts ORDER BY id ASC")
    fun getAllAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE id = :id LIMIT 1")
    suspend fun getAccountById(id: Long): AccountEntity?

    @Query("SELECT SUM(currentBalance) FROM accounts")
    fun getTotalBalance(): Flow<Double?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: AccountEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccounts(accounts: List<AccountEntity>)

    @Update
    suspend fun updateAccount(account: AccountEntity)

    @Query("UPDATE accounts SET currentBalance = currentBalance + :amount WHERE id = :accountId")
    suspend fun updateBalance(accountId: Long, amount: Double)

    @Delete
    suspend fun deleteAccount(account: AccountEntity)
}
