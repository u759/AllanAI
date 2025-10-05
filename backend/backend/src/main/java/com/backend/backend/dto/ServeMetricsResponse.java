package com.backend.backend.dto;

public record ServeMetricsResponse(Integer totalServes,
                                   Integer successfulServes,
                                   Integer faults,
                                   Double successRate,
                                   Double averageServeSpeed,
                                   Double fastestServeSpeed) {
}
