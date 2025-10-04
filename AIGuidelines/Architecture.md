# AllanAI Architecture Guidelines

## Project Overview

**AllanAI** is a table tennis gameplay analysis application that records matches, processes them using AI/OpenCV, and provides detailed statistics and insights to players.

### Core Features
- Record table tennis gameplay via mobile device
- Automated tracking of ball, table, net, and players
- Real-time scoring and statistics
- Performance analytics (shot accuracy, ball speed, serve success rate)
- Historical match data and trends
- **Video playback with timestamped events** (play of the game, scoring moments, misses)
- **Interactive event navigation** - jump to key moments in match video

### Technology Stack
- **Frontend**: Android with Jetpack Compose
- **Backend**: Spring Boot (REST API)
- **AI/ML**: OpenCV for computer vision, potential integration with ML models
- **Storage**: File system for videos, database for match statistics
- **Repository**: Monorepo structure (single repository)

### Research & Dataset Baseline
- **Reference paper**: *TTNet: Real-time temporal and spatial video analysis of table tennis* (CVPR 2020 workshop) – baseline for multi-task event spotting, ball tracking, and semantic segmentation.
- **OpenTTGames dataset** (OSAI Labs):
  - 12 full-HD training videos (120 fps) + 7 test clips, each distributed with JSON markup for **event frame numbers** and **ball coordinates**.
  - Event annotations use keys formatted as `frameNumber: eventName` with three primary labels (ball bounce, net hit, empty) and 4 pre-event / 12 post-event frames enriched with ball trajectories.
  - Segmentation masks are supplied per annotated frame (`{frame}.png`) with channel-wise encoding for players, table, and scoreboard.
  - Dataset ships alongside zipped metadata that can be browsed locally with MongoDB Compass after import, enabling direct inspection of timestamped events.
- **Internal dataset curation**: AllanAI mirrors this structure—every detected or inferred match event stores both the absolute video timestamp (`timestampMs`) and the originating frame index so downstream services can reconstruct event windows.

## System Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     AllanAI System                          │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌──────────────────┐         ┌──────────────────┐        │
│  │  Android Client  │         │  Spring Backend  │        │
│  │                  │         │                  │        │
│  │  Jetpack Compose │◄───────►│   REST API       │        │
│  │                  │  HTTP   │                  │        │
│  │  - Record Video  │         │  - Video Upload  │        │
│  │  - View Stats    │         │  - Processing    │        │
│  │  - Match History │         │  - Statistics    │        │
│  │  - Video Playback│         │  - Event Detect  │        │
│  └──────────────────┘         └─────────┬────────┘        │
│                                          │                  │
│                                          ▼                  │
│                              ┌──────────────────┐          │
│                              │  OpenCV Engine   │          │
│                              │                  │          │
│                              │  - Ball Tracking │          │
│                              │  - Table Detect  │          │
│                              │  - Scoring Logic │          │
│                              │  - Speed Calc    │          │
│                              │  - Event Detect  │          │
│                              └─────────┬────────┘          │
│                                        │                    │
│                                        ▼                    │
│                              ┌──────────────────┐          │
│                              │  Data Storage    │          │
│                              │                  │          │
│                              │  - Videos (FS)   │          │
│                              │  - Stats (DB)    │          │
│                              └──────────────────┘          │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## Architecture Principles

### 1. Simplicity First
- Prioritize clarity over abstraction
- Add complexity only when needed
- Keep layers focused and minimal
- Avoid over-engineering

### 2. Single Repository (Monorepo)
- All code in one repository
- Shared documentation and versioning
- Easier coordination between frontend and backend
- Consistent tooling and CI/CD

### 3. Clear Separation of Concerns
- **Frontend**: UI rendering and user interaction
- **Backend**: Business logic and orchestration
- **OpenCV**: Computer vision processing
- **Storage**: Data persistence

### 4. API-First Design
- Define API contracts before implementation
- Version APIs to support updates
- Comprehensive error handling
- Clear request/response models

### 5. Asynchronous Processing
- Video processing is long-running → use async
- Non-blocking API endpoints
- Polling or webhooks for status updates
- Background job processing

### 6. Performance Optimization
- Efficient video encoding/decoding
- Frame-by-frame processing optimization
- Caching of processed results
- MongoDB indexing for queries (compound indexes on match status, timestamps)

### 7. Error Handling
- Graceful degradation
- Meaningful error messages
- Retry logic for transient failures
- Comprehensive logging

## Repository Structure

```
allanai/
├── README.md
├── docs/
│   ├── architecture-guidelines.md (this file)
│   ├── frontend-architecture.md
│   ├── backend-architecture.md
│   └── opencv-integration.md
│
├── android/                        # Jetpack Compose Frontend
│   ├── app/
│   │   ├── src/
│   │   │   ├── main/
│   │   │   │   ├── java/com/allanai/
│   │   │   │   │   ├── ui/
│   │   │   │   │   ├── viewmodel/
│   │   │   │   │   ├── repository/
│   │   │   │   │   ├── api/
│   │   │   │   │   ├── model/
│   │   │   │   │   └── di/
│   │   │   │   └── res/
│   │   │   └── test/
│   │   ├── build.gradle.kts
│   │   └── proguard-rules.pro
│   ├── gradle/
│   ├── build.gradle.kts
│   └── settings.gradle.kts
│
├── backend/                        # Spring Boot Backend
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/allanai/
│   │   │   │   ├── controller/
│   │   │   │   ├── service/
│   │   │   │   ├── repository/
│   │   │   │   ├── model/
│   │   │   │   ├── opencv/
│   │   │   │   ├── config/
│   │   │   │   └── AllanAIApplication.java
│   │   │   └── resources/
│   │   │       ├── application.properties
│   │   │       └── db/migration/
│   │   └── test/
│   ├── build.gradle
│   └── pom.xml (or build.gradle)
│
├── shared/                         # Shared models/contracts
│   └── api-contracts/
│       ├── openapi.yaml
│       └── README.md
│
├── scripts/                        # Utility scripts
│   ├── setup.sh
│   ├── run-local.sh
│   └── deploy.sh
│
├── .github/
│   └── workflows/
│       ├── android-ci.yml
│       └── backend-ci.yml
│
└── docker-compose.yml             # Local development setup
```

## Data Flow Overview

### 1. Video Recording Flow
```
User Records Match
    ↓
Android Camera API
    ↓
Local Storage (temp)
    ↓
Upload to Backend (multipart/form-data)
    ↓
Spring Controller receives video
    ↓
Save to File System
    ↓
Trigger OpenCV Processing (async)
    ↓
Return matchId to client
```

### 2. Video Processing Flow
```
OpenCV Processor receives video file
    ↓
Initialize video capture
    ↓
For each frame:
    - Detect table boundaries
    - Track ball position
    - Detect net location
    - Identify players
    - Calculate ball speed
    - Detect table contact
    - Detect out-of-bounds
    ↓
Calculate statistics:
    - Shot accuracy
    - Serve success rate
    - Rally length
    - Ball speed (avg/max)
    - Scoring
    ↓
Detect and timestamp events:
    - Score changes (with timestamp)
    - Rally highlights (long/exciting rallies)
    - Play of the game (best rally)
    - Fastest shots
    - Misses/errors
    - Serve aces
  - Persist source frame numbers and 120 fps-aligned event windows (−4/+12 frame slices)
    ↓
Generate highlight reels:
    - Auto-select top moments
    - Rank by importance
    - Create event timeline
    ↓
Save statistics, events, and highlights to database
    ↓
Update processing status
```

### 3. Statistics Retrieval Flow
```
User navigates to Stats screen
    ↓
Android UI requests match list
    ↓
Spring API returns matches with stats
    ↓
User selects specific match
    ↓
Android requests detailed analytics
    ↓
Spring returns comprehensive stats
    ↓
Display graphs, charts, insights
```

### 4. Video Playback with Events Flow
```
User selects match to watch
    ↓
Android requests match events
    ↓
Spring API returns timestamped events:
    - Play of the game
    - Scoring moments
    - Rally highlights
    - Error/miss events
    ↓
Android displays video with event timeline
    ↓
User taps event (e.g., "Play of the Game")
    ↓
Video player seeks to event timestamp
    ↓
Playback starts from that moment
    ↓
Optional: Display overlay annotations
    (ball trajectory, speed, shot type)
```

## API Design Patterns

### RESTful Endpoints
```
POST   /api/matches/upload          # Upload video
GET    /api/matches                 # List all matches
GET    /api/matches/{id}            # Get match details
GET    /api/matches/{id}/status     # Processing status
GET    /api/matches/{id}/statistics # Detailed stats
GET    /api/matches/{id}/video      # Download/stream processed video
GET    /api/matches/{id}/events     # Get timestamped events
GET    /api/matches/{id}/events/{eventId}  # Get specific event details
GET    /api/matches/{id}/highlights # Get auto-generated highlights
DELETE /api/matches/{id}            # Delete match
```

### Response Format
```json
{
  "success": true,
  "data": { ... },
  "error": null,
  "timestamp": "2025-10-04T10:30:00Z"
}
```

### Error Format
```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "PROCESSING_FAILED",
    "message": "Failed to detect table in video",
    "details": "..."
  },
  "timestamp": "2025-10-04T10:30:00Z"
}
```

## Technology Decisions

### Frontend (Android)
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Architecture**: MVVM (View-ViewModel-Repository)
- **Dependency Injection**: Hilt
- **Networking**: Retrofit + OkHttp
- **Async**: Kotlin Coroutines + Flow
- **Video Recording**: CameraX
- **Video Playback**: ExoPlayer (for video streaming with seek controls)
- **Charts/Graphs**: Vico or MPAndroidChart
- **Video Caching**: ExoPlayer cache for offline playback

### Backend (Spring Boot)
- **Language**: Java 17+ or Kotlin
- **Framework**: Spring Boot 3.x
- **Database**: MongoDB (for statistics)
- **ODM**: Spring Data MongoDB
- **Async**: Spring Async + CompletableFuture
- **File Storage**: Local file system (can migrate to S3)
- **API Documentation**: SpringDoc OpenAPI
- **Testing**: JUnit 5, Mockito

### OpenCV Integration
- **Library**: OpenCV 4.x (Java bindings)
- **Processing**: Frame-by-frame analysis
- **Models**: Pre-trained models for object detection (optional)
- **Optimization**: Multi-threading for frame processing
- **Output**: Annotated video + JSON statistics

### Database Schema (MongoDB Collections)
```javascript
// Matches collection
{
  "_id": ObjectId,
  "createdAt": ISODate,
  "videoPath": String,
  "status": String, // UPLOADED, PROCESSING, COMPLETE, FAILED
  "durationSeconds": Number,
  "processedAt": ISODate,
  "statistics": {
    "player1Score": Number,
    "player2Score": Number,
    "totalRallies": Number,
    "avgRallyLength": Number,
    "maxBallSpeed": Number,
    "avgBallSpeed": Number
  },
  "shots": [
    {
      "timestampMs": Number,
      "player": Number, // 1 or 2
      "shotType": String, // SERVE, FOREHAND, BACKHAND
      "speed": Number,
      "accuracy": Number,
      "result": String // IN, OUT, NET
    }
  ],
  "events": [
    {
      "_id": ObjectId,
      "timestampMs": Number,
      "type": String, // PLAY_OF_GAME, SCORE, MISS, RALLY_HIGHLIGHT, SERVE_ACE
      "title": String,
      "description": String,
      "player": Number, // 1 or 2, or null for both
      "importance": Number, // 1-10 rating for highlight priority
      "metadata": {
        "shotSpeed": Number,
        "rallyLength": Number,
        "shotType": String,
        "ballTrajectory": [[Number]], // Array of [x, y] coordinates
        "frameNumber": Number,        // Source frame as provided by detectors
        "eventWindow": {
          "preMs": Number,           // Typically 4 frames (≈33ms each) before event
          "postMs": Number           // Typically 12 frames after event
        },
        "confidence": Number,         // Model confidence score 0-1
        "source": String              // MANUAL | MODEL | HYBRID
      }
    }
  ],
  "highlights": {
    "playOfTheGame": ObjectId, // Reference to events._id
    "topRallies": [ObjectId],
    "fastestShots": [ObjectId],
    "bestServes": [ObjectId]
  }
}
```

## Video Playback & Event System

### Event Detection Algorithm

The OpenCV processing engine automatically detects and timestamps key events during match analysis:

#### Event Types & Detection Logic

1. **PLAY_OF_THE_GAME**
   - Longest rally with high shot quality
   - Multiple direction changes
   - High ball speeds
   - Complex shot patterns
   - Auto-selected as single most impressive moment

2. **SCORE Events**
   - Detected when ball lands out or hits net
   - Point awarded to appropriate player
   - Includes score state before/after

3. **RALLY_HIGHLIGHT**
   - Rallies with 8+ shots
   - Fast-paced exchanges (>5 shots/sec avg)
   - High-speed shots (>30 km/h)
   - Ranked by importance (1-10)

4. **SERVE_ACE**
   - Serve that results in immediate point
   - No return shot detected
   - High serve speed

5. **MISS/ERROR Events**
   - Ball lands significantly out of bounds
   - Ball hits net on non-serve shots
   - Failed returns

6. **FASTEST_SHOT**
   - Top 5 fastest shots in match
   - Includes ball speed and trajectory

### Video Player Implementation

#### Frontend (Android with ExoPlayer)

```kotlin
// Key features of video player
- Seek to specific timestamp (millisecond precision)
- Display event markers on timeline/scrubber
- Overlay annotations during playback:
  * Ball trajectory visualization
  * Shot speed display
  * Player position tracking
- Event timeline UI component:
  * Clickable event chips
  * Visual importance indicators
  * Filterable by event type
- Playback controls:
  * Play/pause
  * Speed adjustment (0.5x, 1x, 2x)
  * Frame-by-frame stepping for analysis
```

#### Backend Video Streaming

```java
// Spring Boot video streaming endpoint
- Range request support (HTTP 206 Partial Content)
- Chunked transfer for smooth playback
- HLS or DASH streaming for adaptive bitrate (future)
- Cache headers for performance
- Event metadata bundled with video requests
```

### Event Timeline UI Design

```
┌─────────────────────────────────────────────────────┐
│  Match Video Player                                 │
│  ┌───────────────────────────────────────────────┐  │
│  │                                               │  │
│  │         [Video Frame Display]                 │  │
│  │                                               │  │
│  │  Overlay: "Fastest Shot - 45 km/h"           │  │
│  └───────────────────────────────────────────────┘  │
│                                                     │
│  ──●────────────●──●────────────●──────────────►   │
│  0:00  ⚡    1:23 🏆 1:45    3:12 ❌      5:00   │
│                                                     │
│  Quick Jump Events:                                │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐  │
│  │ 🏆 Play of  │ │ ⚡ Fastest  │ │ 🎯 Best     │  │
│  │   the Game  │ │   Shot      │ │   Rally     │  │
│  │   1:23      │ │   0:15      │ │   3:45      │  │
│  └─────────────┘ └─────────────┘ └─────────────┘  │
│                                                     │
│  All Events:                                       │
│  [🎾 Score] [⚡ Fast Shot] [❌ Miss] [🎯 Rally]   │
│                                                     │
└─────────────────────────────────────────────────────┘
```

### Event Response Format

```json
{
  "success": true,
  "data": {
    "matchId": "507f1f77bcf86cd799439011",
    "events": [
      {
        "id": "507f1f77bcf86cd799439012",
        "timestampMs": 83500,
        "type": "PLAY_OF_THE_GAME",
        "title": "Play of the Game",
        "description": "Epic 15-shot rally with incredible speed",
        "importance": 10,
        "player": null,
        "metadata": {
          "rallyLength": 15,
          "maxShotSpeed": 45.2,
          "avgShotSpeed": 32.8,
          "frameNumber": 2505
        }
      },
      {
        "id": "507f1f77bcf86cd799439013",
        "timestampMs": 15200,
        "type": "FASTEST_SHOT",
        "title": "Fastest Shot",
        "description": "Player 1 forehand smash",
        "importance": 8,
        "player": 1,
        "metadata": {
          "shotSpeed": 52.3,
          "shotType": "FOREHAND",
          "ballTrajectory": [[120, 340], [245, 280], [380, 220]],
          "frameNumber": 456
        }
      },
      {
        "id": "507f1f77bcf86cd799439014",
        "timestampMs": 92000,
        "type": "SCORE",
        "title": "Point Scored",
        "description": "Player 2 scores on backhand",
        "importance": 5,
        "player": 2,
        "metadata": {
          "scoreAfter": {"player1": 8, "player2": 11},
          "shotType": "BACKHAND",
          "frameNumber": 2760
        }
      }
    ],
    "highlights": {
      "playOfTheGame": "507f1f77bcf86cd799439012",
      "topRallies": [
        "507f1f77bcf86cd799439012",
        "507f1f77bcf86cd799439018"
      ],
      "fastestShots": [
        "507f1f77bcf86cd799439013",
        "507f1f77bcf86cd799439019"
      ]
    }
  }
}
```

### Performance Considerations

#### Video Streaming
- **Progressive Download**: Start playback before full download
- **Seek Optimization**: Index keyframes for instant seeking
- **Bandwidth Adaptation**: Detect connection speed, adjust quality
- **Local Caching**: Cache processed videos on device for offline viewing

#### Event Loading
- **Lazy Loading**: Load events as needed during playback
- **Prefetching**: Preload events near current playback position
- **Thumbnail Generation**: Create thumbnails at event timestamps

### OpenCV Event Detection Pseudocode

```python
def detect_events(video_frames, shots_data):
    events = []
    
    # Detect Play of the Game
    longest_rally = find_longest_rally(shots_data)
    if longest_rally.length > 10:
        events.append({
            'type': 'PLAY_OF_THE_GAME',
            'timestampMs': longest_rally.start_time,
            'importance': 10,
            'metadata': {
                'rallyLength': longest_rally.length,
                'avgSpeed': longest_rally.avg_speed
            }
        })
    
    # Detect Scoring Moments
    for shot in shots_data:
        if shot.result == 'OUT' or shot.result == 'NET':
            events.append({
                'type': 'SCORE',
                'timestampMs': shot.timestamp,
                'player': shot.scoring_player,
                'importance': 5
            })
    
    # Detect Fastest Shots
    fastest_shots = sorted(shots_data, key=lambda x: x.speed, reverse=True)[:5]
    for shot in fastest_shots:
        events.append({
            'type': 'FASTEST_SHOT',
            'timestampMs': shot.timestamp,
            'importance': 8,
            'metadata': {'shotSpeed': shot.speed}
        })
    
    # Detect Rally Highlights
    for rally in find_rallies(shots_data):
        if rally.length >= 8 or rally.avg_speed > 30:
            events.append({
                'type': 'RALLY_HIGHLIGHT',
                'timestampMs': rally.start_time,
                'importance': min(rally.length / 2, 10),
                'metadata': {'rallyLength': rally.length}
            })
    
    return sorted(events, key=lambda x: x['timestampMs'])
```

## Development Workflow

### 1. Local Development
```bash
# Start backend
cd backend
./gradlew bootRun

# Start Android emulator
cd android
./gradlew installDebug

# Or use Android Studio
```

### 2. Testing
```bash
# Backend tests
cd backend
./gradlew test

# Android tests
cd android
./gradlew test
./gradlew connectedAndroidTest
```

### 3. Building
```bash
# Backend JAR
cd backend
./gradlew build

# Android APK
cd android
./gradlew assembleDebug
```

## Deployment Strategy

### Backend Deployment
- Container: Docker image
- Platform: Cloud VM, AWS ECS, or Kubernetes
- Database: Managed MongoDB (MongoDB Atlas, AWS DocumentDB)
- File Storage: S3-compatible storage

### Android Distribution
- Initial: APK direct download
- Future: Google Play Store

## Security Considerations

### Authentication & Authorization
- JWT-based authentication
- Secure token storage on Android (EncryptedSharedPreferences)
- Token refresh mechanism

### Data Privacy
- User videos stored securely
- Option to delete videos after processing
- Anonymized statistics

### API Security
- HTTPS only in production
- Rate limiting to prevent abuse
- Input validation on all endpoints
- File type and size validation

## Performance Requirements

### Video Processing
- Process 1080p video at 30fps
- Target: <5 minutes processing time for 5-minute match
- Frame processing: >10 fps

### API Response Times
- Upload endpoint: Accept quickly, process async
- List matches: <500ms
- Get statistics: <200ms

### Mobile App
- Smooth 60fps UI
- Video recording: 1080p @ 30fps minimum
- Local caching of match history

## Future Enhancements

### Phase 2 Features
- Live match tracking (real-time processing)
- Multi-angle video support
- Social features (share matches, compare stats)
- AI coaching suggestions
- Custom training drills

### Technical Improvements
- ML model for shot classification
- Real-time streaming processing
- Cloud-based processing for scalability
- Mobile-optimized ML models (TensorFlow Lite)

## Key Decisions Log

| Decision | Rationale | Date |
|----------|-----------|------|
| Monorepo structure | Easier coordination, shared docs | 2025-10-04 |
| Async processing | Video processing is CPU-intensive | 2025-10-04 |
| File system storage | Simpler for MVP, can migrate later | 2025-10-04 |
| MongoDB | Flexible schema, good for nested data structures | 2025-10-04 |
| Jetpack Compose | Modern Android UI, declarative | 2025-10-04 |

## Getting Started

### Prerequisites
- Android Studio Hedgehog or later
- JDK 17+
- Gradle 8.0+
- MongoDB 6.0+ (with MongoDB Compass for local development)
- OpenCV 4.x installed

### Quick Start
```bash
# Clone repository
git clone https://github.com/yourorg/allanai.git
cd allanai

# Setup backend
cd backend
./gradlew bootRun

# In another terminal, setup Android
cd android
./gradlew installDebug
```

### Documentation
- **Frontend Architecture**: See `frontend-architecture.md`
- **Backend Architecture**: See `backend-architecture.md`
- **OpenCV Integration**: See `opencv-integration.md`
- **API Reference**: See `/api/swagger-ui`

## Support & Contact

- **Issues**: GitHub Issues
- **Documentation**: `/docs` directory
- **API Docs**: http://localhost:8080/swagger-ui

---

**Last Updated**: October 4, 2025
**Version**: 1.0.0