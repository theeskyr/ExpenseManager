package com.expensetracker.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.expensetracker.app.ui.components.AppNavigationRail
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.expensetracker.app.ui.screens.trips.TripsScreen
import com.expensetracker.app.ui.screens.trips.TripDetailScreen
import com.expensetracker.app.data.local.entity.TransactionType
import com.expensetracker.app.data.service.BankNotificationListenerService
import com.expensetracker.app.ui.screens.accounts.AccountsScreen
import com.expensetracker.app.ui.screens.add.AddTransactionScreen
import com.expensetracker.app.ui.screens.ai.AiAssistantScreen
import com.expensetracker.app.ui.screens.budget.BudgetScreen
import com.expensetracker.app.ui.screens.home.HomeScreen
import com.expensetracker.app.ui.screens.investments.InvestmentScreen
import com.expensetracker.app.ui.screens.settings.SettingsScreen
import com.expensetracker.app.ui.screens.splitbill.SplitBillScreen
import com.expensetracker.app.ui.screens.stats.StatisticsScreen
import com.expensetracker.app.ui.theme.*
import com.expensetracker.app.ui.viewmodel.ExpenseViewModel
import com.expensetracker.app.ui.viewmodel.ViewModelFactory

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "Trang chủ", Icons.Default.Home)
    object Stats : Screen("stats", "Thống kê", Icons.Default.PieChart)
    object AiAssistant : Screen("ai_assistant", "Trợ lý AI", Icons.Default.SmartToy)
    object Budget : Screen("budget", "Ngân sách", Icons.Default.Savings)
    object Investments : Screen("investments", "Đầu tư", Icons.AutoMirrored.Filled.TrendingUp)
    object Accounts : Screen("accounts", "Ví tiền", Icons.Default.AccountBalanceWallet)
    object SplitBill : Screen("split_bill", "Chia tiền", Icons.Default.Groups)
    object Trips : Screen("trips", "Chuyến đi", Icons.Default.Luggage)
    object TripDetail : Screen("trip_detail/{tripId}", "Chi tiết chuyến đi", Icons.Default.Luggage) {
        fun createRoute(tripId: String) = "trip_detail/$tripId"
    }
    object Settings : Screen("settings", "Cài đặt", Icons.Default.Settings)
    object AddTransaction : Screen("add_transaction", "Thêm", Icons.Default.Add)
}

class MainActivity : ComponentActivity() {

    private val viewModel: ExpenseViewModel by viewModels {
        val app = application as ExpenseTrackerApp
        ViewModelFactory(app.repository, app.geminiManager)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Khởi tạo kênh thông báo ngân hàng với độ ưu tiên cao nhất
        BankNotificationListenerService.createNotificationChannel(this)

        val extraNavigateToAdd = intent.getBooleanExtra(BankNotificationListenerService.EXTRA_NAVIGATE_TO_ADD, false)
        val extraAmount = intent.getDoubleExtra(BankNotificationListenerService.EXTRA_AMOUNT, 0.0)
        val extraTypeName = intent.getStringExtra(BankNotificationListenerService.EXTRA_TYPE)
        val extraType = extraTypeName?.let { runCatching { TransactionType.valueOf(it) }.getOrNull() }
        val extraNote = intent.getStringExtra(BankNotificationListenerService.EXTRA_NOTE) ?: ""

        setContent {
            ExpenseTrackerTheme {
                // Xin quyền thông báo trên Android 13+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val permissionLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestPermission()
                    ) { /* Handled */ }

                    LaunchedEffect(Unit) {
                        if (ContextCompat.checkSelfPermission(
                                this@MainActivity,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }

                MainAppNavHost(
                    viewModel = viewModel,
                    initialNavigateToAdd = extraNavigateToAdd,
                    initialAmount = if (extraAmount > 0) extraAmount else null,
                    initialType = extraType,
                    initialNote = extraNote
                )
            }
        }
    }
}

@Composable
fun MainAppNavHost(
    viewModel: ExpenseViewModel,
    initialNavigateToAdd: Boolean = false,
    initialAmount: Double? = null,
    initialType: TransactionType? = null,
    initialNote: String = ""
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    LaunchedEffect(initialNavigateToAdd) {
        if (initialNavigateToAdd) {
            navController.navigate(Screen.AddTransaction.route)
        }
    }

    val windowSizeInfo = rememberWindowSizeInfo()
    val isWideScreen = windowSizeInfo.isWideScreen

    val bottomNavItems = remember {
        listOf(
            Screen.Home,
            Screen.Stats,
            Screen.AiAssistant,
            Screen.Budget,
            Screen.Settings
        )
    }

    val showNavigation = currentRoute != Screen.AddTransaction.route && 
                         currentRoute != Screen.Investments.route && 
                         currentRoute != Screen.Accounts.route &&
                         currentRoute != Screen.SplitBill.route &&
                         currentRoute != Screen.Trips.route &&
                         currentRoute?.startsWith("trip_detail") != true

    CompositionLocalProvider(LocalWindowSizeInfo provides windowSizeInfo) {
        Scaffold(
            bottomBar = {
                // Show floating bottom bar only on compact screens (e.g. phones in portrait)
                if (!isWideScreen && showNavigation) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = ExpressivePillShape,
                            shadowElevation = 2.dp,
                            color = Color.White,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(64.dp)
                                    .padding(horizontal = 6.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceAround,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                bottomNavItems.forEach { screen ->
                                    val isSelected = currentRoute == screen.route
                                    val animatedBgColor by animateColorAsState(
                                        targetValue = if (isSelected) PrimaryContainer else Color.Transparent,
                                        animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
                                        label = "nav_item_bg"
                                    )
                                    val animatedContentColor by animateColorAsState(
                                        targetValue = if (isSelected) PrimaryGreenDark else Color(0xFF64748B),
                                        animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
                                        label = "nav_item_content"
                                    )

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .padding(horizontal = 2.dp)
                                            .clip(ExpressivePillShape)
                                            .background(animatedBgColor)
                                            .clickable {
                                                if (currentRoute != screen.route) {
                                                    navController.navigate(screen.route) {
                                                        popUpTo(navController.graph.findStartDestination().id) {
                                                            saveState = true
                                                        }
                                                        launchSingleTop = true
                                                        restoreState = true
                                                    }
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                imageVector = screen.icon,
                                                contentDescription = screen.title,
                                                tint = animatedContentColor,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = screen.title,
                                                color = animatedContentColor,
                                                fontSize = 10.sp,
                                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Navigation Rail on the left for wide screens (tablets, landscape)
                if (isWideScreen && showNavigation) {
                    AppNavigationRail(
                        currentRoute = currentRoute,
                        items = bottomNavItems,
                        onNavigate = { screen ->
                            if (currentRoute != screen.route) {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        onQuickAdd = {
                            navController.navigate(Screen.AddTransaction.route)
                        }
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = Screen.Home.route,
                        modifier = Modifier.fillMaxSize(),
            enterTransition = {
                fadeIn(animationSpec = tween(150, easing = LinearEasing))
            },
            exitTransition = {
                fadeOut(animationSpec = tween(150, easing = LinearEasing))
            },
            popEnterTransition = {
                fadeIn(animationSpec = tween(150, easing = LinearEasing))
            },
            popExitTransition = {
                fadeOut(animationSpec = tween(150, easing = LinearEasing))
            }
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    viewModel = viewModel,
                    onNavigateToAdd = {
                        navController.navigate(Screen.AddTransaction.route)
                    },
                    onNavigateToAi = {
                        navController.navigate(Screen.AiAssistant.route)
                    },
                    onNavigateToInvestments = {
                        navController.navigate(Screen.Investments.route)
                    },
                    onNavigateToAccounts = {
                        navController.navigate(Screen.Accounts.route)
                    },
                    onNavigateToSplitBill = {
                        viewModel.clearActiveTripForSplit()
                        navController.navigate(Screen.SplitBill.route)
                    },
                    onNavigateToTrips = {
                        navController.navigate(Screen.Trips.route)
                    }
                )
            }
            composable(Screen.Stats.route) {
                StatisticsScreen(viewModel = viewModel)
            }
            composable(Screen.AiAssistant.route) {
                AiAssistantScreen(
                    viewModel = viewModel,
                    onNavigateBack = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            composable(Screen.Budget.route) {
                BudgetScreen(viewModel = viewModel)
            }
            composable(Screen.Investments.route) {
                InvestmentScreen(
                    viewModel = viewModel,
                    onNavigateToAi = {
                        navController.navigate(Screen.AiAssistant.route)
                    },
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
            composable(Screen.Accounts.route) {
                AccountsScreen(
                    viewModel = viewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    viewModel = viewModel,
                    onNavigateToAccounts = {
                        navController.navigate(Screen.Accounts.route)
                    }
                )
            }
            composable(Screen.AddTransaction.route) {
                AddTransactionScreen(
                    viewModel = viewModel,
                    initialAmount = initialAmount,
                    initialType = initialType,
                    initialNote = initialNote,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
            composable(Screen.SplitBill.route) {
                SplitBillScreen(
                    viewModel = viewModel,
                    onNavigateBack = {
                        viewModel.clearActiveTripForSplit()
                        if (!navController.popBackStack()) {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(navController.graph.findStartDestination().id)
                                launchSingleTop = true
                            }
                        }
                    },
                    onNavigateToHome = {
                        viewModel.clearActiveTripForSplit()
                        if (!navController.popBackStack(Screen.Home.route, inclusive = false)) {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(navController.graph.findStartDestination().id)
                                launchSingleTop = true
                            }
                        }
                    }
                )
            }
            composable(Screen.Trips.route) {
                TripsScreen(
                    viewModel = viewModel,
                    onNavigateBack = {
                        if (!navController.popBackStack()) {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(navController.graph.findStartDestination().id)
                                launchSingleTop = true
                            }
                        }
                    },
                    onNavigateToTripDetail = { tripId ->
                        navController.navigate(Screen.TripDetail.createRoute(tripId))
                    }
                )
            }
            composable(
                route = Screen.TripDetail.route,
                arguments = listOf(navArgument("tripId") { type = NavType.StringType })
            ) { backStackEntry ->
                val tripId = backStackEntry.arguments?.getString("tripId") ?: ""
                TripDetailScreen(
                    tripId = tripId,
                    viewModel = viewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onNavigateToSplitBill = {
                        navController.navigate(Screen.SplitBill.route)
                    }
                )
            }
        }
            }
        }
    }
}
}
