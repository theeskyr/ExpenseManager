package com.expensetracker.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.expensetracker.app.utils.CurrencyUtils

data class ChartSlice(
    val label: String,
    val value: Double,
    val color: Color
)

@Composable
fun DonutPieChart(
    slices: List<ChartSlice>,
    modifier: Modifier = Modifier,
    centerTitle: String = "Tổng chi",
    totalAmount: Double = 0.0,
    strokeWidth: Dp = 28.dp
) {
    val total = slices.sumOf { it.value }
    val animationProgress = remember { Animatable(0f) }

    LaunchedEffect(slices) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800)
        )
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            val strokePx = strokeWidth.toPx()
            val diameter = size.minDimension - strokePx
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            val chartSize = Size(diameter, diameter)

            if (total == 0.0 || slices.isEmpty()) {
                // Empty circle
                drawArc(
                    color = Color(0xFFE2E8F0),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = chartSize,
                    style = Stroke(width = strokePx)
                )
            } else {
                var currentStartAngle = -90f
                for (slice in slices) {
                    val sweepAngle = ((slice.value / total) * 360f * animationProgress.value).toFloat()
                    if (sweepAngle > 0.5f) {
                        drawArc(
                            color = slice.color,
                            startAngle = currentStartAngle,
                            sweepAngle = sweepAngle - 1.5f, // slight gap
                            useCenter = false,
                            topLeft = topLeft,
                            size = chartSize,
                            style = Stroke(width = strokePx, cap = StrokeCap.Round)
                        )
                    }
                    currentStartAngle += sweepAngle
                }
            }
        }

        // Center Info
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = centerTitle,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF64748B)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = CurrencyUtils.formatVnd(totalAmount),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

data class BarItem(
    val label: String,
    val value: Double
)

@Composable
fun SimpleBarChart(
    items: List<BarItem>,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary,
    maxHeight: Dp = 140.dp
) {
    val maxValue = (items.maxOfOrNull { it.value } ?: 1.0).coerceAtLeast(1.0)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(maxHeight),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        items.forEach { item ->
            val ratio = (item.value / maxValue).toFloat().coerceIn(0.05f, 1f)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                // Bar
                Box(
                    modifier = Modifier
                        .fillMaxHeight(ratio)
                        .width(18.dp)
                        .background(
                            if (item.value > 0) barColor else Color(0xFFE2E8F0),
                            shape = CircleShape
                        )
                )
                Spacer(modifier = Modifier.height(6.dp))
                // Label
                Text(
                    text = item.label,
                    fontSize = 11.sp,
                    color = Color(0xFF64748B),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

data class BarChartItem(
    val label: String,
    val subLabel: String = "",
    val value: Double,
    val isCurrent: Boolean = false
)

/**
 * Biểu đồ cột chi tiêu trực quan, có hoạt ảnh và tương tác chọn xem chi tiết:
 * So sánh mức độ chi tiêu giữa các tuần hoặc các tháng
 */
@Composable
fun ExpenseBarChart(
    items: List<BarChartItem>,
    modifier: Modifier = Modifier,
    barColor: Color = Color(0xFF10B981),
    currentBarColor: Color = Color(0xFF047857),
    emptyBarColor: Color = Color(0xFFF1F5F9),
    maxHeight: Dp = 150.dp
) {
    if (items.isEmpty()) return

    val maxValue = (items.maxOfOrNull { it.value } ?: 1.0).coerceAtLeast(1.0)
    var selectedIndex by remember(items) { mutableStateOf<Int?>(null) }

    Column(modifier = modifier.fillMaxWidth()) {
        // Tooltip hiển thị số tiền chính xác khi nhấn vào cột
        selectedIndex?.let { idx ->
            if (idx in items.indices) {
                val item = items[idx]
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF0F172A),
                        shadowElevation = 2.dp
                    ) {
                        Text(
                            text = "${item.label} ${if (item.subLabel.isNotBlank()) "(${item.subLabel})" else ""}: ${CurrencyUtils.formatVnd(item.value)}",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        // Khu vực vẽ các cột
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(maxHeight)
        ) {
            // Đường lưới tham chiếu mờ (50% và 100%)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val lineColor = Color(0xFFE2E8F0)
                drawLine(
                    color = lineColor,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx()
                )
                drawLine(
                    color = lineColor.copy(alpha = 0.5f),
                    start = Offset(0f, size.height * 0.5f),
                    end = Offset(size.width, size.height * 0.5f),
                    strokeWidth = 1.dp.toPx()
                )
            }

            // Các cột biểu đồ
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                items.forEachIndexed { index, item ->
                    val isSelected = selectedIndex == index
                    val isHighest = item.value == maxValue && item.value > 0
                    val targetRatio = if (item.value > 0) {
                        (item.value / maxValue).toFloat().coerceIn(0.08f, 1f)
                    } else {
                        0.02f
                    }
                    val animatedRatio by animateFloatAsState(
                        targetValue = targetRatio,
                        animationSpec = tween(durationMillis = 650),
                        label = "bar_ratio_$index"
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable {
                                selectedIndex = if (selectedIndex == index) null else index
                            },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        // Số tiền rút gọn hiển thị phía trên cột
                        if (item.value > 0) {
                            Text(
                                text = CurrencyUtils.formatCompactVnd(item.value),
                                fontSize = 10.sp,
                                fontWeight = if (item.isCurrent || isHighest) FontWeight.Bold else FontWeight.Medium,
                                color = if (item.isCurrent) currentBarColor else if (isSelected) Color(0xFF0F172A) else Color(0xFF64748B),
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }

                        // Thân cột bo góc trên
                        val barEffectiveColor = when {
                            isSelected -> Color(0xFF0F172A)
                            item.isCurrent -> currentBarColor
                            item.value > 0 -> barColor
                            else -> emptyBarColor
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxHeight(animatedRatio)
                                .widthIn(min = 16.dp, max = 34.dp)
                                .fillMaxWidth(0.55f)
                                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                .background(barEffectiveColor)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Dòng nhãn trục hoành (Tuần 1.. hoặc Th 6..)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            items.forEachIndexed { index, item ->
                val isSelected = selectedIndex == index
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            selectedIndex = if (selectedIndex == index) null else index
                        },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = item.label,
                        fontSize = 11.sp,
                        fontWeight = if (item.isCurrent || isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                        color = if (item.isCurrent) currentBarColor else if (isSelected) Color(0xFF0F172A) else Color(0xFF64748B),
                        textAlign = TextAlign.Center
                    )
                    if (item.subLabel.isNotBlank()) {
                        Text(
                            text = item.subLabel,
                            fontSize = 9.sp,
                            color = if (item.isCurrent) currentBarColor.copy(alpha = 0.8f) else Color(0xFF94A3B8),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
