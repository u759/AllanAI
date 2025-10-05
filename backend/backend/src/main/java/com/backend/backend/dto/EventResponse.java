package com.backend.backend.dto;

import java.util.List;

import com.backend.backend.model.EventType;

public record EventResponse(String id,
                            long timestampMs,
                            List<Long> timestampSeries,
                            List<Integer> frameSeries,
                            EventType type,
                            String title,
                            String description,
                            Integer player,
                            int importance,
                            EventMetadataResponse metadata) {
}
