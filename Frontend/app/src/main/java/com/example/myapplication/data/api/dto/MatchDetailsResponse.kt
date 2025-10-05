package com.example.myapplication.data.api.dto

/**
 * Response from GET /api/matches/{id}
 * Complete match details with statistics, shots, events, and highlights
 */
data class MatchDetailsResponse(
    val id: String,
    val userId: String?,
    val createdAt: String,
    val processedAt: String?,
    val status: String,
    val durationSeconds: Int?,
    val originalFilename: String?,
    val player1Name: String?,
    val player2Name: String?,
    val matchTitle: String?,
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
    val avgBallSpeed: Double,
    val rallyMetrics: RallyMetricsResponse? = null,
    val shotSpeedMetrics: ShotSpeedMetricsResponse? = null,
    val serveMetrics: ServeMetricsResponse? = null,
    val returnMetrics: ReturnMetricsResponse? = null,
    val shotTypeBreakdown: List<ShotTypeAggregateResponse>? = null,
    val playerBreakdown: List<PlayerBreakdownResponse>? = null,
    val momentumTimeline: MomentumTimelineResponse? = null
)

data class RallyMetricsResponse(
    val totalRallies: Int? = null,
    val averageRallyLength: Double? = null,
    val longestRallyLength: Int? = null,
    val averageRallyDurationSeconds: Double? = null,
    val longestRallyDurationSeconds: Double? = null,
    val averageRallyShotSpeed: Double? = null
)

data class ShotSpeedMetricsResponse(
    val fastestShotMph: Double? = null,
    val averageShotMph: Double? = null,
    val averageIncomingShotMph: Double? = null,
    val averageOutgoingShotMph: Double? = null
)

data class ServeMetricsResponse(
    val totalServes: Int? = null,
    val successfulServes: Int? = null,
    val faults: Int? = null,
    val successRate: Double? = null,
    val averageServeSpeed: Double? = null,
    val fastestServeSpeed: Double? = null
)

data class ReturnMetricsResponse(
    val totalReturns: Int? = null,
    val successfulReturns: Int? = null,
    val successRate: Double? = null,
    val averageReturnSpeed: Double? = null
)

data class ShotTypeAggregateResponse(
    val shotType: String,
    val count: Int,
    val averageSpeed: Double? = null,
    val averageAccuracy: Double? = null
)

data class PlayerBreakdownResponse(
    val player: Int,
    val totalShots: Int? = null,
    val totalPointsWon: Int? = null,
    val averageShotSpeed: Double? = null,
    val averageAccuracy: Double? = null,
    val serveSuccessRate: Double? = null,
    val returnSuccessRate: Double? = null
)

data class MomentumTimelineResponse(
    val samples: List<MomentumSampleResponse>? = null
)

data class MomentumSampleResponse(
    val timestampMs: Long,
    val scoringPlayer: Int? = null,
    val scoreAfter: ScoreStateResponse
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
