package com.beyond.MKX.domain.member.service;

import com.beyond.MKX.common.email.EmailService;
import com.beyond.MKX.domain.member.entity.Member;
import com.beyond.MKX.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * EMAIL_VERIFICATION_SERVICE: 이메일 인증 서비스
 * - 인증 토큰 생성 및 저장
 * - 이메일 인증 처리
 * - 인증 이메일 재발송
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final MemberRepository memberRepository;
    private final EmailService emailService;

    /** EMAIL_VERIFICATION: 토큰 유효 기간 (24시간) */
    private static final int TOKEN_EXPIRATION_HOURS = 24;

    /**
     * GENERATE_AND_SEND_VERIFICATION_EMAIL: 인증 토큰 생성 및 이메일 발송
     * @param member 회원 엔티티
     */
    @Transactional
    public void generateAndSendVerificationEmail(Member member) {
        String token = generateAndSaveVerificationToken(member);
        emailService.sendVerificationEmail(member.getEmail(), token, member.getName());
        log.info("이메일 인증 메일 발송: memberId={}, email={}", member.getId(), member.getEmail());
    }

    /**
     * GENERATE_VERIFICATION_TOKEN: 인증 토큰 생성 및 저장
     * @param member 회원 엔티티
     */
    @Transactional
    public String generateAndSaveVerificationToken(Member member) {
        String token = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(TOKEN_EXPIRATION_HOURS);

        member.setEmailVerificationToken(token);
        member.setEmailVerificationTokenExpiresAt(expiresAt);
        memberRepository.save(member);

        log.info("이메일 인증 토큰 생성: memberId={}, token={}", member.getId(), token);
        return token;
    }

    /**
     * VERIFY_EMAIL: 이메일 인증 처리
     * @param token 인증 토큰
     * @return 인증된 회원 엔티티
     */
    @Transactional
    public Member verifyEmail(String token) {
        Member member = memberRepository.findByEmailVerificationToken(token)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 인증 토큰입니다."));

        // 이미 인증된 경우
        if (member.getEmailVerifiedAt() != null) {
            throw new IllegalStateException("이미 인증된 이메일입니다.");
        }

        // 토큰 만료 확인
        if (member.getEmailVerificationTokenExpiresAt() == null ||
            member.getEmailVerificationTokenExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("인증 토큰이 만료되었습니다.");
        }

        // 인증 완료 처리
        member.setEmailVerifiedAt(LocalDateTime.now());
        member.setEmailVerificationToken(null);
        member.setEmailVerificationTokenExpiresAt(null);
        memberRepository.save(member);

        log.info("이메일 인증 완료: memberId={}, email={}", member.getId(), member.getEmail());
        return member;
    }

    /**
     * RESEND_VERIFICATION_EMAIL: 인증 이메일 재발송
     * @param email 회원 이메일
     */
    @Transactional
    public void resendVerificationEmail(String email) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("등록되지 않은 이메일입니다."));

        // 이미 인증된 경우
        if (member.getEmailVerifiedAt() != null) {
            throw new IllegalStateException("이미 인증된 이메일입니다.");
        }

        // 새로운 토큰 생성 및 저장
        String token = generateAndSaveVerificationToken(member);

        // 이메일 발송
        emailService.sendVerificationEmail(member.getEmail(), token, member.getName());

        log.info("인증 이메일 재발송: email={}", email);
    }
}

