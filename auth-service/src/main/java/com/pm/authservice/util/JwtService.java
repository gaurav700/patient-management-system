package com.pm.authservice.util;
import com.pm.authservice.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
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
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Service
@Slf4j
public class JwtService {

    private final SecretKey secretKey;
    private final JwtParser jwtParser;
    private final Clock clock = Clock.systemUTC();

    // token lifetimes
    private static final Duration ACCESS_TTL  = Duration.ofMinutes(10);
    private static final Duration REFRESH_TTL = Duration.ofDays(180);
    private static final long CLOCK_SKEW_SECONDS = 60; // tolerate small drift

    public JwtService(
            @Value("${jwt.secretKey}") String secret,
            @Value("${jwt.secretKeyBase64:false}") boolean base64Encoded
    ) {
        try {
            this.secretKey = base64Encoded
                    ? Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret))
                    : Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

            this.jwtParser = Jwts.parser()
                    .verifyWith(secretKey)
                    .clock(() -> Date.from(clock.instant()))
                    .clockSkewSeconds(CLOCK_SKEW_SECONDS)
                    .build();

            log.info("JwtService initialized (base64Key={}).", base64Encoded);
        } catch (WeakKeyException e) {
            // ensure at least 256-bit secret for HS256
            log.error("JWT secret too weak. Use a >=32-byte random secret for HS256.", e);
            throw e;
        }
    }

    public String generateAccessToken(User user) {
        Instant now = clock.instant();
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ACCESS_TTL)))
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    public String generateRefreshToken(User user) {
        Instant now = clock.instant();
        return Jwts.builder()
                .subject(user.getId().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(REFRESH_TTL)))
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    public Long getUserIdFromToken(String token) {
        Claims claims = jwtParser.parseSignedClaims(token).getPayload();
        return Long.parseLong(claims.getSubject());
    }

    // optional helper if you need the email later
    public String getEmailFromToken(String token) {
        Claims claims = jwtParser.parseSignedClaims(token).getPayload();
        return claims.get("email", String.class);
    }
}