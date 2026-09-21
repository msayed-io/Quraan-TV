package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AudioTrack
import com.example.data.PreferencesManager
import com.example.data.QuranScanner
import com.example.data.RepeatMode
import com.example.player.QuranAudioPlayer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class ViewMode {
    ALL,
    FAVORITES
}

data class QuranPlayerUiState(
    val tracks: List<AudioTrack> = emptyList(),
    val currentTrack: AudioTrack? = null,
    val currentTrackIndex: Int = -1,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val progress: Float = 0f,
    val repeatMode: RepeatMode = RepeatMode.ALL,
    val isShuffle: Boolean = false,
    val hasStoragePermission: Boolean = false,
    val isLoadingFiles: Boolean = false,
    val errorMessage: String? = null,
    
    // New Advanced Features State
    val favorites: Set<String> = emptySet(),
    val currentViewMode: ViewMode = ViewMode.ALL,
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val sleepTimerMinutes: Int? = null,
    val sleepTimerRemainingSeconds: Int = 0,
    val isScreensaverActive: Boolean = false
)

class QuranPlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val prefsManager = PreferencesManager(application.applicationContext)

    private val _uiState = MutableStateFlow(
        QuranPlayerUiState(favorites = prefsManager.getFavorites())
    )
    val uiState: StateFlow<QuranPlayerUiState> = _uiState.asStateFlow()

    private var progressJob: Job? = null
    private var sleepTimerJob: Job? = null
    private var onSleepFinishedCallback: (() -> Unit)? = null

    private val player = QuranAudioPlayer(
        context = application.applicationContext,
        onTrackCompleted = { handleTrackCompleted() },
        onError = { msg ->
            _uiState.update {
                it.copy(
                    isPlaying = false,
                    isBuffering = false,
                    errorMessage = msg
                )
            }
            stopProgressTracker()
        }
    )

    fun onPermissionResult(granted: Boolean) {
        _uiState.update { it.copy(hasStoragePermission = granted) }
        if (granted) {
            loadAudioFiles()
        }
    }

    fun loadAudioFiles() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingFiles = true, errorMessage = null) }
            try {
                val scannedTracks = QuranScanner.scanDownloadFolder(getApplication())
                val bookmark = prefsManager.getBookmark()
                
                var restoredTrack: AudioTrack? = null
                var restoredPosMs = 0L

                if (bookmark != null) {
                    restoredTrack = scannedTracks.find { it.filePath == bookmark.first }
                    if (restoredTrack != null) {
                        restoredPosMs = bookmark.second
                    }
                }

                _uiState.update { state ->
                    val selected = state.currentTrack?.let { curr ->
                        scannedTracks.find { it.filePath == curr.filePath }
                    } ?: restoredTrack ?: scannedTracks.firstOrNull()

                    val selectedIndex = if (selected != null) scannedTracks.indexOfFirst { it.id == selected.id } else -1
                    val posMs = if (selected?.filePath == restoredTrack?.filePath) restoredPosMs else 0L
                    val duration = selected?.durationMs ?: 0L
                    val prog = if (duration > 0) (posMs.toFloat() / duration).coerceIn(0f, 1f) else 0f

                    state.copy(
                        tracks = scannedTracks,
                        currentTrack = selected,
                        currentTrackIndex = selectedIndex,
                        currentPositionMs = posMs,
                        durationMs = duration,
                        progress = prog,
                        isLoadingFiles = false
                    )
                }

                // If bookmark track was restored, seek player to saved position
                if (restoredTrack != null && restoredPosMs > 0) {
                    player.playTrack(restoredTrack) {
                        player.seekTo(restoredPosMs)
                        player.pause()
                        _uiState.update { it.copy(isPlaying = false, isBuffering = false) }
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingFiles = false,
                        tracks = emptyList(),
                        errorMessage = "حدث خطأ أثناء فحص مجلد Download: ${e.message}"
                    )
                }
            }
        }
    }

    fun selectAndPlayTrack(track: AudioTrack) {
        saveBookmark()
        val tracks = _uiState.value.tracks
        val index = tracks.indexOfFirst { it.id == track.id }
        _uiState.update {
            it.copy(
                currentTrack = track,
                currentTrackIndex = index,
                isBuffering = true,
                isPlaying = false,
                currentPositionMs = 0L,
                durationMs = track.durationMs,
                progress = 0f,
                errorMessage = null
            )
        }

        player.playTrack(track) {
            val actualDuration = player.duration
            _uiState.update {
                it.copy(
                    isPlaying = true,
                    isBuffering = false,
                    durationMs = if (actualDuration > 0) actualDuration else track.durationMs
                )
            }
            startProgressTracker()
        }
    }

    fun togglePlayPause() {
        val state = _uiState.value
        val track = state.currentTrack ?: state.tracks.firstOrNull() ?: return

        if (state.currentTrack == null) {
            selectAndPlayTrack(track)
            return
        }

        if (state.isPlaying) {
            player.pause()
            saveBookmark()
            _uiState.update { it.copy(isPlaying = false) }
            stopProgressTracker()
        } else {
            if (player.isPlaying) {
                _uiState.update { it.copy(isPlaying = true) }
                startProgressTracker()
            } else {
                player.resume()
                _uiState.update { it.copy(isPlaying = true) }
                startProgressTracker()
            }
        }
    }

    // =========================================================================
    // FAVORITES & VIEW MODE
    // =========================================================================
    fun toggleFavorite(track: AudioTrack) {
        prefsManager.toggleFavorite(track.filePath)
        _uiState.update { it.copy(favorites = prefsManager.getFavorites()) }
    }

    fun setViewMode(mode: ViewMode) {
        _uiState.update { it.copy(currentViewMode = mode) }
    }

    // =========================================================================
    // SEARCH PANEL
    // =========================================================================
    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun toggleSearchActive(active: Boolean) {
        _uiState.update { 
            it.copy(
                isSearchActive = active,
                searchQuery = if (!active) "" else it.searchQuery
            ) 
        }
    }

    // =========================================================================
    // SLEEP TIMER
    // =========================================================================
    fun setSleepTimer(minutes: Int?, onFinish: (() -> Unit)? = null) {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        onSleepFinishedCallback = onFinish

        if (minutes == null || minutes <= 0) {
            _uiState.update { it.copy(sleepTimerMinutes = null, sleepTimerRemainingSeconds = 0) }
            return
        }

        val totalSeconds = minutes * 60
        _uiState.update { it.copy(sleepTimerMinutes = minutes, sleepTimerRemainingSeconds = totalSeconds) }

        sleepTimerJob = viewModelScope.launch {
            var remaining = totalSeconds
            while (isActive && remaining > 0) {
                delay(1000L)
                remaining--
                _uiState.update { it.copy(sleepTimerRemainingSeconds = remaining) }
            }

            if (remaining <= 0) {
                // Sleep Timer Expired: stop playback safely and finish Activity
                player.pause()
                saveBookmark()
                _uiState.update {
                    it.copy(
                        isPlaying = false,
                        sleepTimerMinutes = null,
                        sleepTimerRemainingSeconds = 0
                    )
                }
                stopProgressTracker()
                onSleepFinishedCallback?.invoke()
            }
        }
    }

    // =========================================================================
    // SCREENSAVER
    // =========================================================================
    fun setScreensaverActive(active: Boolean) {
        _uiState.update { it.copy(isScreensaverActive = active) }
    }

    // =========================================================================
    // BOOKMARK SAVING
    // =========================================================================
    fun saveBookmark() {
        val curr = _uiState.value.currentTrack ?: return
        val pos = player.currentPosition
        if (curr.filePath.isNotBlank() && pos >= 0) {
            prefsManager.saveBookmark(curr.filePath, pos)
        }
    }

    fun playNext() {
        val state = _uiState.value
        if (state.tracks.isEmpty()) return

        val nextIndex = if (state.isShuffle) {
            state.tracks.indices.random()
        } else {
            if (state.currentTrackIndex < state.tracks.size - 1) {
                state.currentTrackIndex + 1
            } else {
                0
            }
        }

        val nextTrack = state.tracks.getOrNull(nextIndex) ?: return
        selectAndPlayTrack(nextTrack)
    }

    fun playPrevious() {
        val state = _uiState.value
        if (state.tracks.isEmpty()) return

        if (player.currentPosition > 3000) {
            player.seekTo(0)
            _uiState.update { it.copy(currentPositionMs = 0L, progress = 0f) }
            return
        }

        val prevIndex = if (state.isShuffle) {
            state.tracks.indices.random()
        } else {
            if (state.currentTrackIndex > 0) {
                state.currentTrackIndex - 1
            } else {
                state.tracks.size - 1
            }
        }

        val prevTrack = state.tracks.getOrNull(prevIndex) ?: return
        selectAndPlayTrack(prevTrack)
    }

    fun seekToFraction(fraction: Float) {
        val total = player.duration.takeIf { it > 0 } ?: _uiState.value.durationMs
        if (total > 0) {
            val targetMs = (fraction * total).toLong()
            player.seekTo(targetMs)
            updateCurrentProgressManually()
        }
    }

    fun cycleRepeatMode() {
        _uiState.update {
            val nextMode = when (it.repeatMode) {
                RepeatMode.ALL -> RepeatMode.ONE
                RepeatMode.ONE -> RepeatMode.OFF
                RepeatMode.OFF -> RepeatMode.ALL
            }
            it.copy(repeatMode = nextMode)
        }
    }

    fun toggleShuffle() {
        _uiState.update { it.copy(isShuffle = !it.isShuffle) }
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun handleTrackCompleted() {
        val state = _uiState.value
        when (state.repeatMode) {
            RepeatMode.ONE -> {
                state.currentTrack?.let { selectAndPlayTrack(it) }
            }
            RepeatMode.ALL -> {
                playNext()
            }
            RepeatMode.OFF -> {
                if (state.currentTrackIndex < state.tracks.size - 1) {
                    playNext()
                } else {
                    _uiState.update {
                        it.copy(
                            isPlaying = false,
                            currentPositionMs = 0L,
                            progress = 0f
                        )
                    }
                    stopProgressTracker()
                }
            }
        }
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            var ticks = 0
            while (isActive) {
                val current = player.currentPosition
                val total = player.duration.takeIf { it > 0 } ?: _uiState.value.durationMs
                val prog = if (total > 0) (current.toFloat() / total).coerceIn(0f, 1f) else 0f

                _uiState.update {
                    it.copy(
                        currentPositionMs = current,
                        durationMs = total,
                        progress = prog
                    )
                }
                ticks++
                if (ticks % 10 == 0) { // Every 5 seconds (10 * 500ms)
                    saveBookmark()
                }
                delay(500L) // Ultra-lightweight 500ms cycle for 1GB RAM TV
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        progressJob = null
    }

    private fun updateCurrentProgressManually() {
        val current = player.currentPosition
        val total = player.duration.takeIf { it > 0 } ?: _uiState.value.durationMs
        val prog = if (total > 0) (current.toFloat() / total).coerceIn(0f, 1f) else 0f
        _uiState.update {
            it.copy(
                currentPositionMs = current,
                durationMs = total,
                progress = prog
            )
        }
    }

    fun releasePlayer() {
        stopProgressTracker()
        player.release()
    }

    override fun onCleared() {
        super.onCleared()
        releasePlayer()
    }
}

