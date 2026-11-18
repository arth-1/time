package com.example.time.data

import com.example.time.detector.UiNodeSnapshot

/**
 * In-memory state holder for developer diagnostic snapshots.
 *
 * Implements the user workflow:
 * 1. Open ReelGuard -> Tap "Capture next Instagram screen".
 * 2. Switch to Instagram -> Navigate to the screen of interest.
 * 3. Accessibility Service captures the structural hierarchy.
 * 4. Return to ReelGuard to inspect sanitized JSON metadata.
 *
 * NOTE: Snapshots are held in memory during the app lifecycle and are
 * never persisted to disk or transmitted over the network.
 */
object DiagnosticCaptureRepository {

    enum class CaptureState {
        IDLE,
        AWAITING_CAPTURE,
        CAPTURED
    }

    @Volatile
    var state: CaptureState = CaptureState.IDLE
        private set

    @Volatile
    var capturedSnapshot: UiNodeSnapshot? = null
        private set

    @Volatile
    var capturedJson: String? = null
        private set

    @Volatile
    var capturedTimestamp: Long = 0L
        private set

    @Volatile
    var lastError: String? = null
        private set

    private val listeners = mutableListOf<() -> Unit>()

    @Synchronized
    fun addListener(listener: () -> Unit) {
        listeners.add(listener)
    }

    @Synchronized
    fun removeListener(listener: () -> Unit) {
        listeners.remove(listener)
    }

    private fun notifyChanged() {
        val copy = synchronized(this) { listeners.toList() }
        copy.forEach { it.invoke() }
    }

    @Synchronized
    fun armCapture() {
        state = CaptureState.AWAITING_CAPTURE
        lastError = null
        notifyChanged()
    }

    @Synchronized
    fun disarmCapture() {
        if (state == CaptureState.AWAITING_CAPTURE) {
            state = CaptureState.IDLE
            notifyChanged()
        }
    }

    @Synchronized
    fun deliverSnapshot(snapshot: UiNodeSnapshot) {
        if (state == CaptureState.AWAITING_CAPTURE) {
            capturedSnapshot = snapshot
            capturedJson = snapshot.toJson()
            capturedTimestamp = System.currentTimeMillis()
            state = CaptureState.CAPTURED
            notifyChanged()
        }
    }

    @Synchronized
    fun clear() {
        state = CaptureState.IDLE
        capturedSnapshot = null
        capturedJson = null
        capturedTimestamp = 0L
        lastError = null
        notifyChanged()
    }
}

