package com.beyond.MKX.domain.execution.service;

import com.beyond.MKX.domain.execution.dto.ExecutionEventDTO;
import com.beyond.MKX.domain.execution.entity.Execution;
import com.beyond.MKX.domain.execution.repository.ExecutionInfluxRepository;
// import com.beyond.MKX.domain.execution.stomp.ExecutionStompController; // ✅ 순환 참조 방지: 제거
import com.beyond.MKX.domain.orderbook.service.OrderBookService;
import com.beyond.MKX.domain.orderbook.service.OrderBookStatisticsService;
import com.beyond.MKX.domain.chart.service.ChartService;
import com.beyond.MKX.domain.price.service.CurrentPriceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

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
    private final OrderBookStatisticsService orderBookStatisticsService;
    private final ChartService chartService;
    private final CurrentPriceService currentPriceService;
    // private final ExecutionStompController executionStompController; // ✅ 순환 참조 방지: 제거
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    // Redis Pub/Sub 채널명
    private static final String REDIS_CHANNEL = "market:trades";

    /**
     * 체결 이벤트 처리
     * 1. InfluxDB에 저장
     * 2. 현재가 업데이트 (체결가 = 현재가)
     * 3. 호가 업데이트
     * 4. 차트 업데이트
     * 5. 실시간 체결 WebSocket 브로드캐스트
     * 6. 체결강도 업데이트 (Redis)
     */
    public void processExecution(ExecutionEventDTO executionEventDTO) {
        try {
            log.info("[EXECUTION] Processing: execId={}, ticker={}, side={}, price={}, qty={}", 
                    executionEventDTO.getExecId(), executionEventDTO.getTicker(), 
                    executionEventDTO.getSide(), executionEventDTO.getPrice(), 
                    executionEventDTO.getQuantity());

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

            // 5. 실시간 체결 STOMP 브로드캐스트 (Redis Pub/Sub) - 직접 발행
            publishExecution(executionEventDTO);

            // 6. 체결강도 업데이트 (Redis)
            orderBookStatisticsService.updateExecutionVolume(
                    executionEventDTO.getTicker(),
                    executionEventDTO.getSide(),
                    executionEventDTO.getQuantity()
            );

            log.info("[EXECUTION] ✅ Successfully processed: execId={}, ticker={}, price={}, quantity={}",
                    executionEventDTO.getExecId(), executionEventDTO.getTicker(),
                    executionEventDTO.getPrice(), executionEventDTO.getQuantity());

        } catch (Exception e) {
            log.error("[EXECUTION] ❌ Failed to process execution: {}", executionEventDTO, e);
            throw new RuntimeException("Failed to process execution", e);
        }
    }

    /**
     * 최근 체결 데이터 조회 (STOMP 초기 구독 시 사용)
     * 
     * @param ticker 종목 코드
     * @param limit 조회할 체결 개수
     * @return 최근 체결 데이터 리스트
     */
    public java.util.List<ExecutionEventDTO> getRecentExecutions(String ticker, int limit) {
        try {
            // 현재 시각 기준으로 최근 1시간 데이터 조회
            Instant end = Instant.now();
            Instant start = end.minus(1, java.time.temporal.ChronoUnit.HOURS);
            
            // InfluxDB에서 체결 데이터 조회
            java.util.List<Execution> executions = executionInfluxRepository.findExecutions(ticker, start, end);
            
            // 최신 순으로 정렬 후 limit 개수만큼 DTO로 변환하여 반환
            return executions.stream()
                    .sorted(java.util.Comparator.comparing(Execution::getTimestamp).reversed())
                    .limit(limit)
                    .map(this::convertToDTO)
                    .collect(java.util.stream.Collectors.toList());
                    
        } catch (Exception e) {
            log.error("[EXECUTION/QUERY] Failed to get recent executions: ticker={}, limit={}", 
                    ticker, limit, e);
            return java.util.Collections.emptyList();
        }
    }

    /**
     * Entity를 DTO로 변환
     */
    private ExecutionEventDTO convertToDTO(Execution entity) {
        return ExecutionEventDTO.builder()
                .execId(entity.getExecId())
                .marketOrderId(entity.getMarketOrderId())
                .counterOrderId(entity.getCounterOrderId())
                .ticker(entity.getTicker())
                .side(entity.getSide())
                .price(entity.getPrice())
                .quantity(entity.getQuantity())
                .timestamp(entity.getTimestamp().toEpochMilli())
                .build();
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

    /**
     * 체결 데이터를 Redis Pub/Sub으로 발행 (순환 참조 방지)
     *
     * 채널: market:trades (ticker 정보는 메시지 내부에 포함)
     * RedisPubSubListener가 수신하여 /topic/trades/{ticker}로 전송
     *
     * @param execution 체결 데이터
     */
    private void publishExecution(ExecutionEventDTO execution) {
        try {
            String ticker = execution.getTicker();
            
            // 메시지 구성
            Map<String, Object> message = new HashMap<>();
            message.put("type", "execution");
            message.put("ticker", ticker);
            message.put("data", Map.of(
                    "execId", execution.getExecId(),
                    "ticker", execution.getTicker(),
                    "side", execution.getSide(),
                    "price", execution.getPrice(),
                    "quantity", execution.getQuantity(),
                    "timestamp", execution.getTimestamp()
            ));
            message.put("timestamp", System.currentTimeMillis());
            
            // JSON 직렬화
            String messageJson = objectMapper.writeValueAsString(message);
            
            // Redis Pub/Sub 발행
            redisTemplate.convertAndSend(REDIS_CHANNEL, messageJson);
            
            log.debug("[EXECUTION-STOMP] 📤 Published: channel={}, ticker={}, price={}, qty={}",
                    REDIS_CHANNEL, ticker, execution.getPrice(), execution.getQuantity());
            
        } catch (Exception e) {
            log.error("[EXECUTION-STOMP] ❌ Failed to publish: ticker={}",
                    execution.getTicker(), e);
        }
    }
}
