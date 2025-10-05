package com.example.myapplication.ui.screens.upload

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.repository.AuthRepository
import com.example.myapplication.data.repository.MatchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject

/**
 * ViewModel for the upload screen.
 *
 * Manages the state of video upload including file selection, metadata input,
 * and upload progress. Supports both file selection and video recording flows.
 */
@HiltViewModel
class UploadViewModel @Inject constructor(
    private val matchRepository: MatchRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UploadUiState>(UploadUiState.Idle)
    val uiState: StateFlow<UploadUiState> = _uiState.asStateFlow()

    private val _showMetadataDialog = MutableStateFlow(false)
    val showMetadataDialog: StateFlow<Boolean> = _showMetadataDialog.asStateFlow()

    // Temporarily store the video file while waiting for metadata
    private var pendingVideoFile: File? = null
    private var pendingFilename: String? = null

    /**
     * Called when user selects a video file from device storage
     */
    fun onVideoSelected(uri: Uri, context: Context) {
        viewModelScope.launch {
            try {
                _uiState.value = UploadUiState.ProcessingVideo

                // Copy URI content to a temporary file
                val tempFile = createTempVideoFile(context)
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(tempFile).use { output ->
                        input.copyTo(output)
                    }
                }

                // Extract filename from URI
                val filename = getFilenameFromUri(uri, context) ?: "video_${System.currentTimeMillis()}.mp4"

                // Store file and show metadata dialog
                pendingVideoFile = tempFile
                pendingFilename = filename
                _showMetadataDialog.value = true
                _uiState.value = UploadUiState.WaitingForMetadata

            } catch (e: Exception) {
                Log.e(TAG, "Error processing video", e)
                _uiState.value = UploadUiState.Error("Failed to process video: ${e.message}")
            }
        }
    }

    /**
     * Called when user records a video
     */
    fun onVideoRecorded(file: File) {
        viewModelScope.launch {
            try {
                _uiState.value = UploadUiState.ProcessingVideo

                // Store file and show metadata dialog
                pendingVideoFile = file
                pendingFilename = "recorded_${System.currentTimeMillis()}.mp4"
                _showMetadataDialog.value = true
                _uiState.value = UploadUiState.WaitingForMetadata

            } catch (e: Exception) {
                Log.e(TAG, "Error processing recorded video", e)
                _uiState.value = UploadUiState.Error("Failed to process recorded video: ${e.message}")
            }
        }
    }

    /**
     * Upload video with metadata to backend
     */
    fun uploadVideo(player1: String, player2: String, title: String, context: Context) {
        viewModelScope.launch {
            try {
                // Dismiss metadata dialog
                _showMetadataDialog.value = false

                // Validate we have a file
                val videoFile = pendingVideoFile
                val filename = pendingFilename

                if (videoFile == null || filename == null) {
                    _uiState.value = UploadUiState.Error("No video file selected")
                    return@launch
                }

                // Check user is logged in
                if (!authRepository.isLoggedIn()) {
                    _uiState.value = UploadUiState.Error("You must be logged in to upload videos")
                    return@launch
                }

                // Generate random values if fields are empty
                val finalPlayer1 = player1.ifBlank { "Player_${generateRandomId()}" }
                val finalPlayer2 = player2.ifBlank { "Player_${generateRandomId()}" }
                val finalTitle = title.ifBlank { "Match_${generateRandomId()}" }

                // Start upload
                _uiState.value = UploadUiState.Uploading(0.0f)

                // Upload to repository
                // Note: Real progress tracking would require modifying the repository
                // For now, we'll simulate progress
                val result = matchRepository.uploadMatch(
                    videoFile = videoFile,
                    filename = filename,
                    player1Name = finalPlayer1,
                    player2Name = finalPlayer2,
                    matchTitle = finalTitle
                )

                result.fold(
                    onSuccess = { match ->
                        _uiState.value = UploadUiState.Success(match)
                        // Clean up temp file
                        cleanupTempFile()
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Upload failed", error)
                        _uiState.value = UploadUiState.Error(
                            error.message ?: "Upload failed"
                        )
                        // Clean up temp file
                        cleanupTempFile()
                    }
                )

            } catch (e: Exception) {
                Log.e(TAG, "Error during upload", e)
                _uiState.value = UploadUiState.Error("Upload failed: ${e.message}")
                cleanupTempFile()
            }
        }
    }

    /**
     * Cancel metadata dialog without uploading
     */
    fun cancelMetadataDialog() {
        _showMetadataDialog.value = false
        _uiState.value = UploadUiState.Idle
        cleanupTempFile()
    }

    /**
     * Reset state to idle (after successful upload or error)
     */
    fun resetState() {
        _uiState.value = UploadUiState.Idle
        _showMetadataDialog.value = false
        cleanupTempFile()
    }

    // Helper methods

    private fun createTempVideoFile(context: Context): File {
        val tempDir = context.cacheDir
        return File.createTempFile("upload_", ".mp4", tempDir)
    }

    private fun getFilenameFromUri(uri: Uri, context: Context): String? {
        var filename: String? = null
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && cursor.moveToFirst()) {
                filename = cursor.getString(nameIndex)
            }
        }
        return filename
    }

    private fun generateRandomId(): String {
        return UUID.randomUUID().toString().take(8)
    }

    private fun cleanupTempFile() {
        try {
            pendingVideoFile?.delete()
            pendingVideoFile = null
            pendingFilename = null
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up temp file", e)
        }
    }

    override fun onCleared() {
        super.onCleared()
        cleanupTempFile()
    }

    companion object {
        private const val TAG = "UploadViewModel"
    }
}
