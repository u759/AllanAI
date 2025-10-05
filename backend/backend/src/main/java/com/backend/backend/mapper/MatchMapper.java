package com.backend.backend.mapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.backend.backend.dto.DetectionResponse;
import com.backend.backend.dto.EventMetadataResponse;
import com.backend.backend.dto.EventResponse;
import com.backend.backend.dto.EventWindowResponse;
import com.backend.backend.dto.HighlightReferenceResponse;
import com.backend.backend.dto.HighlightsResponse;
import com.backend.backend.dto.MatchDetailsResponse;
import com.backend.backend.dto.MatchStatisticsResponse;
import com.backend.backend.dto.MatchSummaryResponse;
import com.backend.backend.dto.MomentumSampleResponse;
import com.backend.backend.dto.PlayerBreakdownResponse;
import com.backend.backend.dto.ProcessingSummaryResponse;
import com.backend.backend.dto.RallyMetricsResponse;
import com.backend.backend.dto.ReturnMetricsResponse;
import com.backend.backend.dto.ScoreStateResponse;
import com.backend.backend.dto.ServeMetricsResponse;
import com.backend.backend.dto.ShotResponse;
import com.backend.backend.dto.ShotSpeedMetricsResponse;
import com.backend.backend.dto.ShotTypeBreakdownItemResponse;
import com.backend.backend.model.MatchDocument;
import com.backend.backend.model.MatchDocument.Event;
import com.backend.backend.model.MatchDocument.EventMetadata;
import com.backend.backend.model.MatchDocument.EventWindow;
import com.backend.backend.model.MatchDocument.Detection;
import com.backend.backend.model.MatchDocument.Highlights;
import com.backend.backend.model.MatchDocument.HighlightRef;
import com.backend.backend.model.MatchDocument.MatchStatistics;
import com.backend.backend.model.MatchDocument.ProcessingSummary;
import com.backend.backend.model.MatchDocument.ScoreState;
import com.backend.backend.model.MatchDocument.Shot;

public final class MatchMapper {

    private MatchMapper() {
    }

    public static MatchSummaryResponse toSummary(MatchDocument match) {
        return new MatchSummaryResponse(
            match.getId(),
            match.getCreatedAt(),
            match.getProcessedAt(),
            match.getStatus(),
            match.getDurationSeconds(),
            match.getOriginalFilename());
    }

    public static MatchDetailsResponse toDetails(MatchDocument match) {
        MatchStatisticsResponse statisticsResponse = toStatistics(match.getStatistics());
        List<ShotResponse> shots = match.getShots() == null ? Collections.emptyList()
            : match.getShots().stream().map(MatchMapper::toShot).toList();
        List<EventResponse> events = match.getEvents() == null ? Collections.emptyList()
            : match.getEvents().stream().map(MatchMapper::toEvent).toList();
        HighlightsResponse highlights = toHighlights(match.getHighlights());
        ProcessingSummaryResponse processingSummary = toProcessingSummary(match.getProcessingSummary());
        return new MatchDetailsResponse(
            match.getId(),
            match.getCreatedAt(),
            match.getProcessedAt(),
            match.getStatus(),
            match.getDurationSeconds(),
            match.getOriginalFilename(),
            statisticsResponse,
            shots,
            events,
            highlights,
            processingSummary);
    }

    public static MatchStatisticsResponse toStatistics(MatchStatistics statistics) {
        if (statistics == null) {
            return new MatchStatisticsResponse(
                0,
                0,
                0,
                0.0,
                0.0,
                0.0,
                toRallyMetrics(null),
                toShotSpeedMetrics(null),
                toServeMetrics(null),
                toReturnMetrics(null),
                List.of(),
                List.of(),
                List.of());
        }
        return new MatchStatisticsResponse(
            statistics.getPlayer1Score(),
            statistics.getPlayer2Score(),
            statistics.getTotalRallies(),
            statistics.getAvgRallyLength(),
            statistics.getMaxBallSpeed(),
            statistics.getAvgBallSpeed(),
            toRallyMetrics(statistics.getRallyMetrics()),
            toShotSpeedMetrics(statistics.getShotSpeedMetrics()),
            toServeMetrics(statistics.getServeMetrics()),
            toReturnMetrics(statistics.getReturnMetrics()),
            toShotTypeBreakdown(statistics.getShotTypeBreakdown()),
            toPlayerBreakdown(statistics.getPlayerBreakdown()),
            toMomentumTimeline(statistics.getMomentumTimeline()));
    }

    public static ShotResponse toShot(Shot shot) {
        List<Long> timestampSeries = normalizeTimeline(shot.getTimestampSeries(), shot.getTimestampMs());
        List<Integer> frameSeries = normalizeFrameSeries(shot.getFrameSeries());
        return new ShotResponse(
            shot.getTimestampMs(),
            timestampSeries,
            frameSeries,
            shot.getPlayer(),
            shot.getShotType(),
            shot.getSpeed(),
            shot.getAccuracy(),
            shot.getResult(),
            toDetectionResponses(shot.getDetections()));
    }

    public static EventResponse toEvent(Event event) {
        List<Long> timestampSeries = normalizeTimeline(event.getTimestampSeries(), event.getTimestampMs());
        List<Integer> frameSeries = normalizeFrameSeries(event.getFrameSeries());
        return new EventResponse(
            event.getId(),
            event.getTimestampMs(),
            timestampSeries,
            frameSeries,
            event.getType(),
            event.getTitle(),
            event.getDescription(),
            event.getPlayer(),
            event.getImportance(),
            toEventMetadata(event.getMetadata()));
    }

    public static EventMetadataResponse toEventMetadata(EventMetadata metadata) {
        if (metadata == null) {
            return new EventMetadataResponse(
                null,
                null,
                null,
                null,
                null,
                Collections.emptyList(),
                null,
                Collections.emptyList(),
                null,
                null,
                null,
                null,
                Collections.emptyList());
        }
        ScoreStateResponse scoreState = metadata.getScoreAfter() == null ? null : toScoreState(metadata.getScoreAfter());
        List<List<Double>> ballTrajectory = metadata.getBallTrajectory();
        if (ballTrajectory == null) {
            ballTrajectory = Collections.emptyList();
        }
        EventWindowResponse eventWindow = toEventWindow(metadata.getEventWindow());
        return new EventMetadataResponse(
            metadata.getShotSpeed(),
            metadata.getIncomingShotSpeed(),
            metadata.getOutgoingShotSpeed(),
            metadata.getRallyLength(),
            metadata.getShotType(),
            ballTrajectory,
            metadata.getFrameNumber(),
            Collections.unmodifiableList(normalizeFrameSeries(metadata.getFrameSeries())),
            scoreState,
            eventWindow,
            metadata.getConfidence(),
            metadata.getSource(),
            toDetectionResponses(metadata.getDetections()));
    }

    public static ScoreStateResponse toScoreState(ScoreState scoreState) {
        return new ScoreStateResponse(scoreState.getPlayer1(), scoreState.getPlayer2());
    }

    private static EventWindowResponse toEventWindow(EventWindow eventWindow) {
        if (eventWindow == null) {
            return null;
        }
        return new EventWindowResponse(eventWindow.getPreMs(), eventWindow.getPostMs());
    }

    public static HighlightsResponse toHighlights(Highlights highlights) {
        if (Objects.isNull(highlights)) {
            return new HighlightsResponse(null, Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
        }
        return new HighlightsResponse(
            toHighlightReference(highlights.getPlayOfTheGame()),
            highlights.getTopRallies().stream().map(MatchMapper::toHighlightReference).toList(),
            highlights.getFastestShots().stream().map(MatchMapper::toHighlightReference).toList(),
            highlights.getBestServes().stream().map(MatchMapper::toHighlightReference).toList());
    }

    private static RallyMetricsResponse toRallyMetrics(MatchDocument.RallyMetrics metrics) {
        if (metrics == null) {
            return new RallyMetricsResponse(0, 0.0, 0, 0.0, 0.0, 0.0);
        }
        return new RallyMetricsResponse(
            metrics.getTotalRallies(),
            metrics.getAverageRallyLength(),
            metrics.getLongestRallyLength(),
            metrics.getAverageRallyDurationSeconds(),
            metrics.getLongestRallyDurationSeconds(),
            metrics.getAverageRallyShotSpeed());
    }

    private static ShotSpeedMetricsResponse toShotSpeedMetrics(MatchDocument.ShotSpeedMetrics metrics) {
        if (metrics == null) {
            return new ShotSpeedMetricsResponse(0.0, 0.0, 0.0, 0.0);
        }
        return new ShotSpeedMetricsResponse(
            metrics.getFastestShotMph(),
            metrics.getAverageShotMph(),
            metrics.getAverageIncomingShotMph(),
            metrics.getAverageOutgoingShotMph());
    }

    private static ServeMetricsResponse toServeMetrics(MatchDocument.ServeMetrics metrics) {
        if (metrics == null) {
            return new ServeMetricsResponse(0, 0, 0, 0.0, 0.0, 0.0);
        }
        return new ServeMetricsResponse(
            metrics.getTotalServes(),
            metrics.getSuccessfulServes(),
            metrics.getFaults(),
            metrics.getSuccessRate(),
            metrics.getAverageServeSpeed(),
            metrics.getFastestServeSpeed());
    }

    private static ReturnMetricsResponse toReturnMetrics(MatchDocument.ReturnMetrics metrics) {
        if (metrics == null) {
            return new ReturnMetricsResponse(0, 0, 0.0, 0.0);
        }
        return new ReturnMetricsResponse(
            metrics.getTotalReturns(),
            metrics.getSuccessfulReturns(),
            metrics.getSuccessRate(),
            metrics.getAverageReturnSpeed());
    }

    private static List<ShotTypeBreakdownItemResponse> toShotTypeBreakdown(List<MatchDocument.ShotTypeAggregate> aggregates) {
        if (aggregates == null || aggregates.isEmpty()) {
            return List.of();
        }
        return aggregates.stream()
            .map(aggregate -> new ShotTypeBreakdownItemResponse(
                aggregate.getShotType(),
                aggregate.getCount(),
                aggregate.getAverageSpeed(),
                aggregate.getAverageAccuracy()))
            .toList();
    }

    private static List<PlayerBreakdownResponse> toPlayerBreakdown(List<MatchDocument.PlayerBreakdown> breakdowns) {
        if (breakdowns == null || breakdowns.isEmpty()) {
            return List.of();
        }
        return breakdowns.stream()
            .map(breakdown -> new PlayerBreakdownResponse(
                breakdown.getPlayer(),
                breakdown.getTotalPointsWon(),
                breakdown.getTotalShots(),
                breakdown.getTotalServes(),
                breakdown.getSuccessfulServes(),
                breakdown.getTotalReturns(),
                breakdown.getSuccessfulReturns(),
                breakdown.getWinners(),
                breakdown.getErrors(),
                breakdown.getAverageShotSpeed(),
                breakdown.getAverageAccuracy(),
                breakdown.getPointWinRate(),
                breakdown.getServeSuccessRate(),
                breakdown.getReturnSuccessRate()))
            .toList();
    }

    private static List<MomentumSampleResponse> toMomentumTimeline(MatchDocument.MomentumTimeline timeline) {
        if (timeline == null || timeline.getSamples() == null || timeline.getSamples().isEmpty()) {
            return List.of();
        }
        return timeline.getSamples().stream()
            .map(sample -> new MomentumSampleResponse(
                sample.getTimestampMs(),
                sample.getScoringPlayer(),
                sample.getScoreAfter() == null ? null : toScoreState(sample.getScoreAfter()),
                sample.getLead()))
            .toList();
    }

    private static ProcessingSummaryResponse toProcessingSummary(ProcessingSummary summary) {
        if (summary == null) {
            return new ProcessingSummaryResponse("UNKNOWN", List.of(), List.of());
        }
        List<String> sources = summary.getSources() == null ? List.of() : List.copyOf(summary.getSources());
        List<String> notes = summary.getNotes() == null ? List.of() : List.copyOf(summary.getNotes());
        return new ProcessingSummaryResponse(summary.getPrimarySource(), sources, notes);
    }

    private static List<DetectionResponse> toDetectionResponses(List<Detection> detections) {
        if (detections == null || detections.isEmpty()) {
            return List.of();
        }
        return detections.stream()
            .map(detection -> new DetectionResponse(
                detection.getFrameNumber(),
                detection.getX(),
                detection.getY(),
                detection.getWidth(),
                detection.getHeight(),
                detection.getConfidence()))
            .toList();
    }

    private static HighlightReferenceResponse toHighlightReference(HighlightRef ref) {
        if (ref == null) {
            return null;
        }
        List<Long> timeline = normalizeTimeline(ref.getTimestampSeries(), ref.getTimestampMs());
        return new HighlightReferenceResponse(ref.getEventId(), ref.getTimestampMs(), timeline);
    }

    private static List<Long> normalizeTimeline(List<Long> timeline, long primary) {
        List<Long> safeTimeline = timeline == null ? new ArrayList<>() : new ArrayList<>(timeline);
        if (safeTimeline.isEmpty()) {
            safeTimeline.add(primary);
        } else if (!safeTimeline.contains(primary)) {
            safeTimeline.add(0, primary);
        }
        return List.copyOf(safeTimeline);
    }

    private static List<Integer> normalizeFrameSeries(List<Integer> frames) {
        if (frames == null || frames.isEmpty()) {
            return List.of();
        }
        return List.copyOf(frames);
    }
}
