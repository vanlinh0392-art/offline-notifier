# Phase 01: Setup Environment
Status: ✅ Complete
Dependencies: None

## Objective
Khởi tạo và cài đặt các thư viện cần thiết vào dự án để giao tiếp với Shizuku API Provider, bao gồm cả việc cấu hình AndroidManifest.

## Requirements
### Functional
- [x] Thêm thư viện `dev.rikka.shizuku:api:13.1.5` (hoặc bản mới nhất) vào `build.gradle.kts`.
- [x] Thư viện `dev.rikka.shizuku:provider:13.1.5`.

### Non-Functional
- [x] Không làm phình to size file APK quá nhiều.

## Implementation Steps
1. [x] Thêm dependencies implementation `dev.rikka.shizuku:api:x.x.x` vào `app/build.gradle.kts`.
2. [x] Sync Gradle.
3. [x] Khai báo `android:name="moe.shizuku.manager.permission.API_V23"` trong `AndroidManifest.xml` section `uses-permission`.

## Files to Create/Modify
- `app/build.gradle.kts` - Cài thư viện
- `app/src/main/AndroidManifest.xml` - Khai báo quyền xin Shizuku.

## Test Criteria
- [x] Gradle build thành công, không văng lỗi version dependency xung đột.

---
Next Phase: phase-02-shizuku-logic.md
