package ir.kaveh.callrecorder.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log
import android.widget.Toast

/**
 * وضعیت تماس را رصد می‌کند و به سرویس ضبط خبر می‌دهد.
 *
 * منطق تشخیص جهت تماس:
 *   - اگر قبل از OFFHOOK حالت RINGING دیدیم → تماس ورودی
 *   - اگر مستقیم OFFHOOK شد → تماس خروجی
 */
class PhoneStateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return

        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
        val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)

        Log.d(TAG, "state=$state number=$incomingNumber")

        when (state) {
            TelephonyManager.EXTRA_STATE_RINGING -> {
                lastIncomingNumber = incomingNumber
                sawRinging = true
            }

            TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                // تماس برقرار شد → شروع ضبط
                if (!isCallActive) {
                    isCallActive = true
                    val direction = if (sawRinging) "INCOMING" else "OUTGOING"
                    val number = lastIncomingNumber
                    Toast.makeText(context, "تماس تشخیص داده شد", Toast.LENGTH_SHORT).show()
                    CallRecordingService.startRecording(context, number, direction)
                }
            }

            TelephonyManager.EXTRA_STATE_IDLE -> {
                // تماس تمام شد → توقف ضبط
                if (isCallActive) {
                    isCallActive = false
                    CallRecordingService.stopRecording(context)
                }
                sawRinging = false
                lastIncomingNumber = null
            }
        }
    }

    companion object {
        private const val TAG = "PhoneStateReceiver"
        private var isCallActive = false
        private var sawRinging = false
        private var lastIncomingNumber: String? = null
    }
}
