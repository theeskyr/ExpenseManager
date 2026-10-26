package com.expensetracker.app.ui.screens.add

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.expensetracker.app.data.local.entity.AccountEntity
import com.expensetracker.app.data.local.entity.CategoryEntity
import com.expensetracker.app.data.local.entity.TransactionType
import com.expensetracker.app.ui.components.CategoryIcon
import com.expensetracker.app.ui.theme.*
import com.expensetracker.app.ui.viewmodel.ExpenseViewModel
import com.expensetracker.app.utils.CurrencyUtils
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    viewModel: ExpenseViewModel,
    onNavigateBack: () -> Unit
) {
    var transactionType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var amountText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf("") }
    var selectedAccountId by remember { mutableStateOf(1L) }
    var selectedTimestamp by remember { mutableStateOf(System.currentTimeMillis()) }

    val categories by viewModel.allCategories.collectAsState()
    val accounts by viewModel.allAccounts.collectAsState()

    val filteredCategories = categories.filter { it.type == transactionType }

    // Auto select first category & account
    LaunchedEffect(filteredCategories) {
        if (filteredCategories.isNotEmpty() && selectedCategoryId.isEmpty()) {
            selectedCategoryId = filteredCategories.first().id
        }
    }
    LaunchedEffect(accounts) {
        if (accounts.isNotEmpty() && accounts.none { it.id == selectedAccountId }) {
            selectedAccountId = accounts.first().id
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Thêm giao dịch", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Expense / Income Segmented Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF1F5F9))
                    .padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (transactionType == TransactionType.EXPENSE) ExpenseRed else Color.Transparent)
                        .clickable {
                            transactionType = TransactionType.EXPENSE
                            selectedCategoryId = ""
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Khoản chi",
                        fontWeight = FontWeight.Bold,
                        color = if (transactionType == TransactionType.EXPENSE) Color.White else Color(0xFF64748B)
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (transactionType == TransactionType.INCOME) IncomeGreen else Color.Transparent)
                        .clickable {
                            transactionType = TransactionType.INCOME
                            selectedCategoryId = ""
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Khoản thu",
                        fontWeight = FontWeight.Bold,
                        color = if (transactionType == TransactionType.INCOME) Color.White else Color(0xFF64748B)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Amount Input Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Số tiền",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF64748B)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        OutlinedTextField(
                            value = amountText,
                            onValueChange = { input ->
                                if (input.all { it.isDigit() }) {
                                    amountText = input
                                }
                            },
                            textStyle = LocalTextStyle.current.copy(
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                color = if (transactionType == TransactionType.EXPENSE) ExpenseRed else IncomeGreen
                            ),
                            placeholder = {
                                Text(
                                    "0",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth(),
                                    color = Color(0xFFCBD5E1)
                                )
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            trailingIcon = {
                                Text("₫", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                            },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Quick amount shortcuts (+50k, +100k, +200k, +500k)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(50_000L to "+50k", 100_000L to "+100k", 200_000L to "+200k", 500_000L to "+500k").forEach { (value, label) ->
                            OutlinedButton(
                                onClick = {
                                    val current = amountText.toLongOrNull() ?: 0L
                                    amountText = (current + value).toString()
                                },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(label, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Category Selection Header
            Text(
                text = "Chọn danh mục",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))

            // Category Grid
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filteredCategories.chunked(4).forEach { rowCategories ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowCategories.forEach { category ->
                            val isSelected = category.id == selectedCategoryId
                            val catColor = parseColor(category.colorHex)

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) catColor.copy(alpha = 0.15f) else Color.White)
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) catColor else Color(0xFFE2E8F0),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { selectedCategoryId = category.id }
                                    .padding(vertical = 12.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(catColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CategoryIcon(name = category.iconName, tint = Color.White, modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = category.name,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) catColor else Color(0xFF334155),
                                    maxLines = 1,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                        // Fill empty slots if last row has less than 4 items
                        for (i in 0 until (4 - rowCategories.size)) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Account / Wallet Selection
            Text(
                text = "Tài khoản / Ví thanh toán",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                accounts.forEach { account ->
                    val isSelected = account.id == selectedAccountId
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedAccountId = account.id },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) PrimaryGreen.copy(alpha = 0.15f) else Color.White,
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) PrimaryGreen else Color(0xFFE2E8F0)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = account.name,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) PrimaryGreenDark else Color(0xFF334155),
                                maxLines = 1,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = CurrencyUtils.formatVnd(account.currentBalance),
                                fontSize = 10.sp,
                                color = Color(0xFF64748B),
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Note Text Field
            Text(
                text = "Ghi chú",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                placeholder = { Text("Ví dụ: Ăn trưa phở bò, Mua quà sinh nhật...") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(30.dp))

            // Save Button
            val amount = amountText.toDoubleOrNull() ?: 0.0
            val isValid = amount > 0 && selectedCategoryId.isNotEmpty()

            Button(
                onClick = {
                    if (isValid) {
                        viewModel.addTransaction(
                            amount = amount,
                            type = transactionType,
                            categoryId = selectedCategoryId,
                            accountId = selectedAccountId,
                            note = note,
                            timestamp = selectedTimestamp
                        )
                        onNavigateBack()
                    }
                },
                enabled = isValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (transactionType == TransactionType.EXPENSE) ExpenseRed else IncomeGreen
                )
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Lưu giao dịch",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
