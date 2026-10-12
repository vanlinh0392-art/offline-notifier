package com.offlinenotifier.app.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.offlinenotifier.app.data.SettingsRepository
import com.offlinenotifier.app.services.OfflineNotifierService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class WatchdogReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_WATCHDOG_TICK = "com.offlinenotifier.app.ACTION_WATCHDOG_TICK"
    }

    private val TAG = "WatchdogReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_WATCHDOG_TICK && intent.action != Intent.ACTION_BOOT_COMPLETED) {
            return
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = SettingsRepository(context.applicationContext)
                val isScheduleEnabled = repository.scheduleEnabledFlow.first()

                if (isScheduleEnabled && intent.action == Intent.ACTION_BOOT_COMPLETED) {
                    Log.d(TAG, "Boot completed with shift schedule enabled. Recalculating and scheduling shifts.")
                    com.offlinenotifier.app.shift.ShiftScheduleManager.recalculateAndSchedule(context.applicationContext)
                    return@launch
                }

                val isActive = repository.isActiveFlow.first()

                if (!isActive) {
                    Log.d(TAG, "Monitoring is OFF. Skip watchdog recovery for ${intent.action}.")
                    return@launch
                }

                Log.d(TAG, "Watchdog event ${intent.action} received. Checking service status...")

                if (!OfflineNotifierService.isRunning) {
                    Log.w(TAG, "OfflineNotifierService is DEAD. Attempting to resuscitate...")

                    val serviceIntent = Intent(context, OfflineNotifierService::class.java)
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                    Log.d(TAG, "Resuscitation command sent.")
                } else {
                    Log.d(TAG, "OfflineNotifierService is ALIVE. All good.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to handle watchdog event", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
