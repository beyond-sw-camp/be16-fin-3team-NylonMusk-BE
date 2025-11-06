package com.beyond.MKX.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * JWT 토큰 검증 Provider
 * 
 * WebSocket 인증에서 JWT 토큰을 검증하는 역할을 수행합니다.
 */
@Component
@Slf4j
public class JwtTokenProvider {
    
    @Value("${jwt.secretKeyAt:default-secret-key-for-development-only-minimum-256-bits}")
    private String secretKey;
    
    /**
     * JWT 토큰 검증 및 사용자명 추출
     * 
     * @param token JWT 액세스 토큰
     * @return 사용자명
     * @throws JwtException 토큰이 유효하지 않은 경우
     */
    public String validateAndGetUsername(String token) throws JwtException {
        try {
            // JWT secret이 기본값이면 경고
            if (secretKey.startsWith("default-secret-key")) {
                log.warn("[JWT] ⚠️ Using default JWT secret - should be configured in production");
            }
            
            SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
            Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
            
            String username = claims.getSubject();
            String userId = claims.get("userId", String.class);
            
            // username이 없으면 userId 사용
            String result = username != null ? username : userId;
            
            if (result == null || result.isEmpty()) {
                throw new JwtException("Username not found in token");
            }
            
            log.debug("[JWT] Token validated for user: {}", result);
            return result;
            
        } catch (ExpiredJwtException e) {
            log.warn("[JWT] Token expired");
            throw new JwtException("Token expired", e);
            
        } catch (UnsupportedJwtException e) {
            log.warn("[JWT] Unsupported token");
            throw new JwtException("Unsupported token", e);
            
        } catch (MalformedJwtException e) {
            log.warn("[JWT] Malformed token");
            throw new JwtException("Malformed token", e);
            
        } catch (SecurityException e) {
            log.warn("[JWT] Invalid signature");
            throw new JwtException("Invalid signature", e);
            
        } catch (Exception e) {
            log.error("[JWT] Token validation failed", e);
            throw new JwtException("Token validation failed", e);
        }
    }
    
    /**
     * JWT 예외 클래스
     */
    public static class JwtException extends Exception {
        public JwtException(String message) {
            super(message);
        }
        
        public JwtException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
