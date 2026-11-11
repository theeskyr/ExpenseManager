package com.expensetracker.app.ui.screens.trips

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.expensetracker.app.data.local.entity.TripEntity
import com.expensetracker.app.data.local.entity.TripExpenseEntity
import com.expensetracker.app.data.model.ScannedReceiptItem
import com.expensetracker.app.data.model.SplitMember
import com.expensetracker.app.ui.screens.splitbill.AssignBillItemsDialog
import com.expensetracker.app.ui.theme.*
import com.expensetracker.app.ui.viewmodel.ExpenseViewModel
import com.expensetracker.app.utils.CurrencyUtils
import com.expensetracker.app.utils.ImageUtils
import com.expensetracker.app.utils.TripJsonUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripDetailScreen(
    tripId: String,
    viewModel: ExpenseViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToSplitBill: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val trip by viewModel.getTripByIdFlow(tripId).collectAsState(initial = null)
    val expenses by viewModel.getTripExpenses(tripId).collectAsState(initial = emptyList())

    var selectedDayIndex by remember { mutableStateOf(1) }
    var showAddExpenseDialog by remember { mutableStateOf(false) }

    // Bill Scanning States (Quét hóa đơn bill bằng AI cho chuyến đi)
    var isScanningReceipt by remember { mutableStateOf(false) }
    var scannedItems by remember { mutableStateOf<List<ScannedReceiptItem>>(emptyList()) }
    var showAssignDialog by remember { mutableStateOf(false) }
    var scannedBillPayerId by remember { mutableStateOf("") }
    var showSourceSheet by remember { mutableStateOf(false) }

    val currentTrip = trip
    if (currentTrip == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = PrimaryGreen)
        }
        return
    }

    val members = remember(currentTrip.membersJson) { TripJsonUtils.jsonToMembers(currentTrip.membersJson) }
    val memberMap = remember(members) { members.associateBy { it.id } }

    // Bill Scanner Processing function
    fun processReceiptBitmap(bitmap: Bitmap) {
        isScanningReceipt = true
        coroutineScope.launch {
            try {
                val parsed = viewModel.geminiManager.parseReceiptForSplitBill(bitmap)
                if (parsed.isNotEmpty()) {
                    scannedItems = parsed.map { it.copy(assignedMemberIds = emptySet()) }
                    scannedBillPayerId = members.firstOrNull()?.id ?: ""
                    showAssignDialog = true
                } else {
                    Toast.makeText(
                        context,
                        "Không nhận diện được món ăn nào từ hóa đơn. Vui lòng thử lại với ảnh rõ nét hơn!",
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

    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success) {
            tempCameraUri?.let { uri ->
                val bitmap = ImageUtils.loadAndOptimizeBitmap(context, uri)
                if (bitmap != null) {
                    processReceiptBitmap(bitmap)
                } else {
                    Toast.makeText(context, "Không thể đọc ảnh chụp từ máy ảnh", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            val bitmap = ImageUtils.loadAndOptimizeBitmap(context, it)
            if (bitmap != null) {
                processReceiptBitmap(bitmap)
            } else {
                Toast.makeText(context, "Không thể đọc ảnh từ thư viện", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Calculate maximum day index present, at least 1
    val maxDayPresent = remember(expenses) { (expenses.maxOfOrNull { it.dayIndex } ?: 1).coerceAtLeast(1) }
    val availableDays = remember(maxDayPresent) { (1..maxDayPresent).toList() }

    // Group expenses by day
    val expensesByDay = remember(expenses) { expenses.groupBy { it.dayIndex } }
    val currentDayExpenses = expensesByDay[selectedDayIndex] ?: emptyList()
    val currentDayTotal = remember(currentDayExpenses) { currentDayExpenses.sumOf { it.amount } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            currentTrip.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            maxLines = 1
                        )
                        Text(
                            if (currentTrip.destination.isNotBlank()) "Điểm đến: ${currentTrip.destination}" else "Theo dõi chi tiêu theo ngày",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundLight)
            )
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
                // Trip Overview Card
                item {
                    Spacer(modifier = Modifier.height(2.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = ExpressiveCardShape,
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(Color(0xFF0284C7), Color(0xFF0369A1), Color(0xFF075985))
                                    )
                                )
                                .padding(18.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = ExpressivePillShape,
                                        color = if (currentTrip.isOngoing) Color(0xFFDCFCE7) else Color.White.copy(alpha = 0.25f)
                                    ) {
                                        Text(
                                            text = if (currentTrip.isOngoing) "🟢 ĐANG DIỄN RA" else "✓ ĐÃ HOÀN TẤT & CHIA TIỀN",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (currentTrip.isOngoing) Color(0xFF166534) else Color.White,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }

                                    Text(
                                        text = "${members.size} người tham gia",
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.9f)
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    "Tổng chi tiêu cả chuyến đi",
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                                Text(
                                    text = CurrencyUtils.formatVnd(currentTrip.totalExpense),
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )

                                Spacer(modifier = Modifier.height(8.dp))
                                // Members preview
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(members, key = { it.id }) { m ->
                                        Surface(
                                            shape = ExpressivePillShape,
                                            color = Color.White.copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                text = if (m.isCurrentUser) "Tôi" else m.name,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Days Selector Tabs
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Chi tiêu theo từng ngày",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(
                            onClick = {
                                selectedDayIndex = maxDayPresent + 1
                            }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF0284C7))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Thêm Ngày ${maxDayPresent + 1}", fontWeight = FontWeight.Bold, color = Color(0xFF0284C7), fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    val allDaysToShow = if (selectedDayIndex > maxDayPresent) {
                        availableDays + selectedDayIndex
                    } else {
                        availableDays
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items(allDaysToShow) { day ->
                            val isSelected = selectedDayIndex == day
                            val dayCount = expensesByDay[day]?.size ?: 0

                            Surface(
                                modifier = Modifier
                                    .clip(ExpressivePillShape)
                                    .clickable { selectedDayIndex = day },
                                shape = ExpressivePillShape,
                                color = if (isSelected) Color(0xFF0284C7) else Color.White,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) Color(0xFF0369A1) else Color(0xFFE2E8F0)
                                ),
                                shadowElevation = if (isSelected) 2.dp else 0.dp
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Ngày $day",
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 12.sp,
                                        color = if (isSelected) Color.White else TextPrimary
                                    )
                                    if (dayCount > 0) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = ExpressivePillShape,
                                            color = if (isSelected) Color.White.copy(alpha = 0.25f) else Color(0xFFE0F2FE)
                                        ) {
                                            Text(
                                                "$dayCount món",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) Color.White else Color(0xFF0369A1),
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Current Day Header & Actions (Quét Bill AI + Thêm thủ công)
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = ExpressiveCardShape,
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        "Tổng chi Ngày $selectedDayIndex",
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                    Text(
                                        text = CurrencyUtils.formatVnd(currentDayTotal),
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (currentDayTotal > 0) Color(0xFF0284C7) else TextPrimary
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    // Nút Quét bill AI nhanh
                                    FilledTonalButton(
                                        onClick = { showSourceSheet = true },
                                        shape = ExpressivePillShape,
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = Color(0xFFEDE9FE),
                                            contentColor = Color(0xFF6D28D9)
                                        ),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                        enabled = !isScanningReceipt
                                    ) {
                                        if (isScanningReceipt) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(14.dp),
                                                color = Color(0xFF6D28D9),
                                                strokeWidth = 2.dp
                                            )
                                        } else {
                                            Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(15.dp))
                                        }
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Quét bill AI ⚡", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }

                                    // Nút Thêm thủ công
                                    Button(
                                        onClick = { showAddExpenseDialog = true },
                                        shape = ExpressivePillShape,
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Thêm chi tiêu", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                // Current Day Expenses List
                if (currentDayExpenses.isNotEmpty()) {
                    items(currentDayExpenses, key = { it.id }) { exp ->
                        val payerName = exp.payerMemberName.ifBlank { memberMap[exp.payerMemberId]?.name ?: "Tôi" }
                        val sharers = TripJsonUtils.jsonToStringList(exp.sharerMemberIdsJson)
                        val sharerText = if (sharers.isEmpty() || sharers.size == members.size) {
                            "Tất cả (${members.size} người)"
                        } else {
                            sharers.mapNotNull { memberMap[it]?.name }.joinToString(", ")
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = ExpressiveItemShape,
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9))
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
                                        exp.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = TextPrimary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            "Trả bởi: $payerName",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF0284C7)
                                        )
                                        Text(" • ", fontSize = 11.sp, color = TextSecondary)
                                        Text(
                                            "Chia: $sharerText",
                                            fontSize = 11.sp,
                                            color = TextSecondary
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        CurrencyUtils.formatVnd(exp.amount),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.sp,
                                        color = ExpenseRed
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    IconButton(
                                        onClick = { viewModel.deleteTripExpense(exp) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Xóa", tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                } else {
                    item {
                        Surface(
                            shape = ExpressiveCardShape,
                            color = Color(0xFFF8FAFC),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(28.dp))
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Chưa có khoản chi nào trong Ngày $selectedDayIndex", fontSize = 12.sp, color = TextSecondary)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Quét hóa đơn AI hoặc nhập tay để thêm khoản chi!", fontSize = 11.sp, color = Color(0xFF0284C7), fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }

                // Final Action: Complete Trip & AI Split Bill
                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = ExpressiveCardShape,
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDDD6FE)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFEDE9FE)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFF7C3AED), modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        if (currentTrip.isOngoing) "Kết thúc chuyến đi & Chia tiền AI" else "Xem lại kết quả chia tiền AI",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = TextPrimary
                                    )
                                    Text(
                                        "Tự động tính toán và cho kết quả chia tiền ngay lập tức",
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                }
                            }

                            currentTrip.settlementSummary?.let { summary ->
                                Spacer(modifier = Modifier.height(10.dp))
                                Surface(
                                    shape = ExpressiveItemShape,
                                    color = Color(0xFFFAF5FF),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE9D5FF)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = summary,
                                        fontSize = 12.sp,
                                        color = Color(0xFF4C1D95),
                                        modifier = Modifier.padding(10.dp),
                                        lineHeight = 16.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Button(
                                onClick = {
                                    if (expenses.isEmpty()) {
                                        Toast.makeText(context, "Vui lòng nhập ít nhất 1 khoản chi tiêu cho chuyến đi trước khi chia tiền!", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    // Chuyển toàn bộ dữ liệu chuyến đi sang màn hình Chia tiền AI và tự động tính toán luôn
                                    viewModel.prepareSplitBillFromTrip(currentTrip, expenses, autoCalculate = true)
                                    onNavigateToSplitBill()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                shape = ExpressivePillShape,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5))
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    if (currentTrip.isOngoing) "Tính toán chia tiền bằng AI ngay ⚡" else "Xem lại bảng chia tiền AI",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
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
                    Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null, tint = Color(0xFF6D28D9))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Quét hóa đơn cho Ngày $selectedDayIndex", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Chọn cách tải ảnh hóa đơn/bill để AI tự động bóc tách danh sách món ăn và số tiền:",
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
                                val newUri = ImageUtils.createCameraTempUri(context)
                                tempCameraUri = newUri
                                cameraLauncher.launch(newUri)
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
                                Text("Tải ảnh chụp bill đã lưu trong máy", fontSize = 11.sp, color = Color(0xFF15803D))
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

    // Assign Bill Items Dialog (Mở sau khi quét bill AI thành công)
    if (showAssignDialog) {
        AssignBillItemsDialog(
            items = scannedItems,
            members = members,
            defaultPayerId = scannedBillPayerId.ifBlank { members.firstOrNull()?.id ?: "" },
            onDismiss = { showAssignDialog = false },
            onConfirm = { updatedItems, billPayerId ->
                val payerName = memberMap[billPayerId]?.name ?: "Tôi"
                updatedItems.forEach { item ->
                    val sharers = if (item.assignedMemberIds.isEmpty()) members.map { it.id } else item.assignedMemberIds.toList()
                    val formattedTitle = if (item.quantity > 1) "${item.name} (x${item.quantity})" else item.name
                    viewModel.addTripExpense(
                        tripId = currentTrip.id,
                        dayIndex = selectedDayIndex,
                        title = formattedTitle,
                        amount = item.amount,
                        payerMemberId = billPayerId,
                        payerMemberName = payerName,
                        sharerMemberIds = sharers,
                        note = "Quét từ hóa đơn AI"
                    )
                }
                showAssignDialog = false
                Toast.makeText(context, "Đã thêm ${updatedItems.size} món từ hóa đơn vào Ngày $selectedDayIndex!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Dialog Add Expense for Day (Hỗ trợ nhập tắt k, tr, củ và quét bill trực tiếp)
    if (showAddExpenseDialog) {
        AddTripExpenseDialog(
            dayIndex = selectedDayIndex,
            members = members,
            onScanReceipt = {
                showAddExpenseDialog = false
                showSourceSheet = true
            },
            onDismiss = { showAddExpenseDialog = false },
            onAdd = { title, amount, payerId, payerName, sharerIds, note, keepAdding ->
                viewModel.addTripExpense(
                    tripId = currentTrip.id,
                    dayIndex = selectedDayIndex,
                    title = title,
                    amount = amount,
                    payerMemberId = payerId,
                    payerMemberName = payerName,
                    sharerMemberIds = sharerIds,
                    note = note
                )
                if (!keepAdding) {
                    showAddExpenseDialog = false
                }
                Toast.makeText(context, "Đã thêm: $title (${CurrencyUtils.formatVnd(amount)})", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
fun AddTripExpenseDialog(
    dayIndex: Int,
    members: List<SplitMember>,
    onScanReceipt: () -> Unit,
    onDismiss: () -> Unit,
    onAdd: (title: String, amount: Double, payerId: String, payerName: String, sharers: List<String>, note: String, keepAdding: Boolean) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("") }
    var selectedPayerId by remember { mutableStateOf(members.firstOrNull()?.id ?: "") }
    var selectedSharerIds by remember { mutableStateOf(members.map { it.id }.toSet()) }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = ExpressiveDialogShape,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(ExpressiveSquircleIconShape)
                        .background(Color(0xFFE0F2FE)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("Thêm chi tiêu Ngày $dayIndex", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Banner Quét bill AI
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(ExpressiveItemShape)
                        .noRippleClickable { onScanReceipt() },
                    shape = ExpressiveItemShape,
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F3FF)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDDD6FE))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(ExpressiveSquircleIconShape)
                                .background(Color(0xFF8B5CF6)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Chụp bill / Quét hóa đơn AI ⚡", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF5B21B6))
                            Text("AI tự bóc tách món & chia từng người", fontSize = 10.sp, color = Color(0xFF6D28D9))
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF8B5CF6), modifier = Modifier.size(16.dp))
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE2E8F0))
                    Text(" HOẶC NHẬP TAY ", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8), modifier = Modifier.padding(horizontal = 6.dp))
                    HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE2E8F0))
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Tên khoản chi *") },
                    placeholder = { Text("Ăn lẩu, Cafe, Vé tham quan...") },
                    shape = ExpressiveItemShape,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Số tiền * (nhập 300k, 1.5tr hoặc 500000)") },
                    placeholder = { Text("Ví dụ: 350k, 1.2tr...") },
                    shape = ExpressiveItemShape,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Who paid?
                Text("Ai đã thanh toán?", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(members, key = { it.id }) { member ->
                        val isSelected = selectedPayerId == member.id
                        Surface(
                            modifier = Modifier
                                .clip(ExpressivePillShape)
                                .noRippleClickable { selectedPayerId = member.id },
                            shape = ExpressivePillShape,
                            color = if (isSelected) Color(0xFF0284C7) else Color(0xFFF1F5F9),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) Color(0xFF0369A1) else Color(0xFFCBD5E1)
                            )
                        ) {
                            Text(
                                text = if (member.isCurrentUser) "👑 Tôi trả" else "${member.name} trả",
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else TextPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // Who shares?
                val isAllSelected = selectedSharerIds.size == members.size
                Text("Những ai tham gia chia khoản này?", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item {
                        Surface(
                            modifier = Modifier
                                .clip(ExpressivePillShape)
                                .noRippleClickable {
                                    selectedSharerIds = if (isAllSelected) setOf(members.first().id) else members.map { it.id }.toSet()
                                },
                            shape = ExpressivePillShape,
                            color = if (isAllSelected) PrimaryGreen else Color(0xFFF1F5F9),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isAllSelected) PrimaryGreenDark else Color(0xFFCBD5E1)
                            )
                        ) {
                            Text(
                                text = "Tất cả (${members.size})",
                                fontSize = 11.sp,
                                fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isAllSelected) Color.White else TextPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

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
                            Text(
                                text = if (member.isCurrentUser) "Tôi" else member.name,
                                fontSize = 11.sp,
                                fontWeight = if (isSharer) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSharer) PrimaryGreenDark else Color(0xFF64748B),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            val amount = CurrencyUtils.parseMoneyInput(amountStr)
            val isValid = title.isNotBlank() && amount != null && amount > 0 && selectedPayerId.isNotBlank() && selectedSharerIds.isNotEmpty()

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Thêm & Tiếp tục (cho phép nhập liên tiếp nhiều món)
                OutlinedButton(
                    onClick = {
                        if (isValid && amount != null) {
                            val payerName = members.find { it.id == selectedPayerId }?.name ?: "Tôi"
                            onAdd(title.trim(), amount, selectedPayerId, payerName, selectedSharerIds.toList(), note, true)
                            title = ""
                            amountStr = ""
                        }
                    },
                    enabled = isValid,
                    shape = ExpressivePillShape,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF0284C7)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF0284C7))
                ) {
                    Text("Thêm tiếp +", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                // Lưu & Đóng
                Button(
                    onClick = {
                        if (isValid && amount != null) {
                            val payerName = members.find { it.id == selectedPayerId }?.name ?: "Tôi"
                            onAdd(title.trim(), amount, selectedPayerId, payerName, selectedSharerIds.toList(), note, false)
                        }
                    },
                    enabled = isValid,
                    shape = ExpressivePillShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                ) {
                    Text("Lưu xong", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Đóng")
            }
        }
    )
}
