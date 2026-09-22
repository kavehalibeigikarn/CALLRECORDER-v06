package ir.kaveh.callrecorder

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import ir.kaveh.callrecorder.ui.RecordingsScreen
import ir.kaveh.callrecorder.ui.RecordingsViewModel
import ir.kaveh.callrecorder.ui.theme.CallRecorderTheme

class MainActivity : ComponentActivity() {

    private val vm: RecordingsViewModel by viewModels()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* نتیجه در UI لازم نیست؛ سرویس هنگام تماس بررسی می‌کند */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNeededPermissions()

        setContent {
            CallRecorderTheme {
                // کل رابط راست‌چین
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        RecordingsScreen(vm)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // بعد از بازگشت از تنظیمات، وضعیت سرویس دسترسی‌پذیری را تازه کن
        vm.refreshAccessibility()
    }

    private fun requestNeededPermissions() {
        val perms = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.READ_CALL_LOG
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms += Manifest.permission.POST_NOTIFICATIONS
        }
        permissionLauncher.launch(perms.toTypedArray())
    }
}
