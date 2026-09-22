package ir.kaveh.callrecorder.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * یک رکورد ضبط‌شده. فایل صوتی روی دیسک است و این جدول فقط متادیتا را نگه می‌دارد.
 */
@Entity(tableName = "recordings")
data class Recording(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val filePath: String,
    val contactName: String?,     // نام مخاطب اگر پیدا شد
    val phoneNumber: String?,     // شمارهٔ خط
    val direction: String,        // "INCOMING" یا "OUTGOING"
    val startTime: Long,          // زمان شروع (میلی‌ثانیه)
    val durationMs: Long,         // مدت ضبط
    val sizeBytes: Long,          // حجم فایل
    val method: String            // "ROOT" یا "MIC"
)
