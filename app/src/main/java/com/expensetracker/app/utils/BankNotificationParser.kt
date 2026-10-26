package com.expensetracker.app.utils

import com.expensetracker.app.data.local.entity.TransactionType
import java.util.regex.Pattern

data class ParsedBankNotification(
    val bankName: String,
    val amount: Double,
    val type: TransactionType,
    val rawContent: String,
    val suggestedNote: String
)

object BankNotificationParser {

    private val BANK_PACKAGES = mapOf(
        "com.VCB" to "Vietcombank",
        "com.vcb.digibank" to "Vietcombank",
        "com.mbmobile" to "MB Bank",
        "com.techcombank.bb.retail" to "Techcombank",
        "vn.com.techcombank.bb.app" to "Techcombank",
        "com.tpb.mb.gprsandroid" to "TPBank",
        "com.vnpay.vpbank" to "VPBank",
        "com.vpbank.onlinebanking" to "VPBank",
        "mobile.acb.com.vn" to "ACB",
        "com.vnpay.bidv" to "BIDV",
        "com.vietinbank.ipay" to "VietinBank",
        "com.mservice.momopay" to "MoMo",
        "vn.com.vng.zalopay" to "ZaloPay",
        "com.bplus.vtpay" to "Viettel Money"
    )

    // Regex pattern to extract money amounts in VND: e.g. -50,000VND, +120.000 đ, 50,000 VND
    private val AMOUNT_PATTERN = Pattern.compile(
        """([+-]?\s*[\d]{1,3}(?:[.,]\d{3})*(?:[.,]\d{1,2})?)\s*(?:VND|VNĐ|đ|dong|d\b)""",
        Pattern.CASE_INSENSITIVE
    )

    fun isBankApp(packageName: String): Boolean {
        return BANK_PACKAGES.containsKey(packageName)
    }

    fun getBankName(packageName: String): String {
        return BANK_PACKAGES[packageName] ?: "Ngân hàng"
    }

    fun parse(packageName: String, title: String, content: String): ParsedBankNotification? {
        val fullText = "$title $content"
        val bankName = getBankName(packageName)

        val matcher = AMOUNT_PATTERN.matcher(fullText)
        if (!matcher.find()) {
            return null
        }

        val rawAmountStr = matcher.group(1)?.replace("\\s+".toRegex(), "") ?: return null
        
        // Determine whether this is Expense or Income
        val isExpense = rawAmountStr.startsWith("-") || 
            fullText.contains("thanh toan", ignoreCase = true) ||
            fullText.contains("chi tieu", ignoreCase = true) ||
            fullText.contains("tru tien", ignoreCase = true) ||
            fullText.contains("GD thanh cong", ignoreCase = true) ||
            fullText.contains("rut tien", ignoreCase = true) ||
            fullText.contains("chuyen tien", ignoreCase = true)

        val isIncome = rawAmountStr.startsWith("+") ||
            fullText.contains("nhan duoc", ignoreCase = true) ||
            fullText.contains("cong tien", ignoreCase = true) ||
            fullText.contains("chuyen den", ignoreCase = true)

        val type = if (isIncome && !isExpense) {
            TransactionType.INCOME
        } else {
            TransactionType.EXPENSE
        }

        // Clean amount string to positive Double
        val cleanedNumber = rawAmountStr.replace("+", "").replace("-", "")
            .replace(",", "")
            .replace(".", "")

        val amount = cleanedNumber.toDoubleOrNull() ?: return null
        if (amount <= 0) return null

        val suggestedNote = when {
            content.isNotBlank() -> content.take(60)
            title.isNotBlank() -> title.take(60)
            else -> "Chi tiêu qua $bankName"
        }

        return ParsedBankNotification(
            bankName = bankName,
            amount = amount,
            type = type,
            rawContent = fullText,
            suggestedNote = suggestedNote
        )
    }
}
