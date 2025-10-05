package com.backend.backend.service.model;

import com.backend.backend.config.ModelProperties;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class ModelInferenceResult {

    private Double fps;
    private List<ModelEvent> events;
    private List<ModelShot> shots;
    private ModelStatistics statistics;

    public Double getFps() {
        return fps;
    }

    public void setFps(Double fps) {
        this.fps = fps;
    }

    public List<ModelEvent> getEvents() {
        return events;
    }

    public void setEvents(List<ModelEvent> events) {
        this.events = events;
    }

    public List<ModelShot> getShots() {
        return shots;
    }

    public void setShots(List<ModelShot> shots) {
        this.shots = shots;
    }

    public ModelStatistics getStatistics() {
        return statistics;
    }

    public void setStatistics(ModelStatistics statistics) {
        this.statistics = statistics;
    }

    public ModelInferenceResult normalize(ModelProperties properties) {
        ModelInferenceResult normalized = new ModelInferenceResult();
        normalized.fps = fps == null ? properties.getFallbackFps() : fps;
        normalized.events = events == null ? new ArrayList<>() : new ArrayList<>(events);
        normalized.shots = shots == null ? new ArrayList<>() : new ArrayList<>(shots);
        normalized.statistics = statistics == null ? ModelStatistics.empty() : statistics.withDefaults();

        for (ModelEvent event : normalized.events) {
            event.applyDefaults(properties);
        }
        for (ModelShot shot : normalized.shots) {
            shot.applyDefaults();
        }
        return normalized;
    }

    public static class ModelEvent {
        private Long frame;
        private Double timestampMs;
        private List<Double> timestampSeries;
        private List<Integer> frameSeries;
        private String label;
        private String type;
        private Double confidence;
        private Integer player;
        private Integer importance;
        private Integer rallyLength;
        private Double shotSpeed;
        private String shotType;
        private String result;
        private Integer preEventFrames;
        private Integer postEventFrames;
        private Integer frameNumber;
        private List<List<Double>> ballTrajectory;
    private List<ModelDetection> detections;

        public Long getFrame() {
            return frame;
        }

        public void setFrame(Long frame) {
            this.frame = frame;
        }

        public Double getTimestampMs() {
            return timestampMs;
        }

        public void setTimestampMs(Double timestampMs) {
            this.timestampMs = timestampMs;
        }

        public List<Double> getTimestampSeries() {
            return timestampSeries;
        }

        public void setTimestampSeries(List<Double> timestampSeries) {
            this.timestampSeries = timestampSeries;
        }

        public List<Integer> getFrameSeries() {
            return frameSeries;
        }

        public void setFrameSeries(List<Integer> frameSeries) {
            this.frameSeries = frameSeries;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Double getConfidence() {
            return confidence;
        }

        public void setConfidence(Double confidence) {
            this.confidence = confidence;
        }

        public Integer getPlayer() {
            return player;
        }

        public void setPlayer(Integer player) {
            this.player = player;
        }

        public Integer getImportance() {
            return importance;
        }

        public void setImportance(Integer importance) {
            this.importance = importance;
        }

        public Integer getRallyLength() {
            return rallyLength;
        }

        public void setRallyLength(Integer rallyLength) {
            this.rallyLength = rallyLength;
        }

        public Double getShotSpeed() {
            return shotSpeed;
        }

        public void setShotSpeed(Double shotSpeed) {
            this.shotSpeed = shotSpeed;
        }

        public String getShotType() {
            return shotType;
        }

        public void setShotType(String shotType) {
            this.shotType = shotType;
        }

        public String getResult() {
            return result;
        }

        public void setResult(String result) {
            this.result = result;
        }

        public Integer getPreEventFrames() {
            return preEventFrames;
        }

        public void setPreEventFrames(Integer preEventFrames) {
            this.preEventFrames = preEventFrames;
        }

        public Integer getPostEventFrames() {
            return postEventFrames;
        }

        public void setPostEventFrames(Integer postEventFrames) {
            this.postEventFrames = postEventFrames;
        }

        public Integer getFrameNumber() {
            return frameNumber;
        }

        public void setFrameNumber(Integer frameNumber) {
            this.frameNumber = frameNumber;
        }

        public List<List<Double>> getBallTrajectory() {
            return ballTrajectory;
        }

        public void setBallTrajectory(List<List<Double>> ballTrajectory) {
            this.ballTrajectory = ballTrajectory;
        }

        public List<ModelDetection> getDetections() {
            return detections;
        }

        public void setDetections(List<ModelDetection> detections) {
            this.detections = detections;
        }

        public void applyDefaults(ModelProperties properties) {
            if (preEventFrames == null) {
                preEventFrames = properties.getPreEventFrames();
            }
            if (postEventFrames == null) {
                postEventFrames = properties.getPostEventFrames();
            }
            if (importance == null) {
                importance = 5;
            }
            if (confidence == null) {
                confidence = properties.getConfidenceThreshold();
            }
            if (ballTrajectory == null) {
                ballTrajectory = Collections.emptyList();
            }
            if (timestampSeries == null) {
                timestampSeries = Collections.emptyList();
            }
            if (frameSeries == null) {
                frameSeries = Collections.emptyList();
            }
            if (detections == null) {
                detections = Collections.emptyList();
            }
        }
    }

    public static class ModelShot {
        private Long frame;
        private List<Double> timestampSeries;
        private List<Integer> frameSeries;
        private Integer player;
        private Double speed;
        private Double accuracy;
        private String shotType;
        private String result;
        private Double confidence;
        private List<ModelDetection> detections;

        public Long getFrame() {
            return frame;
        }

        public void setFrame(Long frame) {
            this.frame = frame;
        }

        public List<Double> getTimestampSeries() {
            return timestampSeries;
        }

        public void setTimestampSeries(List<Double> timestampSeries) {
            this.timestampSeries = timestampSeries;
        }

        public List<Integer> getFrameSeries() {
            return frameSeries;
        }

        public void setFrameSeries(List<Integer> frameSeries) {
            this.frameSeries = frameSeries;
        }

        public Integer getPlayer() {
            return player;
        }

        public void setPlayer(Integer player) {
            this.player = player;
        }

        public Double getSpeed() {
            return speed;
        }

        public void setSpeed(Double speed) {
            this.speed = speed;
        }

        public Double getAccuracy() {
            return accuracy;
        }

        public void setAccuracy(Double accuracy) {
            this.accuracy = accuracy;
        }

        public String getShotType() {
            return shotType;
        }

        public void setShotType(String shotType) {
            this.shotType = shotType;
        }

        public String getResult() {
            return result;
        }

        public void setResult(String result) {
            this.result = result;
        }

        public Double getConfidence() {
            return confidence;
        }

        public void setConfidence(Double confidence) {
            this.confidence = confidence;
        }

        public List<ModelDetection> getDetections() {
            return detections;
        }

        public void setDetections(List<ModelDetection> detections) {
            this.detections = detections;
        }

        public void applyDefaults() {
            if (shotType == null) {
                shotType = "UNKNOWN";
            }
            if (result == null) {
                result = "IN";
            }
            if (confidence == null) {
                confidence = 0.5;
            }
            if (timestampSeries == null) {
                timestampSeries = Collections.emptyList();
            }
            if (frameSeries == null) {
                frameSeries = Collections.emptyList();
            }
            if (detections == null) {
                detections = Collections.emptyList();
            }
        }
    }

    public static class ModelDetection {
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

    public static class ModelStatistics {
        private Integer player1Score;
        private Integer player2Score;
        private Integer totalRallies;
        private Double avgRallyLength;
        private Double avgBallSpeed;
        private Double maxBallSpeed;

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

        public Double getAvgBallSpeed() {
            return avgBallSpeed;
        }

        public void setAvgBallSpeed(Double avgBallSpeed) {
            this.avgBallSpeed = avgBallSpeed;
        }

        public Double getMaxBallSpeed() {
            return maxBallSpeed;
        }

        public void setMaxBallSpeed(Double maxBallSpeed) {
            this.maxBallSpeed = maxBallSpeed;
        }

        private ModelStatistics withDefaults() {
            ModelStatistics normalized = new ModelStatistics();
            normalized.player1Score = Objects.requireNonNullElse(player1Score, 0);
            normalized.player2Score = Objects.requireNonNullElse(player2Score, 0);
            normalized.totalRallies = Objects.requireNonNullElse(totalRallies, 0);
            normalized.avgRallyLength = Objects.requireNonNullElse(avgRallyLength, 6.0);
            normalized.avgBallSpeed = Objects.requireNonNullElse(avgBallSpeed, 0.0);
            normalized.maxBallSpeed = Objects.requireNonNullElse(maxBallSpeed, 0.0);
            return normalized;
        }

        public static ModelStatistics empty() {
            ModelStatistics stats = new ModelStatistics();
            stats.player1Score = 0;
            stats.player2Score = 0;
            stats.totalRallies = 0;
            stats.avgRallyLength = 6.0;
            stats.avgBallSpeed = 0.0;
            stats.maxBallSpeed = 0.0;
            return stats;
        }
    }
}
