package com.example.myapplication.data.api.dto

/**
 * Response from GET /api/matches
 * Includes all fields that may be returned in a summary response
 */
data class MatchSummaryResponse(
    val id: String,
    val userId: String?,
    val createdAt: String,
    val processedAt: String?,
    val status: String,
    val durationSeconds: Int?,
    val videoPath: String? = null,
    val originalFilename: String?,
    val player1Name: String?,
    val player2Name: String?,
    val matchTitle: String?,
    val thumbnailPath: String? = null,
    val statistics: MatchStatisticsResponse? = null,
    val shots: List<ShotResponse>? = null,
    val events: List<EventResponse>? = null,
    val highlights: HighlightsResponse? = null
)
