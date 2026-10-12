package com.offlinenotifier.app.viewmodels

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.offlinenotifier.app.data.SettingsRepository
import com.offlinenotifier.app.data.database.AppDatabase
import com.offlinenotifier.app.data.database.EventEntity
import com.offlinenotifier.app.services.OfflineNotifierService
import com.offlinenotifier.app.utils.NetworkStateHelper
import com.offlinenotifier.app.workers.WorkManagerHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val application: Application) : AndroidViewModel(application) {
    private val repository = SettingsRepository(application)
    private val db = AppDatabase.getDatabase(application)

    val senderEmail: StateFlow<String> = repository.senderEmailFlow.stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = "")
    val recipientEmail: StateFlow<String> = repository.recipientEmailFlow.stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = "")
    val emailPass: StateFlow<String> = repository.emailPassFlow.stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = "")
    val intervalIndex: StateFlow<Int> = repository.intervalIndexFlow.stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = 0)
    val customInterval: StateFlow<Int> = repository.customIntervalFlow.stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = 5)
    val isActive: StateFlow<Boolean> = repository.isActiveFlow.stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = false)
    
    val captureSms: StateFlow<Boolean> = repository.captureSmsFlow.stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = true)
    val captureCalls: StateFlow<Boolean> = repository.captureCallsFlow.stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = true)
    val captureApps: StateFlow<Boolean> = repository.captureAppsFlow.stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = true)
    val autoStopOnWifi: StateFlow<Boolean> = repository.autoStopOnWifiFlow.stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = false)
    val showTestButton: StateFlow<Boolean> = repository.showTestButtonFlow.stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = true)
    val selectedApps: StateFlow<String> = repository.selectedAppsFlow.stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = "")
    val filterOngoing: StateFlow<Boolean> = repository.filterOngoingFlow.stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = true)
    val keywordBlacklist: StateFlow<String> = repository.keywordBlacklistFlow.stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = "qu\u1ea3ng c\u00e1o, khuy\u1ebfn m\u00e3i, \u01b0u \u0111\u00e3i, voucher, m\u00e3 gi\u1ea3m gi\u00e1, otp, sao l\u01b0u, \u0111\u1ed3ng b\u1ed9")

    // Shift schedule preferences
    val scheduleEnabled: StateFlow<Boolean> = repository.scheduleEnabledFlow.stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = false)
    val selectedCrew: StateFlow<String> = repository.selectedCrewFlow.stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = "A")
    val dayShiftStartHour: StateFlow<Int> = repository.dayShiftStartHourFlow.stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = 6)
    val dayShiftStartMin: StateFlow<Int> = repository.dayShiftStartMinFlow.stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = 0)
    val dayShiftEndHour: StateFlow<Int> = repository.dayShiftEndHourFlow.stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = 18)
    val dayShiftEndMin: StateFlow<Int> = repository.dayShiftEndMinFlow.stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = 0)
    val nightShiftStartHour: StateFlow<Int> = repository.nightShiftStartHourFlow.stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = 18)
    val nightShiftStartMin: StateFlow<Int> = repository.nightShiftStartMinFlow.stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = 0)
    val nightShiftEndHour: StateFlow<Int> = repository.nightShiftEndHourFlow.stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = 6)
    val nightShiftEndMin: StateFlow<Int> = repository.nightShiftEndMinFlow.stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = 0)

    val todayAllCrewShifts: StateFlow<Map<String, com.offlinenotifier.app.shift.ShiftCalculator.ShiftType>> = kotlinx.coroutines.flow.flow {
        while (true) {
            val today = java.time.LocalDate.now()
            emit(mapOf(
                "A" to com.offlinenotifier.app.shift.ShiftCalculator.getActualShift("A", today),
                "B" to com.offlinenotifier.app.shift.ShiftCalculator.getActualShift("B", today),
                "C" to com.offlinenotifier.app.shift.ShiftCalculator.getActualShift("C", today)
            ))
            kotlinx.coroutines.delay(60000)
        }
    }.stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = emptyMap())

    val shiftDiagnosticInfo: StateFlow<com.offlinenotifier.app.shift.ShiftScheduleManager.ShiftDiagnosticInfo> = com.offlinenotifier.app.shift.ShiftScheduleManager.diagnosticFlow

    val pendingEvents: StateFlow<List<EventEntity>> = db.eventDao().getAllEventsFlow().stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = emptyList())

    fun onSenderEmailChanged(email: String) { viewModelScope.launch { repository.saveSenderEmail(email) } }
    fun onRecipientEmailChanged(email: String) { viewModelScope.launch { repository.saveRecipientEmail(email) } }
    fun onEmailPassChanged(newPass: String) { viewModelScope.launch { repository.saveEmailPass(newPass) } }
    fun onIntervalChanged(newIndex: Int) { viewModelScope.launch { repository.saveIntervalIndex(newIndex); restartWorkerIfActive() } }
    fun onCustomIntervalChanged(minutes: Int) { viewModelScope.launch { repository.saveCustomInterval(minutes); restartWorkerIfActive() } }
    
    fun onCaptureSmsToggled(enabled: Boolean) { viewModelScope.launch { repository.saveCaptureSms(enabled) } }
    fun onCaptureCallsToggled(enabled: Boolean) { viewModelScope.launch { repository.saveCaptureCallsEnabled(enabled) } }
    fun onCaptureAppsToggled(enabled: Boolean) { viewModelScope.launch { repository.saveCaptureApps(enabled) } }
    fun onFilterOngoingToggled(enabled: Boolean) { viewModelScope.launch { repository.saveFilterOngoing(enabled) } }
    fun onKeywordBlacklistChanged(keywords: String) { viewModelScope.launch { repository.saveKeywordBlacklist(keywords) } }

    fun onScheduleEnabledToggled(enabled: Boolean) {
        viewModelScope.launch {
            repository.saveScheduleEnabled(enabled)
            if (enabled) {
                com.offlinenotifier.app.shift.ShiftScheduleManager.recalculateAndSchedule(application)
            } else {
                com.offlinenotifier.app.shift.ShiftScheduleManager.cancelAlarms(application)
            }
        }
    }

    fun onSelectedCrewChanged(crew: String) {
        viewModelScope.launch {
            repository.saveSelectedCrew(crew)
            if (scheduleEnabled.value) {
                com.offlinenotifier.app.shift.ShiftScheduleManager.recalculateAndSchedule(application)
            }
        }
    }

    fun onDayShiftStartChanged(hour: Int, min: Int) {
        viewModelScope.launch {
            repository.saveDayShiftStartHour(hour)
            repository.saveDayShiftStartMin(min)
            if (scheduleEnabled.value) {
                com.offlinenotifier.app.shift.ShiftScheduleManager.recalculateAndSchedule(application)
            }
        }
    }

    fun onDayShiftEndChanged(hour: Int, min: Int) {
        viewModelScope.launch {
            repository.saveDayShiftEndHour(hour)
            repository.saveDayShiftEndMin(min)
            if (scheduleEnabled.value) {
                com.offlinenotifier.app.shift.ShiftScheduleManager.recalculateAndSchedule(application)
            }
        }
    }

    fun onNightShiftStartChanged(hour: Int, min: Int) {
        viewModelScope.launch {
            repository.saveNightShiftStartHour(hour)
            repository.saveNightShiftStartMin(min)
            if (scheduleEnabled.value) {
                com.offlinenotifier.app.shift.ShiftScheduleManager.recalculateAndSchedule(application)
            }
        }
    }

    fun onNightShiftEndChanged(hour: Int, min: Int) {
        viewModelScope.launch {
            repository.saveNightShiftEndHour(hour)
            repository.saveNightShiftEndMin(min)
            if (scheduleEnabled.value) {
                com.offlinenotifier.app.shift.ShiftScheduleManager.recalculateAndSchedule(application)
            }
        }
    }
    fun onAutoStopOnWifiToggled(enabled: Boolean) {
        viewModelScope.launch {
            repository.saveAutoStopOnWifi(enabled)
            clearPendingQueue()
            if (isActive.value && enabled && NetworkStateHelper.isWifiConnected(application)) {
                repository.saveIsActive(false)
                val serviceIntent = Intent(application, OfflineNotifierService::class.java)
                application.stopService(serviceIntent)
                WorkManagerHelper.stopEmailWorker(application)
            } else if (isActive.value) {
                val serviceIntent = Intent(application, OfflineNotifierService::class.java)
                application.startForegroundService(serviceIntent)
            }
        }
    }
    fun onShowTestButtonToggled(show: Boolean) { viewModelScope.launch { repository.saveShowTestButton(show) } }
    fun onSelectedAppsChanged(apps: String) { viewModelScope.launch { repository.saveSelectedApps(apps) } }

    fun onActiveToggled(active: Boolean) {
        viewModelScope.launch { 
            if (active && repository.autoStopOnWifiFlow.first() && NetworkStateHelper.isWifiConnected(application)) {
                repository.saveIsActive(false)
                clearPendingQueue()
                WorkManagerHelper.stopEmailWorker(application)
                return@launch
            }
            repository.saveIsActive(active)
            clearPendingQueue()
            if (active) {
                val serviceIntent = Intent(application, OfflineNotifierService::class.java)
                application.startForegroundService(serviceIntent)
            } else {
                val serviceIntent = Intent(application, OfflineNotifierService::class.java)
                application.stopService(serviceIntent)
                WorkManagerHelper.stopEmailWorker(application)
            }
        }
    }

    private fun restartWorkerIfActive() {
        if (isActive.value) {
            val serviceIntent = Intent(application, OfflineNotifierService::class.java)
            application.startForegroundService(serviceIntent)
        }
    }

    fun testSendNow() {
        WorkManagerHelper.triggerImmediateSend(application)
    }

    fun clearQueue() {
        viewModelScope.launch {
            clearPendingQueue()
        }
    }

    private suspend fun clearPendingQueue() {
        val events = db.eventDao().getAllEvents()
        events.forEach { event ->
            com.offlinenotifier.app.utils.MediaExtractorHelper.deleteMediaFile(event.imagePath)
            com.offlinenotifier.app.utils.MediaExtractorHelper.deleteMediaFile(event.audioPath)
        }
        db.eventDao().clearAll()
    }

    fun syncAndCheckShiftNow() {
        com.offlinenotifier.app.shift.ShiftScheduleManager.recalculateAndSchedule(application)
    }
}
