# Phase 1: API Integration - COMPLETED ✅

## Overview

Phase 1 of the AllanAI frontend development is now complete! The Android app can now communicate with the Spring Boot backend, send video files for processing, and receive match data including statistics, events, and highlights.

## What Was Implemented

### 1. Dependencies Added ✅
Added to `app/build.gradle.kts`:
- **Retrofit 2.9.0** - HTTP client for API communication
- **OkHttp 4.12.0** - Underlying HTTP library with logging
- **Gson 2.10.1** - JSON serialization/deserialization
- **Coroutines 1.7.3** - Async operations

### 2. API Layer Created ✅

#### Response DTOs (`data/api/dto/`)
Created all response data classes matching backend structure:
- `MatchUploadResponse.kt` - Upload confirmation
- `MatchSummaryResponse.kt` - Match list items
- `MatchDetailsResponse.kt` - Complete match data
- `MatchStatusResponse.kt` - Processing status
- Plus nested DTOs: Events, Shots, Statistics, Highlights, etc.

#### API Service (`data/api/AllanAIApiService.kt`)
Retrofit interface defining all backend endpoints:
```kotlin
@POST("api/matches/upload")
suspend fun uploadMatch(@Part video: MultipartBody.Part): MatchUploadResponse

@GET("api/matches")
suspend fun getMatches(): List<MatchSummaryResponse>

@GET("api/matches/{id}")
suspend fun getMatchDetails(@Path("id") matchId: String): MatchDetailsResponse

@GET("api/matches/{id}/events")
suspend fun getMatchEvents(@Path("id") matchId: String): List<EventResponse>

// ... and more
```

#### Response Mappers (`data/api/mapper/ResponseMapper.kt`)
Extension functions to convert API DTOs to domain models:
```kotlin
fun MatchDetailsResponse.toMatch(): Match
fun EventResponse.toEvent(): Event
fun ShotResponse.toShot(): Shot
// ... etc
```

### 3. Repository Implementation ✅

#### ApiMatchRepository (`data/repository/ApiMatchRepository.kt`)
Production implementation of `MatchRepository` interface:
- ✅ Upload video files with multipart/form-data
- ✅ Fetch match lists and details
- ✅ Get events, statistics, and highlights
- ✅ Stream video URLs
- ✅ Delete matches
- ✅ In-memory caching with Flow for reactive UI updates
- ✅ Proper error handling with Result types

### 4. Dependency Injection ✅

#### NetworkModule (`di/NetworkModule.kt`)
Hilt module providing:
- Configured OkHttpClient (60s timeouts for video uploads)
- HTTP logging interceptor
- Retrofit instance with Gson converter
- AllanAIApiService singleton

#### Updated RepositoryModule (`di/RepositoryModule.kt`)
Now binds `ApiMatchRepository` instead of `MockMatchRepository`
```kotlin
@Binds
@Singleton
abstract fun bindMatchRepository(impl: ApiMatchRepository): MatchRepository
```

## Architecture Compliance ✅

This implementation follows the architecture guidelines:

✅ **MVVM Pattern**: Repository → ViewModel → UI (Compose)
✅ **Clean Separation**: API DTOs mapped to domain models
✅ **Reactive Data**: Flow for reactive UI updates
✅ **Error Handling**: Result types for success/failure
✅ **Dependency Injection**: Hilt for all dependencies
✅ **Single Source of Truth**: Repository layer abstracts data sources

## How It Works

### Upload Flow
```
1. User selects video → UploadScreen
2. ViewModel calls repository.uploadMatch(videoFile)
3. ApiMatchRepository creates MultipartBody.Part
4. Retrofit POST to /api/matches/upload
5. Backend returns matchId + status=PROCESSING
6. Match added to local cache
7. UI updates via Flow
```

### Data Retrieval Flow
```
1. ViewModel calls repository.getAllMatches()
2. Flow emits cached data immediately
3. Background refresh calls /api/matches
4. API response mapped to Match domain models
5. Cache updated with new data
6. Flow emits updated list
7. UI automatically re-composes
```

### Match Detail Flow
```
1. User taps match → Navigate to MatchDetailScreen
2. ViewModel calls repository.getMatchStatistics(matchId)
3. API GET /api/matches/{id}
4. Response includes statistics, shots, events, highlights
5. DTOs mapped to domain models via ResponseMapper
6. Match.kt data classes populated
7. UI displays statistics, events, video player
```

## Network Configuration

### Base URL
Configured in `NetworkModule.kt`:
```kotlin
private const val BASE_URL = "http://10.0.2.2:8080/"
```

**For Android Emulator**: `10.0.2.2` is a special alias for host machine's localhost

**For Physical Device**: Update to your computer's IP address:
```kotlin
private const val BASE_URL = "http://192.168.1.100:8080/" // Replace with your IP
```

### Timeouts
```kotlin
.connectTimeout(60, TimeUnit.SECONDS)  // Connection timeout
.readTimeout(60, TimeUnit.SECONDS)     // Read timeout
.writeTimeout(60, TimeUnit.SECONDS)    // Write timeout (video uploads)
```

## Testing the Integration

### Prerequisites
1. **Backend Running**: Start Spring Boot backend on port 8080
   ```bash
   cd backend/backend
   ./mvnw spring-boot:run
   ```

2. **MongoDB Running**: Backend needs MongoDB
   ```bash
   mongod
   ```

3. **Verify Backend**: Test with curl
   ```bash
   curl http://localhost:8080/api/matches
   ```

### Run the Android App

1. **Sync Gradle**: Let Android Studio download new dependencies
2. **Build**: Build → Make Project
3. **Run**: Run on emulator or physical device
4. **Check Logs**: Look for Retrofit HTTP logs in Logcat

### Expected Behavior

#### On App Launch:
- App automatically fetches matches from backend
- Empty list if no matches exist
- Logcat shows HTTP GET request to `/api/matches`

#### When Uploading Video:
- Multipart upload POST to `/api/matches/upload`
- Returns matchId immediately
- Match appears in list with status=PROCESSING
- Can poll status with GET `/api/matches/{id}/status`

#### When Viewing Match Details:
- GET request to `/api/matches/{id}`
- Full match data including events and statistics
- Video streaming URL: `http://10.0.2.2:8080/api/matches/{id}/video`

### Logcat Examples

**Successful Upload:**
```
D/OkHttp: --> POST http://10.0.2.2:8080/api/matches/upload
D/OkHttp: Content-Type: multipart/form-data
D/OkHttp: <-- 202 ACCEPTED
D/OkHttp: {"matchId":"abc123","status":"PROCESSING"}
```

**Fetching Matches:**
```
D/OkHttp: --> GET http://10.0.2.2:8080/api/matches
D/OkHttp: <-- 200 OK
D/OkHttp: [{"id":"abc123","createdAt":"2025-10-05T...","status":"COMPLETE",...}]
```

## Data Destructuring Example

The response DTOs are automatically mapped to `Match.kt` domain models:

```kotlin
// Backend JSON response
{
  "id": "abc123",
  "createdAt": "2025-10-05T10:30:00Z",
  "status": "COMPLETE",
  "statistics": {
    "player1Score": 11,
    "player2Score": 8,
    "totalRallies": 45,
    "avgRallyLength": 6.5,
    "maxBallSpeed": 52.3,
    "avgBallSpeed": 32.8
  },
  "events": [
    {
      "id": "event1",
      "timestampMs": 83500,
      "type": "PLAY_OF_THE_GAME",
      "title": "Play of the Game",
      "importance": 10,
      "metadata": {
        "rallyLength": 15,
        "shotSpeed": 45.2
      }
    }
  ]
}

// Automatically becomes:
Match(
  id = "abc123",
  createdAt = Instant.parse("2025-10-05T10:30:00Z"),
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
      id = "event1",
      timestampMs = 83500L,
      type = EventType.PLAY_OF_THE_GAME,
      title = "Play of the Game",
      importance = 10,
      metadata = EventMetadata(
        rallyLength = 15,
        shotSpeed = 45.2
      )
    )
  )
)

// Ready to display in UI:
Text("Score: ${match.statistics?.player1Score} - ${match.statistics?.player2Score}")
Text("Max Speed: ${match.statistics?.maxBallSpeed} km/h")
```

## What's Next - Phase 2

Now that API integration is complete, the next phase is to implement:

1. **Video Upload UI** ⏭️
   - File picker integration
   - Upload progress indicator
   - Error handling UI

2. **Video Recording** ⏭️
   - CameraX integration
   - Recording controls
   - Video preview

3. **Video Playback** ⏭️
   - ExoPlayer integration
   - Event timeline navigation
   - Seek to timestamp

4. **Statistics Display** ⏭️
   - Chart components for speed/accuracy
   - Score progression visualization
   - Shot type breakdowns

## Troubleshooting

### "Unable to resolve host" Error
- Check backend is running on port 8080
- Verify emulator can reach 10.0.2.2
- Try `adb reverse tcp:8080 tcp:8080` command

### Connection Timeout
- Increase timeouts in NetworkModule
- Check backend isn't hanging on requests
- Verify MongoDB is running

### JSON Parsing Errors
- Check backend response format matches DTOs
- Look for null safety issues
- Verify enum values match (COMPLETE vs Complete)

### No Matches Appearing
- Check Logcat for HTTP errors
- Verify backend has matches: `curl localhost:8080/api/matches`
- Confirm RepositoryModule is using ApiMatchRepository

## Files Created/Modified

### New Files:
```
Frontend/app/src/main/java/com/example/myapplication/
├── data/api/
│   ├── AllanAIApiService.kt
│   ├── dto/
│   │   ├── MatchUploadResponse.kt
│   │   ├── MatchSummaryResponse.kt
│   │   ├── MatchDetailsResponse.kt
│   │   └── MatchStatusResponse.kt
│   └── mapper/
│       └── ResponseMapper.kt
├── data/repository/
│   └── ApiMatchRepository.kt
└── di/
    └── NetworkModule.kt
```

### Modified Files:
- `app/build.gradle.kts` - Added dependencies
- `di/RepositoryModule.kt` - Switched to ApiMatchRepository

## Summary

✅ **Phase 1 Complete!**
- Android app can communicate with backend
- All REST endpoints accessible
- Data properly mapped to domain models
- Ready for UI implementation in Phase 2

The foundation is solid and follows all architecture guidelines. The app is now ready to upload videos, fetch match data, and display statistics in the UI! 🎉
