package com.backend.backend.dto;

import java.util.List;

public record ProcessingSummaryResponse(String primarySource,
                                        boolean heuristicFallbackUsed,
                                        List<String> sources,
                                        List<String> notes) {
}
