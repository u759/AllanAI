package com.example.myapplication.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.model.Match
import com.example.myapplication.data.model.MatchStatus
import com.example.myapplication.data.repository.MatchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Match List (History) screen.
 *
 * This ViewModel manages the state of the match list, fetching data from the
 * MatchRepository and exposing it to the UI through StateFlow.
 *
 * Following MVVM architecture, this ViewModel:
 * - Fetches match data from the repository layer
 * - Transforms Match domain models into UI-friendly MatchItem models
 * - Manages loading and error states
 * - Provides reactive data streams to the Compose UI
 */
@HiltViewModel
class MatchesViewModel @Inject constructor(
    private val matchRepository: MatchRepository
) : ViewModel() {

    // UI State
    private val _uiState = MutableStateFlow<MatchListUiState>(MatchListUiState.Loading)
    val uiState: StateFlow<MatchListUiState> = _uiState.asStateFlow()

    // Polling job for processing matches
    private var pollingJob: Job? = null

    init {
        loadMatches()
    }

    /**
     * Load all matches from the repository.
     */
    private fun loadMatches() {
        viewModelScope.launch {
            _uiState.value = MatchListUiState.Loading

            // Initial fetch from backend
            matchRepository.refreshAllMatches()

            try {
                matchRepository.getAllMatches().collect { matches ->
                    // Show ALL matches (not just COMPLETE)

                    // Transform Match models to UI MatchItems
                    val matchItems = matches.map { match ->
                        MatchItem(
                            id = match.id,
                            player1 = match.player1Name ?: "Player 1",
                            player2 = match.player2Name ?: "Player 2",
                            score1 = match.statistics?.player1Score ?: 0,
                            score2 = match.statistics?.player2Score ?: 0,
                            thumbnailUrl = getThumbnailUrl(match),
                            date = match.createdAt.toString(),
                            duration = formatDuration(match.durationSeconds),
                            status = match.status
                        )
                    }

                    _uiState.value = if (matchItems.isEmpty()) {
                        MatchListUiState.Empty
                    } else {
                        MatchListUiState.Success(matchItems)
                    }

                    // Start or stop polling based on match statuses
                    managePolling(matches)
                }
            } catch (e: Exception) {
                _uiState.value = MatchListUiState.Error(
                    message = e.message ?: "Failed to load matches"
                )
            }
        }
    }

    /**
     * Refresh the match list.
     */
    fun refreshMatches() {
        viewModelScope.launch {
            matchRepository.refreshAllMatches()
        }
    }

    /**
     * Manage polling for matches that are UPLOADED or PROCESSING.
     */
    private fun managePolling(matches: List<Match>) {
        val hasProcessingMatches = matches.any {
            it.status == MatchStatus.UPLOADED || it.status == MatchStatus.PROCESSING
        }

        if (hasProcessingMatches && pollingJob == null) {
            // Start polling
            pollingJob = viewModelScope.launch {
                while (true) {
                    delay(15000) // Poll every 15 seconds
                    matchRepository.refreshAllMatches()
                }
            }
        } else if (!hasProcessingMatches && pollingJob != null) {
            // Stop polling
            pollingJob?.cancel()
            pollingJob = null
        }
    }

    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
    }

    /**
     * Get thumbnail URL for a match.
     * In production, this would fetch from the backend.
     * For now, using placeholder images.
     */
    private fun getThumbnailUrl(match: Match): String {
        // Use a variety of table tennis stock images
        val thumbnails = listOf(
            "https://lh3.googleusercontent.com/aida-public/AB6AXuCEN5R1Phl90yI9Mn33AQNkuFoXCS1W2lJZ36YfuQxf2worp_SsjWkJFZEBLpr4RopEyXVkYUK0LLQlCOaU9xk2ySdoa8aFNPu99inVhBG7SiHAKDWVUuPRrHgv1ST3-kIdd8ayCoIuSEQQ4tHSWqaAAPMLprC45Jp30L_JAJf1PumH9D5wVpn5biB38Wg_gcOh7S31X9J1Wu9uopydS6p7tf1MTAEGBeG8bPccRGicRapnD6eyxU1J-zsNtXN0G-faIOOgIk88YdzA",
            "https://lh3.googleusercontent.com/aida-public/AB6AXuA4Nc688oh5dDMLytkQcDbF2uRCSTqDXLto_X7-80U6Ok8ILSffy_tb2NnHZpUq6I_8pS-reEw2g8nfvv8UZ_8rt6Tm-CDb__-xD8b3uCzR9dVHi88qSUienv19yhJI_6g65MxWUUCQgI4xQ904pp7lqk0ZnkYpFd7EaCOTR_SipuCIsylnNPUvX_PDZa-PwrX4qAX_JQdFiNlkvBTfQAoJcwgvAlk8hA7z7faEet-OE6ivCOe6ZseZE_S7seOwpPEEWR7LJN78c3-1",
            "https://lh3.googleusercontent.com/aida-public/AB6AXuAwJ7L0JmuTZZX5hzpfSKsgwBLMKXv1NgHa45tRl7nDBWG-AHDBfSwRw-3xvS3EjqL5-AH_pEEsyJf1SqEx2UHEVLcuYQHBtkY5wmNOoJ7-jyBDRsetlNLY5UlPpp9eQtHgRa1HEbGWb5eHt8swPPNehW7xqqzgVDCLkpmE1Mf_tUDim9j3RN7xBZSZUi-6znWFi05-sNAzXC9v6FJ9HLjhJ23wlhFvoNaulKidv0fd2ddAUtPylHVxKyI-olIlT55dMpac1cIzNcQG"
        )
        // Use hash of match ID to consistently pick the same thumbnail
        val index = match.id.hashCode().mod(thumbnails.size).let { if (it < 0) it + thumbnails.size else it }
        return thumbnails[index]
    }

    /**
     * Format duration in seconds to a readable string.
     */
    private fun formatDuration(durationSeconds: Int?): String {
        if (durationSeconds == null) return "N/A"
        val minutes = durationSeconds / 60
        val seconds = durationSeconds % 60
        return String.format("%d:%02d", minutes, seconds)
    }
}

/**
 * UI state for the Match List screen.
 */
sealed class MatchListUiState {
    object Loading : MatchListUiState()
    object Empty : MatchListUiState()
    data class Success(val matches: List<MatchItem>) : MatchListUiState()
    data class Error(val message: String) : MatchListUiState()
}

/**
 * Enhanced MatchItem with additional fields for display.
 */
data class MatchItem(
    val id: String,
    val player1: String,
    val player2: String,
    val score1: Int,
    val score2: Int,
    val thumbnailUrl: String,
    val date: String = "",
    val duration: String = "",
    val status: MatchStatus = MatchStatus.COMPLETE
)
