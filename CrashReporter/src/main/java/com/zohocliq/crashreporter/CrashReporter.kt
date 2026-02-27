package com.zohocliq.crashreporter

import android.content.Context
import android.content.pm.PackageInfo
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.lang.Thread.UncaughtExceptionHandler

/**
 * Main Crash Reporter class
 *
 * Usage:
 * ```kotlin
 * // Initialize in Application class
 * val config = CrashReporterConfig(
 *     dataCenter = "us",
 *     zapiKey = "your_zapi_key",
 *     appKey = "your_app_key"
 * )
 * CrashReporter.initialize(context, config)
 *
 * // Send pending crashes on app restart
 * CrashReporter.getInstance().sendPendingCrashes()
 * ```
 */
class CrashReporter private constructor(
    private val context: Context,
    private val config: CrashReporterConfig
) {

    private val storage: CrashStorage = CrashStorage(context)
    private val service: ZohoCliqService = ZohoCliqService(config)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val defaultExceptionHandler: UncaughtExceptionHandler? =
        Thread.getDefaultUncaughtExceptionHandler()

    private var additionalDataProvider: (() -> Map<String, String>)? = null
    private var onCrashListener: ((CrashReport) -> Unit)? = null

    init {
        isLoggingEnabled = config.enableLogging
        logInfo("CrashReporter initialized with config: DC=${config.dataCenter}, AppKey=${config.appKey}")

        // Set up uncaught exception handler
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            handleUncaughtException(thread, throwable)

            // Call the original handler
            defaultExceptionHandler?.uncaughtException(thread, throwable)
        }
    }

    /**
     * Sets a provider for additional data to be included in crash reports
     */
    fun setAdditionalDataProvider(provider: () -> Map<String, String>) {
        this.additionalDataProvider = provider
    }

    /**
     * Sets a listener to be notified when a crash occurs
     */
    fun setOnCrashListener(listener: (CrashReport) -> Unit) {
        this.onCrashListener = listener
    }

    /**
     * Manually report an exception
     */
    fun reportException(throwable: Throwable, additionalData: Map<String, String> = emptyMap()) {
        scope.launch {
            try {
                val mergedData = mergeAdditionalData(additionalData)
                val crash = CrashReport.fromThrowable(
                    throwable = throwable,
                    appVersion = getAppVersion(),
                    additionalData = mergedData
                )

                logInfo("Manually reporting exception: ${throwable.javaClass.simpleName}")

                // Try to send directly first
                val result = service.sendCrashReport(crash)

                if (result.isSuccess) {
                    logInfo("Exception report sent successfully")
                } else {
                    // If failed, store for later
                    logWarning("Failed to send exception report, storing for later")
                    storage.saveCrashReport(crash)
                }

                onCrashListener?.invoke(crash)
            } catch (e: Exception) {
                logError("Failed to report exception", e)
            }
        }
    }

    /**
     * Sends all pending crash reports
     * Call this on app restart, preferably in Application.onCreate()
     */
    fun sendPendingCrashes(onComplete: ((sentCount: Int, failedCount: Int) -> Unit)? = null) {
        scope.launch {
            try {
                val crashes = storage.getAllCrashReports()

                if (crashes.isEmpty()) {
                    logInfo("No pending crashes to send")
                    onComplete?.invoke(0, 0)
                    return@launch
                }

                logInfo("Found ${crashes.size} pending crashes to send")

                val results = service.sendCrashReportsBatch(crashes)

                var sentCount = 0
                var failedCount = 0

                results.forEach { (timestamp, success) ->
                    if (success) {
                        storage.deleteCrashReport(timestamp)
                        sentCount++
                    } else {
                        failedCount++
                    }
                }

                logInfo("Sent $sentCount crashes, $failedCount failed")
                onComplete?.invoke(sentCount, failedCount)

            } catch (e: Exception) {
                logError("Failed to send pending crashes", e)
                onComplete?.invoke(0, storage.getCrashCount())
            }
        }
    }

    /**
     * Gets the count of pending crash reports
     */
    fun getPendingCrashCount(): Int {
        return storage.getCrashCount()
    }

    /**
     * Checks if there are pending crash reports
     */
    fun hasPendingCrashes(): Boolean {
        return storage.hasPendingCrashes()
    }

    /**
     * Clears all stored crash reports
     */
    fun clearPendingCrashes(onComplete: ((Boolean) -> Unit)? = null) {
        scope.launch {
            val success = storage.clearAllCrashReports()
            logInfo("Cleared all pending crashes: $success")
            onComplete?.invoke(success)
        }
    }

    /**
     * Handles uncaught exceptions
     */
    private fun handleUncaughtException(thread: Thread, throwable: Throwable) {
        try {
            logError("Uncaught exception in thread ${thread.name}", throwable)

            val additionalData = mergeAdditionalData(emptyMap())
            val crash = CrashReport.fromThrowable(
                throwable = throwable,
                appVersion = getAppVersion(),
                additionalData = additionalData
            )

            // Try to send immediately
            try {
                // Use runBlocking to attempt immediate send on the crashing thread
                val result = runBlocking {
                    service.sendCrashReport(crash)
                }

                if (result.isFailure) {
                    // If immediate send fails, store for later
                    // Note: This runs synchronously because app is about to crash
                    storeCrashSync(crash)
                }
            } catch (e: Exception) {
                // If sending fails, store for later
                storeCrashSync(crash)
            }

            onCrashListener?.invoke(crash)

        } catch (e: Exception) {
            logError("Failed to handle uncaught exception", e)
        }
    }

    /**
     * Synchronously stores a crash report
     */
    private fun storeCrashSync(crash: CrashReport) {
        try {
            val prefs = context.getSharedPreferences("zoho_cliq_crash_reporter", Context.MODE_PRIVATE)
            val key = "crash_${crash.timestamp}"
            prefs.edit().putString(key, crash.toJson()).commit()

            val crashList = prefs.getStringSet("crash_list", emptySet())?.toMutableSet() ?: mutableSetOf()
            crashList.add(crash.timestamp.toString())
            prefs.edit().putStringSet("crash_list", crashList).commit()

            logInfo("Crash report stored for later upload")
        } catch (e: Exception) {
            logError("Failed to store crash report", e)
        }
    }

    /**
     * Merges additional data from provider with provided data
     */
    private fun mergeAdditionalData(providedData: Map<String, String>): Map<String, String> {
        val providerData = try {
            additionalDataProvider?.invoke() ?: emptyMap()
        } catch (e: Exception) {
            logError("Failed to get additional data from provider", e)
            emptyMap()
        }

        return providerData + providedData
    }

    /**
     * Gets the app version
     */
    private fun getAppVersion(): String {
        return try {
            val packageInfo: PackageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    android.content.pm.PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                "${packageInfo.versionName} (${packageInfo.longVersionCode})"
            } else {
                @Suppress("DEPRECATION")
                "${packageInfo.versionName} (${packageInfo.versionCode})"
            }
        } catch (e: Exception) {
            "Unknown"
        }
    }

    companion object {
        @Volatile
        private var instance: CrashReporter? = null

        /**
         * Initializes the crash reporter
         * Should be called in Application.onCreate()
         */
        fun initialize(context: Context, config: CrashReporterConfig): CrashReporter {
            if (!config.isValid()) {
                throw IllegalArgumentException("Invalid CrashReporterConfig: ensure dataCenter, zapiKey, and appKey are provided")
            }

            return instance ?: synchronized(this) {
                instance ?: CrashReporter(context.applicationContext, config).also {
                    instance = it
                }
            }
        }

        /**
         * Gets the singleton instance
         * Must call initialize() first
         */
        fun getInstance(): CrashReporter {
            return instance ?: throw IllegalStateException(
                "CrashReporter not initialized. Call CrashReporter.initialize() first"
            )
        }

        /**
         * Checks if the crash reporter is initialized
         */
        fun isInitialized(): Boolean {
            return instance != null
        }
    }
}

