package com.expensetracker.app.data.ai

import android.content.Context
import android.graphics.Bitmap
import com.expensetracker.app.data.local.entity.TransactionType
import com.expensetracker.app.data.model.ScannedReceiptItem
import com.expensetracker.app.utils.CurrencyUtils
import com.expensetracker.app.utils.ImageUtils
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

    private fun getReceiptOcrModel(): GenerativeModel {
        val apiKey = getApiKey()
        val modelName = getSelectedModel()
        return GenerativeModel(
            modelName = modelName,
            apiKey = apiKey,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                temperature = 0.1f // Nhiệt độ tối thiểu để OCR đọc số và chữ chính xác 100%, không bị ảo giác
            },
            systemInstruction = content {
                text(
                    """
                    Bạn là chuyên gia OCR hàng đầu chuyên bóc tách hóa đơn, biên lai, phiếu tính tiền, bill cafe, nhà hàng, quán ăn, quán nhậu, siêu thị tại Việt Nam.
                    Nhiệm vụ của bạn:
                    1. Đọc kỹ hình ảnh hóa đơn (kể cả ảnh in nhiệt mờ, chụp nghiêng, chụp trong điều kiện ánh sáng yếu hoặc chữ viết tay) và nhận diện từng dòng món ăn, thức uống, hàng hóa, dịch vụ thực tế.
                    2. Bỏ qua các dòng tiêu đề thông tin quán (tên quán, địa chỉ, số điện thoại, wifi, nhân viên, số bàn, giờ in).
                    3. Bỏ qua các dòng tổng thanh toán cuối bill: 'Tổng cộng', 'Total', 'Tiền khách đưa', 'Tiền thối', 'Tiền thừa', 'Điểm tích lũy'.
                    4. NẾU hóa đơn có dòng riêng biệt về 'Phí dịch vụ' hoặc 'Thuế VAT' / 'Giảm giá', hãy ghi nhận như một mục riêng (ví dụ: 'Phí VAT 8%', 'Phí phục vụ') để tổng tiền phân chia được khớp chuẩn.
                    5. Đối với mỗi món:
                       - 'name': Tên món ăn/dịch vụ rõ ràng, chuẩn tiếng Việt (ví dụ: 'Trà đào cam sả', 'Bò tơ nướng tảng', 'Bia Heineken', 'Khăn lạnh').
                       - 'quantity': Số lượng (số nguyên dương, ví dụ 1, 2, 5). Nếu hóa đơn không ghi số lượng hoặc ghi '1', đặt là 1.
                       - 'amount': Tổng thành tiền của dòng món đó (Double hoặc Long, không có dấu chấm phẩy, không chứa ký tự 'đ' hay 'VND', ví dụ 45000 x 2 = 90000).
                    6. Bắt buộc trả về đúng định dạng JSON chuẩn theo mẫu:
                    {
                      "items": [
                        {
                          "name": "Bò nướng tảng",
                          "quantity": 2,
                          "amount": 250000
                        },
                        {
                          "name": "Bia Heineken",
                          "quantity": 10,
                          "amount": 240000
                        }
                      ]
                    }
                    Nếu hóa đơn chỉ thấy được tổng số tiền thanh toán (do phần trên bị mất hoặc quá mờ), hãy tạo 1 mục 'Hóa đơn tổng' với số tiền đó. Không bao giờ trả về danh sách rỗng nếu hóa đơn có chữ và số tiền!
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

    suspend fun analyzeSplitBillWithAi(
        eventTitle: String,
        totalAmount: Double,
        perPersonAvg: Double,
        myShare: Double,
        itemsBreakdown: String,
        settlementsBreakdown: String
    ): Triple<String, String, String> = withContext(Dispatchers.IO) {
        try {
            if (!hasApiKey()) {
                val fallbackSummary = "Buổi '${eventTitle}' có tổng chi tiêu là ${CurrencyUtils.formatVnd(totalAmount)}, chia đều mỗi người ${CurrencyUtils.formatVnd(perPersonAvg)}."
                val fallbackMessage = "Tiền '${eventTitle}':\n- Tổng: ${CurrencyUtils.formatVnd(totalAmount)} (chia ${CurrencyUtils.formatVnd(perPersonAvg)}/người)\n${settlementsBreakdown}\nMọi người kiểm tra và thanh toán sòng phẳng nhé!"
                val fallbackAdvice = "Chia tiền ngay sau khi đi chơi giúp tài chính minh bạch và gắn kết tình bạn bền chặt!"
                return@withContext Triple(fallbackSummary, fallbackMessage, fallbackAdvice)
            }

            val prompt = """
                Bạn là Trợ lý AI tài chính cá nhân thông minh và vui tính cho ứng dụng quản lý chi tiêu của người Việt Nam.
                Người dùng vừa tổ chức một buổi chi tiêu nhóm:
                - Tên sự kiện: $eventTitle
                - Tổng chi phí: $totalAmount VNĐ
                - Chi phí trung bình mỗi người: $perPersonAvg VNĐ
                - Phần chi phí của 'Tôi' (người dùng): $myShare VNĐ
                - Chi tiết các khoản chi đã thanh toán:
                $itemsBreakdown
                - Ma trận thanh toán nợ ('ai cần chuyển cho ai'):
                $settlementsBreakdown

                Nhiệm vụ của bạn:
                Trả về JSON có đúng cấu trúc sau:
                {
                  "summary": "Lời tóm tắt súc tích, dí dỏm bằng tiếng Việt về buổi ăn chơi và kết quả phân chia tiền.",
                  "suggestedMessage": "Mẫu tin nhắn ngắn gọn, thân thiện và sòng phẳng để copy gửi vào nhóm chat Zalo/Messenger (kèm nhắc STK và lời nhắn nhẹ nhàng vui vẻ).",
                  "aiAdvice": "1 lời khuyên tài chính thông minh hoặc mẹo tiết kiệm khi đi chơi nhóm."
                }
            """.trimIndent()

            val response = getModel().generateContent(prompt)
            val jsonString = response.text ?: ""
            val clean = cleanJsonString(jsonString)
            val json = JSONObject(clean)
            val summary = json.optString("summary", "Tổng chi phí là ${CurrencyUtils.formatVnd(totalAmount)}, chia đều ${CurrencyUtils.formatVnd(perPersonAvg)}/người.")
            val suggestedMessage = json.optString("suggestedMessage", "Tổng tiền ${eventTitle}: ${CurrencyUtils.formatVnd(totalAmount)}.\n$settlementsBreakdown")
            val aiAdvice = json.optString("aiAdvice", "Thanh toán sòng phẳng, tình bạn bền lâu!")
            Triple(summary, suggestedMessage, aiAdvice)
        } catch (e: Exception) {
            e.printStackTrace()
            val fallbackSummary = "Buổi '${eventTitle}' có tổng chi tiêu là ${CurrencyUtils.formatVnd(totalAmount)}, chia đều mỗi người ${CurrencyUtils.formatVnd(perPersonAvg)}."
            val fallbackMessage = "Tiền '${eventTitle}':\n- Tổng: ${CurrencyUtils.formatVnd(totalAmount)} (chia ${CurrencyUtils.formatVnd(perPersonAvg)}/người)\n${settlementsBreakdown}\nMọi người kiểm tra và thanh toán sòng phẳng nhé!"
            val fallbackAdvice = "Chia tiền sòng phẳng giúp nhóm luôn thoải mái và vui vẻ trong các chuyến đi sau!"
            Triple(fallbackSummary, fallbackMessage, fallbackAdvice)
        }
    }

    suspend fun parseReceiptForSplitBill(bitmap: Bitmap): List<ScannedReceiptItem> = withContext(Dispatchers.IO) {
        try {
            if (!hasApiKey()) return@withContext emptyList()

            // Tối ưu hóa kích thước và chất lượng ảnh trước khi gửi cho Gemini (tối đa 2048px nét căng)
            val optimizedBitmap = ImageUtils.optimizeBitmap(bitmap)

            val prompt = """
                Hãy phân tích kỹ hình ảnh hóa đơn / biên lai này và bóc tách tất cả các món ăn, đồ uống hoặc dịch vụ lẻ.
                Đối với mỗi dòng món: 'name' (tên món rõ ràng), 'quantity' (số lượng), 'amount' (tổng thành tiền của dòng đó).
                Bắt buộc trả về đúng định dạng JSON chuẩn:
                {
                  "items": [
                    {
                      "name": "Tên món",
                      "quantity": 1,
                      "amount": 45000
                    }
                  ]
                }
            """.trimIndent()

            val inputContent = content {
                image(optimizedBitmap)
                text(prompt)
            }

            // Gọi model OCR chuyên biệt
            val response = getReceiptOcrModel().generateContent(inputContent)
            val jsonString = response.text ?: ""
            val parsedList = parseScannedReceiptItems(jsonString)

            if (parsedList.isNotEmpty()) {
                parsedList
            } else {
                // Nếu model OCR trả về rỗng, fallback sang model mặc định
                val fallbackResponse = getModel().generateContent(inputContent)
                val fallbackJson = fallbackResponse.text ?: ""
                parseScannedReceiptItems(fallbackJson)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback retry với prompt đơn giản
            try {
                val optimizedBitmap = ImageUtils.optimizeBitmap(bitmap)
                val fallbackResponse = getModel().generateContent(content {
                    image(optimizedBitmap)
                    text("Đọc hóa đơn này và trả về danh sách các món ăn dạng JSON: {\"items\": [{\"name\": \"...\", \"quantity\": 1, \"amount\": 50000}]}")
                })
                val fallbackJson = fallbackResponse.text ?: ""
                parseScannedReceiptItems(fallbackJson)
            } catch (ex: Exception) {
                ex.printStackTrace()
                emptyList()
            }
        }
    }

    /**
     * Bộ phân tích JSON cực kỳ linh hoạt và bao quát cho hóa đơn:
     * Chấp nhận mọi biến thể format từ Gemini (dù trả về mảng trực tiếp, trường "items", "transactions", "dishes", "products", v.v.)
     */
    fun parseScannedReceiptItems(jsonString: String): List<ScannedReceiptItem> {
        val clean = cleanJsonString(jsonString)
        if (clean.isBlank()) return emptyList()

        val results = mutableListOf<ScannedReceiptItem>()

        fun parseItemAmount(obj: JSONObject): Double {
            val amountKeys = listOf("amount", "total", "price", "total_price", "thanh_tien", "cost", "tien", "dongia")
            for (key in amountKeys) {
                if (obj.has(key)) {
                    val rawVal = obj.opt(key)
                    if (rawVal is Number) {
                        val d = rawVal.toDouble()
                        if (d > 0.0) return d
                    } else if (rawVal is String) {
                        val parsed = CurrencyUtils.parseMoneyInput(rawVal)
                        if (parsed != null && parsed > 0.0) return parsed
                    }
                }
            }
            return 0.0
        }

        fun parseItemName(obj: JSONObject): String {
            val nameKeys = listOf("name", "item", "item_name", "title", "dish", "product", "note", "description", "ten_mon")
            for (key in nameKeys) {
                val str = obj.optString(key, "").trim()
                if (str.isNotBlank()) return str
            }
            return ""
        }

        fun parseItemQuantity(obj: JSONObject): Int {
            val qtyKeys = listOf("quantity", "qty", "count", "so_luong", "sl")
            for (key in qtyKeys) {
                val q = obj.optInt(key, 0)
                if (q > 0) return q
            }
            return 1
        }

        fun addItemIfValid(obj: JSONObject) {
            val name = parseItemName(obj)
            val amount = parseItemAmount(obj)
            val quantity = parseItemQuantity(obj)

            val lowerName = name.lowercase()
            val isIgnored = lowerName.contains("tổng cộng") ||
                    lowerName.contains("tiền mặt") ||
                    lowerName.contains("khách đưa") ||
                    lowerName.contains("khách trả") ||
                    lowerName.contains("tiền thừa") ||
                    lowerName.contains("tiền thối") ||
                    (lowerName.contains("thanh toán") && !lowerName.contains("hóa đơn")) ||
                    lowerName.contains("wifi") ||
                    lowerName.contains("hotline")

            if (name.isNotBlank() && amount > 0.0 && !isIgnored) {
                results.add(ScannedReceiptItem(name = name, quantity = quantity, amount = amount))
            }
        }

        try {
            if (clean.startsWith("[")) {
                // Trả về trực tiếp JSONArray
                val arr = JSONArray(clean)
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    addItemIfValid(obj)
                }
            } else {
                // Trả về JSONObject
                val json = JSONObject(clean)

                // Tìm mảng trong các key phổ biến
                val possibleArrayKeys = listOf("items", "transactions", "dishes", "products", "bill_items", "details", "lines", "data", "list")
                var foundArray: JSONArray? = null
                for (k in possibleArrayKeys) {
                    if (json.has(k) && json.optJSONArray(k) != null) {
                        foundArray = json.optJSONArray(k)
                        break
                    }
                }

                // Nếu không có trong danh sách trên, duyệt tất cả các key để tìm JSONArray
                if (foundArray == null) {
                    val keys = json.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        val arr = json.optJSONArray(k)
                        if (arr != null && arr.length() > 0) {
                            foundArray = arr
                            break
                        }
                    }
                }

                if (foundArray != null) {
                    for (i in 0 until foundArray.length()) {
                        val obj = foundArray.optJSONObject(i) ?: continue
                        addItemIfValid(obj)
                    }
                } else {
                    // Nếu trả về 1 object đơn lẻ
                    addItemIfValid(json)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return results
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
