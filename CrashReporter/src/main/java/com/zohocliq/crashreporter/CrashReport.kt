package com.zohocliq.crashreporter

import android.content.Context
import android.os.Build
import android.provider.Settings
import java.util.UUID

/**
 * Data class representing a crash report
 */
data class CrashReport(
    val timestamp: Long,
    val exceptionType: String,
    val exceptionMessage: String,
    val stackTrace: String,
    val appVersion: String,
    val deviceInfo: DeviceInfo,
    val threadName: String,
    val additionalData: Map<String, String> = emptyMap()
) {
    /**
     * Converts crash report to JSON string for storage
     */
    fun toJson(): String {
        return """
            {
                "timestamp": $timestamp,
                "exceptionType": "${exceptionType.escape()}",
                "exceptionMessage": "${exceptionMessage.escape()}",
                "stackTrace": "${stackTrace.escape()}",
                "appVersion": "${appVersion.escape()}",
                "deviceInfo": {
                    "manufacturer": "${deviceInfo.manufacturer.escape()}",
                    "model": "${deviceInfo.model.escape()}",
                    "androidVersion": "${deviceInfo.androidVersion.escape()}",
                    "sdkInt": ${deviceInfo.sdkInt},
                    "deviceId": "${deviceInfo.deviceId.escape()}"
                },
                "threadName": "${threadName.escape()}",
                "additionalData": ${additionalData.toJsonMap()}
            }
        """.trimIndent()
    }

    companion object {
        /**
         * Creates a crash report from a throwable
         */
        fun fromThrowable(
            throwable: Throwable,
            appVersion: String,
            additionalData: Map<String, String> = emptyMap()
        ): CrashReport {
            val stackTraceString = throwable.stackTraceToString()
            val thread = Thread.currentThread()

            return CrashReport(
                timestamp = System.currentTimeMillis(),
                exceptionType = throwable.javaClass.name,
                exceptionMessage = throwable.message ?: "",
                stackTrace = stackTraceString,
                appVersion = appVersion,
                deviceInfo = DeviceInfo.current(),
                threadName = thread.name,
                additionalData = additionalData
            )
        }

        /**
         * Creates a crash report from a throwable with custom device info
         */
        fun fromThrowable(
            throwable: Throwable,
            appVersion: String,
            deviceInfo: DeviceInfo,
            additionalData: Map<String, String> = emptyMap()
        ): CrashReport {
            val stackTraceString = throwable.stackTraceToString()
            val thread = Thread.currentThread()

            return CrashReport(
                timestamp = System.currentTimeMillis(),
                exceptionType = throwable.javaClass.name,
                exceptionMessage = throwable.message ?: "",
                stackTrace = stackTraceString,
                appVersion = appVersion,
                deviceInfo = deviceInfo,
                threadName = thread.name,
                additionalData = additionalData
            )
        }

        /**
         * Parses a crash report from JSON string
         */
        fun fromJson(json: String): CrashReport? {
            return try {
                // Simple JSON parsing without external dependencies
                val timestamp = json.extractJsonLong("timestamp") ?: return null
                val exceptionType = json.extractJsonString("exceptionType") ?: return null
                val exceptionMessage = json.extractJsonString("exceptionMessage") ?: ""
                val stackTrace = json.extractJsonString("stackTrace") ?: return null
                val appVersion = json.extractJsonString("appVersion") ?: return null
                val threadName = json.extractJsonString("threadName") ?: "Unknown"

                val manufacturer = json.extractJsonString("manufacturer") ?: "Unknown"
                val model = json.extractJsonString("model") ?: "Unknown"
                val androidVersion = json.extractJsonString("androidVersion") ?: "Unknown"
                val sdkInt = json.extractJsonInt("sdkInt") ?: 0
                val deviceId = json.extractJsonString("deviceId") ?: "Unknown"

                CrashReport(
                    timestamp = timestamp,
                    exceptionType = exceptionType,
                    exceptionMessage = exceptionMessage,
                    stackTrace = stackTrace,
                    appVersion = appVersion,
                    deviceInfo = DeviceInfo(manufacturer, model, androidVersion, sdkInt, deviceId),
                    threadName = threadName,
                    additionalData = emptyMap()
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}

/**
 * Device information data class
 */
data class DeviceInfo(
    val manufacturer: String,
    val model: String,
    val androidVersion: String,
    val sdkInt: Int,
    val deviceId: String
) {
    companion object {
        fun current(context: Context? = null): DeviceInfo {
            val deviceId = if (context == null) {
                "Unknown"
            } else {
                getDeviceId(context)
            }

            return DeviceInfo(
                manufacturer = Build.MANUFACTURER,
                model = Build.MODEL,
                androidVersion = Build.VERSION.RELEASE,
                sdkInt = Build.VERSION.SDK_INT,
                deviceId = deviceId
            )
        }

        private fun getDeviceId(context: Context): String {
            // Try to get Android ID first
            val androidId = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ANDROID_ID
            )

            if (!androidId.isNullOrBlank()) {
                return androidId
            }

            // If Android ID unavailable, use persisted UUID
            val prefs = context.getSharedPreferences("zoho_cliq_crash_reporter", Context.MODE_PRIVATE)
            val cached = prefs.getString("device_id", null)
            if (!cached.isNullOrBlank()) {
                return cached
            }

            // Generate and store new UUID
            val generated = UUID.randomUUID().toString()
            prefs.edit().putString("device_id", generated).apply()
            return generated
        }
    }
}

// Extension functions for JSON handling
private fun String.escape(): String {
    return this.replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t")
}

private fun Map<String, String>.toJsonMap(): String {
    if (isEmpty()) return "{}"
    return entries.joinToString(
        prefix = "{",
        postfix = "}",
        separator = ","
    ) { (key, value) ->
        "\"${key.escape()}\": \"${value.escape()}\""
    }
}

private fun String.extractJsonString(key: String): String? {
    val pattern = """"$key"\s*:\s*"([^"\\]*(\\.[^"\\]*)*)"""".toRegex()
    return pattern.find(this)?.groupValues?.get(1)?.unescape()
}

private fun String.extractJsonLong(key: String): Long? {
    val pattern = """"$key"\s*:\s*(\d+)""".toRegex()
    return pattern.find(this)?.groupValues?.get(1)?.toLongOrNull()
}

private fun String.extractJsonInt(key: String): Int? {
    val pattern = """"$key"\s*:\s*(\d+)""".toRegex()
    return pattern.find(this)?.groupValues?.get(1)?.toIntOrNull()
}

private fun String.unescape(): String {
    return this.replace("\\n", "\n")
        .replace("\\r", "\r")
        .replace("\\t", "\t")
        .replace("\\\"", "\"")
        .replace("\\\\", "\\")
}
