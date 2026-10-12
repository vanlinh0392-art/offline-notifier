package com.offlinenotifier.app.utils

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.offlinenotifier.app.R
import com.offlinenotifier.app.ShortcutActionActivity
import com.offlinenotifier.app.data.SettingsRepository

object ShortcutHelper {
    const val ACTION_START_MONITORING = "com.offlinenotifier.app.ACTION_START_MONITORING"
    const val ACTION_STOP_MONITORING = "com.offlinenotifier.app.ACTION_STOP_MONITORING"
    const val EXTRA_SHORTCUT_TOKEN = "com.offlinenotifier.app.EXTRA_SHORTCUT_TOKEN"

    suspend fun refreshDynamicShortcuts(context: Context) {
        val appContext = context.applicationContext
        val token = SettingsRepository(appContext).getOrCreateShortcutToken()

        val startShortcut = ShortcutInfoCompat.Builder(appContext, "start_monitoring")
            .setShortLabel(appContext.getString(R.string.shortcut_start_monitoring))
            .setLongLabel(appContext.getString(R.string.shortcut_start_monitoring))
            .setIcon(IconCompat.createWithResource(appContext, R.drawable.ic_shortcut_start))
            .setIntent(
                Intent(appContext, ShortcutActionActivity::class.java).apply {
                    action = ACTION_START_MONITORING
                    putExtra(EXTRA_SHORTCUT_TOKEN, token)
                }
            )
            .build()

        val stopShortcut = ShortcutInfoCompat.Builder(appContext, "stop_monitoring")
            .setShortLabel(appContext.getString(R.string.shortcut_stop_monitoring))
            .setLongLabel(appContext.getString(R.string.shortcut_stop_monitoring))
            .setIcon(IconCompat.createWithResource(appContext, R.drawable.ic_shortcut_stop))
            .setIntent(
                Intent(appContext, ShortcutActionActivity::class.java).apply {
                    action = ACTION_STOP_MONITORING
                    putExtra(EXTRA_SHORTCUT_TOKEN, token)
                }
            )
            .build()

        ShortcutManagerCompat.setDynamicShortcuts(appContext, listOf(startShortcut, stopShortcut))
    }
}
