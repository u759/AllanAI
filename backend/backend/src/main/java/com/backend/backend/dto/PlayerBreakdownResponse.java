package com.backend.backend.dto;

public record PlayerBreakdownResponse(Integer player,
                                      Integer totalPointsWon,
                                      Integer totalShots,
                                      Integer totalServes,
                                      Integer successfulServes,
                                      Integer totalReturns,
                                      Integer successfulReturns,
                                      Integer winners,
                                      Integer errors,
                                      Double averageShotSpeed,
                                      Double averageAccuracy,
                                      Double pointWinRate,
                                      Double serveSuccessRate,
                                      Double returnSuccessRate) {
}
