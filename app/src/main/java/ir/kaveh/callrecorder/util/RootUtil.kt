package ir.kaveh.callrecorder.util

import java.io.File

/**
 * تشخیص دسترسی روت. اگر روت باشد اپ سراغ ضبط مستقیم هر دو طرف می‌رود،
 * وگرنه به روش میکروفون برمی‌گردد.
 */
object RootUtil {

    private val SU_PATHS = arrayOf(
        "/system/bin/su", "/system/xbin/su", "/sbin/su",
        "/su/bin/su", "/system/sd/xbin/su", "/system/bin/failsafe/su",
        "/data/local/su", "/data/local/xbin/su", "/data/local/bin/su"
    )

    /** آیا فایل باینری su روی سیستم هست؟ */
    fun isRootBinaryPresent(): Boolean = SU_PATHS.any { File(it).exists() }

    /**
     * تلاش برای گرفتن دسترسی su. یک دستور بی‌خطر اجرا می‌کند.
     * true یعنی su در دسترس است و اجازه داده شد.
     */
    fun canRunSu(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec("su -c id")
            val exit = process.waitFor()
            exit == 0
        } catch (e: Exception) {
            false
        }
    }

    /** ترکیب هر دو بررسی. */
    fun isDeviceRooted(): Boolean = isRootBinaryPresent() && canRunSu()
}
