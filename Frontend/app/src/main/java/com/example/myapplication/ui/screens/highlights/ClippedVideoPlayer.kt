package com.example.myapplication.ui.screens.highlights

import android.content.Context
import androidx.annotation.OptIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.myapplication.R
import com.example.myapplication.data.model.HighlightClip
import com.example.myapplication.ui.screens.history.DetectionType
import com.example.myapplication.ui.screens.history.DetectionWithType
import kotlinx.coroutines.delay

/**
 * Video player for highlight clips.
 * 
 * This player uses ExoPlayer with clipping to play specific segments of the match video.
 * Following the architecture guidelines:
 * - Seeks to startMs when initialized
 * - Plays until endMs, then loops
 * - Shows ball trajectory and bounding boxes from event metadata
 * - Displays event information overlay
 */
@OptIn(UnstableApi::class)
@Composable
fun ClippedVideoPlayer(
    clip: HighlightClip,
    currentClipIndex: Int,
    totalClips: Int,
    onNavigatePrevious: () -> Unit,
    onNavigateNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    var isPlaying by remember(clip.id) { mutableStateOf(false) }
    var currentPositionMs by remember(clip.id) { mutableStateOf(0L) }  // 0-based relative to clip start
    
    // Create ExoPlayer with clipping for this highlight
    val exoPlayer = remember(clip.id) {
        createClippedPlayer(context, clip).apply {
            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(playing: Boolean) {
                    isPlaying = playing
                }
            })
        }
    }
    
    // Video dimensions for overlay calculations
    var videoWidth by remember { mutableStateOf(1920f) }
    var videoHeight by remember { mutableStateOf(1080f) }
    var displayWidth by remember { mutableStateOf(0f) }
    var displayHeight by remember { mutableStateOf(0f) }
    
    // Manual position tracking and boundary enforcement
    LaunchedEffect(exoPlayer, clip.id) {
        while (true) {
            val position = exoPlayer.currentPosition
            val duration = clip.endMs - clip.startMs
            
            // Check if we've exceeded the clip duration
            if (position >= duration) {
                // Stop playback
                exoPlayer.pause()
                // Reset to beginning
                exoPlayer.seekTo(0)
                currentPositionMs = 0L
            } else {
                // Normal position update
                currentPositionMs = maxOf(0L, position)
            }
            
            delay(50) // Update every 50ms
        }
    }
    
    // Cleanup
    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer.release()
        }
    }
    
    Column(modifier = modifier) {
        // Video player with overlays
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF111827))
        ) {
            // ExoPlayer view
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false // Custom controls
                        
                        exoPlayer.addListener(object : Player.Listener {
                            override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
                                videoWidth = videoSize.width.toFloat()
                                videoHeight = videoSize.height.toFloat()
                            }
                        })
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .onGloballyPositioned { coordinates ->
                        displayWidth = coordinates.size.width.toFloat()
                        displayHeight = coordinates.size.height.toFloat()
                    },
                update = { playerView ->
                    playerView.player = exoPlayer
                }
            )
            
            // Ball trajectory and bounding box overlay
            if (displayWidth > 0 && displayHeight > 0) {
                val trajectory = clip.event.metadata?.ballTrajectory
                val detections = clip.event.metadata?.detections?.map { detection ->
                    DetectionWithType(
                        detection = detection,
                        type = DetectionType.BALL,
                        confidence = detection.confidence
                    )
                } ?: emptyList()
                
                if (trajectory != null || detections.isNotEmpty()) {
                    BallTrajectoryOverlay(
                        trajectory = trajectory,
                        detections = detections,
                        videoWidth = videoWidth,
                        videoHeight = videoHeight,
                        displayWidth = displayWidth,
                        displayHeight = displayHeight,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            
            // Playback controls
            VideoControlsOverlay(
                isPlaying = isPlaying,
                currentPositionMs = currentPositionMs,
                clip = clip,
                currentClipIndex = currentClipIndex,
                totalClips = totalClips,
                onPlayPauseClick = {
                    if (exoPlayer.isPlaying) {
                        exoPlayer.pause()
                    } else {
                        exoPlayer.play()
                    }
                },
                onNavigatePrevious = onNavigatePrevious,
                onNavigateNext = onNavigateNext,
                modifier = Modifier.fillMaxSize()
            )
        }
        
        // Clip information card
        ClipInfoCard(
            clip = clip,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        )
    }
}

/**
 * Create ExoPlayer configured to play a specific clip segment.
 * 
 * Uses ClippingConfiguration to define the clip boundaries.
 * Manual boundary checking ensures we stop exactly at endMs.
 */
private fun createClippedPlayer(context: Context, clip: HighlightClip): ExoPlayer {
    val videoUri = "android.resource://${context.packageName}/${R.raw.test_2}".toUri()
    
    return ExoPlayer.Builder(context).build().apply {
        // Create media item with clipping configuration
        val mediaItem = MediaItem.Builder()
            .setUri(videoUri)
            .setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(clip.startMs)
                    .setEndPositionMs(clip.endMs)
                    .build()
            )
            .build()
        
        setMediaItem(mediaItem)
        prepare()
        
        // Start from beginning of clip
        seekTo(0)
        playWhenReady = true
        
        // NO REPEAT MODE - stop at the end
        repeatMode = Player.REPEAT_MODE_OFF
    }
}

/**
 * Canvas overlay for ball trajectory and detections.
 */
@Composable
private fun BallTrajectoryOverlay(
    trajectory: List<List<Double>>?,
    detections: List<DetectionWithType>,
    videoWidth: Float,
    videoHeight: Float,
    displayWidth: Float,
    displayHeight: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val scaleX = displayWidth / videoWidth
        val scaleY = displayHeight / videoHeight
        
        // Draw ball trajectory
        trajectory?.let { points ->
            if (points.size >= 2) {
                for (i in 0 until points.size - 1) {
                    val start = points[i]
                    val end = points[i + 1]
                    
                    if (start.size >= 2 && end.size >= 2) {
                        val startX = start[0].toFloat() * scaleX
                        val startY = start[1].toFloat() * scaleY
                        val endX = end[0].toFloat() * scaleX
                        val endY = end[1].toFloat() * scaleY
                        
                        // Draw trajectory line
                        drawLine(
                            color = Color.Cyan,
                            start = Offset(startX, startY),
                            end = Offset(endX, endY),
                            strokeWidth = 4f
                        )
                        
                        // Draw point markers
                        drawCircle(
                            color = Color.Yellow,
                            radius = 6f,
                            center = Offset(startX, startY)
                        )
                    }
                }
            }
        }
        
        // Draw bounding boxes
        detections.forEach { detectionWithType ->
            val detection = detectionWithType.detection
            
            if (detection.x != 0.0 && detection.y != 0.0) {
                val x = detection.x * scaleX
                val y = detection.y * scaleY
                val width = detection.width * scaleX
                val height = detection.height * scaleY
                
                val color = when (detectionWithType.type) {
                    DetectionType.BALL -> Color.Green
                    DetectionType.EVENT -> Color.Yellow
                }
                
                // Draw bounding box
                drawRect(
                    color = color,
                    topLeft = Offset(x.toFloat(), y.toFloat()),
                    size = Size(width.toFloat(), height.toFloat()),
                    style = Stroke(width = 3f)
                )
                
                // Draw confidence score
                val confidenceText = "${(detection.confidence * 100).toInt()}%"
                drawContext.canvas.nativeCanvas.apply {
                    val paint = android.graphics.Paint().apply {
                        this.color = android.graphics.Color.WHITE
                        textSize = 24f
                        isAntiAlias = true
                    }
                    drawText(
                        confidenceText,
                        x.toFloat(),
                        (y - 10).toFloat(),
                        paint
                    )
                }
            }
        }
    }
}

/**
 * Video controls overlay with navigation.
 */
@Composable
private fun BoxScope.VideoControlsOverlay(
    isPlaying: Boolean,
    currentPositionMs: Long,
    clip: HighlightClip,
    currentClipIndex: Int,
    totalClips: Int,
    onPlayPauseClick: () -> Unit,
    onNavigatePrevious: () -> Unit,
    onNavigateNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Center play button
    if (!isPlaying) {
        IconButton(
            onClick = onPlayPauseClick,
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
    }
    
    // Top bar with clip counter
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.TopCenter)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.6f),
                        Color.Transparent
                    )
                )
            )
            .padding(16.dp)
    ) {
        Text(
            text = "Highlight ${currentClipIndex + 1} of $totalClips",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Medium
            ),
            color = Color.White,
            modifier = Modifier.align(Alignment.Center)
        )
    }
    
    // Bottom controls
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.BottomCenter)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.7f)
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
            // Previous button
            IconButton(
                onClick = onNavigatePrevious,
                enabled = currentClipIndex > 0
            ) {
                Icon(
                    imageVector = Icons.Default.SkipPrevious,
                    contentDescription = "Previous",
                    tint = if (currentClipIndex > 0) Color.White else Color.Gray,
                    modifier = Modifier.size(28.dp)
                )
            }
            
            // Play/Pause
            IconButton(onClick = onPlayPauseClick) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
            
            // Time display
            Text(
                text = formatClipTime(currentPositionMs, clip),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = Color.White
            )
            
            // Replay button
            Icon(
                imageVector = Icons.Default.Replay,
                contentDescription = "Auto-replay",
                tint = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.size(24.dp)
            )
            
            // Next button
            IconButton(
                onClick = onNavigateNext,
                enabled = currentClipIndex < totalClips - 1
            ) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Next",
                    tint = if (currentClipIndex < totalClips - 1) Color.White else Color.Gray,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

/**
 * Information card displaying clip metadata.
 */
@Composable
private fun ClipInfoCard(
    clip: HighlightClip,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Category badge
            Surface(
                color = getCategoryColor(clip.category),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = clip.category.displayName,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
            
            // Title
            Text(
                text = clip.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            
            // Description
            if (clip.description.isNotEmpty()) {
                Text(
                    text = clip.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            
            // Metadata
            val metadata = clip.event.metadata
            if (metadata != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    metadata.shotSpeed?.let { speed ->
                        MetadataChip(
                            icon = Icons.Default.Speed,
                            label = "${speed.toInt()} km/h"
                        )
                    }
                    
                    metadata.rallyLength?.let { length ->
                        MetadataChip(
                            icon = Icons.Default.SwapHoriz,
                            label = "$length shots"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetadataChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
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

private fun formatClipTime(currentPositionMs: Long, clip: HighlightClip): String {
    // currentPositionMs is already relative to clip start (0-based) due to ClippingConfiguration
    // So we can use it directly without subtracting clip.startMs
    val duration = clip.endMs - clip.startMs
    
    fun formatMs(ms: Long): String {
        val seconds = ms / 1000
        val millis = (ms % 1000) / 100
        return String.format("%d.%ds", seconds, millis)
    }
    
    return "${formatMs(currentPositionMs)} / ${formatMs(duration)}"
}
