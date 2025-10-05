package com.example.myapplication.ui.screens.history

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
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
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.myapplication.R
import com.example.myapplication.data.model.Match
import kotlinx.coroutines.delay

/**
 * Video player with bounding box overlay for match analysis.
 */
@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerWithOverlay(
    match: Match,
    currentPositionMs: Long,
    isPlaying: Boolean,
    onPositionChange: (Long) -> Unit,
    onPlaybackStateChange: (Boolean) -> Unit,
    detections: List<DetectionWithType>,
    trajectory: List<List<Double>>?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Create ExoPlayer
    val exoPlayer = remember {
        createExoPlayer(context).apply {
            // Set up listener for position updates
            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    onPlaybackStateChange(isPlaying)
                }
            })
        }
    }

    // Track video dimensions
    var videoWidth by remember { mutableStateOf(1920f) }
    var videoHeight by remember { mutableStateOf(1080f) }
    var displayWidth by remember { mutableStateOf(0f) }
    var displayHeight by remember { mutableStateOf(0f) }

    // Update position periodically
    LaunchedEffect(exoPlayer) {
        while (true) {
            if (exoPlayer.isPlaying) {
                onPositionChange(exoPlayer.currentPosition)
            }
            delay(50) // Update every 50ms
        }
    }

    // Clean up player on dispose
    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer.release()
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF111827))
    ) {
        // Video player
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false // Use custom controls

                    // Set video dimensions when ready
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

        // Bounding box overlay
        if (detections.isNotEmpty() && displayWidth > 0 && displayHeight > 0) {
            BoundingBoxOverlay(
                detections = detections,
                trajectory = trajectory,
                videoWidth = videoWidth,
                videoHeight = videoHeight,
                displayWidth = displayWidth,
                displayHeight = displayHeight,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Custom controls overlay
        VideoControlsOverlay(
            isPlaying = isPlaying,
            currentPositionMs = currentPositionMs,
            durationMs = exoPlayer.duration.takeIf { it > 0 } ?: (match.durationSeconds?.times(1000L) ?: 0L),
            onPlayPauseClick = {
                if (exoPlayer.isPlaying) {
                    exoPlayer.pause()
                } else {
                    exoPlayer.play()
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

/**
 * Create and configure ExoPlayer instance.
 */
private fun createExoPlayer(context: Context): ExoPlayer {
    return ExoPlayer.Builder(context).build().apply {
        val videoUri = Uri.parse("android.resource://${context.packageName}/${R.raw.test_2}")
        setMediaItem(MediaItem.fromUri(videoUri))
        prepare()
    }
}

/**
 * Canvas overlay for drawing bounding boxes and trajectories.
 */
@Composable
private fun BoundingBoxOverlay(
    detections: List<DetectionWithType>,
    trajectory: List<List<Double>>?,
    videoWidth: Float,
    videoHeight: Float,
    displayWidth: Float,
    displayHeight: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        // Calculate scaling factors
        val scaleX = displayWidth / videoWidth
        val scaleY = displayHeight / videoHeight

        // Draw trajectory lines
        trajectory?.let { points ->
            if (points.size >= 2) {
                for (i in 0 until points.size - 1) {
                    val start = points[i]
                    val end = points[i + 1]

                    val startX = start[0].toFloat() * scaleX
                    val startY = start[1].toFloat() * scaleY
                    val endX = end[0].toFloat() * scaleX
                    val endY = end[1].toFloat() * scaleY

                    drawLine(
                        color = Color.Cyan,
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = 4f
                    )
                }
            }
        }

        // Draw bounding boxes
        detections.forEach { detectionWithType ->
            val detection = detectionWithType.detection

            val x = detection.x * scaleX
            val y = detection.y * scaleY
            val width = detection.width * scaleX
            val height = detection.height * scaleY

            val color = when (detectionWithType.type) {
                DetectionType.BALL -> Color.Green
                DetectionType.EVENT -> Color.Yellow
            }

            // Draw rectangle
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

/**
 * Custom video controls overlay.
 */
@Composable
private fun BoxScope.VideoControlsOverlay(
    isPlaying: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    onPlayPauseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Play/Pause button in center
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

    // Bottom controls
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
            // Play/Pause button
            IconButton(onClick = onPlayPauseClick) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Time display
            Text(
                text = formatTime(currentPositionMs) + " / " + formatTime(durationMs),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = Color.White
            )

            // Fullscreen button
            IconButton(onClick = { /* TODO: Fullscreen */ }) {
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

/**
 * Format time in milliseconds to MM:SS format.
 */
private fun formatTime(timeMs: Long): String {
    val totalSeconds = timeMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}
