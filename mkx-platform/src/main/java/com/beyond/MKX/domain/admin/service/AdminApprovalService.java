package com.beyond.MKX.domain.admin.service;

import com.beyond.MKX.domain.admin.dto.*;
import com.beyond.MKX.domain.admin.entity.Admin;
import com.beyond.MKX.domain.admin.entity.Status;
import com.beyond.MKX.domain.admin.repository.AdminRepository;
import com.beyond.MKX.domain.corporation.entity.Corporation;
import com.beyond.MKX.domain.corporation.repository.CorporationRepository;
import com.beyond.MKX.domain.securities_firm.entity.SecuritiesFirm;
import com.beyond.MKX.domain.securities_firm.repository.SecuritiesFirmRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminApprovalService {

    private final CorporationRepository corporationRepository;
    private final SecuritiesFirmRepository securitiesFirmRepository;
    private final AdminRepository adminRepository;

    @Transactional(readOnly = true)
    // EXCHANGE 목록용 - PENDING 상태의 기업 신청 요약 목록 반환
    public List<CorporationSignUpSummaryDto> getPendingCorporationSummaries() {
        return corporationRepository.findAllByStatus(com.beyond.MKX.domain.corporation.entity.Status.PENDING)
                .stream()
                .map(this::toCorporationSummary)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    // EXCHANGE 목록용 - PENDING 상태의 증권사 신청 요약 목록 반환
    public List<SecuritiesFirmSignUpSummaryDto> getPendingSecuritiesFirmSummaries() {
        return securitiesFirmRepository.findAllByStatus(SecuritiesFirm.Status.PENDING)
                .stream()
                .map(this::toSecuritiesFirmSummary)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    // EXCHANGE 상세 조회 - 기업 신청 + 대표 관리자 정보 포함 반환
    public CorporationSignUpApprovalDetailDto getCorporationDetail(UUID corporationId) {
        Corporation corporation = corporationRepository.findById(corporationId)
                .orElseThrow(() -> new EntityNotFoundException("기업 신청을 찾을 수 없습니다."));

        Admin admin = adminRepository.findByCorporation(corporation)
                .orElseThrow(() -> new EntityNotFoundException("기업 관리자 정보를 찾을 수 없습니다."));

        return CorporationSignUpApprovalDetailDto.from(corporation, AdminSummaryDto.from(admin));
    }

    @Transactional(readOnly = true)
    // EXCHANGE 상세 조회 - 증권사 신청 + 대표 관리자 정보 포함 반환
    public SecuritiesFirmSignUpApprovalDetailDto getSecuritiesFirmDetail(UUID securitiesFirmId) {
        SecuritiesFirm firm = securitiesFirmRepository.findById(securitiesFirmId)
                .orElseThrow(() -> new EntityNotFoundException("증권사 신청을 찾을 수 없습니다."));

        Admin admin = adminRepository.findBySecuritiesFirm(firm)
                .orElseThrow(() -> new EntityNotFoundException("증권사 관리자 정보를 찾을 수 없습니다."));

        return SecuritiesFirmSignUpApprovalDetailDto.from(firm, AdminSummaryDto.from(admin));
    }

    @Transactional(readOnly = true)
    // 대표 관리자 전용 - 자신의 신청 현황(기업 혹은 증권사)을 상세 DTO로 반환
    public MySignUpStatusDto getMySignUpStatus(UUID adminId) {
        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new EntityNotFoundException("관리자를 찾을 수 없습니다."));

        if (admin.getCorporation() != null) {
            Corporation corporation = admin.getCorporation();
            return MySignUpStatusDto.ofCorporation(
                    CorporationSignUpApprovalDetailDto.from(corporation, AdminSummaryDto.from(admin))
            );
        }

        if (admin.getSecuritiesFirm() != null) {
            SecuritiesFirm firm = admin.getSecuritiesFirm();
            return MySignUpStatusDto.ofSecuritiesFirm(
                    SecuritiesFirmSignUpApprovalDetailDto.from(firm, AdminSummaryDto.from(admin))
            );
        }

        throw new IllegalStateException("가입 신청 정보가 존재하지 않습니다.");
    }

    // 기업 신청 승인 처리: 기업/대표 관리자 상태를 ACTIVE 로 전환
    public void approveCorporation(UUID corporationId) {
        Corporation corporation = corporationRepository.findById(corporationId)
                .orElseThrow(() -> new EntityNotFoundException("기업 신청을 찾을 수 없습니다."));

        if (corporation.getStatus() != com.beyond.MKX.domain.corporation.entity.Status.PENDING) {
            throw new IllegalStateException("이미 처리된 기업 신청입니다.");
        }

        Admin admin = adminRepository.findByCorporation(corporation)
                .orElseThrow(() -> new EntityNotFoundException("기업 관리자 정보를 찾을 수 없습니다."));

        corporation.approve();
        admin.changeStatus(Status.ACTIVE);
    }

    // 기업 신청 거절 처리: 거절 사유 저장 후 기업/대표 관리자 상태를 REJECTED 로 전환
    public void rejectCorporation(UUID corporationId, String reason) {
        Corporation corporation = corporationRepository.findById(corporationId)
                .orElseThrow(() -> new EntityNotFoundException("기업 신청을 찾을 수 없습니다."));

        if (corporation.getStatus() != com.beyond.MKX.domain.corporation.entity.Status.PENDING) {
            throw new IllegalStateException("이미 처리된 기업 신청입니다.");
        }

        Admin admin = adminRepository.findByCorporation(corporation)
                .orElseThrow(() -> new EntityNotFoundException("기업 관리자 정보를 찾을 수 없습니다."));

        corporation.reject(reason);
        admin.changeStatus(Status.REJECTED);
    }

    // 증권사 신청 승인 처리: 증권사/대표 관리자 상태를 ACTIVE 로 전환
    public void approveSecuritiesFirm(UUID securitiesFirmId) {
        SecuritiesFirm firm = securitiesFirmRepository.findById(securitiesFirmId)
                .orElseThrow(() -> new EntityNotFoundException("증권사 신청을 찾을 수 없습니다."));

        if (firm.getStatus() != SecuritiesFirm.Status.PENDING) {
            throw new IllegalStateException("이미 처리된 증권사 신청입니다.");
        }

        Admin admin = adminRepository.findBySecuritiesFirm(firm)
                .orElseThrow(() -> new EntityNotFoundException("증권사 관리자 정보를 찾을 수 없습니다."));

        firm.approve();
        admin.changeStatus(Status.ACTIVE);
    }

    // 증권사 신청 거절 처리: 거절 사유 저장 후 증권사/대표 관리자 상태를 REJECTED 로 전환
    public void rejectSecuritiesFirm(UUID securitiesFirmId, String reason) {
        SecuritiesFirm firm = securitiesFirmRepository.findById(securitiesFirmId)
                .orElseThrow(() -> new EntityNotFoundException("증권사 신청을 찾을 수 없습니다."));

        if (firm.getStatus() != SecuritiesFirm.Status.PENDING) {
            throw new IllegalStateException("이미 처리된 증권사 신청입니다.");
        }

        Admin admin = adminRepository.findBySecuritiesFirm(firm)
                .orElseThrow(() -> new EntityNotFoundException("증권사 관리자 정보를 찾을 수 없습니다."));

        firm.reject(reason);
        admin.changeStatus(Status.REJECTED);
    }

    // 기업 신청 엔티티를 목록 응답용 요약 DTO로 변환
    private CorporationSignUpSummaryDto toCorporationSummary(Corporation corporation) {
        Admin admin = adminRepository.findByCorporation(corporation)
                .orElseThrow(() -> new EntityNotFoundException("기업 관리자 정보를 찾을 수 없습니다."));

        return CorporationSignUpSummaryDto.builder()
                .corporationId(corporation.getId())
                .nameKo(corporation.getNameKo())
                .ownerName(corporation.getOwnerName())
                .regNo(corporation.getRegNo())
                .createdAt(corporation.getCreatedAt())
                .adminName(admin.getName())
                .adminEmail(admin.getEmail())
                .build();
    }

    // 증권사 신청 엔티티를 목록 응답용 요약 DTO로 변환
    private SecuritiesFirmSignUpSummaryDto toSecuritiesFirmSummary(SecuritiesFirm firm) {
        Admin admin = adminRepository.findBySecuritiesFirm(firm)
                .orElseThrow(() -> new EntityNotFoundException("증권사 관리자 정보를 찾을 수 없습니다."));

        return SecuritiesFirmSignUpSummaryDto.builder()
                .securitiesFirmId(firm.getId())
                .nameKo(firm.getNameKo())
                .ownerName(firm.getOwnerName())
                .regNo(firm.getRegNo())
                .createdAt(firm.getCreatedAt())
                .adminName(admin.getName())
                .adminEmail(admin.getEmail())
                .build();
    }
}
