# Ý tưởng Dự án: Offline Notifier (Trợ lý thông báo vắng mặt)

## Yêu cầu cốt lõi đã ghi nhận:
- **Chức năng chính:** Gửi các thông tin về cuộc gọi lỡ, tin nhắn, thông báo ứng dụng trong thời gian không sử dụng điện thoại đến 1 email được cài đặt sẵn (có công tắc bật/tắt chức năng).
- **Cấp quyền tối đa:** Yêu cầu các quyền cảnh báo sâu (kể cả phải cấp phát qua ADB) như: `WRITE_SECURE_SETTINGS`, `READ_LOGS`, `Accessibility` (đọc nội dung màn hình), `Notification Listener`...
- **Tùy chỉnh thời gian:** Có Settings cấu hình khoảng thời gian/chu kỳ bao lâu thì check thông tin và gửi 1 lần.
- **Cơ chế Retry (Hàng đợi):** Có cơ chế lưu tạm và gửi lại khi mạng yếu/mất mạng để không bỏ sót thông báo.
- **Tính năng bổ sung (dự kiến):** Cơ chế "Keep Alive" chống bị hệ điều hành (Android) kill app ngầm, mã hóa dữ liệu trước khi gửi, v.v.
- **Yêu cầu kỹ thuật:** Cần viết chi tiết kế hoạch cùng các kỹ thuật sử dụng (sẽ được giải quyết ở bước `/plan`).
