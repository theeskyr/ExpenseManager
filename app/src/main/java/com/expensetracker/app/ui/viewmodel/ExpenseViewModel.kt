package com.expensetracker.app.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.app.data.local.dao.CategorySummary
import com.expensetracker.app.data.local.entity.*
import com.expensetracker.app.data.repository.ExpenseRepository
import com.expensetracker.app.utils.CsvExporter
import com.expensetracker.app.utils.DateUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.util.Calendar

enum class TimePeriod {
    THIS_WEEK, THIS_MONTH, THIS_YEAR
}

class ExpenseViewModel(
    private val repository: ExpenseRepository
) : ViewModel() {

    val allTransactions: StateFlow<List<TransactionEntity>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCategories: StateFlow<List<CategoryEntity>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAccounts: StateFlow<List<AccountEntity>> = repository.allAccounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalBalance: StateFlow<Double> = repository.totalBalance
        .map { it ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    private val currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1
    private val currentYear = Calendar.getInstance().get(Calendar.YEAR)

    private val startOfMonth = DateUtils.getStartOfMonth(currentMonth, currentYear)
    private val endOfMonth = DateUtils.getEndOfMonth(currentMonth, currentYear)

    val currentMonthExpense: StateFlow<Double> = repository.getTotalAmountInRange(
        TransactionType.EXPENSE, startOfMonth, endOfMonth
    ).map { it ?: 0.0 }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val currentMonthIncome: StateFlow<Double> = repository.getTotalAmountInRange(
        TransactionType.INCOME, startOfMonth, endOfMonth
    ).map { it ?: 0.0 }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalMonthlyBudget: StateFlow<BudgetEntity?> = repository.getTotalMonthlyBudget(currentMonth, currentYear)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Statistics Period Selection
    private val _selectedPeriod = MutableStateFlow(TimePeriod.THIS_MONTH)
    val selectedPeriod: StateFlow<TimePeriod> = _selectedPeriod.asStateFlow()

    fun setPeriod(period: TimePeriod) {
        _selectedPeriod.value = period
    }

    val statsCategorySummaries: StateFlow<List<CategorySummary>> = _selectedPeriod.flatMapLatest { period ->
        val (start, end) = getRangeForPeriod(period)
        repository.getCategorySummaries(TransactionType.EXPENSE, start, end)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val statsTotalExpense: StateFlow<Double> = _selectedPeriod.flatMapLatest { period ->
        val (start, end) = getRangeForPeriod(period)
        repository.getTotalAmountInRange(TransactionType.EXPENSE, start, end).map { it ?: 0.0 }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val statsTotalIncome: StateFlow<Double> = _selectedPeriod.flatMapLatest { period ->
        val (start, end) = getRangeForPeriod(period)
        repository.getTotalAmountInRange(TransactionType.INCOME, start, end).map { it ?: 0.0 }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    private fun getRangeForPeriod(period: TimePeriod): Pair<Long, Long> {
        val now = System.currentTimeMillis()
        return when (period) {
            TimePeriod.THIS_WEEK -> Pair(DateUtils.getStartOfWeek(), DateUtils.getEndOfWeek())
            TimePeriod.THIS_MONTH -> Pair(
                DateUtils.getStartOfMonth(currentMonth, currentYear),
                DateUtils.getEndOfMonth(currentMonth, currentYear)
            )
            TimePeriod.THIS_YEAR -> {
                val cal = Calendar.getInstance()
                cal.set(Calendar.DAY_OF_YEAR, 1)
                val start = DateUtils.getStartOfDay(cal.timeInMillis)
                cal.set(Calendar.MONTH, 11)
                cal.set(Calendar.DAY_OF_MONTH, 31)
                val end = DateUtils.getEndOfDay(cal.timeInMillis)
                Pair(start, end)
            }
        }
    }

    // CRUD
    fun addTransaction(
        amount: Double,
        type: TransactionType,
        categoryId: String,
        accountId: Long,
        note: String,
        timestamp: Long
    ) {
        viewModelScope.launch {
            repository.addTransaction(
                TransactionEntity(
                    amount = amount,
                    type = type,
                    categoryId = categoryId,
                    accountId = accountId,
                    note = note,
                    timestamp = timestamp
                )
            )
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }

    fun addAccount(name: String, type: AccountType, initialBalance: Double, colorHex: String) {
        viewModelScope.launch {
            repository.insertAccount(
                AccountEntity(
                    name = name,
                    type = type,
                    initialBalance = initialBalance,
                    currentBalance = initialBalance,
                    colorHex = colorHex
                )
            )
        }
    }

    fun deleteAccount(account: AccountEntity) {
        viewModelScope.launch {
            repository.deleteAccount(account)
        }
    }

    fun setBudget(amount: Double) {
        viewModelScope.launch {
            repository.setBudget(
                BudgetEntity(
                    categoryId = null,
                    month = currentMonth,
                    year = currentYear,
                    amountLimit = amount
                )
            )
        }
    }

    fun exportAndShareCsv(context: Context) {
        val transactions = allTransactions.value
        val catMap = allCategories.value.associateBy { it.id }
        val accMap = allAccounts.value.associateBy { it.id }

        val file = CsvExporter.exportTransactionsToCsv(context, transactions, catMap, accMap)
        file?.let {
            CsvExporter.shareCsvFile(context, it)
        }
    }

    fun resetAllData() {
        viewModelScope.launch {
            repository.deleteAllData()
        }
    }
}
