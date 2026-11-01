package com.expensetracker.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.expensetracker.app.data.local.entity.AccountEntity
import com.expensetracker.app.data.local.entity.CategoryEntity
import com.expensetracker.app.data.local.entity.TransactionEntity
import com.expensetracker.app.data.local.entity.TransactionType
import com.expensetracker.app.ui.theme.*
import com.expensetracker.app.utils.CurrencyUtils
import com.expensetracker.app.utils.DateUtils

@Composable
fun TransactionItem(
    transaction: TransactionEntity,
    category: CategoryEntity?,
    account: AccountEntity?,
    onDelete: () -> Unit,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isExpense = transaction.type == TransactionType.EXPENSE
    val categoryColor = category?.colorHex?.let { parseColor(it) } ?: Color(0xFF64748B)
    val categoryName = category?.name ?: "Khác"

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(ExpressiveItemShape)
            .clickable(enabled = onClick != null) { onClick?.invoke() },
        shape = ExpressiveItemShape,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // M3 Expressive Squircle Icon Container
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(ExpressiveSquircleIconShape)
                    .background(categoryColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                CategoryIcon(
                    name = category?.iconName ?: "Category",
                    tint = categoryColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Transaction Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = categoryName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                if (transaction.note.isNotBlank()) {
                    Text(
                        text = transaction.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 1
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Account Pill Badge (Nguồn tiền)
                    val accColor = account?.colorHex?.let { parseColor(it) } ?: Color(0xFF059669)
                    Surface(
                        shape = ExpressivePillShape,
                        color = accColor.copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(0.6.dp, accColor.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CategoryIcon(
                                name = account?.iconName ?: "AccountBalanceWallet",
                                tint = accColor,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = account?.name ?: "Ví tiền",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = accColor
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "• ${DateUtils.formatTime(transaction.timestamp)}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextTertiary
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Amount & Actions
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                val prefix = if (isExpense) "- " else "+ "
                val amountColor = if (isExpense) ExpenseRed else IncomeGreen
                Text(
                    text = "$prefix${CurrencyUtils.formatVnd(transaction.amount)}",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    color = amountColor
                )
                Spacer(modifier = Modifier.height(2.dp))
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Xóa",
                        tint = TextTertiary,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }
    }
}
