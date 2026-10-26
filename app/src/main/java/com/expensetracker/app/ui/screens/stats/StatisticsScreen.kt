package com.expensetracker.app.ui.screens.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.expensetracker.app.ui.components.CategoryIcon
import com.expensetracker.app.ui.components.ChartSlice
import com.expensetracker.app.ui.components.DonutPieChart
import com.expensetracker.app.ui.theme.*
import com.expensetracker.app.ui.viewmodel.ExpenseViewModel
import com.expensetracker.app.ui.viewmodel.TimePeriod
import com.expensetracker.app.utils.CurrencyUtils

@Composable
fun StatisticsScreen(
    viewModel: ExpenseViewModel
) {
    val selectedPeriod by viewModel.selectedPeriod.collectAsState()
    val totalExpense by viewModel.statsTotalExpense.collectAsState()
    val totalIncome by viewModel.statsTotalIncome.collectAsState()
    val categorySummaries by viewModel.statsCategorySummaries.collectAsState()
    val categories by viewModel.allCategories.collectAsState()

    val categoryMap = categories.associateBy { it.id }

    val chartSlices = categorySummaries.map { summary ->
        val cat = categoryMap[summary.categoryId]
        ChartSlice(
            label = cat?.name ?: "Khác",
            value = summary.totalAmount,
            color = cat?.colorHex?.let { parseColor(it) } ?: PrimaryGreen
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Báo cáo thống kê",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Time Period Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF1F5F9))
                    .padding(4.dp)
            ) {
                listOf(
                    TimePeriod.THIS_WEEK to "Tuần này",
                    TimePeriod.THIS_MONTH to "Tháng này",
                    TimePeriod.THIS_YEAR to "Năm này"
                ).forEach { (period, title) ->
                    val isSelected = selectedPeriod == period
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) Color.White else Color.Transparent)
                            .clickable { viewModel.setPeriod(period) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) PrimaryGreenDark else Color(0xFF64748B),
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // Summary Cards: Expense vs Income
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Expense
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Tổng chi tiêu", fontSize = 12.sp, color = Color(0xFF64748B))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            CurrencyUtils.formatVnd(totalExpense),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = ExpenseRed
                        )
                    }
                }

                // Income
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Tổng thu nhập", fontSize = 12.sp, color = Color(0xFF64748B))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            CurrencyUtils.formatVnd(totalIncome),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = IncomeGreen
                        )
                    }
                }
            }
        }

        // Donut Chart Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Phân bổ chi tiêu",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    DonutPieChart(
                        slices = chartSlices,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                        centerTitle = "Tổng chi",
                        totalAmount = totalExpense
                    )
                }
            }
        }

        // Detailed Category Breakdown
        item {
            Text(
                text = "Chi tiết theo danh mục",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (categorySummaries.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Chưa có chi tiêu nào trong khoảng thời gian này",
                        color = Color(0xFF94A3B8),
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            items(categorySummaries) { summary ->
                val cat = categoryMap[summary.categoryId]
                val catColor = cat?.colorHex?.let { parseColor(it) } ?: PrimaryGreen
                val percentage = if (totalExpense > 0) (summary.totalAmount / totalExpense * 100).toInt() else 0

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(0.5.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(catColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CategoryIcon(
                                name = cat?.iconName ?: "Category",
                                tint = catColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = cat?.name ?: "Khác",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { (percentage / 100f).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = catColor,
                                trackColor = Color(0xFFF1F5F9)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = CurrencyUtils.formatVnd(summary.totalAmount),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = TextPrimary
                            )
                            Text(
                                text = "$percentage%",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}
