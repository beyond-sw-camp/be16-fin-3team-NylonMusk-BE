package com.beyond.MKX.domain.market.service;

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
            // InfluxDB에서 52주(365일) 최고가 조회
            String maxFlux = String.format(
                "from(bucket: \"%s\") " +
                "|> range(start: -52w) " +
                "|> filter(fn: (r) => r._measurement == \"execution\") " +
                "|> filter(fn: (r) => r.ticker == \"%s\") " +
                "|> filter(fn: (r) => r._field == \"price\") " +
                "|> max()",
                bucket, ticker
            );
            
            // InfluxDB에서 52주(365일) 최저가 조회
            String minFlux = String.format(
                "from(bucket: \"%s\") " +
                "|> range(start: -52w) " +
                "|> filter(fn: (r) => r._measurement == \"execution\") " +
                "|> filter(fn: (r) => r.ticker == \"%s\") " +
                "|> filter(fn: (r) => r._field == \"price\") " +
                "|> min()",
                bucket, ticker
            );
            
            Long week52High = 0L;
            Long week52Low = 0L;
            
            // 최고가 조회
            try {
                List<FluxTable> maxTables = influxDBClient.getQueryApi().query(maxFlux, organization);
                for (FluxTable table : maxTables) {
                    for (FluxRecord record : table.getRecords()) {
                        Object value = record.getValue();
                        if (value instanceof Number) {
                            week52High = ((Number) value).longValue();
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to get 52-week high for ticker: {}", ticker, e);
            }
            
            // 최저가 조회
            try {
                List<FluxTable> minTables = influxDBClient.getQueryApi().query(minFlux, organization);
                for (FluxTable table : minTables) {
                    for (FluxRecord record : table.getRecords()) {
                        Object value = record.getValue();
                        if (value instanceof Number) {
                            week52Low = ((Number) value).longValue();
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to get 52-week low for ticker: {}", ticker, e);
            }
            
            // CurrentPrice 업데이트
            CurrentPrice currentPrice = currentPriceService.getCurrentPrice(ticker);
            if (currentPrice != null) {
                currentPrice.setWeek52High(week52High);
                currentPrice.setWeek52Low(week52Low);
                currentPriceService.saveCurrentPrice(currentPrice);
                
                log.info("Updated 52-week range: ticker={}, high={}, low={}", 
                    ticker, week52High, week52Low);
            } else {
                log.warn("No current price found for ticker: {}, cannot update 52-week range", ticker);
            }
            
        } catch (Exception e) {
            log.error("Failed to update 52-week range for ticker: {}", ticker, e);
        }
    }
}
