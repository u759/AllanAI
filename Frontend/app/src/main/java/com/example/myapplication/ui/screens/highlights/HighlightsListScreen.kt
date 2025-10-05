package com.example.myapplication.ui.screens.highlights

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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

data class HighlightGroup(
    val title: String,
    val highlights: List<HighlightItem>
)

data class HighlightItem(
    val id: String,
    val title: String,
    val duration: String,
    val thumbnailUrl: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HighlightsListScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToUpload: () -> Unit = {},
    onNavigateToHistory: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onHighlightClick: (String) -> Unit = {}
) {
    // Mock data
    val highlightGroups = listOf(
        HighlightGroup(
            title = "Match vs. Alex - 23/10/2023",
            highlights = listOf(
                HighlightItem(
                    id = "1",
                    title = "Clutch Play",
                    duration = "0:45",
                    thumbnailUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuA58uYUU3Kl5b6NormCVtZruihKIOljtFsMTGHHO_o6Fhmu_LK-1qJWvMkem0FA8m36Unh52J8FCYoX4zh74w7mwqzsvTTDDL7gPrFyxdMYxO93p73sviLQlbLw5fyFnwupIb6N9WOnuiVeepTfzG_AG2buTjscEXzm-L1Jhz4_1EmhKjn49i8tddrrnmph-OvE0bTTulZ6Mgh1t-1mgM3xJc88bmP4EM3aSlv6FhWgQG2McIVDGAlALOrXGggTcKolx7CPxrYqN-Lu"
                ),
                HighlightItem(
                    id = "2",
                    title = "Epic Comeback",
                    duration = "1:12",
                    thumbnailUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuAmDo-Wzk6mZZFrfOvkzJubkz1n-g9yuDl4bSzYM_5AL0viKTvHFs5fywIbTxitIa62PwrODu4W27dX4X5g3UubeJnFrTwLVimvQLU9hL2j2qtxHVsH_E98RBC0SMkCJmyThlJ2i1ROSdPpRRdmCx1kPzYL0IF40VFzKDjiu-1OzOBbDvrSekWHHm5FNaiYLH9DtUOXkJbYp6p1x9a4AfOgmRwdExKEsilhS12l_wTEKpCtd0MBUBvXMpBHKBmgWpiYdGs3gei9VIxZ"
                )
            )
        ),
        HighlightGroup(
            title = "Training Session - 22/10/2023",
            highlights = listOf(
                HighlightItem(
                    id = "3",
                    title = "Forehand Drills",
                    duration = "2:33",
                    thumbnailUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuCnc8ZOIEm4toCUsiJsPvpjHSRdoMdHI5OvBxyUoS5c4Z_YVoqk52tJHgxswQwzuNEubGyB_ZQJttkkqrWGnq_gHx8s36SDMy_2nOTz3-1JK2_7ptbg_B00xdFO8Sew0fUZN4vozQuQBviJYGBOuK3dfw42Zg1rWUl9-eIuPWDCi6U_dnM1Gs82LNrJXGu3FuYe_u9OmY0Jw0MofVdI498j3Tsw70K4PerH6o7Vc5hqpLmsQvTiCUHQbqEmxQQ8Cz-8x3sYmvgKq_0U"
                ),
                HighlightItem(
                    id = "4",
                    title = "Serve Practice",
                    duration = "0:58",
                    thumbnailUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuCMLtWWa8w8hSnqAhv_nGg137jSlYnZVrOVSJR2B3xNgQEwYPmnQ6nMAJhLXmVj1nRkrhcjN4CXmFjzkqlxgDH0CXTlqUxA32yoSKz8wyIhh7tGqjL2WiAfoOJlHd7VlvhnBLpUap9wXTy9AhqzPmz9CE5oU8anCxuYqaaJDCD41HVJkls_2-jUkxKBK_d5Kipy-G-6__9fbafYqVauKIDCaZt5Bw6icpiL9tPziQk2Uvtlz36EMqwsvJaSdmtCVZyia265uFx7zoiv"
                )
            )
        )
    )

    Scaffold(
        topBar = {
            HighlightsTopBar(onNavigateBack = onNavigateBack)
        },
        bottomBar = {
            HighlightsBottomNav(
                selectedTab = 2,
                onNavigateToUpload = onNavigateToUpload,
                onNavigateToHistory = onNavigateToHistory,
                onNavigateToHighlights = {},
                onNavigateToProfile = onNavigateToProfile
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        HighlightsContent(
            highlightGroups = highlightGroups,
            onHighlightClick = onHighlightClick,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HighlightsTopBar(
    onNavigateBack: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = "Highlights",
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
            Spacer(modifier = Modifier.width(48.dp))
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.8f),
            titleContentColor = Color.White,
        ),
        modifier = Modifier.border(
            width = 1.dp,
            color = Color(0x1A9CA3AF),
            shape = RoundedCornerShape(0.dp)
        )
    )
}

@Composable
private fun HighlightsContent(
    highlightGroups: List<HighlightGroup>,
    onHighlightClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .padding(bottom = 16.dp),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(highlightGroups) { group ->
            HighlightGroupSection(
                group = group,
                onHighlightClick = onHighlightClick
            )
        }
    }
}

@Composable
private fun HighlightGroupSection(
    group: HighlightGroup,
    onHighlightClick: (String) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = group.title,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            ),
            color = Color.White
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            group.highlights.forEach { highlight ->
                HighlightCard(
                    highlight = highlight,
                    onClick = { onHighlightClick(highlight.id) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun HighlightCard(
    highlight: HighlightItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(160.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
    ) {
        // Thumbnail image
        AsyncImage(
            model = highlight.thumbnailUrl,
            contentDescription = highlight.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        
        // Dark overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f))
        )
        
        // Play button
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Play",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }
        
        // Title and duration overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomStart)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.7f)
                        )
                    )
                )
                .padding(8.dp)
        ) {
            Column {
                Text(
                    text = highlight.title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White
                )
                Text(
                    text = highlight.duration,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun HighlightsBottomNav(
    selectedTab: Int,
    onNavigateToUpload: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToHighlights: () -> Unit,
    onNavigateToProfile: () -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.8f),
        modifier = Modifier
            .border(1.dp, Color(0x4D13A4EC), RoundedCornerShape(0.dp))
            .windowInsetsPadding(        // keep the bar above the gesture area
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
    onClick: () -> Unit,
    highlightWhenSelected: Boolean = false
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .height(64.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
            if (highlightWhenSelected && selected) {
                Box(
                    Modifier
                        .size(40.dp)
                        .background(Color(0x4D13A4EC), CircleShape)
                )
            }
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) MaterialTheme.colorScheme.primary else Color(0xFF71717A),
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(Modifier.height(2.dp))
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
fun HighlightsListScreenPreview() {
    MyApplicationTheme {
        HighlightsListScreen()
    }
}
