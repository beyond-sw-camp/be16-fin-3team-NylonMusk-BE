package com.beyond.MKX.domain.stock.repository;

import com.beyond.MKX.domain.stock.entity.Stock;
import com.beyond.MKX.domain.stock.entity.Stock.Status;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

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
}
