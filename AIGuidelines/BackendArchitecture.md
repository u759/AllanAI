# AllanAI Backend Architecture (Spring Boot)

## Overview

The AllanAI backend is built with Spring Boot and provides RESTful APIs for video upload, processing orchestration, and statistics retrieval. It integrates with OpenCV for computer vision analysis of table tennis gameplay.

### Dataset & Event Annotation Strategy
- **Benchmark references**: TTNet (CVPR 2020 workshop) demonstrates multi-task inference on the OpenTTGames dataset; AllanAI extends these ideas with modular services and MongoDB persistence.
- **OpenTTGames alignment**: All matches processed by the backend inherit the dataset convention of 120 fps video, event annotations keyed by frame number, and auxiliary channels for ball coordinates and segmentation masks.
- **Timestamp guarantees**:
    - Every detected event stores both `timestampMs` (wall-clock) and `metadata.frameNumber` for precise frame-to-time mapping.
    - `metadata.eventWindow` preserves the −4/+12 frame slices used during training to provide consistent highlight previews.
    - MongoDB indexes on `{ "events.timestampMs": 1, "status": 1 }` support Compass-friendly exploration of timestamped data.
- **Model lifecycle**: Training scripts (external repository) export YOLO/TTNet-like weights; the backend consumes model outputs as JSON event streams or falls back to heuristic detection when model confidence is low.

## Architecture Pattern: Layered Architecture

```
┌─────────────────────────────────────────────────┐
│          Controller Layer (REST API)            │
│  - HTTP Request/Response handling               │
│  - Input validation                             │
│  - Response formatting                          │
└────────────────┬────────────────────────────────┘
                 │
┌─────────────────────────────────────────────────┐
│            Service Layer                        │
│  - Business logic                               │
│  - Transaction management                       │
│  - Orchestration                                │
└────────────────┬────────────────────────────────┘
                 │
┌─────────────────┴──────────┬────────────────────┐
│                            │                    │
▼                            ▼                    ▼
┌──────────────┐   ┌──────────────┐   ┌──────────────┐
│  Repository  │   │    OpenCV    │   │File Storage  │
│ (MongoDB)    │   │  Processing  │   │   Service    │
└──────────────┘   └──────────────┘   └──────────────┘
```

## Project Structure

Spring Data MongoDB is used for persistence.

```java
@Repository
public interface MatchRepository extends MongoRepository<MatchDocument, String> {

    List<MatchDocument> findAllByStatusOrderByCreatedAtDesc(MatchStatus status);

    @Aggregation("[{ $match: { _id: ?0 } }, { $project: { events: 1, statistics: 1 } }]")
    Optional<MatchDocument> projectAnalytics(String matchId);
}
```

- MongoDB's flexible document model allows us to embed statistics, shots, and events under a single `matches` collection document, mirroring the annotation bundles supplied by OpenTTGames.
- Compound indexes recommended:
  - `{ "status": 1, "createdAt": -1 }` for dashboards.
  - `{ "events.timestampMs": 1 }` for efficient event timeline queries and Compass visualisation.

```
backend/
The `MatchDocument` aggregate persists everything needed for playback, statistics, and highlight reconstruction.

```java
@Document("matches")
public class MatchDocument {
    @Id private String id;
    private Instant createdAt;
    private Instant processedAt;
    private MatchStatus status;
    private String originalFilename;
    private String videoPath;
    private Integer durationSeconds;
    private MatchStatistics statistics;
    private List<Shot> shots = new ArrayList<>();
    private List<Event> events = new ArrayList<>();
    private Highlights highlights;

    public static class Event {
        private String id;
        private Long timestampMs;
        private EventType type;
        private Integer player;
        private Integer importance;
        private EventMetadata metadata;
    }

    public static class EventMetadata {
        private Double shotSpeed;
        private Integer rallyLength;
        private String shotType;
        private Integer frameNumber;   // 120 fps-aligned source frame
        private EventWindow eventWindow;
        private List<List<Double>> ballTrajectory;
        private ScoreState scoreAfter;
        private Double confidence;
    }

    public static class EventWindow {
        private Integer preMs;   // usually 4 frames ≈ 33 ms each
        private Integer postMs;  // usually 12 frames
    }
}
```

> **Timestamp rule**: During ingestion, the backend converts the frame index supplied by the detector (`frameNumber`) to millisecond timestamps using the actual video FPS. This preserves alignment even when training data (fixed 120 fps) differs from live recordings.

├── backend/
│   ├── src/main/java/com/backend/backend/
Key configuration files align with asynchronous processing and MongoDB storage.

**AsyncConfig.java**
```java
@Configuration
@EnableAsync
public class AsyncConfig {
    @Bean(name = "matchProcessingExecutor")
    public Executor matchProcessingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(processingProperties.getThreads().getCore());
        executor.setMaxPoolSize(processingProperties.getThreads().getMax());
        executor.setQueueCapacity(processingProperties.getThreads().getQueueCapacity());
        executor.setThreadNamePrefix("match-processing-");
        executor.initialize();
        return executor;
    }
}
```

**application.properties**
```properties
spring.application.name=allanai-backend
server.port=8080

# Mongo configuration (Compass friendly)
spring.data.mongodb.uri=mongodb://localhost:27017/backend
spring.data.mongodb.auto-index-creation=true

# File system storage
storage.video-root=storage/videos
storage.max-file-size-mb=500

# Async processing defaults
processing.max-frame-samples=720
processing.motion-threshold=18.0
processing.threads.core=2
processing.threads.max=4
processing.threads.queue-capacity=100

spring.servlet.multipart.max-file-size=512MB
spring.servlet.multipart.max-request-size=512MB
```

**ProcessingProperties.java** binds the `processing.*` namespace and exposes values such as maximum frame samples (used to cap heuristic processing) and default pre-/post-event windows.

│   │   ├── BackendApplication.java
│   │   ├── config/
Match-specific exceptions are intentionally lightweight. Controllers raise `IllegalArgumentException` for missing matches, which is mapped to `404 NOT_FOUND` via an `@ExceptionHandler`. Storage errors bubble up as `ResponseStatusException` with helpful messages suitable for frontend display.

- Video streaming errors return `HttpStatus.NOT_FOUND` when the resource is missing and log the path for observability.
- Async processing failures update the corresponding `MatchDocument` status to `FAILED` with `processedAt` timestamps for traceability in MongoDB Compass.


**Guidelines**:
- Keep controllers thin - delegate to services
- Validate input with `@Valid`
- Return proper HTTP status codes
- Use `@RestController` and proper annotations
- Document with OpenAPI annotations

**MatchController.java**:
```java
@RestController
@RequestMapping("/api/matches")
public class MatchController {

    private final MatchService matchService;
    private final MatchProcessingService matchProcessingService;
    private final VideoStorageService videoStorageService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MatchUploadResponse> uploadMatch(@RequestParam("video") MultipartFile video,
                                                           UriComponentsBuilder uriBuilder) {
        String matchId = UUID.randomUUID().toString();
        String storedPath = videoStorageService.store(video, matchId);
        MatchDocument match = matchService.createMatch(matchId, video.getOriginalFilename(), storedPath);
        matchProcessingService.processAsync(match.getId());
        URI location = uriBuilder.path("/api/matches/{id}").buildAndExpand(match.getId()).toUri();
        return ResponseEntity.accepted().location(location)
            .body(new MatchUploadResponse(match.getId(), MatchStatus.PROCESSING));
    }

    @GetMapping
    public List<MatchSummaryResponse> listMatches() {
        return matchService.listMatches().stream().map(MatchMapper::toSummary).toList();
    }

    @GetMapping("/{id}/events")
    public List<EventResponse> getEvents(@PathVariable String id) {
        MatchDocument match = matchService.getById(id);
        return match.getEvents() == null ? List.of() : match.getEvents().stream()
            .map(MatchMapper::toEvent)
            .toList();
    }

    @GetMapping("/{id}/video")
    public ResponseEntity<Resource> streamVideo(@PathVariable String id) {
        MatchDocument match = matchService.getById(id);
        Resource video = videoStorageService.loadAsResource(match.getVideoPath());
        MediaType mediaType = MediaTypeFactory.getMediaType(video).orElse(MediaType.APPLICATION_OCTET_STREAM);
        return ResponseEntity.ok()
            .contentType(mediaType)
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + match.getOriginalFilename() + "\"")
            .body(video);
    }

    // Additional endpoints expose statistics, highlights, status, and deletion
}
```

**StatisticsController.java**:
```java
@RestController
@RequestMapping("/api/matches/{matchId}/statistics")
@RequiredArgsConstructor
@Tag(name = "Statistics", description = "APIs for match statistics and analytics")
public class StatisticsController {
    
    private final StatisticsService statisticsService;
    
    @GetMapping
    @Operation(summary = "Get comprehensive match statistics")
    public ResponseEntity<ApiResponse<StatisticsResponse>> getMatchStatistics(
            @PathVariable String matchId) {
        
        try {
            StatisticsResponse stats = statisticsService.getStatistics(matchId);
            return ResponseEntity.ok(ApiResponse.success(stats));
        } catch (MatchNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("MATCH_NOT_FOUND", e.getMessage()));
        }
    }
    
    @GetMapping("/shots")
    @Operation(summary = "Get detailed shot-by-shot data")
    public ResponseEntity<ApiResponse<List<ShotResponse>>> getShots(
            @PathVariable String matchId) {
        
        try {
            List<ShotResponse> shots = statisticsService.getShots(matchId);
            return ResponseEntity.ok(ApiResponse.success(shots));
        } catch (MatchNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("MATCH_NOT_FOUND", e.getMessage()));
        }
    }
}
```

### 2. Service Layer

**Purpose**: Business logic and orchestration

**MatchService.java**:
```java
@Service
public class MatchService {

    private final MatchRepository repository;

    public MatchService(MatchRepository repository) {
        this.repository = repository;
    }

    public MatchDocument createMatch(String matchId, String originalFilename, String videoPath) {
        MatchDocument match = new MatchDocument();
        match.setId(matchId);
        match.setCreatedAt(Instant.now());
        match.setStatus(MatchStatus.UPLOADED);
        match.setOriginalFilename(originalFilename);
        match.setVideoPath(videoPath);
        return repository.save(match);
    }

    public MatchDocument getById(String id) {
        return repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Match not found: " + id));
    }

    public List<MatchDocument> listMatches() {
        return repository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    public MatchDocument save(MatchDocument match) {
        return repository.save(match);
    }

    public void deleteMatch(MatchDocument match) {
        repository.delete(match);
    }
}
```

**MatchProcessingService.java** (excerpt):
```java
@Service
public class MatchProcessingService {

    @Async("matchProcessingExecutor")
    public void processAsync(String matchId) {
        MatchDocument match = matchService.getById(matchId);
        match.setStatus(MatchStatus.PROCESSING);
        matchService.save(match);
        try {
            processVideo(match);
            match.setStatus(MatchStatus.COMPLETE);
            match.setProcessedAt(Instant.now());
            matchService.save(match);
        } catch (Exception ex) {
            match.setStatus(MatchStatus.FAILED);
            match.setProcessedAt(Instant.now());
            matchService.save(match);
        }
    }

    private void processVideo(MatchDocument match) {
        VideoCapture capture = new VideoCapture(videoStorageService.resolvePath(match.getVideoPath()).toString());
        double fps = Math.max(capture.get(Videoio.CAP_PROP_FPS), 30.0);
        int durationSeconds = (int) Math.round(capture.get(Videoio.CAP_PROP_FRAME_COUNT) / fps);
        match.setDurationSeconds(durationSeconds);

        while (capture.read(frame) && processedFrames < maxSamples) {
            Imgproc.cvtColor(frame, gray, Imgproc.COLOR_BGR2GRAY);
            if (!prevGray.empty()) {
                double motionScore = Core.mean(diff).val[0];
                if (motionScore > 18.0) {
                    long timestampMs = Math.round((frameIndex / fps) * 1000.0);
                    Event event = buildEvent(timestampMs, motionScore, shot, runningScore, events.size());
                    // Event metadata stores frame index, −4/+12 frame window, and confidence values
                    events.add(event);
                }
            }
            gray.copyTo(prevGray);
            frameIndex++;
        }

        match.setStatistics(buildStatistics(events, shots, cumulativeSpeed, maxSpeed));
        match.setEvents(events);
        match.setShots(shots);
        match.setHighlights(buildHighlights(events));
    }
}
```
            // Update status
            updateStatus(matchId, MatchStatus.PROCESSING, 0);
            
            // Get match
            Match match = matchRepository.findById(matchId)
                    .orElseThrow(() -> new MatchNotFoundException("Match not found: " + matchId));
            
            // Get video file
            File videoFile = fileStorageService.getOriginalVideo(matchId);
            
            // Process with OpenCV
            log.info("Processing video with OpenCV: {}", videoFile.getAbsolutePath());
            OpenCVResult result = openCVProcessor.processVideo(
                    videoFile,
                    progress -> updateStatus(matchId, MatchStatus.PROCESSING, progress)
            );
            
            // Save processed video
            fileStorageService.saveProcessedVideo(matchId, result.getAnnotatedVideo());
            
            // Save statistics to database
            saveStatistics(match, result);
            
            // Update match status
            match.setStatus(MatchStatus.COMPLETE);
            match.setProcessedAt(LocalDateTime.now());
            match.setDurationSeconds(result.getDurationSeconds());
            matchRepository.save(match);
            
            updateStatus(matchId, MatchStatus.COMPLETE, 100);
            log.info("Match processing completed: {}", matchId);
            
        } catch (Exception e) {
            log.error("Match processing failed: " + matchId, e);
            updateStatus(matchId, MatchStatus.FAILED, 0, e.getMessage());
            
            // Update match status in DB
            matchRepository.findById(matchId).ifPresent(match -> {
                match.setStatus(MatchStatus.FAILED);
                match.setErrorMessage(e.getMessage());
                matchRepository.save(match);
            });
        }
        
        return CompletableFuture.completedFuture(null);
    }
    
    public ProcessingStatusResponse getStatus(String matchId) {
        // Check cache first
        if (statusCache.containsKey(matchId)) {
            return statusCache.get(matchId);
        }
        
        // Fall back to database
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new MatchNotFoundException("Match not found: " + matchId));
        
        return ProcessingStatusResponse.builder()
                .matchId(matchId)
                .status(match.getStatus().name())
                .progress(match.getStatus() == MatchStatus.COMPLETE ? 100 : 0)
                .errorMessage(match.getErrorMessage())
                .build();
    }
    
    private void updateStatus(String matchId, MatchStatus status, int progress) {
        updateStatus(matchId, status, progress, null);
    }
    
    private void updateStatus(String matchId, MatchStatus status, int progress, String errorMessage) {
        ProcessingStatusResponse statusResponse = ProcessingStatusResponse.builder()
                .matchId(matchId)
                .status(status.name())
                .progress(progress)
                .errorMessage(errorMessage)
                .build();
        
        statusCache.put(matchId, statusResponse);
    }
    
    private void saveStatistics(Match match, OpenCVResult result) {
        // Save match statistics
        MatchStatistics stats = new MatchStatistics();
        stats.setId(UUID.randomUUID().toString());
        stats.setMatch(match);
        stats.setPlayer1Score(result.getPlayer1Score());
        stats.setPlayer2Score(result.getPlayer2Score());
        stats.setTotalRallies(result.getTotalRallies());
        stats.setAvgRallyLength(result.getAvgRallyLength());
        stats.setMaxRallyLength(result.getMaxRallyLength());
        stats.setAvgBallSpeed(result.getAvgBallSpeed());
        stats.setMaxBallSpeed(result.getMaxBallSpeed());
        stats.setPlayer1Accuracy(result.getPlayer1Accuracy());
        stats.setPlayer2Accuracy(result.getPlayer2Accuracy());
        stats.setPlayer1ServeSuccess(result.getPlayer1ServeSuccess());
        stats.setPlayer2ServeSuccess(result.getPlayer2ServeSuccess());
        statisticsRepository.save(stats);
        
        // Save individual shots
        List<Shot> shots = result.getShots().stream()
                .map(shotData -> {
                    Shot shot = new Shot();
                    shot.setId(UUID.randomUUID().toString());
                    shot.setMatch(match);
                    shot.setTimestampMs(shotData.getTimestampMs());
                    shot.setPlayer(shotData.getPlayer());
                    shot.setShotType(shotData.getShotType());
                    shot.setSpeed(shotData.getSpeed());
                    shot.setAccuracy(shotData.getAccuracy());
                    shot.setResult(shotData.getResult());
                    return shot;
                })
                .collect(Collectors.toList());
        
        shotRepository.saveAll(shots);
    }
}
```

**StatisticsService.java**:
```java
@Service
@RequiredArgsConstructor
@Slf4j
public class StatisticsService {
    
    private final MatchRepository matchRepository;
    private final MatchStatisticsRepository statisticsRepository;
    private final ShotRepository shotRepository;
    
    public StatisticsResponse getStatistics(String matchId) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new MatchNotFoundException("Match not found: " + matchId));
        
        if (match.getStatus() != MatchStatus.COMPLETE) {
            throw new IllegalStateException("Match processing not complete");
        }
        
        MatchStatistics stats = statisticsRepository.findByMatchId(matchId)
                .orElseThrow(() -> new RuntimeException("Statistics not found for match: " + matchId));
        
        List<Shot> shots = shotRepository.findByMatchIdOrderByTimestampMs(matchId);
        
        return convertToResponse(stats, shots);
    }
    
    public List<ShotResponse> getShots(String matchId) {
        List<Shot> shots = shotRepository.findByMatchIdOrderByTimestampMs(matchId);
        
        return shots.stream()
                .map(this::convertShotToResponse)
                .collect(Collectors.toList());
    }
    
    private StatisticsResponse convertToResponse(MatchStatistics stats, List<Shot> shots) {
        return StatisticsResponse.builder()
                .matchId(stats.getMatch().getId())
                .player1Score(stats.getPlayer1Score())
                .player2Score(stats.getPlayer2Score())
                .totalRallies(stats.getTotalRallies())
                .avgRallyLength(stats.getAvgRallyLength())
                .maxRallyLength(stats.getMaxRallyLength())
                .avgBallSpeed(stats.getAvgBallSpeed())
                .maxBallSpeed(stats.getMaxBallSpeed())
                .player1Accuracy(stats.getPlayer1Accuracy())
                .player2Accuracy(stats.getPlayer2Accuracy())
                .player1ServeSuccess(stats.getPlayer1ServeSuccess())
                .player2ServeSuccess(stats.getPlayer2ServeSuccess())
                .shots(shots.stream()
                        .map(this::convertShotToResponse)
                        .collect(Collectors.toList()))
                .build();
    }
    
    private ShotResponse convertShotToResponse(Shot shot) {
        return ShotResponse.builder()
                .id(shot.getId())
                .timestampMs(shot.getTimestampMs())
                .player(shot.getPlayer())
                .shotType(shot.getShotType())
                .speed(shot.getSpeed())
                .accuracy(shot.getAccuracy())
                .result(shot.getResult())
                .build();
    }
}
```

**FileStorageService.java**:
```java
@Service
@Slf4j
public class FileStorageService {
    
    private final Path storageLocation;
    
    @Autowired
    public FileStorageService(FileStorageProperties properties) {
        this.storageLocation = Paths.get(properties.getUploadDir())
                .toAbsolutePath()
                .normalize();
        
        try {
            Files.createDirectories(this.storageLocation);
        } catch (IOException e) {
            throw new StorageException("Could not create upload directory", e);
        }
    }
    
    public String saveVideo(String matchId, MultipartFile video) {
        try {
            // Create match directory
            Path matchDir = storageLocation.resolve(matchId);
            Files.createDirectories(matchDir);
            
            // Save original video
            Path targetLocation = matchDir.resolve("original.mp4");
            Files.copy(video.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            
            log.info("Video saved: {}", targetLocation);
            return targetLocation.toString();
            
        } catch (IOException e) {
            throw new StorageException("Failed to store video file", e);
        }
    }
    
    public void saveProcessedVideo(String matchId, File processedVideo) {
        try {
            Path matchDir = storageLocation.resolve(matchId);
            Path targetLocation = matchDir.resolve("processed.mp4");
            Files.copy(processedVideo.toPath(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            
            log.info("Processed video saved: {}", targetLocation);
            
        } catch (IOException e) {
            throw new StorageException("Failed to store processed video", e);
        }
    }
    
    public File getOriginalVideo(String matchId) {
        Path videoPath = storageLocation.resolve(matchId).resolve("original.mp4");
        File videoFile = videoPath.toFile();
        
        if (!videoFile.exists()) {
            throw new StorageException("Video file not found: " + matchId);
        }
        
        return videoFile;
    }
    
    public File getProcessedVideo(String matchId) {
        Path videoPath = storageLocation.resolve(matchId).resolve("processed.mp4");
        File videoFile = videoPath.toFile();
        
        if (!videoFile.exists()) {
            throw new StorageException("Processed video not found: " + matchId);
        }
        
        return videoFile;
    }
    
    public void deleteMatchFiles(String matchId) {
        try {
            Path matchDir = storageLocation.resolve(matchId);
            if (Files.exists(matchDir)) {
                Files.walk(matchDir)
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
            log.info("Match files deleted: {}", matchId);
            
        } catch (IOException e) {
            log.error("Failed to delete match files: " + matchId, e);
        }
    }
}
```


