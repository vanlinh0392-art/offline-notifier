package com.offlinenotifier.app.receivers

enum class PhoneCallState {
    RINGING,
    OFFHOOK,
    IDLE,
    OTHER
}

data class PendingMissedCallLookup(
    val startedAtMillis: Long,
    val incomingNumber: String?
)

class MissedCallStateTracker(
    private val maxTrackedCallMillis: Long = 5 * 60 * 1000L
) {
    private var ringingCall: RingingCall? = null

    @Synchronized
    fun onStateChanged(
        state: PhoneCallState,
        incomingNumber: String?,
        nowMillis: Long,
        deviceNotInUse: Boolean
    ): PendingMissedCallLookup? {
        clearStaleCall(nowMillis)

        return when (state) {
            PhoneCallState.RINGING -> {
                handleRinging(incomingNumber, nowMillis, deviceNotInUse)
                null
            }
            PhoneCallState.OFFHOOK -> {
                ringingCall = ringingCall?.copy(wasAnswered = true)
                null
            }
            PhoneCallState.IDLE -> {
                val completedCall = ringingCall
                ringingCall = null
                if (completedCall != null && !completedCall.wasAnswered) {
                    PendingMissedCallLookup(
                        startedAtMillis = completedCall.startedAtMillis,
                        incomingNumber = completedCall.incomingNumber
                    )
                } else {
                    null
                }
            }
            PhoneCallState.OTHER -> null
        }
    }

    private fun handleRinging(incomingNumber: String?, nowMillis: Long, deviceNotInUse: Boolean) {
        val normalizedIncomingNumber = incomingNumber.takeUnless { it.isNullOrBlank() }
        val currentCall = ringingCall

        if (currentCall != null) {
            if (currentCall.incomingNumber.isNullOrBlank() && !normalizedIncomingNumber.isNullOrBlank()) {
                ringingCall = currentCall.copy(incomingNumber = normalizedIncomingNumber)
            }
            return
        }

        if (deviceNotInUse) {
            ringingCall = RingingCall(
                startedAtMillis = nowMillis,
                incomingNumber = normalizedIncomingNumber
            )
        }
    }

    private fun clearStaleCall(nowMillis: Long) {
        val currentCall = ringingCall ?: return
        if (nowMillis - currentCall.startedAtMillis > maxTrackedCallMillis) {
            ringingCall = null
        }
    }

    private data class RingingCall(
        val startedAtMillis: Long,
        val incomingNumber: String?,
        val wasAnswered: Boolean = false
    )
}
