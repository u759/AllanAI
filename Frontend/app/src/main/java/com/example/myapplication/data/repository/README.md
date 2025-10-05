# Repository Layer Documentation (Android Frontend)

## Overview

The repository layer provides data access abstraction for the AllanAI Android app. Following the MVVM architecture and Android best practices, this layer sits between ViewModels and data sources (API, local database, or mock data).

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│                 Composable UI                           │
│              (Screens & Components)                     │
└───────────────────┬─────────────────────────────────────┘
                    │ observes StateFlow/Flow
                    ▼
┌─────────────────────────────────────────────────────────┐
│                   ViewModel                             │
│            (Business Logic & State)                     │
└───────────────────┬─────────────────────────────────────┘
                    │ calls suspend functions
                    │ observes Flows
                    ▼
┌─────────────────────────────────────────────────────────┐
│              MatchRepository                            │
│              (Interface)                                │
└───────────────────┬─────────────────────────────────────┘
                    │
         ┌──────────┴──────────┐
         │                     │
         ▼                     ▼
┌──────────────────┐  ┌──────────────────┐
│ ApiMatchRepo     │  │ MockMatchRepo    │
│ (Production)     │  │ (Testing/Dev)    │
│                  │  │                  │
│ Uses Retrofit    │  │ In-memory        │
│ Calls Backend    │  │ Sample data      │
└──────────────────┘  └──────────────────┘
```

## Components

### 1. Data Models (`data/model/Match.kt`)

**Location**: `com.example.myapplication.data.model`

**Core Models**:
- `Match` - Main match data class with statistics, events, and highlights
- `MatchStatus` - Enum for processing state (UPLOADED, PROCESSING, COMPLETE, FAILED)
- `MatchStatistics` - Aggregated stats (scores, rally info, ball speeds)
- `Shot` - Individual shot data with type, speed, accuracy
- `Event` - Timestamped event for video navigation
- `EventType` - Enum for event categories (PLAY_OF_THE_GAME, SCORE, etc.)
- `Highlights` - Quick access to important events

**Key Features**:
- Immutable data classes (Kotlin `data class`)
- Nullable fields for optional data
- Default values for lists and optional fields
- Type-safe enums for status and types
- Matches backend schema exactly

**Example**:
```kotlin
val match = Match(
    id = "123",
    createdAt = Instant.now(),
    status = MatchStatus.COMPLETE,
    statistics = MatchStatistics(
        player1Score = 11,
        player2Score = 8,
        totalRallies = 45,
        avgRallyLength = 6.5,
        maxBallSpeed = 52.3,
        avgBallSpeed = 32.8
    ),
    events = listOf(
        Event(
            id = "event-1",
            timestampMs = 83500,
            type = EventType.PLAY_OF_THE_GAME,
            title = "Play of the Game",
            description = "Epic rally",
            importance = 10
        )
    )
)
```

### 2. MatchRepository Interface

**Location**: `com.example.myapplication.data.repository.MatchRepository`

**Purpose**: Defines the contract for all match data operations.

**Key Methods**:

#### Upload & Create
```kotlin
suspend fun uploadMatch(videoFile: File, filename: String): Result<Match>
```
Uploads video to backend, creates new match with UPLOADED status.

#### Query Matches
```kotlin
fun getAllMatches(): Flow<List<Match>>
fun getMatchById(matchId: String): Flow<Match?>
fun getMatchesByStatus(status: MatchStatus): Flow<List<Match>>
```
Returns reactive Flows that emit updates when data changes.

#### Statistics & Events
```kotlin
suspend fun getMatchStatistics(matchId: String): Result<Match>
suspend fun getMatchEvents(matchId: String): Result<List<Event>>
suspend fun getEventById(matchId: String, eventId: String): Result<Event>
suspend fun getMatchHighlights(matchId: String): Result<Highlights>
```
Fetches detailed match data and events for display.

#### Video Playback
```kotlin
suspend fun getVideoUrl(matchId: String): Result<String>
```
Returns URL for ExoPlayer video streaming.

#### Management
```kotlin
suspend fun deleteMatch(matchId: String): Result<Unit>
suspend fun refreshMatch(matchId: String): Result<Match>
suspend fun getMatchStatus(matchId: String): Result<MatchStatus>
```
Manages match lifecycle and data freshness.

**Design Decisions**:
- Uses Kotlin `Flow` for reactive queries (auto-updates UI)
- Uses `suspend` functions for one-shot operations
- Returns `Result<T>` for error handling
- All operations are async (no blocking)

### 3. MockMatchRepository

**Location**: `com.example.myapplication.data.repository.MockMatchRepository`

**Purpose**: In-memory implementation with sample data for development and testing.

**Features**:
- Thread-safe `ConcurrentHashMap` storage
- Simulates network delays (500ms)
- Pre-loaded with 3 sample matches
- Auto-generates realistic sample data
- Simulates async processing (3s delay)
- Supports all repository operations

**Use Cases**:
- UI development without backend
- Jetpack Compose Previews
- Unit testing ViewModels
- Offline demo mode
- Fast iteration during development

**Example Usage**:
```kotlin
@Preview(showBackground = true)
@Composable
fun MatchListPreview() {
    val repository = MockMatchRepository()
    val matches = repository.getAllMatches()
        .collectAsState(initial = emptyList())

    MatchListScreen(matches = matches.value)
}
```

**Sample Data**:
- Match 1: Completed match from 2 hours ago (11-8 score)
- Match 2: Currently processing (uploaded 5 mins ago)
- Match 3: Completed match from yesterday (9-11 score)

**Simulated Processing**:
When you upload a match, it stays in UPLOADED status for 3 seconds, then automatically transitions to COMPLETE with generated statistics and events.

### 4. ApiMatchRepository (Future)

**Location**: `com.example.myapplication.data.repository.ApiMatchRepository` (to be created)

**Purpose**: Production implementation using Retrofit to call backend API.

**Planned Features**:
- Retrofit API client integration
- Local Room database caching
- Network error handling and retry logic
- Token-based authentication
- Multipart file upload for videos
- Efficient data synchronization

**Example Structure**:
```kotlin
class ApiMatchRepository(
    private val apiService: MatchApiService,
    private val matchDao: MatchDao
) : MatchRepository {

    override fun getAllMatches(): Flow<List<Match>> {
        return matchDao.getAllMatches()
            .onStart {
                // Refresh from API
                try {
                    val matches = apiService.getMatches()
                    matchDao.insertAll(matches)
                } catch (e: Exception) {
                    // Use cached data on error
                }
            }
    }

    override suspend fun uploadMatch(
        videoFile: File,
        filename: String
    ): Result<Match> {
        return try {
            val requestBody = videoFile.asRequestBody("video/mp4".toMediaType())
            val part = MultipartBody.Part.createFormData("video", filename, requestBody)
            val match = apiService.uploadVideo(part)
            matchDao.insert(match)
            Result.success(match)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

## Usage in ViewModels

### Basic Pattern

```kotlin
class MatchListViewModel(
    private val repository: MatchRepository
) : ViewModel() {

    // Reactive state using Flow
    val matches: StateFlow<List<Match>> = repository
        .getAllMatches()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // One-shot operations
    fun deleteMatch(matchId: String) {
        viewModelScope.launch {
            repository.deleteMatch(matchId)
                .onSuccess {
                    // Show success message
                }
                .onFailure { error ->
                    // Show error message
                }
        }
    }
}
```

### Upload with Progress

```kotlin
class UploadViewModel(
    private val repository: MatchRepository
) : ViewModel() {

    private val _uploadState = MutableStateFlow<UploadState>(UploadState.Idle)
    val uploadState: StateFlow<UploadState> = _uploadState

    fun uploadVideo(videoFile: File, filename: String) {
        viewModelScope.launch {
            _uploadState.value = UploadState.Uploading

            repository.uploadMatch(videoFile, filename)
                .onSuccess { match ->
                    _uploadState.value = UploadState.Success(match)
                    pollProcessingStatus(match.id)
                }
                .onFailure { error ->
                    _uploadState.value = UploadState.Error(error.message ?: "Upload failed")
                }
        }
    }

    private suspend fun pollProcessingStatus(matchId: String) {
        while (true) {
            delay(2000) // Poll every 2 seconds

            val result = repository.getMatchStatus(matchId)
            result.onSuccess { status ->
                when (status) {
                    MatchStatus.COMPLETE -> {
                        _uploadState.value = UploadState.ProcessingComplete
                        return
                    }
                    MatchStatus.FAILED -> {
                        _uploadState.value = UploadState.Error("Processing failed")
                        return
                    }
                    else -> {
                        // Continue polling
                    }
                }
            }
        }
    }
}

sealed class UploadState {
    object Idle : UploadState()
    object Uploading : UploadState()
    data class Success(val match: Match) : UploadState()
    object ProcessingComplete : UploadState()
    data class Error(val message: String) : UploadState()
}
```

### Event Navigation

```kotlin
class VideoPlayerViewModel(
    private val repository: MatchRepository,
    private val matchId: String
) : ViewModel() {

    val events: StateFlow<List<Event>> = flow {
        repository.getMatchEvents(matchId)
            .onSuccess { emit(it) }
            .onFailure { emit(emptyList()) }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun jumpToEvent(eventId: String) {
        viewModelScope.launch {
            repository.getEventById(matchId, eventId)
                .onSuccess { event ->
                    // Seek video player to event.timestampMs
                    seekTo(event.timestampMs)
                }
        }
    }
}
```

## Dependency Injection (Hilt)

### Setup

Hilt is configured in the project with:
1. **AllanAIApplication** - Application class annotated with `@HiltAndroidApp`
2. **MainActivity** - Annotated with `@AndroidEntryPoint` to enable injection
3. **RepositoryModule** - Hilt module that binds `MatchRepository` interface to `MockMatchRepository`

### Module Setup

```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    /**
     * Binds MockMatchRepository to MatchRepository interface.
     *
     * To switch to production API:
     * 1. Create ApiMatchRepository implementing MatchRepository
     * 2. Change binding:
     *    @Binds
     *    @Singleton
     *    abstract fun bindMatchRepository(impl: ApiMatchRepository): MatchRepository
     */
    @Binds
    @Singleton
    abstract fun bindMatchRepository(
        impl: MockMatchRepository
    ): MatchRepository
}
```

### Application Class

```kotlin
@HiltAndroidApp
class AllanAIApplication : Application()
```

Don't forget to add this to `AndroidManifest.xml`:
```xml
<application
    android:name=".AllanAIApplication"
    ...>
```

### ViewModel Injection

```kotlin
@HiltViewModel
class MatchListViewModel @Inject constructor(
    private val repository: MatchRepository
) : ViewModel() {

    val matches: StateFlow<List<Match>> = repository
        .getAllMatches()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
}
```

### Using ViewModels in Composables

```kotlin
@Composable
fun MatchListScreen(
    viewModel: MatchListViewModel = hiltViewModel()
) {
    val matches by viewModel.matches.collectAsState()

    // UI implementation
}
```

## Testing

### Unit Testing ViewModels

```kotlin
@Test
fun `uploadVideo updates state correctly`() = runTest {
    // Arrange
    val mockRepository = MockMatchRepository()
    val viewModel = UploadViewModel(mockRepository)

    // Act
    viewModel.uploadVideo(testFile, "test.mp4")

    // Assert
    viewModel.uploadState.test {
        assertEquals(UploadState.Uploading, awaitItem())
        val success = awaitItem() as UploadState.Success
        assertNotNull(success.match.id)
    }
}
```

### Testing with Mock Repository

```kotlin
class MatchListViewModelTest {

    private lateinit var repository: MockMatchRepository
    private lateinit var viewModel: MatchListViewModel

    @Before
    fun setup() {
        repository = MockMatchRepository()
        viewModel = MatchListViewModel(repository)
    }

    @Test
    fun `getAllMatches returns sample data`() = runTest {
        val matches = viewModel.matches.first()
        assertEquals(3, matches.size) // Mock has 3 sample matches
    }

    @Test
    fun `deleteMatch removes from list`() = runTest {
        val initialCount = viewModel.matches.first().size
        val matchToDelete = viewModel.matches.first().first()

        viewModel.deleteMatch(matchToDelete.id)

        val updatedMatches = viewModel.matches.first()
        assertEquals(initialCount - 1, updatedMatches.size)
    }
}
```

## Best Practices

### 1. Always Use the Interface

```kotlin
// Good
class MyViewModel(private val repository: MatchRepository)

// Bad
class MyViewModel(private val repository: MockMatchRepository)
```

### 2. Handle Results Properly

```kotlin
// Good
repository.getMatchById(id)
    .onSuccess { match -> /* handle success */ }
    .onFailure { error -> /* handle error */ }

// Bad
val match = repository.getMatchById(id).getOrNull()!! // Can crash
```

### 3. Use Flows for Reactive Data

```kotlin
// Good - UI updates automatically
val matches = repository.getAllMatches()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

// Bad - manual refresh required
suspend fun getMatches() = repository.getAllMatches().first()
```

### 4. Scope Coroutines Properly

```kotlin
// Good - tied to ViewModel lifecycle
viewModelScope.launch {
    repository.uploadMatch(file, name)
}

// Bad - can leak
GlobalScope.launch {
    repository.uploadMatch(file, name)
}
```

### 5. Cache Expensive Operations

```kotlin
// Good - cached flow
val match = repository.getMatchById(matchId)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

// Bad - fetches every time
fun getMatch() = viewModelScope.launch {
    val match = repository.getMatchById(matchId)
}
```

## Performance Considerations

### Flow Collection

- Use `SharingStarted.WhileSubscribed(5000)` to stop collecting when UI is not visible
- Set appropriate initial values to avoid null checks
- Use `distinctUntilChanged()` to avoid redundant emissions

### Network Delays

- Mock repository simulates 500ms delay - realistic for local network
- Production should implement caching to reduce API calls
- Use WorkManager for background sync

### Memory Management

- Mock repository stores all data in memory
- Production should use Room for persistence
- Clear unnecessary data to prevent memory leaks

## Error Handling

### Network Errors (Future)

```kotlin
sealed class NetworkError : Exception() {
    object NoConnection : NetworkError()
    object Timeout : NetworkError()
    object ServerError : NetworkError()
    data class ApiError(val code: Int, override val message: String) : NetworkError()
}
```

### UI Error Display

```kotlin
result.onFailure { error ->
    when (error) {
        is NetworkError.NoConnection -> showSnackbar("No internet connection")
        is NetworkError.Timeout -> showSnackbar("Request timed out")
        else -> showSnackbar("An error occurred: ${error.message}")
    }
}
```

## Migration Path: Mock → Production

1. **Phase 1** (Current): Use `MockMatchRepository` for all development
2. **Phase 2**: Implement `ApiMatchRepository` with Retrofit
3. **Phase 3**: Add Room database for offline caching
4. **Phase 4**: Implement sync logic and conflict resolution
5. **Phase 5**: Add authentication and token management

## Related Documentation

- [Architecture Guidelines](../../../../../../AIGuidelines/architecture.md)
- [Interface Guidelines](../../../../../../AIGuidelines/interfaceguidelines.md)
- [Backend Repository Documentation](../../../../../../backend/backend/src/main/java/com/backend/backend/repository/README.md)
- [ViewModel Best Practices](https://developer.android.com/topic/libraries/architecture/viewmodel)
- [Kotlin Flow Guide](https://developer.android.com/kotlin/flow)

---

**Last Updated**: October 4, 2025
**Maintained By**: AllanAI Android Team
