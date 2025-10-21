package com.beyond.MKX.domain.ipo.allocation.config;

import com.beyond.MKX.common.auth.security.CustomAdminPrincipal;
import com.beyond.MKX.common.auth.security.CustomMemberPrincipal;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.context.SecurityContextHolder;

@Configuration
public class FeignAuthInterceptor implements RequestInterceptor {
    @Override public void apply(RequestTemplate t) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return;
        if (auth.getPrincipal() instanceof CustomAdminPrincipal a) {
            t.header("X-User-Id", a.id().toString());
            t.header("X-User-Role", a.role().name());
        } else if (auth.getPrincipal() instanceof CustomMemberPrincipal m) {
            t.header("X-User-Id", m.id().toString());
            t.header("X-User-Role", "MEMBER");
        }
    }
}
