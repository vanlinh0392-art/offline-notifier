# Phase 02: Local Database (Room)
Status: ⬜ Pending

## Objective
Khởi tạo Room Database để lưu trữ sự kiện Offline (những lúc điện thoại khóa màn hình và không có mạng/chưa đến giờ gửi báo cáo).

## Requirements
- Bảng `EventEntity` (id, type, title, body, timestamp).
- Các truy vấn DAO: `insertEvent`, `getAllEvents`, `deleteEvents`.
- `AppDatabase` khai báo version 1.

## Implementation Steps
1. [ ] Khởi tạo thư mục `data/database`.
2. [ ] Tạo Data Class `EventEntity`.
3. [ ] Tạo Interface `EventDao`.
4. [ ] Tạo Abstract Class `AppDatabase`.

## Test Criteria
- Compile thành công bộ xử lý Room KSP.
