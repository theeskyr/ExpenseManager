package com.expensetracker.app.ui.screens.budget

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.expensetracker.app.ui.theme.*
import com.expensetracker.app.ui.viewmodel.ExpenseViewModel
import com.expensetracker.app.utils.CurrencyUtils
import java.util.Calendar

@Composable
fun BudgetScreen(
    viewModel: ExpenseViewModel
) {
    val currentMonthExpense by viewModel.currentMonthExpense.collectAsState()
    val totalMonthlyBudget by viewModel.totalMonthlyBudget.collectAsState()

    var showSetBudgetDialog by remember { mutableStateOf(false) }
    var budgetInput by remember { mutableStateOf("") }

    val currentBudget = totalMonthlyBudget?.amountLimit ?: 0.0
    val progress = if (currentBudget > 0) (currentMonthExpense / currentBudget).toFloat() else 0f
    val remaining = currentBudget - currentMonthExpense
    val isOver = remaining < 0

    // Remaining days in month
    val calendar = Calendar.getInstance()
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
    val remainingDays = (daysInMonth - currentDay + 1).coerceAtLeast(1)
    val dailyAllowance = if (remaining > 0) remaining / remainingDays else 0.0

    ResponsiveContentContainer(maxWidth = 720.dp) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Quản lý ngân sách",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = {
                        budgetInput = if (currentBudget > 0) CurrencyUtils.formatInputAmount(currentBudget.toLong().toString()) else ""
                        showSetBudgetDialog = true
                    }
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Sửa ngân sách", tint = PrimaryGreen)
                }
            }
        }

        // Main Budget Status Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = ExpressiveHeroShape,
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(1.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9))
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Ngân sách tháng ${calendar.get(Calendar.MONTH) + 1}/${calendar.get(Calendar.YEAR)}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF64748B)
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    if (currentBudget > 0) {
                        Text(
                            text = CurrencyUtils.formatVnd(currentBudget),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary
                        )
                    } else {
                        Text(
                            text = "Chưa thiết lập",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF94A3B8)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                budgetInput = ""
                                showSetBudgetDialog = true
                            },
                            shape = ExpressivePillShape,
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                        ) {
                            Text("Đặt hạn mức chi tiêu", fontWeight = FontWeight.Bold)
                        }
                    }

                    if (currentBudget > 0) {
                        Spacer(modifier = Modifier.height(20.dp))

                        // Progress Bar (M3 Expressive Pill Progress)
                        val barColor = when {
                            isOver -> ExpenseRed
                            progress > 0.8f -> WarningOrange
                            else -> PrimaryGreen
                        }

                        LinearProgressIndicator(
                            progress = { progress.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .clip(ExpressivePillShape),
                            color = barColor,
                            trackColor = Color(0xFFF1F5F9)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Đã tiêu", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF64748B))
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    CurrencyUtils.formatVnd(currentMonthExpense),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = ExpenseRed
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(if (isOver) "Vượt mức" else "Còn lại", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF64748B))
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    CurrencyUtils.formatVnd(if (isOver) -remaining else remaining),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isOver) ExpenseRed else IncomeGreen
                                )
                            }
                        }
                    }
                }
            }
        }

        // Daily Allowance Recommendation Card
        if (currentBudget > 0) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = ExpressiveCardShape,
                    colors = CardDefaults.cardColors(
                        containerColor = if (isOver) Color(0xFFFEF2F2) else Color(0xFFF0FDF4)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isOver) Color(0xFFFECDD3) else Color(0xFFBBF7D0)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(ExpressiveSquircleIconShape)
                                .background(if (isOver) ExpenseRedContainer else PrimaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isOver) Icons.Default.Warning else Icons.Default.Check,
                                contentDescription = null,
                                tint = if (isOver) ExpenseRed else IncomeGreen,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            if (isOver) {
                                Text(
                                    text = "Bạn đã vượt hạn mức tháng!",
                                    fontWeight = FontWeight.Bold,
                                    color = ExpenseRed,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "Hãy cân nhắc cắt giảm các khoản chi không cần thiết cho những ngày còn lại.",
                                    fontSize = 12.sp,
                                    color = Color(0xFF7F1D1D)
                                )
                            } else {
                                Text(
                                    text = "Khuyến nghị chi tiêu mỗi ngày",
                                    fontWeight = FontWeight.Bold,
                                    color = IncomeGreen,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "Còn $remainingDays ngày, bạn có thể chi tối đa ${CurrencyUtils.formatVnd(dailyAllowance)} / ngày để đảm bảo đúng kế hoạch.",
                                    fontSize = 12.sp,
                                    color = Color(0xFF14532D)
                                )
                            }
                        }
                    }
                }
            }
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    // Set Budget Dialog
    if (showSetBudgetDialog) {
        AlertDialog(
            onDismissRequest = { showSetBudgetDialog = false },
            shape = ExpressiveDialogShape,
            title = { Text("Đặt hạn mức ngân sách tháng", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "Nhập số tiền ngân sách tối đa bạn muốn chi trong tháng:",
                        fontSize = 13.sp,
                        color = Color(0xFF64748B)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = budgetInput,
                        onValueChange = { input ->
                            budgetInput = CurrencyUtils.formatInputAmount(input)
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        placeholder = { Text("Ví dụ: 10.000.000") },
                        trailingIcon = { Text("₫", fontWeight = FontWeight.Bold) },
                        shape = ExpressiveInputShape,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = CurrencyUtils.parseAmount(budgetInput)
                        if (amount > 0) {
                            viewModel.setBudget(amount)
                        }
                        showSetBudgetDialog = false
                    },
                    shape = ExpressivePillShape,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                ) {
                    Text("Lưu", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSetBudgetDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }
}
}
