package com.backend.backend.dto;

import com.backend.backend.model.ShotType;

public record ShotTypeBreakdownItemResponse(ShotType shotType,
                                            Integer count,
                                            Double averageSpeed,
                                            Double averageAccuracy) {
}
