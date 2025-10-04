package com.backend.backend.dto;

import com.backend.backend.model.EventType;

public record EventResponse(String id,
                            long timestampMs,
                            EventType type,
                            String title,
                            String description,
                            Integer player,
                            int importance,
                            EventMetadataResponse metadata) {
}
