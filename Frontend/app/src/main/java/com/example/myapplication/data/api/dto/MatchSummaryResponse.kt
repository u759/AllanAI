package com.example.myapplication.data.api.dto

/**
 * Response from GET /api/matches
 */
data class MatchSummaryResponse(
    val id: String,
    val createdAt: String,
    val processedAt: String?,
    val status: String,
    val durationSeconds: Int?,
    val originalFilename: String?
)
