package com.beyond.MKX.domain.financial.repository;

import com.beyond.MKX.domain.financial.entity.FinancialRatios;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.*;

public interface FinancialRatiosRepository extends JpaRepository<FinancialRatios, UUID> {

    Optional<FinancialRatios> findByStockIdAndFiscalYearAndFiscalQuarter(
            UUID stockId, int fiscalYear, Integer fiscalQuarter
    );
    @Query("SELECT r FROM FinancialRatios r JOIN Stock s ON r.stockId = s.id " +
            "WHERE s.ticker = :ticker ORDER BY r.fiscalYear DESC, r.fiscalQuarter DESC")
    List<FinancialRatios> findAllByTickerOrderByDesc(@Param("ticker") String ticker);

    @Query("SELECT r FROM FinancialRatios r WHERE r.stockId = :stockId ORDER BY r.fiscalYear DESC, r.fiscalQuarter DESC")
    List<FinancialRatios> findByStockIdOrderByFiscalYearDescFiscalQuarterDesc(@Param("stockId") UUID stockId);

}
