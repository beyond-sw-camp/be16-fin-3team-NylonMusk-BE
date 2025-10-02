package com.beyond.MKX.domain.securities_firm.repository;

import com.beyond.MKX.domain.securities_firm.entity.SecuritiesFirm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SecuritiesFirmRepository extends JpaRepository<SecuritiesFirm, UUID> {
    // 사업자등록번호 중복 여부 검증
    boolean existsByRegNo(String regNo);

    // 금융투자업 인가번호 중복 여부 검증
    boolean existsByFinancialInvestmentLicenseNo(String financialInvestmentLicenseNo);

    // 상태별 증권사 목록 조회
    List<SecuritiesFirm> findAllByStatus(SecuritiesFirm.Status status);
}
