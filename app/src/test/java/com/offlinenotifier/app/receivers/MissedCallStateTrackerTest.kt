package com.offlinenotifier.app.receivers

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class MissedCallStateTrackerTest {

    @Test
    fun ringingThenIdleWhileDeviceNotInUseProducesLookup() {
        val tracker = MissedCallStateTracker()

        val ringingResult = tracker.onStateChanged(
            state = PhoneCallState.RINGING,
            incomingNumber = "0901234567",
            nowMillis = 1_000L,
            deviceNotInUse = true
        )
        val idleResult = tracker.onStateChanged(
            state = PhoneCallState.IDLE,
            incomingNumber = null,
            nowMillis = 10_000L,
            deviceNotInUse = true
        )

        assertNull(ringingResult)
        assertNotNull(idleResult)
        assertEquals(1_000L, idleResult?.startedAtMillis)
        assertEquals("0901234567", idleResult?.incomingNumber)
    }

    @Test
    fun answeredCallDoesNotProduceLookup() {
        val tracker = MissedCallStateTracker()

        tracker.onStateChanged(
            state = PhoneCallState.RINGING,
            incomingNumber = "0901234567",
            nowMillis = 1_000L,
            deviceNotInUse = true
        )
        tracker.onStateChanged(
            state = PhoneCallState.OFFHOOK,
            incomingNumber = "0901234567",
            nowMillis = 5_000L,
            deviceNotInUse = true
        )
        val idleResult = tracker.onStateChanged(
            state = PhoneCallState.IDLE,
            incomingNumber = null,
            nowMillis = 10_000L,
            deviceNotInUse = true
        )

        assertNull(idleResult)
    }

    @Test
    fun ringingWhileDeviceActiveDoesNotTrackCall() {
        val tracker = MissedCallStateTracker()

        tracker.onStateChanged(
            state = PhoneCallState.RINGING,
            incomingNumber = "0901234567",
            nowMillis = 1_000L,
            deviceNotInUse = false
        )
        val idleResult = tracker.onStateChanged(
            state = PhoneCallState.IDLE,
            incomingNumber = null,
            nowMillis = 10_000L,
            deviceNotInUse = false
        )

        assertNull(idleResult)
    }

    @Test
    fun duplicateRingingBroadcastsProduceSingleLookup() {
        val tracker = MissedCallStateTracker()

        tracker.onStateChanged(
            state = PhoneCallState.RINGING,
            incomingNumber = null,
            nowMillis = 1_000L,
            deviceNotInUse = true
        )
        tracker.onStateChanged(
            state = PhoneCallState.RINGING,
            incomingNumber = "0901234567",
            nowMillis = 2_000L,
            deviceNotInUse = true
        )
        val firstIdleResult = tracker.onStateChanged(
            state = PhoneCallState.IDLE,
            incomingNumber = null,
            nowMillis = 10_000L,
            deviceNotInUse = true
        )
        val secondIdleResult = tracker.onStateChanged(
            state = PhoneCallState.IDLE,
            incomingNumber = null,
            nowMillis = 11_000L,
            deviceNotInUse = true
        )

        assertNotNull(firstIdleResult)
        assertEquals(1_000L, firstIdleResult?.startedAtMillis)
        assertEquals("0901234567", firstIdleResult?.incomingNumber)
        assertNull(secondIdleResult)
    }
}
