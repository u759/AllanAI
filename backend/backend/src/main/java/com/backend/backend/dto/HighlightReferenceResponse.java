package com.backend.backend.dto;

import java.util.List;

public record HighlightReferenceResponse(String eventId,
                                         Long timestampMs,
                                         List<Long> timestampSeries) {
}
