package com.zohocliq.crashreporter

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Service for sending crash reports to Zoho Cliq
 */
internal class ZohoCliqService(private val config: CrashReporterConfig) {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * Sends a crash report to Zoho Cliq
     *
     * @param crash The crash report to send
     * @return true if successful, false otherwise
     */
    suspend fun sendCrashReport(crash: CrashReport): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val message = crash.toCliqMessage()
            val payload = createCliqPayload(message)

            val request = Request.Builder()
                .url(config.getEndpointUrl())
                .post(payload.toRequestBody("application/json".toMediaType()))
                .build()

            logDebug("Sending crash report to: ${config.getEndpointUrl()}")

            val response = client.newCall(request).execute()

            response.use {
                if (it.isSuccessful) {
                    logDebug("Crash report sent successfully: ${it.code}")
                    Result.success(true)
                } else {
                    val errorBody = it.body?.string() ?: "Unknown error"
                    logError("Failed to send crash report: ${it.code} - $errorBody")
                    Result.failure(IOException("HTTP ${it.code}: $errorBody"))
                }
            }
        } catch (e: IOException) {
            logError("Network error while sending crash report", e)
            Result.failure(e)
        } catch (e: Exception) {
            logError("Unexpected error while sending crash report", e)
            Result.failure(e)
        }
    }

    /**
     * Sends multiple crash reports in batch
     *
     * @param crashes List of crash reports to send
     * @return Map of timestamp to success status
     */
    suspend fun sendCrashReportsBatch(crashes: List<CrashReport>): Map<Long, Boolean> {
        val results = mutableMapOf<Long, Boolean>()

        crashes.forEach { crash ->
            val result = sendCrashReport(crash)
            results[crash.timestamp] = result.isSuccess

            // Small delay between requests to avoid rate limiting
            if (crashes.size > 1) {
                kotlinx.coroutines.delay(1000)
            }
        }

        return results
    }

    /**
     * Creates a Zoho Cliq compatible JSON payload
     */
    private fun createCliqPayload(message: String): String {
        return """
            {
                "text": ${message.toJsonString()}
            }
        """.trimIndent()
    }

    /**
     * Converts a string to JSON-safe format
     */
    private fun String.toJsonString(): String {
        val escaped = this.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
            .replace("\b", "\\b")
            .replace("\u000C", "\\f")

        return "\"$escaped\""
    }
}

