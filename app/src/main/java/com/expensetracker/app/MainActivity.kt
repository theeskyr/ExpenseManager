package com.expensetracker.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.expensetracker.app.data.local.entity.TransactionType
import com.expensetracker.app.data.service.BankNotificationListenerService
import com.expensetracker.app.ui.screens.accounts.AccountsScreen
import com.expensetracker.app.ui.screens.add.AddTransactionScreen
import com.expensetracker.app.ui.screens.ai.AiAssistantScreen
import com.expensetracker.app.ui.screens.budget.BudgetScreen
import com.expensetracker.app.ui.screens.home.HomeScreen
import com.expensetracker.app.ui.screens.settings.SettingsScreen
import com.expensetracker.app.ui.screens.stats.StatisticsScreen
import com.expensetracker.app.ui.theme.ExpenseTrackerTheme
import com.expensetracker.app.ui.theme.PrimaryGreen
import com.expensetracker.app.ui.viewmodel.ExpenseViewModel
import com.expensetracker.app.ui.viewmodel.ViewModelFactory

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "Trang chủ", Icons.Default.Home)
    object Stats : Screen("stats", "Thống kê", Icons.Default.PieChart)
    object AiAssistant : Screen("ai_assistant", "Trợ lý AI", Icons.Default.SmartToy)
    object Budget : Screen("budget", "Ngân sách", Icons.Default.Savings)
    object Accounts : Screen("accounts", "Ví tiền", Icons.Default.AccountBalanceWallet)
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

        val extraNavigateToAdd = intent.getBooleanExtra(BankNotificationListenerService.EXTRA_NAVIGATE_TO_ADD, false)
        val extraAmount = intent.getDoubleExtra(BankNotificationListenerService.EXTRA_AMOUNT, 0.0)
        val extraTypeName = intent.getStringExtra(BankNotificationListenerService.EXTRA_TYPE)
        val extraType = extraTypeName?.let { runCatching { TransactionType.valueOf(it) }.getOrNull() }
        val extraNote = intent.getStringExtra(BankNotificationListenerService.EXTRA_NOTE) ?: ""

        setContent {
            ExpenseTrackerTheme {
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

    val bottomNavItems = listOf(
        Screen.Home,
        Screen.Stats,
        Screen.AiAssistant,
        Screen.Budget,
        Screen.Settings
    )

    Scaffold(
        bottomBar = {
            // Hide bottom bar when in AddTransaction screen
            if (currentRoute != Screen.AddTransaction.route) {
                NavigationBar(
                    containerColor = Color.White,
                    tonalElevation = NavigationBarDefaults.Elevation
                ) {
                    bottomNavItems.forEach { screen ->
                        val isSelected = currentRoute == screen.route
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.title) },
                            label = { Text(screen.title) },
                            selected = isSelected,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = PrimaryGreen,
                                selectedTextColor = PrimaryGreen,
                                indicatorColor = PrimaryGreen.copy(alpha = 0.15f)
                            ),
                            onClick = {
                                if (currentRoute != screen.route) {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    viewModel = viewModel,
                    onNavigateToAdd = {
                        navController.navigate(Screen.AddTransaction.route)
                    },
                    onNavigateToAi = {
                        navController.navigate(Screen.AiAssistant.route)
                    }
                )
            }
            composable(Screen.Stats.route) {
                StatisticsScreen(viewModel = viewModel)
            }
            composable(Screen.AiAssistant.route) {
                AiAssistantScreen(viewModel = viewModel)
            }
            composable(Screen.Budget.route) {
                BudgetScreen(viewModel = viewModel)
            }
            composable(Screen.Accounts.route) {
                AccountsScreen(viewModel = viewModel)
            }
            composable(Screen.Settings.route) {
                SettingsScreen(viewModel = viewModel)
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
        }
    }
}
