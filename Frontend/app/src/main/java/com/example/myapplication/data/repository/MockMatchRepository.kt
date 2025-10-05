package com.example.myapplication.data.repository

import android.os.Build
import androidx.annotation.RequiresApi
import com.example.myapplication.data.model.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.io.File
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mock implementation of MatchRepository for testing and development.
 *
 * This implementation stores data in memory using thread-safe collections,
 * making it ideal for:
 * - UI development without backend
 * - Unit testing ViewModels
 * - Preview/demo mode
 * - Offline development
 *
 * The mock simulates realistic delays and generates sample data that matches
 * the expected schema from the backend API.
 *
 * All data is stored in memory and will be lost when the app restarts.
 *
 * This class is marked as @Singleton so Hilt provides a single instance
 * throughout the app lifecycle.
 */
@Singleton
class MockMatchRepository @Inject constructor() : MatchRepository {

    // Thread-safe storage
    private val matches = ConcurrentHashMap<String, Match>()
    private val matchesFlow = MutableStateFlow<List<Match>>(emptyList())

    // Simulate network delay
    private val networkDelayMs = 500L

    init {
        // Initialize with sample data
        generateSampleMatches()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override suspend fun uploadMatch(videoFile: File, filename: String): Result<Match> {
        delay(networkDelayMs)

        val match = Match(
            id = UUID.randomUUID().toString(),
            createdAt = Instant.now(),
            status = MatchStatus.UPLOADED,
            originalFilename = filename,
            videoPath = "/videos/${UUID.randomUUID()}.mp4"
        )

        matches[match.id] = match
        updateFlow()

        // Simulate async processing
        simulateProcessing(match.id)

        return Result.success(match)
    }

    override fun getAllMatches(): Flow<List<Match>> {
        return matchesFlow
    }

    override fun getMatchById(matchId: String): Flow<Match?> {
        return matchesFlow.map { matches ->
            matches.find { it.id == matchId }
        }
    }

    override fun getMatchesByStatus(status: MatchStatus): Flow<List<Match>> {
        return matchesFlow.map { matches ->
            matches.filter { it.status == status }
        }
    }

    override suspend fun getMatchStatistics(matchId: String): Result<Match> {
        delay(networkDelayMs)

        val match = matches[matchId]
            ?: return Result.failure(Exception("Match not found: $matchId"))

        return Result.success(match)
    }

    override suspend fun getMatchEvents(matchId: String): Result<List<Event>> {
        delay(networkDelayMs)

        val match = matches[matchId]
            ?: return Result.failure(Exception("Match not found: $matchId"))

        return Result.success(match.events)
    }

    override suspend fun getEventById(matchId: String, eventId: String): Result<Event> {
        delay(networkDelayMs)

        val match = matches[matchId]
            ?: return Result.failure(Exception("Match not found: $matchId"))

        val event = match.events.find { it.id == eventId }
            ?: return Result.failure(Exception("Event not found: $eventId"))

        return Result.success(event)
    }

    override suspend fun getMatchHighlights(matchId: String): Result<Highlights> {
        delay(networkDelayMs)

        val match = matches[matchId]
            ?: return Result.failure(Exception("Match not found: $matchId"))

        val highlights = match.highlights
            ?: return Result.failure(Exception("Highlights not available"))

        return Result.success(highlights)
    }

    override suspend fun getVideoUrl(matchId: String): Result<String> {
        delay(networkDelayMs)

        val match = matches[matchId]
            ?: return Result.failure(Exception("Match not found: $matchId"))

        val videoUrl = "http://localhost:8080/api/matches/$matchId/video"
        return Result.success(videoUrl)
    }

    override suspend fun deleteMatch(matchId: String): Result<Unit> {
        delay(networkDelayMs)

        matches.remove(matchId)
        updateFlow()

        return Result.success(Unit)
    }

    override suspend fun refreshMatch(matchId: String): Result<Match> {
        delay(networkDelayMs)

        val match = matches[matchId]
            ?: return Result.failure(Exception("Match not found: $matchId"))

        return Result.success(match)
    }

    override suspend fun getMatchStatus(matchId: String): Result<MatchStatus> {
        delay(networkDelayMs)

        val match = matches[matchId]
            ?: return Result.failure(Exception("Match not found: $matchId"))

        return Result.success(match.status)
    }

    // Utility methods for testing
    fun clearAllMatches() {
        matches.clear()
        updateFlow()
    }

    fun addMatch(match: Match) {
        matches[match.id] = match
        updateFlow()
    }

    private fun updateFlow() {
        matchesFlow.value = matches.values.sortedByDescending { it.createdAt }
    }

    private fun simulateProcessing(matchId: String) {
        // Simulate async processing in background
        Thread {
            Thread.sleep(3000) // 3 seconds processing time

            val match = matches[matchId] ?: return@Thread
            val processedMatch = match.copy(
                status = MatchStatus.COMPLETE,
                processedAt = Instant.now(),
                durationSeconds = 180,
                statistics = generateSampleStatistics(),
                shots = generateSampleShots(),
                events = generateSampleEvents(),
                highlights = generateSampleHighlights()
            )

            matches[matchId] = processedMatch
            updateFlow()
        }.start()
    }

    private fun generateSampleMatches() {
        // Sample match 1 - Completed
        val match1 = Match(
            id = "1",
            createdAt = Instant.now().minusSeconds(7200), // 2 hours ago
            processedAt = Instant.now().minusSeconds(7000),
            status = MatchStatus.COMPLETE,
            durationSeconds = 300,
            originalFilename = "match_2024_10_04_morning.mp4",
            videoPath = "/videos/match1.mp4",
            statistics = MatchStatistics(
                player1Score = 11,
                player2Score = 8,
                totalRallies = 45,
                avgRallyLength = 6.5,
                maxBallSpeed = 52.3,
                avgBallSpeed = 32.8
            ),
            shots = generateSampleShots(),
            events = generateSampleEvents(),
            highlights = Highlights(
                playOfTheGame = "event-1",
                topRallies = listOf("event-1", "event-3"),
                fastestShots = listOf("event-2"),
                bestServes = listOf("event-4")
            )
        )

        // Sample match 2 - Processing
        val match2 = Match(
            id = "2",
            createdAt = Instant.now().minusSeconds(300), // 5 minutes ago
            status = MatchStatus.PROCESSING,
            originalFilename = "match_current.mp4",
            videoPath = "/videos/match2.mp4"
        )

        // Sample match 3 - Completed
        val match3 = Match(
            id = "3",
            createdAt = Instant.now().minusSeconds(86400), // 1 day ago
            processedAt = Instant.now().minusSeconds(86200),
            status = MatchStatus.COMPLETE,
            durationSeconds = 420,
            originalFilename = "match_yesterday.mp4",
            videoPath = "/videos/match3.mp4",
            statistics = MatchStatistics(
                player1Score = 9,
                player2Score = 11,
                totalRallies = 52,
                avgRallyLength = 7.2,
                maxBallSpeed = 48.9,
                avgBallSpeed = 30.5
            ),
            shots = generateSampleShots(),
            events = generateSampleEvents(),
            highlights = Highlights(
                playOfTheGame = "event-1",
                topRallies = listOf("event-1"),
                fastestShots = listOf("event-2"),
                bestServes = emptyList()
            )
        )

        matches[match1.id] = match1
        matches[match2.id] = match2
        matches[match3.id] = match3
        updateFlow()
    }

    private fun generateSampleStatistics(): MatchStatistics {
        return MatchStatistics(
            player1Score = 11,
            player2Score = 7,
            totalRallies = 40,
            avgRallyLength = 6.2,
            maxBallSpeed = 50.5,
            avgBallSpeed = 31.2
        )
    }

    private fun generateSampleShots(): List<Shot> {
        return listOf(
            Shot(
                timestampMs = 1500,
                player = 1,
                shotType = ShotType.SERVE,
                speed = 35.2,
                accuracy = 0.95,
                result = ShotResult.IN
            ),
            Shot(
                timestampMs = 2800,
                player = 2,
                shotType = ShotType.FOREHAND,
                speed = 42.1,
                accuracy = 0.88,
                result = ShotResult.IN
            ),
            Shot(
                timestampMs = 4200,
                player = 1,
                shotType = ShotType.BACKHAND,
                speed = 38.7,
                accuracy = 0.72,
                result = ShotResult.OUT
            )
        )
    }

    private fun generateSampleEvents(): List<Event> {
        return listOf(
            Event(
                id = "event-1",
                timestampMs = 83500,
                type = EventType.PLAY_OF_THE_GAME,
                title = "Play of the Game",
                description = "Epic 15-shot rally with incredible speed",
                player = null,
                importance = 10,
                metadata = EventMetadata(
                    rallyLength = 15,
                    shotSpeed = 45.2,
                    frameNumber = 2505
                )
            ),
            Event(
                id = "event-2",
                timestampMs = 15200,
                type = EventType.FASTEST_SHOT,
                title = "Fastest Shot",
                description = "Player 1 forehand smash",
                player = 1,
                importance = 8,
                metadata = EventMetadata(
                    shotSpeed = 52.3,
                    shotType = "FOREHAND",
                    frameNumber = 456
                )
            ),
            Event(
                id = "event-3",
                timestampMs = 45000,
                type = EventType.RALLY_HIGHLIGHT,
                title = "Long Rally",
                description = "Intense 12-shot exchange",
                player = null,
                importance = 7,
                metadata = EventMetadata(
                    rallyLength = 12,
                    frameNumber = 1350
                )
            ),
            Event(
                id = "event-4",
                timestampMs = 92000,
                type = EventType.SCORE,
                title = "Point Scored",
                description = "Player 2 scores on backhand",
                player = 2,
                importance = 5,
                metadata = EventMetadata(
                    shotType = "BACKHAND",
                    scoreAfter = ScoreState(8, 11),
                    frameNumber = 2760
                )
            )
        )
    }

    private fun generateSampleHighlights(): Highlights {
        return Highlights(
            playOfTheGame = "event-1",
            topRallies = listOf("event-1", "event-3"),
            fastestShots = listOf("event-2"),
            bestServes = emptyList()
        )
    }
}
