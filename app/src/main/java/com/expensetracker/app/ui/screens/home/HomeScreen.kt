package com.expensetracker.app.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import com.expensetracker.app.ui.components.CategoryIcon
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
    onNavigateToAccounts: () -> Unit = {}
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

    val categoryMap = categories.associateBy { it.id }
    val accountMap = accounts.associateBy { it.id }

    var editingTransaction by remember { mutableStateOf<TransactionEntity?>(null) }

    val context = LocalContext.current
    var selectedFilterDate by remember { mutableStateOf<Long?>(null) } // null = All days (default)

    val filteredTransactions = remember(transactions, selectedFilterDate) {
        if (selectedFilterDate == null) {
            transactions
        } else {
            transactions.filter { DateUtils.getStartOfDay(it.timestamp) == selectedFilterDate }
        }
    }

    val groupedTransactions = remember(filteredTransactions) {
        filteredTransactions.groupBy { DateUtils.getStartOfDay(it.timestamp) }
    }

    val datePickerDialog = remember(context, selectedFilterDate) {
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
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAdd,
                containerColor = PrimaryGreen,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Thêm giao dịch")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                // Header Total Balance Card with Reserve & Available Balance
                TotalBalanceCard(
                    totalBalance = totalBalance,
                    reserveBalance = reserveBalance,
                    availableBalance = availableBalance,
                    monthExpense = monthExpense,
                    monthIncome = monthIncome,
                    onManageAccounts = onNavigateToAccounts
                )
            }

            // Quick Actions: AI & Investments
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // AI Assistant Card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onNavigateToAi() },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBBF7D0))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryGreen),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.SmartToy, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Trợ lý AI", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF166534))
                                Text("Chat & Quét bill", fontSize = 10.sp, color = Color(0xFF15803D))
                            }
                        }
                    }

                    // Investments Card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onNavigateToInvestments() },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF2563EB)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.TrendingUp, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Đầu tư", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF1E40AF))
                                Text("Vàng, Cổ phiếu...", fontSize = 10.sp, color = Color(0xFF2563EB))
                            }
                        }
                    }
                }
            }

            // My Wallets & Accounts Section (Tiền mặt, Ngân hàng, Ví điện tử...)
            item {
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

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(accounts, key = { it.id }) { acc ->
                        AccountQuickCard(
                            account = acc,
                            onClick = onNavigateToAccounts
                        )
                    }

                    // Add new wallet card
                    item {
                        Surface(
                            modifier = Modifier
                                .width(120.dp)
                                .height(82.dp)
                                .clickable { onNavigateToAccounts() },
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFF8FAFC),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1))
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.AddCircleOutline, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Thêm nguồn tiền", fontSize = 11.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }

            // Budget Alert / Progress Card (if budget is set)
            monthlyBudget?.let { budget ->
                item {
                    BudgetSummaryCard(
                        spent = monthExpense,
                        limit = budget.amountLimit
                    )
                }
            }

            // Recent Transactions & Date Filter Header
            item {
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
                            val subtitle = if (selectedFilterDate == null) {
                                "${transactions.size} giao dịch • Toàn bộ các ngày"
                            } else {
                                "Ngày ${DateUtils.formatDate(selectedFilterDate!!)} • ${filteredTransactions.size} giao dịch"
                            }
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF64748B)
                            )
                        }

                        if (selectedFilterDate != null) {
                            TextButton(
                                onClick = { selectedFilterDate = null },
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

                    // Date Filter Chips Row
                    val todayStart = remember { DateUtils.getStartOfDay() }
                    val yesterdayStart = remember { DateUtils.getStartOfDay(System.currentTimeMillis() - 86400000L) }
                    val isAllSelected = selectedFilterDate == null
                    val isTodaySelected = selectedFilterDate == todayStart
                    val isYesterdaySelected = selectedFilterDate == yesterdayStart
                    val isCustomDate = selectedFilterDate != null && !isTodaySelected && !isYesterdaySelected

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Tất cả
                        item {
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
                        }

                        // 2. Hôm nay
                        item {
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
                        }

                        // 3. Hôm qua
                        item {
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
                        }

                        // 4. Chọn ngày cụ thể
                        item {
                            FilterChip(
                                selected = isCustomDate,
                                onClick = {
                                    datePickerDialog.show()
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
                        }
                    }
                }
            }

            if (filteredTransactions.isEmpty()) {
                item {
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
                groupedTransactions.forEach { (dayTimestamp, txList) ->
                    val dayExpense = txList.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
                    val dayIncome = txList.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }

                    item(key = "day_header_$dayTimestamp") {
                        DayGroupHeader(
                            dayTimestamp = dayTimestamp,
                            totalExpense = dayExpense,
                            totalIncome = dayIncome,
                            count = txList.size
                        )
                    }

                    items(txList, key = { it.id }) { tx ->
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

            item {
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
}

@Composable
fun TotalBalanceCard(
    totalBalance: Double,
    reserveBalance: Double,
    availableBalance: Double,
    monthExpense: Double,
    monthIncome: Double,
    onManageAccounts: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(PrimaryGreenDark, PrimaryGreen)
                    )
                )
                .padding(20.dp)
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
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Surface(
                        modifier = Modifier.clickable { onManageAccounts() },
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.2f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Quản lý ví ›", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = CurrencyUtils.formatVnd(totalBalance),
                    color = Color.White,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Breakdown: Dự trữ vs Khả dụng
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Available Balance
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        color = Color.White.copy(alpha = 0.18f)
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                            Text("💳 Khả dụng chi tiêu", color = Color.White.copy(alpha = 0.85f), fontSize = 10.sp)
                            Text(
                                CurrencyUtils.formatVnd(availableBalance),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }

                    // Reserve Balance
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        color = Color.White.copy(alpha = 0.18f)
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                            Text("🛡️ Quỹ tiền dự trữ", color = Color.White.copy(alpha = 0.85f), fontSize = 10.sp)
                            Text(
                                CurrencyUtils.formatVnd(reserveBalance),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Income & Expense Summary
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Income
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.TrendingUp,
                                contentDescription = "Thu nhập",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Thu trong tháng",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 11.sp
                            )
                            Text(
                                text = CurrencyUtils.formatVnd(monthIncome),
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        }
                    }

                    // Expense
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.TrendingDown,
                                contentDescription = "Chi tiêu",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Chi trong tháng",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 11.sp
                            )
                            Text(
                                text = CurrencyUtils.formatVnd(monthExpense),
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        }
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

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Ngân sách tháng này",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (isOver) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = ExpenseRed, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Đã vượt mức!", color = ExpenseRed, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                } else {
                    Text(
                        text = "Còn lại: ${CurrencyUtils.formatVnd(remaining)}",
                        color = Color(0xFF10B981),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = barColor,
                trackColor = Color(0xFFF1F5F9),
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Đã chi: ${CurrencyUtils.formatVnd(spent)} (${(progress * 100).toInt()}%)",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B)
                )
                Text(
                    text = "Hạn mức: ${CurrencyUtils.formatVnd(limit)}",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B)
                )
            }
        }
    }
}

@Composable
fun AccountQuickCard(
    account: AccountEntity,
    onClick: () -> Unit
) {
    val accColor = parseColor(account.colorHex)
    Surface(
        modifier = Modifier
            .width(135.dp)
            .height(82.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        shadowElevation = 1.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(accColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    CategoryIcon(
                        name = account.iconName,
                        tint = accColor,
                        modifier = Modifier.size(14.dp)
                    )
                }
                if (account.isReserve || account.type == AccountType.RESERVE) {
                    Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFE0F2FE)) {
                        Text("🛡️ Dự trữ", fontSize = 8.sp, color = Color(0xFF0369A1), fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp))
                    }
                }
            }
            Column {
                Text(
                    text = account.name,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = CurrencyUtils.formatVnd(account.currentBalance),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryGreen
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTransactionDialog(
    transaction: TransactionEntity,
    categories: List<CategoryEntity>,
    accounts: List<AccountEntity>,
    onDismiss: () -> Unit,
    onSave: (TransactionEntity) -> Unit,
    onDelete: () -> Unit
) {
    var editType by remember { mutableStateOf(transaction.type) }
    var editAmountText by remember { mutableStateOf(CurrencyUtils.formatInputAmount(transaction.amount.toLong().toString())) }
    var editCategoryId by remember { mutableStateOf(transaction.categoryId) }
    var editAccountId by remember { mutableStateOf(transaction.accountId) }
    var editNote by remember { mutableStateOf(transaction.note) }

    val filteredCategories = categories.filter { it.type == editType }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Chỉnh sửa giao dịch", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Type Switcher: Chi tiêu vs Thu nhập
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFF1F5F9))
                        .padding(2.dp)
                ) {
                    // Chi tiêu
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (editType == TransactionType.EXPENSE) ExpenseRed else Color.Transparent)
                            .clickable {
                                editType = TransactionType.EXPENSE
                                val firstExp = categories.firstOrNull { it.type == TransactionType.EXPENSE }
                                if (firstExp != null && filteredCategories.none { it.id == editCategoryId }) {
                                    editCategoryId = firstExp.id
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Chi tiêu (-)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (editType == TransactionType.EXPENSE) Color.White else Color(0xFF64748B)
                        )
                    }

                    // Thu nhập
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (editType == TransactionType.INCOME) IncomeGreen else Color.Transparent)
                            .clickable {
                                editType = TransactionType.INCOME
                                val firstInc = categories.firstOrNull { it.type == TransactionType.INCOME }
                                if (firstInc != null && filteredCategories.none { it.id == editCategoryId }) {
                                    editCategoryId = firstInc.id
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Thu nhập (+)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (editType == TransactionType.INCOME) Color.White else Color(0xFF64748B)
                        )
                    }
                }

                // Amount Input
                OutlinedTextField(
                    value = editAmountText,
                    onValueChange = { input -> editAmountText = CurrencyUtils.formatInputAmount(input) },
                    label = { Text("Số tiền (₫)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Category Picker
                Text("Danh mục:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF475569))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filteredCategories, key = { it.id }) { cat ->
                        val isSelected = editCategoryId == cat.id
                        val catColor = parseColor(cat.colorHex)
                        FilterChip(
                            selected = isSelected,
                            onClick = { editCategoryId = cat.id },
                            leadingIcon = {
                                CategoryIcon(
                                    name = cat.iconName,
                                    tint = if (isSelected) Color.White else catColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = { Text(cat.name, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = if (editType == TransactionType.EXPENSE) ExpenseRed else IncomeGreen,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                // Account / Wallet Picker
                Text("Nguồn tiền / Ví:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF475569))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(accounts, key = { it.id }) { acc ->
                        val isSelected = editAccountId == acc.id
                        FilterChip(
                            selected = isSelected,
                            onClick = { editAccountId = acc.id },
                            label = { Text(acc.name, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryGreen,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                // Note Input
                OutlinedTextField(
                    value = editNote,
                    onValueChange = { editNote = it },
                    label = { Text("Ghi chú") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = CurrencyUtils.parseAmount(editAmountText)
                    if (amount > 0) {
                        onSave(
                            transaction.copy(
                                amount = amount,
                                type = editType,
                                categoryId = editCategoryId,
                                accountId = editAccountId,
                                note = editNote
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
            ) {
                Text("Lưu thay đổi")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFEF4444))
                ) {
                    Text("Xóa")
                }
                TextButton(onClick = onDismiss) {
                    Text("Hủy")
                }
            }
        }
    )
}

@Composable
fun DayGroupHeader(
    dayTimestamp: Long,
    totalExpense: Double,
    totalIncome: Double,
    count: Int
) {
    val rawLabel = DateUtils.getHeaderDateString(dayTimestamp)
    val dateLabel = rawLabel.replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(Locale("vi", "VN")) else it.toString()
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 4.dp),
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFFF8FAFC),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = dateLabel,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF334155)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFE2E8F0)
                ) {
                    Text(
                        text = "$count",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF475569),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                    )
                }
            }

            // Daily totals (Thu / Chi)
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (totalIncome > 0) {
                    Text(
                        text = "+${CurrencyUtils.formatVnd(totalIncome)}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = IncomeGreen
                    )
                }
                if (totalIncome > 0 && totalExpense > 0) {
                    Text(
                        text = " • ",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
                if (totalExpense > 0) {
                    Text(
                        text = "-${CurrencyUtils.formatVnd(totalExpense)}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ExpenseRed
                    )
                }
            }
        }
    }
}
