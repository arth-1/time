# ReelGuard — Security & Privacy Audit Report

**Date:** November 2026  
**Auditor Role:** Senior Mobile Security Engineer  
**Scope:** Application source code, Gradle build configuration, and merged APK manifest.  
**Target Package:** `com.example.time` (ReelGuard)  
**Binary Analyzed:** `app/build/outputs/apk/debug/app-debug.apk`

---

## 1. Executive Summary

ReelGuard was designed specifically as a privacy-first, local-only Android utility. An independent security review of the project source code and the generated binary confirms that ReelGuard:
1. **Lacks network capability entirely**: `android.permission.INTERNET` is neither requested nor present in the merged manifest.
2. **Minimizes accessibility data access**: Does not invoke methods that read text, message content, passwords, or accessibility event text.
3. **Implements strict package isolation**: Gated to discard all events not originating from `com.instagram.android`.
4. **Contains zero tracking or advertising SDKs**: Free of Firebase, analytics, or third-party telemetry.
5. **Restricts backup persistence**: Excluded from Android Auto-Backup and device-to-device transfers.

---

## 2. Permission Footprint Audit

### 2.1 Manifest Permissions Review

| Permission | Requested? | In Merged Manifest? | Purpose / Justification |
|---|---|---|---|
| `android.permission.INTERNET` | **NO** | **NO** | Strictly prohibited. App is physically incapable of network communication. |
| `android.permission.ACCESS_NETWORK_STATE` | **NO** | **NO** | Unnecessary. |
| `android.permission.READ_EXTERNAL_STORAGE` | **NO** | **NO** | Unnecessary. |
| `android.permission.WRITE_EXTERNAL_STORAGE` | **NO** | **NO** | Unnecessary. |
| `android.permission.CAMERA` | **NO** | **NO** | Unnecessary. |
| `android.permission.RECORD_AUDIO` | **NO** | **NO** | Unnecessary. |
| `android.permission.READ_CONTACTS` | **NO** | **NO** | Unnecessary. |
| `android.permission.ACCESS_FINE_LOCATION` | **NO** | **NO** | Unnecessary. |
| `android.permission.SYSTEM_ALERT_WINDOW` | **NO** | **NO** | Unnecessary. App does not display screen overlays. |
| `android.permission.BIND_ACCESSIBILITY_SERVICE` | **YES** | **YES** | Required by Android framework on `<service>` to restrict binding to the Android OS. |
| `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` | **NO (Auto-merged)** | **YES** | Added by AndroidX Core/Activity for secure intra-app dynamic broadcast protection (signature-level). |

### 2.2 Merged Manifest Verification
The final merged manifest located at `app/build/intermediates/merged_manifest/debug/processDebugMainManifest/AndroidManifest.xml` was verified line-by-line:
- No internet, network, telephony, storage, camera, or sensor permissions exist.
- The only exported components are:
  - `MainActivity` (protected by system launcher filter)
  - `ReelGuardAccessibilityService` (secured by signature permission `android.permission.BIND_ACCESSIBILITY_SERVICE`)

---

## 3. Accessibility Capabilities & Data Minimization

Accessibility is Android's most sensitive capability. The table below audits what data ReelGuard accesses and what data is strictly forbidden in the codebase:

### 3.1 Data Accessed vs. Not Accessed

| Data Category | Status | Implementation Detail |
|---|---|---|
| **Text content (Captions, comments, bio)** | **NEVER ACCESSED** | Audited: Zero calls to `AccessibilityNodeInfo.getText()` or `AccessibilityEvent.getText()`. |
| **Direct Message contents** | **NEVER ACCESSED** | Traversal extracts only view IDs; no message payloads or string buffers are inspected. |
| **Usernames / Handles** | **NEVER ACCESSED** | Not recorded or extracted. |
| **Search Queries** | **NEVER ACCESSED** | Not recorded or extracted. |
| **Passphrases / Inputs** | **NEVER ACCESSED** | Node text extraction is prohibited in `extractSanitizedSnapshot`. |
| **Screen Pixels / Screenshots** | **NEVER ACCESSED** | No `takeScreenshot()`, MediaProjection, or canvas capture APIs used. |
| **Content Descriptions** | **NEVER ACCESSED** | Zero calls to `AccessibilityNodeInfo.getContentDescription()`. |
| **View Resource IDs** | **ACCESSED** | `node.viewIdResourceName` is read to identify `clips_tab` and `clips_viewer_view_pager`. |
| **View Class Names** | **ACCESSED** | `node.className` read to distinguish layout types (`ViewPager2`, `FrameLayout`). |
| **Selection / Click Status** | **ACCESSED** | `node.isSelected`, `node.isClickable`, `node.isVisibleToUser` read to verify tab state. |
| **Bounding Coordinates** | **ACCESSED** | `node.getBoundsInScreen(rect)` read to reject invisible/recycled nodes with empty bounds. |

### 3.2 Service Configuration Scoping

File: `app/src/main/res/xml/accessibility_service_config.xml`
```xml
<accessibility-service xmlns:android="http://schemas.android.com/apk/res/android"
    android:description="@string/accessibility_service_description"
    android:packageNames="com.instagram.android"
    android:accessibilityEventTypes="typeWindowStateChanged|typeWindowContentChanged"
    android:accessibilityFlags="flagReportViewIds"
    android:accessibilityFeedbackType="feedbackGeneric"
    android:notificationTimeout="100"
    android:canRetrieveWindowContent="true" />
```

Audited properties:
- `android:packageNames="com.instagram.android"`: Limits OS event dispatching exclusively to Instagram.
- `FLAG_INCLUDE_NOT_IMPORTANT_VIEWS`: **Disabled**. Does not request offscreen or unimportant nodes.
- `FLAG_RETRIEVE_INTERACTIVE_WINDOWS`: **Disabled**. Does not inspect system windows or soft keyboards.
- `FLAG_REQUEST_FILTER_KEY_EVENTS`: **Disabled**. Never intercepts hardware key presses.
- `FLAG_REQUEST_TOUCH_EXPLORATION_MODE`: **Disabled**. Does not alter standard touch behavior.

---

## 4. Network Isolation & Dependency Audit

### 4.1 Dependency Audit
All dependencies declared in `gradle/libs.versions.toml`:
1. `androidx.core:core-ktx` — Official Android framework extensions.
2. `androidx.activity:activity-compose` — Official Compose Activity integration.
3. `androidx.lifecycle:lifecycle-runtime-ktx` — Official lifecycle state management.
4. `androidx.compose.ui:*`, `material3` — Official Android Jetpack Compose UI library.
5. `junit:junit` — Standard JVM unit testing (test scope only).

**Findings:**
- Zero HTTP client libraries (`okhttp`, `retrofit`, `ktor`, `volley`).
- Zero telemetry / analytics libraries (`firebase`, `appsflyer`, `mixpanel`, `segment`).
- Zero advertising SDKs (`admob`, `unity-ads`, `applovin`).
- Zero crash reporters (`crashlytics`, `sentry`, `bugsnag`).
- Zero WebView components.

---

## 5. Local Storage & Backup Audit

1. **Storage Mechanism**: All preferences are stored in private SharedPreferences (`reelguard_local_prefs`) via `Context.MODE_PRIVATE`.
2. **Cloud Backup Exclusions**:
   - `res/xml/backup_rules.xml` explicitly excludes `sharedpref`, `database`, and `file`.
   - `res/xml/data_extraction_rules.xml` excludes cloud backup and device transfer.
   - `android:allowBackup="false"` is declared on `<application>`.
3. **Diagnostics Isolation**: Diagnostic snapshots captured in memory via `DiagnosticCaptureRepository` are never written to disk or database.

---

## 6. Verification Steps for End Users

To independently verify the security of the compiled APK:

```bash
# 1. Dump APK permissions using aapt2
aapt2 dump permissions app/build/outputs/apk/debug/app-debug.apk

# 2. Verify that android.permission.INTERNET is absent
aapt2 dump badging app/build/outputs/apk/debug/app-debug.apk | grep -i permission

# 3. Disassemble strings to verify absence of HTTP/HTTPS endpoints
strings app/build/outputs/apk/debug/app-debug.apk | grep -E '^https?://'
```

---

## 7. Security Limitations & Threat Model

1. **Accessibility Privilege Surface**: Any Android Accessibility Service possesses high platform privileges. While ReelGuard limits its code to resource ID inspection, an attacker with root or arbitrary code execution privileges on the device could theoretically tamper with app binaries. This risk is mitigated by installing locally compiled binaries directly via ADB.
2. **Instagram UI Mutation**: Instagram may alter internal view IDs without notice. When this happens, ReelGuard defaults to `UNKNOWN` (doing nothing) to prevent accidental interference.
3. **OEM Aggression**: Devices with heavy background killers may terminate accessibility services. Users should whitelist ReelGuard in device settings.

