package com.beyond.MKX.domain.market.service;

import com.beyond.MKX.domain.orderbook.dto.enhanced.MarketSummary;
import com.beyond.MKX.domain.price.entity.CurrentPrice;
import com.beyond.MKX.domain.price.service.CurrentPriceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 시장 요약 정보 생성 서비스
 * 
 * CurrentPrice 및 52주 데이터를 기반으로
 * 호가창에 표시할 시장 요약 정보를 생성
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MarketSummaryService {
    
    private final CurrentPriceService currentPriceService;
    
    // 상한가/하한가 제한 비율 (30%)
    private static final BigDecimal UPPER_LIMIT_RATE = BigDecimal.valueOf(1.30);
    private static final BigDecimal LOWER_LIMIT_RATE = BigDecimal.valueOf(0.70);
    
    /**
     * 시장 요약 정보 생성
     * 
     * @param ticker 종목코드
     * @return MarketSummary 또는 null (데이터 없음)
     */
    public MarketSummary buildMarketSummary(String ticker) {
        try {
            CurrentPrice currentPrice = currentPriceService.getCurrentPrice(ticker);
            
            if (currentPrice == null) {
                log.warn("[MARKET-SUMMARY] No current price data for ticker: {}", ticker);
                return createEmptyMarketSummary(ticker);
            }
            
            // 기준가 결정 (전일 종가 우선, 없으면 당일 시가)
            long basePrice = determineBasePrice(currentPrice);
            
            // 상한가/하한가 계산
            long upperLimit = calculateUpperLimit(basePrice);
            long lowerLimit = calculateLowerLimit(basePrice);
            
            // 등락액 계산
            long change = currentPrice.getPrice() - basePrice;
            
            // 등락률 계산
            BigDecimal changeRate = calculateChangeRate(change, basePrice);
            
            // 등락 방향 판단
            String trend = determineTrend(change);
            
            // 거래량 변화율 계산
            BigDecimal volumeChangeRate = calculateVolumeChangeRate(
                    currentPrice.getVolume(), 
                    currentPrice.getPrevVolume()
            );
            
            MarketSummary summary = MarketSummary.builder()
                    // 52주 범위
                    .week52High(currentPrice.getWeek52High())
                    .week52Low(currentPrice.getWeek52Low())
                    
                    // 상한가/하한가
                    .upperLimit(upperLimit)
                    .lowerLimit(lowerLimit)
                    .basePrice(basePrice)
                    
                    // 당일 가격 정보
                    .openPrice(currentPrice.getOpen())
                    .highPrice(currentPrice.getHigh())
                    .lowPrice(currentPrice.getLow())
                    .currentPrice(currentPrice.getPrice())
                    
                    // 거래량 정보
                    .volume(currentPrice.getVolume())
                    .prevVolume(currentPrice.getPrevVolume())
                    .volumeChangeRate(volumeChangeRate)
                    
                    // 등락 정보
                    .prevClose(currentPrice.getPrevClose())
                    .changeFromYesterday(change)
                    .changeRate(changeRate)
                    .trend(trend)
                    
                    // 타임스탬프
                    .timestamp(System.currentTimeMillis())
                    .build();
            
            log.debug("[MARKET-SUMMARY] Built summary: ticker={}, basePrice={}, upperLimit={}, lowerLimit={}, change={}, changeRate={}%", 
                    ticker, basePrice, upperLimit, lowerLimit, change, changeRate);
            
            return summary;
            
        } catch (Exception e) {
            log.error("[MARKET-SUMMARY] Failed to build market summary for ticker: {}", ticker, e);
            return createEmptyMarketSummary(ticker);
        }
    }
    
    /**
     * 기준가 결정
     * 우선순위: 전일 종가 > 당일 시가 > 현재가
     */
    private long determineBasePrice(CurrentPrice currentPrice) {
        if (currentPrice.getPrevClose() > 0) {
            return currentPrice.getPrevClose();
        } else if (currentPrice.getOpen() > 0) {
            return currentPrice.getOpen();
        } else {
            return currentPrice.getPrice();
        }
    }
    
    /**
     * 상한가 계산 (+30%)
     */
    private long calculateUpperLimit(long basePrice) {
        return BigDecimal.valueOf(basePrice)
                .multiply(UPPER_LIMIT_RATE)
                .setScale(0, RoundingMode.HALF_UP)
                .longValue();
    }
    
    /**
     * 하한가 계산 (-30%)
     */
    private long calculateLowerLimit(long basePrice) {
        return BigDecimal.valueOf(basePrice)
                .multiply(LOWER_LIMIT_RATE)
                .setScale(0, RoundingMode.HALF_UP)
                .longValue();
    }
    
    /**
     * 등락률 계산
     * 
     * @param change 등락액
     * @param basePrice 기준가
     * @return 등락률 (%) 또는 0 (basePrice가 0인 경우)
     */
    private BigDecimal calculateChangeRate(long change, long basePrice) {
        if (basePrice <= 0) {
            return BigDecimal.ZERO;
        }
        
        return BigDecimal.valueOf(change)
                .divide(BigDecimal.valueOf(basePrice), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }
    
    /**
     * 등락 방향 판단
     */
    private String determineTrend(long change) {
        if (change > 0) {
            return "RISE";
        } else if (change < 0) {
            return "FALL";
        } else {
            return "STEADY";
        }
    }
    
    /**
     * 거래량 변화율 계산
     * 
     * @param currentVolume 현재 거래량
     * @param prevVolume 전일 거래량
     * @return 변화율 (%) 또는 0 (prevVolume이 null/0인 경우)
     */
    private BigDecimal calculateVolumeChangeRate(BigDecimal currentVolume, BigDecimal prevVolume) {
        if (prevVolume == null || prevVolume.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        
        if (currentVolume == null) {
            currentVolume = BigDecimal.ZERO;
        }
        
        BigDecimal change = currentVolume.subtract(prevVolume);
        
        return change.divide(prevVolume, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }
    
    /**
     * 빈 MarketSummary 생성 (데이터 없을 때)
     */
    private MarketSummary createEmptyMarketSummary(String ticker) {
        return MarketSummary.builder()
                .week52High(0L)
                .week52Low(0L)
                .upperLimit(0L)
                .lowerLimit(0L)
                .basePrice(0L)
                .openPrice(0L)
                .highPrice(0L)
                .lowPrice(0L)
                .currentPrice(0L)
                .volume(BigDecimal.ZERO)
                .prevVolume(BigDecimal.ZERO)
                .volumeChangeRate(BigDecimal.ZERO)
                .prevClose(0L)
                .changeFromYesterday(0L)
                .changeRate(BigDecimal.ZERO)
                .trend("STEADY")
                .timestamp(System.currentTimeMillis())
                .build();
    }
}
