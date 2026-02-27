package com.zohocliq.crashreporter

/**
 * Configuration class for ZohoCliq Crash Reporter.
 *
 * End users provide only domain and zapiKey. Environment-specific
 * extension credentials are kept internal to the library.
 *
 * @param domain The Zoho Cliq domain (e.g., "cliq.zoho.com" or "cliq.zoho.eu")
 * @param zapiKey The ZAPI key for authentication (obtained from Zoho Cliq webhook)
 * @param enableLogging Enable debug logging (default: false)
 */
data class CrashReporterConfig(
    val domain: String,
    val zapiKey: String,
    val enableLogging: Boolean = false
) {
    private enum class InternalEnvironment {
        TESTING_SANDBOX,
        TESTING_LIVE,
        PRODUCTION_SANDBOX,
        PRODUCTION_LIVE
    }

    private data class InternalCredentials(
        val extensionId: String,
        val appKey: String
    )

    private val environment: InternalEnvironment = InternalEnvironment.PRODUCTION_SANDBOX

    private fun getCredentials(): InternalCredentials {
        return when (environment) {
            InternalEnvironment.TESTING_SANDBOX -> InternalCredentials(
                extensionId = "1",
                appKey = "a"
            )
            InternalEnvironment.TESTING_LIVE -> InternalCredentials(
                extensionId = "1234",
                appKey = "abc"
            )
            InternalEnvironment.PRODUCTION_SANDBOX -> InternalCredentials(
                extensionId = "2305843009213702336",
                appKey = "sbx-ODM4NS1hZTdmMDg2Zi1iOGJkLTRhNzUtYWY4OC1hMTFiYjZlMTcxYTQ="
            )
            InternalEnvironment.PRODUCTION_LIVE -> InternalCredentials(
                extensionId = "8385",
                appKey = "ODM4NS1hZTdmMDg2Zi1iOGJkLTRhNzUtYWY4OC1hMTFiYjZlMTcxYTQ="
            )
        }
    }

    /**
     * Constructs the full endpoint URL for Zoho Cliq.
     */
    fun getEndpointUrl(): String {
        val credentials = getCredentials()

        var cleanDomain = domain.trim()
        if (!cleanDomain.startsWith("http")) {
            cleanDomain = "https://$cleanDomain"
        }
        if (cleanDomain.endsWith("/")) {
            cleanDomain = cleanDomain.substring(0, cleanDomain.length - 1)
        }

        return "$cleanDomain/api/v2/applications/${credentials.extensionId}/incoming?zapikey=$zapiKey&appkey=${credentials.appKey}"
    }

    /**
     * Validates the configuration.
     */
    fun isValid(): Boolean {
        return domain.isNotBlank() && zapiKey.isNotBlank()
    }
}
