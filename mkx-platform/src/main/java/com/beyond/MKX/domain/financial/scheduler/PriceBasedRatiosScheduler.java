package com.beyond.MKX.domain.financial.scheduler;

import com.beyond.MKX.common.apiResponse.CommonDTO;
import com.beyond.MKX.domain.delisting.client.CurrentPriceClient;
import com.beyond.MKX.domain.delisting.dto.CurrentPriceResDto;
import com.beyond.MKX.domain.financial.entity.CompanyFinancials;
import com.beyond.MKX.domain.financial.repository.CompanyFinancialsRepository;
import com.beyond.MKX.domain.financial.util.FinancialRatiosAutoCalculator;
import com.beyond.MKX.domain.stock.entity.Stock;
import com.beyond.MKX.domain.stock.entity.StockPriceRatios;
import com.beyond.MKX.domain.stock.repository.StockPriceRatiosRepository;
import com.beyond.MKX.domain.stock.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * PRICE_BASED_RATIOS_SCHEDULER: 현재가 기반 재무비율 자동 계산 스케줄러
 * - 1시간마다 실행 (매 정시: 00:00, 01:00, 02:00, ...)
 * - 모든 LISTED 상태의 Stock에 대해 PER, PBR, PSR, 시가총액 계산 및 업데이트
 * - StockPriceRatios 엔티티에 저장 (분기/연도와 무관한 실시간 비율)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PriceBasedRatiosScheduler {
    
    private final StockRepository stockRepository;
    private final StockPriceRatiosRepository stockPriceRatiosRepository;
    private final CompanyFinancialsRepository companyFinancialsRepository;
    private final CurrentPriceClient currentPriceClient;
    private final FinancialRatiosAutoCalculator calculator;
    
        /**
         * 1시간마다 실행 (매 정시: 00:00, 01:00, 02:00, ...)
         * - 현재가 기반 재무비율 계산 (PER, PBR, PSR, 시가총액)
         * - StockPriceRatios 엔티티에 저장
         */
        @Scheduled(cron = "0 0 * * * *") // 1시간마다 (매 정시)
    @Transactional
    public void updatePriceBasedRatios() {
        log.info("[SCHEDULER] 시작: 현재가 기반 재무비율 및 시가총액 업데이트");
        
        // 1. 모든 LISTED 상태의 Stock 조회
        List<Stock> listedStocks = stockRepository.findByStatus(Stock.Status.LISTED);
        
        int successCount = 0;
        int failCount = 0;
        
        for (Stock stock : listedStocks) {
            try {
                updateRatiosAndMarketCapForStock(stock);
                successCount++;
            } catch (Exception e) {
                log.warn("[SCHEDULER] 실패: ticker={}, error={}", 
                    stock.getTicker(), e.getMessage(), e);
                failCount++;
            }
        }
        
        log.info("[SCHEDULER] 완료: 성공={}, 실패={}", successCount, failCount);
    }
    
    /**
     * 단일 종목에 대한 재무비율 및 시가총액 업데이트
     */
    private void updateRatiosAndMarketCapForStock(Stock stock) {
        // 2. 현재가 조회 (market-data-service)
        CommonDTO<CurrentPriceResDto> priceResponse = currentPriceClient
            .getCurrentPrice(stock.getTicker());
        
        if (priceResponse == null || priceResponse.getResult() == null) {
            log.debug("[SCHEDULER] 현재가 없음: ticker={}", stock.getTicker());
            return;
        }
        
        CurrentPriceResDto currentPriceDto = priceResponse.getResult();
        Long currentPrice = currentPriceDto.price();
        
        if (currentPrice == null || currentPrice == 0) {
            log.debug("[SCHEDULER] 현재가가 0: ticker={}", stock.getTicker());
            return;
        }
        
        // 3. 시가총액 계산
        Long marketCap = currentPrice * stock.getTotalSharesOutstanding();
        
        log.debug("[SCHEDULER] 시가총액 계산: ticker={}, marketCap={}", 
            stock.getTicker(), marketCap);
        
        // 4. 최신 재무제표 데이터 조회 (PER, PBR, PSR 계산용)
        Optional<CompanyFinancials> latestFinancialOpt = companyFinancialsRepository
            .findFirstByStockIdOrderByFiscalYearDescFiscalQuarterDesc(stock.getId());
        
        if (latestFinancialOpt.isEmpty()) {
            log.debug("[SCHEDULER] 재무제표 데이터 없음: ticker={}", stock.getTicker());
            // 재무제표가 없어도 현재가와 시가총액은 저장 (Enterprise Value는 시가총액과 동일)
            savePriceRatios(stock, currentPrice, marketCap, marketCap, null, null, null);
            return;
        }
        
        CompanyFinancials cf = latestFinancialOpt.get();
        
        // 5. PER, PBR, PSR, Enterprise Value 계산
        BigDecimal eps = cf.getEps();
        BigDecimal bps = calculator.calculateBPS(cf, stock.getTotalSharesOutstanding());
        Long revenue = cf.getRevenue();
        Long totalLiabilities = cf.getTotalLiabilities();
        
        BigDecimal per = calculator.calculatePER(currentPrice, eps);
        BigDecimal pbr = calculator.calculatePBR(currentPrice, bps);
        BigDecimal psr = calculator.calculatePSR(marketCap, revenue);
        
        // Enterprise Value = 시가총액 + 순부채
        // 순부채 = 총부채 - 현금 및 현금성 자산
        // 현재는 현금 필드가 없으므로 총부채를 순부채로 사용 (근사값)
        Long netDebt = totalLiabilities != null ? totalLiabilities : 0L;
        Long enterpriseValue = marketCap + netDebt;
        
        // 6. StockPriceRatios에 저장 (분기/연도와 무관한 실시간 비율)
        savePriceRatios(stock, currentPrice, marketCap, enterpriseValue, per, pbr, psr);
        
        log.debug("[SCHEDULER] 재무비율 업데이트: ticker={}, PER={}, PBR={}, PSR={}, EnterpriseValue={}", 
            stock.getTicker(), per, pbr, psr, enterpriseValue);
    }
    
    /**
     * StockPriceRatios 저장 (없으면 생성, 있으면 업데이트)
     */
    private void savePriceRatios(Stock stock, Long currentPrice, Long marketCap, Long enterpriseValue,
                                 BigDecimal per, BigDecimal pbr, BigDecimal psr) {
        StockPriceRatios priceRatios = stockPriceRatiosRepository
            .findByStockId(stock.getId())
            .orElseGet(() -> {
                log.debug("[SCHEDULER] 새 StockPriceRatios 생성: ticker={}, stockId={}", 
                    stock.getTicker(), stock.getId());
                return StockPriceRatios.builder()
                    .stockId(stock.getId())
                    .build();
            });
        
        boolean isNew = priceRatios.getId() == null;
        
        priceRatios.setCurrentPrice(currentPrice);
        priceRatios.setMarketCap(marketCap);
        priceRatios.setEnterpriseValue(enterpriseValue);
        priceRatios.setPer(per);
        priceRatios.setPbr(pbr);
        priceRatios.setPsr(psr);
        
        StockPriceRatios saved = stockPriceRatiosRepository.save(priceRatios);
        
        // 명시적으로 flush하여 즉시 DB에 반영
        stockPriceRatiosRepository.flush();
        
        log.info("[SCHEDULER] StockPriceRatios 저장 완료: ticker={}, id={}, isNew={}, currentPrice={}, marketCap={}, enterpriseValue={}", 
            stock.getTicker(), saved.getId(), isNew, currentPrice, marketCap, enterpriseValue);
    }
}

