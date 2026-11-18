package com.example.time.data

import android.content.Context
import android.content.SharedPreferences
import java.util.Calendar

/**
 * Interface contract for application configuration and statistics storage.
 */
interface SettingsProvider {
    var isBlockingEnabled: Boolean
    var cooldownMillis: Long
    var lastDetectionType: String
    var lastBlockingAction: String
    var lastActionResult: String
    var detectionCount: Long
    var successCount: Long
    var failureCount: Long

    fun getBlockedCountToday(): Int
    fun incrementBlockedCountToday()
}

/**
 * Local-only repository for ReelGuard configuration, state, and statistics.
 *
 * STRICT LOCAL-ONLY REQUIREMENT:
 * All data is stored purely in device-local SharedPreferences.
 * No data is uploaded, transmitted, or synchronized externally.
 * Excluded from Android Auto-Backup.
 */
class PreferencesRepository(context: Context) : SettingsProvider {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override var isBlockingEnabled: Boolean
        get() = prefs.getBoolean(KEY_BLOCKING_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_BLOCKING_ENABLED, value).apply()

    override var cooldownMillis: Long
        get() = prefs.getLong(KEY_COOLDOWN_MS, 1200L)
        set(value) = prefs.edit().putLong(KEY_COOLDOWN_MS, value).apply()

    override var lastDetectionType: String
        get() = prefs.getString(KEY_LAST_DETECTION, "NONE") ?: "NONE"
        set(value) = prefs.edit().putString(KEY_LAST_DETECTION, value).apply()

    override var lastBlockingAction: String
        get() = prefs.getString(KEY_LAST_ACTION, "None") ?: "None"
        set(value) = prefs.edit().putString(KEY_LAST_ACTION, value).apply()

    override var lastActionResult: String
        get() = prefs.getString(KEY_LAST_RESULT, "Idle") ?: "Idle"
        set(value) = prefs.edit().putString(KEY_LAST_RESULT, value).apply()

    override var detectionCount: Long
        get() = prefs.getLong(KEY_DETECTION_COUNT, 0L)
        set(value) = prefs.edit().putLong(KEY_DETECTION_COUNT, value).apply()

    override var successCount: Long
        get() = prefs.getLong(KEY_SUCCESS_COUNT, 0L)
        set(value) = prefs.edit().putLong(KEY_SUCCESS_COUNT, value).apply()

    override var failureCount: Long
        get() = prefs.getLong(KEY_FAILURE_COUNT, 0L)
        set(value) = prefs.edit().putLong(KEY_FAILURE_COUNT, value).apply()

    /**
     * Today's blocked counter, automatically reset at midnight.
     */
    override fun getBlockedCountToday(): Int {
        checkDailyReset()
        return prefs.getInt(KEY_BLOCKED_COUNT_TODAY, 0)
    }

    @Synchronized
    override fun incrementBlockedCountToday() {
        checkDailyReset()
        val current = prefs.getInt(KEY_BLOCKED_COUNT_TODAY, 0)
        prefs.edit()
            .putInt(KEY_BLOCKED_COUNT_TODAY, current + 1)
            .putLong(KEY_LAST_BLOCKED_TIMESTAMP, System.currentTimeMillis())
            .apply()
    }

    private fun checkDailyReset() {
        val lastTimestamp = prefs.getLong(KEY_LAST_BLOCKED_TIMESTAMP, 0L)
        if (lastTimestamp == 0L) return

        val lastCal = Calendar.getInstance().apply { timeInMillis = lastTimestamp }
        val nowCal = Calendar.getInstance()

        val isDifferentDay = lastCal.get(Calendar.YEAR) != nowCal.get(Calendar.YEAR) ||
                lastCal.get(Calendar.DAY_OF_YEAR) != nowCal.get(Calendar.DAY_OF_YEAR)

        if (isDifferentDay) {
            prefs.edit()
                .putInt(KEY_BLOCKED_COUNT_TODAY, 0)
                .apply()
        }
    }

    companion object {
        private const val PREFS_NAME = "reelguard_local_prefs"
        private const val KEY_BLOCKING_ENABLED = "blocking_enabled"
        private const val KEY_COOLDOWN_MS = "cooldown_ms"
        private const val KEY_BLOCKED_COUNT_TODAY = "blocked_count_today"
        private const val KEY_LAST_BLOCKED_TIMESTAMP = "last_blocked_timestamp"
        private const val KEY_LAST_DETECTION = "last_detection"
        private const val KEY_LAST_ACTION = "last_action"
        private const val KEY_LAST_RESULT = "last_result"
        private const val KEY_DETECTION_COUNT = "detection_count"
        private const val KEY_SUCCESS_COUNT = "success_count"
        private const val KEY_FAILURE_COUNT = "failure_count"

        @Volatile
        private var instance: PreferencesRepository? = null

        fun getInstance(context: Context): PreferencesRepository {
            return instance ?: synchronized(this) {
                instance ?: PreferencesRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}
