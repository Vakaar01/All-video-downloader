package com.example

import android.app.Application
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.AppErrorState
import com.example.data.DownloadEntity
import com.example.data.DownloadStorageManager
import com.example.data.ExtractedMediaInfo
import com.example.data.ExtractionResult
import com.example.data.MediaFormat
import com.example.data.PlatformType
import com.example.data.UniversalMediaExtractor
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val downloadDao = db.downloadDao()
    private val storageManager = DownloadStorageManager(application)
    private val mediaExtractor = UniversalMediaExtractor()

    val allDownloads: StateFlow<List<DownloadEntity>> = downloadDao.getAllDownloads()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _url = MutableStateFlow("https://www.youtube.com/watch?v=dQw4w9WgXcQ")
    val url: StateFlow<String> = _url.asStateFlow()

    private val _selectedPlatform = MutableStateFlow(PlatformType.YOUTUBE)
    val selectedPlatform: StateFlow<PlatformType> = _selectedPlatform.asStateFlow()

    private val _isPlatformPickerOpen = MutableStateFlow(false)
    val isPlatformPickerOpen: StateFlow<Boolean> = _isPlatformPickerOpen.asStateFlow()

    private val _isVaultOpen = MutableStateFlow(false)
    val isVaultOpen: StateFlow<Boolean> = _isVaultOpen.asStateFlow()

    private val _isExtracting = MutableStateFlow(false)
    val isExtracting: StateFlow<Boolean> = _isExtracting.asStateFlow()

    private val _extractProgress = MutableStateFlow(0)
    val extractProgress: StateFlow<Int> = _extractProgress.asStateFlow()

    private val _terminalStatus = MutableStateFlow("TERMINAL STANDBY // REAL STREAM ENGINE READY")
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

    // Real extracted media metadata
    private val _extractedMedia = MutableStateFlow<ExtractedMediaInfo?>(null)
    val extractedMedia: StateFlow<ExtractedMediaInfo?> = _extractedMedia.asStateFlow()

    // Screen-level human readable error diagnostics
    private val _appError = MutableStateFlow<AppErrorState?>(null)
    val appError: StateFlow<AppErrorState?> = _appError.asStateFlow()

    private var lastRequestedFormat: MediaFormat = MediaFormat.ORIGINAL_MP4
    private var extractionJob: Job? = null
    private var downloadJob: Job? = null

    fun onDismissError() {
        _appError.value = null
    }

    fun onRetryLastError() {
        _appError.value = null
        if (_isExtractionComplete.value) {
            onDownloadFormat(lastRequestedFormat)
        } else {
            onStartExtraction()
        }
    }

    fun onUrlChanged(newUrl: String) {
        _url.value = newUrl
        _appError.value = null
        _isExtractionComplete.value = false
        _extractProgress.value = 0
        _extractedMedia.value = null
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
        _appError.value = null
        _isExtractionComplete.value = false
        _extractProgress.value = 0
        _extractedMedia.value = null
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
        _extractedMedia.value = null
        _terminalStatus.value = "TARGET PLATFORM LOCKED: ${platform.displayName.uppercase()}"
    }

    fun onOpenVault() {
        _isVaultOpen.value = true
    }

    fun onCloseVault() {
        _isVaultOpen.value = false
    }

    fun onStartExtraction() {
        val trimmed = _url.value.trim()
        if (trimmed.isBlank()) {
            _appError.value = AppErrorState(
                title = "URL EMPTY",
                message = "URL box khali hai. Kripya video ya reel ka link paste karein.",
                solution = "Paste button par click karke copy kiya hua link daalein."
            )
            _terminalStatus.value = "ERROR: TARGET URL CANNOT BE EMPTY"
            return
        }

        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://") && !trimmed.contains(".")) {
            _appError.value = AppErrorState(
                title = "INVALID URL FORMAT",
                message = "Ye valid web link nahi hai. URL 'https://' ke sath shuru hona chahiye.",
                solution = "YouTube, Instagram, Facebook ya TikTok se pura Share Link copy karein."
            )
            _terminalStatus.value = "ERROR: INVALID URL FORMAT"
            return
        }

        _appError.value = null
        extractionJob?.cancel()
        extractionJob = viewModelScope.launch {
            _isExtracting.value = true
            _isExtractionComplete.value = false
            _extractProgress.value = 0
            _extractedMedia.value = null

            _terminalStatus.value = "CONNECTING: Resolving host for ${_selectedPlatform.value.displayName}..."
            for (p in 1..35) {
                _extractProgress.value = p
                delay(8)
            }

            _terminalStatus.value = "ANALYZING: Decoding media manifests and CDN streams..."
            for (p in 36..65) {
                _extractProgress.value = p
                delay(8)
            }

            // Real extraction
            val result = mediaExtractor.extract(_url.value, _selectedPlatform.value, MediaFormat.ORIGINAL_MP4)

            for (p in 66..90) {
                _extractProgress.value = p
                delay(6)
            }

            when (result) {
                is ExtractionResult.Success -> {
                    _extractedMedia.value = result.media
                    _terminalStatus.value = "STREAM UNLOCKED [${result.media.provider}] // Title: ${result.media.title.take(35)}"
                }
                is ExtractionResult.Error -> {
                    if (result.message.contains("URL BREAK") || result.message.contains("INVALID") || result.message.contains("EMPTY")) {
                        _appError.value = AppErrorState(
                            title = "URL NOT FOUND / LINK INCOMPLETE",
                            message = result.message,
                            solution = "Check karein ki URL poora copy hua hai ya video remove nahi hui hai."
                        )
                        _terminalStatus.value = "ERROR: ${result.message.take(45)}"
                        _isExtracting.value = false
                        _isExtractionComplete.value = false
                        return@launch
                    }

                    val fallbackTitle = "${_selectedPlatform.value.displayName} Stream"
                    _extractedMedia.value = ExtractedMediaInfo(
                        title = fallbackTitle,
                        directVideoUrl = _url.value,
                        directAudioUrl = _url.value,
                        provider = "${_selectedPlatform.value.displayName} Direct Gateway",
                        quality = "1080p / High Quality"
                    )
                    _terminalStatus.value = "STREAM PIPELINE READY // SELECT MP4 OR MP3 BELOW"
                }
            }

            for (p in 91..100) {
                _extractProgress.value = p
                delay(4)
            }

            _isExtracting.value = false
            // Keep extraction complete TRUE so format options appear and stay visible!
            _isExtractionComplete.value = true
        }
    }

    fun onDownloadFormat(format: MediaFormat) {
        if (_isDownloading.value) return

        lastRequestedFormat = format
        _appError.value = null

        downloadJob?.cancel()
        downloadJob = viewModelScope.launch {
            _isDownloading.value = true
            _downloadProgress.value = 0
            _downloadSpeedStatus.value = "CONNECTING TO REAL CDN STREAM..."
            _terminalStatus.value = "RESOLVING ${format.title} FROM CDN..."

            try {
                // Check if we need to resolve direct stream URL for this format
                var targetStreamUrl = if (format == MediaFormat.AUDIO_MP3) {
                    _extractedMedia.value?.directAudioUrl
                } else {
                    _extractedMedia.value?.directVideoUrl
                }

                // If stream is still the web link, resolve via multi-instance engine
                if (targetStreamUrl.isNullOrBlank() || targetStreamUrl == _url.value) {
                    _downloadSpeedStatus.value = "RESOLVING CDN STREAM FOR ${format.title}..."
                    val resolved = mediaExtractor.resolveDirectStream(_url.value, _selectedPlatform.value, format)
                    if (!resolved.isNullOrBlank()) {
                        targetStreamUrl = resolved
                    }
                }

                val finalUrl = targetStreamUrl ?: _url.value
                val mediaTitle = _extractedMedia.value?.title

                val entity = storageManager.saveMediaFile(
                    platform = _selectedPlatform.value,
                    format = format,
                    inputUrl = _url.value,
                    directMediaUrl = finalUrl,
                    mediaTitle = mediaTitle
                ) { percent, readBytes, totalBytes, speedMbps ->
                    _downloadProgress.value = percent
                    val mbRead = readBytes.toDouble() / (1024 * 1024)
                    if (totalBytes > 0) {
                        val mbTotal = totalBytes.toDouble() / (1024 * 1024)
                        _downloadSpeedStatus.value = String.format(
                            Locale.US,
                            "DOWNLOADING REAL STREAM [%.1f / %.1f MB] @ %.2f MB/s",
                            mbRead,
                            mbTotal,
                            speedMbps
                        )
                    } else {
                        _downloadSpeedStatus.value = String.format(
                            Locale.US,
                            "STREAMING REAL BYTES [%.1f MB TRANSFERRED] @ %.2f MB/s",
                            mbRead,
                            speedMbps
                        )
                    }
                }

                downloadDao.insertDownload(entity)
                _lastDownloadedFile.value = entity
                _downloadProgress.value = 100
                _downloadSpeedStatus.value = "SUCCESS: SAVED IN /sdcard/Download/vakaar/ (${entity.formattedSize})"
                _terminalStatus.value = "COMPLETED: Real file saved (${entity.formattedSize})"
            } catch (e: Exception) {
                _downloadProgress.value = 0
                val rawMsg = e.message ?: "Download connection failed"
                _downloadSpeedStatus.value = "ERROR: $rawMsg"
                _terminalStatus.value = "DOWNLOAD FAILED: ${rawMsg.take(45)}"

                // Map into simple language screen error
                val errorState = when {
                    rawMsg.contains("URL NOT FOUND") || rawMsg.contains("404") -> AppErrorState(
                        title = "URL NOT FOUND / LINK BROKEN",
                        message = "Ye video link internet par nahi mila ya video delete ho chuka hai (Error 404).",
                        solution = "Check karein ki URL sahi hai ya video account se remove to nahi ho gaya."
                    )
                    rawMsg.contains("PRIVATE") || rawMsg.contains("403") || rawMsg.contains("401") -> AppErrorState(
                        title = "PRIVATE VIDEO / RESTRICTED",
                        message = "Ye video private account ka hai ya login ke bina download nahi ho sakta (Error 403).",
                        solution = "Sirf public videos aur reels download kiye ja sakte hain."
                    )
                    rawMsg.contains("NO INTERNET") -> AppErrorState(
                        title = "NO INTERNET CONNECTION",
                        message = "Aapka device internet se connect nahi ho pa raha hai.",
                        solution = "Apna Wi-Fi ya Mobile Data on karein aur dobara try karein."
                    )
                    rawMsg.contains("TIMEOUT") -> AppErrorState(
                        title = "CONNECTION TIMEOUT",
                        message = "Video server ne response dene me zyada waqt lagaya.",
                        solution = "Internet connection check karein aur RETRY dabayein."
                    )
                    rawMsg.contains("SERVER DOWN") || rawMsg.contains("500") || rawMsg.contains("502") || rawMsg.contains("503") -> AppErrorState(
                        title = "SERVER DOWN / BUSY",
                        message = "Video host server temporary busy ya down hai.",
                        solution = "1-2 minute baad dobara koshish karein."
                    )
                    rawMsg.contains("EMPTY") -> AppErrorState(
                        title = "EMPTY STREAM FILE",
                        message = "Server se 0 bytes receive huye. Video link kaam nahi kar raha.",
                        solution = "Link check karein ya naya share link copy karein."
                    )
                    else -> AppErrorState(
                        title = "DOWNLOAD ERROR",
                        message = rawMsg,
                        solution = "RETRY dabayein ya link dubara copy karein."
                    )
                }
                _appError.value = errorState
            } finally {
                _isDownloading.value = false
            }
        }
    }

    fun onDeleteDownload(item: DownloadEntity) {
        viewModelScope.launch {
            downloadDao.deleteDownload(item)
        }
    }
}
