package com.example.myapplication.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.model.Detection
import com.example.myapplication.data.model.Event
import com.example.myapplication.data.model.Match
import com.example.myapplication.data.model.ScoreState
import com.example.myapplication.data.model.Shot
import com.example.myapplication.data.repository.MatchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Match Detail screen with video playback and overlay support.
 */
@HiltViewModel
class MatchDetailViewModel @Inject constructor(
    private val matchRepository: MatchRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<MatchDetailUiState>(MatchDetailUiState.Loading)
    val uiState: StateFlow<MatchDetailUiState> = _uiState.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _videoUrl = MutableStateFlow<String?>(null)
    val videoUrl: StateFlow<String?> = _videoUrl.asStateFlow()

    private val _currentShot = MutableStateFlow<Shot?>(null)
    val currentShot: StateFlow<Shot?> = _currentShot.asStateFlow()

    private val _currentEvent = MutableStateFlow<Event?>(null)
    val currentEvent: StateFlow<Event?> = _currentEvent.asStateFlow()

    private val _currentScore = MutableStateFlow<ScoreState?>(null)
    val currentScore: StateFlow<ScoreState?> = _currentScore.asStateFlow()

    private val _liveStats = MutableStateFlow<LiveStats?>(null)
    val liveStats: StateFlow<LiveStats?> = _liveStats.asStateFlow()

    /**
     * Load match details by ID.
     */
    fun loadMatch(matchId: String) {
        viewModelScope.launch {
            _uiState.value = MatchDetailUiState.Loading
            try {
                // Load match details
                matchRepository.getMatchById(matchId).collect { match ->
                    if (match != null) {
                        _uiState.value = MatchDetailUiState.Success(match)
                    } else {
                        _uiState.value = MatchDetailUiState.Error("Match not found")
                    }
                }
            } catch (e: Exception) {
                _uiState.value = MatchDetailUiState.Error(e.message ?: "Failed to load match")
            }
        }

        // Load video URL separately
        viewModelScope.launch {
            try {
                matchRepository.getVideoUrl(matchId).onSuccess { url ->
                    _videoUrl.value = url
                }.onFailure { error ->
                    // Log error but don't fail the whole screen
                    android.util.Log.e("MatchDetailViewModel", "Failed to load video URL", error)
                }
            } catch (e: Exception) {
                android.util.Log.e("MatchDetailViewModel", "Error loading video URL", e)
            }
        }
    }

    /**
     * Update current video playback position and sync real-time stats.
     */
    fun updatePosition(positionMs: Long) {
        _currentPositionMs.value = positionMs
        
        // Update real-time stats based on position
        (_uiState.value as? MatchDetailUiState.Success)?.match?.let { match ->
            updateRealTimeStats(match, positionMs)
        }
    }
    
    /**
     * Update real-time stats based on current video position.
     */
    private fun updateRealTimeStats(match: Match, positionMs: Long) {
        // Find current shot (within ±50ms window)
        val shot = match.shots.firstOrNull { 
            kotlin.math.abs(it.timestampMs - positionMs) <= 50 
        }
        _currentShot.value = shot
        
        // Find current event (within ±200ms window for better UX)
        val event = match.events.firstOrNull {
            kotlin.math.abs(it.timestampMs - positionMs) <= 200
        }
        _currentEvent.value = event
        
        // Update current score from momentum timeline
        val scoreUpdate = match.statistics?.momentumTimeline?.samples
            ?.filter { it.timestampMs <= positionMs }
            ?.maxByOrNull { it.timestampMs }
        _currentScore.value = scoreUpdate?.scoreAfter
        
        // Calculate live stats up to current position
        val shotsUpToNow = match.shots.filter { it.timestampMs <= positionMs }
        val eventsUpToNow = match.events.filter { it.timestampMs <= positionMs }
        
        if (shotsUpToNow.isNotEmpty()) {
            val player1Shots = shotsUpToNow.filter { it.player == 1 }
            val player2Shots = shotsUpToNow.filter { it.player == 2 }
            
            _liveStats.value = LiveStats(
                totalShots = shotsUpToNow.size,
                player1Shots = player1Shots.size,
                player2Shots = player2Shots.size,
                maxSpeed = shotsUpToNow.maxOfOrNull { it.speed } ?: 0.0,
                avgSpeed = shotsUpToNow.map { it.speed }.average(),
                ralliesCompleted = eventsUpToNow.count { 
                    it.type == com.example.myapplication.data.model.EventType.RALLY_HIGHLIGHT
                },
                currentScore = _currentScore.value ?: ScoreState(0, 0)
            )
        }
    }

    /**
     * Update playback state.
     */
    fun updatePlaybackState(isPlaying: Boolean) {
        _isPlaying.value = isPlaying
    }

    /**
     * Get detections for the current video position.
     * Returns detections from shots that match the current timestamp.
     */
    fun getDetectionsForPosition(match: Match, positionMs: Long): List<DetectionWithType> {
        val detections = mutableListOf<DetectionWithType>()

        // Get detections from shots (ball position)
        match.shots.forEach { shot ->
            // Match shots within ±50ms window
            if (kotlin.math.abs(shot.timestampMs - positionMs) <= 50) {
                shot.detections.forEach { detection ->
                    detections.add(
                        DetectionWithType(
                            detection = detection,
                            type = DetectionType.BALL,
                            confidence = detection.confidence
                        )
                    )
                }
            }
        }

        // Get detections from events
        match.events.forEach { event ->
            if (kotlin.math.abs(event.timestampMs - positionMs) <= 100) {
                event.metadata?.detections?.forEach { detection ->
                    detections.add(
                        DetectionWithType(
                            detection = detection,
                            type = DetectionType.EVENT,
                            confidence = detection.confidence
                        )
                    )
                }
            }
        }

        return detections
    }

    /**
     * Get ball trajectory for current event (if any).
     */
    fun getTrajectoryForPosition(match: Match, positionMs: Long): List<List<Double>>? {
        return match.events.firstOrNull { event ->
            kotlin.math.abs(event.timestampMs - positionMs) <= 100
        }?.metadata?.ballTrajectory
    }

    /**
     * Jump to a specific event timestamp.
     */
    fun jumpToEvent(event: Event) {
        _currentPositionMs.value = event.timestampMs
    }
}

/**
 * UI State for Match Detail screen.
 */
sealed class MatchDetailUiState {
    object Loading : MatchDetailUiState()
    data class Success(val match: Match) : MatchDetailUiState()
    data class Error(val message: String) : MatchDetailUiState()
}

/**
 * Detection with type information for rendering.
 */
data class DetectionWithType(
    val detection: Detection,
    val type: DetectionType,
    val confidence: Double
)

/**
 * Type of detection for color-coding overlays.
 */
enum class DetectionType {
    BALL,      // Green - ball position from shots
    EVENT      // Yellow - event-specific detections
}

/**
 * Live statistics calculated up to current video position.
 */
data class LiveStats(
    val totalShots: Int,
    val player1Shots: Int,
    val player2Shots: Int,
    val maxSpeed: Double,
    val avgSpeed: Double,
    val ralliesCompleted: Int,
    val currentScore: ScoreState
)
