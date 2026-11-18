package com.example.time.blocking

import com.example.time.data.SettingsProvider
import com.example.time.detector.DetectionType
import com.example.time.detector.InstagramDetector
import com.example.time.detector.InstagramSignatures
import com.example.time.detector.RectData
import com.example.time.detector.UiNodeSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for ReelsBlockHandler:
 * Tests the interaction between detection, cooldown, navigation executor, and settings.
 */
class ReelsBlockHandlerTest {

    private lateinit var fakeSettings: FakeSettingsProvider
    private lateinit var fakeNavigator: FakeNavigationExecutor
    private lateinit var cooldown: ActionCooldown
    private lateinit var detector: InstagramDetector
    private lateinit var blockHandler: ReelsBlockHandler

    private val igPackage = InstagramSignatures.INSTAGRAM_PACKAGE

    @Before
    fun setUp() {
        fakeSettings = FakeSettingsProvider()
        fakeNavigator = FakeNavigationExecutor()
        cooldown = ActionCooldown(cooldownMillis = 1000L, maxConsecutiveAttempts = 2)
        detector = InstagramDetector()
        blockHandler = ReelsBlockHandler(detector, cooldown, fakeNavigator, fakeSettings)
    }

    @Test
    fun testReelsViewer_ExecutesGlobalBack() {
        val root = createReelsViewerSnapshot()
        val decision = blockHandler.handleEvent(igPackage, root)

        assertEquals(DetectionType.REELS_VIEWER, decision.type)
        assertEquals(1, fakeNavigator.backCallCount)
        assertEquals(1, fakeSettings.getBlockedCountToday())
        assertEquals(1L, fakeSettings.successCount)
        assertEquals("Success", fakeSettings.lastActionResult)
    }

    @Test
    fun testReelsTabSelected_RedirectsToSafeTab() {
        val root = createReelsTabSnapshot(selected = true)
        val decision = blockHandler.handleEvent(igPackage, root)

        assertEquals(DetectionType.REELS_TAB_SELECTED, decision.type)
        assertEquals(1, fakeNavigator.candidateClickCallCount)
        assertEquals(1, fakeSettings.getBlockedCountToday())
        assertEquals("Success", fakeSettings.lastActionResult)
    }

    @Test
    fun testBlockingDisabled_SkipsExecution() {
        fakeSettings.isBlockingEnabled = false
        val root = createReelsViewerSnapshot()

        val decision = blockHandler.handleEvent(igPackage, root)

        assertEquals(DetectionType.REELS_VIEWER, decision.type)
        assertEquals(0, fakeNavigator.backCallCount)
        assertEquals(0, fakeSettings.getBlockedCountToday())
        assertEquals("Skipped", fakeSettings.lastActionResult)
    }

    @Test
    fun testCooldownThrottling_SuppressesDuplicateAction() {
        val root = createReelsViewerSnapshot()

        // First action executes
        val d1 = blockHandler.handleEvent(igPackage, root)
        assertEquals(1, fakeNavigator.backCallCount)

        // Rapid duplicate event immediately after
        val d2 = blockHandler.handleEvent(igPackage, root)
        assertEquals(1, fakeNavigator.backCallCount) // Call count did not increase
        assertEquals("Cooldown Active", fakeSettings.lastActionResult)
    }

    @Test
    fun testLegitimateScreen_ResetsConsecutiveAttempts() {
        cooldown.recordAction("BACK", currentTime = 1000L)
        assertEquals(1, cooldown.getConsecutiveAttempts())

        // Feed snapshot (NONE)
        val feedRoot = UiNodeSnapshot(
            id = "com.instagram.android:id/feed_tab",
            className = "android.widget.FrameLayout",
            isVisibleToUser = true,
            isSelected = true,
            isClickable = true,
            bounds = RectData(0, 2240, 216, 2400)
        )

        blockHandler.handleEvent(igPackage, feedRoot)
        assertEquals(0, cooldown.getConsecutiveAttempts())
    }

    private fun createReelsViewerSnapshot(): UiNodeSnapshot {
        return UiNodeSnapshot(
            id = "com.instagram.android:id/clips_viewer_view_pager",
            className = "androidx.viewpager2.widget.ViewPager2",
            isVisibleToUser = true,
            isSelected = false,
            isClickable = false,
            bounds = RectData(0, 0, 1080, 2400)
        )
    }

    private fun createReelsTabSnapshot(selected: Boolean): UiNodeSnapshot {
        return UiNodeSnapshot(
            id = "com.instagram.android:id/clips_tab",
            className = "android.widget.FrameLayout",
            isVisibleToUser = true,
            isSelected = selected,
            isClickable = true,
            bounds = RectData(648, 2240, 864, 2400)
        )
    }
}

class FakeSettingsProvider : SettingsProvider {
    override var isBlockingEnabled: Boolean = true
    override var cooldownMillis: Long = 1000L
    override var lastDetectionType: String = "NONE"
    override var lastBlockingAction: String = "None"
    override var lastActionResult: String = "Idle"
    override var detectionCount: Long = 0L
    override var successCount: Long = 0L
    override var failureCount: Long = 0L
    private var blockedCount: Int = 0

    override fun getBlockedCountToday(): Int = blockedCount
    override fun incrementBlockedCountToday() { blockedCount++ }
}

class FakeNavigationExecutor : NavigationExecutor {
    var backCallCount: Int = 0
    var candidateClickCallCount: Int = 0
    var backReturnValue: Boolean = true
    var candidateClickReturnValue: Boolean = true

    override fun performGlobalBack(): Boolean {
        backCallCount++
        return backReturnValue
    }

    override fun clickNodeWithCandidates(candidateIds: List<String>): Boolean {
        candidateClickCallCount++
        return candidateClickReturnValue
    }
}

