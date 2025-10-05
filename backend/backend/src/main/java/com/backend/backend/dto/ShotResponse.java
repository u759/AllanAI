package com.backend.backend.dto;

import java.util.List;

import com.backend.backend.model.ShotResult;
import com.backend.backend.model.ShotType;

public record ShotResponse(long timestampMs,
                           List<Long> timestampSeries,
                           List<Integer> frameSeries,
                           int player,
                           ShotType shotType,
                           double speed,
                           double accuracy,
                           ShotResult result,
                           List<DetectionResponse> detections) {
}
