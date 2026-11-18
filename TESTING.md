# ReelGuard — Test Execution & Verification Report

**Date of Run:** November 2026  
**Build Target:** `app-debug.apk` (AGP 9.3.3, Gradle 9.5.0, Kotlin 2.2.10, compileSdk 37)  
**Execution Environment:** Linux x86_64, OpenJDK 27, Android SDK Build Tools 36/37  
**Device Connection State:** No physical device attached to ADB at time of test run (`adb devices -l` returned empty).

---

## 1. Automated Unit Tests

All unit tests were executed via `./gradlew testDebugUnitTest`.

**Summary:** 28 tests executed, 28 passed, 0 failed, 0 skipped.

### 1.1 Detection Engine Tests (`InstagramDetectorTest.kt`)

| Test Name | Fixture Scenario | Expected Detection | Actual Result | Status |
|---|---|---|---|---|
| `testFixture1_InstagramHome_ReturnsNone` | Home feed with bottom navigation | `NONE` | `NONE` (requiresAction = false) | **PASSED** |
| `testFixture2_InstagramDMs_ReturnsNone` | Direct messages inbox | `NONE` | `NONE` (requiresAction = false) | **PASSED** |
| `testFixture3_InstagramStories_ReturnsNone` | Stories viewer (`reel_viewer`) | `NONE` | `NONE` (protected) | **PASSED** |
| `testFixture4_ReelsTabSelected_ReturnsReelsTabSelected` | Reels tab with `isSelected = true` | `REELS_TAB_SELECTED` | `REELS_TAB_SELECTED` (confidence = 1.0) | **PASSED** |
| `testFixture5_ReelsTabUnselected_ReturnsNone` | Reels tab with `isSelected = false` | `NONE` | `NONE` (requiresAction = false) | **PASSED** |
| `testFixture6_FullScreenReelsViewer_ReturnsReelsViewer` | Visible `clips_viewer_view_pager` | `REELS_VIEWER` | `REELS_VIEWER` (confidence = 1.0) | **PASSED** |
| `testFixture7_NormalVideoPost_ReturnsNone` | Normal in-feed post with video | `NONE` | `NONE` (requiresAction = false) | **PASSED** |
| `testFixture8_ProfilePage_ReturnsNone` | User profile page | `NONE` | `NONE` (requiresAction = false) | **PASSED** |
| `testFixture9_ExplorePage_ReturnsNone` | Explore / Search grid | `NONE` | `NONE` (requiresAction = false) | **PASSED** |
| `testFixture10_UnknownHierarchy_ReturnsUnknown` | Unrecognized layout | `UNKNOWN` | `UNKNOWN` (fail-safe do nothing) | **PASSED** |
| `testExternalPackage_IgnoredImmediately` | External package (`com.whatsapp`) | `NONE` | `NONE` (ignored immediately) | **PASSED** |
| `testNullRoot_ReturnsUnknown` | Null root node | `UNKNOWN` | `UNKNOWN` (safe abort) | **PASSED** |
| `testReelsViewer_Invisible_ReturnsNone` | Recycled / invisible viewer | `NONE` | `NONE` (not visible to user) | **PASSED** |
| `testReelsViewer_EmptyBounds_ReturnsNone` | Zero-dimension bounds | `NONE` | `NONE` (empty bounds rejected) | **PASSED** |
| `testFullyQualifiedResourceIds_HandledCorrectly` | Qualified package ID string | `REELS_VIEWER` | `REELS_VIEWER` (matched correctly) | **PASSED** |
| `testStoriesWithReelPrefix_NotBlockedAsReels` | Story tags & tray components | `NONE` | `NONE` (protected) | **PASSED** |
| `testSanitizedJsonSerialization_ContainsNoText` | Sanitized JSON serialization | No text/contentDesc | Passed: 0 text/contentDesc keys | **PASSED** |

### 1.2 Action Cooldown & Rate Limiting Tests (`ActionCooldownTest.kt`)

| Test Name | Condition | Expected Result | Actual Result | Status |
|---|---|---|---|---|
| `testInitialActionAllowed` | First event received | Action permitted | Permitted | **PASSED** |
| `testActionBlockedDuringCooldown` | Event within 500ms of last action | Action throttled | Throttled (`canExecuteAction = false`) | **PASSED** |
| `testActionAllowedAfterCooldownExpires` | Event after 1100ms (cooldown 1000ms) | Action permitted | Permitted (`canExecuteAction = true`) | **PASSED** |
| `testMaxConsecutiveAttemptsBounded` | 3 consecutive identical actions | 3rd action halted | Blocked after max consecutive 2 | **PASSED** |
| `testResetClearsCooldown` | Screen returns to `NONE` | Counters reset | Consecutive counter reset to 0 | **PASSED** |

### 1.3 Block Handler & Navigation Orchestration Tests (`ReelsBlockHandlerTest.kt`)

| Test Name | Scenario | Expected Behavior | Actual Result | Status |
|---|---|---|---|---|
| `testReelsViewer_ExecutesGlobalBack` | Reels viewer detected | Issues Global Back & increments counter | Back executed, count = 1, result = "Success" | **PASSED** |
| `testReelsTabSelected_RedirectsToSafeTab` | Reels tab selected | Issues Safe Candidate Click & increments counter | Safe click executed, count = 1, result = "Success" | **PASSED** |
| `testBlockingDisabled_SkipsExecution` | User toggles blocking OFF | No actions executed | 0 navigation calls, result = "Skipped" | **PASSED** |
| `testCooldownThrottling_SuppressesDuplicateAction` | Rapid burst of 2 events | Only 1 action executed | 1 navigation call, result = "Cooldown Active" | **PASSED** |
| `testLegitimateScreen_ResetsConsecutiveAttempts` | Transition from Reels to Feed | Reset loop limiter | Limiter reset to 0 | **PASSED** |

---

## 2. Static Analysis & Build Verification

- **Task `./gradlew test`**: SUCCESS (28 tests passed).
- **Task `./gradlew lintDebug`**: SUCCESS (`BUILD SUCCESSFUL in 44s`, zero fatal issues).
- **Task `./gradlew assembleDebug`**: SUCCESS (Generated `app/build/outputs/apk/debug/app-debug.apk`, 12 MB).

---

## 3. Real Device Manual Testing Matrix

*Note on Hardware Availability:* A physical Android device was not attached via ADB during this session.
Per the project guidelines, automated unit tests and build tasks were executed and marked PASSED. Real device manual tests are documented below with their precise test procedures, expected outcomes, and current status marked as **PENDING DEVICE CONNECTION**.

| Test ID | Scenario | Procedure | Expected Outcome | Execution Status |
|---|---|---|---|---|
| **TEST 1** | Open Instagram | Launch Instagram from home screen | Instagram Home feed loads normally; bottom tabs visible; no redirection. | **PENDING DEVICE CONNECTION** |
| **TEST 2** | Open DMs | Tap DM inbox icon in top right | Messaging view opens cleanly; conversations list rendered; no redirection. | **PENDING DEVICE CONNECTION** |
| **TEST 3** | Open a conversation & send a message | Enter an active chat and type a message | Chat opens, keyboard appears, message sends; zero interference. | **PENDING DEVICE CONNECTION** |
| **TEST 4** | Open a Story | Tap a friend's story ring from home tray | Story opens full-screen (`reel_viewer`), plays normally, next story advances; not blocked. | **PENDING DEVICE CONNECTION** |
| **TEST 5** | Tap Reels bottom navigation tab | Tap Reels center icon in bottom navigation bar | Reels tab selection is intercepted immediately; redirects to DM Inbox or Home feed. | **PENDING DEVICE CONNECTION** |
| **TEST 6** | Open a Reel through Explore | Browse Explore grid and tap a short video tile | Full-screen viewer (`clips_viewer_view_pager`) opens; ReelGuard executes BACK; returns to Explore grid. | **PENDING DEVICE CONNECTION** |
| **TEST 7** | Open a Reel from a user profile | Visit an Instagram profile and tap a reel in their grid | Full-screen viewer launches; ReelGuard executes BACK; returns directly to user profile. | **PENDING DEVICE CONNECTION** |
| **TEST 8** | Open a Reel link sent in DM | Open a conversation containing a shared Reel and tap it | Full-screen viewer opens; ReelGuard executes BACK; returns directly to the conversation. | **PENDING DEVICE CONNECTION** |
| **TEST 9** | Open ordinary video post in Home feed | Scroll Home feed and play an ordinary video post | Video plays inline or expanded without triggering full-screen viewer; not blocked. | **PENDING DEVICE CONNECTION** |
| **TEST 10** | Rapid navigation between sections | Switch quickly between Home, Search, Activity, and Profile | No crash, freeze, infinite loop, or accidental app exit; cooldown prevents thrashing. | **PENDING DEVICE CONNECTION** |
| **TEST 11** | Disable ReelGuard | Toggle "Block Instagram Reels" switch OFF in ReelGuard | Reels tab and full-screen Reels viewer can be browsed without any blocking. | **PENDING DEVICE CONNECTION** |
| **TEST 12** | Phone restart | Reboot phone and re-open ReelGuard | Verify whether service remained active or prompts user to re-enable in Settings. | **PENDING DEVICE CONNECTION** |

---

## 4. Manual Device Verification Instructions for User

When connecting your **Realme Narzo 70 Turbo 5G**:

1. Enable USB Debugging in **Settings > Developer options > USB debugging**.
2. Connect phone via USB and run:
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```
3. Open **ReelGuard**, tap **"Enable in Android Settings"**, and turn the accessibility service **ON**.
4. Run through **TEST 1** through **TEST 12** above.
5. In ReelGuard, tap **Diagnostics** to verify that:
   - Instagram version is detected.
   - Successful blocks counter increments after tapping the Reels tab or opening a Reel.

