# Part 1: Basic Video Processing & Table Detection - Implementation Plan

## Overview
Part 1 focuses on establishing the foundational OpenCV integration to process uploaded videos and detect the table tennis table in each frame. This is the critical first step that enables all subsequent analysis (ball tracking, player detection, scoring).

## Objectives
1. Set up OpenCV processing pipeline
2. Implement basic video frame extraction
3. Detect table boundaries in frames
4. Save processed frame metadata
5. Verify compilation and functionality with tests

---

## Implementation Tasks

### 1. OpenCV Service Layer Foundation

#### 1.1 Create Base OpenCV Processor
**File**: `backend/backend/src/main/java/com/backend/backend/opencv/OpenCVProcessor.java`

**Purpose**: Core service that orchestrates video processing

**Responsibilities**:
- Accept video file path and match ID
- Extract frames from video using OpenCV VideoCapture
- Coordinate with specialized detectors (TableDetector)
- Store frame analysis results
- Handle processing errors gracefully

**Key Methods**:
```java
public ProcessingResult processVideo(String videoPath, String matchId)
public List<FrameAnalysis> extractFrames(String videoPath)
public void saveProcessingResults(String matchId, ProcessingResult result)
```

**Dependencies**:
- OpenCV VideoCapture for video reading
- TableDetector for table boundary detection
- MatchRepository for updating match status
- Async execution via @Async annotation

---

#### 1.2 Create Table Detection Module
**File**: `backend/backend/src/main/java/com/backend/backend/opencv/TableDetector.java`

**Purpose**: Detect table tennis table boundaries in video frames

**Algorithm Approach**:
1. **Preprocessing**:
   - Convert frame to HSV color space
   - Apply color filtering to isolate green/blue table surface
   - Use Gaussian blur to reduce noise

2. **Edge Detection**:
   - Apply Canny edge detection
   - Find contours in the edge-detected image

3. **Table Identification**:
   - Filter contours by area (table should be large)
   - Approximate contour to polygon (should be ~4 corners)
   - Verify aspect ratio matches table tennis table (~2.74:1.525 = 1.8:1)

4. **Boundary Extraction**:
   - Extract corner points of the detected table
   - Calculate center point and dimensions
   - Detect net location (vertical line in center)

**Key Methods**:
```java
public TableBoundary detectTable(Mat frame)
private Mat preprocessFrame(Mat frame)
private List<MatOfPoint> findTableContours(Mat preprocessed)
private TableBoundary extractBoundary(MatOfPoint contour)
public boolean validateTableDetection(TableBoundary boundary)
```

**Output**: TableBoundary object containing:
- Four corner coordinates (top-left, top-right, bottom-left, bottom-right)
- Center point
- Net location
- Confidence score (0.0 - 1.0)

---

### 2. Domain Models

#### 2.1 Create FrameAnalysis Model
**File**: `backend/backend/src/main/java/com/backend/backend/opencv/model/FrameAnalysis.java`

**Purpose**: Store analysis results for a single frame

**Fields**:
```java
private int frameNumber;
private long timestampMs;
private TableBoundary tableBoundary;
private boolean tableDetected;
private double detectionConfidence;
```

---

#### 2.2 Create TableBoundary Model
**File**: `backend/backend/src/main/java/com/backend/backend/opencv/model/TableBoundary.java`

**Purpose**: Store detected table boundary information

**Fields**:
```java
private Point topLeft;
private Point topRight;
private Point bottomLeft;
private Point bottomRight;
private Point center;
private Point netStart;
private Point netEnd;
private double width;
private double height;
private double confidence;
```

---

#### 2.3 Create ProcessingResult Model
**File**: `backend/backend/src/main/java/com/backend/backend/opencv/model/ProcessingResult.java`

**Purpose**: Aggregate results from entire video processing

**Fields**:
```java
private String matchId;
private int totalFrames;
private int framesWithTableDetected;
private double averageTableConfidence;
private List<FrameAnalysis> frameAnalyses;
private LocalDateTime processingStartTime;
private LocalDateTime processingEndTime;
private ProcessingStatus status; // SUCCESS, FAILED, PARTIAL
private String errorMessage;
```

---

### 3. Service Integration

#### 3.1 Update MatchProcessingService
**File**: `backend/backend/src/main/java/com/backend/backend/service/MatchProcessingService.java`

**Changes**:
- Inject OpenCVProcessor dependency
- Modify `processMatchAsync()` to invoke OpenCVProcessor
- Update match status based on processing results
- Store frame analysis metadata in MatchDocument

**Updated Flow**:
```java
@Async
public CompletableFuture<Void> processMatchAsync(String matchId) {
    // 1. Retrieve match from repository
    // 2. Get video file path
    // 3. Call OpenCVProcessor.processVideo()
    // 4. Update match status to COMPLETE or FAILED
    // 5. Save frame analysis results
    // 6. Log processing metrics
}
```

---

#### 3.2 Update MatchDocument Model
**File**: `backend/backend/src/main/java/com/backend/backend/model/MatchDocument.java`

**Add Fields**:
```java
private Integer totalFramesProcessed;
private Integer framesWithTableDetected;
private Double tableDetectionRate;
private List<FrameAnalysisDTO> sampleFrames; // Store every Nth frame for debugging
```

---

### 4. Configuration & Properties

#### 4.1 Update ProcessingProperties
**File**: `backend/backend/src/main/java/com/backend/backend/config/ProcessingProperties.java`

**Add Properties**:
```java
private int frameSampleRate = 10; // Process every 10th frame for efficiency
private double tableDetectionThreshold = 0.7; // Minimum confidence score
private boolean saveDebugFrames = false; // Save annotated frames for debugging
private String debugFramesPath = "data/debug-frames/";
```

---

#### 4.2 Update application.properties
**File**: `backend/backend/src/main/resources/application.properties`

**Add Configuration**:
```properties
# OpenCV Processing Configuration
processing.frame-sample-rate=10
processing.table-detection-threshold=0.7
processing.save-debug-frames=false
processing.debug-frames-path=data/debug-frames/

# Video Processing
processing.max-video-duration-minutes=30
processing.max-video-size-mb=500
```

---

### 5. Testing Strategy

#### 5.1 Unit Tests

##### TableDetectorTest.java
**File**: `backend/backend/src/test/java/com/backend/backend/opencv/TableDetectorTest.java`

**Test Cases**:
```java
@Test
void testDetectTable_ValidFrame_ReturnsTableBoundary()
// Verify table detection on a known good frame

@Test
void testDetectTable_NoTable_ReturnsNull()
// Verify graceful handling when no table present

@Test
void testValidateTableDetection_ValidBoundary_ReturnsTrue()
// Test validation logic for aspect ratio, area

@Test
void testPreprocessFrame_AppliesFiltersCorrectly()
// Verify preprocessing steps work as expected
```

**Test Data**:
- Create `src/test/resources/test-videos/` folder
- Add sample frame images with known table positions
- Use OpenCV to load test images in tests

---

##### OpenCVProcessorTest.java
**File**: `backend/backend/src/test/java/com/backend/backend/opencv/OpenCVProcessorTest.java`

**Test Cases**:
```java
@Test
void testProcessVideo_ValidVideo_ReturnsSuccess()
// End-to-end test with a short test video

@Test
void testExtractFrames_ValidVideo_ReturnsFrameList()
// Verify frame extraction works correctly

@Test
void testProcessVideo_CorruptedVideo_ThrowsException()
// Test error handling for invalid videos

@Test
void testProcessVideo_SavesResultsToDatabase()
// Verify database integration
```

---

#### 5.2 Integration Tests

##### MatchProcessingIntegrationTest.java
**File**: `backend/backend/src/test/java/com/backend/backend/service/MatchProcessingIntegrationTest.java`

**Test Cases**:
```java
@Test
void testFullProcessingPipeline_UploadToCompletion()
// Test: Upload video → Async processing → Verify results

@Test
void testProcessingStatus_UpdatesCorrectly()
// Verify UPLOADED → PROCESSING → COMPLETE status flow

@Test
void testTableDetectionMetrics_StoredInDatabase()
// Confirm frame analysis data is persisted
```

**Setup**:
- Use @SpringBootTest annotation
- Mock or use embedded MongoDB
- Use test video file from resources

---

### 6. Verification & Validation

#### 6.1 Manual Testing Script
**File**: `backend/backend/scripts/test-part1.sh` (create this)

**Script Steps**:
```bash
#!/bin/bash

echo "=== Part 1: Table Detection Testing ==="

# 1. Start the Spring Boot application
echo "Starting backend..."
mvn spring-boot:run &
BACKEND_PID=$!
sleep 10

# 2. Upload a test video
echo "Uploading test video..."
curl -X POST http://localhost:8080/api/matches/upload \
  -F "video=@test-resources/sample-match.mp4" \
  -F "player1Name=Test Player 1" \
  -F "player2Name=Test Player 2"

# 3. Wait for processing
echo "Waiting for processing..."
sleep 30

# 4. Check match status
echo "Checking match status..."
curl http://localhost:8080/api/matches/{matchId}/status

# 5. Verify table detection metrics
echo "Retrieving table detection metrics..."
curl http://localhost:8080/api/matches/{matchId}

# Clean up
kill $BACKEND_PID
```

---

#### 6.2 Compilation Verification

**Command**: `mvn clean compile`

**Expected Output**:
```
[INFO] Building backend 0.0.1-SNAPSHOT
[INFO] Compiling XX source files
[INFO] BUILD SUCCESS
```

**Verify**:
- All new classes compile without errors
- OpenCV dependency is resolved
- No missing imports or syntax errors

---

#### 6.3 Test Execution

**Command**: `mvn test`

**Success Criteria**:
- All unit tests pass (TableDetectorTest, OpenCVProcessorTest)
- Integration tests pass (MatchProcessingIntegrationTest)
- Test coverage > 70% for new classes
- No critical errors in logs

---

### 7. Success Metrics

#### Functional Requirements Met:
- ✅ Video files can be loaded by OpenCV
- ✅ Frames are extracted at configurable intervals
- ✅ Table boundaries are detected with >70% confidence
- ✅ Frame analysis results are stored in database
- ✅ Processing status updates correctly (UPLOADED → PROCESSING → COMPLETE)

#### Performance Requirements:
- Process 1080p video at minimum 10 fps frame extraction
- Table detection completes in <100ms per frame
- Full video processing (5 min video) completes in <5 minutes

#### Code Quality:
- All new code compiles successfully
- Unit test coverage >70%
- Integration tests verify end-to-end flow
- No critical SonarQube violations

---

## Implementation Order

### Phase 1: Models & Foundation (Day 1)
1. Create TableBoundary model
2. Create FrameAnalysis model
3. Create ProcessingResult model
4. Update configuration properties

### Phase 2: Core OpenCV Logic (Day 2-3)
5. Implement TableDetector with preprocessing and contour detection
6. Write unit tests for TableDetector
7. Implement OpenCVProcessor for video frame extraction
8. Write unit tests for OpenCVProcessor

### Phase 3: Service Integration (Day 4)
9. Update MatchProcessingService to invoke OpenCVProcessor
10. Update MatchDocument with frame analysis fields
11. Test service integration

### Phase 4: Testing & Validation (Day 5)
12. Create integration tests
13. Manual testing with sample videos
14. Fix bugs and edge cases
15. Verify compilation and all tests pass

---

## Folder Structure (After Part 1)

```
backend/backend/src/main/java/com/backend/backend/
├── config/
│   ├── AsyncConfig.java (existing)
│   ├── OpenCvInitializer.java (existing)
│   ├── VideoStorageProperties.java (existing)
│   └── ProcessingProperties.java (updated)
│
├── opencv/
│   ├── OpenCVProcessor.java (NEW)
│   ├── TableDetector.java (NEW)
│   └── model/
│       ├── FrameAnalysis.java (NEW)
│       ├── TableBoundary.java (NEW)
│       └── ProcessingResult.java (NEW)
│
├── service/
│   ├── MatchProcessingService.java (updated)
│   ├── MatchService.java (existing)
│   └── VideoStorageService.java (existing)
│
├── model/
│   └── MatchDocument.java (updated)
│
└── ... (other existing files)

backend/backend/src/test/
├── java/com/backend/backend/
│   ├── opencv/
│   │   ├── TableDetectorTest.java (NEW)
│   │   └── OpenCVProcessorTest.java (NEW)
│   └── service/
│       └── MatchProcessingIntegrationTest.java (NEW)
│
└── resources/
    └── test-videos/
        ├── sample-frame-with-table.jpg (NEW - test data)
        └── sample-match-short.mp4 (NEW - test data)
```

---

## Dependencies Check

### Current Dependencies (from pom.xml):
- ✅ Spring Boot 3.5.6
- ✅ Spring Data MongoDB
- ✅ Spring Validation
- ✅ OpenCV 4.9.0-0 (openpnp)

### Additional Dependencies Needed:
**None** - All required dependencies are already included.

---

## Risk Mitigation

### Risk 1: OpenCV native library loading failures
**Mitigation**:
- OpenCvInitializer already exists with error handling
- Test on target deployment platform early
- Document platform-specific requirements

### Risk 2: Table detection fails on certain lighting conditions
**Mitigation**:
- Implement adaptive thresholding
- Test with diverse video samples
- Add debug frame saving to visualize detection

### Risk 3: Processing takes too long for long videos
**Mitigation**:
- Process every Nth frame (configurable)
- Implement progress reporting
- Add timeout configuration

### Risk 4: Video codec compatibility issues
**Mitigation**:
- Test with multiple video formats (MP4, AVI, MOV)
- Document supported formats
- Add video validation before processing

---

## Deliverables Checklist

- [ ] TableBoundary.java implemented and tested
- [ ] FrameAnalysis.java implemented and tested
- [ ] ProcessingResult.java implemented and tested
- [ ] TableDetector.java implemented with full detection algorithm
- [ ] OpenCVProcessor.java implemented for frame extraction
- [ ] MatchProcessingService.java updated and integrated
- [ ] MatchDocument.java updated with frame analysis fields
- [ ] ProcessingProperties.java updated with new configs
- [ ] application.properties updated
- [ ] TableDetectorTest.java with >70% coverage
- [ ] OpenCVProcessorTest.java with >70% coverage
- [ ] MatchProcessingIntegrationTest.java end-to-end test
- [ ] Test video resources added
- [ ] Manual testing script created
- [ ] **Compilation verified**: `mvn clean compile` succeeds
- [ ] **Tests verified**: `mvn test` all pass
- [ ] Documentation updated (inline JavaDocs)

---

## Next Steps (Future Parts)

**Part 2**: Ball Tracking & Motion Detection
- Implement BallTracker using contour detection
- Track ball position across frames
- Calculate ball speed and trajectory

**Part 3**: Player Detection & Positioning
- Implement PlayerDetector
- Track player movements
- Identify which player hit the ball

**Part 4**: Scoring Logic & Game State
- Implement ScoreCalculator
- Detect ball contact with table/net
- Maintain game score state

**Part 5**: Event Detection & Highlights
- Detect scoring moments
- Identify rally highlights
- Generate timestamped events

---

## How to Verify Part 1 Works

### Step-by-Step Verification:

1. **Compile the project**:
   ```bash
   cd backend/backend
   mvn clean compile
   ```
   **Expected**: BUILD SUCCESS with no compilation errors

2. **Run unit tests**:
   ```bash
   mvn test -Dtest=TableDetectorTest
   mvn test -Dtest=OpenCVProcessorTest
   ```
   **Expected**: All tests pass

3. **Run integration test**:
   ```bash
   mvn test -Dtest=MatchProcessingIntegrationTest
   ```
   **Expected**: Full pipeline test passes

4. **Start the application**:
   ```bash
   mvn spring-boot:run
   ```
   **Expected**: Application starts with "OpenCV native libraries loaded successfully" in logs

5. **Upload a test video via API**:
   ```bash
   curl -X POST http://localhost:8080/api/matches/upload \
     -F "video=@test-video.mp4" \
     -F "player1Name=Player 1" \
     -F "player2Name=Player 2"
   ```
   **Expected**: Returns 200 OK with matchId

6. **Check processing status**:
   ```bash
   curl http://localhost:8080/api/matches/{matchId}/status
   ```
   **Expected**: Status changes from UPLOADED → PROCESSING → COMPLETE

7. **Verify table detection results**:
   ```bash
   curl http://localhost:8080/api/matches/{matchId}
   ```
   **Expected**: Response includes:
   - `totalFramesProcessed` > 0
   - `framesWithTableDetected` > 0
   - `tableDetectionRate` between 0.0 and 1.0

8. **Check logs for processing metrics**:
   ```bash
   tail -f logs/spring-boot-application.log
   ```
   **Expected**: Logs show:
   - "Processing match {matchId} started"
   - "Extracted {N} frames from video"
   - "Table detected in {N} frames"
   - "Processing match {matchId} completed"

---

## Architecture Compliance

This implementation follows the Architecture.md guidelines:

✅ **Layered Architecture**: Controller → Service → OpenCV Processing → Repository

✅ **Folder Structure**: Matches `backend/src/main/java/com/allanai/opencv/` pattern

✅ **Asynchronous Processing**: Uses @Async for video processing

✅ **Error Handling**: Graceful degradation, meaningful error messages

✅ **Testing**: Comprehensive unit and integration tests

✅ **Configuration**: Externalized in application.properties

✅ **No Frontend Changes**: Backend-only implementation

---

## Estimated Timeline

- **Day 1**: Models and configuration (4 hours)
- **Day 2-3**: TableDetector and OpenCVProcessor implementation (12 hours)
- **Day 4**: Service integration and testing (6 hours)
- **Day 5**: Bug fixes, verification, documentation (4 hours)

**Total**: ~26 hours (approximately 1 week with normal working hours)

---

**Last Updated**: October 4, 2025
**Version**: 1.0.0
**Status**: Ready for Implementation
