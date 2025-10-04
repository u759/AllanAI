package com.backend.backend.dto;

import java.time.Instant;

import com.backend.backend.model.MatchStatus;

public record MatchSummaryResponse(String id,
                                   Instant createdAt,
                                   Instant processedAt,
                                   MatchStatus status,
                                   Integer durationSeconds,
                                   String originalFilename) {
}
