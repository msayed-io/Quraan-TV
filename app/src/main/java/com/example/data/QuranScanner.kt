package com.example.data

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Environment
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object QuranScanner {
    private const val TAG = "QuranScanner"

    // All supported audio & video container extensions that can contain audio recitations
    private val AUDIO_EXTENSIONS = setOf(
        "mp3", "wav", "m4a", "aac", "ogg", "flac", "opus",
        "mp4", "m4v", "mka", "webm", "3gp", "amr", "wma",
        "aiff", "mid", "midi"
    )

    /**
     * Scans local storage directories for Quran audio/video files.
     * 100% Offline with zero internet or mock data.
     */
    suspend fun scanDownloadFolder(context: Context): List<AudioTrack> = withContext(Dispatchers.IO) {
        val tracks = mutableListOf<AudioTrack>()
        val seenPaths = mutableSetOf<String>()

        try {
            val scanDirs = mutableListOf<File>()

            // Add standard Android public directories
            try {
                scanDirs.add(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS))
                scanDirs.add(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC))
                scanDirs.add(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES))
                scanDirs.add(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS))
            } catch (e: Exception) {
                Log.w(TAG, "Error getting public directories: ${e.message}")
            }

            // Fallback & common custom Quran folder locations
            val fallbackPaths = listOf(
                "/storage/emulated/0/Download",
                "/storage/emulated/0/Music",
                "/storage/emulated/0/Movies",
                "/storage/emulated/0/Documents",
                "/storage/emulated/0/Quran",
                "/storage/emulated/0/القرآن الكريم",
                "/sdcard/Download",
                "/sdcard/Music",
                "/sdcard/Quran"
            )

            for (path in fallbackPaths) {
                val f = File(path)
                if (f.exists() && f.isDirectory && !scanDirs.contains(f)) {
                    scanDirs.add(f)
                }
            }

            for (dir in scanDirs) {
                if (dir.exists() && dir.isDirectory) {
                    scanDirectory(dir, tracks, seenPaths)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error scanning storage directories: ${e.message}", e)
        }

        // Sort tracks naturally by surah number, then Arabic title
        tracks.sortedWith(
            compareBy<AudioTrack> { extractNumber(it.fileName) }
                .thenBy { it.title }
        )
    }

    private fun scanDirectory(dir: File, tracks: MutableList<AudioTrack>, seenPaths: MutableSet<String>) {
        try {
            val files = dir.listFiles() ?: return
            for (file in files) {
                if (file.isDirectory && !file.name.startsWith(".")) {
                    // Search sub-folders inside Download folder
                    scanDirectory(file, tracks, seenPaths)
                } else if (file.isFile && file.length() > 0) {
                    val ext = file.extension.lowercase()
                    if (AUDIO_EXTENSIONS.contains(ext) && seenPaths.add(file.absolutePath)) {
                        val track = extractTrackMetadata(file)
                        tracks.add(track)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error reading directory ${dir.path}: ${e.message}")
        }
    }

    private fun extractTrackMetadata(file: File): AudioTrack {
        var durationMs = 0L
        var metaTitle: String? = null
        var metaArtist: String? = null

        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(file.absolutePath)
            durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            metaTitle = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            metaArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
        } catch (e: Exception) {
            Log.w(TAG, "Metadata extraction failed for ${file.name}: ${e.message}")
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
                // Ignore release error
            }
        }

        val (surahName, subtitle) = QuranNamesHelper.parseSurahDetails(file.name, metaTitle)
        val finalSubtitle = if (!metaArtist.isNullOrBlank() && metaArtist != "<unknown>") metaArtist else subtitle

        return AudioTrack(
            id = "file_${file.absolutePath.hashCode()}",
            title = surahName,
            surahNameArabic = surahName,
            reciterOrSubtitle = finalSubtitle,
            fileName = file.name,
            filePath = file.absolutePath,
            uri = Uri.fromFile(file),
            durationMs = durationMs,
            sizeBytes = file.length(),
            isSample = false
        )
    }

    private fun extractNumber(str: String): Int {
        val numStr = str.filter { it.isDigit() }
        return numStr.toIntOrNull() ?: Int.MAX_VALUE
    }
}

