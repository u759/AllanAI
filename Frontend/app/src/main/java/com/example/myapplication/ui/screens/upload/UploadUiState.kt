package com.example.myapplication.ui.screens.upload

import com.example.myapplication.data.model.Match

/**
 * UI state for the upload screen.
 *
 * Represents the different states during the video upload process.
 */
sealed class UploadUiState {
    /**
     * Initial state - ready to select or record video
     */
    data object Idle : UploadUiState()

    /**
     * Processing the selected video file
     */
    data object ProcessingVideo : UploadUiState()

    /**
     * Waiting for user to input metadata (player names, match title)
     */
    data object WaitingForMetadata : UploadUiState()

    /**
     * Uploading video to backend
     * @param progress Upload progress from 0.0 to 1.0
     */
    data class Uploading(val progress: Float) : UploadUiState()

    /**
     * Upload completed successfully
     * @param match The created match
     */
    data class Success(val match: Match) : UploadUiState()

    /**
     * Upload failed
     * @param message Error message to display
     */
    data class Error(val message: String) : UploadUiState()
}
