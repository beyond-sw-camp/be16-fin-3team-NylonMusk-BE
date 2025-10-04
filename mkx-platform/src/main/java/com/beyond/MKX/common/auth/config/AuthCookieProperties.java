package com.beyond.MKX.common.auth.config;

import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "auth.cookie")
public class AuthCookieProperties {
    private CookieOption access = new CookieOption();
    private CookieOption refresh = new CookieOption();
    private CookieOption csrf = new CookieOption();

    /**
     * auth.cookie.* 설정을 통으로 바인딩.
     * configs 키는 access/refresh/csrf 등 쿠키 이름.
     */
    private Map<String, CookieOption> configs = new HashMap<>();

    /**
     * 쿠키 이름으로 설정 조회 (없으면 null).
     */
    public CookieOption get(String key) {
        return configs.get(key);
    }

    /**
     * 단일 쿠키에 대한 옵션 묶음.
     */
    @Getter
    @Setter
    public static class CookieOption {
        // ResponseCookie.httpOnly 플래그
        private boolean httpOnly;
        // ResponseCookie.secure 플래그
        private boolean secure;
        // ResponseCookie.sameSite 값
        private String sameSite;
        // ResponseCookie.path 값
        private String path;
    }
}