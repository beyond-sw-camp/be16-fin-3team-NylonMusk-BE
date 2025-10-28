package com.beyond.MKX.domain.delisting.repository;

import com.beyond.MKX.domain.delisting.entity.DelistingCompensation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface DelistingCompensationRepository extends JpaRepository<DelistingCompensation, UUID> {

    /**
     * 주식별 보상 목록 조회 (삭제되지 않은 것만)
     */
    @Query("SELECT dc FROM DelistingCompensation dc WHERE dc.stockId = :stockId AND dc.deletedAt IS NULL ORDER BY dc.requestedAt DESC")
    List<DelistingCompensation> findByStockId(@Param("stockId") UUID stockId);
    
    /**
     * 주식별 모든 보상 목록 조회 (삭제된 것 포함)
     */
    @Query("SELECT dc FROM DelistingCompensation dc WHERE dc.stockId = :stockId ORDER BY dc.requestedAt DESC")
    List<DelistingCompensation> findAllByStockId(@Param("stockId") UUID stockId);

    /**
     * 회원별 보상 목록 조회
     */
    @Query("SELECT dc FROM DelistingCompensation dc WHERE dc.memberAccountId = :memberAccountId ORDER BY dc.requestedAt DESC")
    List<DelistingCompensation> findByMemberAccountId(@Param("memberAccountId") UUID memberAccountId);

    /**
     * 상태별 보상 목록 조회
     */
    @Query("SELECT dc FROM DelistingCompensation dc WHERE dc.status = :status ORDER BY dc.requestedAt DESC")
    List<DelistingCompensation> findByStatus(@Param("status") com.beyond.MKX.domain.delisting.entity.CompensationStatus status);

    /**
     * 대기 중인 보상 목록 조회
     */
    @Query("SELECT dc FROM DelistingCompensation dc WHERE dc.status = 'PENDING' ORDER BY dc.requestedAt ASC")
    List<DelistingCompensation> findPendingCompensations();
}
