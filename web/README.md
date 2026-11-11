# Expense Manager - Progressive Web App (PWA)

Phiên bản Web PWA của ứng dụng Quản Lý Chi Tiêu & Chia Tiền AI Gemini, tối ưu cho iPhone (iOS), iPad, Android và trình duyệt máy tính.

## 🚀 Hướng dẫn Triển khai (Deploy) trong 2 phút

### Cách 1: Triển khai miễn phí qua Vercel (Khuyên dùng)
1. Đăng nhập vào [Vercel](https://vercel.com) bằng tài khoản GitHub của bạn.
2. Bấm **"Add New..."** -> **"Project"**.
3. Chọn repository `ExpenseManager`.
4. Trong phần **Root Directory**, bấm **Edit** và chọn thư mục `web`.
5. Bấm **"Deploy"**. Vercel sẽ cấp cho bạn một đường link HTTPS miễn phí (ví dụ: `https://expense-manager-xxx.vercel.app`).

### Cách 2: Triển khai qua GitHub Pages
1. Vào repository `ExpenseManager` trên GitHub.
2. Vào **Settings** -> **Pages**.
3. Tại mục **Build and deployment** -> Chọn Source là **Deploy from a branch**.
4. Chọn Branch `main` và thư mục `/web` (nếu GitHub hỗ trợ) hoặc dùng GitHub Actions workflow.

### Cách 3: Chạy thử nghiệm trên máy tính
Mở PowerShell tại thư mục `web/` và chạy:
```bash
python -m http.server 8080
```
Sau đó mở trình duyệt truy cập: `http://localhost:8080`

---

## 📱 Cách Cài đặt lên iPhone (iOS)
1. Mở trình duyệt **Safari** trên iPhone và truy cập vào link web đã deploy.
2. Bấm vào nút **Chia sẻ** (biểu tượng hình vuông có mũi tên đi lên ở thanh công cụ dưới cùng Safari).
3. Cuộn xuống và chọn **"Thêm vào Màn hình chính" (Add to Home Screen)**.
4. Bấm **"Thêm" (Add)** ở góc trên bên phải.
5. Ứng dụng sẽ xuất hiện với icon riêng trên màn hình chính, mở lên chạy toàn màn hình (Standalone) không có thanh URL, hoạt động mượt mà và lưu dữ liệu offline 100%.
