package com.beyond.MKX;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    private final String secretKeyValue;
    private SecretKey atKey;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    private static final List<String> AUTH_PATHS = List.of(
            "/auth/**",
            "/health"
    );

    private static final Set<String> SERVICE_PREFIXES = Set.of(
            "/mkx-platform-service",
            "/ordering-service",
            "/matching-engine-service",
            "/marketData-service"
    );

    public JwtAuthFilter(@Value("${jwt.secretKeyAt}") String secretKeyAtValue) {
        this.secretKeyValue = secretKeyAtValue;
    }

    @Override
    public int getOrder() {
        return -100;
    }

    @PostConstruct
    public void initKey() {
        this.atKey = decodeKey(secretKeyValue);
    }

    private SecretKey decodeKey(String value) {
        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(value);
        } catch (IllegalArgumentException ex) {
            keyBytes = value.getBytes(StandardCharsets.UTF_8);
        }
        return new SecretKeySpec(keyBytes, SignatureAlgorithm.HS512.getJcaName());
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String rawPath = request.getURI().getPath();
        String normalizedPath = normalizePath(rawPath);

        if (isAuthPath(normalizedPath)) {
            return chain.filter(exchange);
        }

        HttpCookie cookie = request.getCookies().getFirst("AT");
        if (cookie == null) {
            if (log.isWarnEnabled()) {
                log.warn("AT cookie missing for path {}", rawPath);
            }
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = cookie.getValue();
        if (log.isWarnEnabled()) {
            log.warn("AT token for {} -> {}", rawPath, token);
        }

        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(atKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            String userId = Optional.ofNullable(claims.getSubject()).orElse("");
            String role = Optional.ofNullable(claims.get("role", String.class)).orElse("");

            ServerHttpRequest mutated = request.mutate()
                    .header("X-User-Id", userId)
                    .header("X-User-Role", role)
                    .build();

            return chain.filter(exchange.mutate().request(mutated).build());
        } catch (JwtException e) {
            if (log.isWarnEnabled()) {
                log.warn("Invalid AT for path {}: {}", rawPath, e.getMessage());
            }
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }

    private boolean isAuthPath(String path) {
        String candidate = path.isEmpty() ? "/" : path;
        for (String pattern : AUTH_PATHS) {
            if (pathMatcher.match(pattern, candidate)) {
                return true;
            }
        }
        return false;
    }

    private String normalizePath(String rawPath) {
        if (rawPath == null || rawPath.isEmpty()) {
            return "/";
        }

        for (String prefix : SERVICE_PREFIXES) {
            if (rawPath.equals(prefix)) {
                return "/";
            }
            if (rawPath.startsWith(prefix + "/")) {
                String trimmed = rawPath.substring(prefix.length());
                return trimmed.isEmpty() ? "/" : trimmed;
            }
        }

        return rawPath;
    }
}
