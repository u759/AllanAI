package com.example.myapplication.util

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.core.net.toUri
import java.io.File
import java.io.FileOutputStream

/**
 * Utility for generating thumbnails from highlight video clips.
 * 
 * Extracts the first frame of each highlight (at startMs timestamp) to use as a preview image.
 * Follows the AllanAI architecture where highlights have precise timestamp boundaries.
 */
object HighlightThumbnailGenerator {
    
    private const val THUMBNAIL_DIR = "highlight_thumbnails"
    private const val THUMBNAIL_WIDTH = 640
    private const val THUMBNAIL_HEIGHT = 360
    
    /**
     * Generate a thumbnail for a highlight by extracting the frame at startMs.
     * 
     * @param context Android context
     * @param videoResourceId Resource ID of the source video (e.g., R.raw.test_2)
     * @param timestampMs Timestamp in milliseconds where the highlight starts
     * @param highlightId Unique ID for caching the thumbnail
     * @return File path to the generated thumbnail, or null if generation failed
     */
    fun generateThumbnail(
        context: Context,
        videoResourceId: Int,
        timestampMs: Long,
        highlightId: String
    ): String? {
        return try {
            // Check cache first
            val cachedFile = getCachedThumbnail(context, highlightId)
            if (cachedFile != null && cachedFile.exists()) {
                return cachedFile.absolutePath
            }
            
            // Extract frame from video
            val videoUri = "android.resource://${context.packageName}/$videoResourceId".toUri()
            val bitmap = extractFrame(context, videoUri, timestampMs)
            
            if (bitmap != null) {
                // Save to cache
                val file = saveThumbnailToCache(context, bitmap, highlightId)
                bitmap.recycle()
                file?.absolutePath
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Extract a frame from video at the specified timestamp.
     */
    private fun extractFrame(
        context: Context,
        videoUri: Uri,
        timestampMs: Long
    ): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, videoUri)
            
            // Extract frame at timestamp (convert to microseconds)
            val timestampMicros = timestampMs * 1000
            val bitmap = retriever.getFrameAtTime(
                timestampMicros,
                MediaMetadataRetriever.OPTION_CLOSEST_SYNC
            )
            
            // Scale to thumbnail size
            bitmap?.let { scaleBitmap(it) }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Scale bitmap to thumbnail dimensions.
     */
    private fun scaleBitmap(bitmap: Bitmap): Bitmap {
        if (bitmap.width <= THUMBNAIL_WIDTH && bitmap.height <= THUMBNAIL_HEIGHT) {
            return bitmap
        }
        
        val scale = minOf(
            THUMBNAIL_WIDTH.toFloat() / bitmap.width,
            THUMBNAIL_HEIGHT.toFloat() / bitmap.height
        )
        
        val scaledWidth = (bitmap.width * scale).toInt()
        val scaledHeight = (bitmap.height * scale).toInt()
        
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true)
        
        // Recycle original if we created a new bitmap
        if (scaledBitmap != bitmap) {
            bitmap.recycle()
        }
        
        return scaledBitmap
    }
    
    /**
     * Save thumbnail to cache directory.
     */
    private fun saveThumbnailToCache(
        context: Context,
        bitmap: Bitmap,
        highlightId: String
    ): File? {
        return try {
            val thumbnailDir = File(context.cacheDir, THUMBNAIL_DIR)
            if (!thumbnailDir.exists()) {
                thumbnailDir.mkdirs()
            }
            
            val file = File(thumbnailDir, "$highlightId.jpg")
            val outputStream = FileOutputStream(file)
            
            // Save as JPEG with good quality
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
            outputStream.flush()
            outputStream.close()
            
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Get cached thumbnail file if it exists.
     */
    private fun getCachedThumbnail(context: Context, highlightId: String): File? {
        val thumbnailDir = File(context.cacheDir, THUMBNAIL_DIR)
        val file = File(thumbnailDir, "$highlightId.jpg")
        return if (file.exists()) file else null
    }
    
    /**
     * Clear all cached thumbnails.
     */
    fun clearCache(context: Context) {
        try {
            val thumbnailDir = File(context.cacheDir, THUMBNAIL_DIR)
            if (thumbnailDir.exists()) {
                thumbnailDir.deleteRecursively()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Pre-generate thumbnails for multiple highlights in batch.
     * Useful for loading all thumbnails when entering the highlights screen.
     */
    suspend fun preGenerateThumbnails(
        context: Context,
        videoResourceId: Int,
        highlights: List<Pair<String, Long>> // (highlightId, startTimestampMs)
    ) {
        highlights.forEach { (id, timestamp) ->
            generateThumbnail(context, videoResourceId, timestamp, id)
        }
    }
}
