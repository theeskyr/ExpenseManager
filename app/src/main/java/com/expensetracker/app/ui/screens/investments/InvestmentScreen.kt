package com.expensetracker.app.ui.screens.investments

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.expensetracker.app.data.local.entity.InvestmentEntity
import com.expensetracker.app.data.local.entity.InvestmentType
import com.expensetracker.app.ui.theme.ExpenseRed
import com.expensetracker.app.ui.theme.IncomeGreen
import com.expensetracker.app.ui.theme.PrimaryGreen
import com.expensetracker.app.ui.theme.PrimaryGreenDark
import com.expensetracker.app.ui.viewmodel.ExpenseViewModel
import com.expensetracker.app.utils.CurrencyUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvestmentScreen(
    viewModel: ExpenseViewModel,
    onNavigateToAi: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val investments by viewModel.allInvestments.collectAsState()
    val totalInvested by viewModel.totalInvested.collectAsState()
    val totalCurrentValue by viewModel.totalInvestmentValue.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var nameInput by remember { mutableStateOf("") }
    var investedInput by remember { mutableStateOf("") }
    var currentValInput by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(InvestmentType.STOCK) }
    var noteInput by remember { mutableStateOf("") }

    val profitLoss = totalCurrentValue - totalInvested
    val profitPercentage = if (totalInvested > 0) (profitLoss / totalInvested * 100) else 0.0

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quản Lý Đầu Tư", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    nameInput = ""
                    investedInput = ""
                    currentValInput = ""
                    noteInput = ""
                    selectedType = InvestmentType.STOCK
                    showAddDialog = true
                },
                containerColor = PrimaryGreen,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Thêm khoản đầu tư")
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
                Spacer(modifier = Modifier.height(4.dp))
                // Portfolio Summary Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(Color(0xFF1E3A8A), Color(0xFF3B82F6))
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Column {
                            Text(
                                text = "Tổng giá trị danh mục đầu tư",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = CurrencyUtils.formatVnd(totalCurrentValue),
                                color = Color.White,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Tổng vốn bỏ ra", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                                    Text(
                                        CurrencyUtils.formatVnd(totalInvested),
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Lợi nhuận (Lời / Lỗ)", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                                    val isProfit = profitLoss >= 0
                                    val sign = if (isProfit) "+" else ""
                                    Text(
                                        "$sign${CurrencyUtils.formatVnd(profitLoss)} (${sign}${String.format("%.1f", profitPercentage)}%)",
                                        color = if (isProfit) Color(0xFF86EFAC) else Color(0xFFFCA5A5),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // AI Portfolio Analysis Button
            if (investments.isNotEmpty()) {
                item {
                    Button(
                        onClick = {
                            viewModel.requestAiInvestmentAnalysis {
                                onNavigateToAi()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreenDark)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("🤖 Nhờ Trợ lý AI phân tích danh mục đầu tư", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Danh mục tài sản đầu tư",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${investments.size} khoản",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }

            if (investments.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Chưa có khoản đầu tư nào",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color(0xFF94A3B8)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Bấm dấu (+) bên dưới để thêm Vàng, Cổ phiếu, Quỹ hoặc Tiết kiệm...",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFCBD5E1)
                            )
                        }
                    }
                }
            } else {
                items(investments, key = { it.id }) { inv ->
                    val diff = inv.currentValue - inv.amountInvested
                    val roi = if (inv.amountInvested > 0) (diff / inv.amountInvested * 100) else 0.0
                    val isProfit = diff >= 0

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val (typeLabel, typeColor) = when (inv.type) {
                                InvestmentType.GOLD -> "Vàng" to Color(0xFFEAB308)
                                InvestmentType.STOCK -> "Cổ phiếu" to Color(0xFF2563EB)
                                InvestmentType.FUND -> "Chứng chỉ quỹ" to Color(0xFF7C3AED)
                                InvestmentType.SAVINGS -> "Tiết kiệm" to Color(0xFF059669)
                                InvestmentType.REAL_ESTATE -> "Bất động sản" to Color(0xFFB45309)
                                InvestmentType.CRYPTO -> "Crypto" to Color(0xFFEA580C)
                                InvestmentType.OTHER -> "Khác" to Color(0xFF64748B)
                            }

                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(typeColor.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.TrendingUp, contentDescription = null, tint = typeColor, modifier = Modifier.size(22.dp))
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(inv.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = typeColor.copy(alpha = 0.1f)
                                    ) {
                                        Text(
                                            typeLabel,
                                            fontSize = 10.sp,
                                            color = typeColor,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    if (inv.note.isNotBlank()) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(inv.note, fontSize = 11.sp, color = Color(0xFF64748B), maxLines = 1)
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    "Vốn: ${CurrencyUtils.formatVnd(inv.amountInvested)}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B)
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    CurrencyUtils.formatVnd(inv.currentValue),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                val sign = if (isProfit) "+" else ""
                                Text(
                                    "$sign${String.format("%.1f", roi)}%",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isProfit) IncomeGreen else ExpenseRed
                                )
                                IconButton(
                                    onClick = { viewModel.deleteInvestment(inv) },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Xóa", tint = Color(0xFFCBD5E1), modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(70.dp))
            }
        }
    }

    // Add Investment Dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Thêm Khoản Đầu Tư") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Tên khoản đầu tư") },
                        placeholder = { Text("Ví dụ: Vàng nhẫn 9999, Cổ phiếu FPT, Quỹ DCDS...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = investedInput,
                        onValueChange = { if (it.all { c -> c.isDigit() }) investedInput = it },
                        label = { Text("Số vốn ban đầu (₫)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = currentValInput,
                        onValueChange = { if (it.all { c -> c.isDigit() }) currentValInput = it },
                        label = { Text("Giá trị hiện tại ước tính (₫)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Loại hình đầu tư:", fontSize = 12.sp, color = Color(0xFF64748B))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(
                            InvestmentType.GOLD to "Vàng",
                            InvestmentType.STOCK to "Cổ phiếu",
                            InvestmentType.FUND to "Quỹ",
                            InvestmentType.SAVINGS to "Tiết kiệm"
                        ).forEach { (t, label) ->
                            FilterChip(
                                selected = selectedType == t,
                                onClick = { selectedType = t },
                                label = { Text(label, fontSize = 11.sp) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = noteInput,
                        onValueChange = { noteInput = it },
                        label = { Text("Ghi chú") },
                        placeholder = { Text("Ví dụ: Mua ngày 15/10, tích lũy dài hạn...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val invested = investedInput.toDoubleOrNull() ?: 0.0
                        val current = if (currentValInput.isNotBlank()) (currentValInput.toDoubleOrNull() ?: invested) else invested
                        if (nameInput.isNotBlank() && invested > 0) {
                            viewModel.addInvestment(
                                name = nameInput.trim(),
                                type = selectedType,
                                amountInvested = invested,
                                currentValue = current,
                                note = noteInput.trim()
                            )
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                ) {
                    Text("Thêm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }
}
