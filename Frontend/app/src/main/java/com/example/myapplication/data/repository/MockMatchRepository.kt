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
@RequiresApi(Build.VERSION_CODES.O)
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

    @RequiresApi(Build.VERSION_CODES.O)
    private fun generateSampleMatches() {
        // Sample match 1 - Completed (based on AIGuidelines data)
        val match1 = Match(
            id = "0ee996ad-3f02-4ba9-bafd-465a6a6dcf10",
            createdAt = Instant.now().minusSeconds(7200), // 2 hours ago
            processedAt = Instant.now().minusSeconds(7000),
            status = MatchStatus.COMPLETE,
            durationSeconds = 30,
            originalFilename = "test_2.mp4",
            videoPath = "/videos/match1.mp4",
            statistics = MatchStatistics(
                player1Score = 2,
                player2Score = 0,
                totalRallies = 1,
                avgRallyLength = 2.5,
                maxBallSpeed = 117.0,
                avgBallSpeed = 79.3
            ),
            shots = generateSampleShots(),
            events = generateSampleEvents(),
            highlights = Highlights(
                playOfTheGame = "d3f1aec3-20f8-4511-85f6-3b71263a6495",
                topRallies = emptyList(),
                fastestShots = listOf(
                    "d3f1aec3-20f8-4511-85f6-3b71263a6495",
                    "4c99b9af-dac8-4c3b-8445-1b83f4297070",
                    "1e61435d-2308-4958-8feb-4979dd7416c9"
                ),
                bestServes = emptyList()
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
            durationSeconds = 30,
            originalFilename = "match_yesterday.mp4",
            videoPath = "/videos/match3.mp4",
            statistics = MatchStatistics(
                player1Score = 2,
                player2Score = 0,
                totalRallies = 1,
                avgRallyLength = 2.5,
                maxBallSpeed = 117.0,
                avgBallSpeed = 79.3
            ),
            shots = generateSampleShots(),
            events = generateSampleEvents(),
            highlights = Highlights(
                playOfTheGame = "d3f1aec3-20f8-4511-85f6-3b71263a6495",
                topRallies = emptyList(),
                fastestShots = listOf(
                    "d3f1aec3-20f8-4511-85f6-3b71263a6495",
                    "4c99b9af-dac8-4c3b-8445-1b83f4297070"
                ),
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
            player1Score = 2,
            player2Score = 0,
            totalRallies = 1,
            avgRallyLength = 2.5,
            maxBallSpeed = 117.0,
            avgBallSpeed = 79.3
        )
    }

    private fun generateSampleShots(): List<Shot> {
        return listOf(
            Shot(
                timestampMs = 0,
                player = 1,
                shotType = ShotType.FOREHAND,
                speed = 30.0,
                accuracy = 85.0,
                result = ShotResult.IN,
                detections = listOf(
                    Detection(
                        frameNumber = 0,
                        x = 907.28,
                        y = 436.77,
                        width = 19.12,
                        height = 21.29,
                        confidence = 0.86
                    )
                )
            ),
            Shot(
                timestampMs = 8,
                player = 1,
                shotType = ShotType.FOREHAND,
                speed = 30.0,
                accuracy = 85.0,
                result = ShotResult.IN,
                detections = listOf(
                    Detection(
                        frameNumber = 1,
                        x = 890.10,
                        y = 441.90,
                        width = 19.22,
                        height = 21.51,
                        confidence = 0.88
                    )
                )
            ),
            Shot(
                timestampMs = 17,
                player = 1,
                shotType = ShotType.FOREHAND,
                speed = 30.0,
                accuracy = 85.0,
                result = ShotResult.IN,
                detections = listOf(
                    Detection(
                        frameNumber = 2,
                        x = 874.37,
                        y = 447.91,
                        width = 19.30,
                        height = 21.58,
                        confidence = 0.88
                    )
                )
            )
        )
    }

    private fun generateSampleEvents(): List<Event> {
        return listOf(
            Event(
                id = "56897489-dd37-4c0d-b811-5fde4c4845a4",
                timestampMs = 508,
                type = EventType.SCORE,
                title = "Point Scored",
                description = "Ball Bounce",
                player = 1,
                importance = 5,
                metadata = EventMetadata(
                    shotSpeed = 52.3,
                    rallyLength = 1,
                    shotType = "SMASH",
                    ballTrajectory = listOf(
                        listOf(569.16, 645.42),
                        listOf(85.38, 653.30)
                    ),
                    frameNumber = 61,
                    scoreAfter = ScoreState(1, 0),
                    eventWindow = EventWindow(preMs = 33, postMs = 100),
                    confidence = 0.84,
                    source = "MODEL",
                    detections = emptyList()
                )
            ),
            Event(
                id = "d3f1aec3-20f8-4511-85f6-3b71263a6495",
                timestampMs = 2708,
                type = EventType.FASTEST_SHOT,
                title = "Fastest Shot",
                description = "Fast Shot",
                player = 1,
                importance = 8,
                metadata = EventMetadata(
                    shotSpeed = 115.2,
                    rallyLength = null,
                    shotType = "FOREHAND",
                    ballTrajectory = listOf(
                        listOf(75.58, 524.37),
                        listOf(1408.18, 495.39)
                    ),
                    frameNumber = 325,
                    scoreAfter = null,
                    eventWindow = EventWindow(preMs = 33, postMs = 100),
                    confidence = 0.40,
                    source = "MODEL",
                    detections = emptyList()
                )
            ),
            Event(
                id = "4c99b9af-dac8-4c3b-8445-1b83f4297070",
                timestampMs = 2717,
                type = EventType.FASTEST_SHOT,
                title = "Fastest Shot",
                description = "Fast Shot",
                player = 1,
                importance = 8,
                metadata = EventMetadata(
                    shotSpeed = 117.0,
                    rallyLength = null,
                    shotType = "FOREHAND",
                    ballTrajectory = listOf(
                        listOf(54.41, 525.12),
                        listOf(1407.97, 495.89)
                    ),
                    frameNumber = 326,
                    scoreAfter = null,
                    eventWindow = EventWindow(preMs = 33, postMs = 100),
                    confidence = 0.56,
                    source = "MODEL",
                    detections = emptyList()
                )
            ),
            Event(
                id = "1e61435d-2308-4958-8feb-4979dd7416c9",
                timestampMs = 2758,
                type = EventType.FASTEST_SHOT,
                title = "Fastest Shot",
                description = "Fast Shot",
                player = 2,
                importance = 8,
                metadata = EventMetadata(
                    shotSpeed = 87.9,
                    rallyLength = null,
                    shotType = "FOREHAND",
                    ballTrajectory = listOf(
                        listOf(1408.18, 495.39),
                        listOf(390.47, 482.88)
                    ),
                    frameNumber = 331,
                    scoreAfter = null,
                    eventWindow = EventWindow(preMs = 33, postMs = 100),
                    confidence = 0.79,
                    source = "MODEL",
                    detections = emptyList()
                )
            )
        )
    }

    private fun generateSampleHighlights(): Highlights {
        return Highlights(
            playOfTheGame = "d3f1aec3-20f8-4511-85f6-3b71263a6495",
            topRallies = emptyList(),
            fastestShots = listOf(
                "d3f1aec3-20f8-4511-85f6-3b71263a6495",
                "4c99b9af-dac8-4c3b-8445-1b83f4297070",
                "1e61435d-2308-4958-8feb-4979dd7416c9"
            ),
            bestServes = emptyList()
        )
    }
}
