package com.beyond.MKX.domain.delisting.repository;

import com.beyond.MKX.domain.delisting.entity.DelistingHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface DelistingHistoryRepository extends JpaRepository<DelistingHistory, UUID> {

    /**
     * 주식별 이력 조회
     */
    @Query("SELECT dh FROM DelistingHistory dh WHERE dh.stockId = :stockId ORDER BY dh.executionDate DESC")
    List<DelistingHistory> findByStockId(@Param("stockId") UUID stockId);

    /**
     * 액션 유형별 이력 조회
     */
    @Query("SELECT dh FROM DelistingHistory dh WHERE dh.actionType = :actionType ORDER BY dh.executionDate DESC")
    List<DelistingHistory> findByActionType(@Param("actionType") com.beyond.MKX.domain.delisting.entity.ActionType actionType);

    /**
     * 실행자별 이력 조회
     */
    @Query("SELECT dh FROM DelistingHistory dh WHERE dh.executedBy = :executedBy ORDER BY dh.executionDate DESC")
    List<DelistingHistory> findByExecutedBy(@Param("executedBy") UUID executedBy);

    /**
     * 특정 기간 내 이력 조회
     */
    @Query("SELECT dh FROM DelistingHistory dh WHERE dh.executionDate BETWEEN :startDate AND :endDate ORDER BY dh.executionDate DESC")
    List<DelistingHistory> findByExecutionDateBetween(@Param("startDate") LocalDateTime startDate, 
                                                      @Param("endDate") LocalDateTime endDate);

    /**
     * 단계 변경 이력 조회
     */
    @Query("SELECT dh FROM DelistingHistory dh WHERE dh.actionType = 'STAGE_CHANGE' ORDER BY dh.executionDate DESC")
    List<DelistingHistory> findStageChanges();

    /**
     * 실패한 보상금 기록 조회 (재처리용)
     */
    @Query("SELECT dh FROM DelistingHistory dh WHERE dh.stockId = :stockId AND dh.actionType = :actionType AND dh.executionResult = :executionResult ORDER BY dh.executionDate DESC")
    List<DelistingHistory> findByStockIdAndActionTypeAndExecutionResult(
            @Param("stockId") UUID stockId, 
            @Param("actionType") com.beyond.MKX.domain.delisting.entity.ActionType actionType,
            @Param("executionResult") com.beyond.MKX.domain.delisting.entity.DelistingHistory.ExecutionResult executionResult);

    /**
     * 액션 타입과 실행 결과로 이력 조회 (스케줄러용)
     */
    @Query("SELECT dh FROM DelistingHistory dh WHERE dh.actionType = :actionType AND dh.executionResult = :executionResult ORDER BY dh.executionDate DESC")
    List<DelistingHistory> findByActionTypeAndExecutionResult(
            @Param("actionType") com.beyond.MKX.domain.delisting.entity.ActionType actionType,
            @Param("executionResult") com.beyond.MKX.domain.delisting.entity.DelistingHistory.ExecutionResult executionResult);
}
