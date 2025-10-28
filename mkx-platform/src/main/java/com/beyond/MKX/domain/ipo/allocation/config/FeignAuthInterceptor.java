//package com.beyond.MKX.domain.ipo.allocation.config;
//
//import com.beyond.MKX.common.auth.security.CustomAdminPrincipal;
//import com.beyond.MKX.common.auth.security.CustomMemberPrincipal;
//import feign.RequestInterceptor;
//import feign.RequestTemplate;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.security.core.context.SecurityContextHolder;
//
//@Slf4j
//@Configuration
//public class FeignAuthInterceptor implements RequestInterceptor {
//    @Override
//    public void apply(RequestTemplate t) {
//        var auth = SecurityContextHolder.getContext().getAuthentication();
//
//        // ✅ settlement / scheduler / batch 등 내부 로직일 때 강제로 INTERNAL로 전송
//        if (Thread.currentThread().getName().contains("auto-") // 스케줄러 or async
//                || t.url().contains("/api/accounts/member")) {  // 내부 계좌 호출이면
//            t.header("X-User-Role", "INTERNAL");
//            t.header("X-User-Id", "00000000-0000-0000-0000-000000000000");
//            log.info("[Feign] INTERNAL fallback (detected system-level call) → {}", t.headers());
//            return;
//        }
//
//        if (auth == null) {
//            t.header("X-User-Role", "INTERNAL");
//            t.header("X-User-Id", "00000000-0000-0000-0000-000000000000");
//            log.info("[Feign] INTERNAL fallback (no SecurityContext) → {}", t.headers());
//            return;
//        }
//
//        if (auth.getPrincipal() instanceof CustomAdminPrincipal a) {
//            t.header("X-User-Id", a.id().toString());
//            t.header("X-User-Role", a.role().name());
//        } else if (auth.getPrincipal() instanceof CustomMemberPrincipal m) {
//            t.header("X-User-Id", m.id().toString());
//            t.header("X-User-Role", "MEMBER");
//        }
//        log.info("[Feign] Auth headers={}", t.headers());
//    }
//}
