package com.beyond.MKX.domain.financial.repository;

import com.beyond.MKX.domain.financial.entity.CompanyFinancials;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.*;

public interface CompanyFinancialsRepository extends JpaRepository<CompanyFinancials, UUID> {

    Optional<CompanyFinancials> findByStockIdAndFiscalYearAndFiscalQuarter(
            UUID stockId, int fiscalYear, Integer fiscalQuarter
    );
    @Query("SELECT f FROM CompanyFinancials f JOIN Stock s ON f.stockId = s.id " +
            "WHERE s.ticker = :ticker ORDER BY f.fiscalYear DESC, f.fiscalQuarter DESC")
    List<CompanyFinancials> findAllByTickerOrderByDesc(@Param("ticker") String ticker);

}
