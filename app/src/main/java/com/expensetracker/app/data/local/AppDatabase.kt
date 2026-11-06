package com.expensetracker.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.expensetracker.app.data.local.dao.AccountDao
import com.expensetracker.app.data.local.dao.BudgetDao
import com.expensetracker.app.data.local.dao.CategoryDao
import com.expensetracker.app.data.local.dao.InvestmentDao
import com.expensetracker.app.data.local.dao.TransactionDao
import com.expensetracker.app.data.local.dao.TripDao
import com.expensetracker.app.data.local.entity.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        TransactionEntity::class,
        CategoryEntity::class,
        AccountEntity::class,
        BudgetEntity::class,
        InvestmentEntity::class,
        TripEntity::class,
        TripExpenseEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun accountDao(): AccountDao
    abstract fun budgetDao(): BudgetDao
    abstract fun investmentDao(): InvestmentDao
    abstract fun tripDao(): TripDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "expense_tracker_database"
                )
                .fallbackToDestructiveMigration()
                .addCallback(DatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(database)
                    }
                }
            }

            suspend fun populateInitialData(database: AppDatabase) {
                val categoryDao = database.categoryDao()
                val accountDao = database.accountDao()

                // Default Accounts
                val defaultAccounts = listOf(
                    AccountEntity(id = 1, name = "Tiền mặt", type = AccountType.CASH, initialBalance = 0.0, currentBalance = 0.0, colorHex = "#4CAF50", iconName = "Payments"),
                    AccountEntity(id = 2, name = "Tài khoản ngân hàng", type = AccountType.BANK, initialBalance = 0.0, currentBalance = 0.0, colorHex = "#2196F3", iconName = "AccountBalance"),
                    AccountEntity(id = 3, name = "Ví điện tử (MoMo / ZaloPay)", type = AccountType.E_WALLET, initialBalance = 0.0, currentBalance = 0.0, colorHex = "#E91E63", iconName = "AccountBalanceWallet")
                )
                accountDao.insertAccounts(defaultAccounts)

                // Default Expense Categories
                val defaultExpenseCategories = listOf(
                    CategoryEntity(id = "food", name = "Ăn uống", type = TransactionType.EXPENSE, iconName = "Fastfood", colorHex = "#FF5722"),
                    CategoryEntity(id = "shopping", name = "Mua sắm", type = TransactionType.EXPENSE, iconName = "ShoppingCart", colorHex = "#E91E63"),
                    CategoryEntity(id = "transport", name = "Đi lại / Xăng xe", type = TransactionType.EXPENSE, iconName = "DirectionsCar", colorHex = "#3F51B5"),
                    CategoryEntity(id = "bills", name = "Hóa đơn / Tiện ích", type = TransactionType.EXPENSE, iconName = "Receipt", colorHex = "#FF9800"),
                    CategoryEntity(id = "housing", name = "Nhà cửa / Thuê trọ", type = TransactionType.EXPENSE, iconName = "Home", colorHex = "#795548"),
                    CategoryEntity(id = "entertainment", name = "Giải trí / Giao lưu", type = TransactionType.EXPENSE, iconName = "SportsEsports", colorHex = "#9C27B0"),
                    CategoryEntity(id = "health", name = "Sức khỏe / Y tế", type = TransactionType.EXPENSE, iconName = "MedicalServices", colorHex = "#009688"),
                    CategoryEntity(id = "education", name = "Học tập / Phát triển", type = TransactionType.EXPENSE, iconName = "School", colorHex = "#00BCD4"),
                    CategoryEntity(id = "other_expense", name = "Chi phí khác", type = TransactionType.EXPENSE, iconName = "MoreHoriz", colorHex = "#607D8B")
                )

                // Default Income Categories
                val defaultIncomeCategories = listOf(
                    CategoryEntity(id = "salary", name = "Tiền lương", type = TransactionType.INCOME, iconName = "AttachMoney", colorHex = "#4CAF50"),
                    CategoryEntity(id = "bonus", name = "Thưởng & Hoa hồng", type = TransactionType.INCOME, iconName = "CardGiftcard", colorHex = "#8BC34A"),
                    CategoryEntity(id = "investment", name = "Đầu tư / Lãi tiết kiệm", type = TransactionType.INCOME, iconName = "TrendingUp", colorHex = "#00ACC1"),
                    CategoryEntity(id = "other_income", name = "Thu nhập phụ / Khác", type = TransactionType.INCOME, iconName = "Savings", colorHex = "#FFB300")
                )

                categoryDao.insertCategories(defaultExpenseCategories + defaultIncomeCategories)
            }
        }
    }
}
