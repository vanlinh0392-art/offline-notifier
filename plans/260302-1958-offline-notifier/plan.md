# Plan: Offline Notifier App
Created: 2026-03-02T19:58:26+07:00
Status: 🟡 In Progress

## Overview
Ứng dụng theo dõi cuộc gọi lỡ, tin nhắn và thông báo khi điện thoại (Xiaomi 14, Android 16) đang khóa màn hình. Sau một chu kỳ thời gian tự chọn, app sẽ gom dữ liệu và gửi qua email. Có cơ chế lưu trữ offline khi rớt mạng và yêu cầu cấp quyền đặc quyền qua ADB (do không Root).

## Tech Stack
- Platform: Android (Kotlin)
- UI: Jetpack Compose
- Architecture: MVVM
- Background: Foreground Service & WorkManager
- Storage: Room Database
- Email: JavaMail API (App Password)

## Phases

| Phase | Name | Status | Progress |
|-------|------|--------|----------|
| 01 | Setup Environment & UI Core | ⬜ Pending | 0% |
| 02 | Local Database (Room) | ⬜ Pending | 0% |
| 03 | Services (Listeners & Catchers) | ⬜ Pending | 0% |
| 04 | Background Worker & Email Sender | ⬜ Pending | 0% |
| 05 | Integration & Settings Sync | ⬜ Pending | 0% |
| 06 | Deep HyperOS/Xiaomi Optimizations | ⬜ Pending | 0% |

## Quick Commands
- Start Phase 1: `/code phase-01`
- Check progress: `/next`
- Technical Specs: Mở file `docs/specs/offline_notifier_spec.md`
- Save context: `/save-brain`
