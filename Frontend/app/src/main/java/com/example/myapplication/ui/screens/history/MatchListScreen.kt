package com.example.myapplication.ui.screens.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.myapplication.data.model.MatchStatus
import com.example.myapplication.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchListScreen(
    viewModel: MatchesViewModel,
    onNavigateBack: () -> Unit = {},
    onNavigateToUpload: () -> Unit = {},
    onNavigateToHighlights: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onMatchClick: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            HistoryTopBar(onNavigateBack = onNavigateBack)
        },
        bottomBar = {
            BottomNavigationBar(
                selectedTab = 1,
                onNavigateToUpload = onNavigateToUpload,
                onNavigateToHistory = {},
                onNavigateToHighlights = onNavigateToHighlights,
                onNavigateToProfile = onNavigateToProfile
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (uiState) {
                is MatchListUiState.Loading -> {
                    LoadingState()
                }
                is MatchListUiState.Success -> {
                    MatchListContent(
                        matches = (uiState as MatchListUiState.Success).matches,
                        onMatchClick = onMatchClick,
                        onRefresh = { viewModel.refreshMatches() },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                is MatchListUiState.Empty -> {
                    EmptyState()
                }
                is MatchListUiState.Error -> {
                    ErrorState(
                        message = (uiState as MatchListUiState.Error).message,
                        onRetry = { viewModel.refreshMatches() }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryTopBar(
    onNavigateBack: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = "History",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
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
                    tint = Color(0xFF52525B) // zinc-700 dark:zinc-300
                )
            }
        },
        actions = {
            // Empty spacer to balance the title centering
            Spacer(modifier = Modifier.width(48.dp))
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.8f),
            titleContentColor = Color.White,
        ),
        modifier = Modifier.border(
            width = 1.dp,
            color = Color(0x4D13A4EC),
            shape = RoundedCornerShape(0.dp)
        )
    )
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun MatchListContent(
    matches: List<MatchItem>,
    onMatchClick: (String) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = {
            scope.launch {
                isRefreshing = true
                onRefresh()
                kotlinx.coroutines.delay(1000) // Small delay to show indicator
                isRefreshing = false
            }
        }
    )

    Box(
        modifier = modifier.pullRefresh(pullRefreshState)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            items(matches) { match ->
                MatchListItem(
                    match = match,
                    onMatchClick = { onMatchClick(match.id) }
                )
                HorizontalDivider(
                    color = Color(0x4D13A4EC),
                    thickness = 1.dp
                )
            }
        }

        PullRefreshIndicator(
            refreshing = isRefreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
            backgroundColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun MatchListItem(
    match: MatchItem,
    onMatchClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onMatchClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail
        AsyncImage(
            model = match.thumbnailUrl,
            contentDescription = "Match thumbnail",
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )

        // Match info
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${match.player1} vs ${match.player2}",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White,
                    modifier = Modifier.weight(1f, fill = false)
                )
                // Status badge
                StatusBadge(status = match.status)
            }
            Text(
                text = "Score: ${match.score1} - ${match.score2}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.6f)
            )
        }

        // View button
        Button(
            onClick = onMatchClick,
            modifier = Modifier.height(40.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            shape = RoundedCornerShape(20.dp),
            contentPadding = PaddingValues(horizontal = 20.dp)
        ) {
            Text(
                text = "View",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }
}

@Composable
private fun StatusBadge(status: MatchStatus) {
    val (backgroundColor, textColor, label, showAnimation) = when (status) {
        MatchStatus.UPLOADED -> Quadruple(
            Color(0xFF3B82F6), // Blue
            Color.White,
            "UPLOADED",
            false
        )
        MatchStatus.PROCESSING -> Quadruple(
            Color(0xFFF59E0B), // Orange
            Color.White,
            "PROCESSING",
            true
        )
        MatchStatus.COMPLETE -> Quadruple(
            Color(0xFF10B981), // Green
            Color.White,
            "COMPLETE",
            false
        )
        MatchStatus.FAILED -> Quadruple(
            Color(0xFFEF4444), // Red
            Color.White,
            "FAILED",
            false
        )
    }

    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showAnimation) {
                CircularProgressIndicator(
                    modifier = Modifier.size(12.dp),
                    color = textColor,
                    strokeWidth = 2.dp
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                ),
                color = textColor
            )
        }
    }
}

// Helper data class for status badge properties
private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)

@Composable
private fun BottomNavigationBar(
    selectedTab: Int,
    onNavigateToUpload: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToHighlights: () -> Unit,
    onNavigateToProfile: () -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.8f),
        modifier = Modifier
            .border(
                width = 1.dp,
                color = Color(0x4D13A4EC),
                shape = RoundedCornerShape(0.dp)
            )
    ) {
        NavigationBarItem(
            icon = {
                Icon(
                    imageVector = Icons.Default.Videocam,
                    contentDescription = "Upload"
                )
            },
            label = {
                Text(
                    text = "Upload",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 12.sp
                    )
                )
            },
            selected = selectedTab == 0,
            onClick = onNavigateToUpload,
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                unselectedIconColor = Color(0xFF71717A), // zinc-500 dark:zinc-400
                unselectedTextColor = Color(0xFF71717A),
                indicatorColor = Color.Transparent
            )
        )
        
        NavigationBarItem(
            icon = {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = "History"
                )
            },
            label = {
                Text(
                    text = "History",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 12.sp
                    )
                )
            },
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
            icon = {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Highlights"
                )
            },
            label = {
                Text(
                    text = "Highlights",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 12.sp
                    )
                )
            },
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
            icon = {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Profile"
                )
            },
            label = {
                Text(
                    text = "Profile",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = if (selectedTab == 3) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 12.sp
                    )
                )
            },
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
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary
        )
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.History,
                contentDescription = "No matches",
                modifier = Modifier.size(64.dp),
                tint = Color.White.copy(alpha = 0.4f)
            )
            Text(
                text = "No match history yet",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.6f)
            )
            Text(
                text = "Upload your first match to get started",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.4f),
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
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = "Error",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Text(
                text = "Failed to load matches",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.6f),
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
