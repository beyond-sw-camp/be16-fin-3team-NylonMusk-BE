package com.beyond.MKX.common.auth.filter;

import org.springframework.core.Ordered;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * ✅ CsrfFilter (Spring Cloud Gateway 전역 필터)
 *
 * - 브라우저에서 들어오는 요청에 대해 CSRF 토큰을 검증하는 역할.
 * - Gateway 레벨에서 쿠키 vs 헤더 비교를 통해 CSRF 공격을 방어.
 *
 * 동작 규칙:
 * 1. GET, OPTIONS 요청은 CSRF 무관 → 그대로 통과
 * 2. 화이트리스트 엔드포인트(/auth/login, /auth/refresh 등)는 통과
 * 3. 나머지 POST/PUT/DELETE 요청은 쿠키 `CSRF-TOKEN`과 헤더 `X-CSRF-Token` 비교
 *    - 값이 같으면 정상 요청
 *    - 다르면 401 Unauthorized 반환
 */
@Component
public class CsrfFilter implements GlobalFilter, Ordered {

    // CSRF 예외 경로 (로그인, 토큰 갱신, 로그아웃, 회원가입)
    private static final List<String> CSRF_WHITELIST = List.of(
            "/auth/login",
            "/auth/refresh",
            "/auth/logout",
            "/auth/signup"
    );

    private final AntPathMatcher matcher = new AntPathMatcher();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        // 요청 경로 (rawPath) 정규화
        String path = normalisePath(request.getURI().getRawPath());

        // 요청 메서드 확인 (GET, POST, PUT, DELETE 등)
        HttpMethod method = request.getMethod();

        // (1) GET, OPTIONS 요청은 CSRF 공격 벡터가 없으므로 그대로 통과
        if (HttpMethod.OPTIONS.equals(method) || HttpMethod.GET.equals(method)) {
            return chain.filter(exchange);
        }

        // (2) 화이트리스트 경로는 CSRF 검증 없이 통과
        if (isWhitelisted(path)) {
            return chain.filter(exchange);
        }

        // (3) 쿠키와 헤더에서 CSRF 토큰 추출
        HttpCookie csrfCookie = request.getCookies().getFirst("CSRF-TOKEN");
        String csrfHeader = request.getHeaders().getFirst("X-CSRF-Token");

        // (4) 토큰이 없거나 불일치 → 401 반환
        if (csrfCookie == null || csrfCookie.getValue() == null
                || csrfHeader == null
                || !csrfCookie.getValue().equals(csrfHeader)) {

            byte[] payload = "{\"status_code\":401,\"status_message\":\"CSRF 유효하지 않음\"}"
                    .getBytes(StandardCharsets.UTF_8);

            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

            // JSON 에러 응답 전송
            return exchange.getResponse()
                    .writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(payload)));
        }

        // (5) 쿠키와 헤더 토큰이 일치하면 요청 정상 처리
        return chain.filter(exchange);
    }

    /**
     * ✅ CSRF 화이트리스트 검사
     * - 미리 지정한 경로(/auth/login 등)는 CSRF 예외 처리
     */
    private boolean isWhitelisted(String path) {
        for (String pattern : CSRF_WHITELIST) {
            if (matcher.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    /**
     * ✅ 경로 정규화
     * - 요청 경로가 빈 문자열("")인 경우 "/"로 치환
     * - 루트 경로 요청 시 일관성 있게 처리하기 위함
     */
    private String normalisePath(String rawPath) {
        return rawPath.isEmpty() ? "/" : rawPath;
    }

    /**
     * ✅ 필터 실행 순서 지정
     * - HIGHEST_PRECEDENCE + 1 → 거의 최상단에서 실행
     * - JWT 검증 필터 이후에 붙이고 싶다면 Order 조절 가능
     */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }
}
