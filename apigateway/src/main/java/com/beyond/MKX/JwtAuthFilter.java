package com.beyond.MKX;

import io.jsonwebtoken.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import java.util.List;

@Component
public class JwtAuthFilter implements GlobalFilter {

    @Value("${jwt.secretKeyAt}")
    private String secretKeyAt;

    private static final List<String> ALLOWED_PATHS = List.of(
            "/member/create",
            "/member/doLogin",
            "/member/refresh-token",
            "/product/list"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // token 검증
        System.out.println("token 검증 시작");
        String bearerToken = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        String path = exchange.getRequest().getURI().getRawPath();
        System.out.println(path);
        // 인증이 필요 없는 경로는 필터를 통과
        if (ALLOWED_PATHS.contains(path)) {
            System.out.println("pass url = " + path);
            return chain.filter(exchange);
        }

        try {
            if (bearerToken == null || !bearerToken.startsWith("Bearer ")) {
                throw new IllegalArgumentException("token 관련 예외 발생");
            }
            String token = bearerToken.substring(7);

            // token 검증 및 claims 추출
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(secretKeyAt)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            // 사용자 ID 추출
            String userId = claims.getSubject();
            String role = claims.get("role", String.class);

            // 헤더에 X-User-Id변수로 id값 추가 및 ROLE 추가
            // X를 붙이는 것은 custom header라는 것을 의미하는 널리 쓰이는 관례
            ServerWebExchange modifiedExchange = exchange.mutate()
                    .request(builder -> builder
                            .header("X-User-Id", userId)
                            .header("X-User-Role", "ROLE_" + role) // 역할 추가
                    )
                    .build();

            // Spring Cloud Gateway는 여러 필터를 GatewayFilterChain이라는 구조로 관리
            // 다시 filter chain으로 되돌아 가는 로직.
            return chain.filter(modifiedExchange);
        } catch (IllegalArgumentException | MalformedJwtException | ExpiredJwtException | SignatureException |
                 UnsupportedJwtException e) {
            e.printStackTrace();
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }
}

