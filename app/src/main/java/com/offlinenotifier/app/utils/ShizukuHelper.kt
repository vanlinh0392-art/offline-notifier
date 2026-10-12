package com.offlinenotifier.app.utils

import android.content.pm.PackageManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader

object ShizukuHelper {
    private const val TAG = "ShizukuHelper"

    /**
     * Checks if the Shizuku provider is connected and available.
     */
    fun isShizukuAvailable(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Checks if the application already has Shizuku permission granted.
     */
    fun hasShizukuPermission(): Boolean {
        return try {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Requests Shizuku permission asynchronously. The result is returned to the user via Shizuku.OnRequestPermissionResultListener
     */
    fun requestShizukuPermission(requestCode: Int) {
        try {
            Shizuku.requestPermission(requestCode)
        } catch (e: Exception) {
            Log.e(TAG, "Failed requesting Shizuku Permission", e)
        }
    }

    /**
     * Executes a shell command via Shizuku's process runner.
     */
    suspend fun executeCommand(command: String): Boolean = withContext(Dispatchers.IO) {
        if (!isShizukuAvailable() || !hasShizukuPermission()) {
            return@withContext false
        }
        try {
            // Shizuku v13+ made newProcess private/deprecated to encourage UserService, but for simple commands reflection is easiest
            val newProcessMethod = rikka.shizuku.Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            )
            newProcessMethod.isAccessible = true
            val process = newProcessMethod.invoke(null, arrayOf("sh", "-c", command), null, null) as Process
            
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String? = reader.readLine()
            while (line != null) {
                Log.d(TAG, "Shizuku Output: $line")
                line = reader.readLine()
            }
            val exitCode = process.waitFor()
            Log.d(TAG, "Executed command '$command' with exit code: $exitCode")
            return@withContext exitCode == 0
        } catch (e: Exception) {
            Log.e(TAG, "Command execution failed: $command", e)
            return@withContext false
        }
    }

    /**
     * Executes all standard Offline Notifier ADB background setup commands.
     */
    suspend fun autoGrantPermissions(): Boolean {
        if (!isShizukuAvailable() || !hasShizukuPermission()) return false

        val cmds = listOf(
            "pm grant com.offlinenotifier.app android.permission.READ_CALL_LOG",
            "pm grant com.offlinenotifier.app android.permission.READ_PHONE_STATE",
            "pm grant com.offlinenotifier.app android.permission.RECEIVE_SMS",
            "pm grant com.offlinenotifier.app android.permission.READ_SMS",
            "pm grant com.offlinenotifier.app android.permission.WRITE_SECURE_SETTINGS",
            "appops set com.offlinenotifier.app WRITE_SETTINGS allow",
            "dumpsys deviceidle whitelist +com.offlinenotifier.app",
            // Bật quyền đọc thông báo (Notification Listener) qua ADB
            "cmd notification allow_listener com.offlinenotifier.app/com.offlinenotifier.app.services.NotificationCatcherService"
        )
        
        var allSuccess = true
        for (cmd in cmds) {
            val success = executeCommand(cmd)
            if (!success) {
                allSuccess = false
            }
        }
        return allSuccess
    }
}
