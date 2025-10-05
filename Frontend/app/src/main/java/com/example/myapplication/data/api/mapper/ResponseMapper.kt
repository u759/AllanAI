package com.example.myapplication.data.api.mapper

import com.example.myapplication.data.api.dto.DetectionResponse
import com.example.myapplication.data.api.dto.EventMetadataResponse
import com.example.myapplication.data.api.dto.EventResponse
import com.example.myapplication.data.api.dto.EventWindowResponse
import com.example.myapplication.data.api.dto.HighlightReference
import com.example.myapplication.data.api.dto.HighlightsResponse
import com.example.myapplication.data.api.dto.MatchDetailsResponse
import com.example.myapplication.data.api.dto.MatchStatisticsResponse
import com.example.myapplication.data.api.dto.MatchSummaryResponse
import com.example.myapplication.data.api.dto.ScoreStateResponse
import com.example.myapplication.data.api.dto.ShotResponse
import com.example.myapplication.data.model.Detection
import com.example.myapplication.data.model.Event
import com.example.myapplication.data.model.EventMetadata
import com.example.myapplication.data.model.EventType
import com.example.myapplication.data.model.EventWindow
import com.example.myapplication.data.model.Highlights
import com.example.myapplication.data.model.Match
import com.example.myapplication.data.model.MatchStatistics
import com.example.myapplication.data.model.MatchStatus
import com.example.myapplication.data.model.ScoreState
import com.example.myapplication.data.model.Shot
import com.example.myapplication.data.model.ShotResult
import com.example.myapplication.data.model.ShotType
import java.time.Instant

/**
 * Mapper functions to convert API response DTOs to domain models.
 * 
 * These extensions follow the architecture guidelines by keeping the
 * data layer separate from the API layer and providing clean domain models
 * for the UI layer to consume.
 */

/**
 * Convert MatchSummaryResponse to Match domain model
 */
fun MatchSummaryResponse.toMatch(): Match {
    return Match(
        id = this.id,
        userId = this.userId,
        createdAt = parseInstant(this.createdAt),
        processedAt = this.processedAt?.let { parseInstant(it) },
        status = parseMatchStatus(this.status),
        durationSeconds = this.durationSeconds,
        originalFilename = this.originalFilename,
        player1Name = this.player1Name,
        player2Name = this.player2Name,
        matchTitle = this.matchTitle
    )
}

/**
 * Convert MatchDetailsResponse to complete Match domain model
 */
fun MatchDetailsResponse.toMatch(): Match {
    return Match(
        id = this.id,
        userId = this.userId,
        createdAt = parseInstant(this.createdAt),
        processedAt = this.processedAt?.let { parseInstant(it) },
        status = parseMatchStatus(this.status),
        durationSeconds = this.durationSeconds,
        originalFilename = this.originalFilename,
        player1Name = this.player1Name,
        player2Name = this.player2Name,
        matchTitle = this.matchTitle,
        statistics = this.statistics?.toMatchStatistics(),
        shots = this.shots.map { it.toShot() },
        events = this.events.map { it.toEvent() },
        highlights = this.highlights?.toHighlights()
    )
}

/**
 * Convert MatchStatisticsResponse to MatchStatistics domain model
 */
fun MatchStatisticsResponse.toMatchStatistics(): MatchStatistics {
    return MatchStatistics(
        player1Score = this.player1Score,
        player2Score = this.player2Score,
        totalRallies = this.totalRallies,
        avgRallyLength = this.avgRallyLength,
        maxBallSpeed = this.maxBallSpeed,
        avgBallSpeed = this.avgBallSpeed
    )
}

/**
 * Convert ShotResponse to Shot domain model
 */
fun ShotResponse.toShot(): Shot {
    return Shot(
        timestampMs = this.timestampMs,
        player = this.player,
        shotType = parseShotType(this.shotType),
        speed = this.speed,
        accuracy = this.accuracy,
        result = parseShotResult(this.result),
        detections = this.detections.mapNotNull { it.toDetection() }
    )
}

/**
 * Convert EventResponse to Event domain model
 */
fun EventResponse.toEvent(): Event {
    return Event(
        id = this.id,
        timestampMs = this.timestampMs,
        type = parseEventType(this.type),
        title = this.title,
        description = this.description,
        player = this.player,
        importance = this.importance,
        metadata = this.metadata?.toEventMetadata()
    )
}

/**
 * Convert EventMetadataResponse to EventMetadata domain model
 */
fun EventMetadataResponse.toEventMetadata(): EventMetadata {
    return EventMetadata(
        shotSpeed = this.shotSpeed,
        rallyLength = this.rallyLength,
        shotType = this.shotType,
        ballTrajectory = this.ballTrajectory,
        frameNumber = this.frameNumber,
        scoreAfter = this.scoreAfter?.toScoreState(),
        eventWindow = this.eventWindow?.toEventWindow(),
        confidence = this.confidence,
        source = this.source,
        detections = this.detections?.mapNotNull { it.toDetection() } ?: emptyList()
    )
}

/**
 * Convert EventWindowResponse to EventWindow domain model
 */
fun EventWindowResponse.toEventWindow(): EventWindow {
    return EventWindow(
        preMs = this.preMs,
        postMs = this.postMs
    )
}

/**
 * Convert ScoreStateResponse to ScoreState domain model
 */
fun ScoreStateResponse.toScoreState(): ScoreState {
    return ScoreState(
        player1 = this.player1,
        player2 = this.player2
    )
}

/**
 * Convert DetectionResponse to Detection domain model
 * Returns null if bounding box coordinates are missing
 */
fun DetectionResponse.toDetection(): Detection? {
    val x = this.x ?: return null
    val y = this.y ?: return null
    val width = this.width ?: return null
    val height = this.height ?: return null

    return Detection(
        frameNumber = this.frameNumber,
        x = x,
        y = y,
        width = width,
        height = height,
        confidence = this.confidence
    )
}

/**
 * Convert HighlightsResponse to Highlights domain model
 */
fun HighlightsResponse.toHighlights(): Highlights {
    return Highlights(
        playOfTheGame = this.playOfTheGame?.eventId,
        topRallies = this.topRallies.map { it.eventId },
        fastestShots = this.fastestShots.map { it.eventId },
        bestServes = this.bestServes.map { it.eventId }
    )
}

// Helper functions for parsing enums and types

private fun parseInstant(timestamp: String): Instant {
    return try {
        Instant.parse(timestamp)
    } catch (e: Exception) {
        Instant.now()
    }
}

private fun parseMatchStatus(status: String): MatchStatus {
    return try {
        MatchStatus.valueOf(status)
    } catch (e: IllegalArgumentException) {
        MatchStatus.UPLOADED
    }
}

private fun parseShotType(shotType: String): ShotType {
    return try {
        ShotType.valueOf(shotType)
    } catch (e: IllegalArgumentException) {
        ShotType.FOREHAND
    }
}

private fun parseShotResult(result: String): ShotResult {
    return try {
        ShotResult.valueOf(result)
    } catch (e: IllegalArgumentException) {
        ShotResult.IN
    }
}

private fun parseEventType(type: String): EventType {
    return try {
        EventType.valueOf(type)
    } catch (e: IllegalArgumentException) {
        EventType.SCORE
    }
}
