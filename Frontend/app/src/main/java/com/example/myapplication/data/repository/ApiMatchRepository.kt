package com.example.myapplication.data.repository

import android.os.Build
import androidx.annotation.RequiresApi
import com.example.myapplication.data.api.AllanAIApiService
import com.example.myapplication.data.api.mapper.toEvent
import com.example.myapplication.data.api.mapper.toHighlights
import com.example.myapplication.data.api.mapper.toMatch
import com.example.myapplication.data.model.Event
import com.example.myapplication.data.model.Highlights
import com.example.myapplication.data.model.Match
import com.example.myapplication.data.model.MatchStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Production implementation of MatchRepository using real backend API.
 * 
 * This repository communicates with the Spring Boot backend using Retrofit
 * and follows the architecture guidelines:
 * - Uses suspend functions for async operations
 * - Returns Flow for reactive data
 * - Returns Result for error handling
 * - Maps API DTOs to domain models
 * 
 * @param apiService Retrofit API service for backend communication
 */
@Singleton
class ApiMatchRepository @Inject constructor(
    private val apiService: AllanAIApiService
) : MatchRepository {
    
    // In-memory cache for matches (could be replaced with Room database later)
    private val matchesCache = MutableStateFlow<List<Match>>(emptyList())
    
    /**
     * Upload a video file to create a new match.
     *
     * The video is sent as multipart/form-data to the backend.
     * Backend will process it asynchronously and detect events, shots, etc.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    override suspend fun uploadMatch(
        videoFile: File,
        filename: String,
        player1Name: String?,
        player2Name: String?,
        matchTitle: String?
    ): Result<Match> {
        return try {
            // Create multipart request body
            val requestBody = videoFile.asRequestBody("video/mp4".toMediaType())
            val multipartBody = MultipartBody.Part.createFormData(
                "video",
                filename,
                requestBody
            )

            // Create metadata parts
            val player1Part = player1Name?.takeIf { it.isNotBlank() }
                ?.toRequestBody("text/plain".toMediaTypeOrNull())

            val player2Part = player2Name?.takeIf { it.isNotBlank() }
                ?.toRequestBody("text/plain".toMediaTypeOrNull())

            val titlePart = matchTitle?.takeIf { it.isNotBlank() }
                ?.toRequestBody("text/plain".toMediaTypeOrNull())

            // Upload to backend
            val response = apiService.uploadMatch(
                video = multipartBody,
                player1Name = player1Part,
                player2Name = player2Part,
                matchTitle = titlePart
            )

            // Create Match from response (minimal data, will be updated when processing completes)
            val match = Match(
                id = response.matchId,
                createdAt = java.time.Instant.now(),
                status = MatchStatus.valueOf(response.status),
                player1Name = player1Name,
                player2Name = player2Name,
                matchTitle = matchTitle
            )

            // Add to cache
            val currentMatches = matchesCache.value.toMutableList()
            currentMatches.add(0, match) // Add to beginning
            matchesCache.value = currentMatches

            Result.success(match)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get all matches from the backend.
     * 
     * Returns a Flow that emits the list of matches from the cache.
     * Call refreshMatch() from the ViewModel to update the cache.
     */
    override fun getAllMatches(): Flow<List<Match>> {
        return matchesCache.asStateFlow()
    }
    
    /**
     * Manually refresh all matches from the API.
     * Call this from the ViewModel when you need to update the match list.
     */
    override suspend fun refreshAllMatches(): Result<Unit> {
        return try {
            val response = apiService.getMatches()
            val matches = response.map { it.toMatch() }
            matchesCache.value = matches
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get a specific match by ID.
     */
    override fun getMatchById(matchId: String): Flow<Match?> {
        return matchesCache.map { matches ->
            matches.find { it.id == matchId }
        }
    }
    
    /**
     * Get matches filtered by status.
     */
    override fun getMatchesByStatus(status: MatchStatus): Flow<List<Match>> {
        return matchesCache.map { matches ->
            matches.filter { it.status == status }
        }
    }
    
    /**
     * Get detailed statistics for a match.
     * 
     * Fetches complete match details including statistics, shots, and events.
     */
    override suspend fun getMatchStatistics(matchId: String): Result<Match> {
        return try {
            val response = apiService.getMatchDetails(matchId)
            val match = response.toMatch()
            
            // Update cache
            updateMatchInCache(match)
            
            Result.success(match)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get all events for a match.
     */
    override suspend fun getMatchEvents(matchId: String): Result<List<Event>> {
        return try {
            val response = apiService.getMatchEvents(matchId)
            val events = response.map { it.toEvent() }
            Result.success(events)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get a specific event by ID.
     */
    override suspend fun getEventById(matchId: String, eventId: String): Result<Event> {
        return try {
            val response = apiService.getMatchEvents(matchId)
            val event = response.find { it.id == eventId }
                ?.toEvent()
                ?: throw Exception("Event not found: $eventId")
            Result.success(event)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get highlights for a match.
     */
    override suspend fun getMatchHighlights(matchId: String): Result<Highlights> {
        return try {
            val response = apiService.getMatchHighlights(matchId)
            val highlights = response.toHighlights()
            Result.success(highlights)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get video URL for streaming.
     *
     * Returns the URL that can be used with ExoPlayer for video playback.
     */
    override suspend fun getVideoUrl(matchId: String): Result<String> {
        return try {
            // Construct video URL from Retrofit's base URL
            val baseUrl = apiService.toString() // Get base URL from Retrofit
            val videoUrl = "http://192.168.1.147:8080/api/matches/$matchId/video"
            Result.success(videoUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Delete a match and its associated video.
     */
    override suspend fun deleteMatch(matchId: String): Result<Unit> {
        return try {
            apiService.deleteMatch(matchId)
            
            // Remove from cache
            val currentMatches = matchesCache.value.toMutableList()
            currentMatches.removeAll { it.id == matchId }
            matchesCache.value = currentMatches
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Refresh match data from the server.
     */
    override suspend fun refreshMatch(matchId: String): Result<Match> {
        return try {
            val response = apiService.getMatchDetails(matchId)
            val match = response.toMatch()
            
            // Update cache
            updateMatchInCache(match)
            
            Result.success(match)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Check processing status of a match.
     */
    override suspend fun getMatchStatus(matchId: String): Result<MatchStatus> {
        return try {
            val response = apiService.getMatchStatus(matchId)
            val status = MatchStatus.valueOf(response.status)
            Result.success(status)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Download video file to local cache storage.
     *
     * Note: This method requires OkHttpClient and ApplicationContext to be injected.
     * For now, returning URL for direct streaming. Implement download later if needed.
     */
    override suspend fun downloadVideo(
        matchId: String,
        onProgress: (Float) -> Unit
    ): Result<String> {
        // TODO: Implement video download with progress tracking
        // For now, just return the streaming URL
        return getVideoUrl(matchId)
    }

    // Internal helper methods

    /**
     * Update a specific match in the cache.
     */
    private fun updateMatchInCache(updatedMatch: Match) {
        val currentMatches = matchesCache.value.toMutableList()
        val index = currentMatches.indexOfFirst { it.id == updatedMatch.id }

        if (index != -1) {
            currentMatches[index] = updatedMatch
        } else {
            currentMatches.add(0, updatedMatch)
        }

        matchesCache.value = currentMatches
    }
}
