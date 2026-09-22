package ir.kaveh.callrecorder.util

import android.content.Context
import android.media.MediaRecorder

/**
 * نگهداری تنظیمات ساده. مهم‌ترینش «منبع صوتی» است که کاربر برای یافتن
 * منبعی که روی گوشی خودش هنگام تماس بی‌صدا نمی‌شود، بین چند گزینه عوض می‌کند.
 */
object Prefs {
    private const val FILE = "call_recorder_prefs"
    private const val KEY_SOURCE = "audio_source"

    // منبع پیش‌فرض غیرروت: تشخیص صدا (بدون حذف اکو، بیشترین شانس عبور از فیلتر تماس)
    private val DEFAULT_SOURCE = MediaRecorder.AudioSource.VOICE_RECOGNITION

    fun getSource(context: Context): Int =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getInt(KEY_SOURCE, DEFAULT_SOURCE)

    fun setSource(context: Context, source: Int) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().putInt(KEY_SOURCE, source).apply()
    }

    /** برچسب فارسی هر منبع برای نمایش در رابط. */
    fun label(source: Int): String = when (source) {
        MediaRecorder.AudioSource.MIC -> "میکروفون معمولی"
        MediaRecorder.AudioSource.VOICE_RECOGNITION -> "تشخیص صدا"
        MediaRecorder.AudioSource.VOICE_COMMUNICATION -> "ارتباط صوتی"
        MediaRecorder.AudioSource.VOICE_CALL -> "خط تماس (روت)"
        else -> "نامشخص"
    }
}
