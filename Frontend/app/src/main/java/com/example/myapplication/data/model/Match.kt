package com.example.myapplication.data.model

import java.time.Instant

/**
 * Data model representing a table tennis match.
 *
 * This model follows the backend MatchDocument schema and is used throughout
 * the Android app for displaying match information, statistics, and events.
 */
data class Match(
    val id: String,
    val createdAt: Instant,
    val processedAt: Instant? = null,
    val status: MatchStatus,
    val durationSeconds: Int? = null,
    val videoPath: String? = null,
    val originalFilename: String? = null,
    val statistics: MatchStatistics? = null,
    val shots: List<Shot> = emptyList(),
    val events: List<Event> = emptyList(),
    val highlights: Highlights? = null
)

/**
 * Match processing status.
 */
enum class MatchStatus {
    UPLOADED,
    PROCESSING,
    COMPLETE,
    FAILED
}

/**
 * Match statistics aggregated from video analysis.
 */
data class MatchStatistics(
    val player1Score: Int,
    val player2Score: Int,
    val totalRallies: Int,
    val avgRallyLength: Double,
    val maxBallSpeed: Double,
    val avgBallSpeed: Double
)

/**
 * Individual shot in a match.
 */
data class Shot(
    val timestampMs: Long,
    val player: Int,
    val shotType: ShotType,
    val speed: Double,
    val accuracy: Double,
    val result: ShotResult,
    val detections: List<Detection> = emptyList()
)

/**
 * Type of shot performed.
 */
enum class ShotType {
    SERVE,
    FOREHAND,
    BACKHAND
}

/**
 * Result of a shot.
 */
enum class ShotResult {
    IN,
    OUT,
    NET
}

/**
 * Match event with timestamp for video navigation.
 */
data class Event(
    val id: String,
    val timestampMs: Long,
    val type: EventType,
    val title: String,
    val description: String,
    val player: Int? = null,
    val importance: Int,
    val metadata: EventMetadata? = null
)

/**
 * Type of event detected in the match.
 */
enum class EventType {
    PLAY_OF_THE_GAME,
    SCORE,
    MISS,
    RALLY_HIGHLIGHT,
    SERVE_ACE,
    FASTEST_SHOT
}

/**
 * Additional metadata for events.
 */
data class EventMetadata(
    val shotSpeed: Double? = null,
    val rallyLength: Int? = null,
    val shotType: String? = null,
    val ballTrajectory: List<List<Double>>? = null,
    val frameNumber: Int? = null,
    val scoreAfter: ScoreState? = null,
    val eventWindow: EventWindow? = null,
    val confidence: Double? = null,
    val source: String? = null,
    val detections: List<Detection> = emptyList()
)

/**
 * Event window for video playback context.
 */
data class EventWindow(
    val preMs: Int,
    val postMs: Int
)

/**
 * Score state at a point in time.
 */
data class ScoreState(
    val player1: Int,
    val player2: Int
)

/**
 * Detection bounding box for visual overlays on video.
 */
data class Detection(
    val frameNumber: Int,
    val x: Double,
    val y: Double,
    val width: Double,
    val height: Double,
    val confidence: Double
)

/**
 * Highlights collection for quick navigation.
 */
data class Highlights(
    val playOfTheGame: String? = null,
    val topRallies: List<String> = emptyList(),
    val fastestShots: List<String> = emptyList(),
    val bestServes: List<String> = emptyList()
)
