package com.beyond.MKX.domain.member.service;

import com.beyond.MKX.domain.admin.entity.Admin;
import com.beyond.MKX.domain.admin.entity.Role;
import com.beyond.MKX.domain.admin.repository.AdminRepository;
import com.beyond.MKX.domain.member.dto.MemberAdminSummaryDto;
import com.beyond.MKX.domain.member.dto.MemberLoginReqDto;
import com.beyond.MKX.domain.member.dto.MemberResDto;
import com.beyond.MKX.domain.member.dto.MemberSignUpReqDto;
import com.beyond.MKX.domain.member.entity.Member;
import com.beyond.MKX.domain.member.entity.MemberStatus;
import com.beyond.MKX.domain.member.repository.MemberRepository;
import com.beyond.MKX.domain.securities_firm.entity.SecuritiesFirm;
import com.beyond.MKX.domain.securities_firm.repository.SecuritiesFirmRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberService {

    private final MemberRepository memberRepository;
    private final AdminRepository adminRepository;
    private final SecuritiesFirmRepository securitiesFirmRepository;
    private final PasswordEncoder passwordEncoder;


    public MemberResDto signUp(MemberSignUpReqDto dto) {
        SecuritiesFirm brokerage = securitiesFirmRepository.findById(dto.getBrokerageId())
                .orElseThrow(() -> new IllegalArgumentException("증권사 없음"));

        if (brokerage.getStatus() != SecuritiesFirm.Status.ACTIVE) {
            throw new IllegalStateException("승인된 증권사가 아닙니다.");
        }

        memberRepository.findByEmail(dto.getEmail())
                .ifPresent(member -> {
                    throw new IllegalStateException("이미 가입된 이메일");
                });

        memberRepository.findByPhone(dto.getPhone())
                .ifPresent(member -> {
                    throw new IllegalStateException("이미 가입된 전화번호");
                });

        Member member = Member.builder()
                .brokerage(brokerage)
                .name(dto.getName())
                .email(dto.getEmail())
                .password(passwordEncoder.encode(dto.getPassword()))
                .phone(dto.getPhone())
                .status(MemberStatus.ACTIVE)
                .build();

        Member saved = memberRepository.save(member);
        return MemberResDto.from(saved);
    }

    @Transactional(readOnly = true)
    public MemberResDto getProfile(UUID memberId) {
        Member member = findById(memberId);
        return MemberResDto.from(member);
    }

    @Transactional(readOnly = true)
    public List<MemberAdminSummaryDto> getMembersForAdmin(UUID adminId, Role role) {
        if (role == Role.EXCHANGE) {
            return memberRepository.findAll().stream()
                    .map(MemberAdminSummaryDto::from)
                    .toList();
        }

        if (role == Role.BROKERAGE) {
            Admin admin = adminRepository.findById(adminId)
                    .orElseThrow(() -> new IllegalArgumentException("관리자 없음"));
            SecuritiesFirm brokerage = admin.getSecuritiesFirm();
            if (brokerage == null) {
                throw new IllegalStateException("증권사 관리자 정보 없음");
            }
            return memberRepository.findByBrokerage(brokerage).stream()
                    .map(MemberAdminSummaryDto::from)
                    .toList();
        }

        throw new IllegalArgumentException("접근 권한 없음");
    }

    @Transactional(readOnly = true)
    public MemberResDto getMemberDetailForAdmin(UUID adminId, Role role, UUID memberId) {
        if (role == Role.EXCHANGE) {
            Member member = memberRepository.findById(memberId)
                    .orElseThrow(() -> new IllegalArgumentException("회원 없음"));
            return MemberResDto.from(member);
        }

        if (role == Role.BROKERAGE) {
            Admin admin = adminRepository.findById(adminId)
                    .orElseThrow(() -> new IllegalArgumentException("관리자 없음"));
            SecuritiesFirm brokerage = admin.getSecuritiesFirm();
            if (brokerage == null) {
                throw new IllegalStateException("증권사 관리자 정보 없음");
            }
            Member member = memberRepository.findByIdAndBrokerage(memberId, brokerage)
                    .orElseThrow(() -> new IllegalArgumentException("회원 없음"));
            return MemberResDto.from(member);
        }

        throw new IllegalArgumentException("접근 권한 없음");
    }

    public Member authenticate(MemberLoginReqDto dto) {
        Member member = memberRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("이메일 없음"));

        if (!passwordEncoder.matches(dto.getPassword(), member.getPassword())) {
            throw new BadCredentialsException("비밀번호 불일치");
        }

        return member;
    }

    public Member findById(UUID memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new BadCredentialsException("이메일 없음"));
    }
}
