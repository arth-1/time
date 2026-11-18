package com.example.time.blocking

import com.example.time.data.SettingsProvider
import com.example.time.detector.DetectionDecision
import com.example.time.detector.DetectionType
import com.example.time.detector.InstagramDetector
import com.example.time.detector.InstagramSignatures
import com.example.time.detector.UiNodeSnapshot

/**
 * Coordinates detection analysis with blocking action execution.
 *
 * Responsibilities:
 * 1. Delegates UI inspection to [InstagramDetector].
 * 2. Enforces user preference toggles (only blocks when enabled).
 * 3. Enforces [ActionCooldown] to prevent duplicate/loop actions.
 * 4. Executes verified navigation via [NavigationExecutor].
 * 5. Updates local statistics and diagnostic state.
 */
class ReelsBlockHandler(
    private val detector: InstagramDetector,
    private val cooldown: ActionCooldown,
    private val navigationExecutor: NavigationExecutor,
    private val prefs: SettingsProvider
) {

    /**
     * Handles an incoming accessibility event evaluation.
     *
     * @param packageName Originating application package.
     * @param rootSnapshot Sanitized snapshot of the current view hierarchy.
     * @return The resulting [DetectionDecision].
     */
    fun handleEvent(packageName: String?, rootSnapshot: UiNodeSnapshot?): DetectionDecision {
        val decision = detector.evaluate(packageName, rootSnapshot)

        // Record detection metadata
        prefs.lastDetectionType = decision.type.name
        prefs.detectionCount++

        // If screen is confirmed legitimate, reset loop counters
        if (decision.type == DetectionType.NONE) {
            cooldown.reset()
            return decision
        }

        // If ambiguous or no action required, do nothing
        if (!decision.requiresAction) {
            return decision
        }

        // Check if blocking is active
        if (!prefs.isBlockingEnabled) {
            prefs.lastBlockingAction = "None (Blocking Disabled)"
            prefs.lastActionResult = "Skipped"
            return decision
        }

        // Determine action name for cooldown tracking
        val actionName = when (decision.type) {
            DetectionType.REELS_TAB_SELECTED -> ACTION_REDIRECT_TAB
            DetectionType.REELS_VIEWER -> ACTION_BACK
            else -> "UNKNOWN"
        }

        // Check cooldown and loop guard
        if (!cooldown.canExecuteAction(actionName)) {
            prefs.lastBlockingAction = "$actionName (Throttled)"
            prefs.lastActionResult = "Cooldown Active"
            return decision
        }

        // Execute action based on detection type
        when (decision.type) {
            DetectionType.REELS_TAB_SELECTED -> {
                // Priority: Redirect to DM Inbox, fallback to Home Feed
                val candidateList = InstagramSignatures.SAFE_INBOX_CANDIDATES +
                        InstagramSignatures.SAFE_HOME_CANDIDATES

                val success = navigationExecutor.clickNodeWithCandidates(candidateList)
                if (success) {
                    cooldown.recordAction(actionName)
                    prefs.incrementBlockedCountToday()
                    prefs.successCount++
                    prefs.lastBlockingAction = "Redirected away from Reels tab"
                    prefs.lastActionResult = "Success"
                } else {
                    // Safe tab not found or not clickable, attempt fallback back action
                    val backFallbackSuccess = navigationExecutor.performGlobalBack()
                    if (backFallbackSuccess) {
                        cooldown.recordAction(actionName)
                        prefs.incrementBlockedCountToday()
                        prefs.successCount++
                        prefs.lastBlockingAction = "Fallback Back from Reels tab"
                        prefs.lastActionResult = "Success (Fallback)"
                    } else {
                        prefs.failureCount++
                        prefs.lastBlockingAction = "Attempted Reels tab redirect"
                        prefs.lastActionResult = "Failed: Safe destination not clickable"
                    }
                }
            }

            DetectionType.REELS_VIEWER -> {
                // Return user to previous screen via Back
                val success = navigationExecutor.performGlobalBack()
                if (success) {
                    cooldown.recordAction(actionName)
                    prefs.incrementBlockedCountToday()
                    prefs.successCount++
                    prefs.lastBlockingAction = "Global Back from Reels viewer"
                    prefs.lastActionResult = "Success"
                } else {
                    prefs.failureCount++
                    prefs.lastBlockingAction = "Attempted Global Back"
                    prefs.lastActionResult = "Failed: Global Back rejected"
                }
            }

            else -> {
                // No action
            }
        }

        return decision
    }

    companion object {
        const val ACTION_REDIRECT_TAB = "REDIRECT_TAB"
        const val ACTION_BACK = "BACK"
    }
}
