package com.beyond.MKX.domain.securities_firm.repository;

import com.beyond.MKX.domain.securities_firm.entity.SecuritiesFirm;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SecuritiesFirmRepository extends JpaRepository<SecuritiesFirm, UUID> {
    // 사업자등록번호 중복 여부 검증
    boolean existsByRegNo(String regNo);

    // 금융투자업 인가번호 중복 여부 검증
    boolean existsByFinancialInvestmentLicenseNo(String financialInvestmentLicenseNo);
}
