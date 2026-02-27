# Zoho Cliq Crash Reporter

A lightweight Android crash reporting library that automatically sends crash reports to Zoho Cliq channels.

## Features

- 🚨 **Automatic Crash Detection** - Captures uncaught exceptions automatically
- 💾 **Offline Support** - Stores crashes locally when network is unavailable
- 🔄 **Auto Retry** - Sends pending crashes on next app launch
- 🌍 **Multi-Region Support** - Works with all Zoho data centers
- 📊 **Rich Reports** - Includes device info, stack traces, and custom data
- 🪶 **Lightweight** - No database, uses SharedPreferences only
- 🔧 **Simple Setup** - Just 3 steps to integrate

## Requirements

- Android API 24+ (Android 7.0 or higher)
- Internet permission (automatically added)

## Quick Start

### Step 1: Add Dependency

Add to your app's `build.gradle.kts`:

```kotlin
dependencies {
    implementation(project(":CrashReporter"))
}
```

### Step 2: Get Credentials

From your Zoho Cliq webhook URL, extract:

```
https://cliq.zoho.com/api/v2/extensions/xxx/incoming?zapikey=1001.xxxxx&appkey=yyy
```

- **Domain**: `cliq.zoho.com` (or your region's domain)
- **ZAPI Key**: `1001.xxxxx` (the zapikey parameter)

### Step 3: Initialize

Create an Application class:

```kotlin
import android.app.Application
import com.zohocliq.crashreporter.CrashReporter
import com.zohocliq.crashreporter.CrashReporterConfig

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        val config = CrashReporterConfig(
            domain = "cliq.zoho.com",
            zapiKey = "YOUR_ZAPI_KEY",
            enableLogging = BuildConfig.DEBUG
        )
        
        CrashReporter.initialize(this, config)
        CrashReporter.getInstance().sendPendingCrashes()
    }
}
```

Register in `AndroidManifest.xml`:

```xml
<application
    android:name=".MyApplication"
    ...>
</application>
```

**That's it!** Crashes are now automatically reported.

---

## Configuration

### CrashReporterConfig Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `domain` | String | Yes | Your Zoho domain (e.g., "cliq.zoho.com", "cliq.zoho.eu") |
| `zapiKey` | String | Yes | ZAPI key from Zoho Cliq webhook |
| `enableLogging` | Boolean | No | Enable debug logs (default: false) |

### Regional Domains

| Region | Domain |
|--------|--------|
| US | cliq.zoho.com |
| EU | cliq.zoho.eu |
| India | cliq.zoho.in |
| Australia | cliq.zoho.com.au |
| Japan | cliq.zoho.jp |
| Canada | cliq.zoho.ca |

---

## Usage Examples

### Add Custom Data to Crashes

```kotlin
CrashReporter.getInstance().setAdditionalDataProvider {
    mapOf(
        "user_id" to getCurrentUserId(),
        "screen" to getCurrentScreen(),
        "session_id" to getSessionId()
    )
}
```

### Manual Exception Reporting

```kotlin
try {
    riskyOperation()
} catch (e: Exception) {
    CrashReporter.getInstance().reportException(e)
}

// Or with additional context
CrashReporter.getInstance().reportException(
    throwable = exception,
    additionalData = mapOf("context" to "checkout")
)
```

### Listen for Crash Events

```kotlin
CrashReporter.getInstance().setOnCrashListener { crashReport ->
    // Log to analytics, send to another service, etc.
    analytics.logEvent("app_crash", crashReport.exceptionType)
}
```

### Check Pending Crashes

```kotlin
if (CrashReporter.getInstance().hasPendingCrashes()) {
    val count = CrashReporter.getInstance().getPendingCrashCount()
    Log.d("App", "Found $count pending crashes")
}
```

### Clear Stored Crashes

```kotlin
CrashReporter.getInstance().clearPendingCrashes { success ->
    Log.d("App", "Crashes cleared: $success")
}
```

---

## API Reference

### CrashReporter (Singleton)

#### Initialization
- `initialize(context: Context, config: CrashReporterConfig)` - Initialize the library (call once)
- `getInstance()` - Get the singleton instance
- `isInitialized()` - Check if initialized

#### Crash Reporting
- `reportException(throwable: Throwable, additionalData: Map<String, String> = emptyMap())` - Manually report an exception
- `sendPendingCrashes(onComplete: ((Int, Int) -> Unit)? = null)` - Send all stored crashes

#### Configuration
- `setAdditionalDataProvider(provider: () -> Map<String, String>)` - Set callback for custom data
- `setOnCrashListener(listener: (CrashReport) -> Unit)` - Listen for crash events

#### Query
- `getPendingCrashCount(): Int` - Get number of stored crashes
- `hasPendingCrashes(): Boolean` - Check if crashes are stored
- `clearPendingCrashes(onComplete: ((Boolean) -> Unit)? = null)` - Clear all stored crashes

---

## Crash Report Format

Crashes appear in Zoho Cliq as formatted messages:

```
🚨 Crash Report

Time: 2026-02-27 15:30:45
App Version: 1.0.0 (1)

Exception Type: java.lang.NullPointerException
Message: Attempt to invoke virtual method...
Thread: main

Device Information:
- Model: Pixel 6
- Manufacturer: Google
- Android Version: 14 (API 34)

Stack Trace:
com.example.MainActivity.onCreate(MainActivity.kt:45)
android.app.Activity.performCreate(Activity.java:8000)
...

Additional Data:
- user_id: 12345
- screen: HomeScreen
```

---

## Best Practices

1. **Initialize Early** - Call `initialize()` in `Application.onCreate()` before any other code
2. **Send Pending** - Always call `sendPendingCrashes()` after initialization
3. **Debug Logging** - Use `enableLogging = BuildConfig.DEBUG` to see logs in development
4. **Custom Data** - Add user context with `setAdditionalDataProvider()` for better debugging
5. **Sensitive Data** - Never include passwords, tokens, or PII in additional data
6. **Test Crashes** - Test with `throw RuntimeException("Test crash")` in debug builds

---

## Troubleshooting

### Crashes Not Appearing in Cliq?

1. ✅ Check ZAPI key is correct
2. ✅ Verify internet permission in AndroidManifest.xml
3. ✅ Enable logging: `enableLogging = true`
4. ✅ Check logcat for error messages
5. ✅ Confirm webhook is active in Zoho Cliq
6. ✅ Test network connectivity

### "CrashReporter not initialized" Error?

- Ensure `CrashReporter.initialize()` is called in `Application.onCreate()`
- Verify Application class is registered in AndroidManifest.xml

### Build Errors?

- Sync Gradle files
- Clean and rebuild project
- Check minimum SDK is 24+

---

## How It Works

1. **On Crash**: Library intercepts uncaught exceptions
2. **Try Send**: Attempts immediate send to Zoho Cliq
3. **Store if Failed**: Saves to SharedPreferences if network unavailable
4. **Retry Later**: On next app launch, sends all pending crashes
5. **Clean Up**: Deletes successfully sent crashes

### Storage Details

- **Location**: SharedPreferences (`zoho_cliq_crash_reporter`)
- **Format**: JSON crash reports with timestamp keys
- **Size**: Typically < 10KB per crash
- **Cleanup**: Automatic after successful send

---

## Security & Privacy

- ✅ HTTPS only (all endpoints use SSL)
- ✅ No automatic data collection (only crash data)
- ✅ No analytics or tracking
- ✅ You control what additional data is sent
- ✅ Local storage only until sent

---

## Example Application Class

Complete example with all features:

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Configure
        val config = CrashReporterConfig(
            domain = "cliq.zoho.com",
            zapiKey = "YOUR_ZAPI_KEY",
            enableLogging = BuildConfig.DEBUG
        )
        
        // Initialize
        CrashReporter.initialize(this, config)
        
        // Add custom data
        CrashReporter.getInstance().setAdditionalDataProvider {
            mapOf(
                "user_id" to getUserId(),
                "build_variant" to BuildConfig.BUILD_TYPE,
                "screen" to getCurrentScreen()
            )
        }
        
        // Listen for crashes
        CrashReporter.getInstance().setOnCrashListener { crash ->
            Log.e("App", "Crash: ${crash.exceptionType}")
        }
        
        // Send pending crashes
        CrashReporter.getInstance().sendPendingCrashes { sent, failed ->
            Log.d("App", "Sent $sent crashes, $failed failed")
        }
    }
}
```

---

## Testing

Test crash reporting:

```kotlin
// In a test activity or button click
throw RuntimeException("Test crash for Zoho Cliq")
```

Test offline storage:
1. Turn off device network
2. Trigger a crash
3. Restart app with network on
4. Check Zoho Cliq for the crash report

---

## Permissions

The library automatically adds the required permission:

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

No action needed from you.

---

## Dependencies

The library uses:
- OkHttp for networking
- Gson for JSON serialization
- Kotlin Coroutines for async operations

All dependencies are included in the library module.

---

## License

[Add your license here]

## Support

For issues or questions:
1. Check this documentation
2. Enable logging and check logcat
3. Verify all configuration parameters
4. Test with a simple crash

---
