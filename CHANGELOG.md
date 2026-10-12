# Changelog

Tất cả những thay đổi đáng kể đối với dự án này sẽ được ghi chép trong tệp tin này.

## [1.13.0] - 2026-03-03
### Added
- Bổ sung lệnh `appops set` vào script `grant_permissions.cmd` để cấp thẳng quyền Sửa đổi cài đặt hệ thống (WRITE_SETTINGS) qua ADB không cần chạm màn hình (bypass Xiaomi HyperOS).
- Khai báo thêm quyền `WRITE_SECURE_SETTINGS` vào Manifest để hỗ trợ cấp quyền shell tự động.
- Tool grant_permissions cập nhật UI in Output Report xem kết quả Verify.

### Changed
- Cải thiện thuật toán Track-Screen: Chuyển biến lưu thời gian tắt màn hình `lastScreenOffTime` sang companion object của `OfflineNotifierService` để sửa lỗi vòng lặp logic bị Reset (Do Receiver bị clone liên tục).
- Tối ưu lượng hao pin 300%: Giãn chu kỳ quét sự kiện từ mỗi 10 giây lên 30 giây trong Background Service.
- UI Settings: Fix lỗi nhận diện ảo bằng việc check quyền WRITE_SETTINGS thông qua hàm `Settings.System.canWrite(context)` thay vì kiểm tra Permission thông thường.

### Fixed
- Lỗi bị tràn event lúc sáng màn hình: SMS/Call Receiver giờ ngó lơ event khi màn hình sáng hoặc vừa tắt dưới 5 giây.
- Lỗi không xoá hàng đợi: Cấp ngược tham số `isTest = false` khi trigger auto-send để Worker tự tiêu hủy DB hàng đợi sau khi check email.
- Trống memory leak Coroutine: Cleanup scope bằng `cancel()` tại hàm `onDestroy` của Service chính.

---

## [1.6.0] - 2026-03-03
### Added
- Thêm Nút Clear Data (Xóa lịch sử) thẳng từ Menu `LogHistoryScreen`.
- Tool batch command `grant_permissions.cmd` (Hỗ trợ USB/Wi-Fi tùy IP) cho user cấp quyền ADB.

### Changed
- Refactored `SettingsScreen.kt` thành UI Card Accordion (Collapse Headers). Giữ ứng dụng gọn gàng trên 1 trang 4 mục chính.
- Mặc định khởi chạy hiển thị ở chế độ Dark Mode.
- Phiên bản ứng dụng nhảy lên v1.6.

### Fixed
- Lọc ứng dụng thông báo chỉ cho user thấy các app đã tải (Bỏ qua system apps).
- Khắc phục sự cố push ADB báo lỗi 2 devices khi máy cắm sạc + bật Debug Wi-Fi. Cụ thể định danh device id = `205a60f2`.
- Cảnh báo người dùng khi Permissions `WRITE_SETTINGS` bị Security Exception Managed. Quản lý UI không bắt buộc PM qua shell.

---

## [1.5.0] - 2026-03-03
### Added
- Hoàn thiện Workflow Room DB và Giao diện gửi `Log History`.
- Tích hợp WorkManager lặp theo chu kì User tùy chỉnh.
- Xử lý Catch SMS, Notification, Missed Calls trơn tru qua BroadcastReceiver.
