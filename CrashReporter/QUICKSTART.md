# Quick Start Guide - Zoho Cliq Crash Reporter

## 3-Step Integration

### Step 1: Get Your Credentials

From your Zoho Cliq webhook URL:
```
https://cliq.zoho.com/api/v2/extensions/2305843009213702336/incoming?zapikey=1001.xxxxx&appkey=myapp
```

Extract:
- **Data Center**: `us` (from `cliq.zoho.com`)
- **ZAPI Key**: `1001.xxxxx` (from URL parameter)
- **App Key**: `myapp` (you define this)

### Step 2: Initialize in Application Class

```kotlin
// MyApplication.kt
import android.app.Application
import com.zohocliq.crashreporter.CrashReporter
import com.zohocliq.crashreporter.CrashReporterConfig

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Configure
        val config = CrashReporterConfig(
            dataCenter = "us",              // Change to your DC
            zapiKey = "YOUR_ZAPI_KEY",      // Replace with actual key
            appKey = "YOUR_APP_KEY",        // Replace with your app key
            enableLogging = BuildConfig.DEBUG
        )
        
        // Initialize
        CrashReporter.initialize(this, config)
        
        // Send pending crashes from previous sessions
        CrashReporter.getInstance().sendPendingCrashes()
    }
}
```

### Step 3: Add to AndroidManifest.xml

```xml
<application
    android:name=".MyApplication"
    android:icon="@mipmap/ic_launcher"
    android:label="@string/app_name"
    ...>
    <!-- Your activities -->
</application>
```

## That's It! 🎉

Crashes are now automatically captured and sent to your Zoho Cliq channel.

---

## Optional: Add Custom Data

```kotlin
CrashReporter.getInstance().setAdditionalDataProvider {
    mapOf(
        "user_id" to getCurrentUserId(),
        "screen" to getCurrentScreen()
    )
}
```

## Optional: Manual Exception Reporting

```kotlin
try {
    // risky code
} catch (e: Exception) {
    CrashReporter.getInstance().reportException(e)
}
```

## Optional: Listen to Crashes

```kotlin
CrashReporter.getInstance().setOnCrashListener { crash ->
    // Log to analytics, etc.
}
```

---

## Data Center Reference

| Your Zoho URL | Data Center Code |
|---------------|------------------|
| cliq.zoho.com | `us` |
| cliq.zoho.eu | `eu` |
| cliq.zoho.in | `in` |
| cliq.zoho.com.au | `au` |
| cliq.zoho.jp | `jp` |
| cliq.zoho.ca | `ca` |

---

## What You'll See in Cliq

When a crash occurs, you'll receive a message like:

```
🚨 **Crash Report**

**Time:** 2026-02-26 15:30:45
**App Version:** 1.0.0 (1)

**Exception Type:** `java.lang.NullPointerException`
**Message:** Attempt to invoke virtual method on null object
**Thread:** main

**Device Information:**
- Model: Pixel 6
- Manufacturer: Google
- Android Version: 14 (API 34)

**Stack Trace:**
```
com.example.app.MainActivity.onClick(MainActivity.kt:45)
android.view.View.performClick(View.java:7448)
...
```
```

---

## Troubleshooting

**Crashes not appearing in Cliq?**
1. Check your zapikey is correct
2. Verify internet permission in manifest
3. Enable logging: `enableLogging = true`
4. Check logcat for errors

**Getting "not initialized" error?**
- Make sure you call `CrashReporter.initialize()` in `Application.onCreate()`
- Verify your Application class is registered in AndroidManifest.xml

---

## Support

For detailed documentation, see [README.md](README.md)

For implementation details, see [IMPLEMENTATION.md](IMPLEMENTATION.md)

