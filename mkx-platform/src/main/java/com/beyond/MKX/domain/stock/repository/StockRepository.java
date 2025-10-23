package com.beyond.MKX.domain.stock.repository;

import com.beyond.MKX.domain.stock.entity.Stock;
import com.beyond.MKX.domain.stock.entity.Stock.Status;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

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
}
