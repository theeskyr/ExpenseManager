# 📱 Quản Lý Chi Tiêu (Expense Manager) - Android Native App

Ứng dụng quản lý thu chi cá nhân hiện đại dành riêng cho điện thoại Android, được phát triển bằng **Kotlin** với bộ giao diện **Jetpack Compose (Material 3)** và cơ sở dữ liệu **Room (SQLite Offline)** đảm bảo 100% dữ liệu riêng tư, không cần mạng Internet.

---

## 🌟 Các Tính Năng Nổi Bật

1. **Ghi Chép Thu / Chi Nhanh Chóng**:
   - Chuyển đổi linh hoạt giữa *Khoản chi* và *Khoản thu*.
   - Phân loại theo danh mục trực quan có biểu tượng và màu sắc (Ăn uống, Đi lại, Mua sắm, Hóa đơn, Lương, Thưởng, Đầu tư...).
   - Nhập số tiền kèm phím tắt nhanh (`+50k`, `+100k`, `+200k`, `+500k`) và định dạng tiền tệ Việt Nam Đồng (`₫`).
   - Thêm ghi chú chi tiết và ngày giờ giao dịch.

2. **Quản Lý Nhiều Ví & Nguồn Tiền**:
   - Quản lý đồng thời: Tiền mặt, Tài khoản ngân hàng (Vietcombank, MB, Techcombank...), Ví điện tử (MoMo, ZaloPay), Thẻ tín dụng.
   - Tự động cộng/trừ số dư tức thì khi thêm hoặc xóa giao dịch.
   - Thống kê tổng tài sản ròng thực tế.

3. **Báo Cáo & Thống Kê Trực Quan (Biểu Đồ)**:
   - Lọc báo cáo theo: **Tuần này**, **Tháng này**, **Năm này**.
   - **Biểu đồ tròn (Donut Chart)** trực quan hóa tỷ lệ phần trăm phân bổ chi tiêu giữa các danh mục.
   - Xếp hạng danh mục chi tiêu nhiều nhất kèm tiến độ %.
   - So sánh trực quan giữa Tổng chi vs Tổng thu.

4. **Quản Lý Hạn Mức Ngân Sách (Budget Planning)**:
   - Đặt ngân sách chi tiêu tối đa cho từng tháng.
   - Thanh tiến độ cảnh báo đổi màu theo thời gian thực:
     - 🟢 **Xanh lá**: Dưới 80% ngân sách (an toàn).
     - 🟠 **Vàng cam**: Đạt 80% - 100% ngân sách (cảnh báo).
     - 🔴 **Đỏ**: Vượt quá 100% hạn mức (báo động).
   - **Gợi ý chi tiêu thông minh**: Tính toán tự động số tiền tối đa bạn được phép chi mỗi ngày cho những ngày còn lại của tháng.

5. **Xuất & Sao Lưu Dữ Liệu (Excel / CSV)**:
   - Xuất toàn bộ lịch sử giao dịch ra file `.csv` chuẩn **UTF-8 BOM** (mở trực tiếp bằng Microsoft Excel hoặc Google Sheets trên điện thoại mà không bị lỗi font tiếng Việt).
   - Chia sẻ báo cáo nhanh qua Zalo, Gmail, Drive hoặc lưu vào bộ nhớ máy.

6. **100% Offline & Bảo Mật Tuyệt Đối**:
   - Dữ liệu lưu cục bộ trong máy, không cần tạo tài khoản hay đăng nhập.

---

## 🏗️ Cấu Trúc Dự Án (Architecture)

```text
ExpenseManager/
├── .github/workflows/build-apk.yml    # Tự động build file APK trên GitHub Actions
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml
│   │   ├── java/com/expensetracker/app/
│   │   │   ├── ExpenseTrackerApp.kt   # Application khởi tạo Database & Repository
│   │   │   ├── MainActivity.kt        # Compose Navigation & BottomBar
│   │   │   ├── data/
│   │   │   │   ├── local/             # Room Database, DAO, Entity
│   │   │   │   └── repository/        # Repository quản lý dữ liệu
│   │   │   ├── ui/
│   │   │   │   ├── theme/             # Material 3 Color, Theme, Typography
│   │   │   │   ├── components/        # Charts (Donut Pie), Item card, Icon mapper
│   │   │   │   ├── screens/           # Home, Add, Stats, Budget, Accounts, Settings
│   │   │   │   └── viewmodel/         # ExpenseViewModel & Factory
│   │   │   └── utils/                 # Format tiền tệ, ngày giờ, xuất CSV
│   │   └── res/                       # Resource icons, values, paths
│   └── build.gradle.kts
├── gradle/wrapper/                    # Gradle Wrapper 8.4
├── build.gradle.kts
└── settings.gradle.kts
```

---

## 🚀 Hướng Dẫn Biên Dịch & Cài Đặt File APK

Bạn có thể cài đặt ứng dụng vào điện thoại Android theo 1 trong các cách sau:

### Cách 1: Tự Động Build File APK Qua GitHub (Khuyên Dùng - Nhanh Nhất, Không Cần Cài Android Studio)

Dự án đã được tích hợp sẵn file cấu hình **GitHub Actions** (`.github/workflows/build-apk.yml`):
1. Đẩy thư mục mã nguồn này lên một kho lưu trữ GitHub của bạn (Repository):
   ```bash
   git init
   git add .
   git commit -m "Initial Expense Manager App"
   git branch -M main
   git remote add origin https://github.com/<tai-khoan-cua-ban>/<ten-repo>.git
   git push -u origin main
   ```
2. Mở repository trên trình duyệt, vào tab **Actions**.
3. Bạn sẽ thấy luồng **Build Android APK** tự động chạy trong khoảng 2 phút.
4. Khi chạy xong (dấu tích xanh), bấm vào lần chạy đó và tải file **`ExpenseManager-Debug-APK`** về máy.
5. Gửi file APK vào điện thoại Android qua Zalo / Google Drive / Cáp USB và bấm cài đặt!

---

### Cách 2: Mở Bằng Android Studio Trên Máy Tính

1. Mở phần mềm **Android Studio**.
2. Chọn **Open** và trỏ đến thư mục: `D:\TestSpace\ExpenseManager`.
3. Đợi Android Studio đồng bộ xong (Sync Project with Gradle Files).
4. **Cài đặt vào điện thoại**:
   - Bật chế độ **Tùy chọn cho nhà phát triển** (Developer Options) và **Gỡ lỗi qua USB** (USB Debugging) trên điện thoại Android của bạn.
   - Cắm cáp USB nối điện thoại với máy tính.
   - Trên thanh công cụ Android Studio, chọn thiết bị điện thoại của bạn và bấm nút **Run (Tam giác xanh)** ▶️. App sẽ tự động cài và mở ngay trên màn hình điện thoại!
5. **Hoặc xuất file APK**:
   - Vào menu: **Build** -> **Build Bundle(s) / APK(s)** -> **Build APK(s)**.
   - File APK sẽ được tạo tại: `app/build/outputs/apk/debug/app-debug.apk`.

---

### 📲 Lưu Ý Khi Cài Đặt File APK Trên Điện Thoại Android

Khi bạn bấm cài đặt file `.apk` tải về, điện thoại Android sẽ hỏi bảo mật:
1. Bấm **"Cho phép cài đặt từ nguồn này"** (Allow from this source / Cài đặt ứng dụng không xác định).
2. Nếu Play Protect hiển thị cảnh báo "Ứng dụng chưa được xác minh", chọn **"Vẫn cài đặt" (Install anyway)**.
3. Mở ứng dụng và bắt đầu ghi chép chi tiêu!
