package com.expensetracker.app.ui.screens.trips

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.expensetracker.app.data.local.entity.TripEntity
import com.expensetracker.app.data.model.SplitMember
import com.expensetracker.app.ui.theme.*
import com.expensetracker.app.ui.viewmodel.ExpenseViewModel
import com.expensetracker.app.utils.CurrencyUtils
import com.expensetracker.app.utils.DateUtils
import com.expensetracker.app.utils.TripJsonUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripsScreen(
    viewModel: ExpenseViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToTripDetail: (String) -> Unit
) {
    val context = LocalContext.current
    val allTrips by viewModel.allTrips.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var tripToDelete by remember { mutableStateOf<TripEntity?>(null) }

    val ongoingTrips = remember(allTrips) { allTrips.filter { it.isOngoing } }
    val completedTrips = remember(allTrips) { allTrips.filter { !it.isOngoing } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(ExpressiveSquircleIconShape)
                                .background(Color(0xFF0284C7)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Luggage, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                "Tính toán chuyến đi",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                "Theo dõi chi tiêu theo từng ngày",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }
                },
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
                onClick = { showCreateDialog = true },
                shape = RoundedCornerShape(20.dp),
                containerColor = Color(0xFF0284C7),
                contentColor = Color.White,
                elevation = FloatingActionButtonDefaults.elevation(3.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Tạo chuyến đi", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    ) { paddingValues ->
        ResponsiveContentContainer(maxWidth = 760.dp) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    // Hero Intro Banner
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
                                        colors = listOf(Color(0xFF0284C7), Color(0xFF0D9488), Color(0xFF10B981))
                                    )
                                )
                                .padding(18.dp)
                        ) {
                            Column {
                                Surface(
                                    shape = ExpressivePillShape,
                                    color = Color.White.copy(alpha = 0.22f)
                                ) {
                                    Text(
                                        "✈️ QUẢN LÝ DU LỊCH & ĐI CHƠI NHÓM",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Nhập chi tiêu từng ngày • Chia tiền AI khi kết thúc",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Chuyến đi kéo dài nhiều ngày? Bạn nhập chi tiêu rải rác mỗi ngày. Khi kết thúc, 1 chạm để AI tính toán chia tiền tự động mà không cần gõ lại!",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            }
                        }
                    }
                }

                // Ongoing Trips Section
                if (ongoingTrips.isNotEmpty()) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryGreen)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Chuyến đi đang diễn ra (${ongoingTrips.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    items(ongoingTrips, key = { it.id }) { trip ->
                        TripCardItem(
                            trip = trip,
                            onClick = { onNavigateToTripDetail(trip.id) },
                            onDelete = { tripToDelete = trip }
                        )
                    }
                }

                // Completed Trips Section
                if (completedTrips.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.History, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Lịch sử chuyến đi đã hoàn thành (${completedTrips.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    items(completedTrips, key = { it.id }) { trip ->
                        TripCardItem(
                            trip = trip,
                            onClick = { onNavigateToTripDetail(trip.id) },
                            onDelete = { tripToDelete = trip }
                        )
                    }
                }

                // Empty State
                if (allTrips.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            shape = ExpressiveCardShape,
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFE0F2FE)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Luggage,
                                        contentDescription = null,
                                        tint = Color(0xFF0284C7),
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    "Chưa có chuyến đi nào",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    "Bắt đầu lên kế hoạch cho chuyến du lịch sắp tới với bạn bè! Bấm nút bên dưới để tạo chuyến đi đầu tiên.",
                                    fontSize = 12.sp,
                                    color = TextSecondary,
                                    lineHeight = 17.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { showCreateDialog = true },
                                    shape = ExpressivePillShape,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Tạo chuyến đi mới", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }

    // Dialog Create Trip
    if (showCreateDialog) {
        CreateTripDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { title, destination, members ->
                viewModel.createTrip(title, destination, members) { newTripId ->
                    showCreateDialog = false
                    Toast.makeText(context, "Đã tạo chuyến đi thành công!", Toast.LENGTH_SHORT).show()
                    onNavigateToTripDetail(newTripId)
                }
            }
        )
    }

    // Delete Confirmation Dialog
    tripToDelete?.let { trip ->
        AlertDialog(
            onDismissRequest = { tripToDelete = null },
            shape = ExpressiveDialogShape,
            title = { Text("Xác nhận xóa chuyến đi?", fontWeight = FontWeight.Bold) },
            text = { Text("Bạn có chắc chắn muốn xóa chuyến đi \"${trip.title}\" cùng toàn bộ các khoản chi tiêu đã ghi lại?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteTrip(trip)
                        tripToDelete = null
                        Toast.makeText(context, "Đã xóa chuyến đi", Toast.LENGTH_SHORT).show()
                    },
                    shape = ExpressivePillShape,
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed)
                ) {
                    Text("Xóa", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { tripToDelete = null }) {
                    Text("Hủy")
                }
            }
        )
    }
}

@Composable
private fun TripCardItem(
    trip: TripEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val members = remember(trip.membersJson) { TripJsonUtils.jsonToMembers(trip.membersJson) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ExpressiveCardShape)
            .clickable { onClick() },
        shape = ExpressiveCardShape,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (trip.isOngoing) Color(0xFFBAE6FD) else Color(0xFFE2E8F0)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(ExpressiveSquircleIconShape)
                            .background(if (trip.isOngoing) Color(0xFFE0F2FE) else Color(0xFFF1F5F9)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (trip.isOngoing) Icons.Default.Luggage else Icons.Default.DoneAll,
                            contentDescription = null,
                            tint = if (trip.isOngoing) Color(0xFF0284C7) else Color(0xFF64748B),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = trip.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = TextPrimary
                        )
                        if (trip.destination.isNotBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Place, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(trip.destination, fontSize = 11.sp, color = TextSecondary)
                            }
                        }
                    }
                }

                Surface(
                    shape = ExpressivePillShape,
                    color = if (trip.isOngoing) Color(0xFFDCFCE7) else Color(0xFFF1F5F9)
                ) {
                    Text(
                        text = if (trip.isOngoing) "ĐANG DIỄN RA" else "ĐÃ HOÀN TẤT",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (trip.isOngoing) Color(0xFF166534) else Color(0xFF64748B),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFFF1F5F9))
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Tổng chi phí cả nhóm", fontSize = 11.sp, color = TextSecondary)
                    Text(
                        text = CurrencyUtils.formatVnd(trip.totalExpense),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (trip.totalExpense > 0) Color(0xFF0284C7) else TextPrimary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${members.size} thành viên",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Xóa", tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                    }
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CreateTripDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String, List<SplitMember>) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var destination by remember { mutableStateOf("") }
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
                    Icon(Icons.Default.Luggage, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("Tạo chuyến đi mới", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Tên chuyến đi *") },
                    placeholder = { Text("Ví dụ: Du lịch Đà Lạt 3N2Đ") },
                    shape = ExpressiveInputShape,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = destination,
                    onValueChange = { destination = it },
                    label = { Text("Điểm đến (tùy chọn)") },
                    placeholder = { Text("Ví dụ: Đà Lạt, Lâm Đồng") },
                    shape = ExpressiveInputShape,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(2.dp))
                Text("Thành viên tham gia (${members.size} người):", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextPrimary)

                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(members, key = { it.id }) { member ->
                        Surface(
                            shape = ExpressivePillShape,
                            color = if (member.isCurrentUser) Color(0xFFE0F2FE) else Color(0xFFF1F5F9),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (member.isCurrentUser) Color(0xFF38BDF8) else Color(0xFFCBD5E1)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = member.name,
                                    fontSize = 11.sp,
                                    fontWeight = if (member.isCurrentUser) FontWeight.Bold else FontWeight.Normal,
                                    color = if (member.isCurrentUser) Color(0xFF0369A1) else TextPrimary
                                )
                                if (!member.isCurrentUser && members.size > 1) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Xóa",
                                        tint = Color(0xFF94A3B8),
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clickable {
                                                members = members.filter { it.id != member.id }
                                            }
                                    )
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newMemberName,
                        onValueChange = { newMemberName = it },
                        placeholder = { Text("Thêm tên bạn bè...") },
                        modifier = Modifier.weight(1f),
                        shape = ExpressiveInputShape,
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = {
                            val trimmed = newMemberName.trim()
                            if (trimmed.isNotBlank() && members.none { it.name.equals(trimmed, ignoreCase = true) }) {
                                members = members + SplitMember(name = trimmed)
                                newMemberName = ""
                            }
                        },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0284C7))
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Thêm", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && members.isNotEmpty()) {
                        onCreate(title, destination, members)
                    }
                },
                enabled = title.isNotBlank() && members.isNotEmpty(),
                shape = ExpressivePillShape,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
            ) {
                Text("Bắt đầu chuyến đi", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        }
    )
}
