package com.beyond.MKX.common.auth.controller;


import com.beyond.MKX.common.apiResponse.ApiResponse;
import com.beyond.MKX.common.auth.config.AuthCookieProperties;
import com.beyond.MKX.common.auth.dto.LoginReqDto;
import com.beyond.MKX.common.auth.dto.LoginResponseDto;
import com.beyond.MKX.common.auth.repository.RevokedTokenRepository;
import com.beyond.MKX.common.auth.service.JwtService;
import com.beyond.MKX.domain.admin.entity.Admin;
import com.beyond.MKX.domain.admin.repository.AdminRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;


import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * 단일 서비스(mkx-platform-service) 안에서 인증을 담당하는 컨트롤러.
 * - /auth/login   : 이메일/비번 검증 → AT/RT 발급 → HttpOnly 쿠키 저장
 * - /auth/refresh : RT 쿠키 검증 → (AT/RT) 재발급(로테이션)
 * - /auth/logout  : 쿠키 만료 + (서버 저장소에선 RT 블랙리스트 처리 권장)
 * 중요:
 * - 쿠키는 HttpOnly + Secure + SameSite=Lax 로 설정(HTTPS 전제)
 * - 브라우저 요청 쪽에서는 axios/fetch 에 withCredentials:true 필요 (쿠키 전송)
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class  AuthController {

    private final JwtService jwtService;
    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final RevokedTokenRepository revokedTokenRepository;
    private final AuthCookieProperties cookieProps;

    /**
     * 로그인: 이메일/비밀번호 검증 후
     * - AT/RT를 HttpOnly 쿠키로 발급
     * - CSRF-TOKEN을 non-HttpOnly 쿠키로 발급 (프론트에서 읽어 헤더로 보내도록)
     */

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginReqDto req, HttpServletResponse resp) {

        // 1) 사용자 검증
        Admin admin = adminRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new BadCredentialsException("사용자 없음"));

        if (!passwordEncoder.matches(req.getPassword(), admin.getPassword())) {
            throw new BadCredentialsException("비밀번호 불일치");
        }

        // 2) 토큰 생성
        String role = admin.getRole().name();
        String jti = jwtService.newJti();
        String accessToken  = jwtService.createAccessToken(admin.getId(), role);
        String refreshToken = jwtService.createRefreshToken(admin.getId(), jti);


        // 3) CSRF 토큰 생성 (쿠키+헤더 double submit 방식)
        String csrfToken = UUID.randomUUID().toString();

        // Redis에 RT 저장 (userId → RT)
        long rtTtl = jwtService.getExpirationRtMillis();
        revokedTokenRepository.saveRefreshToken(admin.getId(), refreshToken, rtTtl);

        // 4) 쿠키 세팅

        ResponseCookie atCookie = buildCookie(
                "AT", accessToken,
                Duration.ofMillis(jwtService.getExpirationAtMillis()),
                cookieProps.getAccess());

        ResponseCookie rtCookie = buildCookie(
                "RT", refreshToken,
                Duration.ofMillis(jwtService.getExpirationRtMillis()),
                cookieProps.getRefresh());

        ResponseCookie csrfCookie = buildCookie(
                "CSRF-TOKEN", csrfToken,
                Duration.ofMinutes(15),
                cookieProps.getCsrf());

        resp.addHeader(HttpHeaders.SET_COOKIE, atCookie.toString());
        resp.addHeader(HttpHeaders.SET_COOKIE, rtCookie.toString());
        resp.addHeader(HttpHeaders.SET_COOKIE, csrfCookie.toString());

        LoginResponseDto loginResponseDto = new LoginResponseDto(admin.getId(), admin.getEmail(), role);
        return ApiResponse.ok(loginResponseDto, "로그인 완료");
    }

    /**
     * Refresh: RT 쿠키 검증 후 새 AT/RT/CSRF 발급
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@CookieValue(value = "RT", required = false) String rt,
                                     HttpServletResponse resp) {
        if (rt == null) {
            throw new BadCredentialsException("RT 쿠키 없음");
        }

        // 1) RT 파싱
        Claims rtClaims;
        try {
            rtClaims = jwtService.parseRefreshToken(rt);
        } catch (JwtException e) {
            throw new BadCredentialsException("RT 유효하지 않음");
        }

        UUID userId = UUID.fromString(rtClaims.getSubject());
        String oldJti = rtClaims.getId();

        // 2) 블랙리스트 확인
        if (revokedTokenRepository.isRevoked(oldJti)) {
            throw new BadCredentialsException("이미 사용된 RT");
        }

        // 3) Redis 저장된 RT와 비교
        String savedRt = revokedTokenRepository.getRefreshToken(userId);
        if (savedRt == null || !savedRt.equals(rt)) {
            throw new BadCredentialsException("저장된 RT와 불일치");
        }

        // 4) 사용자 확인
        Admin admin = adminRepository.findById(userId)
                .orElseThrow(() -> new BadCredentialsException("사용자 없음"));
        String role = admin.getRole().name();

        // 5) 새 토큰 발급
        String newAT = jwtService.createAccessToken(admin.getId(), role);
        String newJti = jwtService.newJti();
        String newRT = jwtService.createRefreshToken(admin.getId(), newJti);
        String newCSRF = UUID.randomUUID().toString();

        // 6) Redis 업데이트 (이전 jti 블랙리스트 등록 + 새 RT 저장)
        long ttlMillis = rtClaims.getExpiration().getTime() - System.currentTimeMillis();
        revokedTokenRepository.revoke(oldJti, ttlMillis);
        revokedTokenRepository.saveRefreshToken(userId, newRT, jwtService.getExpirationRtMillis());

        // 7) 쿠키 갱신
        ResponseCookie atCookie = buildCookie(
                "AT", newAT,
                Duration.ofMillis(jwtService.getExpirationAtMillis()),
                cookieProps.getAccess());

        ResponseCookie rtCookie = buildCookie(
                "RT", newRT,
                Duration.ofMillis(jwtService.getExpirationRtMillis()),
                cookieProps.getRefresh());

        ResponseCookie csrfCookie = buildCookie(
                "CSRF-TOKEN", newCSRF,
                Duration.ofMinutes(15),
                cookieProps.getCsrf());

        resp.addHeader(HttpHeaders.SET_COOKIE, atCookie.toString());
        resp.addHeader(HttpHeaders.SET_COOKIE, rtCookie.toString());
        resp.addHeader(HttpHeaders.SET_COOKIE, csrfCookie.toString());

        return ApiResponse.ok(null, "토큰 재발급 완료");
    }

    /**
     * 로그아웃: AT/RT/CSRF 쿠키 만료
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@CookieValue(value = "RT", required = false) String rt,
                                    HttpServletRequest request,
                                    HttpServletResponse resp) {
        if (rt != null) {
            try {
                Claims claims = jwtService.parseRefreshToken(rt);
                UUID userId = UUID.fromString(claims.getSubject());
                String jti = claims.getId();

                long ttlMillis = claims.getExpiration().getTime() - System.currentTimeMillis();
                revokedTokenRepository.revoke(jti, ttlMillis);      // 블랙리스트 등록
                revokedTokenRepository.deleteRefreshToken(userId);  // Redis 저장 RT 삭제
            } catch (JwtException ex) {
                // 변조·만료된 RT가 들어왔을 때 추적용으로 최소 메타정보만 남김
                String clientIp = Optional.ofNullable(request.getHeader("X-Forwarded-For"))
                        .map(ip -> ip.split(",")[0].trim())
                        .filter(s -> !s.isEmpty())
                        .orElse(request.getRemoteAddr());
                String userAgent = Optional.ofNullable(request.getHeader(HttpHeaders.USER_AGENT))
                        .orElse("unknown");
                log.warn("refresh token rejected: reason={}, ip={}, user-agent={}",
                        ex.getClass().getSimpleName(), clientIp, userAgent);
            }
        }

        ResponseCookie atExpired = buildCookie(
                "AT", "", Duration.ZERO, cookieProps.getAccess());
        ResponseCookie rtExpired = buildCookie(
                "RT", "", Duration.ZERO, cookieProps.getRefresh());
        ResponseCookie csrfExpired = buildCookie(
                "CSRF-TOKEN", "", Duration.ZERO, cookieProps.getCsrf());

        resp.addHeader(HttpHeaders.SET_COOKIE, atExpired.toString());
        resp.addHeader(HttpHeaders.SET_COOKIE, rtExpired.toString());
        resp.addHeader(HttpHeaders.SET_COOKIE, csrfExpired.toString());

        return ApiResponse.ok(null, "로그아웃 성공");
    }
    private ResponseCookie buildCookie(String name,
                                       String value,
                                       Duration maxAge,
                                       AuthCookieProperties.CookieOption option) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, value)
                .httpOnly(option.isHttpOnly())
                .secure(option.isSecure())
                .sameSite(option.getSameSite())
                .path(option.getPath());

        if (Duration.ZERO.equals(maxAge)) {
            builder.maxAge(0);
        } else {
            builder.maxAge(maxAge);
        }
        return builder.build();
    }
}