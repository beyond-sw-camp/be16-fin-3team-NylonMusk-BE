package com.beyond.MKX.domain.assets.config;

import feign.RequestInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public class FeignAuthConfig {
    private static final String HDR_USER = "X-User-Id";
    private static final String HDR_ROLE = "X-User-Role";

    @Bean
    public RequestInterceptor gatewayHeaderPropagatingInterceptor() {
        return template -> {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return; // 비동기/배치 등 요청 컨텍스트 없음
            HttpServletRequest req = attrs.getRequest();

            String user = req.getHeader(HDR_USER);
            String role = req.getHeader(HDR_ROLE);

            if (user != null && !user.isBlank()) template.header(HDR_USER, user);
            if (role != null && !role.isBlank()) template.header(HDR_ROLE, role);
        };
    }
}
