package com.expensetracker.app.data.repository

import com.expensetracker.app.data.local.dao.AccountDao
import com.expensetracker.app.data.local.dao.BudgetDao
import com.expensetracker.app.data.local.dao.CategoryDao
import com.expensetracker.app.data.local.dao.CategorySummary
import com.expensetracker.app.data.local.dao.TransactionDao
import com.expensetracker.app.data.local.entity.*
import kotlinx.coroutines.flow.Flow

class ExpenseRepository(
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao,
    private val accountDao: AccountDao,
    private val budgetDao: BudgetDao
) {
    // Transactions
    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()

    fun getTransactionsInRange(startTime: Long, endTime: Long): Flow<List<TransactionEntity>> =
        transactionDao.getTransactionsInRange(startTime, endTime)

    fun getCategorySummaries(type: TransactionType, startTime: Long, endTime: Long): Flow<List<CategorySummary>> =
        transactionDao.getCategorySummaries(type, startTime, endTime)

    fun getTotalAmountInRange(type: TransactionType, startTime: Long, endTime: Long): Flow<Double?> =
        transactionDao.getTotalAmountInRange(type, startTime, endTime)

    fun getExpenseForCategoryInRange(categoryId: String, startTime: Long, endTime: Long): Flow<Double?> =
        transactionDao.getExpenseForCategoryInRange(categoryId, startTime, endTime)

    suspend fun addTransaction(transaction: TransactionEntity) {
        transactionDao.insertTransaction(transaction)
        // Update account balance
        val balanceDelta = if (transaction.type == TransactionType.INCOME) {
            transaction.amount
        } else {
            -transaction.amount
        }
        accountDao.updateBalance(transaction.accountId, balanceDelta)
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) {
        transactionDao.deleteTransaction(transaction)
        // Revert account balance
        val revertDelta = if (transaction.type == TransactionType.INCOME) {
            -transaction.amount
        } else {
            transaction.amount
        }
        accountDao.updateBalance(transaction.accountId, revertDelta)
    }

    // Accounts
    val allAccounts: Flow<List<AccountEntity>> = accountDao.getAllAccounts()
    val totalBalance: Flow<Double?> = accountDao.getTotalBalance()

    suspend fun insertAccount(account: AccountEntity) = accountDao.insertAccount(account)
    suspend fun updateAccount(account: AccountEntity) = accountDao.updateAccount(account)
    suspend fun deleteAccount(account: AccountEntity) = accountDao.deleteAccount(account)

    // Categories
    val allCategories: Flow<List<CategoryEntity>> = categoryDao.getAllCategories()
    fun getCategoriesByType(type: TransactionType): Flow<List<CategoryEntity>> =
        categoryDao.getCategoriesByType(type)
    suspend fun insertCategory(category: CategoryEntity) = categoryDao.insertCategory(category)
    suspend fun deleteCategory(category: CategoryEntity) = categoryDao.deleteCategory(category)

    // Budgets
    fun getBudgetsForMonth(month: Int, year: Int): Flow<List<BudgetEntity>> =
        budgetDao.getBudgetsForMonth(month, year)

    fun getTotalMonthlyBudget(month: Int, year: Int): Flow<BudgetEntity?> =
        budgetDao.getTotalMonthlyBudget(month, year)

    suspend fun setBudget(budget: BudgetEntity) = budgetDao.insertBudget(budget)
    suspend fun deleteBudget(budget: BudgetEntity) = budgetDao.deleteBudget(budget)

    suspend fun deleteAllData() {
        transactionDao.deleteAllTransactions()
    }
}
