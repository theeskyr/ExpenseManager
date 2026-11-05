package com.expensetracker.app.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import kotlinx.coroutines.launch
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar
import java.util.Locale
import com.expensetracker.app.data.local.entity.AccountEntity
import com.expensetracker.app.data.local.entity.AccountType
import com.expensetracker.app.data.local.entity.CategoryEntity
import com.expensetracker.app.data.local.entity.TransactionEntity
import com.expensetracker.app.data.local.entity.TransactionType
import com.expensetracker.app.ui.components.AccountDetailBottomSheet
import com.expensetracker.app.ui.components.CategoryIcon
import com.expensetracker.app.ui.components.DayGroupHeader
import com.expensetracker.app.ui.components.EditTransactionDialog
import com.expensetracker.app.ui.components.TransactionItem
import com.expensetracker.app.ui.theme.*
import com.expensetracker.app.ui.viewmodel.ExpenseViewModel
import com.expensetracker.app.utils.CurrencyUtils
import com.expensetracker.app.utils.DateUtils

@Composable
fun HomeScreen(
    viewModel: ExpenseViewModel,
    onNavigateToAdd: () -> Unit,
    onNavigateToAi: () -> Unit = {},
    onNavigateToInvestments: () -> Unit = {},
    onNavigateToAccounts: () -> Unit = {},
    onNavigateToSplitBill: () -> Unit = {}
) {
    val totalBalance by viewModel.totalBalance.collectAsState()
    val reserveBalance by viewModel.reserveBalance.collectAsState()
    val availableBalance by viewModel.availableBalance.collectAsState()
    val monthExpense by viewModel.currentMonthExpense.collectAsState()
    val monthIncome by viewModel.currentMonthIncome.collectAsState()
    val monthlyBudget by viewModel.totalMonthlyBudget.collectAsState()
    val transactions by viewModel.allTransactions.collectAsState()
    val categories by viewModel.allCategories.collectAsState()
    val accounts by viewModel.allAccounts.collectAsState()

    val categoryMap = remember(categories) { categories.associateBy { it.id } }
    val accountMap = remember(accounts) { accounts.associateBy { it.id } }

    var editingTransaction by remember { mutableStateOf<TransactionEntity?>(null) }
    var selectedAccountForDetail by remember { mutableStateOf<AccountEntity?>(null) }
    var selectedFilterAccountId by remember { mutableStateOf<Long?>(null) }
    var selectedTransactionType by remember { mutableStateOf<TransactionType?>(null) }

    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val context = LocalContext.current
    var selectedFilterDate by remember { mutableStateOf<Long?>(null) } // null = All days (default)

    val todayStart = remember { DateUtils.getStartOfDay() }
    val yesterdayStart = remember { todayStart - 86400000L }

    val filteredTransactions = remember(transactions, selectedFilterDate, selectedFilterAccountId, selectedTransactionType) {
        if (selectedFilterDate == null && selectedFilterAccountId == null && selectedTransactionType == null) {
            transactions
        } else {
            transactions.filter { tx ->
                val matchDate = selectedFilterDate == null || DateUtils.getStartOfDay(tx.timestamp) == selectedFilterDate
                val matchAccount = selectedFilterAccountId == null || tx.accountId == selectedFilterAccountId
                val matchType = selectedTransactionType == null || tx.type == selectedTransactionType
                matchDate && matchAccount && matchType
            }
        }
    }

    val dayGroups = remember(filteredTransactions) {
        filteredTransactions
            .groupBy { DateUtils.getStartOfDay(it.timestamp) }
            .map { (dayTimestamp, txList) ->
                var dayExpense = 0.0
                var dayIncome = 0.0
                for (tx in txList) {
                    if (tx.type == TransactionType.EXPENSE) dayExpense += tx.amount
                    else if (tx.type == TransactionType.INCOME) dayIncome += tx.amount
                }
                val rawLabel = DateUtils.getHeaderDateString(dayTimestamp)
                val dateLabel = rawLabel.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(Locale("vi", "VN")) else it.toString()
                }
                DayTransactionGroup(
                    dayTimestamp = dayTimestamp,
                    dateLabel = dateLabel,
                    dayExpense = dayExpense,
                    dayIncome = dayIncome,
                    transactions = txList
                )
            }
    }

    fun showDatePicker() {
        val calendar = Calendar.getInstance()
        selectedFilterDate?.let { calendar.timeInMillis = it }
        android.app.DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val chosenCal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                selectedFilterDate = chosenCal.timeInMillis
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAdd,
                shape = RoundedCornerShape(20.dp),
                containerColor = PrimaryGreen,
                contentColor = Color.White,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 3.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Thêm giao dịch", modifier = Modifier.size(28.dp))
            }
        }
    ) { paddingValues ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(key = "total_balance", contentType = "total_balance") {
                Spacer(modifier = Modifier.height(4.dp))
                // Header Total Balance Card with Reserve & Available Balance
                TotalBalanceCard(
                    totalBalance = totalBalance,
                    reserveBalance = reserveBalance,
                    availableBalance = availableBalance,
                    monthExpense = monthExpense,
                    monthIncome = monthIncome,
                    onManageAccounts = onNavigateToAccounts,
                    onViewMonthIncome = {
                        selectedTransactionType = TransactionType.INCOME
                        coroutineScope.launch {
                            val targetIndex = if (monthlyBudget != null) 4 else 3
                            listState.animateScrollToItem(targetIndex)
                        }
                    },
                    onViewMonthExpense = {
                        selectedTransactionType = TransactionType.EXPENSE
                        coroutineScope.launch {
                            val targetIndex = if (monthlyBudget != null) 4 else 3
                            listState.animateScrollToItem(targetIndex)
                        }
                    }
                )
            }

            // Quick Actions: AI & Investments (M3 Expressive Cards)
            item(key = "quick_actions", contentType = "quick_actions") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // AI Assistant Card
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xFFF0FDF4), ExpressiveCardShape)
                            .border(1.dp, Color(0xFFBBF7D0), ExpressiveCardShape)
                            .clip(ExpressiveCardShape)
                            .clickable { onNavigateToAi() }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .background(PrimaryGreen, ExpressiveSquircleIconShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.SmartToy, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Trợ lý AI", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF166534))
                            Text("Chat & Quét bill", fontSize = 11.sp, color = Color(0xFF15803D))
                        }
                    }

                    // Investments Card
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xFFEFF6FF), ExpressiveCardShape)
                            .border(1.dp, Color(0xFFBFDBFE), ExpressiveCardShape)
                            .clip(ExpressiveCardShape)
                            .clickable { onNavigateToInvestments() }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .background(Color(0xFF2563EB), ExpressiveSquircleIconShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Đầu tư", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF1E40AF))
                            Text("Vàng, Cổ phiếu...", fontSize = 11.sp, color = Color(0xFF2563EB))
                        }
                    }
                }
            }

            // AI Bill Splitting Banner (Không gian chia tiền nhóm cùng AI)
            item(key = "split_bill_banner", contentType = "split_bill_banner") {
                val splitBannerGradient = remember {
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF4338CA), Color(0xFF6D28D9), Color(0xFF9333EA))
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(splitBannerGradient, ExpressiveCardShape)
                        .clip(ExpressiveCardShape)
                        .clickable { onNavigateToSplitBill() }
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(Color.White.copy(alpha = 0.22f), ExpressiveSquircleIconShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Groups,
                                contentDescription = "Chia tiền",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "⚡ TÍNH NĂNG MỚI • AI",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    modifier = Modifier
                                        .background(Color.White.copy(alpha = 0.22f), ExpressivePillShape)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                "Chia tiền nhóm cùng AI",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                "Tính ai nợ ai sòng phẳng & tự lưu chi tiêu cá nhân",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.88f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // My Wallets & Accounts Section (Tiền mặt, Ngân hàng, Ví điện tử...)
            item(key = "accounts_section", contentType = "accounts_section") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Nguồn tiền & Ví (${accounts.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = onNavigateToAccounts) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = PrimaryGreen)
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("Thêm ví", fontWeight = FontWeight.Bold, color = PrimaryGreen, fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    accounts.forEach { acc ->
                        AccountQuickCard(
                            account = acc,
                            isSelected = (selectedFilterAccountId == acc.id),
                            onClick = { selectedAccountForDetail = acc }
                        )
                    }

                    // Add new wallet card
                    Column(
                        modifier = Modifier
                            .width(130.dp)
                            .height(88.dp)
                            .background(Color.White, ExpressiveItemShape)
                            .border(1.dp, Color(0xFFE2E8F0), ExpressiveItemShape)
                            .clip(ExpressiveItemShape)
                            .clickable { onNavigateToAccounts() }
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(PrimaryContainer, ExpressiveSquircleIconShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Thêm ví mới", fontSize = 11.sp, color = Color(0xFF64748B), fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Budget Alert / Progress Card (if budget is set)
            monthlyBudget?.let { budget ->
                item(key = "budget_card", contentType = "budget_card") {
                    BudgetSummaryCard(
                        spent = monthExpense,
                        limit = budget.amountLimit
                    )
                }
            }

            // Recent Transactions & Date Filter Header
            item(key = "tx_filter_header", contentType = "tx_filter_header") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Lịch sử giao dịch",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            val subtitle = buildString {
                                append("${filteredTransactions.size} giao dịch")
                                if (selectedTransactionType != null) {
                                    append(if (selectedTransactionType == TransactionType.EXPENSE) " • Chi tiêu" else " • Thu nhập")
                                }
                                if (selectedFilterAccountId != null) {
                                    append(" • Ví: ${accountMap[selectedFilterAccountId]?.name ?: ""}")
                                }
                                if (selectedFilterDate != null) {
                                    append(" • Ngày ${DateUtils.formatDate(selectedFilterDate!!)}")
                                } else if (selectedFilterAccountId == null && selectedTransactionType == null) {
                                    append(" • Toàn bộ các ngày")
                                }
                            }
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF64748B)
                            )
                        }

                        if (selectedFilterDate != null || selectedFilterAccountId != null || selectedTransactionType != null) {
                            TextButton(
                                onClick = {
                                    selectedFilterDate = null
                                    selectedFilterAccountId = null
                                    selectedTransactionType = null
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Xem tất cả",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryGreen
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Transaction Type Switcher: Tất cả / Chi tiêu (-) / Thu nhập (+) (M3 Expressive Pill Segmented)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .clip(ExpressivePillShape)
                            .background(Color(0xFFF1F5F9))
                            .padding(3.dp)
                    ) {
                        // 1. Tất cả
                        val isAllType = selectedTransactionType == null
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(ExpressivePillShape)
                                .background(if (isAllType) Color.White else Color.Transparent)
                                .clickable { selectedTransactionType = null },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Tất cả",
                                fontSize = 12.sp,
                                fontWeight = if (isAllType) FontWeight.Bold else FontWeight.Medium,
                                color = if (isAllType) PrimaryGreenDark else Color(0xFF64748B)
                            )
                        }

                        // 2. Chi tiêu (-)
                        val isExpenseSelected = selectedTransactionType == TransactionType.EXPENSE
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(ExpressivePillShape)
                                .background(if (isExpenseSelected) ExpenseRed else Color.Transparent)
                                .clickable {
                                    selectedTransactionType = if (isExpenseSelected) null else TransactionType.EXPENSE
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.TrendingDown,
                                    contentDescription = null,
                                    tint = if (isExpenseSelected) Color.White else ExpenseRed,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Chi tiêu",
                                    fontSize = 12.sp,
                                    fontWeight = if (isExpenseSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isExpenseSelected) Color.White else Color(0xFF64748B)
                                )
                            }
                        }

                        // 3. Thu nhập (+)
                        val isIncomeSelected = selectedTransactionType == TransactionType.INCOME
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(ExpressivePillShape)
                                .background(if (isIncomeSelected) IncomeGreen else Color.Transparent)
                                .clickable {
                                    selectedTransactionType = if (isIncomeSelected) null else TransactionType.INCOME
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.TrendingUp,
                                    contentDescription = null,
                                    tint = if (isIncomeSelected) Color.White else IncomeGreen,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Thu nhập",
                                    fontSize = 12.sp,
                                    fontWeight = if (isIncomeSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isIncomeSelected) Color.White else Color(0xFF64748B)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Date Filter Chips Row
                    val isAllSelected = selectedFilterDate == null
                    val isTodaySelected = selectedFilterDate == todayStart
                    val isYesterdaySelected = selectedFilterDate == yesterdayStart
                    val isCustomDate = selectedFilterDate != null && !isTodaySelected && !isYesterdaySelected

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Tất cả
                        FilterChip(
                            selected = isAllSelected,
                            onClick = { selectedFilterDate = null },
                            label = { Text("Tất cả", fontSize = 12.sp, fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryGreen,
                                selectedLabelColor = Color.White,
                                containerColor = Color.White
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isAllSelected,
                                borderColor = if (isAllSelected) PrimaryGreen else Color(0xFFCBD5E1)
                            )
                        )

                        // 2. Hôm nay
                        FilterChip(
                            selected = isTodaySelected,
                            onClick = {
                                selectedFilterDate = if (isTodaySelected) null else todayStart
                            },
                            label = { Text("Hôm nay", fontSize = 12.sp, fontWeight = if (isTodaySelected) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryGreen,
                                selectedLabelColor = Color.White,
                                containerColor = Color.White
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isTodaySelected,
                                borderColor = if (isTodaySelected) PrimaryGreen else Color(0xFFCBD5E1)
                            )
                        )

                        // 3. Hôm qua
                        FilterChip(
                            selected = isYesterdaySelected,
                            onClick = {
                                selectedFilterDate = if (isYesterdaySelected) null else yesterdayStart
                            },
                            label = { Text("Hôm qua", fontSize = 12.sp, fontWeight = if (isYesterdaySelected) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryGreen,
                                selectedLabelColor = Color.White,
                                containerColor = Color.White
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isYesterdaySelected,
                                borderColor = if (isYesterdaySelected) PrimaryGreen else Color(0xFFCBD5E1)
                            )
                        )

                        // 4. Chọn ngày cụ thể
                        FilterChip(
                            selected = isCustomDate,
                            onClick = {
                                showDatePicker()
                            },
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.CalendarToday,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                        tint = if (isCustomDate) Color.White else Color(0xFF475569)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isCustomDate) DateUtils.formatDate(selectedFilterDate!!) else "Chọn ngày 📅",
                                        fontSize = 12.sp,
                                        fontWeight = if (isCustomDate) FontWeight.Bold else FontWeight.Normal
                                    )
                                    if (isCustomDate) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Hủy chọn ngày",
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clickable { selectedFilterDate = null },
                                            tint = Color.White
                                        )
                                    }
                                }
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryGreen,
                                selectedLabelColor = Color.White,
                                containerColor = Color.White
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isCustomDate,
                                borderColor = if (isCustomDate) PrimaryGreen else Color(0xFFCBD5E1)
                            )
                        )

                        // 5. Filter theo Nguồn tiền / Ví (nếu đang chọn)
                        val filteredAccount = accountMap[selectedFilterAccountId]
                        if (filteredAccount != null) {
                            FilterChip(
                                selected = true,
                                onClick = { selectedFilterAccountId = null },
                                shape = ExpressivePillShape,
                                label = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Ví: ${filteredAccount.name}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Hủy lọc ví",
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimaryGreenDark,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }

            if (filteredTransactions.isEmpty()) {
                item(key = "empty_state", contentType = "empty_state") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.EventBusy,
                                contentDescription = null,
                                tint = Color(0xFFCBD5E1),
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (selectedFilterDate != null)
                                    "Không có giao dịch nào trong ngày này"
                                else
                                    "Chưa có giao dịch nào",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color(0xFF94A3B8)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            if (selectedFilterDate != null) {
                                TextButton(onClick = { selectedFilterDate = null }) {
                                    Text("Xem lại toàn bộ các ngày", color = PrimaryGreen, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Text(
                                    text = "Bấm dấu (+) bên dưới để thêm khoản chi đầu tiên",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFFCBD5E1)
                                )
                            }
                        }
                    }
                }
            } else {
                dayGroups.forEach { group ->
                    item(
                        key = "day_header_${group.dayTimestamp}",
                        contentType = "day_header"
                    ) {
                        DayGroupHeader(
                            dayTimestamp = group.dayTimestamp,
                            dateLabel = group.dateLabel,
                            totalExpense = group.dayExpense,
                            totalIncome = group.dayIncome,
                            count = group.transactions.size
                        )
                    }

                    items(
                        items = group.transactions,
                        key = { it.id },
                        contentType = { "tx_item" }
                    ) { tx ->
                        TransactionItem(
                            transaction = tx,
                            category = categoryMap[tx.categoryId],
                            account = accountMap[tx.accountId],
                            onDelete = { viewModel.deleteTransaction(tx) },
                            onClick = { editingTransaction = tx }
                        )
                    }
                }
            }

            item(key = "fab_spacer", contentType = "fab_spacer") {
                Spacer(modifier = Modifier.height(70.dp)) // padding for FAB
            }
        }
    }

    // Edit Transaction Dialog
    editingTransaction?.let { tx ->
        EditTransactionDialog(
            transaction = tx,
            categories = categories,
            accounts = accounts,
            onDismiss = { editingTransaction = null },
            onSave = { updatedTx ->
                viewModel.updateTransaction(tx, updatedTx)
                editingTransaction = null
            },
            onDelete = {
                viewModel.deleteTransaction(tx)
                editingTransaction = null
            }
        )
    }

    // Account Detail Bottom Sheet (Lịch sử thu / chi của nguồn tiền)
    selectedAccountForDetail?.let { acc ->
        AccountDetailBottomSheet(
            account = acc,
            allTransactions = transactions,
            categories = categories,
            onDismiss = { selectedAccountForDetail = null },
            onEditAccount = {
                selectedAccountForDetail = null
                onNavigateToAccounts()
            },
            onEditTransaction = { tx -> editingTransaction = tx },
            onDeleteTransaction = { tx -> viewModel.deleteTransaction(tx) },
            onFilterOnHomeScreen = { accId ->
                selectedFilterAccountId = accId
            }
        )
    }
}

@Composable
fun TotalBalanceCard(
    totalBalance: Double,
    reserveBalance: Double,
    availableBalance: Double,
    monthExpense: Double,
    monthIncome: Double,
    onManageAccounts: () -> Unit = {},
    onViewMonthIncome: () -> Unit = {},
    onViewMonthExpense: () -> Unit = {}
) {
    val heroGradient = remember {
        Brush.linearGradient(
            colors = listOf(Color(0xFF047857), Color(0xFF059669), Color(0xFF10B981))
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(heroGradient, ExpressiveHeroShape)
            .clip(ExpressiveHeroShape)
            .padding(22.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tổng số dư tài sản",
                    color = Color.White.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Row(
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.2f), ExpressivePillShape)
                        .clip(ExpressivePillShape)
                        .clickable { onManageAccounts() }
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Quản lý ví ›", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = CurrencyUtils.formatVnd(totalBalance),
                color = Color.White,
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.ExtraBold
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Breakdown: Dự trữ vs Khả dụng (M3 Expressive Tonal Pills)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Available Balance
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(Color.White.copy(alpha = 0.16f), ExpressiveItemShape)
                        .border(1.dp, Color.White.copy(alpha = 0.22f), ExpressiveItemShape)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text("💳 Khả dụng chi tiêu", color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        CurrencyUtils.formatVnd(availableBalance),
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp
                    )
                }

                // Reserve Balance
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(Color.White.copy(alpha = 0.16f), ExpressiveItemShape)
                        .border(1.dp, Color.White.copy(alpha = 0.22f), ExpressiveItemShape)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text("🛡️ Quỹ tiền dự trữ", color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        CurrencyUtils.formatVnd(reserveBalance),
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Income & Expense Summary (Clickable Pills to view transactions)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Income Card
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .background(Color.White.copy(alpha = 0.16f), ExpressiveItemShape)
                        .border(1.dp, Color.White.copy(alpha = 0.25f), ExpressiveItemShape)
                        .clip(ExpressiveItemShape)
                        .clickable { onViewMonthIncome() }
                        .padding(horizontal = 10.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(Color.White.copy(alpha = 0.22f), ExpressiveSquircleIconShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.TrendingUp,
                            contentDescription = "Thu nhập",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Thu trong tháng",
                                color = Color.White.copy(alpha = 0.88f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.75f),
                                modifier = Modifier.size(13.dp)
                            )
                        }
                        Text(
                            text = CurrencyUtils.formatVnd(monthIncome),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            maxLines = 1
                        )
                    }
                }

                // Expense Card
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .background(Color.White.copy(alpha = 0.16f), ExpressiveItemShape)
                        .border(1.dp, Color.White.copy(alpha = 0.25f), ExpressiveItemShape)
                        .clip(ExpressiveItemShape)
                        .clickable { onViewMonthExpense() }
                        .padding(horizontal = 10.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(Color.White.copy(alpha = 0.22f), ExpressiveSquircleIconShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.TrendingDown,
                            contentDescription = "Chi tiêu",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Chi trong tháng",
                                color = Color.White.copy(alpha = 0.88f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.75f),
                                modifier = Modifier.size(13.dp)
                            )
                        }
                        Text(
                            text = CurrencyUtils.formatVnd(monthExpense),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BudgetSummaryCard(
    spent: Double,
    limit: Double
) {
    val progress = if (limit > 0) (spent / limit).toFloat() else 0f
    val remaining = limit - spent
    val isOver = remaining < 0

    val barColor = when {
        isOver -> ExpenseRed
        progress > 0.8f -> WarningOrange
        else -> PrimaryGreen
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, ExpressiveCardShape)
            .border(1.dp, Color(0xFFF1F5F9), ExpressiveCardShape)
            .padding(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Ngân sách tháng này",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            if (isOver) {
                Row(
                    modifier = Modifier
                        .background(ExpenseRedContainer, ExpressivePillShape)
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = ExpenseRed, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Đã vượt mức!", color = ExpenseRed, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            } else {
                Text(
                    text = "Còn lại: ${CurrencyUtils.formatVnd(remaining)}",
                    color = PrimaryGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    modifier = Modifier
                        .background(PrimaryContainer, ExpressivePillShape)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }

            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(ExpressivePillShape),
                color = barColor,
                trackColor = Color(0xFFF1F5F9),
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Đã chi: ${CurrencyUtils.formatVnd(spent)} (${(progress * 100).toInt()}%)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF64748B)
                )
                Text(
                    text = "Hạn mức: ${CurrencyUtils.formatVnd(limit)}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF64748B)
                )
            }
    }
}

@Composable
fun AccountQuickCard(
    account: AccountEntity,
    isSelected: Boolean = false,
    onClick: () -> Unit
) {
    val accColor = parseColor(account.colorHex)
    val bgColor = if (isSelected) PrimaryContainer.copy(alpha = 0.5f) else Color.White
    val borderColor = if (isSelected) PrimaryGreen else Color(0xFFF1F5F9)
    val borderWidth = if (isSelected) 1.5.dp else 1.dp

    Column(
        modifier = Modifier
            .width(142.dp)
            .height(88.dp)
            .background(bgColor, ExpressiveItemShape)
            .border(borderWidth, borderColor, ExpressiveItemShape)
            .clip(ExpressiveItemShape)
            .clickable { onClick() }
            .padding(12.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(accColor.copy(alpha = 0.15f), ExpressiveSquircleIconShape),
                contentAlignment = Alignment.Center
            ) {
                CategoryIcon(
                    name = account.iconName,
                    tint = accColor,
                    modifier = Modifier.size(15.dp)
                )
            }
            if (account.isReserve || account.type == AccountType.RESERVE) {
                Text(
                    "🛡️ Dự trữ",
                    fontSize = 8.sp,
                    color = Color(0xFF0369A1),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .background(Color(0xFFE0F2FE), ExpressivePillShape)
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                )
            }
        }
        Column {
            Text(
                text = account.name,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = CurrencyUtils.formatVnd(account.currentBalance),
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                color = PrimaryGreen
            )
        }
    }
}

private data class DayTransactionGroup(
    val dayTimestamp: Long,
    val dateLabel: String,
    val dayExpense: Double,
    val dayIncome: Double,
    val transactions: List<TransactionEntity>
)
