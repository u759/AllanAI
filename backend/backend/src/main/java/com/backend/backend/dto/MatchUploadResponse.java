package com.backend.backend.dto;

import com.backend.backend.model.MatchStatus;

public record MatchUploadResponse(String matchId, MatchStatus status) {
}
