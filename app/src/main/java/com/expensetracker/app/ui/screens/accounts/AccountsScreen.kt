package com.expensetracker.app.ui.screens.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    viewModel: ExpenseViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val accounts by viewModel.allAccounts.collectAsState()
    val totalBalance by viewModel.totalBalance.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var editingAccount by remember { mutableStateOf<AccountEntity?>(null) }
    var accountName by remember { mutableStateOf("") }
    var initialBalanceText by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(AccountType.BANK) }
    var isReserveInput by remember { mutableStateOf(false) }

    // State for edit dialog
    var editBalanceText by remember { mutableStateOf("") }
    var editAccountName by remember { mutableStateOf("") }
    var editIsReserve by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nguồn Tiền & Ví", fontWeight = FontWeight.Bold) },
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
                    accountName = ""
                    initialBalanceText = ""
                    selectedType = AccountType.BANK
                    isReserveInput = false
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
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Quản lý toàn bộ nguồn tiền của bạn (tiền mặt, ngân hàng, ví điện tử, quỹ dự phòng). Chạm vào bất kỳ ví nào để cập nhật số dư thực tế.",
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            editingAccount = account
                            editAccountName = account.name
                            editBalanceText = if (account.currentBalance > 0) CurrencyUtils.formatInputAmount(account.currentBalance.toLong().toString()) else ""
                            editIsReserve = account.isReserve || account.type == AccountType.RESERVE
                        },
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = account.name,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp
                                )
                                if (account.isReserve || account.type == AccountType.RESERVE) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(0xFFE0F2FE)
                                    ) {
                                        Text(
                                            "🛡️ Dự trữ",
                                            fontSize = 10.sp,
                                            color = Color(0xFF0369A1),
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                            val typeLabel = when (account.type) {
                                AccountType.CASH -> "Tiền mặt"
                                AccountType.BANK -> "Tài khoản ngân hàng"
                                AccountType.E_WALLET -> "Ví điện tử"
                                AccountType.CREDIT_CARD -> "Thẻ tín dụng"
                                AccountType.RESERVE -> "Quỹ dự phòng khẩn cấp"
                            }
                            Text(
                                text = "$typeLabel • Chạm để sửa số dư",
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
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Sửa số dư",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(16.dp).padding(top = 4.dp)
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(70.dp))
            }
        }
    }

    // Dialog Edit / Adjust Balance
    editingAccount?.let { acc ->
        AlertDialog(
            onDismissRequest = { editingAccount = null },
            title = { Text("Cập nhật số dư: ${acc.name}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Nhập số dư thực tế hiện tại bạn đang có trong nguồn tiền này:",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )

                    OutlinedTextField(
                        value = editBalanceText,
                        onValueChange = { input -> editBalanceText = CurrencyUtils.formatInputAmount(input) },
                        label = { Text("Số dư thực tế (₫)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editAccountName,
                        onValueChange = { editAccountName = it },
                        label = { Text("Tên nguồn tiền") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = editIsReserve,
                            onCheckedChange = { editIsReserve = it },
                            colors = CheckboxDefaults.colors(checkedColor = PrimaryGreen)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "🛡️ Đặt làm Nguồn tiền dự trữ (không tính vào tiền chi tiêu)",
                            fontSize = 12.sp,
                            color = Color(0xFF334155)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newBal = CurrencyUtils.parseAmount(editBalanceText)
                        viewModel.updateAccount(
                            acc.copy(
                                name = if (editAccountName.isNotBlank()) editAccountName else acc.name,
                                currentBalance = newBal,
                                isReserve = editIsReserve
                            )
                        )
                        editingAccount = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                ) {
                    Text("Lưu số dư")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (accounts.size > 1) {
                        TextButton(
                            onClick = {
                                viewModel.deleteAccount(acc)
                                editingAccount = null
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFEF4444))
                        ) {
                            Text("Xóa ví")
                        }
                    }
                    TextButton(onClick = { editingAccount = null }) {
                        Text("Hủy")
                    }
                }
            }
        )
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Thêm ví / tài khoản mới") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Gợi ý nhanh:", fontSize = 11.sp, color = Color(0xFF64748B))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(
                            Triple("Tiền mặt", AccountType.CASH, false),
                            Triple("Vietcombank", AccountType.BANK, false),
                            Triple("MB Bank", AccountType.BANK, false),
                            Triple("MoMo", AccountType.E_WALLET, false),
                            Triple("Quỹ dự phòng", AccountType.RESERVE, true)
                        ).forEach { (presetName, presetType, presetReserve) ->
                            SuggestionChip(
                                onClick = {
                                    accountName = presetName
                                    selectedType = presetType
                                    isReserveInput = presetReserve
                                },
                                label = { Text(presetName, fontSize = 10.sp) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = accountName,
                        onValueChange = { accountName = it },
                        label = { Text("Tên ví / Ngân hàng") },
                        placeholder = { Text("Ví dụ: VPBank, MoMo, Tiền mặt...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = initialBalanceText,
                        onValueChange = { input -> initialBalanceText = CurrencyUtils.formatInputAmount(input) },
                        label = { Text("Số dư ban đầu (₫)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Loại tài khoản:", fontSize = 12.sp, color = Color(0xFF64748B))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(
                            AccountType.BANK to "Ngân hàng",
                            AccountType.E_WALLET to "Ví điện tử",
                            AccountType.CASH to "Tiền mặt",
                            AccountType.RESERVE to "Quỹ dự trữ"
                        ).forEach { (type, label) ->
                            FilterChip(
                                selected = selectedType == type,
                                onClick = {
                                    selectedType = type
                                    if (type == AccountType.RESERVE) isReserveInput = true
                                },
                                label = { Text(label, fontSize = 10.sp) }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isReserveInput,
                            onCheckedChange = { isReserveInput = it },
                            colors = CheckboxDefaults.colors(checkedColor = PrimaryGreen)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "🛡️ Đặt làm Nguồn tiền dự trữ (không tính vào tiền chi tiêu)",
                            fontSize = 12.sp,
                            color = Color(0xFF334155)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (accountName.isNotBlank()) {
                            val bal = CurrencyUtils.parseAmount(initialBalanceText)
                            val color = when (selectedType) {
                                AccountType.BANK -> "#2196F3"
                                AccountType.E_WALLET -> "#E91E63"
                                AccountType.CASH -> "#4CAF50"
                                AccountType.CREDIT_CARD -> "#FF5722"
                                AccountType.RESERVE -> "#0284C7"
                            }
                            viewModel.addAccount(
                                name = accountName,
                                type = selectedType,
                                initialBalance = bal,
                                colorHex = color,
                                isReserve = isReserveInput || selectedType == AccountType.RESERVE
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
