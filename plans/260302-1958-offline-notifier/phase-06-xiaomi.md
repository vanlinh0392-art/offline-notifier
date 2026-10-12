# Phase 06: Deep HyperOS/Xiaomi Optimizations
Status: ⬜ Pending

## Objective
HyperOS/MIUI rất hung hãn trong việc kill app nền. Mặc dù đã dùng Foreground Service và cấp quyền qua ADB `dumpsys deviceidle whitelist`, Xiaomi vẫn còn 2 lớp bảo vệ pin riêng: AutoStart (Tự khởi chạy) và Battery Saver (Tiết kiệm pin ứng dụng).

Chúng ta cần Code hiển thị Popup khi bật Switch để hướng dẫn user tắt 2 lớp này.

## Requirements
1. Dùng Intent mở trực tiếp màn hình `AutoStart` của Xiaomi (nếu có thể).
2. Dùng Intent mở màn hình `Battery Optimization` riêng lẻ của App.

## Implementation Steps
1. [ ] Cập nhật `SettingsScreen.kt` thêm 1 thẻ cảnh báo Xiaomi.
2. [ ] Thêm Button: "Tắt Tiết Kiệm Pin Xiaomi" gọi Intent mở Permission cho App.
3. [ ] Hoàn thành dự án và chuẩn bị review.

## Test Criteria
- Trên máy tính, user thấy màn hình hiển thị đủ cảnh báo. Do AI không tự click được trên máy bạn, đây là bước bạn phải kiểm tra tay.
