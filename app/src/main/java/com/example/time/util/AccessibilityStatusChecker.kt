package com.example.time.util

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.text.TextUtils
import android.view.accessibility.AccessibilityManager
import com.example.time.accessibility.ReelGuardAccessibilityService

/**
 * Utility to verify whether ReelGuard's Accessibility Service is enabled in system settings
 * and actively running.
 */
object AccessibilityStatusChecker {

    enum class ServiceStatus {
        ACTIVE,
        ENABLED_NOT_RESPONDING,
        DISABLED
    }

    fun getStatus(context: Context): ServiceStatus {
        val isEnabledInSettings = isServiceEnabledInSettings(context)
        if (!isEnabledInSettings) {
            return ServiceStatus.DISABLED
        }

        return if (ReelGuardAccessibilityService.isServiceLive) {
            ServiceStatus.ACTIVE
        } else {
            ServiceStatus.ENABLED_NOT_RESPONDING
        }
    }

    private fun isServiceEnabledInSettings(context: Context): Boolean {
        val targetServiceClass = ReelGuardAccessibilityService::class.java
        val expectedComponent = ComponentName(context, targetServiceClass).flattenToString()
        val expectedShortComponent = "${context.packageName}/${targetServiceClass.name}"

        // Method 1: Query AccessibilityManager enabled services
        val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
        if (accessibilityManager != null) {
            val enabledServices = accessibilityManager.getEnabledAccessibilityServiceList(
                AccessibilityServiceInfo.FEEDBACK_ALL_MASK
            )
            for (service in enabledServices) {
                val serviceInfo = service.resolveInfo?.serviceInfo ?: continue
                if (serviceInfo.packageName == context.packageName &&
                    serviceInfo.name == targetServiceClass.name
                ) {
                    return true
                }
            }
        }

        // Method 2: Query Settings.Secure string fallback
        val enabledServicesSetting = try {
            Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
        } catch (_: Exception) {
            null
        } ?: return false

        val colonSplitter = TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServicesSetting)
        while (colonSplitter.hasNext()) {
            val componentName = colonSplitter.next()
            if (componentName.equals(expectedComponent, ignoreCase = true) ||
                componentName.equals(expectedShortComponent, ignoreCase = true)
            ) {
                return true
            }
        }

        return false
    }
}

