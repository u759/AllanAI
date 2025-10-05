package com.example.myapplication.data.api

import com.example.myapplication.data.api.dto.EventResponse
import com.example.myapplication.data.api.dto.HighlightsResponse
import com.example.myapplication.data.api.dto.MatchDetailsResponse
import com.example.myapplication.data.api.dto.MatchStatisticsResponse
import com.example.myapplication.data.api.dto.MatchStatusResponse
import com.example.myapplication.data.api.dto.MatchSummaryResponse
import com.example.myapplication.data.api.dto.MatchUploadResponse
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Streaming

/**
 * Retrofit API service for AllanAI backend.
 * 
 * This interface defines all REST API endpoints for match operations.
 * Retrofit will automatically implement this interface at runtime.
 * 
 * Base URL: http://10.0.2.2:8080 (Android emulator localhost)
 * 
 * All endpoints follow the backend API structure defined in MatchController.java
 */
interface AllanAIApiService {
    
    /**
     * Upload a video file to create a new match.
     * 
     * POST /api/matches/upload
     * 
     * @param video MultipartBody.Part containing the video file
     * @return MatchUploadResponse with matchId and initial status
     */
    @Multipart
    @POST("api/matches/upload")
    suspend fun uploadMatch(
        @Part video: MultipartBody.Part
    ): MatchUploadResponse
    
    /**
     * Get list of all matches.
     * 
     * GET /api/matches
     * 
     * @return List of match summaries
     */
    @GET("api/matches")
    suspend fun getMatches(): List<MatchSummaryResponse>
    
    /**
     * Get detailed information about a specific match.
     * 
     * GET /api/matches/{id}
     * 
     * @param matchId Unique match identifier
     * @return Complete match details including statistics, events, and highlights
     */
    @GET("api/matches/{id}")
    suspend fun getMatchDetails(
        @Path("id") matchId: String
    ): MatchDetailsResponse
    
    /**
     * Get processing status of a match.
     * 
     * GET /api/matches/{id}/status
     * 
     * @param matchId Unique match identifier
     * @return Current processing status
     */
    @GET("api/matches/{id}/status")
    suspend fun getMatchStatus(
        @Path("id") matchId: String
    ): MatchStatusResponse
    
    /**
     * Get statistics for a match.
     * 
     * GET /api/matches/{id}/statistics
     * 
     * @param matchId Unique match identifier
     * @return Match statistics
     */
    @GET("api/matches/{id}/statistics")
    suspend fun getMatchStatistics(
        @Path("id") matchId: String
    ): MatchStatisticsResponse
    
    /**
     * Get all events for a match.
     * 
     * GET /api/matches/{id}/events
     * 
     * @param matchId Unique match identifier
     * @return List of timestamped events
     */
    @GET("api/matches/{id}/events")
    suspend fun getMatchEvents(
        @Path("id") matchId: String
    ): List<EventResponse>
    
    /**
     * Get highlights for a match.
     * 
     * GET /api/matches/{id}/highlights
     * 
     * @param matchId Unique match identifier
     * @return Highlights collection
     */
    @GET("api/matches/{id}/highlights")
    suspend fun getMatchHighlights(
        @Path("id") matchId: String
    ): HighlightsResponse
    
    /**
     * Stream video file for playback.
     * 
     * GET /api/matches/{id}/video
     * 
     * @param matchId Unique match identifier
     * @return Video file as ResponseBody for streaming
     */
    @Streaming
    @GET("api/matches/{id}/video")
    suspend fun streamVideo(
        @Path("id") matchId: String
    ): ResponseBody
    
    /**
     * Delete a match and its associated video.
     * 
     * DELETE /api/matches/{id}
     * 
     * @param matchId Unique match identifier
     */
    @DELETE("api/matches/{id}")
    suspend fun deleteMatch(
        @Path("id") matchId: String
    )
}
