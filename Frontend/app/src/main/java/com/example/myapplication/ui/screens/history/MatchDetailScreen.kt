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
import com.example.myapplication.data.model.Match
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
    viewModel: MatchDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentPositionMs by viewModel.currentPositionMs.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()

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
                    currentPositionMs = currentPositionMs,
                    isPlaying = isPlaying,
                    onPositionChange = { viewModel.updatePosition(it) },
                    onPlaybackStateChange = { viewModel.updatePlaybackState(it) },
                    getDetections = { viewModel.getDetectionsForPosition(state.match, currentPositionMs) },
                    getTrajectory = { viewModel.getTrajectoryForPosition(state.match, currentPositionMs) },
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
    currentPositionMs: Long,
    isPlaying: Boolean,
    onPositionChange: (Long) -> Unit,
    onPlaybackStateChange: (Boolean) -> Unit,
    getDetections: () -> List<DetectionWithType>,
    getTrajectory: () -> List<List<Double>>?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Video Player with Overlay
        VideoPlayerWithOverlay(
            match = match,
            currentPositionMs = currentPositionMs,
            isPlaying = isPlaying,
            onPositionChange = onPositionChange,
            onPlaybackStateChange = onPlaybackStateChange,
            detections = getDetections(),
            trajectory = getTrajectory()
        )

        // Match Summary
        MatchSummarySection(match = match)

        // Performance Metrics
        PerformanceMetricsSection(match = match)
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
private fun PerformanceMetricsSection(match: Match) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Performance Metrics",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            ),
            color = Color.White
        )
        
        // Statistics Cards
        StatsCard(
            title = "Max Ball Speed",
            value = "${match.statistics?.maxBallSpeed ?: 0} km/h",
            subtitle = "Fastest shot"
        )

        StatsCard(
            title = "Avg Ball Speed",
            value = "${match.statistics?.avgBallSpeed ?: 0} km/h",
            subtitle = "Average speed"
        )

        StatsCard(
            title = "Rally Stats",
            value = "${match.statistics?.totalRallies ?: 0} rallies",
            subtitle = "Avg length: ${match.statistics?.avgRallyLength ?: 0}"
        )
    }
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
