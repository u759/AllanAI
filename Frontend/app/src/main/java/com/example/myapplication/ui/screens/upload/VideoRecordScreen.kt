package com.example.myapplication.ui.screens.upload

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Recording
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.camera.view.video.AudioConfig
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import java.io.File

/**
 * Screen for recording video using the device camera.
 *
 * Uses CameraX for video recording with front-facing camera.
 * After recording, shows metadata dialog before uploading.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoRecordScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: UploadViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var isRecording by remember { mutableStateOf(false) }
    var recordingTime by remember { mutableStateOf(0) }
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val showMetadataDialog by viewModel.showMetadataDialog.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    // Camera controller
    val cameraController = remember {
        LifecycleCameraController(context).apply {
            setEnabledUseCases(CameraController.VIDEO_CAPTURE)
            cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
        }
    }

    // Recording state
    var recording: Recording? by remember { mutableStateOf(null) }
    var videoFile: File? by remember { mutableStateOf(null) }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasPermission = permissions[Manifest.permission.CAMERA] == true &&
                permissions[Manifest.permission.RECORD_AUDIO] == true
    }

    // Request permissions on first launch
    LaunchedEffect(Unit) {
        if (!hasPermission) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO
                )
            )
        }
    }

    // Timer effect
    LaunchedEffect(isRecording) {
        if (isRecording) {
            recordingTime = 0
            while (isRecording) {
                kotlinx.coroutines.delay(1000)
                recordingTime++
            }
        } else {
            recordingTime = 0
        }
    }

    Scaffold(
        topBar = {
            VideoRecordTopBar(onNavigateBack = onNavigateBack)
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (hasPermission) {
                // Camera preview
                AndroidView(
                    factory = { ctx ->
                        PreviewView(ctx).apply {
                            controller = cameraController
                            cameraController.bindToLifecycle(lifecycleOwner)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Recording controls overlay
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Recording timer
                    if (isRecording) {
                        Text(
                            text = formatTime(recordingTime),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color.Red
                        )
                    }

                    // Record/Stop button
                    IconButton(
                        onClick = {
                            if (isRecording) {
                                // Stop recording
                                recording?.stop()
                                recording = null
                                isRecording = false
                            } else {
                                // Start recording
                                val tempFile = File.createTempFile(
                                    "recording_${System.currentTimeMillis()}",
                                    ".mp4",
                                    context.cacheDir
                                )
                                videoFile = tempFile

                                val fileOutputOptions = FileOutputOptions.Builder(tempFile).build()

                                recording = cameraController.startRecording(
                                    fileOutputOptions,
                                    AudioConfig.create(true),
                                    ContextCompat.getMainExecutor(context)
                                ) { event ->
                                    when (event) {
                                        is VideoRecordEvent.Start -> {
                                            isRecording = true
                                        }
                                        is VideoRecordEvent.Finalize -> {
                                            if (event.hasError()) {
                                                // Handle error
                                                videoFile?.delete()
                                                videoFile = null
                                            } else {
                                                // Recording successful, show metadata dialog
                                                videoFile?.let { file ->
                                                    viewModel.onVideoRecorded(file)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .size(80.dp)
                            .background(
                                color = if (isRecording) Color.Red else MaterialTheme.colorScheme.primary,
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.FiberManualRecord,
                            contentDescription = if (isRecording) "Stop Recording" else "Start Recording",
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    Text(
                        text = if (isRecording) "Tap to stop" else "Tap to record",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                }
            } else {
                // Permission denied message
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Camera and microphone permissions are required to record videos",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.CAMERA,
                                    Manifest.permission.RECORD_AUDIO
                                )
                            )
                        }
                    ) {
                        Text("Grant Permissions")
                    }
                }
            }

            // Metadata Dialog
            if (showMetadataDialog) {
                MetadataInputDialog(
                    onDismiss = { viewModel.cancelMetadataDialog() },
                    onConfirm = { player1, player2, title ->
                        viewModel.uploadVideo(player1, player2, title, context)
                    }
                )
            }

            // Show upload status
            when (uiState) {
                is UploadUiState.Uploading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.7f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                CircularProgressIndicator()
                                Text(
                                    text = "Uploading video...",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }
                }
                is UploadUiState.Success -> {
                    // Navigate back on success
                    LaunchedEffect(Unit) {
                        onNavigateBack()
                    }
                }
                is UploadUiState.Error -> {
                    // Show error and navigate back
                    LaunchedEffect(Unit) {
                        // Could show a toast/snackbar here
                        kotlinx.coroutines.delay(2000)
                        onNavigateBack()
                    }
                }
                else -> {}
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VideoRecordTopBar(
    onNavigateBack: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = "Record Video",
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

private fun formatTime(seconds: Int): String {
    val minutes = seconds / 60
    val secs = seconds % 60
    return String.format("%02d:%02d", minutes, secs)
}
