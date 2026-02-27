package com.zohocliq.crashreporter

import android.util.Log

/**
 * Simple logging utilities
 */
internal var isLoggingEnabled = false

internal fun logDebug(message: String) {
    if (isLoggingEnabled) {
        Log.d(TAG, message)
    }
}

internal fun logError(message: String, throwable: Throwable? = null) {
    if (isLoggingEnabled) {
        if (throwable != null) {
            Log.e(TAG, message, throwable)
        } else {
            Log.e(TAG, message)
        }
    }
}

internal fun logInfo(message: String) {
    if (isLoggingEnabled) {
        Log.i(TAG, message)
    }
}

internal fun logWarning(message: String) {
    if (isLoggingEnabled) {
        Log.w(TAG, message)
    }
}

private const val TAG = "ZohoCliqCrashReporter"

