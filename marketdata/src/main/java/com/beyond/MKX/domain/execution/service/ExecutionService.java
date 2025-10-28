package com.beyond.MKX.domain.execution.service;

import com.beyond.MKX.domain.execution.dto.ExecutionEventDTO;
import com.beyond.MKX.domain.execution.entity.Execution;
import com.beyond.MKX.domain.execution.repository.ExecutionInfluxRepository;
import com.beyond.MKX.domain.execution.websocket.ExecutionWebSocketHandler;
import com.beyond.MKX.domain.orderbook.service.OrderBookService;
import com.beyond.MKX.domain.orderbook.service.OrderBookStatisticsService;
import com.beyond.MKX.domain.chart.service.ChartService;
import com.beyond.MKX.domain.price.service.CurrentPriceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

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
    private final ExecutionWebSocketHandler executionWebSocketHandler;

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

            // 5. 실시간 체결 WebSocket 브로드캐스트
            executionWebSocketHandler.broadcastExecution(executionEventDTO);

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
