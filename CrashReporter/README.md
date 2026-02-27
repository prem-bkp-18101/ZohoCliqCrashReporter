# Zoho Cliq Crash Reporter

A lightweight Android crash reporting library that sends crash reports to Zoho Cliq channels. The library automatically captures uncaught exceptions and stores them in SharedPreferences, then uploads them to Zoho Cliq either immediately or on the next app launch.

## Features

- 🚨 **Automatic Crash Detection**: Captures uncaught exceptions automatically
- 💾 **Offline Support**: Stores crashes in SharedPreferences when network is unavailable
- 🔄 **Auto Retry**: Sends pending crashes on next app launch
- 🌍 **Multi-Region Support**: Supports all Zoho data centers (US, EU, IN, AU, JP, CA)
- 📊 **Rich Crash Reports**: Includes device info, stack traces, app version, and custom data
- 🪶 **Lightweight**: No database dependencies, minimal overhead
- 🔧 **Simple Integration**: Easy 3-step setup

## Requirements

- Android API 24 (Android 7.0) or higher
- Internet permission

## Installation

Add the CrashReporter module to your project and include it in your app's dependencies:

```gradle
dependencies {
    implementation(project(":CrashReporter"))
}
```

## Setup

### 1. Get Your Zoho Cliq Credentials

You need the following information from your Zoho Cliq webhook:

- **Zoho Domain**: Your Zoho region domain (e.g., "cliq.zoho.com", "cliq.zoho.eu")
- **ZAPI Key**: The `zapikey` parameter from your webhook URL

**Example Webhook URL:**
```
https://cliq.zoho.com/api/v2/extensions/2305843009213702336/incoming?zapikey=1001.xxxxx&appkey=crash_reporter
```

From this URL:
- Domain: `cliq.zoho.com`
- ZAPI Key: `1001.xxxxx`

### 2. Initialize in Application Class

```kotlin
import android.app.Application
import com.zohocliq.crashreporter.CrashReporter
import com.zohocliq.crashreporter.CrashReporterConfig

class MyApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Configure the crash reporter
        val config = CrashReporterConfig(
            domain = "cliq.zoho.com",     // Your Zoho domain
            zapiKey = "your_zapi_key",    // Your ZAPI key
            enableLogging = BuildConfig.DEBUG // Enable logs in debug builds
        )
        
        // Initialize the crash reporter
        CrashReporter.initialize(this, config)
        
        // Send any pending crashes from previous sessions
        CrashReporter.getInstance().sendPendingCrashes { sentCount, failedCount ->
            Log.d("CrashReporter", "Sent $sentCount crashes, $failedCount failed")
        }
    }
}
```

### 3. Add Application Class to Manifest

```xml
<application
    android:name=".MyApplication"
    ...>
    ...
</application>
```

## Usage

### Automatic Crash Reporting

Once initialized, the library automatically captures all uncaught exceptions. No additional code is needed!

### Manual Exception Reporting

You can manually report exceptions:

```kotlin
try {
    // Your code
} catch (e: Exception) {
    CrashReporter.getInstance().reportException(e)
}
```

### Add Custom Data

You can add custom data to crash reports:

```kotlin
// Set a provider for additional data
CrashReporter.getInstance().setAdditionalDataProvider {
    mapOf(
        "user_id" to getCurrentUserId(),
        "screen" to getCurrentScreen(),
        "custom_flag" to getCustomFlag()
    )
}

// Or pass data when manually reporting
CrashReporter.getInstance().reportException(
    throwable = exception,
    additionalData = mapOf(
        "action" to "checkout",
        "item_count" to "5"
    )
)
```

### Listen to Crash Events

```kotlin
CrashReporter.getInstance().setOnCrashListener { crashReport ->
    // Handle crash event (e.g., log to analytics)
    Log.e("Crash", "App crashed: ${crashReport.exceptionType}")
}
```

### Check Pending Crashes

```kotlin
// Check if there are pending crashes
if (CrashReporter.getInstance().hasPendingCrashes()) {
    val count = CrashReporter.getInstance().getPendingCrashCount()
    Log.d("CrashReporter", "Found $count pending crashes")
}
```

### Clear Pending Crashes

```kotlin
// Clear all stored crash reports
CrashReporter.getInstance().clearPendingCrashes { success ->
    if (success) {
        Log.d("CrashReporter", "Cleared all pending crashes")
    }
}
```

## Configuration Options

### CrashReporterConfig

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `dataCenter` | String | Yes | Zoho data center: "us", "eu", "in", "au", "jp", "ca" |
| `zapiKey` | String | Yes | ZAPI key from Zoho Cliq webhook |
| `appKey` | String | Yes | Unique identifier for your app |
| `extensionId` | String | No | Extension ID (default: 2305843009213702336) |
| `enableLogging` | Boolean | No | Enable debug logging (default: false) |

### Data Center Mapping

| Data Center | Base URL |
|-------------|----------|
| `us` | https://cliq.zoho.com |
| `eu` | https://cliq.zoho.eu |
| `in` | https://cliq.zoho.in |
| `au` | https://cliq.zoho.com.au |
| `jp` | https://cliq.zoho.jp |
| `ca` | https://cliq.zoho.ca |

## API Reference

### CrashReporter

#### Static Methods

- `initialize(context: Context, config: CrashReporterConfig): CrashReporter`
  - Initialize the crash reporter. Call once in `Application.onCreate()`

- `getInstance(): CrashReporter`
  - Get the singleton instance. Throws if not initialized.

- `isInitialized(): Boolean`
  - Check if the crash reporter is initialized

#### Instance Methods

- `reportException(throwable: Throwable, additionalData: Map<String, String> = emptyMap())`
  - Manually report an exception

- `sendPendingCrashes(onComplete: ((sentCount: Int, failedCount: Int) -> Unit)? = null)`
  - Send all stored crash reports

- `setAdditionalDataProvider(provider: () -> Map<String, String>)`
  - Set a callback to provide additional data for all crashes

- `setOnCrashListener(listener: (CrashReport) -> Unit)`
  - Set a listener for crash events

- `getPendingCrashCount(): Int`
  - Get the number of pending crash reports

- `hasPendingCrashes(): Boolean`
  - Check if there are pending crashes

- `clearPendingCrashes(onComplete: ((Boolean) -> Unit)? = null)`
  - Clear all stored crash reports

## Best Practices

1. **Initialize Early**: Initialize in `Application.onCreate()` before any other code
2. **Send Pending Crashes**: Call `sendPendingCrashes()` after initialization to upload stored crashes
3. **Enable Logging in Debug**: Set `enableLogging = true` for debug builds to see what's happening
4. **Add Contextual Data**: Use `setAdditionalDataProvider()` to add user context to crashes
5. **Handle Sensitive Data**: Don't include sensitive information in additional data
6. **Network Availability**: The library handles offline scenarios automatically

## Troubleshooting

### Crashes Not Being Sent

1. Check internet permission in AndroidManifest.xml
2. Verify your ZAPI key and app key are correct
3. Enable logging and check logcat for errors
4. Ensure you're calling `sendPendingCrashes()` after initialization

### Invalid Configuration Error

Ensure all required parameters are provided:
- `dataCenter` must not be blank
- `zapiKey` must not be blank
- `appKey` must not be blank

### Network Errors

If crashes fail to send:
- Check network connectivity
- Verify the webhook URL is accessible
- Check Zoho Cliq webhook settings
- Crashes are stored locally and will retry on next launch

## Storage

Crashes are stored in SharedPreferences at:
- Preferences file: `zoho_cliq_crash_reporter`
- Each crash is stored with key: `crash_{timestamp}`
- List of crash timestamps: `crash_list`

## Permissions

The library requires:

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

This permission is already included in the library's manifest and will be merged automatically.

