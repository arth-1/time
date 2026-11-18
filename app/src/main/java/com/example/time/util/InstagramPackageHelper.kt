package com.example.time.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import com.example.time.detector.InstagramSignatures

/**
 * Diagnostic helper to inspect installed Instagram package metadata
 * and trigger launcher intents.
 */
object InstagramPackageHelper {

    data class InstagramInfo(
        val isInstalled: Boolean,
        val versionName: String?,
        val versionCode: Long?
    )

    fun getInstagramInfo(context: Context): InstagramInfo {
        return try {
            val pm = context.packageManager
            val packageInfo: PackageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(
                    InstagramSignatures.INSTAGRAM_PACKAGE,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(InstagramSignatures.INSTAGRAM_PACKAGE, 0)
            }

            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }

            InstagramInfo(
                isInstalled = true,
                versionName = packageInfo.versionName,
                versionCode = versionCode
            )
        } catch (_: PackageManager.NameNotFoundException) {
            InstagramInfo(
                isInstalled = false,
                versionName = null,
                versionCode = null
            )
        }
    }

    fun openInstagram(context: Context): Boolean {
        return try {
            val intent = context.packageManager.getLaunchIntentForPackage(InstagramSignatures.INSTAGRAM_PACKAGE)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                true
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }
}

