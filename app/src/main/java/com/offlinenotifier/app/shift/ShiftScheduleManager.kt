package com.offlinenotifier.app.shift

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.offlinenotifier.app.data.SettingsRepository
import com.offlinenotifier.app.data.database.AppDatabase
import com.offlinenotifier.app.receivers.ShiftScheduleReceiver
import com.offlinenotifier.app.services.OfflineNotifierService
import com.offlinenotifier.app.utils.NetworkStateHelper
import com.offlinenotifier.app.utils.WifiStateMonitor
import com.offlinenotifier.app.workers.ShiftGuardWorker
import com.offlinenotifier.app.workers.WorkManagerHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object ShiftScheduleManager {
    private const val TAG = "ShiftScheduleManager"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()

    data class ShiftDiagnosticInfo(
        val isScheduleEnabled: Boolean = false,
        val crew: String = "A",
        val todayShift: ShiftCalculator.ShiftType = ShiftCalculator.ShiftType.NGAY,
        val isCurrentlyInShift: Boolean = false,
        val isServiceRunning: Boolean = false,
        val isWifiBlocked: Boolean = false,
        val nextAlarmFormatted: String = "--:--",
        val lastCheckTimestamp: Long = 0L
    )

    private val _diagnosticFlow = MutableStateFlow(ShiftDiagnosticInfo())
    val diagnosticFlow: StateFlow<ShiftDiagnosticInfo> = _diagnosticFlow.asStateFlow()

    /**
     * Entry-point tính toán và đồng bộ lịch kíp (không block Main Thread).
     */
    fun recalculateAndSchedule(context: Context) {
        val appContext = context.applicationContext
        scope.launch {
            recalculateAndScheduleInternal(appContext)
        }
    }

    private suspend fun recalculateAndScheduleInternal(context: Context) = mutex.withLock {
        val repo = SettingsRepository(context)
        val enabled = repo.scheduleEnabledFlow.first()

        if (!enabled) {
            cancelAlarms(context)
            ShiftGuardWorker.cancelGuard(context)
            _diagnosticFlow.value = ShiftDiagnosticInfo(
                isScheduleEnabled = false,
                isServiceRunning = OfflineNotifierService.isRunning,
                lastCheckTimestamp = System.currentTimeMillis()
            )
            return@withLock
        }

        // Bật WifiStateMonitor và ShiftGuardWorker dự phòng
        WifiStateMonitor.startMonitoring(context)
        ShiftGuardWorker.scheduleGuard(context)

        val crew = repo.selectedCrewFlow.first()
        val dayStartHour = repo.dayShiftStartHourFlow.first()
        val dayStartMin = repo.dayShiftStartMinFlow.first()
        val dayEndHour = repo.dayShiftEndHourFlow.first()
        val dayEndMin = repo.dayShiftEndMinFlow.first()

        val nightStartHour = repo.nightShiftStartHourFlow.first()
        val nightStartMin = repo.nightShiftStartMinFlow.first()
        val nightEndHour = repo.nightShiftEndHourFlow.first()
        val nightEndMin = repo.nightShiftEndMinFlow.first()

        val dayStart = LocalTime.of(dayStartHour, dayStartMin)
        val dayEnd = LocalTime.of(dayEndHour, dayEndMin)
        val nightStart = LocalTime.of(nightStartHour, nightStartMin)
        val nightEnd = LocalTime.of(nightEndHour, nightEndMin)

        val now = LocalDateTime.now()
        val today = now.toLocalDate()
        val yesterday = today.minusDays(1)

        val todayShift = ShiftCalculator.getActualShift(crew, today)
        val yesterdayShift = ShiftCalculator.getActualShift(crew, yesterday)

        // 1. Xác định hiện tại có đang trong giờ làm việc của kíp không
        val shouldBeActive = checkActiveStatus(
            todayShift = todayShift,
            yesterdayShift = yesterdayShift,
            dayStart = dayStart,
            dayEnd = dayEnd,
            nightStart = nightStart,
            nightEnd = nightEnd,
            now = now
        )

        val currentActive = repo.isActiveFlow.first()
        val isServiceRunning = OfflineNotifierService.isRunning
        val wifiAutoStop = repo.autoStopOnWifiFlow.first()
        val wifiConnected = NetworkStateHelper.isWifiConnected(context)
        val isWifiBlocked = shouldBeActive && wifiAutoStop && wifiConnected

        Log.d(TAG, "Recalculate Shift: crew=$crew, todayShift=$todayShift (yesterday=$yesterdayShift), shouldBeActive=$shouldBeActive, isServiceRunning=$isServiceRunning, wifiConnected=$wifiConnected, wifiBlocked=$isWifiBlocked")

        // 2. Đồng bộ trạng thái Foreground Service
        if (shouldBeActive) {
            if (isWifiBlocked) {
                Log.d(TAG, "In shift hours, but Wi-Fi auto-stop is active. Stopping service.")
                if (isServiceRunning || currentActive) {
                    repo.saveIsActive(false)
                    clearQueueAndMedia(context)
                    val intent = Intent(context, OfflineNotifierService::class.java)
                    context.stopService(intent)
                    WorkManagerHelper.stopEmailWorker(context)
                }
            } else {
                Log.d(TAG, "In shift hours and not blocked by Wi-Fi. Ensuring service is RUNNING.")
                if (!isServiceRunning || !currentActive) {
                    com.offlinenotifier.app.utils.AutoPermissionManager.checkAndGrantMissingPermissions(context, force = false)
                    clearQueueAndMedia(context)
                    repo.saveIsActive(true)
                    val intent = Intent(context, OfflineNotifierService::class.java)
                    ContextCompat.startForegroundService(context, intent)
                }
            }
        } else {
            // Ngoài giờ làm việc
            Log.d(TAG, "Outside shift hours. Ensuring service is STOPPED.")
            if (isServiceRunning || currentActive) {
                repo.saveIsActive(false)
                clearQueueAndMedia(context)
                WorkManagerHelper.stopEmailWorker(context)
                val intent = Intent(context, OfflineNotifierService::class.java)
                context.stopService(intent)
            }
        }

        // 3. Tìm và lên lịch Alarm cho mốc tiếp theo (Bảo đảm 100% luôn có Alarm tương lai)
        val nextAlarmTime = findNextAlarmTime(
            today = today,
            dayStart = dayStart,
            dayEnd = dayEnd,
            nightStart = nightStart,
            nightEnd = nightEnd,
            now = now
        )

        scheduleAlarm(context, nextAlarmTime)

        val formatter = DateTimeFormatter.ofPattern("HH:mm:ss dd/MM")
        _diagnosticFlow.value = ShiftDiagnosticInfo(
            isScheduleEnabled = true,
            crew = crew,
            todayShift = todayShift,
            isCurrentlyInShift = shouldBeActive,
            isServiceRunning = OfflineNotifierService.isRunning,
            isWifiBlocked = isWifiBlocked,
            nextAlarmFormatted = nextAlarmTime.format(formatter),
            lastCheckTimestamp = System.currentTimeMillis()
        )
    }

    /**
     * Kiểm tra xem thời điểm [now] có đang nằm trong ca làm việc không.
     */
    fun checkActiveStatus(
        todayShift: ShiftCalculator.ShiftType,
        yesterdayShift: ShiftCalculator.ShiftType,
        dayStart: LocalTime,
        dayEnd: LocalTime,
        nightStart: LocalTime,
        nightEnd: LocalTime,
        now: LocalDateTime
    ): Boolean {
        val todayDate = now.toLocalDate()

        // 1. Kiểm tra Ca Ngày hôm nay
        if (todayShift == ShiftCalculator.ShiftType.NGAY) {
            val startDT = todayDate.atTime(dayStart)
            val endDT = todayDate.atTime(dayEnd)
            if (!now.isBefore(startDT) && now.isBefore(endDT)) {
                return true
            }
        }

        // 2. Kiểm tra Ca Đêm hôm nay (bắt đầu tối nay, kết thúc sáng mai)
        if (todayShift == ShiftCalculator.ShiftType.DEM) {
            val startDT = todayDate.atTime(nightStart)
            val endDT = if (nightEnd.isBefore(nightStart) || nightEnd == nightStart) {
                todayDate.plusDays(1).atTime(nightEnd)
            } else {
                todayDate.atTime(nightEnd)
            }
            if (!now.isBefore(startDT) && now.isBefore(endDT)) {
                return true
            }
        }

        // 3. Kiểm tra Ca Đêm hôm qua (bắt đầu tối hôm qua, kết thúc sáng hôm nay)
        if (yesterdayShift == ShiftCalculator.ShiftType.DEM) {
            val startDT = todayDate.minusDays(1).atTime(nightStart)
            val endDT = todayDate.atTime(nightEnd)
            if (!now.isBefore(startDT) && now.isBefore(endDT)) {
                return true
            }
        }

        return false
    }

    /**
     * Tìm mốc giờ Alarm tiếp theo gần nhất trong tương lai.
     */
    fun findNextAlarmTime(
        today: LocalDate,
        dayStart: LocalTime,
        dayEnd: LocalTime,
        nightStart: LocalTime,
        nightEnd: LocalTime,
        now: LocalDateTime
    ): LocalDateTime {
        val candidates = mutableListOf<LocalDateTime>()

        // Quét qua 3 ngày: hôm nay, ngày mai, ngày kia
        for (dayOffset in 0..2) {
            val date = today.plusDays(dayOffset.toLong())
            candidates.add(date.atTime(dayStart))
            candidates.add(date.atTime(dayEnd))
            candidates.add(date.atTime(nightStart))
            candidates.add(date.atTime(nightEnd))
            // Mốc nửa đêm để tính lại ca cho ngày mới
            candidates.add(date.atTime(0, 1))
        }

        // Lấy mốc thời gian sớm nhất strictly in the future (sau now ít nhất 5 giây)
        val safetyNow = now.plusSeconds(5)
        return candidates
            .filter { it.isAfter(safetyNow) }
            .minOrNull() ?: today.plusDays(1).atTime(0, 1)
    }

    private fun scheduleAlarm(context: Context, alarmTime: LocalDateTime) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ShiftScheduleReceiver::class.java).apply {
            action = ShiftScheduleReceiver.ACTION_SHIFT_TRIGGER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            1002,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val zonedDateTime = alarmTime.atZone(ZoneId.systemDefault())
        val triggerAtMillis = zonedDateTime.toInstant().toEpochMilli()

        Log.d(TAG, "Scheduling next shift trigger alarm for $alarmTime ($triggerAtMillis ms)")

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed setting alarm in AlarmManager, fallback to setAndAllowWhileIdle", e)
            try {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } catch (e2: Exception) {
                Log.e(TAG, "AlarmManager setAndAllowWhileIdle also failed", e2)
            }
        }
    }

    private suspend fun clearQueueAndMedia(context: Context) {
        try {
            val db = AppDatabase.getDatabase(context)
            val events = db.eventDao().getAllEvents()
            events.forEach { event ->
                com.offlinenotifier.app.utils.MediaExtractorHelper.deleteMediaFile(event.imagePath)
                com.offlinenotifier.app.utils.MediaExtractorHelper.deleteMediaFile(event.audioPath)
            }
            db.eventDao().clearAll()
            Log.d(TAG, "Cleared ${events.size} pending events and associated media files")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing queue and media", e)
        }
    }

    fun cancelAlarms(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ShiftScheduleReceiver::class.java).apply {
            action = ShiftScheduleReceiver.ACTION_SHIFT_TRIGGER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            1002,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            Log.d(TAG, "Cancelling scheduled shift trigger alarm")
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }
}
