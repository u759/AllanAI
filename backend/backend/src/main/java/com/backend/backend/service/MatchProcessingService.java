package com.backend.backend.service;

import com.backend.backend.config.ModelProperties;
import com.backend.backend.config.ProcessingProperties;
import com.backend.backend.model.EventType;
import com.backend.backend.model.MatchDocument;
import com.backend.backend.model.MatchDocument.Event;
import com.backend.backend.model.MatchDocument.EventMetadata;
import com.backend.backend.model.MatchDocument.EventWindow;
import com.backend.backend.model.MatchDocument.Detection;
import com.backend.backend.model.MatchDocument.Highlights;
import com.backend.backend.model.MatchDocument.HighlightRef;
import com.backend.backend.model.MatchDocument.MatchStatistics;
import com.backend.backend.model.MatchDocument.ScoreState;
import com.backend.backend.model.MatchDocument.Shot;
import com.backend.backend.model.MatchDocument.ProcessingSummary;
import com.backend.backend.model.MatchStatus;
import com.backend.backend.model.ShotResult;
import com.backend.backend.model.ShotType;
import com.backend.backend.service.model.ModelInferenceResult;
import com.backend.backend.service.model.ModelInferenceResult.ModelEvent;
import com.backend.backend.service.model.ModelInferenceResult.ModelDetection;
import com.backend.backend.service.model.ModelInferenceResult.ModelShot;
import com.backend.backend.service.model.ModelInferenceResult.ModelStatistics;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
public class MatchProcessingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MatchProcessingService.class);

    private final MatchService matchService;
    private final VideoStorageService videoStorageService;
    private final ProcessingProperties processingProperties;
    private final ModelEventDetectionService modelEventDetectionService;
    private final ModelProperties modelProperties;

    public MatchProcessingService(MatchService matchService,
                                  VideoStorageService videoStorageService,
                                  ProcessingProperties processingProperties,
                                  ModelEventDetectionService modelEventDetectionService,
                                  ModelProperties modelProperties) {
        this.matchService = matchService;
        this.videoStorageService = videoStorageService;
        this.processingProperties = processingProperties;
        this.modelEventDetectionService = modelEventDetectionService;
        this.modelProperties = modelProperties;
    }

    @Async("matchProcessingExecutor")
    public void processAsync(String matchId) {
        LOGGER.info("Starting asynchronous processing for match {}", matchId);
        MatchDocument match = matchService.getById(matchId);
        match.setStatus(MatchStatus.PROCESSING);
        matchService.save(match);
        try {
            processVideo(match);
            match.setStatus(MatchStatus.COMPLETE);
            LOGGER.info("Finished processing match {}", matchId);
        } catch (Exception ex) {
            LOGGER.error("Processing failed for match {}", matchId, ex);
            match.setStatus(MatchStatus.FAILED);
        } finally {
            match.setProcessedAt(Instant.now());
            matchService.save(match);
        }
    }

    private void processVideo(MatchDocument match) {
        Path videoPath = videoStorageService.resolvePath(match.getVideoPath());
        VideoMetadata metadata = readVideoMetadata(videoPath);
        match.setDurationSeconds(metadata.durationSeconds());

        Optional<ModelInferenceResult> inference = modelEventDetectionService.detect(match.getId(), videoPath)
            .map(result -> result.normalize(modelProperties));

        if (inference.isPresent() && !inference.get().getEvents().isEmpty()) {
            LOGGER.info("Applying research model output for match {}", match.getId());
            applyModelResults(match, inference.get(), metadata);
        } else {
            LOGGER.warn("Research model unavailable or returned no events for match {}; falling back to heuristic processing", match.getId());
            applyHeuristicProcessing(match, videoPath, metadata, inference.isPresent());
        }
    }

    private void applyModelResults(MatchDocument match, ModelInferenceResult result, VideoMetadata videoMetadata) {
        double fps = result.getFps() == null || result.getFps() <= 0 ? videoMetadata.fps() : result.getFps();

        List<Shot> shots = new ArrayList<>();
        for (ModelShot modelShot : result.getShots()) {
            Shot shot = new Shot();
            long timestamp = resolveTimestamp(modelShot.getFrame(), null, fps);
            shot.setTimestampMs(timestamp);
            shot.setTimestampSeries(deriveTimestampSeries(modelShot.getTimestampSeries(), timestamp, modelProperties.getPreEventFrames(), modelProperties.getPostEventFrames(), fps));
            shot.setFrameSeries(deriveFrameSeries(modelShot.getFrameSeries(), modelShot.getFrame()));
            shot.setPlayer(modelShot.getPlayer() == null ? 1 : modelShot.getPlayer());
            shot.setShotType(resolveShotType(modelShot.getShotType()));
            shot.setSpeed(safeDouble(modelShot.getSpeed(), 0.0));
            shot.setAccuracy(safeDouble(modelShot.getAccuracy(), 80.0));
            shot.setResult(resolveShotResult(modelShot.getResult()));
            shot.setDetections(convertDetections(modelShot.getDetections(), modelShot.getFrame(), modelShot.getConfidence()));
            shots.add(shot);
        }

        List<Event> events = new ArrayList<>();
        ScoreState runningScore = new ScoreState(0, 0);
        for (ModelEvent modelEvent : result.getEvents()) {
            Event event = new Event();
            event.setId(UUID.randomUUID().toString());
            EventType type = resolveEventType(modelEvent);
            event.setType(type);
            long timestampMs = resolveTimestamp(modelEvent.getFrame(), modelEvent.getTimestampMs(), fps);
            event.setTimestampMs(timestampMs);
            event.setTimestampSeries(deriveTimestampSeries(modelEvent.getTimestampSeries(), timestampMs, modelEvent.getPreEventFrames(), modelEvent.getPostEventFrames(), fps));
            event.setFrameSeries(deriveFrameSeries(modelEvent.getFrameSeries(), modelEvent.getFrameNumber()));
            event.setPlayer(modelEvent.getPlayer());
            event.setImportance(modelEvent.getImportance() == null ? 6 : modelEvent.getImportance());
            event.setTitle(resolveEventTitle(type));
            event.setDescription(resolveEventDescription(type, modelEvent));

            EventMetadata metadata = new EventMetadata();
            metadata.setShotSpeed(modelEvent.getShotSpeed());
            metadata.setRallyLength(modelEvent.getRallyLength());
            metadata.setShotType(resolveShotType(modelEvent.getShotType()).name());
            metadata.setBallTrajectory(modelEvent.getBallTrajectory() == null ? List.of() : modelEvent.getBallTrajectory());
            metadata.setFrameNumber(resolveFrameNumber(modelEvent, fps));
            metadata.setFrameSeries(new ArrayList<>(event.getFrameSeries()));
            metadata.setEventWindow(buildEventWindow(modelEvent, fps));
            metadata.setConfidence(modelEvent.getConfidence());
            metadata.setSource("MODEL");
            metadata.setDetections(convertDetections(modelEvent.getDetections(), modelEvent.getFrameNumber(), modelEvent.getConfidence()));
            event.setMetadata(metadata);

            events.add(event);
            updateScore(runningScore, event);
        }

        if (events.isEmpty()) {
            LOGGER.warn("Model output returned zero events; reprocessing with heuristic pipeline");
            applyHeuristicProcessing(match, videoStorageService.resolvePath(match.getVideoPath()), videoMetadata, true);
            return;
        }

        boolean synthesizedShots = false;
        if (shots.isEmpty()) {
            LOGGER.warn("Model output returned zero shots; synthesizing shots from events");
            shots = synthesizeShotsFromEvents(events, fps);
            synthesizedShots = true;
        }

        MatchStatistics statistics = buildStatisticsFromModel(result.getStatistics(), events, shots);
        Highlights highlights = buildHighlights(events);

        match.setStatistics(statistics);
        match.setEvents(events);
        match.setShots(shots);
        match.setHighlights(highlights);

        ProcessingSummary summary = new ProcessingSummary();
        summary.setPrimarySource("MODEL");
        summary.setHeuristicFallbackUsed(synthesizedShots);
        summary.getSources().add("MODEL");
        if (synthesizedShots) {
            summary.getSources().add("HEURISTIC_SYNTHESIZED_SHOTS");
            summary.getNotes().add("Synthesized shots from events because model output did not include shot data.");
        }
        match.setProcessingSummary(summary);
    }

    private void applyHeuristicProcessing(MatchDocument match, Path videoPath, VideoMetadata metadata, boolean invokedAsFallback) {
        VideoCapture capture = new VideoCapture(videoPath.toString());
        if (!capture.isOpened()) {
            throw new IllegalStateException("Unable to open video: " + videoPath);
        }

        Mat prevGray = new Mat();
        Mat frame = new Mat();
        Mat gray = new Mat();
        Mat diff = new Mat();

        List<Event> events = new ArrayList<>();
        List<Shot> shots = new ArrayList<>();
        ScoreState runningScore = new ScoreState(0, 0);

        double fps = metadata.fps();
        int maxSamples = Math.max(300, processingProperties.getMaxFrameSamples());
        double threshold = processingProperties.getMotionThreshold();

        int frameIndex = 0;
        int processedFrames = 0;
        double cumulativeSpeed = 0.0;
        double maxSpeed = 0.0;

        try {
            while (capture.read(frame)) {
                if (frame.empty()) {
                    break;
                }
                Imgproc.cvtColor(frame, gray, Imgproc.COLOR_BGR2GRAY);
                if (!prevGray.empty()) {
                    Core.absdiff(gray, prevGray, diff);
                    Scalar mean = Core.mean(diff);
                    double motionScore = mean.val[0];
                    if (motionScore > threshold) {
                        long timestampMs = Math.round((frameIndex / fps) * 1000.0);
                        double estimatedSpeed = Math.min(65.0, threshold + motionScore);
                        cumulativeSpeed += estimatedSpeed;
                        maxSpeed = Math.max(maxSpeed, estimatedSpeed);
                        Shot shot = buildHeuristicShot(timestampMs, estimatedSpeed, events.size(), fps);
                        shots.add(shot);
                        Event event = buildHeuristicEvent(timestampMs, motionScore, shot, runningScore, events.size(), fps);
                        events.add(event);
                        updateScore(runningScore, event);
                    }
                }
                gray.copyTo(prevGray);
                frameIndex++;
                processedFrames++;
                if (processedFrames >= maxSamples) {
                    break;
                }
            }
        } finally {
            prevGray.release();
            frame.release();
            gray.release();
            diff.release();
            capture.release();
        }

        if (events.isEmpty()) {
            events.add(buildFallbackEvent());
        }

        if (shots.isEmpty()) {
            shots.add(buildFallbackShot());
        }

        MatchStatistics statistics = buildStatistics(events, shots, cumulativeSpeed, maxSpeed);
        Highlights highlights = buildHighlights(events);

        match.setStatistics(statistics);
        match.setEvents(events);
        match.setShots(shots);
        match.setHighlights(highlights);

        ProcessingSummary summary = new ProcessingSummary();
        summary.setPrimarySource(invokedAsFallback ? "HEURISTIC_FALLBACK" : "HEURISTIC");
        summary.setHeuristicFallbackUsed(invokedAsFallback);
        summary.getSources().add("HEURISTIC");
        if (invokedAsFallback) {
            summary.getNotes().add("Heuristic pipeline executed because research model output was unavailable or empty.");
        }
        match.setProcessingSummary(summary);
    }

    private MatchStatistics buildStatisticsFromModel(ModelStatistics modelStatistics, List<Event> events, List<Shot> shots) {
        MatchStatistics statistics = new MatchStatistics();
        if (modelStatistics != null) {
            statistics.setPlayer1Score(defaultInteger(modelStatistics.getPlayer1Score(), 0));
            statistics.setPlayer2Score(defaultInteger(modelStatistics.getPlayer2Score(), 0));
            statistics.setTotalRallies(defaultInteger(modelStatistics.getTotalRallies(), events.size()));
            statistics.setAvgRallyLength(defaultDouble(modelStatistics.getAvgRallyLength(), 6.0));
            statistics.setAvgBallSpeed(defaultDouble(modelStatistics.getAvgBallSpeed(), averageSpeed(shots)));
            statistics.setMaxBallSpeed(defaultDouble(modelStatistics.getMaxBallSpeed(), maxSpeed(shots)));
        } else {
            statistics = buildStatistics(events, shots, totalSpeed(shots), maxSpeed(shots));
        }
        return statistics;
    }

    private MatchStatistics buildStatistics(List<Event> events, List<Shot> shots, double cumulativeSpeed, double maxSpeed) {
        MatchStatistics statistics = new MatchStatistics();
        int totalEvents = events.size();
        statistics.setPlayer1Score(totalEvents / 2 + totalEvents % 2);
        statistics.setPlayer2Score(totalEvents / 2);
        statistics.setTotalRallies(Math.max(totalEvents, 6));
        statistics.setAvgRallyLength(Math.round((shots.size() / (double) Math.max(totalEvents, 1)) * 10.0) / 10.0 + 4.0);
        statistics.setAvgBallSpeed(shots.isEmpty() ? 0.0 : Math.round((cumulativeSpeed / shots.size()) * 10.0) / 10.0);
        statistics.setMaxBallSpeed(Math.round(maxSpeed * 10.0) / 10.0);
        return statistics;
    }

    private List<Shot> synthesizeShotsFromEvents(List<Event> events, double fps) {
        List<Shot> shots = new ArrayList<>();
        int index = 0;
        for (Event event : events) {
            Shot shot = new Shot();
            shot.setTimestampMs(event.getTimestampMs());
            shot.setTimestampSeries(new ArrayList<>(event.getTimestampSeries()));
            shot.setFrameSeries(new ArrayList<>(event.getFrameSeries()));
            shot.setPlayer(event.getPlayer() == null ? (index % 2) + 1 : event.getPlayer());
            shot.setShotType(resolveShotType(event.getMetadata() != null ? event.getMetadata().getShotType() : null));
            shot.setSpeed(event.getMetadata() != null && event.getMetadata().getShotSpeed() != null
                ? event.getMetadata().getShotSpeed()
                : 30.0);
            shot.setAccuracy(85.0);
            shot.setResult(ShotResult.IN);
            if (event.getMetadata() != null) {
                shot.setDetections(cloneDetections(event.getMetadata().getDetections()));
            } else {
                shot.setDetections(List.of());
            }
            shots.add(shot);
            index++;
        }
        if (shots.isEmpty()) {
            shots.add(buildFallbackShot());
        }
        return shots;
    }

    private Shot buildHeuristicShot(long timestampMs, double estimatedSpeed, int index, double fps) {
        Shot shot = new Shot();
        shot.setTimestampMs(timestampMs);
        shot.setTimestampSeries(deriveTimestampSeries(null, timestampMs, 2, 4, fps));
        Integer primaryFrame = (int) Math.round((timestampMs / 1000.0) * fps);
        shot.setFrameSeries(deriveFrameSeries(null, primaryFrame));
        shot.setPlayer(index % 2 == 0 ? 1 : 2);
        shot.setShotType(resolveShotType(null));
        shot.setSpeed(Math.round(estimatedSpeed * 10.0) / 10.0);
        shot.setAccuracy(Math.max(0.0, Math.min(100.0, 75.0 + (estimatedSpeed - 25.0))));
        shot.setResult(index % 6 == 0 ? ShotResult.OUT : ShotResult.IN);
        shot.setDetections(List.of());
        return shot;
    }

    private Event buildHeuristicEvent(long timestampMs, double motionScore, Shot shot, ScoreState runningScore, int index, double fps) {
        Event event = new Event();
        event.setId(UUID.randomUUID().toString());
        event.setTimestampMs(timestampMs);
        event.setTimestampSeries(deriveTimestampSeries(null, timestampMs, modelProperties.getPreEventFrames(), modelProperties.getPostEventFrames(), fps));
        EventType eventType = pickEventType(index);
        event.setType(eventType);
        event.setTitle(resolveEventTitle(eventType));
        event.setDescription(resolveEventDescription(eventType, motionScore));
        event.setPlayer(shot.getPlayer());
        event.setImportance(Math.min(10, (int) Math.round(motionScore / 8.0) + 4));

        EventMetadata metadata = new EventMetadata();
        metadata.setShotSpeed(shot.getSpeed());
        metadata.setRallyLength(Math.max(4, (int) Math.round(motionScore / 5.0)));
        metadata.setShotType(shot.getShotType().name());
        int frameNumber = (int) Math.round((timestampMs / 1000.0) * fps);
        metadata.setFrameNumber(frameNumber);
        event.setFrameSeries(deriveFrameSeries(null, frameNumber));
        metadata.setFrameSeries(new ArrayList<>(event.getFrameSeries()));
        metadata.setBallTrajectory(List.of(List.of(0.0, 0.0), List.of(1.0, 1.0)));
        metadata.setEventWindow(buildEventWindow(modelProperties.getPreEventFrames(), modelProperties.getPostEventFrames(), fps));
        metadata.setConfidence(Math.min(1.0, motionScore / (processingProperties.getMotionThreshold() * 2)));
        metadata.setSource("HEURISTIC");
        metadata.setDetections(List.of());
        if (eventType == EventType.SCORE) {
            ScoreState projected = incrementScore(new ScoreState(runningScore.getPlayer1(), runningScore.getPlayer2()), shot.getPlayer());
            metadata.setScoreAfter(projected);
        }
        event.setMetadata(metadata);
        return event;
    }

    private Event buildFallbackEvent() {
        Event fallback = new Event();
        fallback.setId(UUID.randomUUID().toString());
        fallback.setTimestampMs(0L);
        fallback.setTimestampSeries(deriveTimestampSeries(null, 0L, modelProperties.getPreEventFrames(), modelProperties.getPostEventFrames(), modelProperties.getFallbackFps()));
        fallback.setFrameSeries(deriveFrameSeries(null, 0));
        fallback.setType(EventType.PLAY_OF_THE_GAME);
        fallback.setTitle("Kickoff Rally");
        fallback.setDescription("Auto-generated rally highlight");
        fallback.setPlayer(null);
        fallback.setImportance(7);

        EventMetadata metadata = new EventMetadata();
        metadata.setRallyLength(6);
        metadata.setShotSpeed(32.0);
        metadata.setShotType(ShotType.SERVE.name());
        metadata.setFrameNumber(0);
        metadata.setFrameSeries(new ArrayList<>(fallback.getFrameSeries()));
        metadata.setBallTrajectory(List.of(List.of(0.0, 0.0), List.of(0.5, 0.8)));
        metadata.setEventWindow(new EventWindow(
            toMillis(modelProperties.getPreEventFrames(), modelProperties.getFallbackFps()),
            toMillis(modelProperties.getPostEventFrames(), modelProperties.getFallbackFps())));
        metadata.setConfidence(0.5);
        metadata.setSource("HEURISTIC");
        metadata.setDetections(List.of());
        fallback.setMetadata(metadata);
        return fallback;
    }

    private Shot buildFallbackShot() {
        Shot shot = new Shot();
        shot.setTimestampMs(0L);
        shot.setTimestampSeries(deriveTimestampSeries(null, 0L, 2, 4, modelProperties.getFallbackFps()));
        shot.setFrameSeries(deriveFrameSeries(null, 0));
        shot.setPlayer(1);
        shot.setShotType(ShotType.SERVE);
        shot.setSpeed(28.0);
        shot.setAccuracy(80.0);
        shot.setResult(ShotResult.IN);
        shot.setDetections(List.of());
        return shot;
    }

    private double totalSpeed(List<Shot> shots) {
        return shots.stream().mapToDouble(Shot::getSpeed).sum();
    }

    private double maxSpeed(List<Shot> shots) {
        return shots.stream().mapToDouble(Shot::getSpeed).max().orElse(0.0);
    }

    private double averageSpeed(List<Shot> shots) {
        return shots.isEmpty() ? 0.0 : Math.round((totalSpeed(shots) / shots.size()) * 10.0) / 10.0;
    }

    private Highlights buildHighlights(List<Event> events) {
        Highlights highlights = new Highlights();
        if (events.isEmpty()) {
            return highlights;
        }
        Event playOfGame = events.stream()
            .max(Comparator.comparingInt(Event::getImportance))
            .orElse(events.get(0));
        highlights.setPlayOfTheGame(createHighlightRef(playOfGame));
        events.stream()
            .filter(event -> event.getType() == EventType.RALLY_HIGHLIGHT)
            .sorted(Comparator.comparingInt(Event::getImportance).reversed())
            .limit(3)
            .map(this::createHighlightRef)
            .filter(Objects::nonNull)
            .forEach(highlights.getTopRallies()::add);
        events.stream()
            .filter(event -> event.getType() == EventType.FASTEST_SHOT)
            .sorted(Comparator.comparingInt(Event::getImportance).reversed())
            .limit(3)
            .map(this::createHighlightRef)
            .filter(Objects::nonNull)
            .forEach(highlights.getFastestShots()::add);
        events.stream()
            .filter(event -> event.getType() == EventType.SERVE_ACE)
            .limit(3)
            .map(this::createHighlightRef)
            .filter(Objects::nonNull)
            .forEach(highlights.getBestServes()::add);
        return highlights;
    }
    
    private List<Long> deriveTimestampSeries(List<Double> rawSeries, long primary, Integer preFrames, Integer postFrames, double fps) {
        List<Long> timeline = new ArrayList<>();
        if (rawSeries != null) {
            for (Double value : rawSeries) {
                if (value != null) {
                    timeline.add(Math.round(value));
                }
            }
        }
        if (timeline.isEmpty()) {
            int pre = preFrames == null ? modelProperties.getPreEventFrames() : preFrames;
            int post = postFrames == null ? modelProperties.getPostEventFrames() : postFrames;
            long preOffset = toMillis(pre, fps);
            long postOffset = toMillis(post, fps);
            long start = Math.max(0L, primary - preOffset);
            long end = primary + postOffset;
            timeline.add(start);
            timeline.add(primary);
            timeline.add(end);
        } else if (!timeline.contains(primary)) {
            timeline.add(primary);
        }
        timeline.sort(Long::compareTo);
        List<Long> deduped = new ArrayList<>();
        for (Long value : timeline) {
            if (deduped.isEmpty() || !deduped.get(deduped.size() - 1).equals(value)) {
                deduped.add(value);
            }
        }
        return List.copyOf(deduped);
    }

    private List<Integer> deriveFrameSeries(List<Integer> rawSeries, Number fallbackFrame) {
        List<Integer> frames = new ArrayList<>();
        if (rawSeries != null) {
            for (Integer frame : rawSeries) {
                if (frame != null) {
                    frames.add(frame);
                }
            }
        }
        if (fallbackFrame != null) {
            int fallback = fallbackFrame.intValue();
            if (!frames.contains(fallback)) {
                frames.add(fallback);
            }
        }
        frames.removeIf(Objects::isNull);
        frames.sort(Integer::compareTo);
        List<Integer> deduped = new ArrayList<>();
        for (Integer frame : frames) {
            if (deduped.isEmpty() || !deduped.get(deduped.size() - 1).equals(frame)) {
                deduped.add(frame);
            }
        }
        return List.copyOf(deduped);
    }

    private List<Detection> convertDetections(List<ModelDetection> detections, Number fallbackFrame, Double fallbackConfidence) {
        List<Detection> converted = new ArrayList<>();
        if (detections != null) {
            for (ModelDetection modelDetection : detections) {
                if (modelDetection == null) {
                    continue;
                }
                Detection detection = new Detection();
                detection.setFrameNumber(modelDetection.getFrameNumber());
                detection.setX(modelDetection.getX());
                detection.setY(modelDetection.getY());
                detection.setWidth(modelDetection.getWidth());
                detection.setHeight(modelDetection.getHeight());
                detection.setConfidence(modelDetection.getConfidence());
                converted.add(detection);
            }
        }
        if (converted.isEmpty() && fallbackFrame != null) {
            Detection fallbackDetection = new Detection();
            fallbackDetection.setFrameNumber(fallbackFrame.intValue());
            fallbackDetection.setConfidence(fallbackConfidence);
            converted.add(fallbackDetection);
        }
        return converted.isEmpty() ? List.of() : List.copyOf(converted);
    }

    private List<Detection> cloneDetections(List<Detection> detections) {
        if (detections == null || detections.isEmpty()) {
            return List.of();
        }
        List<Detection> clones = new ArrayList<>();
        for (Detection detection : detections) {
            if (detection == null) {
                continue;
            }
            Detection clone = new Detection();
            clone.setFrameNumber(detection.getFrameNumber());
            clone.setX(detection.getX());
            clone.setY(detection.getY());
            clone.setWidth(detection.getWidth());
            clone.setHeight(detection.getHeight());
            clone.setConfidence(detection.getConfidence());
            clones.add(clone);
        }
        return clones.isEmpty() ? List.of() : List.copyOf(clones);
    }

    private HighlightRef createHighlightRef(Event event) {
        if (event == null) {
            return null;
        }
        HighlightRef reference = new HighlightRef();
        reference.setEventId(event.getId());
        reference.setTimestampMs(event.getTimestampMs());
        reference.setTimestampSeries(new ArrayList<>(event.getTimestampSeries()));
        return reference;
    }

    private EventType pickEventType(int index) {
        return switch (index % 6) {
            case 0 -> EventType.SCORE;
            case 1 -> EventType.RALLY_HIGHLIGHT;
            case 2 -> EventType.FASTEST_SHOT;
            case 3 -> EventType.SERVE_ACE;
            case 4 -> EventType.MISS;
            default -> EventType.PLAY_OF_THE_GAME;
        };
    }

    private EventType resolveEventType(ModelEvent modelEvent) {
        if (modelEvent.getType() != null) {
            try {
                return EventType.valueOf(modelEvent.getType().toUpperCase(Locale.US));
            } catch (IllegalArgumentException ignored) {
                // fall through
            }
        }
        if (modelEvent.getLabel() != null) {
            String label = modelEvent.getLabel().toLowerCase(Locale.US);
            if (label.contains("score")) {
                return EventType.SCORE;
            }
            if (label.contains("ace")) {
                return EventType.SERVE_ACE;
            }
            if (label.contains("fast")) {
                return EventType.FASTEST_SHOT;
            }
            if (label.contains("miss") || label.contains("error")) {
                return EventType.MISS;
            }
            if (label.contains("rally") || label.contains("highlight")) {
                return EventType.RALLY_HIGHLIGHT;
            }
            if (label.contains("bounce") || label.contains("point")) {
                return EventType.SCORE;
            }
        }
        return EventType.PLAY_OF_THE_GAME;
    }

    private String resolveEventTitle(EventType type) {
        return switch (type) {
            case SCORE -> "Point Scored";
            case RALLY_HIGHLIGHT -> "Rally Highlight";
            case FASTEST_SHOT -> "Fastest Shot";
            case SERVE_ACE -> "Serve Ace";
            case MISS -> "Missed Return";
            case PLAY_OF_THE_GAME -> "Play of the Game";
        };
    }

    private String resolveEventDescription(EventType type, Object context) {
        if (context instanceof Double motionScore) {
            return switch (type) {
                case SCORE -> "Point concluded after intense exchange";
                case RALLY_HIGHLIGHT -> "Extended rally with high tempo";
                case FASTEST_SHOT -> "High-speed shot registered at motion score " + Math.round(motionScore);
                case SERVE_ACE -> "Serve led directly to a point";
                case MISS -> "Return attempt was unsuccessful";
                case PLAY_OF_THE_GAME -> "Most impactful rally detected";
            };
        }
        if (context instanceof ModelEvent modelEvent) {
            if (modelEvent.getLabel() != null && !modelEvent.getLabel().isBlank()) {
                return capitalize(modelEvent.getLabel());
            }
        }
        return resolveEventDescription(type, 25.0);
    }

    private String resolveEventDescription(EventType type, ModelEvent modelEvent) {
        return resolveEventDescription(type, (Object) modelEvent);
    }

    private ShotType resolveShotType(String type) {
        if (type == null) {
            return ShotType.FOREHAND;
        }
        try {
            return ShotType.valueOf(type.toUpperCase(Locale.US));
        } catch (IllegalArgumentException ex) {
            return switch (type.toLowerCase(Locale.US)) {
                case "serve" -> ShotType.SERVE;
                case "backhand" -> ShotType.BACKHAND;
                case "smash" -> ShotType.SMASH;
                case "defensive" -> ShotType.DEFENSIVE;
                default -> ShotType.FOREHAND;
            };
        }
    }

    private ShotResult resolveShotResult(String result) {
        if (result == null) {
            return ShotResult.IN;
        }
        try {
            return ShotResult.valueOf(result.toUpperCase(Locale.US));
        } catch (IllegalArgumentException ex) {
            if (result.toLowerCase(Locale.US).contains("out")) {
                return ShotResult.OUT;
            }
            if (result.toLowerCase(Locale.US).contains("net")) {
                return ShotResult.NET;
            }
            return ShotResult.IN;
        }
    }

    private long resolveTimestamp(Long frame, Double timestampMs, double fps) {
        if (timestampMs != null) {
            return Math.round(timestampMs);
        }
        if (frame != null && frame >= 0) {
            return Math.round((frame / fps) * 1000.0);
        }
        return 0L;
    }

    private int resolveFrameNumber(ModelEvent modelEvent, double fps) {
        if (modelEvent.getFrameNumber() != null) {
            return modelEvent.getFrameNumber();
        }
        if (modelEvent.getFrame() != null) {
            return modelEvent.getFrame().intValue();
        }
        return (int) Math.round((resolveTimestamp(modelEvent.getFrame(), modelEvent.getTimestampMs(), fps) / 1000.0) * fps);
    }

    private EventWindow buildEventWindow(ModelEvent modelEvent, double fps) {
        int preFrames = modelEvent.getPreEventFrames() == null ? modelProperties.getPreEventFrames() : modelEvent.getPreEventFrames();
        int postFrames = modelEvent.getPostEventFrames() == null ? modelProperties.getPostEventFrames() : modelEvent.getPostEventFrames();
        return buildEventWindow(preFrames, postFrames, fps);
    }

    private EventWindow buildEventWindow(int preFrames, int postFrames, double fps) {
        return new EventWindow(toMillis(preFrames, fps), toMillis(postFrames, fps));
    }

    private int toMillis(int frames, double fps) {
        if (fps <= 0) {
            fps = modelProperties.getFallbackFps();
        }
        return (int) Math.round((frames / fps) * 1000.0);
    }

    private double safeDouble(Double value, double fallback) {
        return value == null ? fallback : value;
    }

    private int defaultInteger(Integer value, int fallback) {
        return value == null ? fallback : value;
    }

    private double defaultDouble(Double value, double fallback) {
        return value == null ? fallback : value;
    }

    private void updateScore(ScoreState runningScore, Event event) {
        if (event.getType() == EventType.SCORE) {
            ScoreState updated = incrementScore(runningScore, event.getPlayer());
            runningScore.setPlayer1(updated.getPlayer1());
            runningScore.setPlayer2(updated.getPlayer2());
            if (event.getMetadata() != null) {
                event.getMetadata().setScoreAfter(new ScoreState(updated.getPlayer1(), updated.getPlayer2()));
            }
        }
    }

    private ScoreState incrementScore(ScoreState score, Integer player) {
        if (player == null || player == 0) {
            return new ScoreState(score.getPlayer1(), score.getPlayer2());
        }
        if (player == 1) {
            return new ScoreState(score.getPlayer1() + 1, score.getPlayer2());
        }
        return new ScoreState(score.getPlayer1(), score.getPlayer2() + 1);
    }

    private VideoMetadata readVideoMetadata(Path videoPath) {
        VideoCapture capture = new VideoCapture(videoPath.toString());
        if (!capture.isOpened()) {
            throw new IllegalStateException("Unable to open video: " + videoPath);
        }
        try {
            double fps = capture.get(Videoio.CAP_PROP_FPS);
            if (fps <= 0) {
                fps = modelProperties.getFallbackFps();
            }
            double frameCount = capture.get(Videoio.CAP_PROP_FRAME_COUNT);
            if (frameCount <= 0) {
                frameCount = processingProperties.getMaxFrameSamples();
            }
            int durationSeconds = (int) Math.round(frameCount / fps);
            return new VideoMetadata(fps, frameCount, durationSeconds);
        } finally {
            capture.release();
        }
    }

    private String capitalize(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        String trimmed = value.trim();
        return trimmed.substring(0, 1).toUpperCase(Locale.US) + trimmed.substring(1);
    }

    private record VideoMetadata(double fps, double frameCount, int durationSeconds) {
    }
}
