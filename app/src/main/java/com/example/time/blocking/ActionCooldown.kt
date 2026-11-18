package com.example.time.blocking

/**
 * Thread-safe rate limiter, debouncer, and safeguard against infinite navigation loops.
 *
 * Implements:
 * 1. Cooldown throttling: Prevents executing back-to-back actions during rapid event bursts
 *    (e.g., multiple TYPE_WINDOW_CONTENT_CHANGED events in under 1 second).
 * 2. Consecutive attempt bounding: Halts repeated identical actions if the screen fails to transition,
 *    preventing endless redirect loops or rapid-fire back key generation.
 * 3. Time-based recovery: Automatically clears loop counters once the UI settles.
 */
class ActionCooldown(
    var cooldownMillis: Long = DEFAULT_COOLDOWN_MS,
    var maxConsecutiveAttempts: Int = DEFAULT_MAX_CONSECUTIVE_ATTEMPTS
) {
    private var lastActionTime: Long = 0L
    private var lastActionName: String? = null
    private var consecutiveAttempts: Int = 0

    @Synchronized
    fun canExecuteAction(actionName: String, currentTime: Long = System.currentTimeMillis()): Boolean {
        val elapsed = currentTime - lastActionTime

        // Check 1: Mandatory cooldown period
        if (elapsed < cooldownMillis) {
            return false
        }

        // Check 2: Loop prevention for repeated identical actions
        if (actionName == lastActionName && elapsed < (cooldownMillis * 3)) {
            if (consecutiveAttempts >= maxConsecutiveAttempts) {
                return false
            }
        }

        return true
    }

    @Synchronized
    fun recordAction(actionName: String, currentTime: Long = System.currentTimeMillis()) {
        val elapsed = currentTime - lastActionTime
        if (actionName == lastActionName && elapsed < (cooldownMillis * 3)) {
            consecutiveAttempts++
        } else {
            consecutiveAttempts = 1
        }
        lastActionName = actionName
        lastActionTime = currentTime
    }

    @Synchronized
    fun reset() {
        lastActionTime = 0L
        lastActionName = null
        consecutiveAttempts = 0
    }

    @Synchronized
    fun getConsecutiveAttempts(): Int = consecutiveAttempts

    @Synchronized
    fun getLastActionTime(): Long = lastActionTime

    companion object {
        const val DEFAULT_COOLDOWN_MS = 1200L
        const val DEFAULT_MAX_CONSECUTIVE_ATTEMPTS = 2
    }
}

