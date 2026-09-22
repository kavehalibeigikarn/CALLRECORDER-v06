package ir.kaveh.callrecorder.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.widget.Toast
import androidx.core.app.NotificationCompat
import ir.kaveh.callrecorder.MainActivity
import ir.kaveh.callrecorder.R
import ir.kaveh.callrecorder.data.AppDatabase
import ir.kaveh.callrecorder.data.Recording
import ir.kaveh.callrecorder.recorder.AudioRecorderEngine
import ir.kaveh.callrecorder.util.ContactUtil
import ir.kaveh.callrecorder.util.Prefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * سرویس پیش‌زمینه که ضبط را در طول تماس پایدار نگه می‌دارد.
 * از طریق اکشن‌های استاتیک start/stop کنترل می‌شود.
 */
class CallRecordingService : Service() {

    private val engine by lazy { AudioRecorderEngine(this) }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var startTime = 0L
    private var number: String? = null
    private var direction: String = "OUTGOING"

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                number = intent.getStringExtra(EXTRA_NUMBER)
                direction = intent.getStringExtra(EXTRA_DIRECTION) ?: "OUTGOING"
                try {
                    startForegroundInternal()
                    beginRecording()
                } catch (e: Exception) {
                    Toast.makeText(
                        this,
                        "سیستم اجازهٔ شروع سرویس را نداد: ${e.javaClass.simpleName}",
                        Toast.LENGTH_LONG
                    ).show()
                    stopSelf()
                }
            }
            ACTION_STOP -> {
                endRecording()
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun beginRecording() {
        startTime = System.currentTimeMillis()
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date(startTime))
        val safeNum = number?.replace(Regex("[^0-9+]"), "") ?: "unknown"
        val source = Prefs.getSource(this)
        val file = engine.start("call_${safeNum}_$stamp", source)
        // بازخورد قابل‌دیدن برای عیب‌یابی
        val msg = if (file != null) "شروع ضبط (${Prefs.label(source)})" else "ضبط شروع نشد!"
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    private fun endRecording() {
        val result = engine.stop()
        if (result == null) {
            Toast.makeText(this, "توقف ضبط: چیزی ذخیره نشد", Toast.LENGTH_SHORT).show()
            return
        }
        val durationMs = System.currentTimeMillis() - startTime
        val name = ContactUtil.lookupName(this, number)

        // فایل بسیار کوچک (خالی) را ذخیره نکن — آستانه برای تست پایین آمده
        if (result.sizeBytes < 512) {
            result.file.delete()
            Toast.makeText(this, "ضبط خالی بود (حجم صفر)", Toast.LENGTH_SHORT).show()
            return
        }
        Toast.makeText(this, "ذخیره شد (${result.sizeBytes / 1024}KB)", Toast.LENGTH_SHORT).show()

        scope.launch {
            AppDatabase.get(this@CallRecordingService).recordingDao().insert(
                Recording(
                    filePath = result.file.absolutePath,
                    contactName = name,
                    phoneNumber = number,
                    direction = direction,
                    startTime = startTime,
                    durationMs = durationMs,
                    sizeBytes = result.sizeBytes,
                    method = result.method
                )
            )
        }
    }

    private fun startForegroundInternal() {
        createChannel()
        val pi = android.app.PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            android.app.PendingIntent.FLAG_IMMUTABLE
        )
        val notif: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notif_recording_title))
            .setContentText(getString(R.string.notif_recording_text))
            .setSmallIcon(R.drawable.ic_mic)
            .setContentIntent(pi)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIF_ID, notif,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(NOTIF_ID, notif)
        }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notif_channel_name),
                NotificationManager.IMPORTANCE_LOW
            )
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(ch)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (engine.isRecording) engine.stop()
    }

    companion object {
        private const val CHANNEL_ID = "recording_channel"
        private const val NOTIF_ID = 1001
        private const val ACTION_START = "ir.kaveh.callrecorder.START"
        private const val ACTION_STOP = "ir.kaveh.callrecorder.STOP"
        private const val EXTRA_NUMBER = "number"
        private const val EXTRA_DIRECTION = "direction"

        fun startRecording(context: Context, number: String?, direction: String) {
            val i = Intent(context, CallRecordingService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_NUMBER, number)
                putExtra(EXTRA_DIRECTION, direction)
            }
            context.startForegroundService(i)
        }

        fun stopRecording(context: Context) {
            val i = Intent(context, CallRecordingService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(i)
        }
    }
}
