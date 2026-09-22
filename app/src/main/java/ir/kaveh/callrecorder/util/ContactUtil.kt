package ir.kaveh.callrecorder.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import androidx.core.content.ContextCompat

object ContactUtil {

    /** نام مخاطب را از روی شماره پیدا می‌کند؛ اگر نبود null برمی‌گرداند. */
    fun lookupName(context: Context, number: String?): String? {
        if (number.isNullOrBlank()) return null
        if (ContextCompat.checkSelfPermission(
                context, Manifest.permission.READ_CONTACTS
            ) != PackageManager.PERMISSION_GRANTED
        ) return null

        val uri: Uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(number)
        )
        context.contentResolver.query(
            uri,
            arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
            null, null, null
        )?.use { c ->
            if (c.moveToFirst()) return c.getString(0)
        }
        return null
    }
}
