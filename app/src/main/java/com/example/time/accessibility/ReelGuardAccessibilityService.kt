package com.example.time.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.graphics.Rect
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.time.blocking.AccessibilityNavigationExecutor
import com.example.time.blocking.ActionCooldown
import com.example.time.blocking.ReelsBlockHandler
import com.example.time.data.DiagnosticCaptureRepository
import com.example.time.data.PreferencesRepository
import com.example.time.detector.InstagramDetector
import com.example.time.detector.InstagramSignatures
import com.example.time.detector.RectData
import com.example.time.detector.UiNodeSnapshot
import java.util.concurrent.atomic.AtomicInteger

/**
 * Core Accessibility Service for ReelGuard.
 *
 * STRICT PRIVACY & MINIMIZATION RULES:
 * 1. Scope: Only handles events originating from "com.instagram.android".
 * 2. Minimization: Never accesses node text, content descriptions, or user input.
 * 3. Local-only: Operates entirely on-device; no data transmission or cloud backup.
 * 4. Safety: Throttles actions with ActionCooldown; aborts on ambiguous structures.
 */
class ReelGuardAccessibilityService : AccessibilityService() {

    private lateinit var prefs: PreferencesRepository
    private lateinit var detector: InstagramDetector
    private lateinit var cooldown: ActionCooldown
    private lateinit var blockHandler: ReelsBlockHandler

    override fun onCreate() {
        super.onCreate()
        prefs = PreferencesRepository.getInstance(applicationContext)
        detector = InstagramDetector()
        cooldown = ActionCooldown(cooldownMillis = prefs.cooldownMillis)
        val navigator = AccessibilityNavigationExecutor { this }
        blockHandler = ReelsBlockHandler(detector, cooldown, navigator, prefs)
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        isServiceLive = true
        notifyStatusChanged()

        // Configure service parameters explicitly at runtime
        val info = serviceInfo ?: AccessibilityServiceInfo()
        info.packageNames = arrayOf(InstagramSignatures.INSTAGRAM_PACKAGE)
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        info.flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
        info.notificationTimeout = AccessibilityConfiguration.NOTIFICATION_TIMEOUT_MS
        serviceInfo = info
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        // STRICT PACKAGE GATING:
        // Ignore any event not explicitly from official Instagram
        val eventPackage = event.packageName?.toString() ?: return
        if (eventPackage != InstagramSignatures.INSTAGRAM_PACKAGE) {
            return
        }

        // Only process window state and content changes
        val eventType = event.eventType
        if (eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        ) {
            return
        }

        val rootNode = rootInActiveWindow ?: return

        try {
            // Extract sanitized structural tree (zero text, zero content descriptions)
            val nodeCounter = AtomicInteger(0)
            val snapshot = extractSanitizedSnapshot(
                node = rootNode,
                depth = 0,
                maxDepth = AccessibilityConfiguration.MAX_HIERARCHY_DEPTH,
                nodeBudget = AccessibilityConfiguration.MAX_NODE_BUDGET,
                nodeCounter = nodeCounter
            )

            // Diagnostic Mode: If capture is armed, deliver snapshot to repository
            if (DiagnosticCaptureRepository.state == DiagnosticCaptureRepository.CaptureState.AWAITING_CAPTURE) {
                if (snapshot != null) {
                    DiagnosticCaptureRepository.deliverSnapshot(snapshot)
                }
            }

            // Run detection and blocking orchestration
            blockHandler.handleEvent(eventPackage, snapshot)
        } finally {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                @Suppress("DEPRECATION")
                rootNode.recycle()
            }
        }
    }

    override fun onInterrupt() {
        cooldown.reset()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        isServiceLive = false
        cooldown.reset()
        notifyStatusChanged()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        isServiceLive = false
        notifyStatusChanged()
        super.onDestroy()
    }

    /**
     * Recursively extracts structural metadata into a sanitized [UiNodeSnapshot].
     *
     * EXPLICIT SAFEGUARD:
     * NEVER reads `node.text` or `node.contentDescription`.
     */
    private fun extractSanitizedSnapshot(
        node: AccessibilityNodeInfo?,
        depth: Int,
        maxDepth: Int,
        nodeBudget: Int,
        nodeCounter: AtomicInteger
    ): UiNodeSnapshot? {
        if (node == null || depth > maxDepth || nodeCounter.incrementAndGet() > nodeBudget) {
            return null
        }

        val boundsRect = Rect()
        node.getBoundsInScreen(boundsRect)
        val rectData = RectData(
            left = boundsRect.left,
            top = boundsRect.top,
            right = boundsRect.right,
            bottom = boundsRect.bottom
        )

        val childSnapshots = mutableListOf<UiNodeSnapshot>()
        val childCount = node.childCount
        for (i in 0 until childCount) {
            val childNode = node.getChild(i) ?: continue
            try {
                val childSnapshot = extractSanitizedSnapshot(
                    node = childNode,
                    depth = depth + 1,
                    maxDepth = maxDepth,
                    nodeBudget = nodeBudget,
                    nodeCounter = nodeCounter
                )
                if (childSnapshot != null) {
                    childSnapshots.add(childSnapshot)
                }
            } finally {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                    @Suppress("DEPRECATION")
                    childNode.recycle()
                }
            }
        }

        return UiNodeSnapshot(
            id = node.viewIdResourceName,
            className = node.className?.toString(),
            isVisibleToUser = node.isVisibleToUser,
            isSelected = node.isSelected,
            isClickable = node.isClickable,
            bounds = rectData,
            children = childSnapshots,
            depth = depth
        )
    }

    companion object {
        @Volatile
        var isServiceLive: Boolean = false
            private set

        private val statusListeners = mutableListOf<() -> Unit>()

        @Synchronized
        fun addStatusListener(listener: () -> Unit) {
            statusListeners.add(listener)
        }

        @Synchronized
        fun removeStatusListener(listener: () -> Unit) {
            statusListeners.remove(listener)
        }

        private fun notifyStatusChanged() {
            val listenersCopy = synchronized(this) { statusListeners.toList() }
            listenersCopy.forEach { it.invoke() }
        }
    }
}

