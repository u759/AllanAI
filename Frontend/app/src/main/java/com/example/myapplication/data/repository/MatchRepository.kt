package com.example.myapplication.data.repository

import com.example.myapplication.data.model.Event
import com.example.myapplication.data.model.EventType
import com.example.myapplication.data.model.Highlights
import com.example.myapplication.data.model.Match
import com.example.myapplication.data.model.MatchStatus
import kotlinx.coroutines.flow.Flow
import java.io.File

/**
 * Repository interface for match data operations.
 *
 * This interface defines the contract for accessing match data, abstracting
 * the underlying data source (API, local database, or mock data). Following
 * the architecture guidelines, this provides a clean separation between the
 * data layer and the UI/ViewModel layer.
 *
 * All methods return Flow or suspend functions to support Kotlin Coroutines
 * and reactive data updates in the UI.
 */
interface MatchRepository {

    /**
     * Uploads a video file to create a new match.
     *
     * The video will be sent to the backend for processing. The backend will
     * analyze the video using OpenCV and generate match statistics and events.
     *
     * @param videoFile the video file to upload
     * @param filename the original filename
     * @param player1Name optional name for player 1
     * @param player2Name optional name for player 2
     * @param matchTitle optional title for the match
     * @return the created match with UPLOADED status
     */
    suspend fun uploadMatch(
        videoFile: File,
        filename: String,
        player1Name: String? = null,
        player2Name: String? = null,
        matchTitle: String? = null
    ): Result<Match>

    /**
     * Retrieves all matches.
     *
     * Returns a Flow that emits the list of matches whenever the data changes.
     * This supports reactive UI updates.
     *
     * @return Flow emitting list of all matches
     */
    fun getAllMatches(): Flow<List<Match>>

    /**
     * Retrieves a specific match by ID.
     *
     * @param matchId the unique match identifier
     * @return Flow emitting the match if found, null otherwise
     */
    fun getMatchById(matchId: String): Flow<Match?>

    /**
     * Retrieves matches with a specific status.
     *
     * Useful for filtering matches by processing state (e.g., show only
     * completed matches, or matches currently being processed).
     *
     * @param status the match status to filter by
     * @return Flow emitting list of matches with the given status
     */
    fun getMatchesByStatus(status: MatchStatus): Flow<List<Match>>

    /**
     * Retrieves detailed statistics for a match.
     *
     * This may fetch additional data from the backend if not already cached.
     *
     * @param matchId the unique match identifier
     * @return the match with full statistics, or null if not found
     */
    suspend fun getMatchStatistics(matchId: String): Result<Match>

    /**
     * Retrieves all events for a match.
     *
     * Events include scoring moments, play of the game, highlights, etc.
     * Used for video navigation and event timeline display.
     *
     * @param matchId the unique match identifier
     * @return list of events ordered by timestamp
     */
    suspend fun getMatchEvents(matchId: String): Result<List<Event>>

    /**
     * Retrieves a specific event by ID.
     *
     * @param matchId the unique match identifier
     * @param eventId the unique event identifier
     * @return the event if found
     */
    suspend fun getEventById(matchId: String, eventId: String): Result<Event>

    /**
     * Retrieves highlights for a match.
     *
     * Highlights include play of the game, top rallies, fastest shots, etc.
     *
     * @param matchId the unique match identifier
     * @return the highlights collection
     */
    suspend fun getMatchHighlights(matchId: String): Result<Highlights>

    /**
     * Retrieves the video URL for streaming.
     *
     * Returns a URL that can be used with ExoPlayer for video playback.
     *
     * @param matchId the unique match identifier
     * @return the video streaming URL
     */
    suspend fun getVideoUrl(matchId: String): Result<String>

    /**
     * Deletes a match and its associated video.
     *
     * @param matchId the unique match identifier
     * @return success or failure result
     */
    suspend fun deleteMatch(matchId: String): Result<Unit>

    /**
     * Refreshes match data from the server.
     *
     * Forces a refresh of cached data, useful for pulling latest processing
     * status or newly detected events.
     *
     * @param matchId the unique match identifier
     * @return the updated match
     */
    suspend fun refreshMatch(matchId: String): Result<Match>

    /**
     * Checks the processing status of a match.
     *
     * Useful for polling during video processing to update UI.
     *
     * @param matchId the unique match identifier
     * @return the current processing status
     */
    suspend fun getMatchStatus(matchId: String): Result<MatchStatus>
}
