package com.backend.backend.dto;

import java.time.Instant;
import java.util.List;

import com.backend.backend.model.MatchStatus;

public record MatchDetailsResponse(String id,
                                   Instant createdAt,
                                   Instant processedAt,
                                   MatchStatus status,
                                   Integer durationSeconds,
                                   String originalFilename,
                                   MatchStatisticsResponse statistics,
                                   List<ShotResponse> shots,
                                   List<EventResponse> events,
                                   HighlightsResponse highlights) {
}
