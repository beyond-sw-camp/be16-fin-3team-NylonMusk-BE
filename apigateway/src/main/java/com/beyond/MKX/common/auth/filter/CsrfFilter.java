package com.beyond.MKX.common.auth.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

/**
 * MKX Gateway CSRF 보호 필터.
 *
 * 역할
 * 1) CSRF(Double Submit Cookie) 토큰 검증을 수행한다.
 *    - 클라이언트는 동일한 토큰을 쿠키(`CSRF-TOKEN`)와 헤더(`X-CSRF-Token`)에 모두 전송해야 한다.
 *    - 서버는 두 값이 일치하는지 확인함으로써 요청 위조를 방지한다.
 *
 * 2) GET 및 OPTIONS 요청은 CSRF 위험이 없으므로 인증 없이 통과시킨다.
 * 3) 인증/헬스체크 등 화이트리스트 경로도 검증에서 제외한다.
 *
 * - Double Submit Cookie 패턴: 세션을 사용하지 않고도 CSRF 보호 가능.
 * - CSRF 토큰은 HttpOnly=false 속성으로 클라이언트 JS에서 접근 가능해야 한다.
 * - 이 필터는 Gateway 수준에서 POST, PUT, DELETE 등의 요청만 검증한다.
 * - 실패 시 JSON 응답(403)으로 명확히 반환하여 디버깅 편의성을 높인다.
 */
@Component
public class CsrfFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(CsrfFilter.class);

    /** CSRF 검증을 생략할 경로 목록 */
    private static final List<String> CSRF_WHITELIST = List.of(
            "/auth/**",   // 로그인/회원가입 등 인증 관련 엔드포인트
            "/health",     // 헬스체크
            "/order/**",
            "/test/**",
            "/public/**",
            "/api/stocks/**",
            "/api/public/**",
            "/ipo/**",
            "/my-stocks",
            "/swagger-ui/**",
            "/trading-home/**"
    );

    /** Gateway에서 경로 정규화 시 제거할 서비스 접두사 */
    private static final Set<String> SERVICE_PREFIXES = Set.of(
            "/mkx-platform-service",
            "/ordering-service",
            "/matching-engine-service",
            "/market-data-service",
            "/community-service",
            "/trading-bot-service"
    );

    private final AntPathMatcher matcher = new AntPathMatcher();

    /**
     * 필터 메인 로직.
     * 1) GET / OPTIONS 요청은 위험이 없으므로 통과.
     * 2) 화이트리스트 경로 통과.
     * 3) 그 외 요청은 Double Submit Cookie 패턴으로 CSRF 토큰 검증.
     * 4) 불일치 또는 누락 시 403 JSON 응답.
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        HttpMethod method = request.getMethod();

        // 1) 안전한 메서드(GET, OPTIONS)는 인증 필요 없음
        if (method == null || HttpMethod.OPTIONS.equals(method) || HttpMethod.GET.equals(method)) {
            return chain.filter(exchange);
        }

        String rawPath = request.getURI().getRawPath();
        String path = normalize(rawPath);

        // 2) 화이트리스트 경로 통과
        if (isWhitelisted(path)) {
            return chain.filter(exchange);
        }

        // 3) 쿠키와 헤더에서 CSRF 토큰 추출
        HttpCookie csrfCookie = request.getCookies().getFirst("CSRF-TOKEN");
        String csrfHeader = request.getHeaders().getFirst("X-CSRF-Token");

        // 4) Double Submit Cookie 검증
        // 쿠키 또는 헤더가 없거나 불일치하면 403 반환
        if (csrfCookie == null || csrfCookie.getValue() == null
                || csrfHeader == null
                || !csrfCookie.getValue().equals(csrfHeader)) {

            log.warn("[CSRF] Invalid or missing CSRF token for {}", rawPath);

            byte[] payload = """
                {
                  "status_code": 403,
                  "status_message": "CSRF 토큰이 유효하지 않습니다."
                }
                """.getBytes(StandardCharsets.UTF_8);

            exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
            exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(payload);
            // WebFlux에서 Content-Length를 명시하지 않고 writeWith만 호출해야
            // chunked 응답으로 안전하게 전달된다.
            return exchange.getResponse().writeWith(Mono.just(buffer));
        }

        // 5) 토큰 일치 → 정상 통과
        return chain.filter(exchange);
    }

    /**
     * 주어진 경로가 CSRF 예외(화이트리스트)에 해당하는지 검사.
     * - ant 패턴을 사용하므로 "/auth/**" 형태의 와일드카드 매칭이 가능하다.
     */
    private boolean isWhitelisted(String path) {
        for (String pattern : CSRF_WHITELIST) {
            if (matcher.match(pattern, path)) return true;
        }
        return false;
    }

    /**
     * 서비스 접두사 제거 유틸.
     * - Gateway 요청 경로에서 "/{service-name}" 형태의 접두사를 제거한다.
     * - 나머지 경로를 기준으로 화이트리스트를 일관되게 체크할 수 있다.
     */
    private String normalize(String rawPath) {
        if (rawPath == null || rawPath.isEmpty()) return "/";
        for (String prefix : SERVICE_PREFIXES) {
            if (rawPath.startsWith(prefix)) {
                String trimmed = rawPath.substring(prefix.length());
                return trimmed.isEmpty() ? "/" : trimmed;
            }
        }
        return rawPath;
    }

    /**
     * 필터 우선순위 설정.
     * - JWT 필터보다 약간 뒤에서 실행되도록 설정한다.
     * - (JwtAuthFilter: +1, CsrfFilter: +2)
     */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 2;
    }
}
