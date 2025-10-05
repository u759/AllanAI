package com.example.myapplication.ui.screens.highlights

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.model.HighlightClip
import com.example.myapplication.data.model.HighlightClipExtractor
import com.example.myapplication.data.repository.MatchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Screen displaying highlight video clips for a match.
 * 
 * Architecture notes:
 * - Follows MVVM pattern with ViewModel managing clip state
 * - Uses HighlightClipExtractor to parse JSON and create clips
 * - Displays clips using ClippedVideoPlayer with ExoPlayer
 * - Supports navigation between clips with Previous/Next buttons
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HighlightDetailScreen(
    viewModel: HighlightDetailViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentClipIndex by viewModel.currentClipIndex.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Match Highlights",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF52525B)
                        )
                    }
                },
                actions = {
                    Spacer(modifier = Modifier.width(48.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = Color.White
                ),
                modifier = Modifier.border(
                    width = 1.dp,
                    color = Color(0x1A9CA3AF),
                    shape = RoundedCornerShape(0.dp)
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is HighlightDetailUiState.Loading -> {
                    LoadingState()
                }
                is HighlightDetailUiState.Success -> {
                    HighlightDetailContent(
                        clips = state.clips,
                        currentClipIndex = currentClipIndex,
                        onNavigatePrevious = { viewModel.previousClip() },
                        onNavigateNext = { viewModel.nextClip() },
                        onClipSelected = { index -> viewModel.selectClip(index) },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                is HighlightDetailUiState.Empty -> {
                    EmptyState()
                }
                is HighlightDetailUiState.Error -> {
                    ErrorState(
                        message = state.message,
                        onRetry = { viewModel.retry() }
                    )
                }
            }
        }
    }
}

@Composable
private fun HighlightDetailContent(
    clips: List<HighlightClip>,
    currentClipIndex: Int,
    onNavigatePrevious: () -> Unit,
    onNavigateNext: () -> Unit,
    onClipSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Current clip video player
        if (clips.isNotEmpty() && currentClipIndex in clips.indices) {
            ClippedVideoPlayer(
                clip = clips[currentClipIndex],
                currentClipIndex = currentClipIndex,
                totalClips = clips.size,
                onNavigatePrevious = onNavigatePrevious,
                onNavigateNext = onNavigateNext,
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        // Clip selector carousel (if more than one clip)
        if (clips.size > 1) {
            Text(
                text = "All Highlights",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = Color.White,
                modifier = Modifier.padding(top = 8.dp)
            )
            
            ClipSelectorCarousel(
                clips = clips,
                selectedIndex = currentClipIndex,
                onClipSelected = onClipSelected
            )
        }
    }
}

@Composable
private fun ClipSelectorCarousel(
    clips: List<HighlightClip>,
    selectedIndex: Int,
    onClipSelected: (Int) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(clips.size) { index ->
            ClipSelectorCard(
                clip = clips[index],
                index = index,
                isSelected = index == selectedIndex,
                onClick = { onClipSelected(index) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClipSelectorCard(
    clip: HighlightClip,
    index: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .width(160.dp)
            .height(100.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        shape = RoundedCornerShape(8.dp),
        border = if (isSelected) {
            CardDefaults.outlinedCardBorder().copy(
                width = 2.dp,
                brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary)
            )
        } else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "#${index + 1}",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White
                )
                
                Surface(
                    color = getCategoryColor(clip.category),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = getCategoryShortName(clip.category),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            
            // Title
            Text(
                text = clip.title,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White,
                maxLines = 2
            )
        }
    }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            Text(
                text = "Loading highlights...",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White
            )
        }
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "No Highlights Available",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Text(
                text = "This match doesn't have highlights yet. They'll be generated during processing.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "Error Loading Highlights",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = Color.White
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Retry")
            }
        }
    }
}

private fun getCategoryShortName(category: com.example.myapplication.data.model.HighlightCategory): String {
    return when (category) {
        com.example.myapplication.data.model.HighlightCategory.PLAY_OF_THE_GAME -> "POTG"
        com.example.myapplication.data.model.HighlightCategory.TOP_RALLY -> "Rally"
        com.example.myapplication.data.model.HighlightCategory.FASTEST_SHOT -> "Fast"
        com.example.myapplication.data.model.HighlightCategory.BEST_SERVE -> "Serve"
        com.example.myapplication.data.model.HighlightCategory.SCORE -> "Score"
        com.example.myapplication.data.model.HighlightCategory.OTHER -> "Other"
    }
}

private fun getCategoryColor(category: com.example.myapplication.data.model.HighlightCategory): Color {
    return when (category) {
        com.example.myapplication.data.model.HighlightCategory.PLAY_OF_THE_GAME -> Color(0xFFFFD700)
        com.example.myapplication.data.model.HighlightCategory.TOP_RALLY -> Color(0xFF00BCD4)
        com.example.myapplication.data.model.HighlightCategory.FASTEST_SHOT -> Color(0xFFFF5722)
        com.example.myapplication.data.model.HighlightCategory.BEST_SERVE -> Color(0xFF4CAF50)
        com.example.myapplication.data.model.HighlightCategory.SCORE -> Color(0xFF9C27B0)
        com.example.myapplication.data.model.HighlightCategory.OTHER -> Color(0xFF607D8B)
    }
}

/**
 * ViewModel for Highlight Detail Screen.
 * 
 * Responsibilities:
 * - Load match data from repository
 * - Extract highlight clips using HighlightClipExtractor
 * - Manage current clip selection
 * - Handle navigation between clips
 */
@HiltViewModel
class HighlightDetailViewModel @Inject constructor(
    private val matchRepository: MatchRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<HighlightDetailUiState>(HighlightDetailUiState.Loading)
    val uiState: StateFlow<HighlightDetailUiState> = _uiState.asStateFlow()
    
    private val _currentClipIndex = MutableStateFlow(0)
    val currentClipIndex: StateFlow<Int> = _currentClipIndex.asStateFlow()
    
    private var matchId: String = ""
    
    /**
     * Load highlights for a match by extracting clips from JSON data.
     */
    fun loadHighlights(matchId: String) {
        this.matchId = matchId
        viewModelScope.launch {
            _uiState.value = HighlightDetailUiState.Loading
            try {
                matchRepository.getMatchById(matchId).collect { match ->
                    if (match == null) {
                        _uiState.value = HighlightDetailUiState.Error("Match not found")
                        return@collect
                    }
                    
                    // Extract clips from match JSON using HighlightClipExtractor
                    val clips = HighlightClipExtractor.extractHighlightClips(match)
                    
                    _uiState.value = if (clips.isEmpty()) {
                        HighlightDetailUiState.Empty
                    } else {
                        HighlightDetailUiState.Success(clips)
                    }
                }
            } catch (e: Exception) {
                _uiState.value = HighlightDetailUiState.Error(
                    e.message ?: "Failed to load highlights"
                )
            }
        }
    }
    
    fun previousClip() {
        val current = _currentClipIndex.value
        if (current > 0) {
            _currentClipIndex.value = current - 1
        }
    }
    
    fun nextClip() {
        val state = _uiState.value
        if (state is HighlightDetailUiState.Success) {
            val current = _currentClipIndex.value
            if (current < state.clips.size - 1) {
                _currentClipIndex.value = current + 1
            }
        }
    }
    
    fun selectClip(index: Int) {
        val state = _uiState.value
        if (state is HighlightDetailUiState.Success && index in state.clips.indices) {
            _currentClipIndex.value = index
        }
    }
    
    fun retry() {
        if (matchId.isNotEmpty()) {
            loadHighlights(matchId)
        }
    }
}

/**
 * UI state for Highlight Detail Screen.
 */
sealed class HighlightDetailUiState {
    object Loading : HighlightDetailUiState()
    object Empty : HighlightDetailUiState()
    data class Success(val clips: List<HighlightClip>) : HighlightDetailUiState()
    data class Error(val message: String) : HighlightDetailUiState()
}
