package com.backend.backend.dto;

public record MatchStatisticsResponse(Integer player1Score,
                                       Integer player2Score,
                                       Integer totalRallies,
                                       Double avgRallyLength,
                                       Double maxBallSpeed,
                                       Double avgBallSpeed) {
}
