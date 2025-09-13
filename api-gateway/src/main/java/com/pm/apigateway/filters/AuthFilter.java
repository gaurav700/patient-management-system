package com.pm.apigateway.filters;

import com.pm.apigateway.service.JwtService;
import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import java.util.Optional;
import java.util.UUID;

@Component
@Slf4j
public class AuthFilter extends AbstractGatewayFilterFactory<AuthFilter.Config> {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String HDR_USER_ID = "UserId";
    private static final String HDR_REQUEST_ID = "X-Request-Id";

    private final JwtService jwtService;

    public AuthFilter(JwtService jwtService) {
        super(Config.class);
        this.jwtService = jwtService;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {


            final String requestId = ensureRequestId(exchange);
            final String method = Optional.ofNullable(exchange.getRequest().getMethod())
                    .map(HttpMethod::name)
                    .orElse("UNKNOWN");
            final String path = exchange.getRequest().getURI().getPath();

            log.info("[{}] Incoming request {} {}", requestId, method, path);

            final String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

            if (!hasBearerToken(authHeader)) {
                log.warn("[{}] Missing/invalid Authorization header", requestId);
                setUnauthorized(exchange, "Bearer token required");
                return exchange.getResponse().setComplete();
            }

            final String token = extractToken(authHeader);
            if (token == null) {
                log.warn("[{}] Authorization header present but token extraction failed", requestId);
                setUnauthorized(exchange, "Invalid Authorization header");
                return exchange.getResponse().setComplete();
            }

            try {
                String userId = jwtService.getUserIdFromToken(token);
                if (userId == null || userId.isBlank()) {
                    log.warn("[{}] JWT parsed but userId missing", requestId);
                    setUnauthorized(exchange, "Invalid token");
                    return exchange.getResponse().setComplete();
                }

                log.info("[{}] Authenticated userId={} for {} {}", requestId, userId, method, path);

                ServerWebExchange modified = exchange.mutate()
                        .request(r -> r.headers(h -> h.set(HDR_USER_ID, userId)))
                        .build();

                return chain.filter(modified);
            } catch (JwtException ex) {
                // Don’t leak token or full details; message is fine for server logs
                log.error("[{}] JWT validation failed: {}", requestId, ex.getMessage());
                setUnauthorized(exchange, "Invalid or expired token");
                return exchange.getResponse().setComplete();
            } catch (Exception ex) {
                log.error("[{}] Unexpected error during auth", requestId, ex);
                exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
                return exchange.getResponse().setComplete();
            }
        };
    }

    private static boolean hasBearerToken(String header) {
        if (header == null) return false;
        // tolerate extra spaces and different casing of 'Bearer'
        String h = header.trim();
        return h.length() > BEARER_PREFIX.length()
                && h.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length());
    }

    private static String extractToken(String header) {
        String h = header.trim();
        // Normalize to the first space after "Bearer"
        int idx = h.indexOf(' ');
        if (idx < 0 || idx == h.length() - 1) return null;
        return h.substring(idx + 1).trim();
    }

    private static String ensureRequestId(ServerWebExchange exchange) {
        String existing = exchange.getRequest().getHeaders().getFirst(HDR_REQUEST_ID);
        String reqId = Optional.ofNullable(existing).orElse(UUID.randomUUID().toString());
        // propagate on response so downstream logs/tools can correlate
        exchange.getResponse().getHeaders().set(HDR_REQUEST_ID, reqId);
        return reqId;
    }

    private static void setUnauthorized(ServerWebExchange exchange, String reason) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        // Helps clients know why it failed; don’t include token details
        exchange.getResponse().getHeaders().set("WWW-Authenticate", "Bearer realm=\"api\", error=\"unauthorized\", error_description=\"" + reason + "\"");
    }

    public static class Config { }
}
