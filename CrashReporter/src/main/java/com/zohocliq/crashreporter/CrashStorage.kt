package com.zohocliq.crashreporter

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Manages storage of crash reports using SharedPreferences
 */
internal class CrashStorage(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    /**
     * Saves a crash report to SharedPreferences
     */
    suspend fun saveCrashReport(crash: CrashReport): Boolean = withContext(Dispatchers.IO) {
        try {
            val key = "${KEY_PREFIX}${crash.timestamp}"
            val json = crash.toJson()

            prefs.edit().putString(key, json).apply()

            // Also maintain a list of crash timestamps
            val crashList = getCrashTimestamps().toMutableList()
            crashList.add(crash.timestamp)
            prefs.edit().putStringSet(KEY_CRASH_LIST, crashList.map { it.toString() }.toSet()).apply()

            true
        } catch (e: Exception) {
            logError("Failed to save crash report", e)
            false
        }
    }

    /**
     * Retrieves all stored crash reports
     */
    suspend fun getAllCrashReports(): List<CrashReport> = withContext(Dispatchers.IO) {
        val crashes = mutableListOf<CrashReport>()
        val timestamps = getCrashTimestamps()

        timestamps.forEach { timestamp ->
            val key = "$KEY_PREFIX$timestamp"
            val json = prefs.getString(key, null)

            if (json != null) {
                val crash = CrashReport.fromJson(json)
                if (crash != null) {
                    crashes.add(crash)
                }
            }
        }

        crashes.sortedBy { it.timestamp }
    }

    /**
     * Deletes a specific crash report
     */
    suspend fun deleteCrashReport(timestamp: Long): Boolean = withContext(Dispatchers.IO) {
        try {
            val key = "$KEY_PREFIX$timestamp"
            prefs.edit().remove(key).apply()

            // Remove from crash list
            val crashList = getCrashTimestamps().toMutableList()
            crashList.remove(timestamp)
            prefs.edit().putStringSet(KEY_CRASH_LIST, crashList.map { it.toString() }.toSet()).apply()

            true
        } catch (e: Exception) {
            logError("Failed to delete crash report", e)
            false
        }
    }

    /**
     * Deletes all crash reports
     */
    suspend fun clearAllCrashReports(): Boolean = withContext(Dispatchers.IO) {
        try {
            val timestamps = getCrashTimestamps()

            val editor = prefs.edit()
            timestamps.forEach { timestamp ->
                editor.remove("$KEY_PREFIX$timestamp")
            }
            editor.remove(KEY_CRASH_LIST)
            editor.apply()

            true
        } catch (e: Exception) {
            logError("Failed to clear crash reports", e)
            false
        }
    }

    /**
     * Gets the count of stored crash reports
     */
    fun getCrashCount(): Int {
        return getCrashTimestamps().size
    }

    /**
     * Checks if there are any pending crash reports
     */
    fun hasPendingCrashes(): Boolean {
        return getCrashCount() > 0
    }

    /**
     * Gets list of crash timestamps
     */
    private fun getCrashTimestamps(): List<Long> {
        val crashSet = prefs.getStringSet(KEY_CRASH_LIST, emptySet()) ?: emptySet()
        return crashSet.mapNotNull { it.toLongOrNull() }.sorted()
    }

    companion object {
        private const val PREFS_NAME = "zoho_cliq_crash_reporter"
        private const val KEY_PREFIX = "crash_"
        private const val KEY_CRASH_LIST = "crash_list"
    }
}

