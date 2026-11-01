package com.expensetracker.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import com.expensetracker.app.data.local.entity.CategoryEntity
import com.expensetracker.app.data.local.entity.TransactionEntity
import com.expensetracker.app.data.local.entity.TransactionType
import com.expensetracker.app.ui.theme.*
import com.expensetracker.app.utils.CurrencyUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTransactionDialog(
    transaction: TransactionEntity,
    categories: List<CategoryEntity>,
    accounts: List<AccountEntity>,
    onDismiss: () -> Unit,
    onSave: (TransactionEntity) -> Unit,
    onDelete: () -> Unit
) {
    var editType by remember { mutableStateOf(transaction.type) }
    var editAmountText by remember { mutableStateOf(CurrencyUtils.formatInputAmount(transaction.amount.toLong().toString())) }
    var editCategoryId by remember { mutableStateOf(transaction.categoryId) }
    var editAccountId by remember { mutableStateOf(transaction.accountId) }
    var editNote by remember { mutableStateOf(transaction.note) }

    val filteredCategories = categories.filter { it.type == editType }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = ExpressiveDialogShape,
        title = { Text("Chỉnh sửa giao dịch", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Type Switcher: Chi tiêu vs Thu nhập (M3 Expressive Pill Segmented)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(ExpressivePillShape)
                        .background(Color(0xFFF1F5F9))
                        .padding(3.dp)
                ) {
                    // Chi tiêu
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(ExpressivePillShape)
                            .background(if (editType == TransactionType.EXPENSE) ExpenseRed else Color.Transparent)
                            .clickable {
                                editType = TransactionType.EXPENSE
                                val firstExp = categories.firstOrNull { it.type == TransactionType.EXPENSE }
                                if (firstExp != null && filteredCategories.none { it.id == editCategoryId }) {
                                    editCategoryId = firstExp.id
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Chi tiêu (-)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (editType == TransactionType.EXPENSE) Color.White else Color(0xFF64748B)
                        )
                    }

                    // Thu nhập
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(ExpressivePillShape)
                            .background(if (editType == TransactionType.INCOME) IncomeGreen else Color.Transparent)
                            .clickable {
                                editType = TransactionType.INCOME
                                val firstInc = categories.firstOrNull { it.type == TransactionType.INCOME }
                                if (firstInc != null && filteredCategories.none { it.id == editCategoryId }) {
                                    editCategoryId = firstInc.id
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Thu nhập (+)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (editType == TransactionType.INCOME) Color.White else Color(0xFF64748B)
                        )
                    }
                }

                // Amount Input
                OutlinedTextField(
                    value = editAmountText,
                    onValueChange = { input -> editAmountText = CurrencyUtils.formatInputAmount(input) },
                    label = { Text("Số tiền (₫)") },
                    shape = ExpressiveInputShape,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Category Picker
                Text("Danh mục:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filteredCategories, key = { it.id }) { cat ->
                        val isSelected = editCategoryId == cat.id
                        val catColor = parseColor(cat.colorHex)
                        FilterChip(
                            selected = isSelected,
                            onClick = { editCategoryId = cat.id },
                            shape = ExpressivePillShape,
                            label = { Text(cat.name, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = if (editType == TransactionType.EXPENSE) ExpenseRed else IncomeGreen,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                // Account / Wallet Picker
                Text("Nguồn tiền / Ví:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(accounts, key = { it.id }) { acc ->
                        val isSelected = editAccountId == acc.id
                        FilterChip(
                            selected = isSelected,
                            onClick = { editAccountId = acc.id },
                            shape = ExpressivePillShape,
                            label = { Text(acc.name, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryGreen,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                // Note Input
                OutlinedTextField(
                    value = editNote,
                    onValueChange = { editNote = it },
                    label = { Text("Ghi chú") },
                    shape = ExpressiveInputShape,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = CurrencyUtils.parseAmount(editAmountText)
                    if (amount > 0) {
                        onSave(
                            transaction.copy(
                                amount = amount,
                                type = editType,
                                categoryId = editCategoryId,
                                accountId = editAccountId,
                                note = editNote
                            )
                        )
                    }
                },
                shape = ExpressivePillShape,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
            ) {
                Text("Lưu thay đổi", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFEF4444))
                ) {
                    Text("Xóa", fontWeight = FontWeight.Bold)
                }
                TextButton(onClick = onDismiss) {
                    Text("Hủy")
                }
            }
        }
    )
}
