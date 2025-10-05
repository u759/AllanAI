package com.backend.backend.security;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.backend.backend.config.JwtProperties;
import com.backend.backend.model.UserDocument;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;

@Service
public class JwtService {

    private final JwtProperties properties;
    private final SecretKey signingKey;

    public JwtService(JwtProperties properties) {
        if (properties == null || !StringUtils.hasText(properties.getSecret())) {
            throw new IllegalStateException("JWT secret must be configured");
        }
        this.properties = properties;
        byte[] keyBytes = properties.getSecret().getBytes(StandardCharsets.UTF_8);
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public JwtToken generateToken(UserDocument user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(Duration.ofMinutes(properties.getExpirationMinutes()));

        String token = Jwts.builder()
            .subject(user.getId())
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiresAt))
            .claim("username", user.getUsername())
            .signWith(signingKey, Jwts.SIG.HS256)
            .compact();

        return new JwtToken(token, expiresAt);
    }

    public JwtPayload parseToken(String token) {
        try {
            Jws<Claims> jws = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token);

            Claims claims = jws.getPayload();
            Instant issuedAt = claims.getIssuedAt() != null ? claims.getIssuedAt().toInstant() : null;
            Instant expiresAt = claims.getExpiration() != null ? claims.getExpiration().toInstant() : null;

            return new JwtPayload(claims.getSubject(),
                claims.get("username", String.class),
                issuedAt,
                expiresAt);
        } catch (JwtException | IllegalArgumentException exception) {
            throw new InvalidJwtException("Failed to parse JWT", exception);
        }
    }
}
