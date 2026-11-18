package com.example.time.accessibility

/**
 * Configuration constants for the ReelGuard Accessibility Service.
 */
object AccessibilityConfiguration {
    /**
     * Notification timeout in milliseconds for debouncing incoming accessibility events.
     */
    const val NOTIFICATION_TIMEOUT_MS = 100L

    /**
     * Maximum depth to traverse when converting AccessibilityNodeInfo trees to UiNodeSnapshot.
     * Prevents stack overflows and excessive processing on unusually deep UI trees.
     */
    const val MAX_HIERARCHY_DEPTH = 14

    /**
     * Maximum total nodes to inspect in a single accessibility event.
     * Guards against performance degradation and memory thrashing.
     */
    const val MAX_NODE_BUDGET = 150
}

