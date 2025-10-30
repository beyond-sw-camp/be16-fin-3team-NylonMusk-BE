package com.beyond.MKX.domain.stock.service;

import com.beyond.MKX.domain.corporation.entity.Corporation;
import com.beyond.MKX.domain.corporation.repository.CorporationRepository;
import com.beyond.MKX.domain.stock.dto.StockDetailResDTO;
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
    private final CorporationRepository corporationRepository;

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

    /**
     * GET_STOCK_DETAIL: 종목 상세 정보 조회 (Corporation 정보 포함)
     * @param ticker 종목 코드
     * @return StockDetailResDTO (종목 + 기업 정보)
     */
    public StockDetailResDTO getStockDetail(String ticker) {
        // Stock 조회
        Stock stock = stockRepository.findByTicker(ticker)
                .orElseThrow(() -> new IllegalArgumentException("종목을 찾을 수 없습니다: " + ticker));
        
        // Corporation 조회
        Corporation corporation = corporationRepository.findById(stock.getCorporationId())
                .orElseThrow(() -> new IllegalArgumentException("기업 정보를 찾을 수 없습니다: " + stock.getCorporationId()));
        
        return StockDetailResDTO.builder()
                .ticker(stock.getTicker())
                .nameKo(stock.getNameKo())
                .totalSharesOutstanding(stock.getTotalSharesOutstanding())
                .listedAt(stock.getCreatedAt())  // 상장일 (Stock 생성일 사용)
                .ownerName(corporation.getOwnerName())  // 대표이사
                .estDate(corporation.getEstDate())  // 설립일
                .nameEng(corporation.getNameEng())  // 영문명
                .build();
    }

    private String emptyToNull(String v) {
        return (v == null || v.isBlank()) ? null : v;
    }
}
