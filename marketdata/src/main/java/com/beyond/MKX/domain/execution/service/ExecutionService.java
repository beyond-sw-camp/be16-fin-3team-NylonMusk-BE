package com.beyond.MKX.domain.execution.service;

import com.beyond.MKX.domain.execution.dto.ExecutionEventDTO;
import com.beyond.MKX.domain.execution.entity.Execution;
import com.beyond.MKX.domain.execution.repository.ExecutionInfluxRepository;
import com.beyond.MKX.domain.execution.websocket.ExecutionWebSocketHandler;
import com.beyond.MKX.domain.orderbook.service.OrderBookService;
import com.beyond.MKX.domain.chart.service.ChartService;
import com.beyond.MKX.domain.price.service.CurrentPriceService;
import com.beyond.MKX.domain.price.entity.CurrentPrice;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

/**
 * 체결 데이터 처리 서비스
 * 
 * Kafka로부터 수신한 체결 데이터를 처리하고 InfluxDB에 저장
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExecutionService {

    private final ExecutionInfluxRepository executionInfluxRepository;
    private final OrderBookService orderBookService;
    private final ChartService chartService;
    private final CurrentPriceService currentPriceService;
    private final ExecutionWebSocketHandler executionWebSocketHandler;
    private final RedisTemplate<String, Object> redisTemplate;

    // Redis key prefix for execution volumes
    private static final String BUY_EXECUTION_VOLUME_KEY = "exec:buy:volume:";
    private static final String SELL_EXECUTION_VOLUME_KEY = "exec:sell:volume:";

    /**
     * 체결 이벤트 처리
     * 1. InfluxDB에 저장
     * 2. 현재가 업데이트 (체결가 = 현재가)
     * 3. 호가 업데이트
     * 4. 차트 업데이트
     * 5. 실시간 체결 WebSocket 브로드캐스트
     * 6. 체결강도 업데이트
     */
    public void processExecution(ExecutionEventDTO executionEventDTO) {
        try {
            log.info("Processing execution: {}", executionEventDTO);

            // DTO를 Entity로 변환
            Execution execution = convertToEntity(executionEventDTO);

            // 1. InfluxDB에 저장
            executionInfluxRepository.save(execution);

            // 2. 현재가 업데이트 (체결가가 현재가가 됨 - 증권거래소와 동일)
            currentPriceService.updateCurrentPrice(executionEventDTO);

            // 3. 호가 업데이트 (체결된 주문은 호가에서 제거)
            orderBookService.updateOrderBookAfterExecution(executionEventDTO);

            // 4. 차트 업데이트 (실시간 차트 데이터 갱신)
            chartService.updateChartData(executionEventDTO);

            // 5. 실시간 체결 WebSocket 브로드캐스트
            executionWebSocketHandler.broadcastExecution(executionEventDTO);

            // 6. 체결강도 업데이트
            updateExecutionStrength(executionEventDTO);

            log.info("Successfully processed execution: execId={}, ticker={}, price={}, quantity={}",
                    executionEventDTO.getExecId(), executionEventDTO.getTicker(),
                    executionEventDTO.getPrice(), executionEventDTO.getQuantity());

        } catch (Exception e) {
            log.error("Failed to process execution: {}", executionEventDTO, e);
            throw new RuntimeException("Failed to process execution", e);
        }
    }

    /**
     * 체결강도 계산 및 업데이트
     * 체결강도 = (매수 체결량 / 매도 체결량) × 100
     */
    private void updateExecutionStrength(ExecutionEventDTO execution) {
        try {
            String ticker = execution.getTicker();
            String side = execution.getSide();
            BigDecimal quantity = execution.getQuantity();
            
            if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
                return;
            }
            
            // Redis에 매수/매도 체결량 누적
            String volumeKey = "BUY".equals(side) ? 
                BUY_EXECUTION_VOLUME_KEY + ticker : 
                SELL_EXECUTION_VOLUME_KEY + ticker;
            
            redisTemplate.opsForValue().increment(volumeKey, quantity.doubleValue());
            
            // TTL 설정 (5분)
            redisTemplate.expire(volumeKey, 5, TimeUnit.MINUTES);
            
            // 체결강도 계산
            Object buyVolumeObj = redisTemplate.opsForValue()
                .get(BUY_EXECUTION_VOLUME_KEY + ticker);
            Object sellVolumeObj = redisTemplate.opsForValue()
                .get(SELL_EXECUTION_VOLUME_KEY + ticker);
            
            Double buyVolume = buyVolumeObj != null ? ((Number) buyVolumeObj).doubleValue() : 0.0;
            Double sellVolume = sellVolumeObj != null ? ((Number) sellVolumeObj).doubleValue() : 0.0;
            
            if (buyVolume > 0 && sellVolume > 0) {
                BigDecimal strength = BigDecimal.valueOf(buyVolume)
                    .divide(BigDecimal.valueOf(sellVolume), 2, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
                
                // CurrentPrice에 체결강도 업데이트
                CurrentPrice currentPrice = currentPriceService.getCurrentPrice(ticker);
                if (currentPrice != null) {
                    currentPrice.setExecutionStrength(strength);
                    currentPriceService.saveCurrentPrice(currentPrice);
                    
                    log.debug("[EXEC-STRENGTH] ticker={}, buyVol={}, sellVol={}, strength={}",
                        ticker, buyVolume, sellVolume, strength);
                }
            } else if (buyVolume > 0 || sellVolume > 0) {
                // 한쪽만 있을 경우 0으로 설정 (데이터 부족)
                CurrentPrice currentPrice = currentPriceService.getCurrentPrice(ticker);
                if (currentPrice != null) {
                    currentPrice.setExecutionStrength(BigDecimal.ZERO);
                    currentPriceService.saveCurrentPrice(currentPrice);
                }
            }
            
        } catch (Exception e) {
            log.error("Failed to update execution strength for ticker: {}", 
                execution.getTicker(), e);
        }
    }

    /**
     * DTO를 Entity로 변환
     */
    private Execution convertToEntity(ExecutionEventDTO dto) {
        return Execution.builder()
                .execId(dto.getExecId())
                .marketOrderId(dto.getMarketOrderId())
                .counterOrderId(dto.getCounterOrderId())
                .ticker(dto.getTicker())
                .side(dto.getSide())
                .price(dto.getPrice())
                .quantity(dto.getQuantity())
                .timestamp(Instant.ofEpochMilli(dto.getTimestamp()))
                .build();
    }
}
