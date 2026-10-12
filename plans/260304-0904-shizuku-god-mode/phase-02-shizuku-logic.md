# Phase 02: Shizuku Scripting
Status: ✅ Complete
Dependencies: Phase 01

## Objective
Xây dựng một lớp Helper/Manager chịu trách nhiệm xin quyền User Shizuku và phóng các lệnh Bash tự cấp quyền nếu xin quyền thành công.

## Requirements
### Functional
- [x] Check xem Client Shizuku có đang Bind không.
- [x] Xin quyền của Shizuku (nếu Android 11+ là động, dưới thì check tĩnh).
- [x] Thực thi tập lệnh Bash `pm grant com.offlinenotifier.app android.permission.xxx`...

## Implementation Steps
1. [x] Tạo file `ShizukuHelper.kt` trong package `com.offlinenotifier.app.utils`.
2. [x] Viết hàm `checkShizukuStatus(): Boolean`.
3. [x] Viết hàm `grantPermissionsViaShizuku()` thực thi list quyền:
    - READ_CALL_LOG
    - RECEIVE_SMS
    - READ_SMS
    - WRITE_SECURE_SETTINGS
    - `appops set com.offlinenotifier.app WRITE_SETTINGS allow`
    - `dumpsys deviceidle whitelist +com.offlinenotifier.app`
4. [ ] Bắt try/catch Exception khi Shizuku không được chạy.

## Files to Create/Modify
- `app/src/main/java/com/offlinenotifier/app/utils/ShizukuHelper.kt` - Logic xử lý chính.

## Test Criteria
- [x] Nếu Shizuku Service đang chạy, lệnh log in ra thực thi thành công 0 lỗi.

---
Next Phase: phase-03-compose-ui.md
