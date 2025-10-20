package com.beyond.MKX.domain.execution.consumer;

import com.beyond.MKX.domain.execution.dto.ExecutionEventDTO;
import com.beyond.MKX.domain.execution.service.ExecutionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * 체결 이벤트 Kafka Consumer
 * 
 * matching-engine에서 발송하는 체결 이벤트를 수신하여 처리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExecutionKafkaConsumer {

    private final ExecutionService executionService;

    /**
     * executions 토픽으로부터 체결 이벤트 수신
     * JsonDeserializer를 사용하여 자동으로 ExecutionEventDTO로 변환
     */
    @KafkaListener(topics = "${kafka.topics.executions}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeExecution(
            @Payload ExecutionEventDTO executionEventDTO,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            Acknowledgment acknowledgment) {
        try {
            log.info("[KAFKA/EXECUTION] 📨 Received from topic={}: execId={}, ticker={}, side={}, price={}, qty={}", 
                    topic,
                    executionEventDTO.getExecId(), executionEventDTO.getTicker(),
                    executionEventDTO.getSide(), executionEventDTO.getPrice(), 
                    executionEventDTO.getQuantity());

            // 체결 이벤트 처리
            executionService.processExecution(executionEventDTO);

            // 수동 커밋
            if (acknowledgment != null) {
                acknowledgment.acknowledge();
            }

            log.info("[KAFKA/EXECUTION] ✅ Successfully processed: execId={}, ticker={}, price={}, quantity={}",
                    executionEventDTO.getExecId(), executionEventDTO.getTicker(),
                    executionEventDTO.getPrice(), executionEventDTO.getQuantity());

        } catch (Exception e) {
            log.error("[KAFKA/EXECUTION] ❌ Failed to process: {}", executionEventDTO, e);
            // 에러 발생 시에도 커밋하여 무한 재처리 방지
            if (acknowledgment != null) {
                acknowledgment.acknowledge();
            }
        }
    }
}
