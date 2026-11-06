package com.expensetracker.app.ui.screens.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.expensetracker.app.data.local.entity.TransactionEntity
import com.expensetracker.app.ui.components.AccountDetailBottomSheet
import com.expensetracker.app.ui.components.CategoryIcon
import com.expensetracker.app.ui.components.EditTransactionDialog
import com.expensetracker.app.ui.theme.*
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
    val transactions by viewModel.allTransactions.collectAsState()
    val categories by viewModel.allCategories.collectAsState()

    var selectedDetailAccount by remember { mutableStateOf<AccountEntity?>(null) }
    var editingTransaction by remember { mutableStateOf<TransactionEntity?>(null) }

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
                title = { Text("Nguồn Tiền & Ví", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundLight)
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
                shape = RoundedCornerShape(20.dp),
                containerColor = PrimaryGreen,
                contentColor = Color.White,
                elevation = FloatingActionButtonDefaults.elevation(3.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Thêm ví mới", modifier = Modifier.size(28.dp))
            }
        }
    ) { paddingValues ->
        ResponsiveContentContainer(maxWidth = 760.dp) {
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

            // Total Balance Card (M3 Expressive Hero Card)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = ExpressiveHeroShape,
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(1.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(text = "Tổng tài sản thực tế", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFF64748B))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = CurrencyUtils.formatVnd(totalBalance),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
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
                        .clip(ExpressiveItemShape)
                        .clickable {
                            selectedDetailAccount = account
                        },
                    shape = ExpressiveItemShape,
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(1.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(ExpressiveSquircleIconShape)
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
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = TextPrimary
                                )
                                if (account.isReserve || account.type == AccountType.RESERVE) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = ExpressivePillShape,
                                        color = Color(0xFFE0F2FE)
                                    ) {
                                        Text(
                                            "🛡️ Dự trữ",
                                            fontSize = 10.sp,
                                            color = Color(0xFF0369A1),
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "$typeLabel • Chạm để xem chi tiết & lịch sử",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B)
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = CurrencyUtils.formatVnd(account.currentBalance),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                color = PrimaryGreen
                            )
                            IconButton(
                                onClick = {
                                    editingAccount = account
                                    editAccountName = account.name
                                    editBalanceText = if (account.currentBalance > 0) CurrencyUtils.formatInputAmount(account.currentBalance.toLong().toString()) else ""
                                    editIsReserve = account.isReserve || account.type == AccountType.RESERVE
                                },
                                modifier = Modifier.size(28.dp).padding(top = 4.dp)
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Sửa số dư",
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(16.dp)
                                )
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
    }

    // Dialog Edit / Adjust Balance
    editingAccount?.let { acc ->
        AlertDialog(
            onDismissRequest = { editingAccount = null },
            shape = ExpressiveDialogShape,
            title = { Text("Cập nhật số dư: ${acc.name}", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Nhập số dư thực tế hiện tại bạn đang có trong nguồn tiền này:",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )

                    OutlinedTextField(
                        value = editBalanceText,
                        onValueChange = { input -> editBalanceText = CurrencyUtils.formatInputAmount(input) },
                        label = { Text("Số dư thực tế (₫)") },
                        shape = ExpressiveInputShape,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editAccountName,
                        onValueChange = { editAccountName = it },
                        label = { Text("Tên nguồn tiền") },
                        shape = ExpressiveInputShape,
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
                    shape = ExpressivePillShape,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                ) {
                    Text("Lưu số dư", fontWeight = FontWeight.Bold)
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
                            Text("Xóa ví", fontWeight = FontWeight.Bold)
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
            shape = ExpressiveDialogShape,
            title = { Text("Thêm ví / tài khoản mới", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Gợi ý nhanh:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
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
                                shape = ExpressivePillShape,
                                label = { Text(presetName, fontSize = 10.sp, fontWeight = FontWeight.SemiBold) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = accountName,
                        onValueChange = { accountName = it },
                        label = { Text("Tên ví / Ngân hàng") },
                        placeholder = { Text("Ví dụ: VPBank, MoMo, Tiền mặt...") },
                        shape = ExpressiveInputShape,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = initialBalanceText,
                        onValueChange = { input -> initialBalanceText = CurrencyUtils.formatInputAmount(input) },
                        label = { Text("Số dư ban đầu (₫)") },
                        shape = ExpressiveInputShape,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Loại tài khoản:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
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
                                shape = ExpressivePillShape,
                                label = { Text(label, fontSize = 10.sp, fontWeight = if (selectedType == type) FontWeight.Bold else FontWeight.Normal) }
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
                    shape = ExpressivePillShape,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                ) {
                    Text("Thêm ví", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }

    // Account Detail Bottom Sheet (Lịch sử thu / chi của nguồn tiền)
    selectedDetailAccount?.let { acc ->
        AccountDetailBottomSheet(
            account = acc,
            allTransactions = transactions,
            categories = categories,
            onDismiss = { selectedDetailAccount = null },
            onEditAccount = { accountToEdit ->
                selectedDetailAccount = null
                editingAccount = accountToEdit
                editAccountName = accountToEdit.name
                editBalanceText = if (accountToEdit.currentBalance > 0) CurrencyUtils.formatInputAmount(accountToEdit.currentBalance.toLong().toString()) else ""
                editIsReserve = accountToEdit.isReserve || accountToEdit.type == AccountType.RESERVE
            },
            onEditTransaction = { tx -> editingTransaction = tx },
            onDeleteTransaction = { tx -> viewModel.deleteTransaction(tx) }
        )
    }

    // Edit Transaction Dialog
    editingTransaction?.let { tx ->
        EditTransactionDialog(
            transaction = tx,
            categories = categories,
            accounts = accounts,
            onDismiss = { editingTransaction = null },
            onSave = { updatedTx ->
                viewModel.updateTransaction(tx, updatedTx)
                editingTransaction = null
            },
            onDelete = {
                viewModel.deleteTransaction(tx)
                editingTransaction = null
            }
        )
    }
}
