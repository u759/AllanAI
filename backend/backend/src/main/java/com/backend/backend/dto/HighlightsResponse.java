package com.backend.backend.dto;

import java.util.List;

public record HighlightsResponse(String playOfTheGame,
                                 List<String> topRallies,
                                 List<String> fastestShots,
                                 List<String> bestServes) {
}
