package com.expensetracker.app.utils

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.expensetracker.app.data.local.entity.AccountEntity
import com.expensetracker.app.data.local.entity.CategoryEntity
import com.expensetracker.app.data.local.entity.TransactionEntity
import com.expensetracker.app.data.local.entity.TransactionType
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object CsvExporter {

    fun exportTransactionsToCsv(
        context: Context,
        transactions: List<TransactionEntity>,
        categories: Map<String, CategoryEntity>,
        accounts: Map<Long, AccountEntity>
    ): File? {
        try {
            val exportDir = File(context.cacheDir, "exports")
            if (!exportDir.exists()) {
                exportDir.mkdirs()
            }

            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val file = File(exportDir, "BaoCaoChiTieu_$timeStamp.csv")

            FileOutputStream(file).use { outputStream ->
                // Write UTF-8 BOM for Microsoft Excel compatibility
                outputStream.write(0xEF)
                outputStream.write(0xBB)
                outputStream.write(0xBF)

                val writer = outputStream.bufferedWriter(Charsets.UTF_8)
                // CSV Header
                writer.write("Mã GD,Ngày Giờ,Loại,Danh Mục,Tài Khoản / Ví,Số Tiền (VND),Ghi Chú\n")

                for (tx in transactions) {
                    val dateStr = DateUtils.formatDate(tx.timestamp) + " " + DateUtils.formatTime(tx.timestamp)
                    val typeStr = when (tx.type) {
                        TransactionType.EXPENSE -> "Chi tiêu"
                        TransactionType.INCOME -> "Thu nhập"
                        TransactionType.TRANSFER -> "Chuyển khoản"
                    }
                    val categoryName = categories[tx.categoryId]?.name ?: "Khác"
                    val accountName = accounts[tx.accountId]?.name ?: "Ví mặc định"
                    val amountStr = String.format(Locale.US, "%.0f", tx.amount)
                    val noteStr = tx.note.replace("\"", "\"\"")

                    writer.write("\"${tx.id}\",\"$dateStr\",\"$typeStr\",\"$categoryName\",\"$accountName\",$amountStr,\"$noteStr\"\n")
                }
                writer.flush()
            }
            return file
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun shareCsvFile(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Báo cáo thu chi - Quản Lý Chi Tiêu")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Chia sẻ báo cáo CSV qua:"))
    }
}
