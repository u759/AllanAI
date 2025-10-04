# AllanAI Architecture Guidelines

## Project Overview

**AllanAI** is a table tennis gameplay analysis application that records matches, processes them using AI/OpenCV, and provides detailed statistics and insights to players.

### Core Features
- Record table tennis gameplay via mobile device
- Automated tracking of ball, table, net, and players
- Real-time scoring and statistics
- Performance analytics (shot accuracy, ball speed, serve success rate)
- Historical match data and trends

### Technology Stack
- **Frontend**: Android with Jetpack Compose
- **Backend**: Spring Boot (REST API)
- **AI/ML**: OpenCV for computer vision, potential integration with ML models
- **Storage**: File system for videos, database for match statistics
- **Repository**: Monorepo structure (single repository)

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
- Database indexing for queries

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
Save statistics to database
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

## API Design Patterns

### RESTful Endpoints
```
POST   /api/matches/upload          # Upload video
GET    /api/matches                 # List all matches
GET    /api/matches/{id}            # Get match details
GET    /api/matches/{id}/status     # Processing status
GET    /api/matches/{id}/statistics # Detailed stats
GET    /api/matches/{id}/video      # Download processed video
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
- **Charts/Graphs**: Vico or MPAndroidChart

### Backend (Spring Boot)
- **Language**: Java 17+ or Kotlin
- **Framework**: Spring Boot 3.x
- **Database**: PostgreSQL (for statistics)
- **ORM**: Spring Data JPA
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

### Database Schema (Simplified)
```sql
-- Matches table
matches (
  id UUID PRIMARY KEY,
  created_at TIMESTAMP,
  video_path VARCHAR,
  status VARCHAR, -- UPLOADED, PROCESSING, COMPLETE, FAILED
  duration_seconds INT,
  processed_at TIMESTAMP
)

-- Match statistics
match_statistics (
  id UUID PRIMARY KEY,
  match_id UUID REFERENCES matches(id),
  player1_score INT,
  player2_score INT,
  total_rallies INT,
  avg_rally_length FLOAT,
  max_ball_speed FLOAT,
  avg_ball_speed FLOAT
)

-- Shot details
shots (
  id UUID PRIMARY KEY,
  match_id UUID REFERENCES matches(id),
  timestamp_ms BIGINT,
  player INT, -- 1 or 2
  shot_type VARCHAR, -- SERVE, FOREHAND, BACKHAND
  speed FLOAT,
  accuracy FLOAT,
  result VARCHAR -- IN, OUT, NET
)
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
- Database: Managed PostgreSQL (AWS RDS, Google Cloud SQL)
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
| PostgreSQL | Robust, good for time-series data | 2025-10-04 |
| Jetpack Compose | Modern Android UI, declarative | 2025-10-04 |

## Getting Started

### Prerequisites
- Android Studio Hedgehog or later
- JDK 17+
- Gradle 8.0+
- PostgreSQL 14+
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