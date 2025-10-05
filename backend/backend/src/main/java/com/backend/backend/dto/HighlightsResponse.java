package com.backend.backend.dto;

import java.util.List;

public record HighlightsResponse(HighlightReferenceResponse playOfTheGame,
                                 List<HighlightReferenceResponse> topRallies,
                                 List<HighlightReferenceResponse> fastestShots,
                                 List<HighlightReferenceResponse> bestServes) {
}
