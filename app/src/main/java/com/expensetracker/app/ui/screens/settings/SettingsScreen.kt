package com.expensetracker.app.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.expensetracker.app.ui.theme.ExpenseRed
import com.expensetracker.app.ui.theme.PrimaryGreen
import com.expensetracker.app.ui.viewmodel.ExpenseViewModel

@Composable
fun SettingsScreen(
    viewModel: ExpenseViewModel
) {
    val context = LocalContext.current
    var showResetDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Cài đặt & Dữ liệu",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        // Export Data Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, tint = PrimaryGreen)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Xuất dữ liệu ra Excel / CSV",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Xuất toàn bộ lịch sử thu chi ra tệp CSV (chuẩn UTF-8 mở được trên Microsoft Excel hoặc Google Sheets) để lưu trữ hoặc chia sẻ.",
                        fontSize = 13.sp,
                        color = Color(0xFF64748B)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = { viewModel.exportAndShareCsv(context) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                    ) {
                        Text("Xuất & Chia sẻ file CSV", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Privacy Guarantee Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Bảo mật & Quyền riêng tư",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Dữ liệu được lưu trữ trực tiếp trên thiết bị (SQLite Offline). Ứng dụng không gửi bất kỳ thông tin nào ra ngoài mạng Internet.",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            }
        }

        // Danger Zone: Reset Data
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null, tint = ExpenseRed)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Xóa toàn bộ dữ liệu",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = ExpenseRed
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Hành động này sẽ xóa vĩnh viễn toàn bộ lịch sử giao dịch đã ghi chép. Số dư ví sẽ không thể khôi phục.",
                        fontSize = 13.sp,
                        color = Color(0xFF64748B)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedButton(
                        onClick = { showResetDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ExpenseRed),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ExpenseRed)
                    ) {
                        Text("Xóa dữ liệu giao dịch", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // App Info
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Quản Lý Chi Tiêu - Phiên bản 1.0.0 (Offline Android)",
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8)
                )
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Xác nhận xóa dữ liệu?") },
            text = { Text("Bạn có chắc chắn muốn xóa toàn bộ giao dịch? Hành động này không thể hoàn tác.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetAllData()
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed)
                ) {
                    Text("Đồng ý xóa")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }
}
