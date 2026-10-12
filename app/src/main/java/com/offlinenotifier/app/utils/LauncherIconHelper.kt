package com.offlinenotifier.app.utils

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log

object LauncherIconHelper {
    private const val TAG = "LauncherIconHelper"

    private fun isAppInForeground(context: Context): Boolean {
        return try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            val packageName = context.packageName
            activityManager.runningAppProcesses.orEmpty().any { process ->
                process.importance == android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND &&
                    process.processName == packageName
            }
        } catch (_: Exception) {
            false
        }
    }

    fun setActiveIcon(context: Context, isActive: Boolean, force: Boolean = false) {
        if (!force && isAppInForeground(context)) {
            Log.d(TAG, "Skip launcher icon switch while app is foreground")
            return
        }

        try {
            val appContext = context.applicationContext
            val packageName = appContext.packageName
            val packageManager = appContext.packageManager
            val activeAlias = ComponentName(packageName, "$packageName.MainActivityActive")
            val inactiveAlias = ComponentName(packageName, "$packageName.MainActivityInactive")

            if (isActive) {
                packageManager.setComponentEnabledSetting(
                    activeAlias,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP
                )
                packageManager.setComponentEnabledSetting(
                    inactiveAlias,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
                )
            } else {
                packageManager.setComponentEnabledSetting(
                    inactiveAlias,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP
                )
                packageManager.setComponentEnabledSetting(
                    activeAlias,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to switch launcher icon", e)
        }
    }
}
