package com.expensetracker.app.data.ai

import android.content.Context
import android.graphics.Bitmap
import com.expensetracker.app.data.local.entity.TransactionType
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
    val detectedTransaction: AiTransactionData? = null
)

class GeminiManager(context: Context) {

    private val prefs = context.getSharedPreferences("gemini_settings", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_API_KEY = "gemini_api_key"
        private const val KEY_MODEL = "gemini_model_name"
        const val DEFAULT_MODEL = "gemini-1.5-flash"

        val AVAILABLE_MODELS = listOf(
            "gemini-1.5-flash" to "Gemini 1.5 Flash (Nhanh, nhẹ, quét bill chuẩn - Khuyên dùng)",
            "gemini-1.5-pro" to "Gemini 1.5 Pro (Tư duy sâu, hóa đơn dài & phức tạp)",
            "gemini-2.0-flash" to "Gemini 2.0 Flash (Thế hệ mới nhất, tốc độ cực nhanh)",
            "gemini-2.0-flash-lite" to "Gemini 2.0 Flash Lite (Siêu nhẹ, tiết kiệm tài nguyên)"
        )
    }

    fun getApiKey(): String {
        return prefs.getString(KEY_API_KEY, "") ?: ""
    }

    fun setApiKey(apiKey: String) {
        prefs.edit().putString(KEY_API_KEY, apiKey.trim()).apply()
    }

    fun getSelectedModel(): String {
        return prefs.getString(KEY_MODEL, DEFAULT_MODEL) ?: DEFAULT_MODEL
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
                    3. Bắt buộc trả về đúng định dạng JSON sau:
                    {
                      "isTransaction": true hoặc false,
                      "amount": 55000,
                      "type": "EXPENSE" hoặc "INCOME",
                      "categoryId": "food" | "shopping" | "transport" | "bills" | "housing" | "entertainment" | "health" | "education" | "salary" | "other_expense" | "other_income",
                      "suggestedAccount": "Tiền mặt" | "VPBank" | "MoMo" | null,
                      "note": "Mô tả ngắn gọn mục đích",
                      "replyMessage": "Lời phản hồi thân thiện, ngắn gọn và tự nhiên gửi lại cho người dùng."
                    }
                    Nếu tin nhắn là trò chuyện, tư vấn tài chính hoặc câu hỏi thông thường, đặt "isTransaction": false và điền câu trả lời hữu ích vào "replyMessage".
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
                    "Bối cảnh chi tiêu hiện tại: $currentSpendingContext\nTin nhắn người dùng: $userText"
                } else {
                    userText
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
                    "Hãy phân tích hóa đơn / bill này kèm ghi chú của tôi: $userPrompt"
                } else {
                    "Hãy phân tích ảnh hóa đơn / bill mua hàng (Shopee, Lazada, siêu thị...) này, tìm tổng số tiền thanh toán thực tế, tên cửa hàng/danh mục và tạo giao dịch chi tiêu phù hợp."
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

    private fun parseAiJson(jsonString: String): AiResponse {
        return try {
            val json = JSONObject(jsonString)
            val isTx = json.optBoolean("isTransaction", false)
            val replyMessage = json.optString("replyMessage", "Đã xử lý thông tin thành công!")

            if (!isTx) {
                return AiResponse(replyText = replyMessage)
            }

            val amount = json.optDouble("amount", 0.0)
            val typeStr = json.optString("type", "EXPENSE")
            val type = if (typeStr.equals("INCOME", ignoreCase = true)) TransactionType.INCOME else TransactionType.EXPENSE
            val categoryId = json.optString("categoryId", "food")
            val accountName = json.optString("suggestedAccount").takeIf { it.isNotBlank() && it != "null" }
            val note = json.optString("note", "Ghi chép qua AI")

            AiResponse(
                replyText = replyMessage,
                detectedTransaction = AiTransactionData(
                    amount = amount,
                    type = type,
                    categoryId = categoryId,
                    suggestedAccountName = accountName,
                    note = note
                )
            )
        } catch (e: Exception) {
            AiResponse(
                replyText = if (jsonString.isNotBlank()) jsonString else "Đã phân tích xong nhưng không thể đọc dữ liệu có cấu trúc."
            )
        }
    }
}
