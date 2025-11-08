package com.beyond.MKX.common.auth.controller;


import com.beyond.MKX.common.apiResponse.ApiResponse;
import com.beyond.MKX.common.auth.config.AuthCookieProperties;
import com.beyond.MKX.common.auth.dto.LoginReqDto;
import com.beyond.MKX.common.auth.dto.LoginResponseDto;
import com.beyond.MKX.common.auth.repository.RevokedTokenRepository;
import com.beyond.MKX.common.auth.service.JwtService;
import com.beyond.MKX.domain.admin.entity.Admin;
import com.beyond.MKX.domain.admin.repository.AdminRepository;
import com.beyond.MKX.domain.member.dto.MemberLoginReqDto;
import com.beyond.MKX.domain.member.entity.Member;
import com.beyond.MKX.domain.member.service.MemberService;
import com.beyond.MKX.common.auth.service.LoginAttemptService;
import com.beyond.MKX.common.auth.service.CaptchaService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;


import java.time.Duration;
import java.util.Map;
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
    private final MemberService memberService;
    private final LoginAttemptService loginAttemptService;
    private final CaptchaService captchaService;

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

        LoginResponseDto loginResponseDto = LoginResponseDto.builder()
                .userId(admin.getId())
                .name(admin.getName())
                .email(admin.getEmail())
                .role(role)
                .status(admin.getStatus().name())
                .corporationId(admin.getCorporation() != null ? admin.getCorporation().getId() : null)
                .build();
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
                .path(option.getPath())
                .domain(option.getDomain());

        if (Duration.ZERO.equals(maxAge)) {
            builder.maxAge(0);
        } else {
            builder.maxAge(maxAge);
        }
        return builder.build();
    }

    @PostMapping(value = "/member/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> memberLogin(@Valid @RequestBody MemberLoginReqDto req, HttpServletResponse resp) {
        // CAPTCHA: 5회 이상 실패 시 CAPTCHA 검증 필수
        boolean requiresCaptcha = loginAttemptService.requiresCaptcha(req.getEmail());
        if (requiresCaptcha) {
            if (req.getCaptchaKey() == null || req.getCaptchaValue() == null || req.getCaptchaType() == null) {
                int remainingAttempts = loginAttemptService.getRemainingAttempts(req.getEmail());
                return ApiResponse.error(
                    HttpStatus.BAD_REQUEST,
                    "로그인 실패 횟수가 5회를 초과했습니다. CAPTCHA를 입력해주세요.",
                    Map.of("requiresCaptcha", true, "failedAttempts", loginAttemptService.getFailedAttempts(req.getEmail()))
                );
            }
            
            // CAPTCHA 검증: 타입에 따라 이미지 또는 음성 검증
            boolean captchaValid = false;
            String captchaType = req.getCaptchaType().toLowerCase();
            
            if ("image".equals(captchaType)) {
                // 이미지 CAPTCHA 검증
                captchaValid = captchaService.verifyCaptcha(req.getCaptchaKey(), req.getCaptchaValue());
            } else if ("audio".equals(captchaType)) {
                // 음성 CAPTCHA 검증 (음성 키 사용)
                captchaValid = captchaService.verifyAudioCaptcha(req.getCaptchaKey(), req.getCaptchaValue());
            } else {
                return ApiResponse.error(
                    HttpStatus.BAD_REQUEST,
                    "잘못된 CAPTCHA 타입입니다. (image 또는 audio)",
                    Map.of("requiresCaptcha", true, "failedAttempts", loginAttemptService.getFailedAttempts(req.getEmail()))
                );
            }
            
            if (!captchaValid) {
                return ApiResponse.error(
                    HttpStatus.BAD_REQUEST,
                    "CAPTCHA 검증에 실패했습니다. 다시 시도해주세요.",
                    Map.of("requiresCaptcha", true, "failedAttempts", loginAttemptService.getFailedAttempts(req.getEmail()))
                );
            }
        }

        try {
            Member member = memberService.authenticate(req);

            // LOGIN_SUCCESS: 로그인 성공 시 실패 횟수 초기화
            loginAttemptService.resetFailedAttempts(req.getEmail());

            String role = "MEMBER";
            String jti = jwtService.newJti();
            String accessToken = jwtService.createAccessToken(member.getId(), role);
            String refreshToken = jwtService.createRefreshToken(member.getId(), jti);
            revokedTokenRepository.saveRefreshToken(member.getId(), refreshToken, jwtService.getExpirationRtMillis());

            String csrfToken = UUID.randomUUID().toString();

            resp.addHeader(HttpHeaders.SET_COOKIE, buildCookie(
                    "AT", accessToken,
                    Duration.ofMillis(jwtService.getExpirationAtMillis()),
                    cookieProps.getAccess()).toString());
            resp.addHeader(HttpHeaders.SET_COOKIE, buildCookie(
                    "RT", refreshToken,
                    Duration.ofMillis(jwtService.getExpirationRtMillis()),
                    cookieProps.getRefresh()).toString());
            resp.addHeader(HttpHeaders.SET_COOKIE, buildCookie(
                    "CSRF-TOKEN", csrfToken,
                    Duration.ofMinutes(15),
                    cookieProps.getCsrf()).toString());

            LoginResponseDto response = LoginResponseDto.builder()
                    .userId(member.getId())
                    .email(member.getEmail())
                    .role(role)
                    .status(member.getStatus().name())
                    .build();

            return ApiResponse.ok(response, "회원 로그인 완료");
            
        } catch (Exception e) {
            // LOGIN_FAILED: 로그인 실패 시 횟수 증가
            int failedAttempts = loginAttemptService.incrementFailedAttempts(req.getEmail());
            int remainingAttempts = loginAttemptService.getRemainingAttempts(req.getEmail());
            boolean nowRequiresCaptcha = loginAttemptService.requiresCaptcha(req.getEmail());
            
            String errorMessage = e.getMessage() != null ? e.getMessage() : "로그인에 실패했습니다.";
            
            if (nowRequiresCaptcha) {
                errorMessage = "로그인 실패 횟수가 5회를 초과했습니다. CAPTCHA를 입력해주세요.";
            } else if (remainingAttempts > 0) {
                errorMessage = String.format("로그인에 실패했습니다. 남은 시도 횟수: %d회", remainingAttempts);
            }
            
            return ApiResponse.error(
                HttpStatus.UNAUTHORIZED,
                errorMessage,
                Map.of(
                    "requiresCaptcha", nowRequiresCaptcha,
                    "failedAttempts", failedAttempts,
                    "remainingAttempts", remainingAttempts
                )
            );
        }
    }

    @PostMapping(value = "/member/refresh")
    public ResponseEntity<?> memberRefresh(@CookieValue(value = "RT", required = false) String refreshToken,
                                           HttpServletResponse resp) {
        if (refreshToken == null) {
            throw new BadCredentialsException("RT 쿠키 없음");
        }

        Claims claims;
        try {
            claims = jwtService.parseRefreshToken(refreshToken);
        } catch (JwtException ex) {
            throw new BadCredentialsException("RT 유효하지 않음", ex);
        }

        UUID memberId = UUID.fromString(claims.getSubject());
        String oldJti = claims.getId();

        if (oldJti != null && revokedTokenRepository.isRevoked(oldJti)) {
            throw new BadCredentialsException("이미 사용된 RT");
        }

        String savedRt = revokedTokenRepository.getRefreshToken(memberId);
        if (savedRt == null || !savedRt.equals(refreshToken)) {
            throw new BadCredentialsException("저장된 RT와 불일치");
        }

        Member member = memberService.findById(memberId);
        String role = "MEMBER";

        String newJti = jwtService.newJti();
        String newAccessToken = jwtService.createAccessToken(memberId, role);
        String newRefreshToken = jwtService.createRefreshToken(memberId, newJti);

        if (oldJti != null) {
            long ttlMillis = Math.max(0, claims.getExpiration().getTime() - System.currentTimeMillis());
            revokedTokenRepository.revoke(oldJti, ttlMillis);
        }

        revokedTokenRepository.saveRefreshToken(memberId, newRefreshToken, jwtService.getExpirationRtMillis());

        String csrfToken = UUID.randomUUID().toString();

        resp.addHeader(HttpHeaders.SET_COOKIE, buildCookie(
                "AT", newAccessToken,
                Duration.ofMillis(jwtService.getExpirationAtMillis()),
                cookieProps.getAccess()).toString());
        resp.addHeader(HttpHeaders.SET_COOKIE, buildCookie(
                "RT", newRefreshToken,
                Duration.ofMillis(jwtService.getExpirationRtMillis()),
                cookieProps.getRefresh()).toString());
        resp.addHeader(HttpHeaders.SET_COOKIE, buildCookie(
                "CSRF-TOKEN", csrfToken,
                Duration.ofMinutes(15),
                cookieProps.getCsrf()).toString());

        LoginResponseDto response = LoginResponseDto.builder()
                .userId(member.getId())
                .email(member.getEmail())
                .role(role)
                .status(member.getStatus().name())
                .build();

        return ApiResponse.ok(response, "회원 토큰 재발급 완료");
    }

    @PostMapping("/member/logout")
    public ResponseEntity<?> memberLogout(@CookieValue(value = "RT", required = false) String refreshToken,
                                          HttpServletResponse resp) {
        if (refreshToken != null) {
            try {
                Claims claims = jwtService.parseRefreshToken(refreshToken);
                UUID memberId = UUID.fromString(claims.getSubject());
                String jti = claims.getId();
                if (jti != null) {
                    long ttlMillis = Math.max(0, claims.getExpiration().getTime() - System.currentTimeMillis());
                    revokedTokenRepository.revoke(jti, ttlMillis);
                }
                revokedTokenRepository.deleteRefreshToken(memberId);
            } catch (JwtException ex) {
                log.warn("member refresh token rejected: {}", ex.getMessage());
            }
        }

        ResponseCookie atExpired = buildCookie("AT", "", Duration.ZERO, cookieProps.getAccess());
        ResponseCookie rtExpired = buildCookie("RT", "", Duration.ZERO, cookieProps.getRefresh());
        ResponseCookie csrfExpired = buildCookie("CSRF-TOKEN", "", Duration.ZERO, cookieProps.getCsrf());

        resp.addHeader(HttpHeaders.SET_COOKIE, atExpired.toString());
        resp.addHeader(HttpHeaders.SET_COOKIE, rtExpired.toString());
        resp.addHeader(HttpHeaders.SET_COOKIE, csrfExpired.toString());

        return ApiResponse.ok(null, "회원 로그아웃 성공");
    }

}
