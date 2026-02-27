package com.zohocliq.crashreporter.example

import android.app.Application
import android.util.Log
import com.zohocliq.crashreporter.BuildConfig
import com.zohocliq.crashreporter.CrashReporter
import com.zohocliq.crashreporter.CrashReporterConfig

/**
 * Example Application class demonstrating how to initialize the Crash Reporter
 *
 * Add this to your AndroidManifest.xml:
 * <application
 *     android:name=".ExampleApplication"
 *     ...>
 */
class ExampleApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        initializeCrashReporter()
    }

    private fun initializeCrashReporter() {
        // Configure the crash reporter
        val config = CrashReporterConfig(
            dataCenter = "us",  // Change to your data center: us, eu, in, au, jp, ca
            zapiKey = "YOUR_ZAPI_KEY",  // Replace with your actual ZAPI key
            appKey = "YOUR_APP_KEY",    // Replace with your app key
            enableLogging = true  // Enable logging for debugging
        )

        // Initialize the crash reporter
        CrashReporter.initialize(this, config)

        // Optional: Set additional data provider
        CrashReporter.getInstance().setAdditionalDataProvider {
            mapOf(
                "user_id" to getCurrentUserId(),
                "session_id" to getCurrentSessionId(),
                "environment" to getEnvironment()
            )
        }

        // Optional: Set crash listener
        CrashReporter.getInstance().setOnCrashListener { crashReport ->
            Log.e("CrashReporter", "Crash detected: ${crashReport.exceptionType}")
            // You can add analytics tracking here
        }

        // Send any pending crashes from previous sessions
        CrashReporter.getInstance().sendPendingCrashes { sentCount, failedCount ->
            if (sentCount > 0) {
                Log.i("CrashReporter", "Successfully sent $sentCount crash report(s)")
            }
            if (failedCount > 0) {
                Log.w("CrashReporter", "Failed to send $failedCount crash report(s)")
            }
        }
    }

    // Example methods for additional data
    private fun getCurrentUserId(): String {
        // Replace with your actual user ID retrieval logic
        return "user_123"
    }

    private fun getCurrentSessionId(): String {
        // Replace with your actual session ID retrieval logic
        return "session_${System.currentTimeMillis()}"
    }

    private fun getEnvironment(): String {
        // Replace with your actual environment detection logic
        return if (BuildConfig.DEBUG) "debug" else "production"
    }
}
