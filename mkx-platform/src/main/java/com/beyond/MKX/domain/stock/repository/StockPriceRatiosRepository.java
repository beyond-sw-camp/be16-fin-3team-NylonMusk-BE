package com.beyond.MKX.domain.stock.repository;

import com.beyond.MKX.domain.stock.entity.StockPriceRatios;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface StockPriceRatiosRepository extends JpaRepository<StockPriceRatios, UUID> {
    
    /**
     * Stock ID로 현재가 기반 비율 조회
     */
    Optional<StockPriceRatios> findByStockId(UUID stockId);
    
    /**
     * Ticker로 현재가 기반 비율 조회
     */
    @Query("SELECT spr FROM StockPriceRatios spr JOIN Stock s ON spr.stockId = s.id WHERE s.ticker = :ticker")
    Optional<StockPriceRatios> findByTicker(@Param("ticker") String ticker);
}

