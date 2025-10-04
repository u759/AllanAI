package com.backend.backend.service;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

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

import com.backend.backend.config.ProcessingProperties;
import com.backend.backend.model.EventType;
import com.backend.backend.model.MatchDocument;
import com.backend.backend.model.MatchDocument.Event;
import com.backend.backend.model.MatchDocument.EventMetadata;
import com.backend.backend.model.MatchDocument.Highlights;
import com.backend.backend.model.MatchDocument.MatchStatistics;
import com.backend.backend.model.MatchDocument.ScoreState;
import com.backend.backend.model.MatchDocument.Shot;
import com.backend.backend.model.MatchStatus;
import com.backend.backend.model.ShotResult;
import com.backend.backend.model.ShotType;

@Service
public class MatchProcessingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MatchProcessingService.class);

    private final MatchService matchService;
    private final VideoStorageService videoStorageService;
    private final ProcessingProperties processingProperties;

    public MatchProcessingService(MatchService matchService,
                                  VideoStorageService videoStorageService,
                                  ProcessingProperties processingProperties) {
        this.matchService = matchService;
        this.videoStorageService = videoStorageService;
        this.processingProperties = processingProperties;
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
            match.setProcessedAt(Instant.now());
            matchService.save(match);
            LOGGER.info("Finished processing match {}", matchId);
        } catch (Exception ex) {
            LOGGER.error("Processing failed for match {}", matchId, ex);
            match.setStatus(MatchStatus.FAILED);
            match.setProcessedAt(Instant.now());
            matchService.save(match);
        }
    }

    private void processVideo(MatchDocument match) {
        Path videoPath = videoStorageService.resolvePath(match.getVideoPath());
        VideoCapture capture = new VideoCapture(videoPath.toString());
        if (!capture.isOpened()) {
            throw new IllegalStateException("Unable to open video: " + videoPath);
        }

        Mat prevGray = new Mat();
        Mat frame = new Mat();
        Mat gray = new Mat();
        Mat diff = new Mat();
        try {
            double fps = capture.get(Videoio.CAP_PROP_FPS);
            if (fps <= 0) {
                fps = 30.0;
            }
            double frameCount = capture.get(Videoio.CAP_PROP_FRAME_COUNT);
            if (frameCount <= 0) {
                frameCount = processingProperties.getMaxFrameSamples();
            }
            int durationSeconds = (int) Math.round(frameCount / fps);
            match.setDurationSeconds(durationSeconds);

            List<Event> events = new ArrayList<>();
            List<Shot> shots = new ArrayList<>();

            int frameIndex = 0;
            int processedFrames = 0;
            double cumulativeSpeed = 0.0;
            double maxSpeed = 0.0;
            ScoreState runningScore = new ScoreState(0, 0);

            int maxSamples = Math.max(300, processingProperties.getMaxFrameSamples());
            while (capture.read(frame)) {
                if (frame.empty()) {
                    break;
                }
                Imgproc.cvtColor(frame, gray, Imgproc.COLOR_BGR2GRAY);
                if (!prevGray.empty()) {
                    Core.absdiff(gray, prevGray, diff);
                    Scalar mean = Core.mean(diff);
                    double motionScore = mean.val[0];
                    if (motionScore > 18.0) {
                        long timestampMs = Math.round((frameIndex / fps) * 1000.0);
                        double estimatedSpeed = Math.min(60.0, 18.0 + motionScore);
                        cumulativeSpeed += estimatedSpeed;
                        maxSpeed = Math.max(maxSpeed, estimatedSpeed);
                        Shot shot = buildShot(timestampMs, estimatedSpeed, events.size());
                        shots.add(shot);
                        Event event = buildEvent(timestampMs, motionScore, shot, runningScore, events.size());
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
        } finally {
            prevGray.release();
            frame.release();
            gray.release();
            diff.release();
            capture.release();
        }
    }

    private Shot buildShot(long timestampMs, double estimatedSpeed, int index) {
        Shot shot = new Shot();
        shot.setTimestampMs(timestampMs);
        shot.setPlayer(index % 2 == 0 ? 1 : 2);
        ShotType type = switch (index % 5) {
            case 0 -> ShotType.SERVE;
            case 1 -> ShotType.FOREHAND;
            case 2 -> ShotType.BACKHAND;
            case 3 -> ShotType.SMASH;
            default -> ShotType.DEFENSIVE;
        };
        shot.setShotType(type);
        shot.setSpeed(Math.round(estimatedSpeed * 10.0) / 10.0);
        shot.setAccuracy(Math.max(0.0, Math.min(100.0, 75.0 + (estimatedSpeed - 25.0))));
        shot.setResult(index % 6 == 0 ? ShotResult.OUT : ShotResult.IN);
        return shot;
    }

    private Event buildEvent(long timestampMs, double motionScore, Shot shot, ScoreState runningScore, int index) {
        Event event = new Event();
        event.setId(UUID.randomUUID().toString());
        event.setTimestampMs(timestampMs);
        EventType eventType = pickEventType(index);
        event.setType(eventType);
        event.setTitle(buildTitle(eventType));
        event.setDescription(buildDescription(eventType, motionScore));
        event.setPlayer(shot.getPlayer());
        event.setImportance(Math.min(10, (int) Math.round(motionScore / 8.0) + 4));
        EventMetadata metadata = new EventMetadata();
        metadata.setShotSpeed(shot.getSpeed());
        metadata.setRallyLength(Math.max(4, (int) Math.round(motionScore / 5.0)));
        metadata.setShotType(shot.getShotType().name());
        metadata.setFrameNumber((int) (timestampMs / 33));
        metadata.setBallTrajectory(List.of(List.of(0.0, 0.0), List.of(1.0, 1.0)));
        if (eventType == EventType.SCORE) {
            ScoreState projected = new ScoreState(runningScore.getPlayer1(), runningScore.getPlayer2());
            projected = incrementScore(projected, shot.getPlayer());
            metadata.setScoreAfter(projected);
        }
        event.setMetadata(metadata);
        return event;
    }

    private Event buildFallbackEvent() {
        Event fallback = new Event();
        fallback.setId(UUID.randomUUID().toString());
        fallback.setTimestampMs(0L);
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
        metadata.setBallTrajectory(List.of(List.of(0.0, 0.0), List.of(0.5, 0.8)));
        fallback.setMetadata(metadata);
        return fallback;
    }

    private Shot buildFallbackShot() {
        Shot shot = new Shot();
        shot.setTimestampMs(0L);
        shot.setPlayer(1);
        shot.setShotType(ShotType.SERVE);
        shot.setSpeed(28.0);
        shot.setAccuracy(80.0);
        shot.setResult(ShotResult.IN);
        return shot;
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

    private Highlights buildHighlights(List<Event> events) {
        Highlights highlights = new Highlights();
        Event playOfGame = events.stream()
            .max(Comparator.comparingInt(Event::getImportance))
            .orElse(events.get(0));
        highlights.setPlayOfTheGame(playOfGame.getId());
        events.stream()
            .filter(event -> event.getType() == EventType.RALLY_HIGHLIGHT)
            .sorted(Comparator.comparingInt(Event::getImportance).reversed())
            .limit(3)
            .map(Event::getId)
            .forEach(highlights.getTopRallies()::add);
        events.stream()
            .filter(event -> event.getType() == EventType.FASTEST_SHOT)
            .sorted(Comparator.comparingInt(Event::getImportance).reversed())
            .limit(3)
            .map(Event::getId)
            .forEach(highlights.getFastestShots()::add);
        events.stream()
            .filter(event -> event.getType() == EventType.SERVE_ACE)
            .limit(3)
            .map(Event::getId)
            .forEach(highlights.getBestServes()::add);
        return highlights;
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

    private String buildTitle(EventType type) {
        return switch (type) {
            case SCORE -> "Point Scored";
            case RALLY_HIGHLIGHT -> "Rally Highlight";
            case FASTEST_SHOT -> "Fastest Shot";
            case SERVE_ACE -> "Serve Ace";
            case MISS -> "Missed Return";
            case PLAY_OF_THE_GAME -> "Play of the Game";
        };
    }

    private String buildDescription(EventType type, double motionScore) {
        return switch (type) {
            case SCORE -> "Point concluded after intense exchange";
            case RALLY_HIGHLIGHT -> "Extended rally with high tempo";
            case FASTEST_SHOT -> "High-speed shot registered at motion score " + Math.round(motionScore);
            case SERVE_ACE -> "Serve led directly to a point";
            case MISS -> "Return attempt was unsuccessful";
            case PLAY_OF_THE_GAME -> "Most impactful rally detected";
        };
    }

    private void updateScore(ScoreState runningScore, Event event) {
        if (event.getType() == EventType.SCORE) {
            ScoreState updated = incrementScore(runningScore, event.getPlayer());
            runningScore.setPlayer1(updated.getPlayer1());
            runningScore.setPlayer2(updated.getPlayer2());
            EventMetadata metadata = event.getMetadata();
            metadata.setScoreAfter(new ScoreState(updated.getPlayer1(), updated.getPlayer2()));
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
}
