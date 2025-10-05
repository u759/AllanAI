package com.backend.backend.dto;

public record DetectionResponse(Integer frameNumber,
                                 Double x,
                                 Double y,
                                 Double width,
                                 Double height,
                                 Double confidence) {
}
