package com.backend.backend.dto;

import java.util.List;

public record EventMetadataResponse(Double shotSpeed,
                                    Integer rallyLength,
                                    String shotType,
                                    List<List<Double>> ballTrajectory,
                                    Integer frameNumber,
                                    ScoreStateResponse scoreAfter,
                                    EventWindowResponse eventWindow,
                                    Double confidence,
                                    String source) {
}
