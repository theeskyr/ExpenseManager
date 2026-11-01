package com.expensetracker.app.ui.screens.ai

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.expensetracker.app.data.local.entity.TransactionType
import com.expensetracker.app.ui.theme.*
import com.expensetracker.app.ui.viewmodel.ChatMessage
import com.expensetracker.app.ui.viewmodel.ExpenseViewModel
import com.expensetracker.app.ui.viewmodel.MessageSender
import com.expensetracker.app.utils.CurrencyUtils
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAssistantScreen(
    viewModel: ExpenseViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val chatMessages by viewModel.chatMessages.collectAsState()
    val isLoading by viewModel.isAiLoading.collectAsState()
    val apiKey by viewModel.geminiApiKey.collectAsState()
    val selectedModel by viewModel.selectedAiModel.collectAsState()

    var inputText by remember { mutableStateOf("") }
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showApiKeyDialog by remember { mutableStateOf(false) }
    var showModelDialog by remember { mutableStateOf(false) }
    var apiKeyInput by remember { mutableStateOf("") }
    var customModelInput by remember { mutableStateOf("") }

    val listState = rememberLazyListState()

    // Scroll to bottom when new messages arrive
    LaunchedEffect(chatMessages.size) {
        if (chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(chatMessages.size - 1)
        }
    }

    // Photo Picker Launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(context.contentResolver, it)
                    ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                        decoder.isMutableRequired = true
                    }
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, it)
                }
                selectedBitmap = bitmap
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Trở về Trang chủ")
                    }
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(com.expensetracker.app.ui.theme.ExpressiveSquircleIconShape)
                                .background(PrimaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.SmartToy, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Trợ lý Chi Tiêu AI", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = if (apiKey.isNotBlank()) selectedModel else "Chưa cài API Key",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (apiKey.isNotBlank()) PrimaryGreen else ExpenseRed
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            customModelInput = selectedModel
                            showModelDialog = true
                        }
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = "Chọn Model AI", tint = PrimaryGreen)
                    }
                    IconButton(
                        onClick = {
                            apiKeyInput = apiKey
                            showApiKeyDialog = true
                        }
                    ) {
                        Icon(Icons.Default.Key, contentDescription = "Cài đặt API Key", tint = PrimaryGreen)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundLight)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // API Key prompt banner if not configured
            if (apiKey.isBlank()) {
                Surface(
                    color = Color(0xFFFEF2F2),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Chưa nhập Gemini API Key", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = ExpenseRed)
                            Text("Nhập API Key để mở khóa tính năng tự động đọc tin nhắn & quét bill hóa đơn.", fontSize = 11.sp, color = Color(0xFF7F1D1D))
                        }
                        Button(
                            onClick = {
                                apiKeyInput = ""
                                showApiKeyDialog = true
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            shape = com.expensetracker.app.ui.theme.ExpressivePillShape,
                            colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed)
                        ) {
                            Text("Nhập Key", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Message List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(chatMessages, key = { it.id }) { msg ->
                    ChatBubble(message = msg)
                }

                if (isLoading) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = PrimaryGreen
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                "Gemini đang phân tích chi tiêu...",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                }
            }

            // Image Preview thumbnail (before sending)
            selectedBitmap?.let { bmp ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF1F5F9),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1))
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Hóa đơn đính kèm",
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Ảnh hóa đơn / bill Shopee sẵn sàng gửi",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { selectedBitmap = null },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Bỏ ảnh", tint = Color(0xFF64748B), modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            // Chat Input Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Attachment button for Receipt / Screenshot
                    IconButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = "Đính kèm hóa đơn",
                            tint = PrimaryGreen,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    // Text Field
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = {
                            Text(
                                "Nhắn chi tiêu (VD: vừa ăn phở 55k)...",
                                fontSize = 13.sp,
                                color = Color(0xFF94A3B8)
                            )
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = Color(0xFFF8FAFC),
                            focusedContainerColor = Color.White,
                            unfocusedBorderColor = Color(0xFFE2E8F0)
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (selectedBitmap != null) {
                                    viewModel.sendAiImageMessage(selectedBitmap!!, inputText)
                                    selectedBitmap = null
                                    inputText = ""
                                } else if (inputText.isNotBlank()) {
                                    viewModel.sendAiTextMessage(inputText)
                                    inputText = ""
                                }
                            }
                        )
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    // Send Button
                    val canSend = (inputText.isNotBlank() || selectedBitmap != null) && !isLoading
                    IconButton(
                        onClick = {
                            if (selectedBitmap != null) {
                                viewModel.sendAiImageMessage(selectedBitmap!!, inputText)
                                selectedBitmap = null
                                inputText = ""
                            } else if (inputText.isNotBlank()) {
                                viewModel.sendAiTextMessage(inputText)
                                inputText = ""
                            }
                        },
                        enabled = canSend
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (canSend) PrimaryGreen else Color(0xFFE2E8F0)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Send,
                                contentDescription = "Gửi",
                                tint = if (canSend) Color.White else Color(0xFF94A3B8),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Gemini API Key Config Dialog
    if (showApiKeyDialog) {
        AlertDialog(
            onDismissRequest = { showApiKeyDialog = false },
            shape = com.expensetracker.app.ui.theme.ExpressiveDialogShape,
            title = { Text("Cấu hình Gemini API Key", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "Dán API Key của Google Gemini vào bên dưới. Key của bạn được lưu an toàn trên máy và chỉ dùng để trao đổi với mô hình Gemini.",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = { apiKeyInput = it },
                        placeholder = { Text("AIzaSy...") },
                        shape = com.expensetracker.app.ui.theme.ExpressiveInputShape,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Nếu chưa có key, bạn có thể lấy miễn phí tại aistudio.google.com",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = PrimaryGreenDark
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveGeminiApiKey(apiKeyInput)
                        showApiKeyDialog = false
                    },
                    shape = com.expensetracker.app.ui.theme.ExpressivePillShape,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                ) {
                    Text("Lưu API Key", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showApiKeyDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }

    // AI Model Selection Dialog
    if (showModelDialog) {
        AlertDialog(
            onDismissRequest = { showModelDialog = false },
            shape = com.expensetracker.app.ui.theme.ExpressiveDialogShape,
            title = { Text("Chọn Mô Hình AI (Model)", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Chọn phiên bản AI phù hợp với nhu cầu của bạn:",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                    com.expensetracker.app.data.ai.GeminiManager.AVAILABLE_MODELS.forEach { (modelId, modelName) ->
                        val isSelected = selectedModel == modelId
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = com.expensetracker.app.ui.theme.ExpressiveItemShape,
                            color = if (isSelected) PrimaryContainer else Color(0xFFF8FAFC),
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
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = modelId,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (isSelected) PrimaryGreenDark else Color(0xFF1E293B)
                                    )
                                    Text(
                                        text = modelName,
                                        fontSize = 11.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Hoặc nhập Model ID tùy chỉnh khác:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B)
                    )
                    OutlinedTextField(
                        value = customModelInput,
                        onValueChange = { customModelInput = it },
                        placeholder = { Text("Ví dụ: gemini-2.0-flash") },
                        shape = com.expensetracker.app.ui.theme.ExpressiveInputShape,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (customModelInput.isNotBlank()) {
                            viewModel.updateAiModel(customModelInput.trim())
                        }
                        showModelDialog = false
                    },
                    shape = com.expensetracker.app.ui.theme.ExpressivePillShape,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                ) {
                    Text("Áp dụng", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showModelDialog = false }) {
                    Text("Đóng")
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatBubble(message: ChatMessage) {
    val context = LocalContext.current
    val isUser = message.sender == MessageSender.USER

    val copyTextAction = {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Chat message", message.text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Đã sao chép tin nhắn vào bộ nhớ tạm!", Toast.LENGTH_SHORT).show()
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(com.expensetracker.app.ui.theme.ExpressiveSquircleIconShape)
                    .background(PrimaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.SmartToy, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            modifier = Modifier.widthIn(max = 285.dp),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            // Attached Image if user sent a receipt
            message.bitmap?.let { bmp ->
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(160.dp)
                        .clip(com.expensetracker.app.ui.theme.ExpressiveItemShape)
                        .padding(bottom = 6.dp)
                )
            }

            // M3 Expressive Speech Bubble with asymmetric corners
            Surface(
                modifier = Modifier.combinedClickable(
                    onClick = {},
                    onLongClick = copyTextAction
                ),
                shape = RoundedCornerShape(
                    topStart = 20.dp,
                    topEnd = 20.dp,
                    bottomStart = if (isUser) 20.dp else 4.dp,
                    bottomEnd = if (isUser) 4.dp else 20.dp
                ),
                color = if (isUser) PrimaryGreen else Color.White,
                shadowElevation = 1.dp,
                border = if (!isUser) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)) else null
            ) {
                Text(
                    text = message.text,
                    modifier = Modifier.padding(14.dp),
                    color = if (isUser) Color.White else Color(0xFF1E293B),
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )
            }

            // Copy button and timestamp row
            Row(
                modifier = Modifier
                    .padding(top = 2.dp, start = 4.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = com.expensetracker.app.utils.DateUtils.formatTime(message.timestamp),
                    fontSize = 10.sp,
                    color = Color(0xFF94A3B8)
                )
                Spacer(modifier = Modifier.width(6.dp))
                IconButton(
                    onClick = copyTextAction,
                    modifier = Modifier.size(18.dp)
                ) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = "Sao chép tin nhắn",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            // Auto-added Transaction Card Preview
            if (message.transactions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF0FDF4),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBBF7D0))
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = IncomeGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            val titleText = if (message.transactions.size == 1) {
                                "Đã tự động lưu vào chi tiêu"
                            } else {
                                "Đã tự động lưu ${message.transactions.size} giao dịch vào chi tiêu"
                            }
                            Text(
                                text = titleText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF166534)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        message.transactions.forEachIndexed { idx, tx ->
                            if (idx > 0) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .height(0.5.dp)
                                        .background(Color(0xFFDCFCE7))
                                )
                            }
                            val isExp = tx.type == TransactionType.EXPENSE
                            val prefix = if (isExp) "- " else "+ "
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = tx.note,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF1E293B),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .weight(1f, fill = false)
                                        .padding(end = 6.dp)
                                )
                                Text(
                                    text = "$prefix${CurrencyUtils.formatVnd(tx.amount)}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isExp) ExpenseRed else IncomeGreen
                                )
                            }
                        }

                        if (message.transactions.size > 1) {
                            val totalAmount = message.transactions.sumOf { it.amount }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                                    .height(1.dp)
                                    .background(Color(0xFF86EFAC))
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Tổng cộng:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF166534)
                                )
                                Text(
                                    text = "- ${CurrencyUtils.formatVnd(totalAmount)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ExpenseRed
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
