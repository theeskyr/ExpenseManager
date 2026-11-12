/**
 * Google Gemini AI Integration for Web (Chat & OCR Bill Scanning)
 */
const GeminiManager = {
  DEFAULT_MODEL: 'gemini-3.1-flash-lite',

  AVAILABLE_MODELS: [
    { id: 'gemini-3.1-flash-lite', name: 'Gemini 3.1 Flash Lite (Mặc định - Nhanh & Mới nhất)' },
    { id: 'gemini-2.5-flash', name: 'Gemini 2.5 Flash (Thế hệ mới, đa phương thức & tư duy)' },
    { id: 'gemini-2.5-flash-lite', name: 'Gemini 2.5 Flash Lite (Phản hồi tức thì, tối ưu chi phí)' }
  ],

  async getApiKey() {
    return await getSetting('gemini_api_key', '');
  },

  async setApiKey(key) {
    await saveSetting('gemini_api_key', key.trim());
  },

  async getSelectedModel() {
    return await getSetting('gemini_model_name', this.DEFAULT_MODEL);
  },

  async setSelectedModel(model) {
    await saveSetting('gemini_model_name', model.trim());
  },

  /**
   * Kiểm tra tính hợp lệ của API Key với Google Gemini API
   */
  async testConnection(customKey = null, customModel = null) {
    const apiKey = (customKey !== null ? customKey : await this.getApiKey()).trim();
    if (!apiKey) {
      throw new Error('Vui lòng nhập API Key để kiểm tra.');
    }

    const model = customModel || await this.getSelectedModel();
    const url = `https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent?key=${apiKey}`;

    const response = await fetch(url, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        contents: [
          {
            role: 'user',
            parts: [{ text: 'Trả lời đúng 2 chữ "Kết nối thành công" bằng tiếng Việt.' }]
          }
        ]
      })
    });

    if (!response.ok) {
      const errData = await response.json().catch(() => ({}));
      const msg = errData?.error?.message || response.statusText;
      throw new Error(`Lỗi kết nối Gemini (${response.status}): ${msg}`);
    }

    return true;
  },

  /**
   * Chat với trợ lý AI tài chính cá nhân (hỗ trợ văn bản và ảnh)
   */
  async chatWithAi(userMessage, imageBase64 = null) {
    const apiKey = await this.getApiKey();
    if (!apiKey) {
      throw new Error('Vui lòng cài đặt Gemini API Key trong tab Trợ lý AI hoặc Cài đặt trước khi tiếp tục.');
    }

    const model = await this.getSelectedModel();
    const url = `https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent?key=${apiKey}`;

    const systemInstruction = `Bạn là Trợ lý Quản lý Chi tiêu và Tài chính Cá nhân Thông minh (Expense Manager AI).
Nhiệm vụ của bạn:
1. Trả lời thân thiện, súc tích bằng tiếng Việt, đưa ra lời khuyên tài chính thực tế và phù hợp với người Việt.
2. Nếu người dùng nói về một khoản thu hoặc chi, hoặc gửi ảnh hóa đơn/bill chuyển khoản và yêu cầu ghi nhận, hãy phân tích và trả về định dạng JSON sau ở cuối câu trả lời:
\`\`\`json
{
  "action": "add_transaction",
  "amount": 45000,
  "type": "EXPENSE",
  "category": "Ăn uống",
  "note": "Ăn phở"
}
\`\`\`
Trong đó "type" là "EXPENSE" hoặc "INCOME", "category" là một trong các loại: Ăn uống, Mua sắm, Di chuyển, Hóa đơn, Giải trí, Khác; "amount" là số nguyên VND.
3. Nếu người dùng gửi ảnh hóa đơn/bill chuyển khoản, hãy đọc số tiền, bên nhận và tóm tắt ngắn gọn.`;

    const contents = [];
    const parts = [];

    if (imageBase64) {
      const mimeType = imageBase64.startsWith('data:image/png') ? 'image/png' : 'image/jpeg';
      const cleanBase64 = imageBase64.replace(/^data:image\/\w+;base64,/, '');
      parts.push({
        inlineData: {
          mimeType: mimeType,
          data: cleanBase64
        }
      });
    }

    parts.push({
      text: userMessage || 'Hãy phân tích hình ảnh hóa đơn/bill này giúp tôi.'
    });

    contents.push({ role: 'user', parts });

    const requestBody = {
      systemInstruction: {
        parts: [{ text: systemInstruction }]
      },
      contents,
      generationConfig: {
        temperature: 0.4
      }
    };

    const response = await fetch(url, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(requestBody)
    });

    if (!response.ok) {
      const errData = await response.json().catch(() => ({}));
      const msg = errData?.error?.message || response.statusText;
      throw new Error(`Lỗi Gemini API (${response.status}): ${msg}`);
    }

    const data = await response.json();
    const candidate = data.candidates?.[0];
    const text = candidate?.content?.parts?.map((p) => p.text).join('') || 'Không nhận được phản hồi từ AI.';

    // Check if JSON transaction action is embedded
    let actionData = null;
    const jsonMatch = text.match(/```json\s*([\s\S]*?)\s*```/);
    if (jsonMatch) {
      try {
        actionData = JSON.parse(jsonMatch[1]);
      } catch (_) {}
    }

    return {
      text: text.replace(/```json[\s\S]*?```/, '').trim(),
      actionData
    };
  },

  /**
   * Quét và bóc tách thông tin hóa đơn/bill (tên quán, tổng tiền, danh mục, từng món)
   */
  async scanReceipt(imageBase64) {
    const apiKey = await this.getApiKey();
    if (!apiKey) {
      throw new Error('Chưa cấu hình API Key. Vui lòng nhập Gemini API Key trong tab Trợ lý AI hoặc Cài đặt.');
    }

    const model = await this.getSelectedModel();
    const url = `https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent?key=${apiKey}`;

    const prompt = `Bạn là hệ thống OCR chuyên bóc tách hóa đơn ăn uống, nhà hàng, mua sắm, siêu thị, vé xe, bill chuyển khoản ngân hàng Việt Nam.
Hãy đọc hình ảnh hóa đơn này và trả về DUY NHẤT một chuỗi JSON hợp lệ với cấu trúc sau:
{
  "storeName": "Tên quán / Cửa hàng / Bên nhận (hoặc để trống nếu không rõ)",
  "totalAmount": 150000,
  "category": "Ăn uống",
  "items": [
    {
      "name": "Tên món/sản phẩm",
      "quantity": 1,
      "amount": 45000
    }
  ]
}

Quy tắc nghiêm ngặt:
- "storeName": Tên thương hiệu/quán/siêu thị/người nhận tiền nếu nhận diện được.
- "totalAmount": Tổng số tiền thanh toán cuối cùng (number số nguyên VND, ví dụ 150000; KHÔNG kèm chữ 'đ', 'k', dấu chấm hay phẩy).
- "category": Phân loại phù hợp nhất: Ăn uống, Mua sắm, Di chuyển, Hóa đơn, Giải trí, Khác.
- "items": Danh sách các món ăn hoặc hàng hóa chi tiết (nếu hóa đơn liệt kê từng món). Nếu là bill chuyển khoản hoặc hóa đơn tổng không có danh sách món, "items" có thể rỗng hoặc chứa 1 dòng ghi nhận tổng tiền.
- Tuyệt đối chỉ trả về JSON, không kèm lời dẫn markdown nào ngoài JSON.`;

    const mimeType = imageBase64.startsWith('data:image/png') ? 'image/png' : 'image/jpeg';
    const cleanBase64 = imageBase64.replace(/^data:image\/\w+;base64,/, '');

    const requestBody = {
      contents: [
        {
          role: 'user',
          parts: [
            {
              inlineData: {
                mimeType: mimeType,
                data: cleanBase64
              }
            },
            {
              text: prompt
            }
          ]
        }
      ],
      generationConfig: {
        temperature: 0.1,
        responseMimeType: 'application/json'
      }
    };

    const response = await fetch(url, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(requestBody)
    });

    if (!response.ok) {
      const errData = await response.json().catch(() => ({}));
      const msg = errData?.error?.message || response.statusText;
      throw new Error(`Lỗi nhận diện hóa đơn (${response.status}): ${msg}`);
    }

    const data = await response.json();
    const rawText = data.candidates?.[0]?.content?.parts?.map((p) => p.text).join('') || '{}';
    return this.parseScannedReceipt(rawText);
  },

  parseScannedReceipt(jsonString) {
    try {
      const cleaned = jsonString.replace(/```json/g, '').replace(/```/g, '').trim();
      const parsed = JSON.parse(cleaned);

      let totalAmount = parsed.totalAmount || parsed.total || parsed.tong_tien || 0;
      if (typeof totalAmount === 'string') {
        totalAmount = parseMoneyInput(totalAmount);
      }

      const storeName = (parsed.storeName || parsed.merchant || parsed.store || parsed.shop || '').trim();
      const category = (parsed.category || parsed.danh_muc || 'Ăn uống').trim();

      const list = parsed.items || parsed.transactions || parsed.dishes || (Array.isArray(parsed) ? parsed : []);
      const items = [];

      list.forEach((raw) => {
        const name = (raw.name || raw.title || raw.item || raw.mon || '').trim();
        const quantity = parseInt(raw.quantity || raw.so_luong || 1, 10) || 1;
        let amount = raw.amount || raw.price || raw.gia || raw.thanh_tien || 0;

        if (typeof amount === 'string') {
          amount = parseMoneyInput(amount);
        }

        if (name && amount > 0) {
          items.push({
            id: 'item_' + Math.random().toString(36).substr(2, 9),
            name,
            quantity,
            amount: Number(amount)
          });
        }
      });

      // Nếu không có totalAmount nhưng có items, tính tổng từ items
      if (!totalAmount && items.length > 0) {
        totalAmount = items.reduce((sum, it) => sum + it.amount, 0);
      }

      return {
        storeName,
        totalAmount: Number(totalAmount) || 0,
        category,
        items
      };
    } catch (e) {
      console.error('Lỗi phân tích JSON hóa đơn:', e, jsonString);
      return {
        storeName: '',
        totalAmount: 0,
        category: 'Ăn uống',
        items: []
      };
    }
  }
};

/**
 * Xử lý chuỗi gõ tắt tiền tệ Việt Nam (50k -> 50,000, 1.5tr -> 1,500,000)
 */
function parseMoneyInput(text) {
  if (!text) return 0;
  const str = text.toString().toLowerCase().trim().replace(/đ|vnd/g, '').replace(/,/g, '.');
  
  if (str.endsWith('tr') || str.endsWith('m')) {
    const num = parseFloat(str.replace(/tr|m/g, ''));
    return isNaN(num) ? 0 : Math.round(num * 1000000);
  }
  
  if (str.endsWith('k')) {
    const num = parseFloat(str.replace(/k/g, ''));
    return isNaN(num) ? 0 : Math.round(num * 1000);
  }

  // Số thường, bỏ dấu chấm phân tách hàng nghìn
  const cleanNum = str.replace(/\./g, '');
  const parsed = parseFloat(cleanNum);
  return isNaN(parsed) ? 0 : Math.round(parsed);
}

function formatMoney(amount) {
  const num = Math.round(Number(amount) || 0);
  return new Intl.NumberFormat('vi-VN').format(num) + ' ₫';
}
