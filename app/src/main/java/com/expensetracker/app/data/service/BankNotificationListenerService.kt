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

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel(this)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val packageName = sbn.packageName ?: return
        val extras = sbn.notification?.extras ?: return

        val title = extras.getCharSequence("android.title")?.toString() ?: ""
        val text = extras.getCharSequence("android.text")?.toString() ?: ""
        val bigText = extras.getCharSequence("android.bigText")?.toString() ?: ""
        val subText = extras.getCharSequence("android.subText")?.toString() ?: ""
        val ticker = sbn.notification?.tickerText?.toString() ?: ""

        val combinedContent = listOf(text, bigText, subText, ticker)
            .filter { it.isNotBlank() }
            .joinToString(" ")

        // 1. Kiểm tra có phải app ngân hàng hoặc SMS ngân hàng không
        if (!BankNotificationParser.isBankAppOrSms(packageName, title, combinedContent)) {
            return
        }

        // 2. Phân tích nội dung giao dịch bằng Regex
        val parsed = BankNotificationParser.parse(packageName, title, combinedContent) ?: return

        // 3. Hiển thị thông báo nhắc người dùng ghi chép chi tiêu ngay lập tức
        showExpensePromptNotification(this, parsed.bankName, parsed.amount, parsed.type, parsed.suggestedNote)
    }

    companion object {
        const val CHANNEL_ID = "bank_expense_reminder_channel"
        const val CHANNEL_NAME = "Nhắc nhở ghi chép giao dịch ngân hàng"
        const val EXTRA_AMOUNT = "extra_amount"
        const val EXTRA_TYPE = "extra_type"
        const val EXTRA_BANK_NAME = "extra_bank_name"
        const val EXTRA_NOTE = "extra_note"
        const val EXTRA_NAVIGATE_TO_ADD = "extra_navigate_to_add"

        fun showExpensePromptNotification(
            context: Context,
            bankName: String,
            amount: Double,
            type: TransactionType,
            note: String
        ) {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_AMOUNT, amount)
                putExtra(EXTRA_TYPE, type.name)
                putExtra(EXTRA_BANK_NAME, bankName)
                putExtra(EXTRA_NOTE, note)
                putExtra(EXTRA_NAVIGATE_TO_ADD, true)
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                System.currentTimeMillis().toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val formattedAmount = CurrencyUtils.formatVnd(amount)
            val isIncome = type == TransactionType.INCOME
            val typeTitle = if (isIncome) "Khoản thu (+)" else "Khoản chi (-)"
            val signPrefix = if (isIncome) "+" else "-"

            // Đảm bảo Channel được tạo
            createNotificationChannel(context)

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("🔔 Phát hiện $typeTitle: $signPrefix$formattedAmount")
                .setContentText("Từ $bankName. Chạm để chọn danh mục & lưu nhanh!")
                .setStyle(
                    NotificationCompat.BigTextStyle().bigText(
                        "Từ $bankName: $signPrefix$formattedAmount ($typeTitle).\nNội dung: $note\nChạm vào đây để phân loại danh mục và lưu trong 1 giây."
                    )
                )
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setCategory(NotificationCompat.CATEGORY_EVENT)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setFullScreenIntent(pendingIntent, false)
                .build()

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notificationId = (System.currentTimeMillis() % 10000).toInt() + 1
            notificationManager.notify(notificationId, notification)
        }

        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Tự động hiển thị khi có biến động số dư từ tài khoản ngân hàng"
                    enableVibration(true)
                    enableLights(true)
                }
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(channel)
            }
        }
    }
}
