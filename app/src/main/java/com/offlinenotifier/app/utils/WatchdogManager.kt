package com.offlinenotifier.app.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.offlinenotifier.app.receivers.WatchdogReceiver

object WatchdogManager {
    private const val TAG = "WatchdogManager"
    private const val INTERVAL_MINUTES = 2L

    fun startWatchdog(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, WatchdogReceiver::class.java).apply {
            action = WatchdogReceiver.ACTION_WATCHDOG_TICK
        }

        // FLAG_IMMUTABLE is required on Android 12+
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            1234, // unique ID
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val intervalMillis = INTERVAL_MINUTES * 60 * 1000

        try {
            // setRepeating is inexact on later APIs, but good enough for a 2-minute watchdog
            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + intervalMillis,
                intervalMillis,
                pendingIntent
            )
            Log.d(TAG, "Watchdog Timer started. Tick every $INTERVAL_MINUTES mins.")
        } catch (e: SecurityException) {
            Log.e(TAG, "Exact alarm permission missing on Android 14+", e)
            // Fallback for missing exact alarm perm if we tried exact
        }
    }

    fun stopWatchdog(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, WatchdogReceiver::class.java).apply {
            action = WatchdogReceiver.ACTION_WATCHDOG_TICK
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            1234,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE // Only get if it exists
        )
        
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d(TAG, "Watchdog Timer stopped.")
        }
    }
}
