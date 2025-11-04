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

    @Query("SELECT f FROM CompanyFinancials f WHERE f.stockId = :stockId ORDER BY f.fiscalYear DESC, f.fiscalQuarter DESC")
    List<CompanyFinancials> findByStockIdOrderByFiscalYearDescFiscalQuarterDesc(@Param("stockId") UUID stockId);

    // Spring Data JPA 메서드 이름 규칙 사용: findFirst는 자동으로 LIMIT 1 적용
    Optional<CompanyFinancials> findFirstByStockIdOrderByFiscalYearDescFiscalQuarterDesc(UUID stockId);

}
