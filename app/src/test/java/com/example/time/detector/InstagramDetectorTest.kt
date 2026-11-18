package com.example.time.detector

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for the InstagramDetector engine using sanitized UI fixtures.
 *
 * Verifies all 10 standard Instagram screen scenarios, edge cases,
 * boundary conditions, and false-positive prevention rules.
 */
class InstagramDetectorTest {

    private lateinit var detector: InstagramDetector
    private val igPackage = InstagramSignatures.INSTAGRAM_PACKAGE

    @Before
    fun setUp() {
        detector = InstagramDetector()
    }

    // =========================================================================
    // 10 REQUIRED FIXTURES
    // =========================================================================

    @Test
    fun testFixture1_InstagramHome_ReturnsNone() {
        val root = createFixtureHomeFeed()
        val decision = detector.evaluate(igPackage, root)
        assertEquals(DetectionType.NONE, decision.type)
        assertFalse(decision.requiresAction)
    }

    @Test
    fun testFixture2_InstagramDMs_ReturnsNone() {
        val root = createFixtureDirectMessages()
        val decision = detector.evaluate(igPackage, root)
        assertEquals(DetectionType.NONE, decision.type)
        assertFalse(decision.requiresAction)
    }

    @Test
    fun testFixture3_InstagramStories_ReturnsNone() {
        val root = createFixtureStoryViewer()
        val decision = detector.evaluate(igPackage, root)
        assertEquals(DetectionType.NONE, decision.type)
        assertFalse(decision.requiresAction)
        assertTrue(InstagramSignatures.matchesId(decision.matchedSignature, InstagramSignatures.ID_REEL_VIEWER))
    }

    @Test
    fun testFixture4_ReelsTabSelected_ReturnsReelsTabSelected() {
        val root = createFixtureReelsTab(selected = true, visible = true)
        val decision = detector.evaluate(igPackage, root)
        assertEquals(DetectionType.REELS_TAB_SELECTED, decision.type)
        assertTrue(decision.requiresAction)
        assertEquals(1.0f, decision.confidence, 0.001f)
    }

    @Test
    fun testFixture5_ReelsTabUnselected_ReturnsNone() {
        val root = createFixtureReelsTab(selected = false, visible = true)
        val decision = detector.evaluate(igPackage, root)
        assertEquals(DetectionType.NONE, decision.type)
        assertFalse(decision.requiresAction)
    }

    @Test
    fun testFixture6_FullScreenReelsViewer_ReturnsReelsViewer() {
        val root = createFixtureReelsViewer(visible = true, emptyBounds = false)
        val decision = detector.evaluate(igPackage, root)
        assertEquals(DetectionType.REELS_VIEWER, decision.type)
        assertTrue(decision.requiresAction)
        assertEquals(1.0f, decision.confidence, 0.001f)
    }

    @Test
    fun testFixture7_NormalVideoPost_ReturnsNone() {
        val root = createFixtureNormalVideoPost()
        val decision = detector.evaluate(igPackage, root)
        assertEquals(DetectionType.NONE, decision.type)
        assertFalse(decision.requiresAction)
    }

    @Test
    fun testFixture8_ProfilePage_ReturnsNone() {
        val root = createFixtureProfilePage()
        val decision = detector.evaluate(igPackage, root)
        assertEquals(DetectionType.NONE, decision.type)
        assertFalse(decision.requiresAction)
    }

    @Test
    fun testFixture9_ExplorePage_ReturnsNone() {
        val root = createFixtureExplorePage()
        val decision = detector.evaluate(igPackage, root)
        assertEquals(DetectionType.NONE, decision.type)
        assertFalse(decision.requiresAction)
    }

    @Test
    fun testFixture10_UnknownHierarchy_ReturnsUnknown() {
        val root = createFixtureUnknownHierarchy()
        val decision = detector.evaluate(igPackage, root)
        assertEquals(DetectionType.UNKNOWN, decision.type)
        assertFalse(decision.requiresAction)
    }

    // =========================================================================
    // EDGE CASES & ROBUSTNESS TESTS
    // =========================================================================

    @Test
    fun testExternalPackage_IgnoredImmediately() {
        val root = createFixtureReelsViewer(visible = true, emptyBounds = false)
        val decision = detector.evaluate("com.whatsapp", root)
        assertEquals(DetectionType.NONE, decision.type)
        assertFalse(decision.requiresAction)
        assertTrue(decision.reason.contains("ignored"))
    }

    @Test
    fun testNullRoot_ReturnsUnknown() {
        val decision = detector.evaluate(igPackage, null)
        assertEquals(DetectionType.UNKNOWN, decision.type)
        assertFalse(decision.requiresAction)
    }

    @Test
    fun testReelsViewer_Invisible_ReturnsNone() {
        val root = createFixtureReelsViewer(visible = false, emptyBounds = false)
        val decision = detector.evaluate(igPackage, root)
        assertEquals(DetectionType.NONE, decision.type)
        assertFalse(decision.requiresAction)
    }

    @Test
    fun testReelsViewer_EmptyBounds_ReturnsNone() {
        val root = createFixtureReelsViewer(visible = true, emptyBounds = true)
        val decision = detector.evaluate(igPackage, root)
        assertEquals(DetectionType.NONE, decision.type)
        assertFalse(decision.requiresAction)
    }

    @Test
    fun testFullyQualifiedResourceIds_HandledCorrectly() {
        val qualifiedId = "com.instagram.android:id/clips_viewer_view_pager"
        val root = UiNodeSnapshot(
            id = qualifiedId,
            className = "androidx.viewpager2.widget.ViewPager2",
            isVisibleToUser = true,
            isSelected = false,
            isClickable = false,
            bounds = RectData(0, 0, 1080, 2400)
        )
        val decision = detector.evaluate(igPackage, root)
        assertEquals(DetectionType.REELS_VIEWER, decision.type)
        assertTrue(decision.requiresAction)
    }

    @Test
    fun testStoriesWithReelPrefix_NotBlockedAsReels() {
        // Story elements often have ids like reel_viewer, reel_tag, reel_tray
        val root = UiNodeSnapshot(
            id = "com.instagram.android:id/reel_viewer",
            className = "android.widget.FrameLayout",
            isVisibleToUser = true,
            isSelected = false,
            isClickable = false,
            bounds = RectData(0, 0, 1080, 2400),
            children = listOf(
                UiNodeSnapshot(
                    id = "com.instagram.android:id/reel_item_media_container",
                    className = "android.view.View",
                    isVisibleToUser = true,
                    isSelected = false,
                    isClickable = true,
                    bounds = RectData(0, 0, 1080, 2400)
                )
            )
        )
        val decision = detector.evaluate(igPackage, root)
        assertEquals(DetectionType.NONE, decision.type)
        assertFalse(decision.requiresAction)
    }

    @Test
    fun testSanitizedJsonSerialization_ContainsNoText() {
        val root = createFixtureHomeFeed()
        val json = root.toJson()
        assertNotNull(json)
        assertTrue(json.contains("\"id\":"))
        assertTrue(json.contains("\"bounds\":"))
        assertFalse(json.contains("\"text\":"))
        assertFalse(json.contains("\"contentDescription\":"))
    }

    // =========================================================================
    // FIXTURE FACTORY METHODS
    // =========================================================================

    private fun createFixtureHomeFeed(): UiNodeSnapshot {
        return UiNodeSnapshot(
            id = "android:id/content",
            className = "android.widget.FrameLayout",
            isVisibleToUser = true,
            isSelected = false,
            isClickable = false,
            bounds = RectData(0, 0, 1080, 2400),
            children = listOf(
                UiNodeSnapshot(
                    id = "com.instagram.android:id/action_bar_inbox_button",
                    className = "android.widget.ImageView",
                    isVisibleToUser = true,
                    isSelected = false,
                    isClickable = true,
                    bounds = RectData(920, 100, 1040, 220)
                ),
                UiNodeSnapshot(
                    id = "com.instagram.android:id/feed_tab",
                    className = "android.widget.FrameLayout",
                    isVisibleToUser = true,
                    isSelected = true,
                    isClickable = true,
                    bounds = RectData(0, 2240, 216, 2400)
                ),
                UiNodeSnapshot(
                    id = "com.instagram.android:id/clips_tab",
                    className = "android.widget.FrameLayout",
                    isVisibleToUser = true,
                    isSelected = false,
                    isClickable = true,
                    bounds = RectData(648, 2240, 864, 2400)
                )
            )
        )
    }

    private fun createFixtureDirectMessages(): UiNodeSnapshot {
        return UiNodeSnapshot(
            id = "com.instagram.android:id/direct_inbox",
            className = "android.widget.FrameLayout",
            isVisibleToUser = true,
            isSelected = false,
            isClickable = false,
            bounds = RectData(0, 0, 1080, 2400),
            children = listOf(
                UiNodeSnapshot(
                    id = "com.instagram.android:id/direct_tab",
                    className = "android.widget.FrameLayout",
                    isVisibleToUser = true,
                    isSelected = true,
                    isClickable = true,
                    bounds = RectData(0, 0, 200, 200)
                )
            )
        )
    }

    private fun createFixtureStoryViewer(): UiNodeSnapshot {
        return UiNodeSnapshot(
            id = "com.instagram.android:id/reel_viewer",
            className = "android.widget.FrameLayout",
            isVisibleToUser = true,
            isSelected = false,
            isClickable = false,
            bounds = RectData(0, 0, 1080, 2400)
        )
    }

    private fun createFixtureReelsTab(selected: Boolean, visible: Boolean): UiNodeSnapshot {
        return UiNodeSnapshot(
            id = "com.instagram.android:id/navigation_bar",
            className = "android.widget.LinearLayout",
            isVisibleToUser = true,
            isSelected = false,
            isClickable = false,
            bounds = RectData(0, 2240, 1080, 2400),
            children = listOf(
                UiNodeSnapshot(
                    id = "com.instagram.android:id/feed_tab",
                    className = "android.widget.FrameLayout",
                    isVisibleToUser = true,
                    isSelected = !selected,
                    isClickable = true,
                    bounds = RectData(0, 2240, 216, 2400)
                ),
                UiNodeSnapshot(
                    id = "com.instagram.android:id/clips_tab",
                    className = "android.widget.FrameLayout",
                    isVisibleToUser = visible,
                    isSelected = selected,
                    isClickable = true,
                    bounds = RectData(648, 2240, 864, 2400)
                )
            )
        )
    }

    private fun createFixtureReelsViewer(visible: Boolean, emptyBounds: Boolean): UiNodeSnapshot {
        val bounds = if (emptyBounds) RectData.EMPTY else RectData(0, 0, 1080, 2400)
        return UiNodeSnapshot(
            id = "com.instagram.android:id/clips_viewer_view_pager",
            className = "androidx.viewpager2.widget.ViewPager2",
            isVisibleToUser = visible,
            isSelected = false,
            isClickable = false,
            bounds = bounds
        )
    }

    private fun createFixtureNormalVideoPost(): UiNodeSnapshot {
        return UiNodeSnapshot(
            id = "com.instagram.android:id/row_feed_photo_profile_imageview",
            className = "android.widget.ImageView",
            isVisibleToUser = true,
            isSelected = false,
            isClickable = true,
            bounds = RectData(20, 200, 120, 300),
            children = listOf(
                UiNodeSnapshot(
                    id = "com.instagram.android:id/feed_tab",
                    className = "android.widget.FrameLayout",
                    isVisibleToUser = true,
                    isSelected = true,
                    isClickable = true,
                    bounds = RectData(0, 2240, 216, 2400)
                )
            )
        )
    }

    private fun createFixtureProfilePage(): UiNodeSnapshot {
        return UiNodeSnapshot(
            id = "com.instagram.android:id/profile_tab",
            className = "android.widget.FrameLayout",
            isVisibleToUser = true,
            isSelected = true,
            isClickable = true,
            bounds = RectData(864, 2240, 1080, 2400)
        )
    }

    private fun createFixtureExplorePage(): UiNodeSnapshot {
        return UiNodeSnapshot(
            id = "com.instagram.android:id/search_tab",
            className = "android.widget.FrameLayout",
            isVisibleToUser = true,
            isSelected = true,
            isClickable = true,
            bounds = RectData(216, 2240, 432, 2400)
        )
    }

    private fun createFixtureUnknownHierarchy(): UiNodeSnapshot {
        return UiNodeSnapshot(
            id = "some_random_unrelated_id",
            className = "android.view.View",
            isVisibleToUser = true,
            isSelected = false,
            isClickable = false,
            bounds = RectData(0, 0, 100, 100)
        )
    }
}
