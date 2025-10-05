package com.backend.backend.dto;

import java.util.List;

public record MatchStatisticsResponse(Integer player1Score,
                                      Integer player2Score,
                                      Integer totalRallies,
                                      Double avgRallyLength,
                                      Double maxBallSpeed,
                                      Double avgBallSpeed,
                                      RallyMetricsResponse rallyMetrics,
                                      ShotSpeedMetricsResponse shotSpeedMetrics,
                                      ServeMetricsResponse serveMetrics,
                                      ReturnMetricsResponse returnMetrics,
                                      List<ShotTypeBreakdownItemResponse> shotTypeBreakdown,
                                      List<PlayerBreakdownResponse> playerBreakdown,
                                      List<MomentumSampleResponse> momentumTimeline) {
}
