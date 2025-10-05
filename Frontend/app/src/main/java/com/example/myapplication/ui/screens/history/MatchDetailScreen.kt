package com.example.myapplication.ui.screens.history

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.myapplication.data.model.EventType
import com.example.myapplication.data.model.Match
import com.example.myapplication.data.model.ShotResult
import com.example.myapplication.data.model.ShotType
import com.example.myapplication.ui.theme.MyApplicationTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchDetailScreen(
    matchId: String = "0ee996ad-3f02-4ba9-bafd-465a6a6dcf10",
    onNavigateBack: () -> Unit = {},
    onNavigateToUpload: () -> Unit = {},
    onNavigateToHistory: () -> Unit = {},
    onNavigateToHighlights: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    viewModel: MatchDetailViewModel = hiltViewModel(),
    authViewModel: com.example.myapplication.ui.screens.profile.AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentPositionMs by viewModel.currentPositionMs.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val videoUrl by viewModel.videoUrl.collectAsState()
    val authToken = authViewModel.authRepository.getToken()

    // Load match when screen opens
    LaunchedEffect(matchId) {
        viewModel.loadMatch(matchId)
    }

    Scaffold(
        topBar = {
            MatchDetailTopBar(onNavigateBack = onNavigateBack)
        },
        bottomBar = {
            MatchDetailBottomNav(
                selectedTab = 1,
                onNavigateToUpload = onNavigateToUpload,
                onNavigateToHistory = onNavigateToHistory,
                onNavigateToHighlights = onNavigateToHighlights,
                onNavigateToProfile = onNavigateToProfile
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        when (val state = uiState) {
            is MatchDetailUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is MatchDetailUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = state.message,
                        color = Color.Red,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
            is MatchDetailUiState.Success -> {
                MatchDetailContent(
                    match = state.match,
                    videoUrl = videoUrl,
                    authToken = authToken,
                    currentPositionMs = currentPositionMs,
                    isPlaying = isPlaying,
                    onPositionChange = { viewModel.updatePosition(it) },
                    onPlaybackStateChange = { viewModel.updatePlaybackState(it) },
                    getDetections = { viewModel.getDetectionsForPosition(state.match, currentPositionMs) },
                    getTrajectory = { viewModel.getTrajectoryForPosition(state.match, currentPositionMs) },
                    viewModel = viewModel,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MatchDetailTopBar(
    onNavigateBack: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = "Match Analysis",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                ),
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
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
            Spacer(modifier = Modifier.width(40.dp))
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.8f),
            titleContentColor = Color.White,
        ),
        modifier = Modifier.border(
            width = 1.dp,
            color = Color(0x1F9CA3AF),
            shape = RoundedCornerShape(0.dp)
        )
    )
}

@Composable
private fun MatchDetailContent(
    match: Match,
    videoUrl: String?,
    authToken: String?,
    currentPositionMs: Long,
    isPlaying: Boolean,
    onPositionChange: (Long) -> Unit,
    onPlaybackStateChange: (Boolean) -> Unit,
    getDetections: () -> List<DetectionWithType>,
    getTrajectory: () -> List<List<Double>>?,
    viewModel: MatchDetailViewModel,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Video Player with Overlay or Loading State
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF111827))
        ) {
            VideoPlayerWithOverlay(
                match = match,
                videoUrl = videoUrl,
                authToken = authToken,
                currentPositionMs = currentPositionMs,
                isPlaying = isPlaying,
                onPositionChange = onPositionChange,
                onPlaybackStateChange = onPlaybackStateChange,
                detections = getDetections(),
                trajectory = getTrajectory()
            )

            // Show loading indicator if video URL is not loaded yet
            if (videoUrl == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.7f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Loading video...",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        // Match Summary
        MatchSummarySection(match = match)

        // Performance Metrics
        PerformanceMetricsSection(
            match = match,
            currentPositionMs = currentPositionMs,
            viewModel = viewModel
        )

        // Shot Statistics
        if (match.shots.isNotEmpty()) {
            ShotStatisticsSection(match = match)
        }

        // Events Summary
        if (match.events.isNotEmpty()) {
            EventsSummarySection(match = match)
        }

        // Player Comparison
        if (match.shots.isNotEmpty()) {
            PlayerComparisonSection(match = match)
        }
    }
}

@Composable
private fun MatchSummarySection(match: Match) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Match Summary",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            ),
            color = Color.White
        )
        
        // Match info card
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = Color(0x1F9CA3AF),
                    shape = RoundedCornerShape(8.dp)
                )
                .background(
                    color = MaterialTheme.colorScheme.background,
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = "https://lh3.googleusercontent.com/aida-public/AB6AXuDC58P__OyJuDMKuqMPuJtwv2WklBoXFBI7AK92FrxFePIMDOuICmJnuAPADkM6CfyzwT-5CrElS00wL6yVi9GBwi3NWlZTF2KyBD4n1IRdKCkhPZRkjitub7s4FAy44ZsEa9o3qkMUhD1oS0hvrw53j5-1_jVhnlmYWjBCfFOZAzaZV10qd97RKh3gIXU2Cs2WqFtkpJajG0muE110lnBkTrCKsO_db4bm3As8816pHjO3XcwgZ1VFLpJSvuD_5Ru3Ebc0oBTWNYK1",
                contentDescription = "Player avatar",
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Ethan's Match",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White
                )
                Text(
                    text = "vs. Alex",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF9CA3AF)
                )
            }
        }
        
        // Match details
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MatchDetailRow("Date", match.createdAt.toString().substringBefore("T"))
            MatchDetailRow("Duration", formatDuration(match.durationSeconds ?: 0))
            MatchDetailRow("Score", "${match.statistics?.player1Score ?: 0} - ${match.statistics?.player2Score ?: 0}")
        }
    }
}

@Composable
private fun MatchDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF9CA3AF)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Medium
            ),
            color = Color(0xFFE5E7EB)
        )
    }
}

@Composable
private fun PerformanceMetricsSection(
    match: Match,
    currentPositionMs: Long,
    viewModel: MatchDetailViewModel
) {
    val liveStats by viewModel.liveStats.collectAsStateWithLifecycle()
    val currentShot by viewModel.currentShot.collectAsStateWithLifecycle()
    val currentScore by viewModel.currentScore.collectAsStateWithLifecycle()
    val stats = match.statistics
    
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Live Performance Metrics",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            ),
            color = Color.White
        )
        
        // Live Score
        currentScore?.let { score ->
            StatsCard(
                title = "Current Score",
                value = "${score.player1} - ${score.player2}",
                subtitle = "Player 1 vs Player 2"
            )
        }
        
        // Current Shot Details (Real-time)
        if (currentShot != null) {
            StatsCard(
                title = "Current Shot",
                value = "${String.format("%.1f", currentShot?.speed ?: 0.0)} km/h",
                subtitle = "${currentShot?.shotType?.name ?: "Unknown"} • ${String.format("%.0f", currentShot?.accuracy ?: 0.0)}% accuracy • ${currentShot?.result?.name ?: "Unknown"}"
            )
        }
        
        // Rally Metrics (from model stats)
        stats?.rallyMetrics?.let { rally ->
            StatsCard(
                title = "Rally Statistics",
                value = "${rally.totalRallies ?: 0} rallies",
                subtitle = "Avg: ${String.format("%.1f", rally.averageRallyLength ?: 0.0)} shots • Longest: ${rally.longestRallyLength ?: 0} shots"
            )
            
            if (rally.averageRallyDurationSeconds != null) {
                StatsCard(
                    title = "Rally Duration",
                    value = "${String.format("%.1f", rally.averageRallyDurationSeconds)}s avg",
                    subtitle = "Longest: ${String.format("%.1f", rally.longestRallyDurationSeconds ?: 0.0)}s"
                )
            }
        }
        
        // Speed Metrics (from model stats)
        stats?.shotSpeedMetrics?.let { speed ->
            StatsCard(
                title = "Shot Speed Analysis",
                value = "${String.format("%.1f", speed.fastestShotMph ?: 0.0)} mph",
                subtitle = "Fastest shot recorded"
            )
            
            StatsCard(
                title = "Average Speed",
                value = "${String.format("%.1f", speed.averageShotMph ?: 0.0)} mph",
                subtitle = "Incoming: ${String.format("%.1f", speed.averageIncomingShotMph ?: 0.0)} • Outgoing: ${String.format("%.1f", speed.averageOutgoingShotMph ?: 0.0)} mph"
            )
        }
        
        // Serve Metrics (from model stats)
        stats?.serveMetrics?.let { serve ->
            StatsCard(
                title = "Serve Performance",
                value = "${String.format("%.1f", serve.successRate ?: 0.0)}%",
                subtitle = "${serve.successfulServes ?: 0}/${serve.totalServes ?: 0} successful • ${serve.faults ?: 0} faults"
            )
            
            if (serve.averageServeSpeed != null) {
                StatsCard(
                    title = "Serve Speed",
                    value = "${String.format("%.1f", serve.averageServeSpeed)} avg",
                    subtitle = "Fastest: ${String.format("%.1f", serve.fastestServeSpeed ?: 0.0)}"
                )
            }
        }
        
        // Return Metrics (from model stats)
        stats?.returnMetrics?.let { returns ->
            StatsCard(
                title = "Return Performance",
                value = "${String.format("%.1f", returns.successRate ?: 0.0)}%",
                subtitle = "${returns.successfulReturns ?: 0}/${returns.totalReturns ?: 0} successful returns"
            )
            
            if (returns.averageReturnSpeed != null) {
                StatsCard(
                    title = "Return Speed",
                    value = "${String.format("%.1f", returns.averageReturnSpeed)} avg",
                    subtitle = "Average return shot speed"
                )
            }
        }
        
        // Shot Type Breakdown
        stats?.shotTypeBreakdown?.let { breakdown ->
            if (breakdown.isNotEmpty()) {
                Text(
                    text = "Shot Type Breakdown",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    ),
                    color = Color.White,
                    modifier = Modifier.padding(top = 8.dp)
                )
                
                breakdown.forEach { shotType ->
                    StatsCard(
                        title = shotType.shotType,
                        value = "${shotType.count} shots",
                        subtitle = "Avg speed: ${String.format("%.1f", shotType.averageSpeed ?: 0.0)} • Accuracy: ${String.format("%.1f", shotType.averageAccuracy ?: 0.0)}%"
                    )
                }
            }
        }
        
        // Player Breakdown
        stats?.playerBreakdown?.let { players ->
            if (players.isNotEmpty()) {
                Text(
                    text = "Player Performance",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    ),
                    color = Color.White,
                    modifier = Modifier.padding(top = 8.dp)
                )
                
                players.forEach { player ->
                    StatsCard(
                        title = "Player ${player.player}",
                        value = "${player.totalPointsWon ?: 0} points",
                        subtitle = "${player.totalShots ?: 0} shots • ${String.format("%.1f", player.averageShotSpeed ?: 0.0)} avg speed • ${String.format("%.1f", player.averageAccuracy ?: 0.0)}% accuracy"
                    )
                }
            }
        }
        
        // Live Stats (Real-time up to current position)
        liveStats?.let { live ->
            Text(
                text = "Live Statistics (Up to Current Time)",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                ),
                color = Color.White,
                modifier = Modifier.padding(top = 8.dp)
            )
            
            StatsCard(
                title = "Shots Played",
                value = "${live.totalShots} total",
                subtitle = "Player 1: ${live.player1Shots} • Player 2: ${live.player2Shots}"
            )
            
            StatsCard(
                title = "Speed (So Far)",
                value = "${String.format("%.1f", live.maxSpeed)} km/h max",
                subtitle = "Average: ${String.format("%.1f", live.avgSpeed)} km/h"
            )
            
            StatsCard(
                title = "Rallies Completed",
                value = "${live.ralliesCompleted}",
                subtitle = "Up to ${formatTimestamp(currentPositionMs)}"
            )
        }
    }
}

private fun formatTimestamp(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}

@Composable
private fun StatsCard(
    title: String,
    value: String,
    subtitle: String
) {
    MetricCard {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = Color.White
            )
            Text(
                text = value,
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 30.sp
                ),
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF9CA3AF)
            )
        }
    }
}

@Composable
private fun MetricCard(
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = Color(0x1F9CA3AF),
                shape = RoundedCornerShape(8.dp)
            )
            .background(
                color = MaterialTheme.colorScheme.background,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(16.dp)
    ) {
        content()
    }
}

@Composable
private fun MatchDetailBottomNav(
    selectedTab: Int,
    onNavigateToUpload: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToHighlights: () -> Unit,
    onNavigateToProfile: () -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.8f),
        modifier = Modifier
            .border(1.dp, Color(0x1F9CA3AF), RoundedCornerShape(0.dp))
            .windowInsetsPadding( // keeps it above the gesture area
                WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)
            )
    ) {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Videocam, null) },
            label = { Text("Upload", style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp)) },
            selected = selectedTab == 0,
            onClick = onNavigateToUpload,
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                unselectedIconColor = Color(0xFF71717A),
                unselectedTextColor = Color(0xFF71717A),
                indicatorColor = Color.Transparent
            )
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.History, null) },
            label = { Text("History", style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp)) },
            selected = selectedTab == 1,
            onClick = onNavigateToHistory,
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                unselectedIconColor = Color(0xFF71717A),
                unselectedTextColor = Color(0xFF71717A),
                indicatorColor = Color.Transparent
            )
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Star, null) },
            label = { Text("Highlights", style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp)) },
            selected = selectedTab == 2,
            onClick = onNavigateToHighlights,
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                unselectedIconColor = Color(0xFF71717A),
                unselectedTextColor = Color(0xFF71717A),
                indicatorColor = Color.Transparent
            )
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Person, null) },
            label = { Text("Profile", style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp)) },
            selected = selectedTab == 3,
            onClick = onNavigateToProfile,
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                unselectedIconColor = Color(0xFF71717A),
                unselectedTextColor = Color(0xFF71717A),
                indicatorColor = Color.Transparent
            )
        )
    }
}


@Composable
private fun RowScope.BottomNavItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        IconButton(onClick = onClick) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) MaterialTheme.colorScheme.primary else Color(0xFF71717A),
                modifier = Modifier.size(24.dp)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 12.sp
            ),
            color = if (selected) MaterialTheme.colorScheme.primary else Color(0xFF71717A)
        )
    }
}

/**
 * Shot Statistics Section - Displays detailed shot metrics
 */
@Composable
private fun ShotStatisticsSection(match: Match) {
    val stats = match.statistics

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Shot Statistics",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            ),
            color = Color.White
        )

        // Use backend shotTypeBreakdown if available, otherwise calculate from shots
        val shotBreakdown = stats?.shotTypeBreakdown
        if (shotBreakdown != null && shotBreakdown.isNotEmpty()) {
            val totalShots = shotBreakdown.sumOf { it.count }

            MetricCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Shot Breakdown",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White
                    )

                    shotBreakdown.forEach { shotType ->
                        ShotTypeRow(shotType.shotType, shotType.count, totalShots, shotType.averageSpeed)
                    }
                }
            }
        } else {
            // Fallback to client-side calculation
            val totalShots = match.shots.size
            val serves = match.shots.count { it.shotType == ShotType.SERVE }
            val forehands = match.shots.count { it.shotType == ShotType.FOREHAND }
            val backhands = match.shots.count { it.shotType == ShotType.BACKHAND }
            val successfulShots = match.shots.count { it.result == ShotResult.IN }
            val successRate = if (totalShots > 0) (successfulShots.toDouble() / totalShots * 100) else 0.0

            MetricCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Shot Breakdown",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White
                    )

                    ShotTypeRow("Serves", serves, totalShots, null)
                    ShotTypeRow("Forehands", forehands, totalShots, null)
                    ShotTypeRow("Backhands", backhands, totalShots, null)

                    HorizontalDivider(color = Color(0x1FFFFFFF), thickness = 1.dp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Success Rate",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF9CA3AF)
                        )
                        Text(
                            text = "${successRate.toInt()}%",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Display Serve Metrics if available
        stats?.serveMetrics?.let { serveMetrics ->
            MetricCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Serve Statistics",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White
                    )

                    serveMetrics.totalServes?.let {
                        StatRow("Total Serves", it.toString())
                    }
                    serveMetrics.successRate?.let {
                        StatRow("Success Rate", "${(it * 100).toInt()}%")
                    }
                    serveMetrics.averageServeSpeed?.let {
                        StatRow("Avg Speed", "%.1f mph".format(it))
                    }
                    serveMetrics.fastestServeSpeed?.let {
                        StatRow("Fastest", "%.1f mph".format(it))
                    }
                }
            }
        }

        // Display Return Metrics if available
        stats?.returnMetrics?.let { returnMetrics ->
            MetricCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Return Statistics",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White
                    )

                    returnMetrics.totalReturns?.let {
                        StatRow("Total Returns", it.toString())
                    }
                    returnMetrics.successRate?.let {
                        StatRow("Success Rate", "${(it * 100).toInt()}%")
                    }
                    returnMetrics.averageReturnSpeed?.let {
                        StatRow("Avg Speed", "%.1f mph".format(it))
                    }
                }
            }
        }
    }
}

@Composable
private fun ShotTypeRow(label: String, count: Int, total: Int, avgSpeed: Double? = null) {
    val percentage = if (total > 0) (count.toDouble() / total * 100).toInt() else 0

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF9CA3AF)
            )
            avgSpeed?.let {
                Text(
                    text = "Avg: %.1f mph".format(it),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF6B7280)
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$count",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = Color.White
            )
            Text(
                text = "($percentage%)",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF9CA3AF)
            )
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF9CA3AF)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold
            ),
            color = Color.White
        )
    }
}

/**
 * Events Summary Section - Displays event counts and types
 */
@Composable
private fun EventsSummarySection(match: Match) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Events",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            ),
            color = Color.White
        )

        val eventCounts = match.events.groupingBy { it.type }.eachCount()

        MetricCard {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                eventCounts.forEach { (type, count) ->
                    EventTypeRow(type = type, count = count)
                }

                if (eventCounts.isEmpty()) {
                    Text(
                        text = "No events detected",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF9CA3AF),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EventTypeRow(type: EventType, count: Int) {
    val (emoji, label, color) = when (type) {
        EventType.PLAY_OF_THE_GAME -> Triple("⭐", "Play of the Game", Color(0xFFFFD700))
        EventType.SCORE -> Triple("🎯", "Score", Color(0xFF4CAF50))
        EventType.MISS -> Triple("❌", "Miss", Color(0xFFF44336))
        EventType.RALLY_HIGHLIGHT -> Triple("🏆", "Rally Highlight", Color(0xFFFF9800))
        EventType.SERVE_ACE -> Triple("⚡", "Serve Ace", Color(0xFF9C27B0))
        EventType.FASTEST_SHOT -> Triple("🚀", "Fastest Shot", Color(0xFFE91E63))
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = emoji,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White
            )
        }
        Text(
            text = "$count",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold
            ),
            color = color
        )
    }
}

/**
 * Player Comparison Section - Compares player performance
 */
@Composable
private fun PlayerComparisonSection(match: Match) {
    val stats = match.statistics

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Player Comparison",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            ),
            color = Color.White
        )

        // Use backend playerBreakdown if available
        val playerBreakdown = stats?.playerBreakdown
        if (playerBreakdown != null && playerBreakdown.size >= 2) {
            val player1Data = playerBreakdown.find { it.player == 1 }
            val player2Data = playerBreakdown.find { it.player == 2 }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PlayerStatsCard(
                    playerName = match.player1Name ?: "Player 1",
                    shots = player1Data?.totalShots ?: 0,
                    avgSpeed = player1Data?.averageShotSpeed ?: 0.0,
                    successRate = (player1Data?.averageAccuracy ?: 0.0) * 100,
                    score = stats.player1Score,
                    pointsWon = player1Data?.totalPointsWon,
                    serveSuccessRate = player1Data?.serveSuccessRate,
                    returnSuccessRate = player1Data?.returnSuccessRate,
                    modifier = Modifier.weight(1f)
                )

                PlayerStatsCard(
                    playerName = match.player2Name ?: "Player 2",
                    shots = player2Data?.totalShots ?: 0,
                    avgSpeed = player2Data?.averageShotSpeed ?: 0.0,
                    successRate = (player2Data?.averageAccuracy ?: 0.0) * 100,
                    score = stats.player2Score,
                    pointsWon = player2Data?.totalPointsWon,
                    serveSuccessRate = player2Data?.serveSuccessRate,
                    returnSuccessRate = player2Data?.returnSuccessRate,
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            // Fallback to client-side calculation
            val player1Shots = match.shots.filter { it.player == 1 }
            val player2Shots = match.shots.filter { it.player == 2 }

            val player1AvgSpeed = if (player1Shots.isNotEmpty())
                player1Shots.map { it.speed }.average() else 0.0
            val player2AvgSpeed = if (player2Shots.isNotEmpty())
                player2Shots.map { it.speed }.average() else 0.0

            val player1SuccessRate = if (player1Shots.isNotEmpty())
                (player1Shots.count { it.result == ShotResult.IN }.toDouble() / player1Shots.size * 100)
                else 0.0
            val player2SuccessRate = if (player2Shots.isNotEmpty())
                (player2Shots.count { it.result == ShotResult.IN }.toDouble() / player2Shots.size * 100)
                else 0.0

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PlayerStatsCard(
                    playerName = match.player1Name ?: "Player 1",
                    shots = player1Shots.size,
                    avgSpeed = player1AvgSpeed,
                    successRate = player1SuccessRate,
                    score = stats?.player1Score ?: 0,
                    pointsWon = null,
                    serveSuccessRate = null,
                    returnSuccessRate = null,
                    modifier = Modifier.weight(1f)
                )

                PlayerStatsCard(
                    playerName = match.player2Name ?: "Player 2",
                    shots = player2Shots.size,
                    avgSpeed = player2AvgSpeed,
                    successRate = player2SuccessRate,
                    score = stats?.player2Score ?: 0,
                    pointsWon = null,
                    serveSuccessRate = null,
                    returnSuccessRate = null,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun PlayerStatsCard(
    playerName: String,
    shots: Int,
    avgSpeed: Double,
    successRate: Double,
    score: Int,
    pointsWon: Int? = null,
    serveSuccessRate: Double? = null,
    returnSuccessRate: Double? = null,
    modifier: Modifier = Modifier
) {
    MetricCard() {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = playerName,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = Color.White,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Text(
                text = "$score",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 48.sp
                ),
                color = MaterialTheme.colorScheme.primary
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                PlayerStatRow("Shots", "$shots")
                PlayerStatRow("Avg Speed", "%.1f mph".format(avgSpeed))
                PlayerStatRow("Accuracy", "${successRate.toInt()}%")

                pointsWon?.let {
                    PlayerStatRow("Points Won", "$it")
                }
                serveSuccessRate?.let {
                    PlayerStatRow("Serve Rate", "${(it * 100).toInt()}%")
                }
                returnSuccessRate?.let {
                    PlayerStatRow("Return Rate", "${(it * 100).toInt()}%")
                }
            }
        }
    }
}

@Composable
private fun PlayerStatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF9CA3AF)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Medium
            ),
            color = Color.White
        )
    }
}

/**
 * Format duration in seconds to readable string.
 */
private fun formatDuration(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return String.format("%d:%02d", mins, secs)
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun MatchDetailScreenPreview() {
    MyApplicationTheme {
        MatchDetailScreen()
    }
}
