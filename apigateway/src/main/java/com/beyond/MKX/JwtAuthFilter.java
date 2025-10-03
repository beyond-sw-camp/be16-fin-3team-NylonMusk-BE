package com.beyond.MKX;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {

    // 화이트리스트 url
    private static final List<String> AUTH_PATHS = List.of(
            "/auth/**"
    );

    private final SecretKey atKey;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public JwtAuthFilter(@Value("${jwt.secretKeyAt}") String secretKeyAtBase64) {
        this.atKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(secretKeyAtBase64));
    }

        @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
            String path = exchange.getRequest().getURI().getRawPath();

            // 1) OPTIONS (CORS preflight)는 항상 통과
            if (HttpMethod.OPTIONS.equals(exchange.getRequest().getMethod())) {
                return chain.filter(exchange);
            }

            // 2) 화이트리스트 패스
            if (isAuthPath(path)) {
                return chain.filter(exchange);
            }

            // 3) 쿠키 "AT" 추출
        HttpCookie atCookie = exchange.getRequest().getCookies().getFirst("AT");

            if (atCookie == null || atCookie.getValue() == null || atCookie.getValue().isBlank()) {
                return unauthorized(exchange, "로그인이 필요합니다.");
            }

        try {
            // 4) AT 검증(서명/만료) → 유효하면 클레임 추출
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(atKey)
                    .build()
                    .parseClaimsJws(atCookie.getValue())
                    .getBody();

            String userId = claims.getSubject();
            String role = claims.get("role", String.class);

            // 5) 내부 서비스로 전달할 헤더 추가
            ServerHttpRequest mutated = exchange.getRequest().mutate()
                    .header("X-User-Id", userId)
                    .header("X-User-Role", role != null ? role : "")
                    .build();

            return chain.filter(exchange.mutate().request(mutated).build());

        } catch (JwtException e) {
            // AT가 만료/위조/포맷오류 → 401
            return unauthorized(exchange, "유효하지 않은 액세스 토큰");
        }
    }

    private boolean isAuthPath(String rawPath) {
        String normalized = rawPath;
        if (normalized.startsWith("/mkx-platform-service")) {
            normalized = normalized.substring("/mkx-platform-service".length());
        }
        if (normalized.isEmpty()) {
            normalized = "/";
        }
        for (String pattern : AUTH_PATHS) {
            if (pathMatcher.match(pattern, normalized)) {
                return true;
            }
        }
        return false;
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] payload = String.format(
                "{\"status_code\":401,\"status_message\":\"%s\"}", message
        ).getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(payload);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        // 인증은 최대한 먼저
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
