package com.beyond.MKX.domain.assets.repository;

import com.beyond.MKX.domain.assets.entity.StockHolding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StockHoldingRepository extends JpaRepository<StockHolding, UUID> {

    Optional<StockHolding> findByMemberAccountIdAndTicker(UUID memberAccountId, String ticker);

    List<StockHolding> findAllByMemberAccountId(UUID memberAccountId);

    /**
     * 특정 ticker의 모든 보유자 조회 (내부 API용)
     */
    @Query("SELECT sh FROM StockHolding sh WHERE sh.ticker = :ticker AND sh.totalQuantity > 0")
    List<StockHolding> findAllByTicker(@Param("ticker") String ticker);
}