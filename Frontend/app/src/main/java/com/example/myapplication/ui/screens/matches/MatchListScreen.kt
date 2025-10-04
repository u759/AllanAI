package com.example.myapplication.ui.screens.matches

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.myapplication.ui.theme.MyApplicationTheme

data class MatchItem(
    val id: String,
    val player1: String,
    val player2: String,
    val score1: Int,
    val score2: Int,
    val thumbnailUrl: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchListScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToUpload: () -> Unit = {},
    onNavigateToHighlights: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onMatchClick: (String) -> Unit = {}
) {
    // Mock data - in production, this would come from ViewModel
    val matches = listOf(
        MatchItem(
            id = "1",
            player1 = "Player 1",
            player2 = "Player 2",
            score1 = 11,
            score2 = 5,
            thumbnailUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuCEN5R1Phl90yI9Mn33AQNkuFoXCS1W2lJZ36YfuQxf2worp_SsjWkJFZEBLpr4RopEyXVkYUK0LLQlCOaU9xk2ySdoa8aFNPu99inVhBG7SiHAKDWVUuPRrHgv1ST3-kIdd8ayCoIuSEQQ4tHSWqaAAPMLprC45Jp30L_JAJf1PumH9D5wVpn5biB38Wg_gcOh7S31X9J1Wu9uopydS6p7tf1MTAEGBeG8bPccRGicRapnD6eyxU1J-zsNtXN0G-faIOOgIk88YdzA"
        ),
        MatchItem(
            id = "2",
            player1 = "Player 1",
            player2 = "Player 2",
            score1 = 8,
            score2 = 11,
            thumbnailUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuA4Nc688oh5dDMLytkQcDbF2uRCSTqDXLto_X7-80U6Ok8ILSffy_tb2NnHZpUq6I_8pS-reEw2g8nfvv8UZ_8rt6Tm-CDb__-xD8b3uCzR9dVHi88qSUienv19yhJI_6g65MxWUUCQgI4xQ904pp7lqk0ZnkYpFd7EaCOTR_SipuCIsylnNPUvX_PDZa-PwrX4qAX_JQdFiNlkvBTfQAoJcwgvAlk8hA7z7faEet-OE6ivCOe6ZseZE_S7seOwpPEEWR7LJN78c3-1"
        ),
        MatchItem(
            id = "3",
            player1 = "Player 1",
            player2 = "Player 2",
            score1 = 11,
            score2 = 9,
            thumbnailUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuAwJ7L0JmuTZZX5hzpfSKsgwBLMKXv1NgHa45tRl7nDBWG-AHDBfSwRw-3xvS3EjqL5-AH_pEEsyJf1SqEx2UHEVLcuYQHBtkY5wmNOoJ7-jyBDRsetlNLY5UlPpp9eQtHgRa1HEbGWb5eHt8swPPNehW7xqqzgVDCLkpmE1Mf_tUDim9j3RN7xBZSZUi-6znWFi05-sNAzXC9v6FJ9HLjhJ23wlhFvoNaulKidv0fd2ddAUtPylHVxKyI-olIlT55dMpac1cIzNcQG"
        ),
        MatchItem(
            id = "4",
            player1 = "Player 1",
            player2 = "Player 2",
            score1 = 5,
            score2 = 11,
            thumbnailUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuBZohyCZrF9b38k7JzTItk74hjn7GzsBskmbjyNCpx8IedRihUTHSavV0o0w0NSfNwzXTCHY17VAmU-qVh_JcFbUXbFfh7ZLyBz-ssq4oatd-ELi0o3_wgX7blnH_lggjqxoGqOWKmFYELc-Jv77WpS8z2IQjCcrGxMwNXBZ66IuLub8dsfNkZ01yhcifJpq6b-xSUI8VuIPzZ4q-CX1gwl3lc1z0gZUthrF-OGDIr5rDsvXSbubbipS-bMoSqfQXJBqamLOfM8bqN2"
        ),
        MatchItem(
            id = "5",
            player1 = "Player 1",
            player2 = "Player 2",
            score1 = 11,
            score2 = 2,
            thumbnailUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuCxa5_9epXLMnNAdLs0YxXZtWCnVIi39tP_HXHgKUgDRMiQTEWFC0ze3gvQZXsl7gOI8t8vA3Yf4b8F6adMsmVvCB9Fq8ep5AaU5G63pUxowdo8a7WdbShPuR8XU-NEAEWDl3kMh_FytZtgT_H2qKQScOfofZmWcR0b9Qb_heLc_J_TOMBnZAmmybawZaOm4TebdCvb_X7AsLHgHZcap-MbShBBbWP1uRClcJ-KjQC3N6ElP41LcdvoOalOKNase1P8GqKl4UTeLEwS"
        )
    )

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
        MatchListContent(
            matches = matches,
            onMatchClick = onMatchClick,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        )
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

@Composable
private fun MatchListContent(
    matches: List<MatchItem>,
    onMatchClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
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
            Text(
                text = "${match.player1} vs ${match.player2}",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = Color.White
            )
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
                    imageVector = Icons.Default.Upload,
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
                    imageVector = Icons.Default.Movie,
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

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun MatchListScreenPreview() {
    MyApplicationTheme {
        MatchListScreen()
    }
}
