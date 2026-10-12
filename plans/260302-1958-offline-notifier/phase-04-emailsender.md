# Phase 04: Background Worker & Email Sender
Status: ⬜ Pending

## Objective
Xây dựng luồng định kỳ kiểm tra database (EventEntity) bằng `WorkManager`. Nếu có events mới và thiết bị đang có mạng Internet, ứng dụng tiến hành định dạng thành mã HTML dễ nhìn và gửi qua e-mail cấu hình sẵn bằng `JavaMail API`.

## Requirements
- `EmailSenderWorker`: Lên lịch bởi WorkManager. Chỉ chạy khi có NETWORK_CONNECTED.
- `SmtpClient`: Đóng gói logic gửi email qua cổng `smtp.gmail.com` (Yêu cầu Mật khẩu Ứng dụng).

## Implementation Steps
1. [ ] Khởi tạo class `SmtpClient.kt` chứa cơ chế SendEmail() BlockingCall.
2. [ ] Khởi tạo `EmailSenderWorker.kt` (Kế thừa CoroutineWorker).
3. [ ] Trong Worker: lấy DataStore Email + Pass -> Lọc lấy List<EventEntity> -> Gọi SmtpClient -> Gửi xong thì Delete từ Database.
4. [ ] Tạo file tiện ích `WorkManagerHelper.kt` hỗ trợ lên lịch worker theo chu kỳ do user chọn (1phut, 15p, 30p, 60p).

## Test Criteria
- Compile Code thành công bộ JavaMail API.
- Viết 1 logic test nút "Test Gửi Mail Ngay Lập Tức" bên UI để thẩm định mật khẩu cấu hình đúng.
