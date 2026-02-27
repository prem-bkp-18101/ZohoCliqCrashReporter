# Zoho Cliq Crash Reporter - Implementation Summary

## Overview

A complete crash reporting solution for Android that sends crash reports to Zoho Cliq channels. The implementation uses **SharedPreferences** for storage (no database) and handles both immediate and deferred crash reporting.

## Architecture

### Core Components

1. **CrashReporterConfig.kt**
   - Configuration data class
   - Constructs endpoint URL from DC and zapikey
   - Supports all Zoho data centers (US, EU, IN, AU, JP, CA)

2. **CrashReport.kt**
   - Data model for crash reports
   - Captures exception details, device info, stack trace
   - Converts to Zoho Cliq markdown format
   - JSON serialization/deserialization (no external library)

3. **CrashStorage.kt**
   - Manages SharedPreferences storage
   - Stores crashes with timestamp-based keys
   - Provides CRUD operations for crash reports
   - No database - simple and lightweight

4. **ZohoCliqService.kt**
   - Network service using OkHttp
   - Sends crash reports to Zoho Cliq
   - Handles batch sending with delays
   - Proper error handling and retries

5. **CrashReporter.kt**
   - Main public API
   - Singleton pattern
   - Intercepts uncaught exceptions
   - Try immediate send, fallback to storage
   - Sends pending crashes on restart

6. **Logger.kt**
   - Simple logging utilities
   - Configurable via `enableLogging` flag

## Data Flow

### On Crash
```
Uncaught Exception
    ↓
CrashReporter.handleUncaughtException()
    ↓
Create CrashReport
    ↓
Try immediate send via ZohoCliqService
    ↓
If success: Done
If failure: Store in SharedPreferences
    ↓
Original exception handler (app crashes)
```

### On App Restart
```
Application.onCreate()
    ↓
CrashReporter.initialize()
    ↓
CrashReporter.sendPendingCrashes()
    ↓
Load crashes from SharedPreferences
    ↓
Send each crash to Zoho Cliq
    ↓
Delete successfully sent crashes
```

## API Endpoint Construction

Developer provides:
- **DC** (Data Center): e.g., "us", "eu", "in"
- **zapikey**: e.g., "1001.1b1c4f6475e4a2b62aab491c52af126d.30f348a6cfca8fa0af3f2d256ea57e6f"

Library constructs:
```
https://cliq.zoho.{dc}/api/v2/extensions/{extensionId}/incoming?zapikey={zapikey}&appkey={appKey}
```

Example:
```
https://cliq.zoho.com/api/v2/extensions/2305843009213702336/incoming?zapikey=1001.xxxxx&appkey=myapp
```

## Key Features

### ✅ No Database
- Uses SharedPreferences only
- Lightweight and simple
- No Room, SQLite, or other database dependencies

### ✅ Immediate Send with Fallback
- Attempts to send crash immediately
- If network fails, stores in SharedPreferences
- Retries on next app launch

### ✅ Multi-Region Support
- Automatically constructs correct URL based on DC
- Supports: US, EU, IN, AU, JP, CA

### ✅ Rich Crash Reports
Includes:
- Exception type and message
- Full stack trace
- Thread name
- Device manufacturer, model
- Android version and API level
- App version (name + code)
- Timestamp
- Custom additional data

### ✅ Developer-Friendly API
```kotlin
// Initialize
val config = CrashReporterConfig(
    dataCenter = "us",
    zapiKey = "your_zapi_key",
    appKey = "your_app_key"
)
CrashReporter.initialize(context, config)

// Send pending
CrashReporter.getInstance().sendPendingCrashes()

// Manual reporting
CrashReporter.getInstance().reportException(exception)

// Custom data
CrashReporter.getInstance().setAdditionalDataProvider {
    mapOf("user_id" to "123")
}
```

## Storage Details

### SharedPreferences Structure
```
Preferences Name: "zoho_cliq_crash_reporter"

Keys:
- crash_list: Set<String>       // List of timestamps
- crash_{timestamp}: String     // JSON crash report
```

### Example Stored Crash
```json
{
  "timestamp": 1708962045000,
  "exceptionType": "java.lang.NullPointerException",
  "exceptionMessage": "Attempt to invoke virtual method...",
  "stackTrace": "java.lang.NullPointerException: ...\n at com.example.MainActivity.onCreate(...)",
  "appVersion": "1.0.0 (1)",
  "deviceInfo": {
    "manufacturer": "Google",
    "model": "Pixel 6",
    "androidVersion": "14",
    "sdkInt": 34
  },
  "threadName": "main",
  "additionalData": {}
}
```

## Dependencies

### Added to build.gradle.kts
```kotlin
// Network
implementation(libs.okhttp)
implementation(libs.okhttp.logging)

// JSON (using Gson, but custom serialization for crash reports)
implementation(libs.gson)

// Coroutines
implementation(libs.kotlin.coroutines.core)
implementation(libs.kotlin.coroutines.android)
```

### Removed (as per requirements)
- Room database
- Timber logging

## File Structure

```
CrashReporter/src/main/java/com/zohocliq/crashreporter/
├── CrashReporter.kt              # Main API
├── CrashReporterConfig.kt        # Configuration
├── CrashReport.kt                # Data model
├── CrashStorage.kt               # SharedPreferences manager
├── ZohoCliqService.kt            # Network service
├── Logger.kt                     # Logging utilities
└── example/
    └── ExampleApplication.kt     # Usage example
```

## Usage Example

### 1. Create Application Class
```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        val config = CrashReporterConfig(
            dataCenter = "us",
            zapiKey = "1001.xxxxx",
            appKey = "myapp",
            enableLogging = BuildConfig.DEBUG
        )
        
        CrashReporter.initialize(this, config)
        
        CrashReporter.getInstance().sendPendingCrashes { sent, failed ->
            Log.i("App", "Sent $sent crashes, $failed failed")
        }
    }
}
```

### 2. Add to AndroidManifest.xml
```xml
<application
    android:name=".MyApplication"
    ...>
</application>
```

### 3. That's It!
Crashes are now automatically captured and sent to Zoho Cliq.

## Testing

### Manual Testing Steps

1. **Test Crash Capture**
   ```kotlin
   // Trigger a crash
   throw RuntimeException("Test crash")
   ```

2. **Verify Storage**
   - Check SharedPreferences file
   - Should contain crash data

3. **Test Sending**
   - Restart app
   - Check Zoho Cliq channel
   - Should receive crash report message

4. **Test Network Failure**
   - Turn off network
   - Trigger crash
   - Turn on network
   - Restart app
   - Should send pending crash

## Configuration Examples

### US Data Center
```kotlin
CrashReporterConfig(
    dataCenter = "us",
    zapiKey = "1001.xxxxx",
    appKey = "myapp"
)
// URL: https://cliq.zoho.com/api/v2/extensions/...
```

### EU Data Center
```kotlin
CrashReporterConfig(
    dataCenter = "eu",
    zapiKey = "1001.xxxxx",
    appKey = "myapp"
)
// URL: https://cliq.zoho.eu/api/v2/extensions/...
```

### India Data Center
```kotlin
CrashReporterConfig(
    dataCenter = "in",
    zapiKey = "1001.xxxxx",
    appKey = "myapp"
)
// URL: https://cliq.zoho.in/api/v2/extensions/...
```

## Security Considerations

1. **API Keys**: Store zapikey securely (consider using Android Keystore for production)
2. **Sensitive Data**: Don't include PII in additional data
3. **HTTPS**: All endpoints use HTTPS by default
4. **Permissions**: Only requires INTERNET permission

## Performance Impact

- **Memory**: Minimal (no heavy objects retained)
- **Storage**: Only stores crashes (typically < 10KB each)
- **Network**: Asynchronous with coroutines
- **Startup**: Negligible (SharedPreferences read is fast)

## Limitations

1. **No Crash Analytics**: Just sends raw crashes to Cliq
2. **No Crash Grouping**: Each crash is a separate message
3. **Storage Limit**: No automatic cleanup of old crashes
4. **Rate Limiting**: 1-second delay between batch sends

## Future Enhancements (Optional)

1. Add crash grouping by exception type
2. Add automatic cleanup of old crashes
3. Add crash statistics
4. Add symbolication support for obfuscated builds
5. Add attachment support (screenshots, logs)

## Conclusion

The implementation provides a **simple, lightweight, and effective** crash reporting solution that meets all requirements:

✅ No database (SharedPreferences only)  
✅ No heavy operations  
✅ Immediate send with fallback  
✅ Multi-region support  
✅ Developer provides only DC and zapikey  
✅ Automatic endpoint construction  
✅ Rich crash reports with device info  

The solution is production-ready and can be integrated into any Android app with minimal effort.

