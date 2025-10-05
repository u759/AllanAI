package com.backend.backend.dto;

public record ShotSpeedMetricsResponse(Double fastestShotMph,
                                       Double averageShotMph,
                                       Double averageIncomingShotMph,
                                       Double averageOutgoingShotMph) {
}
