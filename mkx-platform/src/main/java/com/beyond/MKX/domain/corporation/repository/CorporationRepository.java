package com.beyond.MKX.domain.corporation.repository;

import com.beyond.MKX.domain.corporation.entity.Corporation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CorporationRepository extends JpaRepository<Corporation, UUID> {
    // 사업자등록번호 중복 여부 검증
    boolean existsByRegNo(String regNo);
}
