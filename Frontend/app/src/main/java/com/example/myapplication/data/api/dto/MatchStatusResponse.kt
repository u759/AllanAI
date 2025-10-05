package com.example.myapplication.data.api.dto

/**
 * Response from GET /api/matches/{id}/status
 */
data class MatchStatusResponse(
    val id: String,
    val status: String,
    val processedAt: String?
)
