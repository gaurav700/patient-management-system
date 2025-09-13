package com.pm.apigateway.service;


import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.WeakKeyException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;

@Service
@Slf4j
public class JwtService {

    private final SecretKey secretKey;
    private final Clock clock = Clock.systemUTC();
    private final io.jsonwebtoken.JwtParser jwtParser;

    /**
     * If your secret is base64-encoded, set jwt.secretKeyBase64=true
     * and provide the base64 string. Otherwise, a long random string works too.
     */
    public JwtService(
            @Value("${jwt.secret}") String jwtSecretKey,
            @Value("${jwt.secretKeyBase64:false}") boolean base64Encoded,
            @Value("${jwt.clockSkewSeconds:60}") long clockSkewSeconds
    ) {
        try {
            this.secretKey = base64Encoded
                    ? Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecretKey))
                    : Keys.hmacShaKeyFor(jwtSecretKey.getBytes(StandardCharsets.UTF_8));

            this.jwtParser = Jwts.parser()
                    .verifyWith(secretKey)
                    .clock(() -> java.util.Date.from(clock.instant()))
                    .clockSkewSeconds(clockSkewSeconds)
                    .build();

            log.info("JWT parser initialized (skew={}s, base64Key={})",
                    clockSkewSeconds, base64Encoded);

        } catch (WeakKeyException e) {
            // Happens if key < 256 bits for HS256, etc.
            log.error("JWT secret key is too weak. Provide at least 256-bit key for HS256.", e);
            throw e;
        } catch (Exception e) {
            log.error("Failed to initialize JwtService", e);
            throw e;
        }
    }

    /** Returns the subject (userId) from token or throws JwtException. */
    public String getUserIdFromToken(String token) {
        try {
            Claims claims = jwtParser.parseSignedClaims(token).getPayload();

            // Prefer "sub" as user id; adjust if you store it under a custom claim.
            String userId = claims.getSubject();
            if (userId == null || userId.isBlank()) {
                // If you store userId differently, uncomment:
                // userId = claims.get("userId", String.class);
                // if (userId == null || userId.isBlank()) ...
                throw new JwtException("JWT 'sub' (subject) claim is missing");
            }
            return userId;
        } catch (JwtException | IllegalArgumentException ex) {
            // Keep logs clean—don’t log the token
            log.warn("JWT parsing/verification failed: {}", ex.getMessage());
            throw ex;
        }
    }

    /** Optional: expose all claims if you need roles/permissions downstream. */
    public Claims getAllClaims(String token) {
        try {
            return jwtParser.parseSignedClaims(token).getPayload();
        } catch (JwtException | IllegalArgumentException ex) {
            log.warn("JWT claim extraction failed: {}", ex.getMessage());
            throw ex;
        }
    }

    /** Optional: quick validity check without returning claims. */
    public boolean isValid(String token) {
        try {
            jwtParser.parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }
}
