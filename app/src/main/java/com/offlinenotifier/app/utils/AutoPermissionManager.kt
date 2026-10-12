package com.offlinenotifier.app.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import rikka.shizuku.Shizuku

object AutoPermissionManager {
    private const val TAG = "AutoPermissionManager"
    private const val COOLDOWN_MILLIS = 300_000L // 5 phút cooldown cho background auto check

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()

    private var lastCheckTime = 0L
    private var isListenerRegistered = false

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        Log.d(TAG, "Shizuku Binder received. Checking and granting missing permissions...")
        scope.launch {
            // Có thể cần delay nhỏ để binder ổn định
            kotlinx.coroutines.delay(500)
            checkAndGrantMissingPermissionsInternal(appContext ?: return@launch, force = true)
        }
    }

    private val permissionResultListener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
        if (grantResult == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Shizuku permission granted for requestCode $requestCode. Auto-granting permissions...")
            scope.launch {
                checkAndGrantMissingPermissionsInternal(appContext ?: return@launch, force = true)
            }
        }
    }

    private var appContext: Context? = null

    /**
     * Khởi tạo và đăng ký lắng nghe sự kiện Shizuku (chỉ đăng ký 1 lần).
     */
    fun init(context: Context) {
        appContext = context.applicationContext
        if (!isListenerRegistered) {
            try {
                Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
                Shizuku.addRequestPermissionResultListener(permissionResultListener)
                isListenerRegistered = true
                Log.d(TAG, "Shizuku event listeners registered successfully.")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to register Shizuku listeners", e)
            }
        }

        // Tự động kiểm tra ngay khi khởi tạo
        checkAndGrantMissingPermissions(context, force = false)
    }

    /**
     * Kiểm tra và tự động cấp các quyền còn thiếu qua Shizuku.
     * @param force Nếu true, bỏ qua thời gian cooldown 5 phút.
     */
    fun checkAndGrantMissingPermissions(context: Context, force: Boolean = false) {
        val appCtx = context.applicationContext
        appContext = appCtx
        scope.launch {
            checkAndGrantMissingPermissionsInternal(appCtx, force)
        }
    }

    private suspend fun checkAndGrantMissingPermissionsInternal(context: Context, force: Boolean): Boolean = mutex.withLock {
        val now = System.currentTimeMillis()
        if (!force && (now - lastCheckTime) < COOLDOWN_MILLIS) {
            Log.d(TAG, "Skipping permission check due to cooldown (${(now - lastCheckTime) / 1000}s / ${COOLDOWN_MILLIS / 1000}s)")
            return false
        }
        lastCheckTime = now

        if (!ShizukuHelper.isShizukuAvailable() || !ShizukuHelper.hasShizukuPermission()) {
            Log.d(TAG, "Shizuku is not available or permission not granted. Skipping auto-grant.")
            return false
        }

        val commandsToRun = mutableListOf<String>()
        val packageName = context.packageName

        // 1. Runtime Permissions (Call & SMS & Notifications)
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
            commandsToRun.add("pm grant $packageName android.permission.READ_CALL_LOG")
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            commandsToRun.add("pm grant $packageName android.permission.READ_PHONE_STATE")
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
            commandsToRun.add("pm grant $packageName android.permission.RECEIVE_SMS")
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            commandsToRun.add("pm grant $packageName android.permission.READ_SMS")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                commandsToRun.add("pm grant $packageName android.permission.POST_NOTIFICATIONS")
            }
        }

        // 2. Write Secure Settings
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_SECURE_SETTINGS) != PackageManager.PERMISSION_GRANTED) {
            commandsToRun.add("pm grant $packageName android.permission.WRITE_SECURE_SETTINGS")
        }

        // 3. Write Settings (AppOps)
        if (!Settings.System.canWrite(context)) {
            commandsToRun.add("appops set $packageName WRITE_SETTINGS allow")
        }

        // 4. Chống Android tự động thu hồi quyền khi chạy nền lâu ngày (Android 11+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            commandsToRun.add("appops set $packageName AUTO_REVOKE_PERMISSIONS_IF_UNUSED ignore")
        }

        // 5. Bỏ qua tối ưu hóa pin (Doze whitelist)
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val isIgnoringBattery = powerManager?.isIgnoringBatteryOptimizations(packageName) ?: false
        if (!isIgnoringBattery) {
            commandsToRun.add("dumpsys deviceidle whitelist +$packageName")
        }

        // 6. Notification Listener Service
        val enabledListeners = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners") ?: ""
        val notifListenerServiceComponent = "$packageName/com.offlinenotifier.app.services.NotificationCatcherService"
        if (!enabledListeners.contains(packageName)) {
            commandsToRun.add("cmd notification allow_listener $notifListenerServiceComponent")
        }

        // Diff-checking: Nếu không thiếu quyền nào, không chạy shell command (0% CPU/Battery)
        if (commandsToRun.isEmpty()) {
            Log.d(TAG, "All permissions are already fully granted. No shell commands needed.")
            return true
        }

        Log.d(TAG, "Found ${commandsToRun.size} missing permissions. Executing auto-grant via Shizuku...")
        var allSuccess = true
        for (cmd in commandsToRun) {
            val success = ShizukuHelper.executeCommand(cmd)
            if (!success) {
                allSuccess = false
                Log.w(TAG, "Failed command: $cmd")
            }
        }

        Log.d(TAG, "Auto-grant completed. All success: $allSuccess")
        return allSuccess
    }

    /**
     * Kiểm tra xem toàn bộ các quyền cốt lõi đã được cấp đủ hay chưa.
     */
    fun areAllCorePermissionsGranted(context: Context): Boolean {
        val hasCallLog = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED
        val hasPhoneState = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
        val hasSms = ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
        val hasReadSms = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
        val hasNotifListener = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")?.contains(context.packageName) == true

        return hasCallLog && hasPhoneState && hasSms && hasReadSms && hasNotifListener
    }
}
