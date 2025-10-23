package com.beyond.MKX.domain.delisting.repository;

import com.beyond.MKX.domain.delisting.entity.QuarterlySubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface QuarterlySubmissionRepository extends JpaRepository<QuarterlySubmission, UUID> {

    /**
     * 주식별 제출 목록 조회
     */
    @Query("SELECT qs FROM QuarterlySubmission qs WHERE qs.stockId = :stockId ORDER BY qs.fiscalYear DESC, qs.fiscalQuarter DESC")
    List<QuarterlySubmission> findByStockId(@Param("stockId") UUID stockId);

    /**
     * 지연된 제출 목록 조회
     */
    @Query("SELECT qs FROM QuarterlySubmission qs WHERE qs.submissionStatus = 'OVERDUE' ORDER BY qs.deadlineDate ASC")
    List<QuarterlySubmission> findOverdueSubmissions();

    /**
     * 특정 연도/분기 제출 조회
     */
    @Query("SELECT qs FROM QuarterlySubmission qs WHERE qs.stockId = :stockId AND qs.fiscalYear = :year AND qs.fiscalQuarter = :quarter")
    List<QuarterlySubmission> findByStockIdAndFiscalPeriod(@Param("stockId") UUID stockId, 
                                                          @Param("year") int fiscalYear, 
                                                          @Param("quarter") Integer fiscalQuarter);

    /**
     * 마감일 임박 제출 목록 조회 (3일 이내)
     */
    @Query("SELECT qs FROM QuarterlySubmission qs WHERE qs.deadlineDate BETWEEN :now AND :threeDaysLater AND qs.submissionStatus = 'PENDING'")
    List<QuarterlySubmission> findUpcomingDeadlines(@Param("now") LocalDateTime now, 
                                                   @Param("threeDaysLater") LocalDateTime threeDaysLater);
}
