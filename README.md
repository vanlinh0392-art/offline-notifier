# Offline Notifier

Offline Notifier la ung dung Android ghi nhan cuoc goi, SMS va thong bao ung dung khi thiet bi khong duoc su dung, sau do tong hop va gui bao cao qua email. Repo nay da co implementation hoat dong cho luong chinh tren may that, khong con o muc planning.

## Current Status

- 1 module Android app (`:app`) voi Jetpack Compose, Room, DataStore, WorkManager va JavaMail
- UI da co man hinh `Settings` va `Log History`
- Runtime chinh da co foreground service, notification listener, SMS/call receiver, watchdog va luong gui email
- Chua co test tu dong trong `app/src/test` hoac `app/src/androidTest`

## Stack

- Kotlin + Android SDK
- Jetpack Compose Material 3
- Room cho offline event queue va sent log
- DataStore Preferences cho app settings
- WorkManager cho email send jobs
- JavaMail (`com.sun.mail`) gui SMTP truc tiep
- Shizuku + ADB commands de cap cac quyen dac biet

## Repo Layout

- `app/src/main/java/com/offlinenotifier/app/MainActivity.kt`: entrypoint UI
- `app/src/main/java/com/offlinenotifier/app/ui`: Compose screens
- `app/src/main/java/com/offlinenotifier/app/viewmodels`: ViewModel dieu phoi UI va runtime
- `app/src/main/java/com/offlinenotifier/app/services`: foreground service, notification listener, SMTP
- `app/src/main/java/com/offlinenotifier/app/receivers`: SMS/call receiver, watchdog receiver
- `app/src/main/java/com/offlinenotifier/app/data`: DataStore va Room database
- `app/src/main/java/com/offlinenotifier/app/workers`: WorkManager worker va helper
- `docs/architecture.md`: overview kien truc va runtime flow
- `grant_permissions.cmd`: script cap quyen ADB cho may that

## Runtime Flow

1. User bat monitoring trong `SettingsScreen`.
2. `SettingsViewModel` luu state vao DataStore va start `OfflineNotifierService`.
3. `OfflineNotifierService` theo doi screen on/off, bat watchdog va trigger email send khi queue co du lieu.
4. `NotificationCatcherService` va `CallAndSmsReceiver` ghi su kien vao Room khi man hinh dang tat.
5. `EmailSenderWorker` doc queue, gui email qua `SmtpClient`, ghi lich su vao `sent_logs`, roi xoa queue neu gui thanh cong.

## Build And Run

Repo hien khong kem `gradlew`, vi vay dung Gradle local trong workspace:

```powershell
.\gradle-8.7\bin\gradle.bat :app:assembleDebug
.\gradle-8.7\bin\gradle.bat :app:testDebugUnitTest
```

Yeu cau moi truong:

- JDK 17 hoac 21
- Android SDK / Build Tools phu hop voi `compileSdk = 35`
- Neu dang dung Android Studio, co the dung JBR di kem (`C:\Program Files\Android\Android Studio\jbr`)

Luu y: JDK 25 hien khong build duoc repo nay vi Gradle/Kotlin DSL trong du an chua parse duoc version `25.0.1`.

## Device Setup

App can mot so quyen dac biet de hoat dong on dinh tren may that:

- Notification Listener
- `READ_CALL_LOG`
- `READ_SMS` / `RECEIVE_SMS`
- `WRITE_SECURE_SETTINGS`
- `WRITE_SETTINGS`
- battery whitelist / device idle whitelist

Co the dung:

```powershell
.\grant_permissions.cmd
```

Hoac bat truc tiep trong giao dien thong qua Shizuku neu thiet bi da cap quyen.

## Known Constraints

- App password email dang duoc luu trong DataStore, chua co ma hoa o tang luu tru
- `AppDatabase` dang dung `fallbackToDestructiveMigration()`, doi schema co the lam mat queue/log
- Do app phu thuoc vao notification listener, ADB permissions va hanh vi OEM, test tren Xiaomi/HyperOS van la buoc bat buoc

## Related Docs

- `docs/architecture.md`
- `docs/specs/offline_notifier_spec.md`
- `docs/design-specs.md`
