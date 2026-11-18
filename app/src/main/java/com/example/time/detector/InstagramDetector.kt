package com.example.time.detector

/**
 * Pure Kotlin detection engine for Instagram Reels.
 *
 * Implements strict, confidence-based detection rules:
 * - Rule A: Reels viewer identification (`clips_viewer_view_pager` with non-empty bounds & visible).
 * - Rule B: Reels tab selection verification (`clips_tab` MUST have `isSelected == true`).
 * - Rule C: Stories protection (Explicit safeguard for `reel_*` and `reel_viewer`).
 * - Rule D: Fail-safe unknown handling (Ambiguous hierarchies yield UNKNOWN; no blocking action).
 *
 * ZERO dependencies on Android framework or UI classes. Fully unit testable on JVM.
 */
class InstagramDetector {

    /**
     * Evaluates a sanitized UI tree representation.
     *
     * @param packageName The package emitting the accessibility event.
     * @param root The sanitized root of the UI tree.
     * @return A typed [DetectionDecision] containing the verdict and reasoning.
     */
    fun evaluate(packageName: String?, root: UiNodeSnapshot?): DetectionDecision {
        // Strict package gating: Reject anything not originating from official Instagram
        if (packageName != InstagramSignatures.INSTAGRAM_PACKAGE) {
            return DetectionDecision.none(
                reason = "Event originated from external package '$packageName', ignored.",
                confidence = 1.0f
            )
        }

        // Null root check
        if (root == null) {
            return DetectionDecision.unknown("UI root snapshot is null.")
        }

        // Empty hierarchy check
        if (root.nodeCount == 0) {
            return DetectionDecision.unknown("UI hierarchy has 0 nodes.")
        }

        // RULE C: Stories Protection
        // If a Story viewer is active, explicitly protect it from being classified as Reels.
        val storyViewerNode = root.findFirst { node ->
            InstagramSignatures.matchesId(node.id, InstagramSignatures.ID_REEL_VIEWER) ||
                    (node.id?.let { InstagramSignatures.isStoryIdentifier(it) } == true &&
                            node.isVisibleToUser &&
                            !node.bounds.isEmpty)
        }
        val reelsViewerNode = root.findFirst { node ->
            InstagramSignatures.matchesId(node.id, InstagramSignatures.ID_CLIPS_VIEWER_PAGER)
        }

        if (storyViewerNode != null && reelsViewerNode == null) {
            return DetectionDecision.none(
                reason = "Instagram Stories viewer active ('${storyViewerNode.id}'). Protected.",
                signature = storyViewerNode.id,
                confidence = 1.0f
            )
        }

        // RULE A: Reels Viewer Check
        if (reelsViewerNode != null) {
            // Must be visible to user and have non-empty bounds
            if (reelsViewerNode.isVisibleToUser && !reelsViewerNode.bounds.isEmpty) {
                return DetectionDecision.reelsViewer(
                    signature = reelsViewerNode.id ?: InstagramSignatures.ID_CLIPS_VIEWER_PAGER,
                    confidence = 1.0f
                )
            } else {
                // Node exists in hierarchy (e.g. cached/recycled off-screen), but is not currently visible
                return DetectionDecision.none(
                    reason = "Reels viewer node found but not visible to user or bounds are empty.",
                    signature = reelsViewerNode.id,
                    confidence = 0.9f
                )
            }
        }

        // RULE B: Reels Bottom Navigation Tab Check
        val clipsTabNode = root.findFirst { node ->
            InstagramSignatures.matchesId(node.id, InstagramSignatures.ID_CLIPS_TAB)
        }

        if (clipsTabNode != null) {
            if (clipsTabNode.isSelected && clipsTabNode.isVisibleToUser) {
                return DetectionDecision.reelsTabSelected(
                    signature = clipsTabNode.id ?: InstagramSignatures.ID_CLIPS_TAB,
                    confidence = 1.0f
                )
            } else {
                // Tab exists in bottom bar but is NOT currently selected (e.g., Home, Search, Profile is active)
                return DetectionDecision.none(
                    reason = "Reels tab present in navigation bar but isSelected = false.",
                    signature = clipsTabNode.id,
                    confidence = 0.95f
                )
            }
        }

        // Check for other legitimate Instagram sections
        val knownLegitimateNodes = root.findAll { node ->
            InstagramSignatures.matchesId(node.id, InstagramSignatures.ID_FEED_TAB) ||
                    InstagramSignatures.matchesId(node.id, InstagramSignatures.ID_SEARCH_TAB) ||
                    InstagramSignatures.matchesId(node.id, InstagramSignatures.ID_PROFILE_TAB) ||
                    InstagramSignatures.matchesId(node.id, InstagramSignatures.ID_ACTION_BAR_INBOX_BUTTON) ||
                    InstagramSignatures.matchesId(node.id, InstagramSignatures.ID_DIRECT_TAB) ||
                    InstagramSignatures.matchesId(node.id, InstagramSignatures.ID_INBOX_TAB)
        }

        if (knownLegitimateNodes.isNotEmpty()) {
            return DetectionDecision.none(
                reason = "Standard Instagram view active with verified legitimate navigation nodes.",
                signature = knownLegitimateNodes.first().id,
                confidence = 0.9f
            )
        }

        // RULE D: Fail-Safe for Unknown Screens
        // If neither Reels nor standard navigation anchors can be matched, do NOT guess or block!
        return DetectionDecision.unknown(
            reason = "Ambiguous screen layout with no recognized Instagram navigation signatures."
        )
    }
}

