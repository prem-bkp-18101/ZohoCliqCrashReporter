package com.zohocliq.crashreporter.example

import android.app.Application
import android.util.Log
import com.zohocliq.crashreporter.BuildConfig
import com.zohocliq.crashreporter.CrashReporter
import com.zohocliq.crashreporter.CrashReporterConfig

/**
 * Example Application class demonstrating how to initialize the Crash Reporter.
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
        val config = CrashReporterConfig(
            domain = "cliq.zoho.com",  // Change to your domain (e.g., "cliq.zoho.eu" for EU)
            zapiKey = "YOUR_ZAPI_KEY",  // Replace with your actual ZAPI key obtained from Zoho Cliq
            enableLogging = true  // Enable logging for debugging
        )

        CrashReporter.initialize(this, config)

        CrashReporter.getInstance().setAdditionalDataProvider {
            mapOf(
                "user_id" to getCurrentUserId(),
                "session_id" to getCurrentSessionId(),
                "environment" to getEnvironment()
            )
        }

        CrashReporter.getInstance().setOnCrashListener { crashReport ->
            Log.e("CrashReporter", "Crash detected: ${crashReport.exceptionType}")
        }

        CrashReporter.getInstance().sendPendingCrashes { sentCount, failedCount ->
            if (sentCount > 0) {
                Log.i("CrashReporter", "Successfully sent $sentCount crash report(s)")
            }
            if (failedCount > 0) {
                Log.w("CrashReporter", "Failed to send $failedCount crash report(s)")
            }
        }
    }

    private fun getCurrentUserId(): String {
        return "user_123"
    }

    private fun getCurrentSessionId(): String {
        return "session_${System.currentTimeMillis()}"
    }

    private fun getEnvironment(): String {
        return if (BuildConfig.DEBUG) "debug" else "production"
    }
}
