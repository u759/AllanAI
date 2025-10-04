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

    public static class MatchStatistics {
        private Integer player1Score;
        private Integer player2Score;
        private Integer totalRallies;
        private Double avgRallyLength;
        private Double maxBallSpeed;
        private Double avgBallSpeed;

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
    }

    public static class Shot {
        private long timestampMs;
        private int player;
        private ShotType shotType;
        private double speed;
        private double accuracy;
        private ShotResult result;

        public long getTimestampMs() {
            return timestampMs;
        }

        public void setTimestampMs(long timestampMs) {
            this.timestampMs = timestampMs;
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
    }

    public static class Event {
        private String id;
        private long timestampMs;
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
        private Integer rallyLength;
        private String shotType;
        private List<List<Double>> ballTrajectory;
        private Integer frameNumber;
        private ScoreState scoreAfter;

        public Double getShotSpeed() {
            return shotSpeed;
        }

        public void setShotSpeed(Double shotSpeed) {
            this.shotSpeed = shotSpeed;
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

        public ScoreState getScoreAfter() {
            return scoreAfter;
        }

        public void setScoreAfter(ScoreState scoreAfter) {
            this.scoreAfter = scoreAfter;
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
        private String playOfTheGame;
        private List<String> topRallies = new ArrayList<>();
        private List<String> fastestShots = new ArrayList<>();
        private List<String> bestServes = new ArrayList<>();

        public String getPlayOfTheGame() {
            return playOfTheGame;
        }

        public void setPlayOfTheGame(String playOfTheGame) {
            this.playOfTheGame = playOfTheGame;
        }

        public List<String> getTopRallies() {
            return topRallies;
        }

        public void setTopRallies(List<String> topRallies) {
            this.topRallies = topRallies;
        }

        public List<String> getFastestShots() {
            return fastestShots;
        }

        public void setFastestShots(List<String> fastestShots) {
            this.fastestShots = fastestShots;
        }

        public List<String> getBestServes() {
            return bestServes;
        }

        public void setBestServes(List<String> bestServes) {
                this.bestServes = bestServes;
        }
    }
}
