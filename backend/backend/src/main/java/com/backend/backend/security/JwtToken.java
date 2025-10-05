package com.backend.backend.security;

import java.time.Instant;

public record JwtToken(String value, Instant expiresAt) {
}
