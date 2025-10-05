package com.backend.backend.security;

import java.time.Instant;

public record JwtPayload(String userId,
                         String username,
                         Instant issuedAt,
                         Instant expiresAt) {
}
