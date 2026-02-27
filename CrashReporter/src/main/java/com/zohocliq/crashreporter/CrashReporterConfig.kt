package com.zohocliq.crashreporter

/**
 * Configuration class for ZohoCliq Crash Reporter
 *
 * @param domain The Zoho Cliq domain (e.g., "cliq.zoho.com" or "cliq.zoho.eu")
 * @param zapiKey The ZAPI key for authentication
 * @param extensionId The Zoho Cliq extension ID (default: 2305843009213702336)
 * @param enableLogging Enable debug logging (default: false)
 */
data class CrashReporterConfig(
    val domain: String, // e.g. "cliq.zoho.com" or "https://cliq.zoho.com"
    val zapiKey: String,
    val extensionId: String = "2305843009213702336",
    val enableLogging: Boolean = false
) {
    private val appKey: String = "sbx-ODM4NS1hZTdmMDg2Zi1iOGJkLTRhNzUtYWY4OC1hMTFiYjZlMTcxYTQ="

    /**
     * Constructs the full endpoint URL for Zoho Cliq
     */
    fun getEndpointUrl(): String {
        // Clean domain to ensure it doesn't have protocol or trailing slash if not needed,
        // or just ensure it starts with https:// if missing.

        var cleanDomain = domain.trim()
        if (!cleanDomain.startsWith("http")) {
            cleanDomain = "https://$cleanDomain"
        }
        if (cleanDomain.endsWith("/")) {
            cleanDomain = cleanDomain.substring(0, cleanDomain.length - 1)
        }

        return "$cleanDomain/api/v2/extensions/$extensionId/incoming?zapikey=$zapiKey&appkey=$appKey"
    }

    /**
     * Validates the configuration
     */
    fun isValid(): Boolean {
        return domain.isNotBlank() &&
               zapiKey.isNotBlank() &&
               extensionId.isNotBlank()
    }
}

