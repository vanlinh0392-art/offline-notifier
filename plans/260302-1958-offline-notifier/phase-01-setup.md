# Phase 01: Setup Environment & UI Core
Status: ⬜ Pending
Dependencies: None

## Objective
Khởi tạo dự án Android cơ bản, tạo luồng UI Jetpack Compose cho màn hình Settings (Email, Khoảng thời gian gửi, Switch bật/tắt chức năng). Màn hình cần cung cấp hướng dẫn (và copy/paste lệnh ADB) cho người dùng dễ dàng phân quyền.

## Requirements
### Functional
- [ ] Giao diện (Màn hình chính):
  - Nhập Email đích.
  - Chọn thời gian tự động báo cáo (vd: 15p, 30p, 60p).
  - Công tắc (Switch) On/Off tổng.
  - Phím gọi Intent mở phần cấp quyền "Notification Access" và "Usage Data Access" (Để biết khi màn hình khóa).
- [ ] Logic lưu Preference: Lưu Email, Thời gian, Trạng thái Switch vào `DataStore` (hoặc `SharedPreferences`).

### Non-Functional
- [ ] UI mượt mà, dùng Material Design 3. Thân thiện cả nền tối (Dark mode).
- [ ] Giao diện có mục "ADB Permissions Setup" hiển thị mã lệnh để người dùng nhấn Copy gửi vào PC.

## Implementation Steps
1. [ ] Khởi tạo Android Project cơ bản (Empty Activity).
2. [ ] Thêm các thư viện cần thiết (Jetpack Compose, DataStore, Navigation) vào `build.gradle.kts`.
3. [ ] Viết màn hình Compose chứa: Form Email, Dropdown thời gian, Switch, thẻ "Permision Status".
4. [ ] Tạo DataStore Repository để lưu cấu hình này liên tục.

## Test Criteria
- [ ] Giao diện hiển thị đúng trên máy giả lập/thật.
- [ ] Thử nhập Email, bật Switch, đóng app mở lại vẫn lưu trạng thái đó.
- [ ] Ấn nút Copy thì lệnh quyền ADB phải vào Clipboard.

---
Next Phase: `phase-02-database.md` (Room)
