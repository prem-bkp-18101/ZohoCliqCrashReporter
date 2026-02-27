# Integration Checklist ✅

Use this checklist when integrating the Zoho Cliq Crash Reporter into your Android app.

## Prerequisites

- [ ] Android API 24 (Android 7.0) or higher
- [ ] Zoho Cliq webhook URL with zapikey and appkey

## Step 1: Add Dependency

In your app's `build.gradle.kts`:

```kotlin
dependencies {
    implementation(project(":CrashReporter"))
    // ... other dependencies
}
```

- [ ] Added dependency
- [ ] Synced Gradle

## Step 2: Get Credentials

From your Zoho Cliq webhook URL:
```
https://cliq.zoho.com/api/v2/extensions/2305843009213702336/incoming?zapikey=1001.xxxxx&appkey=myapp
```

Extract:
- [ ] Data Center (e.g., "us" from cliq.zoho.com)
- [ ] ZAPI Key (e.g., "1001.xxxxx")
- [ ] App Key (e.g., "myapp")

## Step 3: Create Application Class

If you don't have one already:

```kotlin
// MyApplication.kt
package com.yourcompany.yourapp

import android.app.Application
import com.zohocliq.crashreporter.CrashReporter
import com.zohocliq.crashreporter.CrashReporterConfig

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initializeCrashReporter()
    }
    
    private fun initializeCrashReporter() {
        val config = CrashReporterConfig(
            dataCenter = "us",              // TODO: Change to your DC
            zapiKey = "YOUR_ZAPI_KEY",      // TODO: Replace
            appKey = "YOUR_APP_KEY",        // TODO: Replace
            enableLogging = BuildConfig.DEBUG
        )
        
        CrashReporter.initialize(this, config)
        CrashReporter.getInstance().sendPendingCrashes()
    }
}
```

- [ ] Created Application class
- [ ] Added initialization code
- [ ] Replaced placeholder values with actual credentials

## Step 4: Register Application Class

In `AndroidManifest.xml`:

```xml
<application
    android:name=".MyApplication"
    android:icon="@mipmap/ic_launcher"
    android:label="@string/app_name"
    android:theme="@style/Theme.YourApp">
    
    <!-- Your activities -->
    
</application>
```

- [ ] Added `android:name` attribute with your Application class

## Step 5: Test Basic Integration

### Test 1: Check Initialization

Run your app and check logcat for:
```
D/ZohoCliqCrashReporter: CrashReporter initialized with config: DC=us, AppKey=myapp
```

- [ ] App starts without crashes
- [ ] Initialization log appears (if logging enabled)

### Test 2: Test Crash Detection

Add a test crash somewhere in your app:

```kotlin
// In a button click or activity
throw RuntimeException("Test crash for Zoho Cliq")
```

- [ ] App crashes (expected)
- [ ] Check Zoho Cliq channel for crash report

### Test 3: Test Offline Storage

1. Turn off device network
2. Trigger another crash
3. Turn network back on
4. Restart the app

- [ ] Crash report appears in Zoho Cliq after restart

## Step 6: Add Custom Data (Optional)

```kotlin
CrashReporter.getInstance().setAdditionalDataProvider {
    mapOf(
        "user_id" to getUserId(),
        "environment" to getEnvironment(),
        "feature_flags" to getFeatureFlags()
    )
}
```

- [ ] Added custom data provider (if needed)
- [ ] Verified custom data appears in crash reports

## Step 7: Production Checklist

Before releasing to production:

- [ ] Replaced test credentials with production credentials
- [ ] Set `enableLogging = false` for production
- [ ] Removed any test crash triggers
- [ ] Tested crash reporting on multiple devices
- [ ] Verified crash reports arrive in correct Cliq channel
- [ ] Documented crash monitoring process for team

## Troubleshooting

### Issue: "CrashReporter not initialized" error

**Solution:**
- Ensure `CrashReporter.initialize()` is called in `Application.onCreate()`
- Verify Application class is registered in AndroidManifest.xml

### Issue: Crashes not appearing in Cliq

**Solution:**
- Check internet connectivity
- Verify zapikey and appkey are correct
- Check Zoho Cliq webhook is active
- Enable logging and check logcat for errors

### Issue: Build errors

**Solution:**
- Ensure Kotlin plugin is properly configured
- Sync Gradle files
- Clean and rebuild project

## Additional Features

### Manual Exception Reporting

```kotlin
try {
    riskyOperation()
} catch (e: Exception) {
    CrashReporter.getInstance().reportException(e, mapOf(
        "context" to "checkout_flow",
        "step" to "payment"
    ))
}
```

- [ ] Implemented manual reporting (if needed)

### Crash Event Listener

```kotlin
CrashReporter.getInstance().setOnCrashListener { crash ->
    // Send to analytics
    analytics.logCrash(crash.exceptionType)
}
```

- [ ] Added crash listener (if needed)

## Documentation Reference

- **Quick Start:** See `QUICKSTART.md`
- **Full Documentation:** See `README.md`
- **Technical Details:** See `IMPLEMENTATION.md`

## Support

If you encounter issues:

1. Check the documentation files
2. Enable logging and check logcat
3. Verify all checklist items are complete
4. Review the ExampleApplication.kt for reference

---

**Completion Date:** _______________

**Tested By:** _______________

**Notes:**
```
[Add any integration-specific notes here]
```

---

✅ Integration Complete!

