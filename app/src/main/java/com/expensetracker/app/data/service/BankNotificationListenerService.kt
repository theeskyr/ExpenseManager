package com.expensetracker.app.data.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import com.expensetracker.app.MainActivity
import com.expensetracker.app.R
import com.expensetracker.app.data.local.entity.TransactionType
import com.expensetracker.app.utils.BankNotificationParser
import com.expensetracker.app.utils.CurrencyUtils

class BankNotificationListenerService : NotificationListenerService() {

    companion object {
        const val CHANNEL_ID = "bank_expense_reminder_channel"
        const val CHANNEL_NAME = "Nhắc nhở ghi chép giao dịch ngân hàng"
        const val EXTRA_AMOUNT = "extra_amount"
        const val EXTRA_TYPE = "extra_type"
        const val EXTRA_BANK_NAME = "extra_bank_name"
        const val EXTRA_NOTE = "extra_note"
        const val EXTRA_NAVIGATE_TO_ADD = "extra_navigate_to_add"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val packageName = sbn.packageName ?: return

        // 1. TỐI ƯU PIN CỰC ĐỘ: Lọc gói tin ngay lập tức trong O(1).
        // Nếu không phải app ngân hàng nằm trong danh sách, thoát ngay lập tức (dưới 1ms)
        // Không cấp phát bộ nhớ, không đánh thức CPU.
        if (!BankNotificationParser.isBankApp(packageName)) {
            return
        }

        val extras = sbn.notification?.extras ?: return
        val title = extras.getCharSequence("android.title")?.toString() ?: ""
        val text = extras.getCharSequence("android.text")?.toString() ?: ""

        // 2. Phân tích nội dung giao dịch bằng Regex
        val parsed = BankNotificationParser.parse(packageName, title, text) ?: return

        // 3. Hiển thị thông báo nhắc người dùng ghi chép chi tiêu ngay lập tức
        showExpensePromptNotification(parsed.bankName, parsed.amount, parsed.type, parsed.suggestedNote)
    }

    private fun showExpensePromptNotification(
        bankName: String,
        amount: Double,
        type: TransactionType,
        note: String
    ) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_AMOUNT, amount)
            putExtra(EXTRA_TYPE, type.name)
            putExtra(EXTRA_BANK_NAME, bankName)
            putExtra(EXTRA_NOTE, note)
            putExtra(EXTRA_NAVIGATE_TO_ADD, true)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val formattedAmount = CurrencyUtils.formatVnd(amount)
        val typeTitle = if (type == TransactionType.EXPENSE) "Khoản chi" else "Khoản thu"

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Phát hiện $typeTitle mới: $formattedAmount")
            .setContentText("Từ $bankName. Chạm để chọn danh mục & lưu chi tiêu nhanh!")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Từ $bankName: $formattedAmount.\nNội dung: $note\nChạm để phân loại danh mục (Ăn uống, Mua sắm...)"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationId = (System.currentTimeMillis() % 10000).toInt()
        notificationManager.notify(notificationId, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Tự động hiển thị khi có biến động số dư từ tài khoản ngân hàng"
                enableVibration(true)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
