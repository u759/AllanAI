package com.example.myapplication.data.api.dto

/**
 * Response from POST /api/matches/upload
 */
data class MatchUploadResponse(
    val matchId: String,
    val status: String
)
