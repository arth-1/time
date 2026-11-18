package com.example.time.detector

/**
 * Result types emitted by the InstagramDetector engine.
 */
enum class DetectionType {
    /**
     * Confirmed legitimate screen (Home, Stories, DMs, Profile, Explore, etc.).
     * No blocking action should be taken.
     */
    NONE,

    /**
     * Bottom navigation Reels tab is confirmed active and selected.
     * Triggers safe navigation to Inbox or Home.
     */
    REELS_TAB_SELECTED,

    /**
     * Full-screen Reels viewer is confirmed open and visible.
     * Triggers back navigation to return to previous context.
     */
    REELS_VIEWER,

    /**
     * UI hierarchy could not be definitively matched against known signatures.
     * Fail-safe behavior: DO NOTHING.
     */
    UNKNOWN
}

/**
 * Rich decision output containing confidence score, matched signature, and rationale.
 */
data class DetectionDecision(
    val type: DetectionType,
    val confidence: Float,
    val matchedSignature: String? = null,
    val reason: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    val requiresAction: Boolean
        get() = type == DetectionType.REELS_TAB_SELECTED || type == DetectionType.REELS_VIEWER

    companion object {
        fun none(reason: String, signature: String? = null, confidence: Float = 1.0f) =
            DetectionDecision(DetectionType.NONE, confidence, signature, reason)

        fun reelsTabSelected(signature: String = InstagramSignatures.ID_CLIPS_TAB, confidence: Float = 1.0f) =
            DetectionDecision(
                DetectionType.REELS_TAB_SELECTED,
                confidence,
                signature,
                "Instagram Reels bottom navigation tab is selected."
            )

        fun reelsViewer(signature: String = InstagramSignatures.ID_CLIPS_VIEWER_PAGER, confidence: Float = 1.0f) =
            DetectionDecision(
                DetectionType.REELS_VIEWER,
                confidence,
                signature,
                "Full-screen Instagram Reels viewer is active and visible."
            )

        fun unknown(reason: String) =
            DetectionDecision(DetectionType.UNKNOWN, 0.0f, null, reason)
    }
}

