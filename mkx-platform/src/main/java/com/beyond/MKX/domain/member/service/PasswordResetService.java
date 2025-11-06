package com.beyond.MKX.domain.member.service;

import com.beyond.MKX.common.email.EmailService;
import com.beyond.MKX.domain.member.entity.Member;
import com.beyond.MKX.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * PASSWORD_RESET_SERVICE: 비밀번호 재설정 서비스
 * - 비밀번호 재설정 토큰 생성 및 저장
 * - 비밀번호 재설정 처리
 * - 재설정 이메일 발송
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final MemberRepository memberRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    /** PASSWORD_RESET: 토큰 유효 기간 (1시간) */
    private static final int TOKEN_EXPIRATION_HOURS = 1;

    /**
     * REQUEST_PASSWORD_RESET: 비밀번호 재설정 요청 처리
     * @param email 회원 이메일
     */
    @Transactional
    public void requestPasswordReset(String email) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("등록되지 않은 이메일입니다."));

        // 재설정 토큰 생성 및 저장
        String token = generateAndSaveResetToken(member);

        // 재설정 이메일 발송
        emailService.sendPasswordResetEmail(member.getEmail(), token, member.getName());

        log.info("비밀번호 재설정 이메일 발송: memberId={}, email={}", member.getId(), email);
    }

    /**
     * GENERATE_AND_SAVE_RESET_TOKEN: 비밀번호 재설정 토큰 생성 및 저장
     * @param member 회원 엔티티
     * @return 생성된 토큰
     */
    @Transactional
    public String generateAndSaveResetToken(Member member) {
        String token = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(TOKEN_EXPIRATION_HOURS);

        member.setPasswordResetToken(token);
        member.setPasswordResetTokenExpiresAt(expiresAt);
        memberRepository.save(member);

        log.info("비밀번호 재설정 토큰 생성: memberId={}, token={}", member.getId(), token);
        return token;
    }

    /**
     * RESET_PASSWORD: 비밀번호 재설정 처리
     * @param token 재설정 토큰
     * @param newPassword 새 비밀번호
     */
    @Transactional
    public void resetPassword(String token, String newPassword) {
        Member member = memberRepository.findByPasswordResetToken(token)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 재설정 토큰입니다."));

        // 토큰 만료 확인
        if (member.getPasswordResetTokenExpiresAt() == null ||
            member.getPasswordResetTokenExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("재설정 토큰이 만료되었습니다.");
        }

        // 새 비밀번호로 변경
        member.setPassword(passwordEncoder.encode(newPassword));
        member.setPasswordResetToken(null);
        member.setPasswordResetTokenExpiresAt(null);
        memberRepository.save(member);

        log.info("비밀번호 재설정 완료: memberId={}, email={}", member.getId(), member.getEmail());
    }
}

