package ir.kaveh.callrecorder.recorder

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import ir.kaveh.callrecorder.util.RootUtil
import java.io.File

/**
 * موتور ضبط. دو مسیر دارد:
 *
 *  1) ROOT: تلاش برای استفاده از منبع VOICE_CALL که هر دو طرف را مستقیم و تمیز می‌گیرد.
 *     این منبع روی اپ‌های غیرسیستمی از اندروید ۱۰ به بعد مسدود است و فقط با روت/سیستمی‌شدن باز می‌شود.
 *
 *  2) MIC: منبع میکروفون. صدای کاربر تمیز، صدای طرف مقابل فقط با بلندگوی روشن.
 *     این تنها راهی است که بدون روت روی اندروید ۱۶ کار می‌کند.
 *
 * توجه: طبق درخواست، منطق ضبط دستکاری نشده — فقط انتخاب منبع بر اساس روت تغییر می‌کند.
 */
class AudioRecorderEngine(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    var isRecording: Boolean = false
        private set

    /** متد فعلی که ضبط با آن انجام شد؛ برای ذخیره در دیتابیس. */
    var lastMethod: String = "MIC"
        private set

    /**
     * شروع ضبط.
     * @param nonRootSource منبع صوتی که وقتی گوشی روت نیست استفاده می‌شود
     *        (کاربر از داخل اپ انتخابش می‌کند). روی گوشی روت‌شده منبع VOICE_CALL جایگزین می‌شود.
     * مسیر فایل خروجی را برمی‌گرداند یا null اگر شکست خورد.
     */
    fun start(
        namePrefix: String,
        nonRootSource: Int = MediaRecorder.AudioSource.MIC
    ): File? {
        if (isRecording) return outputFile

        val dir = File(context.getExternalFilesDir(null), "recordings").apply { mkdirs() }
        val file = File(dir, "$namePrefix.m4a")
        outputFile = file

        val useRoot = RootUtil.isRootBinaryPresent()

        return try {
            val rec = createRecorder()
            configure(rec, file, useRoot, nonRootSource)
            rec.prepare()
            rec.start()
            recorder = rec
            isRecording = true
            Log.i(TAG, "ضبط شروع شد. method=$lastMethod file=${file.absolutePath}")
            file
        } catch (e: Exception) {
            Log.e(TAG, "شروع ضبط با منبع اول شکست خورد: ${e.message}")
            // اگر با منبع VOICE_CALL شکست خورد، با منبع غیرروت انتخابی دوباره تلاش کن
            safeRelease()
            if (useRoot) retryNonRoot(file, nonRootSource) else null
        }
    }

    private fun retryNonRoot(file: File, nonRootSource: Int): File? {
        return try {
            val rec = createRecorder()
            configure(rec, file, useRoot = false, nonRootSource = nonRootSource)
            rec.prepare()
            rec.start()
            recorder = rec
            isRecording = true
            Log.i(TAG, "ضبط با منبع غیرروت شروع شد (fallback).")
            file
        } catch (e: Exception) {
            Log.e(TAG, "ضبط با منبع غیرروت هم شکست خورد: ${e.message}")
            safeRelease()
            null
        }
    }

    private fun configure(rec: MediaRecorder, file: File, useRoot: Boolean, nonRootSource: Int) {
        // انتخاب منبع صوتی
        val source = if (useRoot) {
            // منبع خط تماس؛ فقط با روت/سیستمی در دسترس است
            MediaRecorder.AudioSource.VOICE_CALL
        } else {
            // منبع انتخابی کاربر (میکروفون / تشخیص صدا / ارتباط صوتی)
            nonRootSource
        }
        lastMethod = if (useRoot) "ROOT" else "MIC"

        rec.setAudioSource(source)
        rec.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        rec.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        rec.setAudioEncodingBitRate(128_000)
        rec.setAudioSamplingRate(44_100)
        rec.setOutputFile(file.absolutePath)
    }

    @Suppress("DEPRECATION")
    private fun createRecorder(): MediaRecorder =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            MediaRecorder(context)
        else
            MediaRecorder()

    /** توقف ضبط. مدت (میلی‌ثانیه) و حجم فایل را برمی‌گرداند. */
    fun stop(): Result? {
        if (!isRecording) return null
        val file = outputFile ?: return null
        return try {
            recorder?.stop()
            safeRelease()
            isRecording = false
            Result(file, file.length(), lastMethod)
        } catch (e: Exception) {
            Log.e(TAG, "توقف ضبط خطا داد: ${e.message}")
            safeRelease()
            isRecording = false
            // فایل ناقص را حذف کن
            if (file.exists() && file.length() < 1024) file.delete()
            null
        }
    }

    private fun safeRelease() {
        try {
            recorder?.reset()
            recorder?.release()
        } catch (_: Exception) {
        }
        recorder = null
    }

    data class Result(val file: File, val sizeBytes: Long, val method: String)

    companion object {
        private const val TAG = "AudioRecorderEngine"
    }
}
