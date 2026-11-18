# ReelGuard — Private, Local-Only Instagram Reels Blocker

ReelGuard is a lightweight, fully private, open-source Android application that selectively redirects away from Instagram Reels while preserving complete functionality for normal posts, stories, direct messaging, search, explore, and profiles in the official Instagram Android app (`com.instagram.android`).

Built for personal use with Kotlin, Android Jetpack Compose, and the native Android `AccessibilityService` API.

---

## Key Features

- **Selective Reels Blocking**:
  - Automatically redirects away when the bottom Reels navigation tab is selected (redirects to Direct Messages or Home).
  - Automatically executes a safe `GLOBAL_ACTION_BACK` when a full-screen Reel is opened (from Home recommendations, Explore, DM links, or user profiles) to restore the previous context.
- **Strict Stories & Post Protection**:
  - Distinguishes Stories (`reel_viewer`) and normal video posts from Reels.
  - Explore remains browseable; only active Reels viewer sessions are redirected.
- **Privacy & Security by Design**:
  - **Zero Network Access**: Does not declare `android.permission.INTERNET`.
  - **Data Minimization**: Never reads post captions, comments, messages, or user inputs (`getText()` and `getContentDescription()` are strictly avoided).
  - **Scoped Processing**: Accessibility events from any package other than `com.instagram.android` are immediately discarded.
  - **Zero Trackers**: No analytics, telemetry, crash reporters, or advertising libraries.
  - **No Cloud Sync**: Excluded from Android Auto-Backup.
- **Anti-Loop & Cooldown Safeguards**:
  - Action cooldown throttling (1.2 seconds default) prevents repeated rapid-fire events from causing interface thrashing or infinite navigation loops.
  - Maximum consecutive attempt limits prevent repeated actions on unresponsive screens.
- **Built-in Developer Diagnostics**:
  - "Capture next Instagram screen" feature allows recording sanitized layout structure (node counts, resource IDs, bounds, selection states) directly on-device without leaking personal content.

---

## Architecture Overview

```
app/src/main/java/com/example/time/
├── MainActivity.kt                     # ComponentActivity hosting Jetpack Compose UI
├── accessibility/
│   ├── ReelGuardAccessibilityService.kt # Android AccessibilityService receiving system events
│   └── AccessibilityConfiguration.kt    # Event filters, timeouts, and node budgets
├── detector/
│   ├── UiNodeSnapshot.kt                # Pure Kotlin sanitized node tree representation
│   ├── DetectionResult.kt               # Strongly typed decisions (NONE, REELS_TAB_SELECTED, REELS_VIEWER, UNKNOWN)
│   ├── InstagramSignatures.kt           # Central repository of UI identifiers and selectors
│   └── InstagramDetector.kt             # Pure Kotlin confidence-based rule evaluation
├── blocking/
│   ├── ActionCooldown.kt                # Thread-safe rate limiter and loop safeguard
│   ├── InstagramNavigator.kt            # Decoupled navigation executor (Back vs Safe Tab click)
│   └── ReelsBlockHandler.kt             # Action coordinator linking detector to navigation
├── data/
│   ├── PreferencesRepository.kt         # Local SharedPreferences with daily auto-reset
│   └── DiagnosticCaptureRepository.kt   # In-memory diagnostic snapshot repository
├── util/
│   ├── AccessibilityStatusChecker.kt    # System accessibility status & liveness query
│   └── InstagramPackageHelper.kt        # Instagram installation & version query
└── ui/
    ├── theme/                           # Charcoal dark theme, typography, color palette
    ├── dashboard/                       # Primary control dashboard
    ├── diagnostics/                     # On-device diagnostic & snapshot inspector
    └── components/                      # Reusable Material 3 cards, badges, dialogs
```

---

## How It Works

1. **Service Registration**: The user manually authorizes ReelGuard in Android System Accessibility Settings.
2. **Package Filtering**: ReelGuard's service configuration scopes listening strictly to `com.instagram.android`.
3. **Sanitized Node Extraction**: On window transitions and content changes, the active node hierarchy is traversed up to a strict budget (max depth 14, max nodes 150) extracting **only** view IDs, class names, bounds, visibility, and selection flags.
4. **Confidence-Based Detection**:
   - **Rule A (Reels Viewer)**: Identified when `clips_viewer_view_pager` is visible with non-empty bounds.
   - **Rule B (Reels Tab)**: Identified when `clips_tab` has `isSelected == true` and is visible.
   - **Rule C (Stories Protection)**: Story containers (`reel_viewer` / `reel_*`) are explicitly protected from classification as Reels.
   - **Rule D (Fail-Safe Unknown)**: Unrecognized screens yield `UNKNOWN` and trigger zero actions.
5. **Safe Navigation**:
   - For `REELS_TAB_SELECTED`: Attempts clickable click on DM Inbox (`action_bar_inbox_button`, `direct_tab`, `inbox_tab`) or fallback to Home (`feed_tab`).
   - For `REELS_VIEWER`: Issues `AccessibilityService.GLOBAL_ACTION_BACK` to return to previous screen.

---

## Setup and Installation

### 1. Build the APK

Ensure Android SDK (API 34+) and JDK 17/21 are installed.

```bash
# Run unit tests
./gradlew test

# Run Android lint
./gradlew lintDebug

# Assemble debug APK
./gradlew assembleDebug
```

The compiled APK will be at:
`app/build/outputs/apk/debug/app-debug.apk`

### 2. Install on Device via ADB

Connect your Android phone (e.g. Realme Narzo 70 Turbo 5G) via USB with USB Debugging enabled:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 3. Grant Accessibility Permission

1. Open **ReelGuard**.
2. Tap **"Enable in Android Settings"** (or open **Settings > Accessibility > Installed apps / Downloaded apps**).
3. Find **ReelGuard Reels Blocker** and toggle it **ON**.
4. Confirm the Android system security prompt.
5. Return to **ReelGuard**; the status indicator will turn **green ("Accessibility Service Active")**.

---

## Verification & Diagnostics

ReelGuard includes an on-device diagnostic screen:

1. Open **ReelGuard** and tap **Diagnostics**.
2. Tap **"Arm Next Capture"**.
3. Switch to Instagram and navigate to the screen you want to inspect.
4. Return to ReelGuard to view the sanitized JSON hierarchy, node count, and detected signatures.
5. Copy the sanitized JSON to verify or update selectors if Instagram releases a layout redesign.

---

## Known Limitations

- **Instagram UI Updates**: Instagram periodically updates obfuscated view identifiers. If `clips_viewer_view_pager` or `clips_tab` are renamed in a future release, ReelGuard will enter fail-safe mode (`UNKNOWN`) and avoid blocking until selectors are updated in `InstagramSignatures.kt`.
- **Custom Android ROM Battery Management**: Aggressive battery savers (such as Realme UI / ColorOS / MIUI) may stop background accessibility services after prolonged inactivity. It is recommended to lock ReelGuard in recent apps and set battery usage to "Unrestricted" in app info.
- **Embedded In-Feed Video Players**: Ordinary video posts in the home feed use standard post containers; they are allowed and not blocked. Only full-screen Reels viewer launches are intercepted.

---

## License

Personal, local-only project.
All source code is provided as-is for personal digital wellbeing and focus enhancement.

