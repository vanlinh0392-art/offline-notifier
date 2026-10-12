# Design Specifications: Offline Notifier

## 🎨 Vibe & Concept
**"Stealth & Reliable"**
Vì đây là một ứng dụng chạy ngầm, giao diện cần toát lên sự tin cậy, an toàn và tiết kiệm pin. Chúng ta sẽ sử dụng phong cách **Dark Mode (Nền tối)** chủ đạo, các nút bấm to rõ ràng, không có chi tiết thừa. 

## 🎨 Color Palette
| Name | Hex | Usage |
|------|-----|-------|
| Background | `#121212` | Màu nền chính giúp tiết kiệm pin (OLED Dark). |
| Surface | `#1E1E1E` | Màu nền của các thẻ (Cards), Dialogs. |
| Primary | `#03DAC6` | Màu nhấn (Teal). Dùng cho các icon Trạng thái, viền nút. |
| Accent/Danger| `#CF6679` | Màu đỏ nhạt cảnh báo. Dùng cho Nút Tắt (STOP), báo lỗi. |
| Text Main | `#FFFFFF` | Chữ chính (Tiêu đề, trạng thái). |
| Text Muted | `#A0A0A0` | Chữ phụ (Mô tả, ngày giờ trong Log). |
| Google Blue | `#4285F4` | Màu chuẩn của nút Login Google. |

## 📝 Typography
- **Font chữ:** Roboto (Chuẩn Android) hoặc Inter (Nếu custom).
- **H1 (Status Text):** 32sp, Bold (VD: "ĐANG BẢO VỆ")
- **H2 (Card Title):** 20sp, Medium (VD: "Lịch sử báo cáo")
- **Body:** 14sp, Regular (Nội dung log, chú thích)

## 📐 Layout & Màn hình chính

### 1. Màn hình Dashboard (Trang Chủ)
Đây là nơi người dùng nhìn thấy đầu tiên. Nó phải cực kỳ đơn giản:
- **Phần trên cùng (Header):**
  - Trạng thái đăng nhập: Nút [Đăng nhập bằng Google] hoặc hiển thị Avatar/Email nếu đã đăng nhập.
- **Phần giữa (The Big Switch):**
  - Card lớn chứa Dòng chữ trạng thái (VD: "Ứng dụng đang NGỦ" hoặc "ĐANG GIÁM SÁT").
  - Bên dưới là một Nút bấm **khổng lồ** hình vuông bo góc hoặc hình tròn: **START** (Màu Primary) / **STOP** (Màu Danger).
- **Phần dưới (Bottom Navigation/Tabs):**
  - Mở các trang con: [Cài đặt (Settings)] - [Lịch sử (Logs)].

### 2. Màn hình Log (Lịch sử gửi)
Dạng danh sách (List View / RecyclerView).
- **Empty State (Khi chưa có log):** Hình minh họa một hộp thư trống với chữ "Chưa có báo cáo nào được gửi".
- **Item trong danh sách:**
  - Card bo góc nhẹ (8dp).
  - Trái: Icon trạng thái (Check mark xanh nếu SUCCESS, X đỏ nếu FAILED).
  - Giữa: Thời gian gửi (Ví dụ: 20:30 - 02/03/2026).
  - Dưới: Ghi chú ("Đã tổng hợp 5 sự kiện").

### 3. Màn hình Cài đặt (Settings) - Bản gộp ngọn (v1.6)
Để tránh nhồi nhét quá nhiều thẻ (Card) dọc màn hình gây rối mắt, giao diện Cài đặt sẽ được Refactor sang cấu trúc **Danh mục thu gọn (Accordion / Expandable Sections)**:
- **Phần 1: Trạng thái và Hàng chờ:** Card trên cùng to nhất, chứa thông tin Đang Giám Sát, số sự kiện, Thùng rác xóa hàng chờ + Nút Test Gửi Ngay.
- **Phần 2: 🎯 Cấu Hình Lọc Sự Kiện (Collapsible):** Bấm vào xổ xuống chọn Cuộc gọi, SMS, Thông báo App (Có thanh tìm kiếm).
- **Phần 3: 📧 Cấu Hình Email & Chu Kỳ (Collapsible):** Chứa tài khoản Google gửi, Email nhận, App password, Chu kỳ gửi.
- **Phần 4: 🔐 Cấp Quyền & Khắc Phục Lỗi (Collapsible):** Các nút cấp quyền, Copy lệnh ADB, Hướng dẫn Xiaomi.
Mọi phần xổ xuống đều có icon nhận diện rõ ràng, giúp màn hình chỉ còn 4 khối ngắn gọn khi vừa mở app.

## 🔲 Shape & Component Specs
- **Cards & Dialogs:** Bo góc (Corner Radius) 16dp.
- **Buttons (Nút bấm):** Bo góc tròn hoàn toàn (Circle/Pill) hoặc góc bo lớn 24dp. Nút Bật/Tắt chính có viền sáng (Glow effect) khi đang `ACTIVE`.
- **Shadows:** Dùng ít shadow vì thiết kế mảng Dark Mode chủ yếu dùng sự chênh lệch độ xám (Elevation: Surface #1E1E1E đè lên Background #121212).

## ✨ Component: Google Login Button
- Màu nền trắng `#FFFFFF` (hoặc `#4285F4` tùy theo chuẩn Google Branding).
- Logo G bên trái.
- Padding: 12dp Vertical, 24dp Horizontal.
- Corner Radius: 8dp.
