package ir.kaveh.callrecorder.ui

import android.app.Application
import android.media.MediaPlayer
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ir.kaveh.callrecorder.data.AppDatabase
import ir.kaveh.callrecorder.data.Recording
import ir.kaveh.callrecorder.recorder.AudioRecorderEngine
import ir.kaveh.callrecorder.util.AccessibilityUtil
import ir.kaveh.callrecorder.util.Prefs
import ir.kaveh.callrecorder.util.RootUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class RecordingsViewModel(app: Application) : AndroidViewModel(app) {

    private val dao = AppDatabase.get(app).recordingDao()

    val query = MutableStateFlow("")

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val recordings: StateFlow<List<Recording>> =
        query.flatMapLatest { q ->
            if (q.isBlank()) dao.observeAll() else dao.search(q.trim())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** وضعیت روت، برای نمایش در رابط. */
    val isRooted = MutableStateFlow(false)

    /** آیا سرویس دسترسی‌پذیری روشن است؟ */
    val accessibilityOn = MutableStateFlow(false)

    fun refreshAccessibility() {
        accessibilityOn.value = AccessibilityUtil.isEnabled(getApplication())
    }

    fun openAccessibilitySettings() {
        AccessibilityUtil.openSettings(getApplication())
    }

    /** منبع صوتی انتخاب‌شده (برای تست و ضبط تماس). */
    val selectedSource = MutableStateFlow(Prefs.getSource(getApplication()))

    fun setSource(source: Int) {
        Prefs.setSource(getApplication(), source)
        selectedSource.value = source
    }

    // پخش‌کنندهٔ ساده
    private var player: MediaPlayer? = null
    val playingId = MutableStateFlow<Long?>(null)

    init {
        viewModelScope.launch(Dispatchers.IO) {
            isRooted.value = RootUtil.isDeviceRooted()
        }
    }

    fun setQuery(q: String) { query.value = q }

    // ---------- تست ضبط دستی (مستقل از تماس) ----------
    private val testEngine = AudioRecorderEngine(getApplication())
    private var testStart = 0L
    val isTestRecording = MutableStateFlow(false)

    fun toggleTestRecording() {
        val ctx = getApplication<Application>()
        if (isTestRecording.value) {
            val result = testEngine.stop()
            isTestRecording.value = false
            if (result == null || result.sizeBytes < 512) {
                result?.file?.delete()
                Toast.makeText(ctx, "تست ناموفق: چیزی ضبط نشد", Toast.LENGTH_LONG).show()
                return
            }
            val dur = System.currentTimeMillis() - testStart
            viewModelScope.launch(Dispatchers.IO) {
                dao.insert(
                    Recording(
                        filePath = result.file.absolutePath,
                        contactName = "تست دستی",
                        phoneNumber = null,
                        direction = "OUTGOING",
                        startTime = testStart,
                        durationMs = dur,
                        sizeBytes = result.sizeBytes,
                        method = result.method
                    )
                )
            }
            Toast.makeText(ctx, "تست ذخیره شد (${result.sizeBytes / 1024}KB)", Toast.LENGTH_SHORT).show()
        } else {
            testStart = System.currentTimeMillis()
            val stamp = testStart.toString()
            val file = testEngine.start("test_$stamp", Prefs.getSource(ctx))
            if (file == null) {
                Toast.makeText(ctx, "میکروفون در دسترس نیست", Toast.LENGTH_LONG).show()
                return
            }
            isTestRecording.value = true
            Toast.makeText(ctx, "در حال تست ضبط... دوباره بزنید تا تمام شود", Toast.LENGTH_SHORT).show()
        }
    }

    fun play(rec: Recording) {
        stopPlayback()
        val file = File(rec.filePath)
        if (!file.exists()) return
        player = MediaPlayer().apply {
            setDataSource(file.absolutePath)
            setOnCompletionListener { stopPlayback() }
            prepare()
            start()
        }
        playingId.value = rec.id
    }

    fun stopPlayback() {
        player?.release()
        player = null
        playingId.value = null
    }

    fun delete(rec: Recording) {
        viewModelScope.launch(Dispatchers.IO) {
            File(rec.filePath).delete()
            dao.delete(rec)
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopPlayback()
    }
}
