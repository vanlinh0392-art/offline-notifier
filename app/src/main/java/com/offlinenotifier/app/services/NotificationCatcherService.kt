package com.offlinenotifier.app.services

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.offlinenotifier.app.data.database.AppDatabase
import com.offlinenotifier.app.data.database.EventEntity
import com.offlinenotifier.app.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

class NotificationCatcherService : NotificationListenerService() {

    private val TAG = "NotiCatcher Service"
    private val serviceScope = CoroutineScope(Dispatchers.IO)

    // Dedup: Lưu key (package+title+body) → timestamp để chống ghi trùng
    // Android gọi onNotificationPosted nhiều lần khi app update cùng 1 notification
    private val recentNotifications = java.util.concurrent.ConcurrentHashMap<String, Long>()
    private val DEDUP_WINDOW_MS = 3000L // 3 giây

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        
        if (!OfflineNotifierService.isRunning) {
            Log.d(TAG, "Service is not running. Ignoring notification.")
            return
        }
        
        sbn?.let { notification ->
            // Chỉ bắt thông báo nếu màn hình TẮT hoặc ĐANG KHÓA (User không dùng máy)
            val isDeviceOffline = com.offlinenotifier.app.utils.DeviceStateHelper.isDeviceLockedOrScreenOff(applicationContext)
            if (!isDeviceOffline) {
                Log.d(TAG, "Device is actively in use (screen is on and unlocked). Ignoring notification.")
                return
            }

            val packageName = notification.packageName
            val extras = notification.notification.extras
            val title = extras.getString("android.title") ?: "No Title"
            val text = extras.getCharSequence("android.text")?.toString() ?: "No Content"

            // Bỏ qua các thông báo hệ thống và chính ứng dụng này
            if (packageName == "android" || packageName == "com.android.systemui" || packageName == applicationContext.packageName) {
                return
            }

            // === DEDUP CHECK ===
            val now = System.currentTimeMillis()
            val dedupKey = "$packageName|$title|$text"
            val lastSeen = recentNotifications[dedupKey]
            if (lastSeen != null && (now - lastSeen) < DEDUP_WINDOW_MS) {
                Log.d(TAG, "Dedup: Skipping duplicate notification from $packageName (within ${DEDUP_WINDOW_MS}ms)")
                return
            }
            recentNotifications[dedupKey] = now

            // Dọn cache cũ (> 10 giây) để tránh memory leak
            recentNotifications.entries.removeIf { (now - it.value) > 10000L }

            // Trích xuất ảnh (đã scale và nén tối ưu) & audio từ notification
            val extractedImagePath = com.offlinenotifier.app.utils.MediaExtractorHelper.extractImageFromExtras(applicationContext, extras)
            val extractedAudioPath = com.offlinenotifier.app.utils.MediaExtractorHelper.extractAudioFromExtras(applicationContext, extras)

            // Lưu vào Room DB trong coroutine
            serviceScope.launch {
                val repository = SettingsRepository(applicationContext)
                
                // Kiểm tra setting Master App Capture
                val shouldCaptureApps = repository.captureAppsFlow.first()
                if (!shouldCaptureApps) {
                    com.offlinenotifier.app.utils.MediaExtractorHelper.deleteMediaFile(extractedImagePath)
                    com.offlinenotifier.app.utils.MediaExtractorHelper.deleteMediaFile(extractedAudioPath)
                    return@launch
                }
                
                // Kiểm tra App Filter
                val selectedAppsString = repository.selectedAppsFlow.first()
                if (selectedAppsString.isNotEmpty()) {
                    val selectedApps = selectedAppsString.split(",").map { it.trim() }
                    // Nếu danh sách không rỗng và package này ko được chọn thì bỏ qua
                    if (selectedApps.isNotEmpty() && !selectedApps.contains(packageName)) {
                        com.offlinenotifier.app.utils.MediaExtractorHelper.deleteMediaFile(extractedImagePath)
                        com.offlinenotifier.app.utils.MediaExtractorHelper.deleteMediaFile(extractedAudioPath)
                        return@launch
                    }
                }

                var formattedBody = text
                if (extractedAudioPath != null && (formattedBody.isBlank() || formattedBody == "No Content")) {
                    formattedBody = "[\uD83C\uDFB5 Tin nh\u1eafn tho\u1ea1i / \u00c2m thanh]"
                }

                // 1. Kiểm tra lọc thông báo Ongoing / Progress / Service
                val filterOngoing = repository.filterOngoingFlow.first()
                if (filterOngoing) {
                    val flags = notification.notification.flags
                    val isOngoing = (flags and android.app.Notification.FLAG_ONGOING_EVENT) != 0
                    val isForeground = (flags and android.app.Notification.FLAG_FOREGROUND_SERVICE) != 0
                    val isProgress = extras.getBoolean(android.app.Notification.EXTRA_PROGRESS_INDETERMINATE, false) ||
                                    extras.getInt(android.app.Notification.EXTRA_PROGRESS_MAX, 0) > 0
                    val category = notification.notification.category
                    val isMediaOrService = category in listOf(
                        android.app.Notification.CATEGORY_PROGRESS,
                        android.app.Notification.CATEGORY_SERVICE,
                        android.app.Notification.CATEGORY_TRANSPORT
                    )
                    if (isOngoing || isForeground || isProgress || isMediaOrService) {
                        Log.d(TAG, "Filter: Skipping ongoing/progress notification from $packageName (flags=$flags, cat=$category)")
                        com.offlinenotifier.app.utils.MediaExtractorHelper.deleteMediaFile(extractedImagePath)
                        com.offlinenotifier.app.utils.MediaExtractorHelper.deleteMediaFile(extractedAudioPath)
                        return@launch
                    }
                }

                // 2. Kiểm tra lọc từ khóa Blacklist (Quảng cáo, rác...)
                val blacklistString = repository.keywordBlacklistFlow.first()
                if (blacklistString.isNotBlank()) {
                    val keywords = blacklistString.split(",")
                        .map { it.trim().lowercase() }
                        .filter { it.isNotEmpty() }
                    val targetText = "$title $formattedBody".lowercase()
                    val matchedKeyword = keywords.firstOrNull { targetText.contains(it) }
                    if (matchedKeyword != null) {
                        Log.d(TAG, "Filter: Skipping notification from $packageName matching blacklist keyword: '$matchedKeyword'")
                        com.offlinenotifier.app.utils.MediaExtractorHelper.deleteMediaFile(extractedImagePath)
                        com.offlinenotifier.app.utils.MediaExtractorHelper.deleteMediaFile(extractedAudioPath)
                        return@launch
                    }
                }

                Log.d(TAG, "Caught notification from: $packageName - Title: $title - Text: $formattedBody (image=$extractedImagePath, audio=$extractedAudioPath)")

                val event = EventEntity(
                    type = "APP_NOTIFICATION",
                    title = "$packageName: $title",
                    body = formattedBody,
                    timestamp = System.currentTimeMillis(),
                    imagePath = extractedImagePath,
                    audioPath = extractedAudioPath
                )

                val db = AppDatabase.getDatabase(applicationContext)
                db.eventDao().insertEvent(event)
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        // No action needed for removals right now
    }
}
