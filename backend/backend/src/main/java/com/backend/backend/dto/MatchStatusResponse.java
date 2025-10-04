package com.backend.backend.dto;

import java.time.Instant;

import com.backend.backend.model.MatchStatus;

public record MatchStatusResponse(String matchId,
                                  MatchStatus status,
                                  Instant processedAt) {
}
