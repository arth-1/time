package com.example.time.blocking

import android.accessibilityservice.AccessibilityService
import android.graphics.Rect
import android.os.Build
import android.view.accessibility.AccessibilityNodeInfo
import com.example.time.detector.InstagramSignatures

/**
 * Interface abstracting navigation actions away from Android framework dependencies
 * for testability and clean architecture.
 */
interface NavigationExecutor {
    fun performGlobalBack(): Boolean
    fun clickNodeWithCandidates(candidateIds: List<String>): Boolean
}

/**
 * Android AccessibilityService implementation of NavigationExecutor.
 *
 * Employs safe, verified accessibility actions:
 * - Back navigation via GLOBAL_ACTION_BACK.
 * - Tab navigation by finding verified clickable candidate nodes.
 * - Never clicks arbitrary coordinates.
 */
class AccessibilityNavigationExecutor(
    private val serviceProvider: () -> AccessibilityService?
) : NavigationExecutor {

    override fun performGlobalBack(): Boolean {
        val service = serviceProvider() ?: return false
        return service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
    }

    override fun clickNodeWithCandidates(candidateIds: List<String>): Boolean {
        val service = serviceProvider() ?: return false
        val root = service.rootInActiveWindow ?: return false

        try {
            for (candidateId in candidateIds) {
                val qualifiedId = "${InstagramSignatures.INSTAGRAM_PACKAGE}:id/$candidateId"
                val nodes = root.findAccessibilityNodeInfosByViewId(qualifiedId)

                if (!nodes.isNullOrEmpty()) {
                    for (node in nodes) {
                        if (attemptClick(node)) return true
                    }
                }

                // Fallback: search unqualified ID
                val fallbackNodes = root.findAccessibilityNodeInfosByViewId(candidateId)
                if (!fallbackNodes.isNullOrEmpty()) {
                    for (node in fallbackNodes) {
                        if (attemptClick(node)) return true
                    }
                }
            }
        } finally {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                @Suppress("DEPRECATION")
                root.recycle()
            }
        }

        return false
    }

    private fun attemptClick(node: AccessibilityNodeInfo): Boolean {
        if (!node.isVisibleToUser) return false

        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        if (bounds.isEmpty) return false

        // Check if node itself is directly clickable
        if (node.isClickable) {
            return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        }

        // Check immediate ancestor chain if the click target is a parent container
        var parent: AccessibilityNodeInfo? = node.parent
        var depth = 0
        while (parent != null && depth < 3) {
            if (parent.isClickable && parent.isVisibleToUser) {
                return parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
            parent = parent.parent
            depth++
        }

        return false
    }
}

