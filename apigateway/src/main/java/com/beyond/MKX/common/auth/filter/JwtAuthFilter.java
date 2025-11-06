package com.beyond.MKX.common.auth.filter;

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
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * API Gateway 전역 JWT 인증 필터.
 *
 * 역할
 * 1) 인증 예외 경로(화이트리스트) 및 프리플라이트(OPTIONS) 요청을 무조건 통과시킨다.
 * 2) 나머지 요청은 Gateway 쿠키 "AT"에서 Access Token을 꺼내 HS512로 검증한다.
 * 3) 검증 성공 시 다운스트림 서비스가 신뢰할 수 있도록 헤더(X-User-Id, X-User-Role)를 주입한다.
 *
 */
@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    /** application.yml 에서 주입되는 AT 비밀키(평문 또는 Base64) 원본 값 */
    private final String secretKeyValue;

    /** HS512 서명/검증에 사용할 시크릿 키. @PostConstruct 이후에 초기화된다. */
    private SecretKey atKey;

    /** 경로 패턴 매칭 유틸리티 (ant-style) */
    private final AntPathMatcher matcher = new AntPathMatcher();

    /**
     * 인증이 면제되는 경로 패턴 목록.
     * normalize()로 서비스 접두사 제거 후 이 패턴과 매칭한다.
     * 예: "/auth/**" → 로그인, 토큰 재발급 등
     *     "/health"  → 헬스체크
     *     "/ws/**"   → WebSocket 연결 (Optional 인증)
     */
    private static final List<String> AUTH_WHITELIST = List.of(
            "/auth/**",
            "/health",
            "/order/**",
            "/test/**",
            "/public/**",
//            "/ipo/**"
            "/api/stocks/**",
            "/api/public/**",
            "/my-stocks",
            "/swagger-ui/**",
            "/ws/**",  // WebSocket 연결 (Optional 인증 - MarketData Service에서 처리)
            "/trading-home/**",      // 트레이딩 홈 조회 요청
            "/api/market/rank/**",   // 랭킹 조회 요청
            "/api/v1/market/chart/mini" // mini chart 조회 요청
    );

    /**
     * Gateway 외부에서 들어온 실제 요청 경로는 "/{서비스명}/..." 형태일 수 있다.
     * normalize()는 아래 접두사를 제거해 공통 라우팅 규칙과 화이트리스트 체크를 단순화한다.
     */
    private static final Set<String> SERVICE_PREFIXES = Set.of(
            "/mkx-platform-service",
            "/ordering-service",
            "/matching-engine-service",
            "/market-data-service",
            "/community-service",
            "/trading-bot-service"
    );

    public JwtAuthFilter(@Value("${jwt.secretKeyAt}") String secretKeyAtValue) {
        this.secretKeyValue = secretKeyAtValue;
    }

    /**
     * 필터 실행 순서.
     * Ordered.HIGHEST_PRECEDENCE 는 매우 작은 값이므로, +1을 해도 여전히 최상위권이다.
     * (예: CSRF 필터가 더 높은 우선순위를 원하면 이 값과 상대 비교해 조절)
     */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }


    // Bean 초기화 이후 시점에 서명 키를 준비한다.
    @PostConstruct
    public void initKey() {
        this.atKey = decodeKey(secretKeyValue);
        log.info("[JWT] JwtAuthFilter initialized with HS512 key");
    }

    // HS512용 SecretKey 생성 유틸.
    private SecretKey decodeKey(String value) {
        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(value);
        } catch (IllegalArgumentException ex) {
            keyBytes = value.getBytes(StandardCharsets.UTF_8);
        }
        return new SecretKeySpec(keyBytes, SignatureAlgorithm.HS512.getJcaName());
    }

    /**
     * 필터 메인 로직.
     * 1) OPTIONS(CORS 프리플라이트)는 무조건 통과.
     * 2) 경로 정규화 후 화이트리스트에 해당하면 통과.
     * 3) 쿠키 "AT"에서 토큰을 꺼내 서명/만료를 검증.
     * 4) 성공 시 X-User-Id, X-User-Role 헤더를 주입하고 체인을 진행.
     * 5) 실패 시 401 JSON 응답으로 종료.
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        HttpMethod method = request.getMethod();

        // 1) 프리플라이트 요청은 인증 검증 없이 통과
        if (HttpMethod.OPTIONS.equals(method)) {
            return chain.filter(exchange);
        }

        String rawPath = request.getURI().getPath();
        String normalizedPath = normalize(rawPath);

        // 2) 화이트리스트 경로는 인증 제외
        if (isWhitelisted(normalizedPath)) {
            return chain.filter(exchange);
        }

        // 3) Access Token 쿠키 존재 여부 확인
        HttpCookie atCookie = request.getCookies().getFirst("AT");
        if (atCookie == null || atCookie.getValue() == null || atCookie.getValue().isEmpty()) {
            return writeJson(exchange, HttpStatus.UNAUTHORIZED,
                    "Access Token이 없습니다. 로그인 후 다시 시도해주세요.");
        }

        String token = atCookie.getValue();

        try {
            // 4) JWT 파싱 및 검증 (서명/만료 포함)
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(atKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            // 표준 클레임: subject(사용자 식별자)
            String userId = Optional.ofNullable(claims.getSubject()).orElse("");
            // 커스텀 클레임: role(문자열 권한)
            String role = Optional.ofNullable(claims.get("role", String.class)).orElse("");

            log.debug("[JWT] Authenticated userId={}, role={} (path={})", userId, role, rawPath);

            // 5) 다운스트림 서비스가 인증 컨텍스트를 신뢰할 수 있도록 헤더 주입
            //    - 내부망/서비스 간 통신을 전제로 한다.
            //    - 각 서비스는 외부로부터 동일 이름 헤더가 유입되지 않도록 게이트웨이 앞 단을 강제해야 한다.
            //    - WebSocket 연결 시에도 이 헤더들이 전달되어 STOMP 인증에 사용됨

            // 이메일 정보 추출 (JWT claims에서)
            String email = Optional.ofNullable(claims.get("email", String.class)).orElse("");

            ServerHttpRequest mutated = request.mutate()
                    .header("X-User-Id", userId)
                    .header("X-User-Email", email)
                    .header("X-User-Role", role)
                    .build();

            return chain.filter(exchange.mutate().request(mutated).build());

        } catch (JwtException e) {
            // 6) 서명 불일치, 만료, 포맷 오류 등 JWT 예외를 401로 응답
            log.warn("[JWT] Invalid or expired token for {} -> {}", rawPath, e.getMessage());
            return writeJson(exchange, HttpStatus.UNAUTHORIZED,
                    "유효하지 않거나 만료된 Access Token입니다.");
        }
    }

    /**
     * 간단한 JSON 에러 응답 유틸리티.
     * - 게이트웨이 단계에서 에러를 명확히 전달하기 위해 JSON 바디로 응답한다.
     * - 응답 바디는 간단한 상태 코드/메시지 스키마를 사용한다.
     */
    private Mono<Void> writeJson(ServerWebExchange exchange, HttpStatus status, String message) {
        byte[] body = String.format("""
            {"status_code": %d, "status_message": "%s"}
            """, status.value(), message).getBytes(StandardCharsets.UTF_8);

        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        exchange.getResponse().getHeaders().setContentLength(body.length);

        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(body);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    /**
     * normalize()로 정규화된 경로가 화이트리스트 패턴에 매칭되는지 검사한다.
     * - ant 패턴을 사용하므로 "/auth/**" 같은 와일드카드 매칭이 가능하다.
     */
    private boolean isWhitelisted(String path) {
        for (String pattern : AUTH_WHITELIST) {
            if (matcher.match(pattern, path)) return true;
        }
        return false;
    }

    /**
     * 서비스 접두사 제거 유틸.
     * - 실제 외부 요청 경로가 "/{service}/api.." 형태일 때 공통 라우팅/정책을 적용하기 쉽도록 만든다.
     * - 접두사가 일치하면 해당 부분을 떼고, 빈 문자열이 되면 "/"로 치환한다.
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
}
