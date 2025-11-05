package com.beyond.MKX.domain.stock.service;

import com.beyond.MKX.domain.corporation.entity.Corporation;
import com.beyond.MKX.domain.corporation.repository.CorporationRepository;
import com.beyond.MKX.domain.stock.dto.CardSectionDataDTO;
import com.beyond.MKX.domain.stock.dto.StockBriefDTO;
import com.beyond.MKX.domain.stock.dto.StockDetailResDTO;
import com.beyond.MKX.domain.stock.dto.StockListResDto;
import com.beyond.MKX.domain.stock.dto.StockInfoResDTO;
import com.beyond.MKX.domain.stock.dto.StockPriceRatiosResDTO;
import com.beyond.MKX.domain.stock.entity.Stock;
import com.beyond.MKX.domain.stock.entity.StockPriceRatios;
import com.beyond.MKX.domain.stock.repository.StockPriceRatiosRepository;
import com.beyond.MKX.domain.stock.repository.StockRepository;
import com.beyond.MKX.domain.stockfavorite.repository.StockFavoritesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockQueryService {

    private final StockRepository stockRepository;
    private final CorporationRepository corporationRepository;
    private final StockPriceRatiosRepository stockPriceRatiosRepository;
    private final StockFavoritesRepository stockFavoritesRepository;

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
        
        // StockPriceRatios 조회 (시가총액, 기업 가치 포함)
        Long marketCap = null;
        Long enterpriseValue = null;
        var priceRatiosOpt = stockPriceRatiosRepository.findByStockId(stock.getId());
        if (priceRatiosOpt.isPresent()) {
            StockPriceRatios priceRatios = priceRatiosOpt.get();
            marketCap = priceRatios.getMarketCap();
            enterpriseValue = priceRatios.getEnterpriseValue();
        }
        
        return StockDetailResDTO.builder()
                .ticker(stock.getTicker())
                .nameKo(stock.getNameKo())
                .imageUrl(stock.getImageUrl())
                .totalSharesOutstanding(stock.getTotalSharesOutstanding())
                .listedAt(stock.getCreatedAt())  // 상장일 (Stock 생성일 사용)
                .ownerName(corporation.getOwnerName())  // 대표이사
                .estDate(corporation.getEstDate())  // 설립일
                .nameEng(corporation.getNameEng())  // 영문명
                .marketCap(marketCap)  // 시가총액 (StockPriceRatios에서 조회, 1시간마다 업데이트)
                .enterpriseValue(enterpriseValue)  // 기업 가치 (시가총액 + 순부채, 1시간마다 업데이트)
                .build();
    }

    /**
     * 종목의 현재가 기반 재무비율 조회
     * GET /api/stocks/{ticker}/price-ratios
     * @param ticker 종목 코드
     * @return StockPriceRatiosResDTO (PER, PBR, PSR, 시가총액, Enterprise Value)
     */
    public StockPriceRatiosResDTO getStockPriceRatios(String ticker) {
        // Stock 존재 여부 확인
        Stock stock = stockRepository.findByTicker(ticker)
                .orElseThrow(() -> new IllegalArgumentException("종목을 찾을 수 없습니다: " + ticker));
        
        // StockPriceRatios 조회
        StockPriceRatios priceRatios = stockPriceRatiosRepository.findByTicker(ticker)
                .orElse(null);
        
        if (priceRatios == null) {
            // 데이터가 없으면 null 반환 (프론트엔드에서 처리)
            return null;
        }
        
        return StockPriceRatiosResDTO.builder()
                .ticker(ticker)
                .currentPrice(priceRatios.getCurrentPrice())
                .marketCap(priceRatios.getMarketCap())
                .enterpriseValue(priceRatios.getEnterpriseValue())
                .per(priceRatios.getPer())
                .pbr(priceRatios.getPbr())
                .psr(priceRatios.getPsr())
                .build();
    }

    private String emptyToNull(String v) {
        return (v == null || v.isBlank()) ? null : v;
    }

    /**
     * 카드섹션 데이터 조회 (인기 + 신규 종목)
     * 
     * 한번의 호출로 인기 종목과 신규 종목을 함께 조회
     * 
     * @param limit 각 섹션별 조회 개수
     * @return CardSectionDataDTO (인기 + 신규)
     */
    public CardSectionDataDTO getCardSectionData(int limit) {
        // 1. 인기 종목 TOP N (즐겨찾기 많은 순)
        List<Stock> popularStocks = stockFavoritesRepository.findTopPopularStocks(limit);
        List<StockBriefDTO> popular = popularStocks.stream()
                .map(this::convertToStockBriefDTO)
                .collect(Collectors.toList());

        // 2. 신규 종목 TOP N (최근 상장 순, LISTED만)
        PageRequest pageRequest = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
        List<Stock> newestStocks = stockRepository.findByStatus(Stock.Status.LISTED, pageRequest).getContent();
        List<StockBriefDTO> newest = newestStocks.stream()
                .map(this::convertToStockBriefDTO)
                .collect(Collectors.toList());

        return CardSectionDataDTO.builder()
                .popular(popular)
                .newest(newest)
                .build();
    }

    /**
     * Stock 엔티티를 StockBriefDTO로 변환
     */
    private StockBriefDTO convertToStockBriefDTO(Stock stock) {
        return StockBriefDTO.builder()
                .id(stock.getId())
                .ticker(stock.getTicker())
                .nameKo(stock.getNameKo())
                .status(stock.getStatus())
                .delistingStage(stock.getDelistingStage())
                .imageUrl(stock.getImageUrl())
                .totalSharesOutstanding(stock.getTotalSharesOutstanding())
                .build();
    }
}
