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

    // Regex 0: Explicit signed amount (+50,000 VND, -150.000 đ, + 500,000, - 25.000)
    // Priority #1 because this directly captures the transaction delta (ignoring unsigned final balance)
    private val SIGNED_AMOUNT_PATTERN = Pattern.compile(
        """([+-]\s*[\d]{1,3}(?:[.,]\d{3})*(?:[.,]\d{1,2})?)\s*(?:VND|VNĐ|đ|dong|d\b)?""",
        Pattern.CASE_INSENSITIVE
    )

    // Regex 1: Amount followed by currency symbol (50,000 VND, 100.000 đ, 25.000d)
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
        """(?:Số tiền|So tien|Biến động|Bien dong|GD|GD:|thanh toán|thanh toan)[\s:-]*([+-]?\s*[\d]{1,3}(?:[.,]\d{3})+)""",
        Pattern.CASE_INSENSITIVE
    )

    private val INCOME_KEYWORDS = listOf(
        "nhan tien", "nhận tiền",
        "nhan duoc", "nhận được",
        "cong tien", "cộng tiền",
        "chuyen den", "chuyển đến",
        "chuyen vao", "chuyển vào",
        "tien vao", "tiền vào",
        "tang", "tăng",
        "nap tien", "nạp tiền",
        "hoan tien", "hoàn tiền",
        "tra luong", "trả lương",
        "thanh toan luong",
        "lai tien gui", "lãi tiền gửi",
        "tien lai", "tiền lãi",
        "thu nhap", "thu nhập"
    )

    private val EXPENSE_KEYWORDS = listOf(
        "tru tien", "trừ tiền",
        "thanh toan", "thanh toán",
        "chi tieu", "chi tiêu",
        "tien ra", "tiền ra",
        "rut tien", "rút tiền",
        "chuyen di", "chuyển đi",
        "chuyen cho", "chuyển cho",
        "giam", "giảm",
        "phi dich vu", "phí dịch vụ",
        "mua hang", "mua hàng"
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

        // 1. First priority: Match explicitly signed amounts (+50,000 or -50,000)
        // This avoids mistakenly capturing unsigned balance amounts like "So du: 1,500,000 VND"
        val mSigned = SIGNED_AMOUNT_PATTERN.matcher(fullText)
        if (mSigned.find()) {
            rawAmountStr = mSigned.group(1)
        } else {
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
        }

        if (rawAmountStr == null) {
            return null
        }

        val cleanRaw = rawAmountStr.replace("\\s+".toRegex(), "")

        // 2. Determine Expense vs Income with 100% authoritative priority for '+' and '-'
        val type = when {
            cleanRaw.startsWith("+") -> TransactionType.INCOME
            cleanRaw.startsWith("-") -> TransactionType.EXPENSE
            // If amount itself lacks +/- sign, analyze Vietnamese financial keywords
            INCOME_KEYWORDS.any { fullText.contains(it, ignoreCase = true) } -> TransactionType.INCOME
            EXPENSE_KEYWORDS.any { fullText.contains(it, ignoreCase = true) } -> TransactionType.EXPENSE
            else -> TransactionType.EXPENSE
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
