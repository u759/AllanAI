# AllanAI Frontend Architecture (Jetpack Compose)

## Overview

The AllanAI Android application is built using Jetpack Compose with a clean MVVM architecture pattern. It provides an intuitive interface for recording table tennis matches and viewing detailed analytics.

### Match Timeline & Timestamp Handling
- **Event granularity**: The backend returns event payloads with both `timestampMs` and `metadata.frameNumber`, mirroring the OpenTTGames convention of 120 fps annotations (−4/+12 frame windows). The UI must convert these into `Duration` objects for playback alignment via ExoPlayer.
- **Timeline widgets**: `VideoPlayer` and `MatchDetailScreen` surface a horizontally scrollable strip of events with labels (score change, rally highlight, fastest shot, serve ace, miss). Tapping an event seeks the player to `timestampMs - preRollMs` to start the clip slightly before the highlight.
- **Pre/Post buffers**: Use `metadata.eventWindow` to compute highlight preview segments (default pre-roll 133 ms, post-roll 400 ms). If metadata is absent, fall back to safe defaults.
- **Low-confidence events**: When `metadata.confidence < 0.5`, render the event in a muted style and provide a manual confirmation affordance for the user.

## Architecture Pattern: MVVM

```
┌─────────────────────────────────────────────────┐
│                UI Layer (Compose)               │
│  - Composable Functions                         │
│  - Navigation                                   │
│  - Theme & Design System                        │
└────────────────┬────────────────────────────────┘
                 │ observes State
                 ▼
┌─────────────────────────────────────────────────┐
│           ViewModel Layer                       │
│  - UI State Management                          │
│  - Business Logic                               │
│  - Event Handling                               │
└────────────────┬────────────────────────────────┘
                 │ calls
                 ▼
┌─────────────────────────────────────────────────┐
│          Repository Layer                       │
│  - Data Operations                              │
│  - API Communication                            │
│  - Local Caching                                │
└────────────────┬────────────────────────────────┘
                 │ uses
                 ▼
┌─────────────────────────────────────────────────┐
│            Data Sources                         │
│  - Remote API (Retrofit)                        │
│  - Local Storage                                │
│  - CameraX                                      │
└─────────────────────────────────────────────────┘
```

## Project Structure

```
android/app/src/main/java/com/allanai/
├── AllanAIApplication.kt
│
├── ui/
│   ├── screens/
│   │   ├── home/
│   │   │   ├── HomeScreen.kt
│   │   │   └── HomeViewModel.kt
│   │   ├── record/
│   │   │   ├── RecordScreen.kt
│   │   │   ├── RecordViewModel.kt
│   │   │   └── CameraPreview.kt
│   │   ├── matches/
│   │   │   ├── MatchListScreen.kt
│   │   │   ├── MatchDetailScreen.kt
│   │   │   └── MatchesViewModel.kt
│   │   └── statistics/
│   │       ├── StatisticsScreen.kt
│   │       ├── StatisticsViewModel.kt
│   │       └── components/
│   │           ├── SpeedChart.kt
│   │           ├── AccuracyChart.kt
│   │           └── ScoreDisplay.kt
│   │
│   ├── components/
│   │   ├── LoadingIndicator.kt
│   │   ├── ErrorMessage.kt
│   │   ├── VideoPlayer.kt
│   │   ├── EventTimeline.kt
│   │   └── StatCard.kt
│   │
│   ├── navigation/
│   │   ├── NavGraph.kt
│   │   └── Screen.kt
│   │
│   └── theme/
│       ├── Color.kt
│       ├── Theme.kt
│       ├── Type.kt
│       └── Shape.kt
│
├── viewmodel/
│   └── BaseViewModel.kt
│
├── repository/
│   ├── MatchRepository.kt
│   ├── VideoRepository.kt
│   └── StatisticsRepository.kt
│
├── data/
│   ├── api/
│   │   ├── AllanAIApiService.kt
│   │   ├── ApiClient.kt
│   │   └── interceptors/
│   │       ├── AuthInterceptor.kt
│   │       └── ErrorInterceptor.kt
│   │
│   ├── model/
│   │   ├── Match.kt
│   │   ├── Statistics.kt
│   │   ├── Shot.kt
│   │   ├── Event.kt
│   │   ├── Highlight.kt
│   │   ├── ProcessingStatus.kt
│   │   └── ApiResponse.kt
│   │
│   └── local/
│       ├── PreferencesManager.kt
│       └── VideoCache.kt
│
├── camera/
│   ├── CameraManager.kt
│   └── VideoRecorder.kt
│
├── di/
│   ├── AppModule.kt
│   ├── NetworkModule.kt
│   └── RepositoryModule.kt
│
└── util/
    ├── Constants.kt
    ├── Extensions.kt
    └── NetworkUtils.kt
```

## Layer Responsibilities

### 1. UI Layer (Composable)

**Purpose**: Present data and handle user interactions

**Guidelines**:
- Use `@Composable` functions for all UI
- Keep composables stateless when possible
- Hoist state to appropriate level
- Use `collectAsState()` to observe ViewModel
- Delegate all actions to ViewModel
- No business logic in UI

**Example - RecordScreen.kt**:
```kotlin
@Composable
fun RecordScreen(
    viewModel: RecordViewModel = hiltViewModel(),
    onNavigateToMatches: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Record Match") }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Camera preview
            CameraPreview(
                modifier = Modifier.fillMaxSize(),
                onCameraReady = { viewModel.onCameraReady() }
            )
            
            // Recording controls
            RecordingControls(
                isRecording = state.isRecording,
                duration = state.recordingDuration,
                onStartRecording = { viewModel.startRecording() },
                onStopRecording = { viewModel.stopRecording() },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(32.dp)
            )
            
            // Upload dialog
            if (state.isUploading) {
                UploadProgressDialog(
                    progress = state.uploadProgress,
                    onCancel = { viewModel.cancelUpload() }
                )
            }
            
            // Success/Error handling
            state.error?.let { error ->
                ErrorSnackbar(
                    message = error,
                    onDismiss = { viewModel.clearError() }
                )
            }
        }
    }
}

@Composable
private fun RecordingControls(
    isRecording: Boolean,
    duration: Long,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = formatDuration(duration),
            style = MaterialTheme.typography.headlineMedium,
            color = if (isRecording) Color.Red else Color.White
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        FloatingActionButton(
            onClick = {
                if (isRecording) onStopRecording() else onStartRecording()
            },
            containerColor = if (isRecording) 
                MaterialTheme.colorScheme.error 
            else 
                MaterialTheme.colorScheme.primary
        ) {
            Icon(
                imageVector = if (isRecording) 
                    Icons.Default.Stop 
                else 
                    Icons.Default.FiberManualRecord,
                contentDescription = if (isRecording) "Stop" else "Record"
            )
        }
    }
}
```

**Example - MatchDetailViewModel.kt (event timeline)**:
```kotlin
@HiltViewModel
class MatchDetailViewModel @Inject constructor(
    private val matchRepository: MatchRepository
) : ViewModel() {

    private val _state = MutableStateFlow(MatchDetailState())
    val state: StateFlow<MatchDetailState> = _state.asStateFlow()

    fun loadMatch(matchId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            runCatching { matchRepository.fetchMatch(matchId) }
                .onSuccess { match ->
                    val timeline = match.events.map { event ->
                        EventTimelineItem(
                            id = event.id,
                            label = event.type.toDisplayName(),
                            timestampMs = event.timestampMs,
                            frameNumber = event.metadata?.frameNumber,
                            preRollMs = event.metadata?.eventWindow?.preMs ?: DEFAULT_PRE_ROLL_MS,
                            postRollMs = event.metadata?.eventWindow?.postMs ?: DEFAULT_POST_ROLL_MS,
                            confidence = event.metadata?.confidence ?: 1.0
                        )
                    }
                    _state.update {
                        it.copy(
                            isLoading = false,
                            match = match,
                            timeline = timeline.sortedBy { item -> item.timestampMs }
                        )
                    }
                }
                .onFailure { throwable ->
                    _state.update { it.copy(isLoading = false, error = throwable.message) }
                }
        }
    }
}
```

**Example - StatisticsScreen.kt**:
```kotlin
@Composable
fun StatisticsScreen(
    matchId: String,
    viewModel: StatisticsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    
    LaunchedEffect(matchId) {
        viewModel.loadStatistics(matchId)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Match Statistics") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        when {
            state.isLoading -> {
                LoadingIndicator()
            }
            state.error != null -> {
                ErrorMessage(
                    message = state.error!!,
                    onRetry = { viewModel.loadStatistics(matchId) }
                )
            }
            state.statistics != null -> {
                StatisticsContent(
                    statistics = state.statistics!!,
                    modifier = Modifier.padding(padding)
                )
            }
        }
    }
}

@Composable
private fun StatisticsContent(
    statistics: MatchStatistics,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Score summary
        item {
            ScoreCard(
                player1Score = statistics.player1Score,
                player2Score = statistics.player2Score
            )
        }
        
        // Ball speed chart
        item {
            StatCard(title = "Ball Speed") {
                SpeedChart(
                    speeds = statistics.shots.map { it.speed },
                    avgSpeed = statistics.avgBallSpeed,
                    maxSpeed = statistics.maxBallSpeed
                )
            }
        }
        
        // Shot accuracy
        item {
            StatCard(title = "Shot Accuracy") {
                AccuracyChart(
                    player1Accuracy = statistics.player1Accuracy,
                    player2Accuracy = statistics.player2Accuracy
                )
            }
        }
        
        // Serve success rate
        item {
            StatCard(title = "Serve Success Rate") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    PlayerStat(
                        label = "Player 1",
                        value = "${statistics.player1ServeSuccess}%"
                    )
                    PlayerStat(
                        label = "Player 2",
                        value = "${statistics.player2ServeSuccess}%"
                    )
                }
            }
        }
        
        // Rally statistics
        item {
            StatCard(title = "Rally Statistics") {
                Column {
                    StatRow("Total Rallies", statistics.totalRallies.toString())
                    StatRow("Avg Rally Length", "${statistics.avgRallyLength} shots")
                    StatRow("Longest Rally", "${statistics.maxRallyLength} shots")
                }
            }
        }
    }
}
```

### 2. ViewModel Layer

**Purpose**: Manage UI state and coordinate data operations

**Guidelines**:
- Extend `ViewModel`
- Use `StateFlow` for UI state
- Use `viewModelScope` for coroutines
- Delegate data operations to Repository
- Transform domain data to UI state
- Handle loading, success, error states
- Never reference Android UI components directly

**Example - RecordViewModel.kt**:
```kotlin
@HiltViewModel
class RecordViewModel @Inject constructor(
    private val videoRepository: VideoRepository,
    private val matchRepository: MatchRepository
) : ViewModel() {
    
    private val _state = MutableStateFlow(RecordState())
    val state: StateFlow<RecordState> = _state.asStateFlow()
    
    private var recordingJob: Job? = null
    private var currentVideoFile: File? = null
    
    fun onCameraReady() {
        _state.value = _state.value.copy(cameraReady = true)
    }
    
    fun startRecording() {
        viewModelScope.launch {
            try {
                currentVideoFile = videoRepository.startRecording()
                _state.value = _state.value.copy(isRecording = true)
                
                // Update recording duration
                recordingJob = launch {
                    while (true) {
                        delay(1000)
                        _state.value = _state.value.copy(
                            recordingDuration = _state.value.recordingDuration + 1
                        )
                    }
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    error = "Failed to start recording: ${e.message}"
                )
            }
        }
    }
    
    fun stopRecording() {
        viewModelScope.launch {
            try {
                recordingJob?.cancel()
                videoRepository.stopRecording()
                _state.value = _state.value.copy(isRecording = false)
                
                // Automatically upload
                currentVideoFile?.let { uploadVideo(it) }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    error = "Failed to stop recording: ${e.message}"
                )
            }
        }
    }
    
    private fun uploadVideo(videoFile: File) {
        viewModelScope.launch {
            try {
                _state.value = _state.value.copy(isUploading = true)
                
                matchRepository.uploadMatch(videoFile)
                    .collect { progress ->
                        _state.value = _state.value.copy(
                            uploadProgress = progress
                        )
                    }
                
                _state.value = _state.value.copy(
                    isUploading = false,
                    uploadSuccess = true
                )
                
                // Clean up local file
                videoFile.delete()
                currentVideoFile = null
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isUploading = false,
                    error = "Upload failed: ${e.message}"
                )
            }
        }
    }
    
    fun cancelUpload() {
        // Cancel ongoing upload
        viewModelScope.coroutineContext.cancelChildren()
        _state.value = _state.value.copy(isUploading = false)
    }
    
    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}

data class RecordState(
    val cameraReady: Boolean = false,
    val isRecording: Boolean = false,
    val recordingDuration: Long = 0,
    val isUploading: Boolean = false,
    val uploadProgress: Float = 0f,
    val uploadSuccess: Boolean = false,
    val error: String? = null
)
```

**Example - StatisticsViewModel.kt**:
```kotlin
@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val statisticsRepository: StatisticsRepository
) : ViewModel() {
    
    private val _state = MutableStateFlow(StatisticsState())
    val state: StateFlow<StatisticsState> = _state.asStateFlow()
    
    fun loadStatistics(matchId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            
            try {
                val statistics = statisticsRepository.getMatchStatistics(matchId)
                _state.value = _state.value.copy(
                    isLoading = false,
                    statistics = statistics,
                    error = null
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load statistics"
                )
            }
        }
    }
}

data class StatisticsState(
    val isLoading: Boolean = false,
    val statistics: MatchStatistics? = null,
    val error: String? = null
)
```

### 3. Repository Layer

**Purpose**: Manage data operations and API communication

**Guidelines**:
- Single source of truth for data
- Handle all API calls
- Implement caching when appropriate
- Transform API responses to domain models
- Handle errors and retry logic
- Use `suspend` functions for async operations

**Example - MatchRepository.kt**:
```kotlin
class MatchRepository @Inject constructor(
    private val apiService: AllanAIApiService,
    private val preferencesManager: PreferencesManager
) {
    
    suspend fun uploadMatch(videoFile: File): Flow<Float> = flow {
        val requestBody = videoFile.asRequestBody("video/mp4".toMediaType())
        val multipartBody = MultipartBody.Part.createFormData(
            "video",
            videoFile.name,
            requestBody
        )
        
        // Simulate progress tracking (Retrofit doesn't provide native progress)
        // In production, use a custom RequestBody with progress callback
        emit(0.25f)
        delay(500)
        emit(0.50f)
        delay(500)
        emit(0.75f)
        
        val response = apiService.uploadMatch(multipartBody)
        emit(1.0f)
        
        if (!response.success) {
            throw Exception(response.error?.message ?: "Upload failed")
        }
    }
    
    suspend fun getMatches(): List<Match> {
        val response = apiService.getMatches()
        if (!response.success) {
            throw Exception(response.error?.message ?: "Failed to fetch matches")
        }
        return response.data ?: emptyList()
    }
    
    suspend fun getMatchDetails(matchId: String): Match {
        val response = apiService.getMatchDetails(matchId)
        if (!response.success) {
            throw Exception(response.error?.message ?: "Failed to fetch match details")
        }
        return response.data ?: throw Exception("Match not found")
    }
    
    suspend fun waitForProcessing(matchId: String): Match {
        while (true) {
            val status = getProcessingStatus(matchId)
            when (status.status) {
                ProcessingStatus.COMPLETE -> {
                    return getMatchDetails(matchId)
                }
                ProcessingStatus.FAILED -> {
                    throw Exception(status.errorMessage ?: "Processing failed")
                }
                else -> {
                    delay(2000) // Poll every 2 seconds
                }
            }
        }
    }
    
    private suspend fun getProcessingStatus(matchId: String): ProcessingStatus {
        val response = apiService.getProcessingStatus(matchId)
        if (!response.success) {
            throw Exception("Failed to get processing status")
        }
        return response.data ?: throw Exception("Status not found")
    }
    
    suspend fun deleteMatch(matchId: String) {
        val response = apiService.deleteMatch(matchId)
        if (!response.success) {
            throw Exception(response.error?.message ?: "Failed to delete match")
        }
    }
}

class StatisticsRepository @Inject constructor(
    private val apiService: AllanAIApiService
) {
    
    suspend fun getMatchStatistics(matchId: String): MatchStatistics {
        val response = apiService.getMatchStatistics(matchId)
        if (!response.success) {
            throw Exception(response.error?.message ?: "Failed to fetch statistics")
        }
        return response.data ?: throw Exception("Statistics not found")
    }
}

class VideoRepository @Inject constructor(
    private val cameraManager: CameraManager,
    private val context: Context
) {
    
    private var currentRecording: VideoRecorder? = null
    
    suspend fun startRecording(): File {
        val videoFile = createVideoFile()
        currentRecording = cameraManager.startRecording(videoFile)
        return videoFile
    }
    
    suspend fun stopRecording() {
        currentRecording?.stop()
        currentRecording = null
    }
    
    private fun createVideoFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
        return File(storageDir, "MATCH_${timeStamp}.mp4")
    }
}