package com.expensetracker.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
