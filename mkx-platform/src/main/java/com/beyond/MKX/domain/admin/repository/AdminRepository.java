package com.beyond.MKX.domain.admin.repository;

import com.beyond.MKX.domain.admin.entity.Admin;
import com.beyond.MKX.domain.corporation.entity.Corporation;
import com.beyond.MKX.domain.securities_firm.entity.SecuritiesFirm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AdminRepository extends JpaRepository<Admin, UUID> {
    // 이메일 기반 관리자 조회
    Optional<Admin> findByEmail (String email);

    // 동일 이메일 등록 여부 확인 (대소문자 무시)
    boolean existsByEmailIgnoreCase(String email);

    // 전화번호 중복 여부 확인
    boolean existsByPhone(String phone);

    // 기업 소속 관리자 조회
    Optional<Admin> findByCorporation(Corporation corporation);

    // 증권사 소속 관리자 조회
    Optional<Admin> findBySecuritiesFirm(SecuritiesFirm securitiesFirm);

}
