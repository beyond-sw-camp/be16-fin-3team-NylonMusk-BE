package com.beyond.MKX.domain.financial.repository;

import com.beyond.MKX.domain.financial.entity.CashFlowStatement;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.*;

public interface CashFlowStatementRepository extends JpaRepository<CashFlowStatement, UUID> {

    Optional<CashFlowStatement> findByStockIdAndFiscalYearAndFiscalQuarter(
            UUID stockId, int fiscalYear, Integer fiscalQuarter
    );
    @Query("SELECT c FROM CashFlowStatement c JOIN Stock s ON c.stockId = s.id " +
            "WHERE s.ticker = :ticker ORDER BY c.fiscalYear DESC, c.fiscalQuarter DESC")
    List<CashFlowStatement> findAllByTickerOrderByDesc(@Param("ticker") String ticker);

}
