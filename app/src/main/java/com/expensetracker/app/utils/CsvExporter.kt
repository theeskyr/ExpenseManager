package com.expensetracker.app.utils

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import com.expensetracker.app.data.local.entity.AccountEntity
import com.expensetracker.app.data.local.entity.CategoryEntity
import com.expensetracker.app.data.local.entity.TransactionEntity
import com.expensetracker.app.data.local.entity.TransactionType
import com.expensetracker.app.data.model.SplitBillResult
import com.expensetracker.app.data.model.SplitExpenseItem
import com.expensetracker.app.data.model.SplitMember
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

data class SavedCsvResult(
    val fileName: String,
    val cacheFile: File,
    val providerUri: Uri
)

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

    fun generateSplitBillCsvBytes(
        result: SplitBillResult,
        items: List<SplitExpenseItem>,
        members: List<SplitMember>
    ): ByteArray {
        val byteStream = ByteArrayOutputStream()
        // Write UTF-8 BOM for Microsoft Excel / Google Sheets compatibility
        byteStream.write(0xEF)
        byteStream.write(0xBB)
        byteStream.write(0xBF)

        val writer = byteStream.bufferedWriter(Charsets.UTF_8)
        val displayDate = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())

        // 1. Header & Summary Info
        writer.write("BÁO CÁO PHÂN CHIA TIỀN NHÓM - QUẢN LÝ CHI TIÊU\n")
        writer.write("Sự kiện:,\"${result.eventTitle.replace("\"", "\"\"")}\"\n")
        writer.write("Thời gian tạo:,\"$displayDate\"\n")
        writer.write("Tổng chi phí sự kiện:,${String.format(Locale.US, "%.0f", result.totalAmount)},VND\n")
        writer.write("Số lượng thành viên:,${members.size},người\n")
        writer.write("Trung bình mỗi người:,${String.format(Locale.US, "%.0f", result.perPersonAvg)},VND\n")
        writer.write("\n")

        // 2. Section 1: Detailed Items (Các khoản chi / món ăn)
        writer.write("1. CHI TIẾT TỪNG KHOẢN CHI TIÊU / MÓN ĂN\n")
        writer.write("STT,Khoản chi / Món ăn,Số tiền (VND),Người đã trả trước,Người tham gia chia,Tiền mỗi người chịu (VND)\n")

        val memberMap = members.associateBy { it.id }
        items.forEachIndexed { index, item ->
            val stt = index + 1
            val title = item.title.replace("\"", "\"\"")
            val amountStr = String.format(Locale.US, "%.0f", item.amount)
            val payerName = memberMap[item.paidByMemberId]?.name ?: "Tôi"
            val sharers = if (item.splitAmongMemberIds.isEmpty() || item.splitAmongMemberIds.size == members.size) {
                "Tất cả (${members.size} người)"
            } else {
                item.splitAmongMemberIds.mapNotNull { memberMap[it]?.name }.joinToString(", ")
            }
            val sharerCount = if (item.splitAmongMemberIds.isEmpty()) members.size else item.splitAmongMemberIds.size
            val sharePerPerson = if (sharerCount > 0) {
                String.format(Locale.US, "%.0f", item.amount / sharerCount)
            } else amountStr

            writer.write("$stt,\"$title\",$amountStr,\"$payerName\",\"$sharers\",$sharePerPerson\n")
        }
        writer.write("\n")

        // 3. Section 2: Summary by Member (Tổng kết từng thành viên)
        writer.write("2. TỔNG KẾT TỪNG THÀNH VIÊN\n")
        writer.write("STT,Thành viên,Đã trả trước (VND),Chi phí thực tế phải chịu (VND),Chênh lệch (VND),Trạng thái\n")

        result.memberSummaries.forEachIndexed { index, summary ->
            val stt = index + 1
            val name = if (summary.member.isCurrentUser) "${summary.member.name} (Tôi)" else summary.member.name
            val paidStr = String.format(Locale.US, "%.0f", summary.totalPaid)
            val fairStr = String.format(Locale.US, "%.0f", summary.fairShare)
            val diff = summary.netBalance
            val diffStr = if (diff >= 0) "+${String.format(Locale.US, "%.0f", diff)}" else String.format(Locale.US, "%.0f", diff)
            val status = when {
                diff > 500 -> "Cần nhận lại"
                diff < -500 -> "Cần trả thêm"
                else -> "Đã thanh toán đủ"
            }

            writer.write("$stt,\"$name\",$paidStr,$fairStr,$diffStr,\"$status\"\n")
        }
        writer.write("\n")

        // 4. Section 3: Settlement Matrix (Hướng dẫn chuyển tiền tối ưu)
        writer.write("3. HƯỚNG DẪN CHUYỂN TIỀN (TỐI ƯU SỐ GIAO DỊCH)\n")
        writer.write("STT,Người chuyển,Người nhận,Số tiền cần chuyển (VND)\n")

        if (result.settlements.isEmpty()) {
            writer.write("1,\"Tất cả thành viên đã thanh toán sòng phẳng, không ai nợ ai!\",,\n")
        } else {
            result.settlements.forEachIndexed { index, s ->
                val stt = index + 1
                val from = s.fromMemberName.replace("\"", "\"\"")
                val to = s.toMemberName.replace("\"", "\"\"")
                val amount = String.format(Locale.US, "%.0f", s.amount)
                writer.write("$stt,\"$from\",\"$to\",$amount\n")
            }
        }
        writer.write("\n")

        // 5. Section 4: AI Summary / Advice (nếu có)
        result.aiSummary?.let { aiSumm ->
            writer.write("4. LỜI NHẮN TỪ TRỢ LÝ AI\n")
            writer.write("\"${aiSumm.replace("\"", "\"\"")}\"\n")
            result.aiAdvice?.let { advice ->
                writer.write("\"Mẹo tài chính: ${advice.replace("\"", "\"\"")}\"\n")
            }
        }

        writer.flush()
        return byteStream.toByteArray()
    }

    /**
     * Lưu file CSV vào bộ nhớ máy (thư mục Tải xuống / Downloads)
     * đồng thời tạo cache file để mở duyệt hoặc chia sẻ thủ công bất cứ lúc nào.
     */
    fun saveSplitBillToDownloads(
        context: Context,
        result: SplitBillResult,
        items: List<SplitExpenseItem>,
        members: List<SplitMember>
    ): SavedCsvResult? {
        try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val safeTitle = result.eventTitle
                .replace(Regex("[^a-zA-Z0-9_]"), "_")
                .trim('_')
                .ifBlank { "SuKien" }
                .take(20)

            val fileName = "ChiaTien_${safeTitle}_$timeStamp.csv"
            val csvBytes = generateSplitBillCsvBytes(result, items, members)

            // Lưu bản cache cho FileProvider để mở duyệt trực tiếp hoặc chia sẻ
            val exportDir = File(context.cacheDir, "exports")
            if (!exportDir.exists()) exportDir.mkdirs()
            val cacheFile = File(exportDir, fileName)
            cacheFile.writeBytes(csvBytes)

            val providerUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                cacheFile
            )

            // Lưu trực tiếp vào thư mục công khai Tải xuống (Downloads) của máy
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                val insertUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (insertUri != null) {
                    resolver.openOutputStream(insertUri)?.use { it.write(csvBytes) }
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(insertUri, contentValues, null, null)
                }
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) downloadsDir.mkdirs()
                val destFile = File(downloadsDir, fileName)
                destFile.writeBytes(csvBytes)
                MediaScannerConnection.scanFile(context, arrayOf(destFile.absolutePath), arrayOf("text/csv"), null)
            }

            return SavedCsvResult(
                fileName = fileName,
                cacheFile = cacheFile,
                providerUri = providerUri
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun exportSplitBillToCsv(
        context: Context,
        result: SplitBillResult,
        items: List<SplitExpenseItem>,
        members: List<SplitMember>
    ): File? {
        val res = saveSplitBillToDownloads(context, result, items, members)
        return res?.cacheFile
    }

    fun openCsvFile(context: Context, fileUri: Uri) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(fileUri, "text/csv")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Mở xem duyệt file bằng:"))
        } catch (e: Exception) {
            Toast.makeText(context, "Không tìm thấy ứng dụng đọc file CSV (Microsoft Excel, Google Sheets...)", Toast.LENGTH_LONG).show()
        }
    }

    fun shareCsvFile(
        context: Context,
        file: File,
        subject: String = "Báo cáo thu chi - Quản Lý Chi Tiêu"
    ) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, subject)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Chia sẻ file Excel / CSV qua:"))
    }
}
