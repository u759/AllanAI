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
import androidx.compose.runtime.Composable
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
import coil.compose.AsyncImage
import com.example.myapplication.ui.theme.MyApplicationTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchDetailScreen(
    matchId: String = "1",
    onNavigateBack: () -> Unit = {},
    onNavigateToUpload: () -> Unit = {},
    onNavigateToHistory: () -> Unit = {},
    onNavigateToHighlights: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {}
) {
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
        MatchDetailContent(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        )
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
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Video Player
        VideoPlayerSection()
        
        // Match Summary
        MatchSummarySection()
        
        // Performance Metrics
        PerformanceMetricsSection()
    }
}

@Composable
private fun VideoPlayerSection() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF111827))
    ) {
        // Video poster/thumbnail
        AsyncImage(
            model = "https://lh3.googleusercontent.com/aida-public/AB6AXuDC58P__OyJuDMKuqMPuJtwv2WklBoXFBI7AK92FrxFePIMDOuICmJnuAPADkM6CfyzwT-5CrElS00wL6yVi9GBwi3NWlZTF2KyBD4n1IRdKCkhPZRkjitub7s4FAy44ZsEa9o3qkMUhD1oS0hvrw53j5-1_jVhnlmYWjBCfFOZAzaZV10qd97RKh3gIXU2Cs2WqFtkpJajG0muE110lnBkTrCKsO_db4bm3As8816pHjO3XcwgZ1VFLpJSvuD_5Ru3Ebc0oBTWNYK1",
            contentDescription = "Match video",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        
        // Overlay gradient and play button
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
        ) {
            // Play button
            IconButton(
                onClick = { /* Play video */ },
                modifier = Modifier
                    .size(64.dp)
                    .align(Alignment.Center)
                    .background(
                        color = Color.White.copy(alpha = 0.2f),
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
            
            // Video controls at bottom
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.6f)
                            )
                        )
                    )
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "1:34 / 28:15",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = Color.White
                    )
                    Icon(
                        imageVector = Icons.Default.Fullscreen,
                        contentDescription = "Fullscreen",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MatchSummarySection() {
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
            MatchDetailRow("Date", "July 22, 2024")
            MatchDetailRow("Duration", "45 minutes")
            MatchDetailRow("Winner", "Ethan")
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
private fun PerformanceMetricsSection() {
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
        
        // Score Progression Card
        ScoreProgressionCard()
        
        // Serve Success Rate Card
        ServeSuccessCard()
        
        // Shot Types Card
        ShotTypesCard()
    }
}

@Composable
private fun ScoreProgressionCard() {
    MetricCard {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Score Progression",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = Color.White
            )
            Text(
                text = "11-9",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 30.sp
                ),
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Final Score",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF9CA3AF)
            )
            
            // Simple chart placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(192.dp)
                    .padding(vertical = 16.dp)
            ) {
                // Chart gradient area
                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // This would be a proper chart library in production
                    // For now, showing a simplified representation
                }
                
                Text(
                    text = "Chart visualization would go here",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF9CA3AF),
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            
            // X-axis labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                (1..7).forEach { num ->
                    Text(
                        text = num.toString(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color(0xFF6B7280)
                    )
                }
            }
        }
    }
}

@Composable
private fun ServeSuccessCard() {
    MetricCard {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Serve Success Rate",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = Color.White
            )
            Text(
                text = "75%",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 30.sp
                ),
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Ethan",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF9CA3AF),
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            // Bar chart
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(192.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                // Success bar
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.5f)
                            .fillMaxHeight(0.75f)
                            .background(
                                color = Color(0x4D13A4EC),
                                shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                            )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Success",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color(0xFF6B7280)
                    )
                }
                
                // Failure bar
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.5f)
                            .fillMaxHeight(0.25f)
                            .background(
                                color = Color(0x4D13A4EC),
                                shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                            )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Failure",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color(0xFF6B7280)
                    )
                }
            }
        }
    }
}

@Composable
private fun ShotTypesCard() {
    MetricCard {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Shot Types",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = Color.White
            )
            Text(
                text = "40% Forehand",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 30.sp
                ),
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Ethan",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF9CA3AF),
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            // Progress bars
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ShotTypeProgressBar("Forehand", 0.4f)
                ShotTypeProgressBar("Backhand", 0.35f)
                ShotTypeProgressBar("Smash", 0.25f)
            }
        }
    }
}

@Composable
private fun ShotTypeProgressBar(label: String, progress: Float) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold
            ),
            color = Color(0xFF6B7280),
            modifier = Modifier.width(64.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(Color(0xFF374151))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(5.dp))
                    .background(MaterialTheme.colorScheme.primary)
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
    Surface(
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.9f),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = Color(0x1F9CA3AF),
                shape = RoundedCornerShape(0.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            BottomNavItem(
                icon = Icons.Default.Videocam,
                label = "Upload",
                selected = selectedTab == 0,
                onClick = onNavigateToUpload
            )
            
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                // Special styling for selected History tab
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .offset(y = (-16).dp)
                        .background(
                            color = Color(0x4D13A4EC),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "History",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "History",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            
            BottomNavItem(
                icon = Icons.Default.Star,
                label = "Highlights",
                selected = selectedTab == 2,
                onClick = onNavigateToHighlights
            )
            
            BottomNavItem(
                icon = Icons.Default.Person,
                label = "Profile",
                selected = selectedTab == 3,
                onClick = onNavigateToProfile
            )
        }
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

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun MatchDetailScreenPreview() {
    MyApplicationTheme {
        MatchDetailScreen()
    }
}
