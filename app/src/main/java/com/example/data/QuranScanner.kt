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

    // All supported audio & video container extensions
    private val AUDIO_EXTENSIONS = setOf(
        "mp3", "wav", "m4a", "aac", "ogg", "flac", "opus",
        "mp4", "m4v", "mka", "webm", "3gp", "amr", "wma",
        "aiff", "mid", "midi", "mkv", "avi", "mov", "ts"
    )

    /**
     * Scans local storage directories for Quran audio/video files.
     * 100% Offline with zero internet or mock data.
     */
    suspend fun scanDownloadFolder(context: Context): List<AudioTrack> = withContext(Dispatchers.IO) {
        val tracks = mutableListOf<AudioTrack>()
        val seenPaths = mutableSetOf<String>()
        val seenNames = mutableSetOf<String>()

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
                if (f.exists() && f.isDirectory) {
                    val canonicalDir = try { f.canonicalFile } catch (e: Exception) { f }
                    if (!scanDirs.any { try { it.canonicalPath == canonicalDir.canonicalPath } catch (e: Exception) { it.absolutePath == f.absolutePath } }) {
                        scanDirs.add(canonicalDir)
                    }
                }
            }

            for (dir in scanDirs) {
                if (dir.exists() && dir.isDirectory) {
                    scanDirectory(dir, tracks, seenPaths, seenNames)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error scanning storage directories: ${e.message}", e)
        }

        // Distinct strictly by file name (case-insensitive) to prevent duplicates, then sort naturally
        tracks
            .distinctBy { it.fileName.lowercase() }
            .sortedWith(
                compareBy<AudioTrack> { extractNumber(it.fileName) }
                    .thenBy { it.title }
            )
    }

    private fun scanDirectory(
        dir: File,
        tracks: MutableList<AudioTrack>,
        seenPaths: MutableSet<String>,
        seenNames: MutableSet<String>
    ) {
        try {
            val files = dir.listFiles() ?: return
            for (file in files) {
                val fileName = file.name
                val isHiddenOrTemp = fileName.startsWith(".") || fileName.startsWith("._")

                if (file.isDirectory && !isHiddenOrTemp) {
                    // Search sub-folders inside Download folder
                    scanDirectory(file, tracks, seenPaths, seenNames)
                } else if (file.isFile && !isHiddenOrTemp && file.length() > 0) {
                    val ext = file.extension.lowercase()
                    val canonicalPath = try { file.canonicalPath } catch (e: Exception) { file.absolutePath }
                    val lowerName = fileName.lowercase()

                    if (AUDIO_EXTENSIONS.contains(ext) &&
                        seenPaths.add(canonicalPath) &&
                        seenNames.add(lowerName)
                    ) {
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

        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(file.absolutePath)
            durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        } catch (e: Exception) {
            Log.w(TAG, "Duration extraction failed for ${file.name}: ${e.message}")
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
                // Ignore release error
            }
        }

        // Read real file name from storage with extension removed
        val realFileName = file.nameWithoutExtension.ifBlank { file.name }
        val ext = file.extension.uppercase()
        val formattedSize = AudioTrack.formatSizeBytes(file.length())
        val parentFolder = file.parentFile?.name
        val reciterName = ReciterCategorizer.extractReciterName(file.name)

        val subtitle = if (reciterName.isNotBlank() && reciterName != "أخرى") {
            if (formattedSize.isNotBlank()) "$reciterName • $ext • $formattedSize" else "$reciterName • $ext"
        } else if (!parentFolder.isNullOrBlank() && parentFolder !in listOf("Download", "Music", "Movies", "Documents", "Quran", "sdcard", "0")) {
            "مجلد: $parentFolder • $ext"
        } else if (formattedSize.isNotBlank()) {
            "ملف $ext • $formattedSize"
        } else {
            "ملف $ext"
        }

        return AudioTrack(
            id = "file_${file.absolutePath.hashCode()}",
            title = realFileName,
            surahNameArabic = realFileName,
            reciterOrSubtitle = subtitle,
            fileName = file.name,
            filePath = file.absolutePath,
            uri = Uri.fromFile(file),
            durationMs = durationMs,
            sizeBytes = file.length(),
            isSample = false,
            reciterName = reciterName
        )
    }

    private fun extractNumber(str: String): Int {
        val numStr = str.filter { it.isDigit() }
        return numStr.toIntOrNull() ?: Int.MAX_VALUE
    }
}

