# Phase 03: Compose UI (Easter Egg)
Status: ✅ Complete
Dependencies: Phase 02

## Objective
Lồng ghép thao tác "Gõ 7 lần" vào Giao diện `SettingsScreen.kt` để kích hoạt Nút Giấu Kín "God Mode". Xử lý liên kết Nút này tới ShizukuHelper.

## Requirements
### Functional
- [x] Tạo biến state đếm số click: `var tapCount by remember { mutableStateOf(0) }`.
- [x] Click Modifier trên chuỗi text tiêu đề "Quyền & Khắc Phục Lỗi".
- [x] Nếu đếm == 7, chuyển biến `showGodMode = true`.
- [x] Nút God Mode chỉ hiện trên màn khi boolean đó True. Bấm vào thì chạy hàm của ShizukuHelper.

## Implementation Steps
1. [x] Sửa file `SettingsScreen.kt`.
2. [x] Gắn `Modifier.clickable` trên header Card thứ 4 (Permissions).
3. [x] Cài đặt hiệu ứng Toast chúc mừng khi unlock ("Đã bật chế độ God Mode!").
4. [x] Khởi tạo Component Button xịn xò màu vàng/cam thể hiện chức năng nguy hiểm nâng cao.

## Files to Create/Modify
- `app/src/main/java/com/offlinenotifier/app/ui/SettingsScreen.kt` - Thêm logic UI ẩn và nút bấm

## Test Criteria
- [x] Gõ Header ít hơn 7 lần -> Không hiện nút.
- [x] Gõ đúng 7 lần -> Nút God Mode bật ra.
- [x] Sau khi thoát màn Settings và vào lại -> Trạng thái God Mode ẩn đi bảo mật.

---
Next Phase: phase-04-testing.md
