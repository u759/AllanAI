package com.backend.backend.dto;

public record RallyMetricsResponse(Integer totalRallies,
                                   Double averageRallyLength,
                                   Integer longestRallyLength,
                                   Double averageRallyDurationSeconds,
                                   Double longestRallyDurationSeconds,
                                   Double averageRallyShotSpeed) {
}
