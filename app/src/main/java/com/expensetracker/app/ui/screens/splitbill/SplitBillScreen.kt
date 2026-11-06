package com.expensetracker.app.ui.screens.splitbill

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.expensetracker.app.data.local.entity.AccountEntity
import com.expensetracker.app.data.local.entity.CategoryEntity
import com.expensetracker.app.data.model.DebtSettlement
import com.expensetracker.app.data.model.MemberBalanceSummary
import com.expensetracker.app.data.model.ScannedReceiptItem
import com.expensetracker.app.data.model.SplitBillCalculator
import com.expensetracker.app.data.model.SplitBillResult
import com.expensetracker.app.data.model.SplitExpenseItem
import com.expensetracker.app.data.model.SplitMember
import com.expensetracker.app.ui.components.CategoryIcon
import com.expensetracker.app.ui.theme.*
import com.expensetracker.app.ui.viewmodel.ExpenseViewModel
import com.expensetracker.app.utils.CsvExporter
import com.expensetracker.app.utils.CurrencyUtils
import com.expensetracker.app.utils.SavedCsvResult
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplitBillScreen(
    viewModel: ExpenseViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToHome: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    val accounts by viewModel.allAccounts.collectAsState()
    val categories by viewModel.allCategories.collectAsState()
    val activeTripData by viewModel.activeTripForSplit.collectAsState()

    // Form states
    var eventTitle by remember { mutableStateOf("Bữa ăn nhóm cuối tuần") }

    val defaultMe = remember { SplitMember(name = "Tôi", isCurrentUser = true) }
    var members by remember {
        mutableStateOf(
            listOf(
                defaultMe,
                SplitMember(name = "Tuấn"),
                SplitMember(name = "Linh")
            )
        )
    }
    var newMemberName by remember { mutableStateOf("") }

    // Expenses list
    var expenseItems by remember { mutableStateOf(listOf<SplitExpenseItem>()) }
    var itemTitle by remember { mutableStateOf("") }
    var itemAmountStr by remember { mutableStateOf("") }
    var selectedPayerId by remember { mutableStateOf(defaultMe.id) }
    var selectedSharerIds by remember { mutableStateOf(members.map { it.id }.toSet()) }

    // Tự động nạp toàn bộ thông tin từ Chuyến đi (nếu đến từ tính năng Tính toán chuyến đi)
    LaunchedEffect(activeTripData) {
        activeTripData?.let { data ->
            eventTitle = data.eventTitle
            members = data.members
            expenseItems = data.items
            if (data.members.isNotEmpty()) {
                selectedPayerId = data.members.first().id
                selectedSharerIds = data.members.map { it.id }.toSet()
            }
        }
    }

    // Synchronize selectedSharerIds when members change
    LaunchedEffect(members) {
        val currentMemberIds = members.map { it.id }.toSet()
        selectedSharerIds = selectedSharerIds.filter { it in currentMemberIds }.toSet()
        if (selectedSharerIds.isEmpty()) {
            selectedSharerIds = currentMemberIds
        }
    }

    // Calculation result
    var result by remember { mutableStateOf<SplitBillResult?>(null) }
    var isCalculatingAi by remember { mutableStateOf(false) }

    // Khi chia tiền xong, nếu dữ liệu đến từ Chuyến đi, tự động lưu kết quả chia nợ vào chuyến đi để xem lại sau này
    LaunchedEffect(result) {
        val currentTrip = activeTripData
        val currentRes = result
        if (currentTrip != null && currentRes != null) {
            val settlementLines = if (currentRes.settlements.isEmpty()) {
                "Tất cả thành viên đã sòng phẳng, không ai nợ ai!"
            } else {
                currentRes.settlements.joinToString("\n") { s ->
                    "• ${s.fromMemberName} ➜ chuyển ${s.toMemberName}: ${CurrencyUtils.formatVnd(s.amount)}"
                }
            }
            val fullSummary = "Tổng chi phí chuyến đi: ${CurrencyUtils.formatVnd(currentRes.totalAmount)} (${currentTrip.members.size} người)\n$settlementLines"
            viewModel.markTripCompleted(currentTrip.tripId, fullSummary)
        }
    }

    // Personal expense save state
    var selectedAccountId by remember { mutableStateOf<Long?>(null) }
    var selectedCategoryId by remember { mutableStateOf("food") }
    var hasSavedToPersonal by remember { mutableStateOf(false) }
    var savedCsvResult by remember { mutableStateOf<SavedCsvResult?>(null) }
    var showSavedSuccessDialog by remember { mutableStateOf(false) }

    // Default account selection
    LaunchedEffect(accounts) {
        if (selectedAccountId == null && accounts.isNotEmpty()) {
            selectedAccountId = accounts.first().id
        }
    }

    // Bill Scanning States
    var isScanningReceipt by remember { mutableStateOf(false) }
    var scannedItems by remember { mutableStateOf<List<ScannedReceiptItem>>(emptyList()) }
    var showAssignDialog by remember { mutableStateOf(false) }
    var scannedBillPayerId by remember { mutableStateOf(defaultMe.id) }
    var showSourceSheet by remember { mutableStateOf(false) }

    fun processReceiptBitmap(bitmap: Bitmap) {
        isScanningReceipt = true
        coroutineScope.launch {
            try {
                val parsed = viewModel.geminiManager.parseReceiptForSplitBill(bitmap)
                if (parsed.isNotEmpty()) {
                    // Mặc định không chọn bất kỳ ai, để người dùng tự chọn người cho từng món
                    scannedItems = parsed.map { it.copy(assignedMemberIds = emptySet()) }
                    scannedBillPayerId = defaultMe.id
                    showAssignDialog = true
                } else {
                    Toast.makeText(
                        context,
                        "Không nhận diện được món ăn nào từ hóa đơn. Vui lòng thử lại với ảnh rõ nét hơn hoặc nhập tay!",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Lỗi khi quét hóa đơn: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            } finally {
                isScanningReceipt = false
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            processReceiptBitmap(bitmap)
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(context.contentResolver, it)
                    ImageDecoder.decodeBitmap(source) { decoder, _, _ -> decoder.isMutableRequired = true }
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, it)
                }
                processReceiptBitmap(bitmap)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Không thể đọc ảnh từ thư viện", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(ExpressiveSquircleIconShape)
                                .background(Color(0xFFEDE9FE)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Groups,
                                contentDescription = null,
                                tint = Color(0xFF6D28D9),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                "Chia tiền nhóm cùng AI",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = TextPrimary
                            )
                            Text(
                                "Sòng phẳng • Tự động giải quyết nợ",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại",
                            tint = TextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        expenseItems = emptyList()
                        result = null
                        hasSavedToPersonal = false
                        savedCsvResult = null
                        showSavedSuccessDialog = false
                        Toast.makeText(context, "Đã làm mới dữ liệu", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Làm mới", tint = TextSecondary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { paddingValues ->
        ResponsiveContentContainer(maxWidth = 800.dp) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(Color(0xFFF8FAFC))
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                // AI Split Bill Hero Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = ExpressiveCardShape,
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFF4F46E5), Color(0xFF7C3AED), Color(0xFF9333EA))
                                )
                            )
                            .padding(18.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = ExpressivePillShape,
                                    color = Color.White.copy(alpha = 0.22f)
                                ) {
                                    Text(
                                        "⚡ THUẬT TOÁN TỐI ƯU & AI",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Chia hóa đơn công bằng & thông minh",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Nhập số người và các khoản chi, AI sẽ tìm số giao dịch ít nhất để ai nợ ai cũng thanh toán nhanh gọn!",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.88f)
                            )
                        }
                    }
                }
            }

            // Section 1: Event Name & Members
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = ExpressiveCardShape,
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "1. Tên buổi tiệc / Chuyến đi",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = eventTitle,
                            onValueChange = { eventTitle = it },
                            placeholder = { Text("Vd: Ăn lẩu Haidilao, Du lịch Đà Lạt...") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = ExpressiveItemShape,
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryGreen,
                                unfocusedBorderColor = Color(0xFFCBD5E1)
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Members Section
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Thành viên tham gia (${members.size})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = TextPrimary
                            )

                            // Quick add member button (tăng nhanh số người)
                            TextButton(
                                onClick = {
                                    val nextNum = members.size
                                    members = members + SplitMember(name = "Bạn $nextNum")
                                }
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp), tint = PrimaryGreen)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("+ Thêm nhanh bạn", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PrimaryGreen)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Members Chips
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            items(members, key = { it.id }) { member ->
                                Surface(
                                    shape = ExpressivePillShape,
                                    color = if (member.isCurrentUser) PrimaryContainer else Color(0xFFF1F5F9),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (member.isCurrentUser) PrimaryGreen else Color(0xFFE2E8F0)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            if (member.isCurrentUser) Icons.Default.AccountCircle else Icons.Default.Person,
                                            contentDescription = null,
                                            tint = if (member.isCurrentUser) PrimaryGreenDark else Color(0xFF64748B),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = if (member.isCurrentUser) "${member.name} (Bạn)" else member.name,
                                            fontSize = 12.sp,
                                            fontWeight = if (member.isCurrentUser) FontWeight.ExtraBold else FontWeight.Medium,
                                            color = if (member.isCurrentUser) PrimaryGreenDark else TextPrimary
                                        )
                                        if (!member.isCurrentUser && members.size > 2) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = "Xóa",
                                                tint = Color(0xFF94A3B8),
                                                modifier = Modifier
                                                    .size(14.dp)
                                                    .noRippleClickable {
                                                        members = members.filter { it.id != member.id }
                                                        // Update payer if needed
                                                        if (selectedPayerId == member.id) {
                                                            selectedPayerId = defaultMe.id
                                                        }
                                                    }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Add Custom Member Name Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = newMemberName,
                                onValueChange = { newMemberName = it },
                                placeholder = { Text("Tên bạn bè (vd: Tuấn, Linh, Nam...)", fontSize = 12.sp) },
                                modifier = Modifier.weight(1f),
                                shape = ExpressiveItemShape,
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryGreen,
                                    unfocusedBorderColor = Color(0xFFCBD5E1)
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (newMemberName.isNotBlank()) {
                                        members = members + SplitMember(name = newMemberName.trim())
                                        newMemberName = ""
                                    }
                                },
                                enabled = newMemberName.isNotBlank(),
                                shape = ExpressiveItemShape,
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                            ) {
                                Text("Thêm", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Section 2: Input Expenses (Khoản chi tiêu & Ai đã trả tiền)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = ExpressiveCardShape,
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "2. Nhập các khoản chi tiêu",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = TextPrimary
                        )
                        Text(
                            "Nhập tên món, số tiền và chọn ai là người đã thanh toán trước",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // AI Bill Scanner Card (Quét hóa đơn bằng AI)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(ExpressiveItemShape)
                                .noRippleClickable(enabled = !isScanningReceipt) { showSourceSheet = true },
                            shape = ExpressiveItemShape,
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F3FF)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDDD6FE))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(ExpressiveSquircleIconShape)
                                        .background(Color(0xFF8B5CF6)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.CameraAlt,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Chụp bill / Quét hóa đơn AI ⚡",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color(0xFF5B21B6)
                                    )
                                    Text(
                                        "AI tự bóc tách từng món ăn để bạn chọn ai ăn món nào!",
                                        fontSize = 11.sp,
                                        color = Color(0xFF6D28D9),
                                        lineHeight = 15.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                if (isScanningReceipt) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = Color(0xFF7C3AED),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Surface(
                                        shape = ExpressivePillShape,
                                        color = Color(0xFF7C3AED)
                                    ) {
                                        Text(
                                            "Quét bill",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE2E8F0))
                            Text(
                                " HOẶC NHẬP TAY ",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF94A3B8),
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE2E8F0))
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        // Expense Title
                        OutlinedTextField(
                            value = itemTitle,
                            onValueChange = { itemTitle = it },
                            label = { Text("Tên khoản chi (vd: Lẩu, Trà sữa, Taxi...)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = ExpressiveItemShape,
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryGreen,
                                unfocusedBorderColor = Color(0xFFCBD5E1)
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Expense Amount
                        OutlinedTextField(
                            value = itemAmountStr,
                            onValueChange = { itemAmountStr = it },
                            label = { Text("Số tiền (hỗ trợ nhập 300k, 1.5tr hoặc 500000)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = ExpressiveItemShape,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryGreen,
                                unfocusedBorderColor = Color(0xFFCBD5E1)
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Who paid this expense?
                        Text(
                            "Ai là người đã trả khoản này?",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            items(members, key = { it.id }) { member ->
                                val isSelected = selectedPayerId == member.id
                                Surface(
                                    modifier = Modifier
                                        .clip(ExpressivePillShape)
                                        .noRippleClickable { selectedPayerId = member.id },
                                    shape = ExpressivePillShape,
                                    color = if (isSelected) Color(0xFF4F46E5) else Color(0xFFF1F5F9),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isSelected) Color(0xFF4338CA) else Color(0xFFCBD5E1)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (member.isCurrentUser) "👑 Tôi trả" else "${member.name} trả",
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) Color.White else Color(0xFF475569)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Who shares this expense? (Những người tham gia vào khoản tiền này)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Ai tham gia khoản này? (${selectedSharerIds.size}/${members.size})",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = TextPrimary
                            )

                            val isAllSelected = selectedSharerIds.size == members.size
                            TextButton(
                                onClick = {
                                    selectedSharerIds = if (isAllSelected) {
                                        setOf(defaultMe.id)
                                    } else {
                                        members.map { it.id }.toSet()
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    if (isAllSelected) Icons.Default.CheckCircle else Icons.Default.DoneAll,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = PrimaryGreen
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    if (isAllSelected) "Đang chọn tất cả" else "Chọn tất cả",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryGreen
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 1. Chip "Tất cả"
                            val isAllSelected = selectedSharerIds.size == members.size
                            item {
                                Surface(
                                    modifier = Modifier
                                        .clip(ExpressivePillShape)
                                        .noRippleClickable {
                                            selectedSharerIds = if (isAllSelected) setOf(defaultMe.id) else members.map { it.id }.toSet()
                                        },
                                    shape = ExpressivePillShape,
                                    color = if (isAllSelected) PrimaryGreen else Color(0xFFF1F5F9),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isAllSelected) PrimaryGreenDark else Color(0xFFCBD5E1)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.DoneAll,
                                            contentDescription = null,
                                            tint = if (isAllSelected) Color.White else Color(0xFF64748B),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            "Tất cả (${members.size})",
                                            fontSize = 12.sp,
                                            fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isAllSelected) Color.White else Color(0xFF475569)
                                        )
                                    }
                                }
                            }

                            // 2. Member chips
                            items(members, key = { it.id }) { member ->
                                val isSharer = selectedSharerIds.contains(member.id)
                                Surface(
                                    modifier = Modifier
                                        .clip(ExpressivePillShape)
                                        .noRippleClickable {
                                            selectedSharerIds = if (isSharer) {
                                                if (selectedSharerIds.size > 1) selectedSharerIds - member.id else selectedSharerIds
                                            } else {
                                                selectedSharerIds + member.id
                                            }
                                        },
                                    shape = ExpressivePillShape,
                                    color = if (isSharer) PrimaryContainer else Color(0xFFF8FAFC),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isSharer) PrimaryGreen else Color(0xFFE2E8F0)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            if (isSharer) Icons.Default.Check else Icons.Default.Add,
                                            contentDescription = null,
                                            tint = if (isSharer) PrimaryGreenDark else Color(0xFF94A3B8),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (member.isCurrentUser) "${member.name} (Bạn)" else member.name,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSharer) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSharer) PrimaryGreenDark else Color(0xFF64748B)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Add Expense Button
                        Button(
                            onClick = {
                                val amount = parseMoneyInput(itemAmountStr)
                                if (amount != null && amount > 0) {
                                    if (selectedSharerIds.isEmpty()) {
                                        Toast.makeText(context, "Vui lòng chọn ít nhất 1 người tham gia chia khoản này!", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    val title = if (itemTitle.isNotBlank()) itemTitle.trim() else "Khoản chi #${expenseItems.size + 1}"
                                    expenseItems = expenseItems + SplitExpenseItem(
                                        title = title,
                                        amount = amount,
                                        paidByMemberId = selectedPayerId,
                                        splitAmongMemberIds = selectedSharerIds.toList()
                                    )
                                    itemTitle = ""
                                    itemAmountStr = ""
                                    // Reset sharers to all
                                    selectedSharerIds = members.map { it.id }.toSet()
                                    // Invalidate previous calculation
                                    result = null
                                    hasSavedToPersonal = false
                                } else {
                                    Toast.makeText(context, "Vui lòng nhập số tiền hợp lệ (vd: 200k hoặc 200000)", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = ExpressiveItemShape,
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Thêm khoản chi này", fontWeight = FontWeight.Bold)
                        }

                        // Added expenses list
                        if (expenseItems.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider(color = Color(0xFFF1F5F9))
                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                "Danh sách chi tiêu đã thêm (${expenseItems.size})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = TextPrimary
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            expenseItems.forEachIndexed { index, item ->
                                val payer = members.firstOrNull { it.id == item.paidByMemberId }
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    shape = ExpressiveItemShape,
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                item.title,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = TextPrimary
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Surface(
                                                shape = ExpressivePillShape,
                                                color = if (payer?.isCurrentUser == true) PrimaryContainer else Color(0xFFEDE9FE)
                                            ) {
                                                Text(
                                                    text = if (payer?.isCurrentUser == true) "Người trả: Tôi (Bạn)" else "Người trả: ${payer?.name ?: "Khác"}",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (payer?.isCurrentUser == true) PrimaryGreenDark else Color(0xFF6D28D9),
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(3.dp))
                                            val isAllSharers = item.splitAmongMemberIds.isEmpty() || item.splitAmongMemberIds.size == members.size
                                            val sharerText = if (isAllSharers) {
                                                "Chia đều: Tất cả (${members.size} người)"
                                            } else {
                                                val sharerNames = members.filter { it.id in item.splitAmongMemberIds }
                                                    .map { if (it.isCurrentUser) "Tôi" else it.name }
                                                "Chia cho (${sharerNames.size} người): ${sharerNames.joinToString(", ")}"
                                            }
                                            Text(
                                                text = sharerText,
                                                fontSize = 10.sp,
                                                color = Color(0xFF64748B),
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                CurrencyUtils.formatVnd(item.amount),
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 14.sp,
                                                color = ExpenseRed
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            IconButton(
                                                onClick = {
                                                    expenseItems = expenseItems.filterIndexed { i, _ -> i != index }
                                                    result = null
                                                    hasSavedToPersonal = false
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.DeleteOutline,
                                                    contentDescription = "Xóa",
                                                    tint = Color(0xFF94A3B8),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Total summary banner
                            Spacer(modifier = Modifier.height(10.dp))
                            val total = expenseItems.sumOf { it.amount }
                            val avg = if (members.isNotEmpty()) total / members.size else 0.0

                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = ExpressiveItemShape,
                                color = Color(0xFFF0FDF4),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBBF7D0))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Tổng chi tiêu buổi tiệc", fontSize = 11.sp, color = Color(0xFF166534))
                                        Text(
                                            CurrencyUtils.formatVnd(total),
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF166534)
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Chia đều (${members.size} người)", fontSize = 11.sp, color = Color(0xFF15803D))
                                        Text(
                                            "${CurrencyUtils.formatVnd(avg)} / người",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PrimaryGreenDark
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Section 3: Action Button: Calculate & AI Analysis
            item {
                Button(
                    onClick = {
                        if (expenseItems.isEmpty()) {
                            Toast.makeText(context, "Vui lòng thêm ít nhất 1 khoản chi tiêu", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (members.isEmpty()) {
                            Toast.makeText(context, "Vui lòng có ít nhất 1 thành viên", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        // Step 1: Calculate deterministic math immediately
                        val calcResult = SplitBillCalculator.calculate(
                            eventTitle = eventTitle,
                            members = members,
                            items = expenseItems
                        )
                        result = calcResult

                        // Step 2: Trigger AI in background
                        isCalculatingAi = true
                        coroutineScope.launch {
                            try {
                                val itemsBreakdown = expenseItems.joinToString("\n") { item ->
                                    val payer = members.firstOrNull { it.id == item.paidByMemberId }?.name ?: "Khác"
                                    val isAllSharers = item.splitAmongMemberIds.isEmpty() || item.splitAmongMemberIds.size == members.size
                                    val sharerNames = if (isAllSharers) "tất cả mọi người" else members.filter { it.id in item.splitAmongMemberIds }.joinToString(", ") { if (it.isCurrentUser) "Tôi" else it.name }
                                    "- ${item.title}: ${CurrencyUtils.formatVnd(item.amount)} (Do $payer thanh toán, chia cho: $sharerNames)"
                                }
                                val settlementsBreakdown = if (calcResult.settlements.isEmpty()) {
                                    "Tất cả mọi người đã thanh toán bằng nhau, không ai nợ ai!"
                                } else {
                                    calcResult.settlements.joinToString("\n") { s ->
                                        "- ${s.fromMemberName} ➜ chuyển cho ${s.toMemberName}: ${CurrencyUtils.formatVnd(s.amount)}"
                                    }
                                }

                                val (summary, messageTemplate, advice) = viewModel.geminiManager.analyzeSplitBillWithAi(
                                    eventTitle = eventTitle,
                                    totalAmount = calcResult.totalAmount,
                                    perPersonAvg = calcResult.perPersonAvg,
                                    myShare = calcResult.myActualShare,
                                    itemsBreakdown = itemsBreakdown,
                                    settlementsBreakdown = settlementsBreakdown
                                )

                                result = calcResult.copy(
                                    aiSummary = summary,
                                    aiMessageTemplate = messageTemplate,
                                    aiAdvice = advice
                                )
                            } catch (e: Exception) {
                                e.printStackTrace()
                            } finally {
                                isCalculatingAi = false
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = ExpressivePillShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6D28D9)),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    if (isCalculatingAi) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("AI đang phân tích & tối ưu...", fontWeight = FontWeight.Bold, color = Color.White)
                    } else {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Tính tiền & Phân chia cùng AI", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                    }
                }
            }

            // Section 4: Split Results (Hiển thị sau khi tính tiền)
            result?.let { res ->
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        // Card A: Chi phí của Tôi (Người dùng)
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = ExpressiveCardShape,
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, PrimaryGreen)
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            "Phần chi tiêu của Tôi",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = TextSecondary
                                        )
                                        Text(
                                            CurrencyUtils.formatVnd(res.myActualShare),
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 24.sp,
                                            color = PrimaryGreenDark
                                        )
                                    }
                                    Surface(
                                        shape = ExpressivePillShape,
                                        color = PrimaryContainer
                                    ) {
                                        Text(
                                            "Công bằng 1/${members.size}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PrimaryGreenDark,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Status: Who owes me or who do I owe?
                                if (res.myNetBalance > 1.0) {
                                    Surface(
                                        shape = ExpressiveItemShape,
                                        color = Color(0xFFF0FDF4),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBBF7D0)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                "🎉 Bạn đã trả trước nhiều hơn phần của mình. Các bạn trong nhóm cần chuyển trả lại cho bạn tổng cộng: ",
                                                fontSize = 12.sp,
                                                color = Color(0xFF166534)
                                            )
                                            Text(
                                                CurrencyUtils.formatVnd(res.myNetBalance),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color(0xFF166534)
                                            )
                                        }
                                    }
                                } else if (res.myNetBalance < -1.0) {
                                    Surface(
                                        shape = ExpressiveItemShape,
                                        color = ExpenseRedContainer,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                "👉 Bạn cần chuyển khoản phần của mình: ",
                                                fontSize = 12.sp,
                                                color = Color(0xFF991B1B)
                                            )
                                            Text(
                                                CurrencyUtils.formatVnd(-res.myNetBalance),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = ExpenseRed
                                            )
                                        }
                                    }
                                } else {
                                    Text(
                                        "✓ Bạn đã trả đúng số tiền phần của mình!",
                                        fontSize = 12.sp,
                                        color = PrimaryGreenDark,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        // Card B: AI Settlement Matrix ("Ai chuyển cho ai")
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = ExpressiveCardShape,
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = Color(0xFF6D28D9), modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            "Ai chuyển cho ai? (${res.settlements.size} giao dịch)",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = TextPrimary
                                        )
                                    }

                                    // Copy Zalo / Messenger message button
                                    TextButton(
                                        onClick = {
                                            val copyText = buildString {
                                                appendLine("💸 TỔNG KẾT CHIA TIỀN: ${res.eventTitle.uppercase()}")
                                                appendLine("• Tổng chi: ${CurrencyUtils.formatVnd(res.totalAmount)}")
                                                appendLine("• Chia đều (${members.size} người): ${CurrencyUtils.formatVnd(res.perPersonAvg)}/người")
                                                appendLine()
                                                appendLine("👉 CÁC KHOẢN CẦN CHUYỂN:")
                                                if (res.settlements.isEmpty()) {
                                                    appendLine("Mọi người đã thanh toán bằng nhau, không ai nợ ai!")
                                                } else {
                                                    res.settlements.forEach { s ->
                                                        appendLine("• ${s.fromMemberName} ➜ chuyển cho ${s.toMemberName}: ${CurrencyUtils.formatVnd(s.amount)}")
                                                    }
                                                }
                                                appendLine()
                                                appendLine("Mọi người kiểm tra và chuyển khoản sòng phẳng nhé! Cảm ơn cả nhà ❤️")
                                            }
                                            clipboardManager.setText(AnnotatedString(copyText))
                                            Toast.makeText(context, "Đã sao chép tin nhắn chia tiền để gửi nhóm Zalo/Messenger!", Toast.LENGTH_SHORT).show()
                                        }
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp), tint = PrimaryGreen)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Sao chép", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PrimaryGreen)
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                if (res.settlements.isEmpty()) {
                                    Text(
                                        "Mọi người đã thanh toán tiền bằng nhau, không cần chuyển thêm!",
                                        fontSize = 13.sp,
                                        color = Color(0xFF64748B)
                                    )
                                } else {
                                    res.settlements.forEach { settlement ->
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            shape = ExpressiveItemShape,
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Surface(
                                                        shape = ExpressivePillShape,
                                                        color = Color(0xFFFEE2E2)
                                                    ) {
                                                        Text(
                                                            settlement.fromMemberName,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 12.sp,
                                                            color = Color(0xFF991B1B),
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Icon(
                                                        Icons.AutoMirrored.Filled.ArrowForward,
                                                        contentDescription = null,
                                                        tint = Color(0xFF94A3B8),
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Surface(
                                                        shape = ExpressivePillShape,
                                                        color = Color(0xFFDCFCE7)
                                                    ) {
                                                        Text(
                                                            settlement.toMemberName,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 12.sp,
                                                            color = Color(0xFF166534),
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                        )
                                                    }
                                                }

                                                Text(
                                                    CurrencyUtils.formatVnd(settlement.amount),
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 14.sp,
                                                    color = Color(0xFF4F46E5)
                                                )
                                            }
                                        }
                                    }
                                }

                                // AI Smart Message Box (if generated)
                                res.aiSummary?.let { aiSumm ->
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Surface(
                                        shape = ExpressiveItemShape,
                                        color = Color(0xFFFAF5FF),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE9D5FF)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.SmartToy, contentDescription = null, tint = Color(0xFF7C3AED), modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Lời nhận xét từ Trợ lý AI", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF6D28D9))
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(aiSumm, fontSize = 12.sp, color = Color(0xFF4C1D95), lineHeight = 17.sp)

                                            res.aiAdvice?.let { advice ->
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text("💡 Mẹo tài chính: $advice", fontSize = 11.sp, color = Color(0xFF6D28D9), fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Thẻ Xuất & Lưu file Excel / CSV vào bộ nhớ máy
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = ExpressiveCardShape,
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (savedCsvResult != null) Color(0xFFBBF7D0) else Color(0xFFDDD6FE)
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (savedCsvResult != null) Color(0xFFDCFCE7) else Color(0xFFEDE9FE)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (savedCsvResult != null) Icons.Default.CheckCircle else Icons.Default.FileDownload,
                                            contentDescription = null,
                                            tint = if (savedCsvResult != null) PrimaryGreen else Color(0xFF6D28D9),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            if (savedCsvResult != null) "Đã lưu bảng tính vào máy" else "Xuất bảng tính Excel / CSV",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = TextPrimary
                                        )
                                        Text(
                                            if (savedCsvResult != null) "Tệp: ${savedCsvResult?.fileName}" else "Lưu vào thư mục Tải xuống (Downloads) để duyệt lại trước khi chia sẻ",
                                            fontSize = 11.sp,
                                            color = TextSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                if (savedCsvResult == null) {
                                    // Bấm để lưu vào bộ nhớ máy (thư mục Tải xuống / Downloads)
                                    Button(
                                        onClick = {
                                            val saveRes = CsvExporter.saveSplitBillToDownloads(
                                                context = context,
                                                result = res,
                                                items = expenseItems,
                                                members = members
                                            )
                                            if (saveRes != null) {
                                                savedCsvResult = saveRes
                                                showSavedSuccessDialog = true
                                                Toast.makeText(context, "Đã lưu vào thư mục Tải xuống: ${saveRes.fileName}", Toast.LENGTH_LONG).show()
                                            } else {
                                                Toast.makeText(context, "Không thể lưu file vào bộ nhớ", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp),
                                        shape = ExpressivePillShape,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF4F46E5),
                                            contentColor = Color.White
                                        )
                                    ) {
                                        Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Lưu file vào bộ nhớ máy (Downloads)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                } else {
                                    // Sau khi lưu: Hiển thị 2 nút Mở duyệt file & Chia sẻ thủ công
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                savedCsvResult?.let { csvRes ->
                                                    CsvExporter.openCsvFile(context, csvRes.providerUri)
                                                }
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(44.dp),
                                            shape = ExpressivePillShape,
                                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                                        ) {
                                            Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Mở duyệt file", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                savedCsvResult?.let { csvRes ->
                                                    CsvExporter.shareCsvFile(
                                                        context = context,
                                                        file = csvRes.cacheFile,
                                                        subject = "Báo cáo phân chia tiền: ${res.eventTitle}"
                                                    )
                                                }
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(44.dp),
                                            shape = ExpressivePillShape,
                                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4F46E5)),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF4F46E5))
                                        ) {
                                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Chia sẻ file", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }

                        // Card C: Tự động lưu chi tiêu của Tôi vào chi tiêu cá nhân ở trang chủ
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = ExpressiveCardShape,
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(PrimaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(16.dp))
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            "Lưu phần của tôi vào chi tiêu cá nhân",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = TextPrimary
                                        )
                                        Text(
                                            "Tự động ghi nhận khoản chi ${CurrencyUtils.formatVnd(res.myActualShare)} vào trang chủ",
                                            fontSize = 11.sp,
                                            color = TextSecondary
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Select wallet / account
                                Text("Trừ từ Ví nguồn tiền nào?", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                Spacer(modifier = Modifier.height(6.dp))
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    items(accounts, key = { it.id }) { acc ->
                                        val isAccSelected = selectedAccountId == acc.id
                                        val accColor = parseColor(acc.colorHex)
                                        Surface(
                                            modifier = Modifier
                                                .clip(ExpressivePillShape)
                                                .noRippleClickable { selectedAccountId = acc.id },
                                            shape = ExpressivePillShape,
                                            color = if (isAccSelected) accColor.copy(alpha = 0.15f) else Color(0xFFF1F5F9),
                                            border = androidx.compose.foundation.BorderStroke(
                                                if (isAccSelected) 1.5.dp else 1.dp,
                                                if (isAccSelected) accColor else Color(0xFFE2E8F0)
                                            )
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                CategoryIcon(acc.iconName, tint = accColor, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    acc.name,
                                                    fontSize = 11.sp,
                                                    fontWeight = if (isAccSelected) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isAccSelected) accColor else TextPrimary
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Select category
                                Text("Danh mục chi tiêu:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                Spacer(modifier = Modifier.height(6.dp))
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val expenseCats = categories.filter { it.type == com.expensetracker.app.data.local.entity.TransactionType.EXPENSE }
                                    items(expenseCats, key = { it.id }) { cat ->
                                        val isCatSelected = selectedCategoryId == cat.id
                                        val catColor = parseColor(cat.colorHex)
                                        Surface(
                                            modifier = Modifier
                                                .clip(ExpressivePillShape)
                                                .noRippleClickable { selectedCategoryId = cat.id },
                                            shape = ExpressivePillShape,
                                            color = if (isCatSelected) catColor.copy(alpha = 0.15f) else Color(0xFFF1F5F9),
                                            border = androidx.compose.foundation.BorderStroke(
                                                if (isCatSelected) 1.5.dp else 1.dp,
                                                if (isCatSelected) catColor else Color(0xFFE2E8F0)
                                            )
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                CategoryIcon(cat.iconName, tint = catColor, modifier = Modifier.size(12.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    cat.name,
                                                    fontSize = 11.sp,
                                                    fontWeight = if (isCatSelected) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isCatSelected) catColor else TextPrimary
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                if (hasSavedToPersonal) {
                                    Surface(
                                        shape = ExpressivePillShape,
                                        color = Color(0xFFF0FDF4),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBBF7D0)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("✓ Đã lưu thành công vào chi tiêu cá nhân!", fontWeight = FontWeight.Bold, color = Color(0xFF166534), fontSize = 13.sp)
                                        }
                                    }
                                } else {
                                    Button(
                                        onClick = {
                                            val targetAccId = selectedAccountId ?: accounts.firstOrNull()?.id ?: 1L
                                            viewModel.addPersonalSplitExpense(
                                                amount = res.myActualShare,
                                                accountId = targetAccId,
                                                categoryId = selectedCategoryId,
                                                eventTitle = res.eventTitle
                                            )
                                            hasSavedToPersonal = true
                                            Toast.makeText(context, "Đã thêm ${CurrencyUtils.formatVnd(res.myActualShare)} vào chi tiêu cá nhân!", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = ExpressivePillShape,
                                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                                    ) {
                                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Xác nhận lưu vào chi tiêu cá nhân", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // Duy nhất 1 nút Trở về trang chủ sau khi chia tiền
                        FilledTonalButton(
                            onClick = onNavigateToHome,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = ExpressivePillShape,
                            colors = if (hasSavedToPersonal) {
                                ButtonDefaults.filledTonalButtonColors(containerColor = PrimaryGreen, contentColor = Color.White)
                            } else {
                                ButtonDefaults.filledTonalButtonColors()
                            }
                        ) {
                            Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Trở về Trang chủ", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
        }
    }

    // Source Sheet Dialog (Camera or Gallery)
    if (showSourceSheet) {
        AlertDialog(
            onDismissRequest = { showSourceSheet = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = Color(0xFF6D28D9))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Quét hóa đơn bằng AI", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Chọn cách tải ảnh hóa đơn/bill để AI tự động nhận diện danh sách các món ăn và số tiền:",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    // Option 1: Take Photo with Camera
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(ExpressiveItemShape)
                            .noRippleClickable {
                                showSourceSheet = false
                                cameraLauncher.launch(null)
                            },
                        shape = ExpressiveItemShape,
                        color = Color(0xFFF5F3FF),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDDD6FE))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF8B5CF6)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Chụp ảnh hóa đơn mới", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF4C1D95))
                                Text("Mở camera chụp trực tiếp bill tính tiền", fontSize = 11.sp, color = Color(0xFF6D28D9))
                            }
                        }
                    }

                    // Option 2: Pick from Gallery
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(ExpressiveItemShape)
                            .noRippleClickable {
                                showSourceSheet = false
                                galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                        shape = ExpressiveItemShape,
                        color = Color(0xFFF0FDF4),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBBF7D0))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryGreen),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Chọn ảnh từ Thư viện", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF166534))
                                Text("Tải ảnh chụp màn hình hoặc bill đã lưu", fontSize = 11.sp, color = Color(0xFF15803D))
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showSourceSheet = false }) {
                    Text("Đóng", color = Color(0xFF64748B))
                }
            },
            shape = ExpressiveCardShape
        )
    }

    // Assign Bill Items Dialog
    if (showAssignDialog) {
        AssignBillItemsDialog(
            items = scannedItems,
            members = members,
            defaultPayerId = scannedBillPayerId,
            onDismiss = { showAssignDialog = false },
            onConfirm = { updatedItems, billPayerId ->
                val newSplitItems = updatedItems.map { item ->
                    val sharers = if (item.assignedMemberIds.isEmpty()) members.map { it.id } else item.assignedMemberIds.toList()
                    val itemTitle = if (item.quantity > 1) "${item.name} (x${item.quantity})" else item.name
                    SplitExpenseItem(
                        title = itemTitle,
                        amount = item.amount,
                        paidByMemberId = billPayerId,
                        splitAmongMemberIds = sharers
                    )
                }
                expenseItems = expenseItems + newSplitItems
                result = null
                hasSavedToPersonal = false
                showAssignDialog = false
                Toast.makeText(context, "Đã thêm ${newSplitItems.size} món từ hóa đơn vào buổi chia tiền!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Dialog xác nhận đã lưu file thành công & hỗ trợ mở duyệt ngay
    if (showSavedSuccessDialog && savedCsvResult != null) {
        val currentSaved = savedCsvResult!!
        AlertDialog(
            onDismissRequest = { showSavedSuccessDialog = false },
            shape = ExpressiveDialogShape,
            icon = {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(36.dp))
            },
            title = {
                Text(
                    "Đã lưu vào bộ nhớ máy!",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Tệp bảng tính đã được lưu vào thư mục Tải xuống (Downloads) của máy:",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    Surface(
                        shape = ExpressiveItemShape,
                        color = Color(0xFFF1F5F9),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = currentSaved.fileName,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF334155),
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                    Text(
                        "Bạn có thể mở xem duyệt lại các khoản tính toán ngay bây giờ bằng Microsoft Excel hoặc Google Sheets, hoặc chia sẻ thủ công cho bạn bè sau khi duyệt xong.",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B),
                        lineHeight = 15.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSavedSuccessDialog = false
                        CsvExporter.openCsvFile(context, currentSaved.providerUri)
                    },
                    shape = ExpressivePillShape,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                ) {
                    Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Mở duyệt file ngay", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showSavedSuccessDialog = false },
                    shape = ExpressivePillShape
                ) {
                    Text("Đóng", fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignBillItemsDialog(
    items: List<ScannedReceiptItem>,
    members: List<SplitMember>,
    defaultPayerId: String,
    onDismiss: () -> Unit,
    onConfirm: (List<ScannedReceiptItem>, String) -> Unit
) {
    val context = LocalContext.current
    var billPayerId by remember { mutableStateOf(defaultPayerId) }
    var currentItems by remember { mutableStateOf(items) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.9f),
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(ExpressiveSquircleIconShape)
                            .background(Color(0xFFEDE9FE)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = Color(0xFF6D28D9), modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Phân bổ món từ Bill", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                        Text("${currentItems.size} món • Chọn ai dùng món nào", fontSize = 11.sp, color = TextSecondary)
                    }
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Đóng", tint = Color(0xFF64748B))
                }
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Section 1: Who paid the whole bill?
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = ExpressiveItemShape,
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                "Ai đã thanh toán cả hóa đơn này?",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(members, key = { it.id }) { member ->
                                    val isPayer = billPayerId == member.id
                                    Surface(
                                        modifier = Modifier
                                            .clip(ExpressivePillShape)
                                            .noRippleClickable { billPayerId = member.id },
                                        shape = ExpressivePillShape,
                                        color = if (isPayer) Color(0xFF4F46E5) else Color.White,
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (isPayer) Color(0xFF4338CA) else Color(0xFFCBD5E1)
                                        )
                                    ) {
                                        Text(
                                            text = if (member.isCurrentUser) "👑 Tôi trả" else "${member.name} trả",
                                            fontSize = 11.sp,
                                            fontWeight = if (isPayer) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isPayer) Color.White else TextPrimary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Section 2: Items list
                itemsIndexed(currentItems, key = { _, it -> it.id }) { index, item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = ExpressiveItemShape,
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        item.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = TextPrimary
                                    )
                                    Text(
                                        CurrencyUtils.formatVnd(item.amount),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = ExpenseRed
                                    )
                                }

                                // Stepper điều chỉnh số lượng (quantity)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .background(Color(0xFFF1F5F9), RoundedCornerShape(8.dp))
                                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 2.dp, vertical = 1.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .noRippleClickable {
                                                if (item.quantity > 1) {
                                                    val newQty = item.quantity - 1
                                                    val updated = currentItems.toMutableList()
                                                    val newAssigned = if (item.assignedMemberIds.size > newQty) {
                                                        item.assignedMemberIds.take(newQty).toSet()
                                                    } else {
                                                        item.assignedMemberIds
                                                    }
                                                    updated[index] = item.copy(quantity = newQty, assignedMemberIds = newAssigned)
                                                    currentItems = updated
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Remove, contentDescription = "Giảm", tint = if (item.quantity > 1) TextPrimary else Color(0xFFCBD5E1), modifier = Modifier.size(13.dp))
                                    }
                                    Text(
                                        "SL: ${item.quantity}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (item.quantity > 1) Color(0xFF4F46E5) else TextPrimary,
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .noRippleClickable {
                                                val newQty = item.quantity + 1
                                                val updated = currentItems.toMutableList()
                                                updated[index] = item.copy(quantity = newQty)
                                                currentItems = updated
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Tăng", tint = PrimaryGreenDark, modifier = Modifier.size(13.dp))
                                    }
                                }

                                Spacer(modifier = Modifier.width(4.dp))

                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .noRippleClickable {
                                            currentItems = currentItems.filterIndexed { i, _ -> i != index }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Xóa", tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Hint text based on quantity
                            val assignedCount = item.assignedMemberIds.size
                            val hintText = if (item.quantity <= 1) {
                                if (assignedCount == 0) "Chưa chọn ai (bấm người bên dưới để chọn):" else {
                                    val person = members.firstOrNull { it.id == item.assignedMemberIds.firstOrNull() }
                                    val displayName = if (person?.isCurrentUser == true) "Tôi" else person?.name ?: "1 người"
                                    "Đã chọn: $displayName (bấm lại để đổi hoặc bỏ chọn)"
                                }
                            } else {
                                if (assignedCount == 0) {
                                    "Chưa chọn ai (chọn tối đa ${item.quantity} người):"
                                } else {
                                    "Đã chọn ${assignedCount}/${item.quantity} người cùng dùng:"
                                }
                            }
                            Text(
                                hintText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = when {
                                    assignedCount == 0 -> Color(0xFFE11D48)
                                    item.quantity > 1 && assignedCount >= item.quantity -> PrimaryGreenDark
                                    else -> Color(0xFF4338CA)
                                }
                            )
                            Spacer(modifier = Modifier.height(4.dp))

                            // Member selection chips for this item (KHÔNG CÓ "Tất cả")
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(members, key = { it.id }) { member ->
                                    val isSelected = item.assignedMemberIds.contains(member.id)
                                    Surface(
                                        modifier = Modifier
                                            .clip(ExpressivePillShape)
                                            .noRippleClickable {
                                                val updated = currentItems.toMutableList()
                                                val newSet: Set<String>
                                                if (item.quantity <= 1) {
                                                    // Mỗi người chỉ uống 1 món: Chọn đơn
                                                    newSet = if (isSelected) {
                                                        emptySet()
                                                    } else {
                                                        setOf(member.id)
                                                    }
                                                } else {
                                                    // Số lượng từ 2 trở lên: Chọn tối đa item.quantity người
                                                    if (isSelected) {
                                                        newSet = item.assignedMemberIds - member.id
                                                    } else {
                                                        if (item.assignedMemberIds.size < item.quantity) {
                                                            newSet = item.assignedMemberIds + member.id
                                                        } else {
                                                            Toast.makeText(
                                                                context,
                                                                "Món này có số lượng là ${item.quantity}, đã chọn đủ ${item.quantity} người!",
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                            newSet = item.assignedMemberIds
                                                        }
                                                    }
                                                }
                                                updated[index] = item.copy(assignedMemberIds = newSet)
                                                currentItems = updated
                                            },
                                        shape = ExpressivePillShape,
                                        color = if (isSelected) PrimaryContainer else Color(0xFFF8FAFC),
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (isSelected) PrimaryGreen else Color(0xFFE2E8F0)
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (isSelected) {
                                                Icon(Icons.Default.Check, contentDescription = null, tint = PrimaryGreenDark, modifier = Modifier.size(11.dp))
                                                Spacer(modifier = Modifier.width(3.dp))
                                            }
                                            Text(
                                                text = if (member.isCurrentUser) "Tôi" else member.name,
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) PrimaryGreenDark else Color(0xFF64748B)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val unassigned = currentItems.filter { it.assignedMemberIds.isEmpty() }
                    if (unassigned.isNotEmpty()) {
                        Toast.makeText(
                            context,
                            "Còn ${unassigned.size} món chưa chọn người dùng. Vui lòng chọn người cho từng món!",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@Button
                    }
                    onConfirm(currentItems, billPayerId)
                },
                shape = ExpressivePillShape,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Xác nhận & Thêm ${currentItems.size} món vào buổi chia", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        },
        dismissButton = {},
        shape = ExpressiveCardShape
    )
}

@Composable
private fun Modifier.noRippleClickable(
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier = this.clickable(
    interactionSource = remember { MutableInteractionSource() },
    indication = null,
    enabled = enabled,
    onClick = onClick
)

private fun parseMoneyInput(raw: String): Double? {
    val clean = raw.trim().lowercase()
    if (clean.isBlank()) return null

    val kIdx = clean.indexOf('k')
    if (kIdx != -1) {
        val num = clean.substring(0, kIdx).filter { it.isDigit() || it == '.' || it == ',' }
            .replace(",", ".").toDoubleOrNull()
        if (num != null) return num * 1000
    }

    val trIdx = clean.indexOf("tr")
    if (trIdx != -1) {
        val num = clean.substring(0, trIdx).filter { it.isDigit() || it == '.' || it == ',' }
            .replace(",", ".").toDoubleOrNull()
        if (num != null) return num * 1000000
    }

    val cuIdx = clean.indexOf("củ")
    if (cuIdx != -1) {
        val num = clean.substring(0, cuIdx).filter { it.isDigit() || it == '.' || it == ',' }
            .replace(",", ".").toDoubleOrNull()
        if (num != null) return num * 1000000
    }

    val numeric = clean.filter { it.isDigit() }.toDoubleOrNull()
    return numeric
}
