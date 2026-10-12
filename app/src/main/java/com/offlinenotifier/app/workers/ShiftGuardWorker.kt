package com.offlinenotifier.app.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.offlinenotifier.app.data.SettingsRepository
import com.offlinenotifier.app.shift.ShiftScheduleManager
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class ShiftGuardWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "ShiftGuardWorker"
        private const val WORK_NAME = "offline_notifier_shift_guard_work"

        fun scheduleGuard(context: Context) {
            val workRequest = PeriodicWorkRequestBuilder<ShiftGuardWorker>(15, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
            Log.d(TAG, "ShiftGuardWorker periodic guard scheduled (15 min interval).")
        }

        fun cancelGuard(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            Log.d(TAG, "ShiftGuardWorker periodic guard cancelled.")
        }
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "ShiftGuardWorker running periodic shift sanity check...")
        val repository = SettingsRepository(context)
        val isScheduleEnabled = repository.scheduleEnabledFlow.first()

        if (isScheduleEnabled) {
            try {
                ShiftScheduleManager.recalculateAndSchedule(context)
                Log.d(TAG, "ShiftGuardWorker successfully verified and updated shift schedule.")
            } catch (e: Exception) {
                Log.e(TAG, "Error in ShiftGuardWorker during recalculation", e)
            }
        }
        return Result.success()
    }
}
