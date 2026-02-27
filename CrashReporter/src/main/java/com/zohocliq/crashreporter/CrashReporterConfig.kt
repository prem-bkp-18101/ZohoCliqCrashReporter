package com.zohocliq.crashreporter

/**
 * Configuration class for ZohoCliq Crash Reporter
 *
 * @param dataCenter The Zoho Cliq data center (e.g., "us", "eu", "in", "au", "jp", "ca")
 * @param zapiKey The ZAPI key for authentication
 * @param appKey The application key for identifying the app
 * @param extensionId The Zoho Cliq extension ID (default: 2305843009213702336)
 * @param enableLogging Enable debug logging (default: false)
 */
data class CrashReporterConfig(
    val dataCenter: String,
    val zapiKey: String,
    val appKey: String,
    val extensionId: String = "2305843009213702336",
    val enableLogging: Boolean = false
) {
    /**
     * Constructs the full endpoint URL for Zoho Cliq
     */
    fun getEndpointUrl(): String {
        val baseUrl = when (dataCenter.lowercase()) {
            "us" -> "https://cliq.zoho.com"
            "eu" -> "https://cliq.zoho.eu"
            "in" -> "https://cliq.zoho.in"
            "au" -> "https://cliq.zoho.com.au"
            "jp" -> "https://cliq.zoho.jp"
            "ca" -> "https://cliq.zoho.ca"
            else -> "https://cliq.zoho.com" // Default to US
        }

        return "$baseUrl/api/v2/extensions/$extensionId/incoming?zapikey=$zapiKey&appkey=$appKey"
    }

    /**
     * Validates the configuration
     */
    fun isValid(): Boolean {
        return dataCenter.isNotBlank() &&
               zapiKey.isNotBlank() &&
               appKey.isNotBlank() &&
               extensionId.isNotBlank()
    }
}

