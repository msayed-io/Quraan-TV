package com.example.player

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.audiofx.Equalizer
import android.os.Build
import android.os.PowerManager
import android.util.Log
import com.example.data.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class QuranAudioPlayer(
    private val context: Context,
    private val onTrackCompleted: () -> Unit,
    private val onError: (String) -> Unit
) {
    private var mediaPlayer: MediaPlayer? = null
    private var currentTrack: AudioTrack? = null
    private var equalizer: Equalizer? = null
    private var isNightMode: Boolean = false

    private val playerScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var fadeJob: Job? = null
    private var currentVolume: Float = 1.0f

    val isPlaying: Boolean
        get() = try {
            mediaPlayer?.isPlaying == true
        } catch (e: Exception) {
            false
        }

    val currentPosition: Long
        get() = try {
            mediaPlayer?.currentPosition?.toLong() ?: 0L
        } catch (e: Exception) {
            0L
        }

    val duration: Long
        get() = try {
            val d = mediaPlayer?.duration?.toLong() ?: 0L
            if (d > 0) d else (currentTrack?.durationMs ?: 0L)
        } catch (e: Exception) {
            currentTrack?.durationMs ?: 0L
        }

    val nightModeEnabled: Boolean
        get() = isNightMode

    /**
     * Toggles or sets Night Audio Mode (Vocal Booster Equalizer).
     * 100% Hardware/OS-level equalizer for 1GB RAM efficiency.
     */
    fun setNightMode(enabled: Boolean) {
        isNightMode = enabled
        applyNightModeEqualizer()
    }

    fun playTrack(track: AudioTrack, onPrepared: () -> Unit) {
        try {
            // Cancel previous fade job
            fadeJob?.cancel()
            fadeJob = null

            // If a track is currently playing, smooth fade out before switching
            if (isPlaying) {
                fadeVolume(from = currentVolume, to = 0.0f, durationMs = 250L) {
                    initAndStartTrack(track, onPrepared)
                }
            } else {
                initAndStartTrack(track, onPrepared)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing player: ${e.message}", e)
            onError("تعذر فتح الملف الصوتي: ${e.message}")
        }
    }

    private fun initAndStartTrack(track: AudioTrack, onPrepared: () -> Unit) {
        try {
            releasePlayer()
            currentTrack = track

            val player = MediaPlayer().apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
                    )
                } else {
                    @Suppress("DEPRECATION")
                    setAudioStreamType(AudioManager.STREAM_MUSIC)
                }

                try {
                    setWakeMode(context.applicationContext, PowerManager.PARTIAL_WAKE_LOCK)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to set wake lock: ${e.message}")
                }

                // Strictly local file source (zero internet)
                if (track.uri != null) {
                    setDataSource(context, track.uri)
                } else {
                    setDataSource(track.filePath)
                }

                // Start initially silent for smooth cinematic Fade In
                setVolume(0.0f, 0.0f)
                currentVolume = 0.0f

                setOnPreparedListener { mp ->
                    try {
                        mp.start()
                        setupEqualizer(mp.audioSessionId)
                        applyNightModeEqualizer()
                        fadeIn(durationMs = 500L)
                        onPrepared()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error starting playback: ${e.message}")
                        onError("تعذر بدء تشغيل الملف: ${e.message}")
                    }
                }

                setOnCompletionListener {
                    onTrackCompleted()
                }

                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "MediaPlayer error: what=$what extra=$extra")
                    onError("خطأ في تشغيل الملف الصوتي ($what)")
                    true
                }

                prepareAsync()
            }
            mediaPlayer = player
        } catch (e: Exception) {
            Log.e(TAG, "Error in initAndStartTrack: ${e.message}", e)
            onError("تعذر تشغيل الملف الصوتي: ${e.message}")
        }
    }

    fun resume() {
        try {
            mediaPlayer?.let {
                if (!it.isPlaying) {
                    // Smooth Fade In on Resume
                    it.setVolume(0.0f, 0.0f)
                    currentVolume = 0.0f
                    it.start()
                    fadeIn(durationMs = 450L)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error resuming playback: ${e.message}")
        }
    }

    fun pause(onCompleted: (() -> Unit)? = null) {
        try {
            val mp = mediaPlayer
            if (mp != null && mp.isPlaying) {
                // Smooth Fade Out on Pause
                fadeOut(durationMs = 400L) {
                    try {
                        if (mp.isPlaying) {
                            mp.pause()
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Error during pause after fade: ${e.message}")
                    }
                    onCompleted?.invoke()
                }
            } else {
                onCompleted?.invoke()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error pausing playback: ${e.message}")
            onCompleted?.invoke()
        }
    }

    /**
     * Smoothly ramps volume up to 1.0f over the specified duration.
     */
    fun fadeIn(durationMs: Long = 500L) {
        fadeVolume(from = currentVolume, to = 1.0f, durationMs = durationMs)
    }

    /**
     * Smoothly ramps volume down to 0.0f over the specified duration.
     */
    fun fadeOut(durationMs: Long = 400L, onComplete: (() -> Unit)? = null) {
        fadeVolume(from = currentVolume, to = 0.0f, durationMs = durationMs, onComplete = onComplete)
    }

    private fun fadeVolume(from: Float, to: Float, durationMs: Long, onComplete: (() -> Unit)? = null) {
        fadeJob?.cancel()
        val steps = 15
        val stepDelay = (durationMs / steps).coerceAtLeast(15L)
        val delta = (to - from) / steps

        fadeJob = playerScope.launch {
            var vol = from
            for (i in 1..steps) {
                if (!isActive) break
                vol = (vol + delta).coerceIn(0.0f, 1.0f)
                setVolumeInternal(vol)
                delay(stepDelay)
            }
            if (isActive) {
                setVolumeInternal(to)
                onComplete?.invoke()
            }
        }
    }

    private fun setVolumeInternal(vol: Float) {
        currentVolume = vol
        try {
            mediaPlayer?.setVolume(vol, vol)
        } catch (e: Exception) {
            // Ignore volume setting errors if player state changed
        }
    }

    // =========================================================================
    // NIGHT AUDIO MODE (VOCAL BOOSTER EQUALIZER)
    // =========================================================================
    private fun setupEqualizer(audioSessionId: Int) {
        try {
            equalizer?.release()
            equalizer = null
            if (audioSessionId > 0) {
                equalizer = Equalizer(0, audioSessionId).apply {
                    enabled = isNightMode
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Equalizer hardware not supported on this TV: ${e.message}")
            equalizer = null
        }
    }

    private fun applyNightModeEqualizer() {
        val eq = equalizer ?: return
        try {
            if (!isNightMode) {
                // Flat reset
                val numBands = eq.numberOfBands
                for (band in 0 until numBands) {
                    eq.setBandLevel(band.toShort(), 0.toShort())
                }
                eq.enabled = false
                return
            }

            eq.enabled = true
            val numBands = eq.numberOfBands
            val levelRange = eq.bandLevelRange // [minLevel, maxLevel] in millibels
            val minLevel = levelRange[0]
            val maxLevel = levelRange[1]

            // Speech enhancement profile:
            // 1. Attenuate low rumble (< 250Hz) to prevent room vibration and bass hum at night
            // 2. Boost speech range (500Hz - 3000Hz) to make reciter's articulation clear and crisp
            // 3. Attenuate harsh hiss / static (> 6000Hz) from old tape/hall recordings
            for (band in 0 until numBands) {
                val centerFreq = eq.getCenterFreq(band.toShort()) / 1000 // In Hz
                val targetLevelMb: Short = when {
                    centerFreq < 250 -> {
                        // Low rumble cut: -300 mB (-3 dB)
                        (-300).coerceIn(minLevel.toInt(), maxLevel.toInt()).toShort()
                    }
                    centerFreq in 500..3000 -> {
                        // Vocal booster: +450 mB (+4.5 dB)
                        450.coerceIn(minLevel.toInt(), maxLevel.toInt()).toShort()
                    }
                    centerFreq > 6000 -> {
                        // High hiss softening: -250 mB (-2.5 dB)
                        (-250).coerceIn(minLevel.toInt(), maxLevel.toInt()).toShort()
                    }
                    else -> 0.toShort()
                }
                eq.setBandLevel(band.toShort(), targetLevelMb)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error applying night mode equalizer: ${e.message}")
        }
    }

    fun seekTo(positionMs: Long) {
        try {
            val safePos = positionMs.coerceIn(0L, duration.coerceAtLeast(1L)).toInt()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mediaPlayer?.seekTo(safePos.toLong(), MediaPlayer.SEEK_CLOSEST)
            } else {
                mediaPlayer?.seekTo(safePos)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error seeking: ${e.message}")
        }
    }

    fun release() {
        fadeJob?.cancel()
        fadeJob = null
        releasePlayer()
    }

    private fun releasePlayer() {
        try {
            equalizer?.apply {
                enabled = false
                release()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing Equalizer: ${e.message}")
        } finally {
            equalizer = null
        }

        try {
            mediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                reset()
                release()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing MediaPlayer: ${e.message}")
        } finally {
            mediaPlayer = null
        }
    }

    companion object {
        private const val TAG = "QuranAudioPlayer"
    }
}

