// Add these imports at the top
import com.wavro.studio.rendering.VideoExporter
import android.os.Environment
import java.io.File

// Inside the StudioViewModel class:

    private val exporter = VideoExporter(application)
    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()
    private val _exportProgress = MutableStateFlow(0f)
    val exportProgress: StateFlow<Float> = _exportProgress.asStateFlow()
    private val _exportStatus = MutableStateFlow("")
    val exportStatus: StateFlow<String> = _exportStatus.asStateFlow()

    fun exportVideo() {
        val currentTrack = playback.value.currentTrack ?: return
        if (_isExporting.value) return

        viewModelScope.launch {
            _isExporting.value = true
            _exportStatus.value = "Preparing..."
            try {
                val file = exporter.exportVideo(
                    audioUri = currentTrack.uri,
                    layers = _project.value.layers,
                    outputFileName = "WAVRO_Export_${System.currentTimeMillis()}.mp4"
                ) { progress ->
                    _exportProgress.value = progress
                    _exportStatus.value = "Exporting: ${(progress * 100).toInt()}%"
                }
                _exportStatus.value = "Saved to: ${file.absolutePath}"
            } catch (e: Exception) {
                _exportStatus.value = "Error: ${e.message}"
            } finally {
                _isExporting.value = false
            }
        }
    }