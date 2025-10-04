package com.backend.backend.mapper;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.backend.backend.dto.EventMetadataResponse;
import com.backend.backend.dto.EventResponse;
import com.backend.backend.dto.HighlightsResponse;
import com.backend.backend.dto.MatchDetailsResponse;
import com.backend.backend.dto.MatchStatisticsResponse;
import com.backend.backend.dto.MatchSummaryResponse;
import com.backend.backend.dto.ScoreStateResponse;
import com.backend.backend.dto.ShotResponse;
import com.backend.backend.model.MatchDocument;
import com.backend.backend.model.MatchDocument.Event;
import com.backend.backend.model.MatchDocument.EventMetadata;
import com.backend.backend.model.MatchDocument.Highlights;
import com.backend.backend.model.MatchDocument.MatchStatistics;
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
            highlights);
    }

    public static MatchStatisticsResponse toStatistics(MatchStatistics statistics) {
        if (statistics == null) {
            return new MatchStatisticsResponse(0, 0, 0, 0.0, 0.0, 0.0);
        }
        return new MatchStatisticsResponse(
            statistics.getPlayer1Score(),
            statistics.getPlayer2Score(),
            statistics.getTotalRallies(),
            statistics.getAvgRallyLength(),
            statistics.getMaxBallSpeed(),
            statistics.getAvgBallSpeed());
    }

    public static ShotResponse toShot(Shot shot) {
        return new ShotResponse(
            shot.getTimestampMs(),
            shot.getPlayer(),
            shot.getShotType(),
            shot.getSpeed(),
            shot.getAccuracy(),
            shot.getResult());
    }

    public static EventResponse toEvent(Event event) {
        return new EventResponse(
            event.getId(),
            event.getTimestampMs(),
            event.getType(),
            event.getTitle(),
            event.getDescription(),
            event.getPlayer(),
            event.getImportance(),
            toEventMetadata(event.getMetadata()));
    }

    public static EventMetadataResponse toEventMetadata(EventMetadata metadata) {
        if (metadata == null) {
            return new EventMetadataResponse(null, null, null, Collections.emptyList(), null, null);
        }
        ScoreStateResponse scoreState = metadata.getScoreAfter() == null ? null : toScoreState(metadata.getScoreAfter());
        List<List<Double>> ballTrajectory = metadata.getBallTrajectory();
        if (ballTrajectory == null) {
            ballTrajectory = Collections.emptyList();
        }
        return new EventMetadataResponse(
            metadata.getShotSpeed(),
            metadata.getRallyLength(),
            metadata.getShotType(),
            ballTrajectory,
            metadata.getFrameNumber(),
            scoreState);
    }

    public static ScoreStateResponse toScoreState(ScoreState scoreState) {
        return new ScoreStateResponse(scoreState.getPlayer1(), scoreState.getPlayer2());
    }

    public static HighlightsResponse toHighlights(Highlights highlights) {
        if (Objects.isNull(highlights)) {
            return new HighlightsResponse(null, Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
        }
        return new HighlightsResponse(
            highlights.getPlayOfTheGame(),
            List.copyOf(highlights.getTopRallies()),
            List.copyOf(highlights.getFastestShots()),
            List.copyOf(highlights.getBestServes()));
    }
}
