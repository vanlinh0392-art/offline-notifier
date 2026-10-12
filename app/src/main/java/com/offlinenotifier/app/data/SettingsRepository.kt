package com.offlinenotifier.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    
    companion object {
        val SENDER_EMAIL_KEY = stringPreferencesKey("sender_email")
        val RECIPIENT_EMAIL_KEY = stringPreferencesKey("recipient_email")
        val EMAIL_PASS_KEY = stringPreferencesKey("sender_email_password")
        val INTERVAL_INDEX_KEY = intPreferencesKey("interval_index")
        val CUSTOM_INTERVAL_KEY = intPreferencesKey("custom_interval_minutes")
        val IS_ACTIVE_KEY = booleanPreferencesKey("is_active")
        val CAPTURE_SMS_KEY = booleanPreferencesKey("capture_sms")
        val CAPTURE_CALLS_KEY = booleanPreferencesKey("capture_calls")
        val CAPTURE_APPS_KEY = booleanPreferencesKey("capture_apps")
        val AUTO_STOP_ON_WIFI_KEY = booleanPreferencesKey("auto_stop_on_wifi")
        val SHOW_TEST_BUTTON_KEY = booleanPreferencesKey("show_test_button")
        val SELECTED_APPS_KEY = stringPreferencesKey("selected_apps") // comma-separated package names
        val SHORTCUT_TOKEN_KEY = stringPreferencesKey("shortcut_token")

        // Smart Notification Filters
        val FILTER_ONGOING_KEY = booleanPreferencesKey("filter_ongoing_notifications")
        val KEYWORD_BLACKLIST_KEY = stringPreferencesKey("keyword_blacklist")

        // Auto schedule by shift
        val SCHEDULE_ENABLED_KEY = booleanPreferencesKey("schedule_enabled")
        val SELECTED_CREW_KEY = stringPreferencesKey("selected_crew") // "A", "B", "C"
        val DAY_SHIFT_START_HOUR_KEY = intPreferencesKey("day_shift_start_hour")
        val DAY_SHIFT_START_MIN_KEY = intPreferencesKey("day_shift_start_min")
        val DAY_SHIFT_END_HOUR_KEY = intPreferencesKey("day_shift_end_hour")
        val DAY_SHIFT_END_MIN_KEY = intPreferencesKey("day_shift_end_min")
        val NIGHT_SHIFT_START_HOUR_KEY = intPreferencesKey("night_shift_start_hour")
        val NIGHT_SHIFT_START_MIN_KEY = intPreferencesKey("night_shift_start_min")
        val NIGHT_SHIFT_END_HOUR_KEY = intPreferencesKey("night_shift_end_hour")
        val NIGHT_SHIFT_END_MIN_KEY = intPreferencesKey("night_shift_end_min")
    }

    val senderEmailFlow: Flow<String> = context.dataStore.data.map { it[SENDER_EMAIL_KEY] ?: "" }
    val recipientEmailFlow: Flow<String> = context.dataStore.data.map { it[RECIPIENT_EMAIL_KEY] ?: "" }
    val emailPassFlow: Flow<String> = context.dataStore.data.map { it[EMAIL_PASS_KEY] ?: "" }
    val intervalIndexFlow: Flow<Int> = context.dataStore.data.map { it[INTERVAL_INDEX_KEY] ?: 0 }
    val customIntervalFlow: Flow<Int> = context.dataStore.data.map { it[CUSTOM_INTERVAL_KEY] ?: 5 }
    val isActiveFlow: Flow<Boolean> = context.dataStore.data.map { it[IS_ACTIVE_KEY] ?: false }
    val captureSmsFlow: Flow<Boolean> = context.dataStore.data.map { it[CAPTURE_SMS_KEY] ?: true }
    val captureCallsFlow: Flow<Boolean> = context.dataStore.data.map { it[CAPTURE_CALLS_KEY] ?: true }
    val captureAppsFlow: Flow<Boolean> = context.dataStore.data.map { it[CAPTURE_APPS_KEY] ?: true }
    val autoStopOnWifiFlow: Flow<Boolean> = context.dataStore.data.map { it[AUTO_STOP_ON_WIFI_KEY] ?: false }
    val showTestButtonFlow: Flow<Boolean> = context.dataStore.data.map { it[SHOW_TEST_BUTTON_KEY] ?: true }
    val selectedAppsFlow: Flow<String> = context.dataStore.data.map { it[SELECTED_APPS_KEY] ?: "" }
    val shortcutTokenFlow: Flow<String> = context.dataStore.data.map { it[SHORTCUT_TOKEN_KEY] ?: "" }

    // Smart filter flows
    val filterOngoingFlow: Flow<Boolean> = context.dataStore.data.map { it[FILTER_ONGOING_KEY] ?: true }
    val keywordBlacklistFlow: Flow<String> = context.dataStore.data.map { it[KEYWORD_BLACKLIST_KEY] ?: "qu\u1ea3ng c\u00e1o, khuy\u1ebfn m\u00e3i, \u01b0u \u0111\u00e3i, voucher, m\u00e3 gi\u1ea3m gi\u00e1, otp, sao l\u01b0u, \u0111\u1ed3ng b\u1ed9" }

    // Shift schedule flows
    val scheduleEnabledFlow: Flow<Boolean> = context.dataStore.data.map { it[SCHEDULE_ENABLED_KEY] ?: false }
    val selectedCrewFlow: Flow<String> = context.dataStore.data.map { it[SELECTED_CREW_KEY] ?: "A" }
    val dayShiftStartHourFlow: Flow<Int> = context.dataStore.data.map { it[DAY_SHIFT_START_HOUR_KEY] ?: 6 }
    val dayShiftStartMinFlow: Flow<Int> = context.dataStore.data.map { it[DAY_SHIFT_START_MIN_KEY] ?: 0 }
    val dayShiftEndHourFlow: Flow<Int> = context.dataStore.data.map { it[DAY_SHIFT_END_HOUR_KEY] ?: 18 }
    val dayShiftEndMinFlow: Flow<Int> = context.dataStore.data.map { it[DAY_SHIFT_END_MIN_KEY] ?: 0 }
    val nightShiftStartHourFlow: Flow<Int> = context.dataStore.data.map { it[NIGHT_SHIFT_START_HOUR_KEY] ?: 18 }
    val nightShiftStartMinFlow: Flow<Int> = context.dataStore.data.map { it[NIGHT_SHIFT_START_MIN_KEY] ?: 0 }
    val nightShiftEndHourFlow: Flow<Int> = context.dataStore.data.map { it[NIGHT_SHIFT_END_HOUR_KEY] ?: 6 }
    val nightShiftEndMinFlow: Flow<Int> = context.dataStore.data.map { it[NIGHT_SHIFT_END_MIN_KEY] ?: 0 }

    suspend fun saveSenderEmail(email: String) { context.dataStore.edit { it[SENDER_EMAIL_KEY] = email } }
    suspend fun saveRecipientEmail(email: String) { context.dataStore.edit { it[RECIPIENT_EMAIL_KEY] = email } }
    suspend fun saveEmailPass(pass: String) { context.dataStore.edit { it[EMAIL_PASS_KEY] = pass } }
    suspend fun saveIntervalIndex(index: Int) { context.dataStore.edit { it[INTERVAL_INDEX_KEY] = index } }
    suspend fun saveCustomInterval(minutes: Int) { context.dataStore.edit { it[CUSTOM_INTERVAL_KEY] = minutes } }
    suspend fun saveIsActive(isActive: Boolean) { context.dataStore.edit { it[IS_ACTIVE_KEY] = isActive } }
    suspend fun saveCaptureSms(enabled: Boolean) { context.dataStore.edit { it[CAPTURE_SMS_KEY] = enabled } }
    suspend fun saveCaptureCallsEnabled(enabled: Boolean) { context.dataStore.edit { it[CAPTURE_CALLS_KEY] = enabled } }
    suspend fun saveCaptureApps(enabled: Boolean) { context.dataStore.edit { it[CAPTURE_APPS_KEY] = enabled } }
    suspend fun saveAutoStopOnWifi(enabled: Boolean) { context.dataStore.edit { it[AUTO_STOP_ON_WIFI_KEY] = enabled } }
    suspend fun saveShowTestButton(show: Boolean) { context.dataStore.edit { it[SHOW_TEST_BUTTON_KEY] = show } }
    suspend fun saveSelectedApps(apps: String) { context.dataStore.edit { it[SELECTED_APPS_KEY] = apps } }
    suspend fun saveFilterOngoing(enabled: Boolean) { context.dataStore.edit { it[FILTER_ONGOING_KEY] = enabled } }
    suspend fun saveKeywordBlacklist(keywords: String) { context.dataStore.edit { it[KEYWORD_BLACKLIST_KEY] = keywords } }

    // Shift schedule saving functions
    suspend fun saveScheduleEnabled(enabled: Boolean) { context.dataStore.edit { it[SCHEDULE_ENABLED_KEY] = enabled } }
    suspend fun saveSelectedCrew(crew: String) { context.dataStore.edit { it[SELECTED_CREW_KEY] = crew } }
    suspend fun saveDayShiftStartHour(hour: Int) { context.dataStore.edit { it[DAY_SHIFT_START_HOUR_KEY] = hour } }
    suspend fun saveDayShiftStartMin(min: Int) { context.dataStore.edit { it[DAY_SHIFT_START_MIN_KEY] = min } }
    suspend fun saveDayShiftEndHour(hour: Int) { context.dataStore.edit { it[DAY_SHIFT_END_HOUR_KEY] = hour } }
    suspend fun saveDayShiftEndMin(min: Int) { context.dataStore.edit { it[DAY_SHIFT_END_MIN_KEY] = min } }
    suspend fun saveNightShiftStartHour(hour: Int) { context.dataStore.edit { it[NIGHT_SHIFT_START_HOUR_KEY] = hour } }
    suspend fun saveNightShiftStartMin(min: Int) { context.dataStore.edit { it[NIGHT_SHIFT_START_MIN_KEY] = min } }
    suspend fun saveNightShiftEndHour(hour: Int) { context.dataStore.edit { it[NIGHT_SHIFT_END_HOUR_KEY] = hour } }
    suspend fun saveNightShiftEndMin(min: Int) { context.dataStore.edit { it[NIGHT_SHIFT_END_MIN_KEY] = min } }

    suspend fun getOrCreateShortcutToken(): String {
        var token = ""
        context.dataStore.edit { prefs ->
            val existing = prefs[SHORTCUT_TOKEN_KEY]
            token = if (existing.isNullOrBlank()) {
                UUID.randomUUID().toString().also { prefs[SHORTCUT_TOKEN_KEY] = it }
            } else {
                existing
            }
        }
        return token
    }
}
