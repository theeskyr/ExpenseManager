package com.expensetracker.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.expensetracker.app.data.local.entity.AccountEntity
import com.expensetracker.app.data.local.entity.AccountType
import com.expensetracker.app.data.local.entity.CategoryEntity
import com.expensetracker.app.data.local.entity.TransactionEntity
import com.expensetracker.app.data.local.entity.TransactionType
import com.expensetracker.app.ui.theme.*
import com.expensetracker.app.utils.CurrencyUtils
import com.expensetracker.app.utils.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDetailBottomSheet(
    account: AccountEntity,
    allTransactions: List<TransactionEntity>,
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onEditAccount: (AccountEntity) -> Unit,
    onEditTransaction: (TransactionEntity) -> Unit,
    onDeleteTransaction: (TransactionEntity) -> Unit,
    onFilterOnHomeScreen: ((Long) -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val accountTransactions = remember(allTransactions, account.id) {
        allTransactions.filter { it.accountId == account.id }
    }

    val totalIncome = remember(accountTransactions) {
        accountTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
    }
    val incomeCount = remember(accountTransactions) {
        accountTransactions.count { it.type == TransactionType.INCOME }
    }

    val totalExpense = remember(accountTransactions) {
        accountTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
    }
    val expenseCount = remember(accountTransactions) {
        accountTransactions.count { it.type == TransactionType.EXPENSE }
    }

    val groupedTransactions = remember(accountTransactions) {
        accountTransactions.groupBy { DateUtils.getStartOfDay(it.timestamp) }
    }

    val categoryMap = remember(categories) { categories.associateBy { it.id } }
    val accColor = parseColor(account.colorHex)

    val typeLabel = when (account.type) {
        AccountType.CASH -> "Tiền mặt"
        AccountType.BANK -> "Tài khoản ngân hàng"
        AccountType.E_WALLET -> "Ví điện tử"
        AccountType.CREDIT_CARD -> "Thẻ tín dụng"
        AccountType.RESERVE -> "Quỹ dự phòng khẩn cấp"
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = Color.White,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header: Account Info & Actions
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(ExpressiveSquircleIconShape)
                                .background(accColor.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CategoryIcon(
                                name = account.iconName,
                                tint = accColor,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = account.name,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = ExpressivePillShape,
                                    color = Color(0xFFF1F5F9)
                                ) {
                                    Text(
                                        text = typeLabel,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF475569),
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                    )
                                }
                                if (account.isReserve || account.type == AccountType.RESERVE) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = ExpressivePillShape,
                                        color = Color(0xFFE0F2FE)
                                    ) {
                                        Text(
                                            "🛡️ Dự trữ",
                                            fontSize = 10.sp,
                                            color = Color(0xFF0369A1),
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Row {
                        IconButton(onClick = { onEditAccount(account) }) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Chỉnh sửa nguồn tiền",
                                tint = PrimaryGreen
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Đóng",
                                tint = Color(0xFF64748B)
                            )
                        }
                    }
                }
            }

            // Hero Card: Current Balance & Income/Expense Stats
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = ExpressiveHeroShape,
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    elevation = CardDefaults.cardElevation(0.5.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Column {
                            Text(
                                text = "Số dư hiện tại",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF64748B)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = CurrencyUtils.formatVnd(account.currentBalance),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = PrimaryGreen
                            )
                        }

                        // Stats Row: Total Inflow vs Total Outflow
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Total Inflow (Thu vào)
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = ExpressiveItemShape,
                                color = IncomeGreenContainer
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.TrendingUp,
                                            contentDescription = null,
                                            tint = IncomeGreen,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            "Thu vào ($incomeCount)",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF065F46)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "+${CurrencyUtils.formatVnd(totalIncome)}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = IncomeGreen
                                    )
                                }
                            }

                            // Total Outflow (Chi ra)
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = ExpressiveItemShape,
                                color = ExpenseRedContainer
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.TrendingDown,
                                            contentDescription = null,
                                            tint = ExpenseRed,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            "Chi ra ($expenseCount)",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF991B1B)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "-${CurrencyUtils.formatVnd(totalExpense)}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ExpenseRed
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Quick Actions: Edit Account / Filter on Home Screen
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (onFilterOnHomeScreen != null) {
                        FilledTonalButton(
                            onClick = {
                                onFilterOnHomeScreen(account.id)
                                onDismiss()
                            },
                            shape = ExpressivePillShape,
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 10.dp)
                        ) {
                            Icon(
                                Icons.Default.FilterAlt,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Lọc ở Trang chủ",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = { onEditAccount(account) },
                        shape = ExpressivePillShape,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 10.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Sửa số dư / ví",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Section Header: Lịch sử thu / chi
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Lịch sử thu / chi",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Surface(
                        shape = ExpressivePillShape,
                        color = Color(0xFFF1F5F9)
                    ) {
                        Text(
                            text = "${accountTransactions.size} giao dịch",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64748B),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            // Empty state or grouped transaction items
            if (accountTransactions.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(ExpressiveSquircleIconShape)
                                .background(Color(0xFFF1F5F9)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.ReceiptLong,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Chưa có giao dịch thu / chi nào",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFF334155)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Các khoản thu hoặc chi từ nguồn tiền này sẽ được lưu và hiển thị tại đây.",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }
            } else {
                groupedTransactions.forEach { (dayTimestamp, dayTxs) ->
                    val dayExpense = dayTxs.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
                    val dayIncome = dayTxs.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }

                    item(key = "day_$dayTimestamp") {
                        DayGroupHeader(
                            dayTimestamp = dayTimestamp,
                            totalExpense = dayExpense,
                            totalIncome = dayIncome,
                            count = dayTxs.size
                        )
                    }

                    items(dayTxs, key = { it.id }) { tx ->
                        TransactionItem(
                            transaction = tx,
                            category = categoryMap[tx.categoryId],
                            account = account,
                            onDelete = { onDeleteTransaction(tx) },
                            onClick = { onEditTransaction(tx) }
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }
}
