package com.beyond.MKX.common.auth.filter;

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

@Component
public class CsrfFilter implements GlobalFilter, Ordered {

private static final List<String> CSRF_WHITELIST = List.of(
        "/auth/**",
        "/health"
);

    private static final Set<String> SERVICE_PREFIXES = Set.of(
            "/mkx-platform-service",
            "/ordering-service",
            "/matching-engine-service",
            "/marketData-service"
    );

    private final AntPathMatcher matcher = new AntPathMatcher();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        String rawPath = exchange.getRequest().getURI().getRawPath();
        String path = normalize(rawPath);

        HttpMethod method = request.getMethod();

        if (HttpMethod.OPTIONS.equals(method) || HttpMethod.GET.equals(method)) {
            return chain.filter(exchange);
        }

        if (isWhitelisted(path)) {
            return chain.filter(exchange);
        }

        HttpCookie csrfCookie = request.getCookies().getFirst("CSRF-TOKEN");
        String csrfHeader = request.getHeaders().getFirst("X-CSRF-Token");

        if (csrfCookie == null || csrfCookie.getValue() == null
                || csrfHeader == null
                || !csrfCookie.getValue().equals(csrfHeader)) {

            byte[] payload = String.format(
                    "{\"status_code\":401,\"status_message\":\"%s\"}", "CSRF 유효하지 않음"
            ).getBytes(StandardCharsets.UTF_8);

            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(payload);
            return exchange.getResponse().writeWith(Mono.just(buffer));
        }

        return chain.filter(exchange);
    }

    private boolean isWhitelisted(String path) {
        for (String pattern : CSRF_WHITELIST) {
            if (matcher.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String rawPath) {
        String p = (rawPath == null || rawPath.isEmpty()) ? "/" : rawPath;
        for (String prefix : SERVICE_PREFIXES) {
            if (p.startsWith(prefix)) {
                p = p.substring(prefix.length());
                break;
            }
        }
        return p.isEmpty() ? "/" : p;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }
}
