package ir.kaveh.callrecorder.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import ir.kaveh.callrecorder.service.CallAccessibilityService

object AccessibilityUtil {

    /** آیا سرویس دسترسی‌پذیری این اپ روشن است؟ */
    fun isEnabled(context: Context): Boolean {
        val expected = ComponentName(context, CallAccessibilityService::class.java)
            .flattenToString()
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabled.split(':').any { it.equals(expected, ignoreCase = true) }
    }

    /** باز کردن صفحهٔ تنظیمات دسترسی‌پذیری تا کاربر سرویس را روشن کند. */
    fun openSettings(context: Context) {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
