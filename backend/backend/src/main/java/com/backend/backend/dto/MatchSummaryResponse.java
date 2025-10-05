package com.backend.backend.dto;

import java.time.Instant;
import java.util.List;

import com.backend.backend.model.MatchStatus;

public record MatchSummaryResponse(String id,
                                   String userId,
                                   Instant createdAt,
                                   Instant processedAt,
                                   MatchStatus status,
                                   Integer durationSeconds,
                                   String videoPath,
                                   String originalFilename,
                                   String player1Name,
                                   String player2Name,
                                   String matchTitle,
                                   String thumbnailPath,
                                   MatchStatisticsResponse statistics,
                                   List<ShotResponse> shots,
                                   List<EventResponse> events,
                                   HighlightsResponse highlights) {
}
