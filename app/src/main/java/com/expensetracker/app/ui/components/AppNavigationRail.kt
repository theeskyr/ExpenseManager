package com.expensetracker.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.expensetracker.app.Screen
import com.expensetracker.app.ui.theme.*

@Composable
fun AppNavigationRail(
    currentRoute: String?,
    items: List<Screen>,
    onNavigate: (Screen) -> Unit,
    onQuickAdd: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxHeight()
            .width(96.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Section: App Logo & Quick Add FAB
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // App Logo Squircle
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(PrimaryGreen, ExpressiveSquircleIconShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.AccountBalanceWallet,
                        contentDescription = "Expense Manager",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Quick Add FAB on Rail
                FloatingActionButton(
                    onClick = onQuickAdd,
                    shape = RoundedCornerShape(16.dp),
                    containerColor = PrimaryGreen,
                    contentColor = Color.White,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 2.dp),
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Thêm giao dịch",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Middle Section: Navigation Items
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items.forEach { screen ->
                    val isSelected = currentRoute == screen.route
                    val animatedBgColor by animateColorAsState(
                        targetValue = if (isSelected) PrimaryContainer else Color.Transparent,
                        animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
                        label = "rail_item_bg"
                    )
                    val animatedContentColor by animateColorAsState(
                        targetValue = if (isSelected) PrimaryGreenDark else Color(0xFF64748B),
                        animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
                        label = "rail_item_content"
                    )

                    Column(
                        modifier = Modifier
                            .width(80.dp)
                            .clip(ExpressiveItemShape)
                            .background(animatedBgColor)
                            .clickable { onNavigate(screen) }
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = screen.icon,
                            contentDescription = screen.title,
                            tint = animatedContentColor,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = screen.title,
                            color = animatedContentColor,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1
                        )
                    }
                }
            }

            // Bottom placeholder for balanced spacing
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
