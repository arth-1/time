package com.example.time.detector

/**
 * Repository of Instagram UI Resource Identifiers and Selectors.
 *
 * All selectors used by ReelGuard are centralized here.
 * If Instagram modifies its internal UI structure, updating identifiers
 * in this single file updates the entire detection and navigation engine.
 */
object InstagramSignatures {

    /**
     * Official Instagram Android package name.
     */
    const val INSTAGRAM_PACKAGE = "com.instagram.android"

    // ==========================================
    // REELS IDENTIFIERS (Signatures to detect)
    // ==========================================

    /**
     * Full-screen Reels viewer container (ViewPager2 or custom swipe container).
     * Strong indicator of an active Reels viewing session when visible with non-empty bounds.
     */
    const val ID_CLIPS_VIEWER_PAGER = "clips_viewer_view_pager"

    /**
     * Bottom navigation bar tab for Instagram Reels / Clips.
     * Must be verified as isSelected == true before flagging as REELS_TAB_SELECTED.
     */
    const val ID_CLIPS_TAB = "clips_tab"

    // ==========================================
    // SAFE NAVIGATION IDENTIFIERS
    // ==========================================

    /**
     * Top action bar Direct Messages button on the Home feed.
     * Preferred safe destination when redirecting away from the Reels tab.
     */
    const val ID_ACTION_BAR_INBOX_BUTTON = "action_bar_inbox_button"

    /**
     * Dedicated Direct Messages tab in layouts where direct has a bottom tab.
     */
    const val ID_DIRECT_TAB = "direct_tab"

    /**
     * Inbox tab identifier used in certain versions.
     */
    const val ID_INBOX_TAB = "inbox_tab"

    /**
     * Home / Main feed tab in the bottom navigation bar.
     * Secondary fallback safe destination.
     */
    const val ID_FEED_TAB = "feed_tab"

    /**
     * Explore / Search tab in the bottom navigation bar.
     */
    const val ID_SEARCH_TAB = "search_tab"

    /**
     * User profile tab in the bottom navigation bar.
     */
    const val ID_PROFILE_TAB = "profile_tab"

    // ==========================================
    // PROTECTED IDENTIFIERS (Never block as Reels)
    // ==========================================

    /**
     * Stories viewer container identifier.
     * Instagram Stories use "reel_viewer" and "reel_*" internally.
     * These must NEVER be treated as Reels.
     */
    const val ID_REEL_VIEWER = "reel_viewer"

    /**
     * Prefix used by Story nodes.
     */
    const val PREFIX_REEL = "reel_"

    /**
     * Ordered candidates for safe tab navigation away from Reels tab.
     * Priority: Inbox (DM) -> Fallback: Home Feed.
     */
    val SAFE_INBOX_CANDIDATES = listOf(
        ID_ACTION_BAR_INBOX_BUTTON,
        ID_DIRECT_TAB,
        ID_INBOX_TAB
    )

    val SAFE_HOME_CANDIDATES = listOf(
        ID_FEED_TAB
    )

    /**
     * Matches a resource ID against a target identifier.
     * Supports both unqualified ("clips_tab") and fully qualified ("com.instagram.android:id/clips_tab").
     */
    fun matchesId(resourceId: String?, target: String): Boolean {
        if (resourceId == null) return false
        if (resourceId == target) return true
        if (resourceId.endsWith(":id/$target")) return true
        if (resourceId.endsWith("/$target")) return true
        return false
    }

    /**
     * Returns true if the identifier matches known Stories elements.
     */
    fun isStoryIdentifier(resourceId: String?): Boolean {
        if (resourceId == null) return false
        val cleanId = resourceId.substringAfter(":id/").substringAfter('/')
        return cleanId == ID_REEL_VIEWER || cleanId.startsWith(PREFIX_REEL)
    }
}

