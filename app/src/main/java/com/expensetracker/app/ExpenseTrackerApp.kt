package com.expensetracker.app

import android.app.Application
import com.expensetracker.app.data.local.AppDatabase
import com.expensetracker.app.data.repository.ExpenseRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class ExpenseTrackerApp : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob())

    val database by lazy { AppDatabase.getDatabase(this, applicationScope) }
    val geminiManager by lazy { com.expensetracker.app.data.ai.GeminiManager(this) }
    val repository by lazy {
        ExpenseRepository(
            transactionDao = database.transactionDao(),
            categoryDao = database.categoryDao(),
            accountDao = database.accountDao(),
            budgetDao = database.budgetDao(),
            investmentDao = database.investmentDao(),
            tripDao = database.tripDao()
        )
    }
}
