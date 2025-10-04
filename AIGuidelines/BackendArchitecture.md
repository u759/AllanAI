# AllanAI Backend Architecture (Spring Boot)

## Overview

The AllanAI backend is built with Spring Boot and provides RESTful APIs for video upload, processing orchestration, and statistics retrieval. It integrates with OpenCV for computer vision analysis of table tennis gameplay.

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
│   (JPA/DB)   │   │  Processing  │   │   Service    │
└──────────────┘   └──────────────┘   └──────────────┘
```

## Project Structure

```
backend/src/main/java/com/allanai/
├── AllanAIApplication.java
│
├── controller/
│   ├── MatchController.java
│   ├── StatisticsController.java
│   └── HealthController.java
│
├── service/
│   ├── MatchService.java
│   ├── ProcessingService.java
│   ├── StatisticsService.java
│   ├── FileStorageService.java
│   └── NotificationService.java
│
├── opencv/
│   ├── OpenCVProcessor.java
│   ├── TableDetector.java
│   ├── BallTracker.java
│   ├── PlayerDetector.java
│   ├── ScoreCalculator.java
│   └── model/
│       ├── DetectionResult.java
│       ├── BallPosition.java
│       ├── TableBoundary.java
│       └── FrameAnalysis.java
│
├── repository/
│   ├── MatchRepository.java
│   ├── MatchStatisticsRepository.java
│   └── ShotRepository.java
│
├── model/
│   ├── entity/
│   │   ├── Match.java
│   │   ├── MatchStatistics.java
│   │   └── Shot.java
│   │
│   └── dto/
│       ├── request/
│       │   └── UploadRequest.java
│       │
│       └── response/
│           ├── ApiResponse.java
│           ├── MatchResponse.java
│           ├── StatisticsResponse.java
│           └── ProcessingStatusResponse.java
│
├── config/
│   ├── OpenCVConfig.java
│   ├── AsyncConfig.java
│   ├── CorsConfig.java
│   ├── SecurityConfig.java
│   └── FileStorageProperties.java
│
├── exception/
│   ├── GlobalExceptionHandler.java
│   ├── MatchNotFoundException.java
│   ├── ProcessingException.java
│   └── StorageException.java
│
└── util/
    ├── Constants.java
    └── VideoUtils.java

resources/
├── application.properties
├── application-dev.properties
├── application-prod.properties
└── db/
    └── migration/
        ├── V1__create_matches_table.sql
        ├── V2__create_statistics_table.sql
        └── V3__create_shots_table.sql
```

## Layer Responsibilities

### 1. Controller Layer

**Purpose**: Handle HTTP requests and responses

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
@RequiredArgsConstructor
@Tag(name = "Match Management", description = "APIs for managing table tennis matches")
public class MatchController {
    
    private final MatchService matchService;
    private final ProcessingService processingService;
    
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a match video for processing")
    public ResponseEntity<ApiResponse<UploadResponse>> uploadMatch(
            @RequestParam("video") MultipartFile video) {
        
        if (video.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("INVALID_FILE", "Video file is required"));
        }
        
        // Validate file type
        String contentType = video.getContentType();
        if (contentType == null || !contentType.startsWith("video/")) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("INVALID_FILE_TYPE", 
                            "Only video files are allowed"));
        }
        
        // Validate file size (500MB max)
        if (video.getSize() > 500 * 1024 * 1024) {
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                    .body(ApiResponse.error("FILE_TOO_LARGE", 
                            "Video must be smaller than 500MB"));
        }
        
        try {
            String matchId = matchService.createMatch(video);
            return ResponseEntity.ok(
                    ApiResponse.success(new UploadResponse(matchId))
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("UPLOAD_FAILED", e.getMessage()));
        }
    }
    
    @GetMapping
    @Operation(summary = "Get all matches")
    public ResponseEntity<ApiResponse<List<MatchResponse>>> getAllMatches(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        List<MatchResponse> matches = matchService.getAllMatches(page, size);
        return ResponseEntity.ok(ApiResponse.success(matches));
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Get match details by ID")
    public ResponseEntity<ApiResponse<MatchResponse>> getMatchById(
            @PathVariable String id) {
        
        try {
            MatchResponse match = matchService.getMatchById(id);
            return ResponseEntity.ok(ApiResponse.success(match));
        } catch (MatchNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("MATCH_NOT_FOUND", e.getMessage()));
        }
    }
    
    @GetMapping("/{id}/status")
    @Operation(summary = "Get processing status of a match")
    public ResponseEntity<ApiResponse<ProcessingStatusResponse>> getProcessingStatus(
            @PathVariable String id) {
        
        try {
            ProcessingStatusResponse status = processingService.getStatus(id);
            return ResponseEntity.ok(ApiResponse.success(status));
        } catch (MatchNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("MATCH_NOT_FOUND", e.getMessage()));
        }
    }
    
    @GetMapping("/{id}/video")
    @Operation(summary = "Download processed video")
    public ResponseEntity<Resource> downloadVideo(@PathVariable String id) {
        
        try {
            File videoFile = matchService.getProcessedVideo(id);
            Resource resource = new FileSystemResource(videoFile);
            
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                            "attachment; filename=\"" + videoFile.getName() + "\"")
                    .body(resource);
        } catch (MatchNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a match and its associated data")
    public ResponseEntity<ApiResponse<Void>> deleteMatch(@PathVariable String id) {
        
        try {
            matchService.deleteMatch(id);
            return ResponseEntity.ok(ApiResponse.success(null));
        } catch (MatchNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("MATCH_NOT_FOUND", e.getMessage()));
        }
    }
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
@RequiredArgsConstructor
@Slf4j
public class MatchService {
    
    private final MatchRepository matchRepository;
    private final FileStorageService fileStorageService;
    private final ProcessingService processingService;
    
    @Transactional
    public String createMatch(MultipartFile video) {
        log.info("Creating new match from uploaded video: {}", video.getOriginalFilename());
        
        // Create match entity
        Match match = new Match();
        match.setId(UUID.randomUUID().toString());
        match.setCreatedAt(LocalDateTime.now());
        match.setStatus(MatchStatus.UPLOADED);
        match.setOriginalFilename(video.getOriginalFilename());
        match.setFileSize(video.getSize());
        
        // Save video file
        String videoPath = fileStorageService.saveVideo(match.getId(), video);
        match.setVideoPath(videoPath);
        
        // Save match to database
        matchRepository.save(match);
        
        // Trigger async processing
        processingService.processMatchAsync(match.getId());
        
        log.info("Match created successfully with ID: {}", match.getId());
        return match.getId();
    }
    
    public List<MatchResponse> getAllMatches(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Match> matchPage = matchRepository.findAll(pageable);
        
        return matchPage.getContent().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }
    
    public MatchResponse getMatchById(String id) {
        Match match = matchRepository.findById(id)
                .orElseThrow(() -> new MatchNotFoundException("Match not found: " + id));
        return convertToResponse(match);
    }
    
    public File getProcessedVideo(String matchId) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new MatchNotFoundException("Match not found: " + matchId));
        
        if (match.getStatus() != MatchStatus.COMPLETE) {
            throw new IllegalStateException("Match processing not complete");
        }
        
        return fileStorageService.getProcessedVideo(matchId);
    }
    
    @Transactional
    public void deleteMatch(String id) {
        Match match = matchRepository.findById(id)
                .orElseThrow(() -> new MatchNotFoundException("Match not found: " + id));
        
        // Delete video files
        fileStorageService.deleteMatchFiles(id);
        
        // Delete from database (cascade deletes statistics and shots)
        matchRepository.delete(match);
        
        log.info("Match deleted: {}", id);
    }
    
    private MatchResponse convertToResponse(Match match) {
        return MatchResponse.builder()
                .id(match.getId())
                .createdAt(match.getCreatedAt().toString())
                .status(match.getStatus().name())
                .durationSeconds(match.getDurationSeconds())
                .processedAt(match.getProcessedAt() != null ? 
                        match.getProcessedAt().toString() : null)
                .thumbnailUrl(generateThumbnailUrl(match.getId()))
                .build();
    }
    
    private String generateThumbnailUrl(String matchId) {
        return "/api/matches/" + matchId + "/thumbnail";
    }
}
```

**ProcessingService.java**:
```java
@Service
@RequiredArgsConstructor
@Slf4j
public class ProcessingService {
    
    private final MatchRepository matchRepository;
    private final MatchStatisticsRepository statisticsRepository;
    private final ShotRepository shotRepository;
    private final OpenCVProcessor openCVProcessor;
    private final FileStorageService fileStorageService;
    private final Map<String, ProcessingStatusResponse> statusCache = new ConcurrentHashMap<>();
    
    @Async("taskExecutor")
    @Transactional
    public CompletableFuture<Void> processMatchAsync(String matchId) {
        log.info("Starting async processing for match: {}", matchId);
        
        try {
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

### 3. Repository Layer

**MatchRepository.java**:
```java
@Repository
public interface MatchRepository extends JpaRepository<Match, String> {
    
    List<Match> findByStatus(MatchStatus status);
    
    @Query("SELECT m FROM Match m WHERE m.createdAt >= :startDate AND m.createdAt <= :endDate")
    List<Match> findByDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
}
```

**MatchStatisticsRepository.java**:
```java
@Repository
public interface MatchStatisticsRepository extends JpaRepository<MatchStatistics, String> {
    
    Optional<MatchStatistics> findByMatchId(String matchId);
}
```

**ShotRepository.java**:
```java
@Repository
public interface ShotRepository extends JpaRepository<Shot, String> {
    
    List<Shot> findByMatchIdOrderByTimestampMs(String matchId);
    
    @Query("SELECT s FROM Shot s WHERE s.match.id = :matchId AND s.player = :player")
    List<Shot> findByMatchIdAndPlayer(
            @Param("matchId") String matchId,
            @Param("player") int player
    );
}
```

### 4. Entity Models

**Match.java**:
```java
@Entity
@Table(name = "matches")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Match {
    
    @Id
    private String id;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "processed_at")
    private LocalDateTime processedAt;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MatchStatus status;
    
    @Column(name = "video_path")
    private String videoPath;
    
    @Column(name = "original_filename")
    private String originalFilename;
    
    @Column(name = "file_size")
    private Long fileSize;
    
    @Column(name = "duration_seconds")
    private Integer durationSeconds;
    
    @Column(name = "error_message")
    private String errorMessage;
    
    @OneToOne(mappedBy = "match", cascade = CascadeType.ALL, orphanRemoval = true)
    private MatchStatistics statistics;
    
    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Shot> shots;
}

enum MatchStatus {
    UPLOADED, PROCESSING, COMPLETE, FAILED
}
```

**MatchStatistics.java**:
```java
@Entity
@Table(name = "match_statistics")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MatchStatistics {
    
    @Id
    private String id;
    
    @OneToOne
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;
    
    @Column(name = "player1_score")
    private Integer player1Score;
    
    @Column(name = "player2_score")
    private Integer player2Score;
    
    @Column(name = "total_rallies")
    private Integer totalRallies;
    
    @Column(name = "avg_rally_length")
    private Float avgRallyLength;
    
    @Column(name = "max_rally_length")
    private Integer maxRallyLength;
    
    @Column(name = "avg_ball_speed")
    private Float avgBallSpeed;
    
    @Column(name = "max_ball_speed")
    private Float maxBallSpeed;
    
    @Column(name = "player1_accuracy")
    private Float player1Accuracy;
    
    @Column(name = "player2_accuracy")
    private Float player2Accuracy;
    
    @Column(name = "player1_serve_success")
    private Float player1ServeSuccess;
    
    @Column(name = "player2_serve_success")
    private Float player2ServeSuccess;
}
```

**Shot.java**:
```java
@Entity
@Table(name = "shots")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Shot {
    
    @Id
    private String id;
    
    @ManyToOne
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;
    
    @Column(name = "timestamp_ms", nullable = false)
    private Long timestampMs;
    
    @Column(nullable = false)
    private Integer player; // 1 or 2
    
    @Column(name = "shot_type")
    private String shotType; // SERVE, FOREHAND, BACKHAND
    
    @Column
    private Float speed;
    
    @Column
    private Float accuracy;
    
    @Column
    private String result; // IN, OUT, NET
}
```

### 5. Configuration

**AsyncConfig.java**:
```java
@Configuration
@EnableAsync
public class AsyncConfig {
    
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("opencv-processing-");
        executor.initialize();
        return executor;
    }
}
```

**CorsConfig.java**:
```java
@Configuration
public class CorsConfig implements WebMvcConfigurer {
    
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:3000", "http://10.0.2.2:8080")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
```

**FileStorageProperties.java**:
```java
@Configuration
@ConfigurationProperties(prefix = "file")
@Data
public class FileStorageProperties {
    
    private String uploadDir = "/var/allanai/videos";
}
```

**application.properties**:
```properties
# Server Configuration
server.port=8080
spring.application.name=allanai-backend

# Database Configuration
spring.datasource.url=jdbc:postgresql://localhost:5432/allanai
spring.datasource.username=allanai
spring.datasource.password=changeme
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect

# File Upload Configuration
spring.servlet.multipart.enabled=true
spring.servlet.multipart.max-file-size=500MB
spring.servlet.multipart.max-request-size=500MB

# File Storage
file.upload-dir=/var/allanai/videos

# Async Configuration
spring.task.execution.pool.core-size=2
spring.task.execution.pool.max-size=4
spring.task.execution.pool.queue-capacity=100

# Logging
logging.level.root=INFO
logging.level.com.allanai=DEBUG
logging.file.name=/var/log/allanai/application.log

# OpenAPI Documentation
springdoc.api-docs.path=/api-docs
springdoc.swagger-ui.path=/swagger-ui
```

### 6. Exception Handling

**GlobalExceptionHandler.java**:
```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    @ExceptionHandler(MatchNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleMatchNotFound(MatchNotFoundException e) {
        log.error("Match not found", e);
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("MATCH_NOT_FOUND", e.getMessage()));
    }
    
    @ExceptionHandler(StorageException.class)
    public ResponseEntity<ApiResponse<Void>> handleStorageException(StorageException e) {
        log.error("Storage error", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("STORAGE_ERROR", e.getMessage()));
    }
    
    @ExceptionHandler(ProcessingException.class)
    public ResponseEntity<ApiResponse<Void>> handleProcessingException(ProcessingException e) {
        log.error("Processing error", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("PROCESSING_ERROR", e.getMessage()));
    }
    
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxSizeException(
            MaxUploadSizeExceededException e) {
        log.error("File size exceeded", e);
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(ApiResponse.error("FILE_TOO_LARGE", "File size exceeds maximum limit"));
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception e) {
        log# AllanAI Backend Architecture (Spring Boot)

