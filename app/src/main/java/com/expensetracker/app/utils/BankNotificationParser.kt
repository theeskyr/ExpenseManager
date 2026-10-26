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
        "com.vcb" to "Vietcombank",
        "com.vietcombank" to "Vietcombank",
        "com.mbmobile" to "MB Bank",
        "com.mb.mbbank" to "MB Bank",
        "com.techcombank" to "Techcombank",
        "vn.com.techcombank" to "Techcombank",
        "com.tpb" to "TPBank",
        "com.tpbank" to "TPBank",
        "com.vnpay.vpbank" to "VPBank",
        "com.vpbank" to "VPBank",
        "mobile.acb.com.vn" to "ACB",
        "com.acb" to "ACB",
        "com.vnpay.bidv" to "BIDV",
        "com.bidv" to "BIDV",
        "com.vietinbank" to "VietinBank",
        "com.vnpay.agribank" to "Agribank",
        "com.agribank" to "Agribank",
        "com.sacombank" to "Sacombank",
        "com.vib" to "VIB",
        "com.mservice.momopay" to "MoMo",
        "vn.com.vng.zalopay" to "ZaloPay",
        "com.bplus.vtpay" to "Viettel Money",
        "com.airpay" to "ShopeePay",
        "xyz.be.cake" to "Cake",
        "com.timo" to "Timo"
    )

    private val SMS_PACKAGES = setOf(
        "com.google.android.apps.messaging",
        "com.samsung.android.messaging",
        "com.android.mms",
        "com.android.messaging"
    )

    private val BANK_SMS_SENDERS = listOf(
        "VCB", "VIETCOMBANK", "MBBANK", "MB BANK", "TCB", "TECHCOMBANK",
        "VPBANK", "TPBANK", "ACB", "BIDV", "VIETINBANK", "MOMO", "ZALOPAY",
        "AGRIBANK", "VIB", "SACOMBANK", "SHB", "OCB", "HDBANK"
    )

    // Regex 1: Amount followed by currency symbol (-50,000 VND, 100.000 đ, 25.000d)
    private val AMOUNT_PATTERN_1 = Pattern.compile(
        """([+-]?\s*[\d]{1,3}(?:[.,]\d{3})*(?:[.,]\d{1,2})?)\s*(?:VND|VNĐ|đ|dong|d\b)""",
        Pattern.CASE_INSENSITIVE
    )

    // Regex 2: Currency symbol followed by amount (VND 50,000, đ 100.000)
    private val AMOUNT_PATTERN_2 = Pattern.compile(
        """(?:VND|VNĐ|đ|dong)\s*([+-]?\s*[\d]{1,3}(?:[.,]\d{3})*(?:[.,]\d{1,2})?)""",
        Pattern.CASE_INSENSITIVE
    )

    // Regex 3: Keywords like "So tien:", "Bien dong:", "GD:" followed by amount
    private val AMOUNT_PATTERN_3 = Pattern.compile(
        """(?:Số tiền|So tien|Số dư|So du|Biến động|Bien dong|GD|GD:|thanh toán|thanh toan)[\s:-]*([+-]?\s*[\d]{1,3}(?:[.,]\d{3})+)""",
        Pattern.CASE_INSENSITIVE
    )

    fun isBankAppOrSms(packageName: String, title: String, content: String): Boolean {
        val lowerPkg = packageName.lowercase()
        // Check if package belongs to known banks
        if (BANK_PACKAGES.keys.any { lowerPkg.contains(it) }) {
            return true
        }

        // Check if this is an SMS from a bank
        if (SMS_PACKAGES.contains(lowerPkg) || lowerPkg.contains("messaging") || lowerPkg.contains("mms")) {
            val upperHeader = "$title $content".uppercase()
            return BANK_SMS_SENDERS.any { upperHeader.contains(it) }
        }

        return false
    }

    fun getBankName(packageName: String, title: String): String {
        val lowerPkg = packageName.lowercase()
        for ((key, name) in BANK_PACKAGES) {
            if (lowerPkg.contains(key)) return name
        }

        val upperTitle = title.uppercase()
        for (sender in BANK_SMS_SENDERS) {
            if (upperTitle.contains(sender)) return sender
        }

        return "Ngân hàng"
    }

    fun parse(packageName: String, title: String, content: String): ParsedBankNotification? {
        val fullText = "$title $content"
        val bankName = getBankName(packageName, title)

        var rawAmountStr: String? = null

        val m1 = AMOUNT_PATTERN_1.matcher(fullText)
        if (m1.find()) {
            rawAmountStr = m1.group(1)
        } else {
            val m2 = AMOUNT_PATTERN_2.matcher(fullText)
            if (m2.find()) {
                rawAmountStr = m2.group(1)
            } else {
                val m3 = AMOUNT_PATTERN_3.matcher(fullText)
                if (m3.find()) {
                    rawAmountStr = m3.group(1)
                }
            }
        }

        if (rawAmountStr == null) {
            return null
        }

        val cleanRaw = rawAmountStr.replace("\\s+".toRegex(), "")

        // Determine Expense vs Income
        val isExplicitExpense = cleanRaw.startsWith("-") ||
                fullText.contains("thanh toan", ignoreCase = true) ||
                fullText.contains("thanh toán", ignoreCase = true) ||
                fullText.contains("chi tieu", ignoreCase = true) ||
                fullText.contains("chi tiêu", ignoreCase = true) ||
                fullText.contains("tru tien", ignoreCase = true) ||
                fullText.contains("trừ tiền", ignoreCase = true) ||
                fullText.contains("rut tien", ignoreCase = true) ||
                fullText.contains("rút tiền", ignoreCase = true) ||
                fullText.contains("chuyen tien", ignoreCase = true) ||
                fullText.contains("chuyển tiền", ignoreCase = true) ||
                fullText.contains("GD thanh cong", ignoreCase = true)

        val isExplicitIncome = cleanRaw.startsWith("+") ||
                fullText.contains("nhan duoc", ignoreCase = true) ||
                fullText.contains("nhận được", ignoreCase = true) ||
                fullText.contains("cong tien", ignoreCase = true) ||
                fullText.contains("cộng tiền", ignoreCase = true) ||
                fullText.contains("chuyen den", ignoreCase = true) ||
                fullText.contains("chuyển đến", ignoreCase = true)

        val type = if (isExplicitIncome && !isExplicitExpense) {
            TransactionType.INCOME
        } else {
            TransactionType.EXPENSE
        }

        // Clean amount string to positive Double
        val cleanedNumber = cleanRaw.replace("+", "").replace("-", "")
            .replace(",", "")
            .replace(".", "")

        val amount = cleanedNumber.toDoubleOrNull() ?: return null
        if (amount <= 0 || amount > 100_000_000_000.0) return null

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
