package com.expensetracker.app.data.ai

import android.content.Context
import android.graphics.Bitmap
import com.expensetracker.app.data.local.entity.TransactionType
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class AiTransactionData(
    val amount: Double,
    val type: TransactionType,
    val categoryId: String,
    val suggestedAccountName: String?,
    val note: String
)

data class AiResponse(
    val replyText: String,
    val detectedTransactions: List<AiTransactionData> = emptyList()
) {
    // Backwards-compatible single transaction accessor
    val detectedTransaction: AiTransactionData?
        get() = detectedTransactions.firstOrNull()
}

class GeminiManager(context: Context) {

    private val prefs = context.getSharedPreferences("gemini_settings", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_API_KEY = "gemini_api_key"
        private const val KEY_MODEL = "gemini_model_name"
        const val DEFAULT_MODEL = "gemini-3.1-flash-lite"

        val AVAILABLE_MODELS = listOf(
            "gemini-3.1-flash-lite" to "Gemini 3.1 Flash Lite (Mặc định - Mới nhất & Siêu nhanh)",
            "gemini-2.5-flash" to "Gemini 2.5 Flash (Thế hệ mới, đa phương thức & tư duy)",
            "gemini-2.5-flash-lite" to "Gemini 2.5 Flash Lite (Phản hồi tức thì, tiết kiệm chi phí)"
        )
    }

    fun getApiKey(): String {
        return prefs.getString(KEY_API_KEY, "") ?: ""
    }

    fun setApiKey(apiKey: String) {
        prefs.edit().putString(KEY_API_KEY, apiKey.trim()).apply()
    }

    fun getSelectedModel(): String {
        val saved = prefs.getString(KEY_MODEL, null)
        val isInvalidOldModel = saved == "gemini-1.5-flash" || saved == "gemini-1.5-pro" || saved == "gemini-2.0-flash"
        return if (saved.isNullOrBlank() || isInvalidOldModel) {
            DEFAULT_MODEL
        } else {
            saved
        }
    }

    fun setSelectedModel(model: String) {
        prefs.edit().putString(KEY_MODEL, model.trim()).apply()
    }

    fun hasApiKey(): Boolean = getApiKey().isNotBlank()

    private fun getModel(): GenerativeModel {
        val apiKey = getApiKey()
        val modelName = getSelectedModel()
        return GenerativeModel(
            modelName = modelName,
            apiKey = apiKey,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                temperature = 0.2f
            },
            systemInstruction = content {
                text(
                    """
                    Bạn là Trợ lý Quản lý Chi tiêu AI thông minh cho ứng dụng trên điện thoại Android của người Việt Nam.
                    Nhiệm vụ của bạn:
                    1. Phân tích tin nhắn người dùng hoặc ảnh chụp màn hình hóa đơn, bill mua sắm (Shopee, Lazada, Grab, Tiki, hóa đơn siêu thị, nhà hàng, biến động số dư ngân hàng...).
                    2. Hiểu tiếng Việt đời thường và từ lóng tiền tệ: "50k" = 50000, "1 lít" = 100000, "2 cành" = 2000, "1 củ / 1 chai / 1 tr" = 1000000.
                    3. Đối với ảnh hóa đơn / biên lai / bill mua sắm (như Shopee, Lazada, TikTok Shop, Grab, Siêu thị...):
                       - Nếu có nhiều sản phẩm với số tiền cụ thể, hãy phân tích từng sản phẩm thành một giao dịch trong danh sách 'transactions'.
                       - Không bao gồm dòng tổng tiền thành một giao dịch riêng nếu đã phân tích các món lẻ (để tránh cộng trùng tiền).
                       - Nếu chỉ có duy nhất số tiền tổng thanh toán (hoặc hóa đơn quá dài/chỉ xem tổng bill), hãy tạo 1 giao dịch với số tiền tổng thanh toán thực tế (đã trừ mã giảm giá nếu có).
                    4. Bắt buộc trả về đúng định dạng JSON có cấu trúc sau:
                    {
                      "replyMessage": "Lời phản hồi thân thiện, tự nhiên và ngắn gọn bằng tiếng Việt gửi lại cho người dùng tóm tắt các khoản đã nhận diện.",
                      "transactions": [
                        {
                          "amount": 32549,
                          "type": "EXPENSE",
                          "categoryId": "shopping",
                          "suggestedAccount": "ShopeePay",
                          "note": "Mua móc treo túi xách từ UUMIR Official Store"
                        }
                      ]
                    }
                    5. Các mã danh mục (categoryId) hợp lệ:
                       - Chi tiêu: "food" (ăn uống), "shopping" (mua sắm), "transport" (đi lại/xe), "bills" (hóa đơn), "housing" (nhà cửa), "entertainment" (giải trí), "health" (sức khỏe/y tế), "education" (học tập), "other_expense" (chi phí khác).
                       - Thu nhập: "salary" (tiền lương), "bonus" (thưởng), "investment" (đầu tư), "other_income" (thu nhập khác).
                    6. Nếu tin nhắn là trò chuyện, tư vấn tài chính hoặc câu hỏi thông thường không có giao dịch thu chi:
                       Đặt "transactions": [] và điền câu trả lời tư vấn hữu ích vào "replyMessage".
                    """.trimIndent()
                )
            }
        )
    }

    suspend fun processTextMessage(userText: String, currentSpendingContext: String = ""): AiResponse =
        withContext(Dispatchers.IO) {
            try {
                if (!hasApiKey()) {
                    return@withContext AiResponse(
                        replyText = "Bạn chưa cấu hình Gemini API Key. Vui lòng nhập API Key để kích hoạt Trợ lý AI nhé!"
                    )
                }

                val prompt = if (currentSpendingContext.isNotBlank()) {
                    "Bối cảnh chi tiêu hiện tại: $currentSpendingContext\nTin nhắn người dùng: $userText\nTrả về đúng định dạng JSON có cấu trúc gồm 'replyMessage' và 'transactions'."
                } else {
                    "Tin nhắn người dùng: $userText\nTrả về đúng định dạng JSON có cấu trúc gồm 'replyMessage' và 'transactions'."
                }

                val response = getModel().generateContent(prompt)
                val jsonString = response.text ?: ""
                parseAiJson(jsonString)
            } catch (e: Exception) {
                e.printStackTrace()
                AiResponse(
                    replyText = "Lỗi khi kết nối với Gemini: ${e.localizedMessage ?: "Vui lòng kiểm tra lại API Key hoặc kết nối mạng."}"
                )
            }
        }

    suspend fun processReceiptImage(bitmap: Bitmap, userPrompt: String = ""): AiResponse =
        withContext(Dispatchers.IO) {
            try {
                if (!hasApiKey()) {
                    return@withContext AiResponse(
                        replyText = "Bạn chưa cấu hình Gemini API Key. Vui lòng nhập API Key để sử dụng tính năng quét hóa đơn!"
                    )
                }

                val promptText = if (userPrompt.isNotBlank()) {
                    "Hãy phân tích ảnh hóa đơn/bill này kèm ghi chú của tôi: $userPrompt\nTrả về đúng cấu trúc JSON gồm 'replyMessage' và danh sách 'transactions'."
                } else {
                    "Hãy phân tích ảnh hóa đơn / bill mua hàng (Shopee, Lazada, siêu thị...) này. Nhận diện các mặt hàng hoặc số tiền thanh toán thực tế, phân loại danh mục phù hợp và trả về đúng JSON có cấu trúc gồm 'replyMessage' và 'transactions'."
                }

                val inputContent = content {
                    image(bitmap)
                    text(promptText)
                }

                val response = getModel().generateContent(inputContent)
                val jsonString = response.text ?: ""
                parseAiJson(jsonString)
            } catch (e: Exception) {
                e.printStackTrace()
                AiResponse(
                    replyText = "Lỗi khi phân tích hóa đơn: ${e.localizedMessage ?: "Không thể đọc ảnh, bạn vui lòng thử lại nhé."}"
                )
            }
        }

    suspend fun analyzeInvestmentPortfolio(portfolioDetails: String): AiResponse =
        withContext(Dispatchers.IO) {
            try {
                if (!hasApiKey()) {
                    return@withContext AiResponse(
                        replyText = "Bạn chưa cấu hình Gemini API Key. Vui lòng nhập API Key để kích hoạt Trợ lý AI phân tích danh mục đầu tư nhé!"
                    )
                }

                val prompt = """
                    Tôi có danh mục tài sản và đầu tư như sau:
                    $portfolioDetails
                    
                    Hãy đóng vai trò chuyên gia cố vấn tài chính cá nhân độc lập:
                    1. Đánh giá tỷ lệ phân bổ rủi ro (giữa các tài sản an toàn như vàng, tiết kiệm với tài sản tăng trưởng như cổ phiếu, quỹ, crypto).
                    2. Tính toán hiệu suất sinh lời (ROI) và nhận xét danh mục.
                    3. Đưa ra 3 lời khuyên tài chính thiết thực cho người dùng tại Việt Nam về việc cân đối giữa chi tiêu sinh hoạt, quỹ dự phòng và tái đầu tư.
                    Trình bày ngắn gọn, súc tích, định dạng bullet point rõ ràng dễ đọc.
                """.trimIndent()

                val response = getModel().generateContent(prompt)
                val text = response.text ?: "Không thể phân tích dữ liệu đầu tư."
                AiResponse(replyText = text)
            } catch (e: Exception) {
                e.printStackTrace()
                AiResponse(
                    replyText = "Lỗi khi phân tích danh mục đầu tư: ${e.localizedMessage ?: "Vui lòng thử lại sau."}"
                )
            }
        }

    private fun parseAiJson(jsonString: String): AiResponse {
        val clean = cleanJsonString(jsonString)
        if (clean.isBlank()) {
            return AiResponse(replyText = "Không nhận được phản hồi từ AI.")
        }

        return try {
            val transactions = mutableListOf<AiTransactionData>()
            val replyMessage: String

            if (clean.startsWith("[")) {
                // Gemini returned a direct JSONArray of items
                val jsonArray = JSONArray(clean)
                val replyList = mutableListOf<String>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.optJSONObject(i) ?: continue
                    val isTx = obj.optBoolean("isTransaction", true)
                    val amount = obj.optDouble("amount", 0.0)
                    if (isTx && amount > 0.0) {
                        transactions.add(parseSingleTransaction(obj))
                    }
                    val msg = obj.optString("replyMessage", "").trim()
                    if (msg.isNotBlank() && !replyList.contains(msg)) {
                        replyList.add(msg)
                    }
                }
                replyMessage = when {
                    replyList.isNotEmpty() -> replyList.joinToString("\n\n")
                    transactions.isNotEmpty() -> "Em đã nhận diện và ghi nhận ${transactions.size} giao dịch từ hóa đơn thành công!"
                    else -> "Đã xử lý thông tin thành công."
                }
            } else {
                // Gemini returned a JSONObject
                val json = JSONObject(clean)
                if (json.has("transactions")) {
                    val arr = json.optJSONArray("transactions")
                    if (arr != null) {
                        for (i in 0 until arr.length()) {
                            val obj = arr.optJSONObject(i) ?: continue
                            val amount = obj.optDouble("amount", 0.0)
                            if (amount > 0.0) {
                                transactions.add(parseSingleTransaction(obj))
                            }
                        }
                    }
                } else {
                    val isTx = json.optBoolean("isTransaction", true)
                    val amount = json.optDouble("amount", 0.0)
                    if (isTx && amount > 0.0) {
                        transactions.add(parseSingleTransaction(json))
                    }
                }

                val msg = json.optString("replyMessage", "").trim()
                replyMessage = when {
                    msg.isNotBlank() -> msg
                    transactions.isNotEmpty() -> "Em đã nhận diện và ghi nhận ${transactions.size} khoản chi tiêu từ hóa đơn thành công!"
                    else -> "Đã xử lý thông tin thành công."
                }
            }

            AiResponse(
                replyText = replyMessage,
                detectedTransactions = transactions
            )
        } catch (e: Exception) {
            e.printStackTrace()
            val trimmed = jsonString.trim()
            val fallback = if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
                "Em đã đọc hóa đơn nhưng cấu trúc dữ liệu chưa chuẩn. Bạn vui lòng thử lại hoặc chụp rõ nét hơn nhé!"
            } else {
                if (jsonString.isNotBlank()) jsonString else "Đã phân tích xong nhưng không thể đọc dữ liệu có cấu trúc."
            }
            AiResponse(replyText = fallback)
        }
    }

    private fun cleanJsonString(raw: String): String {
        var str = raw.trim()
        if (str.startsWith("```json", ignoreCase = true)) {
            str = str.substring(7).trim()
        } else if (str.startsWith("```")) {
            str = str.substring(3).trim()
        }
        if (str.endsWith("```")) {
            str = str.substring(0, str.length - 3).trim()
        }

        val firstBrace = str.indexOf('{')
        val firstBracket = str.indexOf('[')

        return if (firstBracket in 0 until (if (firstBrace >= 0) firstBrace else Int.MAX_VALUE)) {
            val lastBracket = str.lastIndexOf(']')
            if (lastBracket > firstBracket) {
                str.substring(firstBracket, lastBracket + 1)
            } else {
                str
            }
        } else if (firstBrace >= 0) {
            val lastBrace = str.lastIndexOf('}')
            if (lastBrace > firstBrace) {
                str.substring(firstBrace, lastBrace + 1)
            } else {
                str
            }
        } else {
            str
        }
    }

    private fun parseSingleTransaction(obj: JSONObject): AiTransactionData {
        val amount = obj.optDouble("amount", 0.0)
        val typeStr = obj.optString("type", "EXPENSE")
        val type = if (typeStr.equals("INCOME", ignoreCase = true)) TransactionType.INCOME else TransactionType.EXPENSE
        val categoryId = obj.optString("categoryId", "shopping").ifBlank { "shopping" }
        val accountName = obj.optString("suggestedAccount").takeIf { it.isNotBlank() && it != "null" }
        val note = obj.optString("note", "Chi tiêu Shopee / Hóa đơn").ifBlank { "Chi tiêu Shopee / Hóa đơn" }

        return AiTransactionData(
            amount = amount,
            type = type,
            categoryId = categoryId,
            suggestedAccountName = accountName,
            note = note
        )
    }
}
