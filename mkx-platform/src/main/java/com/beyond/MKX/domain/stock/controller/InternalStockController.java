package com.beyond.MKX.domain.stock.controller;

import com.beyond.MKX.domain.stock.dto.CardSectionDataDTO;
import com.beyond.MKX.domain.stock.dto.StockBriefDTO;
import com.beyond.MKX.domain.stock.dto.StockInfoResDTO;
import com.beyond.MKX.domain.stock.entity.Stock;
import com.beyond.MKX.domain.stock.repository.StockRepository;
import com.beyond.MKX.domain.stock.service.StockQueryService;
import com.beyond.MKX.domain.stockfavorite.repository.StockFavoritesRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 내부 전용 Stock 조회 API
 * 
 * marketdata 서비스 등 내부 시스템이 종목 정보를 조회할 때 사용
 */
@Slf4j
@RestController
@RequestMapping("/api/internal/stocks")
@RequiredArgsConstructor
@Validated
public class InternalStockController {

    private final StockRepository stockRepository;
    private final StockQueryService stockQueryService;
    private final StockFavoritesRepository stockFavoritesRepository;

    /**
     * 종목 이름 조회 (리스트)
     * 
     * @param tickers 종목 코드 리스트
     * @return ticker -> nameKo 맵
     */
    @GetMapping("/names")
    public Map<String, String> getNames(@RequestParam("tickers") List<String> tickers) {
        return stockRepository.findAll().stream()
                .filter(s -> tickers.contains(s.getTicker()))
                .collect(Collectors.toMap(
                        Stock::getTicker,
                        Stock::getNameKo,
                        (a, b) -> a
                ));
    }

    /**
     * 종목 정보 리스트 조회
     * 
     * @param tickers 종목 코드 리스트
     * @return 종목 상세 정보 리스트
     */
    @GetMapping("/briefs")
    public List<StockBriefDTO> getBriefs(@RequestParam("tickers") List<String> tickers) {
        return stockRepository.findAll().stream()
                .filter(s -> tickers.contains(s.getTicker()))
                .map(stock -> StockBriefDTO.builder()
                        .id(stock.getId())
                        .ticker(stock.getTicker())
                        .nameKo(stock.getNameKo())
                        .status(stock.getStatus())
                        .delistingStage(stock.getDelistingStage())
                        .imageUrl(stock.getImageUrl())
                        .totalSharesOutstanding(stock.getTotalSharesOutstanding())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 카드섹션 데이터 조회 (인기 + 신규 종목)
     * 
     * 한번의 호출로 인기 종목과 신규 종목을 함께 조회하여 반환
     * marketdata 서비스의 카드섹션 API에서 사용
     * 
     * @param limit 각 섹션별 조회 개수 (기본값: 3)
     * @return CardSectionDataDTO (인기 TOP N + 신규 TOP N)
     */
    @GetMapping("/card-section-data")
    public CardSectionDataDTO getCardSectionData(
            @RequestParam(defaultValue = "3") int limit) {
        
        log.info("[INTERNAL-API] Fetching card section data: limit={}", limit);
        
        CardSectionDataDTO data = stockQueryService.getCardSectionData(limit);
        
        log.info("[INTERNAL-API] Card section data retrieved: popular={}, newest={}", 
                data.getPopular().size(), data.getNewest().size());
        
        return data;
    }

    @GetMapping("/{ticker}")
    public StockInfoResDTO getStockByTicker(@PathVariable String ticker) {
        return stockQueryService.getStockByTicker(ticker);
    }

    /**
     * 사용자의 즐겨찾기 종목 ticker 리스트 조회
     * 
     * marketdata 서비스의 관심종목 마켓데이터 조회에서 사용
     * 
     * @param memberId 회원 ID
     * @return ticker 리스트
     */
    @GetMapping("/favorites/{memberId}")
    public List<String> getFavoriteTickers(@PathVariable UUID memberId) {
        log.info("[INTERNAL-API] Fetching favorite tickers for memberId: {}", memberId);

        List<Stock> favoriteStocks = stockFavoritesRepository.findFavoriteStocks(memberId);
        List<String> tickers = favoriteStocks.stream()
                .map(Stock::getTicker)
                .collect(Collectors.toList());

        log.info("[INTERNAL-API] Found {} favorite tickers for memberId: {}", tickers.size(), memberId);

        return tickers;
    }

}


