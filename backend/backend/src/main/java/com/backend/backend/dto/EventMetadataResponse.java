package com.backend.backend.dto;

import java.util.List;

public record EventMetadataResponse(Double shotSpeed,
                                    Double incomingShotSpeed,
                                    Double outgoingShotSpeed,
                                    Integer rallyLength,
                                    String shotType,
                                    List<List<Double>> ballTrajectory,
                                    Integer frameNumber,
                                    List<Integer> frameSeries,
                                    ScoreStateResponse scoreAfter,
                                    EventWindowResponse eventWindow,
                                    Double confidence,
                                    String source,
                                    List<DetectionResponse> detections) {
}
