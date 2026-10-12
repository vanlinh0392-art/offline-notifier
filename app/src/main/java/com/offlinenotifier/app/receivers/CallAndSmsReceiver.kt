package com.offlinenotifier.app.receivers

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.os.Bundle
import android.provider.CallLog
import android.telephony.SmsMessage
import android.telephony.TelephonyManager
import android.util.Log
import com.offlinenotifier.app.data.SettingsRepository
import com.offlinenotifier.app.data.database.AppDatabase
import com.offlinenotifier.app.data.database.EventEntity
import com.offlinenotifier.app.services.OfflineNotifierService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class CallAndSmsReceiver : BroadcastReceiver() {

    private val tag = "CallSmsReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        if (!OfflineNotifierService.isRunning) return

        when (intent.action) {
            SMS_RECEIVED_ACTION -> {
                if (com.offlinenotifier.app.utils.DeviceStateHelper.isDeviceLockedOrScreenOff(context)) {
                    handleSmsReceived(context, intent)
                } else {
                    Log.d(tag, "Screen is ON and device is UNLOCKED. Ignoring SMS event.")
                }
            }
            TelephonyManager.ACTION_PHONE_STATE_CHANGED -> handlePhoneStateChanged(context, intent)
        }
    }

    private fun handleSmsReceived(context: Context, intent: Intent) {
        val bundle: Bundle = intent.extras ?: return
        val pdus = bundle.get("pdus") as? Array<*> ?: return
        val format = bundle.getString("format")
        if (pdus.isEmpty()) return

        var sender = "Unknown Sender"
        val fullMessage = StringBuilder()

        for (pdu in pdus) {
            val pduBytes = pdu as? ByteArray ?: continue
            val sms = if (format != null) {
                SmsMessage.createFromPdu(pduBytes, format)
            } else {
                @Suppress("DEPRECATION")
                SmsMessage.createFromPdu(pduBytes)
            }
            sender = sms.displayOriginatingAddress ?: sender
            fullMessage.append(sms.displayMessageBody ?: "")
        }

        val message = fullMessage.toString().ifBlank { "No Message" }
        Log.d(tag, "SMS received from: $sender - Full Message (${pdus.size} parts): $message")

        val event = EventEntity(
            type = "SMS",
            title = "Tin nh\u1eafn t\u1eeb: $sender",
            body = message,
            timestamp = System.currentTimeMillis()
        )

        enqueueInsert(context, event)
    }

    private fun handlePhoneStateChanged(context: Context, intent: Intent) {
        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE).toPhoneCallState()
        val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
        val now = System.currentTimeMillis()
        val deviceNotInUse = com.offlinenotifier.app.utils.DeviceStateHelper.isDeviceLockedOrScreenOff(context)

        val lookup = missedCallStateTracker.onStateChanged(
            state = state,
            incomingNumber = incomingNumber,
            nowMillis = now,
            deviceNotInUse = deviceNotInUse
        )

        Log.d(
            tag,
            "Phone state=$state incoming=$incomingNumber deviceNotInUse=$deviceNotInUse lookup=${lookup != null}"
        )

        if (lookup != null) {
            enqueueMissedCallConfirmation(context, lookup)
        }
    }

    private fun enqueueMissedCallConfirmation(context: Context, lookup: PendingMissedCallLookup) {
        val pendingResult = goAsync()
        val appContext = context.applicationContext

        receiverScope.launch {
            try {
                delay(CALL_LOG_SETTLE_DELAY_MILLIS)
                val missedCall = findLatestMissedCall(appContext, lookup)
                if (missedCall == null) {
                    Log.d(tag, "No matching MISSED_TYPE call-log row found.")
                    return@launch
                }

                val name = missedCall.cachedName?.takeUnless { it.isBlank() }
                val number = missedCall.number?.takeUnless { it.isBlank() }
                    ?: lookup.incomingNumber?.takeUnless { it.isBlank() }

                val callerDisplay = when {
                    !name.isNullOrBlank() && !number.isNullOrBlank() -> "$name ($number)"
                    !name.isNullOrBlank() -> name
                    !number.isNullOrBlank() -> number
                    else -> UNKNOWN_NUMBER_LABEL
                }

                val event = EventEntity(
                    type = "CALL",
                    title = "Cu\u1ed9c g\u1ecdi nh\u1ee1 t\u1eeb: $callerDisplay",
                    body = if (!number.isNullOrBlank()) "S\u1ed1 \u0111i\u1ec7n tho\u1ea1i: $number" else "\u0110\u00e3 x\u00e1c nh\u1eadn l\u00e0 cu\u1ed9c g\u1ecdi nh\u1ee1 t\u1eeb nh\u1eadt k\u00fd cu\u1ed9c g\u1ecdi.",
                    timestamp = missedCall.timestampMillis
                )

                insertEventToDb(appContext, event)
            } catch (e: SecurityException) {
                Log.e(tag, "READ_CALL_LOG permission missing while confirming missed call.", e)
            } catch (e: Exception) {
                Log.e(tag, "Failed to confirm missed call.", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun enqueueInsert(context: Context, event: EventEntity) {
        val pendingResult = goAsync()
        val appContext = context.applicationContext

        receiverScope.launch {
            try {
                insertEventToDb(appContext, event)
            } catch (e: Exception) {
                Log.e(tag, "Failed to insert offline event.", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun insertEventToDb(context: Context, event: EventEntity) {
        val repository = SettingsRepository(context.applicationContext)
        val shouldCaptureSms = repository.captureSmsFlow.first()
        val shouldCaptureCalls = repository.captureCallsFlow.first()

        if (event.type == "SMS" && !shouldCaptureSms) return
        if (event.type == "CALL" && !shouldCaptureCalls) return

        val db = AppDatabase.getDatabase(context.applicationContext)
        db.eventDao().insertEvent(event)
    }

    private fun findLatestMissedCall(
        context: Context,
        lookup: PendingMissedCallLookup
    ): MissedCallRecord? {
        val startedAfter = (lookup.startedAtMillis - CALL_LOG_LOOKBACK_MARGIN_MILLIS).coerceAtLeast(0L)
        val endedBefore = System.currentTimeMillis() + CALL_LOG_FUTURE_MARGIN_MILLIS
        val selection = "${CallLog.Calls.DATE} >= ? AND " +
            "${CallLog.Calls.DATE} <= ? AND " +
            "${CallLog.Calls.TYPE} = ?"
        val selectionArgs = arrayOf(
            startedAfter.toString(),
            endedBefore.toString(),
            CallLog.Calls.MISSED_TYPE.toString()
        )

        val cursor = context.contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            arrayOf(
                CallLog.Calls.NUMBER,
                CallLog.Calls.CACHED_NAME,
                CallLog.Calls.DATE
            ),
            selection,
            selectionArgs,
            "${CallLog.Calls.DATE} DESC"
        ) ?: return null

        cursor.use {
            while (it.moveToNext()) {
                val number = it.getNullableString(CallLog.Calls.NUMBER)
                if (!numbersMatch(lookup.incomingNumber, number)) continue

                return MissedCallRecord(
                    number = number,
                    cachedName = it.getNullableString(CallLog.Calls.CACHED_NAME),
                    timestampMillis = it.getLongOrNull(CallLog.Calls.DATE) ?: System.currentTimeMillis()
                )
            }
        }

        return null
    }

    private fun String?.toPhoneCallState(): PhoneCallState = when (this) {
        TelephonyManager.EXTRA_STATE_RINGING -> PhoneCallState.RINGING
        TelephonyManager.EXTRA_STATE_OFFHOOK -> PhoneCallState.OFFHOOK
        TelephonyManager.EXTRA_STATE_IDLE -> PhoneCallState.IDLE
        else -> PhoneCallState.OTHER
    }

    private fun numbersMatch(expected: String?, actual: String?): Boolean {
        val normalizedExpected = expected.normalizedPhoneNumber()
        if (normalizedExpected.isBlank()) return true

        val normalizedActual = actual.normalizedPhoneNumber()
        if (normalizedActual.isBlank()) return false

        return normalizedExpected.endsWith(normalizedActual) || normalizedActual.endsWith(normalizedExpected)
    }

    private fun String?.normalizedPhoneNumber(): String =
        orEmpty().filter { it.isDigit() }

    private fun Cursor.getNullableString(columnName: String): String? {
        val columnIndex = getColumnIndex(columnName)
        return if (columnIndex >= 0 && !isNull(columnIndex)) getString(columnIndex) else null
    }

    private fun Cursor.getLongOrNull(columnName: String): Long? {
        val columnIndex = getColumnIndex(columnName)
        return if (columnIndex >= 0 && !isNull(columnIndex)) getLong(columnIndex) else null
    }

    private data class MissedCallRecord(
        val number: String?,
        val cachedName: String?,
        val timestampMillis: Long
    )

    companion object {
        private const val SMS_RECEIVED_ACTION = "android.provider.Telephony.SMS_RECEIVED"
        private const val CALL_LOG_SETTLE_DELAY_MILLIS = 3_000L
        private const val CALL_LOG_LOOKBACK_MARGIN_MILLIS = 60_000L
        private const val CALL_LOG_FUTURE_MARGIN_MILLIS = 10_000L
        private const val UNKNOWN_NUMBER_LABEL = "S\u1ed1 \u1ea8n"

        private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private val missedCallStateTracker = MissedCallStateTracker()
    }
}
