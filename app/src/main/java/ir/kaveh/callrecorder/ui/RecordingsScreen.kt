package ir.kaveh.callrecorder.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallMade
import androidx.compose.material.icons.filled.CallReceived
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import android.media.MediaRecorder
import ir.kaveh.callrecorder.BuildConfig
import ir.kaveh.callrecorder.data.Recording
import ir.kaveh.callrecorder.util.Prefs
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingsScreen(vm: RecordingsViewModel) {
    val recordings by vm.recordings.collectAsState()
    val query by vm.query.collectAsState()
    val playingId by vm.playingId.collectAsState()
    val rooted by vm.isRooted.collectAsState()
    val testing by vm.isTestRecording.collectAsState()
    val accessibilityOn by vm.accessibilityOn.collectAsState()
    val selectedSource by vm.selectedSource.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ضبط تماس  •  نسخه ${BuildConfig.VERSION_NAME}") },
                actions = {
                    val label = if (rooted) "روت: فعال" else "میکروفون"
                    AssistChip(
                        onClick = {},
                        label = { Text(label) },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // هشدار: اگر سرویس دسترسی‌پذیری روشن نباشد، ضبط تماس کار نمی‌کند
            if (!accessibilityOn) {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, null, tint = Color(0xFFE65100))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "سرویس دسترسی‌پذیری خاموش است",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color(0xFFE65100)
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "برای ضبط تماس روی اندروید ۱۴+ باید این سرویس را روشن کنید، " +
                                "وگرنه سیستم میکروفون را هنگام تماس قطع می‌کند.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(Modifier.height(10.dp))
                        Button(onClick = { vm.openAccessibilitySettings() }) {
                            Text("روشن‌کردن سرویس دسترسی‌پذیری")
                        }
                    }
                }
            }

            OutlinedTextField(
                value = query,
                onValueChange = vm::setQuery,
                leadingIcon = { Icon(Icons.Default.Search, null) },
                placeholder = { Text("جستجو بر اساس نام یا شماره") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .padding(top = 4.dp)
            )

            // انتخابگر منبع صوتی — برای یافتن منبعی که هنگام تماس بی‌صدا نمی‌شود
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .padding(top = 12.dp)
            ) {
                Text(
                    "منبع صوتی ضبط:",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(Modifier.height(6.dp))
                val sources = listOf(
                    MediaRecorder.AudioSource.VOICE_RECOGNITION,
                    MediaRecorder.AudioSource.MIC,
                    MediaRecorder.AudioSource.VOICE_COMMUNICATION
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    sources.forEach { src ->
                        FilterChip(
                            selected = selectedSource == src,
                            onClick = { vm.setSource(src) },
                            label = { Text(Prefs.label(src), maxLines = 1) }
                        )
                    }
                }
            }

            // دکمهٔ تست ضبط دستی — برای عیب‌یابی مستقل از تماس
            Button(
                onClick = { vm.toggleTestRecording() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (testing) Color(0xFFC62828) else MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Icon(
                    if (testing) Icons.Default.Stop else Icons.Default.FiberManualRecord,
                    contentDescription = null
                )
                Spacer(Modifier.width(8.dp))
                Text(if (testing) "توقف تست ضبط" else "تست ضبط دستی (میکروفون)")
            }

            if (recordings.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "هنوز ضبطی وجود ندارد.\nپس از اولین تماس اینجا نمایش داده می‌شود.",
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(recordings, key = { it.id }) { rec ->
                        RecordingRow(
                            rec = rec,
                            isPlaying = playingId == rec.id,
                            onPlay = { vm.play(rec) },
                            onStop = { vm.stopPlayback() },
                            onDelete = { vm.delete(rec) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecordingRow(
    rec: Recording,
    isPlaying: Boolean,
    onPlay: () -> Unit,
    onStop: () -> Unit,
    onDelete: () -> Unit
) {
    Card(shape = RoundedCornerShape(14.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (rec.direction == "INCOMING")
                    Icons.Default.CallReceived else Icons.Default.CallMade,
                contentDescription = null,
                tint = if (rec.direction == "INCOMING") Color(0xFF2E7D32) else Color(0xFF1565C0)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = rec.contactName ?: rec.phoneNumber ?: "ناشناس",
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${formatDate(rec.startTime)}  •  ${formatDuration(rec.durationMs)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            IconButton(onClick = if (isPlaying) onStop else onPlay) {
                Icon(
                    if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "توقف" else "پخش"
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "حذف")
            }
        }
    }
}

private fun formatDate(t: Long): String =
    SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.US).format(Date(t))

private fun formatDuration(ms: Long): String {
    val totalSec = ms / 1000
    val m = totalSec / 60
    val s = totalSec % 60
    return "%d:%02d".format(m, s)
}
