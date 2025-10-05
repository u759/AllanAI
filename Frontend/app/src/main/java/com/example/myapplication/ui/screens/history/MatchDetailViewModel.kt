package com.example.myapplication.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.model.Detection
import com.example.myapplication.data.model.Event
import com.example.myapplication.data.model.Match
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
     * Update current video playback position.
     */
    fun updatePosition(positionMs: Long) {
        _currentPositionMs.value = positionMs
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
