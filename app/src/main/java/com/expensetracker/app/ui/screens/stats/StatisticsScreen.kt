package com.expensetracker.app.ui.screens.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.expensetracker.app.data.local.entity.CategoryEntity
import com.expensetracker.app.ui.components.CategoryIcon
import com.expensetracker.app.ui.components.ChartSlice
import com.expensetracker.app.ui.components.DonutPieChart
import com.expensetracker.app.ui.components.BarChartItem
import com.expensetracker.app.ui.components.ExpenseBarChart
import com.expensetracker.app.ui.theme.*
import com.expensetracker.app.data.local.dao.CategorySummary
import com.expensetracker.app.ui.viewmodel.ExpenseViewModel
import com.expensetracker.app.ui.viewmodel.TimePeriod
import com.expensetracker.app.ui.viewmodel.ComparisonPeriod
import com.expensetracker.app.utils.CurrencyUtils

@Composable
fun StatisticsScreen(
    viewModel: ExpenseViewModel
) {
    val windowSizeInfo = LocalWindowSizeInfo.current
    val selectedPeriod by viewModel.selectedPeriod.collectAsState()
    val totalExpense by viewModel.statsTotalExpense.collectAsState()
    val totalIncome by viewModel.statsTotalIncome.collectAsState()
    val categorySummaries by viewModel.statsCategorySummaries.collectAsState()
    val categories by viewModel.allCategories.collectAsState()

    val comparisonPeriod by viewModel.comparisonPeriod.collectAsState()
    val weeklyBars by viewModel.weeklyComparisonBars.collectAsState()
    val monthlyBars by viewModel.monthlyComparisonBars.collectAsState()

    val categoryMap = remember(categories) { categories.associateBy { it.id } }

    val chartSlices = remember(categorySummaries, categoryMap) {
        categorySummaries.map { summary ->
            val cat = categoryMap[summary.categoryId]
            ChartSlice(
                label = cat?.name ?: "Khác",
                value = summary.totalAmount,
                color = cat?.colorHex?.let { parseColor(it) } ?: PrimaryGreen
            )
        }
    }

    if (windowSizeInfo.isWideScreen) {
        // Dual-Pane Responsive Layout for Tablets / Wide Landscape Screens
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Left Pane: Overview (Period Selector, Totals, Bar Chart & Donut Chart)
            Column(
                modifier = Modifier
                    .weight(1.1f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                StatsHeaderSection(
                    selectedPeriod = selectedPeriod,
                    onPeriodSelected = { viewModel.setPeriod(it) }
                )

                StatsTotalsRow(
                    totalExpense = totalExpense,
                    totalIncome = totalIncome
                )

                // Biểu đồ cột thể hiện mức độ chi tiêu giữa các tuần/tháng
                StatsBarChartCard(
                    comparisonPeriod = comparisonPeriod,
                    weeklyBars = weeklyBars,
                    monthlyBars = monthlyBars,
                    onPeriodChanged = { viewModel.setComparisonPeriod(it) }
                )

                StatsChartCard(
                    chartSlices = chartSlices,
                    totalExpense = totalExpense
                )

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Right Pane: Detailed Category Breakdown
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                Text(
                    text = "Chi tiết theo danh mục",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                if (categorySummaries.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Chưa có chi tiêu nào trong khoảng thời gian này",
                            color = Color(0xFF94A3B8),
                            fontSize = 13.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(categorySummaries) { summary ->
                            CategorySummaryCard(
                                summary = summary,
                                category = categoryMap[summary.categoryId],
                                totalExpense = totalExpense
                            )
                        }
                        item {
                            Spacer(modifier = Modifier.height(30.dp))
                        }
                    }
                }
            }
        }
    } else {
        // Single-Column Layout for Mobile Phones & Compact Screens
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(max = 680.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    StatsHeaderSection(
                        selectedPeriod = selectedPeriod,
                        onPeriodSelected = { viewModel.setPeriod(it) }
                    )
                }

                item {
                    StatsTotalsRow(
                        totalExpense = totalExpense,
                        totalIncome = totalIncome
                    )
                }

                // Biểu đồ cột: Mức độ chi tiêu giữa các tuần/tháng
                item {
                    StatsBarChartCard(
                        comparisonPeriod = comparisonPeriod,
                        weeklyBars = weeklyBars,
                        monthlyBars = monthlyBars,
                        onPeriodChanged = { viewModel.setComparisonPeriod(it) }
                    )
                }

                // Biểu đồ tròn: Cơ cấu theo danh mục
                item {
                    StatsChartCard(
                        chartSlices = chartSlices,
                        totalExpense = totalExpense
                    )
                }

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
                        CategorySummaryCard(
                            summary = summary,
                            category = categoryMap[summary.categoryId],
                            totalExpense = totalExpense
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp)) // padding for bottom bar
                }
            }
        }
    }
}

@Composable
private fun StatsHeaderSection(
    selectedPeriod: TimePeriod,
    onPeriodSelected: (TimePeriod) -> Unit
) {
    Column {
        Text(
            text = "Báo cáo thống kê",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Time Period Selector (M3 Expressive Pill Segmented)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .clip(ExpressivePillShape)
                .background(Color(0xFFF1F5F9))
                .padding(3.dp)
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
                        .clip(ExpressivePillShape)
                        .background(if (isSelected) Color.White else Color.Transparent)
                        .clickable { onPeriodSelected(period) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) PrimaryGreenDark else Color(0xFF64748B),
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun StatsTotalsRow(
    totalExpense: Double,
    totalIncome: Double
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Expense Card
        Card(
            modifier = Modifier.weight(1f),
            shape = ExpressiveCardShape,
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(1.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Tổng chi tiêu", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF64748B))
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    CurrencyUtils.formatVnd(totalExpense),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = ExpenseRed
                )
            }
        }

        // Income Card
        Card(
            modifier = Modifier.weight(1f),
            shape = ExpressiveCardShape,
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(1.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Tổng thu nhập", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF64748B))
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    CurrencyUtils.formatVnd(totalIncome),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = IncomeGreen
                )
            }
        }
    }
}

/**
 * Thẻ biểu đồ cột so sánh chi tiêu giữa các tuần trong tháng hoặc các tháng trong năm
 */
@Composable
private fun StatsBarChartCard(
    comparisonPeriod: ComparisonPeriod,
    weeklyBars: List<BarChartItem>,
    monthlyBars: List<BarChartItem>,
    onPeriodChanged: (ComparisonPeriod) -> Unit
) {
    val items = if (comparisonPeriod == ComparisonPeriod.WEEKLY) weeklyBars else monthlyBars
    val totalPeriodExpense = items.sumOf { it.value }
    val avgExpense = if (items.isNotEmpty()) totalPeriodExpense / items.size else 0.0

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = ExpressiveHeroShape,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header Row: Tiêu đề và nút chuyển đổi [Tuần | Tháng]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Mức độ chi tiêu",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (comparisonPeriod == ComparisonPeriod.WEEKLY) "So sánh các tuần trong tháng" else "So sánh 6 tháng gần nhất",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                }

                // Nút chuyển đổi viên thuốc nhỏ: Tuần | Tháng
                Row(
                    modifier = Modifier
                        .height(34.dp)
                        .clip(ExpressivePillShape)
                        .background(Color(0xFFF1F5F9))
                        .padding(2.dp)
                ) {
                    val isWeekly = comparisonPeriod == ComparisonPeriod.WEEKLY
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .clip(ExpressivePillShape)
                            .background(if (isWeekly) Color.White else Color.Transparent)
                            .clickable { onPeriodChanged(ComparisonPeriod.WEEKLY) }
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Tuần",
                            fontSize = 11.sp,
                            fontWeight = if (isWeekly) FontWeight.Bold else FontWeight.Medium,
                            color = if (isWeekly) PrimaryGreenDark else Color(0xFF64748B)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .clip(ExpressivePillShape)
                            .background(if (!isWeekly) Color.White else Color.Transparent)
                            .clickable { onPeriodChanged(ComparisonPeriod.MONTHLY) }
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Tháng",
                            fontSize = 11.sp,
                            fontWeight = if (!isWeekly) FontWeight.Bold else FontWeight.Medium,
                            color = if (!isWeekly) PrimaryGreenDark else Color(0xFF64748B)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Dòng thông tin tổng & trung bình
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF8FAFC))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Trung bình: ${CurrencyUtils.formatCompactVnd(avgExpense)} / ${if (comparisonPeriod == ComparisonPeriod.WEEKLY) "tuần" else "tháng"}",
                    fontSize = 12.sp,
                    color = Color(0xFF475569),
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Tổng: ${CurrencyUtils.formatCompactVnd(totalPeriodExpense)}",
                    fontSize = 12.sp,
                    color = PrimaryGreenDark,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Biểu đồ cột động
            ExpenseBarChart(
                items = items,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun StatsChartCard(
    chartSlices: List<ChartSlice>,
    totalExpense: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = ExpressiveHeroShape,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Phân bổ chi tiêu",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(14.dp))

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

@Composable
private fun CategorySummaryCard(
    summary: CategorySummary,
    category: CategoryEntity?,
    totalExpense: Double
) {
    val catColor = category?.colorHex?.let { parseColor(it) } ?: PrimaryGreen
    val percentage = if (totalExpense > 0) (summary.totalAmount / totalExpense * 100).toInt() else 0

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = ExpressiveItemShape,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(ExpressiveSquircleIconShape)
                    .background(catColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                CategoryIcon(
                    name = category?.iconName ?: "Category",
                    tint = catColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category?.name ?: "Khác",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { (percentage / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(ExpressivePillShape),
                    color = catColor,
                    trackColor = Color(0xFFF1F5F9)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = CurrencyUtils.formatVnd(summary.totalAmount),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    color = TextPrimary
                )
                Text(
                    text = "$percentage%",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64748B)
                )
            }
        }
    }
}
