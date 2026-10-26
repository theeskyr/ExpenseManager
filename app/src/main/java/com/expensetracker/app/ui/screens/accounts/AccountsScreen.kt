package com.expensetracker.app.ui.screens.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
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
import com.expensetracker.app.data.local.entity.AccountEntity
import com.expensetracker.app.data.local.entity.AccountType
import com.expensetracker.app.ui.components.CategoryIcon
import com.expensetracker.app.ui.theme.PrimaryGreen
import com.expensetracker.app.ui.theme.parseColor
import com.expensetracker.app.ui.viewmodel.ExpenseViewModel
import com.expensetracker.app.utils.CurrencyUtils

@Composable
fun AccountsScreen(
    viewModel: ExpenseViewModel
) {
    val accounts by viewModel.allAccounts.collectAsState()
    val totalBalance by viewModel.totalBalance.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var accountName by remember { mutableStateOf("") }
    var initialBalanceText by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(AccountType.BANK) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    accountName = ""
                    initialBalanceText = ""
                    selectedType = AccountType.BANK
                    showAddDialog = true
                },
                containerColor = PrimaryGreen,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Thêm ví mới")
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
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Ví & Tài khoản",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Quản lý các nguồn tiền của bạn",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF64748B)
                )
            }

            // Total Balance Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "Tổng tài sản thực tế", fontSize = 13.sp, color = Color(0xFF64748B))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = CurrencyUtils.formatVnd(totalBalance),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryGreen
                        )
                    }
                }
            }

            items(accounts, key = { it.id }) { account ->
                val accColor = parseColor(account.colorHex)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(0.5.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(accColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CategoryIcon(
                                name = account.iconName,
                                tint = accColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = account.name,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            )
                            val typeLabel = when (account.type) {
                                AccountType.CASH -> "Tiền mặt"
                                AccountType.BANK -> "Tài khoản ngân hàng"
                                AccountType.E_WALLET -> "Ví điện tử"
                                AccountType.CREDIT_CARD -> "Thẻ tín dụng"
                            }
                            Text(
                                text = typeLabel,
                                fontSize = 12.sp,
                                color = Color(0xFF64748B)
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = CurrencyUtils.formatVnd(account.currentBalance),
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            if (accounts.size > 1) {
                                IconButton(
                                    onClick = { viewModel.deleteAccount(account) },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Xóa ví",
                                        tint = Color(0xFFCBD5E1),
                                        modifier = Modifier.size(16.dp)
                                    )
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

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Thêm ví / tài khoản mới") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = accountName,
                        onValueChange = { accountName = it },
                        label = { Text("Tên ví / Ngân hàng") },
                        placeholder = { Text("Ví dụ: VPBank, MoMo...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = initialBalanceText,
                        onValueChange = { input -> if (input.all { it.isDigit() }) initialBalanceText = input },
                        label = { Text("Số dư ban đầu (₫)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Loại tài khoản:", fontSize = 12.sp, color = Color(0xFF64748B))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            AccountType.BANK to "Ngân hàng",
                            AccountType.E_WALLET to "Ví điện tử",
                            AccountType.CASH to "Tiền mặt"
                        ).forEach { (type, label) ->
                            FilterChip(
                                selected = selectedType == type,
                                onClick = { selectedType = type },
                                label = { Text(label, fontSize = 11.sp) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (accountName.isNotBlank()) {
                            val bal = initialBalanceText.toDoubleOrNull() ?: 0.0
                            val color = when (selectedType) {
                                AccountType.BANK -> "#2196F3"
                                AccountType.E_WALLET -> "#E91E63"
                                AccountType.CASH -> "#4CAF50"
                                AccountType.CREDIT_CARD -> "#FF5722"
                            }
                            viewModel.addAccount(
                                name = accountName,
                                type = selectedType,
                                initialBalance = bal,
                                colorHex = color
                            )
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                ) {
                    Text("Thêm ví")
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
