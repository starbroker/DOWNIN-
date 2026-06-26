package com.example.ui

import android.app.Application
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.DownloadItem
import com.example.data.DownloadRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class DetectionState {
    IDLE, DETECTING, DETECTED, ERROR
}

data class MediaFormat(
    val format: String, // MP4, MP3
    val quality: String, // 4K, 1080p, 720p, 320kbps
    val size: String
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: DownloadRepository
    val downloads: StateFlow<List<DownloadItem>>

    private val _urlInput = MutableStateFlow("")
    val urlInput: StateFlow<String> = _urlInput.asStateFlow()

    private val _detectionState = MutableStateFlow(DetectionState.IDLE)
    val detectionState: StateFlow<DetectionState> = _detectionState.asStateFlow()

    private val _availableFormats = MutableStateFlow<List<MediaFormat>>(emptyList())
    val availableFormats: StateFlow<List<MediaFormat>> = _availableFormats.asStateFlow()

    private val _selectedFormat = MutableStateFlow<MediaFormat?>(null)
    val selectedFormat: StateFlow<MediaFormat?> = _selectedFormat.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = DownloadRepository(database.downloadDao())
        downloads = repository.allDownloads.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )
    }

    fun updateUrl(url: String) {
        _urlInput.value = url
        _detectionState.value = DetectionState.IDLE
        _availableFormats.value = emptyList()
        _selectedFormat.value = null
    }

    fun selectFormat(format: MediaFormat) {
        _selectedFormat.value = format
    }

    fun detectContent() {
        if (_urlInput.value.isBlank()) return

        viewModelScope.launch {
            _detectionState.value = DetectionState.DETECTING
            // Simulate network delay for extraction
            delay(1500)
            
            if (_urlInput.value.contains("x.com") || _urlInput.value.contains("twitter.com") || _urlInput.value.contains("instagram.com/p/")) {
                _availableFormats.value = listOf(
                    MediaFormat("JPG", "High", "1.2 MB"),
                    MediaFormat("JPG", "Medium", "0.5 MB")
                )
                _selectedFormat.value = _availableFormats.value[0]
                _detectionState.value = DetectionState.DETECTED
            } else if (_urlInput.value.contains("error")) {
                _detectionState.value = DetectionState.ERROR
            } else {
                _availableFormats.value = listOf(
                    MediaFormat("MP4", "4K", "125 MB"),
                    MediaFormat("MP4", "1080p", "45 MB"),
                    MediaFormat("MP4", "720p", "20 MB"),
                    MediaFormat("MP3", "320kbps", "8 MB")
                )
                _selectedFormat.value = _availableFormats.value[1] // Default 1080p
                _detectionState.value = DetectionState.DETECTED
            }
        }
    }

    fun startDownload() {
        val format = _selectedFormat.value ?: return
        val url = _urlInput.value

        val isImage = format.format.contains("JPG") || format.format.contains("PNG")
        val title = if (isImage) "Image_${System.currentTimeMillis()}" else "Video_${System.currentTimeMillis()}"

        // We use sample URLs for actual downloading to prove functionality works
        val actualDownloadUrl = if (isImage) {
            "https://upload.wikimedia.org/wikipedia/commons/thumb/4/47/PNG_transparency_demonstration_1.png/280px-PNG_transparency_demonstration_1.png"
        } else {
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
        }
        
        val ext = if (format.format == "MP3") "mp3" else if (isImage) "png" else "mp4"
        val fileName = "${title}_${System.currentTimeMillis()}.$ext"

        // Use Android DownloadManager
        val request = DownloadManager.Request(Uri.parse(actualDownloadUrl))
            .setTitle(title)
            .setDescription("Downloading ${format.quality} ${format.format}")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val downloadManager = getApplication<Application>().getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadManager.enqueue(request)

        // Save to Room DB
        viewModelScope.launch {
            repository.insert(
                DownloadItem(
                    url = url,
                    title = title,
                    format = format.format,
                    quality = format.quality,
                    filePath = "${Environment.DIRECTORY_DOWNLOADS}/$fileName"
                )
            )
        }

        // Reset state after download starts
        updateUrl("")
    }
    
    fun deleteDownload(item: DownloadItem) {
        viewModelScope.launch {
            repository.deleteById(item.id)
            // Note: Does not delete actual file for safety, just removes from history.
        }
    }
}
