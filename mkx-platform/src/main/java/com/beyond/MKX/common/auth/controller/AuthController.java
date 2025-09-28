package com.beyond.MKX.common.auth.controller;


import com.beyond.MKX.common.apiResponse.ApiResponse;
import com.beyond.MKX.common.auth.dto.LoginReqDto;
import com.beyond.MKX.common.auth.dto.LoginResponseDto;
import com.beyond.MKX.common.auth.repository.RevokedTokenRepository;
import com.beyond.MKX.common.auth.service.JwtService;
import com.beyond.MKX.common.exception.DuplicateResourceException;
import com.beyond.MKX.domain.member.entity.Member;
import com.beyond.MKX.domain.member.repository.MemberRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;


import javax.naming.AuthenticationException;
import java.time.Duration;
import java.util.UUID;

/**
 * 단일 서비스(mkx-platform-service) 안에서 인증을 담당하는 컨트롤러.
 * - /auth/login   : 이메일/비번 검증 → AT/RT 발급 → HttpOnly 쿠키 저장
 * - /auth/refresh : RT 쿠키 검증 → (AT/RT) 재발급(로테이션)
 * - /auth/logout  : 쿠키 만료 + (서버 저장소에선 RT 블랙리스트 처리 권장)
 *
 * 중요:
 * - 쿠키는 HttpOnly + Secure + SameSite=Lax 로 설정(HTTPS 전제)
 * - 브라우저 요청 쪽에서는 axios/fetch 에 withCredentials:true 필요 (쿠키 전송)
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JwtService jwtService;
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final RevokedTokenRepository revokedTokenRepository;

    /**
     * 로그인: 이메일/비밀번호 검증 후
     * - AT/RT를 HttpOnly 쿠키로 발급
     * - CSRF-TOKEN을 non-HttpOnly 쿠키로 발급 (프론트에서 읽어 헤더로 보내도록)
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginReqDto req, HttpServletResponse resp) {

        // 1) 사용자 검증
        Member member = memberRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new DuplicateResourceException("사용자를 찾을 수 없음"));

        if (!passwordEncoder.matches(req.getPassword(), member.getPasswordHash())) {
            throw new DuplicateResourceException("비밀번호 불일치");
        }

        // 2) 토큰 생성
        String rolesCsv = member.getRole().name();
        String jti = jwtService.newJti();
        String accessToken  = jwtService.createAccessToken(member.getId(), rolesCsv);
        String refreshToken = jwtService.createRefreshToken(member.getId(), jti);


        // 3) CSRF 토큰 생성 (쿠키+헤더 double submit 방식)
        String csrfToken = UUID.randomUUID().toString();

        // Redis에 RT 저장 (userId → RT)
        long rtTtl = jwtService.getExpirationRtMillis();
        revokedTokenRepository.saveRefreshToken(member.getId(), refreshToken, rtTtl);

        // 4) 쿠키 세팅
        ResponseCookie atCookie = ResponseCookie.from("AT", accessToken)
                .httpOnly(true)        // JS 접근 불가 → XSS 방어
                .secure(true)          // HTTPS에서만 전송
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ofMillis(jwtService.getExpirationAtMillis()))
                .build();

        ResponseCookie rtCookie = ResponseCookie.from("RT", refreshToken)
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/") // /auth/refresh 요청에만 자동 전송
                .maxAge(Duration.ofMillis(jwtService.getExpirationRtMillis()))
                .build();

        ResponseCookie csrfCookie = ResponseCookie.from("CSRF-TOKEN", csrfToken)
                .httpOnly(false)       // JS에서 읽을 수 있어야 헤더에 실을 수 있음
                .secure(true)
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ofMinutes(15)) // AT 수명과 유사하게
                .build();

        resp.addHeader(HttpHeaders.SET_COOKIE, atCookie.toString());
        resp.addHeader(HttpHeaders.SET_COOKIE, rtCookie.toString());
        resp.addHeader(HttpHeaders.SET_COOKIE, csrfCookie.toString());

        LoginResponseDto loginResponseDto = new LoginResponseDto(member.getId(), member.getEmail(), rolesCsv);
        return ResponseEntity.ok(ApiResponse.ok(loginResponseDto, "로그인 완료"));
    }

    /**
     * Refresh: RT 쿠키 검증 후 새 AT/RT/CSRF 발급
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@CookieValue(value = "RT", required = false) String rt,
                                     HttpServletResponse resp) throws AuthenticationException {
        if (rt == null) {
            throw new AuthenticationException("RT 쿠키 없음");
        }

        // 1) RT 파싱
        Claims rtClaims;
        try {
            rtClaims = jwtService.parseRefreshToken(rt);
        } catch (JwtException e) {
            throw new AuthenticationException("RT 유효하지 않음");
        }

        UUID userId = UUID.fromString(rtClaims.getSubject());
        String oldJti = rtClaims.getId();

        // 2) 블랙리스트 확인
        if (revokedTokenRepository.isRevoked(oldJti)) {
            throw new AuthenticationException("이미 사용된 RT");
        }

        // 3) Redis 저장된 RT와 비교
        String savedRt = revokedTokenRepository.getRefreshToken(userId);
        if (savedRt == null || !savedRt.equals(rt)) {
            throw new AuthenticationException("저장된 RT와 불일치");
        }

        // 4) 사용자 확인
        Member member = memberRepository.findById(userId)
                .orElseThrow(() -> new AuthenticationException("사용자 없음"));
        String rolesCsv = member.getRole().name();

        // 5) 새 토큰 발급
        String newAT = jwtService.createAccessToken(member.getId(), rolesCsv);
        String newJti = jwtService.newJti();
        String newRT = jwtService.createRefreshToken(member.getId(), newJti);
        String newCSRF = UUID.randomUUID().toString();

        // 6) Redis 업데이트 (이전 jti 블랙리스트 등록 + 새 RT 저장)
        long ttlMillis = rtClaims.getExpiration().getTime() - System.currentTimeMillis();
        revokedTokenRepository.revoke(oldJti, ttlMillis);
        revokedTokenRepository.saveRefreshToken(userId, newRT, jwtService.getExpirationRtMillis());

        // 7) 쿠키 갱신
        ResponseCookie atCookie = ResponseCookie.from("AT", newAT)
                .httpOnly(true).secure(true).sameSite("Lax").path("/")
                .maxAge(Duration.ofMillis(jwtService.getExpirationAtMillis())).build();

        ResponseCookie rtCookie = ResponseCookie.from("RT", newRT)
                .httpOnly(true).secure(false).sameSite("Lax").path("/")
                .maxAge(Duration.ofMillis(jwtService.getExpirationRtMillis())).build();

        ResponseCookie csrfCookie = ResponseCookie.from("CSRF-TOKEN", newCSRF)
                .httpOnly(false).secure(true).sameSite("Lax").path("/")
                .maxAge(Duration.ofMinutes(15)).build();

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
                                    HttpServletResponse resp) {
        if (rt != null) {
            try {
                Claims claims = jwtService.parseRefreshToken(rt);
                UUID userId = UUID.fromString(claims.getSubject());
                String jti = claims.getId();

                long ttlMillis = claims.getExpiration().getTime() - System.currentTimeMillis();
                revokedTokenRepository.revoke(jti, ttlMillis);      // 블랙리스트 등록
                revokedTokenRepository.deleteRefreshToken(userId);  // Redis 저장 RT 삭제
            } catch (JwtException ignore) {}
        }

        ResponseCookie atExpired = ResponseCookie.from("AT", "")
                .httpOnly(true).secure(true).sameSite("Lax").path("/").maxAge(0).build();
        ResponseCookie rtExpired = ResponseCookie.from("RT", "")
                .httpOnly(true).secure(false).sameSite("Lax").path("/").maxAge(0).build();
        ResponseCookie csrfExpired = ResponseCookie.from("CSRF-TOKEN", "")
                .httpOnly(false).secure(true).sameSite("Lax").path("/").maxAge(0).build();


        resp.addHeader(HttpHeaders.SET_COOKIE, atExpired.toString());
        resp.addHeader(HttpHeaders.SET_COOKIE, rtExpired.toString());
        resp.addHeader(HttpHeaders.SET_COOKIE, csrfExpired.toString());

        return ApiResponse.ok(null, "로그아웃 성공");
    }
}