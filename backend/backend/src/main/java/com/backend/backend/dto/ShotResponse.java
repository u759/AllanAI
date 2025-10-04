package com.backend.backend.dto;

import com.backend.backend.model.ShotResult;
import com.backend.backend.model.ShotType;

public record ShotResponse(long timestampMs,
                           int player,
                           ShotType shotType,
                           double speed,
                           double accuracy,
                           ShotResult result) {
}
