package com.beyond.MKX.common.auth.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

/**
 * HS512 + Base64 시크릿 키를 사용해 AT/RT를 발급/검증하는 서비스.
 * - expirationAt / expirationRt : application-local.yml 의 ms 단위 만료시간 사용
 * - AT(Access Token)에는 userId, role(문자열) 같은 클레임을 포함
 * - RT(Refresh Token)에는 최소한 sub(userId) + jti(고유 ID)를 넣고, 서버 저장소에 jti를 기록해서 로테이션/블랙리스트 적용 권장
 */
@Service
public class JwtService {




    @Value("${jwt.secretKeyAt}")
    private String secretKeyAtValue;   // AT 검증/서명 키 (Base64 혹은 plain)
    @Value("${jwt.secretKeyRt}")
    private String secretKeyRtValue;   // RT 검증/서명 키 (Base64 혹은 plain)
    @Value("${jwt.expirationAt}")
    private long expirationAtMillis;    // AT 만료(ms)
    @Value("${jwt.expirationRt}")
    private long expirationRtMillis;    // RT 만료(ms)

    private SecretKey accessKey;
    private SecretKey refreshKey;

    @PostConstruct
    public void initKeys() {
        this.accessKey = toSecretKey(secretKeyAtValue);
        this.refreshKey = toSecretKey(secretKeyRtValue);
    }

    private SecretKey toSecretKey(String value) {
        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(value);
        } catch (IllegalArgumentException ex) {
            keyBytes = value.getBytes(StandardCharsets.UTF_8);
        }
        return new SecretKeySpec(keyBytes, SignatureAlgorithm.HS512.getJcaName());
    }

    /** Access Token 생성: userId + role 포함, 짧은 수명 */
    public String createAccessToken(UUID userId, String role) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + expirationAtMillis);
        return Jwts.builder()
                .setSubject(userId.toString())         // sub = userId
                .claim("role", role)                   // 예: ADMIN
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(accessKey, SignatureAlgorithm.HS512)
                .compact();
    }

    /** Refresh Token 생성: userId + jti 포함, 긴 수명 (서버 저장소에 jti 기록 권장) */
    public String createRefreshToken(UUID userId, String jti) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + expirationRtMillis);
        return Jwts.builder()
                .setSubject(userId.toString())         // sub = userId
                .setId(jti)                                 // jti = RT 식별자(로테이션/블랙리스트 용)
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(refreshKey, SignatureAlgorithm.HS512)
                .compact();
    }

    /** AT 파싱/검증 → 유효하면 Claims 반환, 실패 시 예외 */
    public Claims parseAccessToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(accessKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /** RT 파싱/검증 → 유효하면 Claims 반환, 실패 시 예외 */
    public Claims parseRefreshToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(refreshKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /** RT jti 생성 (RT 로테이션/무효화 추적용) */
    public String newJti() {
        return UUID.randomUUID().toString();
    }

    public long getExpirationAtMillis() {
        return expirationAtMillis;
    }

    public long getExpirationRtMillis() {
        return expirationRtMillis;
    }

}
