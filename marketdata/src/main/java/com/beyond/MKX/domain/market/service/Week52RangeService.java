package com.beyond.MKX.domain.market.service;

import com.beyond.MKX.domain.execution.entity.Execution;
import com.beyond.MKX.domain.price.entity.CurrentPrice;
import com.beyond.MKX.domain.price.service.CurrentPriceService;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 52주 가격 범위 계산 서비스
 * 
 * InfluxDB에서 52주 데이터를 조회하여 최고/최저가 계산
 * 주의: InfluxDB 쿼리는 비용이 크므로 주기적으로만 실행 권장
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Week52RangeService {
    
    private final InfluxDBClient influxDBClient;
    private final CurrentPriceService currentPriceService;
    
    @Value("${influx.bucket}")
    private String bucket;
    
    @Value("${influx.org}")
    private String organization;
    
    /**
     * 52주 최고/최저가 업데이트 (InfluxDB 쿼리)
     * 
     * 주의: 이 메서드는 비용이 크므로 주기적으로만 실행
     * - 권장: 5분마다 또는 특정 이벤트 시
     * 
     * @param ticker 종목코드
     */
    public void update52WeekRange(String ticker) {
        try {
            // ✅ measurement 이름 수정: "execution" → "executions"
            // InfluxDB에서 52주(365일) 최고가 조회
            String maxFlux = String.format(
                "from(bucket: \"%s\") " +
                "|> range(start: -52w) " +
                "|> filter(fn: (r) => r._measurement == \"%s\") " +  // ✅ 수정
                "|> filter(fn: (r) => r.ticker == \"%s\") " +
                "|> filter(fn: (r) => r._field == \"price\") " +
                "|> max()",
                bucket, Execution.MEASUREMENT, ticker  // ✅ 상수 사용
            );
            
            // InfluxDB에서 52주(365일) 최저가 조회
            String minFlux = String.format(
                "from(bucket: \"%s\") " +
                "|> range(start: -52w) " +
                "|> filter(fn: (r) => r._measurement == \"%s\") " +  // ✅ 수정
                "|> filter(fn: (r) => r.ticker == \"%s\") " +
                "|> filter(fn: (r) => r._field == \"price\") " +
                "|> min()",
                bucket, Execution.MEASUREMENT, ticker  // ✅ 상수 사용
            );
            
            Long week52High = null;  // ✅ 초기값을 null로 변경 (데이터 없을 때 구분)
            Long week52Low = null;   // ✅ 초기값을 null로 변경
            
            // 최고가 조회
            try {
                List<FluxTable> maxTables = influxDBClient.getQueryApi().query(maxFlux, organization);
                for (FluxTable table : maxTables) {
                    for (FluxRecord record : table.getRecords()) {
                        Object value = record.getValue();
                        if (value instanceof Number) {
                            week52High = ((Number) value).longValue();
                            log.debug("[WEEK52] 최고가 조회 성공: ticker={}, high={}", ticker, week52High);
                        }
                    }
                }
                
                // ✅ 데이터가 없을 때 로그
                if (week52High == null) {
                    log.warn("[WEEK52] 52주 최고가 데이터가 없습니다: ticker={}", ticker);
                }
                
            } catch (Exception e) {
                log.error("[WEEK52] 52주 최고가 조회 실패: ticker={}", ticker, e);
            }
            
            // 최저가 조회
            try {
                List<FluxTable> minTables = influxDBClient.getQueryApi().query(minFlux, organization);
                for (FluxTable table : minTables) {
                    for (FluxRecord record : table.getRecords()) {
                        Object value = record.getValue();
                        if (value instanceof Number) {
                            week52Low = ((Number) value).longValue();
                            log.debug("[WEEK52] 최저가 조회 성공: ticker={}, low={}", ticker, week52Low);
                        }
                    }
                }
                
                // ✅ 데이터가 없을 때 로그
                if (week52Low == null) {
                    log.warn("[WEEK52] 52주 최저가 데이터가 없습니다: ticker={}", ticker);
                }
                
            } catch (Exception e) {
                log.error("[WEEK52] 52주 최저가 조회 실패: ticker={}", ticker, e);
            }
            
            // ✅ 최고가와 최저가 모두 있을 때만 업데이트
            if (week52High != null && week52Low != null) {
                // ✅ 논리 검증: 최고가가 최저가보다 작으면 안됨
                if (week52High < week52Low) {
                    log.error("[WEEK52] 데이터 오류: 최고가({})가 최저가({})보다 작습니다. ticker={}", 
                            week52High, week52Low, ticker);
                    return;
                }
                
                // CurrentPrice 업데이트
                CurrentPrice currentPrice = currentPriceService.getCurrentPrice(ticker);
                if (currentPrice != null) {
                    currentPrice.setWeek52High(week52High);
                    currentPrice.setWeek52Low(week52Low);
                    currentPriceService.saveCurrentPrice(currentPrice);
                    
                    log.info("[WEEK52] 52주 범위 업데이트 완료: ticker={}, high={}, low={}", 
                        ticker, week52High, week52Low);
                } else {
                    log.warn("[WEEK52] CurrentPrice를 찾을 수 없습니다: ticker={}", ticker);
                }
            } else {
                log.warn("[WEEK52] 52주 데이터가 불완전합니다: ticker={}, high={}, low={}", 
                        ticker, week52High, week52Low);
            }
            
        } catch (Exception e) {
            log.error("[WEEK52] 52주 범위 업데이트 실패: ticker={}", ticker, e);
        }
    }
    
    /**
     * 여러 종목의 52주 범위 일괄 업데이트
     * 
     * @param tickers 종목 코드 리스트
     */
    public void update52WeekRangeBulk(List<String> tickers) {
        log.info("[WEEK52] 52주 범위 일괄 업데이트 시작: {} 종목", tickers.size());
        
        int successCount = 0;
        int failCount = 0;
        
        for (String ticker : tickers) {
            try {
                update52WeekRange(ticker);
                successCount++;
            } catch (Exception e) {
                log.error("[WEEK52] 종목 {} 업데이트 실패", ticker, e);
                failCount++;
            }
        }
        
        log.info("[WEEK52] 52주 범위 일괄 업데이트 완료: 성공={}, 실패={}", successCount, failCount);
    }
}
