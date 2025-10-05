package com.backend.backend.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "matches")
public class MatchDocument {

    @Id
    private String id;
    private Instant createdAt;
    private Instant processedAt;
    private MatchStatus status;
    private Integer durationSeconds;
    private String videoPath;
    private String originalFilename;
    private MatchStatistics statistics;
    private List<Shot> shots = new ArrayList<>();
    private List<Event> events = new ArrayList<>();
    private Highlights highlights;
    private ProcessingSummary processingSummary;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(Instant processedAt) {
        this.processedAt = processedAt;
    }

    public MatchStatus getStatus() {
        return status;
    }

    public void setStatus(MatchStatus status) {
        this.status = status;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Integer durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public String getVideoPath() {
        return videoPath;
    }

    public void setVideoPath(String videoPath) {
        this.videoPath = videoPath;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public MatchStatistics getStatistics() {
        return statistics;
    }

    public void setStatistics(MatchStatistics statistics) {
        this.statistics = statistics;
    }

    public List<Shot> getShots() {
        return shots;
    }

    public void setShots(List<Shot> shots) {
        this.shots = shots;
    }

    public List<Event> getEvents() {
        return events;
    }

    public void setEvents(List<Event> events) {
        this.events = events;
    }

    public Highlights getHighlights() {
        return highlights;
    }

    public void setHighlights(Highlights highlights) {
        this.highlights = highlights;
    }

    public ProcessingSummary getProcessingSummary() {
        return processingSummary;
    }

    public void setProcessingSummary(ProcessingSummary processingSummary) {
        this.processingSummary = processingSummary;
    }

    public static class MatchStatistics {
        private Integer player1Score;
        private Integer player2Score;
        private Integer totalRallies;
        private Double avgRallyLength;
        private Double maxBallSpeed;
        private Double avgBallSpeed;
        private RallyMetrics rallyMetrics = new RallyMetrics();
        private ShotSpeedMetrics shotSpeedMetrics = new ShotSpeedMetrics();
        private ServeMetrics serveMetrics = new ServeMetrics();
        private ReturnMetrics returnMetrics = new ReturnMetrics();
        private List<ShotTypeAggregate> shotTypeBreakdown = new ArrayList<>();
        private List<PlayerBreakdown> playerBreakdown = new ArrayList<>();
        private MomentumTimeline momentumTimeline = new MomentumTimeline();

        public Integer getPlayer1Score() {
            return player1Score;
        }

        public void setPlayer1Score(Integer player1Score) {
            this.player1Score = player1Score;
        }

        public Integer getPlayer2Score() {
            return player2Score;
        }

        public void setPlayer2Score(Integer player2Score) {
            this.player2Score = player2Score;
        }

        public Integer getTotalRallies() {
            return totalRallies;
        }

        public void setTotalRallies(Integer totalRallies) {
            this.totalRallies = totalRallies;
        }

        public Double getAvgRallyLength() {
            return avgRallyLength;
        }

        public void setAvgRallyLength(Double avgRallyLength) {
            this.avgRallyLength = avgRallyLength;
        }

        public Double getMaxBallSpeed() {
            return maxBallSpeed;
        }

        public void setMaxBallSpeed(Double maxBallSpeed) {
            this.maxBallSpeed = maxBallSpeed;
        }

        public Double getAvgBallSpeed() {
            return avgBallSpeed;
        }

        public void setAvgBallSpeed(Double avgBallSpeed) {
            this.avgBallSpeed = avgBallSpeed;
        }

        public RallyMetrics getRallyMetrics() {
            return rallyMetrics;
        }

        public void setRallyMetrics(RallyMetrics rallyMetrics) {
            this.rallyMetrics = rallyMetrics;
        }

        public ShotSpeedMetrics getShotSpeedMetrics() {
            return shotSpeedMetrics;
        }

        public void setShotSpeedMetrics(ShotSpeedMetrics shotSpeedMetrics) {
            this.shotSpeedMetrics = shotSpeedMetrics;
        }

        public ServeMetrics getServeMetrics() {
            return serveMetrics;
        }

        public void setServeMetrics(ServeMetrics serveMetrics) {
            this.serveMetrics = serveMetrics;
        }

        public ReturnMetrics getReturnMetrics() {
            return returnMetrics;
        }

        public void setReturnMetrics(ReturnMetrics returnMetrics) {
            this.returnMetrics = returnMetrics;
        }

        public List<ShotTypeAggregate> getShotTypeBreakdown() {
            return shotTypeBreakdown;
        }

        public void setShotTypeBreakdown(List<ShotTypeAggregate> shotTypeBreakdown) {
            this.shotTypeBreakdown = shotTypeBreakdown;
        }

        public List<PlayerBreakdown> getPlayerBreakdown() {
            return playerBreakdown;
        }

        public void setPlayerBreakdown(List<PlayerBreakdown> playerBreakdown) {
            this.playerBreakdown = playerBreakdown;
        }

        public MomentumTimeline getMomentumTimeline() {
            return momentumTimeline;
        }

        public void setMomentumTimeline(MomentumTimeline momentumTimeline) {
            this.momentumTimeline = momentumTimeline;
        }
    }

    public static class RallyMetrics {
        private Integer totalRallies;
        private Double averageRallyLength;
        private Integer longestRallyLength;
        private Double averageRallyDurationSeconds;
        private Double longestRallyDurationSeconds;
        private Double averageRallyShotSpeed;

        public Integer getTotalRallies() {
            return totalRallies;
        }

        public void setTotalRallies(Integer totalRallies) {
            this.totalRallies = totalRallies;
        }

        public Double getAverageRallyLength() {
            return averageRallyLength;
        }

        public void setAverageRallyLength(Double averageRallyLength) {
            this.averageRallyLength = averageRallyLength;
        }

        public Integer getLongestRallyLength() {
            return longestRallyLength;
        }

        public void setLongestRallyLength(Integer longestRallyLength) {
            this.longestRallyLength = longestRallyLength;
        }

        public Double getAverageRallyDurationSeconds() {
            return averageRallyDurationSeconds;
        }

        public void setAverageRallyDurationSeconds(Double averageRallyDurationSeconds) {
            this.averageRallyDurationSeconds = averageRallyDurationSeconds;
        }

        public Double getLongestRallyDurationSeconds() {
            return longestRallyDurationSeconds;
        }

        public void setLongestRallyDurationSeconds(Double longestRallyDurationSeconds) {
            this.longestRallyDurationSeconds = longestRallyDurationSeconds;
        }

        public Double getAverageRallyShotSpeed() {
            return averageRallyShotSpeed;
        }

        public void setAverageRallyShotSpeed(Double averageRallyShotSpeed) {
            this.averageRallyShotSpeed = averageRallyShotSpeed;
        }
    }

    public static class ShotSpeedMetrics {
        private Double fastestShotMph;
        private Double averageShotMph;
        private Double averageIncomingShotMph;
        private Double averageOutgoingShotMph;

        public Double getFastestShotMph() {
            return fastestShotMph;
        }

        public void setFastestShotMph(Double fastestShotMph) {
            this.fastestShotMph = fastestShotMph;
        }

        public Double getAverageShotMph() {
            return averageShotMph;
        }

        public void setAverageShotMph(Double averageShotMph) {
            this.averageShotMph = averageShotMph;
        }

        public Double getAverageIncomingShotMph() {
            return averageIncomingShotMph;
        }

        public void setAverageIncomingShotMph(Double averageIncomingShotMph) {
            this.averageIncomingShotMph = averageIncomingShotMph;
        }

        public Double getAverageOutgoingShotMph() {
            return averageOutgoingShotMph;
        }

        public void setAverageOutgoingShotMph(Double averageOutgoingShotMph) {
            this.averageOutgoingShotMph = averageOutgoingShotMph;
        }
    }

    public static class ServeMetrics {
        private Integer totalServes;
        private Integer successfulServes;
        private Integer faults;
        private Double successRate;
        private Double averageServeSpeed;
        private Double fastestServeSpeed;

        public Integer getTotalServes() {
            return totalServes;
        }

        public void setTotalServes(Integer totalServes) {
            this.totalServes = totalServes;
        }

        public Integer getSuccessfulServes() {
            return successfulServes;
        }

        public void setSuccessfulServes(Integer successfulServes) {
            this.successfulServes = successfulServes;
        }

        public Integer getFaults() {
            return faults;
        }

        public void setFaults(Integer faults) {
            this.faults = faults;
        }

        public Double getSuccessRate() {
            return successRate;
        }

        public void setSuccessRate(Double successRate) {
            this.successRate = successRate;
        }

        public Double getAverageServeSpeed() {
            return averageServeSpeed;
        }

        public void setAverageServeSpeed(Double averageServeSpeed) {
            this.averageServeSpeed = averageServeSpeed;
        }

        public Double getFastestServeSpeed() {
            return fastestServeSpeed;
        }

        public void setFastestServeSpeed(Double fastestServeSpeed) {
            this.fastestServeSpeed = fastestServeSpeed;
        }
    }

    public static class ReturnMetrics {
        private Integer totalReturns;
        private Integer successfulReturns;
        private Double successRate;
        private Double averageReturnSpeed;

        public Integer getTotalReturns() {
            return totalReturns;
        }

        public void setTotalReturns(Integer totalReturns) {
            this.totalReturns = totalReturns;
        }

        public Integer getSuccessfulReturns() {
            return successfulReturns;
        }

        public void setSuccessfulReturns(Integer successfulReturns) {
            this.successfulReturns = successfulReturns;
        }

        public Double getSuccessRate() {
            return successRate;
        }

        public void setSuccessRate(Double successRate) {
            this.successRate = successRate;
        }

        public Double getAverageReturnSpeed() {
            return averageReturnSpeed;
        }

        public void setAverageReturnSpeed(Double averageReturnSpeed) {
            this.averageReturnSpeed = averageReturnSpeed;
        }
    }

    public static class ShotTypeAggregate {
        private ShotType shotType;
        private Integer count;
        private Double averageSpeed;
        private Double averageAccuracy;

        public ShotType getShotType() {
            return shotType;
        }

        public void setShotType(ShotType shotType) {
            this.shotType = shotType;
        }

        public Integer getCount() {
            return count;
        }

        public void setCount(Integer count) {
            this.count = count;
        }

        public Double getAverageSpeed() {
            return averageSpeed;
        }

        public void setAverageSpeed(Double averageSpeed) {
            this.averageSpeed = averageSpeed;
        }

        public Double getAverageAccuracy() {
            return averageAccuracy;
        }

        public void setAverageAccuracy(Double averageAccuracy) {
            this.averageAccuracy = averageAccuracy;
        }
    }

    public static class PlayerBreakdown {
        private Integer player;
        private Integer totalPointsWon;
        private Integer totalShots;
        private Integer totalServes;
        private Integer successfulServes;
        private Integer totalReturns;
        private Integer successfulReturns;
        private Integer winners;
        private Integer errors;
        private Double averageShotSpeed;
        private Double averageAccuracy;
        private Double pointWinRate;
        private Double serveSuccessRate;
        private Double returnSuccessRate;

        public Integer getPlayer() {
            return player;
        }

        public void setPlayer(Integer player) {
            this.player = player;
        }

        public Integer getTotalPointsWon() {
            return totalPointsWon;
        }

        public void setTotalPointsWon(Integer totalPointsWon) {
            this.totalPointsWon = totalPointsWon;
        }

        public Integer getTotalShots() {
            return totalShots;
        }

        public void setTotalShots(Integer totalShots) {
            this.totalShots = totalShots;
        }

        public Integer getTotalServes() {
            return totalServes;
        }

        public void setTotalServes(Integer totalServes) {
            this.totalServes = totalServes;
        }

        public Integer getSuccessfulServes() {
            return successfulServes;
        }

        public void setSuccessfulServes(Integer successfulServes) {
            this.successfulServes = successfulServes;
        }

        public Integer getTotalReturns() {
            return totalReturns;
        }

        public void setTotalReturns(Integer totalReturns) {
            this.totalReturns = totalReturns;
        }

        public Integer getSuccessfulReturns() {
            return successfulReturns;
        }

        public void setSuccessfulReturns(Integer successfulReturns) {
            this.successfulReturns = successfulReturns;
        }

        public Integer getWinners() {
            return winners;
        }

        public void setWinners(Integer winners) {
            this.winners = winners;
        }

        public Integer getErrors() {
            return errors;
        }

        public void setErrors(Integer errors) {
            this.errors = errors;
        }

        public Double getAverageShotSpeed() {
            return averageShotSpeed;
        }

        public void setAverageShotSpeed(Double averageShotSpeed) {
            this.averageShotSpeed = averageShotSpeed;
        }

        public Double getAverageAccuracy() {
            return averageAccuracy;
        }

        public void setAverageAccuracy(Double averageAccuracy) {
            this.averageAccuracy = averageAccuracy;
        }

        public Double getPointWinRate() {
            return pointWinRate;
        }

        public void setPointWinRate(Double pointWinRate) {
            this.pointWinRate = pointWinRate;
        }

        public Double getServeSuccessRate() {
            return serveSuccessRate;
        }

        public void setServeSuccessRate(Double serveSuccessRate) {
            this.serveSuccessRate = serveSuccessRate;
        }

        public Double getReturnSuccessRate() {
            return returnSuccessRate;
        }

        public void setReturnSuccessRate(Double returnSuccessRate) {
            this.returnSuccessRate = returnSuccessRate;
        }
    }

    public static class MomentumTimeline {
        private List<MomentumSample> samples = new ArrayList<>();

        public List<MomentumSample> getSamples() {
            return samples;
        }

        public void setSamples(List<MomentumSample> samples) {
            this.samples = samples;
        }
    }

    public static class MomentumSample {
        private Long timestampMs;
        private Integer scoringPlayer;
        private ScoreState scoreAfter;
        private Integer lead;

        public Long getTimestampMs() {
            return timestampMs;
        }

        public void setTimestampMs(Long timestampMs) {
            this.timestampMs = timestampMs;
        }

        public Integer getScoringPlayer() {
            return scoringPlayer;
        }

        public void setScoringPlayer(Integer scoringPlayer) {
            this.scoringPlayer = scoringPlayer;
        }

        public ScoreState getScoreAfter() {
            return scoreAfter;
        }

        public void setScoreAfter(ScoreState scoreAfter) {
            this.scoreAfter = scoreAfter;
        }

        public Integer getLead() {
            return lead;
        }

        public void setLead(Integer lead) {
            this.lead = lead;
        }
    }

    public static class Shot {
        private long timestampMs;
        private List<Long> timestampSeries = new ArrayList<>();
        private List<Integer> frameSeries = new ArrayList<>();
        private int player;
        private ShotType shotType;
        private double speed;
        private double accuracy;
        private ShotResult result;
        private List<Detection> detections = new ArrayList<>();

        public long getTimestampMs() {
            return timestampMs;
        }

        public void setTimestampMs(long timestampMs) {
            this.timestampMs = timestampMs;
        }

        public List<Long> getTimestampSeries() {
            return timestampSeries;
        }

        public void setTimestampSeries(List<Long> timestampSeries) {
            this.timestampSeries = timestampSeries;
        }

        public List<Integer> getFrameSeries() {
            return frameSeries;
        }

        public void setFrameSeries(List<Integer> frameSeries) {
            this.frameSeries = frameSeries;
        }

        public int getPlayer() {
            return player;
        }

        public void setPlayer(int player) {
            this.player = player;
        }

        public ShotType getShotType() {
            return shotType;
        }

        public void setShotType(ShotType shotType) {
            this.shotType = shotType;
        }

        public double getSpeed() {
            return speed;
        }

        public void setSpeed(double speed) {
            this.speed = speed;
        }

        public double getAccuracy() {
            return accuracy;
        }

        public void setAccuracy(double accuracy) {
            this.accuracy = accuracy;
        }

        public ShotResult getResult() {
            return result;
        }

        public void setResult(ShotResult result) {
            this.result = result;
        }

        public List<Detection> getDetections() {
            return detections;
        }

        public void setDetections(List<Detection> detections) {
            this.detections = detections;
        }
    }

    public static class Event {
        private String id;
        private long timestampMs;
        private List<Long> timestampSeries = new ArrayList<>();
        private List<Integer> frameSeries = new ArrayList<>();
        private EventType type;
        private String title;
        private String description;
        private Integer player;
        private int importance;
        private EventMetadata metadata;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public long getTimestampMs() {
            return timestampMs;
        }

        public void setTimestampMs(long timestampMs) {
            this.timestampMs = timestampMs;
        }

        public List<Long> getTimestampSeries() {
            return timestampSeries;
        }

        public void setTimestampSeries(List<Long> timestampSeries) {
            this.timestampSeries = timestampSeries;
        }

        public List<Integer> getFrameSeries() {
            return frameSeries;
        }

        public void setFrameSeries(List<Integer> frameSeries) {
            this.frameSeries = frameSeries;
        }

        public EventType getType() {
            return type;
        }

        public void setType(EventType type) {
            this.type = type;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public Integer getPlayer() {
            return player;
        }

        public void setPlayer(Integer player) {
            this.player = player;
        }

        public int getImportance() {
            return importance;
        }

        public void setImportance(int importance) {
            this.importance = importance;
        }

        public EventMetadata getMetadata() {
            return metadata;
        }

        public void setMetadata(EventMetadata metadata) {
            this.metadata = metadata;
        }
    }

    public static class EventMetadata {
        private Double shotSpeed;
    private Double incomingShotSpeed;
    private Double outgoingShotSpeed;
        private Integer rallyLength;
        private String shotType;
        private List<List<Double>> ballTrajectory;
        private Integer frameNumber;
        private List<Integer> frameSeries = new ArrayList<>();
        private ScoreState scoreAfter;
        private EventWindow eventWindow;
        private Double confidence;
        private String source;
        private List<Detection> detections = new ArrayList<>();

        public Double getShotSpeed() {
            return shotSpeed;
        }

        public void setShotSpeed(Double shotSpeed) {
            this.shotSpeed = shotSpeed;
        }

        public Double getIncomingShotSpeed() {
            return incomingShotSpeed;
        }

        public void setIncomingShotSpeed(Double incomingShotSpeed) {
            this.incomingShotSpeed = incomingShotSpeed;
        }

        public Double getOutgoingShotSpeed() {
            return outgoingShotSpeed;
        }

        public void setOutgoingShotSpeed(Double outgoingShotSpeed) {
            this.outgoingShotSpeed = outgoingShotSpeed;
        }

        public Integer getRallyLength() {
            return rallyLength;
        }

        public void setRallyLength(Integer rallyLength) {
            this.rallyLength = rallyLength;
        }

        public String getShotType() {
            return shotType;
        }

        public void setShotType(String shotType) {
            this.shotType = shotType;
        }

        public List<List<Double>> getBallTrajectory() {
            return ballTrajectory;
        }

        public void setBallTrajectory(List<List<Double>> ballTrajectory) {
            this.ballTrajectory = ballTrajectory;
        }

        public Integer getFrameNumber() {
            return frameNumber;
        }

        public void setFrameNumber(Integer frameNumber) {
            this.frameNumber = frameNumber;
        }

        public List<Integer> getFrameSeries() {
            return frameSeries;
        }

        public void setFrameSeries(List<Integer> frameSeries) {
            this.frameSeries = frameSeries;
        }

        public ScoreState getScoreAfter() {
            return scoreAfter;
        }

        public void setScoreAfter(ScoreState scoreAfter) {
            this.scoreAfter = scoreAfter;
        }

        public EventWindow getEventWindow() {
            return eventWindow;
        }

        public void setEventWindow(EventWindow eventWindow) {
            this.eventWindow = eventWindow;
        }

        public Double getConfidence() {
            return confidence;
        }

        public void setConfidence(Double confidence) {
            this.confidence = confidence;
        }

        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source;
        }

        public List<Detection> getDetections() {
            return detections;
        }

        public void setDetections(List<Detection> detections) {
            this.detections = detections;
        }
    }

    public static class EventWindow {
        private Integer preMs;
        private Integer postMs;

        public EventWindow() {
        }

        public EventWindow(Integer preMs, Integer postMs) {
            this.preMs = preMs;
            this.postMs = postMs;
        }

        public Integer getPreMs() {
            return preMs;
        }

        public void setPreMs(Integer preMs) {
            this.preMs = preMs;
        }

        public Integer getPostMs() {
            return postMs;
        }

        public void setPostMs(Integer postMs) {
            this.postMs = postMs;
        }
    }

    public static class ScoreState {
        private int player1;
        private int player2;

        public ScoreState() {
        }

        public ScoreState(int player1, int player2) {
            this.player1 = player1;
            this.player2 = player2;
        }

        public int getPlayer1() {
            return player1;
        }

        public void setPlayer1(int player1) {
            this.player1 = player1;
        }

        public int getPlayer2() {
            return player2;
        }

        public void setPlayer2(int player2) {
            this.player2 = player2;
        }
    }

    public static class Highlights {
        private HighlightRef playOfTheGame;
        private List<HighlightRef> topRallies = new ArrayList<>();
        private List<HighlightRef> fastestShots = new ArrayList<>();
        private List<HighlightRef> bestServes = new ArrayList<>();

        public HighlightRef getPlayOfTheGame() {
            return playOfTheGame;
        }

        public void setPlayOfTheGame(HighlightRef playOfTheGame) {
            this.playOfTheGame = playOfTheGame;
        }

        public List<HighlightRef> getTopRallies() {
            return topRallies;
        }

        public void setTopRallies(List<HighlightRef> topRallies) {
            this.topRallies = topRallies;
        }

        public List<HighlightRef> getFastestShots() {
            return fastestShots;
        }

        public void setFastestShots(List<HighlightRef> fastestShots) {
            this.fastestShots = fastestShots;
        }

        public List<HighlightRef> getBestServes() {
            return bestServes;
        }

        public void setBestServes(List<HighlightRef> bestServes) {
            this.bestServes = bestServes;
        }
    }

    public static class HighlightRef {
        private String eventId;
        private Long timestampMs;
        private List<Long> timestampSeries = new ArrayList<>();

        public String getEventId() {
            return eventId;
        }

        public void setEventId(String eventId) {
            this.eventId = eventId;
        }

        public Long getTimestampMs() {
            return timestampMs;
        }

        public void setTimestampMs(Long timestampMs) {
            this.timestampMs = timestampMs;
        }

        public List<Long> getTimestampSeries() {
            return timestampSeries;
        }

        public void setTimestampSeries(List<Long> timestampSeries) {
            this.timestampSeries = timestampSeries;
        }
    }

    public static class Detection {
        private Integer frameNumber;
        private Double x;
        private Double y;
        private Double width;
        private Double height;
        private Double confidence;

        public Integer getFrameNumber() {
            return frameNumber;
        }

        public void setFrameNumber(Integer frameNumber) {
            this.frameNumber = frameNumber;
        }

        public Double getX() {
            return x;
        }

        public void setX(Double x) {
            this.x = x;
        }

        public Double getY() {
            return y;
        }

        public void setY(Double y) {
            this.y = y;
        }

        public Double getWidth() {
            return width;
        }

        public void setWidth(Double width) {
            this.width = width;
        }

        public Double getHeight() {
            return height;
        }

        public void setHeight(Double height) {
            this.height = height;
        }

        public Double getConfidence() {
            return confidence;
        }

        public void setConfidence(Double confidence) {
            this.confidence = confidence;
        }
    }

    public static class ProcessingSummary {
        private String primarySource;
        private List<String> sources = new ArrayList<>();
        private List<String> notes = new ArrayList<>();

        public String getPrimarySource() {
            return primarySource;
        }

        public void setPrimarySource(String primarySource) {
            this.primarySource = primarySource;
        }

        public List<String> getSources() {
            return sources;
        }

        public void setSources(List<String> sources) {
            this.sources = sources;
        }

        public List<String> getNotes() {
            return notes;
        }

        public void setNotes(List<String> notes) {
            this.notes = notes;
        }
    }
}
