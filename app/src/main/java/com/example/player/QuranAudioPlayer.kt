package com.example.player

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.PowerManager
import android.util.Log
import com.example.data.AudioTrack

class QuranAudioPlayer(
    private val context: Context,
    private val onTrackCompleted: () -> Unit,
    private val onError: (String) -> Unit
) {
    private var mediaPlayer: MediaPlayer? = null
    private var currentTrack: AudioTrack? = null

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

    fun playTrack(track: AudioTrack, onPrepared: () -> Unit) {
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

                setOnPreparedListener { mp ->
                    try {
                        mp.start()
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
            Log.e(TAG, "Error initializing player: ${e.message}", e)
            onError("تعذر فتح الملف الصوتي: ${e.message}")
        }
    }

    fun resume() {
        try {
            mediaPlayer?.let {
                if (!it.isPlaying) {
                    it.start()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error resuming playback: ${e.message}")
        }
    }

    fun pause() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.pause()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error pausing playback: ${e.message}")
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
        releasePlayer()
    }

    private fun releasePlayer() {
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

