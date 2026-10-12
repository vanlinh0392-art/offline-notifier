# Phase 05: Integration & Settings Sync
Status: ⬜ Pending

## Objective
Gắn các mảnh ghép từ Phase 1 tới Phase 4 lại thành luồng (Flow) hoàn chỉnh: Bật Switch -> Kích hoạt Service -> Lên lịch Worker. Tắt Switch -> Dừng mọi thứ.

## Requirements
- `SettingsViewModel` gọi Intent start/stop `OfflineNotifierService`.
- `SettingsViewModel` gọi `WorkManagerHelper` schedule/cancel work.

## Implementation Steps
1. [ ] Cập nhật `SettingsViewModel` có logic nhận Context hoặc Event Channel để kích hoạt Service bên Android Components.
2. [ ] Thêm check điều kiện: khi bấm `Bật`, nếu quyền Notify & Usage chưa có thì báo lỗi UI thay vì chạy luôn.

## Test Criteria
- Compile dự án.
- Bấm switch thấy Service khởi chạy và thông báo hiện lên thanh System Tray.
