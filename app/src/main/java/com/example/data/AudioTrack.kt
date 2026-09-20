package com.example.data

import android.net.Uri

data class AudioTrack(
    val id: String,
    val title: String,
    val surahNameArabic: String,
    val reciterOrSubtitle: String,
    val fileName: String,
    val filePath: String,
    val uri: Uri?,
    val durationMs: Long = 0L,
    val sizeBytes: Long = 0L,
    val isSample: Boolean = false
) {
    val formattedDuration: String
        get() = formatDuration(durationMs)

    val formattedSize: String
        get() {
            if (sizeBytes <= 0) return ""
            val mb = sizeBytes / (1024.0 * 1024.0)
            return String.format("%.1f MB", mb)
        }

    companion object {
        fun formatDuration(ms: Long): String {
            if (ms <= 0) return "00:00"
            val totalSeconds = ms / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            val hours = minutes / 60
            return if (hours > 0) {
                String.format("%02d:%02d:%02d", hours, minutes % 60, seconds)
            } else {
                String.format("%02d:%02d", minutes, seconds)
            }
        }
    }
}

enum class RepeatMode {
    OFF,
    ALL,
    ONE
}
