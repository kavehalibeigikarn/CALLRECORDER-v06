package ir.kaveh.callrecorder.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

/**
 * این سرویس محتوای صفحه را نمی‌خواند و کار خاصی روی رویدادها انجام نمی‌دهد.
 * تنها نقشش این است که با روشن‌بودن، اپ را در حالت «فعال و ممتاز» نگه دارد
 * تا اندروید هنگام تماس دسترسی میکروفون سرویس ضبط را قطع نکند.
 */
class CallAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        isRunning = true
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // نیازی به پردازش نیست
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
    }

    companion object {
        @Volatile
        var isRunning: Boolean = false
            private set
    }
}
