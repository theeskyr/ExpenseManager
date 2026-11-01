package com.expensetracker.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.expensetracker.app.ui.theme.*
import com.expensetracker.app.utils.CurrencyUtils
import com.expensetracker.app.utils.DateUtils
import java.util.Locale

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
        shape = ExpressiveItemShape,
        color = Color(0xFFF8FAFC),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = dateLabel,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    shape = ExpressivePillShape,
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
