package com.beyond.MKX.domain.stock.repository;

import com.beyond.MKX.domain.delisting.entity.DelistingStage;
import com.beyond.MKX.domain.stock.entity.Stock;
import com.beyond.MKX.domain.stock.entity.Stock.Status;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StockRepository extends JpaRepository<Stock, UUID> {

    @Query("""
            select s
            from Stock s
            where (:status is null or s.status = :status)
              and (
                   :q is null
                or lower(s.ticker)  like lower(concat('%', :q, '%'))
                or lower(s.nameKo)  like lower(concat('%', :q, '%'))
              )
            """)
    Page<Stock> search(
            @Param("status") Status status,
            @Param("q") String q,
            Pageable pageable
    );

    boolean existsByTicker(String ticker);

    Optional<Stock> findByTicker(String ticker);

    Optional<Stock> findByCorporationIdAndTicker(UUID corporationId, String ticker);

    @Query("""
            select s.ticker
            from Stock s
            where s.corporationId = :corpId
              and s.status = :status
            """)
    Optional<String> findListedTickerByCorporationId(
            @Param("corpId") UUID corporationId,
            @Param("status") Stock.Status status);

    /**
     * 특정 상태의 주식 목록 조회
     */
    List<Stock> findByStatus(Status status);

    /**
     * 여러 상태의 주식 목록 조회 (자동화용)
     */
    List<Stock> findByStatusIn(List<Status> statuses);

    /**
     * 특정 상태를 제외한 주식 목록 조회
     */
    @Query("""
            select s
            from Stock s
            where (:excludeStatus is null or s.status != :excludeStatus)
              and (
                   :q is null
                or lower(s.ticker)  like lower(concat('%', :q, '%'))
                or lower(s.nameKo)  like lower(concat('%', :q, '%'))
              )
            """)
    Page<Stock> searchExcludingStatus(
            @Param("excludeStatus") Status excludeStatus,
            @Param("q") String q,
            Pageable pageable
    );

    // 거래정지 해제 스케줄러용 (TradingLockService에서 사용)
    List<Stock> findAllByStatus(Stock.Status status);


    interface BriefView {
        UUID getId();
        String getTicker();
        String getNameKo();
        Stock.Status getStatus();
        // DelistingStage 타입 패키지 경로에 맞춰 import 또는 Stock과 같은 패키지면 그대로
        DelistingStage getDelistingStage();
    }

    List<BriefView> findByTickerIn(Collection<String> tickers);
}
