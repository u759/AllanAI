package com.example.myapplication.ui.screens.upload

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.myapplication.ui.theme.MyApplicationTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToHistory: () -> Unit = {},
    onNavigateToHighlights: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToRecord: () -> Unit = {},
    viewModel: UploadViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val showMetadataDialog by viewModel.showMetadataDialog.collectAsState()

    // File picker launcher
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.onVideoSelected(it, context)
        }
    }

    Scaffold(
        topBar = {
            UploadTopBar(onNavigateBack = onNavigateBack)
        },
        bottomBar = {
            BottomNavigationBar(
                selectedTab = 0,
                onNavigateToUpload = {},
                onNavigateToHistory = onNavigateToHistory,
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
            UploadContent(
                uiState = uiState,
                onUploadClick = {
                    videoPickerLauncher.launch("video/*")
                },
                onRecordClick = onNavigateToRecord,
                onResetClick = {
                    viewModel.resetState()
                },
                modifier = Modifier.fillMaxSize()
            )

            // Metadata Dialog
            if (showMetadataDialog) {
                MetadataInputDialog(
                    onDismiss = { viewModel.cancelMetadataDialog() },
                    onConfirm = { player1, player2, title ->
                        viewModel.uploadVideo(player1, player2, title, context)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UploadTopBar(
    onNavigateBack: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = "Upload",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBackIosNew,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
        },
        actions = {
            // Empty spacer to balance the title centering
            Spacer(modifier = Modifier.width(48.dp))
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
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
private fun UploadContent(
    uiState: UploadUiState,
    onUploadClick: () -> Unit,
    onRecordClick: () -> Unit,
    onResetClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        // Title section
        TitleSection()

        // Action buttons
        ActionButtons(
            onUploadClick = onUploadClick,
            onRecordClick = onRecordClick,
            enabled = uiState is UploadUiState.Idle
        )

        // State-based content
        when (uiState) {
            is UploadUiState.Idle -> {
                // Show instructions
                InstructionsCard()
            }
            is UploadUiState.ProcessingVideo -> {
                ProcessingCard("Processing video...")
            }
            is UploadUiState.WaitingForMetadata -> {
                ProcessingCard("Waiting for match details...")
            }
            is UploadUiState.Uploading -> {
                UploadingCard(uiState.progress)
            }
            is UploadUiState.Success -> {
                SuccessCard(
                    matchId = uiState.match.id,
                    onResetClick = onResetClick
                )
            }
            is UploadUiState.Error -> {
                ErrorCard(
                    message = uiState.message,
                    onResetClick = onResetClick
                )
            }
        }
    }
}

@Composable
private fun TitleSection() {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "Upload or record a video",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp
            ),
            color = Color.White
        )
        Text(
            text = "Choose a file from your device or record a new one.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun ActionButtons(
    onUploadClick: () -> Unit,
    onRecordClick: () -> Unit,
    enabled: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Upload button
        Button(
            onClick = onUploadClick,
            enabled = enabled,
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.UploadFile,
                contentDescription = "Upload",
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Upload",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold
                )
            )
        }

        // Record button
        Button(
            onClick = onRecordClick,
            enabled = enabled,
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0x4D13A4EC) // 30% opacity primary
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Videocam,
                contentDescription = "Record",
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Record",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

@Composable
private fun InstructionsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0x3313A4EC)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Instructions",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = Color.White
            )
            Text(
                text = "1. Select a video file or record a new one\n" +
                        "2. Enter player names and match title\n" +
                        "3. Upload and wait for processing\n" +
                        "4. View your match analysis in History",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun ProcessingCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0x3313A4EC)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White
            )
        }
    }
}

@Composable
private fun UploadingCard(progress: Float) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0x3313A4EC)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Uploading video...",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = Color.White
                )
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = Color(0x4D13A4EC)
            )
        }
    }
}

@Composable
private fun SuccessCard(
    matchId: String,
    onResetClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0x3313A4EC)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Upload successful!",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White
                )
            }
            Text(
                text = "Your video is being processed. You can view it in the History tab.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f)
            )
            Text(
                text = "Match ID: $matchId",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.6f)
            )
            Button(
                onClick = onResetClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Upload Another Video")
            }
        }
    }
}

@Composable
private fun ErrorCard(
    message: String,
    onResetClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0x33F44336)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = null,
                    tint = Color(0xFFF44336),
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Upload failed",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White
                )
            }
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f)
            )
            Button(
                onClick = onResetClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Try Again")
            }
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
                    imageVector = Icons.Default.Videocam,
                    contentDescription = "Upload"
                )
            },
            label = {
                Text(
                    text = "Upload",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp
                    )
                )
            },
            selected = selectedTab == 0,
            onClick = onNavigateToUpload,
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                unselectedIconColor = Color.White.copy(alpha = 0.6f),
                unselectedTextColor = Color.White.copy(alpha = 0.6f),
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
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp
                    )
                )
            },
            selected = selectedTab == 1,
            onClick = onNavigateToHistory,
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                unselectedIconColor = Color.White.copy(alpha = 0.6f),
                unselectedTextColor = Color.White.copy(alpha = 0.6f),
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
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp
                    )
                )
            },
            selected = selectedTab == 2,
            onClick = onNavigateToHighlights,
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                unselectedIconColor = Color.White.copy(alpha = 0.6f),
                unselectedTextColor = Color.White.copy(alpha = 0.6f),
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
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp
                    )
                )
            },
            selected = selectedTab == 3,
            onClick = onNavigateToProfile,
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                unselectedIconColor = Color.White.copy(alpha = 0.6f),
                unselectedTextColor = Color.White.copy(alpha = 0.6f),
                indicatorColor = Color.Transparent
            )
        )
    }
}
