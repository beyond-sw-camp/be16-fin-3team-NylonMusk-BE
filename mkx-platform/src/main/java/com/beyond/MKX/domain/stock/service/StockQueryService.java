package com.beyond.MKX.domain.stock.service;

import com.beyond.MKX.domain.stock.dto.StockListResDto;
import com.beyond.MKX.domain.stock.dto.StockInfoResDTO;
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
        Stock.Status excludeStatus = null;
        
        if (status != null && !status.isBlank()) {
            // DELISTED 제외 요청 처리 (!DELISTED)
            if (status.equalsIgnoreCase("!DELISTED")) {
                excludeStatus = Stock.Status.DELISTED;
            } else {
                statusEnum = Stock.Status.valueOf(status.toUpperCase());
            }
        }

        Page<Stock> page;
        if (excludeStatus != null) {
            page = stockRepository.searchExcludingStatus(excludeStatus, emptyToNull(q), pageable);
        } else {
            page = stockRepository.search(statusEnum, emptyToNull(q), pageable);
        }
        
        return page.map(StockListResDto::from);
    }

    public StockInfoResDTO getStockByTicker(String ticker) {
        Stock stock = stockRepository.findByTicker(ticker)
                .orElseThrow(() -> new IllegalArgumentException("종목을 찾을 수 없습니다: " + ticker));
        return StockInfoResDTO.builder()
                .ticker(stock.getTicker())
                .nameKo(stock.getNameKo())
                .build();
    }

    private String emptyToNull(String v) {
        return (v == null || v.isBlank()) ? null : v;
    }
}
