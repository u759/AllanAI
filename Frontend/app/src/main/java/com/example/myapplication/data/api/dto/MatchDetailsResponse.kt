package com.example.myapplication.data.api.dto

/**
 * Response from GET /api/matches/{id}
 * Complete match details with statistics, shots, events, and highlights
 */
data class MatchDetailsResponse(
    val id: String,
    val createdAt: String,
    val processedAt: String?,
    val status: String,
    val durationSeconds: Int?,
    val originalFilename: String?,
    val statistics: MatchStatisticsResponse?,
    val shots: List<ShotResponse>,
    val events: List<EventResponse>,
    val highlights: HighlightsResponse?,
    val processingSummary: ProcessingSummaryResponse?
)

data class MatchStatisticsResponse(
    val player1Score: Int,
    val player2Score: Int,
    val totalRallies: Int,
    val avgRallyLength: Double,
    val maxBallSpeed: Double,
    val avgBallSpeed: Double
)

data class ShotResponse(
    val timestampMs: Long,
    val timestampSeries: List<Long>,
    val frameSeries: List<Int>,
    val player: Int,
    val shotType: String,
    val speed: Double,
    val accuracy: Double,
    val result: String,
    val detections: List<DetectionResponse>
)

data class EventResponse(
    val id: String,
    val timestampMs: Long,
    val timestampSeries: List<Long>,
    val frameSeries: List<Int>,
    val type: String,
    val title: String,
    val description: String,
    val player: Int?,
    val importance: Int,
    val metadata: EventMetadataResponse?
)

data class EventMetadataResponse(
    val shotSpeed: Double?,
    val rallyLength: Int?,
    val shotType: String?,
    val frameNumber: Int?,
    val eventWindow: EventWindowResponse?,
    val ballTrajectory: List<List<Double>>?,
    val scoreAfter: ScoreStateResponse?,
    val confidence: Double?,
    val source: String?,
    val detections: List<DetectionResponse>?
)

data class EventWindowResponse(
    val preMs: Int,
    val postMs: Int
)

data class ScoreStateResponse(
    val player1: Int,
    val player2: Int
)

data class DetectionResponse(
    val frameNumber: Int,
    val x: Double?,
    val y: Double?,
    val width: Double?,
    val height: Double?,
    val confidence: Double
)

data class HighlightReference(
    val eventId: String,
    val timestampMs: Long,
    val timestampSeries: List<Long>
)

data class HighlightsResponse(
    val playOfTheGame: HighlightReference?,
    val topRallies: List<HighlightReference>,
    val fastestShots: List<HighlightReference>,
    val bestServes: List<HighlightReference>
)

data class ProcessingSummaryResponse(
    val primarySource: String?,
    val sources: List<String>,
    val notes: List<String>
)
