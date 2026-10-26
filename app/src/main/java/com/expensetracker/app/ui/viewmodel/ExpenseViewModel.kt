package com.expensetracker.app.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.app.data.ai.AiTransactionData
import com.expensetracker.app.data.ai.GeminiManager
import com.expensetracker.app.data.local.dao.CategorySummary
import com.expensetracker.app.data.local.entity.*
import com.expensetracker.app.data.repository.ExpenseRepository
import com.expensetracker.app.utils.CsvExporter
import com.expensetracker.app.utils.CurrencyUtils
import com.expensetracker.app.utils.DateUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.util.Calendar
import java.util.UUID

enum class TimePeriod {
    THIS_WEEK, THIS_MONTH, THIS_YEAR
}

enum class MessageSender {
    USER, AI
}

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val sender: MessageSender,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val bitmap: Bitmap? = null,
    val transactionData: AiTransactionData? = null
)

class ExpenseViewModel(
    private val repository: ExpenseRepository,
    val geminiManager: GeminiManager
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

    val reserveBalance: StateFlow<Double> = repository.reserveBalance
        .map { it ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val availableBalance: StateFlow<Double> = repository.availableBalance
        .map { it ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Investments
    val allInvestments: StateFlow<List<InvestmentEntity>> = repository.allInvestments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalInvested: StateFlow<Double> = repository.totalInvested
        .map { it ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalInvestmentValue: StateFlow<Double> = repository.totalCurrentValue
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

    fun updateTransaction(oldTransaction: TransactionEntity, newTransaction: TransactionEntity) {
        viewModelScope.launch {
            repository.updateTransaction(oldTransaction, newTransaction)
        }
    }

    fun addAccount(name: String, type: AccountType, initialBalance: Double, colorHex: String, isReserve: Boolean = false) {
        viewModelScope.launch {
            repository.insertAccount(
                AccountEntity(
                    name = name,
                    type = type,
                    initialBalance = initialBalance,
                    currentBalance = initialBalance,
                    colorHex = colorHex,
                    isReserve = isReserve
                )
            )
        }
    }

    fun updateAccount(account: AccountEntity) {
        viewModelScope.launch {
            repository.updateAccount(account)
        }
    }

    fun updateAccountBalance(accountId: Long, newBalance: Double) {
        viewModelScope.launch {
            val acc = allAccounts.value.firstOrNull { it.id == accountId }
            acc?.let {
                repository.updateAccount(it.copy(currentBalance = newBalance))
            }
        }
    }

    // Investment Operations
    fun addInvestment(
        name: String,
        type: InvestmentType,
        amountInvested: Double,
        currentValue: Double,
        note: String
    ) {
        viewModelScope.launch {
            repository.insertInvestment(
                InvestmentEntity(
                    name = name,
                    type = type,
                    amountInvested = amountInvested,
                    currentValue = currentValue,
                    note = note
                )
            )
        }
    }

    fun deleteInvestment(investment: InvestmentEntity) {
        viewModelScope.launch {
            repository.deleteInvestment(investment)
        }
    }

    fun requestAiInvestmentAnalysis(onComplete: () -> Unit) {
        val investments = allInvestments.value
        if (investments.isEmpty()) return

        val sb = StringBuilder()
        sb.appendLine("Tổng vốn đầu tư: ${CurrencyUtils.formatVnd(totalInvested.value)}")
        sb.appendLine("Giá trị hiện tại: ${CurrencyUtils.formatVnd(totalInvestmentValue.value)}")
        sb.appendLine("Chi tiết các danh mục:")
        investments.forEach { inv ->
            val diff = inv.currentValue - inv.amountInvested
            val roi = if (inv.amountInvested > 0) (diff / inv.amountInvested * 100) else 0.0
            sb.appendLine("- ${inv.name} (${inv.type}): Vốn ${CurrencyUtils.formatVnd(inv.amountInvested)}, Giá trị ${CurrencyUtils.formatVnd(inv.currentValue)}, Lời/Lỗ: ${String.format("%.1f", roi)}%")
        }

        val userPrompt = "Hãy phân tích danh mục đầu tư này và đưa ra nhận xét, đánh giá rủi ro và khuyến nghị quản lý vốn."
        val userMsg = ChatMessage(sender = MessageSender.USER, text = userPrompt)
        _chatMessages.value = _chatMessages.value + userMsg

        viewModelScope.launch {
            _isAiLoading.value = true
            val response = geminiManager.analyzeInvestmentPortfolio(sb.toString())
            val aiMsg = ChatMessage(
                sender = MessageSender.AI,
                text = response.replyText
            )
            _chatMessages.value = _chatMessages.value + aiMsg
            _isAiLoading.value = false
            onComplete()
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

    // AI Chat & Receipt Analysis States
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                sender = MessageSender.AI,
                text = "Xin chào! Em là Trợ lý Chi tiêu AI của bạn 🤖.\n\nBạn có thể:\n• Nhắn tự nhiên: \"Vừa ăn phở 55k bằng tiền mặt\" hoặc \"Mua áo 350k trên Shopee\"\n• Gửi ảnh hóa đơn, bill thanh toán Shopee / Lazada để em tự đọc và cộng vào chi tiêu!\n• Hỏi bất kỳ thắc mắc nào về quản lý tài chính."
            )
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    val geminiApiKey = MutableStateFlow(geminiManager.getApiKey())
    val selectedAiModel = MutableStateFlow(geminiManager.getSelectedModel())

    fun saveGeminiApiKey(key: String) {
        geminiManager.setApiKey(key)
        geminiApiKey.value = key
    }

    fun updateAiModel(model: String) {
        geminiManager.setSelectedModel(model)
        selectedAiModel.value = model
    }

    fun sendAiTextMessage(text: String) {
        if (text.isBlank()) return
        val userMsg = ChatMessage(sender = MessageSender.USER, text = text)
        _chatMessages.value = _chatMessages.value + userMsg

        viewModelScope.launch {
            _isAiLoading.value = true
            val spendingContext = "Chi tháng này: ${currentMonthExpense.value} ₫, Thu tháng này: ${currentMonthIncome.value} ₫"
            val response = geminiManager.processTextMessage(text, spendingContext)

            // Auto add transaction if detected
            response.detectedTransaction?.let { txData ->
                autoSaveAiTransaction(txData)
            }

            val aiMsg = ChatMessage(
                sender = MessageSender.AI,
                text = response.replyText,
                transactionData = response.detectedTransaction
            )
            _chatMessages.value = _chatMessages.value + aiMsg
            _isAiLoading.value = false
        }
    }

    fun sendAiImageMessage(bitmap: Bitmap, userPrompt: String = "") {
        val userMsg = ChatMessage(
            sender = MessageSender.USER,
            text = if (userPrompt.isNotBlank()) userPrompt else "Đã gửi 1 ảnh hóa đơn/bill",
            bitmap = bitmap
        )
        _chatMessages.value = _chatMessages.value + userMsg

        viewModelScope.launch {
            _isAiLoading.value = true
            val response = geminiManager.processReceiptImage(bitmap, userPrompt)

            // Auto add transaction if detected
            response.detectedTransaction?.let { txData ->
                autoSaveAiTransaction(txData)
            }

            val aiMsg = ChatMessage(
                sender = MessageSender.AI,
                text = response.replyText,
                transactionData = response.detectedTransaction
            )
            _chatMessages.value = _chatMessages.value + aiMsg
            _isAiLoading.value = false
        }
    }

    private suspend fun autoSaveAiTransaction(txData: AiTransactionData) {
        val currentAccounts = allAccounts.value
        val matchedAccount = txData.suggestedAccountName?.let { accName ->
            currentAccounts.firstOrNull { it.name.contains(accName, ignoreCase = true) }
        } ?: currentAccounts.firstOrNull()

        val accountId = matchedAccount?.id ?: 1L

        repository.addTransaction(
            TransactionEntity(
                amount = txData.amount,
                type = txData.type,
                categoryId = txData.categoryId,
                accountId = accountId,
                note = txData.note,
                timestamp = System.currentTimeMillis()
            )
        )
    }
}
