package com.backend.backend.dto;

public record ReturnMetricsResponse(Integer totalReturns,
                                    Integer successfulReturns,
                                    Double successRate,
                                    Double averageReturnSpeed) {
}
