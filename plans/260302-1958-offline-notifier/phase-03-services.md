# Phase 03: Services (Listeners & Catchers)
Status: ⬜ Pending

## Objective
Xây dựng Foreground Service chính để giữ ứng dụng tồn tại, đồng thời đăng ký BroadcastReceiver nhận trạng thái màn hình (SCREEN_ON / SCREEN_OFF). Xây dựng NotificationListenerService và SMS/Call Receiver.

## Requirements
- `OfflineNotifierService` (Foreground Service) hiển thị thông báo "đang chạy ngầm" theo yêu cầu Android 14+.
- Đăng ký nhận `SCREEN_OFF` và `SCREEN_ON` trong Foreground Service.
- `NotificationCatcherService`: Trích xuất được Package Name (app nào gửi), Title (Người gửi) và Text (Nội dung) của thông báo. Chỉ lưu vào Room DB nếu màn hình đang `SCREEN_OFF`.
- `CallAndSmsReceiver`: Bắt SMS và Cuộc gọi lỡ, cũng chỉ lưu vào DB nếu màn hình đang `SCREEN_OFF`.

## Implementation Steps
1. [ ] Cấu hình Notification Channel cho Foreground Service.
2. [ ] Khởi tạo `OfflineNotifierService.kt` kèm BroadcastReceiver nội bộ lắng nghe màn hình.
3. [ ] Khởi tạo `NotificationCatcherService.kt` kế thừa `NotificationListenerService`.
4. [ ] Khởi tạo `CallAndSmsReceiver.kt` kế thừa `BroadcastReceiver`.
5. [ ] Tích hợp ghi vào Room DB ở tất cả các lớp trên (sử dụng Coroutine).

## Test Criteria
- Ứng dụng xuất hiện trên thanh trạng thái với chữ "Đang chạy ngầm".
- Tắt màn hình đi, dùng máy khác gửi SMS và Logcat In ra là vừa Insert vào DB thành công.
