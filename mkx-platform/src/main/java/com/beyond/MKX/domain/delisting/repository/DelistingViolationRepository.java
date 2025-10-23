package com.beyond.MKX.domain.delisting.repository;

import com.beyond.MKX.domain.delisting.entity.DelistingViolation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface DelistingViolationRepository extends JpaRepository<DelistingViolation, UUID> {

    /**
     * 주식별 위반 목록 조회 (해결되지 않은 것만)
     */
    @Query("SELECT dv FROM DelistingViolation dv WHERE dv.stockId = :stockId AND dv.isResolved = false ORDER BY dv.violationDate DESC")
    List<DelistingViolation> findByStockIdAndUnresolved(@Param("stockId") UUID stockId);

    /**
     * 주식별 모든 위반 목록 조회
     */
    @Query("SELECT dv FROM DelistingViolation dv WHERE dv.stockId = :stockId ORDER BY dv.violationDate DESC")
    List<DelistingViolation> findByStockId(@Param("stockId") UUID stockId);

    /**
     * 기준별 위반 목록 조회
     */
    @Query("SELECT dv FROM DelistingViolation dv WHERE dv.criteriaId = :criteriaId ORDER BY dv.violationDate DESC")
    List<DelistingViolation> findByCriteriaId(@Param("criteriaId") UUID criteriaId);

    /**
     * 위반 유형별 조회
     */
    @Query("SELECT dv FROM DelistingViolation dv WHERE dv.violationType = :violationType ORDER BY dv.violationDate DESC")
    List<DelistingViolation> findByViolationType(@Param("violationType") com.beyond.MKX.domain.delisting.entity.ViolationType violationType);

    /**
     * 해결되지 않은 위반 목록 조회
     */
    @Query("SELECT dv FROM DelistingViolation dv WHERE dv.isResolved = false ORDER BY dv.violationDate DESC")
    List<DelistingViolation> findUnresolvedViolations();

    /**
     * 특정 기간 내 위반 목록 조회
     */
    @Query("SELECT dv FROM DelistingViolation dv WHERE dv.violationDate BETWEEN :startDate AND :endDate ORDER BY dv.violationDate DESC")
    List<DelistingViolation> findByViolationDateBetween(@Param("startDate") LocalDateTime startDate, 
                                                        @Param("endDate") LocalDateTime endDate);

    /**
     * 주식별 연속 위반 기간 조회
     */
    @Query("SELECT dv FROM DelistingViolation dv WHERE dv.stockId = :stockId AND dv.criteriaId = :criteriaId AND dv.isResolved = false ORDER BY dv.violationDate DESC")
    List<DelistingViolation> findConsecutiveViolations(@Param("stockId") UUID stockId, 
                                                      @Param("criteriaId") UUID criteriaId);

    /**
     * 주식별 해결되지 않은 위반 목록 조회 (자동화용)
     */
    @Query("SELECT dv FROM DelistingViolation dv WHERE dv.stockId = :stockId AND dv.isResolved = false ORDER BY dv.violationDate DESC")
    List<DelistingViolation> findByStockIdAndIsResolvedFalse(@Param("stockId") UUID stockId);

    /**
     * 주식별 해결되지 않은 위반 건수 조회
     */
    @Query("SELECT COUNT(dv) FROM DelistingViolation dv WHERE dv.stockId = :stockId AND dv.isResolved = false")
    int countByStockIdAndIsResolvedFalse(@Param("stockId") UUID stockId);
}
