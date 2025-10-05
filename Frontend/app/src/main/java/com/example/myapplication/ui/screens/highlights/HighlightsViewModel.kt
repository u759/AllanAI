package com.example.myapplication.ui.screens.highlights

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.R
import com.example.myapplication.data.model.Event
import com.example.myapplication.data.model.EventType
import com.example.myapplication.data.model.Match
import com.example.myapplication.data.model.MatchStatus
import com.example.myapplication.data.repository.MatchRepository
import com.example.myapplication.util.HighlightThumbnailGenerator
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
     */
    private fun loadHighlights() {
        viewModelScope.launch {
            _uiState.value = HighlightsUiState.Loading
            try {
                matchRepository.getAllMatches().collect { matches ->
                    // Filter only completed matches that have events
                    val completedMatches = matches.filter { 
                        it.status == MatchStatus.COMPLETE && it.events.isNotEmpty()
                    }
                    
                    // Group highlights by match
                    val highlightGroups = completedMatches.map { match ->
                        createHighlightGroup(match)
                    }
                    
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
     * Create a HighlightGroup from a match.
     */
    private fun createHighlightGroup(match: Match): HighlightGroup {
        // Format the match title with date
        val dateStr = formatDate(match.createdAt)
        val title = "Match - $dateStr"
        
        // Get important events as highlights
        val highlights = match.events
            .filter { event ->
                // Only include important event types
                event.type in listOf(
                    EventType.PLAY_OF_THE_GAME,
                    EventType.RALLY_HIGHLIGHT,
                    EventType.FASTEST_SHOT,
                    EventType.SERVE_ACE
                )
            }
            .sortedByDescending { it.importance }
            .take(4) // Limit to top 4 highlights per match
            .map { event ->
                HighlightItem(
                    id = event.id,
                    matchId = match.id,
                    title = event.title,
                    duration = formatClipDuration(event),  // Calculate actual clip duration
                    thumbnailUrl = getThumbnailUrl(match, event),
                    eventType = event.type
                )
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
     * Calculate and format the actual clip duration (preMs + postMs).
     */
    private fun formatClipDuration(event: Event): String {
        // Always use 3 seconds before and after (same as HighlightClip)
        val preMs = 3000L
        val postMs = 3000L
        
        // Calculate actual clip boundaries
        val startMs = maxOf(0, event.timestampMs - preMs)
        val endMs = event.timestampMs + postMs
        val durationMs = endMs - startMs
        
        // Format to seconds
        val totalSeconds = durationMs / 1000
        return "${totalSeconds}s"
    }

    /**
     * Get thumbnail for a highlight event by extracting the first frame.
     * 
     * Generates a thumbnail from the video at the event's timestamp.
     * Thumbnails are cached to avoid regeneration on subsequent loads.
     */
    private fun getThumbnailUrl(match: Match, event: Event): String {
        // Always use 3 seconds before - ignore JSON eventWindow
        val preMs = 3000L  // 3 seconds before for context
        val startMs = maxOf(0, event.timestampMs - preMs)
        
        // Generate thumbnail from the first frame of the highlight
        val thumbnailPath = HighlightThumbnailGenerator.generateThumbnail(
            context = context,
            videoResourceId = R.raw.test_2,
            timestampMs = startMs,  // Use the start timestamp (first frame of highlight)
            highlightId = event.id
        )
        
        // Return file path if successful, otherwise fallback to placeholder
        return thumbnailPath ?: getPlaceholderThumbnail(event.type)
    }
    
    /**
     * Fallback placeholder thumbnail if frame extraction fails.
     */
    private fun getPlaceholderThumbnail(eventType: EventType): String {
        return when (eventType) {
            EventType.PLAY_OF_THE_GAME, EventType.RALLY_HIGHLIGHT -> {
                "https://lh3.googleusercontent.com/aida-public/AB6AXuA58uYUU3Kl5b6NormCVtZruihKIOljtFsMTGHHO_o6Fhmu_LK-1qJWvMkem0FA8m36Unh52J8FCYoX4zh74w7mwqzsvTTDDL7gPrFyxdMYxO93p73sviLQlbLw5fyFnwupIb6N9WOnuiVeepTfzG_AG2buTjscEXzm-L1Jhz4_1EmhKjn49i8tddrrnmph-OvE0bTTulZ6Mgh1t-1mgM3xJc88bmP4EM3aSlv6FhWgQG2McIVDGAlALOrXGggTcKolx7CPxrYqN-Lu"
            }
            EventType.FASTEST_SHOT, EventType.SERVE_ACE -> {
                "https://lh3.googleusercontent.com/aida-public/AB6AXuCnc8ZOIEm4toCUsiJsPvpjHSRdoMdHI5OvBxyUoS5c4Z_YVoqk52tJHgxswQwzuNEubGyB_ZQJttkkqrWGnq_gHx8s36SDMy_2nOTz3-1JK2_7ptbg_B00xdFO8Sew0fUZN4vozQuQBviJYGBOuK3dfw42Zg1rWUl9-eIuPWDCi6U_dnM1Gs82LNrJXGu3FuYe_u9OmY0Jw0MofVdI498j3Tsw70K4PerH6o7Vc5hqpLmsQvTiCUHQbqEmxQQ8Cz-8x3sYmvgKq_0U"
            }
            else -> {
                "https://lh3.googleusercontent.com/aida-public/AB6AXuCEN5R1Phl90yI9Mn33AQNkuFoXCS1W2lJZ36YfuQxf2worp_SsjWkJFZEBLpr4RopEyXVkYUK0LLQlCOaU9xk2ySdoa8aFNPu99inVhBG7SiHAKDWVUuPRrHgv1ST3-kIdd8ayCoIuSEQQ4tHSWqaAAPMLprC45Jp30L_JAJf1PumH9D5wVpn5biB38Wg_gcOh7S31X9J1Wu9uopydS6p7tf1MTAEGBeG8bPccRGicRapnD6eyxU1J-zsNtXN0G-faIOOgIk88YdzA"
            }
        }
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
