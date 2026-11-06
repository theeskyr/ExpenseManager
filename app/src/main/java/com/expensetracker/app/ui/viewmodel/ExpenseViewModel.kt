package com.expensetracker.app.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.app.data.ai.AiTransactionData
import com.expensetracker.app.data.ai.GeminiManager
import com.expensetracker.app.data.local.dao.CategorySummary
import com.expensetracker.app.data.local.entity.*
import com.expensetracker.app.data.model.SplitExpenseItem
import com.expensetracker.app.data.model.SplitMember
import com.expensetracker.app.data.repository.ExpenseRepository
import com.expensetracker.app.utils.CsvExporter
import com.expensetracker.app.utils.CurrencyUtils
import com.expensetracker.app.utils.DateUtils
import com.expensetracker.app.utils.TripJsonUtils
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
    val transactions: List<AiTransactionData> = emptyList()
) {
    // Backwards compatibility for single transaction
    val transactionData: AiTransactionData?
        get() = transactions.firstOrNull()
}

data class TripSplitData(
    val tripId: String,
    val eventTitle: String,
    val members: List<SplitMember>,
    val items: List<SplitExpenseItem>
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

    fun addPersonalSplitExpense(
        amount: Double,
        accountId: Long,
        categoryId: String,
        eventTitle: String
    ) {
        val titleNote = if (eventTitle.isNotBlank()) "Chia tiền: $eventTitle" else "Chia tiền nhóm cùng AI"
        addTransaction(
            amount = amount,
            type = TransactionType.EXPENSE,
            categoryId = categoryId,
            accountId = accountId,
            note = titleNote,
            timestamp = System.currentTimeMillis()
        )
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

            // Auto add all transactions if detected
            response.detectedTransactions.forEachIndexed { index, txData ->
                autoSaveAiTransaction(txData, offsetMs = index * 20L)
            }

            val aiMsg = ChatMessage(
                sender = MessageSender.AI,
                text = response.replyText,
                transactions = response.detectedTransactions
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

            // Auto add all transactions if detected
            response.detectedTransactions.forEachIndexed { index, txData ->
                autoSaveAiTransaction(txData, offsetMs = index * 20L)
            }

            val aiMsg = ChatMessage(
                sender = MessageSender.AI,
                text = response.replyText,
                transactions = response.detectedTransactions
            )
            _chatMessages.value = _chatMessages.value + aiMsg
            _isAiLoading.value = false
        }
    }

    private suspend fun autoSaveAiTransaction(txData: AiTransactionData, offsetMs: Long = 0L) {
        if (txData.amount <= 0.0) return
        val currentAccounts = allAccounts.value

        // Priority 1: Match account name directly
        val matchedByName = txData.suggestedAccountName?.let { accName ->
            currentAccounts.firstOrNull { it.name.contains(accName, ignoreCase = true) }
        }

        // Priority 2: If suggested account mentions Shopee/e-wallet, match E_WALLET account
        val matchedByType = if (matchedByName == null && txData.suggestedAccountName != null) {
            val s = txData.suggestedAccountName.lowercase()
            if (s.contains("shopee") || s.contains("momo") || s.contains("zalo") || s.contains("ví")) {
                currentAccounts.firstOrNull { it.type == AccountType.E_WALLET }
            } else if (s.contains("bank") || s.contains("ngân hàng")) {
                currentAccounts.firstOrNull { it.type == AccountType.BANK }
            } else {
                null
            }
        } else {
            null
        }

        // Priority 3: Non-reserve account, else first account
        val matchedAccount = matchedByName ?: matchedByType
            ?: currentAccounts.firstOrNull { !it.isReserve }
            ?: currentAccounts.firstOrNull()

        val accountId = matchedAccount?.id ?: 1L

        // Validate category exists, or fallback
        val currentCats = allCategories.value
        val validCatId = if (currentCats.any { it.id == txData.categoryId }) {
            txData.categoryId
        } else if (txData.type == TransactionType.INCOME) {
            "other_income"
        } else {
            "shopping"
        }

        repository.addTransaction(
            TransactionEntity(
                amount = txData.amount,
                type = txData.type,
                categoryId = validCatId,
                accountId = accountId,
                note = txData.note,
                timestamp = System.currentTimeMillis() + offsetMs
            )
        )
    }

    // ==========================================
    // TRIPS & GROUP TRAVEL MANAGEMENT
    // ==========================================

    val allTrips: StateFlow<List<TripEntity>> = repository.allTrips
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeTripForSplit = MutableStateFlow<TripSplitData?>(null)

    fun createTrip(
        title: String,
        destination: String = "",
        members: List<SplitMember>,
        onCreated: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            val trip = TripEntity(
                title = title.trim(),
                destination = destination.trim(),
                membersJson = TripJsonUtils.membersToJson(members),
                status = TripEntity.STATUS_ONGOING,
                totalExpense = 0.0
            )
            repository.insertTrip(trip)
            onCreated(trip.id)
        }
    }

    fun deleteTrip(trip: TripEntity) {
        viewModelScope.launch {
            repository.deleteTrip(trip)
            if (activeTripForSplit.value?.tripId == trip.id) {
                activeTripForSplit.value = null
            }
        }
    }

    fun getTripByIdFlow(tripId: String): Flow<TripEntity?> = repository.getTripByIdFlow(tripId)

    fun getTripExpenses(tripId: String): Flow<List<TripExpenseEntity>> = repository.getExpensesForTrip(tripId)

    fun addTripExpense(
        tripId: String,
        dayIndex: Int,
        title: String,
        amount: Double,
        payerMemberId: String,
        payerMemberName: String,
        sharerMemberIds: List<String>,
        note: String = ""
    ) {
        viewModelScope.launch {
            val expense = TripExpenseEntity(
                tripId = tripId,
                dayIndex = dayIndex,
                title = title.trim(),
                amount = amount,
                payerMemberId = payerMemberId,
                payerMemberName = payerMemberName,
                sharerMemberIdsJson = TripJsonUtils.stringListToJson(sharerMemberIds),
                note = note.trim()
            )
            repository.insertTripExpense(expense)

            // Cập nhật tổng tiền chuyến đi
            val trip = repository.getTripById(tripId)
            if (trip != null) {
                repository.updateTrip(trip.copy(totalExpense = trip.totalExpense + amount))
            }
        }
    }

    fun deleteTripExpense(expense: TripExpenseEntity) {
        viewModelScope.launch {
            repository.deleteTripExpense(expense)
            val trip = repository.getTripById(expense.tripId)
            if (trip != null) {
                val newTotal = (trip.totalExpense - expense.amount).coerceAtLeast(0.0)
                repository.updateTrip(trip.copy(totalExpense = newTotal))
            }
        }
    }

    /**
     * Nạp toàn bộ dữ liệu từ Chuyến đi sang Chia tiền bằng AI
     */
    fun prepareSplitBillFromTrip(trip: TripEntity, expenses: List<TripExpenseEntity>) {
        val members = TripJsonUtils.jsonToMembers(trip.membersJson)
        val items = expenses.map { exp ->
            val sharers = TripJsonUtils.jsonToStringList(exp.sharerMemberIdsJson)
            SplitExpenseItem(
                id = exp.id,
                title = "Ngày ${exp.dayIndex}: ${exp.title}",
                amount = exp.amount,
                paidByMemberId = exp.payerMemberId,
                splitAmongMemberIds = if (sharers.isEmpty()) members.map { it.id } else sharers
            )
        }

        activeTripForSplit.value = TripSplitData(
            tripId = trip.id,
            eventTitle = trip.title,
            members = members,
            items = items
        )
    }

    fun markTripCompleted(tripId: String, settlementSummary: String) {
        viewModelScope.launch {
            val trip = repository.getTripById(tripId) ?: return@launch
            repository.updateTrip(
                trip.copy(
                    status = TripEntity.STATUS_COMPLETED,
                    endDate = System.currentTimeMillis(),
                    settlementSummary = settlementSummary
                )
            )
        }
    }

    fun clearActiveTripForSplit() {
        activeTripForSplit.value = null
    }
}
