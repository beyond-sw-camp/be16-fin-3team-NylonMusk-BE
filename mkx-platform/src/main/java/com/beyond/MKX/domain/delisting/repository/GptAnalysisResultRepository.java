package com.beyond.MKX.domain.delisting.repository;

import com.beyond.MKX.domain.delisting.entity.GptAnalysisResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface GptAnalysisResultRepository extends JpaRepository<GptAnalysisResult, UUID> {

    /**
     * 주식별 GPT 분석 결과 조회 (최신순)
     */
    List<GptAnalysisResult> findByStockIdOrderByAnalysisDateDesc(UUID stockId);

    /**
     * 주식별 특정 분석 유형의 최신 결과 조회
     */
    @Query("SELECT gar FROM GptAnalysisResult gar WHERE gar.stockId = :stockId AND gar.analysisType = :analysisType ORDER BY gar.analysisDate DESC")
    List<GptAnalysisResult> findByStockIdAndAnalysisTypeOrderByAnalysisDateDesc(
            @Param("stockId") UUID stockId, 
            @Param("analysisType") String analysisType);

    /**
     * 주식별 최근 N일간의 분석 결과 조회
     */
    @Query("SELECT gar FROM GptAnalysisResult gar WHERE gar.stockId = :stockId AND gar.analysisDate >= :sinceDate ORDER BY gar.analysisDate DESC")
    List<GptAnalysisResult> findByStockIdAndAnalysisDateAfterOrderByAnalysisDateDesc(
            @Param("stockId") UUID stockId, 
            @Param("sinceDate") LocalDateTime sinceDate);

    /**
     * 특정 기준 코드별 분석 결과 조회
     */
    List<GptAnalysisResult> findByCriteriaCodeOrderByAnalysisDateDesc(String criteriaCode);

    /**
     * 성공한 분석 결과만 조회
     */
    List<GptAnalysisResult> findByStockIdAndIsSuccessfulTrueOrderByAnalysisDateDesc(UUID stockId);

    /**
     * 주식별 분석 결과 개수 조회
     */
    long countByStockId(UUID stockId);

    /**
     * 주식별 최근 분석 결과 조회 (1개)
     */
    @Query("SELECT gar FROM GptAnalysisResult gar WHERE gar.stockId = :stockId ORDER BY gar.analysisDate DESC LIMIT 1")
    GptAnalysisResult findLatestByStockId(@Param("stockId") UUID stockId);
}
