package com.example.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.os.Build
import android.util.Log
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Smart Embedded Album Art & Frame Extractor for Audio & Video Files.
 * 
 * Optimized for low-memory (1GB RAM) Android TV devices:
 * 1. Automatic downsampling (max 280px) to prevent OutOfMemory / UI stuttering.
 * 2. Uses RGB_565 configuration to reduce memory usage by 50%.
 * 3. LRU Memory cache (max 15 bitmaps) to avoid repeated disk reading on track switching.
 * 4. Supports all audio formats (MP3, WAV, M4A, AAC, FLAC, OGG) & video formats (MP4, MKV, WEBM, 3GP).
 */
object ArtworkExtractor {
    private const val TAG = "ArtworkExtractor"

    // Max 15 entries cache to maintain ultra-low RAM footprint on TV
    private val bitmapCache = LruCache<String, Bitmap>(15)

    suspend fun getArtwork(filePath: String, maxDimension: Int = 280): Bitmap? = withContext(Dispatchers.IO) {
        if (filePath.isBlank()) return@withContext null

        // Return cached bitmap if available
        bitmapCache.get(filePath)?.let { return@withContext it }

        var bitmap: Bitmap? = null
        val retriever = MediaMetadataRetriever()

        try {
            retriever.setDataSource(filePath)

            // 1. Try embedded picture (Audio MP3, M4A, FLAC, AAC, OGG album art)
            val embeddedPicture = retriever.embeddedPicture
            if (embeddedPicture != null && embeddedPicture.isNotEmpty()) {
                val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(embeddedPicture, 0, embeddedPicture.size, boundsOptions)

                var inSampleSize = 1
                while (boundsOptions.outWidth / inSampleSize > maxDimension || boundsOptions.outHeight / inSampleSize > maxDimension) {
                    inSampleSize *= 2
                }

                val decodeOptions = BitmapFactory.Options().apply {
                    this.inSampleSize = inSampleSize
                    inPreferredConfig = Bitmap.Config.RGB_565 // Half memory footprint compared to ARGB_8888
                }

                bitmap = BitmapFactory.decodeByteArray(embeddedPicture, 0, embeddedPicture.size, decodeOptions)
            }

            // 2. Try video frame extraction (MP4, MKV, WEBM, 3GP, M4V video thumbnails)
            if (bitmap == null) {
                try {
                    val frame = if (Build.VERSION.SDK_INT >= 28) {
                        retriever.getScaledFrameAtTime(-1, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, maxDimension, maxDimension)
                    } else {
                        retriever.getFrameAtTime()
                    }

                    if (frame != null) {
                        if (frame.width > maxDimension || frame.height > maxDimension) {
                            val scale = maxDimension.toFloat() / maxOf(frame.width, frame.height)
                            val scaledW = (frame.width * scale).toInt().coerceAtLeast(1)
                            val scaledH = (frame.height * scale).toInt().coerceAtLeast(1)
                            val scaledBitmap = Bitmap.createScaledBitmap(frame, scaledW, scaledH, true)
                            if (scaledBitmap != frame) {
                                frame.recycle()
                            }
                            bitmap = scaledBitmap
                        } else {
                            bitmap = frame
                        }
                    }
                } catch (e: Exception) {
                    // Frame extraction not applicable or failed
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error extracting artwork for $filePath: ${e.message}")
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
                // Ignore release error
            }
        }

        if (bitmap != null) {
            bitmapCache.put(filePath, bitmap)
        }

        return@withContext bitmap
    }
}
