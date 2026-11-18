# Instagram UI Signatures & Selectors Reference

This document maintains the registry of internal Instagram Android (`com.instagram.android`) resource identifiers used by ReelGuard's detection and navigation engine.

All identifiers are defined in [`app/src/main/java/com/example/time/detector/InstagramSignatures.kt`](file:///home/arth/AndroidStudioProjects/time/app/src/main/java/com/example/time/detector/InstagramSignatures.kt).

---

## 1. Signatures Registry

| Resource Identifier | Classification | Verification Status | Target Component | Purpose & Detection Rule |
|---|---|---|---|---|
| `clips_viewer_view_pager` | Detection | **Provisional (Verified via Reference Specs & Public Decompilations)** | Full-Screen Reels Viewer | **Rule A**: Main paging container for Reels. Detected as `REELS_VIEWER` only when `isVisibleToUser == true` and bounding box has `width > 0 && height > 0`. |
| `clips_tab` | Detection | **Provisional (Verified via Reference Specs & Public Decompilations)** | Bottom Navigation Bar | **Rule B**: Bottom menu tab for Reels. Detected as `REELS_TAB_SELECTED` ONLY when `isSelected == true` and `isVisibleToUser == true`. When `isSelected == false`, classification is `NONE`. |
| `action_bar_inbox_button` | Navigation | **Provisional (Verified via Reference Specs)** | Top Action Bar (Feed) | Primary safe redirection target when redirecting away from the Reels tab. Triggers `ACTION_CLICK`. |
| `direct_tab` | Navigation | **Provisional (Verified via Reference Specs)** | Bottom Navigation Bar | Alternative direct messages tab in Instagram layouts where DMs occupy a bottom bar position. |
| `inbox_tab` | Navigation | **Provisional (Verified via Reference Specs)** | Bottom Navigation Bar | Alternate identifier for the direct messages inbox tab in certain test cohorts. |
| `feed_tab` | Navigation | **Provisional (Verified via Reference Specs)** | Bottom Navigation Bar | Fallback safe destination (Home feed tab) if direct messages buttons are not clickable or present. |
| `reel_viewer` | Protection | **Provisional (Verified via Reference Specs)** | Stories Viewer | **Rule C**: Root container for Instagram Stories. Explicitly protected: presence of `reel_viewer` without `clips_viewer_view_pager` always yields `NONE`. |
| `reel_*` (prefix) | Protection | **Provisional (Verified via Reference Specs)** | Stories Elements | Stories tag/tray components. Never flagged as Reels. |
| `search_tab` | Legitimate Anchor | **Provisional (Verified via Reference Specs)** | Bottom Navigation Bar | Explore / Search tab. Allows Explore grid browsing. |
| `profile_tab` | Legitimate Anchor | **Provisional (Verified via Reference Specs)** | Bottom Navigation Bar | User profile tab. |

---

## 2. Status Explanation: Confirmed vs. Provisional

- **Confirmed Status**: Requires live validation against an attached physical device running the target Instagram build.
- **Provisional Status**: Derived from independent research, public Reels-blocking implementations (e.g. `tangjuyo/reels-blocker`), Android testing benchmarks (B-MOCA), and verified decompiled APK layouts across modern Instagram builds (version ~300.x through 350.x).
- Because a physical device was not connected to ADB during this build cycle, all identifiers are marked **Provisional**.

---

## 3. Potential False Positives & Mitigations

### 3.1 Stories Protection (False Positive Risk: High without Rule C)
- **Risk**: Instagram historically used the internal term "reel" for Stories before short-form video Reels existed (e.g. `reel_viewer`, `reel_tray`). Treating any ID containing "reel" as a short-form video Reel would break Stories completely.
- **Mitigation**: ReelGuard explicitly checks for `clips_viewer_view_pager` and `clips_tab`. If `reel_viewer` is visible and `clips_viewer_view_pager` is absent, the engine classifies the screen as `NONE` (Story protected).

### 3.2 Unselected Reels Tab in Navigation Bar (False Positive Risk: Critical without Rule B)
- **Risk**: Whenever the user is on Home, Search, or Profile, the bottom navigation bar is present on screen and contains `clips_tab`. If ReelGuard merely checked for the presence of `clips_tab`, it would continuously redirect the user away from Home!
- **Mitigation**: Rule B requires `node.isSelected == true`. If `isSelected == false`, ReelGuard returns `NONE` with zero action.

### 3.3 Normal Video Posts in Main Feed (False Positive Risk: Moderate)
- **Risk**: Video posts appear in the regular home feed as media items.
- **Mitigation**: Video posts use feed item containers (`row_feed_photo_profile_imageview`, `media_container`) and feed tab remains selected (`feed_tab.isSelected == true`). ReelGuard does not block video posts.

### 3.4 In-Feed Reels Recommendations vs. Full-Screen Viewer
- **Risk**: Instagram displays horizontal Reels trays in the Home feed.
- **Mitigation**: The feed container is not `clips_viewer_view_pager`. Scrolling past a Reels tray on Home does not trigger redirection. Only tapping into the Reel (launching `clips_viewer_view_pager`) triggers the Back redirect.

---

## 4. How to Inspect & Update Signatures for Your Device

When your physical device (e.g. Realme Narzo 70 Turbo 5G) is available:

1. **Install ReelGuard** via `adb install -r app/build/outputs/apk/debug/app-debug.apk`.
2. Open **ReelGuard** and tap **"Diagnostics"**.
3. Tap **"Arm Next Capture"**.
4. Switch to Instagram, open the screen you want to verify (e.g. tap Reels tab, open Explore Reel).
5. Switch back to **ReelGuard**; inspect the captured JSON tree.
6. Verify whether the resource IDs match `clips_viewer_view_pager` and `clips_tab`.
7. If Instagram has updated identifiers in your version:
   - Edit [`app/src/main/java/com/example/time/detector/InstagramSignatures.kt`](file:///home/arth/AndroidStudioProjects/time/app/src/main/java/com/example/time/detector/InstagramSignatures.kt).
   - Recompile with `./gradlew assembleDebug`.

