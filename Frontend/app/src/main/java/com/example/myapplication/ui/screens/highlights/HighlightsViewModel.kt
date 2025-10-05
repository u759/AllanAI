package com.example.myapplication.ui.screens.highlights

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.model.Event
import com.example.myapplication.data.model.EventType
import com.example.myapplication.data.model.Match
import com.example.myapplication.data.model.MatchStatus
import com.example.myapplication.data.repository.MatchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * ViewModel for the Highlights screen.
 *
 * This ViewModel manages the state of highlights, fetching data from completed
 * matches and organizing events by match into groups for display.
 *
 * Following MVVM architecture, this ViewModel:
 * - Fetches match and event data from the repository layer
 * - Groups highlights by match
 * - Transforms domain models into UI-friendly models
 * - Manages loading and error states
 */
@HiltViewModel
class HighlightsViewModel @Inject constructor(
    private val matchRepository: MatchRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    // UI State
    private val _uiState = MutableStateFlow<HighlightsUiState>(HighlightsUiState.Loading)
    val uiState: StateFlow<HighlightsUiState> = _uiState.asStateFlow()

    init {
        loadHighlights()
    }

    /**
     * Load highlights from all completed matches.
     * 
     * Uses the backend's automatically generated highlights which includes:
     * - Play of the Game (highest importance event)
     * - Top 3 Rallies
     * - Top 3 Fastest Shots
     * - Top 3 Best Serves
     */
    private fun loadHighlights() {
        viewModelScope.launch {
            _uiState.value = HighlightsUiState.Loading
            try {
                matchRepository.getAllMatches().collect { matches ->
                    // Filter only completed matches that have highlights
                    val completedMatches = matches.filter { 
                        it.status == MatchStatus.COMPLETE && 
                        it.highlights != null &&
                        it.events.isNotEmpty()
                    }
                    
                    // Group highlights by match using backend's curated highlights
                    val highlightGroups = completedMatches.map { match ->
                        createHighlightGroup(match)
                    }.filter { it.highlights.isNotEmpty() }
                    
                    _uiState.value = if (highlightGroups.isEmpty()) {
                        HighlightsUiState.Empty
                    } else {
                        HighlightsUiState.Success(highlightGroups)
                    }
                }
            } catch (e: Exception) {
                _uiState.value = HighlightsUiState.Error(
                    message = e.message ?: "Failed to load highlights"
                )
            }
        }
    }

    /**
     * Refresh highlights.
     */
    fun refreshHighlights() {
        loadHighlights()
    }

    /**
     * Create a HighlightGroup from a match using backend's curated highlights.
     * 
     * The backend automatically generates highlights based on event importance:
     * - Play of the Game: Single highest importance event
     * - Top Rallies: Up to 3 best rally highlights
     * - Fastest Shots: Up to 3 fastest shot events
     * - Best Serves: Up to 3 best serve aces
     */
    private fun createHighlightGroup(match: Match): HighlightGroup {
        // Format the match title with date
        val dateStr = formatDate(match.createdAt)
        val title = "Match - $dateStr"
        
        // Get curated highlights from backend
        val backendHighlights = match.highlights
        if (backendHighlights == null) {
            return HighlightGroup(
                matchId = match.id,
                title = title,
                highlights = emptyList()
            )
        }
        
        // Collect all highlight event IDs from backend's curated list
        val highlightEventIds = mutableListOf<String>()
        
        // Add play of the game (highest priority)
        backendHighlights.playOfTheGame?.let { highlightEventIds.add(it) }
        
        // Add top rallies (up to 3)
        highlightEventIds.addAll(backendHighlights.topRallies)
        
        // Add fastest shots (up to 3)
        highlightEventIds.addAll(backendHighlights.fastestShots)
        
        // Add best serves (up to 3)
        highlightEventIds.addAll(backendHighlights.bestServes)
        
        // Look up the actual event objects and create highlight items
        val highlights = highlightEventIds.mapNotNull { eventId ->
            match.events.find { it.id == eventId }?.let { event ->
                HighlightItem(
                    id = event.id,
                    matchId = match.id,
                    title = event.title,
                    duration = formatClipDuration(event),  // 3s before + 3s after = 6s clip
                    thumbnailUrl = getThumbnailUrl(match, event),
                    eventType = event.type
                )
            }
        }
        
        return HighlightGroup(
            matchId = match.id,
            title = title,
            highlights = highlights
        )
    }

    /**
     * Format Instant to readable date string.
     */
    private fun formatDate(instant: Instant): String {
        return try {
            val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
                .withZone(ZoneId.systemDefault())
            formatter.format(instant)
        } catch (e: Exception) {
            "Unknown Date"
        }
    }

    /**
     * Format timestamp in milliseconds to MM:SS format.
     */
    private fun formatDuration(timestampMs: Long): String {
        val totalSeconds = timestampMs / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%d:%02d", minutes, seconds)
    }
    
    /**
     * Calculate and format the actual clip duration.
     * 
     * Creates a 6-second clip: 3 seconds before the event + 3 seconds after.
     * This provides context around the highlight moment.
     */
    private fun formatClipDuration(event: Event): String {
        // 3 seconds before the event frame
        val preMs = 3000L
        // 3 seconds after the event frame
        val postMs = 3000L
        
        // Calculate actual clip boundaries
        val startMs = maxOf(0, event.timestampMs - preMs)
        val endMs = event.timestampMs + postMs
        val durationMs = endMs - startMs
        
        // Format to seconds (typically 6s for highlights)
        val totalSeconds = durationMs / 1000
        return "${totalSeconds}s"
    }

    /**
     * Get thumbnail URL from match's thumbnail path.
     * 
     * Uses the backend-generated thumbnail for the match.
     * Event-specific thumbnails could be generated server-side in the future.
     */
    private fun getThumbnailUrl(match: Match, event: Event): String {
        // Use match thumbnail if available
        // In the future, the backend could generate event-specific thumbnails
        return match.thumbnailPath ?: ""
    }
}

/**
 * UI state for the Highlights screen.
 */
sealed class HighlightsUiState {
    object Loading : HighlightsUiState()
    object Empty : HighlightsUiState()
    data class Success(val highlightGroups: List<HighlightGroup>) : HighlightsUiState()
    data class Error(val message: String) : HighlightsUiState()
}

/**
 * Group of highlights from a single match.
 */
data class HighlightGroup(
    val matchId: String,
    val title: String,
    val highlights: List<HighlightItem>
)

/**
 * Individual highlight item for display.
 */
data class HighlightItem(
    val id: String,
    val matchId: String,
    val title: String,
    val duration: String,
    val thumbnailUrl: String,
    val eventType: EventType
)
