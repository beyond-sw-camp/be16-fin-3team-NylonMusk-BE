package com.beyond.MKX.domain.member.service;

import com.beyond.MKX.domain.admin.entity.Admin;
import com.beyond.MKX.domain.admin.entity.Role;
import com.beyond.MKX.domain.admin.repository.AdminRepository;
import com.beyond.MKX.domain.member.client.MemberAccountInternalClient;
import com.beyond.MKX.domain.member.dto.MemberAdminSummaryDto;
import com.beyond.MKX.domain.member.dto.MemberResDto;
import com.beyond.MKX.domain.member.entity.Member;
import com.beyond.MKX.domain.member.entity.MemberStatus;
import com.beyond.MKX.domain.member.repository.MemberRepository;
import com.beyond.MKX.domain.securities_firm.entity.SecuritiesFirm;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberAdminQueryService {

    private final MemberRepository memberRepository;
    private final AdminRepository adminRepository;
    private final MemberAccountInternalClient memberAccountInternalClient;

    public Page<MemberAdminSummaryDto> list(UUID adminId, Role role, String q, String status, Pageable pageable) {
        MemberStatus statusEnum = null;
        if (status != null && !status.isBlank()) {
            statusEnum = MemberStatus.valueOf(status.toUpperCase());
        }
        String qNorm = (q == null || q.isBlank()) ? null : q;

        UUID brokerageIdFilter = null;
        if (role == Role.BROKERAGE) {
            Admin admin = adminRepository.findById(adminId)
                    .orElseThrow(() -> new NoSuchElementException("관리자를 찾을 수 없습니다."));
            SecuritiesFirm brokerage = admin.getSecuritiesFirm();
            if (brokerage == null) {
                throw new IllegalStateException("증권사 관리자 정보가 없습니다.");
            }
            brokerageIdFilter = brokerage.getId();
        }

        return memberRepository.searchAdmin(brokerageIdFilter, statusEnum, qNorm, pageable)
                .map(MemberAdminSummaryDto::from);
    }

    public MemberResDto detail(UUID adminId, Role role, UUID memberId) {
        Member target;
        if (role == Role.EXCHANGE) {
            target = memberRepository.findById(memberId)
                    .orElseThrow(() -> new NoSuchElementException("회원을 찾을 수 없습니다."));
        } else if (role == Role.BROKERAGE) {
            Admin admin = adminRepository.findById(adminId)
                    .orElseThrow(() -> new NoSuchElementException("관리자를 찾을 수 없습니다."));
            SecuritiesFirm brokerage = Optional.ofNullable(admin.getSecuritiesFirm())
                    .orElseThrow(() -> new IllegalStateException("증권사 관리자 정보가 없습니다."));
            target = memberRepository.findByIdAndBrokerage(memberId, brokerage)
                    .orElseThrow(() -> new NoSuchElementException("회원을 찾을 수 없습니다."));
        } else {
            throw new IllegalArgumentException("접근 권한이 없습니다.");
        }

        MemberResDto dto = MemberResDto.from(target);
        try {
            MemberAccountInternalClient.MemberAccountDetailRes acc = memberAccountInternalClient.getDetailByMember(target.getId());
            if (acc != null) {
                dto.setAccountId(acc.accountId());
                dto.setAccountBrokerageId(acc.brokerageId());
                dto.setAccountNumber(acc.accountNumber());
                dto.setAccountStatus(acc.status());
                dto.setAccountBalance(acc.balance());
                dto.setAccountAvailable(acc.availableBalance());
                dto.setAccountCreatedAt(acc.createdAt());
            }
        } catch (Exception ignore) {
            // 내부 계좌 조회 실패 시 계좌 필드는 null 유지
        }
        return dto;
    }
}
