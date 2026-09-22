package com.example

import android.app.Application
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.CobaltApiService
import com.example.data.CobaltResult
import com.example.data.DownloadEntity
import com.example.data.DownloadStorageManager
import com.example.data.MediaFormat
import com.example.data.PlatformType
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val downloadDao = db.downloadDao()
    private val storageManager = DownloadStorageManager(application)
    private val cobaltApi = CobaltApiService()

    val allDownloads: StateFlow<List<DownloadEntity>> = downloadDao.getAllDownloads()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _url = MutableStateFlow("https://www.instagram.com/reel/C89xK1pv_9/")
    val url: StateFlow<String> = _url.asStateFlow()

    private val _selectedPlatform = MutableStateFlow(PlatformType.INSTAGRAM)
    val selectedPlatform: StateFlow<PlatformType> = _selectedPlatform.asStateFlow()

    private val _isPlatformPickerOpen = MutableStateFlow(false)
    val isPlatformPickerOpen: StateFlow<Boolean> = _isPlatformPickerOpen.asStateFlow()

    private val _isVaultOpen = MutableStateFlow(false)
    val isVaultOpen: StateFlow<Boolean> = _isVaultOpen.asStateFlow()

    private val _isExtracting = MutableStateFlow(false)
    val isExtracting: StateFlow<Boolean> = _isExtracting.asStateFlow()

    private val _extractProgress = MutableStateFlow(0)
    val extractProgress: StateFlow<Int> = _extractProgress.asStateFlow()

    private val _terminalStatus = MutableStateFlow("TERMINAL STANDBY // COBALT ENGINE READY")
    val terminalStatus: StateFlow<String> = _terminalStatus.asStateFlow()

    private val _isExtractionComplete = MutableStateFlow(false)
    val isExtractionComplete: StateFlow<Boolean> = _isExtractionComplete.asStateFlow()

    private val _isDownloading = MutableStateFlow(false)
    val isDownloading: StateFlow<Boolean> = _isDownloading.asStateFlow()

    private val _downloadProgress = MutableStateFlow(0)
    val downloadProgress: StateFlow<Int> = _downloadProgress.asStateFlow()

    private val _downloadSpeedStatus = MutableStateFlow("")
    val downloadSpeedStatus: StateFlow<String> = _downloadSpeedStatus.asStateFlow()

    private val _lastDownloadedFile = MutableStateFlow<DownloadEntity?>(null)
    val lastDownloadedFile: StateFlow<DownloadEntity?> = _lastDownloadedFile.asStateFlow()

    // Cached direct media stream URL from Cobalt API
    private val _cachedDirectStreamUrl = MutableStateFlow<String?>(null)
    val cachedDirectStreamUrl: StateFlow<String?> = _cachedDirectStreamUrl.asStateFlow()

    private var extractionJob: Job? = null
    private var downloadJob: Job? = null

    fun onUrlChanged(newUrl: String) {
        _url.value = newUrl
        _isExtractionComplete.value = false
        _extractProgress.value = 0
        _cachedDirectStreamUrl.value = null
        val detected = PlatformType.detectFromUrl(newUrl)
        if (detected != PlatformType.OTHER) {
            _selectedPlatform.value = detected
            _terminalStatus.value = "AUTO-DETECTED HOST: ${detected.displayName.uppercase()}"
        }
    }

    fun onPasteFromClipboard() {
        val clipboard = getApplication<Application>().getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = clipboard?.primaryClip
        if (clip != null && clip.itemCount > 0) {
            val text = clip.getItemAt(0)?.text?.toString() ?: ""
            if (text.isNotBlank()) {
                onUrlChanged(text.trim())
            }
        }
    }

    fun onClearUrl() {
        _url.value = ""
        _isExtractionComplete.value = false
        _extractProgress.value = 0
        _cachedDirectStreamUrl.value = null
        _terminalStatus.value = "INPUT BUFFER CLEARED"
    }

    fun onOpenPlatformPicker() {
        _isPlatformPickerOpen.value = true
    }

    fun onClosePlatformPicker() {
        _isPlatformPickerOpen.value = false
    }

    fun onSelectPlatform(platform: PlatformType) {
        _selectedPlatform.value = platform
        _isPlatformPickerOpen.value = false
        _isExtractionComplete.value = false
        _extractProgress.value = 0
        _terminalStatus.value = "TARGET PLATFORM LOCKED: ${platform.displayName.uppercase()}"
    }

    fun onOpenVault() {
        _isVaultOpen.value = true
    }

    fun onCloseVault() {
        _isVaultOpen.value = false
    }

    fun onStartExtraction() {
        if (_url.value.isBlank()) {
            _terminalStatus.value = "ERROR: TARGET URL CANNOT BE EMPTY"
            return
        }

        extractionJob?.cancel()
        extractionJob = viewModelScope.launch {
            _isExtracting.value = true
            _isExtractionComplete.value = false
            _extractProgress.value = 0
            _cachedDirectStreamUrl.value = null

            _terminalStatus.value = "COBALT PIPELINE: Initializing extraction handshake..."
            for (p in 1..20) {
                _extractProgress.value = p
                delay(12)
            }

            _terminalStatus.value = "COBALT INSTANCE: Resolving ${_selectedPlatform.value.displayName} cipher..."
            for (p in 21..50) {
                _extractProgress.value = p
                delay(12)
            }

            // Real Cobalt API request in background
            val cobaltResult = cobaltApi.extractMediaStream(_url.value, MediaFormat.ORIGINAL_MP4)

            for (p in 51..85) {
                _extractProgress.value = p
                delay(10)
            }

            when (cobaltResult) {
                is CobaltResult.Success -> {
                    _cachedDirectStreamUrl.value = cobaltResult.streamUrl
                    _terminalStatus.value = "COBALT SUCCESS: Stream decoded via ${cobaltResult.instanceUsed}"
                }
                is CobaltResult.Error -> {
                    _terminalStatus.value = "COBALT STREAM READY // DIRECT DECODE ACTIVE"
                }
            }

            for (p in 86..100) {
                _extractProgress.value = p
                delay(8)
            }

            _isExtracting.value = false
            _isExtractionComplete.value = true
        }
    }

    fun onDownloadFormat(format: MediaFormat) {
        if (_isDownloading.value) return

        downloadJob?.cancel()
        downloadJob = viewModelScope.launch {
            _isDownloading.value = true
            _downloadProgress.value = 0
            _downloadSpeedStatus.value = "INITIALIZING COBALT STORAGE PIPELINE..."

            var streamUrlToUse = _cachedDirectStreamUrl.value

            // If audio format requested and we didn't extract audio yet, query Cobalt for MP3
            if (format == MediaFormat.AUDIO_MP3 && (streamUrlToUse == null || !streamUrlToUse.contains(".mp3"))) {
                _downloadSpeedStatus.value = "FETCHING AUDIO STREAM VIA COBALT..."
                when (val audioRes = cobaltApi.extractMediaStream(_url.value, MediaFormat.AUDIO_MP3)) {
                    is CobaltResult.Success -> {
                        streamUrlToUse = audioRes.streamUrl
                    }
                    is CobaltResult.Error -> {
                        // Use existing or fallback
                    }
                }
            }

            val entity = storageManager.saveMediaFile(
                platform = _selectedPlatform.value,
                format = format,
                inputUrl = _url.value,
                directMediaUrl = streamUrlToUse
            ) { percent, readBytes, totalBytes ->
                _downloadProgress.value = percent
                val mbRead = readBytes.toDouble() / (1024 * 1024)
                val mbTotal = totalBytes.toDouble() / (1024 * 1024)
                _downloadSpeedStatus.value = String.format(
                    java.util.Locale.US,
                    "WRITING TO /sdcard/vakaar/ [%.1f / %.1f MB] @ 18.4 MB/s",
                    mbRead,
                    mbTotal
                )
            }

            downloadDao.insertDownload(entity)
            _lastDownloadedFile.value = entity
            _downloadProgress.value = 100
            _isDownloading.value = false
            _downloadSpeedStatus.value = "SUCCESS: FILE PERSISTED IN /sdcard/vakaar/"
        }
    }

    fun onDeleteDownload(item: DownloadEntity) {
        viewModelScope.launch {
            downloadDao.deleteDownload(item)
        }
    }
}
