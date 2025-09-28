package com.beyond.MKX;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.List;

@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {

    private final SecretKey atKey;

    public JwtAuthFilter(@Value("${jwt.secretKeyAt}") String secretKeyAtBase64) {
        this.atKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(secretKeyAtBase64));
    }
        private static final List<String> ALLOWED_PREFIXES = List.of(
                "/auth/**"
        );

        private final AntPathMatcher pathMatcher = new AntPathMatcher();


        @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
            String path = exchange.getRequest().getURI().getRawPath();

            // 1) OPTIONS (CORS preflight)는 항상 통과
            if (HttpMethod.OPTIONS.equals(exchange.getRequest().getMethod())) {
                return chain.filter(exchange);
            }

            // 2) 화이트리스트 패스
            for (String allow : ALLOWED_PREFIXES) {
                if (pathMatcher.match(allow, path)) {
                    return chain.filter(exchange);
                }
            }

            // 3) 쿠키 "AT" 추출
        HttpCookie atCookie = exchange.getRequest().getCookies().getFirst("AT");

        // 비로그인 요청은 그냥 통과 (권한 필요한 엔드포인트에서만 401 처리)
            if (atCookie == null || atCookie.getValue() == null) {
                return chain.filter(exchange);
            }

        try {
            // 4) AT 검증(서명/만료) → 유효하면 클레임 추출
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(atKey)
                    .build()
                    .parseClaimsJws(atCookie.getValue())
                    .getBody();

            String userId = claims.getSubject();
            String roles  = claims.get("roles", String.class);

            // 5) 내부 서비스로 전달할 헤더 추가
            ServerHttpRequest mutated = exchange.getRequest().mutate()
                    .header("X-User-Id", userId)
                    .header("X-Roles", roles != null ? roles : "")
                    .build();

            return chain.filter(exchange.mutate().request(mutated).build());

        } catch (JwtException e) {
            // AT가 만료/위조/포맷오류 → 401
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }

    @Override
    public int getOrder() {
        // 인증은 최대한 먼저
        return Ordered.HIGHEST_PRECEDENCE;
    }
}