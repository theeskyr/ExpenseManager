package com.expensetracker.app.ui.screens.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import com.expensetracker.app.data.ai.GeminiManager
import com.expensetracker.app.data.local.entity.TransactionType
import com.expensetracker.app.data.service.BankNotificationListenerService
import com.expensetracker.app.ui.theme.*
import com.expensetracker.app.ui.viewmodel.ExpenseViewModel

@Composable
fun SettingsScreen(
    viewModel: ExpenseViewModel,
    onNavigateToAccounts: () -> Unit = {}
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

        // Manage Accounts & Wallets Card (M3 Expressive Card)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToAccounts() },
                shape = com.expensetracker.app.ui.theme.ExpressiveCardShape,
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(1.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9))
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(com.expensetracker.app.ui.theme.ExpressiveSquircleIconShape)
                            .background(com.expensetracker.app.ui.theme.PrimaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Quản lý nguồn tiền & ví",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Xem, thêm và điều chỉnh số dư tiền mặt, tài khoản ngân hàng, ví điện tử, quỹ dự phòng.",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF94A3B8))
                }
            }
        }

        // Export Data Card (M3 Expressive Card)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = com.expensetracker.app.ui.theme.ExpressiveCardShape,
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(1.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(com.expensetracker.app.ui.theme.ExpressiveSquircleIconShape)
                                .background(com.expensetracker.app.ui.theme.SecondaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null, tint = com.expensetracker.app.ui.theme.SecondaryTeal, modifier = Modifier.size(22.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Xuất dữ liệu ra Excel / CSV",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Xuất toàn bộ lịch sử thu chi ra tệp CSV (chuẩn UTF-8 mở được trên Microsoft Excel hoặc Google Sheets) để lưu trữ hoặc chia sẻ.",
                        fontSize = 13.sp,
                        color = Color(0xFF64748B)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = { viewModel.exportAndShareCsv(context) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = com.expensetracker.app.ui.theme.ExpressivePillShape,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                    ) {
                        Text("Xuất & Chia sẻ file CSV", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Bank Notification Auto Detection (0% Battery Impact)
        item {
            val isListenerEnabled = remember {
                NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = com.expensetracker.app.ui.theme.ExpressiveCardShape,
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(1.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(com.expensetracker.app.ui.theme.ExpressiveSquircleIconShape)
                                    .background(com.expensetracker.app.ui.theme.PrimaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(22.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Tự động đọc biến động số dư",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Khi ngân hàng (Vietcombank, MB, Techcombank, TPBank, VPBank, MoMo...) gửi thông báo trừ/cộng tiền hoặc tin nhắn SMS, ứng dụng sẽ lập tức hiển thị thông báo nhắc bạn chọn mục chi tiêu để lưu 1 chạm.",
                        fontSize = 13.sp,
                        color = Color(0xFF64748B)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Battery optimization badge (M3 Expressive Pill Badge)
                    Surface(
                        shape = com.expensetracker.app.ui.theme.ExpressivePillShape,
                        color = Color(0xFFF0FDF4),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBBF7D0))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.BatteryChargingFull, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Tối ưu hóa: 0% pin ngầm do cơ chế Event-driven của Android",
                                fontSize = 11.sp,
                                color = Color(0xFF166534),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Diagnostic notes
                    Surface(
                        shape = com.expensetracker.app.ui.theme.ExpressiveItemShape,
                        color = Color(0xFFF8FAFC),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "💡 Hướng dẫn nhận thông báo nhạy nhất:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF334155)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "1. Bấm nút cấp quyền đọc thông báo bên dưới.\n2. Bật quyền gửi thông báo (Android 13+).\n3. Cài đặt chế độ Pin của app là 'Không hạn chế' (Unrestricted) trong cài đặt thiết bị để tránh bị hệ thống tắt.",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B),
                                lineHeight = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                            context.startActivity(intent)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = com.expensetracker.app.ui.theme.ExpressivePillShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isListenerEnabled) Color(0xFF059669) else PrimaryGreen
                        )
                    ) {
                        Text(
                            text = if (isListenerEnabled) "✓ Đã cấp quyền đọc thông báo" else "Cấp quyền truy cập thông báo",
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                try {
                                    BankNotificationListenerService.showExpensePromptNotification(
                                        context = context,
                                        bankName = "Vietcombank",
                                        amount = 55000.0,
                                        type = TransactionType.EXPENSE,
                                        note = "Thanh toán Phở bò (Test Chi)"
                                    )
                                    Toast.makeText(context, "Đã gửi thông báo test Chi (-55k)!", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = com.expensetracker.app.ui.theme.ExpressivePillShape
                        ) {
                            Text("🧪 Test Chi (-)", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                try {
                                    BankNotificationListenerService.showExpensePromptNotification(
                                        context = context,
                                        bankName = "MB Bank",
                                        amount = 500000.0,
                                        type = TransactionType.INCOME,
                                        note = "Nhận lương / Chuyển khoản (Test Thu)"
                                    )
                                    Toast.makeText(context, "Đã gửi thông báo test Thu (+500k)!", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = com.expensetracker.app.ui.theme.ExpressivePillShape
                        ) {
                            Text("🧪 Test Thu (+)", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Gemini AI API Key Setting Card (M3 Expressive Card)
        item {
            val apiKey by viewModel.geminiApiKey.collectAsState()
            val currentModel by viewModel.selectedAiModel.collectAsState()
            var showKeyDialog by remember { mutableStateOf(false) }
            var showModelDialog by remember { mutableStateOf(false) }
            var tempKey by remember { mutableStateOf("") }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = com.expensetracker.app.ui.theme.ExpressiveCardShape,
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(1.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(com.expensetracker.app.ui.theme.ExpressiveSquircleIconShape)
                                    .background(com.expensetracker.app.ui.theme.TertiaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.SmartToy, contentDescription = null, tint = com.expensetracker.app.ui.theme.TertiaryViolet, modifier = Modifier.size(22.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Trợ lý AI & Mô hình Gemini",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (apiKey.isNotBlank()) "Trạng thái API Key: Đã kết nối (${apiKey.take(6)}...${apiKey.takeLast(4)})" else "Trạng thái: Chưa cấu hình API Key",
                        fontSize = 13.sp,
                        color = if (apiKey.isNotBlank()) Color(0xFF059669) else ExpenseRed,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    val modelDisplayTitle = GeminiManager.AVAILABLE_MODELS.firstOrNull { it.first == currentModel }?.second ?: currentModel
                    Text(
                        text = "Model hiện tại: $modelDisplayTitle",
                        fontSize = 12.sp,
                        color = PrimaryGreen,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                tempKey = apiKey
                                showKeyDialog = true
                            },
                            modifier = Modifier.weight(1f),
                            shape = com.expensetracker.app.ui.theme.ExpressivePillShape
                        ) {
                            Text(if (apiKey.isNotBlank()) "Đổi API Key" else "Nhập Key", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        Button(
                            onClick = { showModelDialog = true },
                            modifier = Modifier.weight(1f),
                            shape = com.expensetracker.app.ui.theme.ExpressivePillShape,
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Chọn Model", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }

            if (showKeyDialog) {
                AlertDialog(
                    onDismissRequest = { showKeyDialog = false },
                    shape = com.expensetracker.app.ui.theme.ExpressiveDialogShape,
                    title = { Text("Cấu hình Gemini API Key", fontWeight = FontWeight.Bold) },
                    text = {
                        Column {
                            Text(
                                "Nhập Gemini API Key của bạn từ aistudio.google.com:",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = tempKey,
                                onValueChange = { tempKey = it },
                                placeholder = { Text("AIzaSy...") },
                                shape = com.expensetracker.app.ui.theme.ExpressiveInputShape,
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.saveGeminiApiKey(tempKey)
                                showKeyDialog = false
                            },
                            shape = com.expensetracker.app.ui.theme.ExpressivePillShape,
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                        ) {
                            Text("Lưu", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showKeyDialog = false }) {
                            Text("Hủy")
                        }
                    }
                )
            }

            if (showModelDialog) {
                AlertDialog(
                    onDismissRequest = { showModelDialog = false },
                    shape = com.expensetracker.app.ui.theme.ExpressiveDialogShape,
                    title = { Text("Chọn Model AI Gemini", fontWeight = FontWeight.Bold) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            GeminiManager.AVAILABLE_MODELS.forEach { (modelId, label) ->
                                val isSelected = currentModel == modelId
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth(),
                                    shape = com.expensetracker.app.ui.theme.ExpressiveItemShape,
                                    color = if (isSelected) com.expensetracker.app.ui.theme.PrimaryContainer else Color(0xFFF8FAFC),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isSelected) PrimaryGreen else Color(0xFFE2E8F0)
                                    ),
                                    onClick = {
                                        viewModel.updateAiModel(modelId)
                                        showModelDialog = false
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = {
                                                viewModel.updateAiModel(modelId)
                                                showModelDialog = false
                                            },
                                            colors = RadioButtonDefaults.colors(selectedColor = PrimaryGreen)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = label,
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) PrimaryGreen else Color(0xFF1E293B)
                                        )
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showModelDialog = false }) {
                            Text("Đóng")
                        }
                    }
                )
            }
        }

        // Privacy Guarantee Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = com.expensetracker.app.ui.theme.ExpressiveCardShape,
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(com.expensetracker.app.ui.theme.ExpressiveSquircleIconShape)
                            .background(Color(0xFFE2E8F0)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFF475569), modifier = Modifier.size(22.dp))
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "Bảo mật & Quyền riêng tư",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
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
                shape = com.expensetracker.app.ui.theme.ExpressiveCardShape,
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(1.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(com.expensetracker.app.ui.theme.ExpressiveSquircleIconShape)
                                .background(com.expensetracker.app.ui.theme.ExpenseRedContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.DeleteForever, contentDescription = null, tint = ExpenseRed, modifier = Modifier.size(22.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Xóa toàn bộ dữ liệu",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = ExpenseRed
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Hành động này sẽ xóa vĩnh viễn toàn bộ lịch sử giao dịch đã ghi chép. Số dư ví sẽ không thể khôi phục.",
                        fontSize = 13.sp,
                        color = Color(0xFF64748B)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedButton(
                        onClick = { showResetDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = com.expensetracker.app.ui.theme.ExpressivePillShape,
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
                    text = "Quản Lý Chi Tiêu - Phiên bản 1.0.0 (Material 3 Expressive)",
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8)
                )
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            shape = com.expensetracker.app.ui.theme.ExpressiveDialogShape,
            title = { Text("Xác nhận xóa dữ liệu?", fontWeight = FontWeight.Bold) },
            text = { Text("Bạn có chắc chắn muốn xóa toàn bộ giao dịch? Hành động này không thể hoàn tác.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetAllData()
                        showResetDialog = false
                    },
                    shape = com.expensetracker.app.ui.theme.ExpressivePillShape,
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed)
                ) {
                    Text("Đồng ý xóa", fontWeight = FontWeight.Bold)
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
