package com.offlinenotifier.app.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.offlinenotifier.app.shift.ShiftScheduleManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ShiftScheduleReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_SHIFT_TRIGGER = "com.offlinenotifier.app.ACTION_SHIFT_TRIGGER"
        private const val TAG = "ShiftScheduleReceiver"
        private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Received broadcast: ${intent.action}")
        if (intent.action == ACTION_SHIFT_TRIGGER) {
            val pendingResult = goAsync()
            val appContext = context.applicationContext
            receiverScope.launch {
                try {
                    Log.d(TAG, "Executing ShiftScheduleManager.recalculateAndSchedule via goAsync...")
                    ShiftScheduleManager.recalculateAndSchedule(appContext)
                } catch (e: Exception) {
                    Log.e(TAG, "Error executing shift schedule recalculation in receiver", e)
                } finally {
                    try {
                        pendingResult.finish()
                    } catch (e: Exception) {
                        Log.w(TAG, "Error finishing pendingResult", e)
                    }
                }
            }
        }
    }
}
