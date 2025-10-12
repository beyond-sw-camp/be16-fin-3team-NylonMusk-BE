package com.beyond.MKX.domain.stock.service;

import com.beyond.MKX.domain.stock.dto.StockListResDto;
import com.beyond.MKX.domain.stock.entity.Stock;
import com.beyond.MKX.domain.stock.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockQueryService {

    private final StockRepository stockRepository;

    public Page<StockListResDto> getStocks(String q, String status, Pageable pageable) {
        Stock.Status statusEnum = null;
        if (status != null && !status.isBlank()) {
            statusEnum = Stock.Status.valueOf(status.toUpperCase());
        }

        Page<Stock> page = stockRepository.search(statusEnum, emptyToNull(q), pageable);
        return page.map(StockListResDto::from);
    }

    private String emptyToNull(String v) {
        return (v == null || v.isBlank()) ? null : v;
    }
}
