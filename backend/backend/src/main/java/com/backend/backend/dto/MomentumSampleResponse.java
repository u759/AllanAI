package com.backend.backend.dto;

public record MomentumSampleResponse(Long timestampMs,
                                     Integer scoringPlayer,
                                     ScoreStateResponse scoreAfter,
                                     Integer lead) {
}
